package com.aifinance.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

interface UserPreferencesRepository {
    val themeMode: Flow<AppThemeMode>
    suspend fun setThemeMode(mode: AppThemeMode)

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
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val themeMode: Flow<AppThemeMode> = dataStore.data.map { preferences ->
        val raw = preferences[THEME_MODE_KEY] ?: AppThemeMode.LIGHT.name
        runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.LIGHT)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
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

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val NICKNAME_KEY = stringPreferencesKey("nickname")
        val GENDER_KEY = stringPreferencesKey("gender")
        val PHONE_KEY = stringPreferencesKey("phone")
        val AVATAR_URI_KEY = stringPreferencesKey("avatar_uri")
    }
}