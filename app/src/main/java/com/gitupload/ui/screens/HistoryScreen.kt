package com.gitupload.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gitupload.data.db.UploadLogEntity
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uploadLogs by viewModel.uploadLogs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Commit & Upload History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )

            Text(
                text = "${uploadLogs.size} logs",
                style = MaterialTheme.typography.labelSmall,
                color = GitTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uploadLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = GitTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No upload history yet", style = MaterialTheme.typography.bodyLarge, color = GitTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Uploaded folder trees & files will be logged here.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uploadLogs, key = { it.id }) { log ->
                    UploadLogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun UploadLogCard(log: UploadLogEntity) {
    val context = LocalContext.current
    val dateStr = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val formattedSize = remember(log.totalSizeBytes) {
        if (log.totalSizeBytes < 1024) "${log.totalSizeBytes} B"
        else if (log.totalSizeBytes < 1024 * 1024) "%.1f KB".format(log.totalSizeBytes / 1024.0)
        else "%.2f MB".format(log.totalSizeBytes / (1024.0 * 1024.0))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (log.isSuccess) GitPrimaryGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = log.repoFullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GitTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = GitCardBorder,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = log.branch,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = GitAccentCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.commitMessage.ifBlank { "Upload ${log.fileCount} files" },
                style = MaterialTheme.typography.bodySmall,
                color = GitTextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.fileCount} files • $formattedSize • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = GitTextSecondary
                )

                if (!log.commitHtmlUrl.isNull_or_empty()) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Commit URL", log.commitHtmlUrl))
                            Toast.makeText(context, "Commit Web Link Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy Link", tint = GitAccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
