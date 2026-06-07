package com.aifinance.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_ins` (
                `id` TEXT NOT NULL,
                `savingsGoalId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `amount` TEXT NOT NULL,
                `note` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_savingsGoalId` ON `check_ins` (`savingsGoalId`)")
    }
}
