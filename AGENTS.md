# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

GerenciadorFinanceiro is a personal financial management Android application built with Jetpack Compose. It tracks bank accounts, transactions, and credit cards with a focus on Brazilian banking (supports 23+ Brazilian banks).

**Package:** `com.example.gerenciadorfinanceiro`
**Min SDK:** 26 (Android 8.0)

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build

# Install on device/emulator
./gradlew installDebug

# Generate APK
./gradlew assembleDebug
```

## Architecture

The project follows **Clean Architecture** with MVVM pattern:

```
data/
├── local/
│   ├── database/       # Room database, DAOs, Converters
│   └── entity/         # Room entities with annotations
└── repository/         # Data access layer (Repository pattern)

domain/
└── model/              # Business models (Enums: Bank, Category, etc.)

ui/
├── screens/            # Feature-based screens (each with ViewModel)
├── navigation/         # NavHost and Screen definitions
└── theme/              # Material 3 theming

di/                     # Hilt dependency injection modules

util/                   # Shared utilities (Currency, Date)

worker/                 # WorkManager background workers
```

### Key Architectural Decisions

1. **Bank and Category are Enums, NOT entities**
   - Defined in `domain/model/Bank.kt` and `domain/model/Category.kt`
   - No repositories needed (data is hardcoded)
   - TypeConverters handle Room database storage
   - Bank: 23 Brazilian banks (Nubank, Inter, Itaú, Bradesco, etc.)
   - Category: 25+ categories with type (INCOME/EXPENSE/BOTH) and colors

2. **Currency stored as Long (cents)**
   - All monetary values in database are in cents
   - Use `Long.toReais()` to format for display (Brazilian Real)
   - Use `String.toCents()` to parse user input
   - Located in `util/CurrencyUtils.kt`

3. **Room Database Setup**
   - Database name: `financial_app.db`
   - Current version: 11
   - Using `.fallbackToDestructiveMigration()` during development
   - All DAOs provided via Hilt in `di/DatabaseModule.kt`

4. **Navigation Pattern**
   - Sealed class `Screen` in `ui/navigation/AppNavigation.kt`
   - Bottom navigation with 4 tabs: Home, Transações, Contas, Cartões
   - Each add/edit screen uses `?id={id}` pattern with default value `-1`

5. **State Management**
   - ViewModels use `StateFlow<UiState>` pattern
   - Repository returns `Flow<T>` for reactive updates
   - UI observes with `collectAsState()`

6. **Background Processing with WorkManager**
   - `WorkManagerModule` in `di/` provides WorkManager instance
   - `BillClosureWorker` runs daily at midnight to process credit card bill closures
   - Uses `PeriodicWorkRequest` with 24-hour interval and 1-hour flex window
   - Constraints: requires battery not low
   - Work is scheduled with `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicates

See `app/financial-app-development-phases.md` for detailed implementation roadmap.

## Database Schema

**Entities:**
- `Account` - Bank accounts with balance (links to Bank enum)
- `Transaction` - Income/expense transactions (links to Account and Category enums)
- `CreditCard` - Credit cards with limit and payment info (links to Bank enum)
- `CreditCardBill` - Monthly credit card bills with status
- `CreditCardItem` - Individual purchases on credit cards (links to CreditCardBill, supports installments)
- `Recurrence` - Recurring transactions/expenses with frequency settings
- `Transfer` - Money transfers between accounts with optional fees
- `ProcessedNotification` - Tracks notifications already processed for auto-capture

**Helper Classes:**
- `TransactionWithAccount` - Transaction joined with its Account for display
- `TransferWithAccounts` - Transfer joined with source and destination accounts

**Type Converters** (in `Converters.kt`):
- All enums: Bank, Category, TransactionType, TransactionStatus, BillStatus, PaymentMethod, Frequency, NotificationSource

## Common Patterns

### Adding a New Screen

1. Create entity in `data/local/entity/`
2. Create DAO in `data/local/database/dao/`
3. Update `AppDatabase.kt` (add entity, abstract dao method)
4. Update `DatabaseModule.kt` (provide dao)
5. Create repository in `data/repository/`
6. Create ViewModels in `ui/screens/{feature}/`
7. Create Composable screens in `ui/screens/{feature}/`
8. Add routes to `AppNavigation.kt`
9. Increment database version if schema changed

### ViewModel Pattern

```kotlin
data class FeatureUiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    val uiState: StateFlow<FeatureUiState> = repository.getData()
        .map { FeatureUiState(data = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureUiState()
        )
}
```

### Add/Edit ViewModel Pattern

```kotlin
@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val itemId: Long = savedStateHandle.get<String>("itemId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (itemId > 0) loadItem()
    }
}
```

### WorkManager Worker Pattern

```kotlin
@HiltWorker
class FeatureWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: Repository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Perform background work
            Result.success()
        } catch (e: Exception) {
            Result.retry()  // or Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "feature_work"
        const val TAG = "feature_worker"
    }
}
```

## Dependency Injection

All dependencies managed with **Hilt**:
- `@HiltAndroidApp` on `FinancialApp` application class
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` on all ViewModels
- `@HiltWorker` on WorkManager workers
- DAOs provided in `DatabaseModule`
- WorkManager provided in `WorkManagerModule`
- Repositories are `@Singleton` with `@Inject constructor`

## Material 3 Theming

The app uses Material 3 design with:
- `FinancialAppTheme` wrapper
- Extended Material Icons for comprehensive icon set
- Bottom navigation for main app sections
- FABs for "add new" actions
- Cards for list items
- Dialogs for delete confirmations

## Testing Strategy

When adding tests:
- Unit tests for ViewModels and Repositories
- Use `TestDispatcher` for coroutines
- Mock repositories in ViewModel tests
- Use in-memory Room database for DAO tests

## Key Libraries & Versions

- **Kotlin**: 2.0.21
- **Compose BOM**: 2024.12.01
- **Room**: 2.6.1
- **Hilt**: 2.50
- **Navigation Compose**: 2.8.5
- **WorkManager**: 2.9.0
- **Coroutines**: 1.9.0
- **DataStore Preferences**: 1.1.1
- **ComposeCharts**: 0.0.13 (io.github.ehsannarmani)
- **Gson**: 2.10.1
- **Firebase Analytics**: via BOM 34.7.0

## Use Cases (Domain Layer)

Complex business logic is extracted into Use Cases in `domain/usecase/`:

**Transaction Use Cases:**
- `CreateTransactionUseCase` - Create transactions with account balance updates
- `CompleteTransactionUseCase` - Mark pending transactions as completed
- `UpdateTransactionUseCase` - Edit existing transactions with balance adjustments

**Credit Card Use Cases:**
- `CreateCreditCardPurchaseUseCase` - Add purchases to credit card bills
- `CreateInstallmentPurchaseUseCase` - Handle installment purchases across multiple bills
- `AddCreditCardItemUseCase` / `UpdateCreditCardItemUseCase` - CRUD for bill items
- `GetOrCreateBillUseCase` - Ensure bill exists for given month/year
- `CloseBillUseCase` - Close monthly bills for payment
- `ImportCsvBillUseCase` - Import bill items from CSV files

**Transfer Use Cases:**
- `ExecuteTransferUseCase` - Create transfer between accounts
- `CompleteTransferUseCase` - Mark transfer as completed with balance updates
- `DeleteTransferUseCase` - Delete transfer with proper balance rollback

**Analytics Use Cases:**
- `GetDashboardDataUseCase` / `GetDashboardSummaryUseCase` - Dashboard aggregations
- `GetCategoryAnalyticsUseCase` - Spending by category
- `GetMonthlyExpensesUseCase` - Monthly expense summaries
- `GetAccountBalanceAnalyticsUseCase` - Balance over time
- `GetCreditCardUtilizationUseCase` - Credit utilization metrics
- `GetPaymentMethodAnalyticsUseCase` - Payment method breakdown
- `GetTimeSeriesDataUseCase` - Time-based trend data

**Other Use Cases:**
- `ProcessNotificationUseCase` - Parse bank notifications into transactions
- `ConfirmRecurrencePaymentUseCase` - Convert recurrence into actual transaction
- `ExportBackupUseCase` / `ImportBackupUseCase` - Data backup functionality
- `GetBalanceAfterPaymentsUseCase` - Project balance after pending payments

## Services

**NotificationListenerService** (`service/FinancialNotificationListener.kt`):
- Listens to bank app notifications (Itaú, Nubank, Google Wallet)
- Requires `BIND_NOTIFICATION_LISTENER_SERVICE` permission
- Auto-creates transactions from notification parsing
- Can be enabled/disabled via settings

## Utility Functions

**CurrencyUtils.kt:**
- `Long.toReais()` - Converts cents to formatted "R$ 1.234,56"
- `String.toCents()` - Parses user input to cents (handles "R$ 1.234,56" format)

**DateUtils.kt:**
- `getMonthBounds(month, year)` - Get start/end epoch millis for a month
- `formatMonthYear(month, year)` - Format as "Janeiro de 2024"
- `Long.toLocalDate()` - Convert epoch millis to LocalDate
- `LocalDate.toEpochMilli()` - Convert LocalDate to epoch millis

## Android Best Practices Enforced

1. **No blocking calls on Main thread** - All DB operations via suspend functions or Flow
2. **Unidirectional data flow** - UI → ViewModel → Repository → DAO
3. **Single source of truth** - Room database is the source of truth
4. **Lifecycle awareness** - Using `WhileSubscribed(5000)` in stateIn
5. **Separation of concerns** - UI, ViewModel, Repository, Use Cases
6. **Immutable UI state** - Data classes with copy() for state updates
7. **Composition over inheritance** - Compose functions, not class hierarchies
8. **Handle configuration changes** - ViewModel survives config changes
9. **Avoid memory leaks** - ViewModelScope for coroutines in ViewModels
10. **Use DataStore for preferences** - Not SharedPreferences (`SettingsRepository`)

## Screen Feature Modules

Each feature follows consistent structure:
```
ui/screens/{feature}/
├── {Feature}Screen.kt         # List/main screen composable
├── {Feature}ViewModel.kt      # Main screen ViewModel
├── AddEdit{Feature}Screen.kt  # Add/Edit form composable
└── AddEdit{Feature}ViewModel.kt  # Form ViewModel
```

**Current Features:**
- `accounts/` - Bank account management
- `transactions/` - Income/expense tracking + transfers
- `creditcards/` - Credit card and bill management + CSV import
- `recurrences/` - Recurring transaction setup
- `dashboard/` - Home screen with summaries
- `analytics/` - Charts and reports
- `settings/` - App settings and notifications
- `mais/` - "More" menu with additional options

## Important Notes

- Never commit changes without reviewing the git status from development phases document
- Always increment database version when changing schema
- Use enum-based Bank and Category (not entity-based)
- All monetary values must be in cents in database
- Follow existing naming conventions for consistency
- Keep ViewModels lean - complex logic goes in use cases (when needed)
- Use git karma for the commit messages
- Don't reference Codex or any AI tools in commit messages or code comments
- I'm using the app already, so if you are doing a DB change, make sure you create a migration that preserves my data
