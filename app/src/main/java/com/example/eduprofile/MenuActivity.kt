package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.materialswitch.MaterialSwitch

class MenuActivity : AppCompatActivity() {

    private lateinit var drawerContainer: LinearLayout
    private lateinit var menuScrollView: NestedScrollView
    private lateinit var viewDismissBackdrop: View
    private lateinit var btnMenuClose: FrameLayout

    // Header User Card
    private lateinit var tvMenuUserName: TextView
    private lateinit var tvMenuUserSub: TextView
    private lateinit var tvMenuStreak: TextView
    private lateinit var tvMenuProgressCount: TextView
    private lateinit var pbMenuStreak: ProgressBar

    // Section 1: Không gian học tập
    private lateinit var menuItemLookup: LinearLayout
    private lateinit var menuItemNotebook: LinearLayout
    private lateinit var menuItemSRS: LinearLayout
    private lateinit var menuItemStats: LinearLayout

    // Section 2: Quản lý dữ liệu & Cài đặt
    private lateinit var menuItemExport: LinearLayout
    private lateinit var menuItemImport: LinearLayout
    private lateinit var menuItemOffline: LinearLayout
    private lateinit var switchOffline: MaterialSwitch
    private lateinit var menuItemReminder: LinearLayout
    private lateinit var switchReminder: MaterialSwitch
    private lateinit var menuItemDarkMode: LinearLayout
    private lateinit var switchDarkMode: MaterialSwitch
    private lateinit var menuItemLogout: LinearLayout

    // Footer
    private lateinit var tvMenuProjectLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        setupWindowInsets()
        initViews()
        setupListeners()
        setupBackHandler()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menuRoot)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            menuScrollView.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        drawerContainer = findViewById(R.id.drawerContainer)
        menuScrollView = findViewById(R.id.menuScrollView)
        viewDismissBackdrop = findViewById(R.id.viewDismissBackdrop)
        btnMenuClose = findViewById(R.id.btnMenuClose)

        tvMenuUserName = findViewById(R.id.tvMenuUserName)
        tvMenuUserSub = findViewById(R.id.tvMenuUserSub)
        tvMenuStreak = findViewById(R.id.tvMenuStreak)
        tvMenuProgressCount = findViewById(R.id.tvMenuProgressCount)
        pbMenuStreak = findViewById(R.id.pbMenuStreak)

        menuItemLookup = findViewById(R.id.menuItemLookup)
        menuItemNotebook = findViewById(R.id.menuItemNotebook)
        menuItemSRS = findViewById(R.id.menuItemSRS)
        menuItemStats = findViewById(R.id.menuItemStats)

        menuItemExport = findViewById(R.id.menuItemExport)
        menuItemImport = findViewById(R.id.menuItemImport)
        menuItemOffline = findViewById(R.id.menuItemOffline)
        switchOffline = findViewById(R.id.switchOffline)
        menuItemReminder = findViewById(R.id.menuItemReminder)
        switchReminder = findViewById(R.id.switchReminder)
        menuItemDarkMode = findViewById(R.id.menuItemDarkMode)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        menuItemLogout = findViewById(R.id.menuItemLogout)

        tvMenuProjectLink = findViewById(R.id.tvMenuProjectLink)
    }

    private fun setupListeners() {
        // Close menu
        btnMenuClose.setOnClickListener {
            closeMenuWithAnimation()
        }

        viewDismissBackdrop.setOnClickListener {
            closeMenuWithAnimation()
        }

        // Section 1: Không gian học tập
        menuItemLookup.setOnClickListener {
            closeMenuWithAnimation()
            startActivity(Intent(this, LookupActivity::class.java))
        }

        menuItemNotebook.setOnClickListener {
            closeMenuWithAnimation()
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        menuItemSRS.setOnClickListener {
            closeMenuWithAnimation()
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        menuItemStats.setOnClickListener {
            Toast.makeText(this, "Phân tích & Tiến độ học: 70% hoàn thành hôm nay", Toast.LENGTH_SHORT).show()
        }

        // Section 2: Quản lý dữ liệu & Cài đặt
        menuItemExport.setOnClickListener {
            Toast.makeText(this, "Xuất dữ liệu: Hỗ trợ tệp CSV, JSON và Anki Deck (.apkg)", Toast.LENGTH_SHORT).show()
        }

        menuItemImport.setOnClickListener {
            Toast.makeText(this, "Nhập bộ từ vựng cá nhân từ tệp CSV", Toast.LENGTH_SHORT).show()
        }

        menuItemOffline.setOnClickListener {
            switchOffline.isChecked = !switchOffline.isChecked
        }

        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) {
                "Đã kích hoạt Từ điển ngoại tuyến (Gói âm thanh US/UK 42MB sẵn sàng)"
            } else {
                "Đã chuyển sang chế độ trực tuyến"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        menuItemReminder.setOnClickListener {
            switchReminder.isChecked = !switchReminder.isChecked
        }

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) {
                "Đã đặt lịch nhắc ôn tập mỗi ngày lúc 20:30 tối"
            } else {
                "Đã tắt thông báo nhắc ôn tập"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        menuItemDarkMode.setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Đã bật Giao diện Ban đêm (Dark Mode)", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Đã bật Giao diện Ban ngày (Light Mode)", Toast.LENGTH_SHORT).show()
            }
        }

        // Đăng xuất
        menuItemLogout.setOnClickListener {
            Toast.makeText(this, "Đã đăng xuất khỏi tài khoản", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        tvMenuProjectLink.setOnClickListener {
            Toast.makeText(this, "Cassavas Lexicon - Đồ Án Mobile v2.5", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeMenuWithAnimation()
            }
        })
    }

    private fun closeMenuWithAnimation() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_out_left)
    }
}

