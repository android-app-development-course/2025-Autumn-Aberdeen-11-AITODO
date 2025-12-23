package io.project.finalwork.aitodo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.project.finalwork.aitodo.data.model.AIProviderConfig
import io.project.finalwork.aitodo.data.model.ChatSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class AISettingsRepository(private val context: Context) {
    private val gson = Gson()
    private val dataStore = context.dataStore

    companion object {
        private val PROVIDERS_KEY = stringPreferencesKey("providers_list")
        private val SETTINGS_KEY = stringPreferencesKey("chat_settings")
    }

    val providers: Flow<List<AIProviderConfig>> = dataStore.data
        .map { preferences ->
            val json = preferences[PROVIDERS_KEY]
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<AIProviderConfig>>() {}.type
                gson.fromJson(json, type)
            }
        }

    val chatSettings: Flow<ChatSettings> = dataStore.data
        .map { preferences ->
            val json = preferences[SETTINGS_KEY]
            if (json.isNullOrEmpty()) {
                ChatSettings()
            } else {
                gson.fromJson(json, ChatSettings::class.java)
            }
        }

    suspend fun saveProviders(providers: List<AIProviderConfig>) {
        dataStore.edit { preferences ->
            preferences[PROVIDERS_KEY] = gson.toJson(providers)
        }
    }

    suspend fun saveChatSettings(settings: ChatSettings) {
        dataStore.edit { preferences ->
            preferences[SETTINGS_KEY] = gson.toJson(settings)
        }
    }
}
