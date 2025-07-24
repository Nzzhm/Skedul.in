package com.aplikasi.skedulin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private lateinit var editfullname : EditText
    private lateinit var editEmail : EditText
    private lateinit var editPassword : EditText
    private lateinit var editPasswordConf : EditText

    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // FIXED: Assign to class variables, don't redeclare
        editfullname = findViewById(R.id.full_name)
        editEmail = findViewById(R.id.email)
        editPassword = findViewById(R.id.password)
        editPasswordConf = findViewById(R.id.password_conf)
        val btnRegister : Button = findViewById(R.id.btn_register)
        val tvLogin : TextView = findViewById(R.id.tv_login)

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            if (editfullname.text.isNotEmpty() && editEmail.text.isNotEmpty() &&
                editPassword.text.isNotEmpty() && editPasswordConf.text.isNotEmpty()) {

                // FIXED: Compare password with confirmation password
                if (editPassword.text.toString() == editPasswordConf.text.toString()) {
                    proccessRegister()
                } else {
                    Toast.makeText(this, "Password Tidak Sama", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Isi semua datanya", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proccessRegister() {
        val fullname = editfullname.text.toString()
        val email = editEmail.text.toString()
        val password = editPassword.text.toString()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userUpdateProfile = userProfileChangeRequest {
                        displayName = fullname
                    }
                    val user = task.result.user
                    user?.updateProfile(userUpdateProfile)
                        ?.addOnSuccessListener {
                            Toast.makeText(this, "Registrasi Berhasil", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        ?.addOnFailureListener { error2 ->
                            Toast.makeText(this, error2.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, task.exception?.localizedMessage ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }
}