package com.aplikasi.skedulin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseRepository {
    // Mendapatkan referensi database yang sesuai dengan user saat ini
    private fun getUserTugasReference() = FirebaseDatabase.getInstance()
        .getReference("tugas")
        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")

    /**
     * Menambahkan tugas baru untuk user yang sedang login
     * Tugas akan disimpan di path: tugas/userId/taskId
     */
    fun addTugas(tugas: Tugas) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userTugasRef = getUserTugasReference()
            val id = userTugasRef.push().key!! // Generate ID unik dari Firebase

            // Simpan tugas dengan ID yang baru di-generate
            userTugasRef.child(id).setValue(tugas.copy(id = id))
        } else {
            println("Error: User belum login, tidak dapat menambah tugas")
        }
    }

    /**
     * Mengambil semua tugas milik user yang sedang login
     * Hanya menampilkan tugas dari path: tugas/userId/
     */
    fun getTugas(onDataChange: (List<Tugas>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userTugasRef = getUserTugasReference()

            userTugasRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tugasList = mutableListOf<Tugas>()

                    // Iterasi melalui semua tugas milik user ini
                    snapshot.children.forEach { taskSnapshot ->
                        taskSnapshot.getValue(Tugas::class.java)?.let { tugas ->
                            tugasList.add(tugas)
                        }
                    }

                    // Callback dengan list tugas yang sudah difilter per user
                    onDataChange(tugasList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Tangani error jika pengambilan data dibatalkan
                    println("Database error: ${error.message}")
                    onDataChange(emptyList()) // Return list kosong jika error
                }
            })
        } else {
            println("Error: User belum login, tidak dapat mengambil tugas")
            onDataChange(emptyList()) // Return list kosong jika user belum login
        }
    }

    /**
     * Mengupdate tugas tertentu milik user yang sedang login
     */
    fun updateTugas(tugas: Tugas) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugas.id.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()
            userTugasRef.child(tugas.id).setValue(tugas)
                .addOnSuccessListener {
                    println("Tugas berhasil diupdate: ${tugas.namatugas}")
                }
                .addOnFailureListener { error ->
                    println("Gagal mengupdate tugas: ${error.message}")
                }
        }
    }

    /**
     * Menghapus tugas tertentu milik user yang sedang login
     */
    fun deleteTugas(tugasId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugasId.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()
            userTugasRef.child(tugasId).removeValue()
                .addOnSuccessListener {
                    println("Tugas berhasil dihapus: $tugasId")
                }
                .addOnFailureListener { error ->
                    println("Gagal menghapus tugas: ${error.message}")
                }
        }
    }

    /**
     * Mengambil tugas berdasarkan ID tertentu milik user yang sedang login
     */
    fun getTugasById(tugasId: String, onDataChange: (Tugas?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tugasId.isNotEmpty()) {
            val userTugasRef = getUserTugasReference()
            userTugasRef.child(tugasId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tugas = snapshot.getValue(Tugas::class.java)
                    onDataChange(tugas)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Database error: ${error.message}")
                    onDataChange(null)
                }
            })
        } else {
            onDataChange(null)
        }
    }
}