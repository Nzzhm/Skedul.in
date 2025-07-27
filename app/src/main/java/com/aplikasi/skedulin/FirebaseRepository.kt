package com.aplikasi.skedulin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

object FirebaseRepository {
    private fun getUserTugasReference() = FirebaseDatabase.getInstance()
        .getReference("tugas")
        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")

    fun addTugas(tugas: Tugas, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userTugasRef = getUserTugasReference()
            val id = userTugasRef.push().key

            if (id != null) {
                userTugasRef.child(id).setValue(tugas.copy(id = id))
                    .addOnSuccessListener {
                        onComplete(true, "Tugas berhasil ditambahkan")
                    }
                    .addOnFailureListener { error ->
                        onComplete(false, "Gagal menambahkan tugas: ${error.message}")
                    }
            } else {
                onComplete(false, "Gagal membuat ID tugas")
            }
        } else {
            onComplete(false, "User belum login, tidak dapat menambah tugas")
        }
    }

    fun getTugas(onDataChange: (List<Tugas>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            println("Error: User belum login, tidak dapat mengambil tugas")
            onDataChange(emptyList())
            return
        }

        try {
            val userTugasRef = getUserTugasReference()

            userTugasRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val tugasList = mutableListOf<Tugas>()
                        snapshot.children.forEach { taskSnapshot ->
                            try {
                                taskSnapshot.getValue(Tugas::class.java)?.let { tugas ->
                                    if (tugas.id.isNotEmpty() && tugas.namatugas.isNotEmpty()) {
                                        tugasList.add(tugas)
                                    } else {
                                        println("Warning: Tugas dengan data tidak lengkap ditemukan: ${taskSnapshot.key}")
                                    }
                                }
                            } catch (e: Exception) {
                                println("Error parsing tugas ${taskSnapshot.key}: ${e.message}")
                            }
                        }

                        // Sort by creation date (newest first)
                        tugasList.sortByDescending { it.tanggalDibuat }

                        onDataChange(tugasList)

                    } catch (e: Exception) {
                        println("Error processing data snapshot: ${e.message}")
                        onDataChange(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Database error: ${error.message}")
                    onDataChange(emptyList())
                }
            })
        } catch (e: Exception) {
            println("Error setting up database listener: ${e.message}")
            onDataChange(emptyList())
        }
    }

    // Get only incomplete tasks (untuk TampilTugas.kt)
    fun getIncompleteTugas(onDataChange: (List<Tugas>) -> Unit) {
        getTugas { tugasList ->
            val incompleteTugas = tugasList.filter { !it.selesai }
                .sortedWith(compareBy<Tugas> {
                    when(it.prioritas) {
                        "Tinggi" -> 1
                        "Sedang" -> 2
                        "Rendah" -> 3
                        else -> 4
                    }
                }.thenBy { it.deadline ?: Long.MAX_VALUE })
            onDataChange(incompleteTugas)
        }
    }

    // Get only completed tasks (untuk HistoryTugas.kt)
    fun getCompletedTugas(onDataChange: (List<Tugas>) -> Unit) {
        getTugas { tugasList ->
            val completedTugas = tugasList.filter { it.selesai }
                .sortedWith(compareByDescending<Tugas> { it.tanggalSelesai ?: 0L }
                    .thenByDescending { it.tanggalDibuat })
            onDataChange(completedTugas)
        }
    }

    fun updateTugas(tugas: Tugas, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugas.id.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()

            // Preserve the original creation time by getting the existing data first
            userTugasRef.child(tugas.id).get().addOnSuccessListener { snapshot ->
                val existingTugas = snapshot.getValue(Tugas::class.java)
                val updatedTugas = if (existingTugas != null) {
                    tugas.copy(tanggalDibuat = existingTugas.tanggalDibuat)
                } else {
                    tugas
                }

                userTugasRef.child(tugas.id).setValue(updatedTugas)
                    .addOnSuccessListener {
                        onComplete(true, "Tugas berhasil diperbarui")
                    }
                    .addOnFailureListener { error ->
                        onComplete(false, "Gagal memperbarui tugas: ${error.message}")
                    }
            }.addOnFailureListener { error ->
                // If can't get existing data, just update with current data
                userTugasRef.child(tugas.id).setValue(tugas)
                    .addOnSuccessListener {
                        onComplete(true, "Tugas berhasil diperbarui")
                    }
                    .addOnFailureListener { updateError ->
                        onComplete(false, "Gagal memperbarui tugas: ${updateError.message}")
                    }
            }
        } else {
            val errorMsg = when {
                currentUser == null -> "User belum login"
                tugas.id.isEmpty() -> "ID tugas tidak valid"
                else -> "Parameter tidak valid"
            }
            onComplete(false, errorMsg)
        }
    }

    fun deleteTugas(tugasId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugasId.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()
            userTugasRef.child(tugasId).removeValue()
                .addOnSuccessListener {
                    onComplete(true, "Tugas berhasil dihapus")
                }
                .addOnFailureListener { error ->
                    onComplete(false, "Gagal menghapus tugas: ${error.message}")
                }
        } else {
            val errorMsg = when {
                currentUser == null -> "User belum login"
                tugasId.isEmpty() -> "ID tugas tidak valid"
                else -> "Parameter tidak valid"
            }
            onComplete(false, errorMsg)
        }
    }

    // Method tambahan untuk mendapatkan tugas berdasarkan ID
    fun getTugasById(tugasId: String, onComplete: (Tugas?, String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugasId.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()
            userTugasRef.child(tugasId).get()
                .addOnSuccessListener { snapshot ->
                    val tugas = snapshot.getValue(Tugas::class.java)
                    onComplete(tugas, null)
                }
                .addOnFailureListener { error ->
                    onComplete(null, "Gagal mengambil data tugas: ${error.message}")
                }
        } else {
            val errorMsg = when {
                currentUser == null -> "User belum login"
                tugasId.isEmpty() -> "ID tugas tidak valid"
                else -> "Parameter tidak valid"
            }
            onComplete(null, errorMsg)
        }
    }

    // Method untuk mendapatkan statistik tugas
    fun getTugasStats(onComplete: (Int, Int, Int) -> Unit) {
        getTugas { tugasList ->
            val totalTugas = tugasList.size
            val tugasSelesai = tugasList.count { it.selesai }
            val tugasBelumSelesai = totalTugas - tugasSelesai
            onComplete(totalTugas, tugasSelesai, tugasBelumSelesai)
        }
    }

    // Method untuk mendapatkan tugas berdasarkan prioritas
    fun getTugasByPrioritas(prioritas: String, onComplete: (List<Tugas>) -> Unit) {
        getTugas { tugasList ->
            val filteredList = tugasList.filter { it.prioritas == prioritas }
            onComplete(filteredList)
        }
    }

    // Method untuk mendapatkan tugas yang deadline-nya sudah lewat (overdue)
    fun getTugasOverdue(onComplete: (List<Tugas>) -> Unit) {
        getIncompleteTugas { tugasList ->
            val currentTime = System.currentTimeMillis()
            val overdueList = tugasList.filter { tugas ->
                tugas.deadline != null &&
                        tugas.deadline < currentTime &&
                        !tugas.selesai
            }
            onComplete(overdueList)
        }
    }

    // Method untuk mendapatkan tugas berdasarkan tanggal deadline (untuk TampilTugas.kt - tugas belum selesai)
    fun getIncompleteTugasByDate(date: Long, onComplete: (List<Tugas>) -> Unit) {
        getIncompleteTugas { tugasList ->
            val filteredList = tugasList.filter { tugas ->
                tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }

                    deadlineCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                            deadlineCalendar.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
                            deadlineCalendar.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
                } ?: false
            }
            onComplete(filteredList)
        }
    }

    // Method untuk mendapatkan tugas yang sudah selesai berdasarkan tanggal deadline (untuk HistoryTugas.kt)
    fun getCompletedTugasByDate(date: Long, onComplete: (List<Tugas>) -> Unit) {
        getCompletedTugas { tugasList ->
            val filteredList = tugasList.filter { tugas ->
                tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }

                    deadlineCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                            deadlineCalendar.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
                            deadlineCalendar.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
                } ?: false
            }
            onComplete(filteredList)
        }
    }

    // Method untuk mendapatkan semua tugas berdasarkan tanggal deadline (untuk keperluan umum)
    fun getTugasByDate(date: Long, onComplete: (List<Tugas>) -> Unit) {
        getTugas { tugasList ->
            val filteredList = tugasList.filter { tugas ->
                tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }

                    deadlineCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                            deadlineCalendar.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
                            deadlineCalendar.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
                } ?: false
            }
            onComplete(filteredList)
        }
    }

    // Method untuk toggle status selesai tugas
    fun toggleTugasStatus(tugasId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        getTugasById(tugasId) { tugas, error ->
            if (tugas != null) {
                val updatedTugas = if (tugas.selesai) {
                    // Jika sudah selesai, kembalikan ke belum selesai dan hapus tanggal selesai
                    tugas.copy(selesai = false, tanggalSelesai = null)
                } else {
                    // Jika belum selesai, tandai sebagai selesai dan set tanggal selesai
                    tugas.copy(selesai = true, tanggalSelesai = System.currentTimeMillis())
                }
                updateTugas(updatedTugas, onComplete)
            } else {
                onComplete(false, error ?: "Tugas tidak ditemukan")
            }
        }
    }

    // Method untuk menandai tugas sebagai selesai
    fun completeTugas(tugasId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        getTugasById(tugasId) { tugas, error ->
            if (tugas != null) {
                val updatedTugas = tugas.copy(
                    selesai = true,
                    tanggalSelesai = System.currentTimeMillis()
                )
                updateTugas(updatedTugas, onComplete)
            } else {
                onComplete(false, error ?: "Tugas tidak ditemukan")
            }
        }
    }

    // Method untuk mengembalikan tugas selesai menjadi belum selesai
    fun incompleteTugas(tugasId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        getTugasById(tugasId) { tugas, error ->
            if (tugas != null) {
                val updatedTugas = tugas.copy(
                    selesai = false,
                    tanggalSelesai = null
                )
                updateTugas(updatedTugas, onComplete)
            } else {
                onComplete(false, error ?: "Tugas tidak ditemukan")
            }
        }
    }

    // Method untuk mendapatkan tugas berdasarkan status dan bulan tertentu
    fun getTugasByMonthAndStatus(year: Int, month: Int, isCompleted: Boolean, onComplete: (List<Tugas>) -> Unit) {
        getTugas { tugasList ->
            val filteredList = tugasList.filter { tugas ->
                val taskHasCorrectStatus = tugas.selesai == isCompleted
                val taskInCorrectMonth = tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    deadlineCalendar.get(Calendar.YEAR) == year &&
                            deadlineCalendar.get(Calendar.MONTH) == month
                } ?: false

                taskHasCorrectStatus && taskInCorrectMonth
            }
            onComplete(filteredList)
        }
    }

    // Method untuk mendapatkan jumlah tugas per hari dalam bulan tertentu
    fun getTugasCountByDaysInMonth(year: Int, month: Int, isCompleted: Boolean, onComplete: (Map<Int, Int>) -> Unit) {
        getTugasByMonthAndStatus(year, month, isCompleted) { tugasList ->
            val dayCountMap = mutableMapOf<Int, Int>()

            tugasList.forEach { tugas ->
                tugas.deadline?.let { deadline ->
                    val deadlineCalendar = Calendar.getInstance().apply { timeInMillis = deadline }
                    val day = deadlineCalendar.get(Calendar.DAY_OF_MONTH)
                    dayCountMap[day] = (dayCountMap[day] ?: 0) + 1
                }
            }

            onComplete(dayCountMap)
        }
    }
}