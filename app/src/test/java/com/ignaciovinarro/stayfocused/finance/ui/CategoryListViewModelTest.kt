package com.ignaciovinarro.stayfocused.finance.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    private val repository = mockk<CategoryRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categoria(id: Long, type: String) =
        CategoryEntity(id = id, name = "Cat $id", type = type, iconKey = "i", colorKey = "blue")

    @Test
    fun `muestra primero los gastos y al cambiar de pestana los ingresos`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(categoria(1, "EXPENSE"), categoria(2, "INCOME"), categoria(3, "EXPENSE"))
        )
        val vm = CategoryListViewModel(repository)

        vm.uiState.test {
            var state = awaitItem()
            while (state.categories.isEmpty()) state = awaitItem()
            assertEquals(CategoryType.EXPENSE, state.selectedType)
            assertEquals(listOf(1L, 3L), state.categories.map { it.id })

            vm.onTypeChange(CategoryType.INCOME)
            state = awaitItem()
            assertEquals(CategoryType.INCOME, state.selectedType)
            assertEquals(listOf(2L), state.categories.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
