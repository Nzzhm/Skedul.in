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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = Color(0xFFF8F9FA)
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

    // State variables
    var namatugas by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var prioritasPilihan by remember { mutableStateOf("Sedang") }
    var isLoading by remember { mutableStateOf(false) }

    // Calendar instances
    val deadlineCalendar = remember { Calendar.getInstance() }
    val pengingatCalendar = remember { Calendar.getInstance() }

    var deadlineText by remember { mutableStateOf("") }
    var pengingatText by remember { mutableStateOf("") }

    val saveIcon = ImageVector.vectorResource(id = R.drawable.ic_save)

    // DateFormat
    val sdf = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

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

    // Function untuk mengatur alarm pengingat
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
                    tugasId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

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

    // Simpan tugas function
    val simpanTugas = remember {
        {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (namatugas.isBlank()) {
                Toast.makeText(context, "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else if (currentUser != null) {
                isLoading = true
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
                    isLoading = false
                    if (sukses) {
                        Toast.makeText(context, "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()

                        // Set alarm jika ada pengingat
                        if (pengingatMillis != null && pengingatMillis > System.currentTimeMillis()) {
                            setAlarmForTask(tugas.id, pengingatMillis, namatugas)
                        }

                        // Set alarm untuk H-1 deadline jika ada
                        if (deadlineMillis != null) {
                            val oneDayBefore = deadlineMillis - (24 * 60 * 60 * 1000)
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
            // Modern Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Buat Tugas Baru",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Atur dan kelola tugas Anda dengan mudah",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .scale(scale),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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

                // Modern Date Time Picker
                ModernDateTimePicker(
                    title = "Deadline",
                    subtitle = "Kapan tugas ini harus selesai?",
                    selectedTime = deadlineText,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_schedule),
                    iconColor = Color(0xFFEF4444),
                    onClick = { showDatePicker(deadlineCalendar) { deadlineText = it } }
                )

                ModernDateTimePicker(
                    title = "Pengingat",
                    subtitle = "Kapan Anda ingin diingatkan?",
                    selectedTime = pengingatText,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_alarm),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { showDatePicker(pengingatCalendar) { pengingatText = it } }
                )

                // Modern Priority Selector
                ModernPrioritySelector(
                    selectedPriority = prioritasPilihan,
                    onPrioritySelected = { prioritasPilihan = it },
                    priorityColors = priorityColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modern Save Button
                ModernActionButton(
                    text = if (isLoading) "Menyimpan..." else "Simpan Tugas",
                    icon = if (isLoading) null else saveIcon,
                    isLoading = isLoading,
                    gradientColors = gradientColors,
                    onClick = simpanTugas
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ModernInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    maxLines: Int = 1,
    minHeight: Dp = 56.dp
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF667EEA),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = Color(0xFF9CA3AF)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                maxLines = maxLines,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF667EEA),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun ModernDateTimePicker(
    title: String,
    subtitle: String,
    selectedTime: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_panah),
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ModernPrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    priorityColors: Map<String, Color>
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_bendera),
                    contentDescription = null,
                    tint = Color(0xFF667EEA),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Prioritas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Rendah", "Sedang", "Tinggi").forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val color = priorityColors[priority] ?: Color.Gray

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPrioritySelected(priority) }
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color(0xFFF9FAFB)
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(color, color)),
                                width = 2.dp
                            )
                        } else {
                            CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)))
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, CircleShape)
                            )
                            Text(
                                text = priority,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) color else Color(0xFF6B7280),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernActionButton(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading) { onClick() },
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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}