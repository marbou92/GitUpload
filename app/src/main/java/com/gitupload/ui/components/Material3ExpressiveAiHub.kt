package com.gitupload.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitupload.data.ai.AiAssistantManager
import com.gitupload.data.ai.AiProvider
import com.gitupload.data.ai.DynamicProviderInfo
import com.gitupload.data.ai.PiModelInfo
import com.gitupload.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Material3ExpressiveAiHub(
    selectedProvider: AiProvider,
    selectedModel: String,
    customApiKey: String,
    customBaseUrl: String,
    showApiKeyVisible: Boolean,
    isTestingAi: Boolean,
    aiTestStatus: String?,
    accentColor: Color = GitAccentPurple,
    onProviderChange: (AiProvider) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onToggleShowApiKey: () -> Unit,
    onTestConnection: () -> Unit,
    onSaveConfig: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val catalogModels by AiAssistantManager.catalogModels.collectAsState()
    val dynamicProviders by AiAssistantManager.dynamicProviders.collectAsState()

    var isFetchingCatalog by remember { mutableStateOf(false) }
    var activeProviderId by remember { mutableStateOf("google") }
    var modelSearchQuery by remember { mutableStateOf("") }
    var selectedCapabilityFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        if (catalogModels.isEmpty()) {
            isFetchingCatalog = true
            AiAssistantManager.fetchModelsCatalog()
            isFetchingCatalog = false
        }
    }

    // Filter models for active selected provider
    val modelsForActiveProvider = remember(catalogModels, activeProviderId, modelSearchQuery, selectedCapabilityFilter) {
        var list = catalogModels

        // 1. Filter by provider ID
        if (activeProviderId != "pi_dev" && activeProviderId != "openrouter" && activeProviderId != "custom") {
            list = list.filter {
                it.provider.equals(activeProviderId, ignoreCase = true) ||
                        it.id.startsWith("$activeProviderId/", ignoreCase = true) ||
                        it.id.substringBefore('/', "").equals(activeProviderId, ignoreCase = true) ||
                        (activeProviderId == "google" && (it.provider.contains("google") || it.id.contains("gemini", ignoreCase = true))) ||
                        (activeProviderId == "openai" && (it.provider.contains("openai") || it.id.contains("gpt", ignoreCase = true))) ||
                        (activeProviderId == "anthropic" && (it.provider.contains("anthropic") || it.id.contains("claude", ignoreCase = true))) ||
                        (activeProviderId == "meta" && (it.provider.contains("meta") || it.provider.contains("llama") || it.id.contains("llama", ignoreCase = true))) ||
                        (activeProviderId == "xai" && (it.provider.contains("xai") || it.provider.contains("grok") || it.id.contains("grok", ignoreCase = true)))
            }
        }

        // 2. Search query filter
        if (modelSearchQuery.isNotBlank()) {
            val q = modelSearchQuery.lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.id.lowercase().contains(q) ||
                        it.description.lowercase().contains(q)
            }
        }

        // 3. Capability filter
        when (selectedCapabilityFilter) {
            "Reasoning" -> list = list.filter { it.reasoning }
            "Open Weights" -> list = list.filter { it.openWeights }
            "100k+ Ctx" -> list = list.filter { it.contextLimit >= 100000 }
            "Tool Call" -> list = list.filter { it.toolCall }
        }

        list
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        // =========================================================
        // 1. EXPRESSIVE HEADER BANNER (models.dev Live Sync)
        // =========================================================
        Surface(
            color = accentColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.35f))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Hub",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "pi.dev Live AI Catalog",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GitTextPrimary
                            )
                        }
                        Text(
                            text = if (catalogModels.isNotEmpty()) "${catalogModels.size} Models • ${dynamicProviders.size} Providers active" else "Syncing live catalog from pi.dev...",
                            style = MaterialTheme.typography.labelMedium,
                            color = GitAccentCyan
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        isFetchingCatalog = true
                        scope.launch {
                            AiAssistantManager.fetchPiModelsCatalog()
                            isFetchingCatalog = false
                            Toast.makeText(context, "Refreshed catalog from pi.dev", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isFetchingCatalog,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    if (isFetchingCatalog) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Catalog",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // =========================================================
        // 2. STEP 1: PROVIDER ENGINES SELECTION (M3 Expressive Chips)
        // =========================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "1. AI Engine Provider (${dynamicProviders.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )

            // Horizontal Provider Filter Chips Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dynamicProviders, key = { it.id }) { prov ->
                    val isSelected = activeProviderId.equals(prov.id, ignoreCase = true)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeProviderId = prov.id
                            val matchingEnum = when (prov.id.lowercase()) {
                                "google" -> AiProvider.GEMINI
                                "models_dev" -> AiProvider.MODELS_DEV
                                "openrouter" -> AiProvider.OPENROUTER
                                "opencode" -> AiProvider.OPENCODE
                                "custom" -> AiProvider.CUSTOM_OPENAI
                                else -> AiProvider.CUSTOM_OPENAI
                            }
                            onProviderChange(matchingEnum)

                            // Preselect first model if available
                            val provModels = catalogModels.filter { it.provider.equals(prov.id, ignoreCase = true) }
                            if (provModels.isNotEmpty()) {
                                onModelChange(provModels.first().id)
                            }
                        },
                        label = {
                            Text(
                                text = prov.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        trailingIcon = {
                            Badge(
                                containerColor = if (isSelected) Color.White.copy(alpha = 0.3f) else GitCardBorder,
                                contentColor = if (isSelected) Color.White else GitTextSecondary
                            ) {
                                Text(prov.modelCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White,
                            containerColor = GitCardBg,
                            labelColor = GitTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = accentColor,
                            borderColor = GitCardBorder
                        )
                    )
                }
            }
        }

        // =========================================================
        // 3. STEP 2: MODEL CATALOGUE & SEARCH
        // =========================================================
        val activeProviderInfo = dynamicProviders.find { it.id.equals(activeProviderId, ignoreCase = true) }
        val providerTitle = activeProviderInfo?.displayName ?: activeProviderId.replaceFirstChar { it.uppercase() }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "2. Available Models for $providerTitle (${modelsForActiveProvider.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )

            // Search input field
            OutlinedTextField(
                value = modelSearchQuery,
                onValueChange = { modelSearchQuery = it },
                placeholder = { Text("Search models in $providerTitle...", color = GitTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GitTextSecondary) },
                trailingIcon = if (modelSearchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { modelSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = GitTextSecondary)
                        }
                    }
                } else null,
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GitCardBorder,
                    focusedContainerColor = GitCardBg,
                    unfocusedContainerColor = GitCardBg,
                    focusedTextColor = GitTextPrimary,
                    unfocusedTextColor = GitTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Capability filter chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val capabilityFilters = listOf("All", "Reasoning", "Open Weights", "100k+ Ctx", "Tool Call")
                capabilityFilters.forEach { cap ->
                    val isCapSelected = selectedCapabilityFilter == cap
                    FilterChip(
                        selected = isCapSelected,
                        onClick = { selectedCapabilityFilter = cap },
                        label = { Text(cap, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GitAccentCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = GitCardBg,
                            labelColor = GitTextSecondary
                        )
                    )
                }
            }

            // Model Cards Container
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (modelsForActiveProvider.isEmpty()) {
                    Surface(
                        color = GitCardBg,
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GitCardBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No models matching criteria for $providerTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = GitTextSecondary
                            )
                        }
                    }
                } else {
                    modelsForActiveProvider.take(100).forEach { model ->
                        val isSelected = selectedModel.equals(model.id, ignoreCase = true) || selectedModel.equals(model.name, ignoreCase = true)

                        Surface(
                            onClick = { onModelChange(model.id) },
                            color = if (isSelected) accentColor.copy(alpha = 0.16f) else GitCardBg,
                            shape = RoundedCornerShape(18.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) accentColor else GitCardBorder)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = model.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GitTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = model.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = GitAccentCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        color = if (isSelected) accentColor else GitCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "Active" else "Select",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else GitTextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (model.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GitTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    if (model.contextLimit > 0) {
                                        val ctxKb = model.contextLimit / 1000
                                        Surface(color = GitCardBorder, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "${ctxKb}k context",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = GitTextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (model.reasoning) {
                                        Surface(color = accentColor.copy(alpha = 0.25f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "Reasoning",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (model.openWeights) {
                                        Surface(color = GitPrimaryGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "Open Weights",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = GitPrimaryGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (model.toolCall) {
                                        Surface(color = GitAccentCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "Tool Call",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = GitAccentCyan,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        // 4. STEP 3: API CREDENTIALS & BASE ENDPOINT CONFIG
        // =========================================================
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "3. API Credentials & Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GitTextPrimary
            )

            // Active Model ID Field
            OutlinedTextField(
                value = selectedModel,
                onValueChange = onModelChange,
                label = { Text("Active Model Identifier") },
                placeholder = { Text("e.g. google/gemini-2.5-flash or claude-3-5-sonnet") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GitCardBorder,
                    focusedContainerColor = GitCardBg,
                    unfocusedContainerColor = GitCardBg,
                    focusedTextColor = GitTextPrimary,
                    unfocusedTextColor = GitTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // API Key Field
            OutlinedTextField(
                value = customApiKey,
                onValueChange = onApiKeyChange,
                label = { Text(if (selectedProvider == AiProvider.GEMINI_OAUTH) "OAuth Bearer Token" else "API Key (models.dev / Google / OpenAI / Anthropic)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (showApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleShowApiKey) {
                        Icon(
                            imageVector = if (showApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle API Key Visibility",
                            tint = GitTextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GitCardBorder,
                    focusedContainerColor = GitCardBg,
                    unfocusedContainerColor = GitCardBg,
                    focusedTextColor = GitTextPrimary,
                    unfocusedTextColor = GitTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Base URL Endpoint Field
            if (selectedProvider == AiProvider.OPENCODE || selectedProvider == AiProvider.CUSTOM_OPENAI || selectedProvider == AiProvider.MODELS_DEV || selectedProvider == AiProvider.OPENROUTER) {
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("API Base Endpoint URL") },
                    placeholder = { Text("https://api.models.dev/v1/chat/completions") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = GitCardBorder,
                        focusedContainerColor = GitCardBg,
                        unfocusedContainerColor = GitCardBg,
                        focusedTextColor = GitTextPrimary,
                        unfocusedTextColor = GitTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Dual Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GitAccentCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    if (isTestingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GitAccentCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Test Connection")
                }

                Button(
                    onClick = onSaveConfig,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            }

            // Test result banner
            if (!aiTestStatus.isNullOrBlank()) {
                val isSuccess = aiTestStatus.contains("successfully", true) || aiTestStatus.contains("OK", true)
                Surface(
                    color = if (isSuccess) GitPrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isSuccess) GitPrimaryGreen else MaterialTheme.colorScheme.error)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isSuccess) GitPrimaryGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiTestStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = GitTextPrimary
                        )
                    }
                }
            }
        }
    }
}
