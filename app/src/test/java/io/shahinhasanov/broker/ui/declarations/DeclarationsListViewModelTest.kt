package io.shahinhasanov.broker.ui.declarations

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.data.repository.DeclarationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DeclarationsListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: DeclarationRepository = mockk(relaxed = true)
    private val stream = MutableStateFlow<List<Declaration>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.observeDeclarations(null) } returns stream.asStateFlow()
        coEvery { repository.observeDeclarations(DeclarationStatus.DRAFT) } returns stream.asStateFlow()
        coEvery { repository.refresh() } returns Result.success(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observes declarations from repository`() = runTest(dispatcher) {
        val vm = DeclarationsListViewModel(repository)

        vm.uiState.test {
            assertThat(awaitItem().declarations).isEmpty()
            stream.value = listOf(sample("A"))
            val next = awaitItem()
            assertThat(next.declarations).hasSize(1)
            assertThat(next.declarations.first().id).isEqualTo("A")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh surfaces repository error`() = runTest(dispatcher) {
        coEvery { repository.refresh() } returns Result.failure(RuntimeException("no network"))
        val vm = DeclarationsListViewModel(repository)

        vm.uiState.test {
            skipItems(1)
            vm.refresh()
            val failing = awaitItem()
            if (failing.error == null) {
                val final = awaitItem()
                assertThat(final.error).isEqualTo("no network")
            } else {
                assertThat(failing.error).isEqualTo("no network")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter switches source stream`() = runTest(dispatcher) {
        val vm = DeclarationsListViewModel(repository)

        vm.setFilter(DeclarationStatus.DRAFT)

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.filter).isEqualTo(DeclarationStatus.DRAFT)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.observeDeclarations(DeclarationStatus.DRAFT) }
    }

    @Test
    fun `refresh triggered on init`() = runTest(dispatcher) {
        DeclarationsListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.refresh() }
    }

    private fun sample(id: String) = Declaration(
        id = id,
        reference = "REF-$id",
        declarantName = "Declarant",
        operator = "Op",
        status = DeclarationStatus.DRAFT,
        createdAt = Instant.ofEpochMilli(0),
        updatedAt = Instant.ofEpochMilli(1),
        lines = emptyList(),
        attachments = emptyList(),
        history = emptyList()
    )
}
