package com.novel.assistant.data.remote.ai

import com.novel.assistant.data.local.dao.StyleReferenceDao
import com.novel.assistant.data.local.entity.StyleReferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleAnalyzer @Inject constructor(
    private val memoryAnalyzer: MemoryAnalyzer,
    private val styleReferenceDao: StyleReferenceDao
) {
    suspend fun analyzeOriginalNovel(novelId: Long, sourceNovelDescription: String, sourceNovelName: String) = withContext(Dispatchers.IO) {
        if (sourceNovelDescription.isBlank()) return@withContext
        
        // This is a stub for the actual AI call.
        // In reality, we'd use MemoryAnalyzer or a new method in it to analyze the style.
        // For now, we simulate an extraction based on the description text.
        val summary = memoryAnalyzer.summarizeScene("Truyện gốc: $sourceNovelName\nMô tả: $sourceNovelDescription")
        
        val style = StyleReferenceEntity(
            novelId = novelId,
            rhythmNotes = "Nhịp độ tùy thuộc vào mô tả: $summary",
            dialogueStyle = "Hội thoại tự nhiên",
            emotionStyle = "Đa dạng",
            descriptionStyle = "Chi tiết",
            sampleText = sourceNovelDescription.take(200)
        )
        styleReferenceDao.insertStyle(style)
    }
}
