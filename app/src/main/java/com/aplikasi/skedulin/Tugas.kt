package com.aplikasi.skedulin

import com.google.firebase.Timestamp
data class Tugas(
    val id: String = "",
    val namatugas: String = "",
    val deskripsi: String = "",
    val pembuat: String = "", // Diubah dari pembuatUid menjadi pembuat (String untuk nama)
    val deadline: Timestamp? = null,
    val pengingat: Timestamp? = null,
    val prioritas: String = "Sedang",
    val selesai: Boolean = false,
    val tanggalDibuat: Timestamp = Timestamp.now()
)