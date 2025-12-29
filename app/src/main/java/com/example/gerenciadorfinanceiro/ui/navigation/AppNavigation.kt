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
import com.example.gerenciadorfinanceiro.ui.screens.creditcards.CreditCardsScreen
import com.example.gerenciadorfinanceiro.ui.screens.creditcards.AddEditCreditCardScreen
import com.example.gerenciadorfinanceiro.ui.screens.creditcards.CreditCardDetailScreen

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
    object CreditCards : Screen("credit_cards")
    object AddEditCreditCard : Screen("credit_cards/add_edit?cardId={cardId}") {
        fun createRoute(cardId: Long? = null) = "credit_cards/add_edit?cardId=${cardId ?: -1}"
    }
    object CreditCardDetail : Screen("credit_cards/detail/{cardId}") {
        fun createRoute(cardId: Long) = "credit_cards/detail/$cardId"
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

        composable(Screen.CreditCards.route) {
            CreditCardsScreen(
                onNavigateToAddEdit = { cardId ->
                    navController.navigate(Screen.AddEditCreditCard.createRoute(cardId))
                },
                onNavigateToDetail = { cardId ->
                    navController.navigate(Screen.CreditCardDetail.createRoute(cardId))
                }
            )
        }

        composable(
            route = Screen.AddEditCreditCard.route,
            arguments = listOf(navArgument("cardId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) {
            AddEditCreditCardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreditCardDetail.route,
            arguments = listOf(navArgument("cardId") {
                type = NavType.StringType
            })
        ) {
            CreditCardDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    val cardId = it.arguments?.getString("cardId")?.toLongOrNull() ?: -1
                    navController.navigate(Screen.AddEditCreditCard.createRoute(cardId))
                }
            )
        }

        // Will add more composables as we create screens
    }
}



