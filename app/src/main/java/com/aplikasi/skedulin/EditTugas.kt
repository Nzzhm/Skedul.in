package com.aplikasi.skedulin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = Color(0xFFF8F9FA)
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
    var isLoading by remember { mutableStateOf(false) }

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

    // Gradient colors
    val gradientColors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2)
    )

    // Priority colors
    val priorityColors = mapOf(
        "Rendah" to Color(0xFF10B981),
        "Sedang" to Color(0xFFF59E0B),
        "Tinggi" to Color(0xFFEF4444)
    )

    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

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
                isLoading = true
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
                    tanggalDibuat = 0L
                )

                FirebaseRepository.updateTugas(updatedTugas) { sukses, pesan ->
                    isLoading = false
                    if (sukses) {
                        Toast.makeText(context, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()

                        // Batalkan alarm lama terlebih dahulu
                        NotificationHelper.cancelTaskReminder(context, updatedTugas.id)
                        Log.d("EditTugas", "Membatalkan alarm lama untuk tugas ID: ${updatedTugas.id}")

                        // Set pengingat baru jika ada
                        updatedTugas.pengingat?.let { waktuPengingat ->
                            if (waktuPengingat > System.currentTimeMillis()) {
                                Log.d("EditTugas", "Menyetel pengingat baru pada: $waktuPengingat")
                                NotificationHelper.setTaskReminder(
                                    context = context,
                                    taskId = updatedTugas.id,
                                    taskName = updatedTugas.namatugas,
                                    reminderTime = waktuPengingat
                                )
                            }
                        }

                        // Set deadline reminder baru jika ada
                        updatedTugas.deadline?.let { waktuDeadline ->
                            Log.d("EditTugas", "Menyetel deadline reminder baru untuk deadline: $waktuDeadline")
                            NotificationHelper.setDeadlineReminder(
                                context = context,
                                taskId = updatedTugas.id,
                                taskName = updatedTugas.namatugas,
                                deadlineTime = waktuDeadline
                            )
                        }

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

    // Clear functions
    val clearDeadline = remember {
        {
            deadlineText = ""
            deadlineCalendar.timeInMillis = System.currentTimeMillis()
        }
    }

    val clearPengingat = remember {
        {
            pengingatText = ""
            pengingatCalendar.timeInMillis = System.currentTimeMillis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Modern Header with Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = "Edit Tugas",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Perbarui detail tugas Anda",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .scale(scale),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Task Status Card
                ModernStatusCard(
                    title = "Status Tugas",
                    isCompleted = selesai,
                    onStatusChanged = { selesai = it }
                )

                // Modern Input Fields
                ModernInputField(
                    value = namatugas,
                    onValueChange = { namatugas = it },
                    label = "Nama Tugas",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_tugas),
                    placeholder = "Masukkan nama tugas..."
                )

                ModernInputField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = "Deskripsi",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_deskripsi),
                    placeholder = "Deskripsikan tugas Anda...",
                    maxLines = 4,
                    minHeight = 120.dp
                )

                // Modern Date Time Picker with Clear Option
                ModernDateTimePickerWithClear(
                    title = "Deadline",
                    subtitle = "Kapan tugas ini harus selesai?",
                    selectedTime = deadlineText,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_schedule),
                    iconColor = Color(0xFFEF4444),
                    onClick = { showDatePicker(deadlineCalendar) { deadlineText = it } },
                    onClear = clearDeadline
                )

                ModernDateTimePickerWithClear(
                    title = "Pengingat",
                    subtitle = "Kapan Anda ingin diingatkan?",
                    selectedTime = pengingatText,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_alarm),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { showDatePicker(pengingatCalendar) { pengingatText = it } },
                    onClear = clearPengingat
                )

                // Modern Priority Selector
                ModernPrioritySelector(
                    selectedPriority = prioritasPilihan,
                    onPrioritySelected = { prioritasPilihan = it },
                    priorityColors = priorityColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modern Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isLoading) { onBackPressed() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)))
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Batal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                            }
                        }
                    }

                    // Update Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isLoading) { updateTugas() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(gradientColors),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = if (isLoading) "Memperbarui..." else "Perbarui",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ModernStatusCard(
    title: String,
    isCompleted: Boolean,
    onStatusChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF0FDF4) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isCompleted) {
                                listOf(
                                    Color(0xFF10B981).copy(alpha = 0.2f),
                                    Color(0xFF10B981).copy(alpha = 0.1f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF6B7280).copy(alpha = 0.2f),
                                    Color(0xFF6B7280).copy(alpha = 0.1f)
                                )
                            }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) {
                        Icons.Default.CheckCircle
                    } else {
                        ImageVector.vectorResource(id = R.drawable.radio_button_unchecked)
                    },
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF10B981) else Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = if (isCompleted) "Tugas telah selesai" else "Tugas belum selesai",
                    fontSize = 14.sp,
                    color = if (isCompleted) Color(0xFF059669) else Color(0xFF6B7280),
                    fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Switch(
                checked = isCompleted,
                onCheckedChange = onStatusChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E7EB)
                )
            )
        }
    }
}

@Composable
fun ModernDateTimePickerWithClear(
    title: String,
    subtitle: String,
    selectedTime: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    iconColor.copy(alpha = 0.2f),
                                    iconColor.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = if (selectedTime.isEmpty()) subtitle else selectedTime,
                        fontSize = 14.sp,
                        color = if (selectedTime.isEmpty()) Color(0xFF6B7280) else Color(0xFF059669),
                        fontWeight = if (selectedTime.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF667EEA),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (selectedTime.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}