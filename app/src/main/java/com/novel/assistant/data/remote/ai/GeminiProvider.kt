package com.novel.assistant.data.remote.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gọi Gemini REST API trực tiếp qua OkHttp.
 * Không dùng com.google.ai.client.generativeai (đã deprecated).
 * Pattern tham khảo từ RoleplayChatAndroid/GeminiClient.kt.
 */
@Singleton
class GeminiProvider @Inject constructor(
    private val keyRotationManager: KeyRotationManager
) : AiProvider {

    companion object {
        private const val TAG = "GeminiProvider"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private var generationModelName: String = "gemini-2.0-flash"
    private var memoryModelName: String = "gemini-2.0-flash"
    private var maxOutputTokensMain: Int = 8192
    private var maxOutputTokensMemory: Int = 2048
    private val gson = Gson()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

    // ─────────────────────────────────────────────────────────────────────────
    // Xây dựng JSON payload cho Gemini REST API
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildPayload(
        userPrompt: String,
        systemInstruction: String?,
        temperature: Float = 0.85f,
        topP: Float = 0.95f,
        topK: Int = 40,
        maxOutputTokens: Int
    ): String {
        val root = JsonObject()

        // systemInstruction
        if (!systemInstruction.isNullOrBlank()) {
            val sysObj = JsonObject()
            val sysPartsArr = JsonArray()
            val sysPart = JsonObject()
            sysPart.addProperty("text", systemInstruction)
            sysPartsArr.add(sysPart)
            sysObj.add("parts", sysPartsArr)
            root.add("systemInstruction", sysObj)
        }

        // contents
        val contentsArr = JsonArray()
        val contentObj = JsonObject()
        contentObj.addProperty("role", "user")
        val partsArr = JsonArray()
        val part = JsonObject()
        part.addProperty("text", userPrompt)
        partsArr.add(part)
        contentObj.add("parts", partsArr)
        contentsArr.add(contentObj)
        root.add("contents", contentsArr)

        // safetySettings — tắt filter để AI viết văn sáng tạo thoải mái
        val safetyArr = JsonArray()
        listOf(
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT",
            "HARM_CATEGORY_CIVIC_INTEGRITY"
        ).forEach { category ->
            val s = JsonObject()
            s.addProperty("category", category)
            s.addProperty("threshold", "OFF")
            safetyArr.add(s)
        }
        root.add("safetySettings", safetyArr)

        // generationConfig
        val genConfig = JsonObject()
        genConfig.addProperty("temperature", temperature)
        genConfig.addProperty("topP", topP)
        genConfig.addProperty("topK", topK)
        genConfig.addProperty("maxOutputTokens", maxOutputTokens)
        root.add("generationConfig", genConfig)

        return gson.toJson(root)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Streaming: dùng SSE endpoint streamGenerateContent
    // Đọc từng dòng "data: {...}" và emit chunk text vào Flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun streamContent(
        apiKey: String,
        modelName: String,
        payload: String
    ): Flow<String> = flow {
        val url = "$BASE_URL/$modelName:streamGenerateContent?key=$apiKey&alt=sse"
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = withContext(Dispatchers.IO) { response.body?.string() ?: "" }
            throw Exception("Gemini API lỗi ${response.code}: ${errorBody.take(300)}")
        }

        val body = response.body ?: throw Exception("Gemini trả về body rỗng")

        try {
            val reader = BufferedReader(
                InputStreamReader(body.byteStream(), Charsets.UTF_8)
            )
            while (true) {
                val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                if (!line.startsWith("data:")) continue
                val jsonStr = line.removePrefix("data:").trim()
                if (jsonStr == "[DONE]" || jsonStr.isBlank()) continue
                try {
                    val chunk = gson.fromJson(jsonStr, JsonObject::class.java)
                    val text = extractTextFromChunk(chunk)
                    if (text.isNotEmpty()) emit(text)
                } catch (_: Exception) {
                    // chunk JSON không hợp lệ, bỏ qua
                }
            }
        } finally {
            withContext(Dispatchers.IO) { body.close() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Non-streaming: dùng generateContent endpoint thông thường
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun generateContent(
        apiKey: String,
        modelName: String,
        payload: String
    ): String = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Gemini API lỗi ${response.code}: ${responseBody.take(300)}")
        }

        extractTextFromResponse(responseBody)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractTextFromChunk(chunkObj: JsonObject): String {
        return try {
            val candidates = chunkObj.getAsJsonArray("candidates") ?: return ""
            if (candidates.size() == 0) return ""
            val candidate = candidates[0]?.asJsonObject ?: return ""
            val content = candidate.getAsJsonObject("content") ?: return ""
            val parts = content.getAsJsonArray("parts") ?: return ""
            buildString {
                parts.forEach { part ->
                    val text = part?.asJsonObject?.get("text")?.asString ?: return@forEach
                    append(text)
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractTextFromResponse(responseBody: String): String {
        return try {
            val root = gson.fromJson(responseBody, JsonObject::class.java)

            // Kiểm tra block từ phía prompt
            val promptFeedback = root.getAsJsonObject("promptFeedback")
            val blockReason = promptFeedback?.get("blockReason")?.asString
            if (!blockReason.isNullOrBlank()) {
                throw Exception("Gemini chặn nội dung: $blockReason")
            }

            val candidates = root.getAsJsonArray("candidates")
                ?: throw Exception("Gemini không trả về candidates")
            if (candidates.size() == 0) throw Exception("Gemini trả về candidates rỗng")

            val first = candidates[0].asJsonObject
            val finishReason = first.get("finishReason")?.asString?.uppercase()
            if (finishReason != null && finishReason in setOf(
                    "SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII"
                )
            ) {
                throw Exception("Gemini chặn phản hồi: $finishReason")
            }

            val content = first.getAsJsonObject("content")
                ?: throw Exception("Gemini candidate không có content")
            val parts = content.getAsJsonArray("parts")
                ?: throw Exception("Gemini content không có parts")

            buildString {
                parts.forEach { part ->
                    val text = part?.asJsonObject?.get("text")?.asString ?: return@forEach
                    if (text.isNotBlank()) append(text)
                }
            }.trim()
        } catch (e: Exception) {
            if (e.message?.startsWith("Gemini") == true) throw e
            throw Exception("Không thể đọc phản hồi Gemini: ${e.message}")
        }
    }

    private fun extractErrorCode(e: Exception): Int? {
        val message = e.message ?: return null
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

    // ─────────────────────────────────────────────────────────────────────────
    // AiProvider interface implementation
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateTemperature(unpredictabilityLevel: Int): Float {
        return when (unpredictabilityLevel) {
            0 -> 0.4f // Đúng ý
            1 -> 0.85f // Cân bằng
            2 -> 1.4f // Khó đoán
            else -> 0.85f
        }
    }

    private fun calculateTopP(continuityLevel: Int): Float {
        return when (continuityLevel) {
            0 -> 0.95f // Nhẹ
            1 -> 0.8f // Cân bằng
            2 -> 0.5f // Chặt chẽ (Continuity cao)
            else -> 0.8f
        }
    }

    private fun calculateMaxTokens(presetName: String, cinematicLevel: Int): Int {
        var baseTokens = when (presetName) {
            "Melancholy", "Healing" -> 2048 // Ngắn gọn, tĩnh lặng
            "Visual novel Hàn", "Drama nhẹ" -> 4096 // Vừa phải
            "Slow burn", "Điện ảnh đời thường" -> 6144 // Dài hơn
            else -> 4096
        }
        // Điện ảnh cao -> cần miêu tả nhiều -> cộng thêm token
        if (cinematicLevel == 2) baseTokens += 2048 
        return baseTokens.coerceAtMost(8192)
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

        val temp = calculateTemperature(request.promptSettings.unpredictabilityLevel)
        val topP = calculateTopP(request.promptSettings.continuityLevel)
        val tokens = calculateMaxTokens(request.promptSettings.presetName, request.promptSettings.cinematicLevel)

        val payload = buildPayload(
            userPrompt = userMessage,
            systemInstruction = fullSystemPrompt,
            temperature = temp,
            topP = topP,
            maxOutputTokens = tokens
        )

        var lastError: Exception? = null
        val maxRetries = keyRotationManager.getMaxRetries()

        for (attempt in 0 until maxRetries) {
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.GENERATOR)
                ?: throw Exception("Không có API key nào khả dụng")

            try {
                streamContent(apiKey, generationModelName, payload).collect { chunk ->
                    emit(chunk)
                }
                keyRotationManager.markKeySucceeded(apiKey)
                return@flow
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
                val errorCode = extractErrorCode(e)
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed (code=$errorCode): ${e.message}")
                keyRotationManager.markKeyFailed(apiKey, errorCode)
                delay(1000L * (attempt + 1))
            }
        }

        throw lastError ?: Exception("Đã thử $maxRetries lần nhưng không thành công")
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

        val payload = buildPayload(
            userPrompt = userMessage,
            systemInstruction = systemPrompt,
            maxOutputTokens = maxOutputTokensMain
        )

        val maxRetries = keyRotationManager.getMaxRetries()
        var lastError: Exception? = null

        for (attempt in 0 until maxRetries) {
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.GENERATOR)
                ?: throw Exception("Không có API key")

            try {
                streamContent(apiKey, generationModelName, payload).collect { chunk ->
                    emit(chunk)
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

    override suspend fun analyzeForMemories(
        sceneContent: String,
        novelContext: String
    ): List<MemorySuggestion> {
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

        val payload = buildPayload(
            userPrompt = prompt,
            systemInstruction = systemPrompt,
            temperature = 0.3f,
            topP = 0.9f,
            topK = 20,
            maxOutputTokens = maxOutputTokensMemory
        )

        repeat(keyRotationManager.getMaxRetries()) { attempt ->
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.MEMORY)
                ?: return emptyList()
            try {
                val text = generateContent(apiKey, memoryModelName, payload)
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
        val systemPrompt =
            "Tóm tắt đoạn truyện sau trong 1-2 câu ngắn gọn, tập trung vào sự kiện và cảm xúc chính."

        val payload = buildPayload(
            userPrompt = sceneContent,
            systemInstruction = systemPrompt,
            temperature = 0.3f,
            topP = 0.9f,
            topK = 20,
            maxOutputTokens = maxOutputTokensMemory
        )

        repeat(keyRotationManager.getMaxRetries()) { attempt ->
            val apiKey = keyRotationManager.getNextKeyWithFallback(KeyGroup.MEMORY)
                ?: return ""
            try {
                val result = generateContent(apiKey, memoryModelName, payload)
                keyRotationManager.markKeySucceeded(apiKey)
                return result.trim()
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
}
