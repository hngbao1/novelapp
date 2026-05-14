package com.novel.assistant.ui.novel.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novel.assistant.data.local.entity.CharacterEntity
import com.novel.assistant.data.remote.ai.PromptSettings
import com.novel.assistant.ui.components.VibeTagChip
import com.novel.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromptBuilderSheet(
    settings: PromptSettings,
    characters: List<CharacterEntity>,
    selectedCharIds: List<Long>,
    onSettingsChange: (PromptSettings) -> Unit,
    onToggleCharacter: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextHint) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Tuỳ chỉnh phân cảnh", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

            // Mood
            SectionLabel("Tâm trạng cảnh")
            val moods = listOf("Buồn nhẹ","Cô đơn","Ấm áp","Đau lòng","Căng thẳng","Lãng mạn","Buồn man mác","Vui vẻ","Chữa lành","Ngượng ngùng")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.forEach { mood ->
                    VibeTagChip(tag = mood, isSelected = settings.mood == mood,
                        onClick = { onSettingsChange(settings.copy(mood = if (settings.mood == mood) "" else mood)) })
                }
            }

            // Speed
            SectionLabel("Tốc độ cảnh")
            SingleChoiceRow(options = listOf("Chậm","Vừa","Nhanh"), selected = settings.speed,
                onSelect = { onSettingsChange(settings.copy(speed = it)) })

            // Dialogue level
            SectionLabel("Mức thoại")
            SingleChoiceRow(options = listOf("Ít thoại","Bình thường","Nhiều thoại"), selected = settings.dialogueLevel,
                onSelect = { onSettingsChange(settings.copy(dialogueLevel = it)) })

            // Viewpoint
            SectionLabel("Góc nhìn")
            SingleChoiceRow(options = listOf("Nhân vật chính","Nhân vật khác","Toàn cảnh"), selected = settings.viewpoint,
                onSelect = { onSettingsChange(settings.copy(viewpoint = it)) })

            // Focus
            SectionLabel("Trọng tâm")
            val focuses = listOf("Nội tâm","Tương tác tình cảm","Hành động","Cảm xúc","Bất ngờ cốt truyện")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                focuses.forEach { focus ->
                    VibeTagChip(tag = focus, isSelected = settings.focus == focus,
                        onClick = { onSettingsChange(settings.copy(focus = if (settings.focus == focus) "" else focus)) })
                }
            }

            SectionLabel("Không khí phụ")
            val extraVibes = listOf("Chữa lành","Cô đơn","Tình cảm chậm","Dịu dàng","Nặng lòng","Bình yên","Ngượng ngùng","Mơ hồ")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                extraVibes.forEach { vibe ->
                    val selected = settings.vibeTags.contains(vibe)
                    VibeTagChip(
                        tag = vibe,
                        isSelected = selected,
                        onClick = {
                            val next = if (selected) {
                                settings.vibeTags - vibe
                            } else {
                                settings.vibeTags + vibe
                            }
                            onSettingsChange(settings.copy(vibeTags = next))
                        }
                    )
                }
            }

            // Scene Goal
            SectionLabel("Mục tiêu phân cảnh")
            val goals = listOf("Chữa lành","Tăng tension","Tạo khoảng lặng","Build chemistry",
                "Foreshadow","Chuẩn bị confession","Reveal bí mật","Giải quyết xung đột","Phát triển nhân vật")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { goal ->
                    VibeTagChip(tag = goal, isSelected = settings.sceneGoal == goal,
                        onClick = { onSettingsChange(settings.copy(sceneGoal = if (settings.sceneGoal == goal) "" else goal)) })
                }
            }

            // Location
            SectionLabel("Địa điểm")
            var locationInput by remember(settings.location) { mutableStateOf(settings.location) }
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it; onSettingsChange(settings.copy(location = it)) },
                placeholder = { Text("VD: Sân thượng, quán cà phê, bệnh viện...", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                    cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Time
            SectionLabel("Thời gian")
            var timeInput by remember(settings.time) { mutableStateOf(settings.time) }
            OutlinedTextField(
                value = timeInput,
                onValueChange = { timeInput = it; onSettingsChange(settings.copy(time = it)) },
                placeholder = { Text("VD: Ban đêm, sáng sớm, hoàng hôn...", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                    cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Unresolved Topics
            SectionLabel("Chủ đề tồn đọng")
            var unresolvedInput by remember(settings.unresolvedTopics) { mutableStateOf(settings.unresolvedTopics) }
            OutlinedTextField(
                value = unresolvedInput,
                onValueChange = { unresolvedInput = it; onSettingsChange(settings.copy(unresolvedTopics = it)) },
                placeholder = { Text("VD: Lời hứa chưa giữ, bí mật chưa nói...", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                    cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp), minLines = 2, maxLines = 3
            )

            // Characters in scene
            if (characters.isNotEmpty()) {
                SectionLabel("Nhân vật trong cảnh")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    characters.forEach { char ->
                        VibeTagChip(tag = char.name, isSelected = selectedCharIds.contains(char.id),
                            onClick = { onToggleCharacter(char.id) }, color = BlueSky)
                    }
                }
            }

            // Close button
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                Text("Xong")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurplePrimary.copy(alpha = 0.25f),
                    selectedLabelColor = PurpleLight,
                    containerColor = DarkCard,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DarkDivider,
                    selectedBorderColor = PurplePrimary,
                    enabled = true,
                    selected = selected == option
                )
            )
        }
    }
}
