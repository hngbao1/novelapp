package com.novel.assistant.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.local.dao.NovelDao
import com.novel.assistant.data.local.dao.SceneDao
import com.novel.assistant.data.local.entity.NovelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovelWithLastScene(
    val novel: NovelEntity,
    val lastSceneTitle: String = ""
)

data class HomeUiState(
    val novels: List<NovelWithLastScene> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val novelDao: NovelDao,
    private val sceneDao: SceneDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadNovels()
    }

    private fun loadNovels() {
        viewModelScope.launch {
            novelDao.getAllNovels().collect { novels ->
                val novelsWithScenes = novels.map { novel ->
                    val lastScene = sceneDao.getRecentScenes(novel.id, 1).firstOrNull()
                    NovelWithLastScene(
                        novel = novel,
                        lastSceneTitle = lastScene?.title ?: ""
                    )
                }
                _uiState.value = HomeUiState(
                    novels = novelsWithScenes,
                    isLoading = false
                )
            }
        }
    }

    fun deleteNovel(novel: NovelEntity) {
        viewModelScope.launch {
            novelDao.deleteNovel(novel)
        }
    }
}
