package com.novel.assistant.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scenes ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE relationships ADD COLUMN dynamics TEXT NOT NULL DEFAULT ''")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Character Voice Lock
                db.execSQL("ALTER TABLE characters ADD COLUMN voiceRhythm TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN evasionLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN initiativeLevel INTEGER NOT NULL DEFAULT 0")
                
                // Style Vibe Preservation
                db.execSQL("ALTER TABLE style_references ADD COLUMN atmosphere TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE style_references ADD COLUMN emotionalRhythm TEXT NOT NULL DEFAULT ''")
            }
        }
    }

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
