package com.novel.assistant.data.remote.ai

import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ngữ cảnh thông minh — tự động chọn và ghép context phù hợp trước khi gọi AI
 * Giảm token, giữ nhất quán lâu dài
 */
@Singleton
class ContextBuilder @Inject constructor(
    private val sceneDao: SceneDao,
    private val characterDao: CharacterDao,
    private val correctionDao: CharacterCorrectionDao,
    private val memoryDao: MemoryDao,
    private val relationshipDao: RelationshipDao,
    private val timelineEventDao: TimelineEventDao,
    private val styleReferenceDao: StyleReferenceDao,
    private val novelDao: NovelDao
) {
    private val gson = Gson()

    suspend fun buildSystemPrompt(novel: NovelEntity, settings: PromptSettings): String {
        val vibeTags = try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(novel.styleVibeTags, type)
        } catch (e: Exception) { emptyList<String>() }

        val sceneVibeTags = settings.vibeTags.joinToString(", ")
        val novelVibeTags = vibeTags.joinToString(", ")

        return buildString {
            appendLine("Bạn là một nhà văn novel/webnovel tiếng Việt tài năng.")
            appendLine("Nhiệm vụ: viết scene truyện hoàn chỉnh từ ý tưởng ngắn của người dùng.")
            appendLine()
            appendLine("QUY TẮC QUAN TRỌNG:")
            appendLine("- Viết văn truyện hoàn chỉnh, KHÔNG phải chat hay tóm tắt")
            appendLine("- Có mô tả chi tiết, hành động, suy nghĩ, lời thoại, cảm xúc")
            appendLine("- Giữ đúng tính cách và cách nói của từng nhân vật")
            appendLine("- Giữ nhịp cảm xúc và continuity giữa các scene")
            appendLine("- Ưu tiên cảm xúc, không khí, chemistry hơn là viết hoa mỹ")
            appendLine("- Viết đủ dài, chi tiết, có khoảng lặng và nhịp thở")
            appendLine()
            if (novelVibeTags.isNotBlank()) {
                appendLine("PHONG CÁCH TRUYỆN: $novelVibeTags")
            }
            if (novel.sourceNovelName.isNotBlank()) {
                appendLine("TRUYỆN GỐC: ${novel.sourceNovelName}")
                if (novel.sourceNovelDescription.isNotBlank()) {
                    appendLine("MÔ TẢ: ${novel.sourceNovelDescription}")
                }
            }
            appendLine()
            appendLine("CÀI ĐẶT SCENE NÀY:")
            if (settings.mood.isNotBlank()) appendLine("- Tâm trạng: ${settings.mood}")
            if (sceneVibeTags.isNotBlank()) appendLine("- Vibe: $sceneVibeTags")
            appendLine("- Tốc độ: ${settings.speed}")
            appendLine("- Mức thoại: ${settings.dialogueLevel}")
            appendLine("- Góc nhìn: ${settings.viewpoint}")
            if (settings.focus.isNotBlank()) appendLine("- Trọng tâm: ${settings.focus}")
        }
    }

    suspend fun buildRoleplaySystemPrompt(novel: NovelEntity, characterName: String): String {
        return buildString {
            appendLine("Bạn là một nhà văn novel/webnovel tiếng Việt tài năng, đang đồng sáng tác.")
            appendLine()
            appendLine("CHẾ ĐỘ NHẬP VAI:")
            appendLine("- Người dùng điều khiển nhân vật: $characterName")
            appendLine("- Người dùng sẽ viết hành động/suy nghĩ/cảm xúc của $characterName")
            appendLine("- Bạn viết: phản ứng nhân vật khác, lời thoại, môi trường, diễn biến")
            appendLine("- Giữ đúng tính cách tất cả nhân vật")
            appendLine("- Viết chi tiết, có cảm xúc, có khoảng lặng")
            appendLine("- KHÔNG điều khiển $characterName - chỉ viết phản ứng xung quanh")
        }
    }

    suspend fun buildCharacterContext(novelId: Long, selectedCharIds: List<Long>? = null): String {
        val characters = if (selectedCharIds != null && selectedCharIds.isNotEmpty()) {
            characterDao.getCharactersByIds(selectedCharIds)
        } else {
            characterDao.getCharactersByNovelOnce(novelId)
        }

        if (characters.isEmpty()) return ""

        val corrections = correctionDao.getCorrectionsByNovel(novelId)
        val correctionMap = corrections.groupBy { it.characterId }
        val relationships = relationshipDao.getRelationshipsByNovelOnce(novelId)
        val characterIds = characters.map { it.id }.toSet()

        return buildString {
            appendLine("=== NHÂN VẬT ===")
            characters.forEach { char ->
                appendLine("【${char.name}】${if (char.isMainCharacter) " (nhân vật chính)" else ""}")
                if (char.personality.isNotBlank()) appendLine("  Tính cách: ${char.personality}")
                if (char.speechStyle.isNotBlank()) appendLine("  Cách nói: ${char.speechStyle}")
                if (char.fears.isNotBlank()) appendLine("  Nỗi sợ: ${char.fears}")
                if (char.importantThings.isNotBlank()) appendLine("  Điều quan trọng: ${char.importantThings}")
                if (char.currentEmotionalState.isNotBlank()) {
                    appendLine("  Trạng thái hiện tại: ${char.currentEmotionalState}")
                }
                // Corrections
                val charCorrections = correctionMap[char.id]
                if (!charCorrections.isNullOrEmpty()) {
                    appendLine("  LƯU Ý QUAN TRỌNG:")
                    charCorrections.forEach { c ->
                        appendLine("    - ${c.wrongExample} → ${c.rightDescription}")
                    }
                }
                appendLine()
            }

            val relatedRelationships = relationships.filter {
                it.char1Id in characterIds || it.char2Id in characterIds
            }
            if (relatedRelationships.isNotEmpty()) {
                val namesById = characters.associateBy({ it.id }, { it.name })
                appendLine("=== QUAN HỆ NHÂN VẬT ===")
                relatedRelationships.forEach { rel ->
                    val left = namesById[rel.char1Id] ?: "Nhân vật ${rel.char1Id}"
                    val right = namesById[rel.char2Id] ?: "Nhân vật ${rel.char2Id}"
                    appendLine("- $left và $right: ${rel.status.ifBlank { rel.description }}")
                    if (rel.description.isNotBlank() && rel.description != rel.status) {
                        appendLine("  ${rel.description}")
                    }
                }
            }
        }
    }

    suspend fun buildStoryContext(novelId: Long): String {
        return buildString {
            // Recent scenes (summaries)
            val recentScenes = sceneDao.getRecentScenes(novelId, 3)
            if (recentScenes.isNotEmpty()) {
                appendLine("=== CÁC SCENE GẦN NHẤT ===")
                recentScenes.reversed().forEachIndexed { idx, scene ->
                    val preview = scene.content.take(300).let {
                        if (scene.content.length > 300) "$it..." else it
                    }
                    appendLine("Scene ${idx + 1}: ${scene.title.ifBlank { "(chưa đặt tên)" }}")
                    appendLine(preview)
                    appendLine()
                }
            }

            // Permanent memories
            val permanentMemories = memoryDao.getPermanentMemories(novelId)
            if (permanentMemories.isNotEmpty()) {
                appendLine("=== KÝ ỨC QUAN TRỌNG ===")
                permanentMemories.forEach { mem ->
                    appendLine("- ${mem.summary.ifBlank { mem.content }}")
                }
                appendLine()
            }

            // Arc memories
            val arcMemories = memoryDao.getArcMemories(novelId)
            if (arcMemories.isNotEmpty()) {
                appendLine("=== ARC HIỆN TẠI ===")
                arcMemories.forEach { mem ->
                    appendLine("- ${mem.summary.ifBlank { mem.content }}")
                }
                appendLine()
            }

            // Recent timeline events
            val events = timelineEventDao.getRecentEvents(novelId, 5)
            if (events.isNotEmpty()) {
                appendLine("=== DIỄN BIẾN GẦN ĐÂY ===")
                events.reversed().forEach { event ->
                    appendLine("- ${event.eventDescription}")
                }
                appendLine()
            }
        }
    }

    suspend fun buildStyleContext(novelId: Long): String {
        val styles = styleReferenceDao.getRecentStyles(novelId, 2)
        if (styles.isEmpty()) return ""

        return buildString {
            appendLine("=== PHONG CÁCH MẪU (đoạn được đánh dấu yêu thích) ===")
            styles.forEach { style ->
                if (style.sampleText.isNotBlank()) {
                    appendLine("Đoạn mẫu: \"${style.sampleText.take(200)}\"")
                }
                if (style.rhythmNotes.isNotBlank()) appendLine("Nhịp văn: ${style.rhythmNotes}")
                if (style.dialogueStyle.isNotBlank()) appendLine("Kiểu thoại: ${style.dialogueStyle}")
                if (style.emotionStyle.isNotBlank()) appendLine("Cảm xúc: ${style.emotionStyle}")
                appendLine()
            }
            appendLine("Hãy tham khảo phong cách trên khi viết.")
        }
    }

    /**
     * Build complete context for a scene generation request
     */
    suspend fun buildFullContext(
        novelId: Long,
        settings: PromptSettings,
        selectedCharIds: List<Long>? = null,
        isRoleplay: Boolean = false,
        roleplayCharName: String = ""
    ): SceneRequest {
        val novel = novelDao.getNovelById(novelId) ?: throw IllegalStateException("Novel not found")

        val systemPrompt = if (isRoleplay) {
            buildRoleplaySystemPrompt(novel, roleplayCharName)
        } else {
            buildSystemPrompt(novel, settings)
        }

        return SceneRequest(
            userPrompt = "", // Will be filled by caller
            systemContext = systemPrompt,
            characterContext = buildCharacterContext(novelId, selectedCharIds),
            storyContext = buildStoryContext(novelId),
            styleContext = buildStyleContext(novelId),
            promptSettings = settings,
            isRoleplay = isRoleplay,
            roleplayCharacterName = roleplayCharName
        )
    }
}
