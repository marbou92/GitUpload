package com.gitupload.data.repository

import android.util.Base64
import com.gitupload.data.api.GitHubApiService
import com.gitupload.data.api.GitHubRetrofitClient
import com.gitupload.data.db.AccountDao
import com.gitupload.data.db.AccountEntity
import com.gitupload.data.db.BookmarkedRepoDao
import com.gitupload.data.db.BookmarkedRepoEntity
import com.gitupload.data.db.CachedFileTreeDao
import com.gitupload.data.db.CachedFileTreeEntity
import com.gitupload.data.db.UploadLogDao
import com.gitupload.data.db.UploadLogEntity
import com.gitupload.data.models.*
import com.gitupload.util.TokenCrypto

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GitUploadRepository(
    private val apiService: GitHubApiService = GitHubRetrofitClient.apiService,
    private val accountDao: AccountDao,
    private val uploadLogDao: UploadLogDao,
    private val bookmarkedRepoDao: BookmarkedRepoDao,
    private val cachedFileTreeDao: CachedFileTreeDao
) {


    val accountsFlow: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val selectedAccountFlow: Flow<AccountEntity?> = accountDao.getSelectedAccountFlow()
    val uploadLogsFlow: Flow<List<UploadLogEntity>> = uploadLogDao.getAllLogs()
    val bookmarkedReposFlow: Flow<List<BookmarkedRepoEntity>> = bookmarkedRepoDao.getAllBookmarks()

    private val _uploadProgress = MutableStateFlow(UploadProgress())
    val uploadProgress: StateFlow<UploadProgress> = _uploadProgress.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun saveAccountToken(token: String): Result<AccountEntity> = withContext(Dispatchers.IO) {
        try {
            val formattedToken = GitHubRetrofitClient.formatAuthToken(token)
            val response = apiService.getAuthenticatedUser(formattedToken)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                // Encrypt the token at rest with the Android Keystore before
                // persisting it. The stored value (and the Room primary key)
                // is the Base64 ciphertext; it is decrypted lazily via
                // [activeBearerToken] whenever a real Authorization header is
                // needed for a GitHub API call.
                val encryptedToken = TokenCrypto.encrypt(formattedToken)
                    ?: return@withContext Result.failure(
                        Exception("Failed to securely encrypt token (Android Keystore unavailable).")
                    )
                val entity = AccountEntity(
                    token = encryptedToken,
                    username = user.login,
                    displayName = user.name ?: user.login,
                    avatarUrl = user.avatarUrl,
                    email = user.email,
                    isSelected = true
                )
                accountDao.insertAccount(entity)
                accountDao.setSelectedAccount(encryptedToken)
                Result.success(entity)
            } else {
                Result.failure(Exception("Invalid Token or User fetch failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the decrypted Bearer token for the given (possibly null) account,
     * or null if there is no account or the stored ciphertext could not be
     * decrypted (e.g. legacy plaintext token from an older install, or a
     * Keystore key rotation). Callers should treat null as "not signed in".
     */
    private fun activeBearerToken(account: AccountEntity?): String? {
        if (account == null) return null
        return TokenCrypto.decrypt(account.token)
    }

    suspend fun switchAccount(token: String) = withContext(Dispatchers.IO) {
        accountDao.setSelectedAccount(token)
    }

    suspend fun removeAccount(token: String) = withContext(Dispatchers.IO) {
        accountDao.deleteAccount(token)
    }

    suspend fun fetchUserRepos(): Result<List<GitHubRepository>> = withContext(Dispatchers.IO) {
        try {
            val activeAccount = accountDao.getSelectedAccount()
            val bearer = activeBearerToken(activeAccount)
            if (bearer != null) {
                val response = apiService.getUserRepos(bearer)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch repos: ${response.code()}"))
                }
            } else {
                // No decryptable token on file (signed out, legacy plaintext,
                // or Keystore unavailable). Fall back to demo repositories so
                // the app remains explorable.
                Result.success(getDemoRepositories())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPublicRepos(query: String): Result<List<GitHubRepository>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchRepositories(query)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.items)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRepoBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        try {
            val account = accountDao.getSelectedAccount()
            val response = apiService.getRepoBranches(owner, repo, activeBearerToken(account))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Return an empty list instead of fabricating a bogus branch
                // with a fake SHA. The caller falls back to ["main"] for the
                // branch picker, and the real HEAD SHA is resolved at upload
                // time via the Git ref endpoint.
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun fetchRepoContents(
        owner: String,
        repo: String,
        path: String = "",
        ref: String? = null
    ): Result<List<GitHubContentItem>> = withContext(Dispatchers.IO) {
        val repoFullName = "$owner/$repo"
        val branch = ref ?: "main"
        val cacheKey = "$repoFullName:$branch:$path"

        try {
            val account = accountDao.getSelectedAccount()
            val response = apiService.getRepoContents(owner, repo, path, ref, activeBearerToken(account))
            if (response.isSuccessful && response.body() != null) {
                val rawBody = response.body()!!
                val items = parseContentsResponse(rawBody)

                // Save to Room Cache for offline browsing
                try {
                    val listType = Types.newParameterizedType(List::class.java, GitHubContentItem::class.java)
                    val adapter = moshi.adapter<List<GitHubContentItem>>(listType)
                    val json = adapter.toJson(items)
                    cachedFileTreeDao.cacheTree(
                        CachedFileTreeEntity(
                            id = cacheKey,
                            repoFullName = repoFullName,
                            branch = branch,
                            path = path,
                            itemsJson = json
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Result.success(items)
            } else {
                // Try retrieving cached tree from Room if network fails
                val cachedEntity = cachedFileTreeDao.getCachedTree(repoFullName, branch, path)
                if (cachedEntity != null) {
                    val listType = Types.newParameterizedType(List::class.java, GitHubContentItem::class.java)
                    val adapter = moshi.adapter<List<GitHubContentItem>>(listType)
                    val cachedItems = adapter.fromJson(cachedEntity.itemsJson)
                    if (cachedItems != null) {
                        return@withContext Result.success(cachedItems)
                    }
                }

                if (account == null) {
                    Result.success(getDemoFolderContents(path))
                } else {
                    Result.failure(Exception("Failed to fetch folder contents: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            // Offline Fallback to Room DB cache
            val cachedEntity = cachedFileTreeDao.getCachedTree(repoFullName, branch, path)
            if (cachedEntity != null) {
                try {
                    val listType = Types.newParameterizedType(List::class.java, GitHubContentItem::class.java)
                    val adapter = moshi.adapter<List<GitHubContentItem>>(listType)
                    val cachedItems = adapter.fromJson(cachedEntity.itemsJson)
                    if (cachedItems != null) {
                        return@withContext Result.success(cachedItems)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            Result.failure(e)
        }
    }


    suspend fun fetchSingleFileContent(
        owner: String,
        repo: String,
        path: String,
        ref: String? = null
    ): Result<GitHubContentItem> = withContext(Dispatchers.IO) {
        try {
            val account = accountDao.getSelectedAccount()
            val bearer = activeBearerToken(account)
            if (owner == "demo-developer" || bearer == null && (owner.contains("demo") || repo.contains("starter") || repo.contains("portfolio"))) {
                return@withContext Result.success(getFallbackFileContent(path))
            }
            val response = apiService.getRepoContents(owner, repo, path, ref, bearer)
            if (response.isSuccessful && response.body() != null) {
                val rawBody = response.body()!!
                val parsedItems = parseContentsResponse(rawBody)
                if (parsedItems.isNotEmpty()) {
                    Result.success(parsedItems.first())
                } else {
                    Result.success(getFallbackFileContent(path))
                }
            } else {
                Result.success(getFallbackFileContent(path))
            }
        } catch (e: Exception) {
            Result.success(getFallbackFileContent(path))
        }
    }

    suspend fun fetchRepoCommits(
        owner: String,
        repo: String,
        ref: String? = null
    ): Result<List<GitHubCommitItem>> = withContext(Dispatchers.IO) {
        try {
            val account = accountDao.getSelectedAccount()
            val response = apiService.getRepoCommits(owner, repo, ref, 20, activeBearerToken(account))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes atomic PC-like Folder & Multi-File Commit Upload to GitHub repository!
     */
    suspend fun executeFolderUpload(
        owner: String,
        repo: String,
        branch: String,
        targetSubfolder: String, // e.g. "" or "src/components/"
        commitMessage: String,
        stagedFiles: List<StagedFile>
    ): Result<UploadLogEntity> = withContext(Dispatchers.IO) {
        val selectedFiles = stagedFiles.filter { it.selected && it.contentBytes != null }
        if (selectedFiles.isEmpty()) {
            return@withContext Result.failure(Exception("No files selected for upload"))
        }

        val activeAccount = accountDao.getSelectedAccount()
        val token = activeBearerToken(activeAccount)
        if (token == null) {
            return@withContext Result.failure(Exception("GitHub Personal Access Token required for commits. Please add your PAT token in Accounts tab."))
        }
        val repoFullName = "$owner/$repo"
        val totalBytes = selectedFiles.sumOf { it.sizeBytes }

        try {
            _uploadProgress.value = UploadProgress(
                state = UploadStatusState.PREPARING,
                message = "Connecting to GitHub repository...",
                totalFiles = selectedFiles.size
            )

            // Step 1: Get branch HEAD commit SHA
            val refResponse = apiService.getBranchRef(owner, repo, branch, token)
            if (!refResponse.isSuccessful || refResponse.body() == null) {
                throw Exception("Could not find branch '$branch' ref in $repoFullName (${refResponse.code()})")
            }
            val parentCommitSha = refResponse.body()!!.gitObject.sha

            // Step 2: Create Git Blobs for all files
            _uploadProgress.value = _uploadProgress.value.copy(
                state = UploadStatusState.UPLOADING_BLOBS,
                message = "Creating Git Blobs for ${selectedFiles.size} files..."
            )

            val treeEntries = mutableListOf<GitTreeEntry>()

            for ((index, file) in selectedFiles.withIndex()) {
                val fileNum = index + 1
                val filePercent = (fileNum.toFloat() / selectedFiles.size) * 0.7f

                _uploadProgress.value = _uploadProgress.value.copy(
                    currentFileName = file.relativePath,
                    completedFiles = index,
                    progressPercent = filePercent,
                    message = "Uploading blob ($fileNum/${selectedFiles.size}): ${file.fileName}"
                )

                val base64Content = Base64.encodeToString(file.contentBytes, Base64.NO_WRAP)
                val blobResponse = apiService.createBlob(owner, repo, token, CreateBlobRequest(content = base64Content, encoding = "base64"))

                if (!blobResponse.isSuccessful || blobResponse.body() == null) {
                    throw Exception("Failed to create blob for ${file.relativePath}: ${blobResponse.code()}")
                }

                val blobSha = blobResponse.body()!!.sha

                // Calculate final repo relative path
                val finalPath = normalizeRepoPath(targetSubfolder, file.relativePath)

                treeEntries.add(
                    GitTreeEntry(
                        path = finalPath,
                        mode = "100644",
                        type = "blob",
                        sha = blobSha
                    )
                )
            }

            // Step 3: Create Git Tree
            _uploadProgress.value = _uploadProgress.value.copy(
                state = UploadStatusState.CREATING_TREE,
                progressPercent = 0.8f,
                message = "Building repository Git Tree node..."
            )

            val createTreeResponse = apiService.createTree(
                owner, repo, token,
                CreateTreeRequest(baseTree = parentCommitSha, tree = treeEntries)
            )

            if (!createTreeResponse.isSuccessful || createTreeResponse.body() == null) {
                throw Exception("Failed to build Git tree: ${createTreeResponse.code()}")
            }
            val newTreeSha = createTreeResponse.body()!!.sha

            // Step 4: Create Commit
            _uploadProgress.value = _uploadProgress.value.copy(
                state = UploadStatusState.CREATING_COMMIT,
                progressPercent = 0.9f,
                message = "Creating Git commit..."
            )

            val commitResp = apiService.createCommit(
                owner, repo, token,
                CreateCommitRequest(
                    message = commitMessage.ifBlank { "Upload ${selectedFiles.size} files via GitUpload App" },
                    tree = newTreeSha,
                    parents = listOf(parentCommitSha)
                )
            )

            if (!commitResp.isSuccessful || commitResp.body() == null) {
                throw Exception("Failed to create commit: ${commitResp.code()}")
            }
            val newCommitSha = commitResp.body()!!.sha
            val commitHtmlUrl = commitResp.body()!!.htmlUrl ?: "https://github.com/$owner/$repo/commit/$newCommitSha"

            // Step 5: Update Branch Reference
            _uploadProgress.value = _uploadProgress.value.copy(
                state = UploadStatusState.UPDATING_REF,
                progressPercent = 0.95f,
                message = "Updating branch reference '$branch'..."
            )

            val updateRefResp = apiService.updateRef(
                owner, repo, branch, token,
                UpdateRefRequest(sha = newCommitSha, force = false)
            )

            if (!updateRefResp.isSuccessful) {
                throw Exception("Failed to update branch head '$branch': ${updateRefResp.code()}")
            }

            val logEntity = UploadLogEntity(
                repoFullName = repoFullName,
                branch = branch,
                targetSubfolder = targetSubfolder,
                commitMessage = commitMessage,
                fileCount = selectedFiles.size,
                totalSizeBytes = totalBytes,
                commitSha = newCommitSha,
                commitHtmlUrl = commitHtmlUrl,
                isSuccess = true
            )

            uploadLogDao.insertLog(logEntity)

            _uploadProgress.value = UploadProgress(
                state = UploadStatusState.SUCCESS,
                completedFiles = selectedFiles.size,
                totalFiles = selectedFiles.size,
                progressPercent = 1.0f,
                message = "Successfully uploaded ${selectedFiles.size} files to $repoFullName!",
                commitSha = newCommitSha,
                commitHtmlUrl = commitHtmlUrl
            )

            Result.success(logEntity)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = e.message ?: "Upload encountered an error"
            _uploadProgress.value = UploadProgress(
                state = UploadStatusState.ERROR,
                errorMessage = errorMsg,
                message = "Upload failed: $errorMsg"
            )

            val logEntity = UploadLogEntity(
                repoFullName = repoFullName,
                branch = branch,
                targetSubfolder = targetSubfolder,
                commitMessage = commitMessage,
                fileCount = selectedFiles.size,
                totalSizeBytes = totalBytes,
                commitSha = null,
                commitHtmlUrl = null,
                isSuccess = false,
                errorMessage = errorMsg
            )
            uploadLogDao.insertLog(logEntity)

            Result.failure(e)
        }
    }

    fun resetUploadProgress() {
        _uploadProgress.value = UploadProgress()
    }

    suspend fun toggleBookmark(repo: GitHubRepository) = withContext(Dispatchers.IO) {
        val isBookmarked = bookmarkedRepoDao.isBookmarked(repo.fullName)
        if (isBookmarked) {
            bookmarkedRepoDao.deleteBookmark(repo.fullName)
        } else {
            bookmarkedRepoDao.insertBookmark(
                BookmarkedRepoEntity(
                    repoFullName = repo.fullName,
                    repoName = repo.name,
                    ownerLogin = repo.owner.login,
                    avatarUrl = repo.owner.avatarUrl,
                    description = repo.description,
                    isPrivate = repo.private,
                    stargazersCount = repo.stargazersCount,
                    defaultBranch = repo.defaultBranch
                )
            )
        }
    }

    private fun parseContentsResponse(rawBody: Any): List<GitHubContentItem> {
        return try {
            val jsonString = moshi.adapter(Any::class.java).toJson(rawBody)
            if (rawBody is List<*>) {
                val listType = Types.newParameterizedType(List::class.java, GitHubContentItem::class.java)
                val adapter = moshi.adapter<List<GitHubContentItem>>(listType)
                adapter.fromJson(jsonString) ?: emptyList()
            } else {
                val adapter = moshi.adapter(GitHubContentItem::class.java)
                val single = adapter.fromJson(jsonString)
                if (single != null) listOf(single) else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun normalizeRepoPath(subfolder: String, fileRelativePath: String): String {
        val cleanSubfolder = subfolder.trim().trim('/')
        val cleanFile = fileRelativePath.trim().trim('/')
        return if (cleanSubfolder.isEmpty()) {
            cleanFile
        } else {
            "$cleanSubfolder/$cleanFile"
        }
    }

    private fun getDemoRepositories(): List<GitHubRepository> {
        val owner = GitHubRepoOwner("demo-developer", "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png", "https://github.com/demo-developer")
        return listOf(
            GitHubRepository(
                id = 101,
                name = "android-portfolio-app",
                fullName = "demo-developer/android-portfolio-app",
                owner = owner,
                private = false,
                htmlUrl = "https://github.com/demo-developer/android-portfolio-app",
                description = "Modern Kotlin Jetpack Compose Showcase & UI Library",
                stargazersCount = 142,
                forksCount = 28,
                language = "Kotlin",
                defaultBranch = "main"
            ),
            GitHubRepository(
                id = 102,
                name = "fullstack-nextjs-starter",
                fullName = "demo-developer/fullstack-nextjs-starter",
                owner = owner,
                private = false,
                htmlUrl = "https://github.com/demo-developer/fullstack-nextjs-starter",
                description = "Production ready Next.js 15, Tailwind, Prisma and PostgreSQL template",
                stargazersCount = 89,
                forksCount = 14,
                language = "TypeScript",
                defaultBranch = "main"
            ),
            GitHubRepository(
                id = 103,
                name = "private-backend-microservice",
                fullName = "demo-developer/private-backend-microservice",
                owner = owner,
                private = true,
                htmlUrl = "https://github.com/demo-developer/private-backend-microservice",
                description = "High throughput Go microservice for event streaming and metrics",
                stargazersCount = 12,
                forksCount = 2,
                language = "Go",
                defaultBranch = "main"
            )
        )
    }

    private fun getDemoFolderContents(path: String): List<GitHubContentItem> {
        return if (path.isEmpty()) {
            listOf(
                GitHubContentItem("src", "src", "sha_src", 0, "", type = "dir"),
                GitHubContentItem("docs", "docs", "sha_docs", 0, "", type = "dir"),
                GitHubContentItem("README.md", "README.md", "sha_readme", 2048, "", type = "file"),
                GitHubContentItem("build.gradle.kts", "build.gradle.kts", "sha_build", 1420, "", type = "file"),
                GitHubContentItem(".gitignore", ".gitignore", "sha_ignore", 350, "", type = "file")
            )
        } else if (path == "src") {
            listOf(
                GitHubContentItem("main", "src/main", "sha_main", 0, "", type = "dir"),
                GitHubContentItem("App.kt", "src/App.kt", "sha_app", 1024, "", type = "file")
            )
        } else {
            listOf(
                GitHubContentItem("Sample.kt", "$path/Sample.kt", "sha_sample", 890, "", type = "file")
            )
        }
    }

    private fun getFallbackFileContent(path: String): GitHubContentItem {
        val fileName = if (path.contains('/')) path.substringAfterLast('/') else path
        val ext = if (fileName.contains('.')) fileName.substringAfterLast('.').lowercase() else ""

        val rawText = when {
            ext == "kt" || ext == "java" || fileName.endsWith(".kt") -> """
                package com.gitupload.app

                import androidx.compose.runtime.Composable
                import androidx.compose.material3.Text
                import androidx.compose.foundation.layout.Box
                import androidx.compose.ui.Modifier

                /**
                 * $fileName - Core Application Component
                 * Source preview powered by GitUpload Android
                 */
                @Composable
                fun MainComponent(modifier: Modifier = Modifier) {
                    Box(modifier = modifier) {
                        Text(text = "Hello from $fileName!")
                    }
                }
            """.trimIndent()
            ext == "md" -> """
                # ${fileName.substringBeforeLast('.')}
                
                Welcome to this repository project!

                ## Features
                - Kotlin Jetpack Compose Android Architecture
                - Git Tree & Subfolder Commit Upload Engine
                - Multi-Provider AI Assistant (Gemini, OpenRouter, OpenCode)

                ## Getting Started
                Build and run the project using Android Studio Ladybug or later.
            """.trimIndent()
            path.endsWith("gradle.kts") || ext == "gradle" -> """
                plugins {
                    alias(libs.plugins.android.application)
                    alias(libs.plugins.kotlin.compose)
                }

                android {
                    namespace = "com.gitupload.app"
                    compileSdk = 35
                }
            """.trimIndent()
            ext == "json" -> """
                {
                  "name": "$fileName",
                  "version": "1.0.0",
                  "private": true,
                  "dependencies": {
                    "react": "^18.2.0"
                  }
                }
            """.trimIndent()
            else -> """
                // $fileName
                // File Path: $path
                // Repository preview file
            """.trimIndent()
        }

        val base64Content = Base64.encodeToString(rawText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return GitHubContentItem(
            name = fileName,
            path = path,
            sha = "sha_fallback_${path.hashCode()}",
            size = rawText.length.toLong(),
            url = "",
            htmlUrl = "",
            downloadUrl = "",
            type = "file",
            content = base64Content,
            encoding = "base64"
        )
    }
}
