package com.example.eduprofile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var llBack: View
    private lateinit var tvNavLogin: TextView

    private lateinit var tilFullname: TextInputLayout
    private lateinit var etFullname: TextInputEditText

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText

    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText

    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var cgLevel: ChipGroup
    private lateinit var cgDailyGoal: ChipGroup
    private lateinit var cbAgreeTerms: MaterialCheckBox
    private lateinit var btnRegister: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        llBack = findViewById(R.id.llBack)
        tvNavLogin = findViewById(R.id.tvNavLogin)
        tilFullname = findViewById(R.id.tilFullname)
        etFullname = findViewById(R.id.etFullname)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cgLevel = findViewById(R.id.cgLevel)
        cgDailyGoal = findViewById(R.id.cgDailyGoal)
        cbAgreeTerms = findViewById(R.id.cbAgreeTerms)
        btnRegister = findViewById(R.id.btnRegister)
    }

    private fun setupListeners() {
        llBack.setOnClickListener {
            finish()
        }

        tvNavLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val fullname = etFullname.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString()?.trim().orEmpty()
        val isTermsAgreed = cbAgreeTerms.isChecked

        var isValid = true

        if (fullname.isEmpty()) {
            tilFullname.error = getString(R.string.err_empty_fullname)
            isValid = false
        } else {
            tilFullname.error = null
        }

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.err_empty_email)
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.length < 8) {
            tilPassword.error = getString(R.string.err_password_short)
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (confirmPassword != password) {
            tilConfirmPassword.error = getString(R.string.err_password_not_match)
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        if (!isTermsAgreed) {
            Toast.makeText(this, getString(R.string.err_terms_not_accepted), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            val selectedLevelId = cgLevel.checkedChipId
            val selectedLevel = if (selectedLevelId != View.NO_ID) {
                findViewById<Chip>(selectedLevelId)?.text?.toString().orEmpty()
            } else ""

            val selectedGoalId = cgDailyGoal.checkedChipId
            val selectedGoal = if (selectedGoalId != View.NO_ID) {
                findViewById<Chip>(selectedGoalId)?.text?.toString().orEmpty()
            } else ""

            Toast.makeText(
                this,
                "${getString(R.string.msg_register_success)}\nTrình độ: $selectedLevel - Mục tiêu: $selectedGoal",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }
}

