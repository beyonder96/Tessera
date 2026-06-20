package com.example.viewmodel

import android.content.Context
import com.example.LocalLLMManager
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
import com.example.data.SPTransService
import com.example.data.OverpassService
import com.example.data.TransportTimeline
import com.example.data.TransportParada
import com.example.data.FootballService
import com.example.data.FootballMatchInfo

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

    val localLLMManager = LocalLLMManager(applicationContext)

    val isLocalLLMActive: Boolean
        get() = localLLMManager.isLocalActive

    suspend fun generateAIResponse(userPrompt: String): String {
        return localLLMManager.generateResponse(userPrompt)
    }

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

    init {
        fetchWeather()
        fetchFootballScores()
        viewModelScope.launch(Dispatchers.IO) {
            localLLMManager.startInference("/storage/emulated/0/Download/gemma-4-e2b-it-qat.bin")
            refreshAIInsightsAndMetric()
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

                // Try to use AI if active
                if (localLLMManager.isLocalActive) {
                    val summaryPrompt = """
                        Você é a Tessera AI, uma companheira de conversação amigável e atenciosa.
                        Gere um breve resumo matinal/diário personalizado em português de até 2 ou 3 frases para o usuário Kenned, baseado nos dados:
                        - Sono de ontem: $sleepText de sono com eficiência estimada de $sleepEfficiency%.
                        - Passos de hoje: $todaySteps de 10.000 passos concluídos.
                        - Hábitos: $completedHabits de $totalHabits rituais diários concluídos.
                        - Saldo atual/Patrimônio: R$ $realBalance
                        - Compromissos pet: Marie e Churchill têm ${petEvents.count { !it.isCompleted }} pendentes.
                        - Compras pendentes no mercado: $pendingMarketCount itens.
                        - Medicamentos pendentes: $pendingMeds.
                        
                        Fale diretamente ao Kenned com tom positivo, motivador e minimalista. Não mencione formatação técnica, apenas o texto fluido do resumo.
                    """.trimIndent()
                    
                    try {
                        val aiBrief = localLLMManager.generateResponse(summaryPrompt)
                        if (aiBrief.isNotBlank()) {
                            _dailyBriefingText.value = aiBrief
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val prompt = """
                        Você é a Tessera AI analisando os dados locais do Kenned:
                        - Passos de hoje: $todaySteps/10000
                        - Hábitos diários: $completedHabits/$totalHabits concluídos
                        - Saldo atual: R$ $realBalance
                        - Itens pendentes no mercado: $pendingMarketCount
                        - Medicamentos pendentes hoje: $pendingMeds
                        
                        Determine a métrica prioritária de hoje (responda no formato METRIC: [PASSOS|HABITOS|FINANCAS|MERCADO] | META: [META_VALOR] | VALOR: [VALOR_VALOR] | ICONE: [DirectionsWalk|CheckCircle|AttachMoney|LocalMall]).
                        Gere também exatamente 3 cards de insights no formato:
                        CARD 1: [TITULO] | [DESCRICAO] | [ICONE: Medication|LocalMall|AttachMoney|Pets|Timer] | [CATEGORIA: health|market|finance|pets|goals]
                        CARD 2: ...
                        CARD 3: ...
                    """.trimIndent()
                    
                    val aiResponse = localLLMManager.generateResponse(prompt)
                    var parsedMetric: DynamicHeroMetric? = null
                    val parsedInsights = mutableListOf<InsightCard>()

                    try {
                        val lines = aiResponse.split("\n")
                        for (line in lines) {
                            if (line.startsWith("METRIC:")) {
                                val parts = line.substringAfter("METRIC:").split("|").map { it.trim() }
                                val mName = parts[0]
                                val mMeta = parts.find { it.startsWith("META:") }?.substringAfter("META:")?.toFloatOrNull() ?: 100f
                                val mVal = parts.find { it.startsWith("VALOR:") }?.substringAfter("VALOR:")?.toFloatOrNull() ?: 0f
                                val mIcon = parts.find { it.startsWith("ICONE:") }?.substringAfter("ICONE:") ?: "CheckCircle"
                                
                                val (name, label, color) = when (mName) {
                                    "PASSOS" -> Triple("PASSOS COMPLETADOS", "PASSOS DIÁRIOS", "#34C759")
                                    "HABITOS" -> Triple("RITUAIS DIÁRIOS", "HÁBITOS DE HOJE", "#71D7CD")
                                    "MERCADO" -> Triple("COMPRAS PENDENTES", "ITENS NO MERCADO", "#FF3B30")
                                    else -> Triple("BALANÇO FINANCEIRO", "SALDO DE HOJE", "#007AFF")
                                }
                                parsedMetric = DynamicHeroMetric(name, label, mVal, mMeta, mIcon, color)
                            } else if (line.startsWith("CARD")) {
                                val content = line.substringAfter(":").trim()
                                val parts = content.split("|").map { it.trim() }
                                if (parts.size >= 4) {
                                    parsedInsights.add(
                                        InsightCard(
                                            id = "ai_insight_${parsedInsights.size}",
                                            title = parts[0],
                                            description = parts[1],
                                            iconName = parts[2],
                                            category = parts[3]
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (parsedMetric != null && parsedInsights.isNotEmpty()) {
                        _heroMetric.value = parsedMetric
                        _aiInsights.value = parsedInsights.take(3)
                        return@launch
                    }
                }

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
            if (isRealized && accountOrCardName.isNotEmpty()) {
                adjustBalances(accountOrCardName, value, isIncome)
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
                
                if (installmentRealized && accountOrCardName.isNotEmpty()) {
                    adjustBalances(accountOrCardName, valuePerInstallment, isIncome)
                }
            }
        }
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        viewModelScope.launch {
            if (oldTransaction.isRealized && oldTransaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(oldTransaction.accountOrCardName, oldTransaction.value, oldTransaction.isIncome)
            }
            repository.insertTransaction(newTransaction)
            if (newTransaction.isRealized && newTransaction.accountOrCardName.isNotEmpty()) {
                adjustBalances(newTransaction.accountOrCardName, newTransaction.value, newTransaction.isIncome)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.isRealized && transaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(transaction.accountOrCardName, transaction.value, transaction.isIncome)
            }
            repository.deleteTransaction(transaction)
        }
    }

    fun realizeRecurrentTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // 1. Mark current transaction as realized
            val realizedTx = transaction.copy(
                isRealized = true,
                timestamp = System.currentTimeMillis() // Set realization timestamp to now
            )
            repository.insertTransaction(realizedTx)
            if (realizedTx.accountOrCardName.isNotEmpty()) {
                adjustBalances(realizedTx.accountOrCardName, realizedTx.value, realizedTx.isIncome)
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

    private fun adjustBalances(name: String, value: Double, isIncome: Boolean) {
        viewModelScope.launch {
            val accounts = repository.allBankAccounts.first()
            val matchingAccount = accounts.find { it.name == name }
            if (matchingAccount != null) {
                val newBalance = if (isIncome) matchingAccount.balance + value else matchingAccount.balance - value
                repository.insertBankAccount(matchingAccount.copy(balance = newBalance))
                return@launch
            }
            val cards = repository.allCreditCards.first()
            val matchingCard = cards.find { it.name == name }
            if (matchingCard != null) {
                val newUsedLimit = if (isIncome) (matchingCard.usedLimit - value).coerceAtLeast(0.0) else matchingCard.usedLimit + value
                repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
            }
        }
    }

    private fun rollbackBalances(name: String, value: Double, isIncome: Boolean) {
        viewModelScope.launch {
            val accounts = repository.allBankAccounts.first()
            val matchingAccount = accounts.find { it.name == name }
            if (matchingAccount != null) {
                val newBalance = if (isIncome) matchingAccount.balance - value else matchingAccount.balance + value
                repository.insertBankAccount(matchingAccount.copy(balance = newBalance))
                return@launch
            }
            val cards = repository.allCreditCards.first()
            val matchingCard = cards.find { it.name == name }
            if (matchingCard != null) {
                val newUsedLimit = if (isIncome) matchingCard.usedLimit + value else (matchingCard.usedLimit - value).coerceAtLeast(0.0)
                repository.insertCreditCard(matchingCard.copy(usedLimit = newUsedLimit))
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
        viewModelScope.launch {
            repository.deleteCreditCard(card)
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
                val response = metroService.getConcessionarias()
                _metroConcessionarias.value = response.empresas ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _metroError.value = "Falha ao carregar linhas: ${e.localizedMessage ?: "erro de rede"}"
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
                val response = metroService.getStatus()
                _metroStatus.value = response.empresas ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _metroError.value = "Falha ao carregar status: ${e.localizedMessage ?: "erro de rede"}"
            } finally {
                _isLoadingMetroStatus.value = false
            }
        }
    }

    // SPTrans (Olho Vivo) & Transporte Integrado
    private val sptransService = com.example.data.SPTransService.create()
    private val overpassService = com.example.data.OverpassService.create()
    private val cachedStations = mutableListOf<com.example.data.OverpassElement>()
    private val overpassQuery = """
        [out:json][timeout:25];
        area["name"="São Paulo"]->.searchArea;
        (
          node["railway"="station"]["subway"="yes"](area.searchArea);
          node["railway"="station"]["station"="subway"](area.searchArea);
        );
        out body;
    """.trimIndent()


    private val _transportTimelines = MutableStateFlow<List<com.example.data.TransportTimeline>>(emptyList())
    val transportTimelines: StateFlow<List<com.example.data.TransportTimeline>> = _transportTimelines.asStateFlow()

    private val _isLoadingTransport = MutableStateFlow(false)
    val isLoadingTransport: StateFlow<Boolean> = _isLoadingTransport.asStateFlow()

    private val _transportError = MutableStateFlow<String?>(null)
    val transportError: StateFlow<String?> = _transportError.asStateFlow()

    private val _userLocationName = MutableStateFlow("São Paulo")
    val userLocationName: StateFlow<String> = _userLocationName.asStateFlow()

    fun fetchTransportData(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingTransport.value = true
            _transportError.value = null
            
            val token = try {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.getField("SPTRANS_TOKEN")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }

            try {
                var authenticated = false
                if (token.isNotBlank() && token != "MY_SPTRANS_TOKEN") {
                    val authResponse = sptransService.authenticate(token)
                    authenticated = authResponse.body() == true
                }
                
                val timelines = mutableListOf<com.example.data.TransportTimeline>()
                
                // --- ÔNIBUS (SPTrans) ---
                if (authenticated) {
                    val paradas = sptransService.getParadasPorPosicao(lat, lng, 800)
                    if (paradas.isNotEmpty()) {
                        val paradaMaisProxima = paradas.minByOrNull { parada ->
                            calculateDistance(lat, lng, parada.py, parada.px)
                        }
                        
                        if (paradaMaisProxima != null) {
                            _userLocationName.value = paradaMaisProxima.np
                            
                             val previsao = sptransService.getPrevisaoParada(paradaMaisProxima.cp)
                             previsao.p?.l?.forEach { linha ->
                                 val proximas = mutableListOf<com.example.data.TransportParada>()
                                 
                                 val proximoVeiculo = linha.vs?.minByOrNull { it.t }
                                 val tempoRestanteMin = if (proximoVeiculo != null) {
                                     val tStr = proximoVeiculo.t
                                     try {
                                         val parts = tStr.split(":")
                                         val prevHour = parts[0].toInt()
                                         val prevMin = parts[1].toInt()
                                         val calendar = java.util.Calendar.getInstance()
                                         val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                                         val currentMin = calendar.get(java.util.Calendar.MINUTE)
                                         val diff = (prevHour * 60 + prevMin) - (currentHour * 60 + currentMin)
                                         if (diff > 0) diff else 2
                                     } catch (e: Exception) {
                                         5
                                     }
                                 } else {
                                     15
                                 }

                                 val paradasLinha = try {
                                     sptransService.getParadasPorLinha(linha.cl)
                                 } catch (e: Exception) {
                                     emptyList()
                                 }

                                 if (paradasLinha.isNotEmpty()) {
                                     var indexAtual = paradasLinha.indexOfFirst { it.cp == paradaMaisProxima.cp }
                                     if (indexAtual == -1) {
                                         indexAtual = paradasLinha.indexOfFirst { it.np.contains(paradaMaisProxima.np, ignoreCase = true) || paradaMaisProxima.np.contains(it.np, ignoreCase = true) }
                                     }

                                     if (indexAtual != -1) {
                                         // 1. Paradas anteriores
                                         if (indexAtual > 0) {
                                             proximas.add(
                                                 com.example.data.TransportParada(
                                                     paradaNome = paradasLinha[0].np,
                                                     horarioPrevisto = "--:--",
                                                     status = "passou"
                                                 )
                                             )

                                             val intermediariasPassadas = indexAtual - 1
                                             if (intermediariasPassadas > 2) {
                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = "Ride $intermediariasPassadas stops",
                                                         horarioPrevisto = "",
                                                         status = "passou",
                                                         mensagem = "Paradas intermediárias"
                                                     )
                                                 )
                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = paradasLinha[indexAtual - 1].np,
                                                         horarioPrevisto = "--:--",
                                                         status = "passou"
                                                     )
                                                 )
                                             } else {
                                                 for (i in 1 until indexAtual) {
                                                     proximas.add(
                                                         com.example.data.TransportParada(
                                                             paradaNome = paradasLinha[i].np,
                                                             horarioPrevisto = "--:--",
                                                             status = "passou"
                                                         )
                                                     )
                                                 }
                                             }
                                         }

                                         // 2. Parada Atual
                                         proximas.add(
                                             com.example.data.TransportParada(
                                                 paradaNome = paradaMaisProxima.np,
                                                 horarioPrevisto = proximoVeiculo?.t ?: "--:--",
                                                 status = "atual",
                                                 mensagem = "Você está aqui / Ônibus a ${tempoRestanteMin} min"
                                             )
                                         )

                                         // 3. Paradas futuras
                                         val totalParadas = paradasLinha.size
                                         val intermediariasFuturas = totalParadas - indexAtual - 1
                                         if (intermediariasFuturas > 0) {
                                             if (intermediariasFuturas > 4) {
                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = paradasLinha[indexAtual + 1].np,
                                                         horarioPrevisto = "--:--",
                                                         status = "proxima"
                                                     )
                                                 )
                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = paradasLinha[indexAtual + 2].np,
                                                         horarioPrevisto = "--:--",
                                                         status = "proxima"
                                                     )
                                                 )

                                                 val restamOcultar = intermediariasFuturas - 3
                                                 val tempoIntermediarioEstimado = restamOcultar * 2
                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = "Ride $restamOcultar stops",
                                                         horarioPrevisto = "",
                                                         status = "proxima",
                                                         mensagem = "Aprox. ${tempoIntermediarioEstimado} min"
                                                     )
                                                 )

                                                 proximas.add(
                                                     com.example.data.TransportParada(
                                                         paradaNome = paradasLinha.last().np,
                                                         horarioPrevisto = "--:--",
                                                         status = "destino"
                                                     )
                                                 )
                                             } else {
                                                 for (i in (indexAtual + 1) until totalParadas) {
                                                     val status = if (i == totalParadas - 1) "destino" else "proxima"
                                                     proximas.add(
                                                         com.example.data.TransportParada(
                                                             paradaNome = paradasLinha[i].np,
                                                             horarioPrevisto = "--:--",
                                                             status = status
                                                         )
                                                     )
                                                 }
                                             }
                                         }
                                     } else {
                                         proximas.add(com.example.data.TransportParada("Terminal: " + linha.lt1, "--:--", "passou"))
                                         proximas.add(com.example.data.TransportParada(paradaMaisProxima.np, proximoVeiculo?.t ?: "--:--", "atual", "Você está aqui / Ônibus a ${tempoRestanteMin} min"))
                                         proximas.add(com.example.data.TransportParada("Terminal: " + linha.lt0, "--:--", "destino"))
                                     }
                                 } else {
                                     proximas.add(com.example.data.TransportParada("Terminal: " + linha.lt1, "--:--", "passou"))
                                     proximas.add(com.example.data.TransportParada(paradaMaisProxima.np, proximoVeiculo?.t ?: "--:--", "atual", "Você está aqui / Ônibus a ${tempoRestanteMin} min"))
                                     proximas.add(com.example.data.TransportParada("Terminal: " + linha.lt0, "--:--", "destino"))
                                 }
                                 
                                 timelines.add(
                                     com.example.data.TransportTimeline(
                                         tipoTransporte = "onibus",
                                         linhaIdentificador = linha.c,
                                         linhaNome = "${linha.lt1} -> ${linha.lt0}",
                                         corTema = "#12723a",
                                         tempoRestanteTotalMin = tempoRestanteMin,
                                         statusLinha = "Operação Normal",
                                         proximasParadas = proximas
                                     )
                                 )
                             }
                        }
                    }
                }
                
                // --- METRÔ & TREM DINÂMICO (Overpass API) ---
                if (cachedStations.isEmpty()) {
                    try {
                        val response = overpassService.getStations(overpassQuery)
                        response.elements?.let {
                            cachedStations.addAll(it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val estacaoDinamicaProxima = if (cachedStations.isNotEmpty()) {
                    cachedStations.minByOrNull { calculateDistance(lat, lng, it.lat, it.lon) }
                } else {
                    null
                }
                
                val distDinamica = estacaoDinamicaProxima?.let { calculateDistance(lat, lng, it.lat, it.lon) } ?: Double.MAX_VALUE
                
                if (estacaoDinamicaProxima != null && distDinamica <= 800.0) {
                    val estacaoNome = estacaoDinamicaProxima.tags?.name ?: "Estação Próxima"
                    _userLocationName.value = "Estação $estacaoNome"
                    
                    val tags = estacaoDinamicaProxima.tags
                    val rawLine = tags?.line ?: tags?.colour ?: when {
                        estacaoNome.contains("Consolação", true) || estacaoNome.contains("Brigadeiro", true) || estacaoNome.contains("Trianon", true) -> "Verde"
                        estacaoNome.contains("Sé", true) || estacaoNome.contains("Luz", true) || estacaoNome.contains("República", true) -> "Azul"
                        else -> "Verde"
                    }
                    
                    val (lineCode, lineName, lineColor) = when {
                        rawLine.contains("azul", true) || rawLine.contains("1", true) -> Triple("1", "Linha 1 - Azul", "#005CA9")
                        rawLine.contains("verde", true) || rawLine.contains("2", true) -> Triple("2", "Linha 2 - Verde", "#008940")
                        rawLine.contains("vermelha", true) || rawLine.contains("3", true) -> Triple("3", "Linha 3 - Vermelha", "#EE3E23")
                        rawLine.contains("amarela", true) || rawLine.contains("4", true) -> Triple("4", "Linha 4 - Amarela", "#FFFFD100")
                        rawLine.contains("lilás", true) || rawLine.contains("5", true) -> Triple("5", "Linha 5 - Lilás", "#90278E")
                        rawLine.contains("prata", true) || rawLine.contains("15", true) -> Triple("15", "Linha 15 - Prata", "#97A0A6")
                        else -> Triple("2", "Linha 2 - Verde", "#008940")
                    }

                    var statusLinhaStr = "Operação Normal"
                    if (metroStatus.value.isEmpty()) {
                        try {
                            val metroResponse = metroService.getStatus()
                            _metroStatus.value = metroResponse.empresas ?: emptyList()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val matchingEmpresa = metroStatus.value.find { empresa ->
                        empresa.linhas?.any { it.codigo == lineCode } == true
                    }
                    val matchingLinha = matchingEmpresa?.linhas?.find { it.codigo == lineCode }
                    if (matchingLinha != null) {
                        statusLinhaStr = matchingLinha.status?.situacao ?: "Operação Normal"
                    }

                    val estacoesDaLinha = cachedStations.filter { element ->
                        val elLine = element.tags?.line ?: element.tags?.colour ?: ""
                        val elName = element.tags?.name ?: ""
                        elLine.contains(lineCode) || elLine.contains(rawLine, true) || elName.contains(estacaoNome, true) || when (lineCode) {
                            "2" -> elName.contains("Consolação", true) || elName.contains("Brigadeiro", true) || elName.contains("Trianon-Masp", true) || elName.contains("Paraíso", true) || elName.contains("Ana Rosa", true)
                            "4" -> elName.contains("Paulista", true) || elName.contains("Mackenzie", true) || elName.contains("República", true) || elName.contains("Luz", true)
                            else -> false
                        }
                    }.sortedBy { it.lat }

                    val paradasMetro = if (estacoesDaLinha.isNotEmpty()) {
                        var indexUser = estacoesDaLinha.indexOfFirst { (it.tags?.name ?: "") == estacaoNome }
                        if (indexUser == -1) indexUser = 0
                        
                        estacoesDaLinha.mapIndexed { idx, element ->
                            val isUserHere = idx == indexUser
                            val status = when {
                                idx < indexUser -> "passou"
                                idx == indexUser -> "atual"
                                else -> "proxima"
                            }
                            com.example.data.TransportParada(
                                paradaNome = element.tags?.name ?: "Estação",
                                horarioPrevisto = if (isUserHere) "Agora" else "--:--",
                                status = status,
                                mensagem = if (isUserHere) "Você está aqui" else null
                            )
                        }
                    } else {
                        listOf(
                            com.example.data.TransportParada(estacaoNome, "Agora", "atual", "Você está aqui")
                        )
                    }

                    timelines.add(
                        com.example.data.TransportTimeline(
                            tipoTransporte = "metro",
                            linhaIdentificador = "L$lineCode",
                            linhaNome = lineName,
                            corTema = lineColor,
                            tempoRestanteTotalMin = 0,
                            statusLinha = statusLinhaStr,
                            proximasParadas = paradasMetro
                        )
                    )
                } else {
                    // Fallback estático secundário caso o GPS não esteja em SP ou Overpass API esteja offline
                    val metroEstacoes = listOf(
                        MetroEstacaoConfig("Consolação", "2", -23.5587, -46.6601, "Linha 2 - Verde", "#008940"),
                        MetroEstacaoConfig("Trianon-Masp", "2", -23.5645, -46.6528, "Linha 2 - Verde", "#008940"),
                        MetroEstacaoConfig("Brigadeiro", "2", -23.5686, -46.6477, "Linha 2 - Verde", "#008940"),
                        MetroEstacaoConfig("Paraíso", "2", -23.5761, -46.6409, "Linha 2 - Verde", "#008940"),
                        MetroEstacaoConfig("Ana Rosa", "2", -23.5813, -46.6383, "Linha 2 - Verde", "#008940"),
                        MetroEstacaoConfig("Paulista", "4", -23.5552, -46.6620, "Linha 4 - Amarela", "#FFFFD100"),
                        MetroEstacaoConfig("Higienópolis-Mackenzie", "4", -23.5489, -46.6523, "Linha 4 - Amarela", "#FFFFD100"),
                        MetroEstacaoConfig("República", "4", -23.5431, -46.6429, "Linha 4 - Amarela", "#FFFFD100"),
                        MetroEstacaoConfig("Luz", "4", -23.5365, -46.6343, "Linha 4 - Amarela", "#FFFFD100"),
                        MetroEstacaoConfig("Sé", "1", -23.5502, -46.6339, "Linha 1 - Azul", "#005CA9"),
                        MetroEstacaoConfig("Liberdade", "1", -23.5554, -46.6353, "Linha 1 - Azul", "#005CA9"),
                        MetroEstacaoConfig("São Joaquim", "1", -23.5615, -46.6387, "Linha 1 - Azul", "#005CA9")
                    )
                    
                    val estacaoMaisProxima = metroEstacoes.minByOrNull { calculateDistance(lat, lng, it.lat, it.lng) }
                    val metroProximoValido = estacaoMaisProxima != null && calculateDistance(lat, lng, estacaoMaisProxima.lat, estacaoMaisProxima.lng) <= 800.0
                    
                    if (metroProximoValido && estacaoMaisProxima != null) {
                        _userLocationName.value = "Estação " + estacaoMaisProxima.nome
                        
                        val lineCode = estacaoMaisProxima.linhaCodigo
                        var statusLinhaStr = "Operação Normal"
                        
                        if (metroStatus.value.isEmpty()) {
                            try {
                                val metroResponse = metroService.getStatus()
                                _metroStatus.value = metroResponse.empresas ?: emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        val matchingEmpresa = metroStatus.value.find { empresa ->
                            empresa.linhas?.any { it.codigo == lineCode } == true
                        }
                        val matchingLinha = matchingEmpresa?.linhas?.find { it.codigo == lineCode }
                        if (matchingLinha != null) {
                            statusLinhaStr = matchingLinha.status?.situacao ?: "Operação Normal"
                        }
                        
                        val paradasMetro = metroEstacoes.filter { it.linhaCodigo == lineCode }.map { estacao ->
                            val isUserHere = estacao.nome == estacaoMaisProxima.nome
                            val status = if (isUserHere) "atual" else "proxima"
                            val msg = if (isUserHere) "Você está aqui" else null
                            
                            com.example.data.TransportParada(
                                paradaNome = estacao.nome,
                                horarioPrevisto = if (isUserHere) "Agora" else "--:--",
                                status = status,
                                mensagem = msg
                            )
                        }
                        
                        timelines.add(
                            com.example.data.TransportTimeline(
                                tipoTransporte = "metro",
                                linhaIdentificador = "L$lineCode",
                                linhaNome = estacaoMaisProxima.linhaNome,
                                corTema = estacaoMaisProxima.corHex,
                                tempoRestanteTotalMin = 0,
                                statusLinha = statusLinhaStr,
                                proximasParadas = paradasMetro
                            )
                        )
                    }
                }

                // Fallback fictício se as listas de dados estiverem vazias para fins demonstrativos
                if (timelines.isEmpty()) {
                    _userLocationName.value = "Airport T2"
                    
                    timelines.add(
                        com.example.data.TransportTimeline(
                            tipoTransporte = "onibus",
                            linhaIdentificador = "809P-10",
                            linhaNome = "Metrô Barra Funda / Terminal Pinheiros",
                            corTema = "#12723a",
                            tempoRestanteTotalMin = 25,
                            statusLinha = "Operação Normal",
                            proximasParadas = listOf(
                                com.example.data.TransportParada("Metrô Barra Funda", "14:10", "passou"),
                                com.example.data.TransportParada("Ride 8 stops", "", "passou", "Aprox. 12 min"),
                                com.example.data.TransportParada("Av. Prof. Francisco Morato, 250", "14:22", "passou"),
                                com.example.data.TransportParada("Estação Vital Brasil", "14:25", "atual", "Você está aqui / Ônibus a 2 min"),
                                com.example.data.TransportParada("Praça Jorge de Lima", "14:29", "proxima"),
                                com.example.data.TransportParada("Ride 5 stops", "", "proxima", "Aprox. 10 min"),
                                com.example.data.TransportParada("Terminal Pinheiros", "14:45", "destino")
                            )
                        )
                    )
                    
                    timelines.add(
                        com.example.data.TransportTimeline(
                            tipoTransporte = "metro",
                            linhaIdentificador = "L1",
                            linhaNome = "Linha 1 - Azul",
                            corTema = "#EE3E23",
                            tempoRestanteTotalMin = 35,
                            statusLinha = "Operação Normal",
                            proximasParadas = listOf(
                                com.example.data.TransportParada("Urquinaona", "13:22", "passou", "Hospital de Bellvitge"),
                                com.example.data.TransportParada("Ride 10 stops", "", "passou", "18min"),
                                com.example.data.TransportParada("Catalunya", "13:40", "proxima", "20m"),
                                com.example.data.TransportParada("Universitat", "13:42", "proxima", "22m"),
                                com.example.data.TransportParada("Urgell", "13:44", "proxima", "24m"),
                                com.example.data.TransportParada("Rocafort", "13:46", "proxima", "26m"),
                                com.example.data.TransportParada("Pl. Espanya", "13:47", "proxima", "27m"),
                                com.example.data.TransportParada("Hostafrancs", "13:50", "proxima", "30m"),
                                com.example.data.TransportParada("Plaça de Sants", "13:51", "proxima", "31m"),
                                com.example.data.TransportParada("Mercat Nou", "13:53", "proxima", "33m"),
                                com.example.data.TransportParada("Santa Eulàlia", "13:55", "proxima", "35m"),
                                com.example.data.TransportParada("Torrasa", "13:57", "destino", "take left side / 13:42")
                            )
                        )
                    )
                }
                
                _transportTimelines.value = timelines
                
            } catch (e: Exception) {
                e.printStackTrace()
                _transportError.value = "Erro ao buscar dados: ${e.localizedMessage ?: "falha de rede"}"
                
                _userLocationName.value = "Airport T2 (Offline)"
                _transportTimelines.value = listOf(
                    com.example.data.TransportTimeline(
                        tipoTransporte = "metro",
                        linhaIdentificador = "L1",
                        linhaNome = "Linha 1 - Azul",
                        corTema = "#EE3E23",
                        tempoRestanteTotalMin = 35,
                        statusLinha = "Operação Normal",
                        proximasParadas = listOf(
                            com.example.data.TransportParada("Urquinaona", "13:22", "passou", "Hospital de Bellvitge"),
                            com.example.data.TransportParada("Ride 10 stops", "", "passou", "18min"),
                            com.example.data.TransportParada("Catalunya", "13:40", "proxima", "20m"),
                            com.example.data.TransportParada("Universitat", "13:42", "proxima", "22m"),
                            com.example.data.TransportParada("Urgell", "13:44", "proxima", "24m"),
                            com.example.data.TransportParada("Rocafort", "13:46", "proxima", "26m"),
                            com.example.data.TransportParada("Pl. Espanya", "13:47", "proxima", "27m"),
                            com.example.data.TransportParada("Hostafrancs", "13:50", "proxima", "30m"),
                            com.example.data.TransportParada("Plaça de Sants", "13:51", "proxima", "31m"),
                            com.example.data.TransportParada("Mercat Nou", "13:53", "proxima", "33m"),
                            com.example.data.TransportParada("Santa Eulàlia", "13:55", "proxima", "35m"),
                            com.example.data.TransportParada("Torrasa", "13:57", "destino", "take left side / 13:42")
                        )
                    )
                )
            } finally {
                _isLoadingTransport.value = false
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
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

    private data class MetroEstacaoConfig(
        val nome: String,
        val linhaCodigo: String,
        val lat: Double,
        val lng: Double,
        val linhaNome: String,
        val corHex: String
    )

    // Football Integration (API-FOOTBALL)
    private val footballService = com.example.data.FootballService.create()

    private val _footballMatches = MutableStateFlow<List<com.example.data.FootballMatchInfo>>(emptyList())
    val footballMatches: StateFlow<List<com.example.data.FootballMatchInfo>> = _footballMatches.asStateFlow()

    private val _isLoadingFootball = MutableStateFlow(false)
    val isLoadingFootball: StateFlow<Boolean> = _isLoadingFootball.asStateFlow()

    fun fetchFootballScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingFootball.value = true
            
            val token = try {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.getField("FOOTBALL_API_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }

            try {
                if (token.isNotBlank() && token != "MY_FOOTBALL_API_KEY") {
                    val list = mutableListOf<com.example.data.FootballMatchInfo>()
                    
                    val teamIds = listOf(
                        6 to "Brasil",
                        127 to "Flamengo"
                    )
                    
                    teamIds.forEach { (id, name) ->
                        val lastResponse = footballService.getFixtures(apiKey = token, teamId = id, last = 1)
                        val lastFixture = lastResponse.response?.firstOrNull()
                        
                        val nextResponse = footballService.getFixtures(apiKey = token, teamId = id, next = 1)
                        val nextFixture = nextResponse.response?.firstOrNull()
                        
                        val lastDetail = lastFixture?.let {
                            com.example.data.MatchDetail(
                                homeTeamName = it.teams?.home?.name ?: "",
                                homeTeamLogo = it.teams?.home?.logo ?: "",
                                awayTeamName = it.teams?.away?.name ?: "",
                                awayTeamLogo = it.teams?.away?.logo ?: "",
                                homeGoals = it.goals?.home,
                                awayGoals = it.goals?.away,
                                statusShort = it.fixture?.status?.short ?: "FT",
                                dateFormatted = formatFootballDate(it.fixture?.date),
                                leagueName = it.league?.name ?: ""
                            )
                        }
                        
                        val nextDetail = nextFixture?.let {
                            com.example.data.MatchDetail(
                                homeTeamName = it.teams?.home?.name ?: "",
                                homeTeamLogo = it.teams?.home?.logo ?: "",
                                awayTeamName = it.teams?.away?.name ?: "",
                                awayTeamLogo = it.teams?.away?.logo ?: "",
                                homeGoals = null,
                                awayGoals = null,
                                statusShort = it.fixture?.status?.short ?: "NS",
                                dateFormatted = formatFootballDate(it.fixture?.date),
                                leagueName = it.league?.name ?: ""
                            )
                        }
                        
                        list.add(
                            com.example.data.FootballMatchInfo(
                                teamName = name,
                                lastMatch = lastDetail,
                                nextMatch = nextDetail
                            )
                        )
                    }
                    
                    if (list.isNotEmpty() && list.any { it.lastMatch != null || it.nextMatch != null }) {
                        _footballMatches.value = list
                        return@launch
                    }
                }
                
                loadMockFootballScores()
                
            } catch (e: Exception) {
                e.printStackTrace()
                loadMockFootballScores()
            } finally {
                _isLoadingFootball.value = false
            }
        }
    }

    private fun loadMockFootballScores() {
        _footballMatches.value = listOf(
            com.example.data.FootballMatchInfo(
                teamName = "Brasil",
                lastMatch = com.example.data.MatchDetail(
                    homeTeamName = "Inglaterra",
                    homeTeamLogo = "https://media.api-sports.io/football/teams/10.png",
                    awayTeamName = "Brasil",
                    awayTeamLogo = "https://media.api-sports.io/football/teams/6.png",
                    homeGoals = 0,
                    awayGoals = 1,
                    statusShort = "FT",
                    dateFormatted = "23/03 16:00",
                    leagueName = "Amistoso Internacional"
                ),
                nextMatch = com.example.data.MatchDetail(
                    homeTeamName = "Brasil",
                    homeTeamLogo = "https://media.api-sports.io/football/teams/6.png",
                    awayTeamName = "Argentina",
                    awayTeamLogo = "https://media.api-sports.io/football/teams/26.png",
                    homeGoals = null,
                    awayGoals = null,
                    statusShort = "NS",
                    dateFormatted = "05/09 21:30",
                    leagueName = "Eliminatórias da Copa"
                )
            ),
            com.example.data.FootballMatchInfo(
                teamName = "Flamengo",
                lastMatch = com.example.data.MatchDetail(
                    homeTeamName = "Flamengo",
                    homeTeamLogo = "https://media.api-sports.io/football/teams/127.png",
                    awayTeamName = "Vasco",
                    awayTeamLogo = "https://media.api-sports.io/football/teams/133.png",
                    homeGoals = 2,
                    awayGoals = 0,
                    statusShort = "FT",
                    dateFormatted = "14/06 16:00",
                    leagueName = "Série A"
                ),
                nextMatch = com.example.data.MatchDetail(
                    homeTeamName = "Flamengo",
                    homeTeamLogo = "https://media.api-sports.io/football/teams/127.png",
                    awayTeamName = "Palmeiras",
                    awayTeamLogo = "https://media.api-sports.io/football/teams/121.png",
                    homeGoals = null,
                    awayGoals = null,
                    statusShort = "NS",
                    dateFormatted = "25/06 20:00",
                    leagueName = "Série A"
                )
            )
        )
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
