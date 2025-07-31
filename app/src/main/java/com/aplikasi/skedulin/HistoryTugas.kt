package com.aplikasi.skedulin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class HistoryTugas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    HistoryTugasScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTugasScreen() {
    val context = LocalContext.current
    var tugasList by remember { mutableStateOf<List<Tugas>>(emptyList()) }
    var allTugasList by remember { mutableStateOf<List<Tugas>>(emptyList()) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }

    // PERBAIKAN: State management yang lebih baik
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    var showYearPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tugasToDelete by remember { mutableStateOf<Tugas?>(null) }
    var showFullCalendar by remember { mutableStateOf(false) }

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

    // Load all tasks (completed and incomplete for calendar view)
    LaunchedEffect(Unit) {
        FirebaseRepository.getTugas { allTugas ->
            allTugasList = allTugas
            tugasList = allTugas.filter { it.selesai }
        }
    }

    // Filter tasks based on search and selected date
    val filteredTugas = remember(tugasList, searchQuery, selectedDate) {
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

        filtered
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

    // Full Calendar Dialog
    if (showFullCalendar) {
        AlertDialog(
            onDismissRequest = { showFullCalendar = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.time))
                    Row {
                        IconButton(onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear -= 1
                            } else {
                                currentMonth -= 1
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                        }
                        IconButton(onClick = {
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
                }
            },
            text = {
                FullCalendarView(
                    currentCalendar = currentCalendar,
                    allTugasList = allTugasList,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        selectedDate = date
                        showFullCalendar = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullCalendar = false }) {
                    Text("Tutup")
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

            Text(
                text = "Riwayat Tugas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Row {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = {
                    selectedDate = null
                    searchQuery = ""
                    isSearchActive = false
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
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

        // Month/Year Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
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
                onClick = { showFullCalendar = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Full Calendar",
                    modifier = Modifier.size(13.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kalender", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Row for History
        HistoryCalendarRow(
            currentMonth = currentCalendar,
            selectedDate = selectedDate,
            onDateSelected = { date -> selectedDate = date },
            tugasList = tugasList,
            allTugasList = allTugasList
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Task Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when {
                        searchQuery.isNotEmpty() && selectedDate != null ->
                            "Hasil pencarian \"$searchQuery\" pada ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(selectedDate!!.time)}"
                        searchQuery.isNotEmpty() ->
                            "Hasil pencarian \"$searchQuery\" (${filteredTugas.size})"
                        selectedDate != null ->
                            "Tugas pada ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(selectedDate!!.time)}"
                        else ->
                            "Semua Tugas Selesai (${filteredTugas.size})"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

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
                        imageVector = if (searchQuery.isNotEmpty()) {
                            Icons.Default.Search
                        } else {
                            ImageVector.vectorResource(id = R.drawable.ic_tugas)
                        },
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            searchQuery.isNotEmpty() -> "Tidak ada tugas yang cocok dengan pencarian \"$searchQuery\""
                            selectedDate != null -> "Tidak ada tugas yang selesai pada tanggal ini"
                            else -> "Belum ada tugas yang diselesaikan"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            searchQuery.isNotEmpty() -> "Coba kata kunci lain atau hapus filter"
                            selectedDate != null -> "Pilih tanggal lain atau lihat semua tugas"
                            else -> "Selesaikan beberapa tugas untuk melihat riwayat"
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
                    CompletedTugasItem(
                        tugas = tugas,
                        onDeleteClick = {
                            tugasToDelete = tugas
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FullCalendarView(
    currentCalendar: Calendar,
    allTugasList: List<Tugas>,
    selectedDate: Calendar?,
    onDateSelected: (Calendar?) -> Unit
) {
    val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = Calendar.getInstance().apply {
        time = currentCalendar.time
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
    val today = Calendar.getInstance()

    Column {
        // Day headers
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")) { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Empty cells for days before first day of month
            items(startDayOfWeek) {
                Box(modifier = Modifier.size(40.dp))
            }

            // Days of month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dayCalendar = Calendar.getInstance().apply {
                    time = currentCalendar.time
                    set(Calendar.DAY_OF_MONTH, day)
                }

                val isSelected = selectedDate?.let {
                    it.get(Calendar.DAY_OF_MONTH) == day &&
                            it.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                            it.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                } ?: false

                val isToday = today.get(Calendar.DAY_OF_MONTH) == day &&
                        today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                        today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)

                // Check task status for this day
                val tasksForDay = allTugasList.filter { tugas ->
                    tugas.deadline?.let { deadline ->
                        val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                        taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                                taskCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                                taskCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                    } ?: false
                }

                val hasCompletedTask = tasksForDay.any { it.selesai }
                val hasCompletedLateTask = tasksForDay.any { tugas ->
                    tugas.selesai && (tugas.isCompletedLate == true ||
                            (tugas.tanggalSelesai != null && tugas.deadline != null &&
                                    tugas.tanggalSelesai!! > tugas.deadline!!))
                }
                val hasOverdueTask = tasksForDay.any { tugas ->
                    !tugas.selesai && tugas.deadline != null && tugas.deadline < System.currentTimeMillis()
                }

                FullCalendarDayItem(
                    day = day,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasCompletedTask = hasCompletedTask,
                    hasCompletedLateTask = hasCompletedLateTask,
                    hasOverdueTask = hasOverdueTask,
                    taskCount = tasksForDay.size,
                    onClick = {
                        onDateSelected(if (isSelected) null else dayCalendar)
                    }
                )
            }
        }
    }
}

@Composable
fun FullCalendarDayItem(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasCompletedTask: Boolean,
    hasCompletedLateTask: Boolean,
    hasOverdueTask: Boolean,
    taskCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> Color(0xFF6366F1)
                    isToday -> Color(0xFF6366F1).copy(alpha = 0.3f)
                    hasOverdueTask -> Color(0xFFD32F2F).copy(alpha = 0.2f)
                    hasCompletedLateTask -> Color(0xFFFF9800).copy(alpha = 0.2f) // Orange for late completion
                    hasCompletedTask -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> Color.White
                    isToday -> Color(0xFF6366F1)
                    hasOverdueTask -> Color(0xFFD32F2F)
                    hasCompletedLateTask -> Color(0xFFFF9800)
                    hasCompletedTask -> Color(0xFF2E7D32)
                    else -> Color.Black
                },
                fontWeight = when {
                    isSelected || isToday || hasCompletedTask || hasOverdueTask || hasCompletedLateTask -> FontWeight.Bold
                    else -> FontWeight.Normal
                },
                fontSize = 12.sp
            )

            if (taskCount > 0 && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hasOverdueTask -> Color(0xFFD32F2F)
                                hasCompletedLateTask -> Color(0xFFFF9800)
                                hasCompletedTask -> Color(0xFF4CAF50)
                                else -> Color(0xFF6366F1)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun HistoryCalendarRow(
    currentMonth: Calendar,
    selectedDate: Calendar?,
    onDateSelected: (Calendar?) -> Unit,
    tugasList: List<Tugas>,
    allTugasList: List<Tugas>
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

            // Check if there are completed tasks on this day
            val hasCompletedTask = tugasList.any { tugas ->
                tugas.deadline?.let { deadline ->
                    val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                            taskCalendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            taskCalendar.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
                } ?: false
            }

            // Check if there are completed late tasks on this day
            val hasCompletedLateTask = tugasList.any { tugas ->
                tugas.deadline?.let { deadline ->
                    val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                            taskCalendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            taskCalendar.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                            (tugas.isCompletedLate == true ||
                                    (tugas.tanggalSelesai != null && tugas.tanggalSelesai!! > deadline))
                } ?: false
            }

            // Check if there are overdue tasks on this day (from all tasks)
            val hasOverdueTask = allTugasList.any { tugas ->
                !tugas.selesai && tugas.deadline?.let { deadline ->
                    val taskCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                            taskCalendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                            taskCalendar.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                            deadline < System.currentTimeMillis()
                } ?: false
            }

            HistoryCalendarDayItem(
                day = day,
                dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(dayCalendar.time),
                isSelected = isSelected,
                isToday = isToday,
                hasCompletedTask = hasCompletedTask,
                hasCompletedLateTask = hasCompletedLateTask,
                hasOverdueTask = hasOverdueTask,
                onClick = {
                    onDateSelected(if (isSelected) null else dayCalendar)
                }
            )
        }
    }
}

@Composable
fun HistoryCalendarDayItem(
    day: Int,
    dayName: String,
    isSelected: Boolean,
    isToday: Boolean,
    hasCompletedTask: Boolean,
    hasCompletedLateTask: Boolean,
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
                        isSelected -> Color(0xFF4CAF50)
                        isToday -> Color(0xFF6366F1).copy(alpha = 0.3f)
                        hasOverdueTask -> Color(0xFFD32F2F).copy(alpha = 0.3f)
                        hasCompletedLateTask -> Color(0xFFFF9800).copy(alpha = 0.3f) // Orange for late completion
                        hasCompletedTask -> Color(0xFF4CAF50).copy(alpha = 0.2f)
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
                    hasCompletedLateTask -> Color(0xFFFF9800)
                    hasCompletedTask -> Color(0xFF2E7D32)
                    else -> Color.Black
                },
                fontWeight = when {
                    isSelected || isToday || hasCompletedTask || hasOverdueTask || hasCompletedLateTask -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
            )
        }

        if ((hasCompletedTask || hasOverdueTask || hasCompletedLateTask) && !isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasOverdueTask -> Color(0xFFD32F2F)
                            hasCompletedLateTask -> Color(0xFFFF9800)
                            hasCompletedTask -> Color(0xFF4CAF50)
                            else -> Color(0xFF4CAF50)
                        }
                    )
            )
        }
    }
}

@Composable
fun CompletedTugasItem(
    tugas: Tugas,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) // Added time format
    val createdSdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    val priorityColor = when (tugas.prioritas) {
        "Tinggi" -> Color(0xFF6366F1)
        "Sedang" -> Color(0xFF8B5CF6)
        "Rendah" -> Color(0xFF06B6D4)
        else -> Color.Gray
    }

    // Check if task was completed late
    val wasCompletedLate = tugas.isCompletedLate == true ||
            (tugas.tanggalSelesai != null && tugas.deadline != null &&
                    tugas.tanggalSelesai!! > tugas.deadline!!)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wasCompletedLate) Color(0xFFFFF3E0) else Color(0xFFF0F9FF) // Light orange for late, light blue for on time
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        if (wasCompletedLate) Color(0xFFFF9800) else priorityColor,
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Completed Task Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (wasCompletedLate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (wasCompletedLate) Icons.Default.Info else Icons.Default.CheckCircle,
                    contentDescription = "Completed",
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
                            color = if (wasCompletedLate) Color(0xFFE65100) else Color(0xFF2E7D32),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        tugas.deadline?.let { deadline ->
                            Text(
                                text = "Deadline: ${sdf.format(Date(deadline))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        tugas.tanggalSelesai?.let { selesai ->
                            Text(
                                text = if (wasCompletedLate)
                                    "Diselesaikan terlambat: ${createdSdf.format(Date(selesai))}"
                                else
                                    "Diselesaikan: ${createdSdf.format(Date(selesai))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (wasCompletedLate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
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
                            color = if (wasCompletedLate) Color(0xFFFF9800) else priorityColor
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
                                color = if (wasCompletedLate) Color(0xFFFF9800) else priorityColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Dibuat: ${createdSdf.format(Date(tugas.tanggalDibuat))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (wasCompletedLate) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (wasCompletedLate) "TERLAMBAT" else "SELESAI",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Tugas", color = Color.White)
                    }
                }
            }
        }
    }
}