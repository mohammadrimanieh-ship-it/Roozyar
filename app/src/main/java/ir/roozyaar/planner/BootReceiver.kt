package ir.roozyaar.planner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            try {
                TaskDbHelper(context.applicationContext)
                    .pendingReminders(System.currentTimeMillis())
                    .forEach { ReminderScheduler.schedule(context, it) }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
