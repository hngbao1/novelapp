package com.novel.assistant.ui.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novel.assistant.ui.components.EmptyState
import com.novel.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(onBack: () -> Unit, viewModel: CharacterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddDialog) {
        CharacterFormDialog(uiState = uiState, viewModel = viewModel, onDismiss = { viewModel.showAddDialog(false) })
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(title = { Text("Nhân vật") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary))
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog(true) }, containerColor = PurplePrimary, contentColor = TextOnPrimary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.PersonAdd, "Thêm nhân vật")
            }
        }
    ) { padding ->
        if (uiState.characters.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { EmptyState("Chưa có nhân vật nào\nBấm + để thêm") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.characters, key = { it.id }) { char ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(char.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        if (char.isMainCharacter) { Surface(shape = RoundedCornerShape(8.dp), color = PurplePrimary.copy(alpha = 0.2f)) { Text("Chính", style = MaterialTheme.typography.labelSmall, color = PurpleLight, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) } }
                                    }
                                    if (char.currentEmotionalState.isNotBlank()) { Text("💭 ${char.currentEmotionalState}", style = MaterialTheme.typography.labelMedium, color = TextHint, modifier = Modifier.padding(top = 4.dp)) }
                                }
                                IconButton(onClick = { viewModel.editCharacter(char) }) { Icon(Icons.Default.Edit, "Sửa", tint = TextHint) }
                            }
                            if (char.personality.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("Tính cách: ${char.personality}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                            if (char.speechStyle.isNotBlank()) { Text("Cách nói: ${char.speechStyle}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterFormDialog(uiState: CharacterUiState, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkSurfaceVariant,
        title = { Text(if (uiState.editingCharacter != null) "Sửa nhân vật" else "Thêm nhân vật", color = TextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val tfColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint)
                OutlinedTextField(uiState.name, viewModel::updateName, label = { Text("Tên") }, colors = tfColors, shape = RoundedCornerShape(12.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.personality, viewModel::updatePersonality, label = { Text("Tính cách") }, placeholder = { Text("Ví dụ: hay tự trách, càng buồn càng ít nói") }, colors = tfColors, shape = RoundedCornerShape(12.dp), minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.speechStyle, viewModel::updateSpeechStyle, label = { Text("Cách nói chuyện") }, placeholder = { Text("Ví dụ: nói ngắn, ít dùng emoji") }, colors = tfColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.fears, viewModel::updateFears, label = { Text("Nỗi sợ") }, colors = tfColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.importantThings, viewModel::updateImportantThings, label = { Text("Điều quan trọng") }, colors = tfColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(uiState.description, viewModel::updateDescription, label = { Text("Mô tả thêm") }, colors = tfColors, shape = RoundedCornerShape(12.dp), minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.isMainCharacter, onCheckedChange = { viewModel.toggleMainCharacter() }, colors = CheckboxDefaults.colors(checkedColor = PurplePrimary, uncheckedColor = TextHint))
                    Text("Nhân vật chính", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { Button(onClick = viewModel::saveCharacter, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ", color = TextSecondary) } })
}
