package com.example.gerenciadorfinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gerenciadorfinanceiro.ui.navigation.AppNavigation
import com.example.gerenciadorfinanceiro.ui.navigation.Screen
import com.example.gerenciadorfinanceiro.ui.theme.FinancialAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancialAppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Transações") },
                    label = { Text("Transações") },
                    selected = currentRoute == Screen.Transactions.route,
                    onClick = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Contas") },
                    label = { Text("Contas") },
                    selected = currentRoute == Screen.Accounts.route,
                    onClick = {
                        navController.navigate(Screen.Accounts.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Cartões") },
                    label = { Text("Cartões") },
                    selected = currentRoute == Screen.CreditCards.route,
                    onClick = {
                        navController.navigate(Screen.CreditCards.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Repeat, contentDescription = "Recorrências") },
                    label = { Text("Recorrências") },
                    selected = currentRoute == Screen.Recurrences.route,
                    onClick = {
                        navController.navigate(Screen.Recurrences.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(navController = navController)
        }
    }
}

