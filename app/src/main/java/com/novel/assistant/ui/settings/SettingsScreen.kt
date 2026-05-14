package com.novel.assistant.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.novel.assistant.data.backup.ConflictResolution
import com.novel.assistant.data.backup.FullBackup
import com.novel.assistant.data.local.datastore.AppPreferences
import com.novel.assistant.data.remote.ai.GeminiProvider
import com.novel.assistant.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(val fontSize: Float = 17f, val lineHeight: Float = 30f, val modelName: String = "", val autoBackup: Boolean = true, val isExporting: Boolean = false, val showConflictDialog: Boolean = false, val pendingImport: FullBackup? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val backupManager: BackupManager,
    private val geminiProvider: GeminiProvider
) : ViewModel() {

    // Since combine doesn't easily let us mutate UI state properties like showConflictDialog independently, we should separate persistent flow from UI state flow.
    private val _uiState = MutableStateFlow(SettingsUiState())
    val viewState: StateFlow<SettingsUiState> = combine(
        prefs.fontSize, prefs.lineHeight, prefs.modelName, prefs.autoBackup, _uiState
    ) { fs, lh, mn, ab, state ->
        state.copy(fontSize = fs, lineHeight = lh, modelName = mn, autoBackup = ab)
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
    fun startImport(jsonString: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val fullBackup = backupManager.parseBackup(jsonString)
                if (fullBackup == null || fullBackup.novels.isEmpty()) {
                    onResult("File sao lưu không hợp lệ")
                    return@launch
                }
                
                if (backupManager.checkConflicts(fullBackup)) {
                    _uiState.value = _uiState.value.copy(showConflictDialog = true, pendingImport = fullBackup)
                } else {
                    backupManager.importBackup(fullBackup)
                    onResult("Khôi phục thành công")
                }
            } catch (e: Exception) {
                onResult("Lỗi khôi phục: ${e.message}")
            }
        }
    }

    fun resolveImportConflict(resolution: ConflictResolution, onResult: (String) -> Unit) {
        val fullBackup = _uiState.value.pendingImport
        _uiState.value = _uiState.value.copy(showConflictDialog = false, pendingImport = null)
        
        if (fullBackup != null && resolution != ConflictResolution.SKIP) {
            viewModelScope.launch {
                try {
                    backupManager.importBackup(fullBackup, resolution)
                    onResult("Khôi phục thành công")
                } catch (e: Exception) {
                    onResult("Lỗi khôi phục: ${e.message}")
                }
            }
        }
    }

    fun dismissConflictDialog() {
        _uiState.value = _uiState.value.copy(showConflictDialog = false, pendingImport = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val jsonString = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (!jsonString.isNullOrBlank()) {
                viewModel.startImport(jsonString) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
            }
        }
    }

    if (viewState.showConflictDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflictDialog() },
            containerColor = DarkSurfaceVariant,
            title = { Text("Trùng lặp dữ liệu", color = TextPrimary) },
            text = { Text("Bản sao lưu có chứa các truyện đã tồn tại trong ứng dụng. Bạn muốn làm gì?", color = TextSecondary) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.resolveImportConflict(ConflictResolution.OVERWRITE) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RedSoft)) { Text("Ghi đè (Xóa bản cũ)", color = TextOnPrimary) }
                    Button(onClick = { viewModel.resolveImportConflict(ConflictResolution.KEEP_BOTH) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Giữ cả hai (Tạo bản sao)", color = TextOnPrimary) }
                    TextButton(onClick = { viewModel.resolveImportConflict(ConflictResolution.SKIP) { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } }, modifier = Modifier.fillMaxWidth()) { Text("Bỏ qua truyện trùng", color = TextSecondary) }
                }
            }
        )
    }

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
            SettingsSlider("Cỡ chữ", viewState.fontSize, 14f..24f) { viewModel.setFontSize(it) }
            SettingsSlider("Khoảng cách dòng", viewState.lineHeight, 24f..40f) { viewModel.setLineHeight(it) }

            Spacer(Modifier.height(8.dp))

            // Writing assistant settings
            SettingsSection("Trợ lý viết")
            var modelInput by remember(viewState.modelName) { mutableStateOf(viewState.modelName) }
            OutlinedTextField(value = modelInput, onValueChange = { modelInput = it },
                label = { Text("Mẫu viết") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider, cursorColor = PurplePrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = PurplePrimary, unfocusedLabelColor = TextHint),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                trailingIcon = { if (modelInput != viewState.modelName) IconButton(onClick = { viewModel.setModelName(modelInput) }) { Icon(Icons.Default.Check, "Lưu", tint = GreenSoft) } })

            Spacer(Modifier.height(8.dp))

            // Backup
            SettingsSection("Sao lưu dữ liệu")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tự động sao lưu", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = viewState.autoBackup, onCheckedChange = viewModel::setAutoBackup, colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary, checkedTrackColor = PurplePrimary.copy(alpha = 0.3f)))
            }
            Text("Lưu vào thư mục sao lưu riêng của ứng dụng", style = MaterialTheme.typography.labelSmall, color = TextHint)

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.exportAll { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardElevated)) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Xuất", color = TextPrimary)
                }
                Button(onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Khôi phục", color = TextOnPrimary)
                }
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
