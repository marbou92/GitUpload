package com.gitupload.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.gitupload.data.models.GitHubContentItem
import com.gitupload.data.models.StagedFile
import com.gitupload.data.models.UploadProgress
import com.gitupload.data.models.UploadStatusState
import com.gitupload.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHeader(
    username: String?,
    avatarUrl: String?,
    onAccountClick: () -> Unit
) {
    Surface(
        color = GitCardBg,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GitPrimaryGreen)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "App Logo",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "GitUpload",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GitTextPrimary
                    )
                    Text(
                        text = "Folder & File Uploader",
                        style = MaterialTheme.typography.labelSmall,
                        color = GitTextSecondary
                    )
                }
            }

            // Account status badge
            Surface(
                onClick = onAccountClick,
                shape = RoundedCornerShape(20.dp),
                color = GitCardBorder,
                modifier = Modifier.testTag("account_header_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Account",
                            tint = GitTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = username ?: "Demo Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (username != null) GitPrimaryGreen else GitAccentCyan
                    )
                }
            }
        }
    }
}


@Composable
fun StagedFileCard(
    file: StagedFile,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("staged_file_${file.fileName}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = file.selected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GitPrimaryGreen,
                            uncheckedColor = GitTextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (file.isText) Icons.Outlined.InsertDriveFile else Icons.Outlined.Image,
                        contentDescription = "File Type",
                        tint = if (file.isText) GitAccentCyan else GitAccentPurple,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.relativePath,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GitTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${file.fileName} • ${file.formattedSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GitTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove File",
                        tint = GitTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (file.textPreview != null && file.selected) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = GitCodeBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = file.textPreview,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = GitTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateTextFileDialog(
    onDismiss: () -> Unit,
    onCreate: (fileName: String, relativePath: String, content: String) -> Unit
) {
    var fileName by remember { mutableStateOf("new_file.kt") }
    var relativePath by remember { mutableStateOf("src/new_file.kt") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GitCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = null,
                    tint = GitPrimaryGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Text File", color = GitTextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        if (!relativePath.contains('/')) {
                            relativePath = it
                        } else {
                            val parent = relativePath.substringBeforeLast('/', "")
                            relativePath = if (parent.isEmpty()) it else "$parent/$it"
                        }
                    },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = relativePath,
                    onValueChange = { relativePath = it },
                    label = { Text("Relative Repository Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("File Content") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onCreate(fileName, relativePath, content)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen)
            ) {
                Text("Add File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GitTextSecondary)
            }
        }
    )
}@Composable
fun FileContentViewerDialog(
    item: GitHubContentItem,
    content: String?,
    isLoading: Boolean,
    aiSummary: String?,
    onGenerateAiSummary: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    val fileExt = if (item.name.contains('.')) item.name.substringAfterLast('.').uppercase() else "TEXT"
    val codeLines = remember(content) {
        content?.lines() ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = GitDarkBg,
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(GitAccentCyan.copy(alpha = 0.5f))
            ),
            modifier = Modifier
                .fillMaxWidth(if (isFullScreen) 0.98f else 0.95f)
                .fillMaxHeight(if (isFullScreen) 0.95f else 0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = GitAccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = fileExt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GitAccentCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GitTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isFullScreen = !isFullScreen }) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = GitTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GitTextSecondary
                            )
                        }
                    }
                }

                // Subheader Metadata & Copy Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.path} • ${codeLines.size} lines • ${item.size} bytes",
                        style = MaterialTheme.typography.labelSmall,
                        color = GitTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(
                            onClick = {
                                if (!content.isNullOrEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", content))
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = GitAccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // AI Explanation Banner if present or button to generate
                if (aiSummary != null) {
                    Surface(
                        color = GitCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(GitAccentPurple.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GitAccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI File Explanation",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GitAccentPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = GitTextPrimary
                            )
                        }
                    }
                } else if (!isLoading && content != null) {
                    OutlinedButton(
                        onClick = onGenerateAiSummary,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GitAccentPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ Explain Code with AI Assistant", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Code Viewport with Monospaced Line Gutter
                Surface(
                    color = GitCodeBg,
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GitPrimaryGreen, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Fetching file source code...", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
                            }
                        }
                    } else if (content != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            itemsIndexed(codeLines) { index, lineText ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                ) {
                                    // Line Number Gutter
                                    Text(
                                        text = (index + 1).toString().padStart(4, ' '),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = GitTextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.width(36.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Line text
                                    Text(
                                        text = if (lineText.isEmpty()) " " else lineText,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        color = GitTextPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Unable to load file content", color = GitTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close Viewer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UploadProgressOverlay(
    progress: UploadProgress,
    onClose: () -> Unit
) {
    if (progress.state == UploadStatusState.IDLE) return

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (progress.state == UploadStatusState.SUCCESS || progress.state == UploadStatusState.ERROR) {
                onClose()
            }
        },
        containerColor = GitDarkBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (progress.state) {
                    UploadStatusState.SUCCESS -> Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GitPrimaryGreen)
                    UploadStatusState.ERROR -> Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    else -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GitPrimaryGreen, strokeWidth = 2.dp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (progress.state) {
                        UploadStatusState.SUCCESS -> "Upload Successful! 🎉"
                        UploadStatusState.ERROR -> "Upload Failed"
                        else -> "Uploading to GitHub..."
                    },
                    color = GitTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GitTextPrimary
                )

                if (progress.state != UploadStatusState.SUCCESS && progress.state != UploadStatusState.ERROR) {
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GitPrimaryGreen,
                        trackColor = GitCardBorder,
                    )

                    if (progress.totalFiles > 0) {
                        Text(
                            text = "File ${progress.completedFiles}/${progress.totalFiles} • ${progress.currentFileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GitTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (progress.state == UploadStatusState.SUCCESS) {
                    Surface(
                        color = GitCardBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Commit SHA:", style = MaterialTheme.typography.labelSmall, color = GitTextSecondary)
                            Text(
                                text = progress.commitSha ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = GitAccentCyan
                            )

                            if (!progress.commitHtmlUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Commit URL", progress.commitHtmlUrl))
                                        Toast.makeText(context, "Commit Link Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GitPrimaryGreen)
                                ) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Commit Web Link")
                                }
                            }
                        }
                    }
                }

                if (progress.state == UploadStatusState.ERROR) {
                    Text(
                        text = progress.errorMessage ?: "Unknown error occurred",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (progress.state == UploadStatusState.SUCCESS || progress.state == UploadStatusState.ERROR) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen)
                ) {
                    Text("Done")
                }
            }
        }
    )
}

@Composable
fun PersistentUploadSnackbar(
    progress: UploadProgress,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (progress.state == UploadStatusState.IDLE) return

    val context = LocalContext.current
    val isFinished = progress.state == UploadStatusState.SUCCESS || progress.state == UploadStatusState.ERROR
    val isSuccess = progress.state == UploadStatusState.SUCCESS

    Surface(
        color = GitDarkBg,
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSuccess) GitPrimaryGreen
                else if (progress.state == UploadStatusState.ERROR) MaterialTheme.colorScheme.error
                else GitAccentCyan
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("persistent_upload_snackbar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isFinished) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GitAccentCyan,
                            strokeWidth = 2.dp
                        )
                    } else if (isSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = GitPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = when (progress.state) {
                                UploadStatusState.SCANNING -> "Scanning files & directories..."
                                UploadStatusState.PREPARING -> "Preparing upload payload..."
                                UploadStatusState.UPLOADING_BLOBS -> "Uploading file blobs to GitHub..."
                                UploadStatusState.CREATING_TREE -> "Building Git repository tree..."
                                UploadStatusState.CREATING_COMMIT -> "Generating Git commit..."
                                UploadStatusState.UPDATING_REF -> "Updating target branch ref..."
                                UploadStatusState.SUCCESS -> "Upload completed successfully!"
                                UploadStatusState.ERROR -> "Upload failed"
                                else -> "Processing upload..."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GitTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (progress.currentFileName.isNotBlank() && !isFinished) {
                            Text(
                                text = "File ${progress.completedFiles}/${progress.totalFiles}: ${progress.currentFileName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GitTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isFinished && progress.message.isNotBlank()) {
                            Text(
                                text = progress.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSuccess) GitPrimaryGreen else MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSuccess && !progress.commitHtmlUrl.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Commit URL", progress.commitHtmlUrl))
                                Toast.makeText(context, "Copied Commit Link to Clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Copy Commit Link",
                                tint = GitAccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = GitTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!isFinished) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GitPrimaryGreen,
                        trackColor = GitCardBorder
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${(progress.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GitAccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
