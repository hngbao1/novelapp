package com.novel.assistant.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.novel.assistant.BuildConfig
import com.novel.assistant.data.backup.BackupManager
import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.local.database.AppDatabase
import com.novel.assistant.data.local.datastore.AppPreferences
import com.novel.assistant.data.local.datastore.dataStore
import com.novel.assistant.data.remote.ai.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // === Database ===
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "novel_ai_database"
        ).build()
    }

    // === DAOs ===
    @Provides fun provideNovelDao(db: AppDatabase): NovelDao = db.novelDao()
    @Provides fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()
    @Provides fun provideSceneDao(db: AppDatabase): SceneDao = db.sceneDao()
    @Provides fun provideSceneVersionDao(db: AppDatabase): SceneVersionDao = db.sceneVersionDao()
    @Provides fun provideCharacterDao(db: AppDatabase): CharacterDao = db.characterDao()
    @Provides fun provideCorrectionDao(db: AppDatabase): CharacterCorrectionDao = db.characterCorrectionDao()
    @Provides fun provideRelationshipDao(db: AppDatabase): RelationshipDao = db.relationshipDao()
    @Provides fun provideMemoryDao(db: AppDatabase): MemoryDao = db.memoryDao()
    @Provides fun provideTimelineEventDao(db: AppDatabase): TimelineEventDao = db.timelineEventDao()
    @Provides fun provideStyleReferenceDao(db: AppDatabase): StyleReferenceDao = db.styleReferenceDao()

    // === DataStore ===
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAppPreferences(dataStore: DataStore<Preferences>): AppPreferences {
        return AppPreferences(dataStore)
    }

    // === AI ===
    @Provides
    @Singleton
    fun provideKeyRotationManager(): KeyRotationManager {
        val manager = KeyRotationManager()
        manager.initialize(
            mainKeys = BuildConfig.GEMINI_MAIN_KEYS,
            memoryKeys = BuildConfig.GEMINI_MEMORY_KEYS,
            generatorKeys = BuildConfig.GEMINI_GENERATOR_KEYS
        )
        return manager
    }

    @Provides
    @Singleton
    fun provideGeminiProvider(keyRotationManager: KeyRotationManager): GeminiProvider {
        val provider = GeminiProvider(keyRotationManager)
        provider.configure(
            generationModel = BuildConfig.GEMINI_MODEL_MAIN,
            memoryModel = BuildConfig.GEMINI_MODEL_MEMORY,
            mainTokens = BuildConfig.GEMINI_MAX_OUTPUT_TOKENS_MAIN,
            memoryTokens = BuildConfig.GEMINI_MAX_OUTPUT_TOKENS_MEMORY
        )
        return provider
    }

    @Provides
    @Singleton
    fun provideAiProvider(geminiProvider: GeminiProvider): AiProvider = geminiProvider

    // === Backup ===
    @Provides
    @Singleton
    fun provideBackupManager(
        database: AppDatabase,
        @ApplicationContext context: Context
    ): BackupManager {
        return BackupManager(database, context)
    }
}
