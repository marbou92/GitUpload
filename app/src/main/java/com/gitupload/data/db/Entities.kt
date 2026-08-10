package com.gitupload.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "github_accounts")
data class AccountEntity(
    @PrimaryKey val token: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val email: String?,
    val isSelected: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "upload_logs")
data class UploadLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoFullName: String,
    val branch: String,
    val targetSubfolder: String,
    val commitMessage: String,
    val fileCount: Int,
    val totalSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val commitSha: String?,
    val commitHtmlUrl: String?,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

@Entity(tableName = "bookmarked_repos")
data class BookmarkedRepoEntity(
    @PrimaryKey val repoFullName: String, // e.g. "owner/reponame"
    val repoName: String,
    val ownerLogin: String,
    val avatarUrl: String?,
    val description: String?,
    val isPrivate: Boolean,
    val stargazersCount: Int,
    val defaultBranch: String,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_file_trees")
data class CachedFileTreeEntity(
    @PrimaryKey val id: String, // format: "repoFullName:branch:path"
    val repoFullName: String,
    val branch: String,
    val path: String,
    val itemsJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

