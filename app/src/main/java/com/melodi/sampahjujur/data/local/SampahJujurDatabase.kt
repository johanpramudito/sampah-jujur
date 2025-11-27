package com.melodi.sampahjujur.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.melodi.sampahjujur.data.local.converter.TransactionConverters
import com.melodi.sampahjujur.data.local.dao.TransactionDao
import com.melodi.sampahjujur.data.local.dao.UserDao
import com.melodi.sampahjujur.data.local.dao.WasteItemDao
import com.melodi.sampahjujur.data.local.entity.TransactionEntity
import com.melodi.sampahjujur.data.local.entity.UserEntity
import com.melodi.sampahjujur.data.local.entity.WasteItemEntity

/**
 * Room Database for Sampah Jujur application.
 * Provides local caching and offline-first capabilities for:
 * - Draft waste items (offline creation)
 * - User profiles (faster startup)
 * - Transaction history (offline viewing, complex queries)
 *
 * Database version: 1
 * Entities: WasteItemEntity, UserEntity, TransactionEntity
 */
@Database(
    entities = [
        WasteItemEntity::class,
        UserEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TransactionConverters::class)
abstract class SampahJujurDatabase : RoomDatabase() {

    /**
     * Provides access to WasteItem operations
     */
    abstract fun wasteItemDao(): WasteItemDao

    /**
     * Provides access to User operations
     */
    abstract fun userDao(): UserDao

    /**
     * Provides access to Transaction operations
     */
    abstract fun transactionDao(): TransactionDao

    companion object {
        /**
         * Database name
         */
        const val DATABASE_NAME = "sampah_jujur_db"
    }
}
