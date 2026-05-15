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
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        
        // --- Vibe & Engine Settings ---
        val KEY_AI_PRESET = stringPreferencesKey("ai_preset")
        val KEY_SCENE_ENERGY = intPreferencesKey("scene_energy")
        val KEY_UNPREDICTABILITY = intPreferencesKey("unpredictability_level")
        val KEY_CONTINUITY = intPreferencesKey("continuity_level")
        val KEY_CINEMATIC = intPreferencesKey("cinematic_level")
        val KEY_INTROSPECTION = intPreferencesKey("introspection_level")
        val KEY_MELANCHOLY = intPreferencesKey("melancholy_level")
        
        // --- API Keys ---
        val KEY_CUSTOM_API_KEYS = stringPreferencesKey("custom_api_keys") // JSON array
    }

    val fontSize: Flow<Float> = dataStore.data.map { it[KEY_FONT_SIZE] ?: 17f }
    val lineHeight: Flow<Float> = dataStore.data.map { it[KEY_LINE_HEIGHT] ?: 30f }
    val autoBackup: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_BACKUP] ?: true }
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }

    // --- Vibe & Engine Flows ---
    val aiPreset: Flow<String> = dataStore.data.map { it[KEY_AI_PRESET] ?: "Visual novel Hàn" }
    val sceneEnergy: Flow<Int> = dataStore.data.map { it[KEY_SCENE_ENERGY] ?: 1 } // 0: Tĩnh, 1: Nhẹ, 2: Căng ngầm, 3: Bùng nổ
    val unpredictabilityLevel: Flow<Int> = dataStore.data.map { it[KEY_UNPREDICTABILITY] ?: 1 } // 0: Đúng ý, 1: Cân bằng, 2: Khó đoán
    val continuityLevel: Flow<Int> = dataStore.data.map { it[KEY_CONTINUITY] ?: 1 } // 0: Nhẹ, 1: Cân bằng, 2: Chặt
    val cinematicLevel: Flow<Int> = dataStore.data.map { it[KEY_CINEMATIC] ?: 1 } // 0: Văn truyện, 1: Cân bằng, 2: Điện ảnh
    val introspectionLevel: Flow<Int> = dataStore.data.map { it[KEY_INTROSPECTION] ?: 1 } // 0: Ít, 1: Vừa, 2: Nhiều
    val melancholyLevel: Flow<Int> = dataStore.data.map { it[KEY_MELANCHOLY] ?: 0 } // 0: Ít, 1: Vừa, 2: Nhiều
    
    // --- API Keys Flow ---
    val customApiKeys: Flow<String> = dataStore.data.map { it[KEY_CUSTOM_API_KEYS] ?: "[]" }

    suspend fun setFontSize(size: Float) { dataStore.edit { it[KEY_FONT_SIZE] = size } }
    suspend fun setLineHeight(height: Float) { dataStore.edit { it[KEY_LINE_HEIGHT] = height } }
    suspend fun setAutoBackup(enabled: Boolean) { dataStore.edit { it[KEY_AUTO_BACKUP] = enabled } }
    suspend fun setFirstLaunchDone() { dataStore.edit { it[KEY_FIRST_LAUNCH] = false } }

    suspend fun setAiPreset(preset: String) { dataStore.edit { it[KEY_AI_PRESET] = preset } }
    suspend fun setSceneEnergy(level: Int) { dataStore.edit { it[KEY_SCENE_ENERGY] = level } }
    suspend fun setUnpredictability(level: Int) { dataStore.edit { it[KEY_UNPREDICTABILITY] = level } }
    suspend fun setContinuity(level: Int) { dataStore.edit { it[KEY_CONTINUITY] = level } }
    suspend fun setCinematic(level: Int) { dataStore.edit { it[KEY_CINEMATIC] = level } }
    suspend fun setIntrospection(level: Int) { dataStore.edit { it[KEY_INTROSPECTION] = level } }
    suspend fun setMelancholy(level: Int) { dataStore.edit { it[KEY_MELANCHOLY] = level } }
    
    suspend fun setCustomApiKeys(json: String) { dataStore.edit { it[KEY_CUSTOM_API_KEYS] = json } }
}
