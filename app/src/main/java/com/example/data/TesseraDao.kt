package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TesseraDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

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
}
