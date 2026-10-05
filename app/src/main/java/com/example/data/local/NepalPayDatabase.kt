package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        BankDirectoryEntity::class,
        LinkedBankAccountEntity::class,
        ReconciliationLogEntity::class,
        ApiErrorLogEntity::class,
        TransactionEntity::class,
        MoneyRequestEntity::class,
        KycSubmissionEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NepalPayDatabase : RoomDatabase() {
    abstract fun nepalPayDao(): NepalPayDao

    companion object {
        @Volatile
        private var INSTANCE: NepalPayDatabase? = null

        fun getDatabase(context: Context): NepalPayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NepalPayDatabase::class.java,
                    "nepalpay_sandbox_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
