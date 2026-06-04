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

    fun addTransaction(title: String, subtitle: String, value: Double, isIncome: Boolean, category: String, accountOrCardName: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    title = title,
                    subtitle = subtitle,
                    value = value,
                    isIncome = isIncome,
                    timestamp = System.currentTimeMillis(),
                    category = category,
                    accountOrCardName = accountOrCardName
                )
            )
            if (accountOrCardName.isNotEmpty()) {
                adjustBalances(accountOrCardName, value, isIncome)
            }
        }
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        viewModelScope.launch {
            if (oldTransaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(oldTransaction.accountOrCardName, oldTransaction.value, oldTransaction.isIncome)
            }
            repository.insertTransaction(newTransaction)
            if (newTransaction.accountOrCardName.isNotEmpty()) {
                adjustBalances(newTransaction.accountOrCardName, newTransaction.value, newTransaction.isIncome)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.accountOrCardName.isNotEmpty()) {
                rollbackBalances(transaction.accountOrCardName, transaction.value, transaction.isIncome)
            }
            repository.deleteTransaction(transaction)
        }
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
                repository.insertTransaction(Transaction(title = "Salário Mensal", subtitle = "Depósito Recebido", value = 18500.00, isIncome = true, timestamp = System.currentTimeMillis() - 86400000 * 5, category = "Salário", accountOrCardName = "XP Investimentos"))
                repository.insertTransaction(Transaction(title = "Mercado Municipal", subtitle = "Compras da semana", value = 450.20, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 3, category = "Alimentação", accountOrCardName = "Nubank"))
                repository.insertTransaction(Transaction(title = "Posto Ipiranga", subtitle = "Combustível", value = 220.00, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 2, category = "Transporte", accountOrCardName = "Nubank Ultravioleta"))
                repository.insertTransaction(Transaction(title = "Assinatura Netflix", subtitle = "Mensalidade", value = 55.90, isIncome = false, timestamp = System.currentTimeMillis() - 86400000 * 1, category = "Lazer", accountOrCardName = "Inter Black"))
                repository.insertTransaction(Transaction(title = "Jantar Premium", subtitle = "Restaurante", value = 380.00, isIncome = false, timestamp = System.currentTimeMillis() - 3600000 * 4, category = "Alimentação", accountOrCardName = "C6 Carbon"))
            }

            val habits = repository.allHabits.first()
            if (habits.isEmpty()) {
                repository.insertHabit(Habit(name = "Hidratação (3L)", isCompleted = false, streak = 12, iconName = "WaterDrop", colorHex = "#71D7CD", orderIndex = 0))
                repository.insertHabit(Habit(name = "Leitura Profunda", isCompleted = false, streak = 5, iconName = "MenuBook", colorHex = "#F9A826", orderIndex = 1))
                repository.insertHabit(Habit(name = "Mindfulness", isCompleted = true, streak = 21, iconName = "SelfImprovement", colorHex = "#D7B4F3", orderIndex = 2))
            }

            val goals = repository.allPurchaseGoals.first()
            if (goals.isEmpty()) {
                repository.insertPurchaseGoal(PurchaseGoal(title = "MacBook Pro M3", targetValue = 24000.00, currentValue = 15000.00, imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = System.currentTimeMillis() + 86400000L * 90, colorHex = "#71D7CD"))
                repository.insertPurchaseGoal(PurchaseGoal(title = "Viagem Kyoto", targetValue = 35000.00, currentValue = 8000.00, imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop", deadlineTimestamp = System.currentTimeMillis() + 86400000L * 180, colorHex = "#F9A826"))
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

    fun addPurchaseGoal(title: String, target: Double, current: Double, imageUrl: String, deadline: Long, colorHex: String) {
        viewModelScope.launch {
            repository.insertPurchaseGoal(PurchaseGoal(title = title, targetValue = target, currentValue = current, imageUrl = imageUrl, deadlineTimestamp = deadline, colorHex = colorHex))
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
            repository.clearHealthConnectWeightRecords()
            weights.forEach { repository.insertWeightRecord(it) }

            repository.clearHealthConnectSleepRecords()
            sleeps.forEach { repository.insertSleepRecord(it) }

            repository.clearHealthConnectStepsRecords()
            steps.forEach { repository.insertStepsRecord(it) }
        }
    }

    fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>> {
        return repository.getStepsForRoutine(routineId)
    }

    fun addRoutine(name: String, iconName: String) {
        viewModelScope.launch {
            repository.insertRoutine(Routine(name = name, iconName = iconName))
        }
    }

    fun addRoutineStep(routineId: Int, title: String, durationSeconds: Int, iconName: String, orderIndex: Int) {
        viewModelScope.launch {
            repository.insertRoutineStep(RoutineStep(routineId = routineId, title = title, durationSeconds = durationSeconds, iconName = iconName, orderIndex = orderIndex))
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
