package io.shahinhasanov.broker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxOperation {
    APPROVE, REJECT
}

enum class OutboxState {
    PENDING, IN_FLIGHT, FAILED
}

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val idempotencyKey: String,
    @ColumnInfo(name = "declaration_id") val declarationId: String,
    val operation: OutboxOperation,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val state: OutboxState,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null
)
