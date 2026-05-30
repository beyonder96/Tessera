package com.tesserahub.app.data.local.dao

import androidx.room.*
import com.tesserahub.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TesseraDao {
    // Finanças
    @Query("SELECT * FROM finances ORDER BY timestamp DESC")
    fun getAllFinances(): Flow<List<FinanceEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(finance: FinanceEntity)

    // Mercado
    @Query("SELECT * FROM market_items")
    fun getAllMarketItems(): Flow<List<MarketItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItem(item: MarketItemEntity)

    // Pets
    @Query("SELECT * FROM pet_routines ORDER BY time ASC")
    fun getPetRoutines(): Flow<List<PetRoutineEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetRoutine(routine: PetRoutineEntity)
    
    // Você pode adicionar os inserts/selects de Saúde e Metas aqui seguindo o mesmo padrão
}
