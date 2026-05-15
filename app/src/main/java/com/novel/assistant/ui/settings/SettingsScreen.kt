package com.novel.assistant.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.novel.assistant.data.remote.ai.GeminiProvider
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
    
    // Model
    val modelName: String = "",
    
    // Vibe Settings
    val aiPreset: String = "Visual novel Hàn",
    val sceneEnergy: Int = 1,
    val unpredictabilityLevel: Int = 1,
    val continuityLevel: Int = 1,
    val cinematicLevel: Int = 1,
    val introspectionLevel: Int = 1,
    val melancholyLevel: Int = 0,
    
    // Reader Theme
    val readerTheme: String = "WarmDark",
    
    // Backup State
    val showConflictDialog: Boolean = false,
    val pendingImport: FullBackup? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val backupManager: BackupManager,
    private val keyManager: KeyRotationManager,
    private val geminiProvider: GeminiProvider
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

    private val appStateFlow = combine(
        prefs.fontSize, prefs.lineHeight, prefs.autoBackup, prefs.modelName, prefs.readerTheme
    ) { fs, lh, ab, mn, rt ->
        SettingsUiState(
            fontSize = fs, lineHeight = lh, autoBackup = ab,
            modelName = mn, readerTheme = rt
        )
    }

    val viewState: StateFlow<SettingsUiState> = combine(appStateFlow, vibeFlow) { app, vibe ->
        _uiState.value.copy(
            fontSize = app.fontSize, lineHeight = app.lineHeight, autoBackup = app.autoBackup,
            modelName = app.modelName, readerTheme = app.readerTheme,
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
    fun setModelName(name: String) {
        viewModelScope.launch {
            prefs.setModelName(name)
            if (name.isNotBlank()) geminiProvider.setModelName(name)
        }
    }
    
    fun setReaderTheme(themeName: String) {
        viewModelScope.launch { prefs.setReaderTheme(themeName) }
    }

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VibeSection(viewState, viewModel)

            // Advanced Toggle
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nâng cao", color = PurplePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Icon(
                        if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = PurplePrimary
                    )
                }
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ModelSection(viewState, viewModel)
                    AdvancedVibeSection(viewState, viewModel)
                    KeyManagerSection(viewModel)
                    ReadingSection(viewState, viewModel)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val presets = listOf(
        "Visual novel Hàn" to "🇰🇷",
        "Melancholy" to "🌧️",
        "Slow burn" to "🔥",
        "Healing" to "🌿",
        "Drama nhẹ" to "🎭",
        "Điện ảnh đời thường" to "🎬"
    )

    SettingsCard {
        SectionTitle("✨ Phong cách viết")
        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, emoji) ->
                val isSelected = state.aiPreset == name
                Surface(
                    color = if (isSelected) PurplePrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, PurplePrimary) else BorderStroke(1.dp, DarkDivider),
                    modifier = Modifier.clickable { viewModel.updateVibe(preset = name) }
                ) {
                    Text(
                        "$emoji $name",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (isSelected) PurplePrimary else TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    SettingsCard {
        SectionTitle("🎚️ Điều chỉnh Vibe")
        Spacer(Modifier.height(8.dp))

        EmotionSlider("Năng lượng Scene", state.sceneEnergy,
            listOf("Tĩnh lặng", "Nhẹ nhàng", "Căng ngầm", "Bùng nổ")
        ) { viewModel.updateVibe(energy = it) }

        EmotionSlider("Độ bất ngờ", state.unpredictabilityLevel,
            listOf("Đúng ý", "Cân bằng", "Khó đoán")
        ) { viewModel.updateVibe(unpredict = it) }

        EmotionSlider("Góc nhìn", state.cinematicLevel,
            listOf("Văn kể", "Cân bằng", "Điện ảnh")
        ) { viewModel.updateVibe(cinematic = it) }

        EmotionSlider("Nội tâm", state.introspectionLevel,
            listOf("Ít", "Vừa", "Sâu sắc")
        ) { viewModel.updateVibe(introspect = it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val models = listOf(
        "" to "Mặc định (BuildConfig)",
        "gemini-2.5-flash" to "Gemini 2.5 Flash",
        "gemini-2.5-pro" to "Gemini 2.5 Pro",
        "gemini-2.0-flash" to "Gemini 2.0 Flash",
        "gemini-2.0-flash-lite" to "Gemini 2.0 Flash Lite"
    )

    var customModelInput by remember(state.modelName) { mutableStateOf(state.modelName) }

    SettingsCard {
        SectionTitle("🤖 Model AI")
        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            models.forEach { (id, label) ->
                val isSelected = state.modelName == id
                Surface(
                    color = if (isSelected) PurplePrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, PurplePrimary) else BorderStroke(1.dp, DarkDivider),
                    modifier = Modifier.clickable { 
                        viewModel.setModelName(id)
                        customModelInput = id 
                    }
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = if (isSelected) PurplePrimary else TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = customModelInput,
            onValueChange = { customModelInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Hoặc tự nhập tên model (VD: gemini-1.5-pro)...", color = TextHint) },
            trailingIcon = {
                if (customModelInput != state.modelName) {
                    IconButton(onClick = { viewModel.setModelName(customModelInput.trim()) }) {
                        Icon(Icons.Default.Check, "Lưu Model", tint = GreenSoft)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary, unfocusedBorderColor = DarkDivider,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (state.modelName.isBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Đang dùng model từ cấu hình build", color = TextHint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AdvancedVibeSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsCard {
        SectionTitle("📖 Cốt truyện")
        Spacer(Modifier.height(8.dp))

        EmotionSlider("Liên kết (Continuity)", state.continuityLevel,
            listOf("Lỏng lẻo", "Cân bằng", "Chặt chẽ")
        ) { viewModel.updateVibe(continuity = it) }

        EmotionSlider("Khoảng lặng", state.melancholyLevel,
            listOf("Ít", "Vừa phải", "Rất nhiều")
        ) { viewModel.updateVibe(melancholy = it) }
    }
}

@Composable
fun EmotionSlider(label: String, value: Int, labels: List<String>, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Surface(color = PurplePrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    labels[value],
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = PurplePrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Step indicator row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, _ ->
                val isActive = index <= value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isActive) PurplePrimary else DarkDivider)
                        .clickable { onValueChange(index) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, text ->
                Text(
                    text,
                    color = if (index == value) PurplePrimary else TextHint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == value) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun KeyManagerSection(viewModel: SettingsViewModel) {
    val keyStates by viewModel.keyStates.collectAsStateWithLifecycle()
    var newKeyInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    SettingsCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("🔑 API Keys")
            Surface(
                color = GreenSoft.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { viewModel.validateAllKeys() }
            ) {
                Text("Ping tất cả", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = GreenSoft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = newKeyInput,
            onValueChange = { newKeyInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Dán Gemini API Key...", color = TextHint) },
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
    val statusEmoji = when (state.status) {
        KeyStatus.ACTIVE -> "🟢"
        KeyStatus.COOLDOWN -> "🟡"
        KeyStatus.DEAD -> "🔴"
        KeyStatus.UNTESTED -> "⚪"
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(DarkBackground, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$statusEmoji ${state.label}", color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(
                "${state.key.take(8)}...${state.key.takeLast(4)}",
                color = TextHint, style = MaterialTheme.typography.labelSmall
            )
        }
        Row {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, "Copy", tint = TextHint, modifier = Modifier.size(16.dp))
            }
            if (state.isCustom) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Xoá", tint = RedSoft, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ReadingSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsCard {
        SectionTitle("📖 Hiển thị")
        Spacer(Modifier.height(8.dp))
        SettingsSlider("Cỡ chữ", state.fontSize, 14f..24f) { viewModel.setFontSize(it) }
        SettingsSlider("Khoảng cách dòng", state.lineHeight, 24f..40f) { viewModel.setLineHeight(it) }
        
        Spacer(Modifier.height(16.dp))
        Text("Chủ đề đọc truyện", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderTheme.entries.forEach { theme ->
                val isSelected = state.readerTheme == theme.name
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) PurplePrimary.copy(alpha=0.2f) else DarkBackground,
                    border = BorderStroke(1.dp, if (isSelected) PurplePrimary else DarkDivider),
                    modifier = Modifier.clickable { viewModel.setReaderTheme(theme.name) }
                ) {
                    Text(
                        text = theme.title,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) PurplePrimary else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text("%.0f".format(value), color = PurplePrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary, inactiveTrackColor = DarkDivider)
        )
    }
}

// ─── Shared UI Helpers ─────────────────────────────────────
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
}
