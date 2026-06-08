package com.aifinance.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate transactions table with correct INTEGER createdAt
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions_new` (
                `id` TEXT NOT NULL,
                `accountId` TEXT NOT NULL,
                `categoryId` TEXT,
                `type` TEXT NOT NULL,
                `amount` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `date` TEXT NOT NULL,
                `time` INTEGER NOT NULL,
                `isPending` INTEGER NOT NULL,
                `receiptImagePath` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `sourceType` TEXT NOT NULL,
                `importBatchId` TEXT,
                `rawText` TEXT,
                `aiCategory` TEXT,
                `aiConfidence` REAL,
                `userConfirmed` INTEGER NOT NULL,
                `ocrSourceId` TEXT,
                `paymentMethod` TEXT,
                `paymentAccount` TEXT,
                `linkedTransactionId` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `transactions_new` (
                `id`, `accountId`, `categoryId`, `type`, `amount`, `currency`, `title`, `description`,
                `date`, `time`, `isPending`, `receiptImagePath`,
                `createdAt`, `updatedAt`,
                `sourceType`,
                `importBatchId`, `rawText`, `aiCategory`, `aiConfidence`, `userConfirmed`, `ocrSourceId`,
                `paymentMethod`, `paymentAccount`, `linkedTransactionId`
            )
            SELECT
                `id`, `accountId`, `categoryId`, `type`, `amount`, `currency`, `title`, `description`,
                `date`, `time`, `isPending`, `receiptImagePath`,
                CASE
                    WHEN typeof(`createdAt`) = 'integer' THEN `createdAt`
                    WHEN `createdAt` GLOB '[0-9]*' THEN CAST(`createdAt` AS INTEGER)
                    ELSE CAST(strftime('%s', `createdAt`) AS INTEGER) * 1000
                END,
                CASE
                    WHEN typeof(`updatedAt`) = 'integer' THEN `updatedAt`
                    WHEN `updatedAt` GLOB '[0-9]*' THEN CAST(`updatedAt` AS INTEGER)
                    ELSE CAST(strftime('%s', `updatedAt`) AS INTEGER) * 1000
                END,
                `sourceType`,
                `importBatchId`, `rawText`, `aiCategory`, `aiConfidence`, `userConfirmed`, `ocrSourceId`,
                `paymentMethod`, `paymentAccount`, `linkedTransactionId`
            FROM `transactions`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")

        // Recreate savings_records table with correct INTEGER createdAt
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `savings_records_new` (
                `id` TEXT NOT NULL,
                `savingsGoalId` TEXT NOT NULL,
                `amount` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `note` TEXT,
                `periodIndex` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `savings_records_new` (`id`, `savingsGoalId`, `amount`, `date`, `note`, `periodIndex`, `createdAt`)
            SELECT `id`, `savingsGoalId`, `amount`, `date`, `note`, `periodIndex`,
                   CASE
                       WHEN typeof(`createdAt`) = 'integer' THEN `createdAt`
                       WHEN `createdAt` GLOB '[0-9]*' THEN CAST(`createdAt` AS INTEGER)
                       ELSE CAST(strftime('%s', `createdAt`) AS INTEGER) * 1000
                   END
            FROM `savings_records`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `savings_records`")
        db.execSQL("ALTER TABLE `savings_records_new` RENAME TO `savings_records`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_records_savingsGoalId` ON `savings_records` (`savingsGoalId`)")
    }
}
