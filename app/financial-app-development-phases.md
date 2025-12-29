# Financial App - Development Phases

A step-by-step guide to building the app in testable phases. Each phase results in a working, testable feature.

---

## Overview

```
Phase 1: Project Setup & Base Infrastructure          ✅ COMPLETED
Phase 2: Independent Entities (Bank, Category)        ✅ COMPLETED (as Enums)
Phase 3: Account Management                           ✅ COMPLETED
Phase 4: Basic Transactions                           ✅ COMPLETED
Phase 5: Credit Card Structure                        ✅ COMPLETED
Phase 6: Credit Card Items & Installments             ✅ COMPLETED
Phase 7: Recurrences                                  ✅ COMPLETED
Phase 8: Transfers                                    ✅ COMPLETED
Phase 9: Dashboard & Projections                      ✅ COMPLETED
Phase 10: Polish & Extras                             ⏳ PENDING
```

---

## 🎯 Current Project Status

**Last Updated:** December 29, 2025
**Current Phase:** Phase 9 Complete - Ready for Phase 10 (Polish & Extras)
**Last Commits:**
- Phase 9 implementation: Dashboard & Projections with comprehensive UI
- Phase 8 implementation: Complete transfers system with from/to accounts
- `fd03176` - feat: Add BOLETO payment method and flexible recurrence payment options
- Phase 7 implementation: Recurrences with projection and confirmation
- `d398ee5` - fix: correct installment distribution based on bill status
- `5381d3c` - fix: resolve flow collection and duplicate key issues

### ✅ Completed Phases

#### Phase 1: Project Setup & Base Infrastructure
- ✅ Android project created with Jetpack Compose
- ✅ All dependencies configured (Hilt, Room, Navigation, Material 3)
- ✅ Base project structure established
- ✅ Utility functions created (Currency and Date)
- ✅ Type converters for Room database
- ✅ Hilt DI modules configured

#### Phase 2: Bank & Category (Modified Implementation)
**⚠️ Architecture Change:** Bank and Category implemented as **Enums** instead of entities
- ✅ Bank enum with 23 Brazilian banks (Nubank, Inter, Itaú, etc.)
- ✅ Category enum with 25+ categories (Expense, Income, Both types)
- ✅ All enums in `domain/model/` package
- ✅ Type converters registered for Room database
- ⚠️ **No repositories needed** (data is hardcoded in enums)

#### Phase 3: Account Management
- ✅ Account entity with Room annotations
- ✅ AccountDao with full CRUD operations
- ✅ AccountRepository with reactive flows
- ✅ AccountsViewModel with combined state (accounts + total balance)
- ✅ AddEditAccountViewModel with form validation
- ✅ AccountsScreen with Material 3 UI
- ✅ AddEditAccountScreen with bank dropdown selector
- ✅ Navigation integration (Screen.Accounts + Screen.AddEditAccount)
- ✅ Bottom navigation updated with "Contas" tab
- ✅ Currency formatting (Brazilian Real)
- ✅ Empty states and error handling
- ✅ Delete confirmation dialogs

#### Phase 4: Basic Transactions
- ✅ Transaction entity with Room annotations
- ✅ TransactionDao with queries for month, account, category, status
- ✅ TransactionRepository with reactive flows
- ✅ TransactionsViewModel with monthly filtering
- ✅ AddEditTransactionViewModel with form validation
- ✅ TransactionsScreen with Material 3 UI
- ✅ AddEditTransactionScreen with account/category dropdowns
- ✅ Navigation integration (Screen.Transactions + Screen.AddEditTransaction)
- ✅ Bottom navigation updated with "Transações" tab
- ✅ Transaction type toggle (Income/Expense)
- ✅ Status toggle (Pending/Completed)
- ✅ Date picker integration
- ✅ Account balance updates on transaction completion

#### Phase 5: Credit Card Structure
- ✅ CreditCard entity with Room annotations
- ✅ CreditCardBill entity with Room annotations
- ✅ CreditCardDao with full CRUD operations
- ✅ CreditCardBillDao with advanced queries (by card, month, status)
- ✅ CreditCardRepository with reactive flows
- ✅ CreditCardBillRepository with status management
- ✅ CreditCardsViewModel with active cards list
- ✅ AddEditCreditCardViewModel with complex form validation
- ✅ CreditCardDetailViewModel with bill history
- ✅ CreditCardsScreen with Material 3 UI
- ✅ AddEditCreditCardScreen with bank/account dropdowns
- ✅ CreditCardDetailScreen with current bill and history
- ✅ Navigation integration (Screen.CreditCards + routes)
- ✅ Bottom navigation updated with "Cartões" tab
- ✅ Credit limit, closing/due day tracking
- ✅ Payment account association (optional)

#### Phase 6: Credit Card Items & Installments
- ✅ CreditCardItem entity with installment support
- ✅ CreditCardItemDao with bill queries and installment group support
- ✅ CreditCardItemRepository with full CRUD operations
- ✅ AddCreditCardItemUseCase (single items with bill total update)
- ✅ CreateInstallmentPurchaseUseCase (2-12x installments across bills)
- ✅ GetOrCreateBillUseCase (automatic bill creation based on closing day)
- ✅ AddEditCreditCardItemViewModel with installment selection
- ✅ CreditCardDetailViewModel updated to load bill items with flatMapLatest
- ✅ AddEditCreditCardItemScreen with category dropdown and installment selector
- ✅ CreditCardDetailScreen updated to display items with FAB
- ✅ Navigation integration (Screen.AddEditCreditCardItem)
- ✅ Bill total amount auto-calculation
- ✅ Automatic bill creation for current and future months
- ✅ Delete item with installment group support
- ✅ Database version updated to 5
- ✅ **Bug Fix:** Flow collection issues resolved (nested infinite flows)
- ✅ **Bug Fix:** LazyColumn duplicate key crashes fixed
- ✅ **Bug Fix:** Installment distribution respects bill status (OPEN vs CLOSED)
- ✅ **Feature:** Items start from current bill if OPEN, next bill if CLOSED

#### Phase 7: Recurrences
- ✅ Recurrence entity with paymentMethod field
- ✅ RecurrenceDao with CRUD operations
- ✅ RecurrenceRepository
- ✅ GetMonthlyExpensesUseCase with projected recurrences
- ✅ ConfirmRecurrencePaymentUseCase (with optional account selection)
- ✅ RecurrencesViewModel and AddEditRecurrenceViewModel
- ✅ RecurrencesScreen with list, add, edit, delete functionality
- ✅ AddEditRecurrenceScreen with payment method selection
- ✅ TransactionsScreen shows projected recurrences with confirm button
- ✅ Account selection dialog for BOLETO and unassigned recurrences
- ✅ All frequencies supported: DAILY, WEEKLY, MONTHLY, YEARLY

#### Phase 8: Transfers
- ✅ Transfer entity with from/to account references and optional fee
- ✅ TransferWithAccounts data class for display purposes
- ✅ TransferDao with CRUD operations and date range queries
- ✅ TransferRepository with full CRUD operations
- ✅ ExecuteTransferUseCase (creates transfer and updates balances atomically)
- ✅ CompleteTransferUseCase (completes pending transfer and updates balances)
- ✅ GetMonthlyTransfersUseCase (gets transfers with account info)
- ✅ AddTransferViewModel with form validation
- ✅ AddTransferScreen with from/to account dropdowns and optional fee
- ✅ TransactionsScreen updated to show transfers section
- ✅ FAB shows menu dialog to choose between Transaction or Transfer
- ✅ Transfer cards with different styling (tertiary color scheme)
- ✅ Delete confirmation dialog for transfers
- ✅ Complete pending transfers functionality
- ✅ Database version updated to 8
- ✅ Navigation integration (Screen.AddTransfer)

#### Phase 9: Dashboard & Projections
- ✅ GetDashboardSummaryUseCase (total balance, unpaid bills, monthly income/expenses)
- ✅ GetBalanceAfterPaymentsUseCase (projected balance after all pending payments)
- ✅ DashboardSummary data class with all summary fields
- ✅ BalanceProjection data class with projection breakdown
- ✅ DashboardViewModel with month selection
- ✅ DashboardScreen with comprehensive UI:
  - ✅ Month selector with navigation (prev/next)
  - ✅ General balance card (current + projected)
  - ✅ Monthly overview card (income, expenses, balance)
  - ✅ Projection details card (breakdown of all factors)
  - ✅ Accounts carousel (horizontal scroll)
  - ✅ Upcoming bills section (credit card bills)
  - ✅ Upcoming recurrences section
  - ✅ Empty state for new users
- ✅ Reactive updates with Kotlin Flow

### 📁 Current Project Structure

```
GerenciadorFinanceiro/
├── app/src/main/java/com/example/gerenciadorfinanceiro/
│   ├── FinancialApp.kt                    ✅ Hilt application
│   ├── MainActivity.kt                    ✅ Main entry with bottom nav
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt        ✅ Room DB (v8, all entities)
│   │   │   │   ├── Converters.kt         ✅ All enum converters
│   │   │   │   └── dao/
│   │   │   │       ├── AccountDao.kt     ✅ Account CRUD
│   │   │   │       ├── TransactionDao.kt ✅ Transaction CRUD
│   │   │   │       ├── CreditCardDao.kt  ✅ CreditCard CRUD
│   │   │   │       ├── CreditCardBillDao.kt ✅ CreditCardBill CRUD
│   │   │   │       ├── CreditCardItemDao.kt ✅ CreditCardItem CRUD
│   │   │   │       ├── RecurrenceDao.kt  ✅ Recurrence CRUD
│   │   │   │       └── TransferDao.kt    ✅ Transfer CRUD
│   │   │   └── entity/
│   │   │       ├── Account.kt            ✅ Account entity
│   │   │       ├── Transaction.kt        ✅ Transaction entity
│   │   │       ├── CreditCard.kt         ✅ CreditCard entity
│   │   │       ├── CreditCardBill.kt     ✅ CreditCardBill entity
│   │   │       ├── CreditCardItem.kt     ✅ CreditCardItem entity
│   │   │       ├── Recurrence.kt         ✅ Recurrence entity (with paymentMethod)
│   │   │       └── Transfer.kt           ✅ Transfer entity (with TransferWithAccounts)
│   │   └── repository/
│   │       ├── AccountRepository.kt      ✅ Account repository
│   │       ├── TransactionRepository.kt  ✅ Transaction repository
│   │       ├── CreditCardRepository.kt   ✅ CreditCard repository
│   │       ├── CreditCardBillRepository.kt ✅ CreditCardBill repository
│   │       ├── CreditCardItemRepository.kt ✅ CreditCardItem repository
│   │       ├── RecurrenceRepository.kt   ✅ Recurrence repository
│   │       └── TransferRepository.kt     ✅ Transfer repository
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Bank.kt                   ✅ Bank enum (23 banks)
│   │   │   ├── Category.kt               ✅ Category enum (25+ categories)
│   │   │   ├── Enums.kt                  ✅ TransactionType, Status, PaymentMethod (with BOLETO), Frequency
│   │   │   └── ProjectedRecurrence.kt    ✅ Projected recurrence model
│   │   └── usecase/
│   │       ├── CreateTransactionUseCase.kt ✅ Transaction creation
│   │       ├── CompleteTransactionUseCase.kt ✅ Transaction completion
│   │       ├── GetMonthlyTransactionsUseCase.kt ✅ Monthly transactions
│   │       ├── AddCreditCardItemUseCase.kt ✅ Add credit card item
│   │       ├── CreateInstallmentPurchaseUseCase.kt ✅ Installment purchases
│   │       ├── GetOrCreateBillUseCase.kt ✅ Auto bill creation
│   │       ├── GetMonthlyExpensesUseCase.kt ✅ Monthly expenses with projected recurrences
│   │       ├── ConfirmRecurrencePaymentUseCase.kt ✅ Confirm recurrence (with optional account selection)
│   │       ├── ExecuteTransferUseCase.kt ✅ Execute transfer (create + balance update)
│   │       ├── CompleteTransferUseCase.kt ✅ Complete pending transfer
│   │       ├── GetMonthlyTransfersUseCase.kt ✅ Get monthly transfers with accounts
│   │       ├── GetDashboardSummaryUseCase.kt ✅ Dashboard summary (balance, bills, income/expenses)
│   │       └── GetBalanceAfterPaymentsUseCase.kt ✅ Balance projection after pending payments
│   │
│   ├── di/
│   │   └── DatabaseModule.kt             ✅ Hilt DI (provides all DAOs including TransferDao)
│   │
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── dashboard/
│   │   │   │   ├── DashboardScreen.kt    ✅ Full dashboard UI with projections
│   │   │   │   └── DashboardViewModel.kt ✅ Dashboard ViewModel
│   │   │   ├── accounts/
│   │   │   │   ├── AccountsScreen.kt     ✅ List screen
│   │   │   │   ├── AccountsViewModel.kt  ✅ ViewModel
│   │   │   │   ├── AddEditAccountScreen.kt ✅ Form screen
│   │   │   │   └── AddEditAccountViewModel.kt ✅ ViewModel
│   │   │   ├── transactions/
│   │   │   │   ├── TransactionsScreen.kt ✅ List screen (with transfers & recurrences)
│   │   │   │   ├── TransactionsViewModel.kt ✅ ViewModel (with transfers support)
│   │   │   │   ├── AddEditTransactionScreen.kt ✅ Form screen
│   │   │   │   ├── AddEditTransactionViewModel.kt ✅ ViewModel
│   │   │   │   ├── AddTransferScreen.kt  ✅ Transfer form screen
│   │   │   │   └── AddTransferViewModel.kt ✅ Transfer ViewModel
│   │   │   ├── creditcards/
│   │   │   │   ├── CreditCardsScreen.kt  ✅ List screen
│   │   │   │   ├── CreditCardsViewModel.kt ✅ ViewModel
│   │   │   │   ├── AddEditCreditCardScreen.kt ✅ Form screen
│   │   │   │   ├── AddEditCreditCardViewModel.kt ✅ ViewModel
│   │   │   │   ├── CreditCardDetailScreen.kt ✅ Detail screen (with items)
│   │   │   │   ├── CreditCardDetailViewModel.kt ✅ ViewModel
│   │   │   │   ├── AddEditCreditCardItemScreen.kt ✅ Add item screen
│   │   │   │   └── AddEditCreditCardItemViewModel.kt ✅ ViewModel
│   │   │   └── recurrences/
│   │   │       ├── RecurrencesScreen.kt  ✅ List screen
│   │   │       ├── RecurrencesViewModel.kt ✅ ViewModel
│   │   │       ├── AddEditRecurrenceScreen.kt ✅ Form screen (with payment method selection)
│   │   │       └── AddEditRecurrenceViewModel.kt ✅ ViewModel
│   │   ├── navigation/
│   │   │   └── AppNavigation.kt          ✅ NavHost with routes
│   │   ├── theme/                         ✅ Material 3 theme
│   │   └── components/                    ⏳ Ready for reusable components
│   │
│   └── util/
│       ├── CurrencyUtils.kt              ✅ toReais(), toCents()
│       └── DateUtils.kt                  ✅ Date formatting utils
│
├── .gitignore                            ✅ Comprehensive Android ignores
└── docs/                                 🚫 Excluded from git
```

### 🔧 Key Implementation Details

**Database Version:** 5 (updated in Phase 6)
**Package Name:** com.example.gerenciadorfinanceiro
**Min SDK:** 26 (Android 8.0)

**Architecture:**
- MVVM pattern with Repository
- Hilt for Dependency Injection
- Room for local database
- Kotlin Flows for reactive data
- Material 3 for UI

**Deviations from Original Plan:**
1. ✅ Bank and Category are **enums** (not entities) - simpler and more maintainable
2. ✅ No BankRepository or CategoryRepository needed
3. ✅ Account entity uses Bank enum directly (foreign key not needed)
4. ✅ Type converters handle enum ↔ String conversion for Room

---

## Phase 1: Project Setup & Base Infrastructure

### Goals
- Create project with proper architecture
- Set up dependencies
- Create base classes and utilities

### Tasks

#### 1.1 Create Android Project
- New Android Studio project with Compose
- Package name: `com.yourname.financialapp`
- Minimum SDK: 26 (Android 8.0)

#### 1.2 Add Dependencies (`build.gradle.kts` app level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

#### 1.3 Create Base Project Structure

```
app/src/main/java/com/yourname/financialapp/
├── FinancialApp.kt                 # Application class
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── Converters.kt
│   │   │   └── dao/
│   │   └── entity/
│   └── repository/
├── domain/
│   ├── model/
│   └── usecase/
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/
│   ├── navigation/
│   │   └── AppNavigation.kt
│   └── screens/
└── di/
    ├── AppModule.kt
    └── DatabaseModule.kt
```

#### 1.4 Create Enums

```kotlin
// data/local/entity/Enums.kt
package com.yourname.financialapp.data.local.entity

enum class TransactionType { INCOME, EXPENSE }
enum class TransactionStatus { PENDING, COMPLETED, CANCELLED }
enum class BillStatus { OPEN, CLOSED, PAID, OVERDUE }
enum class CategoryType { INCOME, EXPENSE, BOTH }
enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }
enum class PaymentMethod { DEBIT, PIX, TRANSFER, CREDIT_CARD }
```

#### 1.5 Create Utility Functions

```kotlin
// util/CurrencyUtils.kt
package com.yourname.financialapp.util

import java.text.NumberFormat
import java.util.Locale

fun Long.toReais(): String {
    val value = this / 100.0
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

fun String.toCents(): Long? {
    return try {
        val cleaned = this
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        (cleaned.toDouble() * 100).toLong()
    } catch (e: Exception) {
        null
    }
}
```

```kotlin
// util/DateUtils.kt
package com.yourname.financialapp.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getMonthBounds(month: Int, year: Int): Pair<Long, Long> {
    val start = LocalDate.of(year, month, 1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    
    val end = LocalDate.of(year, month, 1)
        .plusMonths(1)
        .minusDays(1)
        .atTime(23, 59, 59)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    
    return start to end
}

fun formatMonthYear(month: Int, year: Int): String {
    val date = LocalDate.of(year, month, 1)
    val formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR"))
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun LocalDate.toEpochMilli(): Long {
    return this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
```

#### 1.6 Create Application Class

```kotlin
// FinancialApp.kt
package com.yourname.financialapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinancialApp : Application()
```

#### 1.7 Create Empty Database (will add entities later)

```kotlin
// data/local/database/AppDatabase.kt
package com.yourname.financialapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [],  // Will add entities as we create them
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

```kotlin
// data/local/database/Converters.kt
package com.yourname.financialapp.data.local.database

import androidx.room.TypeConverter
import com.yourname.financialapp.data.local.entity.*

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    
    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus): String = value.name
    
    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)
    
    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name
    
    @TypeConverter
    fun toBillStatus(value: String): BillStatus = BillStatus.valueOf(value)
    
    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name
    
    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
    
    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name
    
    @TypeConverter
    fun toFrequency(value: String): Frequency = Frequency.valueOf(value)
    
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name
    
    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}
```

#### 1.8 Create DI Modules

```kotlin
// di/DatabaseModule.kt
package com.yourname.financialapp.di

import android.content.Context
import androidx.room.Room
import com.yourname.financialapp.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "financial_app.db"
        ).build()
    }
}
```

#### 1.9 Create Basic Navigation

```kotlin
// ui/navigation/AppNavigation.kt
package com.yourname.financialapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Banks : Screen("banks")
    object AddEditBank : Screen("banks/add_edit?bankId={bankId}") {
        fun createRoute(bankId: Long? = null) = "banks/add_edit?bankId=${bankId ?: -1}"
    }
    object Categories : Screen("categories")
    object AddEditCategory : Screen("categories/add_edit?categoryId={categoryId}") {
        fun createRoute(categoryId: Long? = null) = "categories/add_edit?categoryId=${categoryId ?: -1}"
    }
    // Will add more screens later
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // Will add composables as we create screens
    }
}
```

#### 1.10 Create MainActivity

```kotlin
// MainActivity.kt
package com.yourname.financialapp

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
import com.yourname.financialapp.ui.navigation.AppNavigation
import com.yourname.financialapp.ui.navigation.Screen
import com.yourname.financialapp.ui.theme.FinancialAppTheme
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
                    onClick = { navController.navigate(Screen.Dashboard.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Banks") },
                    label = { Text("Bancos") },
                    selected = currentRoute == Screen.Banks.route,
                    onClick = { navController.navigate(Screen.Banks.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
                    label = { Text("Categorias") },
                    selected = currentRoute == Screen.Categories.route,
                    onClick = { navController.navigate(Screen.Categories.route) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(navController = navController)
        }
    }
}
```

### Test Phase 1 ✅ COMPLETED
- [x] Project compiles without errors (BUILD SUCCESSFUL)
- [x] App launches and shows DashboardScreen with bottom navigation
- [x] Navigation bar items are visible (Home and Contas tabs)

---

## Phase 2: Independent Entities (Bank, Category)

### Goals
- Create Bank and Category entities (no foreign keys)
- Full CRUD operations for both
- Testable list and add/edit screens

### Phase 2A: Bank Entity

#### 2A.1 Create Bank Entity

```kotlin
// data/local/entity/Bank.kt
package com.yourname.financialapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUrl: String? = null
)
```

#### 2A.2 Create BankDao

```kotlin
// data/local/database/dao/BankDao.kt
package com.yourname.financialapp.data.local.database.dao

import androidx.room.*
import com.yourname.financialapp.data.local.entity.Bank
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAll(): Flow<List<Bank>>
    
    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getById(id: Long): Bank?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: Bank): Long
    
    @Update
    suspend fun update(bank: Bank)
    
    @Delete
    suspend fun delete(bank: Bank)
    
    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

#### 2A.3 Update AppDatabase

```kotlin
// data/local/database/AppDatabase.kt
@Database(
    entities = [Bank::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
}
```

#### 2A.4 Update DatabaseModule

```kotlin
// di/DatabaseModule.kt - Add this
@Provides
fun provideBankDao(database: AppDatabase): BankDao = database.bankDao()
```

#### 2A.5 Create BankRepository

```kotlin
// data/repository/BankRepository.kt
package com.yourname.financialapp.data.repository

import com.yourname.financialapp.data.local.database.dao.BankDao
import com.yourname.financialapp.data.local.entity.Bank
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankRepository @Inject constructor(
    private val bankDao: BankDao
) {
    fun getAll(): Flow<List<Bank>> = bankDao.getAll()
    
    suspend fun getById(id: Long): Bank? = bankDao.getById(id)
    
    suspend fun insert(bank: Bank): Long = bankDao.insert(bank)
    
    suspend fun update(bank: Bank) = bankDao.update(bank)
    
    suspend fun delete(bank: Bank) = bankDao.delete(bank)
    
    suspend fun deleteById(id: Long) = bankDao.deleteById(id)
}
```

#### 2A.6 Create BanksViewModel

```kotlin
// ui/screens/banks/BanksViewModel.kt
package com.yourname.financialapp.ui.screens.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.financialapp.data.local.entity.Bank
import com.yourname.financialapp.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BanksUiState(
    val banks: List<Bank> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BanksViewModel @Inject constructor(
    private val bankRepository: BankRepository
) : ViewModel() {
    
    val uiState: StateFlow<BanksUiState> = bankRepository.getAll()
        .map { BanksUiState(banks = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BanksUiState()
        )
    
    fun deleteBank(bank: Bank) {
        viewModelScope.launch {
            bankRepository.delete(bank)
        }
    }
}
```

#### 2A.7 Create AddEditBankViewModel

```kotlin
// ui/screens/banks/AddEditBankViewModel.kt
package com.yourname.financialapp.ui.screens.banks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.financialapp.data.local.entity.Bank
import com.yourname.financialapp.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditBankUiState(
    val name: String = "",
    val imageUrl: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditBankViewModel @Inject constructor(
    private val bankRepository: BankRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val bankId: Long = savedStateHandle.get<String>("bankId")?.toLongOrNull() ?: -1
    
    private val _uiState = MutableStateFlow(AddEditBankUiState())
    val uiState: StateFlow<AddEditBankUiState> = _uiState.asStateFlow()
    
    init {
        if (bankId > 0) {
            loadBank()
        }
    }
    
    private fun loadBank() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bank = bankRepository.getById(bankId)
            if (bank != null) {
                _uiState.update {
                    it.copy(
                        name = bank.name,
                        imageUrl = bank.imageUrl ?: "",
                        isEditing = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Banco não encontrado") }
            }
        }
    }
    
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }
    
    fun onImageUrlChange(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }
    
    fun save() {
        val currentState = _uiState.value
        
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome é obrigatório") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val bank = Bank(
                id = if (currentState.isEditing) bankId else 0,
                name = currentState.name.trim(),
                imageUrl = currentState.imageUrl.takeIf { it.isNotBlank() }
            )
            
            if (currentState.isEditing) {
                bankRepository.update(bank)
            } else {
                bankRepository.insert(bank)
            }
            
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
```

#### 2A.8 Create BanksScreen

```kotlin
// ui/screens/banks/BanksScreen.kt
package com.yourname.financialapp.ui.screens.banks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.financialapp.data.local.entity.Bank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksScreen(
    onNavigateToAddEdit: (Long?) -> Unit,
    viewModel: BanksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bankToDelete by remember { mutableStateOf<Bank?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bancos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar banco")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.banks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum banco cadastrado")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.banks, key = { it.id }) { bank ->
                    BankItem(
                        bank = bank,
                        onClick = { onNavigateToAddEdit(bank.id) },
                        onDelete = { bankToDelete = bank }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    bankToDelete?.let { bank ->
        AlertDialog(
            onDismissRequest = { bankToDelete = null },
            title = { Text("Excluir banco") },
            text = { Text("Deseja excluir o banco '${bank.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBank(bank)
                    bankToDelete = null
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { bankToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BankItem(
    bank: Bank,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = bank.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

#### 2A.9 Create AddEditBankScreen

```kotlin
// ui/screens/banks/AddEditBankScreen.kt
package com.yourname.financialapp.ui.screens.banks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Banco" else "Novo Banco") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome do banco") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } },
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.imageUrl,
                onValueChange = viewModel::onImageUrlChange,
                label = { Text("URL da imagem (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.isEditing) "Salvar" else "Criar")
                }
            }
        }
    }
}
```

#### 2A.10 Update Navigation

```kotlin
// ui/navigation/AppNavigation.kt - Update NavHost
NavHost(
    navController = navController,
    startDestination = Screen.Dashboard.route
) {
    composable(Screen.Dashboard.route) {
        // Temporary placeholder
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Dashboard - Em breve")
        }
    }
    
    composable(Screen.Banks.route) {
        BanksScreen(
            onNavigateToAddEdit = { bankId ->
                navController.navigate(Screen.AddEditBank.createRoute(bankId))
            }
        )
    }
    
    composable(
        route = Screen.AddEditBank.route,
        arguments = listOf(navArgument("bankId") { defaultValue = "-1" })
    ) {
        AddEditBankScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

### Test Phase 2A ✅ COMPLETED (Modified as Enum)
**⚠️ Implementation changed:** Bank is now an enum, not an entity
- [x] Bank enum created with 23 Brazilian banks
- [x] ~~Banks list/add/edit/delete screens~~ **N/A** - Not needed (enum approach)
- [x] Bank.kt includes: Nubank, Inter, Itaú, Bradesco, Santander, etc.
- [x] Bank enum has `displayName` property for UI
- [x] Bank enum has `fromName()` companion function
- [x] Type converter registered in Converters.kt

---

### Phase 2B: Category Entity

#### 2B.1 Create Category Entity

```kotlin
// data/local/entity/Category.kt
package com.yourname.financialapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "category",  // Material icon name
    val color: String = "#6200EE",  // Hex color
    val type: CategoryType,
    val parentId: Long? = null,
    val isSystem: Boolean = false
)
```

#### 2B.2 Create CategoryDao

```kotlin
// data/local/database/dao/CategoryDao.kt
package com.yourname.financialapp.data.local.database.dao

import androidx.room.*
import com.yourname.financialapp.data.local.entity.Category
import com.yourname.financialapp.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE type = :type OR type = 'BOTH' ORDER BY name ASC")
    fun getByType(type: CategoryType): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
```

#### 2B.3 Update AppDatabase

```kotlin
@Database(
    entities = [Bank::class, Category::class],
    version = 1,
    exportSchema = true
)
```

Add to DatabaseModule:
```kotlin
@Provides
fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()
```

#### 2B.4 Create CategoryRepository

```kotlin
// data/repository/CategoryRepository.kt
package com.yourname.financialapp.data.repository

import com.yourname.financialapp.data.local.database.dao.CategoryDao
import com.yourname.financialapp.data.local.entity.Category
import com.yourname.financialapp.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAll()
    
    fun getByType(type: CategoryType): Flow<List<Category>> = categoryDao.getByType(type)
    
    suspend fun getById(id: Long): Category? = categoryDao.getById(id)
    
    suspend fun insert(category: Category): Long = categoryDao.insert(category)
    
    suspend fun update(category: Category) = categoryDao.update(category)
    
    suspend fun delete(category: Category) = categoryDao.delete(category)
    
    suspend fun deleteById(id: Long) = categoryDao.deleteById(id)
    
    suspend fun count(): Int = categoryDao.count()
    
    suspend fun insertDefaultCategories() {
        if (count() == 0) {
            val defaults = listOf(
                // Expense categories
                Category(name = "Alimentação", icon = "restaurant", color = "#FF5722", type = CategoryType.EXPENSE),
                Category(name = "Transporte", icon = "directions_car", color = "#2196F3", type = CategoryType.EXPENSE),
                Category(name = "Moradia", icon = "home", color = "#4CAF50", type = CategoryType.EXPENSE),
                Category(name = "Saúde", icon = "medical_services", color = "#F44336", type = CategoryType.EXPENSE),
                Category(name = "Educação", icon = "school", color = "#9C27B0", type = CategoryType.EXPENSE),
                Category(name = "Lazer", icon = "sports_esports", color = "#FF9800", type = CategoryType.EXPENSE),
                Category(name = "Compras", icon = "shopping_bag", color = "#E91E63", type = CategoryType.EXPENSE),
                Category(name = "Serviços", icon = "build", color = "#607D8B", type = CategoryType.EXPENSE),
                // Income categories
                Category(name = "Salário", icon = "payments", color = "#4CAF50", type = CategoryType.INCOME),
                Category(name = "Freelance", icon = "work", color = "#2196F3", type = CategoryType.INCOME),
                Category(name = "Investimentos", icon = "trending_up", color = "#FF9800", type = CategoryType.INCOME),
                Category(name = "Outros", icon = "attach_money", color = "#9E9E9E", type = CategoryType.BOTH)
            )
            categoryDao.insertAll(defaults.map { it.copy(isSystem = true) })
        }
    }
}
```

#### 2B.5 Create CategoriesViewModel

```kotlin
// ui/screens/categories/CategoriesViewModel.kt
package com.yourname.financialapp.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.financialapp.data.local.entity.Category
import com.yourname.financialapp.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    val uiState: StateFlow<CategoriesUiState> = categoryRepository.getAll()
        .map { CategoriesUiState(categories = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoriesUiState()
        )
    
    init {
        viewModelScope.launch {
            categoryRepository.insertDefaultCategories()
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.delete(category)
        }
    }
}
```

#### 2B.6 Create AddEditCategoryViewModel

```kotlin
// ui/screens/categories/AddEditCategoryViewModel.kt
package com.yourname.financialapp.ui.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.financialapp.data.local.entity.Category
import com.yourname.financialapp.data.local.entity.CategoryType
import com.yourname.financialapp.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditCategoryUiState(
    val name: String = "",
    val icon: String = "category",
    val color: String = "#6200EE",
    val type: CategoryType = CategoryType.EXPENSE,
    val isEditing: Boolean = false,
    val isSystem: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1
    
    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()
    
    init {
        if (categoryId > 0) {
            loadCategory()
        }
    }
    
    private fun loadCategory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val category = categoryRepository.getById(categoryId)
            if (category != null) {
                _uiState.update {
                    it.copy(
                        name = category.name,
                        icon = category.icon,
                        color = category.color,
                        type = category.type,
                        isEditing = true,
                        isSystem = category.isSystem,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Categoria não encontrada") }
            }
        }
    }
    
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }
    
    fun onIconChange(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }
    
    fun onColorChange(color: String) {
        _uiState.update { it.copy(color = color) }
    }
    
    fun onTypeChange(type: CategoryType) {
        _uiState.update { it.copy(type = type) }
    }
    
    fun save() {
        val currentState = _uiState.value
        
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome é obrigatório") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val category = Category(
                id = if (currentState.isEditing) categoryId else 0,
                name = currentState.name.trim(),
                icon = currentState.icon,
                color = currentState.color,
                type = currentState.type,
                isSystem = currentState.isSystem
            )
            
            if (currentState.isEditing) {
                categoryRepository.update(category)
            } else {
                categoryRepository.insert(category)
            }
            
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
```

#### 2B.7 Create CategoriesScreen

```kotlin
// ui/screens/categories/CategoriesScreen.kt
package com.yourname.financialapp.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.financialapp.data.local.entity.Category
import com.yourname.financialapp.data.local.entity.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateToAddEdit: (Long?) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Categorias") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEdit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar categoria")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onClick = { onNavigateToAddEdit(category.id) },
                        onDelete = { categoryToDelete = category }
                    )
                }
            }
        }
    }
    
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Excluir categoria") },
            text = { Text("Deseja excluir a categoria '${category.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    categoryToDelete = null
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = categoryColor
                    )
                }
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (category.type) {
                            CategoryType.INCOME -> "Receita"
                            CategoryType.EXPENSE -> "Despesa"
                            CategoryType.BOTH -> "Ambos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!category.isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

#### 2B.8 Create AddEditCategoryScreen

```kotlin
// ui/screens/categories/AddEditCategoryScreen.kt
package com.yourname.financialapp.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.financialapp.data.local.entity.CategoryType

val predefinedColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
    "#795548", "#9E9E9E", "#607D8B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar Categoria" else "Nova Categoria") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome da categoria") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } },
                singleLine = true,
                enabled = !uiState.isSystem
            )
            
            // Type selector
            Text("Tipo", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryType.values().forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    CategoryType.INCOME -> "Receita"
                                    CategoryType.EXPENSE -> "Despesa"
                                    CategoryType.BOTH -> "Ambos"
                                }
                            )
                        },
                        enabled = !uiState.isSystem
                    )
                }
            }
            
            // Color selector
            Text("Cor", style = MaterialTheme.typography.titleSmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(150.dp)
            ) {
                items(predefinedColors) { colorHex ->
                    val color = Color(android.graphics.Color.parseColor(colorHex))
                    val isSelected = uiState.color == colorHex
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.onColorChange(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selecionado",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.isEditing) "Salvar" else "Criar")
                }
            }
        }
    }
}
```

#### 2B.9 Update Navigation

Add to AppNavigation.kt:

```kotlin
composable(Screen.Categories.route) {
    CategoriesScreen(
        onNavigateToAddEdit = { categoryId ->
            navController.navigate(Screen.AddEditCategory.createRoute(categoryId))
        }
    )
}

composable(
    route = Screen.AddEditCategory.route,
    arguments = listOf(navArgument("categoryId") { defaultValue = "-1" })
) {
    AddEditCategoryScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Test Phase 2B ✅ COMPLETED (Modified as Enum)
**⚠️ Implementation changed:** Category is now an enum, not an entity
- [x] Category enum created with 25+ categories
- [x] ~~Categories list/add/edit/delete screens~~ **N/A** - Not needed (enum approach)
- [x] Categories include: Food, Transport, Housing, Health, Salary, Freelance, etc.
- [x] Each category has: displayName, type (INCOME/EXPENSE/BOTH), color
- [x] Category enum has helper methods: expenses(), incomes(), fromName()
- [x] Type converter registered in Converters.kt

---

## Phase 3: Account Management

### Goals
- Create Account entity (depends on Bank)
- Full CRUD with bank selection
- Display balance

### Tasks

#### 3.1 Create Account Entity

```kotlin
// data/local/entity/Account.kt
package com.yourname.financialapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = Bank::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bankId"])]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val agency: String,
    val number: String,
    val bankId: Long,
    val balance: Long,  // in cents
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 3.2 Create AccountDao

```kotlin
// data/local/database/dao/AccountDao.kt
package com.yourname.financialapp.data.local.database.dao

import androidx.room.*
import com.yourname.financialapp.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveAccounts(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAll(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?
    
    @Query("SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE isActive = 1")
    fun getTotalBalance(): Flow<Long>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long
    
    @Update
    suspend fun update(account: Account)
    
    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun increaseBalance(accountId: Long, amount: Long)
    
    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :accountId")
    suspend fun decreaseBalance(accountId: Long, amount: Long)
    
    @Delete
    suspend fun delete(account: Account)
    
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

#### 3.3 Create Account with Bank relation

```kotlin
// data/local/entity/AccountWithBank.kt
package com.yourname.financialapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AccountWithBank(
    @Embedded val account: Account,
    @Relation(
        parentColumn = "bankId",
        entityColumn = "id"
    )
    val bank: Bank
)
```

Add to AccountDao:
```kotlin
@Transaction
@Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
fun getActiveAccountsWithBank(): Flow<List<AccountWithBank>>

@Transaction
@Query("SELECT * FROM accounts ORDER BY name ASC")
fun getAllWithBank(): Flow<List<AccountWithBank>>
```

#### 3.4 Update Database & DI

Update AppDatabase entities and add dao.

#### 3.5 Create AccountRepository

```kotlin
// data/repository/AccountRepository.kt
package com.yourname.financialapp.data.repository

import com.yourname.financialapp.data.local.database.dao.AccountDao
import com.yourname.financialapp.data.local.entity.Account
import com.yourname.financialapp.data.local.entity.AccountWithBank
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getActiveAccounts(): Flow<List<Account>> = accountDao.getActiveAccounts()
    
    fun getActiveAccountsWithBank(): Flow<List<AccountWithBank>> = accountDao.getActiveAccountsWithBank()
    
    fun getAllWithBank(): Flow<List<AccountWithBank>> = accountDao.getAllWithBank()
    
    fun getTotalBalance(): Flow<Long> = accountDao.getTotalBalance()
    
    suspend fun getById(id: Long): Account? = accountDao.getById(id)
    
    suspend fun insert(account: Account): Long = accountDao.insert(account)
    
    suspend fun update(account: Account) = accountDao.update(account)
    
    suspend fun increaseBalance(accountId: Long, amount: Long) = accountDao.increaseBalance(accountId, amount)
    
    suspend fun decreaseBalance(accountId: Long, amount: Long) = accountDao.decreaseBalance(accountId, amount)
    
    suspend fun delete(account: Account) = accountDao.delete(account)
}
```

#### 3.6 Create AccountsViewModel, AddEditAccountViewModel

Similar structure to Banks, but with:
- Bank selection dropdown
- Balance input (currency formatted)
- Agency and number fields

#### 3.7 Create AccountsScreen, AddEditAccountScreen

Similar to Banks screens, but displaying:
- Bank name
- Balance (formatted as currency)
- Account number

#### 3.8 Update Navigation

Add Accounts screen to navigation and bottom bar.

### Test Phase 3 ✅ COMPLETED
- [x] ~~Cannot add account without a bank~~ **N/A** - Banks are now enums (always available)
- [x] Can add account with bank selection from dropdown (23 Brazilian banks)
- [x] Balance displays correctly formatted in Brazilian Real (R$)
- [x] Can edit account (name, bank, agency, number, balance)
- [x] Can delete account with confirmation dialog
- [x] Total balance updates correctly and displays in summary card

---

## Phase 4: Basic Transactions

### Goals
- Create Transaction entity
- Add income/expense transactions
- Link to Account and Category
- Update account balance on transaction completion

### Tasks

#### 4.1 Create Transaction Entity

(Use entity from recommendations document)

#### 4.2 Create TransactionDao

With queries for:
- Get by month
- Get by account
- Get by category
- Get pending
- Get completed

#### 4.3 Create TransactionRepository

#### 4.4 Create Use Cases

- `CreateTransactionUseCase` (updates balance if completed)
- `CompleteTransactionUseCase` (marks as completed, updates balance)
- `GetMonthlyTransactionsUseCase`

#### 4.5 Create TransactionsViewModel, AddEditTransactionViewModel

#### 4.6 Create TransactionsScreen, AddEditTransactionScreen

With:
- Account dropdown
- Category dropdown
- Type toggle (Income/Expense)
- Status toggle (Pending/Completed)
- Date picker
- Value input (currency)

### Test Phase 4 ✅ COMPLETED
- [x] Can add income transaction
- [x] Can add expense transaction
- [x] Account balance updates when transaction is completed
- [x] Can filter by month
- [x] Can edit transaction
- [x] Can delete transaction

---

## Phase 5: Credit Card Structure

### Goals
- Create CreditCard entity
- Create CreditCardBill entity
- Basic credit card management

### Tasks

#### 5.1 Create CreditCard Entity

#### 5.2 Create CreditCardBill Entity

#### 5.3 Create DAOs

#### 5.4 Create Repositories

#### 5.5 Create CreditCardsScreen, AddEditCreditCardScreen

With:
- Name, last 4 digits
- Credit limit
- Closing day, due day
- Bank selection
- Payment account selection

#### 5.6 Create CreditCardDetailScreen

Showing:
- Card info
- Current bill (if exists)
- Bill history

### Test Phase 5 ✅ COMPLETED
- [x] Can add credit card
- [x] Can view credit card details
- [x] Can edit credit card
- [x] Can delete credit card (cascade deletes bills)

---

## Phase 6: Credit Card Items & Installments

### Goals
- Create CreditCardItem entity
- Add items to bills
- Support installment purchases

### Tasks

#### 6.1 Create CreditCardItem Entity

#### 6.2 Create CreditCardItemDao

#### 6.3 Create Use Cases

- `AddCreditCardItemUseCase`
- `CreateInstallmentPurchaseUseCase`
- `GetBillWithItemsUseCase`

#### 6.4 Create AddCreditCardItemScreen

With:
- Description
- Value
- Category
- Installments option (1x, 2x, 3x... 12x)

#### 6.5 Update CreditCardDetailScreen

To show bill items

### Test Phase 6 ✅ COMPLETED
- [x] Can add single item to bill
- [x] Can add installment purchase (2-12x)
- [x] Installments create items in future bills automatically
- [x] Installments start from current bill if OPEN, next bill if CLOSED
- [x] Bill is created automatically for current month on card detail view
- [x] Bill total calculates correctly and updates reactively
- [x] Can delete item (with installment group support)
- [x] UI displays items with category, amount, and installment info
- [x] FAB appears on current bill to add items
- [x] No crashes when adding/viewing items (flow and key issues fixed)
- [x] Navigation works correctly between screens

---

## Phase 7: Recurrences

### Goals
- Create Recurrence entity
- Project recurrences on-demand
- Confirm recurrence payments

### Tasks

#### 7.1 Create Recurrence Entity

#### 7.2 Create RecurrenceDao

#### 7.3 Create RecurrenceRepository

#### 7.4 Create Use Cases

- `GetMonthlyExpensesUseCase` (combines real + projected)
- `ConfirmRecurrencePaymentUseCase`

#### 7.5 Create RecurrencesScreen, AddEditRecurrenceScreen

With:
- Description
- Value
- Payment method (Account or Credit Card)
- Frequency
- Day of month
- Start/End date

#### 7.6 Update TransactionsScreen

To show projected recurrences with "confirm" button

### Test Phase 7 ✅ COMPLETED
- [x] Can add account-based recurrence
- [x] Can add credit card recurrence
- [x] Projected items show in monthly view (TransactionsScreen)
- [x] Can confirm recurrence (creates real transaction/item)
- [x] No duplication between recurrence and credit card items
- [x] Can edit/delete recurrence
- [x] Recurrence projection logic works for all frequencies (DAILY, WEEKLY, MONTHLY, YEARLY)
- [x] RecurrencesScreen with list, add, edit, delete functionality
- [x] Navigation integrated with app routes

---

## Phase 8: Transfers ✅ COMPLETED

### Goals
- Create Transfer entity
- Transfer between accounts

### Tasks

#### 8.1 Create Transfer Entity ✅
- Transfer entity with fromAccountId, toAccountId, amount, fee, status, date
- TransferWithAccounts data class for UI display

#### 8.2 Create TransferDao ✅
- Full CRUD operations
- Date range queries for monthly view
- Account-specific queries
- Status queries

#### 8.3 Create TransferRepository ✅
- Wrapper around TransferDao
- All repository methods implemented

#### 8.4 Create Transfer Use Cases ✅
- ExecuteTransferUseCase: Creates transfer and updates both account balances atomically
- CompleteTransferUseCase: Completes pending transfer and updates balances
- GetMonthlyTransfersUseCase: Gets transfers with account info for display

Updates both account balances atomically:
- Source account: decreases by (amount + fee)
- Destination account: increases by amount

#### 8.5 Create Transfer UI ✅
- AddTransferScreen with:
  - Description field
  - Amount field
  - From account dropdown (with validation)
  - To account dropdown (with validation)
  - Optional fee field
  - Status toggle (Pending/Completed)
  - Date field
  - Notes field
- AddTransferViewModel with form validation
- TransferItem composable for list display

#### 8.6 Integrate with TransactionsScreen ✅
- FAB shows dialog to choose between Transaction or Transfer
- Transfers displayed in separate section
- Transfer cards with tertiary color scheme
- Delete confirmation dialog for transfers
- Complete pending transfers functionality

### Test Phase 8
- [x] Can transfer between accounts
- [x] Both balances update correctly (when completed)
- [x] Fee deducts from source account
- [x] Can view transfer history in monthly view
- [x] Can complete pending transfers
- [x] Can delete transfers
- [x] Cannot select same account for from and to

---

## Phase 9: Dashboard & Projections

### Goals
- Create dashboard with summaries
- Balance after payments projection
- Monthly overview

### Tasks

#### 9.1 Create DashboardDao

With aggregation queries

#### 9.2 Create Use Cases

- `GetDashboardSummaryUseCase`
- `GetBalanceAfterPaymentsUseCase`

#### 9.3 Create DashboardScreen

Showing:
- General balance (all accounts)
- Balance after payments
- Total unpaid credit card bills
- Monthly expenses summary
- Upcoming bills

#### 9.4 Create MonthlyOverviewScreen (optional)

Detailed monthly view with:
- Income vs Expenses chart
- Category breakdown
- Pending items

### Test Phase 9
- [ ] Dashboard shows correct totals
- [ ] Balance after payments calculates correctly
- [ ] Includes pending transactions
- [ ] Includes projected recurrences
- [ ] Includes unpaid credit card bills

---

## Phase 10: Polish & Extras

### Goals
- UI improvements
- Settings
- Data management

### Tasks

#### 10.1 Settings Screen

- Theme (light/dark/system)
- Currency format
- Default account

#### 10.2 Data Export/Import

- Export to JSON
- Import from JSON
- Backup reminder

#### 10.3 Widgets (optional)

- Balance widget
- Quick add widget

#### 10.4 Notifications (optional)

- Bill due reminders
- Low balance alerts

#### 10.5 Polish

- Loading states
- Error handling
- Empty states
- Animations

### Test Phase 10
- [ ] Settings persist correctly
- [ ] Export creates valid file
- [ ] Import restores data correctly
- [ ] App handles errors gracefully

---

## Database Migration Strategy

When moving between phases, increment database version and add migrations:

```kotlin
@Database(
    entities = [Bank::class, Category::class, Account::class, ...],
    version = 2,  // increment as you add entities
    exportSchema = true
)

// Add migration
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                ...
            )
        """)
    }
}
```

During development, you can use `.fallbackToDestructiveMigration()` but remove it for production.

---

## Summary Checklist

| Phase | Description | Dependencies | Status |
|-------|-------------|--------------|--------|
| 1 | Project Setup | None | ✅ COMPLETED |
| 2A | Bank (Enum) | None | ✅ COMPLETED |
| 2B | Category (Enum) | None | ✅ COMPLETED |
| 3 | Account | Bank | ✅ COMPLETED |
| 4 | Transaction | Account, Category | ✅ COMPLETED |
| 5 | CreditCard, Bill | Bank, Account | ✅ COMPLETED |
| 6 | CreditCardItem | CreditCardBill, Category | ✅ COMPLETED |
| 7 | Recurrence | Account, CreditCard, Category | ✅ COMPLETED |
| 8 | Transfer | Account | ✅ COMPLETED |
| 9 | Dashboard | All | ✅ COMPLETED |
| 10 | Polish | All | ⏳ **NEXT** |

**Current Status:** Phase 9 Complete ✅
**Next Phase:** Phase 10 - Polish & Extras
**Last Implementation:**
- Phase 9: Dashboard & Projections with comprehensive UI including:
  - General balance (current + projected after payments)
  - Monthly overview (income, expenses, balance)
  - Projection details breakdown
  - Accounts carousel
  - Upcoming bills and recurrences sections
- Phase 8: Complete transfers system with from/to accounts, optional fees

Each phase builds on the previous, and you can test thoroughly before moving forward.
