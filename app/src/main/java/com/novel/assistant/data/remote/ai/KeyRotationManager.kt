package com.novel.assistant.data.remote.ai

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class KeyGroup { MAIN, MEMORY, GENERATOR }

@Singleton
class KeyRotationManager @Inject constructor() {

    private val keyPools = ConcurrentHashMap<KeyGroup, MutableList<String>>()
    private val currentIndex = ConcurrentHashMap<KeyGroup, Int>()
    private val cooldownUntil = ConcurrentHashMap<String, Long>() // key -> timestamp
    private val usageCount = ConcurrentHashMap<String, Int>()
    private val failureCount = ConcurrentHashMap<String, Int>()
    private val mutex = Mutex()

    companion object {
        private const val TAG = "KeyRotation"
        private const val COOLDOWN_MS = 60_000L // 60s cooldown after rate limit
        private const val MAX_RETRIES = 5
    }

    fun initialize(mainKeys: Array<String>, memoryKeys: Array<String>, generatorKeys: Array<String>) {
        keyPools[KeyGroup.MAIN] = mainKeys.distinct().toMutableList()
        keyPools[KeyGroup.MEMORY] = memoryKeys.ifEmpty { mainKeys }.distinct().toMutableList()
        keyPools[KeyGroup.GENERATOR] = generatorKeys.ifEmpty { mainKeys }.distinct().toMutableList()
        currentIndex[KeyGroup.MAIN] = 0
        currentIndex[KeyGroup.MEMORY] = 0
        currentIndex[KeyGroup.GENERATOR] = 0
        Log.d(
            TAG,
            "Initialized: MAIN=${keyPools[KeyGroup.MAIN]?.size ?: 0}, " +
                "MEMORY=${keyPools[KeyGroup.MEMORY]?.size ?: 0}, " +
                "GEN=${keyPools[KeyGroup.GENERATOR]?.size ?: 0}"
        )
    }

    suspend fun getNextKey(group: KeyGroup): String? = mutex.withLock {
        val keys = keyPools[group] ?: return null
        if (keys.isEmpty()) return null

        val now = System.currentTimeMillis()
        val startIdx = currentIndex[group] ?: 0
        var attempts = 0

        while (attempts < keys.size) {
            val idx = (startIdx + attempts) % keys.size
            val key = keys[idx]
            val cooldown = cooldownUntil[key] ?: 0L

            if (now >= cooldown) {
                currentIndex[group] = (idx + 1) % keys.size
                usageCount[key] = (usageCount[key] ?: 0) + 1
                return key
            }
            attempts++
        }

        // All keys on cooldown, return the one with shortest remaining cooldown
        val bestKey = keys.minByOrNull { cooldownUntil[it] ?: 0L }
        Log.w(TAG, "All ${group.name} keys on cooldown, using least-cooled key")
        return bestKey
    }

    suspend fun getNextKeyWithFallback(primaryGroup: KeyGroup, fallbackGroup: KeyGroup = KeyGroup.MAIN): String? {
        return getNextKey(primaryGroup) ?: getNextKey(fallbackGroup)
    }

    suspend fun markKeyFailed(key: String, errorCode: Int?) = mutex.withLock {
        val failures = (failureCount[key] ?: 0) + 1
        failureCount[key] = failures

        val cooldownDuration = when (errorCode) {
            429 -> COOLDOWN_MS * 2
            500, 503 -> COOLDOWN_MS
            401, 403 -> COOLDOWN_MS * 10
            else -> (COOLDOWN_MS / 2) * failures.coerceAtMost(4)
        }
        cooldownUntil[key] = System.currentTimeMillis() + cooldownDuration
        Log.w(TAG, "Key cooldown (${errorCode ?: "unknown"}): ${key.take(10)}... for ${cooldownDuration / 1000}s")
    }

    suspend fun markKeySucceeded(key: String) = mutex.withLock {
        failureCount.remove(key)
    }

    fun getMaxRetries(): Int = MAX_RETRIES

    fun getKeyCount(group: KeyGroup): Int = keyPools[group]?.size ?: 0
}
