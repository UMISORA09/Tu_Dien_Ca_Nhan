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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    // [ĐÃ XÓA]: btnSelectRoleUser, btnSelectRoleAdmin, tvRoleUserText, tvRoleAdminText, cbRememberMe, selectedRole theo PROMPT 2

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvDemoAccount: TextView
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnGithub: MaterialButton
    private lateinit var btnGuestMode: AppCompatButton
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [THÊM]: Ghi nhớ đăng nhập bằng phiên Firebase Auth thật theo PROMPT 2
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
            return
        }

        setupLoginScreen()
    }

    private fun setupLoginScreen() {
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
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvDemoAccount = findViewById(R.id.tvDemoAccount)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGithub = findViewById(R.id.btnGithub)
        btnGuestMode = findViewById(R.id.btnGuestMode)
        tvRegister = findViewById(R.id.tvRegister)
    }

    private fun setupListeners() {
        // [THÊM]: Bấm dòng demo thì điền tài khoản User demo xuanhoa.dev@tdc.edu.vn / 12345678
        tvDemoAccount.setOnClickListener {
            etEmail.setText("xuanhoa.dev@tdc.edu.vn")
            etPassword.setText("12345678")
            tilEmail.error = null
            tilPassword.error = null
            Toast.makeText(this, "Đã điền tài khoản Học viên demo (User)", Toast.LENGTH_SHORT).show()
        }

        // Nhấn giữ dòng demo để điền tài khoản Admin demo admin@cassavas.edu.vn / 12345678
        tvDemoAccount.setOnLongClickListener {
            etEmail.setText("admin@cassavas.edu.vn")
            etPassword.setText("12345678")
            tilEmail.error = null
            tilPassword.error = null
            Toast.makeText(this, "Đã điền tài khoản Quản trị demo (Admin)", Toast.LENGTH_SHORT).show()
            true
        }

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            if (email.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã gửi email đặt lại mật khẩu đến $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi gửi email đặt lại mật khẩu: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Vui lòng nhập email để nhận liên kết đặt lại mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Đang kết nối dịch vụ Google Sign-In...", Toast.LENGTH_SHORT).show()
        }

        btnGithub.setOnClickListener {
            Toast.makeText(this, "Đang kết nối dịch vụ GitHub OAuth...", Toast.LENGTH_SHORT).show()
        }

        // [SỬA]: Chế độ Khách theo PROMPT 2: vào HomeActivity với vai trò khách (không có tài khoản Auth)
        btnGuestMode.setOnClickListener {
            Toast.makeText(this, "Đang vào ứng dụng với Chế độ Khách...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("IS_GUEST", true)
            startActivity(intent)
            finish()
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // [ĐÃ XÓA]: selectRole() dòng 111-139 theo PROMPT 2 vì role chỉ đọc từ Firestore users/{uid}.role

    // [SỬA]: Viết lại handleLogin() theo Firebase Auth thật và đọc role từ Firestore
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

        if (!isValid) return

        btnLogin.isEnabled = false
        btnLogin.text = "Đang đăng nhập..."

        // [ĐÃ XÓA]: SharedPreferences putString("CURRENT_ROLE") cũ
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.btn_login_notebook)

                            if (doc != null && doc.exists()) {
                                val isBlocked = doc.getBoolean("blocked") ?: false
                                if (isBlocked) {
                                    FirebaseAuth.getInstance().signOut()
                                    Toast.makeText(this, "Tài khoản của bạn đã bị khóa bởi Quản trị viên", Toast.LENGTH_LONG).show()
                                    return@addOnSuccessListener
                                }

                                val role = doc.getString("role") ?: "USER"
                                if (role == "ADMIN") {
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
                            } else {
                                // Nếu document chưa có field role, mặc định là USER
                                Toast.makeText(this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.btn_login_notebook)
                            Toast.makeText(this, "Không tải được thông tin tài khoản, thử lại", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.btn_login_notebook)
                tilPassword.error = "Email hoặc mật khẩu không đúng"
            }
    }

    // [THÊM]: Hàm kiểm tra role và điều hướng tự động khi có phiên đăng nhập sẵn
    private fun checkUserRoleAndNavigate(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val isBlocked = doc.getBoolean("blocked") ?: false
                    if (isBlocked) {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(this, "Tài khoản của bạn đã bị khóa", Toast.LENGTH_LONG).show()
                        setupLoginScreen()
                        return@addOnSuccessListener
                    }

                    val role = doc.getString("role") ?: "USER"
                    if (role == "ADMIN") {
                        startActivity(Intent(this, AdminCmsActivity::class.java))
                    } else {
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Không tải được thông tin tài khoản, thử lại", Toast.LENGTH_SHORT).show()
                setupLoginScreen()
            }
    }
}
