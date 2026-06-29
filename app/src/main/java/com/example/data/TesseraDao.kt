package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TesseraDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM market_items WHERE isBought = 0 ORDER BY orderIndex ASC")
    fun getPendingMarketItems(): Flow<List<MarketItem>>

    @Query("SELECT * FROM market_items WHERE isBought = 1 ORDER BY id DESC")
    fun getBoughtMarketItems(): Flow<List<MarketItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItem(item: MarketItem)

    @Update
    suspend fun updateMarketItem(item: MarketItem)

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

    // Finance clear operations
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM bank_accounts")
    suspend fun clearAllBankAccounts()

    @Query("DELETE FROM credit_cards")
    suspend fun clearAllCreditCards()

    @androidx.room.Transaction
    suspend fun clearAllFinances() {
        clearAllTransactions()
        clearAllBankAccounts()
        clearAllCreditCards()
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
}
