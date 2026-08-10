package com.gitupload.data.api

import com.gitupload.data.models.*
import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") token: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") token: String,
        @Query("type") type: String = "all",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): Response<List<GitHubRepository>>

    @GET("users/{username}/repos")
    suspend fun getPublicReposForUser(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Response<List<GitHubRepository>>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30
    ): Response<SearchRepoResponse>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepoDetails(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String? = null
    ): Response<GitHubRepository>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getRepoBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String? = null
    ): Response<List<GitHubBranch>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepoContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String = "",
        @Query("ref") ref: String? = null,
        @Header("Authorization") token: String? = null
    ): Response<Any> // Returns List<GitHubContentItem> or single GitHubContentItem

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") ref: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Header("Authorization") token: String? = null
    ): Response<List<GitHubCommitItem>>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Header("Authorization") token: String,
        @Body request: PutFileContentRequest
    ): Response<PutFileContentResponse>

    // Git Database API (for atomic multi-file / folder commit trees)

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Header("Authorization") token: String
    ): Response<GitRefResponse>

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String,
        @Body request: CreateBlobRequest
    ): Response<CreateBlobResponse>

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String,
        @Body request: CreateTreeRequest
    ): Response<CreateTreeResponse>

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String,
        @Body request: CreateCommitRequest
    ): Response<CreateCommitResponse>

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Header("Authorization") token: String,
        @Body request: UpdateRefRequest
    ): Response<UpdateRefResponse>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String,
        @Body request: CreateRefRequest
    ): Response<GitRefResponse>
}

data class SearchRepoResponse(
    val items: List<GitHubRepository> = emptyList()
)

data class GitRefResponse(
    val ref: String,
    @Json(name = "object") val gitObject: GitRefObject
)

data class GitRefObject(
    val sha: String,
    val type: String,
    val url: String
)

data class CreateRefRequest(
    val ref: String, // e.g. "refs/heads/feature-branch"
    val sha: String  // parent commit sha
)
