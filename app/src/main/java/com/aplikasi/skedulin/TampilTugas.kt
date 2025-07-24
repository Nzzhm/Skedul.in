package com.aplikasi.skedulin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*

class TampilTugas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Jika menggunakan Navigation, pastikan NavController tersedia
                    // TampilTugasScreen(navController = navController)
                    TampilTugasScreenStandalone()
                }
            }
        }
    }
}

/**
 * Fungsi helper untuk memformat Timestamp menjadi string yang mudah dibaca
 */
fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    return if (timestamp != null) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(timestamp.toDate())
    } else {
        "Tidak diatur"
    }
}

/**
 * Versi standalone dari TampilTugasScreen (tanpa NavController)
 * Gunakan ini jika tidak menggunakan Navigation Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TampilTugasScreenStandalone() {
    // State untuk menyimpan list tugas yang diambil dari Firebase
    val tugasList = remember { mutableStateListOf<Tugas>() }

    // State untuk menampilkan loading indicator
    var isLoading by remember { mutableStateOf(true) }

    // Ambil data tugas saat komposisi pertama kali
    LaunchedEffect(Unit) {
        FirebaseRepository.getTugas { tugasFromFirebase ->
            tugasList.clear()
            tugasList.addAll(tugasFromFirebase)
            isLoading = false // Matikan loading setelah data berhasil dimuat
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Tugas Saya") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Implementasi navigasi ke halaman tambah tugas
                    // navController.navigate("tambah_tugas")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                isLoading -> {
                    // Tampilkan loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                tugasList.isEmpty() -> {
                    // Tampilkan pesan jika tidak ada tugas
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum ada tugas",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap tombol + untuk menambah tugas baru",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    // Tampilkan daftar tugas
                    LazyColumn {
                        items(tugasList) { tugas ->
                            TugasCard(
                                tugas = tugas,
                                onTugasClick = {
                                    // TODO: Implementasi aksi ketika tugas diklik
                                    // Misalnya navigasi ke detail atau edit tugas
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Versi dengan NavController untuk digunakan dengan Navigation Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TampilTugasScreen(navController: NavHostController) {
    val tugasList = remember { mutableStateListOf<Tugas>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseRepository.getTugas { tugasFromFirebase ->
            tugasList.clear()
            tugasList.addAll(tugasFromFirebase)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Tugas Saya") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigasi ke halaman tambah tugas
                    navController.navigate("tambah_tugas")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                tugasList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum ada tugas",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap tombol + untuk menambah tugas baru",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn {
                        items(tugasList) { tugas ->
                            TugasCard(
                                tugas = tugas,
                                onTugasClick = {
                                    // Navigasi ke detail tugas atau edit
                                    // navController.navigate("detail_tugas/${tugas.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Card untuk menampilkan satu tugas
 * Dipecah menjadi fungsi terpisah untuk reusability dan readability
 */
@Composable
fun TugasCard(
    tugas: Tugas,
    onTugasClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onTugasClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header dengan nama tugas dan prioritas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tugas.namatugas,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Badge prioritas
                Surface(
                    color = when (tugas.prioritas) {
                        "Tinggi" -> Color(0xFFFF5722)
                        "Sedang" -> Color(0xFFFF9800)
                        "Rendah" -> Color(0xFF4CAF50)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = tugas.prioritas,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Deskripsi tugas dalam kotak dengan border
            if (tugas.deskripsi.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFF42A5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"${tugas.deskripsi}\"",
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer dengan deadline dan pengingat
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deadline: ${formatTimestamp(tugas.deadline)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengingat: ${formatTimestamp(tugas.pengingat)}",
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )

                    // Status selesai
                    if (tugas.selesai) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Selesai",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}