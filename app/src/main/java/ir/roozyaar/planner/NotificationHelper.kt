package ir.roozyaar.planner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "task_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری کارها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری کارهای ثبت‌شده در روز‌یار"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showTask(context: Context, task: TaskItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = PendingIntent.getActivity(
            context,
            ReminderScheduler.safeRequestCode(task.id) + 100_000,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.safeRequestCode(task.id) + 200_000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_COMPLETE
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.safeRequestCode(task.id) + 300_000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SNOOZE_10
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(task.title)
            .setContentText(buildString {
                append(task.category.emoji).append(' ').append(task.category.label)
                task.dueAt?.let { append(" • ").append(PersianDate.relativeDue(it)) }
            })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, "انجام شد", doneIntent)
            .addAction(0, "۱۰ دقیقه بعد", snoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(ReminderScheduler.safeRequestCode(task.id), notification)
    }
}
