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
    @Query("SELECT * FROM purchase_goals ORDER BY deadlineTimestamp ASC")
    fun getAllPurchaseGoals(): Flow<List<PurchaseGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseGoal(goal: PurchaseGoal)

    @Update
    suspend fun updatePurchaseGoal(goal: PurchaseGoal)

    @Delete
    suspend fun deletePurchaseGoal(goal: PurchaseGoal)
}
