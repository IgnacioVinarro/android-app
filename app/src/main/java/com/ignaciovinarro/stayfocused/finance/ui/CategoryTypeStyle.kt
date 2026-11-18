package com.ignaciovinarro.stayfocused.finance.ui

import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType

fun categoryTypeLabel(type: CategoryType): Int = when (type) {
    CategoryType.EXPENSE -> R.string.categories_type_expense
    CategoryType.INCOME -> R.string.categories_type_income
}
