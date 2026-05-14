package com.novel.assistant.ui.character

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.local.dao.CharacterCorrectionDao
import com.novel.assistant.data.local.dao.CharacterDao
import com.novel.assistant.data.local.entity.CharacterCorrectionEntity
import com.novel.assistant.data.local.entity.CharacterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterUiState(
    val characters: List<CharacterEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingCharacter: CharacterEntity? = null,
    val name: String = "", val description: String = "", val personality: String = "",
    val speechStyle: String = "", val fears: String = "", val importantThings: String = "",
    val isMainCharacter: Boolean = false, val emotionalState: String = "",
    val showCorrectionsSheet: Boolean = false,
    val selectedCharacterId: Long? = null,
    val corrections: List<CharacterCorrectionEntity> = emptyList(),
    val correctionType: String = "Giọng điệu",
    val correctionWrong: String = "",
    val correctionRight: String = ""
)

@HiltViewModel
class CharacterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val characterDao: CharacterDao,
    private val correctionDao: CharacterCorrectionDao
) : ViewModel() {
    private val novelId: Long = savedStateHandle.get<Long>("novelId") ?: 0L
    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()
    private var correctionsJob: kotlinx.coroutines.Job? = null

    init { viewModelScope.launch { characterDao.getCharactersByNovel(novelId).collect { _uiState.value = _uiState.value.copy(characters = it) } } }

    fun showAddDialog(show: Boolean) { _uiState.value = _uiState.value.copy(showAddDialog = show, editingCharacter = null, name = "", description = "", personality = "", speechStyle = "", fears = "", importantThings = "", isMainCharacter = false, emotionalState = "") }

    fun editCharacter(char: CharacterEntity) { _uiState.value = _uiState.value.copy(showAddDialog = true, editingCharacter = char, name = char.name, description = char.description, personality = char.personality, speechStyle = char.speechStyle, fears = char.fears, importantThings = char.importantThings, isMainCharacter = char.isMainCharacter, emotionalState = char.currentEmotionalState) }

    fun updateName(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun updateDescription(v: String) { _uiState.value = _uiState.value.copy(description = v) }
    fun updatePersonality(v: String) { _uiState.value = _uiState.value.copy(personality = v) }
    fun updateSpeechStyle(v: String) { _uiState.value = _uiState.value.copy(speechStyle = v) }
    fun updateFears(v: String) { _uiState.value = _uiState.value.copy(fears = v) }
    fun updateImportantThings(v: String) { _uiState.value = _uiState.value.copy(importantThings = v) }
    fun toggleMainCharacter() { _uiState.value = _uiState.value.copy(isMainCharacter = !_uiState.value.isMainCharacter) }

    fun saveCharacter() {
        val s = _uiState.value; if (s.name.isBlank()) return
        viewModelScope.launch {
            val char = (s.editingCharacter ?: CharacterEntity(novelId = novelId, name = "")).copy(
                name = s.name, description = s.description, personality = s.personality, speechStyle = s.speechStyle,
                fears = s.fears, importantThings = s.importantThings, isMainCharacter = s.isMainCharacter,
                currentEmotionalState = s.emotionalState, updatedAt = System.currentTimeMillis()
            )
            if (s.editingCharacter != null) characterDao.updateCharacter(char) else characterDao.insertCharacter(char)
            showAddDialog(false)
        }
    }

    fun deleteCharacter(char: CharacterEntity) { viewModelScope.launch { characterDao.deleteCharacter(char) } }

    fun showCorrections(charId: Long?) {
        _uiState.value = _uiState.value.copy(showCorrectionsSheet = charId != null, selectedCharacterId = charId, correctionWrong = "", correctionRight = "")
        correctionsJob?.cancel()
        if (charId != null) {
            correctionsJob = viewModelScope.launch {
                correctionDao.getCorrectionsByCharacter(charId).collect {
                    _uiState.value = _uiState.value.copy(corrections = it)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(corrections = emptyList())
        }
    }

    fun updateCorrectionType(type: String) { _uiState.value = _uiState.value.copy(correctionType = type) }
    fun updateCorrectionWrong(wrong: String) { _uiState.value = _uiState.value.copy(correctionWrong = wrong) }
    fun updateCorrectionRight(right: String) { _uiState.value = _uiState.value.copy(correctionRight = right) }

    fun saveCorrection() {
        val s = _uiState.value
        val charId = s.selectedCharacterId ?: return
        if (s.correctionWrong.isBlank() || s.correctionRight.isBlank()) return
        viewModelScope.launch {
            correctionDao.insertCorrection(CharacterCorrectionEntity(characterId = charId, novelId = novelId, correctionType = s.correctionType, wrongExample = s.correctionWrong, rightDescription = s.correctionRight))
            _uiState.value = _uiState.value.copy(correctionWrong = "", correctionRight = "")
        }
    }

    fun deleteCorrection(correction: CharacterCorrectionEntity) {
        viewModelScope.launch { correctionDao.deleteCorrection(correction) }
    }
}
