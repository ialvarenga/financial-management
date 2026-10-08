package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.csv.CsvImportConfig
import com.example.gerenciadorfinanceiro.data.csv.CsvParseResult
import com.example.gerenciadorfinanceiro.data.csv.SkippedLine
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.model.CsvBillItem
import com.example.gerenciadorfinanceiro.domain.usecase.ImportCsvBillUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.LocalDate
import javax.inject.Inject

data class ImportCsvUiState(
    val creditCard: CreditCard? = null,
    val customConfig: CsvImportConfig = CsvImportConfig(),
    val skippedLines: List<SkippedLine> = emptyList(),
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
) {
    // The delimiter setting doesn't apply to Excel files
    val isSpreadsheet: Boolean
        get() = fileName?.endsWith(".xlsx", ignoreCase = true) == true
}

data class ImportSuccessInfo(
    val itemCount: Int,
    val totalAmount: Long,
    val duplicatesSkipped: Int = 0
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
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(ImportCsvUiState())
    val uiState: StateFlow<ImportCsvUiState> = _uiState.asStateFlow()

    init {
        loadCreditCard()
        loadCustomConfig()
    }

    private fun loadCustomConfig() {
        viewModelScope.launch {
            // Load once so the persisted config doesn't overwrite in-flight user edits
            val config = settingsRepository.getCsvImportConfig().first()
            _uiState.update { it.copy(customConfig = config) }
        }
    }

    fun updateCustomConfig(config: CsvImportConfig) {
        _uiState.update { it.copy(customConfig = config) }
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
    }
    
    fun parseFile(getInputStream: () -> InputStream?) {
        val inputStream = getInputStream() ?: run {
            _uiState.update { it.copy(errorMessage = "Não foi possível ler o arquivo") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, errorMessage = null) }
            
            val state = _uiState.value
            val config = state.customConfig
            val result = withContext(Dispatchers.IO) {
                importCsvBillUseCase.parsePreview(inputStream, config)
            }

            when (result) {
                is CsvParseResult.Success -> {
                    val allIndices = result.items.indices.toSet()
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            previewItems = result.items,
                            selectedItems = allIndices, // Select all by default
                            previewTotal = result.items.sumOf { item -> item.amount },
                            skippedLines = result.skippedLines,
                            step = ImportStep.PREVIEW
                        )
                    }
                    // Persist the config that just worked
                    launch { settingsRepository.setCsvImportConfig(config) }
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
                            importSuccess = ImportSuccessInfo(result.itemCount, result.totalAmount, result.duplicatesSkipped),
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
                        previewTotal = 0,
                        skippedLines = emptyList()
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
                customConfig = it.customConfig,
                selectedMonth = LocalDate.now().monthValue,
                selectedYear = LocalDate.now().year
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

