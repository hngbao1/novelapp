package com.novel.assistant.ui.novel.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novel.assistant.data.local.entity.ChapterEntity
import com.novel.assistant.data.local.entity.CharacterEntity
import com.novel.assistant.data.local.entity.NovelEntity
import com.novel.assistant.data.local.entity.SceneEntity
import com.novel.assistant.data.local.entity.SceneVersionEntity
import com.novel.assistant.data.local.entity.RelationshipEntity
import com.novel.assistant.ui.components.*
import com.novel.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    onBack: () -> Unit,
    onCharacters: (Long) -> Unit,
    onReader: (Long) -> Unit,
    onTimeline: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: WritingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    if (uiState.showSaveDialog) {
        SaveSceneDialog(
            title = uiState.saveSceneTitle,
            onTitleChange = viewModel::updateSaveSceneTitle,
            onSave = viewModel::saveScene,
            onDismiss = { viewModel.showSaveDialog(false) }
        )
    }

    if (uiState.showPromptBuilder) {
        PromptBuilderSheet(
            settings = uiState.promptSettings,
            characters = uiState.characters,
            selectedCharIds = uiState.selectedCharacterIds,
            onSettingsChange = viewModel::updatePromptSettings,
            onToggleCharacter = viewModel::toggleCharacterSelection,
            onDismiss = { viewModel.showPromptBuilder(false) }
        )
    }

    if (uiState.showChapterDrawer) {
        ChapterSceneSheet(
            chapters = uiState.chapters,
            scenes = uiState.scenes,
            currentChapterId = uiState.currentChapterId,
            onChapterClick = viewModel::selectChapter,
            onSceneClick = viewModel::loadScene,
            onDismiss = { viewModel.showChapterDrawer(false) }
        )
    }

    if (uiState.showStatusSheet) {
        StoryStatusSheet(
            novel = uiState.novel,
            characters = uiState.characters,
            relationships = uiState.relationships,
            onUpdateRelationshipDynamics = viewModel::updateRelationshipDynamics,
            onDismiss = { viewModel.showStatusSheet(false) }
        )
    }

    if (uiState.showVersionHistory) {
        VersionHistorySheet(
            versions = uiState.sceneVersions,
            onRestore = viewModel::restoreVersion,
            onDismiss = { viewModel.showVersionHistory(false) }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.novel?.title ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Đang dùng: ${uiState.providerName}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            if (uiState.isRoleplayMode) {
                                Text("• Nhập vai: ${uiState.roleplayCharacterName}", style = MaterialTheme.typography.labelSmall, color = PinkSoft)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) }
                },
                actions = {
                    IconButton(onClick = { viewModel.showChapterDrawer(true) }) {
                        Icon(Icons.Default.List, "Chương và phân cảnh", tint = TextSecondary)
                    }
                    IconButton(onClick = viewModel::toggleRoleplayMode) {
                        Icon(Icons.Default.TheaterComedy, "Nhập vai", tint = if (uiState.isRoleplayMode) PinkSoft else TextHint)
                    }
                    IconButton(onClick = { viewModel.showStatusSheet(true) }) {
                        Icon(Icons.Default.Insights, "Trạng thái truyện", tint = TextSecondary)
                    }
                    IconButton(onClick = { uiState.novel?.id?.let { onTimeline(it) } }) {
                        Icon(Icons.Default.Timeline, "Dòng thời gian", tint = TextSecondary)
                    }
                    IconButton(onClick = { uiState.novel?.id?.let { onCharacters(it) } }) {
                        Icon(Icons.Default.People, "Nhân vật", tint = TextSecondary)
                    }
                    IconButton(onClick = { uiState.novel?.id?.let { onReader(it) } }) {
                        Icon(Icons.Default.MenuBook, "Đọc truyện", tint = TextSecondary)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Cài đặt", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface, titleContentColor = TextPrimary)
            )
        },
        bottomBar = {
            WritingInputBar(
                userPrompt = uiState.userPrompt, isGenerating = uiState.isGenerating, isRoleplayMode = uiState.isRoleplayMode,
                onPromptChange = viewModel::updateUserPrompt, onSend = viewModel::generateScene,
                onStop = viewModel::stopGeneration, onPromptBuilder = { viewModel.showPromptBuilder(true) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState)) {
            AnimatedVisibility(visible = uiState.isAnalyzingScene) {
                Surface(color = PurplePrimary.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PurplePrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI đang phân tích và lưu ký ức ngầm...", style = MaterialTheme.typography.labelSmall, color = PurpleLight)
                    }
                }
            }
            if (uiState.generatedContent.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    NovelContentText(content = uiState.generatedContent, modifier = Modifier.fillMaxWidth())
                    if (uiState.isGenerating) { ShimmerLoading(modifier = Modifier.fillMaxWidth()) }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!uiState.isGenerating) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.showSaveDialog(true) }, shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Lưu phân cảnh")
                            }
                            OutlinedButton(onClick = { viewModel.showVersionHistory(true) }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, DarkDivider), modifier = Modifier.size(width = 56.dp, height = 48.dp), contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.History, "Lịch sử", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                            OutlinedButton(onClick = { viewModel.toggleCurrentFavorite() }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, DarkDivider), modifier = Modifier.size(width = 56.dp, height = 48.dp), contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.FavoriteBorder, "Yêu thích", tint = GoldWarm, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Chỉnh nhanh:", style = MaterialTheme.typography.labelMedium, color = TextHint)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Buồn hơn","Ấm áp hơn","Đúng tính cách hơn","Thêm nội tâm","Thêm khoảng lặng","Ít thoại hơn","Viết dài hơn","Viết chậm hơn")) { text ->
                                QuickActionChip(text = text, onClick = { viewModel.refineScene(text) })
                            }
                        }
                    }
                }
            } else if (!uiState.isGenerating) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("Viết", style = MaterialTheme.typography.headlineLarge, color = PurplePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Mô tả cảnh bạn tưởng tượng\ntrợ lý sẽ viết thành truyện", style = MaterialTheme.typography.bodyMedium, color = TextHint, lineHeight = 22.sp)
                    }
                }
            }
            uiState.error?.let { error ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = RedSoft.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Text(text = error, color = RedSoft, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun WritingInputBar(userPrompt: String, isGenerating: Boolean, isRoleplayMode: Boolean,
    onPromptChange: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit, onPromptBuilder: () -> Unit) {
    Surface(color = DarkSurface, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).navigationBarsPadding(), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onPromptBuilder, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Palette, "Tuỳ chỉnh", tint = PurplePrimary)
            }
            OutlinedTextField(value = userPrompt, onValueChange = onPromptChange,
                placeholder = { Text(if (isRoleplayMode) "Nhập hành động/suy nghĩ…" else "Mô tả cảnh bạn tưởng tượng…", style = MaterialTheme.typography.bodyMedium, color = TextHint) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary.copy(alpha = 0.5f), unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                shape = RoundedCornerShape(20.dp), maxLines = 5, textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
            IconButton(onClick = { if (isGenerating) onStop() else onSend() },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isGenerating) RedSoft else PurplePrimary)) {
                Icon(if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send, if (isGenerating) "Dừng" else "Viết", tint = TextOnPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SaveSceneDialog(title: String, onTitleChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkSurfaceVariant,
        title = { Text("Lưu phân cảnh", color = TextPrimary) },
        text = {
            OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Tên phân cảnh") },
                placeholder = { Text("Ví dụ: Lời tỏ tình trên sân thượng") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint),
                shape = RoundedCornerShape(12.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ", color = TextSecondary) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSceneSheet(
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    currentChapterId: Long?,
    onChapterClick: (Long) -> Unit,
    onSceneClick: (SceneEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextHint) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Chương và phân cảnh", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            if (chapters.isEmpty()) {
                Text("Chưa có chương nào", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            }
            chapters.forEach { chapter ->
                val isCurrent = chapter.id == currentChapterId
                Surface(
                    color = if (isCurrent) PurplePrimary.copy(alpha = 0.16f) else DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(chapter.id) }
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(chapter.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        val chapterScenes = scenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
                        if (chapterScenes.isEmpty()) {
                            Text("Chưa có phân cảnh", style = MaterialTheme.typography.labelMedium, color = TextHint)
                        } else {
                            chapterScenes.forEach { scene ->
                                Text(
                                    text = scene.title.ifBlank { "Phân cảnh ${scene.orderIndex + 1}" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSceneClick(scene)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryStatusSheet(
    novel: NovelEntity?,
    characters: List<CharacterEntity>,
    relationships: List<RelationshipEntity>,
    onUpdateRelationshipDynamics: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextHint) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Trạng thái truyện", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Không khí hiện tại", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text(
                text = novel?.currentMood?.takeIf { it.isNotBlank() } ?: "Chưa xác định",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Text("Nhân vật", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            if (characters.isEmpty()) {
                Text("Chưa có nhân vật nào", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            } else {
                characters.forEach { character ->
                    Surface(color = DarkCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(character.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = character.currentEmotionalState.ifBlank { "Chưa có trạng thái cảm xúc" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (relationships.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quan hệ & Dynamics", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                val characterMap = characters.associateBy { it.id }
                
                relationships.forEach { rel ->
                    val char1 = characterMap[rel.char1Id]?.name ?: "?"
                    val char2 = characterMap[rel.char2Id]?.name ?: "?"
                    var dynamicsText by remember(rel.id, rel.dynamics) { mutableStateOf(rel.dynamics) }
                    
                    Surface(color = DarkCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$char1 ↔ $char2", style = MaterialTheme.typography.titleSmall, color = PurpleLight)
                            if (rel.status.isNotBlank()) {
                                Text("Status: ${rel.status}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            
                            OutlinedTextField(
                                value = dynamicsText,
                                onValueChange = { dynamicsText = it; onUpdateRelationshipDynamics(rel.id, it) },
                                label = { Text("Dynamics / Chemistry") },
                                placeholder = { Text("Ví dụ: Căng thẳng ngầm, thả thính...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                                    cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            
                            // Gợi ý nhanh
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val suggestions = listOf("slow burn", "tension", "awkward", "healing", "unspoken feelings", "rivals", "protective", "clingy", "avoidant")
                                suggestions.forEach { tag ->
                                    val isSelected = dynamicsText.contains(tag, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newText = if (isSelected) {
                                                dynamicsText.replace(Regex("(?i)$tag,?\\s*"), "").trim().removeSuffix(",")
                                            } else {
                                                if (dynamicsText.isBlank()) tag else "$dynamicsText, $tag"
                                            }
                                            dynamicsText = newText
                                            onUpdateRelationshipDynamics(rel.id, newText)
                                        },
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurplePrimary.copy(alpha = 0.2f),
                                            selectedLabelColor = PurpleLight,
                                            containerColor = DarkBackground,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = DarkDivider,
                                            selectedBorderColor = PurplePrimary,
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionHistorySheet(
    versions: List<SceneVersionEntity>,
    onRestore: (SceneVersionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextHint) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Lịch sử phiên bản", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            if (versions.isEmpty()) {
                Text("Chưa có phiên bản nào trước đó", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(versions.sortedByDescending { it.createdAt }) { version ->
                        Surface(color = DarkCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    val time = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(version.createdAt))
                                    Text("Phiên bản ${version.versionNumber}", style = MaterialTheme.typography.titleSmall, color = PurplePrimary)
                                    Text(time, style = MaterialTheme.typography.labelSmall, color = TextHint)
                                }
                                Text(
                                    text = version.content.take(80).replace("\n", " ") + "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = { onRestore(version) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Khôi phục", color = PurplePrimary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
