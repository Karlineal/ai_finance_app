package com.aifinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class PangkaReplyStyle {
    BALANCED,
    CONCISE,
    DETAILED,
}

data class SettingsPreferences(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val monthlyStatsStartDay: Int = 1,
    val pangkaReplyStyle: PangkaReplyStyle = PangkaReplyStyle.BALANCED,
    val showRecordImages: Boolean = true,
    val showLocationInRecords: Boolean = true,
)

interface UserPreferencesRepository {
    val themeMode: Flow<AppThemeMode>
    val settingsPreferences: Flow<SettingsPreferences>
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setMonthlyStatsStartDay(day: Int)
    suspend fun setPangkaReplyStyle(style: PangkaReplyStyle)
    suspend fun setShowRecordImages(enabled: Boolean)
    suspend fun setShowLocationInRecords(enabled: Boolean)

    val isLoggedIn: Flow<Boolean>
    suspend fun setLoggedIn(loggedIn: Boolean)

    val nickname: Flow<String>
    suspend fun setNickname(nickname: String)

    val gender: Flow<String>
    suspend fun setGender(gender: String)

    val phone: Flow<String>
    suspend fun setPhone(phone: String)

    val avatarUri: Flow<String>
    suspend fun setAvatarUri(uri: String)

    val email: Flow<String>
    suspend fun setEmail(email: String)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val themeMode: Flow<AppThemeMode> = dataStore.data.map { preferences ->
        preferences.themeMode
    }

    override val settingsPreferences: Flow<SettingsPreferences> = dataStore.data.map { preferences ->
        SettingsPreferences(
            themeMode = preferences.themeMode,
            monthlyStatsStartDay = (preferences[MONTHLY_STATS_START_DAY_KEY] ?: 1).coerceIn(1, 28),
            pangkaReplyStyle = preferences.pangkaReplyStyle,
            showRecordImages = preferences[SHOW_RECORD_IMAGES_KEY] ?: true,
            showLocationInRecords = preferences[SHOW_LOCATION_IN_RECORDS_KEY] ?: true,
        )
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    override suspend fun setMonthlyStatsStartDay(day: Int) {
        dataStore.edit { preferences ->
            preferences[MONTHLY_STATS_START_DAY_KEY] = day.coerceIn(1, 28)
        }
    }

    override suspend fun setPangkaReplyStyle(style: PangkaReplyStyle) {
        dataStore.edit { preferences ->
            preferences[PANGKA_REPLY_STYLE_KEY] = style.name
        }
    }

    override suspend fun setShowRecordImages(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_RECORD_IMAGES_KEY] = enabled
        }
    }

    override suspend fun setShowLocationInRecords(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_LOCATION_IN_RECORDS_KEY] = enabled
        }
    }

    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    override suspend fun setLoggedIn(loggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = loggedIn
        }
    }

    override val nickname: Flow<String> = dataStore.data.map { it[NICKNAME_KEY] ?: "小皮皮" }
    override suspend fun setNickname(nickname: String) {
        dataStore.edit { it[NICKNAME_KEY] = nickname }
    }

    override val gender: Flow<String> = dataStore.data.map { it[GENDER_KEY] ?: "未填写" }
    override suspend fun setGender(gender: String) {
        dataStore.edit { it[GENDER_KEY] = gender }
    }

    override val phone: Flow<String> = dataStore.data.map { it[PHONE_KEY] ?: "185****2721" }
    override suspend fun setPhone(phone: String) {
        dataStore.edit { it[PHONE_KEY] = phone }
    }

    override val avatarUri: Flow<String> = dataStore.data.map { it[AVATAR_URI_KEY] ?: "" }
    override suspend fun setAvatarUri(uri: String) {
        dataStore.edit { it[AVATAR_URI_KEY] = uri }
    }

    override val email: Flow<String> = dataStore.data.map { it[EMAIL_KEY] ?: "" }
    override suspend fun setEmail(email: String) {
        dataStore.edit { it[EMAIL_KEY] = email }
    }

    private val Preferences.themeMode: AppThemeMode
        get() {
            val raw = this[THEME_MODE_KEY] ?: AppThemeMode.LIGHT.name
            return runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.LIGHT)
        }

    private val Preferences.pangkaReplyStyle: PangkaReplyStyle
        get() {
            val raw = this[PANGKA_REPLY_STYLE_KEY] ?: PangkaReplyStyle.BALANCED.name
            return runCatching { PangkaReplyStyle.valueOf(raw) }.getOrDefault(PangkaReplyStyle.BALANCED)
        }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val MONTHLY_STATS_START_DAY_KEY = intPreferencesKey("monthly_stats_start_day")
        val PANGKA_REPLY_STYLE_KEY = stringPreferencesKey("pangka_reply_style")
        val SHOW_RECORD_IMAGES_KEY = booleanPreferencesKey("show_record_images")
        val SHOW_LOCATION_IN_RECORDS_KEY = booleanPreferencesKey("show_location_in_records")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val NICKNAME_KEY = stringPreferencesKey("nickname")
        val GENDER_KEY = stringPreferencesKey("gender")
        val PHONE_KEY = stringPreferencesKey("phone")
        val AVATAR_URI_KEY = stringPreferencesKey("avatar_uri")
        val EMAIL_KEY = stringPreferencesKey("email")
    }
}
