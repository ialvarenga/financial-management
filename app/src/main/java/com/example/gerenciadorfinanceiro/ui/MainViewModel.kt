package com.example.gerenciadorfinanceiro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.BuildConfig
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.model.ReleaseNote
import com.example.gerenciadorfinanceiro.domain.model.ReleaseNotes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _showWhatsNew = MutableStateFlow<ReleaseNote?>(null)
    val showWhatsNew: StateFlow<ReleaseNote?> = _showWhatsNew.asStateFlow()

    init {
        checkForNewVersion()
    }

    private fun checkForNewVersion() {
        viewModelScope.launch {
            val lastSeenVersion = settingsRepository.getLastSeenVersion().first()
            val currentVersion = BuildConfig.VERSION_NAME

            if (lastSeenVersion != currentVersion) {
                val releaseNote = ReleaseNotes.getNoteForVersion(currentVersion)
                if (releaseNote != null) {
                    _showWhatsNew.value = releaseNote
                } else {
                    settingsRepository.setLastSeenVersion(currentVersion)
                }
            }
        }
    }

    fun dismissWhatsNew() {
        viewModelScope.launch {
            settingsRepository.setLastSeenVersion(BuildConfig.VERSION_NAME)
            _showWhatsNew.value = null
        }
    }
}
