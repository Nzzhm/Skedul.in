package com.aplikasi.skedulin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "daily_task_reminder_channel"
        const val NOTIFICATION_ID = 3000
    }

    override suspend fun doWork(): Result {
        return try {
            // Cek apakah user sudah login
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                return Result.success() // Skip jika belum login
            }

            createNotificationChannel()

            // Get tugas untuk hari ini dan besok
            val todayTasks = getTodayTasks()
            val tomorrowTasks = getTomorrowTasks()
            val overdueTasks = getOverdueTasks()

            // Tampilkan notifikasi berdasarkan kondisi
            when {
                overdueTasks.isNotEmpty() -> {
                    showOverdueNotification(overdueTasks.size)
                }
                todayTasks.isNotEmpty() -> {
                    showTodayTasksNotification(todayTasks.size)
                }
                tomorrowTasks.isNotEmpty() -> {
                    showTomorrowTasksNotification(tomorrowTasks.size)
                }
                else -> {
                    showNoTasksNotification()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Task Reminders"
            val descriptionText = "Daily notifications about your tasks"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun getTodayTasks(): List<Tugas> = suspendCancellableCoroutine { continuation ->
        val today = System.currentTimeMillis()
        FirebaseRepository.getIncompleteTugasByDate(today) { tasks ->
            continuation.resume(tasks)
        }
    }

    private suspend fun getTomorrowTasks(): List<Tugas> = suspendCancellableCoroutine { continuation ->
        val tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        FirebaseRepository.getIncompleteTugasByDate(tomorrow) { tasks ->
            continuation.resume(tasks)
        }
    }

    private suspend fun getOverdueTasks(): List<Tugas> = suspendCancellableCoroutine { continuation ->
        FirebaseRepository.getTugasOverdue { tasks ->
            continuation.resume(tasks)
        }
    }

    private fun showOverdueNotification(count: Int) {
        val title = "🚨 $count Tugas Terlambat!"
        val message = if (count == 1) {
            "Ada 1 tugas yang sudah melewati deadline. Segera selesaikan!"
        } else {
            "Ada $count tugas yang sudah melewati deadline. Segera selesaikan!"
        }

        showNotification(title, message, "🚨")
    }

    private fun showTodayTasksNotification(count: Int) {
        val title = "📅 Tugas Hari Ini"
        val message = if (count == 1) {
            "Kamu punya 1 tugas untuk dikerjakan hari ini"
        } else {
            "Kamu punya $count tugas untuk dikerjakan hari ini"
        }

        showNotification(title, message, "📅")
    }

    private fun showTomorrowTasksNotification(count: Int) {
        val title = "⏳ Deadline Besok"
        val message = if (count == 1) {
            "Ada 1 tugas yang deadline-nya besok"
        } else {
            "Ada $count tugas yang deadline-nya besok"
        }

        showNotification(title, message, "⏳")
    }

    private fun showNoTasksNotification() {
        val title = "✅ Semua Tugas Selesai!"
        val message = "Tidak ada tugas yang perlu dikerjakan hari ini. Good job! 🎉"

        showNotification(title, message, "✅")
    }

    private fun showNotification(title: String, message: String, emoji: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent untuk melihat tugas
        val viewTasksIntent = Intent(applicationContext, TampilTugas::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val viewTasksPendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            viewTasksIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Lihat Tugas",
                viewTasksPendingIntent
            )

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}