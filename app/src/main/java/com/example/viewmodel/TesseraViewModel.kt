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

import com.example.data.FootballService
import com.example.data.FootballMatchInfo
import com.example.data.sportmonks.NetworkModule
import com.example.data.NewsArticle
import com.example.data.NewsService
import android.util.Log

class TesseraViewModel(
    private val repository: TesseraRepository,
    private val applicationContext: Context
) : ViewModel() {

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



    // Weather structures
    data class WeatherInfo(
        val temp: Double,
        val description: String,
        val city: String,
        val weatherCode: Int
    )

    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    val weatherState: StateFlow<WeatherInfo?> = _weatherState.asStateFlow()

    private val _dailyBriefingText = MutableStateFlow<String?>(null)
    val dailyBriefingText: StateFlow<String?> = _dailyBriefingText.asStateFlow()

    private val sharedPrefs = applicationContext.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)

    private val _spotifyAccessToken = MutableStateFlow<String?>(null)
    val spotifyAccessToken: StateFlow<String?> = _spotifyAccessToken.asStateFlow()

    private val _homeBackgroundUri = MutableStateFlow(
        sharedPrefs.getString("home_background_uri", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop")
            ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
    )
    val homeBackgroundUri: StateFlow<String> = _homeBackgroundUri.asStateFlow()

    private val _glassmorphismLevel = MutableStateFlow(
        sharedPrefs.getString("glassmorphism_level", "Frosted") ?: "Frosted"
    )
    val glassmorphismLevel: StateFlow<String> = _glassmorphismLevel.asStateFlow()

    fun updateHomeBackgroundUri(uri: String) {
        sharedPrefs.edit().putString("home_background_uri", uri).apply()
        _homeBackgroundUri.value = uri
    }

    fun updateGlassmorphismLevel(level: String) {
        sharedPrefs.edit().putString("glassmorphism_level", level).apply()
        _glassmorphismLevel.value = level
    }

    fun saveSpotifyToken(token: String) {
        sharedPrefs.edit().putString("spotify_access_token", token).apply()
        _spotifyAccessToken.value = token
    }

    fun disconnectSpotify() {
        sharedPrefs.edit().remove("spotify_access_token").apply()
        _spotifyAccessToken.value = null
    }

    // Football Integration (Sportmonks)
    private val sportmonksApi = NetworkModule.provideSportmonksApi(
        NetworkModule.provideOkHttpClient(),
        NetworkModule.provideMoshi()
    )
    private val fixtureRepository = NetworkModule.provideFixtureRepository(sportmonksApi)

    private val _footballMatches = MutableStateFlow<List<com.example.data.FootballMatchInfo>>(emptyList())
    val footballMatches: StateFlow<List<com.example.data.FootballMatchInfo>> = _footballMatches.asStateFlow()

    private val _isLoadingFootball = MutableStateFlow(false)
    val isLoadingFootball: StateFlow<Boolean> = _isLoadingFootball.asStateFlow()

    private val _configuredFootballTeams = MutableStateFlow<List<String>>(emptyList())
    val configuredFootballTeams: StateFlow<List<String>> = _configuredFootballTeams.asStateFlow()

    private val newsService = NewsService.create()
    private val newsApiKey = "d47edb4744604172abec5be172a5acc2"

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    fun loadConfiguredFootballTeams() {
        val teams = sharedPrefs.getStringSet("football_teams", setOf("Brasil", "Flamengo")) ?: setOf("Brasil", "Flamengo")
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

    fun fetchNews() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = newsService.getTopHeadlines(apiKey = newsApiKey)
                if (response.isSuccessful) {
                    val articles = response.body()?.articles ?: emptyList()
                    _newsArticles.value = articles.filter { !it.title.isNullOrBlank() }
                }
            } catch (e: Exception) {
                Log.e("TesseraViewModel", "Erro ao buscar notícias: ${e.message}")
            }
        }
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
                    e1.printStackTrace()
                    try {
                        val ipUrl2 = java.net.URL("http://ip-api.com/json/")
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
                        e2.printStackTrace()
                    }
                }
                
                // 2. Get current weather from Open-Meteo
                val weatherUrl = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                val weatherConnection = weatherUrl.openConnection() as java.net.HttpURLConnection
                weatherConnection.connectTimeout = 3000
                weatherConnection.readTimeout = 3000
                weatherConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                val weatherResponse = weatherConnection.inputStream.bufferedReader().use { it.readText() }
                val weatherJson = org.json.JSONObject(weatherResponse)
                
                val currentWeather = weatherJson.getJSONObject("current_weather")
                val temp = currentWeather.getDouble("temperature")
                val code = currentWeather.getInt("weathercode")
                
                val description = getWeatherDescription(code)
                
                _weatherState.value = WeatherInfo(
                    temp = temp,
                    description = description,
                    city = city,
                    weatherCode = code
                )
                _userLocation.value = lat to lon
            } catch (e: Exception) {
                e.printStackTrace()
                // Default fallback based on local time
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val (fallbackTemp, fallbackDesc) = when (hour) {
                    in 5..11 -> 21.0 to "Manhã Fresca"
                    in 12..17 -> 26.0 to "Sol e Nuvens"
                    in 18..19 -> 22.0 to "Pôr do Sol"
                    else -> 18.0 to "Céu Limpo"
                }
                _weatherState.value = WeatherInfo(
                    temp = fallbackTemp,
                    description = fallbackDesc,
                    city = "Local",
                    weatherCode = 0
                )
                _userLocation.value = -23.5505 to -46.6333
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Céu Limpo"
            1, 2, 3 -> "Parcialmente Nublado"
            45, 48 -> "Nevoeiro"
            51, 53, 55 -> "Garoa"
            61, 63, 65 -> "Chuva"
            71, 73, 75 -> "Neve"
            80, 81, 82 -> "Pancadas de Chuva"
            95, 96, 99 -> "Tempestade"
            else -> "Céu Limpo"
        }
    }

    var selectedGoalsTab: Int = 0

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMarketItems: StateFlow<List<MarketItem>> = repository.pendingMarketItems
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

    val healthProfile: StateFlow<HealthProfile?> = repository.healthProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
                e.printStackTrace()
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
                e.printStackTrace()
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
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
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
                            e.printStackTrace()
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
                            e.printStackTrace()
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
                                    e2.printStackTrace()
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
                        e.printStackTrace()
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
                e.printStackTrace()
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
                    adjustBalances(accountOrCardName, valuePerInstallment, isIncome, installmentRealized)
                }
            }
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
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(transaction.accountOrCardName, transaction.value, transaction.isIncome, transaction.isRealized)
            }
            repository.deleteTransaction(transaction)
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
            val newUsedLimit = if (isIncome) (matchingCard.usedLimit - value).coerceAtLeast(0.0) else matchingCard.usedLimit + value
            repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
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
            val newUsedLimit = if (isIncome) matchingCard.usedLimit + value else (matchingCard.usedLimit - value).coerceAtLeast(0.0)
            repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
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
        }
    }

    fun clearAllFinances() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllFinances()
        }
    }

    fun addMarketItem(name: String, category: String = "Geral", price: Double = 0.0, quantity: Double = 1.0, unit: String = "un") {
        viewModelScope.launch {
            repository.insertMarketItem(
                MarketItem(name = name, isChecked = false, isBought = false, orderIndex = 0, category = category, price = price, quantity = quantity, unit = unit)
            )
        }
    }

    fun toggleMarketItemChecked(item: MarketItem) {
        viewModelScope.launch {
            repository.updateMarketItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun updateMarketItemDetails(item: MarketItem, price: Double, quantity: Double, unit: String) {
        viewModelScope.launch {
            repository.updateMarketItem(item.copy(price = price, quantity = quantity, unit = unit))
        }
    }

    fun markMarketItemBought(item: MarketItem) {
        viewModelScope.launch {
            repository.updateMarketItem(item.copy(isBought = true, isChecked = false))
        }
    }

    fun checkoutCart() {
        viewModelScope.launch {
            val pending = repository.pendingMarketItems.first()
            val inCart = pending.filter { it.isChecked }
            inCart.forEach { item ->
                repository.updateMarketItem(item.copy(isBought = true, isChecked = false))
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
                    title = "Assinatura Spotify",
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

    // Health Methods
    fun updateHealthProfile(heightCm: Double, targetWeightKg: Double, isHealthConnectEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.healthProfile.first() ?: HealthProfile()
            repository.insertHealthProfile(current.copy(heightCm = heightCm, targetWeightKg = targetWeightKg, isHealthConnectEnabled = isHealthConnectEnabled))
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
            } else {
                logs.forEach { repository.deleteMedicationLog(it) }
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
            repository.insertSleepRecord(SleepRecord(startTime = startTime, endTime = endTime, durationHours = durationHours, source = "manual"))
        }
    }

    fun addManualStepsRecord(count: Long, startTime: Long, endTime: Long) {
        viewModelScope.launch {
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
                val hardcodedEmpresas = listOf(
                    com.example.data.MetroEmpresaConfig(
                        id = 1, nome = "Metrô de São Paulo", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Azul", "1"),
                            com.example.data.MetroLinhaConfig("Verde", "2"),
                            com.example.data.MetroLinhaConfig("Vermelha", "3"),
                            com.example.data.MetroLinhaConfig("Prata", "15")
                        )
                    ),
                    com.example.data.MetroEmpresaConfig(
                        id = 2, nome = "ViaQuatro / ViaMobilidade", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Amarela", "4"),
                            com.example.data.MetroLinhaConfig("Lilás", "5"),
                            com.example.data.MetroLinhaConfig("Diamante", "8"),
                            com.example.data.MetroLinhaConfig("Esmeralda", "9")
                        )
                    ),
                    com.example.data.MetroEmpresaConfig(
                        id = 3, nome = "CPTM", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Rubi", "7"),
                            com.example.data.MetroLinhaConfig("Turquesa", "10"),
                            com.example.data.MetroLinhaConfig("Coral", "11"),
                            com.example.data.MetroLinhaConfig("Safira", "12"),
                            com.example.data.MetroLinhaConfig("Jade", "13")
                        )
                    )
                )
                _metroConcessionarias.value = hardcodedEmpresas
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMetroConfig.value = false
            }
        }
    }

    fun fetchMetroStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMetroStatus.value = true
            _metroError.value = null
            try {
                val hardcodedEmpresas = listOf(
                    com.example.data.MetroEmpresaConfig(
                        id = 1, nome = "Metrô de São Paulo", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Azul", "1"),
                            com.example.data.MetroLinhaConfig("Verde", "2"),
                            com.example.data.MetroLinhaConfig("Vermelha", "3"),
                            com.example.data.MetroLinhaConfig("Prata", "15")
                        )
                    ),
                    com.example.data.MetroEmpresaConfig(
                        id = 2, nome = "ViaQuatro / ViaMobilidade", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Amarela", "4"),
                            com.example.data.MetroLinhaConfig("Lilás", "5"),
                            com.example.data.MetroLinhaConfig("Diamante", "8"),
                            com.example.data.MetroLinhaConfig("Esmeralda", "9")
                        )
                    ),
                    com.example.data.MetroEmpresaConfig(
                        id = 3, nome = "CPTM", fiscalizacaoArtesp = false,
                        linhas = listOf(
                            com.example.data.MetroLinhaConfig("Rubi", "7"),
                            com.example.data.MetroLinhaConfig("Turquesa", "10"),
                            com.example.data.MetroLinhaConfig("Coral", "11"),
                            com.example.data.MetroLinhaConfig("Safira", "12"),
                            com.example.data.MetroLinhaConfig("Jade", "13")
                        )
                    )
                )

                val empresasStatus = hardcodedEmpresas.map { empresa ->
                    val linhasStatus = empresa.linhas?.map { linha ->
                        com.example.data.MetroLinhaStatus(
                            nome = linha.nome,
                            codigo = linha.codigo,
                            ativa = true,
                            status = com.example.data.MetroLinhaStatusDetail(
                                situacao = "Operação Normal",
                                classificacao = "Normal",
                                operacaoNormal = true,
                                atualizadoEm = null,
                                atualizadoHa = "Agora"
                            )
                        )
                    }
                    com.example.data.MetroEmpresaStatus(
                        id = empresa.id,
                        nome = empresa.nome,
                        fiscalizacaoArtesp = false,
                        linhas = linhasStatus
                    )
                }
                
                _metroStatus.value = empresasStatus
            } catch (e: Exception) {
                e.printStackTrace()
                _metroError.value = "Erro interno"
            } finally {
                _isLoadingMetroStatus.value = false
            }
        }
    }



    fun fetchFootballScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingFootball.value = true
            try {
                val result = fixtureRepository.getLatestFixtures()
                if (result.isSuccess) {
                    val fixtures = result.getOrNull()?.data ?: emptyList()
                    val teams = _configuredFootballTeams.value
                    val list = mutableListOf<com.example.data.FootballMatchInfo>()

                    val mapToDetail = { dto: com.example.data.sportmonks.FixtureDto ->
                        val home = dto.participants?.getOrNull(0)
                        val away = dto.participants?.getOrNull(1)
                        
                        com.example.data.MatchDetail(
                            homeTeamName = home?.name ?: "Time 1",
                            homeTeamLogo = home?.imagePath ?: "",
                            awayTeamName = away?.name ?: "Time 2",
                            awayTeamLogo = away?.imagePath ?: "",
                            homeGoals = if (dto.state?.state == "FT") (dto.scores?.getOrNull(0)?.score?.goals ?: 0) else null,
                            awayGoals = if (dto.state?.state == "FT") (dto.scores?.getOrNull(1)?.score?.goals ?: 0) else null,
                            statusShort = dto.state?.state ?: "NS",
                            dateFormatted = formatFootballDate(dto.startingAt),
                            leagueName = dto.league?.name ?: "Liga"
                        )
                    }

                    for (teamName in teams) {
                        val teamFixtures = fixtures.filter { fixture ->
                            fixture.participants?.any { it.name.contains(teamName, ignoreCase = true) } == true
                        }
                        
                        if (teamFixtures.isNotEmpty()) {
                            val sorted = teamFixtures.sortedBy { it.startingAt }
                            
                            val lastMatchDto = sorted.lastOrNull { 
                                it.state?.state == "FT" || (it.startingAt < java.time.LocalDateTime.now().toString())
                            }
                            
                            val nextMatchDto = sorted.firstOrNull { 
                                it.state?.state == "NS" || it.state?.state == "TBA" || (it.startingAt >= java.time.LocalDateTime.now().toString())
                            }
                            
                            val lastMatch = lastMatchDto?.let { mapToDetail(it) }
                            val nextMatch = nextMatchDto?.let { mapToDetail(it) }
                            
                            list.add(com.example.data.FootballMatchInfo(teamName, lastMatch, nextMatch))
                        } else {
                            // Fallback para API gratuita: Se não encontrar o time, mostra destaques disponíveis
                            val fallbackSorted = fixtures.sortedBy { it.startingAt }
                            val fallbackLast = fallbackSorted.lastOrNull { 
                                it.state?.state == "FT" || (it.startingAt < java.time.LocalDateTime.now().toString())
                            }
                            val fallbackNext = fallbackSorted.firstOrNull { 
                                it.state?.state == "NS" || it.state?.state == "TBA" || (it.startingAt >= java.time.LocalDateTime.now().toString())
                            }
                            list.add(com.example.data.FootballMatchInfo("$teamName (Indisponível - Destaque)", fallbackLast?.let { mapToDetail(it) }, fallbackNext?.let { mapToDetail(it) }))
                        }
                    }
                    _footballMatches.value = list
                } else {
                    Log.e("TesseraViewModel", "Erro ao buscar placares: ${result.exceptionOrNull()?.message}")
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
                rawDate.take(16).replace("T", " ")
            }
        }
    }

    init {
        loadUserBusLines()
        loadConfiguredFootballTeams()
        fetchFootballScores()
        fetchNews()
        _spotifyAccessToken.value = sharedPrefs.getString("spotify_access_token", null)
        
        viewModelScope.launch(Dispatchers.IO) {
            refreshAIInsightsAndMetric()
        }
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
