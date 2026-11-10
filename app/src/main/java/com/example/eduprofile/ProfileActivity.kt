package com.example.eduprofile

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class ProfileActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Header
    private lateinit var tvProfileTitle: TextView
    private lateinit var tvProfileSubtitle: TextView
    private lateinit var btnTopMenu: FrameLayout

    // Profile Card
    private lateinit var cardProfile: CardView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvSyncStatus: TextView
    private lateinit var btnSyncNow: LinearLayout

    // TTS & Learning Settings
    private lateinit var cardSettingsTTS: CardView
    private lateinit var pillVoiceUS: LinearLayout
    private lateinit var pillVoiceUK: LinearLayout
    private lateinit var tvVoiceUS: TextView
    private lateinit var tvVoiceUK: TextView
    private lateinit var switchAlarmSM2: SwitchMaterial
    private lateinit var btnExportFile: LinearLayout

    // Security & Session
    private lateinit var cardSecurity: CardView
    private lateinit var layoutChangePassword: LinearLayout
    private lateinit var btnLogout: LinearLayout

    // Bottom Navigation (6 Tabs)
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    // State
    private var isVoiceUS = true
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        setupWindowInsets()
        initViews()
        setupListeners()

        tts = TextToSpeech(this, this)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfile)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.scrollContainer).setPadding(
                0,
                systemBars.top,
                0,
                0
            )
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }
    }

    private fun initViews() {
        tvProfileTitle = findViewById(R.id.tvProfileTitle)
        tvProfileSubtitle = findViewById(R.id.tvProfileSubtitle)
        btnTopMenu = findViewById(R.id.btnTopMenu)

        cardProfile = findViewById(R.id.cardProfile)
        ivAvatar = findViewById(R.id.ivAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        btnSyncNow = findViewById(R.id.btnSyncNow)

        cardSettingsTTS = findViewById(R.id.cardSettingsTTS)
        pillVoiceUS = findViewById(R.id.pillVoiceUS)
        pillVoiceUK = findViewById(R.id.pillVoiceUK)
        tvVoiceUS = findViewById(R.id.tvVoiceUS)
        tvVoiceUK = findViewById(R.id.tvVoiceUK)
        switchAlarmSM2 = findViewById(R.id.switchAlarmSM2)
        btnExportFile = findViewById(R.id.btnExportFile)

        cardSecurity = findViewById(R.id.cardSecurity)
        layoutChangePassword = findViewById(R.id.layoutChangePassword)
        btnLogout = findViewById(R.id.btnLogout)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupListeners() {
        // Top Menu Button -> MenuActivity
        btnTopMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Profile Avatar Tap
        ivAvatar.setOnClickListener {
            Toast.makeText(this, "Hồ sơ: Văn Nguyễn Xuân Hòa (Học viên C1)", Toast.LENGTH_SHORT).show()
        }

        // Sync Now Button
        btnSyncNow.setOnClickListener {
            val rotate = ObjectAnimator.ofFloat(btnSyncNow, "rotation", 0f, 360f).apply {
                duration = 600
                interpolator = AccelerateDecelerateInterpolator()
            }
            rotate.start()

            tvSyncStatus.text = "🔄 Đang đồng bộ dữ liệu..."
            btnSyncNow.isEnabled = false

            handler.postDelayed({
                tvSyncStatus.text = "☁ Vừa xong (SQLite ↔ Cloud)"
                btnSyncNow.isEnabled = true
                Toast.makeText(this, "✅ Đã đồng bộ 8 từ vựng với Cloud thành công!", Toast.LENGTH_SHORT).show()
            }, 1000)
        }

        // TTS Voice Accent Switchers
        pillVoiceUS.setOnClickListener {
            if (!isVoiceUS) {
                setVoiceAccent(isUS = true)
            }
        }

        pillVoiceUK.setOnClickListener {
            if (isVoiceUS) {
                setVoiceAccent(isUS = false)
            }
        }

        // Switch Alarm Notification SM-2
        switchAlarmSM2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(
                    this,
                    "⏰ Đã bật nhắc nhở ôn tập SM-2 vào lúc 20:30 mỗi tối!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Đã tắt thông báo nhắc ôn tập", Toast.LENGTH_SHORT).show()
            }
        }

        // Export Database (.CSV / .JSON)
        btnExportFile.setOnClickListener {
            showExportDialog()
        }

        // Change Password
        layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logout Button
        btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        // Bottom Navigation (6 Tabs)
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
            finish()
        }

        navNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
            finish()
        }

        navSRS.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
            finish()
        }

        navStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
            finish()
        }

        navAccount.setOnClickListener {
            Toast.makeText(this, "Bạn đang ở màn hình Tài khoản & Cài đặt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setVoiceAccent(isUS: Boolean) {
        isVoiceUS = isUS
        if (isUS) {
            pillVoiceUS.setBackgroundResource(R.drawable.bg_pill_voice_active)
            tvVoiceUS.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            pillVoiceUK.setBackgroundResource(R.drawable.bg_pill_voice_inactive)
            tvVoiceUK.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            if (isTtsReady) {
                tts?.language = Locale.US
                tts?.speak("Default voice set to American English", TextToSpeech.QUEUE_FLUSH, null, "TTS_US")
            }
            Toast.makeText(this, "Đã chọn giọng đọc chuẩn Mỹ (US)", Toast.LENGTH_SHORT).show()
        } else {
            pillVoiceUK.setBackgroundResource(R.drawable.bg_pill_voice_active)
            tvVoiceUK.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            pillVoiceUS.setBackgroundResource(R.drawable.bg_pill_voice_inactive)
            tvVoiceUS.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            if (isTtsReady) {
                tts?.language = Locale.UK
                tts?.speak("Default voice set to British English", TextToSpeech.QUEUE_FLUSH, null, "TTS_UK")
            }
            Toast.makeText(this, "Đã chọn giọng đọc chuẩn Anh (UK)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportDialog() {
        val options = arrayOf("Xuất toàn bộ từ điển (.CSV)", "Xuất dữ liệu ôn tập SM-2 (.JSON)")
        MaterialAlertDialogBuilder(this)
            .setTitle("Sao lưu & Xuất từ điển")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "📁 Đã xuất file: TuDienCaNhan_Backup.csv (8 từ)", Toast.LENGTH_LONG).show()
                    1 -> Toast.makeText(this, "📁 Đã xuất file: SRS_Vocabulary_Data.json (2.4 MB)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Đổi mật khẩu tài khoản")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val etOldPass = EditText(this).apply {
            hint = "Mật khẩu hiện tại"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPass = EditText(this).apply {
            hint = "Mật khẩu mới (tối thiểu 6 ký tự)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirmPass = EditText(this).apply {
            hint = "Xác nhận mật khẩu mới"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etOldPass)
        layout.addView(etNewPass)
        layout.addView(etConfirmPass)

        builder.setView(layout)
        builder.setPositiveButton("Cập nhật") { _, _ ->
            val newP = etNewPass.text.toString().trim()
            val confirmP = etConfirmPass.text.toString().trim()
            if (newP.isEmpty() || confirmP.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show()
            } else if (newP != confirmP) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🔒 Đã cập nhật mật khẩu thành công!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Hủy", null)
        builder.show()
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản trên thiết bị này?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                Toast.makeText(this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

