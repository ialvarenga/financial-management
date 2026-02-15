package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.domain.usecase.CloseBillUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetOrCreateBillUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class BillStatusFilter(val displayName: String) {
    ALL("Todas"),
    OPEN("Aberta"),
    CLOSED("Fechada"),
    PAID("Paga"),
    OVERDUE("Atrasada")
}

enum class BillSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

data class CreditCardDetailUiState(
    val card: CreditCard? = null,
    val currentBill: CreditCardBill? = null,
    val currentBillItems: List<CreditCardItem> = emptyList(),
    val billHistory: List<CreditCardBill> = emptyList(),
    val billItems: Map<Long, List<CreditCardItem>> = emptyMap(),  // Map of bill ID to items
    val usedLimit: Long = 0,  // Total of unpaid bills (in cents)
    val availableLimit: Long = 0,  // creditLimit - usedLimit (in cents)
    val statusFilter: BillStatusFilter = BillStatusFilter.ALL,
    val sortOrder: BillSortOrder = BillSortOrder.NEWEST_FIRST,
    val expandedBillIds: Set<Long> = emptySet(),
    val isCurrentBillExpanded: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreditCardDetailViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val billRepository: CreditCardBillRepository,
    private val itemRepository: CreditCardItemRepository,
    private val accountRepository: com.example.gerenciadorfinanceiro.data.repository.AccountRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val closeBillUseCase: CloseBillUseCase,
    private val markBillAsPaidUseCase: com.example.gerenciadorfinanceiro.domain.usecase.MarkBillAsPaidUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1

    // Flow of active accounts for bill payment selection
    val activeAccounts = accountRepository.getActiveAccounts()

    // Filter and sort state
    private val _statusFilter = MutableStateFlow(BillStatusFilter.ALL)
    private val _sortOrder = MutableStateFlow(BillSortOrder.NEWEST_FIRST)

    // Expansion state (persists across navigation)
    private val _expandedBillIds = MutableStateFlow(emptySet<Long>())
    private val _isCurrentBillExpanded = MutableStateFlow(false)

    val uiState: StateFlow<CreditCardDetailUiState> = combine(
        creditCardRepository.getByIdFlow(cardId),
        billRepository.getBillsByCard(cardId),
        itemRepository.getTotalUnpaidItemsByCard(cardId),
        _statusFilter,
        _sortOrder,
        _expandedBillIds,
        _isCurrentBillExpanded
    ) { values ->
        val card = values[0] as CreditCard?
        val bills = values[1] as List<CreditCardBill>
        val usedLimit = values[2] as Long
        val statusFilter = values[3] as BillStatusFilter
        val sortOrder = values[4] as BillSortOrder
        val expandedBillIds = values[5] as Set<Long>
        val isCurrentBillExpanded = values[6] as Boolean
        val now = LocalDate.now()
        val currentMonthBill = bills.firstOrNull {
            it.month == now.monthValue && it.year == now.year
        }

        // If current month's bill is closed and paid, show the next month's bill
        val currentBill = if (currentMonthBill?.status == BillStatus.PAID) {
            val nextMonth = now.plusMonths(1)
            bills.firstOrNull {
                it.month == nextMonth.monthValue && it.year == nextMonth.year
            }
        } else {
            currentMonthBill
        }

        // Calculate available limit
        val availableLimit = (card?.creditLimit ?: 0) - usedLimit

        // Apply status filter
        val filteredBills = when (statusFilter) {
            BillStatusFilter.ALL -> bills
            BillStatusFilter.OPEN -> bills.filter { it.status == BillStatus.OPEN }
            BillStatusFilter.CLOSED -> bills.filter { it.status == BillStatus.CLOSED }
            BillStatusFilter.PAID -> bills.filter { it.status == BillStatus.PAID }
            BillStatusFilter.OVERDUE -> bills.filter { it.status == BillStatus.OVERDUE }
        }

        // Apply sort order
        val sortedBills = when (sortOrder) {
            BillSortOrder.NEWEST_FIRST -> filteredBills.sortedByDescending { it.year * 100 + it.month }
            BillSortOrder.OLDEST_FIRST -> filteredBills.sortedBy { it.year * 100 + it.month }
        }

        CreditCardDetailUiState(
            card = card,
            currentBill = currentBill,
            currentBillItems = emptyList(),
            billHistory = sortedBills,
            billItems = emptyMap(),
            usedLimit = usedLimit,
            availableLimit = availableLimit.coerceAtLeast(0),
            statusFilter = statusFilter,
            sortOrder = sortOrder,
            expandedBillIds = expandedBillIds,
            isCurrentBillExpanded = isCurrentBillExpanded,
            isLoading = false
        )
    }.flatMapLatest { state ->
        // Load items for all bills
        val billItemFlows = state.billHistory.map { bill ->
            itemRepository.getItemsByBill(bill.id).map { items ->
                bill.id to items
            }
        }

        if (billItemFlows.isNotEmpty()) {
            combine(billItemFlows) { billItemPairs ->
                val billItemsMap = billItemPairs.toMap()
                val currentBillItems = state.currentBill?.let { billItemsMap[it.id] } ?: emptyList()

                state.copy(
                    currentBillItems = currentBillItems,
                    billItems = billItemsMap
                )
            }
        } else {
            flowOf(state)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardDetailUiState()
    )

    init {
        // Ensure current month bill exists
        viewModelScope.launch {
            ensureCurrentBillExists()
        }
    }

    private suspend fun ensureCurrentBillExists() {
        if (cardId > 0) {
            try {
                getOrCreateBillUseCase.getCurrentMonthBill(cardId)
            } catch (e: Exception) {
                // Card might not exist yet, ignore
            }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            uiState.value.card?.let { card ->
                creditCardRepository.delete(card)
            }
        }
    }

    fun deleteItem(item: CreditCardItem) {
        viewModelScope.launch {
            // Collect all bill IDs that will be affected
            val affectedBillIds = mutableSetOf<Long>()
            affectedBillIds.add(item.creditCardBillId)

            if (item.installmentGroupId != null) {
                // Get all items in the installment group to find affected bills
                val groupItems = itemRepository.getItemsByInstallmentGroupSync(item.installmentGroupId)
                groupItems.forEach { affectedBillIds.add(it.creditCardBillId) }

                // Delete all installments in the group
                itemRepository.deleteByInstallmentGroup(item.installmentGroupId)
            } else {
                // Delete single item
                itemRepository.delete(item)
            }

            // Update totals for all affected bills
            for (billId in affectedBillIds) {
                val newTotal = itemRepository.getTotalAmountByBill(billId)
                billRepository.updateTotalAmount(billId, newTotal)
            }
        }
    }

    fun markBillAsPaid(billId: Long, accountId: Long) {
        viewModelScope.launch {
            markBillAsPaidUseCase(billId, accountId)
        }
    }

    fun closeBill(billId: Long) {
        viewModelScope.launch {
            uiState.value.card?.let { card ->
                closeBillUseCase.closeBillManually(billId, card)
            }
        }
    }

    fun setStatusFilter(filter: BillStatusFilter) {
        _statusFilter.value = filter
    }

    fun setSortOrder(order: BillSortOrder) {
        _sortOrder.value = order
    }

    fun toggleCurrentBillExpanded() {
        _isCurrentBillExpanded.value = !_isCurrentBillExpanded.value
    }

    fun toggleBillExpanded(billId: Long) {
        _expandedBillIds.value = if (_expandedBillIds.value.contains(billId)) {
            _expandedBillIds.value - billId
        } else {
            _expandedBillIds.value + billId
        }
    }
}
