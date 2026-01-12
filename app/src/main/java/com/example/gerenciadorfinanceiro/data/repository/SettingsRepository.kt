package com.example.gerenciadorfinanceiro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val NOTIFICATION_PARSING_ENABLED = booleanPreferencesKey("notification_parsing_enabled")
    private val ITAU_ENABLED = booleanPreferencesKey("itau_enabled")
    private val NUBANK_ENABLED = booleanPreferencesKey("nubank_enabled")
    private val GOOGLE_WALLET_ENABLED = booleanPreferencesKey("google_wallet_enabled")
    private val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")

    fun isNotificationParsingEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[NOTIFICATION_PARSING_ENABLED] ?: false
        }

    suspend fun setNotificationParsingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_PARSING_ENABLED] = enabled
        }
    }

    fun isSourceEnabled(source: NotificationSource): Flow<Boolean> =
        dataStore.data.map { preferences ->
            when (source) {
                NotificationSource.ITAU -> preferences[ITAU_ENABLED] ?: true
                NotificationSource.NUBANK -> preferences[NUBANK_ENABLED] ?: true
                NotificationSource.GOOGLE_WALLET -> preferences[GOOGLE_WALLET_ENABLED] ?: true
            }
        }

    suspend fun setSourceEnabled(source: NotificationSource, enabled: Boolean) {
        dataStore.edit { preferences ->
            when (source) {
                NotificationSource.ITAU -> preferences[ITAU_ENABLED] = enabled
                NotificationSource.NUBANK -> preferences[NUBANK_ENABLED] = enabled
                NotificationSource.GOOGLE_WALLET -> preferences[GOOGLE_WALLET_ENABLED] = enabled
            }
        }
    }

    fun getLastSeenVersion(): Flow<String> =
        dataStore.data.map { preferences ->
            preferences[LAST_SEEN_VERSION] ?: ""
        }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SEEN_VERSION] = version
        }
    }
}
