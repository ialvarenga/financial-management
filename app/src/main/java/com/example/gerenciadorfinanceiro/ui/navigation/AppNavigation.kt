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
import com.example.gerenciadorfinanceiro.ui.screens.creditcards.AddEditCreditCardItemScreen
import com.example.gerenciadorfinanceiro.ui.screens.creditcards.ImportCsvScreen
import com.example.gerenciadorfinanceiro.ui.screens.recurrences.RecurrencesScreen
import com.example.gerenciadorfinanceiro.ui.screens.recurrences.AddEditRecurrenceScreen
import com.example.gerenciadorfinanceiro.ui.screens.transactions.AddTransferScreen
import com.example.gerenciadorfinanceiro.ui.screens.settings.NotificationSettingsScreen
import com.example.gerenciadorfinanceiro.ui.screens.settings.NotificationPermissionScreen
import com.example.gerenciadorfinanceiro.ui.screens.settings.BackupSettingsScreen
import com.example.gerenciadorfinanceiro.ui.screens.mais.MaisScreen

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
    object AddEditCreditCardItem : Screen("credit_cards/bills/{billId}/items/add_edit?itemId={itemId}") {
        fun createRoute(billId: Long, itemId: Long? = null) = "credit_cards/bills/$billId/items/add_edit?itemId=${itemId ?: -1}"
    }
    object ImportCsv : Screen("credit_cards/{cardId}/import_csv") {
        fun createRoute(cardId: Long) = "credit_cards/$cardId/import_csv"
    }
    object Recurrences : Screen("recurrences")
    object AddEditRecurrence : Screen("recurrences/add_edit?recurrenceId={recurrenceId}") {
        fun createRoute(recurrenceId: Long? = null) = "recurrences/add_edit?recurrenceId=${recurrenceId ?: -1}"
    }
    object AddTransfer : Screen("transactions/add_transfer?transferId={transferId}") {
        fun createRoute(transferId: Long? = null) = "transactions/add_transfer?transferId=${transferId ?: -1}"
    }
    object NotificationSettings : Screen("notification_settings")
    object NotificationPermission : Screen("notification_permission")
    object Settings : Screen("settings")
    object Mais : Screen("mais")

    // Will add more screens later
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecurrences = {
                    navController.navigate(Screen.Recurrences.route)
                },
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddEditTransaction.createRoute())
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToCreditCards = {
                    navController.navigate(Screen.CreditCards.route)
                }
            )
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
                },
                onNavigateToAddTransfer = { transferId ->
                    navController.navigate(Screen.AddTransfer.createRoute(transferId))
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
            val cardId = it.arguments?.getString("cardId")?.toLongOrNull() ?: -1
            CreditCardDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.AddEditCreditCard.createRoute(cardId))
                },
                onNavigateToAddItem = { billId ->
                    navController.navigate(Screen.AddEditCreditCardItem.createRoute(billId))
                },
                onNavigateToImportCsv = {
                    navController.navigate(Screen.ImportCsv.createRoute(cardId))
                }
            )
        }

        composable(
            route = Screen.AddEditCreditCardItem.route,
            arguments = listOf(
                navArgument("billId") {
                    type = NavType.StringType
                },
                navArgument("itemId") {
                    type = NavType.StringType
                    defaultValue = "-1"
                }
            )
        ) {
            AddEditCreditCardItemScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImportCsv.route,
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.StringType
                }
            )
        ) {
            ImportCsvScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recurrences.route) {
            RecurrencesScreen(
                onNavigateToAddEdit = { recurrenceId ->
                    navController.navigate(Screen.AddEditRecurrence.createRoute(recurrenceId))
                }
            )
        }

        composable(
            route = Screen.AddEditRecurrence.route,
            arguments = listOf(navArgument("recurrenceId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) {
            AddEditRecurrenceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransfer.route,
            arguments = listOf(navArgument("transferId") {
                type = NavType.StringType
                defaultValue = "-1"
            })
        ) {
            AddTransferScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermission = {
                    navController.navigate(Screen.NotificationPermission.route)
                }
            )
        }

        composable(Screen.NotificationPermission.route) {
            NotificationPermissionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            BackupSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                }
            )
        }

        composable(Screen.Mais.route) {
            MaisScreen(
                onNavigateToCreditCards = {
                    navController.navigate(Screen.CreditCards.route)
                },
                onNavigateToRecurrences = {
                    navController.navigate(Screen.Recurrences.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Will add more composables as we create screens
    }
}



