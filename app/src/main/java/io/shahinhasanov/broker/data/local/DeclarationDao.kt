package io.shahinhasanov.broker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.shahinhasanov.broker.data.model.DeclarationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeclarationDao {

    @Query("SELECT * FROM declarations ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<DeclarationEntity>>

    @Query("SELECT * FROM declarations WHERE status = :status ORDER BY updated_at DESC")
    fun observeByStatus(status: DeclarationStatus): Flow<List<DeclarationEntity>>

    @Query("SELECT * FROM declarations WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DeclarationEntity?>

    @Query("SELECT * FROM declarations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DeclarationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeclarationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DeclarationEntity>)

    @Transaction
    @Query("UPDATE declarations SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: DeclarationStatus, updatedAt: Long)

    @Query("DELETE FROM declarations WHERE id = :id")
    suspend fun deleteById(id: String)
}
