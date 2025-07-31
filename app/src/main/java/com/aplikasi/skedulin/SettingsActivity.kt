package com.aplikasi.skedulin

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private lateinit var textFullName: TextView
    private lateinit var textEmail: TextView
    private lateinit var editProfileCard: CardView
    private lateinit var changePasswordCard: CardView
    private lateinit var helpCard: CardView
    private lateinit var logoutCard: CardView

    // Bottom Navigation
    private lateinit var bottomNavHome: ImageView
    private lateinit var bottomNavCalendar: ImageView
    private lateinit var bottomNavProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_settings)

        initViews()
        setupUserInfo()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initViews() {
        textFullName = findViewById<TextView>(R.id.full_name)
        textEmail = findViewById<TextView>(R.id.email)
        editProfileCard = findViewById<CardView>(R.id.edit_profile_card)
        changePasswordCard = findViewById<CardView>(R.id.change_password_card)
        helpCard = findViewById<CardView>(R.id.help_card)
        logoutCard = findViewById<CardView>(R.id.logout_card)

        // Bottom Navigation
        bottomNavHome = findViewById<ImageView>(R.id.bottom_nav_home)
        bottomNavCalendar = findViewById<ImageView>(R.id.bottom_nav_calendar)
        bottomNavProfile = findViewById<ImageView>(R.id.bottom_nav_profile)
    }

    private fun setupUserInfo() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            textFullName.text = firebaseUser.displayName ?: "User"
            textEmail.text = firebaseUser.email ?: "No Email"
        } else {
            // Redirect to login if user is not authenticated
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Method untuk refresh user info setelah update
    private fun refreshUserInfo() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            // Reload user untuk mendapatkan informasi terbaru
            firebaseUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    textFullName.text = firebaseUser.displayName ?: "User"
                    textEmail.text = firebaseUser.email ?: "No Email"
                }
            }
        }
    }

    private fun setupClickListeners() {
        editProfileCard.setOnClickListener {
            showModernEditUsernameDialog()
        }

        changePasswordCard.setOnClickListener {
            showModernChangePasswordDialog()
        }

        helpCard.setOnClickListener {
            showHelpDialog()
        }

        logoutCard.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        bottomNavCalendar.setOnClickListener {
            startActivity(Intent(this, TampilTugas::class.java))
        }

        // Profile navigation is already active (current page)
        bottomNavProfile.setOnClickListener {
            // Already on profile page, do nothing or refresh
        }
    }

    private fun showModernEditUsernameDialog() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Create custom dialog
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).create()

        // Create main container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
        }

        // Header with icon and title
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(16), 0)
            }
        }

        val titleView = TextView(this).apply {
            text = "Edit Username"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        headerContainer.addView(iconView)
        headerContainer.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "Ubah username Anda untuk personalisasi akun"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(24))
            }
        }

        // Input field with TextInputLayout
        val textInputLayout = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat())
            hint = "Username baru"
            setHintTextColor(ContextCompat.getColorStateList(this@SettingsActivity, R.color.text_secondary))
            setBoxStrokeColor(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(24))
            }
        }

        val editText = TextInputEditText(this).apply {
            setText(currentUser.displayName ?: "")
            setSelection(text?.length ?: 0)
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
        }

        textInputLayout.addView(editText)

        // Character counter
        val counterView = TextView(this).apply {
            text = "${editText.text?.length ?: 0}/20"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(24))
            }
        }

        // Add text watcher for counter
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                counterView.text = "${s?.length ?: 0}/20"
                val color = if ((s?.length ?: 0) > 20) R.color.logout_color else R.color.text_secondary
                counterView.setTextColor(ContextCompat.getColor(this@SettingsActivity, color))
            }
        })

        // Buttons container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelButton = createModernButton("Batal", false)
        val saveButton = createModernButton("Simpan", true)

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val newUsername = editText.text.toString().trim()

            if (newUsername.isEmpty()) {
                textInputLayout.error = "Username tidak boleh kosong"
                return@setOnClickListener
            }

            if (newUsername.length < 3) {
                textInputLayout.error = "Username minimal 3 karakter"
                return@setOnClickListener
            }

            if (newUsername.length > 20) {
                textInputLayout.error = "Username maksimal 20 karakter"
                return@setOnClickListener
            }

            textInputLayout.error = null

            // Show loading
            saveButton.text = "Menyimpan..."
            saveButton.isEnabled = false

            // Update username using FirebaseRepository
            FirebaseRepository.updateUserDisplayName(newUsername) { success, message ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, message ?: "Username berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        refreshUserInfo()
                        dialog.dismiss()
                    } else {
                        textInputLayout.error = message ?: "Gagal memperbarui username"
                        saveButton.text = "Simpan"
                        saveButton.isEnabled = true
                    }
                }
            }
        }

        buttonContainer.addView(cancelButton)
        buttonContainer.addView(saveButton)

        // Add all views to main container
        mainContainer.addView(headerContainer)
        mainContainer.addView(subtitleView)
        mainContainer.addView(textInputLayout)
        mainContainer.addView(counterView)
        mainContainer.addView(buttonContainer)

        dialog.setView(mainContainer)
        dialog.show()

        // Focus and show keyboard
        editText.requestFocus()
    }

    private fun showModernChangePasswordDialog() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Create custom dialog
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).create()

        // Create main container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
        }

        // Header with icon and title
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_lock)
            setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(16), 0)
            }
        }

        val titleView = TextView(this).apply {
            text = "Ubah Password"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        headerContainer.addView(iconView)
        headerContainer.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "Pastikan password baru Anda kuat dan mudah diingat"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(24))
            }
        }

        // Current password field
        val currentPasswordLayout = createPasswordInputLayout("Password saat ini", "Masukkan password lama Anda")
        val currentPasswordEdit = currentPasswordLayout.editText as TextInputEditText

        // New password field
        val newPasswordLayout = createPasswordInputLayout("Password baru", "Minimal 6 karakter")
        val newPasswordEdit = newPasswordLayout.editText as TextInputEditText

        // Confirm password field
        val confirmPasswordLayout = createPasswordInputLayout("Konfirmasi password", "Ulangi password baru")
        val confirmPasswordEdit = confirmPasswordLayout.editText as TextInputEditText

        // Password strength indicator
        val strengthIndicator = TextView(this).apply {
            text = "Kekuatan password: -"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(24))
            }
        }

        // Add password strength checker
        newPasswordEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val strength = getPasswordStrength(password)
                strengthIndicator.text = "Kekuatan password: $strength"

                val color = when(strength) {
                    "Lemah" -> R.color.logout_color
                    "Sedang" -> android.R.color.holo_orange_dark
                    "Kuat" -> android.R.color.holo_green_dark
                    else -> R.color.text_secondary
                }
                strengthIndicator.setTextColor(ContextCompat.getColor(this@SettingsActivity, color))
            }
        })

        // Buttons container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelButton = createModernButton("Batal", false)
        val changeButton = createModernButton("Ubah Password", true)

        cancelButton.setOnClickListener { dialog.dismiss() }

        changeButton.setOnClickListener {
            val currentPassword = currentPasswordEdit.text.toString().trim()
            val newPassword = newPasswordEdit.text.toString().trim()
            val confirmPassword = confirmPasswordEdit.text.toString().trim()

            // Clear previous errors
            currentPasswordLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null

            // Validasi input
            var hasError = false

            if (currentPassword.isEmpty()) {
                currentPasswordLayout.error = "Password saat ini tidak boleh kosong"
                hasError = true
            }

            if (newPassword.isEmpty()) {
                newPasswordLayout.error = "Password baru tidak boleh kosong"
                hasError = true
            } else if (newPassword.length < 6) {
                newPasswordLayout.error = "Password baru minimal 6 karakter"
                hasError = true
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = "Konfirmasi password tidak boleh kosong"
                hasError = true
            } else if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Konfirmasi password tidak cocok"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            // Show loading
            changeButton.text = "Mengubah..."
            changeButton.isEnabled = false

            // Update password using FirebaseRepository
            FirebaseRepository.updateUserPassword(currentPassword, newPassword) { success, message ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, message ?: "Password berhasil diubah", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        if (message?.contains("Password saat ini salah") == true) {
                            currentPasswordLayout.error = message
                        } else {
                            newPasswordLayout.error = message ?: "Gagal mengubah password"
                        }
                        changeButton.text = "Ubah Password"
                        changeButton.isEnabled = true
                    }
                }
            }
        }

        buttonContainer.addView(cancelButton)
        buttonContainer.addView(changeButton)

        // Add all views to main container
        mainContainer.addView(headerContainer)
        mainContainer.addView(subtitleView)
        mainContainer.addView(currentPasswordLayout)
        mainContainer.addView(newPasswordLayout)
        mainContainer.addView(confirmPasswordLayout)
        mainContainer.addView(strengthIndicator)
        mainContainer.addView(buttonContainer)

        dialog.setView(mainContainer)
        dialog.show()

        // Focus on first field
        currentPasswordEdit.requestFocus()
    }

    private fun createPasswordInputLayout(hint: String, helperText: String): TextInputLayout {
        val textInputLayout = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat())
            this.hint = hint
            this.helperText = helperText
            setHintTextColor(ContextCompat.getColorStateList(this@SettingsActivity, R.color.text_secondary))
            setBoxStrokeColor(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            isPasswordVisibilityToggleEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
        }

        val editText = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
        }

        textInputLayout.addView(editText)
        return textInputLayout
    }

    private fun createModernButton(text: String, isPrimary: Boolean): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(48)
            ).apply {
                setMargins(dpToPx(8), 0, 0, 0)
            }
            layoutParams = params

            if (isPrimary) {
                setTextColor(Color.WHITE)
                background = createRoundedBackground(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            } else {
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
                background = createRoundedBackground(Color.TRANSPARENT, ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            }

            setPadding(dpToPx(24), 0, dpToPx(24), 0)
            isAllCaps = false
        }
    }

    private fun createLogoutButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(48)
            ).apply {
                setMargins(dpToPx(8), 0, 0, 0)
            }
            layoutParams = params

            background = createRoundedBackground(ContextCompat.getColor(this@SettingsActivity, R.color.logout_color))
            setPadding(dpToPx(24), 0, dpToPx(24), 0)
            isAllCaps = false
        }
    }

    private fun createRoundedBackground(fillColor: Int, strokeColor: Int = Color.TRANSPARENT): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(24).toFloat()
            setColor(fillColor)
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dpToPx(1), strokeColor)
            }
        }
    }

    private fun getPasswordStrength(password: String): String {
        if (password.length < 6) return "Lemah"

        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }

        return when {
            score >= 3 && password.length >= 8 -> "Kuat"
            score >= 2 -> "Sedang"
            else -> "Lemah"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showHelpDialog() {
        // Create custom dialog
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).create()

        // Create main container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
        }

        // Header with icon and title
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_help) // atau gunakan R.drawable.ic_info
            setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.warna_button))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(16), 0)
            }
        }

        val titleView = TextView(this).apply {
            text = "Bantuan & Panduan"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        headerContainer.addView(iconView)
        headerContainer.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "Panduan lengkap menggunakan aplikasi Skedulin"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(8), 0, dpToPx(24))
            }
        }

        // Create scrollable content
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 0, 0, dpToPx(24))
            }
        }

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Help sections
        val helpSections = listOf(
            HelpSection("🏠 Navigasi Utama", listOf(
                "Home: Halaman utama aplikasi",
                "Calendar: Lihat dan kelola tugas Anda",
                "Profile: Pengaturan akun dan profil"
            )),
            HelpSection("✨ Fitur Utama", listOf(
                "Tambah tugas baru dengan deadline",
                "Lihat daftar tugas yang sudah dibuat",
                "Edit username di pengaturan profil",
                "Ubah password untuk keamanan akun",
                "Notifikasi pengingat tugas",
                "Sinkronisasi dengan akun Firebase"
            )),
            HelpSection("💡 Tips & Trik", listOf(
                "Selalu update aplikasi ke versi terbaru",
                "Pastikan koneksi internet stabil",
                "Username minimal 3 karakter",
                "Password minimal 6 karakter",
                "Gunakan password yang kuat untuk keamanan",
                "Backup data secara berkala"
            ))
        )

        helpSections.forEach { section ->
            contentContainer.addView(createHelpSectionView(section))
        }

        scrollView.addView(contentContainer)

        // Support info card
        val supportCard = CardView(this).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.background_light))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(24))
            }
        }

        val supportContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val supportTitle = TextView(this).apply {
            text = "📞 Butuh Bantuan Lebih?"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val supportDesc = TextView(this).apply {
            text = "Hubungi tim support kami untuk bantuan lebih lanjut"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, 0)
            }
        }

        supportContainer.addView(supportTitle)
        supportContainer.addView(supportDesc)
        supportCard.addView(supportContainer)

        // Buttons container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val supportButton = createModernButton("Kontak Support", false)
        val closeButton = createModernButton("Mengerti", true)

        supportButton.setOnClickListener {
            Toast.makeText(this, "Fitur kontak support akan segera tersedia", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener { dialog.dismiss() }

        buttonContainer.addView(supportButton)
        buttonContainer.addView(closeButton)

        // Add all views to main container
        mainContainer.addView(headerContainer)
        mainContainer.addView(subtitleView)
        mainContainer.addView(scrollView)
        mainContainer.addView(supportCard)
        mainContainer.addView(buttonContainer)

        dialog.setView(mainContainer)
        dialog.show()
    }

    private fun createHelpSectionView(section: HelpSection): View {
        val sectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(20))
            }
        }

        // Section title
        val titleView = TextView(this).apply {
            text = section.title
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
        }

        sectionContainer.addView(titleView)

        // Section items
        section.items.forEach { item ->
            val itemView = TextView(this).apply {
                text = "• $item"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dpToPx(12), 0, 0, dpToPx(4))
                }
            }
            sectionContainer.addView(itemView)
        }

        return sectionContainer
    }

    private fun showLogoutConfirmationDialog() {
        // Create custom dialog
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).create()

        // Create main container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
        }

        // Header with icon and title
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_logout) // atau gunakan R.drawable.ic_exit_to_app
            setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.logout_color))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(16), 0)
            }
        }

        val titleView = TextView(this).apply {
            text = "Konfirmasi Logout"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        headerContainer.addView(iconView)
        headerContainer.addView(titleView)

        // Message container with icon
        val messageContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(24), 0, dpToPx(32))
            }
        }

        val messageView = TextView(this).apply {
            text = "Apakah Anda yakin ingin keluar dari aplikasi?"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
        }

        val warningView = TextView(this).apply {
            text = "Anda akan diminta untuk login kembali saat membuka aplikasi"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
        }

        messageContainer.addView(messageView)
        messageContainer.addView(warningView)

        // Warning card
        val warningCard = CardView(this).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.warning_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(24))
            }
        }

        val warningCardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }

        val warningIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_warning) // atau gunakan R.drawable.ic_info
            setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.warning_color))
            layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                setMargins(0, 0, dpToPx(12), 0)
            }
        }

        val warningText = TextView(this).apply {
            text = "Data lokal akan tetap tersimpan dengan aman"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.warning_text))
        }

        warningCardContainer.addView(warningIcon)
        warningCardContainer.addView(warningText)
        warningCard.addView(warningCardContainer)

        // Buttons container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelButton = createModernButton("Batal", false)
        val logoutButton = createLogoutButton("Ya, Logout")

        cancelButton.setOnClickListener { dialog.dismiss() }

        logoutButton.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        buttonContainer.addView(cancelButton)
        buttonContainer.addView(logoutButton)

        // Add all views to main container
        mainContainer.addView(headerContainer)
        mainContainer.addView(messageContainer)
        mainContainer.addView(warningCard)
        mainContainer.addView(buttonContainer)

        dialog.setView(mainContainer)
        dialog.show()
    }

    private fun performLogout() {
        try {
            firebaseAuth.signOut()
            Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show()

            // Redirect to login activity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user info when activity resumes
        refreshUserInfo()
    }

    // Data class untuk help sections
    data class HelpSection(
        val title: String,
        val items: List<String>
    )
}