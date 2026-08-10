package com.gitupload.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gitupload.data.models.StagedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

object FolderScanner {

    suspend fun scanFolderUri(
        context: Context,
        treeUri: Uri,
        onProgress: (scannedCount: Int, currentName: String) -> Unit = { _, _ -> }
    ): List<StagedFile> = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val stagedFiles = mutableListOf<StagedFile>()
        val countRef = intArrayOf(0)

        // rootDir name will serve as the top-level directory name or we start relative paths inside it
        traverseDirectory(
            context = context,
            dir = rootDir,
            currentRelativePath = "",
            stagedFiles = stagedFiles,
            countRef = countRef,
            onProgress = onProgress
        )

        stagedFiles
    }

    private fun traverseDirectory(
        context: Context,
        dir: DocumentFile,
        currentRelativePath: String,
        stagedFiles: MutableList<StagedFile>,
        countRef: IntArray,
        onProgress: (scannedCount: Int, currentName: String) -> Unit
    ) {
        val files = dir.listFiles()
        for (file in files) {
            val fileName = file.name ?: continue
            // Skip hidden git or system directories if needed, or include them if non-empty
            if (fileName.startsWith(".git") || fileName.startsWith(".DS_Store")) {
                continue
            }

            val relativePath = if (currentRelativePath.isEmpty()) {
                fileName
            } else {
                "$currentRelativePath/$fileName"
            }

            if (file.isDirectory) {
                traverseDirectory(context, file, relativePath, stagedFiles, countRef, onProgress)
            } else if (file.isFile) {
                countRef[0]++
                onProgress(countRef[0], fileName)

                val stagedFile = readSingleFile(context, file.uri, fileName, relativePath)
                if (stagedFile != null) {
                    stagedFiles.add(stagedFile)
                }
            }
        }
    }

    suspend fun scanMultipleFiles(
        context: Context,
        uris: List<Uri>
    ): List<StagedFile> = withContext(Dispatchers.IO) {
        val staged = mutableListOf<StagedFile>()
        for (uri in uris) {
            val doc = DocumentFile.fromSingleUri(context, uri)
            val name = doc?.name ?: getUriFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
            val file = readSingleFile(context, uri, name, name)
            if (file != null) {
                staged.add(file)
            }
        }
        staged
    }

    fun readSingleFile(
        context: Context,
        uri: Uri,
        fileName: String,
        relativePath: String
    ): StagedFile? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: return null

            val isText = isTextFile(fileName, mimeType)
            val preview = if (isText) {
                val str = String(bytes, Charsets.UTF_8)
                if (str.length > 300) str.substring(0, 300) + "..." else str
            } else null

            StagedFile(
                id = UUID.randomUUID().toString(),
                uri = uri,
                fileName = fileName,
                relativePath = relativePath,
                sizeBytes = bytes.size.toLong(),
                isText = isText,
                mimeType = mimeType,
                contentBytes = bytes,
                textPreview = preview,
                selected = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createRawTextStagedFile(
        fileName: String,
        relativePath: String,
        content: String
    ): StagedFile {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val preview = if (content.length > 300) content.substring(0, 300) + "..." else content
        return StagedFile(
            id = UUID.randomUUID().toString(),
            uri = null,
            fileName = fileName,
            relativePath = relativePath,
            sizeBytes = bytes.size.toLong(),
            isText = true,
            mimeType = "text/plain",
            contentBytes = bytes,
            textPreview = preview,
            selected = true
        )
    }

    private fun getUriFileName(context: Context, uri: Uri): String? {
        val doc = DocumentFile.fromSingleUri(context, uri)
        return doc?.name
    }

    private fun isTextFile(fileName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml")) return true
        val textExtensions = listOf(
            "txt", "md", "json", "kt", "java", "xml", "gradle", "kts", "properties",
            "js", "ts", "jsx", "tsx", "html", "css", "scss", "py", "sh", "yml", "yaml",
            "c", "cpp", "h", "go", "rs", "rb", "php", "sql", "gitignore", "env"
        )
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return textExtensions.contains(ext)
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "md" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}
