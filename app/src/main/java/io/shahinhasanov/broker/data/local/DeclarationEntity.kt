package io.shahinhasanov.broker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationStatus
import java.time.Instant

@Entity(tableName = "declarations")
data class DeclarationEntity(
    @PrimaryKey val id: String,
    val reference: String,
    @ColumnInfo(name = "declarant_name") val declarantName: String,
    val operator: String,
    val status: DeclarationStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "lines_json") val linesJson: String,
    @ColumnInfo(name = "attachments_json") val attachmentsJson: String,
    @ColumnInfo(name = "history_json") val historyJson: String
) {
    fun toDomain(codec: JsonCodec): Declaration = Declaration(
        id = id,
        reference = reference,
        declarantName = declarantName,
        operator = operator,
        status = status,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        lines = codec.decodeLines(linesJson),
        attachments = codec.decodeAttachments(attachmentsJson),
        history = codec.decodeHistory(historyJson)
    )

    companion object {
        fun fromDomain(model: Declaration, codec: JsonCodec): DeclarationEntity = DeclarationEntity(
            id = model.id,
            reference = model.reference,
            declarantName = model.declarantName,
            operator = model.operator,
            status = model.status,
            createdAt = model.createdAt.toEpochMilli(),
            updatedAt = model.updatedAt.toEpochMilli(),
            linesJson = codec.encodeLines(model.lines),
            attachmentsJson = codec.encodeAttachments(model.attachments),
            historyJson = codec.encodeHistory(model.history)
        )
    }
}
