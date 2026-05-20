package com.aifinance.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

class ScheduledRuleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ruleIdStr = intent.getStringExtra(KEY_RULE_ID) ?: return
        val data = Data.Builder()
            .putString(ScheduledTransactionWorker.KEY_RULE_ID, ruleIdStr)
            .build()
        val request = OneTimeWorkRequestBuilder<ScheduledTransactionWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(ruleIdStr),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val KEY_RULE_ID = "scheduled_rule_id"

        fun pendingIntent(context: Context, ruleId: UUID): PendingIntent {
            val intent = Intent(context, ScheduledRuleAlarmReceiver::class.java).apply {
                putExtra(KEY_RULE_ID, ruleId.toString())
            }
            return PendingIntent.getBroadcast(
                context,
                ruleId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun uniqueWorkName(ruleIdStr: String) = "scheduled_tx_$ruleIdStr"
    }
}
