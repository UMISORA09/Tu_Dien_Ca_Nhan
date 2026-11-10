package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var btnSelectRoleUser: LinearLayout
    private lateinit var btnSelectRoleAdmin: LinearLayout
    private lateinit var tvRoleUserText: TextView
    private lateinit var tvRoleAdminText: TextView

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRememberMe: MaterialCheckBox
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnGithub: MaterialButton
    private lateinit var btnGuestMode: AppCompatButton
    private lateinit var tvRegister: TextView

    private var selectedRole = "USER" // "USER" or "ADMIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        selectRole("USER")
    }

    private fun initViews() {
        btnSelectRoleUser = findViewById(R.id.btnSelectRoleUser)
        btnSelectRoleAdmin = findViewById(R.id.btnSelectRoleAdmin)
        tvRoleUserText = findViewById(R.id.tvRoleUserText)
        tvRoleAdminText = findViewById(R.id.tvRoleAdminText)

        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGithub = findViewById(R.id.btnGithub)
        btnGuestMode = findViewById(R.id.btnGuestMode)
        tvRegister = findViewById(R.id.tvRegister)
    }

    private fun setupListeners() {
        btnSelectRoleUser.setOnClickListener {
            selectRole("USER")
        }

        btnSelectRoleAdmin.setOnClickListener {
            selectRole("ADMIN")
        }

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Chức năng quên mật khẩu sẽ gửi mã OTP về email", Toast.LENGTH_SHORT).show()
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Đang kết nối dịch vụ Google Sign-In...", Toast.LENGTH_SHORT).show()
        }

        btnGithub.setOnClickListener {
            Toast.makeText(this, "Đang kết nối dịch vụ GitHub OAuth...", Toast.LENGTH_SHORT).show()
        }

        btnGuestMode.setOnClickListener {
            Toast.makeText(this, "Đang vào ứng dụng với Chế độ Khách (Offline SQLite)...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        val colorWhite = ContextCompat.getColor(this, R.color.white)
        val colorDark = ContextCompat.getColor(this, R.color.admin_slate_dark)

        if (role == "USER") {
            btnSelectRoleUser.setBackgroundResource(R.drawable.bg_tab_admin_active)
            tvRoleUserText.setTextColor(colorWhite)

            btnSelectRoleAdmin.setBackgroundResource(R.drawable.bg_btn_menu_white)
            tvRoleAdminText.setTextColor(colorDark)

            etEmail.setText("xuanhoa.dev@tdc.edu.vn")
            etPassword.setText("12345678")
            btnLogin.text = "➔ Đăng nhập Sổ Tay Học Viên (User)"
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_purple))
        } else {
            btnSelectRoleAdmin.setBackgroundResource(R.drawable.bg_tab_admin_active)
            tvRoleAdminText.setTextColor(colorWhite)

            btnSelectRoleUser.setBackgroundResource(R.drawable.bg_btn_menu_white)
            tvRoleUserText.setTextColor(colorDark)

            etEmail.setText("admin@cassavas.edu.vn")
            etPassword.setText("12345678")
            btnLogin.text = "🛡️ Đăng nhập Quản Trị Hệ Thống (Admin)"
            btnLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.admin_orange))
        }
    }

    private fun handleLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.err_empty_email)
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.err_empty_password)
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (isValid) {
            // Save role into SharedPreferences
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putString("CURRENT_ROLE", selectedRole).apply()

            if (selectedRole == "ADMIN") {
                Toast.makeText(this, "Đăng nhập thành công với quyền Quản trị viên (Admin) 🛡️", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AdminCmsActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

