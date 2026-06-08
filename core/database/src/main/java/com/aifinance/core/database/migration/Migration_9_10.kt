package com.aifinance.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Fix: createdAt was TEXT in some installed versions, should be INTEGER.
        // SQLite doesn't support ALTER COLUMN, so recreate the table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_ins_new` (
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
        db.execSQL(
            """
            INSERT INTO `check_ins_new` (`id`, `savingsGoalId`, `date`, `amount`, `note`, `createdAt`)
            SELECT `id`, `savingsGoalId`, `date`, `amount`, `note`,
                   CASE
                       WHEN typeof(`createdAt`) = 'integer' THEN `createdAt`
                       WHEN `createdAt` GLOB '[0-9]*' THEN CAST(`createdAt` AS INTEGER)
                       ELSE CAST(strftime('%s', `createdAt`) AS INTEGER) * 1000
                   END
            FROM `check_ins`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `check_ins`")
        db.execSQL("ALTER TABLE `check_ins_new` RENAME TO `check_ins`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_savingsGoalId` ON `check_ins` (`savingsGoalId`)")
    }
}
