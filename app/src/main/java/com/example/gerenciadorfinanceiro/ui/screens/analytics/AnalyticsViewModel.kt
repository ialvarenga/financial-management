package com.example.gerenciadorfinanceiro.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class AnalyticsUiState(
    val categoryBreakdown: CategoryBreakdown = CategoryBreakdown(),
    val timeSeriesData: TimeSeriesData = TimeSeriesData(),
    val paymentMethodBreakdown: PaymentMethodBreakdown = PaymentMethodBreakdown(),
    val accountBreakdown: AccountBreakdown = AccountBreakdown(),
    val creditCardUtilization: CreditCardUtilizationData = CreditCardUtilizationData(),
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val filterType: DataSourceFilter = DataSourceFilter.ALL,
    val timeRange: TimeRangeFilter = TimeRangeFilter.THREE_MONTHS,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCategoryAnalyticsUseCase: GetCategoryAnalyticsUseCase,
    private val getTimeSeriesDataUseCase: GetTimeSeriesDataUseCase,
    private val getPaymentMethodAnalyticsUseCase: GetPaymentMethodAnalyticsUseCase,
    private val getAccountBalanceAnalyticsUseCase: GetAccountBalanceAnalyticsUseCase,
    private val getCreditCardUtilizationUseCase: GetCreditCardUtilizationUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    private val _filterType = MutableStateFlow(DataSourceFilter.ALL)
    private val _timeRange = MutableStateFlow(TimeRangeFilter.THREE_MONTHS)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedMonth,
        _selectedYear,
        _filterType,
        _timeRange
    ) { month, year, filter, timeRange ->
        AnalyticsParams(month, year, filter, timeRange)
    }.flatMapLatest { params ->
        combine(
            getCategoryAnalyticsUseCase(params.month, params.year, params.filter),
            getTimeSeriesDataUseCase(params.month, params.year, params.timeRange),
            getPaymentMethodAnalyticsUseCase(params.month, params.year, params.filter),
            getAccountBalanceAnalyticsUseCase(),
            getCreditCardUtilizationUseCase()
        ) { categoryBreakdown, timeSeriesData, paymentMethodBreakdown, accountBreakdown, creditCardUtilization ->
            // Show/hide sections based on filter type for better data consistency
            val shouldShowTimeSeries = params.filter == DataSourceFilter.ALL || params.filter == DataSourceFilter.TRANSACTIONS
            val shouldShowAccountBalance = params.filter == DataSourceFilter.ALL || params.filter == DataSourceFilter.TRANSACTIONS
            val shouldShowCreditCardUtilization = params.filter == DataSourceFilter.ALL || params.filter == DataSourceFilter.CREDIT_CARDS

            AnalyticsUiState(
                categoryBreakdown = categoryBreakdown,
                timeSeriesData = if (shouldShowTimeSeries) timeSeriesData else TimeSeriesData(),
                paymentMethodBreakdown = paymentMethodBreakdown,
                accountBreakdown = if (shouldShowAccountBalance) accountBreakdown else AccountBreakdown(),
                creditCardUtilization = if (shouldShowCreditCardUtilization) creditCardUtilization else CreditCardUtilizationData(),
                selectedMonth = params.month,
                selectedYear = params.year,
                filterType = params.filter,
                timeRange = params.timeRange,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun selectPreviousMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = currentYear - 1
        } else {
            _selectedMonth.value = currentMonth - 1
        }
    }

    fun selectNextMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = currentYear + 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
    }

    fun setFilter(filter: DataSourceFilter) {
        _filterType.value = filter
    }

    fun setTimeRange(timeRange: TimeRangeFilter) {
        _timeRange.value = timeRange
    }
}

private data class AnalyticsParams(
    val month: Int,
    val year: Int,
    val filter: DataSourceFilter,
    val timeRange: TimeRangeFilter
)
