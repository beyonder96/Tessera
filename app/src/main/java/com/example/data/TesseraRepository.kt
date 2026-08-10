package com.example.data

import kotlinx.coroutines.flow.Flow

class TesseraRepository(private val dao: TesseraDao) {
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val pendingMarketItems: Flow<List<MarketItem>> = dao.getPendingMarketItems()
    val shoppingMarketItems: Flow<List<MarketItem>> = dao.getShoppingMarketItems()
    val boughtMarketItems: Flow<List<MarketItem>> = dao.getBoughtMarketItems()
    val allPetEvents: Flow<List<PetEvent>> = dao.getAllPetEvents()

    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
    }

    suspend fun insertMarketItem(item: MarketItem) {
        dao.insertMarketItem(item)
    }

    suspend fun updateMarketItem(item: MarketItem) {
        dao.updateMarketItem(item)
    }

    suspend fun deleteMarketItem(item: MarketItem) {
        dao.deleteMarketItem(item)
    }

    suspend fun deletePlanningItemsByNames(names: List<String>) {
        dao.deletePlanningItemsByNames(names)
    }

    suspend fun syncMarketItems(
        itemsToInsert: List<MarketItem>,
        itemsToUpdate: List<MarketItem>,
        itemsToDelete: List<MarketItem>
    ) {
        dao.syncMarketItems(itemsToInsert, itemsToUpdate, itemsToDelete)
    }

    suspend fun updatePetEvent(event: PetEvent) {
        dao.updatePetEvent(event)
    }
    
    suspend fun getPetEventsCount(): Int {
        return dao.getPetEventsCount()
    }

    suspend fun insertPetEvents(events: List<PetEvent>) {
        dao.insertPetEvents(events)
    }

    suspend fun insertPetEvent(event: PetEvent) {
        dao.insertPetEvent(event)
    }

    suspend fun deletePetEvent(event: PetEvent) {
        dao.deletePetEvent(event)
    }

    val allBankAccounts: Flow<List<BankAccount>> = dao.getAllBankAccounts()
    val allCreditCards: Flow<List<CreditCard>> = dao.getAllCreditCards()

    suspend fun insertBankAccount(account: BankAccount) {
        dao.insertBankAccount(account)
    }

    suspend fun deleteBankAccount(account: BankAccount) {
        dao.deleteBankAccount(account)
    }

    suspend fun insertCreditCard(card: CreditCard) {
        dao.insertCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCard) {
        dao.deleteCreditCard(card)
    }

    suspend fun payInvoice(cardId: Int) {
        dao.payInvoice(cardId)
    }

    // Benefit Cards
    val allBenefitCards: Flow<List<BenefitCard>> = dao.getAllBenefitCards()

    suspend fun insertBenefitCard(card: BenefitCard) {
        dao.insertBenefitCard(card)
    }

    suspend fun deleteBenefitCard(card: BenefitCard) {
        dao.deleteBenefitCard(card)
    }

    suspend fun clearAllFinances() {
        dao.clearAllFinances()
    }

    val allHabits: Flow<List<Habit>> = dao.getAllHabits()
    val allPurchaseGoals: Flow<List<PurchaseGoal>> = dao.getAllPurchaseGoals()

    suspend fun insertHabit(habit: Habit) {
        dao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        dao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        dao.deleteHabit(habit)
    }

    suspend fun insertPurchaseGoal(goal: PurchaseGoal) {
        dao.insertPurchaseGoal(goal)
    }

    suspend fun updatePurchaseGoal(goal: PurchaseGoal) {
        dao.updatePurchaseGoal(goal)
    }

    suspend fun deletePurchaseGoal(goal: PurchaseGoal) {
        dao.deletePurchaseGoal(goal)
    }

    // Health
    val healthProfile: Flow<HealthProfile?> = dao.getHealthProfile()
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()
    val allWeightRecords: Flow<List<WeightRecord>> = dao.getAllWeightRecords()
    val allSleepRecords: Flow<List<SleepRecord>> = dao.getAllSleepRecords()

    suspend fun insertHealthProfile(profile: HealthProfile) = dao.insertHealthProfile(profile)
    suspend fun insertMedication(medication: Medication) = dao.insertMedication(medication)
    suspend fun updateMedication(medication: Medication) = dao.updateMedication(medication)
    suspend fun deleteMedication(medication: Medication) = dao.deleteMedication(medication)
    
    suspend fun insertWeightRecord(record: WeightRecord) = dao.insertWeightRecord(record)
    suspend fun clearHealthConnectWeightRecords() = dao.clearHealthConnectWeightRecords()
    
    suspend fun insertSleepRecord(record: SleepRecord) = dao.insertSleepRecord(record)
    suspend fun clearHealthConnectSleepRecords() = dao.clearHealthConnectSleepRecords()
    suspend fun clearManualSleepForDay(startOfDay: Long, endOfDay: Long) = dao.clearManualSleepForDay(startOfDay, endOfDay)

    val allPets: Flow<List<PetEntity>> = dao.getAllPets()

    suspend fun insertPet(pet: PetEntity): Long {
        return dao.insertPet(pet)
    }

    suspend fun updatePet(pet: PetEntity) {
        dao.updatePet(pet)
    }

    suspend fun deletePet(pet: PetEntity) {
        dao.deletePet(pet)
    }

    fun getWeightHistoryForPet(petId: Int): Flow<List<PetWeightHistoryEntity>> {
        return dao.getWeightHistoryForPet(petId)
    }

    suspend fun insertWeightHistory(record: PetWeightHistoryEntity) {
        dao.insertWeightHistory(record)
    }

    suspend fun deleteWeightHistory(record: PetWeightHistoryEntity) {
        dao.deleteWeightHistory(record)
    }

    fun getMedicationLogsForRange(start: Long, end: Long): Flow<List<MedicationLog>> {
        return dao.getMedicationLogsForRange(start, end)
    }

    fun getLogsForMedication(medicationId: Int, start: Long, end: Long): Flow<List<MedicationLog>> {
        return dao.getLogsForMedication(medicationId, start, end)
    }

    suspend fun insertMedicationLog(log: MedicationLog) {
        dao.insertMedicationLog(log)
    }

    suspend fun deleteMedicationLog(log: MedicationLog) {
        dao.deleteMedicationLog(log)
    }

    val allStepsRecords: Flow<List<StepsRecord>> = dao.getAllStepsRecords()

    suspend fun insertStepsRecord(record: StepsRecord) {
        dao.insertStepsRecord(record)
    }

    suspend fun clearHealthConnectStepsRecords() {
        dao.clearHealthConnectStepsRecords()
    }

    suspend fun clearManualStepsForDay(startOfDay: Long, endOfDay: Long) {
        dao.clearManualStepsForDay(startOfDay, endOfDay)
    }

    // Routines
    val allRoutines: Flow<List<Routine>> = dao.getAllRoutines()

    fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>> {
        return dao.getStepsForRoutine(routineId)
    }

    suspend fun insertRoutine(routine: Routine): Long {
        return dao.insertRoutine(routine)
    }

    suspend fun insertRoutineStep(step: RoutineStep) {
        dao.insertRoutineStep(step)
    }

    suspend fun clearStepsForRoutine(routineId: Int) {
        dao.clearStepsForRoutine(routineId)
    }

    suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        dao.saveRoutineWithSteps(routine, steps)
    }

    suspend fun deleteRoutine(routine: Routine) {
        dao.deleteRoutine(routine)
    }

    val allDebts: Flow<List<Debt>> = dao.getAllDebts()

    suspend fun insertDebt(debt: Debt) {
        dao.insertDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        dao.deleteDebt(debt)
    }

    private val bibleMoshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val bibleRetrofit = retrofit2.Retrofit.Builder()
        .baseUrl("https://bolls.life/")
        .client(
            okhttp3.OkHttpClient.Builder()
                .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                })
                .build()
        )
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(bibleMoshi))
        .build()

    private val bibleApi = bibleRetrofit.create(BibleApi::class.java)

    suspend fun getRandomBibleVerse(): BibleVerseResponse {
        return bibleApi.getRandomVerse()
    }
}
