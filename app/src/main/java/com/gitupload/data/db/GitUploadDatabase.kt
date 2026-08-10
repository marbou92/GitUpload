package com.gitupload.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM github_accounts ORDER BY addedAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM github_accounts WHERE isSelected = 1 LIMIT 1")
    fun getSelectedAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM github_accounts WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("UPDATE github_accounts SET isSelected = 0")
    suspend fun clearSelectedAccounts()

    @Transaction
    suspend fun setSelectedAccount(token: String) {
        clearSelectedAccounts()
        accountSelectedByToken(token)
    }

    @Query("UPDATE github_accounts SET isSelected = 1 WHERE token = :token")
    suspend fun accountSelectedByToken(token: String)

    @Query("DELETE FROM github_accounts WHERE token = :token")
    suspend fun deleteAccount(token: String)

    @Query("DELETE FROM github_accounts")
    suspend fun deleteAllAccounts()
}

@Dao
interface UploadLogDao {
    @Query("SELECT * FROM upload_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UploadLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UploadLogEntity)

    @Query("DELETE FROM upload_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM upload_logs")
    suspend fun clearAllLogs()
}

@Dao
interface BookmarkedRepoDao {
    @Query("SELECT * FROM bookmarked_repos ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedRepoEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_repos WHERE repoFullName = :repoFullName)")
    suspend fun isBookmarked(repoFullName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedRepoEntity)

    @Query("DELETE FROM bookmarked_repos WHERE repoFullName = :repoFullName")
    suspend fun deleteBookmark(repoFullName: String)
}

@Dao
interface CachedFileTreeDao {
    @Query("SELECT * FROM cached_file_trees WHERE repoFullName = :repoFullName AND branch = :branch AND path = :path LIMIT 1")
    suspend fun getCachedTree(repoFullName: String, branch: String, path: String): CachedFileTreeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheTree(tree: CachedFileTreeEntity)

    @Query("DELETE FROM cached_file_trees WHERE repoFullName = :repoFullName")
    suspend fun clearCacheForRepo(repoFullName: String)

    @Query("DELETE FROM cached_file_trees")
    suspend fun clearAllCache()
}

@Database(
    entities = [AccountEntity::class, UploadLogEntity::class, BookmarkedRepoEntity::class, CachedFileTreeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GitUploadDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun uploadLogDao(): UploadLogDao
    abstract fun bookmarkedRepoDao(): BookmarkedRepoDao
    abstract fun cachedFileTreeDao(): CachedFileTreeDao


    companion object {
        @Volatile
        private var INSTANCE: GitUploadDatabase? = null

        fun getDatabase(context: android.content.Context): GitUploadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GitUploadDatabase::class.java,
                    "gitupload_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
