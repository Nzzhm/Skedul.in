package com.aplikasi.skedulin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        setContentView(R.layout.activity_splash)

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)

        //Load Animation
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.wipe_in_up)
        val titleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in).apply {
            startOffset = 300
        }
        val  subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in).apply {
            startOffset = 400
        }

        logo.startAnimation(logoAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2000)
    }
}