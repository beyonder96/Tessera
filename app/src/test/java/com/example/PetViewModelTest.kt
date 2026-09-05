package com.example

import com.example.data.*
import com.example.viewmodel.PetViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTesseraDao : TesseraDao {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun insertTransaction(transaction: Transaction) {}
    override suspend fun insertTransactions(transactions: List<Transaction>) {}
    override suspend fun deleteTransaction(transaction: Transaction) {}
    override suspend fun deleteTransactions(transactions: List<Transaction>) {}
    override suspend fun updateTransactionsCategory(ids: List<Int>, category: String) {}
    override fun getPendingMarketItems(): Flow<List<MarketItem>> = flowOf(emptyList())
    override fun getBoughtMarketItems(): Flow<List<MarketItem>> = flowOf(emptyList())
    override suspend fun insertMarketItem(item: MarketItem) {}
    override suspend fun updateMarketItem(item: MarketItem) {}
    override suspend fun getPetEventsCount(): Int = 0
    override fun getAllPetEvents(): Flow<List<PetEvent>> = flowOf(emptyList())
    override suspend fun updatePetEvent(event: PetEvent) {}
    override suspend fun insertPetEvents(events: List<PetEvent>) {}
    override suspend fun insertPetEvent(event: PetEvent) {}
    override suspend fun deletePetEvent(event: PetEvent) {}
    override fun getAllBankAccounts(): Flow<List<BankAccount>> = flowOf(emptyList())
    override suspend fun insertBankAccount(account: BankAccount) {}
    override suspend fun deleteBankAccount(account: BankAccount) {}
    override fun getAllCreditCards(): Flow<List<CreditCard>> = flowOf(emptyList())
    override suspend fun insertCreditCard(card: CreditCard) {}
    override suspend fun deleteCreditCard(card: CreditCard) {}
    override fun getAllHabits(): Flow<List<Habit>> = flowOf(emptyList())
    override suspend fun insertHabit(habit: Habit) {}
    override suspend fun updateHabit(habit: Habit) {}
    override suspend fun deleteHabit(habit: Habit) {}
    override fun getAllPurchaseGoals(): Flow<List<PurchaseGoal>> = flowOf(emptyList())
    override suspend fun insertPurchaseGoal(goal: PurchaseGoal) {}
    override suspend fun updatePurchaseGoal(goal: PurchaseGoal) {}
    override suspend fun deletePurchaseGoal(goal: PurchaseGoal) {}
    override fun getHealthProfile(): Flow<HealthProfile?> = flowOf(null)
    override suspend fun insertHealthProfile(profile: HealthProfile) {}
    override fun getAllMedications(): Flow<List<Medication>> = flowOf(emptyList())
    override suspend fun insertMedication(medication: Medication) {}
    override suspend fun updateMedication(medication: Medication) {}
    override suspend fun deleteMedication(medication: Medication) {}
    override fun getAllWeightRecords(): Flow<List<WeightRecord>> = flowOf(emptyList())
    override suspend fun insertWeightRecord(record: WeightRecord) {}
    override suspend fun clearHealthConnectWeightRecords() {}
    override fun getAllSleepRecords(): Flow<List<SleepRecord>> = flowOf(emptyList())
    override suspend fun insertSleepRecord(record: SleepRecord) {}
    override suspend fun clearHealthConnectSleepRecords() {}

    // Pet Entity additions
    override fun getAllPets(): Flow<List<PetEntity>> = flowOf(emptyList())
    override suspend fun insertPet(pet: PetEntity): Long = 0L
    override suspend fun deletePet(pet: PetEntity) {}
    override fun getWeightHistoryForPet(petId: Int): Flow<List<PetWeightHistoryEntity>> = flowOf(emptyList())
    override suspend fun insertWeightHistory(record: PetWeightHistoryEntity) {}
    override suspend fun deleteWeightHistory(record: PetWeightHistoryEntity) {}

    // Medication Logs additions
    override fun getMedicationLogsForRange(start: Long, end: Long): Flow<List<MedicationLog>> = flowOf(emptyList())
    override fun getLogsForMedication(medicationId: Int, start: Long, end: Long): Flow<List<MedicationLog>> = flowOf(emptyList())
    override suspend fun insertMedicationLog(log: MedicationLog) {}
    override suspend fun deleteMedicationLog(log: MedicationLog) {}

    // Steps Records additions
    override fun getAllStepsRecords(): Flow<List<StepsRecord>> = flowOf(emptyList())
    override suspend fun insertStepsRecord(record: StepsRecord) {}
    override suspend fun clearHealthConnectStepsRecords() {}

    override suspend fun updatePet(pet: PetEntity) {}
    override fun getAllRoutines(): Flow<List<Routine>> = flowOf(emptyList())
    override fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>> = flowOf(emptyList())
    override suspend fun insertRoutine(routine: Routine): Long = 0L
    override suspend fun insertRoutineStep(step: RoutineStep) {}
    override suspend fun clearStepsForRoutine(routineId: Int) {}
    override suspend fun deleteRoutine(routine: Routine) {}

    override suspend fun payInvoice(cardId: Int) {}
    override suspend fun clearAllTransactions() {}
    override suspend fun clearAllBankAccounts() {}
    override suspend fun clearAllCreditCards() {}

    override fun getShoppingMarketItems(): Flow<List<MarketItem>> = flowOf(emptyList())
    override suspend fun deleteMarketItem(item: MarketItem) {}
    override suspend fun deletePlanningItemsByNames(names: List<String>) {}
    override fun getAllBenefitCards(): Flow<List<BenefitCard>> = flowOf(emptyList())
    override suspend fun insertBenefitCard(card: BenefitCard) {}
    override suspend fun deleteBenefitCard(card: BenefitCard) {}
    override suspend fun clearAllBenefitCards() {}
    override suspend fun clearManualSleepForDay(startOfDay: Long, endOfDay: Long) {}
    override suspend fun clearManualStepsForDay(startOfDay: Long, endOfDay: Long) {}
    override fun getAllDebts(): Flow<List<Debt>> = flowOf(emptyList())
    override suspend fun insertDebt(debt: Debt) {}
    override suspend fun deleteDebt(debt: Debt) {}
    override suspend fun clearAllDebts() {}

    // MealRecord methods
    override fun getAllMealRecords(): Flow<List<MealRecord>> = flowOf(emptyList())
    override fun getMealRecordsForDate(date: String): Flow<List<MealRecord>> = flowOf(emptyList())
    override suspend fun insertMealRecord(meal: MealRecord): Long = 0L
    override suspend fun updateMealRecord(meal: MealRecord) {}
    override suspend fun deleteMealRecord(meal: MealRecord) {}
    override suspend fun deleteMealRecordById(id: Int) {}

    // WaterRecord methods
    override fun getAllWaterRecords(): Flow<List<WaterRecord>> = flowOf(emptyList())
    override fun getWaterRecordsForDate(date: String): Flow<List<WaterRecord>> = flowOf(emptyList())
    override suspend fun insertWaterRecord(record: WaterRecord): Long = 0L
    override suspend fun deleteWaterRecord(record: WaterRecord) {}
    override suspend fun deleteWaterRecordById(id: Int) {}
}

class PetViewModelTest {

    private val dao = FakeTesseraDao()
    private val repository = TesseraRepository(dao)
    private val viewModel = PetViewModel(repository)

    @Test
    fun testRgaValidation_correctFormat() {
        assertTrue(viewModel.validateRga("1234567"))
        assertTrue(viewModel.validateRga("0000000"))
        assertTrue(viewModel.validateRga("9999999"))
    }

    @Test
    fun testRgaValidation_incorrectFormat() {
        assertFalse(viewModel.validateRga("123456"))    // 6 digits
        assertFalse(viewModel.validateRga("12345678"))  // 8 digits
        assertFalse(viewModel.validateRga("123a567"))   // non-numeric
        assertFalse(viewModel.validateRga(""))           // empty
    }

    @Test
    fun testMicrochipValidation_correctFormat() {
        assertTrue(viewModel.validateMicrochip("123456789012345"))
        assertTrue(viewModel.validateMicrochip("000000000000000"))
        assertTrue(viewModel.validateMicrochip("999999999999999"))
    }

    @Test
    fun testMicrochipValidation_incorrectFormat() {
        assertFalse(viewModel.validateMicrochip("12345678901234"))   // 14 digits
        assertFalse(viewModel.validateMicrochip("1234567890123456")) // 16 digits
        assertFalse(viewModel.validateMicrochip("1234567890abcde"))  // non-numeric
        assertFalse(viewModel.validateMicrochip(""))                  // empty
    }

    @Test
    fun testVaccineExpiration_isExpired() {
        // null is expired (pending)
        assertTrue(viewModel.isVaccineExpired(null))

        // exactly 365 days ago is expired
        val exactly365DaysAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        assertTrue(viewModel.isVaccineExpired(exactly365DaysAgo))

        // 400 days ago is expired
        val olderDate = System.currentTimeMillis() - (400L * 24 * 60 * 60 * 1000)
        assertTrue(viewModel.isVaccineExpired(olderDate))
    }

    @Test
    fun testVaccineExpiration_isNotExpired() {
        // 10 days ago is not expired
        val recentDate = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        assertFalse(viewModel.isVaccineExpired(recentDate))

        // 364 days ago is not expired
        val almostAYearAgo = System.currentTimeMillis() - (364L * 24 * 60 * 60 * 1000)
        assertFalse(viewModel.isVaccineExpired(almostAYearAgo))
    }
}
