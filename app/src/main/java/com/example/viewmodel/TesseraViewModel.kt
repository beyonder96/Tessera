package com.example.viewmodel

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import kotlinx.coroutines.launch

class TesseraViewModel(private val repository: TesseraRepository) : ViewModel() {

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
            repository.updatePetEvent(event.copy(isCompleted = !event.isCompleted))
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
                repository.insertPurchaseGoal(PurchaseGoal(title = "MacBook Pro M3", targetValue = 24000.00, currentValue = 15000.00, imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = System.currentTimeMillis() + 86400000L * 90, colorHex = "#71D7CD", priorityOrder = 1, priorityClassification = "Urgente"))
                repository.insertPurchaseGoal(PurchaseGoal(title = "Viagem Kyoto", targetValue = 35000.00, currentValue = 8000.00, imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = System.currentTimeMillis() + 86400000L * 180, colorHex = "#F9A826", priorityOrder = 2, priorityClassification = "Moderado"))
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

    fun addPurchaseGoal(title: String, target: Double, current: Double, imageUrl: String, deadline: Long, colorHex: String, priorityOrder: Int = 1, priorityClassification: String = "Moderado") {
        viewModelScope.launch {
            repository.insertPurchaseGoal(PurchaseGoal(title = title, targetValue = target, currentValue = current, imageUrl = imageUrl, deadlineTimestamp = deadline, colorHex = colorHex, priorityOrder = priorityOrder, priorityClassification = priorityClassification))
        }
    }

    fun updatePurchaseGoalProgress(goal: PurchaseGoal, addedValue: Double) {
        viewModelScope.launch {
            repository.updatePurchaseGoal(goal.copy(currentValue = goal.currentValue + addedValue))
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
            val routineId = repository.insertRoutine(routine).toInt()
            repository.clearStepsForRoutine(routineId)
            steps.forEachIndexed { index, step ->
                repository.insertRoutineStep(step.copy(routineId = routineId, orderIndex = index))
            }
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
}

class TesseraViewModelFactory(private val repository: TesseraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TesseraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TesseraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
