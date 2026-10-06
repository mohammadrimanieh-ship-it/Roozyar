package ir.roozyaar.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, task: TaskItem) {
        val at = task.reminderAt ?: return
        if (task.status == TaskStatus.DONE || at <= System.currentTimeMillis()) return

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmPendingIntent(context, task.id)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                // Fallback if the user has not granted "Alarms & reminders" access yet.
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun canScheduleExactly(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarm.canScheduleExactAlarms()
    }

    fun cancel(context: Context, taskId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(alarmPendingIntent(context, taskId))
    }

    private fun alarmPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_ALARM
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            safeRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun safeRequestCode(id: Long): Int = (id xor (id ushr 32)).toInt()
}
