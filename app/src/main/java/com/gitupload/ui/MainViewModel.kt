package com.gitupload.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitupload.data.ai.AiAssistantManager
import com.gitupload.data.db.AccountEntity
import com.gitupload.data.db.GitUploadDatabase
import com.gitupload.data.db.UploadLogEntity
import com.gitupload.data.models.*
import com.gitupload.data.repository.GitUploadRepository
import com.gitupload.data.scanner.FolderScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GitUploadDatabase.getDatabase(application)
    val repository = GitUploadRepository(
        accountDao = db.accountDao(),
        uploadLogDao = db.uploadLogDao(),
        bookmarkedRepoDao = db.bookmarkedRepoDao(),
        cachedFileTreeDao = db.cachedFileTreeDao()
    )

    val selectedAccount: StateFlow<AccountEntity?> = repository.selectedAccountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountEntity>> = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uploadLogs: StateFlow<List<UploadLogEntity>> = repository.uploadLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent Commits for Activity Dashboard
    private val _recentCommits = MutableStateFlow<List<GitHubCommitItem>>(emptyList())
    val recentCommits: StateFlow<List<GitHubCommitItem>> = _recentCommits.asStateFlow()

    private val _isCommitsLoading = MutableStateFlow(false)
    val isCommitsLoading: StateFlow<Boolean> = _isCommitsLoading.asStateFlow()


    // UI States
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Navigation Rail Customization State
    // Tab IDs: 0 = Repositories, 1 = PC Upload, 2 = History, 3 = AI Assistant, 4 = Settings
    // Default: Repos (0), History (2), AI Assistant (3), Settings (4) -- PC Upload (1) removed by default
    private val _enabledNavSections = MutableStateFlow<Set<Int>>(setOf(0, 2, 3, 4))
    val enabledNavSections: StateFlow<Set<Int>> = _enabledNavSections.asStateFlow()

    fun toggleNavSection(sectionId: Int) {
        val current = _enabledNavSections.value.toMutableSet()
        if (current.contains(sectionId)) {
            if (current.size > 1 && sectionId != 0) {
                current.remove(sectionId)
            }
        } else {
            current.add(sectionId)
        }
        _enabledNavSections.value = current
    }

    fun resetNavSections() {
        _enabledNavSections.value = setOf(0, 2, 3, 4)
    }

    fun navigateToUpload(repo: GitHubRepository? = null, subfolder: String = "") {
        if (repo != null) {
            setTargetRepo(repo)
        }
        if (subfolder.isNotEmpty()) {
            setTargetSubfolder(subfolder)
        }
        _activeTab.value = 1 // Switch to PC Upload screen
    }

    private val _userRepos = MutableStateFlow<List<GitHubRepository>>(emptyList())
    val userRepos: StateFlow<List<GitHubRepository>> = _userRepos.asStateFlow()

    private val _isReposLoading = MutableStateFlow(false)
    val isReposLoading: StateFlow<Boolean> = _isReposLoading.asStateFlow()

    private val _repoQuery = MutableStateFlow("")
    val repoQuery: StateFlow<String> = _repoQuery.asStateFlow()

    // Upload Staging State
    private val _stagedFiles = MutableStateFlow<List<StagedFile>>(emptyList())
    val stagedFiles: StateFlow<List<StagedFile>> = _stagedFiles.asStateFlow()

    private val _targetRepo = MutableStateFlow<GitHubRepository?>(null)
    val targetRepo: StateFlow<GitHubRepository?> = _targetRepo.asStateFlow()

    private val _targetBranch = MutableStateFlow("main")
    val targetBranch: StateFlow<String> = _targetBranch.asStateFlow()

    private val _availableBranches = MutableStateFlow<List<String>>(listOf("main", "master"))
    val availableBranches: StateFlow<List<String>> = _availableBranches.asStateFlow()

    private val _targetSubfolder = MutableStateFlow("")
    val targetSubfolder: StateFlow<String> = _targetSubfolder.asStateFlow()

    private val _commitMessage = MutableStateFlow("")
    val commitMessage: StateFlow<String> = _commitMessage.asStateFlow()

    private val _isGeneratingCommitMsg = MutableStateFlow(false)
    val isGeneratingCommitMsg: StateFlow<Boolean> = _isGeneratingCommitMsg.asStateFlow()

    private val _isScanningFolder = MutableStateFlow(false)
    val isScanningFolder: StateFlow<Boolean> = _isScanningFolder.asStateFlow()

    private val _scannedFileCount = MutableStateFlow(0)
    val scannedFileCount: StateFlow<Int> = _scannedFileCount.asStateFlow()

    val uploadProgress: StateFlow<UploadProgress> = repository.uploadProgress

    // Account token input
    private val _patInput = MutableStateFlow("")
    val patInput: StateFlow<String> = _patInput.asStateFlow()

    private val _accountError = MutableStateFlow<String?>(null)
    val accountError: StateFlow<String?> = _accountError.asStateFlow()

    private val _isAddingAccount = MutableStateFlow(false)
    val isAddingAccount: StateFlow<Boolean> = _isAddingAccount.asStateFlow()

    // Explorer State
    private val _selectedRepoForExplorer = MutableStateFlow<GitHubRepository?>(null)
    val selectedRepoForExplorer: StateFlow<GitHubRepository?> = _selectedRepoForExplorer.asStateFlow()

    private val _explorerCurrentPath = MutableStateFlow("")
    val explorerCurrentPath: StateFlow<String> = _explorerCurrentPath.asStateFlow()

    private val _explorerBranch = MutableStateFlow("main")
    val explorerBranch: StateFlow<String> = _explorerBranch.asStateFlow()

    private val _explorerContents = MutableStateFlow<List<GitHubContentItem>>(emptyList())
    val explorerContents: StateFlow<List<GitHubContentItem>> = _explorerContents.asStateFlow()

    private val _isExplorerLoading = MutableStateFlow(false)
    val isExplorerLoading: StateFlow<Boolean> = _isExplorerLoading.asStateFlow()

    private val _selectedFileForView = MutableStateFlow<GitHubContentItem?>(null)
    val selectedFileForView: StateFlow<GitHubContentItem?> = _selectedFileForView.asStateFlow()

    private val _decodedFileContent = MutableStateFlow<String?>(null)
    val decodedFileContent: StateFlow<String?> = _decodedFileContent.asStateFlow()

    private val _isFileContentLoading = MutableStateFlow(false)
    val isFileContentLoading: StateFlow<Boolean> = _isFileContentLoading.asStateFlow()

    private val _aiFileSummary = MutableStateFlow<String?>(null)
    val aiFileSummary: StateFlow<String?> = _aiFileSummary.asStateFlow()

    init {
        loadUserRepos()

        // Reload repos when account changes
        viewModelScope.launch {
            selectedAccount.collect {
                loadUserRepos()
            }
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun setRepoQuery(query: String) {
        _repoQuery.value = query
    }

    fun setPatInput(input: String) {
        _patInput.value = input
        _accountError.value = null
    }

    fun loadUserRepos() {
        viewModelScope.launch {
            _isReposLoading.value = true
            val result = repository.fetchUserRepos()
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                _userRepos.value = list
                if (_targetRepo.value == null && list.isNotEmpty()) {
                    setTargetRepo(list.first())
                }
            }
            _isReposLoading.value = false
        }
    }

    fun savePatToken() {
        val token = _patInput.value.trim()
        if (token.isEmpty()) {
            _accountError.value = "Please enter a valid Personal Access Token (PAT)"
            return
        }

        viewModelScope.launch {
            _isAddingAccount.value = true
            _accountError.value = null
            val result = repository.saveAccountToken(token)
            if (result.isSuccess) {
                _patInput.value = ""
                _accountError.value = null
                loadUserRepos()
            } else {
                _accountError.value = result.exceptionOrNull()?.message ?: "Failed to verify token"
            }
            _isAddingAccount.value = false
        }
    }

    fun switchAccount(token: String) {
        viewModelScope.launch {
            repository.switchAccount(token)
            loadUserRepos()
        }
    }

    fun removeAccount(token: String) {
        viewModelScope.launch {
            repository.removeAccount(token)
            loadUserRepos()
        }
    }

    /** Signs out of every stored GitHub account and wipes their tokens. */
    fun signOutAllAccounts() {
        viewModelScope.launch {
            repository.deleteAllAccounts()
            // Clear any in-memory target repo / staged state that referenced
            // the now-removed account so the UI doesn't try to commit against
            // a stale selection.
            _targetRepo.value = null
            _availableBranches.value = listOf("main", "master")
            loadUserRepos()
        }
    }

    /** Deletes a single upload-history entry. */
    fun deleteUploadLog(id: Long) {
        viewModelScope.launch {
            repository.deleteUploadLog(id)
        }
    }

    /** Clears every upload-history entry. */
    fun clearAllUploadLogs() {
        viewModelScope.launch {
            repository.clearAllUploadLogs()
        }
    }

    /** Clears the offline file-tree cache for all repositories. */
    fun clearFileTreeCache() {
        viewModelScope.launch {
            repository.clearFileTreeCache()
        }
    }

    fun setTargetRepo(repo: GitHubRepository) {
        _targetRepo.value = repo
        _targetBranch.value = repo.defaultBranch
        loadBranchesForRepo(repo.owner.login, repo.name)
    }

    fun setTargetBranch(branch: String) {
        _targetBranch.value = branch
    }

    fun setTargetSubfolder(subfolder: String) {
        _targetSubfolder.value = subfolder
    }

    fun setCommitMessage(msg: String) {
        _commitMessage.value = msg
    }

    private fun loadBranchesForRepo(owner: String, repo: String) {
        viewModelScope.launch {
            val res = repository.fetchRepoBranches(owner, repo)
            if (res.isSuccess) {
                val branches = res.getOrNull()?.map { it.name } ?: listOf("main")
                _availableBranches.value = if (branches.isEmpty()) listOf("main") else branches
                if (!branches.contains(_targetBranch.value) && branches.isNotEmpty()) {
                    _targetBranch.value = branches.first()
                }
            }
        }
    }

    /**
     * Hard cap on the number of files that can be staged at once. Each
     * staged file holds its full bytes in memory until the upload completes,
     * so an unbounded list will OOM on large directory trees. 500 files * an
     * average ~50 KB source file is ~25 MB of resident memory, well within
     * budget on API 24+ devices.
     */
    private val maxStagedFiles = 500

    // Staging Actions
    fun scanFolderUri(uri: Uri) {
        viewModelScope.launch {
            _isScanningFolder.value = true
            _scannedFileCount.value = 0

            val scanned = FolderScanner.scanFolderUri(
                context = getApplication(),
                treeUri = uri,
                onProgress = { count, _ ->
                    _scannedFileCount.value = count
                }
            )

            // Merge with current staged files, avoiding duplicates and
            // respecting the total staged-files cap.
            val current = _stagedFiles.value.toMutableList()
            for (f in scanned) {
                if (current.size >= maxStagedFiles) break
                if (!current.any { it.relativePath == f.relativePath }) {
                    current.add(f)
                }
            }
            _stagedFiles.value = current
            _isScanningFolder.value = false

            // Auto generate commit message if empty
            if (_commitMessage.value.isEmpty() && current.isNotEmpty()) {
                generateAiCommitMessage()
            }
        }
    }

    fun scanMultipleFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _isScanningFolder.value = true
            val scanned = FolderScanner.scanMultipleFiles(getApplication(), uris)
            val current = _stagedFiles.value.toMutableList()
            for (f in scanned) {
                if (current.size >= maxStagedFiles) break
                if (!current.any { it.relativePath == f.relativePath }) {
                    current.add(f)
                }
            }
            _stagedFiles.value = current
            _isScanningFolder.value = false

            if (_commitMessage.value.isEmpty() && current.isNotEmpty()) {
                generateAiCommitMessage()
            }
        }
    }

    fun addRawTextFile(fileName: String, relativePath: String, content: String) {
        val file = FolderScanner.createRawTextStagedFile(fileName, relativePath, content)
        val current = _stagedFiles.value.toMutableList()
        current.add(file)
        _stagedFiles.value = current

        if (_commitMessage.value.isEmpty()) {
            _commitMessage.value = "feat: add $fileName"
        }
    }

    fun removeStagedFile(id: String) {
        _stagedFiles.value = _stagedFiles.value.filter { it.id != id }
    }

    fun toggleStagedFileSelected(id: String) {
        _stagedFiles.value = _stagedFiles.value.map {
            if (it.id == id) it.copy(selected = !it.selected) else it
        }
    }

    fun toggleAllStagedFilesSelected(selected: Boolean) {
        _stagedFiles.value = _stagedFiles.value.map { it.copy(selected = selected) }
    }

    fun clearStagedFiles() {
        _stagedFiles.value = emptyList()
        _commitMessage.value = ""
        repository.resetUploadProgress()
    }

    fun generateAiCommitMessage() {
        viewModelScope.launch {
            _isGeneratingCommitMsg.value = true
            val msg = AiAssistantManager.generateCommitMessage(_stagedFiles.value)
            _commitMessage.value = msg
            _isGeneratingCommitMsg.value = false
        }
    }

    fun executeUpload() {
        val repo = _targetRepo.value ?: return
        val branch = _targetBranch.value
        val subfolder = _targetSubfolder.value
        val msg = _commitMessage.value
        val files = _stagedFiles.value

        viewModelScope.launch {
            repository.executeFolderUpload(
                owner = repo.owner.login,
                repo = repo.name,
                branch = branch,
                targetSubfolder = subfolder,
                commitMessage = msg,
                stagedFiles = files
            )
        }
    }

    fun resetUploadProgress() {
        repository.resetUploadProgress()
    }

    // Repository Explorer Actions
    fun openRepoInExplorer(repo: GitHubRepository, path: String = "") {
        _selectedRepoForExplorer.value = repo
        _explorerBranch.value = repo.defaultBranch
        _explorerCurrentPath.value = path
        _selectedFileForView.value = null
        _decodedFileContent.value = null
        _activeTab.value = 0 // Repositories Tab
        loadExplorerContents()
        loadRepoCommits(repo.owner.login, repo.name, repo.defaultBranch)
    }

    fun closeExplorerRepo() {
        _selectedRepoForExplorer.value = null
        _explorerCurrentPath.value = ""
        _selectedFileForView.value = null
        _decodedFileContent.value = null
    }

    fun navigateExplorerSubfolder(subfolderPath: String) {
        _explorerCurrentPath.value = subfolderPath
        _selectedFileForView.value = null
        _decodedFileContent.value = null
        loadExplorerContents()
    }

    fun navigateExplorerUp() {
        val current = _explorerCurrentPath.value
        if (current.isEmpty()) return
        val parent = current.substringBeforeLast('/', "")
        _explorerCurrentPath.value = parent
        _selectedFileForView.value = null
        _decodedFileContent.value = null
        loadExplorerContents()
    }

    fun changeExplorerBranch(branch: String) {
        _explorerBranch.value = branch
        loadExplorerContents()
        val repo = _selectedRepoForExplorer.value
        if (repo != null) {
            loadRepoCommits(repo.owner.login, repo.name, branch)
        }
    }

    fun loadRepoCommits(owner: String, repo: String, branch: String? = null) {
        viewModelScope.launch {
            _isCommitsLoading.value = true
            val res = repository.fetchRepoCommits(owner, repo, branch)
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                _recentCommits.value = list.take(5)
            } else {
                _recentCommits.value = emptyList()
            }
            _isCommitsLoading.value = false
        }
    }


    fun loadExplorerContents() {
        val repo = _selectedRepoForExplorer.value ?: return
        val path = _explorerCurrentPath.value
        val branch = _explorerBranch.value

        viewModelScope.launch {
            _isExplorerLoading.value = true
            val res = repository.fetchRepoContents(repo.owner.login, repo.name, path, branch)
            if (res.isSuccess) {
                _explorerContents.value = res.getOrNull() ?: emptyList()
            } else {
                _explorerContents.value = emptyList()
            }
            _isExplorerLoading.value = false
        }
    }

    fun closeFileView() {
        _selectedFileForView.value = null
        _decodedFileContent.value = null
        _aiFileSummary.value = null
        _isFileContentLoading.value = false
    }

    fun selectFileForView(item: GitHubContentItem) {
        _selectedFileForView.value = item
        _decodedFileContent.value = null
        _aiFileSummary.value = null
        val repo = _selectedRepoForExplorer.value

        viewModelScope.launch {
            _isFileContentLoading.value = true
            val owner = repo?.owner?.login ?: "demo-developer"
            val repoName = repo?.name ?: "android-portfolio-app"
            val res = repository.fetchSingleFileContent(owner, repoName, item.path, _explorerBranch.value)
            if (res.isSuccess) {
                val contentItem = res.getOrNull()
                val rawContent = contentItem?.content
                val encoding = contentItem?.encoding

                if (rawContent != null) {
                    val decoded = if (encoding == "base64") {
                        try {
                            val cleanBase64 = rawContent.replace("\n", "").replace("\r", "").trim()
                            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                            String(bytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            "Binary or non-UTF8 content"
                        }
                    } else {
                        rawContent
                    }
                    _decodedFileContent.value = decoded
                } else {
                    _decodedFileContent.value = "Content preview unavailable for binary or large files."
                }
            } else {
                // Surface the repository's actual failure reason (HTTP code,
                // parse error, network exception, etc.) instead of a generic
                // string, so the user knows whether it was a 404, a rate
                // limit, or a connectivity issue.
                val reason = res.exceptionOrNull()?.message ?: "Unknown error"
                _decodedFileContent.value = "Failed to load file contents: $reason"
            }
            _isFileContentLoading.value = false
        }
    }

    fun generateAiFileSummary() {
        val item = _selectedFileForView.value ?: return
        val content = _decodedFileContent.value ?: return

        viewModelScope.launch {
            val summary = AiAssistantManager.explainCodeOrFile(item.name, content)
            _aiFileSummary.value = summary
        }
    }

    fun prepareUploadForExplorerFolder(repo: GitHubRepository, targetPath: String) {
        setTargetRepo(repo)
        setTargetSubfolder(targetPath)
        _activeTab.value = 1 // Switch to PC Upload tab!
    }
}
