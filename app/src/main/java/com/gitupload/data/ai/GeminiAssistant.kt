package com.gitupload.data.ai

import com.gitupload.data.models.StagedFile

object GeminiAssistant {

    suspend fun generateCommitMessage(stagedFiles: List<StagedFile>): String {
        return AiAssistantManager.generateCommitMessage(stagedFiles)
    }

    suspend fun explainCodeOrFile(fileName: String, content: String): String {
        return AiAssistantManager.explainCodeOrFile(fileName, content)
    }

    suspend fun askAssistant(userMessage: String, contextInfo: String = ""): String {
        return AiAssistantManager.askAssistant(userMessage, contextInfo)
    }
}
