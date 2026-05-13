package com.novel.assistant.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.entity.*

@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class,
        SceneEntity::class,
        SceneVersionEntity::class,
        CharacterEntity::class,
        CharacterCorrectionEntity::class,
        RelationshipEntity::class,
        MemoryEntity::class,
        TimelineEventEntity::class,
        StyleReferenceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun sceneDao(): SceneDao
    abstract fun sceneVersionDao(): SceneVersionDao
    abstract fun characterDao(): CharacterDao
    abstract fun characterCorrectionDao(): CharacterCorrectionDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun memoryDao(): MemoryDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun styleReferenceDao(): StyleReferenceDao
}
