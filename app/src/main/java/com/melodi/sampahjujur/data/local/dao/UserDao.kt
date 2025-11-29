package com.melodi.sampahjujur.data.local.dao

import androidx.room.*
import com.melodi.sampahjujur.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User operations.
 * Provides methods for caching and retrieving user profile data locally.
 */
@Dao
interface UserDao {

    /**
     * Insert or update a user in the cache
     * @param user The user entity to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    /**
     * Get a user by ID
     * @param userId The user ID
     * @return The cached user or null if not found
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Observe a user by ID (reactive)
     * @param userId The user ID
     * @return Flow of user entity (null if not found)
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: String): Flow<UserEntity?>

    /**
     * Update user profile information
     * @param user The user entity to update
     */
    @Update
    suspend fun update(user: UserEntity)

    /**
     * Delete a user from cache
     * @param user The user to delete
     */
    @Delete
    suspend fun delete(user: UserEntity)

    /**
     * Delete a user by ID
     * @param userId The user ID to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String): Int

    /**
     * Clear all cached users (useful for logout)
     * @return Number of rows deleted
     */
    @Query("DELETE FROM users")
    suspend fun clearAll(): Int

    /**
     * Get all cached users
     * @return List of all cached users
     */
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    /**
     * Check if a user is cached locally
     * @param userId The user ID
     * @return True if user exists in cache, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)")
    suspend fun isUserCached(userId: String): Boolean

    /**
     * Get the timestamp of last sync for a user
     * @param userId The user ID
     * @return Last sync timestamp or null if user not found
     */
    @Query("SELECT lastSyncedAt FROM users WHERE id = :userId")
    suspend fun getLastSyncTime(userId: String): Long?
}
