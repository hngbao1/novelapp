package com.novel.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val sourceNovelName: String = "",
    val sourceNovelDescription: String = "",
    val coverImagePath: String? = null,
    val currentMood: String = "",
    val styleVibeTags: String = "[]", // JSON list
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val title: String,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId"), Index("novelId")]
)
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val novelId: Long,
    val title: String = "",
    val content: String = "",
    val userPrompt: String = "",
    val promptSettings: String = "{}", // JSON
    val mood: String = "",
    val vibeTags: String = "[]", // JSON list
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val favoriteNotes: String = "",
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scene_versions",
    foreignKeys = [ForeignKey(
        entity = SceneEntity::class,
        parentColumns = ["id"],
        childColumns = ["sceneId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sceneId")]
)
data class SceneVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sceneId: Long,
    val content: String,
    val versionNumber: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "characters",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val name: String,
    val description: String = "",
    val personality: String = "",
    val speechStyle: String = "",
    val fears: String = "",
    val importantThings: String = "",
    val avatarPath: String? = null,
    val currentEmotionalState: String = "",
    val isMainCharacter: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "character_corrections",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId"), Index("novelId")]
)
data class CharacterCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val novelId: Long,
    val correctionType: String = "", // speech, behavior, reaction
    val wrongExample: String = "",
    val rightDescription: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "relationships",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val char1Id: Long,
    val char2Id: Long,
    val description: String = "",
    val status: String = "",
    val intimacyLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "memories",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val content: String,
    val summary: String = "",
    val type: String = "PERMANENT", // PERMANENT, TEMPORARY, ARC
    val category: String = "", // emotion_change, relationship, promise, trauma, development
    val relatedCharacterIds: String = "[]", // JSON list of IDs
    val relatedSceneId: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timeline_events",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val sceneId: Long? = null,
    val chapterId: Long? = null,
    val eventDescription: String,
    val eventType: String = "", // plot_point, secret_revealed, relationship_change, arc_start, arc_end
    val involvedCharacterIds: String = "[]", // JSON list
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "style_references",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class StyleReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val sceneId: Long? = null,
    val rhythmNotes: String = "",
    val dialogueStyle: String = "",
    val emotionStyle: String = "",
    val descriptionStyle: String = "",
    val sampleText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
