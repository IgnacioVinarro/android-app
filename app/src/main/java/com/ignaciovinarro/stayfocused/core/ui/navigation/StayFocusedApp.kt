package com.ignaciovinarro.stayfocused.core.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ignaciovinarro.stayfocused.finance.ui.FinanceScreen
import com.ignaciovinarro.stayfocused.gym.ui.GymScreen
import com.ignaciovinarro.stayfocused.meditation.ui.MeditationScreen
import com.ignaciovinarro.stayfocused.nutrition.ui.NutritionScreen
import com.ignaciovinarro.stayfocused.planner.ui.CreateTaskScreen
import com.ignaciovinarro.stayfocused.planner.ui.TodayScreen

private const val ROUTE_CREATE_TASK = "planner/task/new"

@Composable
fun StayFocusedApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { StayFocusedBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StayFocusedDestination.TODAY.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(StayFocusedDestination.TODAY.route) {
                TodayScreen(onAddTask = { navController.navigate(ROUTE_CREATE_TASK) })
            }
            composable(StayFocusedDestination.FINANCE.route) { FinanceScreen() }
            composable(StayFocusedDestination.GYM.route) { GymScreen() }
            composable(StayFocusedDestination.NUTRITION.route) { NutritionScreen() }
            composable(StayFocusedDestination.MEDITATION.route) { MeditationScreen() }
            composable(ROUTE_CREATE_TASK) {
                CreateTaskScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
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
