package com.example.eduprofile

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdminCmsActivity : AppCompatActivity() {

    private lateinit var scrollContainer: NestedScrollView
    private lateinit var btnAdminExit: FrameLayout

    // KPI Cards
    private lateinit var cardKpiPending: LinearLayout
    private lateinit var cardKpiReports: LinearLayout
    private lateinit var tvKpiPendingCount: TextView
    private lateinit var tvKpiReportsCount: TextView

    // Segmented Tabs
    private lateinit var tabPending: TextView
    private lateinit var tabUsers: TextView
    private lateinit var tabPacksIssues: TextView

    // Tab Contents
    private lateinit var layoutContentPending: LinearLayout
    private lateinit var layoutContentUsers: LinearLayout
    private lateinit var layoutContentPacksIssues: LinearLayout

    // Tab 1: Pending Words
    private lateinit var tvHeaderPendingSection: TextView
    private lateinit var btnImportCsv: LinearLayout
    private lateinit var btnAddStandardWord: LinearLayout
    private lateinit var cardPendingWord1: LinearLayout
    private lateinit var cardPendingWord2: LinearLayout
    private lateinit var cardPendingWord3: LinearLayout
    private lateinit var btnApproveWord1: TextView
    private lateinit var btnRejectWord1: TextView
    private lateinit var btnApproveWord2: TextView
    private lateinit var btnRejectWord2: TextView
    private lateinit var btnApproveWord3: TextView
    private lateinit var btnRejectWord3: TextView

    // Tab 2: Users
    private lateinit var etSearchStudent: EditText
    private lateinit var cardUser1: LinearLayout
    private lateinit var cardUser2: LinearLayout
    private lateinit var cardUser3: LinearLayout
    private lateinit var tvUser1Status: TextView
    private lateinit var btnChangeRoleUser1: TextView
    private lateinit var btnToggleBlockUser1: TextView
    private lateinit var btnChangeRoleUser2: TextView
    private lateinit var btnToggleBlockUser2: TextView
    private lateinit var btnChangeRoleUser3: TextView
    private lateinit var btnToggleBlockUser3: TextView

    // Tab 3: Packs & Issues
    private lateinit var btnTogglePack1: TextView
    private lateinit var btnTogglePack2: TextView
    private lateinit var btnTogglePack3: TextView
    private lateinit var tvHeaderIssuesSection: TextView
    private lateinit var btnResolveIssue1: TextView
    private lateinit var btnResolveIssue2: TextView

    // Bottom Navigation
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAdmin: LinearLayout

    // State Variables
    private var pendingWordCount = 3
    private var errorReportCount = 2
    private var isUser1Blocked = false
    private var isUser2Blocked = false
    private var isUser3Blocked = false
    private var currentTab = 0 // 0: Pending, 1: Users, 2: Packs & Issues

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_cms)

        setupWindowInsets()
        initViews()
        setupListeners()
        setupSearch()
        switchTab(0)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAdminCms)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            scrollContainer.setPadding(0, systemBars.top, 0, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }
    }

    private fun initViews() {
        scrollContainer = findViewById(R.id.scrollContainer)
        btnAdminExit = findViewById(R.id.btnAdminExit)

        cardKpiPending = findViewById(R.id.cardKpiPending)
        cardKpiReports = findViewById(R.id.cardKpiReports)
        tvKpiPendingCount = findViewById(R.id.tvKpiPendingCount)
        tvKpiReportsCount = findViewById(R.id.tvKpiReportsCount)

        tabPending = findViewById(R.id.tabPending)
        tabUsers = findViewById(R.id.tabUsers)
        tabPacksIssues = findViewById(R.id.tabPacksIssues)

        layoutContentPending = findViewById(R.id.layoutContentPending)
        layoutContentUsers = findViewById(R.id.layoutContentUsers)
        layoutContentPacksIssues = findViewById(R.id.layoutContentPacksIssues)

        tvHeaderPendingSection = findViewById(R.id.tvHeaderPendingSection)
        btnImportCsv = findViewById(R.id.btnImportCsv)
        btnAddStandardWord = findViewById(R.id.btnAddStandardWord)

        cardPendingWord1 = findViewById(R.id.cardPendingWord1)
        cardPendingWord2 = findViewById(R.id.cardPendingWord2)
        cardPendingWord3 = findViewById(R.id.cardPendingWord3)
        btnApproveWord1 = findViewById(R.id.btnApproveWord1)
        btnRejectWord1 = findViewById(R.id.btnRejectWord1)
        btnApproveWord2 = findViewById(R.id.btnApproveWord2)
        btnRejectWord2 = findViewById(R.id.btnRejectWord2)
        btnApproveWord3 = findViewById(R.id.btnApproveWord3)
        btnRejectWord3 = findViewById(R.id.btnRejectWord3)

        etSearchStudent = findViewById(R.id.etSearchStudent)
        cardUser1 = findViewById(R.id.cardUser1)
        cardUser2 = findViewById(R.id.cardUser2)
        cardUser3 = findViewById(R.id.cardUser3)
        tvUser1Status = findViewById(R.id.tvUser1Status)
        btnChangeRoleUser1 = findViewById(R.id.btnChangeRoleUser1)
        btnToggleBlockUser1 = findViewById(R.id.btnToggleBlockUser1)
        btnChangeRoleUser2 = findViewById(R.id.btnChangeRoleUser2)
        btnToggleBlockUser2 = findViewById(R.id.btnToggleBlockUser2)
        btnChangeRoleUser3 = findViewById(R.id.btnChangeRoleUser3)
        btnToggleBlockUser3 = findViewById(R.id.btnToggleBlockUser3)

        btnTogglePack1 = findViewById(R.id.btnTogglePack1)
        btnTogglePack2 = findViewById(R.id.btnTogglePack2)
        btnTogglePack3 = findViewById(R.id.btnTogglePack3)
        tvHeaderIssuesSection = findViewById(R.id.tvHeaderIssuesSection)
        btnResolveIssue1 = findViewById(R.id.btnResolveIssue1)
        btnResolveIssue2 = findViewById(R.id.btnResolveIssue2)

        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAdmin = findViewById(R.id.navAdmin)
    }

    private fun setupListeners() {
        // Exit
        btnAdminExit.setOnClickListener {
            finish()
        }

        // Tab selection
        tabPending.setOnClickListener { switchTab(0) }
        tabUsers.setOnClickListener { switchTab(1) }
        tabPacksIssues.setOnClickListener { switchTab(2) }

        // KPI card quick clicks
        cardKpiPending.setOnClickListener { switchTab(0) }
        cardKpiReports.setOnClickListener {
            switchTab(2)
            scrollContainer.post {
                scrollContainer.smoothScrollTo(0, tvHeaderIssuesSection.top)
            }
        }

        // Tab 1: Actions
        btnImportCsv.setOnClickListener {
            Toast.makeText(this, "Mở trình nhập dữ liệu CSV/Excel từ điển", Toast.LENGTH_SHORT).show()
        }

        btnAddStandardWord.setOnClickListener {
            val intent = Intent(this, AddWordActivity::class.java)
            startActivity(intent)
        }

        // Word 1 actions
        btnApproveWord1.setOnClickListener {
            approveWord(cardPendingWord1, "Concurrency")
        }
        btnRejectWord1.setOnClickListener {
            rejectWord(cardPendingWord1, "Concurrency")
        }

        // Word 2 actions
        btnApproveWord2.setOnClickListener {
            approveWord(cardPendingWord2, "Zero-day")
        }
        btnRejectWord2.setOnClickListener {
            rejectWord(cardPendingWord2, "Zero-day")
        }

        // Word 3 actions
        btnApproveWord3.setOnClickListener {
            approveWord(cardPendingWord3, "Idempotent")
        }
        btnRejectWord3.setOnClickListener {
            rejectWord(cardPendingWord3, "Idempotent")
        }

        // Tab 2: User management
        btnChangeRoleUser1.setOnClickListener {
            Toast.makeText(this, "Đã cập nhật quyền hạn cho Xuân Hòa", Toast.LENGTH_SHORT).show()
        }
        btnToggleBlockUser1.setOnClickListener {
            isUser1Blocked = !isUser1Blocked
            updateUserBlockState(isUser1Blocked, tvUser1Status, btnToggleBlockUser1, "Văn Nguyễn Xuân Hòa")
        }

        btnChangeRoleUser2.setOnClickListener {
            Toast.makeText(this, "Tài khoản Quản trị viên cấp cao nhất", Toast.LENGTH_SHORT).show()
        }
        btnToggleBlockUser2.setOnClickListener {
            Toast.makeText(this, "Không thể khóa tài khoản Root Admin!", Toast.LENGTH_SHORT).show()
        }

        btnChangeRoleUser3.setOnClickListener {
            Toast.makeText(this, "Đã cập nhật quyền hạn cho Lê Hoàng Nguyên", Toast.LENGTH_SHORT).show()
        }
        btnToggleBlockUser3.setOnClickListener {
            isUser3Blocked = !isUser3Blocked
            val tvStatus = cardUser3.findViewById<TextView>(R.id.tvUser1Status)
            updateUserBlockState(isUser3Blocked, tvStatus, btnToggleBlockUser3, "Lê Hoàng Nguyên")
        }

        // Tab 3: Packs toggle
        btnTogglePack1.setOnClickListener { togglePackState(btnTogglePack1, "600 Từ Vựng TOEIC Cốt Lõi") }
        btnTogglePack2.setOnClickListener { togglePackState(btnTogglePack2, "Tiếng Anh Chuyên Ngành CNTT") }
        btnTogglePack3.setOnClickListener { togglePackState(btnTogglePack3, "Từ Vựng JLPT N3 - N2") }

        // Tab 3: Resolve issues
        btnResolveIssue1.setOnClickListener {
            resolveIssue(btnResolveIssue1, "Articulate")
        }
        btnResolveIssue2.setOnClickListener {
            resolveIssue(btnResolveIssue2, "Idempotent")
        }

        // Bottom Navigation
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
        navAdmin.setOnClickListener {
            scrollContainer.smoothScrollTo(0, 0)
        }
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex

        val colorActiveText = ContextCompat.getColor(this, R.color.white)
        val colorInactiveText = ContextCompat.getColor(this, R.color.admin_slate_sub)

        val bgActive = R.drawable.bg_tab_admin_active
        val bgInactive = R.drawable.bg_tab_admin_inactive

        when (tabIndex) {
            0 -> {
                tabPending.setBackgroundResource(bgActive)
                tabPending.setTextColor(colorActiveText)
                tabUsers.setBackgroundResource(bgInactive)
                tabUsers.setTextColor(colorInactiveText)
                tabPacksIssues.setBackgroundResource(bgInactive)
                tabPacksIssues.setTextColor(colorInactiveText)

                layoutContentPending.visibility = View.VISIBLE
                layoutContentUsers.visibility = View.GONE
                layoutContentPacksIssues.visibility = View.GONE
            }
            1 -> {
                tabPending.setBackgroundResource(bgInactive)
                tabPending.setTextColor(colorInactiveText)
                tabUsers.setBackgroundResource(bgActive)
                tabUsers.setTextColor(colorActiveText)
                tabPacksIssues.setBackgroundResource(bgInactive)
                tabPacksIssues.setTextColor(colorInactiveText)

                layoutContentPending.visibility = View.GONE
                layoutContentUsers.visibility = View.VISIBLE
                layoutContentPacksIssues.visibility = View.GONE
            }
            2 -> {
                tabPending.setBackgroundResource(bgInactive)
                tabPending.setTextColor(colorInactiveText)
                tabUsers.setBackgroundResource(bgInactive)
                tabUsers.setTextColor(colorInactiveText)
                tabPacksIssues.setBackgroundResource(bgActive)
                tabPacksIssues.setTextColor(colorActiveText)

                layoutContentPending.visibility = View.GONE
                layoutContentUsers.visibility = View.GONE
                layoutContentPacksIssues.visibility = View.VISIBLE
            }
        }
    }

    private fun approveWord(card: LinearLayout, word: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Phê duyệt từ vựng")
            .setMessage("Bạn có chắc chắn muốn duyệt từ \"$word\" vào từ điển hệ thống chuẩn của Cassavas?")
            .setPositiveButton("Xác nhận duyệt") { _, _ ->
                card.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {
                        card.visibility = View.GONE
                        pendingWordCount = maxOf(0, pendingWordCount - 1)
                        updatePendingCounters()
                        Toast.makeText(this, "Đã duyệt \"$word\" vào từ điển chuẩn thành công! 🎉", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun rejectWord(card: LinearLayout, word: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Từ chối đóng góp")
            .setMessage("Từ chối từ \"$word\" từ người đóng góp?")
            .setPositiveButton("Từ chối") { _, _ ->
                card.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {
                        card.visibility = View.GONE
                        pendingWordCount = maxOf(0, pendingWordCount - 1)
                        updatePendingCounters()
                        Toast.makeText(this, "Đã từ chối từ vựng \"$word\".", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updatePendingCounters() {
        tvKpiPendingCount.text = "$pendingWordCount từ"
        tabPending.text = "Duyệt từ ($pendingWordCount)"
        tvHeaderPendingSection.text = "ĐÓNG GÓP CHỜ PHÊ DUYỆT ($pendingWordCount)"
    }

    private fun updateUserBlockState(isBlocked: Boolean, tvStatus: TextView?, btnToggle: TextView, userName: String) {
        if (isBlocked) {
            tvStatus?.text = getString(R.string.admin_status_blocked)
            tvStatus?.setBackgroundResource(R.drawable.bg_badge_admin_status_red)
            tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.admin_red_text))
            btnToggle.text = getString(R.string.admin_btn_unblock_user)
            Toast.makeText(this, "Đã khóa tài khoản $userName", Toast.LENGTH_SHORT).show()
        } else {
            tvStatus?.text = getString(R.string.admin_status_active)
            tvStatus?.setBackgroundResource(R.drawable.bg_badge_admin_status_green)
            tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.admin_green_text))
            btnToggle.text = getString(R.string.admin_btn_block_user)
            Toast.makeText(this, "Đã mở khóa tài khoản $userName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePackState(btnToggle: TextView, packName: String) {
        val currentText = btnToggle.text.toString()
        if (currentText.contains("Đang phát hành")) {
            btnToggle.text = getString(R.string.admin_status_draft)
            btnToggle.setBackgroundResource(R.drawable.bg_tab_admin_inactive)
            btnToggle.setTextColor(ContextCompat.getColor(this, R.color.admin_slate_sub))
            Toast.makeText(this, "Đã chuyển gói \"$packName\" về bản nháp", Toast.LENGTH_SHORT).show()
        } else {
            btnToggle.text = getString(R.string.admin_status_published)
            btnToggle.setBackgroundResource(R.drawable.bg_badge_admin_status_green)
            btnToggle.setTextColor(ContextCompat.getColor(this, R.color.admin_green_text))
            Toast.makeText(this, "Đã phát hành gói \"$packName\" cho toàn bộ học viên", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveIssue(btnResolve: TextView, word: String) {
        if (btnResolve.text.toString().contains("Đã khắc phục")) {
            Toast.makeText(this, "Báo lỗi này đã được xử lý xong", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Xử lý báo lỗi từ vựng")
            .setMessage("Cập nhật phiên âm / giải nghĩa cho từ \"$word\" theo phản hồi của học viên?")
            .setPositiveButton("Đã chỉnh sửa & Hoàn tất") { _, _ ->
                btnResolve.text = getString(R.string.admin_status_resolved)
                btnResolve.setTextColor(ContextCompat.getColor(this, R.color.admin_green_btn))
                errorReportCount = maxOf(0, errorReportCount - 1)
                tvKpiReportsCount.text = "$errorReportCount lỗi"
                tvHeaderIssuesSection.text = "BÁO LỖI NỘI DUNG TỪ HỌC VIÊN ($errorReportCount)"
                Toast.makeText(this, "Đã cập nhật và sửa lỗi từ \"$word\"! ✓", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Để sau", null)
            .show()
    }

    private fun setupSearch() {
        etSearchStudent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                filterUsers(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterUsers(query: String) {
        if (query.isEmpty()) {
            cardUser1.visibility = View.VISIBLE
            cardUser2.visibility = View.VISIBLE
            cardUser3.visibility = View.VISIBLE
            return
        }

        val match1 = "văn nguyễn xuân hòa".contains(query) || "xuanhoa.dev@tdc.edu.vn".contains(query)
        val match2 = "quản trị hệ thống".contains(query) || "admin@cassavas.edu.vn".contains(query)
        val match3 = "lê hoàng nguyên".contains(query) || "nguyen.le@tdc.edu.vn".contains(query)

        cardUser1.visibility = if (match1) View.VISIBLE else View.GONE
        cardUser2.visibility = if (match2) View.VISIBLE else View.GONE
        cardUser3.visibility = if (match3) View.VISIBLE else View.GONE
    }
}

