package com.gitupload.data.models

import android.net.Uri

data class StagedFile(
    val id: String,
    val uri: Uri?,
    val fileName: String,
    val relativePath: String, // e.g. "src/components/Header.kt"
    val sizeBytes: Long,
    val isText: Boolean,
    val mimeType: String,
    val contentBytes: ByteArray?,
    val textPreview: String? = null,
    val selected: Boolean = true
) {
    val formattedSize: String
        get() {
            if (sizeBytes < 1024) return "$sizeBytes B"
            if (sizeBytes < 1024 * 1024) return "%.1f KB".format(sizeBytes / 1024.0)
            return "%.2f MB".format(sizeBytes / (1024.0 * 1024.0))
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StagedFile

        if (id != other.id) return false
        if (relativePath != other.relativePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + relativePath.hashCode()
        return result
    }
}

enum class UploadStatusState {
    IDLE,
    SCANNING,
    PREPARING,
    UPLOADING_BLOBS,
    CREATING_TREE,
    CREATING_COMMIT,
    UPDATING_REF,
    SUCCESS,
    ERROR
}

data class UploadProgress(
    val state: UploadStatusState = UploadStatusState.IDLE,
    val currentFileName: String = "",
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val progressPercent: Float = 0f,
    val message: String = "",
    val errorMessage: String? = null,
    val commitSha: String? = null,
    val commitHtmlUrl: String? = null
)
