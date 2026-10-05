package ir.roozyaar.planner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val db = TaskDbHelper(context.applicationContext)
        val task = db.get(taskId) ?: return

        when (intent.action) {
            ACTION_COMPLETE -> {
                db.update(task.copy(status = TaskStatus.DONE, reminderAt = null, updatedAt = System.currentTimeMillis()))
                ReminderScheduler.cancel(context, taskId)
                NotificationManagerCompat.from(context).cancel(ReminderScheduler.safeRequestCode(taskId))
            }
            ACTION_SNOOZE_10 -> {
                val snoozed = task.copy(
                    reminderAt = System.currentTimeMillis() + 10 * 60_000L,
                    updatedAt = System.currentTimeMillis()
                )
                db.update(snoozed)
                ReminderScheduler.schedule(context, snoozed)
                NotificationManagerCompat.from(context).cancel(ReminderScheduler.safeRequestCode(taskId))
            }
            ACTION_ALARM -> NotificationHelper.showTask(context, task)
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_ALARM = "ir.roozyaar.planner.ALARM"
        const val ACTION_COMPLETE = "ir.roozyaar.planner.COMPLETE"
        const val ACTION_SNOOZE_10 = "ir.roozyaar.planner.SNOOZE_10"
    }
}
