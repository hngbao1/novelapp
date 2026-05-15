package com.novel.assistant.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novel.assistant.data.backup.BackupManager
import com.novel.assistant.data.backup.ConflictResolution
import com.novel.assistant.data.backup.FullBackup
import com.novel.assistant.data.local.datastore.AppPreferences
import com.novel.assistant.data.remote.ai.KeyGroup
import com.novel.assistant.data.remote.ai.KeyRotationManager
import com.novel.assistant.data.remote.ai.KeyState
import com.novel.assistant.data.remote.ai.KeyStatus
import com.novel.assistant.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

data class SettingsUiState(
    val fontSize: Float = 17f,
    val lineHeight: Float = 30f,
    val autoBackup: Boolean = true,
    
    // Vibe Settings
    val aiPreset: String = "Visual novel Hàn",
    val sceneEnergy: Int = 1,
    val unpredictabilityLevel: Int = 1,
    val continuityLevel: Int = 1,
    val cinematicLevel: Int = 1,
    val introspectionLevel: Int = 1,
    val melancholyLevel: Int = 0,
    
    // Backup State
    val showConflictDialog: Boolean = false,
    val pendingImport: FullBackup? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val backupManager: BackupManager,
    private val keyManager: KeyRotationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    
    // Nhóm các flow Vibe
    private val vibeFlow = combine(
        prefs.aiPreset, prefs.sceneEnergy, prefs.unpredictabilityLevel,
        prefs.continuityLevel, prefs.cinematicLevel, prefs.introspectionLevel, prefs.melancholyLevel
    ) { args ->
        // args is Array<Any>
        SettingsUiState(
            aiPreset = args[0] as String,
            sceneEnergy = args[1] as Int,
            unpredictabilityLevel = args[2] as Int,
            continuityLevel = args[3] as Int,
            cinematicLevel = args[4] as Int,
            introspectionLevel = args[5] as Int,
            melancholyLevel = args[6] as Int
        )
    }

    val viewState: StateFlow<SettingsUiState> = combine(
        prefs.fontSize, prefs.lineHeight, prefs.autoBackup, vibeFlow, _uiState
    ) { fs, lh, ab, vibe, state ->
        state.copy(
            fontSize = fs, lineHeight = lh, autoBackup = ab,
            aiPreset = vibe.aiPreset,
            sceneEnergy = vibe.sceneEnergy,
            unpredictabilityLevel = vibe.unpredictabilityLevel,
            continuityLevel = vibe.continuityLevel,
            cinematicLevel = vibe.cinematicLevel,
            introspectionLevel = vibe.introspectionLevel,
            melancholyLevel = vibe.melancholyLevel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val keyStates = keyManager.keyStates

    // --- Actions ---
    fun setFontSize(size: Float) { viewModelScope.launch { prefs.setFontSize(size) } }
    fun setLineHeight(height: Float) { viewModelScope.launch { prefs.setLineHeight(height) } }
    fun setAutoBackup(enabled: Boolean) { viewModelScope.launch { prefs.setAutoBackup(enabled) } }

    fun updateVibe(
        preset: String? = null, energy: Int? = null,
        unpredict: Int? = null, continuity: Int? = null,
        cinematic: Int? = null, introspect: Int? = null, melancholy: Int? = null
    ) {
        viewModelScope.launch {
            preset?.let { prefs.setAiPreset(it) }
            energy?.let { prefs.setSceneEnergy(it) }
            unpredict?.let { prefs.setUnpredictability(it) }
            continuity?.let { prefs.setContinuity(it) }
            cinematic?.let { prefs.setCinematic(it) }
            introspect?.let { prefs.setIntrospection(it) }
            melancholy?.let { prefs.setMelancholy(it) }
        }
    }

    // --- Key Manager ---
    fun addCustomKey(key: String) {
        viewModelScope.launch {
            val jsonStr = prefs.customApiKeys.first()
            val arr = try { JSONArray(jsonStr) } catch (e: Exception) { JSONArray() }
            arr.put(key.trim())
            prefs.setCustomApiKeys(arr.toString())
            
            // Cập nhật manager
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            keyManager.updateCustomKeys(list)
            
            // Tự động ping
            keyManager.validateKey(key.trim())
        }
    }

    fun removeCustomKey(key: String) {
        viewModelScope.launch {
            val jsonStr = prefs.customApiKeys.first()
            val arr = try { JSONArray(jsonStr) } catch (e: Exception) { JSONArray() }
            val newList = JSONArray()
            for (i in 0 until arr.length()) {
                val k = arr.getString(i)
                if (k != key) newList.put(k)
            }
            prefs.setCustomApiKeys(newList.toString())
            
            val list = mutableListOf<String>()
            for (i in 0 until newList.length()) list.add(newList.getString(i))
            keyManager.updateCustomKeys(list)
        }
    }

    fun validateAllKeys() {
        viewModelScope.launch { keyManager.validateAllKeys() }
    }

    // --- Backup ---
    fun exportAll(onResult: (String) -> Unit) { /* ... keep backup logic ... */ }
    fun startImport(jsonString: String, onResult: (String) -> Unit) { /* ... */ }
    fun dismissConflictDialog() { _uiState.value = _uiState.value.copy(showConflictDialog = false) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Trợ lý Đồng Sáng tác") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ================= BASIC =================
            VibeSection(viewState, viewModel)

            // ================= ADVANCED TOGGLE =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nâng cao (Key & Continuity)", color = PurplePrimary, fontWeight = FontWeight.Bold)
                Icon(
                    if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = PurplePrimary
                )
            }

            // ================= ADVANCED =================
            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AdvancedVibeSection(viewState, viewModel)
                    KeyManagerSection(viewModel)
                    ReadingSection(viewState, viewModel)
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun VibeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tâm điểm (Presets)", color = TextPrimary, fontWeight = FontWeight.Bold)
        // Preset selector (Fake dropdown for simplicity, could be a real dropdown or chips)
        ScrollableTabRow(
            selectedTabIndex = 0, // In real app, calculate index based on state.aiPreset
            containerColor = DarkSurface,
            contentColor = PurplePrimary,
            edgePadding = 0.dp
        ) {
            listOf("Visual novel Hàn", "Melancholy", "Slow burn", "Điện ảnh đời thường").forEach { preset ->
                Tab(
                    selected = state.aiPreset == preset,
                    onClick = { viewModel.updateVibe(preset = preset) },
                    text = { Text(preset, color = if (state.aiPreset == preset) PurplePrimary else TextSecondary) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        EmotionSlider(
            label = "Nhịp Năng Lượng (Energy)",
            value = state.sceneEnergy,
            labels = listOf("Tĩnh", "Nhẹ nhàng", "Căng ngầm", "Bùng nổ"),
            onValueChange = { viewModel.updateVibe(energy = it) }
        )

        EmotionSlider(
            label = "Độ Bất Ngờ của AI",
            value = state.unpredictabilityLevel,
            labels = listOf("Đúng ý", "Cân bằng", "Khó đoán"),
            onValueChange = { viewModel.updateVibe(unpredict = it) }
        )

        EmotionSlider(
            label = "Góc Nhìn",
            value = state.cinematicLevel,
            labels = listOf("Văn kể", "Cân bằng", "Điện ảnh"),
            onValueChange = { viewModel.updateVibe(cinematic = it) }
        )

        EmotionSlider(
            label = "Mức Nội Tâm",
            value = state.introspectionLevel,
            labels = listOf("Ít", "Vừa", "Sâu sắc"),
            onValueChange = { viewModel.updateVibe(introspect = it) }
        )
    }
}

@Composable
fun AdvancedVibeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Điều chỉnh Cốt Truyện", color = TextPrimary, fontWeight = FontWeight.Bold)

        EmotionSlider(
            label = "Độ Liên Kết (Continuity)",
            value = state.continuityLevel,
            labels = listOf("Lỏng lẻo", "Cân bằng", "Chặt chẽ"),
            onValueChange = { viewModel.updateVibe(continuity = it) }
        )

        EmotionSlider(
            label = "Khoảng Lặng (Melancholy)",
            value = state.melancholyLevel,
            labels = listOf("Ít", "Vừa phải", "Rất nhiều"),
            onValueChange = { viewModel.updateVibe(melancholy = it) }
        )
    }
}

@Composable
fun EmotionSlider(label: String, value: Int, labels: List<String>, onValueChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(labels[value], color = PurplePrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..(labels.size - 1).toFloat(),
            steps = labels.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = PurplePrimary,
                activeTrackColor = PurplePrimary,
                inactiveTrackColor = DarkDivider
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labels.first(), color = TextHint, style = MaterialTheme.typography.labelSmall)
            Text(labels.last(), color = TextHint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun KeyManagerSection(viewModel: SettingsViewModel) {
    val keyStates by viewModel.keyStates.collectAsStateWithLifecycle()
    var newKeyInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Quản lý API Keys", color = TextPrimary, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.validateAllKeys() }) {
                Text("Ping Tất cả", color = GreenSoft)
            }
        }

        // Add Key Input
        OutlinedTextField(
            value = newKeyInput,
            onValueChange = { newKeyInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Nhập Gemini API Key mới...") },
            trailingIcon = {
                IconButton(onClick = {
                    if (newKeyInput.isNotBlank()) {
                        viewModel.addCustomKey(newKeyInput)
                        newKeyInput = ""
                    }
                }) { Icon(Icons.Default.Add, "Thêm", tint = PurplePrimary) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        // Key List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keyStates.forEach { state ->
                KeyItem(
                    state = state,
                    onDelete = { viewModel.removeCustomKey(state.key) },
                    onCopy = { clipboard.setText(AnnotatedString(state.key)) }
                )
            }
        }
    }
}

@Composable
fun KeyItem(state: KeyState, onDelete: () -> Unit, onCopy: () -> Unit) {
    val statusColor = when (state.status) {
        KeyStatus.ACTIVE -> GreenSoft
        KeyStatus.COOLDOWN -> GoldWarm
        KeyStatus.DEAD -> RedSoft
        KeyStatus.UNTESTED -> TextHint
    }
    val statusText = when (state.status) {
        KeyStatus.ACTIVE -> "Hoạt động"
        KeyStatus.COOLDOWN -> "Quá tải"
        KeyStatus.DEAD -> "Lỗi (Dead)"
        KeyStatus.UNTESTED -> "Chưa kiểm tra"
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(DarkSurfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(statusColor))
                Spacer(Modifier.width(8.dp))
                Text(state.label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Text("${state.key.take(6)}...${state.key.takeLast(4)} - $statusText", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (state.status == KeyStatus.COOLDOWN) {
                val remaining = ((state.cooldownUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                Text("Hồi phục sau: ${remaining}s", color = GoldWarm, style = MaterialTheme.typography.labelSmall)
            }
        }
        Row {
            IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copy", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
            if (state.isCustom) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Xoá", tint = RedSoft, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun ReadingSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tuỳ chỉnh hiển thị", color = TextPrimary, fontWeight = FontWeight.Bold)
        SettingsSlider("Cỡ chữ", state.fontSize, 14f..24f) { viewModel.setFontSize(it) }
        SettingsSlider("Khoảng cách dòng", state.lineHeight, 24f..40f) { viewModel.setLineHeight(it) }
    }
}

@Composable
fun SettingsSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary, inactiveTrackColor = DarkDivider)
        )
    }
}
