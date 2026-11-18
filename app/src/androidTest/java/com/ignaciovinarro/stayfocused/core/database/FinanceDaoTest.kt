package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.finance.data.AccountDao
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.BudgetDao
import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryDao
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.TagDao
import com.ignaciovinarro.stayfocused.finance.data.TagEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionDao
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionTagDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var tagDao: TagDao
    private lateinit var linkDao: TransactionTagDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        accountDao = database.accountDao()
        categoryDao = database.categoryDao()
        transactionDao = database.transactionDao()
        budgetDao = database.budgetDao()
        tagDao = database.tagDao()
        linkDao = database.transactionTagDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun cuenta(name: String = "Efectivo") =
        AccountEntity(name = name, type = "CASH", iconKey = "i", colorKey = "green")

    private fun categoria(name: String, type: String = "EXPENSE") =
        CategoryEntity(name = name, type = type, iconKey = "i", colorKey = "blue")

    private fun gasto(accountId: Long, categoryId: Long?, date: Long = 19000, amount: Long = 1_000) =
        TransactionEntity(type = "EXPENSE", amount = amount, date = date, accountId = accountId, categoryId = categoryId)

    // --- cuentas ---

    @Test
    fun cuenta_insertarYObtener() = runBlocking {
        val id = accountDao.insert(cuenta("Banco"))

        assertThat(accountDao.getById(id)?.name).isEqualTo("Banco")
        assertThat(accountDao.observeAll().first().map { it.name }).containsExactly("Banco")
    }

    @Test
    fun cuenta_countTransactionsCuentaComoOrigenYComoDestinoEIgnoraBorradas() = runBlocking {
        val a = accountDao.insert(cuenta("A"))
        val b = accountDao.insert(cuenta("B"))
        val c = accountDao.insert(cuenta("C"))
        transactionDao.insert(gasto(a, null))
        transactionDao.insert(
            TransactionEntity(type = "TRANSFER", amount = 100, date = 19000, accountId = a, transferAccountId = b)
        )
        val borrada = transactionDao.insert(gasto(b, null))
        transactionDao.softDelete(borrada, System.currentTimeMillis())

        assertThat(accountDao.countTransactions(a)).isEqualTo(2)
        assertThat(accountDao.countTransactions(b)).isEqualTo(1) // solo como destino; la borrada no cuenta
        assertThat(accountDao.countTransactions(c)).isEqualTo(0)
    }

    @Test
    fun cuenta_softDeleteLaSacaDeLaLista() = runBlocking {
        val id = accountDao.insert(cuenta("Banco"))

        accountDao.softDelete(id, System.currentTimeMillis())

        assertThat(accountDao.getById(id)).isNull()
        assertThat(accountDao.observeAll().first()).isEmpty()
    }

    // --- categorías ---

    @Test
    fun categoria_borrarReasignandoMueveTransaccionesYBorraPresupuestos() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val origen = categoryDao.insert(categoria("Comida"))
        val destino = categoryDao.insert(categoria("Otros"))
        val txId = transactionDao.insert(gasto(cuentaId, origen))
        budgetDao.insert(BudgetEntity(categoryId = origen, effectiveFrom = 202611, amount = 5_000))

        categoryDao.deleteReassigning(origen, destino, System.currentTimeMillis())

        assertThat(categoryDao.getById(origen)).isNull()
        assertThat(transactionDao.getById(txId)?.categoryId).isEqualTo(destino)
        assertThat(budgetDao.observeAll().first()).isEmpty()
    }

    @Test
    fun categoria_contarTransaccionesIgnoraLasBorradas() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val cat = categoryDao.insert(categoria("Comida"))
        transactionDao.insert(gasto(cuentaId, cat))
        val borrada = transactionDao.insert(gasto(cuentaId, cat))
        transactionDao.softDelete(borrada, System.currentTimeMillis())

        assertThat(categoryDao.countTransactions(cat)).isEqualTo(1)
    }

    @Test
    fun categoria_countAllRowsIncluyeLasBorradas() = runBlocking {
        val id = categoryDao.insert(categoria("Comida"))
        categoryDao.deleteReassigning(id, null, System.currentTimeMillis())

        assertThat(categoryDao.countAllRows()).isEqualTo(1)
        assertThat(categoryDao.observeAll().first()).isEmpty()
    }

    // --- transacciones ---

    @Test
    fun transaccion_observeBetweenFiltraPorRangoYExcluyeBorradas() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val cat = categoryDao.insert(categoria("Comida"))
        transactionDao.insert(gasto(cuentaId, cat, date = 100))
        transactionDao.insert(gasto(cuentaId, cat, date = 110))
        transactionDao.insert(gasto(cuentaId, cat, date = 200))
        val borrada = transactionDao.insert(gasto(cuentaId, cat, date = 105))
        transactionDao.softDelete(borrada, System.currentTimeMillis())

        val enRango = transactionDao.observeBetween(100, 110).first()

        assertThat(enRango.map { it.date }).containsExactly(110L, 100L).inOrder()
    }

    @Test
    fun transaccion_borrarQuitaTambienSusUnionesConEtiquetas() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val txId = transactionDao.insert(gasto(cuentaId, null))
        val tagId = tagDao.insert(TagEntity(name = "viaje"))
        linkDao.replaceTags(txId, setOf(tagId), System.currentTimeMillis())

        transactionDao.softDelete(txId, System.currentTimeMillis())

        assertThat(linkDao.observeActive().first()).isEmpty()
    }

    // --- presupuestos ---

    @Test
    fun presupuesto_findExactEncuentraTambienElTotalConCategoriaNull() = runBlocking {
        budgetDao.insert(BudgetEntity(categoryId = null, effectiveFrom = 202611, amount = 20_000))
        budgetDao.insert(BudgetEntity(categoryId = 1L, effectiveFrom = 202611, amount = 5_000))

        assertThat(budgetDao.findExact(null, 202611)?.amount).isEqualTo(20_000L)
        assertThat(budgetDao.findExact(1L, 202611)?.amount).isEqualTo(5_000L)
        assertThat(budgetDao.findExact(1L, 202612)).isNull()
    }

    // --- etiquetas ---

    @Test
    fun etiqueta_borrarlaLaQuitaDeLasTransacciones() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val txId = transactionDao.insert(gasto(cuentaId, null))
        val tagId = tagDao.insert(TagEntity(name = "viaje"))
        linkDao.replaceTags(txId, setOf(tagId), System.currentTimeMillis())

        tagDao.softDelete(tagId, System.currentTimeMillis())

        assertThat(tagDao.observeAll().first()).isEmpty()
        assertThat(linkDao.observeActive().first()).isEmpty()
    }

    @Test
    fun etiqueta_replaceTagsAgregaQuitaYReactivaSinChocarConElIndiceUnico() = runBlocking {
        val cuentaId = accountDao.insert(cuenta())
        val txId = transactionDao.insert(gasto(cuentaId, null))
        val a = tagDao.insert(TagEntity(name = "a"))
        val b = tagDao.insert(TagEntity(name = "b"))
        val ahora = System.currentTimeMillis()

        linkDao.replaceTags(txId, setOf(a, b), ahora)
        assertThat(linkDao.observeActive().first().map { it.tagId }).containsExactly(a, b)

        linkDao.replaceTags(txId, setOf(a), ahora)
        assertThat(linkDao.observeActive().first().map { it.tagId }).containsExactly(a)

        // Volver a poner "b" reactiva la fila borrada: no debe fallar por el índice único.
        linkDao.replaceTags(txId, setOf(a, b), ahora)
        assertThat(linkDao.observeActive().first().map { it.tagId }).containsExactly(a, b)
        assertThat(linkDao.getAllForTransaction(txId)).hasSize(2)
    }
}
