package com.aifinance.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS savings_records (
                id TEXT NOT NULL PRIMARY KEY,
                savingsGoalId TEXT NOT NULL,
                amount TEXT NOT NULL,
                date TEXT NOT NULL,
                note TEXT,
                periodIndex INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                FOREIGN KEY (savingsGoalId) REFERENCES savings_goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_savings_records_savingsGoalId ON savings_records(savingsGoalId)")
    }
}
