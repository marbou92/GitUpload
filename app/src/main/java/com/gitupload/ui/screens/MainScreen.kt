package com.gitupload.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.components.PersistentUploadSnackbar
import com.gitupload.ui.components.TopBarHeader
import com.gitupload.ui.theme.*

data class NavItem(
    val id: Int,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val enabledNavSections by viewModel.enabledNavSections.collectAsState()
    val stagedFiles by viewModel.stagedFiles.collectAsState()

    val allNavItems = remember {
        listOf(
            NavItem(0, "Repos", Icons.Default.Folder, Icons.Outlined.Folder, "nav_repos"),
            NavItem(1, "PC Upload", Icons.Default.CloudUpload, Icons.Outlined.CloudUpload, "nav_upload"),
            NavItem(2, "History", Icons.Default.History, Icons.Outlined.History, "nav_history"),
            NavItem(3, "AI Assistant", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
            NavItem(4, "Settings", Icons.Default.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    // Filter items based on customizable navigation settings
    val visibleNavItems = remember(enabledNavSections, allNavItems) {
        allNavItems.filter { enabledNavSections.contains(it.id) }
    }

    val selectedStagedCount = remember(stagedFiles) { stagedFiles.count { it.selected } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(GitDarkBg)) {
        val isWideScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Side Floating Navigation Rail for Wide Screens
            if (isWideScreen) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight()
                        .width(104.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        // Top Header Branding
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GitPrimaryGreen.copy(alpha = 0.18f))
                                    .border(1.dp, GitPrimaryGreen.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "GitUpload Logo",
                                    tint = GitPrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GitUpload",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GitPrimaryGreen,
                                fontSize = 11.sp
                            )
                        }

                        // Middle Navigation Items
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visibleNavItems.forEach { item ->
                                val isSelected = activeTab == item.id
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.setActiveTab(item.id) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (item.id == 1 && selectedStagedCount > 0) {
                                                    Badge(containerColor = GitPrimaryGreen, contentColor = Color.Black) {
                                                        Text(selectedStagedCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (item.id == 3) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(GitAccentPurple)
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = GitPrimaryGreen,
                                        selectedTextColor = GitPrimaryGreen,
                                        indicatorColor = GitPrimaryGreen.copy(alpha = 0.22f),
                                        unselectedIconColor = GitTextSecondary,
                                        unselectedTextColor = GitTextSecondary
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .testTag(item.tag)
                                )
                            }
                        }

                        // Bottom Account Quick Button
                        IconButton(
                            onClick = { viewModel.setActiveTab(4) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GitCardBorder.copy(alpha = 0.4f))
                        ) {
                            if (!selectedAccount?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedAccount?.avatarUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Account",
                                    tint = GitTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area
            Scaffold(
                topBar = {
                    TopBarHeader(
                        username = selectedAccount?.username,
                        avatarUrl = selectedAccount?.avatarUrl,
                        onAccountClick = { viewModel.setActiveTab(4) }
                    )
                },
                bottomBar = {
                    // Floating Bottom Navigation Bar for Mobile / Compact Screens
                    if (!isWideScreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(32.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                                ),
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    visibleNavItems.forEach { item ->
                                        val isSelected = activeTab == item.id

                                        Surface(
                                            onClick = { viewModel.setActiveTab(item.id) },
                                            color = if (isSelected) GitPrimaryGreen.copy(alpha = 0.18f) else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.testTag(item.tag)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                BadgedBox(
                                                    badge = {
                                                        if (item.id == 1 && selectedStagedCount > 0) {
                                                            Badge(containerColor = GitPrimaryGreen, contentColor = Color.Black) {
                                                                Text(selectedStagedCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        } else if (item.id == 3) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(GitAccentPurple)
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                        contentDescription = item.title,
                                                        tint = if (isSelected) GitPrimaryGreen else GitTextSecondary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GitPrimaryGreen,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = GitDarkBg,
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "screen_transition"
                    ) { targetScreenIndex ->
                        when (targetScreenIndex) {
                            0 -> RepositoriesScreen(viewModel = viewModel)
                            1 -> UploadScreen(viewModel = viewModel)
                            2 -> HistoryScreen(viewModel = viewModel)
                            3 -> AiChatScreen(viewModel = viewModel)
                            4 -> AccountSettingsScreen(viewModel = viewModel)
                            else -> RepositoriesScreen(viewModel = viewModel)
                        }
                    }

                    // Persistent overlay upload progress snackbar
                    PersistentUploadSnackbar(
                        progress = uploadProgress,
                        onDismiss = { viewModel.resetUploadProgress() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 76.dp)
                    )
                }
            }
        }
    }
}

