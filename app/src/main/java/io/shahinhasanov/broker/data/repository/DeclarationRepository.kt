package io.shahinhasanov.broker.data.repository

import io.shahinhasanov.broker.data.local.DeclarationDao
import io.shahinhasanov.broker.data.local.DeclarationEntity
import io.shahinhasanov.broker.data.local.JsonCodec
import io.shahinhasanov.broker.data.local.OutboxDao
import io.shahinhasanov.broker.data.local.OutboxEntity
import io.shahinhasanov.broker.data.local.OutboxOperation
import io.shahinhasanov.broker.data.local.OutboxState
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationLine
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.data.model.StatusEvent
import io.shahinhasanov.broker.data.remote.ApprovalRequestDto
import io.shahinhasanov.broker.data.remote.BrokerApi
import io.shahinhasanov.broker.data.remote.DeclarationDto
import io.shahinhasanov.broker.data.remote.DeclarationLineDto
import io.shahinhasanov.broker.data.remote.RejectionRequestDto
import io.shahinhasanov.broker.data.remote.StatusEventDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationRepository @Inject constructor(
    private val api: BrokerApi,
    private val declarationDao: DeclarationDao,
    private val outboxDao: OutboxDao,
    private val codec: JsonCodec,
    private val clock: () -> Instant = { Instant.now() }
) {

    fun observeDeclarations(filter: DeclarationStatus? = null): Flow<List<Declaration>> {
        val source = if (filter == null) {
            declarationDao.observeAll()
        } else {
            declarationDao.observeByStatus(filter)
        }
        return source.map { rows -> rows.map { it.toDomain(codec) } }
    }

    fun observeDeclaration(id: String): Flow<Declaration?> =
        declarationDao.observeById(id).map { it?.toDomain(codec) }

    suspend fun refresh(): Result<Int> = runCatching {
        val dtos = api.listDeclarations()
        val entities = dtos.map { it.toEntity() }
        declarationDao.upsertAll(entities)
        entities.size
    }

    suspend fun refreshOne(id: String): Result<Unit> = runCatching {
        val dto = api.declaration(id)
        declarationDao.upsert(dto.toEntity())
    }

    suspend fun approve(id: String, note: String?): Result<Unit> = runCatching {
        val now = clock()
        val key = newIdempotencyKey()
        declarationDao.updateStatus(id, DeclarationStatus.SUBMITTED, now.toEpochMilli())
        outboxDao.enqueue(
            OutboxEntity(
                idempotencyKey = key,
                declarationId = id,
                operation = OutboxOperation.APPROVE,
                payloadJson = codec.encodeAttachments(listOfNotNull(note)),
                state = OutboxState.PENDING,
                createdAt = now.toEpochMilli()
            )
        )
    }

    suspend fun reject(id: String, reason: String): Result<Unit> = runCatching {
        require(reason.isNotBlank()) { "rejection reason must be provided" }
        val now = clock()
        val key = newIdempotencyKey()
        declarationDao.updateStatus(id, DeclarationStatus.REJECTED, now.toEpochMilli())
        outboxDao.enqueue(
            OutboxEntity(
                idempotencyKey = key,
                declarationId = id,
                operation = OutboxOperation.REJECT,
                payloadJson = codec.encodeAttachments(listOf(reason)),
                state = OutboxState.PENDING,
                createdAt = now.toEpochMilli()
            )
        )
    }

    suspend fun drainOutbox(): DrainResult {
        val pending = outboxDao.pending()
        var delivered = 0
        var failed = 0
        for (row in pending) {
            outboxDao.markState(row.idempotencyKey, OutboxState.IN_FLIGHT)
            val payload = codec.decodeAttachments(row.payloadJson)
            val outcome = runCatching {
                when (row.operation) {
                    OutboxOperation.APPROVE -> api.approve(
                        row.declarationId,
                        ApprovalRequestDto(row.idempotencyKey, payload.firstOrNull())
                    )
                    OutboxOperation.REJECT -> api.reject(
                        row.declarationId,
                        RejectionRequestDto(row.idempotencyKey, payload.first())
                    )
                }
            }
            outcome.fold(
                onSuccess = { dto ->
                    declarationDao.upsert(dto.toEntity())
                    outboxDao.delete(row.idempotencyKey)
                    delivered++
                },
                onFailure = { error ->
                    outboxDao.markFailed(row.idempotencyKey, OutboxState.FAILED, error.message)
                    failed++
                }
            )
        }
        return DrainResult(delivered = delivered, failed = failed)
    }

    private fun DeclarationDto.toEntity(): DeclarationEntity = DeclarationEntity(
        id = id,
        reference = reference,
        declarantName = declarantName,
        operator = operator,
        status = DeclarationStatus.valueOf(status),
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
        linesJson = codec.encodeLines(lines.map { it.toDomain() }),
        attachmentsJson = codec.encodeAttachments(attachments),
        historyJson = codec.encodeHistory(history.map { it.toDomain() })
    )

    private fun DeclarationLineDto.toDomain(): DeclarationLine = DeclarationLine(
        lineNumber = lineNumber,
        hsCode = hsCode,
        description = description,
        originCountry = originCountry,
        quantity = BigDecimal(quantity),
        unit = unit,
        customsValue = BigDecimal(customsValue),
        currency = currency
    )

    private fun StatusEventDto.toDomain(): StatusEvent = StatusEvent(
        status = DeclarationStatus.valueOf(status),
        occurredAt = Instant.parse(occurredAt),
        actor = actor,
        note = note
    )

    private fun newIdempotencyKey(): String = UUID.randomUUID().toString()

    data class DrainResult(val delivered: Int, val failed: Int)
}
