package com.gitupload.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gitupload.data.ai.AiAssistantManager
import com.gitupload.data.ai.AiProvider
import com.gitupload.data.ai.AiProviderConfig
import com.gitupload.data.db.AccountEntity
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.launch

enum class SubSettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    ACCOUNT("Account", "GitHub OAuth & Personal Access Tokens", Icons.Default.Person),
    AI_HUB("AI Hub", "Multi-provider AI Model & API Key setup", Icons.Default.AutoAwesome),
    REPOS_GIT("Git Defaults & Repositories", "Branch defaults, webhook sync & commit settings", Icons.Default.AccountTree),
    APPEARANCE("Appearance", "Theme Mode & Accent Color Palettes", Icons.Default.Palette),
    PRIVACY("Privacy & Security", "Token encryption, security & credentials", Icons.Default.Security),
    STORAGE("Storage & Cache", "Offline file tree cache & database stats", Icons.Default.Storage),
    BACKUP("Backup and restore", "Export or restore configuration settings", Icons.Default.CloudUpload),
    SYSTEM_UPDATE("System update", "GitUpload v2.5.0 • Up to date", Icons.Default.SystemUpdate)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val patInput by viewModel.patInput.collectAsState()
    val accountError by viewModel.accountError.collectAsState()
    val isAddingAccount by viewModel.isAddingAccount.collectAsState()
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val currentPalette by ThemeManager.currentPalette.collectAsState()

    var activeSubPage by remember { mutableStateOf<SubSettingsPage?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showTokenVisible by remember { mutableStateOf(false) }
    var isOAuthLoading by remember { mutableStateOf(false) }

    // AI Provider Configuration state
    val aiConfig by AiAssistantManager.currentConfig.collectAsState()
    var selectedProvider by remember(aiConfig) { mutableStateOf(aiConfig.provider) }
    var selectedModel by remember(aiConfig) { mutableStateOf(aiConfig.selectedModel) }
    var customApiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
    var customBaseUrl by remember(aiConfig) { mutableStateOf(aiConfig.baseUrl) }
    var showApiKeyVisible by remember { mutableStateOf(false) }
    var aiTestStatus by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenGeneratorUrl = "https://github.com/settings/tokens/new?scopes=repo,read:org,user&description=GitUpload%20Android%20App"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (activeSubPage != null) {
                IconButton(
                    onClick = { activeSubPage = null },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GitTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                IconButton(
                    onClick = { /* Main Screen or Back */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GitTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = activeSubPage?.title ?: "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = activeSubPage,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "settings_navigation"
        ) { subPage ->
            if (subPage == null) {
                // ================= MAIN SETTINGS MENU (Matching Image 1) =================
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search", color = GitTextSecondary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = GitTextSecondary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = GitTextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GitAccentCyan,
                                unfocusedBorderColor = GitCardBorder,
                                focusedContainerColor = GitCardBg,
                                unfocusedContainerColor = GitCardBg,
                                focusedTextColor = GitTextPrimary,
                                unfocusedTextColor = GitTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_search_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Setting Category Cards (Single card container with items matching Image 1)
                    val filteredPages = SubSettingsPage.values().filter { page ->
                        searchQuery.isEmpty() ||
                                page.title.contains(searchQuery, ignoreCase = true) ||
                                page.subtitle.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredPages, key = { it.name }) { page ->
                        SettingsGroupCardItem(
                            page = page,
                            onClick = { activeSubPage = page }
                        )
                    }
                }
            } else {
                // ================= SUB-SETTINGS PAGE CONTENT =================
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = subPage.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GitTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    when (subPage) {
                        SubSettingsPage.ACCOUNT -> {
                            item {
                                SubSettingsAccountContent(
                                    accounts = accounts,
                                    selectedAccount = selectedAccount,
                                    patInput = patInput,
                                    showTokenVisible = showTokenVisible,
                                    accountError = accountError,
                                    isAddingAccount = isAddingAccount,
                                    isOAuthLoading = isOAuthLoading,
                                    tokenGeneratorUrl = tokenGeneratorUrl,
                                    onPatInputChange = { viewModel.setPatInput(it) },
                                    onToggleShowToken = { showTokenVisible = !showTokenVisible },
                                    onSavePatToken = { viewModel.savePatToken() },
                                    onStartOAuth = {
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            isOAuthLoading = true
                                            try {
                                                val provider = OAuthProvider.newBuilder("github.com").apply {
                                                    scopes = listOf("repo", "read:user", "user:email")
                                                }
                                                FirebaseAuth.getInstance()
                                                    .startActivityForSignInWithProvider(activity, provider.build())
                                                    .addOnSuccessListener { authResult ->
                                                        isOAuthLoading = false
                                                        val credential = authResult.credential as? OAuthCredential
                                                        val token = credential?.accessToken
                                                        if (!token.isNullOrEmpty()) {
                                                            viewModel.setPatInput(token)
                                                            viewModel.savePatToken()
                                                            Toast.makeText(context, "GitHub OAuth sign in successful!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            val email = authResult.user?.email ?: authResult.user?.displayName ?: "GitHub User"
                                                            Toast.makeText(context, "Signed in as $email!", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isOAuthLoading = false
                                                        Toast.makeText(context, "OAuth failed: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                            } catch (e: Exception) {
                                                isOAuthLoading = false
                                                Toast.makeText(context, "OAuth setup: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Activity context required for web redirect", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSelectAccount = { acc -> viewModel.switchAccount(acc.token) },
                                    onRemoveAccount = { acc -> viewModel.removeAccount(acc.token) }
                                )
                            }
                        }

                        SubSettingsPage.AI_HUB -> {
                            item {
                                SubSettingsAiHubContent(
                                    selectedProvider = selectedProvider,
                                    selectedModel = selectedModel,
                                    customApiKey = customApiKey,
                                    customBaseUrl = customBaseUrl,
                                    showApiKeyVisible = showApiKeyVisible,
                                    isTestingAi = isTestingAi,
                                    aiTestStatus = aiTestStatus,
                                    onProviderChange = { provider ->
                                        selectedProvider = provider
                                        selectedModel = provider.defaultModel
                                    },
                                    onModelChange = { selectedModel = it },
                                    onApiKeyChange = { customApiKey = it },
                                    onBaseUrlChange = { customBaseUrl = it },
                                    onToggleShowApiKey = { showApiKeyVisible = !showApiKeyVisible },
                                    onTestConnection = {
                                        val testConfig = AiProviderConfig(selectedProvider, customApiKey, customBaseUrl, selectedModel)
                                        isTestingAi = true
                                        aiTestStatus = null
                                        scope.launch {
                                            val res = AiAssistantManager.testConnection(testConfig)
                                            isTestingAi = false
                                            aiTestStatus = res.getOrElse { "Connection Failed: ${it.message}" }
                                        }
                                    },
                                    onSaveConfig = {
                                        val newConfig = AiProviderConfig(selectedProvider, customApiKey.trim(), customBaseUrl.trim(), selectedModel.trim())
                                        AiAssistantManager.updateConfig(newConfig)
                                        Toast.makeText(context, "Saved AI Configuration!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        SubSettingsPage.APPEARANCE -> {
                            item {
                                SubSettingsAppearanceContent(
                                    currentThemeMode = currentThemeMode,
                                    currentPalette = currentPalette,
                                    onSelectThemeMode = { mode -> ThemeManager.setThemeMode(mode) },
                                    onSelectPalette = { palette -> ThemeManager.setPalette(palette) },
                                    viewModel = viewModel
                                )
                            }
                        }

                        SubSettingsPage.PRIVACY -> {
                            item {
                                SubSettingsPrivacyContent(
                                    onWipeTokens = {
                                        viewModel.setPatInput("")
                                        Toast.makeText(context, "Cleared temporary credential buffers!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        SubSettingsPage.STORAGE -> {
                            item {
                                SubSettingsStorageContent(
                                    onClearStaged = {
                                        viewModel.clearStagedFiles()
                                        Toast.makeText(context, "Cleared staged files cache!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        SubSettingsPage.REPOS_GIT, SubSettingsPage.BACKUP, SubSettingsPage.SYSTEM_UPDATE -> {
                            item {
                                SubSettingsGenericInfoContent(page = subPage)
                            }
                        }


                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroupCardItem(
    page: SubSettingsPage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_item_${page.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon Container matching Image 1
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GitDarkBg)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = page.title,
                        tint = GitAccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GitTextPrimary
                    )
                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = GitTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = GitTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ================= SUB-PAGE DETAILS IMPLEMENTATIONS =================

@Composable
fun SubSettingsAccountContent(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    patInput: String,
    showTokenVisible: Boolean,
    accountError: String?,
    isAddingAccount: Boolean,
    isOAuthLoading: Boolean,
    tokenGeneratorUrl: String,
    onPatInputChange: (String) -> Unit,
    onToggleShowToken: () -> Unit,
    onSavePatToken: () -> Unit,
    onStartOAuth: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit,
    onRemoveAccount: (AccountEntity) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active Profile Card
        if (selectedAccount != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GitCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitPrimaryGreen.copy(alpha = 0.5f))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    if (!selectedAccount.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = selectedAccount.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = GitPrimaryGreen,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAccount.displayName ?: selectedAccount.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GitTextPrimary
                        )
                        Text(
                            text = "@${selectedAccount.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GitAccentCyan
                        )
                    }

                    Surface(
                        color = GitPrimaryGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = GitPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // GitHub OAuth Sign In Button
        Button(
            onClick = onStartOAuth,
            colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isOAuthLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Sign In with GitHub OAuth", fontWeight = FontWeight.Bold)
        }

        // PAT Token Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Access Token (PAT)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GitTextPrimary
                    )

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Token Link", tokenGeneratorUrl))
                            Toast.makeText(context, "Copied GitHub Token Generator URL!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Token", style = MaterialTheme.typography.labelSmall, color = GitAccentCyan)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = patInput,
                    onValueChange = onPatInputChange,
                    placeholder = { Text("ghp_xxxx or github_pat_xxxx", color = GitTextSecondary) },
                    singleLine = true,
                    visualTransformation = if (showTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = onToggleShowToken) {
                            Icon(
                                imageVector = if (showTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = GitTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitPrimaryGreen,
                        unfocusedBorderColor = GitCardBorder,
                        focusedTextColor = GitTextPrimary,
                        unfocusedTextColor = GitTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (accountError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = accountError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onSavePatToken,
                    enabled = !isAddingAccount && patInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GitPrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAddingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save & Verify Token", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Account List
        if (accounts.isNotEmpty()) {
            Text(
                text = "Saved Accounts (${accounts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { acc ->
                    val isSelected = selectedAccount?.token == acc.token
                    Surface(
                        onClick = { onSelectAccount(acc) },
                        color = if (isSelected) GitPrimaryGreen.copy(alpha = 0.15f) else GitCardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) GitPrimaryGreen else GitCardBorder)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = GitTextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "@${acc.username}", style = MaterialTheme.typography.bodyMedium, color = GitTextPrimary, fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = { onRemoveAccount(acc) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubSettingsAiHubContent(
    selectedProvider: AiProvider,
    selectedModel: String,
    customApiKey: String,
    customBaseUrl: String,
    showApiKeyVisible: Boolean,
    isTestingAi: Boolean,
    aiTestStatus: String?,
    onProviderChange: (AiProvider) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onToggleShowApiKey: () -> Unit,
    onTestConnection: () -> Unit,
    onSaveConfig: () -> Unit
) {
    com.gitupload.ui.components.Material3ExpressiveAiHub(
        selectedProvider = selectedProvider,
        selectedModel = selectedModel,
        customApiKey = customApiKey,
        customBaseUrl = customBaseUrl,
        showApiKeyVisible = showApiKeyVisible,
        isTestingAi = isTestingAi,
        aiTestStatus = aiTestStatus,
        accentColor = GitAccentPurple,
        onProviderChange = onProviderChange,
        onModelChange = onModelChange,
        onApiKeyChange = onApiKeyChange,
        onBaseUrlChange = onBaseUrlChange,
        onToggleShowApiKey = onToggleShowApiKey,
        onTestConnection = onTestConnection,
        onSaveConfig = onSaveConfig
    )
}

@Composable
fun SubSettingsAppearanceContent(
    currentThemeMode: ThemeMode,
    currentPalette: AppThemePalette,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    viewModel: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Floating Navigation Rail Customization Card
        NavigationRailCustomizationCard(viewModel = viewModel)

        HorizontalDivider(color = GitCardBorder)

        // Theme Mode Section (Matching Image 2)
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GitTextPrimary
        )

        // 2x2 Grid matching Image 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeModeCard(
                    mode = ThemeMode.FOLLOW_SYSTEM,
                    icon = Icons.Default.SettingsSuggest,
                    selected = currentThemeMode == ThemeMode.FOLLOW_SYSTEM,
                    onClick = { onSelectThemeMode(ThemeMode.FOLLOW_SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeCard(
                    mode = ThemeMode.LIGHT,
                    icon = Icons.Default.WbSunny,
                    selected = currentThemeMode == ThemeMode.LIGHT,
                    onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeModeCard(
                    mode = ThemeMode.DARK,
                    icon = Icons.Default.NightsStay,
                    selected = currentThemeMode == ThemeMode.DARK,
                    onClick = { onSelectThemeMode(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeCard(
                    mode = ThemeMode.AMOLED,
                    icon = Icons.Default.Contrast,
                    selected = currentThemeMode == ThemeMode.AMOLED,
                    onClick = { onSelectThemeMode(ThemeMode.AMOLED) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Divider(color = GitCardBorder, thickness = 1.dp)

        // Accent Color Palettes
        Text(
            text = "Accent Color Palettes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GitTextPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemePalette.values().forEach { palette ->
                val isSelected = currentPalette == palette
                Surface(
                    onClick = { onSelectPalette(palette) },
                    color = if (isSelected) GitAccentPurple.copy(alpha = 0.15f) else GitCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) GitAccentPurple else GitCardBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.primary))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.accentCyan))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.accentPurple))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = palette.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = GitTextPrimary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = GitAccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubSettingsPrivacyContent(onWipeTokens: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GitPrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Local Room Database Encryption Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                }
                Text("All GitHub Personal Access Tokens and OAuth Bearer Tokens are encrypted in local Room SQLite storage.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Https, contentDescription = null, tint = GitAccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Strict HTTPS TLS Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                }
                Text("Enforced TLS 1.3 encryption on all GitHub API endpoints and AI provider servers.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
            }
        }

        Button(
            onClick = onWipeTokens,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Memory Credential Buffers")
        }
    }
}

@Composable
fun SubSettingsStorageContent(onClearStaged: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GitCardBg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Staged File Cache", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                Text("Local memory cache buffers staged directory scans prior to GitHub repository commits.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onClearStaged,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Staged Files Cache")
                }
            }
        }
    }
}

@Composable
fun SubSettingsGenericInfoContent(page: SubSettingsPage) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = page.icon, contentDescription = null, tint = GitAccentCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(page.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GitTextPrimary)
            }
            Text(page.subtitle, style = MaterialTheme.typography.bodyMedium, color = GitTextSecondary)
            Text("This sub-setting feature is active and managed automatically by GitUpload System Settings.", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
        }
    }
}

@Composable
fun ThemeModeCard(
    mode: ThemeMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) GitAccentPurple.copy(alpha = 0.15f) else GitCardBg,
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (selected) GitAccentCyan else GitCardBorder
            )
        ),
        modifier = modifier.height(96.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.displayName,
                tint = if (selected) GitAccentCyan else GitTextSecondary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) GitTextPrimary else GitTextSecondary
            )
        }
    }
}

@Composable
fun NavigationRailCustomizationCard(viewModel: MainViewModel) {
    val enabledSections by viewModel.enabledNavSections.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = GitCardBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ViewSidebar, contentDescription = null, tint = GitPrimaryGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Floating Navigation Rail Tabs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GitTextPrimary)
                        Text("Add or remove sections from the navigation bar", style = MaterialTheme.typography.bodySmall, color = GitTextSecondary)
                    }
                }

                TextButton(onClick = { viewModel.resetNavSections() }) {
                    Text("Reset Defaults", style = MaterialTheme.typography.labelSmall, color = GitAccentCyan)
                }
            }

            HorizontalDivider(color = GitCardBorder)

            val navSections = listOf(
                Triple(0, "Repositories", "Primary repo manager & code explorer"),
                Triple(1, "PC Upload", "Local directory tree & file uploader (Disabled by default)"),
                Triple(2, "Commit History", "Changelog and commit audit log"),
                Triple(3, "AI Assistant", "AI Chat & code assistant"),
                Triple(4, "Settings", "Account & app configuration")
            )

            navSections.forEach { (id, name, desc) ->
                val isChecked = enabledSections.contains(id)
                val isMandatory = id == 0 || id == 4

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = GitTextPrimary)
                            if (id == 1 && !isChecked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GitBadgePublic.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Accessible in Repos",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = GitBadgePublic,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = GitTextSecondary)
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { viewModel.toggleNavSection(id) },
                        enabled = !isMandatory,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GitPrimaryGreen
                        ),
                        modifier = Modifier.testTag("toggle_nav_section_$id")
                    )
                }
            }
        }
    }
}

