package com.aifinance.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            BootReceiverEntryPoint::class.java,
        )
        val scheduler = entryPoint.scheduledRuleScheduler()

        scope.launch {
            scheduler.rescheduleAllEnabled()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun scheduledRuleScheduler(): ScheduledRuleScheduler
}
