package com.melodi.sampahjujur.di

import android.content.Context
import androidx.room.Room
import com.melodi.sampahjujur.data.local.SampahJujurDatabase
import com.melodi.sampahjujur.data.local.dao.PickupRequestDao
import com.melodi.sampahjujur.data.local.dao.TransactionDao
import com.melodi.sampahjujur.data.local.dao.UserDao
import com.melodi.sampahjujur.data.local.dao.WasteItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Room Database and DAO instances.
 * All database-related dependencies are scoped as Singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the SampahJujurDatabase instance.
     * Database is created as a singleton and persists throughout the app lifecycle.
     *
     * @param context Application context
     * @return SampahJujurDatabase instance
     */
    @Provides
    @Singleton
    fun provideSampahJujurDatabase(
        @ApplicationContext context: Context
    ): SampahJujurDatabase {
        return Room.databaseBuilder(
            context,
            SampahJujurDatabase::class.java,
            SampahJujurDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    /**
     * Provides WasteItemDao for waste item operations
     *
     * @param database SampahJujurDatabase instance
     * @return WasteItemDao
     */
    @Provides
    @Singleton
    fun provideWasteItemDao(database: SampahJujurDatabase): WasteItemDao {
        return database.wasteItemDao()
    }

    /**
     * Provides UserDao for user profile operations
     *
     * @param database SampahJujurDatabase instance
     * @return UserDao
     */
    @Provides
    @Singleton
    fun provideUserDao(database: SampahJujurDatabase): UserDao {
        return database.userDao()
    }

    /**
     * Provides TransactionDao for transaction operations
     *
     * @param database SampahJujurDatabase instance
     * @return TransactionDao
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: SampahJujurDatabase): TransactionDao {
        return database.transactionDao()
    }

    /**
     * Provides PickupRequestDao for pickup request operations
     *
     * @param database SampahJujurDatabase instance
     * @return PickupRequestDao
     */
    @Provides
    @Singleton
    fun providePickupRequestDao(database: SampahJujurDatabase): PickupRequestDao {
        return database.pickupRequestDao()
    }
}
