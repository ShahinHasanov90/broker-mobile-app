package io.shahinhasanov.broker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE state = 'PENDING' ORDER BY created_at ASC")
    suspend fun pending(): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox WHERE state IN ('PENDING','IN_FLIGHT')")
    suspend fun pendingCount(): Int

    @Query("UPDATE outbox SET state = :state, attempts = attempts + 1, last_error = :error WHERE idempotencyKey = :key")
    suspend fun markFailed(key: String, state: OutboxState, error: String?)

    @Query("UPDATE outbox SET state = :state WHERE idempotencyKey = :key")
    suspend fun markState(key: String, state: OutboxState)

    @Query("DELETE FROM outbox WHERE idempotencyKey = :key")
    suspend fun delete(key: String)
}
