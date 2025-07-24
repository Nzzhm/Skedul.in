package com.aplikasi.skedulin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    // State untuk form input
    var namatugas by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var prioritasPilihan by remember { mutableStateOf("Sedang") }
    var isSelesai by remember { mutableStateOf(false) }

    // State untuk tanggal dan waktu
    val deadlineCalendar = remember { Calendar.getInstance() }
    val pengingatCalendar = remember { Calendar.getInstance() }

    // State untuk teks yang ditampilkan di UI
    var deadlineText by remember { mutableStateOf("") }
    var pengingatText by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())

    // Fungsi untuk menampilkan Time Picker, dipanggil setelah Date Picker selesai
    fun showTimePicker(calendar: Calendar, onTimeSelected: (String) -> Unit) {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            onTimeSelected(sdf.format(calendar.time))
        }
        TimePickerDialog(
            context,
            timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    // Fungsi untuk menampilkan Date Picker
    fun showDatePicker(calendar: Calendar, onTimeSelected: (String) -> Unit) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Langsung panggil Time Picker setelah tanggal dipilih
            showTimePicker(calendar, onTimeSelected)
        }
        DatePickerDialog(
            context,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = namatugas,
                onValueChange = { namatugas = it },
                label = { Text("Nama Tugas") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deadlineText,
                onValueChange = { },
                readOnly = true,
                label = { Text("Deadline") },
                trailingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pilih Tanggal Deadline",
                        modifier = Modifier.clickable {
                            showDatePicker(deadlineCalendar) { deadlineText = it }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pengingatText,
                onValueChange = { },
                readOnly = true,
                label = { Text("Pengingat Notifikasi") },
                trailingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pilih Tanggal Pengingat",
                        modifier = Modifier.clickable {
                            showDatePicker(pengingatCalendar) { pengingatText = it }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("Prioritas", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Tinggi", "Sedang", "Rendah").forEach { teks ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (teks == prioritasPilihan),
                            onClick = { prioritasPilihan = teks }
                        )
                        Text(text = teks, modifier = Modifier.padding(start = 2.dp, end = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tandai sebagai selesai", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isSelesai,
                    onCheckedChange = { isSelesai = it }
                )
            }
            Spacer(Modifier.height(24.dp))

            // PERBAIKAN UTAMA: Struktur database berdasarkan user
            Button(
                onClick = {
                    if (currentUser != null && namatugas.isNotBlank() && deadlineText.isNotBlank()) {
                        // Ambil User ID dari FirebaseAuth
                        val userId = currentUser.uid

                        // Buat referensi database dengan struktur: tugas/userId/taskId
                        val databaseReference = FirebaseDatabase.getInstance()
                            .getReference("tugas")
                            .child(userId) // Tambahkan userId sebagai parent node

                        val taskId = databaseReference.push().key ?: ""

                        val tugasBaru = Tugas(
                            id = taskId,
                            namatugas = namatugas,
                            deskripsi = deskripsi,
                            pembuat = currentUser.displayName ?: "Unknown User", // Berikan default jika null
                            deadline = Timestamp(deadlineCalendar.time),
                            pengingat = Timestamp(pengingatCalendar.time),
                            prioritas = prioritasPilihan,
                            selesai = isSelesai,
                            tanggalDibuat = Timestamp.now()
                        )

                        if (taskId.isNotEmpty()) {
                            // Simpan tugas di bawah user tertentu: tugas/userId/taskId
                            databaseReference.child(taskId).setValue(tugasBaru)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    onTaskAdded() // Panggil fungsi untuk menutup activity
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        // Tampilkan pesan error yang lebih spesifik
                        when {
                            currentUser == null -> Toast.makeText(context, "User belum login", Toast.LENGTH_SHORT).show()
                            namatugas.isBlank() -> Toast.makeText(context, "Nama Tugas wajib diisi", Toast.LENGTH_SHORT).show()
                            deadlineText.isBlank() -> Toast.makeText(context, "Deadline wajib diisi", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Simpan Tugas")
            }
        }
    }
}