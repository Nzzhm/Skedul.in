package com.aplikasi.skedulin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Cek apakah user sudah login
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Restart semua alarm yang aktif
                restartActiveAlarms(context)
            }
        }
    }

    private fun restartActiveAlarms(context: Context) {
        // Get semua tugas yang belum selesai dan masih memiliki pengingat/deadline di masa depan
        FirebaseRepository.getIncompleteTugas { tugasList ->
            val currentTime = System.currentTimeMillis()

            tugasList.forEach { tugas ->
                // Restart alarm pengingat jika masih valid
                tugas.pengingat?.let { reminderTime ->
                    if (reminderTime > currentTime) {
                        NotificationHelper.setTaskReminder(
                            context,
                            tugas.id,
                            tugas.namatugas,
                            reminderTime
                        )
                    }
                }

                // Restart alarm deadline jika masih valid
                tugas.deadline?.let { deadlineTime ->
                    val oneDayBefore = deadlineTime - (24 * 60 * 60 * 1000)
                    if (oneDayBefore > currentTime) {
                        NotificationHelper.setDeadlineReminder(
                            context,
                            tugas.id,
                            tugas.namatugas,
                            deadlineTime
                        )
                    }
                }
            }
        }
    }
}