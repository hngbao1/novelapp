package com.novel.assistant.data.remote.ai

import kotlinx.coroutines.flow.Flow

data class SceneRequest(
    val userPrompt: String,
    val systemContext: String,
    val characterContext: String = "",
    val storyContext: String = "",
    val styleContext: String = "",
    val promptSettings: PromptSettings = PromptSettings(),
    val isRoleplay: Boolean = false,
    val roleplayCharacterName: String = ""
)

data class PromptSettings(
    val mood: String = "",
    val speed: String = "vừa",
    val dialogueLevel: String = "bình thường",
    val viewpoint: String = "nhân vật chính",
    val focus: String = "",
    val vibeTags: List<String> = emptyList(),
    val location: String = "",
    val time: String = "",
    val sceneGoal: String = "",
    val unresolvedTopics: String = "",
    
    // --- Vibe & Engine Settings ---
    val presetName: String = "Mặc định",
    val sceneEnergy: Int = 1,
    val unpredictabilityLevel: Int = 1,
    val continuityLevel: Int = 1,
    val cinematicLevel: Int = 1,
    val introspectionLevel: Int = 1,
    val melancholyLevel: Int = 0
)

data class MemorySuggestion(
    val content: String,
    val type: String, // PERMANENT, TEMPORARY, ARC
    val category: String,
    val relatedCharacterNames: List<String> = emptyList()
)

data class CharacterStateUpdate(
    val characterName: String,
    val newEmotionalState: String
)

/**
 * Interface cho AI chuyên viết truyện và sinh phân cảnh.
 * Mở đường để sử dụng Claude hoặc OpenAI.
 */
interface SceneGenerator {
    suspend fun generateScene(request: SceneRequest): Flow<String>
    suspend fun refineScene(currentContent: String, instruction: String, context: String): Flow<String>
}

/**
 * Interface cho AI chuyên xử lý logic và phân tích.
 * Ưu tiên dùng Gemini vì xử lý context dài tốt và rẻ.
 */
interface MemoryAnalyzer {
    suspend fun analyzeForMemories(sceneContent: String, novelContext: String): List<MemorySuggestion>
    suspend fun summarizeScene(sceneContent: String): String
}

/**
 * Interface gộp chung cho các Provider hỗ trợ cả hai (như GeminiProvider hiện tại)
 */
interface AiProvider : SceneGenerator, MemoryAnalyzer

