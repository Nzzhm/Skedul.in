package com.aplikasi.skedulin

data class Tugas(
    val id: String = "",
    val namatugas: String = "",
    val deskripsi: String = "",
    val pembuat: String = "",
    val deadline: Long? = null,
    val pengingat: Long? = null,
    val prioritas: String = "Sedang",
    val selesai: Boolean = false,
    // DAN INI (Gunakan System.currentTimeMillis() untuk default)
    val tanggalDibuat: Long = System.currentTimeMillis(),
    val tanggalSelesai: Long? = null,
    val isCompletedLate: Boolean = false
)