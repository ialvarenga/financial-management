package com.example.gerenciadorfinanceiro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gerenciadorfinanceiro.data.csv.CsvImportConfig
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

    private val CSV_DATE_COL = intPreferencesKey("csv_custom_date_col")
    private val CSV_DESC_COL = intPreferencesKey("csv_custom_desc_col")
    private val CSV_AMOUNT_COL = intPreferencesKey("csv_custom_amount_col")
    private val CSV_SKIP_ROWS = intPreferencesKey("csv_custom_skip_rows")
    private val CSV_DELIMITER = stringPreferencesKey("csv_custom_delimiter")
    private val CSV_DATE_PATTERN = stringPreferencesKey("csv_custom_date_pattern")
    private val CSV_DECIMAL_STYLE = stringPreferencesKey("csv_custom_decimal_style")
    private val CSV_NEGATIVE_HANDLING = stringPreferencesKey("csv_custom_negative_handling")

    fun getCsvImportConfig(): Flow<CsvImportConfig> =
        dataStore.data.map { preferences ->
            val defaults = CsvImportConfig()
            CsvImportConfig(
                dateColumn = preferences[CSV_DATE_COL] ?: defaults.dateColumn,
                descriptionColumn = preferences[CSV_DESC_COL] ?: defaults.descriptionColumn,
                amountColumn = preferences[CSV_AMOUNT_COL] ?: defaults.amountColumn,
                skipRows = preferences[CSV_SKIP_ROWS] ?: defaults.skipRows,
                delimiter = preferences[CSV_DELIMITER].toEnum(defaults.delimiter),
                datePattern = preferences[CSV_DATE_PATTERN] ?: defaults.datePattern,
                decimalStyle = preferences[CSV_DECIMAL_STYLE].toEnum(defaults.decimalStyle),
                negativeHandling = preferences[CSV_NEGATIVE_HANDLING].toEnum(defaults.negativeHandling)
            )
        }

    suspend fun setCsvImportConfig(config: CsvImportConfig) {
        dataStore.edit { preferences ->
            preferences[CSV_DATE_COL] = config.dateColumn
            preferences[CSV_DESC_COL] = config.descriptionColumn
            preferences[CSV_AMOUNT_COL] = config.amountColumn
            preferences[CSV_SKIP_ROWS] = config.skipRows
            preferences[CSV_DELIMITER] = config.delimiter.name
            preferences[CSV_DATE_PATTERN] = config.datePattern
            preferences[CSV_DECIMAL_STYLE] = config.decimalStyle.name
            preferences[CSV_NEGATIVE_HANDLING] = config.negativeHandling.name
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

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
