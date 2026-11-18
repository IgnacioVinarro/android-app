package com.ignaciovinarro.stayfocused.finance.data

import androidx.annotation.StringRes
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

// Categorías predefinidas (FI-04). Los nombres viven en strings.xml; ícono y color son claves de texto.
// Los `iconKey` "category_*" todavía no tienen dibujo asociado: el mapeo clave→ícono llega con la
// pantalla (ticket 0029), igual que `TaskIconKey` para las tareas.
data class DefaultCategorySpec(
    @StringRes val nameRes: Int,
    val type: CategoryType,
    val iconKey: String,
    val colorKey: String
)

val DEFAULT_CATEGORY_SPECS = listOf(
    DefaultCategorySpec(R.string.finance_category_food, CategoryType.EXPENSE, "category_food", "orange"),
    DefaultCategorySpec(R.string.finance_category_groceries, CategoryType.EXPENSE, "category_groceries", "green"),
    DefaultCategorySpec(R.string.finance_category_transport, CategoryType.EXPENSE, "category_transport", "blue"),
    DefaultCategorySpec(R.string.finance_category_home, CategoryType.EXPENSE, "category_home", "teal"),
    DefaultCategorySpec(R.string.finance_category_health, CategoryType.EXPENSE, "category_health", "red"),
    DefaultCategorySpec(R.string.finance_category_leisure, CategoryType.EXPENSE, "category_leisure", "purple"),
    DefaultCategorySpec(R.string.finance_category_education, CategoryType.EXPENSE, "category_education", "yellow"),
    DefaultCategorySpec(R.string.finance_category_clothing, CategoryType.EXPENSE, "category_clothing", "pink"),
    DefaultCategorySpec(R.string.finance_category_other_expense, CategoryType.EXPENSE, "category_other", "gray"),
    DefaultCategorySpec(R.string.finance_category_salary, CategoryType.INCOME, "category_salary", "green"),
    DefaultCategorySpec(R.string.finance_category_other_income, CategoryType.INCOME, "category_other", "gray")
)

// Arma las filas a sembrar. `getString` se inyecta para poder probarlo sin Android.
fun defaultCategories(getString: (Int) -> String): List<CategoryEntity> =
    DEFAULT_CATEGORY_SPECS.map {
        CategoryEntity(
            name = getString(it.nameRes),
            type = it.type.name,
            iconKey = it.iconKey,
            colorKey = it.colorKey,
            isDefault = true
        )
    }

// Siembra las categorías predefinidas la primera vez que corre la app (la tabla nunca tuvo filas).
@Singleton
class FinanceSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository
) {
    suspend fun seedIfNeeded() {
        categoryRepository.seedDefaults(defaultCategories { context.getString(it) })
    }
}
