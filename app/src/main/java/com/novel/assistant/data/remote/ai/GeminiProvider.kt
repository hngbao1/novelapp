package com.novel.assistant.data.remote.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor(
    private val keyRotationManager: KeyRotationManager
) : AiProvider {

    companion object {
        private const val TAG = "GeminiProvider"
    }

    private var generationModelName: String = "gemini-2.0-flash"
    private var memoryModelName: String = "gemini-2.0-flash"
    private var maxOutputTokensMain: Int = 8192
    private var maxOutputTokensMemory: Int = 2048
    private val gson = Gson()

    fun setModelName(name: String) {
        generationModelName = name
    }

    fun configure(
        generationModel: String,
        memoryModel: String,
        mainTokens: Int,
        memoryTokens: Int
    ) {
        generationModelName = generationModel
        memoryModelName = memoryModel
        maxOutputTokensMain = mainTokens
        maxOutputTokensMemory = memoryTokens
    }

    private fun createModel(
        apiKey: String,
        modelName: String,
        maxTokens: Int,
        systemInstruction: String? = null
    ): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.85f
                topP = 0.95f
                topK = 40
                maxOutputTokens = maxTokens
            },
            systemInstruction = if (systemInstruction != null) {
                content { text(systemInstruction) }
            } else null
        )
    }

    override suspend fun generateScene(request: SceneRequest): Flow<String> = flow {
        val fullSystemPrompt = buildString {
            appendLine(request.systemContext)
            if (request.characterContext.isNotBlank()) {
                appendLine()
                appendLine(request.characterContext)
            }
            if (request.storyContext.isNotBlank()) {
                appendLine()
                appendLine(request.storyContext)
            }
            if (request.styleContext.isNotBlank()) {
                appendLine()
                appendLine(request.styleContext)
            }
        }

        val userMessage = if (request.isRoleplay) {
            "[${request.roleplayCharacterName}]: ${request.userPrompt}"
        } else {
            "Ý tưởng scene:\n${request.userPrompt}\n\nHãy viết thành scene truyện hoàn chỉnh."
        }

        var lastError: Exception? = null
        val maxRetries = keyRotationManager.getMaxRetries()

        for (attempt in 0 until maxRetries) {
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.GENERATOR)
                ?: throw Exception("Không có API key nào khả dụng")

            try {
                val model = createModel(
                    apiKey = apiKey,
                    modelName = generationModelName,
                    maxTokens = maxOutputTokensMain,
                    systemInstruction = fullSystemPrompt
                )
                val stream = model.generateContentStream(userMessage)

                stream.collect { chunk ->
                    chunk.text?.let { text ->
                        emit(text)
                    }
                }
                keyRotationManager.markKeySucceeded(apiKey)
                return@flow // Success
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
                val errorCode = extractErrorCode(e)
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed (code=$errorCode): ${e.message}")

                keyRotationManager.markKeyFailed(apiKey, errorCode)
                delay(1000L * (attempt + 1))
            }
        }

        throw lastError ?: Exception("Đã thử ${maxRetries} lần nhưng không thành công")
    }

    override suspend fun refineScene(
        currentContent: String,
        instruction: String,
        context: String
    ): Flow<String> = flow {
        val systemPrompt = buildString {
            appendLine("Bạn là nhà văn novel tiếng Việt. Người dùng muốn chỉnh sửa một đoạn truyện.")
            appendLine("KHÔNG viết lại toàn bộ. Chỉ sửa theo yêu cầu, giữ nguyên phần còn lại.")
            appendLine("Giữ đúng phong cách, tính cách nhân vật, và vibe.")
            if (context.isNotBlank()) {
                appendLine()
                appendLine(context)
            }
        }

        val userMessage = buildString {
            appendLine("Đoạn truyện hiện tại:")
            appendLine("---")
            appendLine(currentContent)
            appendLine("---")
            appendLine()
            appendLine("Yêu cầu chỉnh sửa: $instruction")
            appendLine()
            appendLine("Hãy viết lại đoạn truyện theo yêu cầu trên.")
        }

        val maxRetries = keyRotationManager.getMaxRetries()
        var lastError: Exception? = null

        for (attempt in 0 until maxRetries) {
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.GENERATOR)
                ?: throw Exception("Không có API key")

            try {
                val model = createModel(
                    apiKey = apiKey,
                    modelName = generationModelName,
                    maxTokens = maxOutputTokensMain,
                    systemInstruction = systemPrompt
                )
                model.generateContentStream(userMessage).collect { chunk ->
                    chunk.text?.let { emit(it) }
                }
                keyRotationManager.markKeySucceeded(apiKey)
                return@flow
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
                val errorCode = extractErrorCode(e)
                keyRotationManager.markKeyFailed(apiKey, errorCode)
                delay(1000L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Thử lại thất bại")
    }

    override suspend fun analyzeForMemories(sceneContent: String, novelContext: String): List<MemorySuggestion> {
        val systemPrompt = buildString {
            appendLine("Phân tích đoạn truyện sau và tìm các ký ức/sự kiện quan trọng cần ghi nhớ.")
            appendLine("Trả về JSON array, mỗi item có: content, type (PERMANENT/TEMPORARY/ARC), category (emotion_change/relationship/promise/trauma/development)")
            appendLine("Chỉ lấy sự kiện THỰC SỰ quan trọng, không lấy chi tiết nhỏ.")
            appendLine("Nếu không có gì đáng nhớ, trả về []")
        }

        val prompt = buildString {
            if (novelContext.isNotBlank()) {
                appendLine(novelContext)
                appendLine()
            }
            appendLine(sceneContent)
        }

        repeat(keyRotationManager.getMaxRetries()) { attempt ->
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.MEMORY) ?: return emptyList()
            try {
                val model = createModel(apiKey, memoryModelName, maxOutputTokensMemory, systemPrompt)
                val response = model.generateContent(prompt)
                val text = response.text ?: return emptyList()
                val jsonText = text.replace("```json", "").replace("```", "").trim()
                val type = object : TypeToken<List<MemorySuggestion>>() {}.type
                keyRotationManager.markKeySucceeded(apiKey)
                return gson.fromJson(jsonText, type) ?: emptyList()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorCode = extractErrorCode(e)
                keyRotationManager.markKeyFailed(apiKey, errorCode)
                Log.e(TAG, "Memory analysis failed (${attempt + 1}): ${e.message}")
                delay(800L * (attempt + 1))
            }
        }
        return emptyList()
    }

    override suspend fun summarizeScene(sceneContent: String): String {
        repeat(keyRotationManager.getMaxRetries()) { attempt ->
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.MEMORY) ?: return ""
            try {
                val model = createModel(
                    apiKey = apiKey,
                    modelName = memoryModelName,
                    maxTokens = maxOutputTokensMemory,
                    systemInstruction = "Tóm tắt đoạn truyện sau trong 1-2 câu ngắn gọn, tập trung vào sự kiện và cảm xúc chính."
                )
                val response = model.generateContent(sceneContent)
                keyRotationManager.markKeySucceeded(apiKey)
                return response.text?.trim() ?: ""
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorCode = extractErrorCode(e)
                keyRotationManager.markKeyFailed(apiKey, errorCode)
                Log.e(TAG, "Summarize failed (${attempt + 1}): ${e.message}")
                delay(800L * (attempt + 1))
            }
        }
        return ""
    }

    private fun extractErrorCode(e: Exception): Int? {
        val message = e.message ?: return 0
        return when {
            message.contains("429") || message.contains("RESOURCE_EXHAUSTED") -> 429
            message.contains("503") || message.contains("UNAVAILABLE") -> 503
            message.contains("500") || message.contains("INTERNAL") -> 500
            message.contains("401") || message.contains("UNAUTHENTICATED") -> 401
            message.contains("403") || message.contains("PERMISSION_DENIED") -> 403
            message.contains("400") || message.contains("INVALID_ARGUMENT") -> 400
            else -> null
        }
    }
}
