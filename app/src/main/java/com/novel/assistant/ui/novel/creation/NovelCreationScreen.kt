package com.novel.assistant.ui.novel.creation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Using built-in Compose FlowRow from ExperimentalLayoutApi
import com.novel.assistant.ui.components.VibeTagChip
import com.novel.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovelCreationScreen(
    onBack: () -> Unit,
    onNovelCreated: (Long) -> Unit,
    viewModel: CreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when saved
    LaunchedEffect(uiState.savedNovelId) {
        uiState.savedNovelId?.let { onNovelCreated(it) }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Tạo truyện mới") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Novel name
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Tên truyện") },
                placeholder = { Text("Ví dụ: Lily of the Valley After Story") },
                modifier = Modifier.fillMaxWidth(),
                colors = novelTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Mô tả truyện") },
                placeholder = { Text("Truyện kể về điều gì?") },
                modifier = Modifier.fillMaxWidth(),
                colors = novelTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )

            // Source novel (optional)
            Text(
                "Truyện gốc (không bắt buộc)",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )

            OutlinedTextField(
                value = uiState.sourceNovelName,
                onValueChange = viewModel::updateSourceName,
                label = { Text("Tên truyện gốc") },
                placeholder = { Text("Tên truyện gốc bạn muốn viết tiếp hoặc viết ngoại truyện") },
                modifier = Modifier.fillMaxWidth(),
                colors = novelTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.sourceNovelDescription,
                onValueChange = viewModel::updateSourceDescription,
                label = { Text("Mô tả truyện gốc") },
                placeholder = { Text("Nội dung chính, nhân vật, phong cách...") },
                modifier = Modifier.fillMaxWidth(),
                colors = novelTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )

            // Vibe tags
            Text(
                "Không khí truyện",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.availableVibes.forEach { vibe ->
                    VibeTagChip(
                        tag = vibe,
                        isSelected = uiState.selectedVibes.contains(vibe),
                        onClick = { viewModel.toggleVibe(vibe) },
                        color = getVibeColor(vibe)
                    )
                }
            }

            // Error
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = RedSoft,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Create button
            Button(
                onClick = viewModel::createNovel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextOnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Bắt đầu viết", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun novelTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = DarkDivider,
    focusedLabelColor = PurplePrimary,
    unfocusedLabelColor = TextHint,
    cursorColor = PurplePrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedPlaceholderColor = TextHint,
    unfocusedPlaceholderColor = TextHint,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface
)

private fun getVibeColor(vibe: String): androidx.compose.ui.graphics.Color = when (vibe) {
    "Buồn nhẹ" -> VibeSad
    "Chữa lành" -> VibeHealing
    "Cô đơn" -> VibeLonely
    "Căng thẳng" -> VibeTense
    "Ngượng ngùng" -> VibeAwkward
    "Tình cảm chậm" -> VibeSlowBurn
    "Buồn man mác" -> VibeMelancholy
    "Vui vẻ" -> VibeHappy
    "Lãng mạn" -> VibeRomantic
    "Đau lòng" -> VibeTense
    "Ấm áp" -> GoldWarm
    else -> PurplePrimary
}
