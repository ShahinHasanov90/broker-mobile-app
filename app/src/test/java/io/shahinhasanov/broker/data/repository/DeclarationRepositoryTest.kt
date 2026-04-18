package io.shahinhasanov.broker.data.repository

import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.shahinhasanov.broker.data.local.DeclarationDao
import io.shahinhasanov.broker.data.local.JsonCodec
import io.shahinhasanov.broker.data.local.OutboxDao
import io.shahinhasanov.broker.data.local.OutboxEntity
import io.shahinhasanov.broker.data.local.OutboxOperation
import io.shahinhasanov.broker.data.local.OutboxState
import io.shahinhasanov.broker.data.model.DeclarationLine
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.data.model.StatusEvent
import io.shahinhasanov.broker.data.remote.ApprovalRequestDto
import io.shahinhasanov.broker.data.remote.BrokerApi
import io.shahinhasanov.broker.data.remote.DeclarationDto
import io.shahinhasanov.broker.data.remote.RejectionRequestDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DeclarationRepositoryTest {

    private val api: BrokerApi = mockk()
    private val declarationDao: DeclarationDao = mockk()
    private val outboxDao: OutboxDao = mockk()
    private val codec: JsonCodec = FakeCodec
    private val frozen = Instant.parse("2024-05-01T10:00:00Z")

    private fun repo() = DeclarationRepository(
        api = api,
        declarationDao = declarationDao,
        outboxDao = outboxDao,
        codec = codec,
        clock = { frozen }
    )

    @Test
    fun `approve writes outbox and updates status`() = runTest {
        coEvery { declarationDao.updateStatus(any(), any(), any()) } just Runs
        val slot = slot<OutboxEntity>()
        coEvery { outboxDao.enqueue(capture(slot)) } just Runs

        val result = repo().approve("D1", note = "ok to lodge")

        assertThat(result.isSuccess).isTrue()
        assertThat(slot.captured.declarationId).isEqualTo("D1")
        assertThat(slot.captured.operation).isEqualTo(OutboxOperation.APPROVE)
        assertThat(slot.captured.state).isEqualTo(OutboxState.PENDING)
        coVerify {
            declarationDao.updateStatus("D1", DeclarationStatus.SUBMITTED, frozen.toEpochMilli())
        }
    }

    @Test
    fun `reject requires non-blank reason`() = runTest {
        val outcome = repo().reject("D2", reason = "   ")

        assertThat(outcome.isFailure).isTrue()
        coVerify(exactly = 0) { declarationDao.updateStatus(any(), any(), any()) }
        coVerify(exactly = 0) { outboxDao.enqueue(any()) }
    }

    @Test
    fun `reject persists reason and marks status`() = runTest {
        coEvery { declarationDao.updateStatus(any(), any(), any()) } just Runs
        val slot = slot<OutboxEntity>()
        coEvery { outboxDao.enqueue(capture(slot)) } just Runs

        val outcome = repo().reject("D3", reason = "value mismatch")

        assertThat(outcome.isSuccess).isTrue()
        assertThat(slot.captured.operation).isEqualTo(OutboxOperation.REJECT)
        assertThat(codec.decodeAttachments(slot.captured.payloadJson)).containsExactly("value mismatch")
    }

    @Test
    fun `drainOutbox delivers pending and removes successful rows`() = runTest {
        val pending = OutboxEntity(
            idempotencyKey = "K1",
            declarationId = "D4",
            operation = OutboxOperation.APPROVE,
            payloadJson = codec.encodeAttachments(emptyList()),
            state = OutboxState.PENDING,
            createdAt = frozen.toEpochMilli()
        )
        coEvery { outboxDao.pending() } returns listOf(pending)
        coEvery { outboxDao.markState("K1", OutboxState.IN_FLIGHT) } just Runs
        coEvery { api.approve("D4", any()) } returns sampleDto("D4", "SUBMITTED")
        coEvery { declarationDao.upsert(any()) } just Runs
        coEvery { outboxDao.delete("K1") } just Runs

        val result = repo().drainOutbox()

        assertThat(result.delivered).isEqualTo(1)
        assertThat(result.failed).isEqualTo(0)
        coVerify { api.approve("D4", match<ApprovalRequestDto> { it.idempotencyKey == "K1" }) }
        coVerify { outboxDao.delete("K1") }
    }

    @Test
    fun `drainOutbox marks failed rows on network error`() = runTest {
        val pending = OutboxEntity(
            idempotencyKey = "K2",
            declarationId = "D5",
            operation = OutboxOperation.REJECT,
            payloadJson = codec.encodeAttachments(listOf("bad docs")),
            state = OutboxState.PENDING,
            createdAt = frozen.toEpochMilli()
        )
        coEvery { outboxDao.pending() } returns listOf(pending)
        coEvery { outboxDao.markState("K2", OutboxState.IN_FLIGHT) } just Runs
        coEvery { api.reject("D5", any<RejectionRequestDto>()) } throws RuntimeException("boom")
        coEvery { outboxDao.markFailed("K2", OutboxState.FAILED, any()) } just Runs

        val result = repo().drainOutbox()

        assertThat(result.delivered).isEqualTo(0)
        assertThat(result.failed).isEqualTo(1)
        coVerify { outboxDao.markFailed("K2", OutboxState.FAILED, "boom") }
    }

    private fun sampleDto(id: String, status: String) = DeclarationDto(
        id = id,
        reference = "REF-$id",
        declarantName = "Declarant",
        operator = "Op",
        status = status,
        createdAt = frozen.toString(),
        updatedAt = frozen.toString(),
        lines = emptyList(),
        attachments = emptyList(),
        history = emptyList()
    )

    private object FakeCodec : JsonCodec {
        override fun encodeLines(value: List<DeclarationLine>) = "lines:${value.size}"
        override fun decodeLines(raw: String) = emptyList<DeclarationLine>()
        override fun encodeAttachments(value: List<String>) = value.joinToString(prefix = "[", postfix = "]", separator = "|")
        override fun decodeAttachments(raw: String): List<String> {
            if (!raw.startsWith("[") || !raw.endsWith("]")) return emptyList()
            val body = raw.substring(1, raw.length - 1)
            return if (body.isEmpty()) emptyList() else body.split("|")
        }
        override fun encodeHistory(value: List<StatusEvent>) = "history:${value.size}"
        override fun decodeHistory(raw: String) = emptyList<StatusEvent>()
    }
}
