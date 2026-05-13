package com.novel.assistant.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.novel.assistant.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novel_settings")

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_FONT_SIZE = floatPreferencesKey("font_size")
        val KEY_LINE_HEIGHT = floatPreferencesKey("line_height")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val fontSize: Flow<Float> = dataStore.data.map { it[KEY_FONT_SIZE] ?: 17f }
    val lineHeight: Flow<Float> = dataStore.data.map { it[KEY_LINE_HEIGHT] ?: 30f }
    val modelName: Flow<String> = dataStore.data.map { it[KEY_MODEL_NAME] ?: BuildConfig.GEMINI_MODEL_MAIN }
    val autoBackup: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_BACKUP] ?: true }
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setLineHeight(height: Float) {
        dataStore.edit { it[KEY_LINE_HEIGHT] = height }
    }

    suspend fun setModelName(name: String) {
        dataStore.edit { it[KEY_MODEL_NAME] = name }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_BACKUP] = enabled }
    }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }
}
