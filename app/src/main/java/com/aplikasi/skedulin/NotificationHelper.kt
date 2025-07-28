package com.aplikasi.skedulin

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val TASK_REMINDER_CHANNEL = "task_reminder_channel"
    const val DAILY_REMINDER_CHANNEL = "daily_reminder_channel"
    const val GENERAL_CHANNEL = "general_channel"

    // Tambahkan method debug ini ke NotificationHelper.kt

    fun debugSetTaskReminder(context: Context, taskId: String, taskName: String, reminderTime: Long) {
        Log.d("NotificationHelper", "Setting alarm untuk: $taskName")
        Log.d("NotificationHelper", "TaskId: $taskId")
        Log.d("NotificationHelper", "ReminderTime: $reminderTime")
        Log.d("NotificationHelper", "Current Time: ${System.currentTimeMillis()}")
        Log.d("NotificationHelper", "Time diff: ${reminderTime - System.currentTimeMillis()} ms")

        if (reminderTime <= System.currentTimeMillis()) {
            Log.e("NotificationHelper", "Reminder time sudah lewat! Tidak set alarm.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskName", taskName)
            putExtra("notificationType", "REMINDER")
        }

        Log.d("NotificationHelper", "Creating PendingIntent...")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            Log.d("NotificationHelper", "Setting exact alarm...")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            Log.d("NotificationHelper", "Alarm berhasil diset!")

            // Test apakah exact alarm permission ada
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val canSchedule = alarmManager.canScheduleExactAlarms()
                Log.d("NotificationHelper", "Can schedule exact alarms: $canSchedule")
            }

        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException: ${e.message}")
            // Handle jika tidak ada permission untuk exact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            Log.d("NotificationHelper", "Fallback to regular alarm")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error setting alarm: ${e.message}")
        }
    }

    /**
     * Membuat semua notification channels yang dibutuhkan
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel untuk pengingat tugas spesifik
            val taskReminderChannel = NotificationChannel(
                TASK_REMINDER_CHANNEL,
                "Pengingat Tugas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk tugas tertentu"
                enableVibration(true)
                enableLights(true)
            }

            // Channel untuk pengingat harian
            val dailyReminderChannel = NotificationChannel(
                DAILY_REMINDER_CHANNEL,
                "Pengingat Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi pengingat harian tentang tugas"
                enableVibration(true)
            }

            // Channel untuk notifikasi umum
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL,
                "Notifikasi Umum",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi umum aplikasi"
            }

            notificationManager.createNotificationChannels(listOf(
                taskReminderChannel,
                dailyReminderChannel,
                generalChannel
            ))
        }
    }

    /**
     * Set alarm untuk pengingat tugas
     */
    fun setTaskReminder(context: Context, taskId: String, taskName: String, reminderTime: Long) {
        if (reminderTime <= System.currentTimeMillis()) {
            return // Jangan set alarm untuk waktu yang sudah lewat
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskName", taskName)
            putExtra("notificationType", "REMINDER")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle jika tidak ada permission untuk exact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    /**
     * Set alarm untuk pengingat deadline (H-1)
     */
    fun setDeadlineReminder(context: Context, taskId: String, taskName: String, deadlineTime: Long) {
        val oneDayBefore = deadlineTime - (24 * 60 * 60 * 1000) // H-1

        if (oneDayBefore <= System.currentTimeMillis()) {
            return // Jangan set alarm untuk waktu yang sudah lewat
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("taskName", taskName)
            putExtra("notificationType", "DEADLINE_REMINDER")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${taskId}_deadline".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                oneDayBefore,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                oneDayBefore,
                pendingIntent
            )
        }
    }

    /**
     * Batalkan alarm pengingat tugas
     */
    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Batalkan alarm pengingat
        val reminderIntent = Intent(context, AlarmReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)

        // Batalkan alarm deadline
        val deadlineIntent = Intent(context, AlarmReceiver::class.java)
        val deadlinePendingIntent = PendingIntent.getBroadcast(
            context,
            "${taskId}_deadline".hashCode(),
            deadlineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(deadlinePendingIntent)
    }

    /**
     * Tampilkan notifikasi sederhana
     */
    fun showSimpleNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = GENERAL_CHANNEL,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Cek apakah ada permission untuk exact alarm (Android 12+)
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Request permission untuk exact alarm
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    }
}