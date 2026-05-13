# Ứng Dụng Viết Truyện Hỗ Trợ AI — Implementation Plan (v2)

## Tổng quan

Xây dựng ứng dụng Android hỗ trợ viết truyện bằng AI, dành cho người **"nghĩ bằng hình ảnh nhưng khó diễn tả thành chữ"**. Giao diện tối cinematic, hoàn toàn tiếng Việt. Người dùng chỉ cần ném ý tưởng/hình ảnh trong đầu → AI biến thành scene truyện hoàn chỉnh, nhất quán về cảm xúc, nhân vật và vibe.

### Điểm khác biệt chính:
- **Prompt Builder trực quan** — người dùng chọn mood/tốc độ/góc nhìn thay vì viết prompt
- **Ngữ cảnh thông minh** — app tự chọn context phù hợp, không gửi full history
- **Hệ thống ký ức & timeline** — AI nhớ dài hạn, giữ nhất quán
- **Chế độ nhập vai** — user điều khiển nhân vật chính, AI viết phản ứng
- **Key rotation** — tự động xoay 15 API key khi bị rate limit
- **Backup local** — tự động backup JSON, export/import dễ dàng

---

## User Review Required

> [!IMPORTANT]
> **Phân chia Phase**: Vì dự án rất lớn, tôi đề xuất chia **2 phase**:
> - **Phase 1 (Build lần này)**: Toàn bộ core app — KHÔNG cắt bớt tính năng. Tất cả 10 tính năng bạn yêu cầu sẽ được build trong phase này.
> - **Phase 2 (Sau khi dùng thử)**: Polish UI, fix bugs, thêm cloud sync nếu cần, multi-AI provider (Claude/OpenAI).
>
> **Hiện tại Phase 1 sẽ dùng Gemini làm AI duy nhất** (vì bạn có sẵn 15 key). Kiến trúc sẽ được thiết kế để dễ thêm provider khác sau.

> [!WARNING]
> **Layout trên điện thoại**: 3 cột sẽ rất chật. Giải pháp:
> - Drawer trái: danh sách chương/phân cảnh + timeline
> - Main: khung viết + prompt builder + đọc novel
> - Bottom sheet: trạng thái nhân vật, quan hệ, mood
> - Tablet: có thể hiện 2-3 cột nếu đủ rộng

> [!IMPORTANT]  
> **Backup & Dữ liệu**: Tôi sẽ implement:
> - Auto-backup JSON vào thư mục Documents trên thiết bị (SAF - Storage Access Framework)
> - Nút Export/Import thủ công (backup toàn bộ truyện ra file `.json`)
> - Dữ liệu ảnh bìa/avatar backup cùng
> - Khi gỡ app hoặc đổi máy: chỉ cần import file backup
>
> Đồng ý cách này không?

---

## Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room (SQLite) |
| AI | Google Gemini API (streaming, key rotation) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Compose Navigation |
| Backup | JSON export/import + SAF |
| Font | Noto Serif (novel) + Be Vietnam Pro (UI) |
| Min SDK | API 26 (Android 8.0) |

---

## Proposed Changes

### 1. Project Setup & Build Configuration

#### [NEW] Project skeleton
- Android project Kotlin + Compose
- `libs.versions.toml` version catalog
- Hilt, Room, Gemini SDK, Compose Navigation, DataStore
- ProGuard rules cho Gemini SDK
- `.env` reader để load API keys vào BuildConfig

**Package structure:**
```
com.novel.assistant/
├── NovelApp.kt                    # Application + Hilt
├── MainActivity.kt                # Single Activity
├── data/
│   ├── local/
│   │   ├── database/AppDatabase.kt
│   │   ├── entity/                # Room entities (8 tables)
│   │   ├── dao/                   # Room DAOs
│   │   └── datastore/AppPreferences.kt  # Settings
│   ├── remote/
│   │   └── ai/
│   │       ├── AiProvider.kt      # Interface cho multi-provider
│   │       ├── GeminiProvider.kt   # Gemini implementation
│   │       ├── KeyRotationManager.kt  # Key rotation + retry
│   │       └── ContextBuilder.kt  # Ngữ cảnh thông minh
│   ├── repository/                # Implementations
│   └── backup/
│       ├── BackupManager.kt       # Export/Import JSON
│       └── AutoBackupWorker.kt    # Periodic auto-backup
├── domain/
│   ├── model/                     # Domain models
│   ├── repository/                # Interfaces
│   └── usecase/                   # Business logic
├── di/                            # Hilt modules
└── ui/
    ├── theme/                     # Dark cinematic theme
    ├── navigation/NavGraph.kt
    ├── home/                      # Trang chủ
    ├── novel/
    │   ├── creation/              # Tạo truyện mới
    │   ├── editor/                # Khung viết chính
    │   ├── chapters/              # Danh sách chương/phân cảnh
    │   ├── reader/                # Chế độ đọc novel
    │   ├── timeline/              # Timeline truyện
    │   └── status/                # Trạng thái nhân vật/quan hệ
    ├── character/                 # Quản lý nhân vật
    ├── promptbuilder/             # Prompt Builder UI
    ├── settings/                  # Cài đặt + Backup
    └── components/                # Shared composables
```

---

### 2. Theme & Design System

#### [NEW] ui/theme/ — Dark Cinematic

**Color Palette:**
```
Background:       #0A0A10  (đen xanh đêm sâu)
Surface:          #13131D  (xám tối)
Surface Variant:  #1C1C2B  (xám nhạt hơn)  
Card:             #17172280 (card với opacity)
Primary:          #8B7EC8  (tím pastel - accent chính)
Secondary:        #5B8FB9  (xanh đêm nhẹ)
Tertiary:         #C9A0DC  (tím hồng nhẹ)
Accent Warm:      #D4A574  (vàng ấm cho "khoảnh khắc yêu thích")
Error:            #CF6679
Text Primary:     #E8E4F0  (trắng ấm)
Text Secondary:   #9590A8  (xám nhạt)
Text Hint:        #5C586A
Divider:          #2A2A3A
```

**Typography:**
- Novel content: `Noto Serif` — cảm giác đọc webnovel
- UI headings: `Be Vietnam Pro Semi-Bold`
- UI body: `Be Vietnam Pro Regular`
- Kích thước đọc truyện: 16-18sp, line height 1.8

**Animations:**
- Fade in/out: 300-500ms ease
- Slide transitions: 400ms
- Shimmer loading khi AI đang viết
- Typing effect khi stream AI response
- Glow effect nhẹ cho "khoảnh khắc yêu thích"

---

### 3. Database Schema (Room)

#### [NEW] data/local/entity/ — 10 tables

```kotlin
// === CORE ===
NovelEntity(
    id, title, description, sourceNovelName, sourceNovelDescription,
    coverImagePath, currentMood, styleVibeTags, // JSON list: ["buồn nhẹ", "slow burn"]
    createdAt, updatedAt
)

ChapterEntity(
    id, novelId, title, orderIndex, createdAt
)

SceneEntity(
    id, chapterId, novelId, title, content, 
    userPrompt,        // Ý tưởng gốc user nhập
    promptSettings,    // JSON: mood, speed, dialogLevel, viewpoint, focus
    mood, vibeTags,    // Tags riêng cho scene
    orderIndex, isFavorite,  // "Khoảnh khắc yêu thích"
    favoriteNotes,     // Ghi chú vì sao thích (style reference)
    createdAt, updatedAt
)

SceneVersionEntity(
    id, sceneId, content, versionNumber, createdAt
)

// === NHÂN VẬT ===
CharacterEntity(
    id, novelId, name, description, personality, speechStyle,
    fears, importantThings, avatarPath,
    currentEmotionalState,  // Trạng thái cảm xúc hiện tại
    createdAt, updatedAt
)

CharacterCorrectionEntity(
    id, characterId, novelId,
    correctionType,  // "speech", "behavior", "reaction"
    wrongExample,    // "Da-in không nói kiểu này"
    rightDescription, // Mô tả đúng
    createdAt
)

RelationshipEntity(
    id, novelId, char1Id, char2Id, 
    description, status, intimacyLevel,
    createdAt, updatedAt
)

// === KÝ ỨC ===
MemoryEntity(
    id, novelId, content, summary,
    type,  // PERMANENT, TEMPORARY, ARC
    category, // emotion_change, relationship, promise, trauma, development
    relatedCharacterIds, // JSON list
    relatedSceneId,
    isActive,  // Ký ức tạm có thể hết hiệu lực
    createdAt
)

// === TIMELINE ===
TimelineEventEntity(
    id, novelId, sceneId, chapterId,
    eventDescription, 
    eventType,  // plot_point, secret_revealed, relationship_change, arc_start, arc_end
    involvedCharacterIds, // JSON list
    orderIndex,
    createdAt
)

// === STYLE REFERENCE ===
StyleReferenceEntity(
    id, novelId, sceneId,
    rhythmNotes,     // Nhịp văn
    dialogueStyle,   // Kiểu thoại
    emotionStyle,    // Cảm xúc
    descriptionStyle, // Cách mô tả
    sampleText,       // Đoạn mẫu
    createdAt
)
```

#### [NEW] data/local/dao/ — DAOs cho từng entity
- `NovelDao` — CRUD + query với stats
- `ChapterDao` — CRUD + reorder
- `SceneDao` — CRUD + query by chapter + favorites
- `SceneVersionDao` — Save/load versions
- `CharacterDao` — CRUD + corrections
- `CharacterCorrectionDao` — CRUD corrections
- `RelationshipDao` — CRUD + query by novel
- `MemoryDao` — CRUD + query by type/character
- `TimelineEventDao` — CRUD + query ordered
- `StyleReferenceDao` — CRUD + query by novel

---

### 4. AI System — Ngữ cảnh thông minh + Key Rotation

#### [NEW] data/remote/ai/AiProvider.kt
```kotlin
interface AiProvider {
    suspend fun generateScene(request: SceneRequest): Flow<String>  // Streaming
    suspend fun refineScene(scene: String, instruction: String): Flow<String>
    suspend fun analyzeForMemories(scene: String): List<MemorySuggestion>
    suspend fun updateCharacterState(scene: String, characters: List<Character>): List<CharacterStateUpdate>
}
```

#### [NEW] data/remote/ai/KeyRotationManager.kt
- Quản lý 3 nhóm key: `MAIN` (11 key), `MEMORY` (2 key), `GENERATOR` (2 key)
- Round-robin rotation
- Tự động switch key khi gặp: 429 (rate limit), 503 (service unavailable), 500
- Retry với exponential backoff
- Cooldown per key (60s sau khi bị rate limit)
- Track usage count per key

#### [NEW] data/remote/ai/ContextBuilder.kt — **Ngữ cảnh thông minh**
```
Khi user gửi prompt, app sẽ TỰ ĐỘNG build context:

1. System Prompt (cố định):
   - Hướng dẫn viết novel tiếng Việt
   - Phong cách/vibe từ tags của truyện
   - Style references từ "khoảnh khắc yêu thích"

2. Character Context (chọn tự động):
   - Chỉ nhân vật liên quan tới scene
   - Tính cách + cách nói + corrections
   - Trạng thái cảm xúc HIỆN TẠI
   - Quan hệ với nhân vật khác trong scene

3. Story Context (chọn tự động):
   - 2-5 scene gần nhất (tóm tắt ngắn, không full text)
   - Ký ức PERMANENT liên quan
   - Ký ức ARC đang active
   - Timeline events gần nhất

4. Scene Settings (từ Prompt Builder):
   - Tâm trạng, tốc độ, mức thoại, góc nhìn, trọng tâm
   - Vibe tags cụ thể

5. User Prompt:
   - Ý tưởng ngắn của user
```

---

### 5. Prompt Builder — UI chọn lựa tiếng Việt

#### [NEW] ui/promptbuilder/PromptBuilderSheet.kt

Bottom sheet hiện lên khi user bắt đầu viết scene mới:

```
┌─────────────────────────────────┐
│  🎭 Tạo phân cảnh mới           │
│                                  │
│  Tâm trạng:                     │
│  [buồn nhẹ] [cô đơn] [ấm áp]   │
│  [đau lòng] [căng thẳng]        │
│  [lãng mạn] [melancholy]        │
│                                  │
│  Tốc độ:                        │
│  ○ Chậm   ● Vừa   ○ Nhanh      │
│                                  │
│  Mức thoại:                     │
│  ○ Ít   ● Bình thường   ○ Nhiều │
│                                  │
│  Góc nhìn:                      │
│  [Seo-eun ▼] [toàn cảnh]       │
│                                  │
│  Trọng tâm:                     │
│  [nội tâm] [chemistry]          │
│  [hành động] [cảm xúc]          │
│  [plot twist]                    │
│                                  │
│  Nhân vật trong cảnh:           │
│  ☑ Seo-eun  ☑ Da-in             │
│  ☐ Min-ho   ☐ Ji-yeon           │
│                                  │
│  ─────────────────────────────  │
│  [Ý tưởng của bạn...]           │
│  "Sân thượng. Ban đêm.          │
│   Seo-eun nhìn Da-in ngủ."      │
│                                  │
│          [✨ Viết scene]         │
└─────────────────────────────────┘
```

---

### 6. Chế độ Nhập vai

#### [NEW] ui/novel/editor/RoleplayMode.kt

Khi bật chế độ nhập vai:
- Input field chỉ nhận hành động/suy nghĩ/cảm xúc của nhân vật chính
- Hiển thị tag `[Nhập vai: Seo-eun]` 
- AI tự viết phản ứng nhân vật khác + hội thoại + môi trường
- Nút chuyển giữa "Viết scene" và "Nhập vai"

---

### 7. Timeline truyện

#### [NEW] ui/novel/timeline/TimelineScreen.kt

Hiển thị trong drawer hoặc tab riêng:
```
Timeline:
────────────────────
Ch.1 │ ● Seo-eun gặp Da-in lần đầu
     │ ● Bí mật: Seo-eun biết về quá khứ Da-in
Ch.2 │ ● Quan hệ: thân thiết hơn
     │ ● Arc bắt đầu: "Chữa lành"
Ch.3 │ ● Lời hứa: "Sẽ không bỏ đi"
     │ ● ★ Khoảnh khắc yêu thích
────────────────────
```

- Timeline tự động cập nhật khi lưu scene
- AI phân tích scene để detect events
- User có thể thêm/sửa events thủ công

---

### 8. Chế độ Đọc Novel

#### [NEW] ui/novel/reader/NovelReaderScreen.kt

- Full screen, ẩn hết UI kỹ thuật
- Chỉ hiện nội dung truyện sạch, nối liền theo chương
- Font Noto Serif, khoảng cách đẹp
- Swipe để chuyển chương
- Nút bookmark
- Tuỳ chỉnh: cỡ chữ, khoảng cách dòng, màu nền
- Cảm giác đọc webnovel thật

---

### 9. Backup System

#### [NEW] data/backup/BackupManager.kt

```kotlin
// Export: Novel → JSON file (bao gồm tất cả data)
data class NovelBackup(
    val novel: NovelEntity,
    val chapters: List<ChapterEntity>,
    val scenes: List<SceneEntity>,
    val sceneVersions: List<SceneVersionEntity>,
    val characters: List<CharacterEntity>,
    val corrections: List<CharacterCorrectionEntity>,
    val relationships: List<RelationshipEntity>,
    val memories: List<MemoryEntity>,
    val timelineEvents: List<TimelineEventEntity>,
    val styleReferences: List<StyleReferenceEntity>,
    val exportDate: String,
    val appVersion: String
)
```

- **Export**: Xuất 1 truyện hoặc tất cả → file `.novelai.json`
- **Import**: Đọc file → merge hoặc overwrite vào database
- **Auto-backup**: Mỗi khi lưu scene → backup tự động vào `Documents/NovelAI/backup/`
- **Ảnh**: Copy ảnh bìa/avatar vào thư mục backup kèm theo

---

### 10. Settings

#### [NEW] ui/settings/SettingsScreen.kt

- **API Key**: Quản lý danh sách key (thêm/xoá/sửa), chia nhóm
- **Model**: Chọn model Gemini (gemini-3-flash, gemini-2.5-pro, etc.)
- **Đọc truyện**: Cỡ chữ, font, khoảng cách dòng, màu nền reader
- **Sao lưu**: Export/Import, bật/tắt auto-backup
- **Thông tin**: Version app, dung lượng database

---

### 11. UI Screens Summary

| Màn hình | Mô tả |
|---|---|
| **Trang chủ** | Danh sách truyện, card với bìa + mood + scene gần nhất |
| **Tạo truyện** | Form tạo mới: tên, mô tả, truyện gốc, nhân vật, vibe tags |
| **Viết truyện** | Drawer chương + khung viết chính + bottom sheet trạng thái |
| **Prompt Builder** | Bottom sheet chọn mood/tốc độ/thoại/góc nhìn/trọng tâm |
| **Nhập vai** | Mode đặc biệt: user nhập hành động → AI viết phản ứng |
| **Đọc novel** | Full screen reader, sạch, như webnovel thật |
| **Nhân vật** | Danh sách + form chi tiết + corrections |
| **Timeline** | Dòng thời gian truyện, events, arcs |
| **Cài đặt** | API keys, model, font, backup |

---

### 12. Nút chỉnh nhanh AI & Lịch sử phiên bản

#### Dưới mỗi scene AI viết:
```
[❤ Yêu thích] [📝 Sửa] [💾 Lưu phân cảnh]

Chỉnh nhanh:
[Buồn hơn] [Ấm áp hơn] [Đúng tính cách hơn]
[Thêm nội tâm] [Thêm khoảng lặng] 
[Ít thoại hơn] [Viết dài hơn] [Viết chậm hơn]

Sửa nhân vật:
[⚠ Da-in không nói kiểu này]
[⚠ Seo-eun sẽ không phản ứng vậy]
```

- Mỗi lần chỉnh → lưu version cũ tự động
- Nút "Lịch sử phiên bản" → xem & quay lại bản cũ
- Corrections được lưu vào `CharacterCorrectionEntity`

---

## File List (ước tính ~45 files)

```
e:\bot\novel\
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/novel/assistant/
│           ├── NovelApp.kt
│           ├── MainActivity.kt
│           ├── di/
│           │   ├── AppModule.kt
│           │   ├── DatabaseModule.kt
│           │   └── AiModule.kt
│           ├── data/
│           │   ├── local/
│           │   │   ├── database/AppDatabase.kt
│           │   │   ├── entity/   (10 files)
│           │   │   ├── dao/      (10 files)
│           │   │   └── datastore/AppPreferences.kt
│           │   ├── remote/ai/
│           │   │   ├── AiProvider.kt
│           │   │   ├── GeminiProvider.kt
│           │   │   ├── KeyRotationManager.kt
│           │   │   └── ContextBuilder.kt
│           │   ├── repository/   (5 files)
│           │   └── backup/
│           │       └── BackupManager.kt
│           ├── domain/
│           │   ├── model/        (5 files)
│           │   ├── repository/   (5 files)
│           │   └── usecase/      (8 files)
│           └── ui/
│               ├── theme/ (Color.kt, Type.kt, Theme.kt)
│               ├── navigation/NavGraph.kt
│               ├── home/ (HomeScreen.kt, HomeViewModel.kt)
│               ├── novel/
│               │   ├── creation/ (NovelCreationScreen.kt, CreationViewModel.kt)
│               │   ├── editor/ (WritingScreen.kt, WritingViewModel.kt, RoleplayMode.kt)
│               │   ├── chapters/ (ChapterDrawer.kt)
│               │   ├── reader/ (NovelReaderScreen.kt, ReaderViewModel.kt)
│               │   ├── timeline/ (TimelineScreen.kt)
│               │   └── status/ (StatusSheet.kt)
│               ├── character/ (CharacterScreen.kt, CharacterViewModel.kt)
│               ├── promptbuilder/ (PromptBuilderSheet.kt)
│               ├── settings/ (SettingsScreen.kt, SettingsViewModel.kt)
│               └── components/ (NovelCard.kt, SceneContent.kt, 
│                                LoadingShimmer.kt, VibeTags.kt,
│                                QuickActionBar.kt)
├── build.gradle.kts (project)
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
└── .env (existing - API keys)
```

---

## Verification Plan

### Build Verification
```bash
./gradlew assembleDebug
```

### Functional Testing (Manual on device/emulator)
1. ✅ Trang chủ: tạo truyện mới, hiển thị danh sách
2. ✅ Nhân vật: thêm/sửa nhân vật, corrections
3. ✅ Prompt Builder: chọn options → build prompt
4. ✅ Viết scene: AI streaming response, hiển thị như novel
5. ✅ Lưu phân cảnh: lưu vào chương, đọc lại
6. ✅ Nút chỉnh nhanh: chỉnh → lưu version → quay lại
7. ✅ Khoảnh khắc yêu thích: đánh dấu, lưu style reference
8. ✅ Nhập vai: nhập hành động → AI viết phản ứng
9. ✅ Timeline: hiển thị events
10. ✅ Đọc novel: full screen reader sạch
11. ✅ Backup: export → gỡ app → cài lại → import → data còn nguyên
12. ✅ Key rotation: test rate limit → tự switch key
13. ✅ UI tiếng Việt: không có text tiếng Anh nào trên giao diện