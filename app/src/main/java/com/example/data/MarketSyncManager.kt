package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarketSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var db: FirebaseFirestore? = null
    private var docRef: DocumentReference? = null
    private var firestoreListener: ListenerRegistration? = null
    
    private val _syncStatus = MutableStateFlow(SyncStatus.INACTIVE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured

    private var activeListId: String? = null
    private var isUpdatingFromRemote = false
    private var localFlowJob: Job? = null

    enum class SyncStatus {
        INACTIVE,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private var lastSyncedHash: Int? = null

    private fun List<MarketItem>.syncHash(): Int {
        // Compute hash ignoring local-only fields
        return this.sortedBy { it.name.lowercase() }.map { 
            "${it.name}|${it.isChecked}|${it.isBought}|${it.price}|${it.quantity}|${it.unit}|${it.category}" 
        }.hashCode()
    }

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            val appId = BuildConfig.FIREBASE_APP_ID

            if (apiKey.isBlank() || apiKey == "MY_FIREBASE_API_KEY" ||
                projectId.isBlank() || projectId == "MY_FIREBASE_PROJECT_ID" ||
                appId.isBlank() || appId == "MY_FIREBASE_APP_ID") {
                Log.w("MarketSyncManager", "Firebase credentials not configured in .env")
                _isConfigured.value = false
                return
            }

            // Check if Firebase is already initialized
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            db = FirebaseFirestore.getInstance(app)
            _isConfigured.value = true
            Log.d("MarketSyncManager", "Firebase programmatically initialized successfully")
        } catch (e: Exception) {
            Log.e("MarketSyncManager", "Error initializing Firebase: ${e.message}", e)
            _isConfigured.value = false
        }
    }

    fun startSync(listId: String) {
        if (!_isConfigured.value || db == null) {
            Log.w("MarketSyncManager", "Firebase not configured. Cannot start sync.")
            return
        }

        stopSync()
        activeListId = listId
        _syncStatus.value = SyncStatus.CONNECTING

        val document = db!!.collection("market_lists").document(listId)
        docRef = document

        // 1. Listen to Remote Changes (Firestore)
        firestoreListener = document.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MarketSyncManager", "Firestore listen failed: ${error.message}")
                _syncStatus.value = SyncStatus.ERROR
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                _syncStatus.value = SyncStatus.CONNECTED
                val remoteItems = snapshot.get("items") as? List<Map<String, Any>>
                if (remoteItems != null) {
                    scope.launch {
                        syncRemoteToLocal(remoteItems)
                    }
                }
            } else {
                // If document does not exist, upload current local list to initialize it
                _syncStatus.value = SyncStatus.CONNECTED
                scope.launch {
                    uploadLocalList()
                }
            }
        }

        // 2. Listen to Local Changes (observe Room Flow)
        localFlowJob = scope.launch {
            repository.pendingMarketItems.collect { localItems ->
                if (!isUpdatingFromRemote && activeListId != null) {
                    uploadLocalList(localItems)
                }
            }
        }
    }

    fun stopSync() {
        firestoreListener?.remove()
        firestoreListener = null
        localFlowJob?.cancel()
        localFlowJob = null
        docRef = null
        activeListId = null
        _syncStatus.value = SyncStatus.INACTIVE
    }

    private suspend fun syncRemoteToLocal(remoteItems: List<Map<String, Any>>) {
        isUpdatingFromRemote = true
        try {
            val localItems = repository.pendingMarketItems.first()
            
            // Map remote list to Kotlin objects
            val remoteParsed = remoteItems.mapNotNull { map ->
                val name = map["name"] as? String ?: return@mapNotNull null
                val isChecked = map["isChecked"] as? Boolean ?: false
                val isBought = map["isBought"] as? Boolean ?: false
                val price = (map["price"] as? Number)?.toDouble() ?: 0.0
                val quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0
                val unit = map["unit"] as? String ?: "un"
                val category = map["category"] as? String ?: "Geral"
                
                MarketItem(
                    name = name,
                    isChecked = isChecked,
                    isBought = isBought,
                    orderIndex = 0,
                    price = price,
                    quantity = quantity,
                    unit = unit,
                    category = category
                )
            }

            // Update hash to avoid local echo upload
            lastSyncedHash = remoteParsed.syncHash()

            val inserts = mutableListOf<MarketItem>()
            val updates = mutableListOf<MarketItem>()
            val deletes = mutableListOf<MarketItem>()

            // Prepare items for batch transaction
            remoteParsed.forEach { remoteItem ->
                val existing = localItems.find { it.name.equals(remoteItem.name, ignoreCase = true) }
                if (existing != null) {
                    if (existing.isChecked != remoteItem.isChecked ||
                        existing.price != remoteItem.price ||
                        existing.quantity != remoteItem.quantity ||
                        existing.unit != remoteItem.unit ||
                        existing.isBought != remoteItem.isBought) {
                        updates.add(existing.copy(
                            isChecked = remoteItem.isChecked,
                            price = remoteItem.price,
                            quantity = remoteItem.quantity,
                            unit = remoteItem.unit,
                            isBought = remoteItem.isBought
                        ))
                    }
                } else {
                    inserts.add(remoteItem)
                }
            }

            localItems.forEach { localItem ->
                val stillExists = remoteParsed.any { it.name.equals(localItem.name, ignoreCase = true) }
                if (!stillExists) {
                    deletes.add(localItem)
                }
            }
            
            // Execute all changes in a single Room transaction
            repository.syncMarketItems(inserts, updates, deletes)
            
        } catch (e: Exception) {
            Log.e("MarketSyncManager", "Error syncing remote to local: ${e.message}", e)
        } finally {
            isUpdatingFromRemote = false
        }
    }

    private suspend fun uploadLocalList() {
        val localItems = repository.pendingMarketItems.first()
        uploadLocalList(localItems)
    }

    private suspend fun uploadLocalList(localItems: List<MarketItem>) {
        val ref = docRef ?: return
        try {
            val currentHash = localItems.syncHash()
            if (currentHash == lastSyncedHash) {
                // Ignore upload if data is structurally identical to last known sync state
                return
            }

            val itemsList = localItems.map { item ->
                mapOf(
                    "name" to item.name,
                    "isChecked" to item.isChecked,
                    "isBought" to item.isBought,
                    "price" to item.price,
                    "quantity" to item.quantity,
                    "unit" to item.unit,
                    "category" to item.category
                )
            }

            val data = mapOf(
                "updatedAt" to System.currentTimeMillis(),
                "items" to itemsList
            )

            lastSyncedHash = currentHash

            // Perform write to Firestore asynchronously
            ref.set(data)
                .addOnFailureListener { e ->
                    Log.e("MarketSyncManager", "Failed to upload list: ${e.message}")
                    lastSyncedHash = null
                }
        } catch (e: Exception) {
            Log.e("MarketSyncManager", "Error preparing local list for upload: ${e.message}", e)
        }
    }
}
