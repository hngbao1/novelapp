package com.novel.assistant.ui.novel.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChapterWithScenes(val chapter: ChapterEntity, val scenes: List<SceneEntity>)

data class ReaderUiState(
    val novelTitle: String = "",
    val chaptersWithScenes: List<ChapterWithScenes> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val sceneDao: SceneDao
) : ViewModel() {
    private val novelId: Long = savedStateHandle.get<Long>("novelId") ?: 0L
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val novel = novelDao.getNovelById(novelId)
            _uiState.value = _uiState.value.copy(novelTitle = novel?.title ?: "")
        }
        viewModelScope.launch {
            chapterDao.getChaptersByNovel(novelId).combine(sceneDao.getScenesByNovel(novelId)) { chapters, scenes ->
                chapters.map { ch -> ChapterWithScenes(ch, scenes.filter { it.chapterId == ch.id }.sortedBy { it.orderIndex }) }
            }.collect { _uiState.value = _uiState.value.copy(chaptersWithScenes = it, isLoading = false) }
        }
    }

    fun toggleBookmark(sceneId: Long) {
        viewModelScope.launch {
            val scene = sceneDao.getSceneById(sceneId) ?: return@launch
            sceneDao.setBookmark(sceneId, !scene.isBookmarked)
        }
    }

    fun toggleFavorite(sceneId: Long) {
        viewModelScope.launch {
            val scene = sceneDao.getSceneById(sceneId) ?: return@launch
            sceneDao.setFavorite(sceneId, !scene.isFavorite)
        }
    }
}
