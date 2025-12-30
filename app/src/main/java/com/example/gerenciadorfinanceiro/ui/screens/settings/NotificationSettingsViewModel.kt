package com.example.gerenciadorfinanceiro.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.util.isNotificationAccessGranted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val isEnabled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val itauEnabled: Boolean = true,
    val nubankEnabled: Boolean = true,
    val googleWalletEnabled: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _permissionGranted = MutableStateFlow(isNotificationAccessGranted(application))
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        settingsRepository.isNotificationParsingEnabled(),
        _permissionGranted,
        settingsRepository.isSourceEnabled(NotificationSource.ITAU),
        settingsRepository.isSourceEnabled(NotificationSource.NUBANK),
        settingsRepository.isSourceEnabled(NotificationSource.GOOGLE_WALLET)
    ) { enabled, permission, itau, nubank, wallet ->
        NotificationSettingsUiState(
            isEnabled = enabled,
            isPermissionGranted = permission,
            itauEnabled = itau,
            nubankEnabled = nubank,
            googleWalletEnabled = wallet
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationSettingsUiState()
    )

    fun toggleFeature(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationParsingEnabled(enabled)
        }
    }

    fun toggleSource(source: NotificationSource, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSourceEnabled(source, enabled)
        }
    }

    fun refreshPermissionStatus() {
        _permissionGranted.value = isNotificationAccessGranted(getApplication())
    }
}
