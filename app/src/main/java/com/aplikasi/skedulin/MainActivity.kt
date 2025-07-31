package com.aplikasi.skedulin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    // UI Components
    private lateinit var textGreeting: TextView
    private lateinit var textUserName: TextView
    private lateinit var cardTodayTask: RelativeLayout
    private lateinit var textTodayTaskTitle: TextView
    private lateinit var textTodayTaskDate: TextView
    private lateinit var textTodayTaskCount: TextView
    private lateinit var textTugasHariIni: TextView
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var btnCekTugasLain: Button
    private lateinit var bottomNavHome: ImageView
    private lateinit var bottomNavCalendar: ImageView
    private lateinit var bottomNavProfile: ImageView

    // Data
    private lateinit var taskAdapter: MainTaskAdapter
    private val todayTasks = mutableListOf<Tugas>()

    // FIXED: Kontrol notifikasi yang lebih tepat
    private var hasShownWelcomeNotification = false
    private var isAppJustStarted = true

    companion object {
        const val CHANNEL_ID = "task_notification_channel"
        const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        // Konstanta untuk mengontrol frekuensi notifikasi (2 jam untuk mencegah spam)
        private const val NOTIFICATION_INTERVAL = 2 * 60 * 60 * 1000L // 2 jam

        // Key untuk SharedPreferences
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_LAST_NOTIF_TIME = "last_notif_time"
        private const val KEY_LAST_NOTIF_DATE = "last_notif_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        // PENTING: Inisialisasi semua notification channels
        createNotificationChannels()

        // Request permissions yang diperlukan untuk alarm
        requestNecessaryPermissions()

        // Setup WorkManager untuk daily reminder
        setupDailyReminder()

        initializeViews()
        setupUserData()
        setupRecyclerView()
        setupButtonListeners()
        loadTodayTasks()

        // FIXED: Set flag bahwa aplikasi baru saja dimulai
        isAppJustStarted = true
    }

    private fun initializeViews() {
        textGreeting = findViewById(R.id.text_greeting)
        textUserName = findViewById(R.id.text_user_name)
        cardTodayTask = findViewById(R.id.card_today_task)
        textTodayTaskTitle = findViewById(R.id.text_today_task_title)
        textTodayTaskDate = findViewById(R.id.text_today_task_date)
        textTodayTaskCount = findViewById(R.id.text_today_task_count)

        textTugasHariIni = findViewById(R.id.text_tugas_hari_ini)
        recyclerViewTasks = findViewById(R.id.recycler_view_tasks)
        btnCekTugasLain = findViewById(R.id.btn_cek_tugas_lain)
        bottomNavHome = findViewById(R.id.bottom_nav_home)
        bottomNavCalendar = findViewById(R.id.bottom_nav_calendar)
        bottomNavProfile = findViewById(R.id.bottom_nav_profile)
    }

    private fun setupUserData() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            // Set greeting based on time
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Selamat Pagi"
                hour < 15 -> "Selamat Siang"
                hour < 18 -> "Selamat Sore"
                else -> "Selamat Malam"
            }

            textGreeting.text = greeting + " 👋"
            textUserName.text = firebaseUser.displayName ?: "User"

            // Set today's date
            val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
            textTodayTaskDate.text = sdf.format(Date())

            // Load statistik tugas
            loadTugasStats()

            // FIXED: Hanya tampilkan notifikasi welcome saat pertama kali membuka aplikasi
            if (isAppJustStarted && !hasShownWelcomeNotification) {
                showWelcomeNotification()
                hasShownWelcomeNotification = true
                android.util.Log.d("MainActivity", "Showing welcome notification - app just started")
            } else {
                android.util.Log.d("MainActivity", "Skipping welcome notification - already shown or not app start")
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = MainTaskAdapter(todayTasks) { tugas ->
            // Handle task click - go to detail or edit
            val intent = Intent(this, EditTugas::class.java).apply {
                putExtra("TUGAS_ID", tugas.id)
                putExtra("NAMA_TUGAS", tugas.namatugas)
                putExtra("DESKRIPSI", tugas.deskripsi)
                putExtra("PRIORITAS", tugas.prioritas)
                putExtra("DEADLINE", tugas.deadline ?: -1L)
                putExtra("PENGINGAT", tugas.pengingat ?: -1L)
                putExtra("SELESAI", tugas.selesai)
            }
            startActivity(intent)
        }

        recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun setupButtonListeners() {
        btnCekTugasLain.setOnClickListener {
            startActivity(Intent(this, TampilTugas::class.java))
        }

        bottomNavHome.setOnClickListener {
            // Already on home - tidak perlu melakukan apa-apa
        }

        bottomNavCalendar.setOnClickListener {
            startActivity(Intent(this, TampilTugas::class.java))
        }

        bottomNavProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        cardTodayTask.setOnClickListener {
            startActivity(Intent(this, TampilTugas::class.java))
        }
    }


    private fun loadTodayTasks() {
        val today = System.currentTimeMillis()
        FirebaseRepository.getTugasByDate(today) { tugasHariIni ->
            runOnUiThread {
                val activeTasks = tugasHariIni.filter { !it.selesai }
                todayTasks.clear()
                todayTasks.addAll(activeTasks.take(3)) // Show only first 3 tasks
                taskAdapter.notifyDataSetChanged()

                // Update today task card
                updateTodayTaskCard(activeTasks)

                // Update task list visibility
                if (todayTasks.isEmpty()) {
                    textTugasHariIni.text = "Tidak ada tugas hari ini"
                    recyclerViewTasks.visibility = View.GONE
                } else {
                    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                    textTugasHariIni.text = "TUGAS HARI INI - ${dateFormat.format(Date(today))}"
                    recyclerViewTasks.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateTodayTaskCard(activeTasks: List<Tugas>) {
        val overdueCount = activeTasks.count { tugas ->
            tugas.deadline?.let { it < System.currentTimeMillis() } ?: false
        }

        // Get current day name in Indonesian
        val calendar = Calendar.getInstance()
        val dayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Hari Ini"
        }

        when {
            activeTasks.isEmpty() -> {
                textTodayTaskTitle.text = "$dayName"
                textTodayTaskCount.text = "Tidak ada tugas hari ini 😊"
                cardTodayTask.setBackgroundResource(R.drawable.card_today_task_empty)
            }
            overdueCount > 0 -> {
                textTodayTaskTitle.text = "$dayName"
                textTodayTaskCount.text = "Ada $overdueCount tugas terlambat"
                cardTodayTask.setBackgroundResource(R.drawable.card_today_task_overdue)
            }
            else -> {
                textTodayTaskTitle.text = "$dayName"
                textTodayTaskCount.text = "Ada ${activeTasks.size} tugas untuk hari ini"
                cardTodayTask.setBackgroundResource(R.drawable.card_today_task_normal)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume called, isAppJustStarted: $isAppJustStarted, hasShownWelcomeNotification: $hasShownWelcomeNotification")

        // Refresh data ketika kembali ke MainActivity
        loadTugasStats()
        loadTodayTasks()

        // FIXED: Tandai bahwa ini bukan lagi app start setelah onResume pertama
        if (isAppJustStarted) {
            isAppJustStarted = false
            android.util.Log.d("MainActivity", "App start phase ended")
        }

        // TIDAK ADA LAGI panggilan showWelcomeNotification() di sini
        // Notifikasi hanya ditampilkan saat onCreate() pertama kali
    }

    // Override onBackPressed untuk handle back button behavior
    override fun onBackPressed() {
        // Langsung keluar dari aplikasi tanpa navigasi ke activity lain
        finishAffinity()
    }

    private fun loadTugasStats() {
        FirebaseRepository.getTugasStats { total, selesai, pending ->
            runOnUiThread {
                // Update action bar subtitle if needed
                supportActionBar?.subtitle = "Total: $total | Selesai: $selesai | Pending: $pending"
            }
        }
    }

    // Function untuk membuat semua notification channels yang dibutuhkan
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel untuk general notifications (existing)
            val generalChannel = NotificationChannel(
                CHANNEL_ID,
                "Task Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders and updates"
                enableVibration(true)
                enableLights(true)
            }

            // Channel untuk alarm notifications (high priority)
            val alarmChannel = NotificationChannel(
                NotificationHelper.TASK_REMINDER_CHANNEL,
                "Pengingat Tugas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk tugas tertentu"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true) // Bypass Do Not Disturb
            }

            // Channel untuk daily reminders
            val dailyChannel = NotificationChannel(
                NotificationHelper.DAILY_REMINDER_CHANNEL,
                "Pengingat Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi pengingat harian tentang tugas"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(
                generalChannel,
                alarmChannel,
                dailyChannel
            ))
        }

        // PENTING: Panggil juga method dari NotificationHelper
        NotificationHelper.createNotificationChannels(this)
    }

    // Request permissions yang diperlukan untuk alarm system
    private fun requestNecessaryPermissions() {
        // Request exact alarm permission untuk Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!NotificationHelper.hasExactAlarmPermission(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Diperlukan")
                    .setMessage("Aplikasi memerlukan permission untuk mengatur alarm pengingat tugas yang tepat waktu. Tanpa ini, pengingat mungkin tidak bekerja dengan baik.")
                    .setPositiveButton("Beri Permission") { _, _ ->
                        NotificationHelper.requestExactAlarmPermission(this)
                    }
                    .setNegativeButton("Nanti") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        // Request notification permission untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, setup notifications
                    createNotificationChannels()
                } else {
                    // Permission denied, show explanation
                    AlertDialog.Builder(this)
                        .setTitle("Permission Notifikasi")
                        .setMessage("Tanpa permission notifikasi, Anda tidak akan menerima pengingat tugas. Anda bisa mengaktifkannya nanti di Settings.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    // FIXED: Function untuk menampilkan welcome notification dengan kontrol yang lebih ketat
    private fun showWelcomeNotification() {
        val currentTime = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Cek waktu notifikasi terakhir
        val lastNotifTime = prefs.getLong(KEY_LAST_NOTIF_TIME, 0L)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastNotifDate = prefs.getString(KEY_LAST_NOTIF_DATE, "")

        // FIXED: Cek apakah sudah pernah kirim notifikasi hari ini
        if (lastNotifDate == today) {
            android.util.Log.d("MainActivity", "Notification already sent today: $lastNotifDate")
            return
        }

        // FIXED: Cek interval waktu (minimal 2 jam sejak notifikasi terakhir)
        if (currentTime - lastNotifTime < NOTIFICATION_INTERVAL) {
            android.util.Log.d("MainActivity", "Notification interval not met: ${(currentTime - lastNotifTime) / 1000 / 60} minutes since last")
            return
        }

        android.util.Log.d("MainActivity", "Checking tasks for notification...")

        val todayTimestamp = System.currentTimeMillis()

        FirebaseRepository.getTugasByDate(todayTimestamp) { tugasHariIni ->
            val tugasBelumSelesai = tugasHariIni.filter { !it.selesai }
            val tugasOverdue = tugasHariIni.filter {
                !it.selesai && it.deadline != null && it.deadline!! < todayTimestamp
            }

            android.util.Log.d("MainActivity", "Tasks found - Total: ${tugasHariIni.size}, Incomplete: ${tugasBelumSelesai.size}, Overdue: ${tugasOverdue.size}")

            if (tugasBelumSelesai.isNotEmpty() || tugasOverdue.isNotEmpty()) {
                val title = when {
                    tugasOverdue.isNotEmpty() -> "⚠️ Ada ${tugasOverdue.size} tugas terlambat!"
                    tugasBelumSelesai.size == 1 -> "📝 Kamu punya 1 tugas hari ini"
                    else -> "📝 Kamu punya ${tugasBelumSelesai.size} tugas hari ini"
                }

                val message = when {
                    tugasOverdue.isNotEmpty() && tugasBelumSelesai.size > tugasOverdue.size -> {
                        "${tugasOverdue.size} terlambat, ${tugasBelumSelesai.size - tugasOverdue.size} untuk hari ini"
                    }
                    tugasOverdue.isNotEmpty() -> {
                        val overdueWithTime = tugasOverdue.take(2).map { tugas ->
                            val deadlineStr = if (tugas.deadline != null) {
                                val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                " (${dateFormat.format(Date(tugas.deadline!!))})"
                            } else ""
                            "${tugas.namatugas}$deadlineStr"
                        }
                        "Segera selesaikan: ${overdueWithTime.joinToString(", ")}"
                    }
                    else -> {
                        val tasksWithTime = tugasBelumSelesai.take(2).map { tugas ->
                            val deadlineStr = if (tugas.deadline != null) {
                                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                " (${dateFormat.format(Date(tugas.deadline!!))})"
                            } else ""
                            "${tugas.namatugas}$deadlineStr"
                        }
                        tasksWithTime.joinToString(", ")
                    }
                }

                android.util.Log.d("MainActivity", "Showing notification: $title")
                showNotification(title, message)

                // FIXED: Simpan waktu dan tanggal notifikasi terakhir
                prefs.edit()
                    .putLong(KEY_LAST_NOTIF_TIME, currentTime)
                    .putString(KEY_LAST_NOTIF_DATE, today)
                    .apply()
            } else {
                android.util.Log.d("MainActivity", "No tasks to notify about")
            }
        }
    }

    // Function untuk menampilkan notifikasi
    private fun showNotification(title: String, message: String) {
        // Cek permission dulu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return // Jangan tampilkan notifikasi jika tidak ada permission
            }
        }

        val intent = Intent(this, TampilTugas::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Handle jika tidak ada permission
            e.printStackTrace()
        }
    }

    // Setup WorkManager untuk daily reminder
    private fun setupDailyReminder() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_task_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    // Hitung delay untuk mulai reminder di jam 9 pagi
    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Jika sudah lewat jam 9 hari ini, set untuk besok
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
    }
}