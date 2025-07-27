package com.aplikasi.skedulin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

class SettingsActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private lateinit var textfullname : TextView
    private lateinit var textemail : TextView
    private lateinit var btnLogout : Button
    private lateinit var btnTambahTugas: Button
    private lateinit var btnTampilTugas: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        textfullname = findViewById(R.id.full_name)
        textemail = findViewById(R.id.email)
        btnLogout = findViewById(R.id.btn_logout)
        btnTambahTugas = findViewById(R.id.btn_tambahtugas)
        btnTampilTugas = findViewById(R.id.btn_tugas)

        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            textfullname.text = firebaseUser.displayName ?: "No Name"
            textemail.text = firebaseUser.email ?: "No Email"
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnTambahTugas.setOnClickListener {
            startActivity(Intent(this, TambahTugas::class.java))
        }

        btnTampilTugas.setOnClickListener {
            startActivity(Intent(this, TampilTugas::class.java))

        }


        btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
