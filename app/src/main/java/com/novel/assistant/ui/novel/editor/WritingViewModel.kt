package com.novel.assistant.ui.novel.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.backup.BackupManager
import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.datastore.AppPreferences
import com.novel.assistant.data.local.entity.*
import com.novel.assistant.data.remote.ai.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WritingUiState(
    val novel: NovelEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val scenes: List<SceneEntity> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val currentChapterId: Long? = null,
    val currentSceneContent: String = "",
    val userPrompt: String = "",
    val isGenerating: Boolean = false,
    val isRoleplayMode: Boolean = false,
    val roleplayCharacterName: String = "",
    val promptSettings: PromptSettings = PromptSettings(),
    val selectedCharacterIds: List<Long> = emptyList(),
    val showPromptBuilder: Boolean = false,
    val showChapterDrawer: Boolean = false,
    val showStatusSheet: Boolean = false,
    val error: String? = null,
    val generatedContent: String = "",
    val showSaveDialog: Boolean = false,
    val saveSceneTitle: String = "",
    val activeSceneId: Long? = null
)

@HiltViewModel
class WritingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val sceneDao: SceneDao,
    private val sceneVersionDao: SceneVersionDao,
    private val characterDao: CharacterDao,
    private val memoryDao: MemoryDao,
    private val timelineEventDao: TimelineEventDao,
    private val styleReferenceDao: StyleReferenceDao,
    private val contextBuilder: ContextBuilder,
    private val aiProvider: AiProvider,
    private val backupManager: BackupManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val novelId: Long = savedStateHandle.get<Long>("novelId") ?: 0L
    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()
    private var generationJob: Job? = null
    private val gson = Gson()

    init {
        loadNovelData()
    }

    private fun loadNovelData() {
        viewModelScope.launch {
            // Load novel
            novelDao.observeNovel(novelId).collect { novel ->
                _uiState.value = _uiState.value.copy(novel = novel)
            }
        }
        viewModelScope.launch {
            chapterDao.getChaptersByNovel(novelId).collect { chapters ->
                val state = _uiState.value
                _uiState.value = state.copy(
                    chapters = chapters,
                    currentChapterId = state.currentChapterId ?: chapters.firstOrNull()?.id
                )
            }
        }
        viewModelScope.launch {
            sceneDao.getScenesByNovel(novelId).collect { scenes ->
                _uiState.value = _uiState.value.copy(scenes = scenes)
            }
        }
        viewModelScope.launch {
            characterDao.getCharactersByNovel(novelId).collect { characters ->
                _uiState.value = _uiState.value.copy(characters = characters)
            }
        }
    }

    fun updateUserPrompt(text: String) {
        _uiState.value = _uiState.value.copy(userPrompt = text)
    }

    fun updatePromptSettings(settings: PromptSettings) {
        _uiState.value = _uiState.value.copy(promptSettings = settings)
    }

    fun toggleRoleplayMode() {
        val state = _uiState.value
        val mainChar = state.characters.firstOrNull { it.isMainCharacter }
        _uiState.value = state.copy(
            isRoleplayMode = !state.isRoleplayMode,
            roleplayCharacterName = mainChar?.name ?: ""
        )
    }

    fun setRoleplayCharacter(name: String) {
        _uiState.value = _uiState.value.copy(roleplayCharacterName = name)
    }

    fun toggleCharacterSelection(charId: Long) {
        val current = _uiState.value.selectedCharacterIds.toMutableList()
        if (current.contains(charId)) current.remove(charId) else current.add(charId)
        _uiState.value = _uiState.value.copy(selectedCharacterIds = current)
    }

    fun showPromptBuilder(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPromptBuilder = show)
    }

    fun showChapterDrawer(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChapterDrawer = show)
    }

    fun showStatusSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showStatusSheet = show)
    }

    fun selectChapter(chapterId: Long) {
        _uiState.value = _uiState.value.copy(currentChapterId = chapterId)
    }

    fun loadScene(scene: SceneEntity) {
        _uiState.value = _uiState.value.copy(
            generatedContent = scene.content,
            currentSceneContent = scene.content,
            userPrompt = scene.userPrompt,
            currentChapterId = scene.chapterId,
            saveSceneTitle = scene.title,
            activeSceneId = scene.id
        )
    }

    fun generateScene() {
        val state = _uiState.value
        if (state.userPrompt.isBlank()) return

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _uiState.value = state.copy(
                isGenerating = true,
                generatedContent = "",
                error = null
            )

            try {
                val request = contextBuilder.buildFullContext(
                    novelId = novelId,
                    settings = state.promptSettings,
                    selectedCharIds = state.selectedCharacterIds.ifEmpty { null },
                    isRoleplay = state.isRoleplayMode,
                    roleplayCharName = state.roleplayCharacterName
                ).copy(userPrompt = state.userPrompt)

                val contentBuilder = StringBuilder()

                aiProvider.generateScene(request).collect { chunk ->
                    contentBuilder.append(chunk)
                    _uiState.value = _uiState.value.copy(
                        generatedContent = contentBuilder.toString()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    currentSceneContent = contentBuilder.toString()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Lỗi: ${e.message}"
                )
            }
        }
    }

    fun refineScene(instruction: String) {
        val state = _uiState.value
        if (state.generatedContent.isBlank()) return

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val activeSceneId = state.activeSceneId
            if (activeSceneId != null) {
                saveCurrentVersion(activeSceneId, state.generatedContent)
            }

            // Save current version before refining
            _uiState.value = state.copy(isGenerating = true, error = null)

            try {
                val charContext = contextBuilder.buildCharacterContext(
                    novelId, state.selectedCharacterIds.ifEmpty { null }
                )

                val contentBuilder = StringBuilder()
                aiProvider.refineScene(
                    currentContent = state.generatedContent,
                    instruction = instruction,
                    context = charContext
                ).collect { chunk ->
                    contentBuilder.append(chunk)
                    _uiState.value = _uiState.value.copy(
                        generatedContent = contentBuilder.toString()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    currentSceneContent = contentBuilder.toString()
                )
                if (activeSceneId != null) {
                    sceneDao.updateContent(activeSceneId, contentBuilder.toString())
                    novelDao.touchNovel(novelId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Lỗi chỉnh sửa: ${e.message}"
                )
            }
        }
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSaveDialog = show)
    }

    fun updateSaveSceneTitle(title: String) {
        _uiState.value = _uiState.value.copy(saveSceneTitle = title)
    }

    fun saveScene() {
        val state = _uiState.value
        val chapterId = state.currentChapterId ?: return
        if (state.generatedContent.isBlank()) return

        viewModelScope.launch {
            try {
                val sceneId = if (state.activeSceneId != null) {
                    val existing = sceneDao.getSceneById(state.activeSceneId) ?: return@launch
                    if (existing.content != state.generatedContent) {
                        saveCurrentVersion(state.activeSceneId, existing.content)
                    }
                    sceneDao.updateScene(
                        existing.copy(
                            title = state.saveSceneTitle.ifBlank { existing.title },
                            content = state.generatedContent,
                            userPrompt = state.userPrompt,
                            promptSettings = gson.toJson(state.promptSettings),
                            mood = state.promptSettings.mood,
                            vibeTags = gson.toJson(state.promptSettings.vibeTags),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    state.activeSceneId
                } else {
                    val orderIndex = sceneDao.getNextOrderIndex(chapterId)
                    val scene = SceneEntity(
                        chapterId = chapterId,
                        novelId = novelId,
                        title = state.saveSceneTitle.ifBlank { "Phân cảnh ${orderIndex + 1}" },
                        content = state.generatedContent,
                        userPrompt = state.userPrompt,
                        promptSettings = gson.toJson(state.promptSettings),
                        mood = state.promptSettings.mood,
                        vibeTags = gson.toJson(state.promptSettings.vibeTags),
                        orderIndex = orderIndex
                    )
                    val newSceneId = sceneDao.insertScene(scene)
                    sceneVersionDao.insertVersion(
                        SceneVersionEntity(
                            sceneId = newSceneId,
                            content = state.generatedContent,
                            versionNumber = 1
                        )
                    )
                    newSceneId
                }

                // Update novel mood
                if (state.promptSettings.mood.isNotBlank()) {
                    novelDao.updateMood(novelId, state.promptSettings.mood)
                }
                novelDao.touchNovel(novelId)

                analyzeSavedScene(sceneId, state.generatedContent, state.saveSceneTitle)

                // Auto-backup
                if (appPreferences.autoBackup.first()) {
                    backupManager.autoBackup(novelId)
                }

                _uiState.value = _uiState.value.copy(
                    showSaveDialog = false,
                    saveSceneTitle = "",
                    userPrompt = "",
                    activeSceneId = sceneId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Lỗi lưu: ${e.message}")
            }
        }
    }

    fun addChapter(title: String) {
        viewModelScope.launch {
            val orderIndex = chapterDao.getNextOrderIndex(novelId)
            chapterDao.insertChapter(
                ChapterEntity(
                    novelId = novelId,
                    title = title.ifBlank { "Chương ${orderIndex + 1}" },
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun toggleFavorite(sceneId: Long, notes: String = "") {
        viewModelScope.launch {
            val scene = sceneDao.getSceneById(sceneId) ?: return@launch
            sceneDao.setFavorite(sceneId, !scene.isFavorite, notes)
            if (!scene.isFavorite) {
                saveStyleReference(scene.copy(isFavorite = true), notes)
            }
        }
    }

    fun toggleCurrentFavorite(notes: String = "") {
        val state = _uiState.value
        val sceneId = state.activeSceneId
        if (sceneId == null) {
            _uiState.value = state.copy(error = "Hãy lưu phân cảnh trước khi đánh dấu yêu thích")
            return
        }
        toggleFavorite(sceneId, notes)
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    private suspend fun saveCurrentVersion(sceneId: Long, content: String) {
        if (content.isBlank()) return
        val nextVersion = sceneVersionDao.getNextVersionNumber(sceneId)
        sceneVersionDao.insertVersion(
            SceneVersionEntity(
                sceneId = sceneId,
                content = content,
                versionNumber = nextVersion
            )
        )
    }

    private suspend fun analyzeSavedScene(sceneId: Long, content: String, sceneTitle: String) {
        val storyContext = contextBuilder.buildStoryContext(novelId)
        val summary = aiProvider.summarizeScene(content)
        val timelineText = summary.ifBlank { sceneTitle.ifBlank { content.take(140) } }

        if (timelineText.isNotBlank()) {
            val order = timelineEventDao.getNextOrderIndex(novelId)
            timelineEventDao.insertEvent(
                TimelineEventEntity(
                    novelId = novelId,
                    sceneId = sceneId,
                    chapterId = _uiState.value.currentChapterId,
                    eventDescription = timelineText,
                    eventType = "plot_point",
                    orderIndex = order
                )
            )
        }

        val charactersByName = characterDao.getCharactersByNovelOnce(novelId)
            .associateBy { it.name.lowercase() }
        aiProvider.analyzeForMemories(content, storyContext).forEach { suggestion ->
            val relatedIds = suggestion.relatedCharacterNames.mapNotNull { name ->
                charactersByName[name.lowercase()]?.id
            }
            memoryDao.insertMemory(
                MemoryEntity(
                    novelId = novelId,
                    content = suggestion.content,
                    summary = suggestion.content.take(180),
                    type = suggestion.type,
                    category = suggestion.category,
                    relatedCharacterIds = gson.toJson(relatedIds),
                    relatedSceneId = sceneId
                )
            )
        }
    }

    private suspend fun saveStyleReference(scene: SceneEntity, notes: String) {
        styleReferenceDao.insertStyle(
            StyleReferenceEntity(
                novelId = novelId,
                sceneId = scene.id,
                rhythmNotes = notes,
                emotionStyle = scene.mood,
                sampleText = scene.content.take(900)
            )
        )
    }
}
