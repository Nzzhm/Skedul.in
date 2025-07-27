package com.aplikasi.skedulin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.ArrowBack
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

class EditTugas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ambil data tugas dari intent
        val tugasId = intent.getStringExtra("TUGAS_ID") ?: ""
        val namatugas = intent.getStringExtra("NAMA_TUGAS") ?: ""
        val deskripsi = intent.getStringExtra("DESKRIPSI") ?: ""
        val prioritas = intent.getStringExtra("PRIORITAS") ?: "Sedang"
        val deadline = intent.getLongExtra("DEADLINE", -1L)
        val pengingat = intent.getLongExtra("PENGINGAT", -1L)
        val selesai = intent.getBooleanExtra("SELESAI", false)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditTugasScreen(
                        tugasId = tugasId,
                        initialNamatugas = namatugas,
                        initialDeskripsi = deskripsi,
                        initialPrioritas = prioritas,
                        initialDeadline = if (deadline != -1L) deadline else null,
                        initialPengingat = if (pengingat != -1L) pengingat else null,
                        initialSelesai = selesai,
                        onTaskUpdated = { finish() },
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTugasScreen(
    tugasId: String,
    initialNamatugas: String,
    initialDeskripsi: String,
    initialPrioritas: String,
    initialDeadline: Long?,
    initialPengingat: Long?,
    initialSelesai: Boolean,
    onTaskUpdated: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    // State variables
    var namatugas by remember { mutableStateOf(initialNamatugas) }
    var deskripsi by remember { mutableStateOf(initialDeskripsi) }
    var prioritasPilihan by remember { mutableStateOf(initialPrioritas) }
    var selesai by remember { mutableStateOf(initialSelesai) }

    // Calendar instances
    val deadlineCalendar = remember { Calendar.getInstance() }
    val pengingatCalendar = remember { Calendar.getInstance() }

    // DateFormat
    val sdf = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

    // Initialize calendar and text values
    var deadlineText by remember {
        mutableStateOf(
            if (initialDeadline != null) {
                deadlineCalendar.timeInMillis = initialDeadline
                sdf.format(Date(initialDeadline))
            } else ""
        )
    }

    var pengingatText by remember {
        mutableStateOf(
            if (initialPengingat != null) {
                pengingatCalendar.timeInMillis = initialPengingat
                sdf.format(Date(initialPengingat))
            } else ""
        )
    }

    // Time picker function
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

    // Date picker function
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

    // Update tugas function
    val updateTugas = remember {
        {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (namatugas.isBlank()) {
                Toast.makeText(context, "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else if (currentUser != null) {
                val deadlineMillis = if (deadlineText.isNotEmpty()) deadlineCalendar.timeInMillis else null
                val pengingatMillis = if (pengingatText.isNotEmpty()) pengingatCalendar.timeInMillis else null

                val updatedTugas = Tugas(
                    id = tugasId,
                    namatugas = namatugas,
                    deskripsi = deskripsi,
                    pembuat = currentUser.uid,
                    prioritas = prioritasPilihan,
                    deadline = deadlineMillis,
                    pengingat = pengingatMillis,
                    selesai = selesai,
                    tanggalDibuat = System.currentTimeMillis() // Keep original creation time in real implementation
                )

                FirebaseRepository.updateTugas(updatedTugas) { sukses, pesan ->
                    if (sukses) {
                        Toast.makeText(context, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        onTaskUpdated()
                    } else {
                        Toast.makeText(context, "Error: $pesan", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Anda harus login untuk mengubah tugas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Clear deadline function
    val clearDeadline = remember {
        {
            deadlineText = ""
            deadlineCalendar.timeInMillis = System.currentTimeMillis()
        }
    }

    // Clear pengingat function
    val clearPengingat = remember {
        {
            pengingatText = ""
            pengingatCalendar.timeInMillis = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Tugas") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
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
                singleLine = true
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
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status selesai
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Status Tugas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selesai,
                            onCheckedChange = { selesai = it }
                        )
                        Text(
                            text = if (selesai) "Selesai" else "Belum Selesai",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline picker with clear option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Row {
                            IconButton(
                                onClick = { showDatePicker(deadlineCalendar) { deadlineText = it } }
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Pilih Tanggal",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (deadlineText.isNotEmpty()) {
                                TextButton(onClick = clearDeadline) {
                                    Text("Hapus", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pengingat picker with clear option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Row {
                            IconButton(
                                onClick = { showDatePicker(pengingatCalendar) { pengingatText = it } }
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Pilih Tanggal",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (pengingatText.isNotEmpty()) {
                                TextButton(onClick = clearPengingat) {
                                    Text("Hapus", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prioritas section
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
                                modifier = Modifier.weight(1f)
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

            // Tombol update dan batal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = updateTugas,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    )
                ) {
                    Text(
                        text = "Perbarui",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}