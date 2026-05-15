package com.novel.assistant.data.remote.ai

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class KeyGroup { MAIN, MEMORY, GENERATOR }

enum class KeyStatus { ACTIVE, COOLDOWN, DEAD, UNTESTED }

data class KeyState(
    val key: String,
    val group: KeyGroup,
    val status: KeyStatus = KeyStatus.UNTESTED,
    val cooldownUntil: Long = 0L,
    val isCustom: Boolean = false,
    val label: String = ""
)

@Singleton
class KeyRotationManager @Inject constructor() {

    private val _keyStates = MutableStateFlow<List<KeyState>>(emptyList())
    val keyStates: StateFlow<List<KeyState>> = _keyStates.asStateFlow()

    private val currentIndex = mutableMapOf<KeyGroup, Int>()
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder().build()

    companion object {
        private const val TAG = "KeyRotation"
        private const val COOLDOWN_MS = 60_000L // 60s cooldown
        private const val MAX_RETRIES = 5
    }

    /**
     * Khởi tạo danh sách keys (System keys từ BuildConfig).
     */
    suspend fun initialize(
        mainKeys: Array<String>,
        memoryKeys: Array<String>,
        generatorKeys: Array<String>
    ) = mutex.withLock {
        val newStates = mutableListOf<KeyState>()
        
        // Add System Keys
        mainKeys.distinct().forEachIndexed { i, k -> 
            if (k.isNotBlank()) newStates.add(KeyState(k, KeyGroup.MAIN, label = "System Main ${i+1}"))
        }
        val effMemoryKeys = memoryKeys.ifEmpty { mainKeys }
        effMemoryKeys.distinct().forEachIndexed { i, k -> 
            if (k.isNotBlank()) newStates.add(KeyState(k, KeyGroup.MEMORY, label = "System Memory ${i+1}"))
        }
        val effGenKeys = generatorKeys.ifEmpty { mainKeys }
        effGenKeys.distinct().forEachIndexed { i, k -> 
            if (k.isNotBlank()) newStates.add(KeyState(k, KeyGroup.GENERATOR, label = "System Gen ${i+1}"))
        }

        // Giữ lại custom keys cũ
        val customKeys = _keyStates.value.filter { it.isCustom }
        newStates.addAll(customKeys)

        val oldStates = _keyStates.value.associateBy { it.key }
        val mergedStates = newStates.map { newSt ->
            oldStates[newSt.key]?.copy(group = newSt.group, label = newSt.label, isCustom = newSt.isCustom) ?: newSt
        }

        _keyStates.value = mergedStates
        currentIndex[KeyGroup.MAIN] = 0
        currentIndex[KeyGroup.MEMORY] = 0
        currentIndex[KeyGroup.GENERATOR] = 0
        
        Log.d(TAG, "Initialized KeyManager with ${mergedStates.size} total keys.")
    }

    /**
     * Cập nhật danh sách Custom Keys từ DataStore
     */
    suspend fun updateCustomKeys(customKeys: List<String>) = mutex.withLock {
        val systemKeys = _keyStates.value.filter { !it.isCustom }
        val newCustomStates = customKeys.distinct().mapIndexed { i, k ->
            KeyState(k, KeyGroup.MAIN, isCustom = true, label = "Custom Key ${i+1}")
        }
        
        val merged = (systemKeys + newCustomStates).map { newSt ->
            _keyStates.value.find { it.key == newSt.key }?.copy(
                group = newSt.group, label = newSt.label, isCustom = newSt.isCustom
            ) ?: newSt
        }
        _keyStates.value = merged
        Log.d(TAG, "Updated custom keys. Total keys: ${merged.size}")
    }

    /**
     * Lấy key khả dụng tiếp theo. 
     * Nếu key bị Cooldown nhưng đã hết hạn -> tự động gỡ Cooldown thành ACTIVE.
     */
    suspend fun getNextKey(group: KeyGroup): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        
        // Tự động gỡ Cooldown
        _keyStates.update { currentList ->
            currentList.map {
                if (it.status == KeyStatus.COOLDOWN && now >= it.cooldownUntil) {
                    it.copy(status = KeyStatus.ACTIVE, cooldownUntil = 0L)
                } else it
            }
        }

        val keysInGroup = _keyStates.value.filter { it.group == group || it.group == KeyGroup.MAIN }
        if (keysInGroup.isEmpty()) return null

        val startIdx = currentIndex[group] ?: 0
        var attempts = 0

        while (attempts < keysInGroup.size) {
            val idx = (startIdx + attempts) % keysInGroup.size
            val keyState = keysInGroup[idx]

            if (keyState.status == KeyStatus.ACTIVE || keyState.status == KeyStatus.UNTESTED) {
                currentIndex[group] = (idx + 1) % keysInGroup.size
                return keyState.key
            }
            attempts++
        }

        // Fallback: Nếu tất cả đều Cooldown/Dead, lấy key Cooldown có thời gian ngắn nhất
        val bestCooldownKey = keysInGroup.filter { it.status == KeyStatus.COOLDOWN }
            .minByOrNull { it.cooldownUntil }
        
        if (bestCooldownKey != null) {
            Log.w(TAG, "All keys on cooldown, forcing to use least-cooled key.")
            return bestCooldownKey.key
        }

        return null
    }

    suspend fun getNextKeyWithFallback(primaryGroup: KeyGroup, fallbackGroup: KeyGroup = KeyGroup.MAIN): String? {
        getNextKey(primaryGroup)?.let { return it }
        return getNextKey(fallbackGroup)
    }

    /**
     * Khi gọi API thất bại, cập nhật trạng thái Key.
     */
    suspend fun markKeyFailed(key: String, errorCode: Int?) = mutex.withLock {
        _keyStates.update { currentList ->
            currentList.map { state ->
                if (state.key == key) {
                    val status = when (errorCode) {
                        400, 401, 403, 404 -> KeyStatus.DEAD
                        else -> KeyStatus.COOLDOWN
                    }
                    val cooldownTime = if (status == KeyStatus.COOLDOWN) {
                        System.currentTimeMillis() + (if (errorCode == 429) COOLDOWN_MS * 2 else COOLDOWN_MS)
                    } else 0L
                    
                    Log.w(TAG, "Key $key marked as $status (Error $errorCode)")
                    state.copy(status = status, cooldownUntil = cooldownTime)
                } else state
            }
        }
    }

    /**
     * Khi gọi API thành công, đánh dấu Key là ACTIVE.
     */
    suspend fun markKeySucceeded(key: String) = mutex.withLock {
        _keyStates.update { currentList ->
            currentList.map {
                if (it.key == key && it.status != KeyStatus.ACTIVE) {
                    it.copy(status = KeyStatus.ACTIVE, cooldownUntil = 0L)
                } else it
            }
        }
    }

    /**
     * Ping API (test siêu nhẹ ~1 token) để kiểm tra tất cả các key.
     */
    suspend fun validateAllKeys() {
        val currentKeys = _keyStates.value
        currentKeys.forEach { validateKey(it.key) }
    }

    /**
     * Ping API một key cụ thể.
     */
    suspend fun validateKey(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
            val payload = """
                {
                    "contents": [{"parts":[{"text":"ping"}]}],
                    "generationConfig": { "maxOutputTokens": 1 }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            response.close()

            if (code in 200..299) {
                markKeySucceeded(key)
                true
            } else {
                markKeyFailed(key, code)
                false
            }
        } catch (e: Exception) {
            markKeyFailed(key, 500)
            false
        }
    }

    fun getMaxRetries(): Int = MAX_RETRIES
}
