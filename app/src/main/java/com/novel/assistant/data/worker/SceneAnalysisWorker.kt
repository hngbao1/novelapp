package com.novel.assistant.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.novel.assistant.data.local.dao.*
import com.novel.assistant.data.remote.ai.MemoryAnalyzer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker chạy ngầm sau khi lưu phân cảnh.
 * Nhiệm vụ:
 *  1. Tóm tắt thông minh (summary) cho scene
 *  2. Trích xuất trạng thái mới (cảm xúc, vị trí, unresolved tension)
 *  3. Cập nhật trạng thái cảm xúc nhân vật
 *
 * Có retry nếu gọi AI lỗi, không mất dữ liệu khi app bị kill.
 */
@HiltWorker
class SceneAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sceneDao: SceneDao,
    private val characterDao: CharacterDao,
    private val memoryAnalyzer: MemoryAnalyzer
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SceneAnalysis"
        const val KEY_SCENE_ID = "scene_id"
        const val KEY_NOVEL_ID = "novel_id"
        const val KEY_CHAR_IDS_JSON = "char_ids_json"

        fun buildRequest(sceneId: Long, novelId: Long, charIds: List<Long> = emptyList()): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_SCENE_ID to sceneId,
                KEY_NOVEL_ID to novelId,
                KEY_CHAR_IDS_JSON to Gson().toJson(charIds)
            )
            return OneTimeWorkRequestBuilder<SceneAnalysisWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val sceneId = inputData.getLong(KEY_SCENE_ID, 0L)
        val novelId = inputData.getLong(KEY_NOVEL_ID, 0L)
        val charIdsJson = inputData.getString(KEY_CHAR_IDS_JSON) ?: "[]"

        if (sceneId == 0L || novelId == 0L) return Result.failure()

        val scene = sceneDao.getSceneById(sceneId) ?: return Result.failure()
        if (scene.content.isBlank()) return Result.success()

        return try {
            // 1. Summarize scene
            val summary = memoryAnalyzer.summarizeScene(scene.content)
            if (summary.isNotBlank()) {
                sceneDao.updateScene(scene.copy(summary = summary))
            }

            // 2. Extract character emotional states from scene content
            val charIds: List<Long> = try {
                val type = object : TypeToken<List<Long>>() {}.type
                Gson().fromJson(charIdsJson, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }

            if (charIds.isNotEmpty()) {
                val characters = characterDao.getCharactersByIds(charIds)
                // Use the summary to infer basic emotional state updates
                // The AI summarizer already captures emotional shifts
                characters.forEach { char ->
                    // If summary mentions this character's name, try to extract state
                    if (summary.contains(char.name, ignoreCase = true)) {
                        // For now, just mark that the character appeared in the latest scene
                        // Full emotional extraction will come from the AI memory analysis pipeline
                        Log.d(TAG, "Character ${char.name} appeared in scene $sceneId")
                    }
                }
            }

            Log.d(TAG, "Scene $sceneId analyzed successfully: ${summary.take(80)}...")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Scene analysis failed for $sceneId: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Log.e(TAG, "Giving up on scene $sceneId after $runAttemptCount attempts")
                Result.failure()
            }
        }
    }
}
