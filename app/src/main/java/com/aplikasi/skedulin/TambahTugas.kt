package com.aplikasi.skedulin

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class TambahTugas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TambahTugasScreen(onTaskAdded = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahTugasScreen(onTaskAdded: () -> Unit) {
    val context = LocalContext.current

    // State variables - menggunakan derivedStateOf untuk optimasi
    var namatugas by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var prioritasPilihan by remember { mutableStateOf("Sedang") }

    // Calendar instances - hanya dibuat sekali
    val deadlineCalendar = remember { Calendar.getInstance() }
    val pengingatCalendar = remember { Calendar.getInstance() }

    var deadlineText by remember { mutableStateOf("") }
    var pengingatText by remember { mutableStateOf("") }

    // DateFormat - dibuat sekali dan di-remember
    val sdf = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

    // TAMBAHAN: Function untuk mengatur alarm pengingat
    val setAlarmForTask = remember {
        { tugasId: String, pengingatTime: Long, taskName: String ->
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("taskId", tugasId)
                    putExtra("taskName", taskName)
                    putExtra("notificationType", "REMINDER")
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    tugasId.hashCode(), // Unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Set alarm exact (API 19+)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    pengingatTime,
                    pendingIntent
                )

                Toast.makeText(context, "Pengingat berhasil diatur", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengatur pengingat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Optimized time picker function - menghindari recreate dialog
    val showTimePicker = remember {
        { calendar: Calendar, onTimeSelected: (String) -> Unit ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    onTimeSelected(sdf.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // Optimized date picker function
    val showDatePicker = remember {
        { calendar: Calendar, onTimeSelected: (String) -> Unit ->
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    showTimePicker(calendar, onTimeSelected)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // Simpan tugas function - DIUPDATE untuk menambahkan alarm
    val simpanTugas = remember {
        {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (namatugas.isBlank()) {
                Toast.makeText(context, "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else if (currentUser != null) {
                val deadlineMillis = if (deadlineText.isNotEmpty()) deadlineCalendar.timeInMillis else null
                val pengingatMillis = if (pengingatText.isNotEmpty()) pengingatCalendar.timeInMillis else null

                val tugas = Tugas(
                    namatugas = namatugas,
                    deskripsi = deskripsi,
                    pembuat = currentUser.uid,
                    prioritas = prioritasPilihan,
                    deadline = deadlineMillis,
                    pengingat = pengingatMillis
                )

                FirebaseRepository.addTugas(tugas) { sukses, pesan ->
                    if (sukses) {
                        Toast.makeText(context, "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()

                        // TAMBAHAN: Set alarm jika ada pengingat
                        if (pengingatMillis != null && pengingatMillis > System.currentTimeMillis()) {
                            setAlarmForTask(tugas.id, pengingatMillis, namatugas)
                        }

                        // TAMBAHAN: Set alarm untuk H-1 deadline jika ada
                        if (deadlineMillis != null) {
                            val oneDayBefore = deadlineMillis - (24 * 60 * 60 * 1000) // H-1
                            if (oneDayBefore > System.currentTimeMillis()) {
                                val intent = Intent(context, AlarmReceiver::class.java).apply {
                                    putExtra("taskId", tugas.id)
                                    putExtra("taskName", namatugas)
                                    putExtra("notificationType", "DEADLINE_REMINDER")
                                }

                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    "${tugas.id}_deadline".hashCode(),
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    oneDayBefore,
                                    pendingIntent
                                )
                            }
                        }

                        onTaskAdded()
                    } else {
                        Toast.makeText(context, "Error: $pesan", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Anda harus login untuk menambah tugas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Tugas Baru") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        // Menggunakan LazyColumn untuk performa yang lebih baik
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Input nama tugas
            OutlinedTextField(
                value = namatugas,
                onValueChange = { namatugas = it },
                label = { Text("Nama Tugas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true // Optimasi untuk single line
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input deskripsi
            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5 // Batasi jumlah baris untuk performa
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SOLUSI UTAMA: Custom date picker button yang lebih responsif
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { showDatePicker(deadlineCalendar) { deadlineText = it } },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Deadline",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (deadlineText.isEmpty()) "Pilih tanggal deadline" else deadlineText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (deadlineText.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pilih Tanggal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom pengingat picker dengan desain yang sama
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { showDatePicker(pengingatCalendar) { pengingatText = it } },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pengingat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (pengingatText.isEmpty()) "Pilih waktu pengingat" else pengingatText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pengingatText.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pilih Tanggal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prioritas section dengan Card untuk konsistensi
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Prioritas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Rendah", "Sedang", "Tinggi").forEach { prioritas ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f) // Distribusi ruang yang merata
                            ) {
                                RadioButton(
                                    selected = prioritasPilihan == prioritas,
                                    onClick = { prioritasPilihan = prioritas }
                                )
                                Text(
                                    text = prioritas,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol simpan dengan feedback visual
            Button(
                onClick = simpanTugas,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Height yang konsisten
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                )
            ) {
                Text(
                    text = "Simpan Tugas",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            // Tambah spacer di bawah untuk scroll yang lebih nyaman
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}