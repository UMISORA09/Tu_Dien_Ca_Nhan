package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

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
    }

    private fun initViews() {
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
            Toast.makeText(this, "Đang khởi tạo cơ sở dữ liệu Offline SQLite...", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            Toast.makeText(this, "Chuyển sang màn hình Đăng ký tài khoản", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
        }
    }
}

