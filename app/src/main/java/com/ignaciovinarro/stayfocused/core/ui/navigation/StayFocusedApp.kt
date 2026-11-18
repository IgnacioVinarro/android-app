package com.ignaciovinarro.stayfocused.core.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ignaciovinarro.stayfocused.core.settings.ui.SettingsScreen
import com.ignaciovinarro.stayfocused.finance.ui.AccountEditorScreen
import com.ignaciovinarro.stayfocused.finance.ui.FinanceScreen
import com.ignaciovinarro.stayfocused.finance.ui.TransactionEditorScreen
import com.ignaciovinarro.stayfocused.meditation.ui.MeditationScreen
import com.ignaciovinarro.stayfocused.planner.ui.CreateTaskScreen
import com.ignaciovinarro.stayfocused.planner.ui.RoutineEditorScreen
import com.ignaciovinarro.stayfocused.planner.ui.RoutinesScreen
import com.ignaciovinarro.stayfocused.planner.ui.TodayScreen
import com.ignaciovinarro.stayfocused.screentime.ui.ScreenTimeScreen
import java.time.LocalDate

private const val ROUTE_CREATE_TASK = "planner/task/{taskId}?date={date}&newDate={newDate}&start={start}&gap={gap}"
private const val ROUTE_ROUTINES = "planner/routines"
private const val ROUTE_ROUTINE_EDITOR = "planner/routines/editor/{routineId}"
private const val ROUTE_ACCOUNT_EDITOR = "finance/accounts/editor/{accountId}"
private const val ARG_ACCOUNT_ID = "accountId"
private const val ROUTE_TRANSACTION_EDITOR = "finance/transactions/editor/{transactionId}"
private const val ARG_TRANSACTION_ID = "transactionId"
private const val ARG_TASK_ID = "taskId"
private const val ARG_OCCURRENCE_DATE = "date"
private const val ARG_NEW_DATE = "newDate"
private const val ARG_START = "start"
private const val ARG_GAP = "gap"
private const val ARG_ROUTINE_ID = "routineId"
private const val SIN_FECHA_DE_OCURRENCIA = -1L

// Tarea nueva en `date` (epochDay del día que se está viendo). Si viene de un hueco libre (PL-03), también
// su hora de inicio y su duración; sin hueco, esos dos quedan en -1 (sin dato).
private fun createTaskRoute(date: Long, startMinutes: Int = SIN_FECHA_DE_OCURRENCIA.toInt(), gapMinutes: Int = SIN_FECHA_DE_OCURRENCIA.toInt()): String =
    "planner/task/0?date=$SIN_FECHA_DE_OCURRENCIA&newDate=$date&start=$startMinutes&gap=$gapMinutes"

// `occurrenceDate` es la fecha (epochDay) del bloque tocado en la línea de tiempo: para una
// tarea recurrente es la ocurrencia puntual que se va a editar/borrar (PL-15).
private fun editTaskRoute(taskId: Long, occurrenceDate: Long): String = "planner/task/$taskId?date=$occurrenceDate"

private fun routineEditorRoute(routineId: Long): String = "planner/routines/editor/$routineId"

// 0 = cuenta nueva.
private fun accountEditorRoute(accountId: Long): String = "finance/accounts/editor/$accountId"

// 0 = movimiento nuevo.
private fun transactionEditorRoute(transactionId: Long): String = "finance/transactions/editor/$transactionId"

@Composable
fun StayFocusedApp(launchRequest: AppLaunchRequest? = null, onLaunchRequestHandled: () -> Unit = {}) {
    val navController = rememberNavController()

    // Pedido de arranque (widget, PL-16): se atiende y se avisa que ya está hecho, para no repetirlo.
    LaunchedEffect(launchRequest) {
        when (launchRequest) {
            AppLaunchRequest.NEW_TASK -> navController.navigate(createTaskRoute(LocalDate.now().toEpochDay()))
            null -> return@LaunchedEffect
        }
        onLaunchRequestHandled()
    }

    Scaffold(
        bottomBar = { StayFocusedBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StayFocusedDestination.TODAY.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(StayFocusedDestination.TODAY.route) {
                TodayScreen(
                    onAddTask = { date -> navController.navigate(createTaskRoute(date)) },
                    onCreateTaskInGap = { date, start, gap -> navController.navigate(createTaskRoute(date, start, gap)) },
                    onOpenRoutines = { navController.navigate(ROUTE_ROUTINES) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onEditTask = { taskId, occurrenceDate ->
                        navController.navigate(editTaskRoute(taskId, occurrenceDate))
                    }
                )
            }
            composable(StayFocusedDestination.FINANCE.route) {
                FinanceScreen(
                    onAddAccount = { navController.navigate(accountEditorRoute(0L)) },
                    onEditAccount = { accountId -> navController.navigate(accountEditorRoute(accountId)) },
                    onAddTransaction = { navController.navigate(transactionEditorRoute(0L)) },
                    onEditTransaction = { id -> navController.navigate(transactionEditorRoute(id)) }
                )
            }
            composable(StayFocusedDestination.GYM.route) { GymDietScreen() }
            composable(StayFocusedDestination.MEDITATION.route) { MeditationScreen() }
            composable(StayFocusedDestination.SCREEN_TIME.route) { ScreenTimeScreen() }
            // Destino secundario: se abre desde "Hoy" y se vuelve con la flecha.
            composable(ROUTE_SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(
                ROUTE_CREATE_TASK,
                arguments = listOf(
                    navArgument(ARG_TASK_ID) { type = NavType.LongType },
                    navArgument(ARG_OCCURRENCE_DATE) { type = NavType.LongType; defaultValue = SIN_FECHA_DE_OCURRENCIA },
                    navArgument(ARG_NEW_DATE) { type = NavType.LongType; defaultValue = SIN_FECHA_DE_OCURRENCIA },
                    navArgument(ARG_START) { type = NavType.LongType; defaultValue = SIN_FECHA_DE_OCURRENCIA },
                    navArgument(ARG_GAP) { type = NavType.LongType; defaultValue = SIN_FECHA_DE_OCURRENCIA }
                )
            ) {
                CreateTaskScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                ROUTE_ACCOUNT_EDITOR,
                arguments = listOf(navArgument(ARG_ACCOUNT_ID) { type = NavType.LongType })
            ) {
                AccountEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(
                ROUTE_TRANSACTION_EDITOR,
                arguments = listOf(navArgument(ARG_TRANSACTION_ID) { type = NavType.LongType })
            ) {
                TransactionEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(ROUTE_ROUTINES) {
                RoutinesScreen(
                    onBack = { navController.popBackStack() },
                    onCreateRoutine = { navController.navigate(routineEditorRoute(0L)) },
                    onEditRoutine = { routineId -> navController.navigate(routineEditorRoute(routineId)) }
                )
            }
            composable(
                ROUTE_ROUTINE_EDITOR,
                arguments = listOf(navArgument(ARG_ROUTINE_ID) { type = NavType.LongType })
            ) {
                RoutineEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun StayFocusedBottomBar(navController: androidx.navigation.NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        StayFocusedDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.contentDescriptionRes)
                    )
                },
                label = { Text(label) }
            )
        }
    }
}
