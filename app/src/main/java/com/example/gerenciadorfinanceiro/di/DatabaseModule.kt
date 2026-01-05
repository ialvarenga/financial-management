package com.example.gerenciadorfinanceiro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gerenciadorfinanceiro.data.backup.BackupFileService
import com.example.gerenciadorfinanceiro.data.backup.GsonConfig
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add recurrenceId column to transactions table
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceId INTEGER DEFAULT NULL")

            // Add recurrenceId column to credit_card_items table
            db.execSQL("ALTER TABLE credit_card_items ADD COLUMN recurrenceId INTEGER DEFAULT NULL")

            // Create indexes for the new columns
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_recurrenceId ON transactions(recurrenceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_items_recurrenceId ON credit_card_items(recurrenceId)")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add isPlaceholder column to credit_cards table
            db.execSQL("ALTER TABLE credit_cards ADD COLUMN isPlaceholder INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "financial_app.db"
        )
        .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
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

    @Provides
    fun provideProcessedNotificationDao(database: AppDatabase) = database.processedNotificationDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonConfig.createGson()

    @Provides
    @Singleton
    fun provideBackupFileService(
        @ApplicationContext context: Context,
        gson: Gson
    ): BackupFileService = BackupFileService(context, gson)
}

