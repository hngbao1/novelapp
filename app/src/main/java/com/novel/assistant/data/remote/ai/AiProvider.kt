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
    val vibeTags: List<String> = emptyList()
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
 * Interface cho AI provider — thiết kế mở để dễ thêm Claude/OpenAI sau
 */
interface AiProvider {
    suspend fun generateScene(request: SceneRequest): Flow<String>
    suspend fun refineScene(currentContent: String, instruction: String, context: String): Flow<String>
    suspend fun analyzeForMemories(sceneContent: String, novelContext: String): List<MemorySuggestion>
    suspend fun summarizeScene(sceneContent: String): String
}
