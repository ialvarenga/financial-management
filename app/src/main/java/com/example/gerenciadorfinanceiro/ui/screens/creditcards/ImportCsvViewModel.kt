package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.csv.CsvFormat
import com.example.gerenciadorfinanceiro.data.csv.CsvParseResult
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import com.example.gerenciadorfinanceiro.domain.usecase.ImportCsvBillUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import javax.inject.Inject

data class ImportCsvUiState(
    val creditCard: CreditCard? = null,
    val selectedFormat: CsvFormat = CsvFormat.GENERIC,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val previewItems: List<CsvBillItem> = emptyList(),
    val selectedItems: Set<Int> = emptySet(), // Indices of selected items
    val previewTotal: Long = 0,
    val isLoading: Boolean = false,
    val isParsing: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val importSuccess: ImportSuccessInfo? = null,
    val step: ImportStep = ImportStep.SELECT_FILE
)

data class ImportSuccessInfo(
    val itemCount: Int,
    val totalAmount: Long
)

enum class ImportStep {
    SELECT_FILE,
    PREVIEW,
    SUCCESS
}

@HiltViewModel
class ImportCsvViewModel @Inject constructor(
    private val importCsvBillUseCase: ImportCsvBillUseCase,
    private val creditCardRepository: CreditCardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1
    
    private val _uiState = MutableStateFlow(ImportCsvUiState())
    val uiState: StateFlow<ImportCsvUiState> = _uiState.asStateFlow()
    
    init {
        loadCreditCard()
    }
    
    private fun loadCreditCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val card = creditCardRepository.getById(cardId)
            _uiState.update { 
                it.copy(
                    creditCard = card,
                    isLoading = false
                )
            }
        }
    }
    
    fun setFormat(format: CsvFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
        // Re-parse if file is selected
        if (_uiState.value.fileUri != null) {
            _uiState.value.fileUri?.let { /* Will need to re-parse with getInputStream */ }
        }
    }
    
    fun setMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
    }
    
    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }
    
    fun selectPreviousMonth() {
        val current = _uiState.value
        if (current.selectedMonth == 1) {
            _uiState.update { it.copy(selectedMonth = 12, selectedYear = current.selectedYear - 1) }
        } else {
            _uiState.update { it.copy(selectedMonth = current.selectedMonth - 1) }
        }
    }
    
    fun selectNextMonth() {
        val current = _uiState.value
        if (current.selectedMonth == 12) {
            _uiState.update { it.copy(selectedMonth = 1, selectedYear = current.selectedYear + 1) }
        } else {
            _uiState.update { it.copy(selectedMonth = current.selectedMonth + 1) }
        }
    }
    
    fun onFileSelected(uri: Uri, fileName: String?, getInputStream: () -> InputStream?) {
        _uiState.update { 
            it.copy(
                fileUri = uri, 
                fileName = fileName,
                errorMessage = null,
                previewItems = emptyList()
            )
        }
        
        // Try to auto-detect format
        val detectedFormat = getInputStream()?.let { stream ->
            importCsvBillUseCase.detectFormat(stream)
        }
        
        if (detectedFormat != null) {
            _uiState.update { it.copy(selectedFormat = detectedFormat) }
        }
    }
    
    fun parseFile(getInputStream: () -> InputStream?) {
        val inputStream = getInputStream() ?: run {
            _uiState.update { it.copy(errorMessage = "Não foi possível ler o arquivo") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, errorMessage = null) }
            
            val result = importCsvBillUseCase.parsePreview(inputStream, _uiState.value.selectedFormat)
            
            when (result) {
                is CsvParseResult.Success -> {
                    val allIndices = result.items.indices.toSet()
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            previewItems = result.items,
                            selectedItems = allIndices, // Select all by default
                            previewTotal = result.items.sumOf { item -> item.amount },
                            step = ImportStep.PREVIEW
                        )
                    }
                }
                is CsvParseResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isParsing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun toggleItemSelection(index: Int) {
        _uiState.update { state ->
            val newSelection = if (index in state.selectedItems) {
                state.selectedItems - index
            } else {
                state.selectedItems + index
            }
            val newTotal = state.previewItems
                .filterIndexed { i, _ -> i in newSelection }
                .sumOf { it.amount }
            state.copy(
                selectedItems = newSelection,
                previewTotal = newTotal
            )
        }
    }

    fun selectAllItems() {
        _uiState.update { state ->
            val allIndices = state.previewItems.indices.toSet()
            state.copy(
                selectedItems = allIndices,
                previewTotal = state.previewItems.sumOf { it.amount }
            )
        }
    }

    fun deselectAllItems() {
        _uiState.update { state ->
            state.copy(
                selectedItems = emptySet(),
                previewTotal = 0
            )
        }
    }

    fun importBill() {
        val state = _uiState.value

        // Filter to only selected items
        val selectedItemsList = state.previewItems
            .filterIndexed { index, _ -> index in state.selectedItems }

        if (selectedItemsList.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Nenhum item selecionado para importar") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            
            val result = importCsvBillUseCase.importParsedItems(
                items = selectedItemsList,
                creditCardId = cardId,
                month = state.selectedMonth,
                year = state.selectedYear
            )
            
            when (result) {
                is ImportResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isImporting = false,
                            importSuccess = ImportSuccessInfo(result.itemCount, result.totalAmount),
                            step = ImportStep.SUCCESS
                        )
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isImporting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun goBack() {
        val currentStep = _uiState.value.step
        when (currentStep) {
            ImportStep.PREVIEW -> {
                _uiState.update { 
                    it.copy(
                        step = ImportStep.SELECT_FILE,
                        previewItems = emptyList(),
                        selectedItems = emptySet(),
                        previewTotal = 0
                    )
                }
            }
            else -> { /* Can't go back from first or last step */ }
        }
    }
    
    fun reset() {
        _uiState.update { 
            ImportCsvUiState(
                creditCard = it.creditCard,
                selectedMonth = LocalDate.now().monthValue,
                selectedYear = LocalDate.now().year
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

