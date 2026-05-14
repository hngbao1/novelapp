package com.novel.assistant.data.remote.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeProvider @Inject constructor() : SceneGenerator {
    override suspend fun generateScene(request: SceneRequest): Flow<String> = flow {
        emit("ClaudeProvider: Đang phát triển...")
    }

    override suspend fun refineScene(
        currentContent: String,
        instruction: String,
        context: String
    ): Flow<String> = flow {
        emit("ClaudeProvider: Đang phát triển...")
    }
}
