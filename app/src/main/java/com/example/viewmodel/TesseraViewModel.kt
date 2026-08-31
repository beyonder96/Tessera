package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.MarketItem
import com.example.data.PetEvent
import com.example.data.TesseraRepository
import com.example.data.Transaction
import com.example.data.Habit
import com.example.data.PurchaseGoal
import com.example.data.HealthProfile
import com.example.data.Medication
import com.example.data.WeightRecord
import com.example.data.SleepRecord
import com.example.data.MedicationLog
import com.example.data.StepsRecord
import com.example.data.Routine
import com.example.data.RoutineStep
import com.example.data.BibleVerseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

import com.example.data.FootballService
import com.example.data.FootballMatchInfo
import com.example.data.apifootball.NetworkModule
import android.util.Log

class TesseraViewModel(
    private val repository: TesseraRepository,
    private val applicationContext: Context
) : ViewModel() {

    val syncManager = com.example.data.MarketSyncManager(applicationContext, repository)
    val supabaseMarketSync = com.example.data.supabase.SupabaseMarketSyncManager(applicationContext, repository)
    val supabaseFinanceSync = com.example.data.supabase.SupabaseFinanceSyncManager(applicationContext, repository)

    private val marketSharedPrefs = applicationContext.getSharedPreferences("tessera_market_prefs", Context.MODE_PRIVATE)
    val marketListId = MutableStateFlow<String?>(marketSharedPrefs.getString("shared_list_id", null))

    init {
        com.example.data.supabase.SupabaseClientProvider.init(applicationContext)
        supabaseMarketSync.startContinuousSync()
        supabaseFinanceSync.startContinuousSync()

        marketListId.value?.let { id ->
            syncManager.startSync(id)
        }
        loadDailyVerse()
    }

    fun startMarketSharing(listId: String) {
        marketSharedPrefs.edit().putString("shared_list_id", listId).apply()
        marketListId.value = listId
        syncManager.startSync(listId)
    }

    fun stopMarketSharing() {
        marketSharedPrefs.edit().remove("shared_list_id").apply()
        marketListId.value = null
        syncManager.stopSync()
    }

    private val _dailyVerse = MutableStateFlow<BibleVerseResponse?>(null)
    val dailyVerse: StateFlow<BibleVerseResponse?> = _dailyVerse.asStateFlow()

    // ==========================================
    // BÍBLIA SAGRADA (BIBLIAAPI v2)
    // ==========================================
    sealed interface ChapterUiState {
        object Loading : ChapterUiState
        data class Success(val chapterData: com.example.data.BibliaChapterData) : ChapterUiState
        data class Error(val message: String) : ChapterUiState
    }

    private val biblePrefs = applicationContext.getSharedPreferences("tessera_bible_prefs", Context.MODE_PRIVATE)

    val selectedBibleVersion = MutableStateFlow(biblePrefs.getString("selected_version", "NVT") ?: "NVT")
    val selectedBibleBook = MutableStateFlow(
        com.example.data.BibliaBookItem(
            id = biblePrefs.getInt("selected_book_id", 19),
            name = biblePrefs.getString("selected_book_name", "Salmos") ?: "Salmos",
            abbrev = biblePrefs.getString("selected_book_abbrev", "sl") ?: "sl",
            testament = biblePrefs.getString("selected_book_testament", "VT") ?: "VT"
        )
    )
    val selectedBibleChapter = MutableStateFlow(biblePrefs.getInt("selected_chapter", 23))

    private val _bibleVersions = MutableStateFlow<List<com.example.data.BibliaVersionItem>>(emptyList())
    val bibleVersions: StateFlow<List<com.example.data.BibliaVersionItem>> = _bibleVersions.asStateFlow()

    private val _bibleBooks = MutableStateFlow<List<com.example.data.BibliaBookItem>>(emptyList())
    val bibleBooks: StateFlow<List<com.example.data.BibliaBookItem>> = _bibleBooks.asStateFlow()

    private val _chapterUiState = MutableStateFlow<ChapterUiState>(ChapterUiState.Loading)
    val chapterUiState: StateFlow<ChapterUiState> = _chapterUiState.asStateFlow()

    private val _verseHighlights = MutableStateFlow<Map<String, String>>(emptyMap())
    val verseHighlights: StateFlow<Map<String, String>> = _verseHighlights.asStateFlow()

    val targetScrollVerse = MutableStateFlow<Int?>(null)

    fun loadBibleMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            val versions = repository.getBibleVersions()
            _bibleVersions.value = versions
            val books = repository.getBibleBooks()
            _bibleBooks.value = books
        }
    }

    fun loadCurrentChapter() {
        viewModelScope.launch(Dispatchers.IO) {
            _chapterUiState.value = ChapterUiState.Loading
            val version = selectedBibleVersion.value
            val bookAbbrev = selectedBibleBook.value.abbrev
            val chapter = selectedBibleChapter.value

            val result = repository.getBibleChapter(version, bookAbbrev, chapter)
            result.onSuccess { data ->
                _chapterUiState.value = ChapterUiState.Success(data)
                loadHighlightsForCurrentChapter()
            }.onFailure { err ->
                _chapterUiState.value = ChapterUiState.Error(err.message ?: "Erro ao carregar capítulo")
            }
        }
    }

    fun selectBookAndChapter(book: com.example.data.BibliaBookItem, chapter: Int) {
        selectedBibleBook.value = book
        selectedBibleChapter.value = chapter
        biblePrefs.edit()
            .putInt("selected_book_id", book.id)
            .putString("selected_book_name", book.name)
            .putString("selected_book_abbrev", book.abbrev)
            .putString("selected_book_testament", book.testament)
            .putInt("selected_chapter", chapter)
            .apply()
        loadCurrentChapter()
    }

    fun openBibleAtVerse(bookName: String, bookAbbrev: String, chapter: Int, verse: Int, version: String = "NVT") {
        val cleanAbbrev = bookAbbrev.lowercase().trim()
        val books = _bibleBooks.value
        val matchingBook = books.find { 
            it.abbrev.equals(cleanAbbrev, ignoreCase = true) || it.name.equals(bookName, ignoreCase = true) 
        } ?: com.example.data.BibliaBookItem(
            id = 0,
            name = bookName,
            abbrev = cleanAbbrev,
            testament = if (listOf("gn","ex","lv","nm","dt","js","jz","rt","1sm","2sm","1rs","2rs","1cr","2cr","ed","ne","et","job","sl","pv","ec","ct","is","jr","lm","ez","dn","os","jl","am","ob","jn","mq","na","hc","sf","ag","zc","ml").contains(cleanAbbrev)) "VT" else "NT"
        )
        selectedBibleVersion.value = version
        selectedBibleBook.value = matchingBook
        selectedBibleChapter.value = chapter
        targetScrollVerse.value = verse
        biblePrefs.edit()
            .putInt("selected_book_id", matchingBook.id)
            .putString("selected_book_name", matchingBook.name)
            .putString("selected_book_abbrev", matchingBook.abbrev)
            .putString("selected_book_testament", matchingBook.testament)
            .putInt("selected_chapter", chapter)
            .putString("selected_version", version)
            .apply()
        loadCurrentChapter()
    }

    fun clearTargetScrollVerse() {
        targetScrollVerse.value = null
    }

    fun selectBibleVersion(versionCode: String) {
        selectedBibleVersion.value = versionCode
        biblePrefs.edit().putString("selected_version", versionCode).apply()
        loadCurrentChapter()
        loadDailyVerse(forceRefresh = true)
    }

    fun nextChapter() {
        val currentCap = selectedBibleChapter.value
        selectBookAndChapter(selectedBibleBook.value, currentCap + 1)
    }

    fun previousChapter() {
        val currentCap = selectedBibleChapter.value
        if (currentCap > 1) {
            selectBookAndChapter(selectedBibleBook.value, currentCap - 1)
        }
    }

    private fun loadHighlightsForCurrentChapter() {
        val allHighlights = biblePrefs.all.filterKeys { it.startsWith("hl_") }
        val map = mutableMapOf<String, String>()
        allHighlights.forEach { (k, v) ->
            if (v is String) {
                map[k.removePrefix("hl_")] = v
            }
        }
        _verseHighlights.value = map
    }

    fun setVerseHighlight(bookAbbrev: String, chapter: Int, verseNumber: Int, colorHex: String) {
        val key = "${bookAbbrev}_${chapter}_${verseNumber}"
        biblePrefs.edit().putString("hl_$key", colorHex).apply()
        _verseHighlights.value = _verseHighlights.value + (key to colorHex)
    }

    fun removeVerseHighlight(bookAbbrev: String, chapter: Int, verseNumber: Int) {
        val key = "${bookAbbrev}_${chapter}_${verseNumber}"
        biblePrefs.edit().remove("hl_$key").apply()
        _verseHighlights.value = _verseHighlights.value - key
    }

    fun loadDailyVerse(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = applicationContext.getSharedPreferences("tessera_bible_prefs", Context.MODE_PRIVATE)
                val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                val savedDay = prefs.getInt("saved_day", -1)
                val currentVersion = selectedBibleVersion.value
                val savedVersion = prefs.getString("verse_version", "NVT")
                
                if (!forceRefresh && savedDay == today && savedVersion == currentVersion) {
                    val text = prefs.getString("verse_text", null)
                    val bookName = prefs.getString("verse_book_name", null)
                    val chapter = prefs.getInt("verse_chapter", -1)
                    val verse = prefs.getInt("verse_verse", -1)
                    val bookAbbrev = prefs.getString("verse_book_abbrev", "sl") ?: "sl"
                    if (text != null && bookName != null) {
                        _dailyVerse.value = BibleVerseResponse(
                            book = com.example.data.ABibliaBook(name = bookName, version = currentVersion),
                            chapter = chapter,
                            verse = verse,
                            text = text,
                            bookAbbrev = bookAbbrev,
                            versionCode = currentVersion
                        )
                        return@launch
                    }
                }
                
                val response = repository.getRandomBibleVerse(currentVersion)
                _dailyVerse.value = response
                
                prefs.edit()
                    .putInt("saved_day", today)
                    .putString("verse_version", currentVersion)
                    .putString("verse_text", response.text)
                    .putString("verse_book_name", response.book?.name ?: "")
                    .putString("verse_book_abbrev", response.bookAbbrev ?: "sl")
                    .putInt("verse_chapter", response.chapter ?: -1)
                    .putInt("verse_verse", response.verse ?: -1)
                    .apply()
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Error loading bible verse", e)
            }
        }
    }

    data class InsightCard(
        val id: String,
        val title: String,
        val description: String,
        val iconName: String,
        val category: String
    )

    data class DynamicHeroMetric(
        val name: String,
        val label: String,
        val value: Float,
        val target: Float,
        val iconName: String,
        val colorHex: String
    )

    private val _aiInsights = MutableStateFlow<List<InsightCard>>(emptyList())
    val aiInsights: StateFlow<List<InsightCard>> = _aiInsights.asStateFlow()

    private val _heroMetric = MutableStateFlow<DynamicHeroMetric?>(null)
    val heroMetric: StateFlow<DynamicHeroMetric?> = _heroMetric.asStateFlow()



    data class SharedWishState(
        val title: String = "",
        val buyUrl: String = "",
        val imageUrl: String = "",
        val targetValue: String = ""
    )

    private val _sharedWishState = MutableStateFlow<SharedWishState?>(null)
    val sharedWishState: StateFlow<SharedWishState?> = _sharedWishState.asStateFlow()

    fun consumeSharedWishState() {
        _sharedWishState.value = null
    }

    fun handleSharedText(sharedText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val urlRegex = "(https?://[^\\s]+)".toRegex()
            val url = urlRegex.find(sharedText)?.value ?: return@launch
            
            var extractedTitle = sharedText.substringBefore(url).trim()
            if (extractedTitle.length > 50) extractedTitle = extractedTitle.take(50) + "..."
            
            var extractedImg = ""
            var extractedPrice = ""
            
            try {
                val doc = org.jsoup.Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").get()
                
                val ogTitle = doc.select("meta[property=og:title]").attr("content")
                if (ogTitle.isNotBlank()) extractedTitle = ogTitle
                
                val ogImage = doc.select("meta[property=og:image]").attr("content")
                if (ogImage.isNotBlank()) extractedImg = ogImage
                
                val priceSelectors = listOf(".a-price-whole", "#priceblock_ourprice", ".a-offscreen")
                for (selector in priceSelectors) {
                    val priceEl = doc.select(selector).firstOrNull()
                    if (priceEl != null) {
                        val pText = priceEl.text().replace(Regex("[^0-9,]"), "")
                        if (pText.isNotBlank()) {
                            extractedPrice = pText.replace(",", ".")
                            break
                        }
                    }
                }
            } catch (e: Exception) { }
            
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _sharedWishState.value = SharedWishState(
                    title = extractedTitle,
                    buyUrl = url,
                    imageUrl = extractedImg,
                    targetValue = extractedPrice
                )
                triggerWishesAction(WishesAction.ADD_WISH)
            }
        }
    }

    // Weather structures
    data class WeatherInfo(
        val temp: Double,
        val description: String,
        val city: String,
        val weatherCode: Int,
        val isDay: Boolean = true,
        val sunriseTime: String? = null,
        val sunsetTime: String? = null,
        val dayProgress: Float = 0.5f
    )

    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    val weatherState: StateFlow<WeatherInfo?> = _weatherState.asStateFlow()

    private val _dailyBriefingText = MutableStateFlow<String?>(null)
    val dailyBriefingText: StateFlow<String?> = _dailyBriefingText.asStateFlow()

    private val sharedPrefs = applicationContext.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)

    enum class FinanceAction {
        ADD_EXPENSE,
        ADD_INCOME
    }

    enum class HealthAction {
        ADD_STEPS,
        ADD_SLEEP
    }
    
    enum class WishesAction {
        ADD_WISH,
        SEARCH_WISHES
    }

    private val _financeActionTrigger = MutableSharedFlow<FinanceAction>(extraBufferCapacity = 1)
    val financeActionTrigger = _financeActionTrigger.asSharedFlow()

    private val _healthActionTrigger = MutableSharedFlow<HealthAction>(extraBufferCapacity = 1)
    val healthActionTrigger = _healthActionTrigger.asSharedFlow()
    
    private val _wishesActionTrigger = MutableSharedFlow<WishesAction>(extraBufferCapacity = 1)
    val wishesActionTrigger = _wishesActionTrigger.asSharedFlow()

    fun triggerFinanceAction(action: FinanceAction) {
        _financeActionTrigger.tryEmit(action)
    }

    fun triggerHealthAction(action: HealthAction) {
        _healthActionTrigger.tryEmit(action)
    }
    
    fun triggerWishesAction(action: WishesAction) {
        _wishesActionTrigger.tryEmit(action)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        // Removed spotify token check
    }



    private val _appTheme = MutableStateFlow(
        sharedPrefs.getString("app_theme", "dark") ?: "dark"
    )
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _glassmorphismLevel = MutableStateFlow(
        sharedPrefs.getString("glassmorphism_level", "Frosted") ?: "Frosted"
    )
    val glassmorphismLevel: StateFlow<String> = _glassmorphismLevel.asStateFlow()

    fun updateAppTheme(theme: String) {
        sharedPrefs.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    fun updateGlassmorphismLevel(level: String) {
        sharedPrefs.edit().putString("glassmorphism_level", level).apply()
        _glassmorphismLevel.value = level
    }

    private val _vrResetDate = MutableStateFlow(sharedPrefs.getInt("vr_reset_date", 1))
    val vrResetDate: StateFlow<Int> = _vrResetDate.asStateFlow()

    private val _vrBalance = MutableStateFlow(sharedPrefs.getFloat("vr_balance", 0f))
    val vrBalance: StateFlow<Float> = _vrBalance.asStateFlow()

    private val _vrLastPromptMonth = MutableStateFlow(sharedPrefs.getInt("vr_last_prompt_month", -1))
    private val _showVrPrompt = MutableStateFlow(false)
    val showVrPrompt: StateFlow<Boolean> = _showVrPrompt.asStateFlow()

    init {
        checkVrPrompt()
        fetchWeather()
    }

    fun setVrResetDate(date: Int) {
        sharedPrefs.edit().putInt("vr_reset_date", date).apply()
        _vrResetDate.value = date
        checkVrPrompt()
    }

    fun updateVrBalance(balance: Float) {
        sharedPrefs.edit().putFloat("vr_balance", balance).apply()
        _vrBalance.value = balance
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        sharedPrefs.edit().putInt("vr_last_prompt_month", currentMonth).apply()
        _vrLastPromptMonth.value = currentMonth
        _showVrPrompt.value = false
    }

    fun dismissVrPrompt() {
        _showVrPrompt.value = false
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        sharedPrefs.edit().putInt("vr_last_prompt_month", currentMonth).apply()
        _vrLastPromptMonth.value = currentMonth
    }

    fun checkVrPrompt() {
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        
        if (currentDay == _vrResetDate.value && _vrLastPromptMonth.value != currentMonth) {
            _showVrPrompt.value = true
        }
    }



    // Football Integration (API-Football)
    private val apiFootballService = NetworkModule.provideApiFootballService()
    private val fixtureRepository = NetworkModule.provideApiFootballRepository(apiFootballService)

    private val _featuredMatch = MutableStateFlow<com.example.data.DetailedFixture?>(null)
    val featuredMatch: StateFlow<com.example.data.DetailedFixture?> = _featuredMatch.asStateFlow()

    private val _matchStandings = MutableStateFlow<com.example.data.apifootball.StandingsData?>(null)
    val matchStandings: StateFlow<com.example.data.apifootball.StandingsData?> = _matchStandings.asStateFlow()

    private val _availableFootballTeams = MutableStateFlow<List<String>>(listOf("Flamengo (Principal)", "Palmeiras", "São Paulo", "Corinthians", "Fluminense", "Vasco", "Botafogo", "Real Madrid", "Barcelona"))
    val availableFootballTeams: StateFlow<List<String>> = _availableFootballTeams.asStateFlow()

    private val _isLoadingFootball = MutableStateFlow(false)
    val isLoadingFootball: StateFlow<Boolean> = _isLoadingFootball.asStateFlow()

    private val _configuredFootballTeams = MutableStateFlow<List<String>>(emptyList())
    val configuredFootballTeams: StateFlow<List<String>> = _configuredFootballTeams.asStateFlow()

    fun loadConfiguredFootballTeams() {
        val teams = sharedPrefs.getStringSet("football_teams", setOf("Flamengo (Principal)")) ?: setOf("Flamengo (Principal)")
        _configuredFootballTeams.value = teams.toList()
    }

    fun addFootballTeam(team: String) {
        val updated = _configuredFootballTeams.value.toMutableSet().apply { add(team) }
        _configuredFootballTeams.value = updated.toList()
        sharedPrefs.edit().putStringSet("football_teams", updated).apply()
        fetchFootballScores()
    }

    fun removeFootballTeam(team: String) {
        val updated = _configuredFootballTeams.value.toMutableSet().apply { remove(team) }
        _configuredFootballTeams.value = updated.toList()
        sharedPrefs.edit().putStringSet("football_teams", updated).apply()
        fetchFootballScores()
    }

    fun fetchWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get location by IP
                var lat = -23.5505
                var lon = -46.6333
                var city = "São Paulo"
                
                try {
                    val ipUrl = java.net.URL("https://ipapi.co/json/")
                    val ipConnection = ipUrl.openConnection() as java.net.HttpURLConnection
                    ipConnection.connectTimeout = 3000
                    ipConnection.readTimeout = 3000
                    ipConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    val ipResponse = ipConnection.inputStream.bufferedReader().use { it.readText() }
                    val ipJson = org.json.JSONObject(ipResponse)
                    lat = ipJson.optDouble("latitude", -23.5505)
                    lon = ipJson.optDouble("longitude", -46.6333)
                    city = ipJson.optString("city", "São Paulo")
                } catch (e1: Exception) {
                    Log.e("TesseraViewModel", "Erro ao buscar IP (ipapi.co)", e1)
                    try {
                        val ipUrl2 = java.net.URL("https://ip-api.com/json/")
                        val ipConnection2 = ipUrl2.openConnection() as java.net.HttpURLConnection
                        ipConnection2.connectTimeout = 3000
                        ipConnection2.readTimeout = 3000
                        ipConnection2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        val ipResponse2 = ipConnection2.inputStream.bufferedReader().use { it.readText() }
                        val ipJson2 = org.json.JSONObject(ipResponse2)
                        lat = ipJson2.optDouble("lat", -23.5505)
                        lon = ipJson2.optDouble("lon", -46.6333)
                        city = ipJson2.optString("city", "São Paulo")
                    } catch (e2: Exception) {
                        Log.e("TesseraViewModel", "Erro ao buscar IP (ip-api.com)", e2)
                    }
                }
                
                // 2. Get current weather and sunrise/sunset from Open-Meteo
                val weatherUrl = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=sunrise,sunset&timezone=auto")
                val weatherConnection = weatherUrl.openConnection() as java.net.HttpURLConnection
                weatherConnection.connectTimeout = 3000
                weatherConnection.readTimeout = 3000
                weatherConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                val weatherResponse = weatherConnection.inputStream.bufferedReader().use { it.readText() }
                val weatherJson = org.json.JSONObject(weatherResponse)
                
                val currentWeather = weatherJson.getJSONObject("current_weather")
                val temp = currentWeather.getDouble("temperature")
                val code = currentWeather.getInt("weathercode")
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isDay = if (currentWeather.has("is_day")) {
                    currentWeather.getInt("is_day") == 1
                } else {
                    currentHour in 6..18
                }

                var sunriseTime: String? = null
                var sunsetTime: String? = null
                val dailyObj = weatherJson.optJSONObject("daily")
                if (dailyObj != null) {
                    val sunriseArr = dailyObj.optJSONArray("sunrise")
                    val sunsetArr = dailyObj.optJSONArray("sunset")
                    if (sunriseArr != null && sunriseArr.length() > 0) {
                        val fullSunrise = sunriseArr.getString(0)
                        sunriseTime = fullSunrise.substringAfter("T")
                    }
                    if (sunsetArr != null && sunsetArr.length() > 0) {
                        val fullSunset = sunsetArr.getString(0)
                        sunsetTime = fullSunset.substringAfter("T")
                    }
                }

                val nowCal = Calendar.getInstance()
                val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                val sunriseMinutes = if (sunriseTime != null && sunriseTime.contains(":")) {
                    val parts = sunriseTime.split(":")
                    parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 360
                } else 360
                val sunsetMinutes = if (sunsetTime != null && sunsetTime.contains(":")) {
                    val parts = sunsetTime.split(":")
                    parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 1080
                } else 1080

                val dayProgress = if (sunsetMinutes > sunriseMinutes) {
                    ((nowMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat()).coerceIn(0f, 1f)
                } else {
                    0.5f
                }
                
                val description = getWeatherDescription(code, isDay)
                
                _weatherState.value = WeatherInfo(
                    temp = temp,
                    description = description,
                    city = city,
                    weatherCode = code,
                    isDay = isDay,
                    sunriseTime = sunriseTime,
                    sunsetTime = sunsetTime,
                    dayProgress = dayProgress
                )
                _userLocation.value = lat to lon
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao buscar clima", e)
                // Default fallback based on local time
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isDayFallback = hour in 6..18
                val (fallbackTemp, fallbackDesc) = when (hour) {
                    in 6..11 -> 21.0 to "Manhã Fresca"
                    in 12..17 -> 26.0 to "Sol e Nuvens"
                    in 18..19 -> 22.0 to "Pôr do Sol"
                    else -> 18.0 to "Noite Limpa"
                }
                val fallbackProgress = ((hour - 6).toFloat() / 12f).coerceIn(0f, 1f)
                _weatherState.value = WeatherInfo(
                    temp = fallbackTemp,
                    description = fallbackDesc,
                    city = "Local",
                    weatherCode = 0,
                    isDay = isDayFallback,
                    sunriseTime = "06:00",
                    sunsetTime = "18:00",
                    dayProgress = fallbackProgress
                )
                _userLocation.value = -23.5505 to -46.6333
            }
        }
    }

    private fun getWeatherDescription(code: Int, isDay: Boolean = true): String {
        return when (code) {
            0 -> if (isDay) "Céu Limpo" else "Noite Limpa"
            1, 2, 3 -> if (isDay) "Parcialmente Nublado" else "Noite Parcialmente Nublada"
            45, 48 -> "Nevoeiro"
            51, 53, 55 -> "Garoa"
            61, 63, 65 -> "Chuva"
            71, 73, 75 -> "Neve"
            80, 81, 82 -> "Pancadas de Chuva"
            95, 96, 99 -> "Tempestade"
            else -> if (isDay) "Céu Limpo" else "Noite Limpa"
        }
    }

    var selectedGoalsTab: Int = 0

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMarketItems: StateFlow<List<MarketItem>> = repository.pendingMarketItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingMarketItems: StateFlow<List<MarketItem>> = repository.shoppingMarketItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val boughtMarketItems: StateFlow<List<MarketItem>> = repository.boughtMarketItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchaseGoals: StateFlow<List<PurchaseGoal>> = repository.allPurchaseGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutines: StateFlow<List<Routine>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPetEvents: StateFlow<List<PetEvent>> = repository.allPetEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBankAccounts: StateFlow<List<BankAccount>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCards: StateFlow<List<CreditCard>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBenefitCards: StateFlow<List<com.example.data.BenefitCard>> = repository.allBenefitCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<com.example.data.Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthProfile: StateFlow<HealthProfile?> = repository.healthProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMealRecords: StateFlow<List<com.example.data.MealRecord>> = repository.allMealRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWaterRecords: StateFlow<List<com.example.data.WaterRecord>> = repository.allWaterRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun refreshAIInsightsAndMetric() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val habits = repository.allHabits.first()
                val marketItems = repository.pendingMarketItems.first()
                val transactions = repository.allTransactions.first()
                val petEvents = repository.allPetEvents.first()
                val medications = repository.allMedications.first()
                val steps = repository.allStepsRecords.first()
                val sleepRecords = repository.allSleepRecords.first()
                
                val todayStart = getStartOfToday()
                val todayEnd = getEndOfToday()
                val todaySteps = steps.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }
                
                val completedHabits = habits.count { it.isCompleted }
                val totalHabits = habits.size
                
                val pendingMeds = medications.count { !it.isTaken }
                val totalMeds = medications.size

                val pendingMarketCount = marketItems.size
                
                val realIncome = transactions.filter { it.isIncome }.sumOf { it.value }
                val realExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
                val realBalance = realIncome - realExpense

                val latestSleepRecord = sleepRecords.lastOrNull()
                val latestSleep = latestSleepRecord?.durationHours ?: 7.5
                val hours = latestSleep.toInt()
                val minutes = ((latestSleep - hours) * 60).toInt()
                val sleepText = if (minutes > 0) "${hours}h${minutes.toString().padStart(2, '0')}" else "${hours}h"
                val sleepEfficiency = if (latestSleep == 0.0) 92
                else {
                    val base = 88 + (latestSleep % 1.0 * 8).toInt()
                    base.coerceIn(60, 98)
                }

                // Fallback metric
                val fallbackMetric = when {
                    totalHabits > 0 && completedHabits < totalHabits -> {
                        DynamicHeroMetric(
                            name = "RITUAIS DIÁRIOS",
                            label = "HÁBITOS DE HOJE",
                            value = completedHabits.toFloat(),
                            target = totalHabits.toFloat(),
                            iconName = "CheckCircle",
                            colorHex = "#71D7CD"
                        )
                    }
                    todaySteps < 10000 -> {
                        DynamicHeroMetric(
                            name = "PASSOS COMPLETADOS",
                            label = "PASSOS DIÁRIOS",
                            value = todaySteps.toFloat(),
                            target = 10000f,
                            iconName = "DirectionsWalk",
                            colorHex = "#34C759"
                        )
                    }
                    pendingMarketCount > 0 -> {
                        DynamicHeroMetric(
                            name = "COMPRAS PENDENTES",
                            label = "ITENS NO MERCADO",
                            value = (marketItems.size - pendingMarketCount).toFloat(),
                            target = marketItems.size.toFloat(),
                            iconName = "LocalMall",
                            colorHex = "#FF3B30"
                        )
                    }
                    else -> {
                        DynamicHeroMetric(
                            name = "BALANÇO FINANCEIRO",
                            label = "SALDO DE HOJE",
                            value = realBalance.toFloat().coerceAtLeast(0f),
                            target = 5000f,
                            iconName = "AttachMoney",
                            colorHex = "#007AFF"
                        )
                    }
                }

                // Fallback insights
                val fallbackInsights = mutableListOf<InsightCard>()
                if (pendingMeds > 0) {
                    fallbackInsights.add(
                        InsightCard(
                            id = "med_insight",
                            title = "Dica de Saúde",
                            description = "Você tem $pendingMeds medicamentos pendentes hoje. Lembre-se de tomá-los para manter seu tratamento em dia.",
                            iconName = "Medication",
                            category = "health"
                        )
                    )
                }
                if (pendingMarketCount > 0) {
                    fallbackInsights.add(
                        InsightCard(
                            id = "market_insight",
                            title = "Alerta de Compras",
                            description = "Há $pendingMarketCount itens pendentes na sua lista de mercado. Aproveite para completá-la.",
                            iconName = "LocalMall",
                            category = "market"
                        )
                    )
                }
                if (realExpense > 500.0) {
                    fallbackInsights.add(
                        InsightCard(
                            id = "finance_insight",
                            title = "Alerta Financeiro",
                            description = "Seus gastos acumulados hoje são de R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", realExpense)}. Mantenha o foco nas metas financeiras.",
                            iconName = "AttachMoney",
                            category = "finance"
                        )
                    )
                }
                if (petEvents.any { !it.isCompleted }) {
                    val pendingPets = petEvents.count { !it.isCompleted }
                    fallbackInsights.add(
                        InsightCard(
                            id = "pets_insight",
                            title = "Lembrete Pet",
                            description = "Há $pendingPets compromissos com pets pendentes para hoje. Marie e Churchill contam com você!",
                            iconName = "Pets",
                            category = "pets"
                        )
                    )
                }
                if (fallbackInsights.size < 3) {
                    fallbackInsights.add(
                        InsightCard(
                            id = "general_insight",
                            title = "Foco Mental",
                            description = "Hoje é um excelente dia para iniciar uma sessão de Pomodoro de 25 minutos. Reduza distrações e concentre-se.",
                            iconName = "Timer",
                            category = "goals"
                        )
                    )
                }

                // Fallback for daily briefing text
                val habitsText = if (totalHabits > 0) {
                    val pending = totalHabits - completedHabits
                    if (pending > 0) "Você concluiu $completedHabits de $totalHabits rituais diários. Mantenha o foco!" else "Incrível, todos os rituais de hoje foram concluídos!"
                } else {
                    "Seus rituais estão em dia."
                }
                val petText = if (petEvents.any { !it.isCompleted }) " Lembre-se de cuidar da Marie e do Churchill hoje." else ""
                val stepsText = if (todaySteps > 0) " Você já caminhou $todaySteps passos hoje." else " Que tal dar uma caminhada hoje?"
                val fallbackBriefing = "Você dormiu $sleepText com $sleepEfficiency% de eficiência. $habitsText$petText$stepsText"
                _dailyBriefingText.value = fallbackBriefing

                _heroMetric.value = fallbackMetric
                _aiInsights.value = fallbackInsights.take(3)
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao atualizar insights AI", e)
            }
        }
    }

    val allMedications: StateFlow<List<Medication>> = combine(
        repository.allMedications,
        repository.getMedicationLogsForRange(getStartOfToday(), getEndOfToday())
    ) { meds, logs ->
        meds.map { med ->
            val hasLog = logs.any { it.medicationId == med.id }
            med.copy(isTaken = hasLog)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStepsRecords: StateFlow<List<StepsRecord>> = repository.allStepsRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeightRecords: StateFlow<List<WeightRecord>> = repository.allWeightRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSleepRecords: StateFlow<List<SleepRecord>> = repository.allSleepRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SPTrans Integration ---
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _userBusLines = MutableStateFlow<List<Triple<Int, String, String>>>(emptyList()) // (lineCode, lineNumber, destination)
    val userBusLines: StateFlow<List<Triple<Int, String, String>>> = _userBusLines.asStateFlow()

    private val _busSearchResults = MutableStateFlow<List<com.example.data.SPTransLinha>>(emptyList())
    val busSearchResults: StateFlow<List<com.example.data.SPTransLinha>> = _busSearchResults.asStateFlow()

    private val _isSearchingBus = MutableStateFlow(false)
    val isSearchingBus: StateFlow<Boolean> = _isSearchingBus.asStateFlow()

    private val _savedBusLines = MutableStateFlow<List<com.example.data.SavedBusLine>>(emptyList())
    val savedBusLines: StateFlow<List<com.example.data.SavedBusLine>> = _savedBusLines.asStateFlow()

    private val _isLoadingBus = MutableStateFlow(false)
    val isLoadingBus: StateFlow<Boolean> = _isLoadingBus.asStateFlow()

    private val _busError = MutableStateFlow<String?>(null)
    val busError: StateFlow<String?> = _busError.asStateFlow()

    fun addBenefitCard(name: String, balance: Double, numberLastFour: String, colorHex: String, holderName: String, id: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBenefitCard(
                com.example.data.BenefitCard(
                    id = id,
                    name = name,
                    balance = balance,
                    numberLastFour = numberLastFour,
                    colorHex = colorHex,
                    holderName = holderName
                )
            )
        }
    }

    fun deleteBenefitCard(card: com.example.data.BenefitCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBenefitCard(card)
        }
    }

    fun getSavedBusLinesFromPrefs(): Set<String> {
        return sharedPrefs.getStringSet("saved_bus_lines", setOf(
            "1273;34041-10;Term. Lapa",
            "34214;8000-10;Praça Ramos"
        )) ?: setOf(
            "1273;34041-10;Term. Lapa",
            "34214;8000-10;Praça Ramos"
        )
    }

    fun loadUserBusLines() {
        val set = getSavedBusLinesFromPrefs()
        _userBusLines.value = set.mapNotNull {
            val parts = it.split(";")
            if (parts.size >= 3) {
                Triple(parts[0].toIntOrNull() ?: 0, parts[1], parts[2])
            } else null
        }
    }

    fun saveBusLine(lineCode: Int, lineNumber: String, destination: String) {
        val currentSet = getSavedBusLinesFromPrefs().toMutableSet()
        currentSet.removeAll { it.startsWith("$lineCode;") }
        currentSet.add("$lineCode;$lineNumber;$destination")
        sharedPrefs.edit().putStringSet("saved_bus_lines", currentSet).apply()
        loadUserBusLines()
        fetchBusPredictions()
    }

    fun removeBusLine(lineCode: Int) {
        val currentSet = getSavedBusLinesFromPrefs().toMutableSet()
        currentSet.removeAll { it.startsWith("$lineCode;") }
        sharedPrefs.edit().putStringSet("saved_bus_lines", currentSet).apply()
        loadUserBusLines()
        fetchBusPredictions()
    }

    fun searchBusLines(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearchingBus.value = true
            try {
                if (query.isBlank()) {
                    _busSearchResults.value = emptyList()
                    return@launch
                }
                
                if (com.example.data.SPTransApi.API_TOKEN.isBlank()) {
                    _busError.value = "Chave da API SPTrans não configurada."
                    return@launch
                }
                
                val authResponse = com.example.data.SPTransApi.service.autenticar(com.example.data.SPTransApi.API_TOKEN)
                if (authResponse.isSuccessful && authResponse.body()?.string() == "true") {
                    val lines = com.example.data.SPTransApi.service.buscarLinha(query)
                    _busSearchResults.value = lines
                } else {
                    _busError.value = "Falha na autenticação da SPTrans."
                }
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao buscar linhas de ônibus", e)
                _busError.value = "Erro ao buscar linhas: ${e.message}"
            } finally {
                _isSearchingBus.value = false
            }
        }
    }

    fun fetchRealLocation(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                
                val provider = when {
                    isGpsEnabled -> android.location.LocationManager.GPS_PROVIDER
                    isNetworkEnabled -> android.location.LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                
                if (provider != null) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        _userLocation.value = location.latitude to location.longitude
                        fetchBusPredictions()
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        locationManager.getCurrentLocation(
                            provider,
                            null,
                            context.mainExecutor
                        ) { newLocation ->
                            if (newLocation != null) {
                                _userLocation.value = newLocation.latitude to newLocation.longitude
                                fetchBusPredictions()
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("TesseraViewModel", "Erro de permissão ao buscar localização", e)
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao buscar localização real", e)
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun fetchBusPredictions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingBus.value = true
            _busError.value = null
            try {
                if (_userBusLines.value.isEmpty()) {
                    loadUserBusLines()
                }
                val savedLinesList = _userBusLines.value
                if (savedLinesList.isEmpty()) {
                    _savedBusLines.value = emptyList()
                    return@launch
                }
                
                if (com.example.data.SPTransApi.API_TOKEN.isBlank()) {
                    _busError.value = "Chave da API SPTrans não configurada."
                    return@launch
                }

                val authResponse = com.example.data.SPTransApi.service.autenticar(com.example.data.SPTransApi.API_TOKEN)
                if (!authResponse.isSuccessful || authResponse.body()?.string() != "true") {
                    _busError.value = "Falha na autenticação da SPTrans."
                    return@launch
                }

                val userLoc = _userLocation.value
                val results = mutableListOf<com.example.data.SavedBusLine>()
                
                for ((codigo, numero, destino) in savedLinesList) {
                    try {
                        var closestCp = -1
                        var stopName = "Nenhum ponto encontrado"
                        var horario = "Sem prev."

                        // Try to get static stops first
                        var paradas = emptyList<com.example.data.SPTransParada>()
                        try {
                            paradas = com.example.data.SPTransApi.service.getParadasPorLinha(codigo)
                        } catch (e: Exception) {
                            Log.e("TesseraViewModel", "Erro ao buscar paradas estáticas", e)
                        }
                        
                        var closestStaticStop: com.example.data.SPTransParada? = null
                        if (paradas.isNotEmpty()) {
                            closestStaticStop = if (userLoc != null) {
                                paradas.minByOrNull { calculateDistance(userLoc.first, userLoc.second, it.py, it.px) }
                            } else {
                                paradas.firstOrNull()
                            }
                        }

                        // Now try to get predictions for the whole line
                        try {
                            val previsaoResponse = com.example.data.SPTransApi.service.getPrevisaoLinha(codigo)
                            val dynamicStops = previsaoResponse.ps ?: emptyList()
                            
                            // If we didn't find static stops, try to use dynamic stops to find the closest one
                            if (closestStaticStop == null && dynamicStops.isNotEmpty()) {
                                val closestDynamic = if (userLoc != null) {
                                    dynamicStops.minByOrNull { calculateDistance(userLoc.first, userLoc.second, it.py ?: 0.0, it.px ?: 0.0) }
                                } else {
                                    dynamicStops.firstOrNull()
                                }
                                if (closestDynamic != null) {
                                    closestCp = closestDynamic.cp
                                    stopName = closestDynamic.np
                                    val proximoVeiculo = closestDynamic.vs?.minByOrNull { it.t }
                                    horario = proximoVeiculo?.t ?: "Sem prev."
                                }
                            } else if (closestStaticStop != null) {
                                // We have a static stop, let's find its prediction in the dynamic response
                                closestCp = closestStaticStop.cp
                                stopName = closestStaticStop.np
                                val dynamicMatch = dynamicStops.find { it.cp == closestCp }
                                val proximoVeiculo = dynamicMatch?.vs?.minByOrNull { it.t }
                                horario = proximoVeiculo?.t ?: "Sem prev."
                            }
                        } catch (e: Exception) {
                            Log.e("TesseraViewModel", "Erro ao buscar previsões dinâmicas", e)
                            // If Previsao/Linha fails, fallback to Previsao/Parada if we have a static stop
                            if (closestStaticStop != null) {
                                closestCp = closestStaticStop.cp
                                stopName = closestStaticStop.np
                                try {
                                    val fallbackPrev = com.example.data.SPTransApi.service.getPrevisaoParada(closestCp, codigo)
                                    val linhaPrevisao = fallbackPrev.p?.l?.find { it.cl == codigo }
                                    val proximoVeiculo = linhaPrevisao?.vs?.minByOrNull { it.t }
                                    horario = proximoVeiculo?.t ?: "Sem prev."
                                } catch (e2: Exception) {
                                    Log.e("TesseraViewModel", "Erro no fallback de previsão", e2)
                                }
                            }
                        }
                        
                        results.add(
                            com.example.data.SavedBusLine(
                                id = codigo.toString(),
                                lineCode = codigo,
                                lineNumber = numero,
                                destination = destino,
                                estimatedArrivalText = horario,
                                stopName = stopName
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("TesseraViewModel", "Erro ao processar linha salva", e)
                        // Still add it so it doesn't disappear from the UI on network error
                        results.add(
                            com.example.data.SavedBusLine(
                                id = codigo.toString(),
                                lineCode = codigo,
                                lineNumber = numero,
                                destination = destino,
                                estimatedArrivalText = "Erro",
                                stopName = "Erro ao carregar dados"
                            )
                        )
                    }
                }
                _savedBusLines.value = results
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro geral em fetchBusPredictions", e)
                _busError.value = "Erro ao buscar previsões."
            } finally {
                _isLoadingBus.value = false
            }
        }
    }
    // ---------------------------

    fun addTransaction(
        title: String,
        subtitle: String,
        value: Double,
        isIncome: Boolean,
        category: String,
        accountOrCardName: String = "",
        isRealized: Boolean = true,
        isRecurrent: Boolean = false,
        recurrenceInterval: String = "Mensal",
        dueDate: Long = 0L,
        customTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            val txTime = customTimestamp ?: if (dueDate > 0L) dueDate else System.currentTimeMillis()
            val mainTx = Transaction(
                title = title,
                subtitle = subtitle,
                value = value,
                isIncome = isIncome,
                timestamp = txTime,
                category = category,
                accountOrCardName = accountOrCardName,
                isRealized = isRealized,
                isRecurrent = isRecurrent,
                recurrenceInterval = recurrenceInterval,
                dueDate = if (dueDate > 0L) dueDate else txTime
            )
            repository.insertTransaction(mainTx)
            if (accountOrCardName.isNotEmpty()) {
                adjustBalances(accountOrCardName, value, isIncome, isRealized)
            }
            
            // Se for recorrente e já foi paga, agenda automaticamente o próximo vencimento
            if (isRecurrent && isRealized) {
                val nextDueDate = calculateNextDueDate(if (dueDate > 0L) dueDate else txTime, recurrenceInterval)
                val nextTx = mainTx.copy(
                    id = 0, // Novo ID autogerado
                    isRealized = false,
                    dueDate = nextDueDate,
                    timestamp = nextDueDate
                )
                repository.insertTransaction(nextTx)
            }
            supabaseFinanceSync.triggerSync()
        }
    }

    fun importBatchTransactions(
        accountName: String,
        transactions: List<com.example.utils.ParsedStatementTransaction>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val selected = transactions.filter { it.isSelected }
            if (selected.isEmpty()) return@launch

            for (item in selected) {
                val mainTx = Transaction(
                    title = item.title,
                    subtitle = "Extrato PDF • $accountName",
                    value = item.amount,
                    isIncome = item.isIncome,
                    timestamp = item.timestamp,
                    category = item.category,
                    accountOrCardName = accountName,
                    isRealized = true,
                    isRecurrent = false,
                    recurrenceInterval = "Mensal",
                    dueDate = item.timestamp
                )
                repository.insertTransaction(mainTx)
                if (accountName.isNotEmpty()) {
                    adjustBalances(accountName, item.amount, item.isIncome, true)
                }
            }
            supabaseFinanceSync.triggerSync()
        }
    }

    fun addInstallmentTransaction(
        title: String,
        value: Double,
        isIncome: Boolean,
        category: String,
        accountOrCardName: String = "",
        isRealized: Boolean = true,
        installmentsCount: Int,
        dueDate: Long = 0L
    ) {
        viewModelScope.launch {
            val baseTime = if (dueDate > 0L) dueDate else System.currentTimeMillis()
            val valuePerInstallment = value / installmentsCount
            
            if (accountOrCardName.isNotEmpty()) {
                val cards = repository.allCreditCards.first()
                val isCreditCard = cards.any { it.name == accountOrCardName }
                if (isCreditCard) {
                    // Para cartão de crédito, desconta integralmente o valor total da compra do saldo do cartão
                    adjustBalances(accountOrCardName, value, isIncome, true)
                }
            }

            for (i in 1..installmentsCount) {
                // Desloca o vencimento em i-1 meses
                val cal = Calendar.getInstance()
                cal.timeInMillis = baseTime
                cal.add(Calendar.MONTH, i - 1)
                val installmentDueDate = cal.timeInMillis
                
                // A primeira parcela segue a escolha de "realizada/paga" do usuário. As seguintes são sempre pendentes.
                val installmentRealized = if (i == 1) isRealized else false
                
                val installmentTx = Transaction(
                    title = "$title ($i/$installmentsCount)",
                    subtitle = "Parcela $i de $installmentsCount",
                    value = valuePerInstallment,
                    isIncome = isIncome,
                    timestamp = installmentDueDate,
                    category = category,
                    accountOrCardName = accountOrCardName,
                    isRealized = installmentRealized,
                    isRecurrent = false,
                    dueDate = installmentDueDate
                )
                repository.insertTransaction(installmentTx)
                
                if (accountOrCardName.isNotEmpty()) {
                    val cards = repository.allCreditCards.first()
                    val isCreditCard = cards.any { it.name == accountOrCardName }
                    if (!isCreditCard) {
                        // Para contas bancárias/benefício, ajusta o saldo conforme a parcela realizada
                        adjustBalances(accountOrCardName, valuePerInstallment, isIncome, installmentRealized)
                    }
                }
            }
            supabaseFinanceSync.triggerSync()
        }
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        viewModelScope.launch {
            if (oldTransaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(oldTransaction.accountOrCardName, oldTransaction.value, oldTransaction.isIncome, oldTransaction.isRealized)
            }
            repository.insertTransaction(newTransaction)
            if (newTransaction.accountOrCardName.isNotEmpty()) {
                adjustBalances(newTransaction.accountOrCardName, newTransaction.value, newTransaction.isIncome, newTransaction.isRealized)
            }
            supabaseFinanceSync.triggerSync()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.accountOrCardName.isNotEmpty()) {
                try {
                    rollbackBalances(transaction.accountOrCardName, transaction.value, transaction.isIncome, transaction.isRealized)
                } catch (e: Exception) {
                    Log.e("TesseraViewModel", "Erro ao fazer rollback do balanço", e)
                }
            }
            repository.deleteTransaction(transaction)
            supabaseFinanceSync.triggerSync()
        }
    }

    fun realizeRecurrentTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(transaction.accountOrCardName, transaction.value, transaction.isIncome, transaction.isRealized)
            }
            // 1. Mark current transaction as realized
            val realizedTx = transaction.copy(
                isRealized = true,
                timestamp = System.currentTimeMillis() // Set realization timestamp to now
            )
            repository.insertTransaction(realizedTx)
            if (realizedTx.accountOrCardName.isNotEmpty()) {
                adjustBalances(realizedTx.accountOrCardName, realizedTx.value, realizedTx.isIncome, realizedTx.isRealized)
            }

            // 2. Schedule the next recurrence
            val nextDueDate = calculateNextDueDate(transaction.dueDate, transaction.recurrenceInterval)
            val nextTx = transaction.copy(
                id = 0, // Generate new ID
                isRealized = false,
                dueDate = nextDueDate,
                timestamp = nextDueDate // Set timestamp to the due date so it sorts or shows up in the future
            )
            repository.insertTransaction(nextTx)
            supabaseFinanceSync.triggerSync()
        }
    }

    fun calculateNextDueDate(currentDueDate: Long, interval: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = if (currentDueDate > 0) currentDueDate else System.currentTimeMillis()
        when (interval) {
            "Semanal" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Mensal" -> cal.add(Calendar.MONTH, 1)
            "Anual" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    private suspend fun adjustBalances(name: String, value: Double, isIncome: Boolean, isRealized: Boolean) {
        val accounts = repository.allBankAccounts.first()
        val matchingAccount = accounts.find { it.name == name }
        if (matchingAccount != null) {
            if (isRealized) {
                val newBalance = if (isIncome) matchingAccount.balance + value else matchingAccount.balance - value
                repository.insertBankAccount(matchingAccount.copy(balance = newBalance))
            }
            return
        }
        val cards = repository.allCreditCards.first()
        val matchingCard = cards.find { it.name == name }
        if (matchingCard != null) {
            if (isRealized) {
                val newUsedLimit = if (isIncome) (matchingCard.usedLimit - value).coerceAtLeast(0.0) else matchingCard.usedLimit + value
                repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
            }
            return
        }
        val benefitCards = repository.allBenefitCards.first()
        val matchingBenefit = benefitCards.find { it.name == name }
        if (matchingBenefit != null) {
            if (isRealized) {
                val newBalance = if (isIncome) matchingBenefit.balance + value else matchingBenefit.balance - value
                repository.insertBenefitCard(matchingBenefit.copy(balance = newBalance))
            }
        }
    }

    private suspend fun rollbackBalances(name: String, value: Double, isIncome: Boolean, isRealized: Boolean) {
        val accounts = repository.allBankAccounts.first()
        val matchingAccount = accounts.find { it.name == name }
        if (matchingAccount != null) {
            if (isRealized) {
                val newBalance = if (isIncome) matchingAccount.balance - value else matchingAccount.balance + value
                repository.insertBankAccount(matchingAccount.copy(balance = newBalance))
            }
            return
        }
        val cards = repository.allCreditCards.first()
        val matchingCard = cards.find { it.name == name }
        if (matchingCard != null) {
            // Em cartões de crédito, compras parceladas debitam o limite integral na criação.
            // Ao deletar uma parcela (mesmo futura/pendente), estorna o limite correspondente.
            val newUsedLimit = if (isIncome) matchingCard.usedLimit + value else (matchingCard.usedLimit - value).coerceAtLeast(0.0)
            repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
            return
        }
        val benefitCards = repository.allBenefitCards.first()
        val matchingBenefit = benefitCards.find { it.name == name }
        if (matchingBenefit != null) {
            if (isRealized) {
                val newBalance = if (isIncome) matchingBenefit.balance - value else matchingBenefit.balance + value
                repository.insertBenefitCard(matchingBenefit.copy(balance = newBalance))
            }
        }
    }

    fun addBankAccount(name: String, balance: Double, type: String, colorHex: String, id: Int = 0) {
        viewModelScope.launch {
            repository.insertBankAccount(BankAccount(id = id, name = name, balance = balance, type = type, colorHex = colorHex))
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
        }
    }

    fun addCreditCard(name: String, limit: Double, usedLimit: Double, numberLastFour: String, colorHex: String, holderName: String, id: Int = 0) {
        viewModelScope.launch {
            repository.insertCreditCard(CreditCard(id = id, name = name, limit = limit, usedLimit = usedLimit, numberLastFour = numberLastFour, colorHex = colorHex, holderName = holderName))
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCreditCard(card)
        }
    }

    fun payInvoice(cardId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.payInvoice(cardId)
            supabaseFinanceSync.triggerSync()
        }
    }

    fun installmentInvoice(
        card: CreditCard,
        downPayment: Double,
        debitAccountName: String?,
        installmentsCount: Int,
        installmentAmount: Double,
        totalWithInterest: Double,
        firstDueDate: Long = 0L
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val baseTime = if (firstDueDate > 0L) firstDueDate else {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, 1)
                cal.timeInMillis
            }

            // 1. Processa a entrada à vista, se houver
            if (downPayment > 0.0) {
                val downPaymentTx = Transaction(
                    title = "Entrada Parcelamento • ${card.name}",
                    subtitle = "Entrada à vista da fatura",
                    value = downPayment,
                    isIncome = false,
                    timestamp = System.currentTimeMillis(),
                    category = "Cartão de Crédito",
                    accountOrCardName = debitAccountName ?: "",
                    isRealized = true,
                    isRecurrent = false,
                    dueDate = System.currentTimeMillis()
                )
                repository.insertTransaction(downPaymentTx)

                if (!debitAccountName.isNullOrEmpty()) {
                    adjustBalances(debitAccountName, downPayment, isIncome = false, isRealized = true)
                }
            }

            // 2. Quita/Zera a fatura atual do cartão
            repository.payInvoice(card.id)

            // 3. Gera as parcelas futuras
            val interestTotal = (installmentAmount * installmentsCount) - (card.usedLimit - downPayment)
            for (i in 1..installmentsCount) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = baseTime
                cal.add(Calendar.MONTH, i - 1)
                val installmentDueDate = cal.timeInMillis

                val installmentTx = Transaction(
                    title = "Parcelamento Fatura • ${card.name} ($i/$installmentsCount)",
                    subtitle = "Parcela $i de $installmentsCount" + if (interestTotal > 0.01) " • Juros totais: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", interestTotal)}" else "",
                    value = installmentAmount,
                    isIncome = false,
                    timestamp = installmentDueDate,
                    category = "Cartão de Crédito",
                    accountOrCardName = card.name,
                    isRealized = false,
                    isRecurrent = false,
                    dueDate = installmentDueDate
                )
                repository.insertTransaction(installmentTx)
            }

            // 4. Sincroniza em tempo real
            supabaseFinanceSync.triggerSync()
        }
    }

    fun clearAllFinances() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllFinances()
        }
    }

    fun addMarketItem(name: String, category: String = "Geral", price: Double = 0.0, quantity: Double = 1.0, unit: String = "un", isChecked: Boolean = false, inMarket: Boolean = false) {
        viewModelScope.launch {
            val resolvedChecked = if (inMarket) true else isChecked
            repository.insertMarketItem(
                MarketItem(name = name, isChecked = resolvedChecked, isBought = false, orderIndex = 0, category = category, price = price, quantity = quantity, unit = unit, inMarket = inMarket)
            )
        }
    }

    fun deleteMarketItem(item: MarketItem) {
        viewModelScope.launch {
            repository.deleteMarketItem(item)
        }
    }

    fun toggleMarketItemChecked(item: MarketItem) {
        viewModelScope.launch {
            repository.updateMarketItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun updateMarketItemDetails(item: MarketItem, price: Double, quantity: Double, unit: String) {
        viewModelScope.launch {
            val autoCheck = if (price > 0.0 && !item.isChecked) true else item.isChecked
            repository.updateMarketItem(item.copy(price = price, quantity = quantity, unit = unit, isChecked = autoCheck))
        }
    }
    fun clearCompletedMarketItems() {
        viewModelScope.launch {
            val boughtItems = repository.boughtMarketItems.first()
            boughtItems.forEach { item ->
                repository.deleteMarketItem(item)
            }
        }
    }

    fun markMarketItemBought(item: MarketItem) {
        viewModelScope.launch {
            repository.updateMarketItem(item.copy(isBought = true, isChecked = false))
        }
    }

    fun checkoutCart() {
        viewModelScope.launch {
            val shoppingItems = repository.shoppingMarketItems.first()
            val inCart = shoppingItems.filter { it.isChecked }
            val inCartNames = inCart.map { it.name.lowercase().trim() }.distinct()
            
            // Delete checked items from cart
            inCart.forEach { item ->
                repository.deleteMarketItem(item)
            }
            
            // Delete same items from planning
            if (inCartNames.isNotEmpty()) {
                repository.deletePlanningItemsByNames(inCartNames)
            }
        }
    }

    fun checkoutCartWithDebit(accountName: String, amount: Double) {
        viewModelScope.launch {
            // 1. Debit the amount from the selected account/benefit card
            adjustBalances(accountName, amount, isIncome = false, isRealized = true)
            
            val shoppingItems = repository.shoppingMarketItems.first()
            val inCart = shoppingItems.filter { it.isChecked }
            val inCartNames = inCart.map { it.name.lowercase().trim() }.distinct()

            // 2. Registrar no extrato financeiro a transação de despesa do mercado
            if (amount > 0.0) {
                val itemCountText = if (inCart.isNotEmpty()) "${inCart.size} itens" else "Compras"
                repository.insertTransaction(
                    Transaction(
                        title = "Supermercado",
                        subtitle = itemCountText,
                        value = amount,
                        isIncome = false,
                        timestamp = System.currentTimeMillis(),
                        category = "Mercado",
                        accountOrCardName = accountName,
                        isRealized = true
                    )
                )
            }
            
            // 3. Delete checked items from cart
            inCart.forEach { item ->
                repository.deleteMarketItem(item)
            }
            
            // 4. Delete same items from planning
            if (inCartNames.isNotEmpty()) {
                repository.deletePlanningItemsByNames(inCartNames)
            }
        }
    }

    fun togglePetEventCompleted(event: PetEvent) {
        viewModelScope.launch {
            repository.insertPetEvent(event.copy(isCompleted = !event.isCompleted))
        }
    }

    fun updatePetEvent(event: PetEvent) {
        viewModelScope.launch {
            repository.insertPetEvent(event)
        }
    }
    
    fun initializeDataIfNeeded() {
        viewModelScope.launch {
            val profile = repository.healthProfile.first()
            if (profile == null) {
                repository.insertHealthProfile(HealthProfile(id = 1, heightCm = 0.0, targetWeightKg = 0.0, isHealthConnectEnabled = false))
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            val count = repository.getPetEventsCount()
            if (count == 0) {
                repository.insertPetEvents(listOf(
                    PetEvent(petName = "Marie", title = "Passeio Matinal", time = "07:30", isCompleted = false, isNext = false),
                    PetEvent(petName = "Marie", title = "Alimentação", time = "12:00", isCompleted = false, isNext = true),
                    PetEvent(petName = "Churchill", title = "Medicamento", time = "18:00", isCompleted = false, isNext = false)
                ))
            }

            val accounts = repository.allBankAccounts.first()
            if (accounts.isEmpty()) {
                repository.insertBankAccount(BankAccount(name = "Nubank", balance = 12450.80, type = "Corrente", colorHex = "#8A05BE"))
                repository.insertBankAccount(BankAccount(name = "Itaú Uniclass", balance = 45200.00, type = "Corrente", colorHex = "#FF8C00"))
                repository.insertBankAccount(BankAccount(name = "XP Investimentos", balance = 150000.00, type = "Investimento", colorHex = "#E6C619"))
            }

            val cards = repository.allCreditCards.first()
            if (cards.isEmpty()) {
                repository.insertCreditCard(CreditCard(name = "Inter Black", limit = 50000.00, usedLimit = 12400.00, numberLastFour = "8899", colorHex = "#FF7A00", holderName = "KENNETH S. O."))
                repository.insertCreditCard(CreditCard(name = "Nubank Ultravioleta", limit = 30000.00, usedLimit = 4560.20, numberLastFour = "1234", colorHex = "#8A05BE", holderName = "KENNETH S. O."))
                repository.insertCreditCard(CreditCard(name = "C6 Carbon", limit = 80000.00, usedLimit = 25100.50, numberLastFour = "7766", colorHex = "#1C1C1C", holderName = "KENNETH S. O."))
            }

            val txs = repository.allTransactions.first()
            if (txs.isEmpty()) {
                // Realized transactions
                repository.insertTransaction(Transaction(title = "Salário Mensal", subtitle = "Depósito Recebido", value = 18500.00, isIncome = true, timestamp = System.currentTimeMillis() - 86400000 * 5, category = "Salário", accountOrCardName = "XP Investimentos", isRealized = true))
                repository.insertTransaction(Transaction(title = "Mercado Municipal", subtitle = "Compras da semana", value = 450.20, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 3, category = "Alimentação", accountOrCardName = "Nubank", isRealized = true))
                repository.insertTransaction(Transaction(title = "Posto Ipiranga", subtitle = "Combustível", value = 220.00, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 2, category = "Transporte", accountOrCardName = "Nubank Ultravioleta", isRealized = true))
                repository.insertTransaction(Transaction(title = "Assinatura Netflix", subtitle = "Mensalidade", value = 55.90, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 1, category = "Lazer", accountOrCardName = "Inter Black", isRealized = true))
                repository.insertTransaction(Transaction(title = "Jantar Premium", subtitle = "Restaurante", value = 380.00, isIncome = false, timestamp = System.currentTimeMillis() - 3600000 * 4, category = "Alimentação", accountOrCardName = "C6 Carbon", isRealized = true))

                // Recurrent and pending transactions (overdue)
                repository.insertTransaction(Transaction(
                    title = "Assinatura de Música",
                    subtitle = "Mensalidade Premium",
                    value = 34.90,
                    isIncome = false,
                    timestamp = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                    category = "Lazer",
                    accountOrCardName = "Nubank Ultravioleta",
                    isRealized = false,
                    isRecurrent = true,
                    recurrenceInterval = "Mensal",
                    dueDate = System.currentTimeMillis() - 86400000 * 3
                ))

                // Recurrent and pending transactions (future)
                repository.insertTransaction(Transaction(
                    title = "Aluguel Apartamento",
                    subtitle = "Custo Fixo Mensal",
                    value = 2800.00,
                    isIncome = false,
                    timestamp = System.currentTimeMillis() + 86400000 * 10, // 10 days in future
                    category = "Outros",
                    accountOrCardName = "Nubank",
                    isRealized = false,
                    isRecurrent = true,
                    recurrenceInterval = "Mensal",
                    dueDate = System.currentTimeMillis() + 86400000 * 10
                ))

                // One-off pending transaction (future)
                repository.insertTransaction(Transaction(
                    title = "Manutenção Notebook",
                    subtitle = "Conserto de cooler",
                    value = 450.00,
                    isIncome = false,
                    timestamp = System.currentTimeMillis() + 86400000 * 5, // 5 days in future
                    category = "Outros",
                    accountOrCardName = "Inter Black",
                    isRealized = false,
                    isRecurrent = false,
                    dueDate = System.currentTimeMillis() + 86400000 * 5
                ))

                // One-off pending transaction (overdue)
                repository.insertTransaction(Transaction(
                    title = "Ajuste Costureira",
                    subtitle = "Ajuste de ternos",
                    value = 120.00,
                    isIncome = false,
                    timestamp = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                    category = "Outros",
                    accountOrCardName = "Itaú Uniclass",
                    isRealized = false,
                    isRecurrent = false,
                    dueDate = System.currentTimeMillis() - 86400000 * 2
                ))
            }

            val habits = repository.allHabits.first()
            if (habits.isEmpty()) {
                repository.insertHabit(Habit(name = "Hidratação (3L)", isCompleted = false, streak = 12, iconName = "WaterDrop", colorHex = "#71D7CD", orderIndex = 0))
                repository.insertHabit(Habit(name = "Leitura Profunda", isCompleted = false, streak = 5, iconName = "MenuBook", colorHex = "#F9A826", orderIndex = 1))
                repository.insertHabit(Habit(name = "Mindfulness", isCompleted = true, streak = 21, iconName = "SelfImprovement", colorHex = "#D7B4F3", orderIndex = 2))
            }

            val goals = repository.allPurchaseGoals.first()
            if (goals.isEmpty()) {
                repository.insertPurchaseGoal(PurchaseGoal(title = "MacBook Pro M3", targetValue = 24000.00, currentValue = 15000.00, imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = 0L, colorHex = "#71D7CD", priorityOrder = 1, priorityClassification = "Urgente", buyUrl = "https://www.apple.com/br/macbook-pro/", category = "Eletrônicos"))
                repository.insertPurchaseGoal(PurchaseGoal(title = "Viagem Kyoto", targetValue = 35000.00, currentValue = 8000.00, imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = 0L, colorHex = "#F9A826", priorityOrder = 2, priorityClassification = "Moderado", buyUrl = "https://www.japan.travel/pt/br/", category = "Viagem"))
            }

            val routines = repository.allRoutines.first()
            if (routines.isEmpty()) {
                val r1Id = repository.insertRoutine(Routine(name = "Rotina Matinal", iconName = "Spa"))
                repository.insertRoutineStep(RoutineStep(routineId = r1Id.toInt(), title = "Beber Água", durationSeconds = 60, iconName = "WaterDrop", orderIndex = 0))
                repository.insertRoutineStep(RoutineStep(routineId = r1Id.toInt(), title = "Meditação", durationSeconds = 300, iconName = "SelfImprovement", orderIndex = 1))
                repository.insertRoutineStep(RoutineStep(routineId = r1Id.toInt(), title = "Alongamento", durationSeconds = 180, iconName = "Spa", orderIndex = 2))

                val r2Id = repository.insertRoutine(Routine(name = "Rotina Noturna", iconName = "Spa"))
                repository.insertRoutineStep(RoutineStep(routineId = r2Id.toInt(), title = "Reflexão Diária", durationSeconds = 300, iconName = "MenuBook", orderIndex = 0))
                repository.insertRoutineStep(RoutineStep(routineId = r2Id.toInt(), title = "Higiene do Sono", durationSeconds = 120, iconName = "Spa", orderIndex = 1))
                repository.insertRoutineStep(RoutineStep(routineId = r2Id.toInt(), title = "Respiração Profunda", durationSeconds = 180, iconName = "SelfImprovement", orderIndex = 2))
            }

            val profile = repository.healthProfile.first()
            if (profile == null) {
                repository.insertHealthProfile(HealthProfile(id = 1, heightCm = 175.0, targetWeightKg = 70.0, isHealthConnectEnabled = false))
            } else {
                repository.insertHealthProfile(profile.copy(heightCm = 175.0, targetWeightKg = 70.0))
            }
        }
    }

    fun addPetEvent(petName: String, title: String, time: String) {
        viewModelScope.launch {
            repository.insertPetEvent(
                PetEvent(petName = petName, title = title, time = time, isCompleted = false, isNext = false)
            )
        }
    }

    fun deletePetEvent(event: PetEvent) {
        viewModelScope.launch {
            repository.deletePetEvent(event)
        }
    }

    fun toggleHabitCompleted(habit: Habit) {
        viewModelScope.launch {
            val newCompleted = !habit.isCompleted
            val newStreak = if (newCompleted) habit.streak + 1 else maxOf(0, habit.streak - 1)
            repository.updateHabit(habit.copy(isCompleted = newCompleted, streak = newStreak))
            refreshAIInsightsAndMetric()
        }
    }

    fun addHabit(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val count = repository.allHabits.first().size
            repository.insertHabit(Habit(name = name, isCompleted = false, streak = 0, iconName = iconName, colorHex = colorHex, orderIndex = count))
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun addPurchaseGoal(
        title: String,
        target: Double,
        current: Double,
        imageUrl: String,
        deadline: Long = 0L,
        colorHex: String,
        priorityOrder: Int = 1,
        priorityClassification: String = "Moderado",
        buyUrl: String = "",
        category: String = "Geral"
    ) {
        viewModelScope.launch {
            repository.insertPurchaseGoal(
                PurchaseGoal(
                    title = title,
                    targetValue = target,
                    currentValue = current,
                    imageUrl = imageUrl,
                    deadlineTimestamp = deadline,
                    colorHex = colorHex,
                    priorityOrder = priorityOrder,
                    priorityClassification = priorityClassification,
                    buyUrl = buyUrl,
                    category = category
                )
            )
        }
    }

    fun updatePurchaseGoalProgress(goal: PurchaseGoal, addedValue: Double) {
        viewModelScope.launch {
            repository.updatePurchaseGoal(goal.copy(currentValue = goal.currentValue + addedValue))
        }
    }

    fun addFundsToPurchaseGoal(goal: PurchaseGoal, amount: Double, accountName: String) {
        viewModelScope.launch {
            repository.updatePurchaseGoal(goal.copy(currentValue = goal.currentValue + amount))
            addTransaction(
                title = "Aporte: ${goal.title}",
                subtitle = "Reserva para Desejo",
                value = amount,
                isIncome = false,
                category = "Desejos",
                accountOrCardName = accountName,
                isRealized = true
            )
        }
    }

    fun buyPurchaseGoal(goal: PurchaseGoal, accountName: String) {
        viewModelScope.launch {
            val remainingValue = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
            repository.updatePurchaseGoal(goal.copy(currentValue = goal.targetValue, isBought = true))
            if (remainingValue > 0.0) {
                addTransaction(
                    title = "Compra: ${goal.title}",
                    subtitle = "Conclusão de Desejo",
                    value = remainingValue,
                    isIncome = false,
                    category = "Desejos",
                    accountOrCardName = accountName,
                    isRealized = true
                )
            }
        }
    }

    fun updatePurchaseGoal(goal: PurchaseGoal) {
        viewModelScope.launch {
            repository.updatePurchaseGoal(goal)
        }
    }

    fun deletePurchaseGoal(goal: PurchaseGoal) {
        viewModelScope.launch {
            repository.deletePurchaseGoal(goal)
        }
    }

    fun insertDebt(debt: com.example.data.Debt) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDebt(debt)
        }
    }

    fun deleteDebt(debt: com.example.data.Debt) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDebt(debt)
        }
    }

    fun payDebtInstallment(debt: com.example.data.Debt, accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextInstallment = debt.installmentsPaid + 1
            val isFullyPaid = nextInstallment >= debt.installmentsTotal
            
            if (debt.id < 0) {
                // É uma dívida sintética gerada por uma transação vencida (!isRealized e vencimento passado)
                val originalTxId = -debt.id
                val allTxs = repository.allTransactions.first()
                val originalTx = allTxs.find { it.id == originalTxId }
                if (originalTx != null) {
                    // Marca a transação original como realizada
                    repository.insertTransaction(originalTx.copy(isRealized = true))
                    // Ajusta o saldo da conta que realizou o pagamento
                    adjustBalances(accountName, originalTx.value, originalTx.isIncome, isRealized = true)
                }
            } else {
                val updatedDebt = debt.copy(
                    installmentsPaid = nextInstallment,
                    isPaid = isFullyPaid
                )
                repository.insertDebt(updatedDebt)
                
                // Register a transaction
                val installmentValue = debt.value / debt.installmentsTotal
                addTransaction(
                    title = "Pgto: ${debt.title} ($nextInstallment/${debt.installmentsTotal})",
                    subtitle = "Parcela de Dívida",
                    value = installmentValue,
                    isIncome = false,
                    category = "Dívidas",
                    accountOrCardName = accountName,
                    isRealized = true
                )
            }
        }
    }

    // Health Methods
    fun updateHealthProfile(heightCm: Double, targetWeightKg: Double, isHealthConnectEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.healthProfile.first() ?: HealthProfile()
            repository.insertHealthProfile(current.copy(heightCm = heightCm, targetWeightKg = targetWeightKg, isHealthConnectEnabled = isHealthConnectEnabled))
        }
    }

    fun updateNutritionGoals(
        dailyCalories: Double,
        dailyProtein: Double,
        dailyCarbs: Double,
        dailyFat: Double,
        dailyFiber: Double,
        dailyWaterGoalMl: Int? = null
    ) {
        viewModelScope.launch {
            val current = repository.healthProfile.first() ?: HealthProfile()
            repository.insertHealthProfile(
                current.copy(
                    dailyCalorieGoal = dailyCalories,
                    dailyProteinGoal = dailyProtein,
                    dailyCarbGoal = dailyCarbs,
                    dailyFatGoal = dailyFat,
                    dailyFiberGoal = dailyFiber,
                    dailyWaterGoalMl = dailyWaterGoalMl ?: current.dailyWaterGoalMl
                )
            )
        }
    }

    fun updateWaterGoal(dailyWaterGoalMl: Int) {
        viewModelScope.launch {
            val current = repository.healthProfile.first() ?: HealthProfile()
            repository.insertHealthProfile(current.copy(dailyWaterGoalMl = dailyWaterGoalMl))
        }
    }

    fun addWaterRecord(amountMl: Int, date: String) {
        viewModelScope.launch {
            repository.insertWaterRecord(
                com.example.data.WaterRecord(
                    amountMl = amountMl,
                    timestamp = System.currentTimeMillis(),
                    date = date
                )
            )
        }
    }

    fun deleteWaterRecord(record: com.example.data.WaterRecord) {
        viewModelScope.launch {
            repository.deleteWaterRecord(record)
        }
    }

    fun deleteWaterRecordById(id: Int) {
        viewModelScope.launch {
            repository.deleteWaterRecordById(id)
        }
    }

    fun addMealRecord(
        mealType: String,
        name: String,
        calories: Double,
        protein: Double = 0.0,
        carbs: Double = 0.0,
        fat: Double = 0.0,
        fiber: Double = 0.0,
        portion: String = "1 porção",
        imageUrl: String? = null,
        barcode: String? = null,
        date: String
    ) {
        viewModelScope.launch {
            repository.insertMealRecord(
                com.example.data.MealRecord(
                    mealType = mealType,
                    name = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    portion = portion,
                    imageUrl = imageUrl,
                    barcode = barcode,
                    timestamp = System.currentTimeMillis(),
                    date = date
                )
            )
        }
    }

    fun updateMealRecord(meal: com.example.data.MealRecord) {
        viewModelScope.launch {
            repository.updateMealRecord(meal)
        }
    }

    fun deleteMealRecord(meal: com.example.data.MealRecord) {
        viewModelScope.launch {
            repository.deleteMealRecord(meal)
        }
    }

    fun deleteMealRecordById(id: Int) {
        viewModelScope.launch {
            repository.deleteMealRecordById(id)
        }
    }

    fun addMedication(name: String, time: String, dosage: String, colorHex: String, recurrence: String = "DAILY") {
        viewModelScope.launch {
            repository.insertMedication(Medication(name = name, time = time, isTaken = false, dosage = dosage, colorHex = colorHex, recurrence = recurrence))
        }
    }

    fun toggleMedicationTaken(medication: Medication) {
        viewModelScope.launch {
            val start = getStartOfToday()
            val end = getEndOfToday()
            val logs = repository.getLogsForMedication(medication.id, start, end).first()
            if (logs.isEmpty()) {
                repository.insertMedicationLog(
                    MedicationLog(medicationId = medication.id, takenTimestamp = System.currentTimeMillis())
                )
                repository.updateMedication(medication.copy(isTaken = true))
            } else {
                logs.forEach { repository.deleteMedicationLog(it) }
                repository.updateMedication(medication.copy(isTaken = false))
            }
            refreshAIInsightsAndMetric()
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    fun addManualWeightRecord(weightKg: Double) {
        viewModelScope.launch {
            repository.insertWeightRecord(WeightRecord(weightKg = weightKg, timestamp = System.currentTimeMillis(), source = "manual"))
        }
    }

    fun addManualSleepRecord(startTime: Long, endTime: Long, durationHours: Double) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = endTime }
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L
            repository.clearManualSleepForDay(startOfDay, endOfDay)
            repository.insertSleepRecord(SleepRecord(startTime = startTime, endTime = endTime, durationHours = durationHours, source = "manual"))
        }
    }

    fun addManualStepsRecord(count: Long, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = endTime }
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L
            repository.clearManualStepsForDay(startOfDay, endOfDay)
            repository.insertStepsRecord(StepsRecord(count = count, startTime = startTime, endTime = endTime, source = "manual"))
        }
    }

    fun syncHealthConnectData(weights: List<WeightRecord>, sleeps: List<SleepRecord>, steps: List<StepsRecord>) {
        viewModelScope.launch {
            val existingWeights = repository.allWeightRecords.first()
            if (existingWeights.isEmpty()) {
                // Adiciona como dados iniciais no histórico
                weights.forEach { repository.insertWeightRecord(it) }
            } else {
                val latestDbWeight = existingWeights.lastOrNull()
                val latestHcWeight = weights.maxByOrNull { it.timestamp }
                if (latestHcWeight != null && latestDbWeight != null) {
                    // Se o peso mais recente mudou em relação ao último registrado, adiciona novo registro
                    if (Math.abs(latestHcWeight.weightKg - latestDbWeight.weightKg) >= 0.05) {
                        repository.insertWeightRecord(latestHcWeight)
                    }
                } else if (latestHcWeight != null) {
                    repository.insertWeightRecord(latestHcWeight)
                }
            }

            repository.clearHealthConnectSleepRecords()
            sleeps.forEach { repository.insertSleepRecord(it) }

            repository.clearHealthConnectStepsRecords()
            steps.forEach { repository.insertStepsRecord(it) }
        }
    }

    fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>> {
        return repository.getStepsForRoutine(routineId)
    }

    fun addRoutine(name: String, iconName: String, id: Int = 0) {
        viewModelScope.launch {
            repository.insertRoutine(Routine(id = id, name = name, iconName = iconName))
        }
    }

    fun addRoutineStep(routineId: Int, title: String, durationSeconds: Int, iconName: String, orderIndex: Int) {
        viewModelScope.launch {
            repository.insertRoutineStep(RoutineStep(routineId = routineId, title = title, durationSeconds = durationSeconds, iconName = iconName, orderIndex = orderIndex))
        }
    }

    fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        viewModelScope.launch {
            repository.saveRoutineWithSteps(routine, steps)
        }
    }

    fun completeRoutine(routine: Routine) {
        viewModelScope.launch {
            val habitsList = repository.allHabits.first()
            val matchingHabit = habitsList.find { it.name.equals(routine.name, ignoreCase = true) }
            if (matchingHabit != null) {
                if (!matchingHabit.isCompleted) {
                    val newStreak = matchingHabit.streak + 1
                    repository.updateHabit(matchingHabit.copy(isCompleted = true, streak = newStreak))
                }
            } else {
                val count = habitsList.size
                repository.insertHabit(Habit(
                    name = routine.name,
                    isCompleted = true,
                    streak = 1,
                    iconName = routine.iconName,
                    colorHex = "#71D7CD",
                    orderIndex = count
                ))
            }
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    // Metro & Trem (ARTESP) Integration
    private val metroService = com.example.data.MetroService.create()

    private val _metroConcessionarias = MutableStateFlow<List<com.example.data.MetroEmpresaConfig>>(emptyList())
    val metroConcessionarias: StateFlow<List<com.example.data.MetroEmpresaConfig>> = _metroConcessionarias.asStateFlow()

    private val _isLoadingMetroConfig = MutableStateFlow(false)
    val isLoadingMetroConfig: StateFlow<Boolean> = _isLoadingMetroConfig.asStateFlow()

    private val _metroStatus = MutableStateFlow<List<com.example.data.MetroEmpresaStatus>>(emptyList())
    val metroStatus: StateFlow<List<com.example.data.MetroEmpresaStatus>> = _metroStatus.asStateFlow()

    private val _isLoadingMetroStatus = MutableStateFlow(false)
    val isLoadingMetroStatus: StateFlow<Boolean> = _isLoadingMetroStatus.asStateFlow()

    private val _metroError = MutableStateFlow<String?>(null)
    val metroError: StateFlow<String?> = _metroError.asStateFlow()

    fun fetchMetroConcessionarias() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMetroConfig.value = true
            _metroError.value = null
            try {
                _metroConcessionarias.value = com.example.data.MetroCptmApi.defaultEmpresas
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao carregar concessionárias", e)
            } finally {
                _isLoadingMetroConfig.value = false
            }
        }
    }

    fun fetchMetroStatus(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMetroStatus.value = true
            _metroError.value = null
            try {
                val prefs = applicationContext.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
                val apiKey = prefs.getString("artesp_api_key", null)?.trim()?.ifBlank { null }
                val liveStatus = com.example.data.MetroCptmApi.getLiveMetroAndTrainStatus(
                    apiKey = apiKey,
                    forceRefresh = forceRefresh
                )
                _metroStatus.value = liveStatus
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao buscar status do metrô e trens", e)
                _metroError.value = e.message ?: "Erro ao atualizar status das linhas"
            } finally {
                _isLoadingMetroStatus.value = false
            }
        }
    }



    fun fetchFootballScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingFootball.value = true
            try {
                val teamsToWatch = _configuredFootballTeams.value
                val primaryTeam = teamsToWatch.firstOrNull()?.ifBlank { "Flamengo" } ?: "Flamengo"
                val cleanTeamName = primaryTeam.replace("(principal)", "").replace("(equipe principal)", "").trim().lowercase()

                var teamId = com.example.data.TheSportsDbApi.knownBrazilianTeams.entries.find { cleanTeamName.contains(it.key) }?.value

                if (teamId == null) {
                    try {
                        val searchResponse = com.example.data.TheSportsDbApi.service.searchTeam(cleanTeamName)
                        teamId = searchResponse.teams?.firstOrNull()?.idTeam
                    } catch (e: Exception) {
                        Log.e("TesseraViewModel", "Erro ao buscar time no TheSportsDB", e)
                    }
                }

                // Fallback para Flamengo se nenhum ID for encontrado
                val targetTeamId = teamId ?: "134287"

                // 1. Tenta buscar próximos jogos
                var selectedEvent: com.example.data.TSDBEvent? = null
                try {
                    val nextResponse = com.example.data.TheSportsDbApi.service.getNextEvents(targetTeamId)
                    val nextEvents = nextResponse.events ?: nextResponse.results ?: emptyList()
                    selectedEvent = nextEvents.firstOrNull()
                } catch (e: Exception) {
                    Log.e("TesseraViewModel", "Erro ao buscar próximos jogos", e)
                }

                // 2. Se não houver próximos jogos agendados, busca últimos jogos finalizados
                if (selectedEvent == null) {
                    try {
                        val lastResponse = com.example.data.TheSportsDbApi.service.getLastEvents(targetTeamId)
                        val lastEvents = lastResponse.events ?: lastResponse.results ?: emptyList()
                        selectedEvent = lastEvents.firstOrNull()
                    } catch (e: Exception) {
                        Log.e("TesseraViewModel", "Erro ao buscar últimos jogos", e)
                    }
                }

                if (selectedEvent != null) {
                    val homeName = selectedEvent.strHomeTeam ?: "Time Casa"
                    val awayName = selectedEvent.strAwayTeam ?: "Time Fora"
                    val homeBadge = selectedEvent.strHomeTeamBadge ?: ""
                    val awayBadge = selectedEvent.strAwayTeamBadge ?: ""
                    val homeScore = selectedEvent.intHomeScore?.toIntOrNull()
                    val awayScore = selectedEvent.intAwayScore?.toIntOrNull()
                    val status = selectedEvent.strStatus ?: "NS"
                    val venue = selectedEvent.strVenue
                    val league = selectedEvent.strLeague ?: "Brasileirão Série A"

                    val formattedDate = com.example.data.formatUtcMatchDateTime(
                        selectedEvent.dateEvent,
                        selectedEvent.strTime
                    )

                    val matchDetail = com.example.data.MatchDetail(
                        homeTeamName = homeName,
                        homeTeamLogo = homeBadge,
                        awayTeamName = awayName,
                        awayTeamLogo = awayBadge,
                        homeGoals = homeScore,
                        awayGoals = awayScore,
                        statusShort = status,
                        dateFormatted = formattedDate.ifBlank { "Em breve" },
                        leagueName = league
                    )

                    val detailedFixture = com.example.data.DetailedFixture(
                        matchDetail = matchDetail,
                        venueName = venue,
                        events = emptyList(),
                        homeLineup = emptyList(),
                        awayLineup = emptyList()
                    )

                    _featuredMatch.value = detailedFixture
                } else {
                    Log.e("TesseraViewModel", "Nenhuma partida encontrada para o time no TheSportsDB")
                }

                // 3. Busca Tabela de Classificação do Brasileirão Série A em Tempo Real (Oficial com Fallback)
                val liveStandings = com.example.data.BrasileiraoRepository.getLiveStandings()
                if (liveStandings != null) {
                    _matchStandings.value = liveStandings
                } else {
                    try {
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val apiFootballResult = fixtureRepository.getStandings(71L, currentYear)
                        apiFootballResult.onSuccess { data ->
                            _matchStandings.value = data
                        }.onFailure {
                            fixtureRepository.getStandings(71L, currentYear - 1).onSuccess { prevData ->
                                _matchStandings.value = prevData
                            }
                        }
                    } catch (e2: Exception) {
                        Log.e("TesseraViewModel", "Erro no fallback API-Football Standings", e2)
                    }
                }
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro na API de Futebol: ${e.message}")
            } finally {
                _isLoadingFootball.value = false
            }
        }
    }

    private fun formatFootballDate(rawDate: String?): String {
        if (rawDate == null) return ""
        return try {
            val instant = java.time.Instant.parse(rawDate)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            try {
                val clean = rawDate.substringBefore("+").substringBefore("Z")
                val localDateTime = java.time.LocalDateTime.parse(clean)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")
                formatter.format(localDateTime)
            } catch (e2: Exception) {
                Log.e("TesseraViewModel", "Erro ao formatar data do futebol", e2)
                rawDate.take(16).replace("T", " ")
            }
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        loadUserBusLines()
        loadConfiguredFootballTeams()
        fetchFootballScores()


        
        viewModelScope.launch(Dispatchers.IO) {
            refreshAIInsightsAndMetric()
        }

        // Observa mudanças na mídia ativa para pre-fetch automático em background
        viewModelScope.launch(Dispatchers.IO) {
            com.example.media.MediaHubManager.activeMediaState.collect { mediaState ->
                if (mediaState != null && mediaState.title.isNotBlank()) {
                    com.example.data.media.MusicContextRepository.prefetchDossier(
                        title = mediaState.title,
                        artist = mediaState.artist,
                        album = mediaState.album,
                        durationMs = mediaState.durationMs,
                        packageName = mediaState.packageName
                    )
                }
            }
        }
    }

    // ==========================================
    // SMART MEDIA HUB & MUSIC DOSSIER
    // ==========================================
    val activeMediaState = com.example.media.MediaHubManager.activeMediaState

    private val _musicDossier = MutableStateFlow<com.example.data.media.MusicContextDossier?>(null)
    val musicDossier: StateFlow<com.example.data.media.MusicContextDossier?> = _musicDossier.asStateFlow()

    private val _isLoadingMusicDossier = MutableStateFlow(false)
    val isLoadingMusicDossier: StateFlow<Boolean> = _isLoadingMusicDossier.asStateFlow()

    private val _musicDossierError = MutableStateFlow<String?>(null)
    val musicDossierError: StateFlow<String?> = _musicDossierError.asStateFlow()

    fun fetchMusicDossier(forceRefresh: Boolean = false) {
        val currentMedia = activeMediaState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMusicDossier.value = true
            _musicDossierError.value = null
            try {
                val dossier = com.example.data.media.MusicContextRepository.getMusicContextDossier(
                    title = currentMedia.title,
                    artist = currentMedia.artist,
                    album = currentMedia.album,
                    durationMs = currentMedia.durationMs,
                    packageName = currentMedia.packageName,
                    forceRefresh = forceRefresh
                )
                _musicDossier.value = dossier
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao carregar dossiê musical", e)
                _musicDossierError.value = "Não foi possível carregar o dossiê da faixa."
            } finally {
                _isLoadingMusicDossier.value = false
            }
        }
    }

    fun playMedia() = com.example.media.MediaHubManager.play()
    fun pauseMedia() = com.example.media.MediaHubManager.pause()
    fun toggleMediaPlayPause() = com.example.media.MediaHubManager.togglePlayPause()
    fun skipMediaNext() = com.example.media.MediaHubManager.skipToNext()
    fun skipMediaPrevious() = com.example.media.MediaHubManager.skipToPrevious()
    fun seekMediaTo(positionMs: Long) = com.example.media.MediaHubManager.seekTo(positionMs)

    fun isMediaListenerPermissionGranted(context: Context): Boolean {
        return com.example.media.MediaHubManager.isNotificationListenerGranted(context)
    }

    fun openMediaListenerSettings(context: Context) {
        com.example.media.MediaHubManager.openNotificationListenerSettings(context)
    }

    // ==========================================
    // WGER GYM WORKOUT & EXERCISE CATALOG
    // ==========================================
    val wgerCategoryFilters = com.example.data.wger.WgerRepository.categoryFilters

    private val _selectedWgerCategory = MutableStateFlow<Int?>(null)
    val selectedWgerCategory: StateFlow<Int?> = _selectedWgerCategory.asStateFlow()

    private val _wgerSearchQuery = MutableStateFlow("")
    val wgerSearchQuery: StateFlow<String> = _wgerSearchQuery.asStateFlow()

    private val _wgerExercises = MutableStateFlow<List<com.example.data.wger.WgerExercise>>(emptyList())
    val wgerExercises: StateFlow<List<com.example.data.wger.WgerExercise>> = _wgerExercises.asStateFlow()

    private val _isLoadingWgerExercises = MutableStateFlow(false)
    val isLoadingWgerExercises: StateFlow<Boolean> = _isLoadingWgerExercises.asStateFlow()

    private val _wgerExerciseError = MutableStateFlow<String?>(null)
    val wgerExerciseError: StateFlow<String?> = _wgerExerciseError.asStateFlow()

    fun fetchWgerExercises(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingWgerExercises.value = true
            _wgerExerciseError.value = null
            try {
                val list = com.example.data.wger.WgerRepository.getExercises(
                    categoryId = _selectedWgerCategory.value,
                    searchQuery = _wgerSearchQuery.value,
                    forceRefresh = forceRefresh
                )
                _wgerExercises.value = list
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao carregar exercícios Wger", e)
                _wgerExerciseError.value = "Falha ao carregar catálogo. Exibindo dados locais."
                _wgerExercises.value = com.example.data.wger.WgerRepository.getFallbackExercises()
            } finally {
                _isLoadingWgerExercises.value = false
            }
        }
    }

    fun setWgerCategory(categoryId: Int?) {
        _selectedWgerCategory.value = categoryId
        fetchWgerExercises(forceRefresh = false)
    }

    fun setWgerSearchQuery(query: String) {
        _wgerSearchQuery.value = query
        fetchWgerExercises(forceRefresh = false)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

}

class TesseraViewModelFactory(
    private val repository: TesseraRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TesseraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TesseraViewModel(repository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
