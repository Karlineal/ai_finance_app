package com.aifinance.feature.scheduled

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat.startActivity

/**
 * 精确闹钟权限检测和引导工具
 * 
 * Android 12+ (API 31+) 引入了 SCHEDULE_EXACT_ALARM 权限，这是一个特殊权限：
 * - 不能像普通权限那样在运行时请求弹窗
 * - 用户必须在系统设置中手动开启
 * - 权限状态可能随时被用户在设置中关闭
 */
object ExactAlarmPermissionHelper {

    /**
     * 检查当前应用是否有精确闹钟权限
     * 
     * @return true 表示可以设置精确闹钟，false 需要引导用户去设置开启
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        // Android 12 以下不需要此权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * 检查是否需要显示权限引导（Android 12+ 且没有权限）
     */
    fun shouldShowPermissionGuidance(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)
    }

    /**
     * 跳转到系统设置页面，让用户开启精确闹钟权限
     * 
     * 设置路径：设置 > 应用 > iCookie > 闹钟和提醒 > 允许设置闹钟和提醒
     */
    fun openAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // 如果 ACTION_REQUEST_SCHEDULE_EXACT_ALARM 不可用，打开应用详情页
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(fallbackIntent)
            }
        } else {
            // Android 12 以下不需要此权限
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * 获取权限状态的描述文本
     */
    fun getPermissionStatusText(context: Context): String {
        return if (canScheduleExactAlarms(context)) {
            "定时记账功能正常，将在设定时间准时触发"
        } else {
            "需要开启精确闹钟权限，否则定时记账可能延迟或无法触发"
        }
    }
}
