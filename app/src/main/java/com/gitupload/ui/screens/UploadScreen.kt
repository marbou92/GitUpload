package com.gitupload.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gitupload.data.models.GitHubRepository
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.components.CreateTextFileDialog
import com.gitupload.ui.components.StagedFileCard
import com.gitupload.ui.components.UploadProgressOverlay
import com.gitupload.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userRepos by viewModel.userRepos.collectAsState()
    val targetRepo by viewModel.targetRepo.collectAsState()
    val targetBranch by viewModel.targetBranch.collectAsState()
    val availableBranches by viewModel.availableBranches.collectAsState()
    val targetSubfolder by viewModel.targetSubfolder.collectAsState()
    val stagedFiles by viewModel.stagedFiles.collectAsState()
    val commitMessage by viewModel.commitMessage.collectAsState()
    val isGeneratingCommitMsg by viewModel.isGeneratingCommitMsg.collectAsState()
    val isScanningFolder by viewModel.isScanningFolder.collectAsState()
    val scannedFileCount by viewModel.scannedFileCount.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    var showRepoDropdown by remember { mutableStateOf(false) }
    var showBranchDropdown by remember { mutableStateOf(false) }
    var showCreateTextDialog by remember { mutableStateOf(false) }

    // Folder Tree SAF Launcher
    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanFolderUri(uri)
        }
    }

    // Multiple Files SAF Launcher
    val multiFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.scanMultipleFiles(uris)
        }
    }

    val selectedCount = remember(stagedFiles) { stagedFiles.count { it.selected } }
    val totalSizeBytes = remember(stagedFiles) { stagedFiles.filter { it.selected }.sumOf { it.sizeBytes } }
    val formattedTotalSize = remember(totalSizeBytes) {
        if (totalSizeBytes < 1024) "$totalSizeBytes B"
        else if (totalSizeBytes < 1024 * 1024) "%.1f KB".format(totalSizeBytes / 1024.0)
        else "%.2f MB".format(totalSizeBytes / (1024.0 * 1024.0))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Section: Target Repository & Branch Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TARGET REPOSITORY & DESTINATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GitTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Target Repo Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showRepoDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GitTextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_repo_dropdown")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Folder, contentDescription = null, tint = GitAccentCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = targetRepo?.name ?: "Select Repository",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showRepoDropdown,
                            onDismissRequest = { showRepoDropdown = false },
                            modifier = Modifier.background(GitCardBg)
                        ) {
                            userRepos.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.fullName, color = GitTextPrimary) },
                                    onClick = {
                                        viewModel.setTargetRepo(r)
                                        showRepoDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Target Branch Dropdown
                    Box(modifier = Modifier.width(110.dp)) {
                        OutlinedButton(
                            onClick = { showBranchDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GitTextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(targetBranch, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showBranchDropdown,
                            onDismissRequest = { showBranchDropdown = false },
                            modifier = Modifier.background(GitCardBg)
                        ) {
                            availableBranches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b, color = GitTextPrimary) },
                                    onClick = {
                                        viewModel.setTargetBranch(b)
                                        showBranchDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Target Subfolder path inside repository
                OutlinedTextField(
                    value = targetSubfolder,
                    onValueChange = { viewModel.setTargetSubfolder(it) },
                    placeholder = { Text("Subfolder path (optional, e.g. 'src/components/')", color = GitTextSecondary, style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    label = { Text("Destination Subfolder Path") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitPrimaryGreen,
                        unfocusedBorderColor = GitCardBorder,
                        focusedTextColor = GitTextPrimary,
                        unfocusedTextColor = GitTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PC-Like Upload Actions: Select Folder / Files / New File
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SELECT SOURCE FILES / FOLDER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GitTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderTreeLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_folder_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Folder", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { multiFilesLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GitAccentCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_files_btn")
                    ) {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Files", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = { showCreateTextDialog = true },
                        modifier = Modifier
                            .background(GitCodeBg, RoundedCornerShape(8.dp))
                            .testTag("create_file_btn")
                    ) {
                        Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "Create Text File", tint = GitAccentPurple)
                    }
                }

                if (isScanningFolder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GitPrimaryGreen, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning directory tree... ($scannedFileCount files found)",
                            style = MaterialTheme.typography.labelSmall,
                            color = GitAccentCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Staged Files Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = stagedFiles.isNotEmpty() && stagedFiles.all { it.selected },
                    onCheckedChange = { checked ->
                        viewModel.toggleAllStagedFilesSelected(checked)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = GitPrimaryGreen, uncheckedColor = GitTextSecondary)
                )

                Text(
                    text = "Staged (${selectedCount}/${stagedFiles.size}) • $formattedTotalSize",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GitTextPrimary
                )
            }

            if (stagedFiles.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearStagedFiles() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Staged Files List
        if (stagedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GitCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, GitCardBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, tint = GitTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No files staged yet", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Click 'Pick Folder' above to upload an entire directory tree, like on PC web GitHub!", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stagedFiles, key = { it.id }) { file ->
                    StagedFileCard(
                        file = file,
                        onToggleSelect = { viewModel.toggleStagedFileSelected(file.id) },
                        onDelete = { viewModel.removeStagedFile(file.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Commit Message & Execute Section
        if (stagedFiles.isNotEmpty()) {
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
                        Text(
                            text = "COMMIT MESSAGE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GitTextSecondary
                        )

                        TextButton(
                            onClick = { viewModel.generateAiCommitMessage() },
                            enabled = !isGeneratingCommitMsg,
                            colors = ButtonDefaults.textButtonColors(contentColor = GitAccentPurple)
                        ) {
                            if (isGeneratingCommitMsg) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = GitAccentPurple)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✨ AI Commit Msg", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { viewModel.setCommitMessage(it) },
                        placeholder = { Text("e.g. 'feat: add components folder'", color = GitTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GitPrimaryGreen,
                            unfocusedBorderColor = GitCardBorder,
                            focusedTextColor = GitTextPrimary,
                            unfocusedTextColor = GitTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.executeUpload() },
                        enabled = selectedCount > 0 && targetRepo != null,
                        colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("execute_upload_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload $selectedCount Files to GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Interactive Dialogs
    if (showCreateTextDialog) {
        CreateTextFileDialog(
            onDismiss = { showCreateTextDialog = false },
            onCreate = { fileName, relativePath, content ->
                viewModel.addRawTextFile(fileName, relativePath, content)
            }
        )
    }

    // Upload Execution Live Progress Modal
    UploadProgressOverlay(
        progress = uploadProgress,
        onClose = { viewModel.resetUploadProgress() }
    )
}
