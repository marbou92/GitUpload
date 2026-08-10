package com.gitupload.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitupload.data.ai.AiAssistantManager
import com.gitupload.data.ai.AiProvider
import com.gitupload.data.ai.AiProviderConfig
import com.gitupload.ui.MainViewModel
import com.gitupload.ui.theme.*
import kotlinx.coroutines.launch

// Claude Accent Color Palette
val ClaudeOrange = Color(0xFFD97706)
val ClaudeWarmBg = Color(0xFF1E1B18)
val ClaudeCardBg = Color(0xFF2A2622)
val ClaudeBorder = Color(0xFF3D3730)
val ClaudeUserBubble = Color(0xFF38322B)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val providerName: String = "models.dev",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val aiConfig by AiAssistantManager.currentConfig.collectAsState()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "Hello! I am your AI Coding & Git Assistant. How can I help you analyze, upload, or generate code for your GitHub repositories today?",
                    isUser = false,
                    providerName = "${aiConfig.provider.displayName} • ${aiConfig.selectedModel}"
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showProviderModal by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val targetRepo by viewModel.targetRepo.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClaudeWarmBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header - Claude Aesthetic
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Claude Spark Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClaudeOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Claude Assistant",
                        tint = ClaudeOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Claude AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GitTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = ClaudeOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "models.dev",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = ClaudeOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (targetRepo != null) "Repo: ${targetRepo!!.name}" else "Git & Repository Specialist",
                        style = MaterialTheme.typography.labelSmall,
                        color = GitTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Model Selector Pill Button
                Surface(
                    onClick = { showProviderModal = true },
                    color = ClaudeCardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ClaudeBorder)),
                    modifier = Modifier.testTag("ai_model_selector_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GitPrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiConfig.selectedModel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GitTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = GitTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Clear Chat History Button
                IconButton(
                    onClick = {
                        messages = listOf(
                            ChatMessage(
                                text = "Chat reset. How can I help you today?",
                                isUser = false,
                                providerName = "${aiConfig.provider.displayName} • ${aiConfig.selectedModel}"
                            )
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = GitTextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(color = ClaudeBorder, thickness = 1.dp)

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message Thread
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(messages, key = { it.id }) { msg ->
                ClaudeChatBubble(msg = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested Prompts Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SuggestionChip(
                onClick = { inputText = "Draft a clean README.md with setup instructions for this repository" },
                label = { Text("Draft README.md", style = MaterialTheme.typography.labelSmall, color = GitTextPrimary) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ClaudeCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeBorder)
            )

            SuggestionChip(
                onClick = { inputText = "Generate Conventional Commit messages for staged file changes" },
                label = { Text("Suggest Commit Msg", style = MaterialTheme.typography.labelSmall, color = GitTextPrimary) },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ClaudeCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeBorder)
            )
        }


        Spacer(modifier = Modifier.height(8.dp))

        // Claude Bottom Input Field
        Surface(
            color = ClaudeCardBg,
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (inputText.isNotBlank()) ClaudeOrange else ClaudeBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Claude (${aiConfig.selectedModel})...", color = GitTextSecondary, fontSize = 14.sp) },
                    singleLine = false,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = GitTextPrimary,
                        unfocusedTextColor = GitTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )

                IconButton(
                    onClick = {
                        val prompt = inputText.trim()
                        if (prompt.isNotBlank() && !isSending) {
                            val activeModel = "${aiConfig.provider.displayName} • ${aiConfig.selectedModel}"
                            val userMsg = ChatMessage(text = prompt, isUser = true)
                            messages = messages + userMsg
                            inputText = ""
                            isSending = true

                            scope.launch {
                                val repoContext = targetRepo?.fullName ?: "None"
                                val reply = AiAssistantManager.askAssistant(prompt, contextInfo = "Target Repository: $repoContext")
                                messages = messages + ChatMessage(text = reply, isUser = false, providerName = activeModel)
                                isSending = false
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) ClaudeOrange else ClaudeBorder)
                        .testTag("send_ai_chat_btn")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Quick Provider Settings Dialog
        if (showProviderModal) {
            AiProviderSelectorDialog(
                currentConfig = aiConfig,
                onDismiss = { showProviderModal = false },
                onSave = { newConfig ->
                    AiAssistantManager.updateConfig(newConfig)
                    showProviderModal = false
                }
            )
        }
    }
}

@Composable
fun ClaudeChatBubble(msg: ChatMessage) {
    val context = LocalContext.current

    Row(
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ClaudeOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ClaudeOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (msg.isUser) ClaudeUserBubble else ClaudeCardBg,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ClaudeBorder)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (msg.isUser) "You" else msg.providerName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (msg.isUser) GitAccentCyan else ClaudeOrange
                    )

                    if (!msg.isUser) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Claude Response", msg.text))
                                Toast.makeText(context, "Copied response to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = GitTextSecondary, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Render Code Blocks vs Markdown Text cleanly
                if (msg.text.contains("```")) {
                    val parts = msg.text.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 1) {
                            // Code snippet block with dark header
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GitDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ClaudeBorder)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CODE SNIPPET", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = GitTextSecondary)
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = GitAccentCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = part.trim(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = GitAccentCyan
                                    )
                                }
                            }
                        } else if (part.isNotBlank()) {
                            Text(
                                text = part.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = GitTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = GitTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AiProviderSelectorDialog(
    currentConfig: AiProviderConfig,
    onDismiss: () -> Unit,
    onSave: (AiProviderConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var selectedModel by remember { mutableStateOf(currentConfig.selectedModel) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var isShowKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ClaudeOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Model & Engine Hub",
                    color = GitTextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                com.gitupload.ui.components.Material3ExpressiveAiHub(
                    selectedProvider = selectedProvider,
                    selectedModel = selectedModel,
                    customApiKey = apiKey,
                    customBaseUrl = baseUrl,
                    showApiKeyVisible = isShowKey,
                    isTestingAi = isTesting,
                    aiTestStatus = testStatus,
                    accentColor = ClaudeOrange,
                    onProviderChange = { selectedProvider = it },
                    onModelChange = { selectedModel = it },
                    onApiKeyChange = { apiKey = it },
                    onBaseUrlChange = { baseUrl = it },
                    onToggleShowApiKey = { isShowKey = !isShowKey },
                    onTestConnection = {
                        val testConfig = AiProviderConfig(selectedProvider, apiKey.trim(), baseUrl.trim(), selectedModel.trim())
                        isTesting = true
                        testStatus = "Testing connection to ${selectedProvider.displayName}..."
                        scope.launch {
                            val res = AiAssistantManager.testConnection(testConfig)
                            isTesting = false
                            res.fold(
                                onSuccess = { msg ->
                                    testStatus = msg
                                    Toast.makeText(context, "Connection successful!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    testStatus = "Connection Failed: ${err.message}"
                                    Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    onSaveConfig = {
                        onSave(
                            AiProviderConfig(
                                provider = selectedProvider,
                                selectedModel = selectedModel.trim(),
                                apiKey = apiKey.trim(),
                                baseUrl = baseUrl.trim()
                            )
                        )
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GitTextSecondary)
            }
        },
        containerColor = GitDarkBg
    )
}

