package com.novel.assistant.ui.novel.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.local.dao.TimelineEventDao
import com.novel.assistant.data.local.entity.TimelineEventEntity
import com.novel.assistant.ui.components.EmptyState
import com.novel.assistant.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventDao: TimelineEventDao
) : ViewModel() {
    private val novelId: Long = savedStateHandle.get<Long>("novelId") ?: 0L
    val events: StateFlow<List<TimelineEventEntity>> = eventDao.getEventsByNovel(novelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(description: String, type: String) {
        viewModelScope.launch {
            val order = eventDao.getNextOrderIndex(novelId)
            eventDao.insertEvent(TimelineEventEntity(novelId = novelId, eventDescription = description, eventType = type, orderIndex = order))
        }
    }

    fun deleteEvent(event: TimelineEventEntity) { viewModelScope.launch { eventDao.deleteEvent(event) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(onBack: () -> Unit, viewModel: TimelineViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddEventDialog(onAdd = { desc, type -> viewModel.addEvent(desc, type); showAddDialog = false }, onDismiss = { showAddDialog = false })
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(title = { Text("Dòng thời gian") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary))
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PurplePrimary, contentColor = TextOnPrimary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "Thêm sự kiện")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { EmptyState("Chưa có sự kiện nào trong timeline") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(events, key = { it.id }) { event ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        // Timeline line + dot
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            val dotColor = getEventColor(event.eventType)
                            Icon(Icons.Default.Circle, null, tint = dotColor, modifier = Modifier.size(12.dp))
                            if (events.last() != event) {
                                HorizontalDivider(modifier = Modifier.width(2.dp).height(40.dp), color = DarkDivider)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.eventDescription, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(getEventTypeLabel(event.eventType), style = MaterialTheme.typography.labelSmall, color = TextHint, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEventDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("plot_point") }
    val types = listOf("plot_point" to "Cốt truyện", "secret_revealed" to "Bí mật", "relationship_change" to "Quan hệ", "arc_start" to "Bắt đầu arc", "arc_end" to "Kết thúc arc")

    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkSurfaceVariant,
        title = { Text("Thêm sự kiện", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(description, { description = it }, label = { Text("Mô tả sự kiện") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint),
                    shape = RoundedCornerShape(12.dp), minLines = 2)
                Text("Loại sự kiện:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                types.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedType == key, onClick = { selectedType = key }, colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary, unselectedColor = TextHint))
                        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (description.isNotBlank()) onAdd(description, selectedType) }, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ", color = TextSecondary) } })
}

private fun getEventColor(type: String): Color = when (type) {
    "plot_point" -> PurplePrimary; "secret_revealed" -> GoldWarm; "relationship_change" -> PinkSoft; "arc_start" -> GreenSoft; "arc_end" -> BlueSky; else -> TextHint
}

private fun getEventTypeLabel(type: String): String = when (type) {
    "plot_point" -> "Cốt truyện"; "secret_revealed" -> "Bí mật"; "relationship_change" -> "Quan hệ thay đổi"; "arc_start" -> "Bắt đầu arc"; "arc_end" -> "Kết thúc arc"; else -> ""
}
