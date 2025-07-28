package com.aplikasi.skedulin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver dipanggil!")

        val taskId = intent.getStringExtra("taskId") ?: run {
            Log.e(TAG, "TaskId tidak ditemukan!")
            return
        }
        val taskName = intent.getStringExtra("taskName") ?: "Tugas"
        val notificationType = intent.getStringExtra("notificationType") ?: "REMINDER"

        Log.d(TAG, "TaskId: $taskId, TaskName: $taskName, Type: $notificationType")

        when (notificationType) {
            "REMINDER" -> {
                Log.d(TAG, "Menampilkan alarm full screen...")
                showFullScreenAlarm(context, taskId, taskName)
            }
            "DEADLINE_REMINDER" -> {
                Log.d(TAG, "Menampilkan deadline notification...")
                showDeadlineNotification(context, taskName)
            }
        }
    }

    private fun showFullScreenAlarm(context: Context, taskId: String, taskName: String) {
        Log.d(TAG, "Membuat intent untuk AlarmActivity...")

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskName", taskName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }

        try {
            Log.d(TAG, "Starting AlarmActivity...")
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka AlarmActivity: ${e.message}")
            // Fallback ke notifikasi biasa jika gagal buka activity
            showFallbackNotification(context, taskName, taskId)
        }
    }

    private fun showDeadlineNotification(context: Context, taskName: String) {
        // Pastikan notification channel sudah dibuat
        NotificationHelper.createNotificationChannels(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.TASK_REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Deadline Besok!")
            .setContentText("Jangan lupa: $taskName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskName.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handle jika tidak ada permission notifikasi
            e.printStackTrace()
        }
    }

    private fun showFallbackNotification(context: Context, taskName: String, taskId: String) {
        NotificationHelper.createNotificationChannels(context)

        // Intent untuk membuka AlarmActivity ketika notifikasi ditekan
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskName", taskName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            alarmIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.TASK_REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Waktunya Mengerjakan!")
            .setContentText(taskName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // Coba tampilkan full screen
            .setContentIntent(pendingIntent)
            .setAutoCancel(false) // Jangan auto cancel untuk alarm
            .setOngoing(true) // Membuat notifikasi tidak bisa di-swipe
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}