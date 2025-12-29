package com.example.gerenciadorfinanceiro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gerenciadorfinanceiro.ui.screens.dashboard.DashboardScreen
import com.example.gerenciadorfinanceiro.ui.screens.accounts.AccountsScreen
import com.example.gerenciadorfinanceiro.ui.screens.accounts.AddEditAccountScreen
import com.example.gerenciadorfinanceiro.ui.screens.transactions.TransactionsScreen
import com.example.gerenciadorfinanceiro.ui.screens.transactions.AddEditTransactionScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object AddEditAccount : Screen("accounts/add_edit?accountId={accountId}") {
        fun createRoute(accountId: Long? = null) = "accounts/add_edit?accountId=${accountId ?: -1}"
    }
    object Transactions : Screen("transactions")
    object AddEditTransaction : Screen("transactions/add_edit?transactionId={transactionId}") {
        fun createRoute(transactionId: Long? = null) = "transactions/add_edit?transactionId=${transactionId ?: -1}"
    }

    // Will add more screens later
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                onNavigateToAddEdit = { accountId ->
                    navController.navigate(Screen.AddEditAccount.createRoute(accountId))
                }
            )
        }

        composable(
            route = Screen.AddEditAccount.route,
            arguments = listOf(navArgument("accountId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) {
            AddEditAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateToAddEdit = { transactionId ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(
            route = Screen.AddEditTransaction.route,
            arguments = listOf(navArgument("transactionId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) {
            AddEditTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Will add more composables as we create screens
    }
}



