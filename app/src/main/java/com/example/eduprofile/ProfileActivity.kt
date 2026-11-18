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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

// [SỬA] ProfileActivity nạp thông tin user thật từ Firestore, bảo mật quyền mở Admin CMS
class ProfileActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Header
    // [ĐÃ XÓA]: btnTopMenu (nút hamburger menu góc phải) đã được gỡ bỏ
    private lateinit var tvProfileTitle: TextView
    private lateinit var tvProfileSubtitle: TextView

    // Profile Card
    // [ĐÃ XÓA]: btnSyncNow (nút Đồng bộ ngay) đã được gỡ bỏ vì chuyển sang đồng bộ liên tục tự động
    private lateinit var cardProfile: CardView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvSyncStatus: TextView

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

    // [THÊM] Firebase Auth & Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống Status Bar theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_profile)

        setupWindowInsets()
        initViews()
        setupListeners()
        loadUserProfile()

        tts = TextToSpeech(this, this)
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo Status Bar luôn ẩn khi quay lại màn hình
        hideSystemStatusBar()
        loadUserProfile()
    }

    // [THÊM] Nạp thông tin người dùng từ Firebase Auth và Firestore
    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            tvUserEmail.text = user.email ?: "user@cassavas.edu.vn"
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("displayName") ?: user.displayName ?: "Văn Nguyễn Xuân Hòa"
                        val role = doc.getString("role") ?: "USER"
                        tvUserName.text = name

                        if (role.equals("ADMIN", ignoreCase = true)) {
                            tvUserRole.text = "Quản trị viên (ADMIN) • Chạm để mở CMS"
                            tvUserRole.setOnClickListener {
                                Toast.makeText(this, "Đang mở Bảng Quản trị Admin CMS...", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, AdminCmsActivity::class.java))
                            }
                        } else {
                            tvUserRole.text = "Học viên (USER)"
                            tvUserRole.setOnClickListener {
                                Toast.makeText(this, "Tài khoản của bạn là Học viên (USER). Bảng CMS chỉ dành cho Admin.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        tvUserName.text = user.displayName ?: "Học viên"
                        tvUserRole.text = "Học viên (USER)"
                        tvUserRole.setOnClickListener {
                            Toast.makeText(this, "Tài khoản của bạn là Học viên (USER).", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    tvUserRole.text = "Học viên (USER)"
                }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfile)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // [SỬA]: padTop = 12dp để tiêu đề không bị cấn khi ẩn Status Bar
            val padTop = (12 * resources.displayMetrics.density).toInt()
            findViewById<View>(R.id.scrollContainer).setPadding(
                0,
                padTop,
                0,
                0
            )
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padNavTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padNavTop, padHoriz, systemBars.bottom + padNavTop)
            insets
        }
    }

    private fun initViews() {
        tvProfileTitle = findViewById(R.id.tvProfileTitle)
        tvProfileSubtitle = findViewById(R.id.tvProfileSubtitle)

        cardProfile = findViewById(R.id.cardProfile)
        ivAvatar = findViewById(R.id.ivAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)

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
        // Profile Avatar Tap
        ivAvatar.setOnClickListener {
            val name = tvUserName.text.toString()
            val role = tvUserRole.text.toString()
            Toast.makeText(this, "Hồ sơ: $name ($role)", Toast.LENGTH_SHORT).show()
        }

        // [ĐÃ XÓA]: btnTopMenu.setOnClickListener và btnSyncNow.setOnClickListener
        // [THÊM]: Listener thông báo trạng thái đồng bộ liên tục khi người dùng chạm vào
        tvSyncStatus.setOnClickListener {
            Toast.makeText(this, "Hệ thống đang đồng bộ liên tục với Cloud", Toast.LENGTH_SHORT).show()
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
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập tài khoản để đổi mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

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
            hint = "Mật khẩu mới (tối thiểu 8 ký tự)"
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
            val oldP = etOldPass.text.toString().trim()
            val newP = etNewPass.text.toString().trim()
            val confirmP = etConfirmPass.text.toString().trim()

            if (oldP.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newP.length < 8) {
                Toast.makeText(this, "Mật khẩu mới phải có tối thiểu 8 ký tự", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newP != confirmP) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // [THÊM]: Reauthenticate và cập nhật mật khẩu thật qua Firebase Auth theo PROMPT 2
            val credential = EmailAuthProvider.getCredential(email, oldP)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newP)
                        .addOnSuccessListener {
                            Toast.makeText(this, "🔒 Đã cập nhật mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi cập nhật mật khẩu: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Mật khẩu hiện tại không chính xác", Toast.LENGTH_SHORT).show()
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
                // [THÊM]: Đăng xuất Firebase Auth và xóa các session key cũ theo PROMPT 2
                FirebaseAuth.getInstance().signOut()
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().remove("CURRENT_ROLE").remove("IS_LOGGED_IN").remove("LOGGED_IN_EMAIL").apply()

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
