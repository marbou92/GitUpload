package com.gitupload.data.ai

import com.gitupload.BuildConfig
import com.gitupload.data.models.StagedFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class AiProvider(val displayName: String, val defaultModel: String, val description: String) {
    MODELS_DEV("models.dev", "claude-3-5-sonnet", "models.dev API gateway for Claude, DeepSeek & Gemini models"),
    GEMINI("Google Gemini API", "gemini-2.5-flash", "Direct API key auth via Google Generative AI"),
    GEMINI_OAUTH("Gemini OAuth 2.0", "gemini-2.5-flash", "Google OAuth Bearer Token (generative-language scope)"),
    OPENROUTER("OpenRouter AI", "anthropic/claude-3.5-sonnet", "Unified API key for Claude, Llama & DeepSeek models"),
    OPENCODE("OpenCode Server", "gpt-4o-mini", "Self-hosted OpenCode AI endpoint (e.g. http://localhost:4096)"),
    CUSTOM_OPENAI("Custom OpenAI API", "gpt-4o-mini", "OpenAI compatible endpoint with custom base URL & key")
}


data class PiModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val family: String = "",
    val description: String = "",
    val contextLimit: Int = 0,
    val reasoning: Boolean = false,
    val toolCall: Boolean = false,
    val openWeights: Boolean = false
)

data class DynamicProviderInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val modelCount: Int,
    val isCustom: Boolean = false
)

data class AiProviderConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1/chat/completions",
    val selectedModel: String = AiProvider.GEMINI.defaultModel,
    val selectedProviderId: String = "google"
)


object AiAssistantManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val _currentConfig = MutableStateFlow(AiProviderConfig())
    val currentConfig: StateFlow<AiProviderConfig> = _currentConfig.asStateFlow()

    private val _catalogModels = MutableStateFlow<List<PiModelInfo>>(emptyList())
    val catalogModels: StateFlow<List<PiModelInfo>> = _catalogModels.asStateFlow()

    private val _dynamicProviders = MutableStateFlow<List<DynamicProviderInfo>>(emptyList())
    val dynamicProviders: StateFlow<List<DynamicProviderInfo>> = _dynamicProviders.asStateFlow()

    fun updateConfig(config: AiProviderConfig) {
        _currentConfig.value = config
    }

    private fun getActiveApiKey(): String {
        val config = _currentConfig.value
        if (config.apiKey.isNotBlank()) return config.apiKey.trim()
        
        // Fallback to BuildConfig GEMINI_API_KEY for default Gemini provider
        if (config.provider == AiProvider.GEMINI) {
            return try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") "" else key
            } catch (e: Throwable) {
                ""
            }
        }
        return ""
    }

    suspend fun generateCommitMessage(stagedFiles: List<StagedFile>): String = withContext(Dispatchers.IO) {
        val filePaths = stagedFiles.filter { it.selected }.joinToString("\n") { "- ${it.relativePath} (${it.formattedSize})" }
        if (filePaths.isBlank()) return@withContext "Update repository files"

        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() && _currentConfig.value.provider != AiProvider.GEMINI_OAUTH) {
            val mainFolder = stagedFiles.firstOrNull()?.relativePath?.substringBefore('/', "")
            return@withContext if (!mainFolder.isNullOrBlank()) {
                "feat: add $mainFolder files (${stagedFiles.size} items)"
            } else {
                "feat: upload ${stagedFiles.size} project files"
            }
        }

        val prompt = """
            You are a Git commit message generator. Generate a concise, clear Conventional Commit message for uploading these files:
            $filePaths
            
            Rules:
            1. Respond ONLY with the single-line commit message (e.g. "feat(components): add Header and Footer components").
            2. Do not enclose in quotes or markdown.
            3. Maximum 72 characters.
        """.trimIndent()

        try {
            val reply = queryAiProvider(prompt)
            reply.lines().firstOrNull { it.isNotBlank() }?.trim('"', '`') ?: "feat: upload ${stagedFiles.size} files"
        } catch (e: Exception) {
            "feat: upload ${stagedFiles.size} files"
        }
    }

    suspend fun explainCodeOrFile(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isEmpty() && _currentConfig.value.provider != AiProvider.GEMINI_OAUTH) {
            return@withContext "File: $fileName (${content.length} characters)\nPreview:\n" + content.take(300)
        }

        val prompt = """
            Explain the following repository file in 3 concise bullet points:
            File Name: $fileName
            Content snippet:
            ${content.take(1500)}
        """.trimIndent()

        try {
            queryAiProvider(prompt)
        } catch (e: Exception) {
            "Could not generate AI explanation: ${e.message}"
        }
    }

    suspend fun askAssistant(userMessage: String, contextInfo: String = ""): String = withContext(Dispatchers.IO) {
        val config = _currentConfig.value
        val apiKey = getActiveApiKey()

        if (apiKey.isEmpty() && config.provider != AiProvider.GEMINI_OAUTH) {
            return@withContext "API Key for ${config.provider.displayName} is not configured. Tap the AI Model Selector in the top bar to set your API Key."
        }

        val prompt = """
            You are GitUpload AI Assistant (${config.provider.displayName} - ${config.selectedModel}), an expert on Git, GitHub repos, folder uploads, and code structures.
            Current Context: $contextInfo
            
            User Question: $userMessage
        """.trimIndent()

        try {
            queryAiProvider(prompt)
        } catch (e: Exception) {
            "Error from ${config.provider.displayName}: ${e.message}"
        }
    }

    suspend fun fetchPiModelsCatalog(): Result<List<PiModelInfo>> = withContext(Dispatchers.IO) {
        val endpoint = "https://pi.dev/models"
        
        try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "GitUpload-Android/2026.1 (pi.dev catalog integration)")
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful && body.isNotBlank()) {
                    val parsedList = mutableListOf<PiModelInfo>()
                    val rowRegex = """<tr[^>]*data-model-row="true"[^>]*>""".toRegex()
                    val rows = rowRegex.findAll(body)
                    
                    for (rowMatch in rows) {
                        val rowHtml = rowMatch.value
                        val mIdMatch = """data-model-id="([^"]+)"""".toRegex().find(rowHtml)
                        val mNameMatch = """data-model-name="([^"]+)"""".toRegex().find(rowHtml)
                        val mProvMatch = """data-model-provider="([^"]+)"""".toRegex().find(rowHtml)
                        
                        if (mIdMatch != null) {
                            val modelId = mIdMatch.groupValues[1]
                            val rawName = mNameMatch?.groupValues[1] ?: modelId.substringAfter('/')
                            val rawProvider = mProvMatch?.groupValues[1] ?: modelId.substringBefore('/', "pi_dev")
                            
                            val cleanName = rawName.split(' ')
                                .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                            
                            parsedList.add(
                                PiModelInfo(
                                    id = modelId,
                                    name = cleanName,
                                    provider = rawProvider.lowercase(),
                                    family = rawProvider,
                                    description = "pi.dev model ($rawProvider)",
                                    contextLimit = if (modelId.contains("claude") || modelId.contains("gemini") || modelId.contains("gpt-4")) 200000 else 128000,
                                    reasoning = modelId.contains("r1") || modelId.contains("o1") || modelId.contains("o3") || modelId.contains("reason"),
                                    toolCall = modelId.contains("claude") || modelId.contains("gpt") || modelId.contains("gemini"),
                                    openWeights = modelId.contains("llama") || modelId.contains("deepseek") || modelId.contains("mistral") || modelId.contains("qwen")
                                )
                            )
                        }
                    }

                    if (parsedList.isNotEmpty()) {
                        _catalogModels.value = parsedList
                        buildDynamicProviders(parsedList)
                        return@withContext Result.success(parsedList)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback if offline
        }

        val fallbackList = getFallbackModelsList()
        _catalogModels.value = fallbackList
        buildDynamicProviders(fallbackList)
        Result.success(fallbackList)
    }

    suspend fun fetchModelsCatalog(): Result<List<PiModelInfo>> = fetchPiModelsCatalog()

    private fun buildDynamicProviders(models: List<PiModelInfo>) {
        val providerGroups = models.groupBy { it.provider }
        val dynamicList = mutableListOf<DynamicProviderInfo>()

        fun formatName(provId: String): String = when (provId.lowercase()) {
            "google", "google-vertex" -> "Google Gemini"
            "openai", "azure-openai-responses" -> "OpenAI"
            "anthropic" -> "Anthropic Claude"
            "deepseek" -> "DeepSeek AI"
            "mistral" -> "Mistral AI"
            "meta", "llama" -> "Meta Llama"
            "cohere" -> "Cohere"
            "xai", "grok" -> "xAI Grok"
            "nvidia" -> "NVIDIA AI"
            "amazon-bedrock" -> "Amazon Bedrock"
            "cloudflare-workers-ai", "cloudflare-ai-gateway" -> "Cloudflare AI"
            "github-copilot" -> "GitHub Copilot"
            "groq" -> "Groq"
            "huggingface" -> "Hugging Face"
            "fireworks" -> "Fireworks AI"
            "together" -> "Together AI"
            "cerebras" -> "Cerebras"
            "baseten" -> "Baseten"
            "minimax", "minimax-cn" -> "MiniMax"
            "moonshotai", "kimi-coding" -> "Moonshot Kimi"
            "zhipuai", "glm" -> "Zhipu AI"
            "alibaba", "qwen" -> "Alibaba Qwen"
            "perplexity" -> "Perplexity AI"
            "pi_dev", "pi.dev" -> "pi.dev Catalog"
            "openrouter" -> "OpenRouter AI"
            "opencode" -> "OpenCode Server"
            "custom" -> "Custom Endpoint"
            else -> provId.split("-", "_", ".").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        // 1. All-in-one Catalog Gateway
        dynamicList.add(DynamicProviderInfo("pi_dev", "pi.dev Catalog", "Unified catalog with ${models.size} models across ${providerGroups.size} providers", models.size))

        // 2. Primary Featured AI Engines
        val primaryOrder = listOf("anthropic", "google", "openai", "deepseek", "mistral", "amazon-bedrock", "groq", "meta", "xai", "cloudflare-workers-ai", "huggingface", "fireworks", "together", "cohere", "nvidia")
        
        primaryOrder.forEach { pId ->
            val count = providerGroups[pId]?.size ?: 0
            if (count > 0 || pId in listOf("google", "openai", "anthropic", "deepseek")) {
                dynamicList.add(
                    DynamicProviderInfo(
                        id = pId,
                        displayName = formatName(pId),
                        description = if (count > 0) "$count models available" else "Featured AI Engine",
                        modelCount = if (count > 0) count else 15
                    )
                )
            }
        }

        // 3. Dynamically discovered providers from pi.dev
        providerGroups.forEach { (provId, provModels) ->
            if (provId !in primaryOrder && provId !in listOf("pi_dev", "openrouter", "opencode", "custom") && provId.isNotBlank()) {
                dynamicList.add(
                    DynamicProviderInfo(
                        id = provId,
                        displayName = formatName(provId),
                        description = "${provModels.size} models active",
                        modelCount = provModels.size
                    )
                )
            }
        }

        // 4. Custom & Server Gateways
        dynamicList.add(DynamicProviderInfo("openrouter", "OpenRouter AI", "Unified OpenRouter API endpoint", models.size))
        dynamicList.add(DynamicProviderInfo("opencode", "OpenCode Server", "Self-hosted local/remote OpenCode endpoint", 10))
        dynamicList.add(DynamicProviderInfo("custom", "Custom Endpoint", "OpenAI-compatible custom base URL & key", 1, isCustom = true))

        _dynamicProviders.value = dynamicList
    }

    private fun getFallbackModelsList(): List<PiModelInfo> {
        return listOf(
            PiModelInfo("google/gemini-2.5-flash", "Gemini 2.5 Flash", "google", "gemini", "Fast multimodal reasoning model", 1000000, reasoning = true, toolCall = true),
            PiModelInfo("google/gemini-2.5-pro", "Gemini 2.5 Pro", "google", "gemini", "Flagship multimodal reasoning model", 2000000, reasoning = true, toolCall = true),
            PiModelInfo("openai/gpt-4.1", "GPT-4.1", "openai", "gpt", "High performance workhorse for coding", 1047576, toolCall = true),
            PiModelInfo("openai/o4-mini", "o4-mini", "openai", "o-mini", "Compact fast reasoning model", 200000, reasoning = true, toolCall = true),
            PiModelInfo("anthropic/claude-3.7-sonnet", "Claude 3.7 Sonnet", "anthropic", "claude", "State of the art coding assistant", 200000, reasoning = true, toolCall = true),
            PiModelInfo("deepseek/deepseek-r1", "DeepSeek R1", "deepseek", "deepseek", "Open weights reasoning model", 128000, reasoning = true, openWeights = true),
            PiModelInfo("mistral/mistral-large-2411", "Mistral Large 2", "mistral", "mistral", "Flagship open weights coding model", 128000, openWeights = true, toolCall = true),
            PiModelInfo("meta/llama-3.3-70b-instruct", "Llama 3.3 70B", "meta", "llama", "Highly efficient open weights model", 128000, openWeights = true),
            PiModelInfo("xai/grok-2-1212", "Grok 2", "xai", "grok", "Frontier AI model from xAI", 128000, toolCall = true)
        )
    }

    suspend fun fetchRemoteModels(provider: AiProvider, apiKey: String, baseUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        // Fetch live catalog directly from models.dev
        val catalogRes = fetchModelsCatalog()
        if (catalogRes.isSuccess) {
            val allModels = catalogRes.getOrNull() ?: emptyList()
            val filtered = when (provider) {
                AiProvider.MODELS_DEV -> allModels.map { it.id }
                AiProvider.GEMINI, AiProvider.GEMINI_OAUTH -> allModels.filter { it.provider == "google" || it.id.contains("gemini") }.map { it.id }
                AiProvider.OPENROUTER -> allModels.map { it.id }
                AiProvider.OPENCODE -> allModels.filter { it.provider == "openai" || it.provider == "anthropic" }.map { it.id }
                AiProvider.CUSTOM_OPENAI -> allModels.map { it.id }
            }
            if (filtered.isNotEmpty()) {
                return@withContext Result.success(filtered.distinct())
            }
        }

        try {
            val endpoint = when (provider) {
                AiProvider.MODELS_DEV -> "https://models.dev/models.json"
                AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/models"
                AiProvider.GEMINI, AiProvider.GEMINI_OAUTH -> return@withContext Result.success(listOf("google/gemini-2.5-flash", "google/gemini-2.5-pro"))
                else -> {
                    val base = if (baseUrl.isBlank()) "https://api.openai.com/v1" else baseUrl.trimEnd('/')
                    if (base.endsWith("/models")) base else "$base/models"
                }
            }

            val requestBuilder = Request.Builder().url(endpoint)
            if (apiKey.isNotBlank()) {
                val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
                requestBuilder.header("Authorization", authHeader)
            }

            client.newCall(requestBuilder.build()).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) return@use
                val modelList = mutableListOf<String>()
                val parsedJson = try { moshi.adapter(Any::class.java).fromJson(body) } catch (e: Exception) { null }

                if (parsedJson is Map<*, *>) {
                    val dataList = parsedJson["data"] as? List<*>
                    if (dataList != null) {
                        for (item in dataList) {
                            if (item is Map<*, *>) {
                                val id = item["id"] as? String
                                if (!id.isNullOrBlank()) modelList.add(id)
                            } else if (item is String) {
                                modelList.add(item)
                            }
                        }
                    } else {
                        for ((key, value) in parsedJson) {
                            if (key is String && !key.startsWith("$")) modelList.add(key)
                        }
                    }
                }

                if (modelList.isNotEmpty()) {
                    return@withContext Result.success(modelList.distinct())
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        Result.success(listOf("google/gemini-2.5-flash", "openai/gpt-4.1", "anthropic/claude-3.7-sonnet", "deepseek/deepseek-r1"))
    }

    suspend fun testConnection(config: AiProviderConfig): Result<String> = withContext(Dispatchers.IO) {
        val prevConfig = _currentConfig.value
        try {
            _currentConfig.value = config
            val response = queryAiProvider("Hello! Reply with 'OK' if working.")
            _currentConfig.value = config
            Result.success("Connected successfully to ${config.provider.displayName} (${config.selectedModel})! Response: $response")
        } catch (e: Exception) {
            _currentConfig.value = prevConfig
            Result.failure(e)
        }
    }

    private fun queryAiProvider(prompt: String): String {
        val config = _currentConfig.value
        val apiKey = getActiveApiKey()

        return when (config.provider) {
            AiProvider.MODELS_DEV -> {
                val url = if (config.baseUrl.isBlank() || config.baseUrl.contains("openai.com")) "https://api.models.dev/v1/chat/completions" else config.baseUrl
                callOpenAiCompatibleApi(apiKey, url, config.selectedModel, prompt)
            }
            AiProvider.GEMINI -> callGeminiApi(apiKey, config.selectedModel, prompt)
            AiProvider.GEMINI_OAUTH -> callGeminiOAuthApi(apiKey, config.selectedModel, prompt)
            AiProvider.OPENROUTER -> callOpenRouterApi(apiKey, config.selectedModel, prompt)
            AiProvider.OPENCODE -> {
                val url = if (config.baseUrl.isBlank()) "http://localhost:4096/v1/chat/completions" else config.baseUrl
                callOpenAiCompatibleApi(apiKey, url, config.selectedModel, prompt)
            }
            AiProvider.CUSTOM_OPENAI -> {
                val url = if (config.baseUrl.isBlank()) "https://api.openai.com/v1/chat/completions" else config.baseUrl
                callOpenAiCompatibleApi(apiKey, url, config.selectedModel, prompt)
            }
        }
    }


    private fun callGeminiApi(apiKey: String, model: String, prompt: String): String {
        val cleanModel = model.removePrefix("google/")
        // Pass the API key via the x-goog-api-key header rather than as a
        // URL query parameter so it never leaks into logs, proxies or
        // crash-report URL captures.
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val payload = """
            {
              "contents": [{"parts": [{"text": ${escapeJson(prompt)}}]}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Gemini API Error ${resp.code}: $body")
            return parseGeminiResponse(body)
        }
    }

    private fun callGeminiOAuthApi(oauthToken: String, model: String, prompt: String): String {
        val cleanModel = model.removePrefix("google/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val payload = """
            {
              "contents": [{"parts": [{"text": ${escapeJson(prompt)}}]}]
            }
        """.trimIndent()

        val tokenHeader = if (oauthToken.startsWith("Bearer ", ignoreCase = true)) oauthToken else "Bearer $oauthToken"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", tokenHeader)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Gemini OAuth Error ${resp.code}: $body")
            return parseGeminiResponse(body)
        }
    }

    private fun callOpenRouterApi(apiKey: String, model: String, prompt: String): String {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val payload = """
            {
              "model": ${escapeJson(model)},
              "messages": [
                {"role": "system", "content": "You are a Git and software engineering assistant."},
                {"role": "user", "content": ${escapeJson(prompt)}}
              ]
            }
        """.trimIndent()

        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .header("HTTP-Referer", "https://github.com/marbou92/GitUpload")
            .header("X-Title", "GitUpload Android App")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("OpenRouter Error ${resp.code}: $body")
            return parseOpenAiResponse(body)
        }
    }

    private fun callOpenAiCompatibleApi(apiKey: String, baseUrl: String, model: String, prompt: String): String {
        val endpoint = if (baseUrl.isBlank()) "https://api.openai.com/v1/chat/completions" else baseUrl
        val payload = """
            {
              "model": ${escapeJson(model)},
              "messages": [
                {"role": "system", "content": "You are an expert Git & Kotlin Android assistant."},
                {"role": "user", "content": ${escapeJson(prompt)}}
              ]
            }
        """.trimIndent()

        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", authHeader)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("OpenCode/OpenAI Error ${resp.code}: $body")
            return parseOpenAiResponse(body)
        }
    }

    private fun escapeJson(text: String): String {
        return moshi.adapter(String::class.java).toJson(text)
    }

    private fun parseGeminiResponse(json: String): String {
        return try {
            val root = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *> ?: return ""
            val candidates = root["candidates"] as? List<*> ?: return ""
            val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return ""
            val content = firstCandidate["content"] as? Map<*, *> ?: return ""
            val parts = content["parts"] as? List<*> ?: return ""
            val firstPart = parts.firstOrNull() as? Map<*, *> ?: return ""
            (firstPart["text"] as? String) ?: ""
        } catch (e: Exception) {
            "Response parsing error"
        }
    }

    private fun parseOpenAiResponse(json: String): String {
        return try {
            val root = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *> ?: return ""
            val choices = root["choices"] as? List<*> ?: return ""
            val firstChoice = choices.firstOrNull() as? Map<*, *> ?: return ""
            val message = firstChoice["message"] as? Map<*, *> ?: return ""
            (message["content"] as? String) ?: ""
        } catch (e: Exception) {
            "Response parsing error"
        }
    }
}
