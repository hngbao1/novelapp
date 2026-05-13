package com.novel.assistant.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.backup.BackupManager
import com.novel.assistant.data.local.datastore.AppPreferences
import com.novel.assistant.data.remote.ai.GeminiProvider
import com.novel.assistant.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(val fontSize: Float = 17f, val lineHeight: Float = 30f, val modelName: String = "", val autoBackup: Boolean = true, val isExporting: Boolean = false)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val backupManager: BackupManager,
    private val geminiProvider: GeminiProvider
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(prefs.fontSize, prefs.lineHeight, prefs.modelName, prefs.autoBackup) { fs, lh, mn, ab ->
        SettingsUiState(fontSize = fs, lineHeight = lh, modelName = mn, autoBackup = ab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setFontSize(size: Float) { viewModelScope.launch { prefs.setFontSize(size) } }
    fun setLineHeight(height: Float) { viewModelScope.launch { prefs.setLineHeight(height) } }
    fun setModelName(name: String) {
        viewModelScope.launch {
            prefs.setModelName(name)
            geminiProvider.setModelName(name)
        }
    }
    fun setAutoBackup(enabled: Boolean) { viewModelScope.launch { prefs.setAutoBackup(enabled) } }

    fun exportAll(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportAllToJson()
                val dir = backupManager.getBackupDir()
                val file = java.io.File(dir, "novel_backup_all_${System.currentTimeMillis()}.json")
                file.writeText(json, Charsets.UTF_8)
                onResult("Đã xuất vào: ${file.absolutePath}")
            } catch (e: Exception) { onResult("Lỗi: ${e.message}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(title = { Text("Cài đặt") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary))
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reading settings
            SettingsSection("Tuỳ chỉnh đọc truyện")
            SettingsSlider("Cỡ chữ", uiState.fontSize, 14f..24f) { viewModel.setFontSize(it) }
            SettingsSlider("Khoảng cách dòng", uiState.lineHeight, 24f..40f) { viewModel.setLineHeight(it) }

            Spacer(Modifier.height(8.dp))

            // Writing assistant settings
            SettingsSection("Trợ lý viết")
            var modelInput by remember(uiState.modelName) { mutableStateOf(uiState.modelName) }
            OutlinedTextField(value = modelInput, onValueChange = { modelInput = it },
                label = { Text("Mẫu viết") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                trailingIcon = { if (modelInput != uiState.modelName) IconButton(onClick = { viewModel.setModelName(modelInput) }) { Icon(Icons.Default.Check, "Lưu", tint = GreenSoft) } })

            Spacer(Modifier.height(8.dp))

            // Backup
            SettingsSection("Sao lưu dữ liệu")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tự động sao lưu", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.autoBackup, onCheckedChange = viewModel::setAutoBackup, colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary, checkedTrackColor = PurplePrimary.copy(alpha = 0.3f)))
            }
            Text("Lưu vào thư mục sao lưu riêng của ứng dụng", style = MaterialTheme.typography.labelSmall, color = TextHint)

            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.exportAll { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkCardElevated)) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xuất toàn bộ dữ liệu", color = TextPrimary)
            }

            Spacer(Modifier.height(24.dp))

            // About
            SettingsSection("Thông tin")
            Text("Viết Truyện v1.0.0", style = MaterialTheme.typography.bodySmall, color = TextHint)
            Text("Ứng dụng hỗ trợ biến ý tưởng ngắn thành phân cảnh truyện", style = MaterialTheme.typography.bodySmall, color = TextHint)
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = PurplePrimary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text("${value.toInt()}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary, inactiveTrackColor = DarkDivider))
    }
}
