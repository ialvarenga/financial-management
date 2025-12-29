package com.example.gerenciadorfinanceiro.di

import android.content.Context
import androidx.room.Room
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "financial_app.db"
        )
        .fallbackToDestructiveMigration()  // For development - will use proper migrations in production
        .build()
    }

    @Provides
    fun provideAccountDao(database: AppDatabase) = database.accountDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase) = database.transactionDao()

    @Provides
    fun provideCreditCardDao(database: AppDatabase) = database.creditCardDao()

    @Provides
    fun provideCreditCardBillDao(database: AppDatabase) = database.creditCardBillDao()

    @Provides
    fun provideCreditCardItemDao(database: AppDatabase) = database.creditCardItemDao()

    @Provides
    fun provideRecurrenceDao(database: AppDatabase) = database.recurrenceDao()

    @Provides
    fun provideTransferDao(database: AppDatabase) = database.transferDao()
}

