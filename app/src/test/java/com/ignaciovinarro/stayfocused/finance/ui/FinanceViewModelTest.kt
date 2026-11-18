package com.ignaciovinarro.stayfocused.finance.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TagEntity
import com.ignaciovinarro.stayfocused.finance.data.TagRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionTagEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {

    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun cuenta(id: Long, initial: Long) =
        AccountEntity(id = id, name = "Cuenta $id", type = "BANK", initialBalance = initial, iconKey = "i", colorKey = "blue")

    private fun tx(type: String, amount: Long, accountId: Long, transferAccountId: Long? = null) =
        TransactionEntity(
            type = type, amount = amount, date = 19000, accountId = accountId,
            categoryId = if (type == "TRANSFER") null else 1L, transferAccountId = transferAccountId
        )

    private fun viewModel(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = emptyList(),
        tags: List<TagEntity> = emptyList(),
        links: List<TransactionTagEntity> = emptyList()
    ): FinanceViewModel {
        every { accountRepository.observeAll() } returns flowOf(accounts)
        every { transactionRepository.observeAll() } returns flowOf(transactions)
        every { categoryRepository.observeAll() } returns flowOf(categories)
        every { tagRepository.observeAll() } returns flowOf(tags)
        every { tagRepository.observeLinks() } returns flowOf(links)
        return FinanceViewModel(accountRepository, transactionRepository, categoryRepository, tagRepository)
    }

    @Test
    fun `cada movimiento muestra los nombres de sus etiquetas ordenados`() = runTest(dispatcher) {
        val vm = viewModel(
            accounts = listOf(cuenta(1, 0)),
            transactions = listOf(tx("EXPENSE", 100, accountId = 1).copy(id = 7), tx("EXPENSE", 200, accountId = 1).copy(id = 8)),
            tags = listOf(TagEntity(id = 1, name = "viaje"), TagEntity(id = 2, name = "auto")),
            links = listOf(
                TransactionTagEntity(transactionId = 7, tagId = 1),
                TransactionTagEntity(transactionId = 7, tagId = 2)
            )
        )

        vm.uiState.test {
            awaitItem() // estado inicial (cargando)
            val rows = awaitItem().transactions.associateBy { it.transaction.id }
            assertEquals(listOf("auto", "viaje"), rows.getValue(7).tagNames)
            assertEquals(emptyList<String>(), rows.getValue(8).tagNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `arranca cargando y luego muestra las cuentas con su saldo y el total`() = runTest(dispatcher) {
        val vm = viewModel(
            accounts = listOf(cuenta(1, 100_000), cuenta(2, 50_000)),
            transactions = listOf(tx("EXPENSE", 20_000, accountId = 1), tx("INCOME", 5_000, accountId = 2))
        )

        vm.uiState.test {
            assertTrue(awaitItem().isLoading)
            val estado = awaitItem()
            assertFalse(estado.isLoading)
            assertEquals(listOf(80_000L, 55_000L), estado.accounts.map { it.balance })
            assertEquals(135_000L, estado.totalBalance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `una transferencia entre cuentas no cambia el saldo total`() = runTest(dispatcher) {
        val vm = viewModel(
            accounts = listOf(cuenta(1, 100_000), cuenta(2, 0)),
            transactions = listOf(tx("TRANSFER", 30_000, accountId = 1, transferAccountId = 2))
        )

        vm.uiState.test {
            awaitItem()
            val estado = awaitItem()
            assertEquals(listOf(70_000L, 30_000L), estado.accounts.map { it.balance })
            assertEquals(100_000L, estado.totalBalance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sin cuentas no esta cargando y el total es cero`() = runTest(dispatcher) {
        val vm = viewModel(accounts = emptyList(), transactions = emptyList())

        vm.uiState.test {
            awaitItem()
            val estado = awaitItem()
            assertFalse(estado.isLoading)
            assertTrue(estado.accounts.isEmpty())
            assertEquals(0L, estado.totalBalance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `las filas de movimientos traen los nombres de categoria y cuentas`() = runTest(dispatcher) {
        val comida = CategoryEntity(id = 1, name = "Comida", type = "EXPENSE", iconKey = "i", colorKey = "blue")
        val vm = viewModel(
            accounts = listOf(cuenta(1, 0), cuenta(2, 0)),
            transactions = listOf(
                tx("EXPENSE", 1_000, accountId = 1),
                tx("TRANSFER", 500, accountId = 1, transferAccountId = 2)
            ),
            categories = listOf(comida)
        )

        vm.uiState.test {
            awaitItem()
            val filas = awaitItem().transactions
            assertEquals(2, filas.size)
            assertEquals(TransactionType.EXPENSE, filas[0].type)
            assertEquals("Comida", filas[0].categoryName)
            assertEquals("Cuenta 1", filas[0].accountName)
            assertNull(filas[0].transferAccountName)
            assertEquals(TransactionType.TRANSFER, filas[1].type)
            assertNull(filas[1].categoryName)
            assertEquals("Cuenta 2", filas[1].transferAccountName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
