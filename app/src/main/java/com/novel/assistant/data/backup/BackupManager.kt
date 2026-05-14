package com.novel.assistant.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.novel.assistant.data.local.database.AppDatabase
import com.novel.assistant.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

enum class ConflictResolution {
    OVERWRITE, KEEP_BOTH, SKIP
}

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
    val styleReferences: List<StyleReferenceEntity>
)

data class FullBackup(
    val appVersion: String = "1.0.0",
    val exportDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val novels: List<NovelBackup>
)

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase,
    private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportAllToJson(): String = withContext(Dispatchers.IO) {
        val novels = database.novelDao().getAllNovelsOnce()
        val backups = novels.map { novel -> exportNovel(novel) }
        val fullBackup = FullBackup(novels = backups)
        gson.toJson(fullBackup)
    }

    suspend fun exportNovelToJson(novelId: Long): String = withContext(Dispatchers.IO) {
        val novel = database.novelDao().getNovelById(novelId) ?: throw Exception("Không tìm thấy truyện")
        val backup = exportNovel(novel)
        val fullBackup = FullBackup(novels = listOf(backup))
        gson.toJson(fullBackup)
    }

    private suspend fun exportNovel(novel: NovelEntity): NovelBackup {
        val novelId = novel.id
        return NovelBackup(
            novel = novel,
            chapters = database.chapterDao().getChaptersByNovelOnce(novelId),
            scenes = database.sceneDao().getScenesByNovelOnce(novelId),
            sceneVersions = database.sceneDao().getScenesByNovelOnce(novelId).flatMap { scene ->
                database.sceneVersionDao().getVersionsBySceneOnce(scene.id)
            },
            characters = database.characterDao().getCharactersByNovelOnce(novelId),
            corrections = database.characterCorrectionDao().getCorrectionsByNovel(novelId),
            relationships = database.relationshipDao().getRelationshipsByNovelOnce(novelId),
            memories = database.memoryDao().getActiveMemoriesOnce(novelId),
            timelineEvents = database.timelineEventDao().getEventsByNovelOnce(novelId),
            styleReferences = database.styleReferenceDao().getStylesByNovelOnce(novelId)
        )
    }

    suspend fun parseBackup(jsonString: String): FullBackup? = withContext(Dispatchers.IO) {
        try { gson.fromJson(jsonString, FullBackup::class.java) } catch (e: Exception) { null }
    }

    suspend fun checkConflicts(fullBackup: FullBackup): Boolean = withContext(Dispatchers.IO) {
        val existingNovels = database.novelDao().getAllNovelsOnce()
        fullBackup.novels.any { backup ->
            existingNovels.any { it.id == backup.novel.id || it.title == backup.novel.title }
        }
    }

    suspend fun importBackup(fullBackup: FullBackup, resolution: ConflictResolution = ConflictResolution.KEEP_BOTH) = withContext(Dispatchers.IO) {
        val existingNovels = database.novelDao().getAllNovelsOnce()

        fullBackup.novels.forEach { backup ->
            val existing = existingNovels.find { it.id == backup.novel.id || it.title == backup.novel.title }
            
            if (existing != null) {
                when (resolution) {
                    ConflictResolution.SKIP -> return@forEach
                    ConflictResolution.OVERWRITE -> {
                        database.novelDao().deleteNovel(existing)
                    }
                    ConflictResolution.KEEP_BOTH -> {
                        // proceed to insert as new
                    }
                }
            }

            // Insert novel with new ID (or original ID if we deleted the existing one, but using 0 is safer to auto-generate and avoid constraint issues if ID was not the conflict reason)
            val newNovelId = database.novelDao().insertNovel(backup.novel.copy(id = 0, title = if (resolution == ConflictResolution.KEEP_BOTH && existing != null) "${backup.novel.title} (Bản sao)" else backup.novel.title))

            // Re-map chapter IDs
            val chapterIdMap = mutableMapOf<Long, Long>()
            backup.chapters.forEach { chapter ->
                val newId = database.chapterDao().insertChapter(
                    chapter.copy(id = 0, novelId = newNovelId)
                )
                chapterIdMap[chapter.id] = newId
            }

            // Re-map scene IDs
            val sceneIdMap = mutableMapOf<Long, Long>()
            backup.scenes.forEach { scene ->
                val newChapterId = chapterIdMap[scene.chapterId] ?: return@forEach
                val newId = database.sceneDao().insertScene(
                    scene.copy(id = 0, novelId = newNovelId, chapterId = newChapterId)
                )
                sceneIdMap[scene.id] = newId
            }

            // Scene versions
            backup.sceneVersions.forEach { version ->
                val newSceneId = sceneIdMap[version.sceneId] ?: return@forEach
                database.sceneVersionDao().insertVersion(
                    version.copy(id = 0, sceneId = newSceneId)
                )
            }

            // Re-map character IDs
            val charIdMap = mutableMapOf<Long, Long>()
            backup.characters.forEach { char ->
                val newId = database.characterDao().insertCharacter(
                    char.copy(id = 0, novelId = newNovelId)
                )
                charIdMap[char.id] = newId
            }

            // Corrections
            backup.corrections.forEach { correction ->
                val newCharId = charIdMap[correction.characterId] ?: return@forEach
                database.characterCorrectionDao().insertCorrection(
                    correction.copy(id = 0, characterId = newCharId, novelId = newNovelId)
                )
            }

            // Relationships
            backup.relationships.forEach { rel ->
                database.relationshipDao().insertRelationship(
                    rel.copy(id = 0, novelId = newNovelId)
                )
            }

            // Memories
            backup.memories.forEach { memory ->
                database.memoryDao().insertMemory(
                    memory.copy(id = 0, novelId = newNovelId)
                )
            }

            // Timeline events
            backup.timelineEvents.forEach { event ->
                val newSceneId = sceneIdMap[event.sceneId ?: 0]
                database.timelineEventDao().insertEvent(
                    event.copy(id = 0, novelId = newNovelId, sceneId = newSceneId)
                )
            }

            // Style references
            backup.styleReferences.forEach { style ->
                database.styleReferenceDao().insertStyle(
                    style.copy(id = 0, novelId = newNovelId)
                )
            }
        }
    }

    fun getBackupDir(): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val dir = File(documentsDir, "NovelAI/backup")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun autoBackup(novelId: Long) = withContext(Dispatchers.IO) {
        try {
            val json = exportNovelToJson(novelId)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(getBackupDir(), "novel_${novelId}_$dateStr.json")
            file.writeText(json, Charsets.UTF_8)

            // Keep only last 5 backups per novel
            val backups = getBackupDir().listFiles { f ->
                f.name.startsWith("novel_${novelId}_") && f.extension == "json"
            }?.sortedByDescending { it.lastModified() }

            backups?.drop(5)?.forEach { it.delete() }
        } catch (e: Exception) {
            // Silently fail auto-backup
            android.util.Log.e("BackupManager", "Auto-backup failed: ${e.message}")
        }
    }
}
