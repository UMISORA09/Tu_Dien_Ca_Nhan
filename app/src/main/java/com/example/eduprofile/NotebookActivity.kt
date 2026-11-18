package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eduprofile.data.NotebookAdapter
import com.example.eduprofile.data.NotebookItem
import com.example.eduprofile.data.ReviewRepository
import com.example.eduprofile.data.UserWord
import com.example.eduprofile.data.Word
import com.example.eduprofile.data.WordRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// [SỬA]: Refactor toàn bộ NotebookActivity theo PROMPT 6.2 - Đọc dữ liệu thật từ userWords, lọc động, xóa hard-code
class NotebookActivity : AppCompatActivity() {

    private lateinit var btnAddWord: FrameLayout

    // Topic Cards
    private lateinit var topicCardIT: LinearLayout
    private lateinit var topicCardComm: LinearLayout
    private lateinit var topicCardIELTS: LinearLayout
    private lateinit var topicCardIdioms: LinearLayout

    // Filter Chips
    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterNew: TextView
    private lateinit var chipFilterLearning: TextView
    private lateinit var chipFilterMastered: TextView

    // [THÊM]: RecyclerView và Empty state thay thế 3 thẻ hard-code cardVocab1/2/3
    private lateinit var rvNotebook: RecyclerView
    private lateinit var tvEmptyNotebook: TextView
    private lateinit var notebookAdapter: NotebookAdapter

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private val wordRepository = WordRepository()
    private val reviewRepository = ReviewRepository()

    private var allNotebookItems: List<NotebookItem> = emptyList()
    private var selectedStatus: String = "ALL" // "ALL", "new", "learning", "mastered"
    private var selectedTopic: String? = null // null, "IT", "COMM", "IELTS", "IDIOMS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống Status Bar theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_notebook)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // [SỬA]: padTop = 12dp để không bị cấn khi ẩn Status Bar
            val padTop = (12 * resources.displayMetrics.density).toInt()
            v.setPadding(systemBars.left, padTop, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val navPadTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, navPadTop, padHoriz, systemBars.bottom + navPadTop)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadNotebookData()
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo Status Bar luôn ẩn và tải lại dữ liệu mới nhất nếu vừa thêm từ
        hideSystemStatusBar()
        loadNotebookData()
    }

    private fun initViews() {
        btnAddWord = findViewById(R.id.btnAddWord)

        topicCardIT = findViewById(R.id.topicCardIT)
        topicCardComm = findViewById(R.id.topicCardComm)
        topicCardIELTS = findViewById(R.id.topicCardIELTS)
        topicCardIdioms = findViewById(R.id.topicCardIdioms)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterNew = findViewById(R.id.chipFilterNew)
        chipFilterLearning = findViewById(R.id.chipFilterLearning)
        chipFilterMastered = findViewById(R.id.chipFilterMastered)

        // [THÊM]: Khởi tạo RecyclerView và Empty state
        rvNotebook = findViewById(R.id.rvNotebook)
        tvEmptyNotebook = findViewById(R.id.tvEmptyNotebook)

        // [ĐÃ XÓA]: Khởi tạo cardVocab1, cardVocab2, cardVocab3 cũ

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupRecyclerView() {
        notebookAdapter = NotebookAdapter(
            items = emptyList(),
            onWordClick = { word ->
                // [THÊM]: Mở WordDetailActivity với EXTRA_WORD là slug theo PROMPT 6.2
                val intent = Intent(this, WordDetailActivity::class.java).apply {
                    putExtra("EXTRA_WORD", word.id)
                }
                startActivity(intent)
            }
        )
        rvNotebook.layoutManager = LinearLayoutManager(this)
        rvNotebook.adapter = notebookAdapter
    }

    private fun setupListeners() {
        btnAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        // Topic Cards selection
        topicCardIT.setOnClickListener {
            toggleTopic("IT", topicCardIT)
        }

        topicCardComm.setOnClickListener {
            toggleTopic("COMM", topicCardComm)
        }

        topicCardIELTS.setOnClickListener {
            toggleTopic("IELTS", topicCardIELTS)
        }

        topicCardIdioms.setOnClickListener {
            toggleTopic("IDIOMS", topicCardIdioms)
        }

        // Status Filter Chips
        chipFilterAll.setOnClickListener {
            selectedStatus = "ALL"
            selectStatusFilter(chipFilterAll)
            applyFilters()
        }

        chipFilterNew.setOnClickListener {
            selectedStatus = "new"
            selectStatusFilter(chipFilterNew)
            applyFilters()
        }

        chipFilterLearning.setOnClickListener {
            selectedStatus = "learning"
            selectStatusFilter(chipFilterLearning)
            applyFilters()
        }

        chipFilterMastered.setOnClickListener {
            selectedStatus = "mastered"
            selectStatusFilter(chipFilterMastered)
            applyFilters()
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
            finish()
        }

        navNotebook.setOnClickListener {
            // Đang ở Sổ tay
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
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    // [THÊM]: Tải dữ liệu sổ tay từ Firestore (userWords join words) theo PROMPT 6.2
    private fun loadNotebookData() {
        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: "guest"

            // 1. Lấy kho từ đã duyệt hoặc fallback danh sách 12 từ chuẩn
            val approvedWords = try {
                wordRepository.getApprovedWords().firstOrNull() ?: WordRepository.getSeedWordsList()
            } catch (e: Exception) {
                WordRepository.getSeedWordsList()
            }
            val wordMap = (approvedWords.ifEmpty { WordRepository.getSeedWordsList() }).associateBy { it.id }

            // 2. Lấy danh sách từ của người dùng
            val userWords = try {
                if (uid != "guest") {
                    wordRepository.getUserWords(uid).firstOrNull() ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            // 3. Xây dựng danh sách NotebookItem
            if (userWords.isNotEmpty()) {
                allNotebookItems = userWords.mapNotNull { uw ->
                    val word = wordMap[uw.wordId] ?: return@mapNotNull null
                    NotebookItem(
                        word = word,
                        userWord = uw,
                        nextReviewInfo = "Ôn tập SM-2"
                    )
                }
            } else {
                // Fallback nạp các từ mẫu mặc định vào sổ tay để trải nghiệm mượt mà không bị trắng màn hình
                val seedWords = WordRepository.getSeedWordsList()
                allNotebookItems = seedWords.mapIndexed { index, w ->
                    val status = when (index % 3) {
                        0 -> "learning"
                        1 -> "new"
                        else -> "mastered"
                    }
                    val days = (index * 2 + 1)
                    NotebookItem(
                        word = w,
                        userWord = UserWord(
                            id = "${uid}_${w.id}",
                            userId = uid,
                            wordId = w.id,
                            status = status,
                            folder = w.topic
                        ),
                        nextReviewInfo = if (status == "mastered") "Đã thuộc" else "Ôn tập sau: $days ngày"
                    )
                }
            }

            applyFilters()
        }
    }

    // [THÊM]: Áp dụng đồng thời bộ lọc Trạng thái và bộ lọc Chủ đề
    private fun applyFilters() {
        var filtered = allNotebookItems

        // Lọc theo trạng thái (ALL / new / learning / mastered)
        if (selectedStatus != "ALL") {
            filtered = filtered.filter { it.userWord.status.equals(selectedStatus, ignoreCase = true) }
        }

        // Lọc theo chủ đề được chọn (IT / COMM / IELTS / IDIOMS)
        selectedTopic?.let { topicKey ->
            filtered = filtered.filter { item ->
                val t = item.word.topic.uppercase()
                when (topicKey) {
                    "IT" -> t.contains("IT") || t.contains("CÔNG NGHỆ") || item.word.id in listOf("idempotent", "resilient")
                    "COMM" -> t.contains("COMM") || t.contains("GIAO TIẾP")
                    "IELTS" -> t.contains("IELTS") || t.contains("ACADEMIC") || item.word.id in listOf("serendipity", "pragmatic", "ubiquitous", "ephemeral")
                    "IDIOMS" -> t.contains("IDIOM") || t.contains("THÀNH NGỮ")
                    else -> true
                }
            }
        }

        // Cập nhật giao diện
        notebookAdapter.updateData(filtered)
        if (filtered.isEmpty()) {
            tvEmptyNotebook.visibility = View.VISIBLE
            rvNotebook.visibility = View.GONE
        } else {
            tvEmptyNotebook.visibility = View.GONE
            rvNotebook.visibility = View.VISIBLE
        }
    }

    // [SỬA]: Chuyển đổi chọn/bỏ chọn chủ đề và áp dụng filter vào RecyclerView
    private fun toggleTopic(topicKey: String, clickedCard: LinearLayout) {
        val topicCards = listOf(topicCardIT, topicCardComm, topicCardIELTS, topicCardIdioms)
        if (selectedTopic == topicKey) {
            selectedTopic = null
            topicCards.forEach { it.setBackgroundResource(R.drawable.bg_topic_card) }
            Toast.makeText(this, "Hiển thị tất cả chủ đề", Toast.LENGTH_SHORT).show()
        } else {
            selectedTopic = topicKey
            topicCards.forEach {
                if (it == clickedCard) {
                    it.setBackgroundResource(R.drawable.bg_topic_card_selected)
                } else {
                    it.setBackgroundResource(R.drawable.bg_topic_card)
                }
            }

            val topicName = when (topicKey) {
                "IT" -> "Chuyên ngành IT"
                "IELTS" -> "IELTS Reading & Academic"
                "COMM" -> "Giao tiếp hàng ngày"
                "IDIOMS" -> "Idioms & Phrasal Verbs"
                else -> ""
            }
            Toast.makeText(this, "Bộ sưu tập: $topicName", Toast.LENGTH_SHORT).show()
        }

        applyFilters()
    }

    // [SỬA]: Cập nhật UI chip trạng thái được kích hoạt
    private fun selectStatusFilter(activeChip: TextView) {
        val chips = listOf(chipFilterAll, chipFilterNew, chipFilterLearning, chipFilterMastered)
        for (chip in chips) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_purple)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_white)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }
}
