package com.novel.assistant.data.remote.ai

import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ngữ cảnh thông minh — Scene-Aware Context Builder
 *
 * Lắp ghép prompt theo thứ tự ưu tiên nghiêm ngặt:
 * 1. Current Scene State (vị trí, thời gian, ai đang ở cùng, unresolved tension)
 * 2. Character Dynamics (chemistry giữa các nhân vật có mặt)
 * 3. Emotional Continuity (cảm xúc hiện tại chuyển tiếp từ scene trước)
 * 4. Scene Goal (mục tiêu phân cảnh)
 * 5. Style Rules (phong cách phải giữ)
 * 6. Anti-AI Rules (tránh văn AI generic)
 * 7. Relevant Memories (ký ức được chọn lọc theo relevance)
 * 8. Timeline (diễn biến sự kiện)
 * 9. User Prompt (ý tưởng ngắn)
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

    // ═══════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════

    /**
     * Build complete context for a scene generation request.
     * Assembles all context blocks in strict priority order.
     */
    suspend fun buildFullContext(
        novelId: Long,
        settings: PromptSettings,
        selectedCharIds: List<Long>? = null,
        isRoleplay: Boolean = false,
        roleplayCharName: String = ""
    ): SceneRequest {
        val novel = novelDao.getNovelById(novelId) ?: throw IllegalStateException("Novel not found")
        val charIds = selectedCharIds?.ifEmpty { null }

        val systemPrompt = if (isRoleplay) {
            buildRoleplaySystemPrompt(novel, roleplayCharName)
        } else {
            buildSystemPrompt(novel, settings)
        }

        return SceneRequest(
            userPrompt = "", // Will be filled by caller
            systemContext = systemPrompt,
            characterContext = buildCharacterContext(novelId, charIds),
            storyContext = buildStoryContext(novelId, charIds, settings),
            styleContext = buildStyleContext(novelId),
            promptSettings = settings,
            isRoleplay = isRoleplay,
            roleplayCharacterName = roleplayCharName
        )
    }

    // For refineScene — lighter context
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
        val namesById = characters.associateBy({ it.id }, { it.name })

        return buildString {
            // ── Block 2: Character Dynamics ──
            val relatedRelationships = relationships.filter {
                it.char1Id in characterIds || it.char2Id in characterIds
            }
            if (relatedRelationships.isNotEmpty()) {
                appendLine("=== TƯƠNG TÁC NHÂN VẬT (DYNAMICS) ===")
                relatedRelationships.forEach { rel ->
                    val left = namesById[rel.char1Id] ?: "?"
                    val right = namesById[rel.char2Id] ?: "?"
                    appendLine("$left ↔ $right:")
                    if (rel.status.isNotBlank()) appendLine("  Quan hệ: ${rel.status}")
                    if (rel.description.isNotBlank()) appendLine("  Mô tả: ${rel.description}")
                    if (rel.dynamics.isNotBlank()) appendLine("  Chemistry: ${rel.dynamics}")
                }
                appendLine()
            }

            // ── Block 3: Emotional Continuity (character profiles + current state) ──
            appendLine("=== NHÂN VẬT ===")
            characters.forEach { char ->
                appendLine("【${char.name}】${if (char.isMainCharacter) " (nhân vật chính)" else ""}")
                if (char.personality.isNotBlank()) appendLine("  Tính cách: ${char.personality}")
                if (char.speechStyle.isNotBlank()) appendLine("  Cách nói: ${char.speechStyle}")
                if (char.fears.isNotBlank()) appendLine("  Nỗi sợ: ${char.fears}")
                if (char.importantThings.isNotBlank()) appendLine("  Điều quan trọng: ${char.importantThings}")
                if (char.currentEmotionalState.isNotBlank()) {
                    appendLine("  ⚡ CẢM XÚC HIỆN TẠI: ${char.currentEmotionalState}")
                }
                
                // --- Trụ cột 2: Voice Lock ---
                if (char.voiceRhythm.isNotBlank()) appendLine("  🗣️ NHỊP THOẠI: ${char.voiceRhythm}")
                val evasionStr = when (char.evasionLevel) {
                    0 -> "Trực diện, thẳng thắn"
                    1 -> "Đôi khi nói vòng vo, né tránh chủ đề khó"
                    2 -> "Cực kỳ lảng tránh cảm xúc, đổi chủ đề khi căng thẳng"
                    else -> "Trực diện"
                }
                appendLine("  🛡️ PHẢN ỨNG TÂM LÝ: $evasionStr")
                val initStr = when (char.initiativeLevel) {
                    0 -> "Thụ động, chờ người khác mở lời"
                    1 -> "Bình thường"
                    2 -> "Chủ động, hay dẫn dắt câu chuyện"
                    else -> "Bình thường"
                }
                appendLine("  🔥 MỨC CHỦ ĐỘNG: $initStr")
                // Corrections — high priority for AI to learn character voice
                val charCorrections = correctionMap[char.id]
                if (!charCorrections.isNullOrEmpty()) {
                    appendLine("  ⚠ LƯU Ý QUAN TRỌNG (AI hay viết sai):")
                    charCorrections.forEach { c ->
                        appendLine("    ❌ ${c.wrongExample}")
                        appendLine("    ✅ ${c.rightDescription}")
                    }
                }
                appendLine()
            }
        }
    }

    // ═══════════════════════════════════════
    // SYSTEM PROMPT BUILDERS
    // ═══════════════════════════════════════

    private suspend fun buildSystemPrompt(novel: NovelEntity, settings: PromptSettings): String {
        val vibeTags = parseJsonList(novel.styleVibeTags)
        val novelVibeTags = vibeTags.joinToString(", ")
        val sceneVibeTags = settings.vibeTags.joinToString(", ")

        // Load style references for instruction (not just sample text)
        val styles = styleReferenceDao.getRecentStyles(novel.id, 2)

        return buildString {
            appendLine("Bạn là một nhà văn novel/webnovel tiếng Việt tài năng.")
            appendLine("Nhiệm vụ: viết scene truyện hoàn chỉnh từ ý tưởng ngắn của người dùng.")
            appendLine()

            // ── Block 1: Current Scene State ──
            appendLine("=== TRẠNG THÁI SCENE HIỆN TẠI ===")
            if (settings.location.isNotBlank()) appendLine("Địa điểm: ${settings.location}")
            if (settings.time.isNotBlank()) appendLine("Thời gian: ${settings.time}")
            if (settings.mood.isNotBlank()) appendLine("Tâm trạng: ${settings.mood}")
            if (sceneVibeTags.isNotBlank()) appendLine("Không khí: $sceneVibeTags")
            if (settings.unresolvedTopics.isNotBlank()) {
                appendLine("Chủ đề tồn đọng / Tension chưa giải quyết: ${settings.unresolvedTopics}")
            }
            appendLine()

            // ── Block 4: Scene Goal ──
            if (settings.sceneGoal.isNotBlank()) {
                appendLine("=== MỤC TIÊU SCENE ===")
                appendLine(settings.sceneGoal)
                appendLine("Hãy viết scene xoay quanh mục tiêu này. Không viết lan man.")
                appendLine()
            }

            // ── Block 5: Style Rules (Vibe & Preset) ──
            appendLine("=== PHONG CÁCH PHẢI GIỮ ===")
            if (novelVibeTags.isNotBlank()) appendLine("Vibe truyện gốc: $novelVibeTags")
            appendLine(PromptModules.getPresetRules(settings.presetName))
            appendLine(PromptModules.getEnergyRules(settings.sceneEnergy))
            appendLine(PromptModules.getCinematicAndIntrospectionRules(settings.cinematicLevel, settings.introspectionLevel, settings.melancholyLevel))
            appendLine("- Giữ emotional continuity với scene trước")
            
            // Style references (Trụ cột 3: Vibe Preservation)
            if (styles.isNotEmpty()) {
                appendLine()
                appendLine("=== LONG-TERM STYLE MEMORY ===")
                styles.forEach { style ->
                    if (style.atmosphere.isNotBlank()) appendLine("Bầu không khí cần giữ: ${style.atmosphere}")
                    if (style.emotionalRhythm.isNotBlank()) appendLine("Nhịp cảm xúc cần giữ: ${style.emotionalRhythm}")
                    if (style.rhythmNotes.isNotBlank()) appendLine("Nhịp văn: ${style.rhythmNotes}")
                }
            }
            appendLine()

            // ── Block 6: Anti-AI Rules ──
            appendLine(PromptModules.getAntiAiRules())
            appendLine()

            // Source novel info
            if (novel.sourceNovelName.isNotBlank()) {
                appendLine("TRUYỆN GỐC: ${novel.sourceNovelName}")
                if (novel.sourceNovelDescription.isNotBlank()) {
                    appendLine("MÔ TẢ: ${novel.sourceNovelDescription}")
                }
                appendLine()
            }
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
            appendLine()
            appendLine("=== TRÁNH ===")
            appendLine("- KHÔNG thoại triết lý hoặc quá hoàn hảo")
            appendLine("- KHÔNG giải thích cảm xúc thay vì thể hiện")
            appendLine("- KHÔNG reset mood hoặc quên trạng thái nhân vật")
        }
    }

    // ═══════════════════════════════════════
    // STORY CONTEXT — Smart Relevance
    // ═══════════════════════════════════════

    suspend fun buildStoryContext(
        novelId: Long,
        selectedCharIds: List<Long>? = null,
        settings: PromptSettings = PromptSettings()
    ): String {
        return buildString {
            // ── Recent scene summaries (not raw text) ──
            val recentScenes = sceneDao.getRecentScenes(novelId, 3)
            if (recentScenes.isNotEmpty()) {
                appendLine("=== CÁC SCENE GẦN NHẤT ===")
                recentScenes.reversed().forEachIndexed { idx, scene ->
                    val preview = scene.summary.ifBlank {
                        scene.content.take(200).let {
                            if (scene.content.length > 200) "$it..." else it
                        }
                    }
                    appendLine("Scene ${idx + 1}: ${scene.title.ifBlank { "(chưa đặt tên)" }}")
                    appendLine(preview)
                    appendLine()
                }
            }

            // ── Block 7: Relevant Memories (Smart Relevance Scoring) ──
            val allMemories = memoryDao.getActiveMemoriesOnce(novelId)
            val relevantMemories = scoreAndFilterMemories(allMemories, selectedCharIds, settings)

            if (relevantMemories.isNotEmpty()) {
                appendLine("=== KÝ ỨC LIÊN QUAN ===")
                relevantMemories.forEach { mem ->
                    val prefix = when (mem.type) {
                        "PERMANENT" -> "★"
                        "ARC" -> "◆"
                        else -> "·"
                    }
                    appendLine("$prefix ${mem.summary.ifBlank { mem.content }}")
                }
                appendLine()
            }

            // ── Block 8: Timeline ──
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

    // ═══════════════════════════════════════
    // STYLE CONTEXT
    // ═══════════════════════════════════════

    suspend fun buildStyleContext(novelId: Long): String {
        val styles = styleReferenceDao.getRecentStyles(novelId, 2)
        if (styles.isEmpty()) return ""

        return buildString {
            appendLine("=== ĐOẠN MẪU YÊU THÍCH ===")
            styles.forEach { style ->
                if (style.sampleText.isNotBlank()) {
                    appendLine("\"${style.sampleText.take(200)}\"")
                }
                appendLine()
            }
            appendLine("Tham khảo cách viết trên để giữ phong cách nhất quán.")
        }
    }

    // ═══════════════════════════════════════
    // SMART RELEVANCE SCORING
    // ═══════════════════════════════════════

    /**
     * Lọc và xếp hạng ký ức theo mức độ liên quan tới scene hiện tại.
     * Ưu tiên:
     *  1. Memory gần đây nhất
     *  2. Memory PERMANENT / quan trọng
     *  3. Memory chứa unresolved promises/conflicts
     *  4. Memory liên quan trực tiếp đến nhân vật đang có trong scene
     *  5. Memory có category khớp mood/focus
     * Giới hạn tối đa 8 ký ức để tiết kiệm token.
     */
    private fun scoreAndFilterMemories(
        allMemories: List<MemoryEntity>,
        selectedCharIds: List<Long>?,
        settings: PromptSettings
    ): List<MemoryEntity> {
        if (allMemories.isEmpty()) return emptyList()

        val charIdSet = selectedCharIds?.toSet() ?: emptySet()
        val moodKeywords = buildMoodKeywords(settings.mood, settings.focus, settings.sceneGoal)

        val scored = allMemories.map { mem ->
            var score = 0.0

            // Recency bonus (newer = higher)
            val ageHours = (System.currentTimeMillis() - mem.createdAt) / 3_600_000.0
            score += (100.0 / (1.0 + ageHours / 24.0)) // Decays over days

            // Type priority
            when (mem.type) {
                "PERMANENT" -> score += 40
                "ARC" -> score += 30
                "TEMPORARY" -> score += 5
            }

            // Category relevance to mood/focus
            if (mem.category.isNotBlank() && moodKeywords.any { mem.category.contains(it, ignoreCase = true) }) {
                score += 25
            }
            // Content relevance to mood
            if (moodKeywords.any { mem.content.contains(it, ignoreCase = true) || mem.summary.contains(it, ignoreCase = true) }) {
                score += 15
            }

            // Character relevance
            if (charIdSet.isNotEmpty()) {
                val memCharIds = parseJsonLongList(mem.relatedCharacterIds)
                if (memCharIds.any { it in charIdSet }) {
                    score += 35
                }
            }

            // Unresolved/promise/conflict bonus
            val unresolvedKeywords = listOf("hứa", "chưa", "bí mật", "giấu", "xung đột", "tension", "chờ", "mâu thuẫn")
            if (unresolvedKeywords.any { mem.content.contains(it, ignoreCase = true) }) {
                score += 20
            }

            Pair(mem, score)
        }

        return scored
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }
    }

    /**
     * Build mood-related keywords to match memories against.
     */
    private fun buildMoodKeywords(mood: String, focus: String, sceneGoal: String): List<String> {
        val keywords = mutableListOf<String>()

        when {
            mood.contains("buồn", ignoreCase = true) || mood.contains("melancholy", ignoreCase = true) ->
                keywords.addAll(listOf("chia ly", "mất mát", "lời hứa", "khoảng lặng", "buồn", "nỗi đau", "nhớ"))
            mood.contains("lãng mạn", ignoreCase = true) || mood.contains("ngượng", ignoreCase = true) ->
                keywords.addAll(listOf("chemistry", "tình cảm", "hẹn hò", "yêu", "tim", "ngượng", "gần"))
            mood.contains("căng thẳng", ignoreCase = true) ->
                keywords.addAll(listOf("xung đột", "tension", "giận", "bí mật", "phản bội", "nghi ngờ"))
            mood.contains("chữa lành", ignoreCase = true) || mood.contains("ấm", ignoreCase = true) ->
                keywords.addAll(listOf("chữa lành", "ấm áp", "bình yên", "tha thứ", "hiểu", "chấp nhận"))
        }

        if (focus.isNotBlank()) keywords.add(focus.lowercase())
        if (sceneGoal.isNotBlank()) keywords.add(sceneGoal.lowercase())

        return keywords
    }

    // ═══════════════════════════════════════
    // UTILS
    // ═══════════════════════════════════════

    private fun parseJsonList(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsonLongList(json: String): List<Long> {
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
