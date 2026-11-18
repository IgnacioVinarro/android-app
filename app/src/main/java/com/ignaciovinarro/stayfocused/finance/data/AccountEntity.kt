package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Cuenta de dinero (FI-05). El saldo NO se guarda: se calcula con `accountBalance` a partir del saldo
// inicial y las transacciones. `type` es el nombre de `AccountType`; dinero en centavos de ARS.
@Entity(tableName = "account", indices = [Index(value = ["uuid"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val initialBalance: Long = 0,
    val iconKey: String,
    val colorKey: String,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
