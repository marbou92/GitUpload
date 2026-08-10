package com.gitupload.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gitupload.data.models.GitHubContentItem
import com.gitupload.data.models.GitHubRepository
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.components.FileContentViewerDialog
import com.gitupload.ui.theme.*
import com.gitupload.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userRepos by viewModel.userRepos.collectAsState()
    val isLoading by viewModel.isReposLoading.collectAsState()
    val repoQuery by viewModel.repoQuery.collectAsState()
    val selectedRepoForExplorer by viewModel.selectedRepoForExplorer.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()

    var repoTypeFilter by remember { mutableStateOf("All") } // "All", "Public", "Private"

    val filteredRepos = remember(userRepos, repoQuery, repoTypeFilter) {
        userRepos.filter { repo ->
            val matchesQuery = repoQuery.isBlank() ||
                repo.name.contains(repoQuery, ignoreCase = true) ||
                (repo.description?.contains(repoQuery, ignoreCase = true) == true)
            val matchesType = when (repoTypeFilter) {
                "Public" -> !repo.private
                "Private" -> repo.private
                else -> true
            }
            matchesQuery && matchesType
        }
    }

    if (selectedRepoForExplorer != null) {
        // Show Repo Directory Tree Explorer
        RepoExplorerView(
            repo = selectedRepoForExplorer!!,
            viewModel = viewModel,
            onBack = { viewModel.closeExplorerRepo() }
        )
    } else {
        // Show Repo List
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // PC Upload Hero Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GitCardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GitPrimaryGreen.copy(alpha = 0.5f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GitPrimaryGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = GitPrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PC Upload & Local File Sync",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GitTextPrimary
                            )
                            Text(
                                text = "Upload local files or folder trees to your GitHub repositories",
                                style = MaterialTheme.typography.bodySmall,
                                color = GitTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.navigateToUpload() },
                        colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_pc_upload_hero_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PC Upload", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar & Filter
            OutlinedTextField(
                value = repoQuery,
                onValueChange = { viewModel.setRepoQuery(it) },
                placeholder = { Text("Search repositories...", color = GitTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GitTextSecondary) },
                trailingIcon = {
                    if (repoQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setRepoQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = GitTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GitCardBg,
                    unfocusedContainerColor = GitCardBg,
                    focusedBorderColor = GitPrimaryGreen,
                    unfocusedBorderColor = GitCardBorder,
                    focusedTextColor = GitTextPrimary,
                    unfocusedTextColor = GitTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_repos_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Material 3 Filter Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                listOf("All", "Public", "Private").forEach { filterType ->
                    val isSelected = repoTypeFilter == filterType
                    FilterChip(
                        selected = isSelected,
                        onClick = { repoTypeFilter = filterType },
                        label = { Text(filterType, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GitPrimaryGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = GitCardBg,
                            labelColor = GitTextSecondary
                        )
                    )
                }
            }

            if (selectedAccount == null) {
                Surface(
                    color = GitCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitBadgePrivate)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GitBadgePrivate)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exploring in Demo Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                            Text("Add your GitHub Personal Access Token in Accounts tab to view private repositories & commit directly.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repositories (${filteredRepos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GitTextPrimary
                )

                IconButton(
                    onClick = { viewModel.loadUserRepos() },
                    modifier = Modifier.testTag("refresh_repos_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = GitAccentCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadUserRepos() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("repos_pull_to_refresh_box")
            ) {
                if (filteredRepos.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.FolderOff, contentDescription = null, tint = GitTextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No repositories found", style = MaterialTheme.typography.bodyLarge, color = GitTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Swipe down to sync with GitHub", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredRepos, key = { it.id }) { repo ->
                            RepositoryCard(
                                repo = repo,
                                onExploreClick = { viewModel.openRepoInExplorer(repo, "") },
                                onUploadHereClick = { viewModel.prepareUploadForExplorerFolder(repo, "") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryCard(
    repo: GitHubRepository,
    onExploreClick: () -> Unit,
    onUploadHereClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExploreClick() }
            .testTag("repo_card_${repo.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        imageVector = if (repo.private) Icons.Default.Lock else Icons.Outlined.Folder,
                        contentDescription = "Repo Icon",
                        tint = if (repo.private) GitBadgePrivate else GitAccentCyan,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GitTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Repository Status Badge: Synced / Local Changes / Push Required
                    Surface(
                        color = GitPrimaryGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Status",
                                tint = GitPrimaryGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = GitPrimaryGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = if (repo.private) GitBadgePrivate.copy(alpha = 0.2f) else GitBadgePublic.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (repo.private) "Private" else "Public",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (repo.private) GitBadgePrivate else GitBadgePublic,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = repo.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = GitTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (repo.language != null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getLanguageColor(repo.language))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = repo.language, style = MaterialTheme.typography.labelSmall, color = GitTextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Icon(imageVector = Icons.Outlined.Star, contentDescription = "Stars", tint = GitTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall, color = GitTextSecondary)

                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(imageVector = Icons.Outlined.AccountTree, contentDescription = "Branch", tint = GitTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = repo.defaultBranch, style = MaterialTheme.typography.labelSmall, color = GitTextSecondary)
                }

                Button(
                    onClick = onUploadHereClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun RepoExplorerView(
    repo: GitHubRepository,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentPath by viewModel.explorerCurrentPath.collectAsState()
    val currentBranch by viewModel.explorerBranch.collectAsState()
    val availableBranches by viewModel.availableBranches.collectAsState()
    val contents by viewModel.explorerContents.collectAsState()
    val isLoading by viewModel.isExplorerLoading.collectAsState()
    val recentCommits by viewModel.recentCommits.collectAsState()
    val isCommitsLoading by viewModel.isCommitsLoading.collectAsState()

    val selectedFileForView by viewModel.selectedFileForView.collectAsState()
    val decodedFileContent by viewModel.decodedFileContent.collectAsState()
    val isFileContentLoading by viewModel.isFileContentLoading.collectAsState()
    val aiFileSummary by viewModel.aiFileSummary.collectAsState()

    var showBranchDropdown by remember { mutableStateOf(false) }
    var fileSearchQuery by remember { mutableStateOf("") }
    var showRecentActivity by remember { mutableStateOf(true) }

    val filteredContents = remember(contents, fileSearchQuery) {
        if (fileSearchQuery.isBlank()) contents
        else contents.filter { it.name.contains(fileSearchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Repo Header & Navigation Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    if (currentPath.isNotEmpty()) {
                        viewModel.navigateExplorerUp()
                    } else {
                        onBack()
                    }
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GitTextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GitTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (currentPath.isEmpty()) "root /" else "root / $currentPath",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = GitAccentCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Branch Dropdown Chip
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showBranchDropdown = true },
                    label = { Text(currentBranch, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = GitCardBg, labelColor = GitTextPrimary)
                )

                DropdownMenu(
                    expanded = showBranchDropdown,
                    onDismissRequest = { showBranchDropdown = false },
                    modifier = Modifier.background(GitCardBg)
                ) {
                    availableBranches.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b, color = GitTextPrimary) },
                            onClick = {
                                viewModel.changeExplorerBranch(b)
                                showBranchDropdown = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.closeExplorerRepo() },
                modifier = Modifier.testTag("exit_repo_explorer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Repository",
                    tint = GitTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // File Browser Search Bar
        OutlinedTextField(
            value = fileSearchQuery,
            onValueChange = { fileSearchQuery = it },
            placeholder = { Text("Search files & folders by name...", color = GitTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GitTextSecondary) },
            trailingIcon = {
                if (fileSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { fileSearchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = GitTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GitCardBg,
                unfocusedContainerColor = GitCardBg,
                focusedBorderColor = GitPrimaryGreen,
                unfocusedBorderColor = GitCardBorder,
                focusedTextColor = GitTextPrimary,
                unfocusedTextColor = GitTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_browser_search")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Recent Activity Dashboard (Last 5 Commits)
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(10.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRecentActivity = !showRecentActivity },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = GitAccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Activity (Last 5 Commits)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GitTextPrimary
                        )
                    }
                    Icon(
                        imageVector = if (showRecentActivity) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Activity",
                        tint = GitTextSecondary
                    )
                }

                AnimatedVisibility(visible = showRecentActivity) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (isCommitsLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                CircularProgressIndicator(color = GitPrimaryGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching GitHub commits...", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
                            }
                        } else if (recentCommits.isEmpty()) {
                            Text(
                                text = "No commit history loaded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GitTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            recentCommits.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.commit.message.lines().firstOrNull() ?: "Commit",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GitTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.commit.author.name} • ${item.commit.author.date?.take(10) ?: "Recent"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GitTextSecondary
                                        )
                                    }
                                    Surface(
                                        color = GitCardBorder,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.sha.take(7),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = GitAccentCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = GitCardBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.prepareUploadForExplorerFolder(repo, currentPath) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GitPrimaryGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Folder / Files to '${if (currentPath.isEmpty()) "root" else currentPath}'")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GitPrimaryGreen)
            }
        } else if (filteredContents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (fileSearchQuery.isNotEmpty()) "No files match '$fileSearchQuery'" else "Directory is empty or path not found",
                    color = GitTextSecondary
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredContents, key = { it.path }) { item ->
                    ContentItemRow(
                        item = item,
                        onClick = {
                            if (item.type == "dir") {
                                viewModel.navigateExplorerSubfolder(item.path)
                            } else {
                                viewModel.selectFileForView(item)
                            }
                        }
                    )
                }
            }
        }
    }


    // Modal File Code Viewer
    if (selectedFileForView != null) {
        FileContentViewerDialog(
            item = selectedFileForView!!,
            content = decodedFileContent,
            isLoading = isFileContentLoading,
            aiSummary = aiFileSummary,
            onGenerateAiSummary = { viewModel.generateAiFileSummary() },
            onDismiss = { viewModel.closeFileView() }
        )
    }
}

@Composable
fun ContentItemRow(
    item: GitHubContentItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = GitCardBg,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (item.type == "dir") Icons.Default.Folder else Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = if (item.type == "dir") GitAccentCyan else GitTextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (item.type != "dir") FontFamily.Monospace else FontFamily.Default,
                    color = GitTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.type != "dir") {
                Text(
                    text = item.size.formatFileSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GitTextSecondary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = GitTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getLanguageColor(lang: String): Color {
    return when (lang.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "typescript" -> Color(0xFF3178C6)
        "javascript" -> Color(0xFFF1E05A)
        "python" -> Color(0xFF3572A5)
        "go" -> Color(0xFF00ADD8)
        "rust" -> Color(0xFFDEA584)
        "html" -> Color(0xFFE34C26)
        "css" -> Color(0xFF563D7C)
        else -> GitAccentCyan
    }
}

