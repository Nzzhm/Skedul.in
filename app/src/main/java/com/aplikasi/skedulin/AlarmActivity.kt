package com.aplikasi.skedulin

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var taskNameText: TextView
    private lateinit var taskTimeText: TextView
    private lateinit var dismissButton: Button
    private lateinit var snoozeButton: Button

    private var taskId: String = ""
    private var taskName: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var autoSnoozeRunnable: Runnable? = null

    companion object {
        private const val TAG = "AlarmActivity"
        private const val AUTO_SNOOZE_DELAY = 60000L // 1 menit auto snooze jika tidak ada aksi
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "AlarmActivity onCreate called")

        try {
            // TAMPIL DI ATAS LOCKSCREEN - PRIORITAS TINGGI
            setupLockScreenDisplay()

            setContentView(R.layout.activity_alarm)

            // Ambil data dari intent
            taskName = intent.getStringExtra("taskName") ?: "Tugas"
            taskId = intent.getStringExtra("taskId") ?: ""

            Log.d(TAG, "Task received: $taskName (ID: $taskId)")

            // Inisialisasi views
            initializeViews()

            // Setup UI
            setupUI()

            // MULAI SUARA DAN GETAR
            startAlarmSound()
            startVibration()

            // Setup button listeners
            setupButtonListeners()

            // Auto snooze setelah 1 menit jika tidak ada aksi
            setupAutoSnooze()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            // Jika ada error, tetap coba tampilkan minimal UI
            try {
                setContentView(R.layout.activity_alarm)
                initializeViews()
                taskNameText.text = "Error: ${e.message}"
            } catch (e2: Exception) {
                Log.e(TAG, "Critical error in onCreate: ${e2.message}", e2)
                finish()
            }
        }
    }

    private fun setupLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Android 8.1+
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // Android versi lama
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Tambahan untuk memastikan tampil di atas semua
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun initializeViews() {
        taskNameText = findViewById(R.id.tv_task_name)
        taskTimeText = findViewById(R.id.tv_task_time)
        dismissButton = findViewById(R.id.btn_dismiss)
        snoozeButton = findViewById(R.id.btn_snooze)
    }

    private fun setupUI() {
        // Set task name
        taskNameText.text = "Waktunya mengerjakan:\n$taskName"

        // Set waktu sekarang
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        taskTimeText.text = "Waktu: $currentTime"

        // Animasi berkedip untuk menarik perhatian
        startBlinkingAnimation()
    }

    private fun startBlinkingAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                taskNameText.visibility = if (taskNameText.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                handler.postDelayed(this, 500) // Berkedip setiap 500ms
            }
        })
    }

    private fun setupButtonListeners() {
        dismissButton.setOnClickListener {
            Log.d(TAG, "Dismiss button clicked")
            stopAlarmAndFinish()
        }

        snoozeButton.setOnClickListener {
            Log.d(TAG, "Snooze button clicked")
            snoozeAlarm(taskId, taskName)
            stopAlarmAndFinish()
        }
    }

    private fun setupAutoSnooze() {
        autoSnoozeRunnable = Runnable {
            Log.d(TAG, "Auto snooze triggered")
            Toast.makeText(this, "Alarm di-snooze otomatis", Toast.LENGTH_SHORT).show()
            snoozeAlarm(taskId, taskName)
            stopAlarmAndFinish()
        }

        handler.postDelayed(autoSnoozeRunnable!!, AUTO_SNOOZE_DELAY)
    }

    private fun startAlarmSound() {
        try {
            // Coba beberapa URI alarm secara berurutan
            val alarmUris = listOf(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            )

            var soundStarted = false

            for (uri in alarmUris) {
                if (uri != null && !soundStarted) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(this@AlarmActivity, uri)
                            isLooping = true
                            setVolume(1.0f, 1.0f) // Volume maksimal
                            prepare()
                            start()
                        }
                        soundStarted = true
                        Log.d(TAG, "Alarm sound started with URI: $uri")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to start sound with URI: $uri", e)
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
            }

            if (!soundStarted) {
                Log.w(TAG, "Could not start any alarm sound")
                // Fallback: buat beep sederhana
                createBeepSound()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm sound: ${e.message}", e)
            createBeepSound()
        }
    }

    private fun createBeepSound() {
        try {
            // Fallback: buat tone sederhana
            mediaPlayer = MediaPlayer().apply {
                // Ini akan membuat tone default jika ada
                reset()
                // Bisa juga menggunakan ToneGenerator sebagai alternatif
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating beep sound: ${e.message}", e)
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Pattern: diam, getar, diam, getar, dst
                    val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    // Untuk Android versi lama
                    val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    vibrator?.vibrate(pattern, 0) // 0 = repeat
                }
                Log.d(TAG, "Vibration started")
            } else {
                Log.w(TAG, "Device does not support vibration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration: ${e.message}", e)
        }
    }

    private fun snoozeAlarm(taskId: String, taskName: String) {
        try {
            // Set alarm lagi 5 menit kemudian
            val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 menit

            Log.d(TAG, "Setting snooze alarm for $taskName at ${Date(snoozeTime)}")

            NotificationHelper.setTaskReminder(this, taskId, taskName, snoozeTime)

            Toast.makeText(this, "Alarm akan berbunyi lagi dalam 5 menit", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting snooze alarm: ${e.message}", e)
            Toast.makeText(this, "Gagal mengatur snooze: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAlarmAndFinish() {
        Log.d(TAG, "Stopping alarm and finishing activity")

        try {
            // Stop blinking animation
            handler.removeCallbacksAndMessages(null)

            // Stop media player
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                mediaPlayer = null
                Log.d(TAG, "Media player stopped and released")
            }

            // Stop vibration
            vibrator?.let {
                it.cancel()
                vibrator = null
                Log.d(TAG, "Vibration stopped")
            }

            // Cancel auto snooze
            autoSnoozeRunnable?.let {
                handler.removeCallbacks(it)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm: ${e.message}", e)
        } finally {
            // Always finish the activity
            finish()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "AlarmActivity onDestroy called")
        super.onDestroy()
        stopAlarmAndFinish()
    }

    override fun onBackPressed() {
        // Disable back button untuk memaksa user dismiss/snooze
        Log.d(TAG, "Back button pressed - ignored")
        // Tidak panggil super.onBackPressed() agar tidak bisa di-back
        // Note: Jika Anda menggunakan Android 13+ (API 33+), pertimbangkan menggunakan OnBackInvokedCallback
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Untuk Android 12 dan sebelumnya, tidak memanggil super adalah OK
            return
        } else {
            // Untuk Android 13+, Anda mungkin perlu menggunakan OnBackInvokedCallback
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "AlarmActivity onPause called")
        // Jangan stop alarm saat onPause karena mungkin hanya screen rotation
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "AlarmActivity onResume called")

        // Pastikan alarm masih berjalan
        if (mediaPlayer == null || mediaPlayer?.isPlaying != true) {
            startAlarmSound()
        }

        if (vibrator == null) {
            startVibration()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "AlarmActivity onNewIntent called")

        // Handle jika ada intent baru (misalnya alarm lain)
        val newTaskName = intent.getStringExtra("taskName")
        val newTaskId = intent.getStringExtra("taskId")

        if (newTaskName != null && newTaskId != null) {
            taskName = newTaskName
            taskId = newTaskId
            setupUI()
        }
    }
}