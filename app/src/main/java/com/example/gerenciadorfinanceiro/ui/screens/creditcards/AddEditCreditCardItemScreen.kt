package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gerenciadorfinanceiro.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCreditCardItemScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditCreditCardItemViewModel = hiltViewModel()
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
                title = { Text("Adicionar Item") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bill info
            uiState.bill?.let { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Fatura: ${bill.month}/${bill.year}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("Descrição") == true,
                singleLine = true
            )

            // Amount
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("Valor") == true,
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                singleLine = true,
                placeholder = { Text("0,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Category Dropdown
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    uiState.availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Installments Selector
            Text("Parcelas", style = MaterialTheme.typography.titleSmall)
            var expandedInstallments by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedInstallments,
                onExpandedChange = { expandedInstallments = it }
            ) {
                OutlinedTextField(
                    value = if (uiState.installments == 1) {
                        "À vista"
                    } else {
                        "${uiState.installments}x"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Número de parcelas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInstallments) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedInstallments,
                    onDismissRequest = { expandedInstallments = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("À vista") },
                        onClick = {
                            viewModel.onInstallmentsChange(1)
                            expandedInstallments = false
                        }
                    )
                    (2..12).forEach { installments ->
                        DropdownMenuItem(
                            text = { Text("${installments}x") },
                            onClick = {
                                viewModel.onInstallmentsChange(installments)
                                expandedInstallments = false
                            }
                        )
                    }
                }
            }

            if (uiState.installments > 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Esta compra será dividida em ${uiState.installments} parcelas nas próximas faturas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
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
                    Text("Adicionar")
                }
            }
        }
    }
}
