package com.gitupload.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val name: String?,
    val email: String?,
    @Json(name = "public_repos") val publicRepos: Int? = 0,
    @Json(name = "total_private_repos") val privateRepos: Int? = 0,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepoOwner(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepository(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val owner: GitHubRepoOwner,
    val private: Boolean,
    @Json(name = "html_url") val htmlUrl: String,
    val description: String?,
    val fork: Boolean = false,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    val language: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "size") val sizeKb: Long = 0
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String,
    val commit: BranchCommit
)

@JsonClass(generateAdapter = true)
data class BranchCommit(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long = 0,
    val url: String,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null,
    val type: String, // "file" or "dir"
    val content: String? = null, // base64 encoded for single file endpoint
    val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitUser(
    val name: String,
    val email: String,
    val date: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitDetail(
    val author: GitHubCommitUser,
    val message: String
)

@JsonClass(generateAdapter = true)
data class GitHubCommitItem(
    val sha: String,
    @Json(name = "html_url") val htmlUrl: String,
    val commit: GitHubCommitDetail
)

// Git Database API Data Structures (for multi-file atomic folder uploads)
@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class GitTreeEntry(
    val path: String,
    val mode: String = "100644", // 100644 for file, 100755 for executable
    val type: String = "blob",   // "blob" or "tree"
    val sha: String? = null,     // blob sha
    val content: String? = null  // direct text content (for small text files)
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    @Json(name = "base_tree") val baseTree: String?,
    val tree: List<GitTreeEntry>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
    val sha: String,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UpdateRefResponse(
    val ref: String,
    val objectSha: String? = null
)

@JsonClass(generateAdapter = true)
data class PutFileContentRequest(
    val message: String,
    val content: String, // base64 encoded
    val sha: String? = null, // required if updating existing file
    val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class PutFileContentResponse(
    val content: GitHubContentItem?,
    val commit: CreateCommitResponse?
)
