package com.aplikasi.skedulin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class TampilTugas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    TampilTugasScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TampilTugasScreen() {
    val context = LocalContext.current
    var tugasList by remember { mutableStateOf<List<Tugas>>(emptyList()) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }

    // PERBAIKAN: Gunakan state yang lebih sederhana untuk month dan year
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    var showYearPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tugasToDelete by remember { mutableStateOf<Tugas?>(null) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var tugasToComplete by remember { mutableStateOf<Tugas?>(null) }

    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // PERBAIKAN: Buat Calendar object dari currentYear dan currentMonth
    val currentCalendar = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Load incomplete tasks only
    LaunchedEffect(Unit) {
        FirebaseRepository.getTugas { tugas ->
            tugasList = tugas.filter { !it.selesai }
        }
    }

    // Filter tasks based on selected date and search query
    val filteredTugas = remember(tugasList, selectedDate, searchQuery) {
        var filtered = tugasList

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { tugas ->
                tugas.namatugas.contains(searchQuery, ignoreCase = true) ||
                        tugas.deskripsi.contains(searchQuery, ignoreCase = true) ||
                        tugas.prioritas.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply date filter
        if (selectedDate != null) {
            filtered = filtered.filter { tugas ->
                tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    val selectedCalendar = selectedDate!!

                    deadlineCalendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                            deadlineCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                            deadlineCalendar.get(Calendar.DAY_OF_MONTH) == selectedCalendar.get(Calendar.DAY_OF_MONTH)
                } ?: false
            }
        }

        // Sort tasks - overdue tasks first, then by priority, then by deadline
        filtered.sortedWith(compareBy<Tugas> { tugas ->
            val isOverdue = tugas.deadline?.let { it < System.currentTimeMillis() } ?: false
            if (isOverdue) 0 else 1
        }.thenBy { tugas ->
            when(tugas.prioritas) {
                "Tinggi" -> 1
                "Sedang" -> 2
                "Rendah" -> 3
                else -> 4
            }
        }.thenBy { it.deadline ?: Long.MAX_VALUE })
    }

    // Year picker dialog
    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("Pilih Tahun") },
            text = {
                LazyColumn {
                    items((2020..2030).toList()) { year ->
                        TextButton(
                            onClick = {
                                currentYear = year
                                showYearPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = year.toString(),
                                color = if (year == currentYear) Color(0xFF6366F1) else Color.Black,
                                fontWeight = if (year == currentYear) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && tugasToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                tugasToDelete = null
            },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus tugas \"${tugasToDelete?.namatugas}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tugasToDelete?.let { tugas ->
                            FirebaseRepository.deleteTugas(tugas.id) { sukses, pesan ->
                                if (sukses) {
                                    Toast.makeText(context, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: $pesan", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        showDeleteDialog = false
                        tugasToDelete = null
                    }
                ) {
                    Text("Hapus", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        tugasToDelete = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Complete task confirmation dialog
    if (showCompleteDialog && tugasToComplete != null) {
        AlertDialog(
            onDismissRequest = {
                showCompleteDialog = false
                tugasToComplete = null
            },
            title = { Text("Selesaikan Tugas") },
            text = { Text("Apakah Anda yakin ingin menandai tugas \"${tugasToComplete?.namatugas}\" sebagai selesai?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tugasToComplete?.let { tugas ->
                            val isOverdue = tugas.deadline?.let { it < System.currentTimeMillis() } ?: false
                            val updatedTugas = tugas.copy(
                                selesai = true,
                                tanggalSelesai = System.currentTimeMillis(),
                                // Add a flag to indicate if it was completed late
                                isCompletedLate = isOverdue
                            )
                            FirebaseRepository.updateTugas(updatedTugas) { sukses, pesan ->
                                if (sukses) {
                                    Toast.makeText(context, "Tugas berhasil diselesaikan!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: $pesan", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        showCompleteDialog = false
                        tugasToComplete = null
                    }
                ) {
                    Text("Ya, Selesaikan", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompleteDialog = false
                        tugasToComplete = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                (context as? ComponentActivity)?.finish()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Row {
                IconButton(onClick = {
                    val intent = Intent(context, HistoryTugas::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Face, contentDescription = "History")
                }
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { /* Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        }

        // Search Bar
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari tugas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Month/Year Navigation with Add Task Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    // PERBAIKAN: Update state dengan cara yang lebih sederhana
                    if (currentMonth == 0) {
                        currentMonth = 11
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showYearPicker = true }
                ) {
                    Text(
                        text = SimpleDateFormat("MMM, yyyy", Locale.getDefault()).format(currentCalendar.time),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Year",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = {
                    // PERBAIKAN: Update state dengan cara yang lebih sederhana
                    if (currentMonth == 11) {
                        currentMonth = 0
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            Button(
                onClick = {
                    val intent = Intent(context, TambahTugas::class.java)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task", fontSize = 12.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Row - PERBAIKAN: Pass currentCalendar sebagai parameter
        CalendarRow(
            currentMonth = currentCalendar,
            selectedDate = selectedDate,
            onDateSelected = { date -> selectedDate = date },
            tugasList = tugasList
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Task Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    searchQuery.isNotEmpty() && selectedDate != null ->
                        "Hasil pencarian \"$searchQuery\" pada ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(selectedDate!!.time)}"
                    searchQuery.isNotEmpty() ->
                        "Hasil pencarian \"$searchQuery\" (${filteredTugas.size})"
                    selectedDate != null ->
                        "Tugas pada ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(selectedDate!!.time)}"
                    else ->
                        "Semua Tugas Aktif (${filteredTugas.size})"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (selectedDate != null || searchQuery.isNotEmpty()) {
                TextButton(onClick = {
                    selectedDate = null
                    searchQuery = ""
                }) {
                    Text("Reset", color = Color(0xFF6366F1))
                }
            }
        }

        // Task List
        if (filteredTugas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            searchQuery.isNotEmpty() -> "Tidak ada tugas yang cocok dengan pencarian \"$searchQuery\""
                            selectedDate != null -> "Tidak ada tugas untuk tanggal ini"
                            else -> "Belum ada tugas aktif"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            searchQuery.isNotEmpty() -> "Coba kata kunci lain atau hapus filter"
                            selectedDate != null -> "Pilih tanggal lain atau tambah tugas baru"
                            else -> "Tambah tugas baru untuk memulai"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTugas) { tugas ->
                    EnhancedTugasItem(
                        tugas = tugas,
                        onEditClick = {
                            val intent = Intent(context, EditTugas::class.java).apply {
                                putExtra("TUGAS_ID", tugas.id)
                                putExtra("NAMA_TUGAS", tugas.namatugas)
                                putExtra("DESKRIPSI", tugas.deskripsi)
                                putExtra("PRIORITAS", tugas.prioritas)
                                putExtra("DEADLINE", tugas.deadline ?: -1L)
                                putExtra("PENGINGAT", tugas.pengingat ?: -1L)
                                putExtra("SELESAI", tugas.selesai)
                            }
                            context.startActivity(intent)
                        },
                        onDeleteClick = {
                            tugasToDelete = tugas
                            showDeleteDialog = true
                        },
                        onCompleteTask = {
                            tugasToComplete = tugas
                            showCompleteDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarRow(
    currentMonth: Calendar,
    selectedDate: Calendar?,
    onDateSelected: (Calendar?) -> Unit,
    tugasList: List<Tugas>
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()

    LazyRow(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daysInMonth) { dayIndex ->
            val day = dayIndex + 1
            val dayCalendar = Calendar.getInstance().apply {
                time = currentMonth.time
                set(Calendar.DAY_OF_MONTH, day)
            }

            val isSelected = selectedDate?.let {
                it.get(Calendar.DAY_OF_MONTH) == day &&
                        it.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                        it.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
            } ?: false

            val isToday = today.get(Calendar.DAY_OF_MONTH) == day &&
                    today.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                    today.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)

            // Check if there are active tasks on this day
            val hasTask = tugasList.any { tugas ->
                tugas.deadline?.let { deadline ->
                    val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                            taskCalendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            taskCalendar.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
                } ?: false
            }

            // Check if there are overdue tasks on this day
            val hasOverdueTask = tugasList.any { tugas ->
                tugas.deadline?.let { deadline ->
                    val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                            taskCalendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            taskCalendar.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                            deadline < System.currentTimeMillis()
                } ?: false
            }

            CalendarDayItem(
                day = day,
                dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(dayCalendar.time),
                isSelected = isSelected,
                isToday = isToday,
                hasTask = hasTask,
                hasOverdueTask = hasOverdueTask,
                onClick = {
                    onDateSelected(if (isSelected) null else dayCalendar)
                }
            )
        }
    }
}

@Composable
fun CalendarDayItem(
    day: Int,
    dayName: String,
    isSelected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    hasOverdueTask: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> Color(0xFF6366F1)
                        isToday -> Color(0xFF6366F1).copy(alpha = 0.3f)
                        hasOverdueTask -> Color(0xFFD32F2F).copy(alpha = 0.3f)
                        hasTask -> Color(0xFF6366F1).copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> Color.White
                    isToday -> Color(0xFF6366F1)
                    hasOverdueTask -> Color(0xFFD32F2F)
                    hasTask -> Color(0xFF6366F1)
                    else -> Color.Black
                },
                fontWeight = when {
                    isSelected || isToday || hasTask || hasOverdueTask -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
            )
        }

        if ((hasTask || hasOverdueTask) && !isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (hasOverdueTask) Color(0xFFD32F2F) else Color(0xFF6366F1))
            )
        }
    }
}

@Composable
fun EnhancedTugasItem(
    tugas: Tugas,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCompleteTask: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) // Added time format
    val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val createdSdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    val priorityColor = when (tugas.prioritas) {
        "Tinggi" -> Color(0xffed1d1d)
        "Sedang" -> Color(0xFF8B5CF6)
        "Rendah" -> Color(0xFF06B6D4)
        else -> Color.Gray
    }

    // Check if task is overdue
    val isOverdue = tugas.deadline?.let { deadline ->
        deadline < System.currentTimeMillis() && !tugas.selesai
    } ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) Color(0xFFFFCDD2) else Color.White // Light red for overdue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            // Priority Color Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        if (isOverdue) Color(0xFFD32F2F) else priorityColor,
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isOverdue) Color(0xFFD32F2F) else priorityColor,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOverdue) Icons.Default.Warning else Icons.Default.Star,
                    contentDescription = "Task",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tugas.namatugas,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) Color(0xFFD32F2F) else Color.Black,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        tugas.deadline?.let { deadline ->
                            Text(
                                text = "${if (isOverdue) "Terlambat: " else "Deadline: "}${sdf.format(Date(deadline))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) Color(0xFFD32F2F) else Color.Gray,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Selesaikan Tugas") },
                                onClick = {
                                    showMenu = false
                                    onCompleteTask()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Tugas") },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Tugas", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            )
                        }
                    }
                }

                // Expanded Content
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (tugas.deskripsi.isNotEmpty()) {
                        Text(
                            text = "Deskripsi:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) Color(0xFFD32F2F) else priorityColor
                        )
                        Text(
                            text = tugas.deskripsi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Prioritas: ${tugas.prioritas}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) Color(0xFFD32F2F) else priorityColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Dibuat: ${createdSdf.format(Date(tugas.tanggalDibuat))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            tugas.pengingat?.let { pengingat ->
                                Text(
                                    text = "Pengingat: ${createdSdf.format(Date(pengingat))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (isOverdue) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "TERLAMBAT",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCompleteTask,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverdue) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isOverdue) "Selesaikan (Terlambat)" else "Selesaikan Tugas",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}