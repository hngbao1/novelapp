package com.novel.assistant.ui.novel.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.local.dao.ChapterDao
import com.novel.assistant.data.local.dao.NovelDao
import com.novel.assistant.data.local.entity.ChapterEntity
import com.novel.assistant.data.local.entity.NovelEntity
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.novel.assistant.data.remote.ai.StyleAnalyzer

data class CreationUiState(
    val title: String = "",
    val description: String = "",
    val sourceNovelName: String = "",
    val sourceNovelDescription: String = "",
    val selectedVibes: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val savedNovelId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val styleAnalyzer: StyleAnalyzer
) : ViewModel() {

    private val gson = Gson()
    private val _uiState = MutableStateFlow(CreationUiState())
    val uiState: StateFlow<CreationUiState> = _uiState.asStateFlow()

    val availableVibes = listOf(
        "Buồn nhẹ", "Chữa lành", "Cô đơn", "Căng thẳng",
        "Ngượng ngùng", "Tình cảm chậm", "Buồn man mác", "Vui vẻ",
        "Lãng mạn", "Đau lòng", "Ấm áp"
    )

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun updateDescription(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun updateSourceName(value: String) {
        _uiState.value = _uiState.value.copy(sourceNovelName = value)
    }

    fun updateSourceDescription(value: String) {
        _uiState.value = _uiState.value.copy(sourceNovelDescription = value)
    }

    fun toggleVibe(vibe: String) {
        val current = _uiState.value.selectedVibes.toMutableList()
        if (current.contains(vibe)) current.remove(vibe) else current.add(vibe)
        _uiState.value = _uiState.value.copy(selectedVibes = current)
    }

    fun createNovel() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Vui lòng nhập tên truyện")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                val novelId = novelDao.insertNovel(
                    NovelEntity(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        sourceNovelName = state.sourceNovelName.trim(),
                        sourceNovelDescription = state.sourceNovelDescription.trim(),
                        currentMood = state.selectedVibes.firstOrNull() ?: "",
                        styleVibeTags = gson.toJson(state.selectedVibes)
                    )
                )
                // Auto-create Chapter 1
                chapterDao.insertChapter(
                    ChapterEntity(novelId = novelId, title = "Chương 1", orderIndex = 0)
                )
                
                // Analyze style if source description is provided
                if (state.sourceNovelDescription.isNotBlank()) {
                    try {
                        styleAnalyzer.analyzeOriginalNovel(novelId, state.sourceNovelDescription, state.sourceNovelName)
                    } catch (e: Exception) {
                        // ignore or log, don't fail novel creation
                    }
                }
                
                _uiState.value = _uiState.value.copy(savedNovelId = novelId, isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Lỗi tạo truyện: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
}
