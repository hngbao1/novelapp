package com.novel.assistant.data.local.dao

import androidx.room.*
import com.novel.assistant.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels ORDER BY updatedAt DESC")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels ORDER BY updatedAt DESC")
    suspend fun getAllNovelsOnce(): List<NovelEntity>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: Long): NovelEntity?

    @Query("SELECT * FROM novels WHERE id = :id")
    fun observeNovel(id: Long): Flow<NovelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity): Long

    @Update
    suspend fun updateNovel(novel: NovelEntity)

    @Delete
    suspend fun deleteNovel(novel: NovelEntity)

    @Query("UPDATE novels SET updatedAt = :time WHERE id = :novelId")
    suspend fun touchNovel(novelId: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET currentMood = :mood, updatedAt = :time WHERE id = :novelId")
    suspend fun updateMood(novelId: Long, mood: String, time: Long = System.currentTimeMillis())
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY orderIndex ASC")
    fun getChaptersByNovel(novelId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY orderIndex ASC")
    suspend fun getChaptersByNovelOnce(novelId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM chapters WHERE novelId = :novelId")
    suspend fun getNextOrderIndex(novelId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getScenesByChapter(chapterId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE novelId = :novelId ORDER BY orderIndex ASC")
    fun getScenesByNovel(novelId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE novelId = :novelId ORDER BY orderIndex ASC")
    suspend fun getScenesByNovelOnce(novelId: Long): List<SceneEntity>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: Long): SceneEntity?

    @Query("SELECT * FROM scenes WHERE novelId = :novelId AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteScenes(novelId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE novelId = :novelId AND isBookmarked = 1 ORDER BY orderIndex ASC")
    fun getBookmarkedScenes(novelId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE novelId = :novelId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentScenes(novelId: Long, limit: Int = 5): List<SceneEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM scenes WHERE chapterId = :chapterId")
    suspend fun getNextOrderIndex(chapterId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity): Long

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Delete
    suspend fun deleteScene(scene: SceneEntity)

    @Query("UPDATE scenes SET isFavorite = :isFavorite, favoriteNotes = :notes WHERE id = :sceneId")
    suspend fun setFavorite(sceneId: Long, isFavorite: Boolean, notes: String = "")

    @Query("UPDATE scenes SET isBookmarked = :isBookmarked WHERE id = :sceneId")
    suspend fun setBookmark(sceneId: Long, isBookmarked: Boolean)

    @Query("UPDATE scenes SET content = :content, updatedAt = :time WHERE id = :sceneId")
    suspend fun updateContent(sceneId: Long, content: String, time: Long = System.currentTimeMillis())
}

@Dao
interface SceneVersionDao {
    @Query("SELECT * FROM scene_versions WHERE sceneId = :sceneId ORDER BY versionNumber DESC")
    fun getVersionsByScene(sceneId: Long): Flow<List<SceneVersionEntity>>

    @Query("SELECT * FROM scene_versions WHERE sceneId = :sceneId ORDER BY versionNumber DESC")
    suspend fun getVersionsBySceneOnce(sceneId: Long): List<SceneVersionEntity>

    @Query("SELECT COALESCE(MAX(versionNumber), 0) + 1 FROM scene_versions WHERE sceneId = :sceneId")
    suspend fun getNextVersionNumber(sceneId: Long): Int

    @Insert
    suspend fun insertVersion(version: SceneVersionEntity): Long

    @Delete
    suspend fun deleteVersion(version: SceneVersionEntity)
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY isMainCharacter DESC, name ASC")
    fun getCharactersByNovel(novelId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY isMainCharacter DESC, name ASC")
    suspend fun getCharactersByNovelOnce(novelId: Long): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): CharacterEntity?

    @Query("SELECT * FROM characters WHERE id IN (:ids)")
    suspend fun getCharactersByIds(ids: List<Long>): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("UPDATE characters SET currentEmotionalState = :state, updatedAt = :time WHERE id = :charId")
    suspend fun updateEmotionalState(charId: Long, state: String, time: Long = System.currentTimeMillis())
}

@Dao
interface CharacterCorrectionDao {
    @Query("SELECT * FROM character_corrections WHERE characterId = :charId ORDER BY createdAt DESC")
    fun getCorrectionsByCharacter(charId: Long): Flow<List<CharacterCorrectionEntity>>

    @Query("SELECT * FROM character_corrections WHERE novelId = :novelId ORDER BY createdAt DESC")
    suspend fun getCorrectionsByNovel(novelId: Long): List<CharacterCorrectionEntity>

    @Insert
    suspend fun insertCorrection(correction: CharacterCorrectionEntity): Long

    @Delete
    suspend fun deleteCorrection(correction: CharacterCorrectionEntity)
}

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationships WHERE novelId = :novelId")
    fun getRelationshipsByNovel(novelId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE novelId = :novelId")
    suspend fun getRelationshipsByNovelOnce(novelId: Long): List<RelationshipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipEntity): Long

    @Update
    suspend fun updateRelationship(relationship: RelationshipEntity)

    @Delete
    suspend fun deleteRelationship(relationship: RelationshipEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE novelId = :novelId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveMemories(novelId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE novelId = :novelId AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveMemoriesOnce(novelId: Long): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE novelId = :novelId AND type = :type AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getMemoriesByType(novelId: Long, type: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE novelId = :novelId AND type = 'PERMANENT' AND isActive = 1")
    suspend fun getPermanentMemories(novelId: Long): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE novelId = :novelId AND type = 'ARC' AND isActive = 1")
    suspend fun getArcMemories(novelId: Long): List<MemoryEntity>

    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET isActive = 0 WHERE id = :memoryId")
    suspend fun deactivateMemory(memoryId: Long)
}

@Dao
interface TimelineEventDao {
    @Query("SELECT * FROM timeline_events WHERE novelId = :novelId ORDER BY orderIndex ASC")
    fun getEventsByNovel(novelId: Long): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE novelId = :novelId ORDER BY orderIndex ASC")
    suspend fun getEventsByNovelOnce(novelId: Long): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE novelId = :novelId ORDER BY orderIndex DESC LIMIT :limit")
    suspend fun getRecentEvents(novelId: Long, limit: Int = 10): List<TimelineEventEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM timeline_events WHERE novelId = :novelId")
    suspend fun getNextOrderIndex(novelId: Long): Int

    @Insert
    suspend fun insertEvent(event: TimelineEventEntity): Long

    @Update
    suspend fun updateEvent(event: TimelineEventEntity)

    @Delete
    suspend fun deleteEvent(event: TimelineEventEntity)
}

@Dao
interface StyleReferenceDao {
    @Query("SELECT * FROM style_references WHERE novelId = :novelId ORDER BY createdAt DESC")
    fun getStylesByNovel(novelId: Long): Flow<List<StyleReferenceEntity>>

    @Query("SELECT * FROM style_references WHERE novelId = :novelId ORDER BY createdAt DESC")
    suspend fun getStylesByNovelOnce(novelId: Long): List<StyleReferenceEntity>

    @Query("SELECT * FROM style_references WHERE novelId = :novelId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentStyles(novelId: Long, limit: Int = 3): List<StyleReferenceEntity>

    @Insert
    suspend fun insertStyle(style: StyleReferenceEntity): Long

    @Delete
    suspend fun deleteStyle(style: StyleReferenceEntity)
}
