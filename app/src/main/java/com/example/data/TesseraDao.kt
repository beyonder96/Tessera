package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TesseraDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransactions(transactions: List<Transaction>)

    @Query("UPDATE transactions SET category = :category WHERE id IN (:ids)")
    suspend fun updateTransactionsCategory(ids: List<Int>, category: String)

    @Query("SELECT * FROM market_items WHERE isBought = 0 AND inMarket = 0 ORDER BY orderIndex ASC")
    fun getPendingMarketItems(): Flow<List<MarketItem>>

    @Query("SELECT * FROM market_items WHERE isBought = 0 AND inMarket = 1 ORDER BY orderIndex ASC")
    fun getShoppingMarketItems(): Flow<List<MarketItem>>

    @Query("SELECT * FROM market_items WHERE isBought = 1 ORDER BY id DESC")
    fun getBoughtMarketItems(): Flow<List<MarketItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItem(item: MarketItem)

    @Update
    suspend fun updateMarketItem(item: MarketItem)

    @Delete
    suspend fun deleteMarketItem(item: MarketItem)

    @Query("DELETE FROM market_items WHERE LOWER(name) IN (:names) AND inMarket = 0")
    suspend fun deletePlanningItemsByNames(names: List<String>)

    @androidx.room.Transaction
    suspend fun syncMarketItems(
        itemsToInsert: List<MarketItem>,
        itemsToUpdate: List<MarketItem>,
        itemsToDelete: List<MarketItem>
    ) {
        itemsToInsert.forEach { insertMarketItem(it) }
        itemsToUpdate.forEach { updateMarketItem(it) }
        itemsToDelete.forEach { deleteMarketItem(it) }
    }

    @Query("SELECT COUNT(*) FROM pet_events")
    suspend fun getPetEventsCount(): Int

    @Query("SELECT * FROM pet_events ORDER BY id ASC")
    fun getAllPetEvents(): Flow<List<PetEvent>>

    @Update
    suspend fun updatePetEvent(event: PetEvent)
    
    @Insert
    suspend fun insertPetEvents(events: List<PetEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetEvent(event: PetEvent)

    @Delete
    suspend fun deletePetEvent(event: PetEvent)

    // Bank Account Queries
    @Query("SELECT * FROM bank_accounts ORDER BY id ASC")
    fun getAllBankAccounts(): Flow<List<BankAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccount)

    @Delete
    suspend fun deleteBankAccount(account: BankAccount)

    // Credit Card Queries
    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCard)

    @Delete
    suspend fun deleteCreditCard(card: CreditCard)

    @Query("UPDATE credit_cards SET usedLimit = 0.0 WHERE id = :cardId")
    suspend fun payInvoice(cardId: Int)

    // Benefit Card Queries
    @Query("SELECT * FROM benefit_cards ORDER BY id ASC")
    fun getAllBenefitCards(): Flow<List<BenefitCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenefitCard(card: BenefitCard)

    @Delete
    suspend fun deleteBenefitCard(card: BenefitCard)

    // Finance clear operations
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM bank_accounts")
    suspend fun clearAllBankAccounts()

    @Query("DELETE FROM credit_cards")
    suspend fun clearAllCreditCards()

    @Query("DELETE FROM benefit_cards")
    suspend fun clearAllBenefitCards()

    @Query("DELETE FROM debts")
    suspend fun clearAllDebts()

    @androidx.room.Transaction
    suspend fun clearAllFinances() {
        clearAllTransactions()
        clearAllBankAccounts()
        clearAllCreditCards()
        clearAllBenefitCards()
        clearAllDebts()
    }

    // Habits
    @Query("SELECT * FROM habits ORDER BY orderIndex ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Purchase Goals
    @Query("SELECT * FROM purchase_goals ORDER BY priorityOrder ASC, deadlineTimestamp ASC")
    fun getAllPurchaseGoals(): Flow<List<PurchaseGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseGoal(goal: PurchaseGoal)

    @Update
    suspend fun updatePurchaseGoal(goal: PurchaseGoal)

    @Delete
    suspend fun deletePurchaseGoal(goal: PurchaseGoal)

    // Health Profile
    @Query("SELECT * FROM health_profile WHERE id = 1")
    fun getHealthProfile(): Flow<HealthProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthProfile(profile: HealthProfile)

    // Medications
    @Query("SELECT * FROM medications ORDER BY time ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    // Weight Records
    @Query("SELECT * FROM weight_records ORDER BY timestamp ASC")
    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecord)

    @Query("DELETE FROM weight_records WHERE source = 'Health Connect'")
    suspend fun clearHealthConnectWeightRecords()

    // Sleep Records
    @Query("SELECT * FROM sleep_records ORDER BY endTime DESC")
    fun getAllSleepRecords(): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecord)

    @Query("DELETE FROM sleep_records WHERE source = 'Health Connect'")
    suspend fun clearHealthConnectSleepRecords()

    @Query("DELETE FROM sleep_records WHERE source = 'manual' AND endTime >= :startOfDay AND endTime <= :endOfDay")
    suspend fun clearManualSleepForDay(startOfDay: Long, endOfDay: Long)

    // Pets Queries
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    // Pet Weight History Queries
    @Query("SELECT * FROM pet_weight_history WHERE petId = :petId ORDER BY date ASC")
    fun getWeightHistoryForPet(petId: Int): Flow<List<PetWeightHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightHistory(record: PetWeightHistoryEntity)

    @Delete
    suspend fun deleteWeightHistory(record: PetWeightHistoryEntity)

    // Medication Logs
    @Query("SELECT * FROM medication_logs WHERE takenTimestamp >= :start AND takenTimestamp <= :end")
    fun getMedicationLogsForRange(start: Long, end: Long): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND takenTimestamp >= :start AND takenTimestamp <= :end")
    fun getLogsForMedication(medicationId: Int, start: Long, end: Long): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationLog(log: MedicationLog)

    @Delete
    suspend fun deleteMedicationLog(log: MedicationLog)

    // Steps Records
    @Query("SELECT * FROM steps_records ORDER BY startTime DESC")
    fun getAllStepsRecords(): Flow<List<StepsRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepsRecord(record: StepsRecord)

    @Query("DELETE FROM steps_records WHERE source = 'Health Connect'")
    suspend fun clearHealthConnectStepsRecords()

    @Query("DELETE FROM steps_records WHERE source = 'manual' AND endTime >= :startOfDay AND endTime <= :endOfDay")
    suspend fun clearManualStepsForDay(startOfDay: Long, endOfDay: Long)

    // Routines
    @Query("SELECT * FROM routines ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getStepsForRoutine(routineId: Int): Flow<List<RoutineStep>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineStep(step: RoutineStep)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun clearStepsForRoutine(routineId: Int)

    @androidx.room.Transaction
    suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        val routineId = insertRoutine(routine).toInt()
        clearStepsForRoutine(routineId)
        steps.forEachIndexed { index, step ->
            insertRoutineStep(step.copy(id = 0, routineId = routineId, orderIndex = index))
        }
    }

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    // Debt Queries
    @Query("SELECT * FROM debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    // Nutrition & Meal Records
    @Query("SELECT * FROM meal_records ORDER BY timestamp DESC")
    fun getAllMealRecords(): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY timestamp ASC")
    fun getMealRecordsForDate(date: String): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealRecord(meal: MealRecord): Long

    @Update
    suspend fun updateMealRecord(meal: MealRecord)

    @Delete
    suspend fun deleteMealRecord(meal: MealRecord)

    @Query("DELETE FROM meal_records WHERE id = :id")
    suspend fun deleteMealRecordById(id: Int)

    // Water Intake Records
    @Query("SELECT * FROM water_records ORDER BY timestamp DESC")
    fun getAllWaterRecords(): Flow<List<WaterRecord>>

    @Query("SELECT * FROM water_records WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterRecordsForDate(date: String): Flow<List<WaterRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterRecord(record: WaterRecord): Long

    @Delete
    suspend fun deleteWaterRecord(record: WaterRecord)

    @Query("DELETE FROM water_records WHERE id = :id")
    suspend fun deleteWaterRecordById(id: Int)

    // Activity Records (Cardio & Treino por Grupo Muscular)
    @Query("SELECT * FROM activity_records ORDER BY timestamp DESC")
    fun getAllActivityRecords(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE date = :date ORDER BY timestamp DESC")
    fun getActivityRecordsForDate(date: String): Flow<List<ActivityRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityRecord(record: ActivityRecord): Long

    @Delete
    suspend fun deleteActivityRecord(record: ActivityRecord)

    @Query("DELETE FROM activity_records WHERE id = :id")
    suspend fun deleteActivityRecordById(id: Int)

    // Persistência segura não-destrutiva de sono e passos
    @Query("SELECT * FROM sleep_records WHERE startTime = :startTime AND endTime = :endTime LIMIT 1")
    suspend fun findSleepRecord(startTime: Long, endTime: Long): SleepRecord?

    @Query("SELECT * FROM steps_records WHERE startTime >= :startOfDay AND endTime <= :endOfDay AND source = :source LIMIT 1")
    suspend fun findStepsRecordForDayAndSource(startOfDay: Long, endOfDay: Long, source: String): StepsRecord?

    @Update
    suspend fun updateStepsRecord(record: StepsRecord)

    @Update
    suspend fun updateSleepRecord(record: SleepRecord)

    // Bible Verse Videos
    @Query("SELECT * FROM bible_verse_videos WHERE bookAbbrev = :bookAbbrev AND chapter = :chapter ORDER BY verseNumber ASC, createdAt ASC")
    fun getVerseVideosForChapter(bookAbbrev: String, chapter: Int): Flow<List<BibleVerseVideo>>

    @Query("SELECT * FROM bible_verse_videos ORDER BY createdAt DESC")
    fun getAllVerseVideos(): Flow<List<BibleVerseVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerseVideo(video: BibleVerseVideo): Long

    @Delete
    suspend fun deleteVerseVideo(video: BibleVerseVideo)

    @Query("DELETE FROM bible_verse_videos WHERE id = :id")
    suspend fun deleteVerseVideoById(id: Int)

    // Bible Reading Sessions (Perseverança)
    @Query("SELECT DISTINCT date FROM bible_reading_sessions ORDER BY date DESC")
    fun getDistinctReadingDates(): Flow<List<String>>

    @Query("SELECT * FROM bible_reading_sessions ORDER BY timestamp DESC")
    fun getAllReadingSessions(): Flow<List<BibleReadingSession>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadingSession(session: BibleReadingSession): Long
}
