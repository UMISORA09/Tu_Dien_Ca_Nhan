package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eduprofile.data.ReviewRepository
import com.example.eduprofile.data.Word
import com.example.eduprofile.data.WordAdapter
import com.example.eduprofile.data.WordRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class LookupActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var etSearchBox: EditText
    private lateinit var btnVoiceSearch: FrameLayout
    private lateinit var btnCameraSearch: FrameLayout

    // Dictionary tabs
    private lateinit var tabDictAll: LinearLayout
    private lateinit var tabDictEn: TextView
    private lateinit var tabDictJp: TextView

    // Filter chips
    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterNoun: TextView
    private lateinit var chipFilterVerb: TextView
    private lateinit var chipFilterAdj: TextView
    private lateinit var chipFilterIdiom: TextView

    // Recent search chips
    private lateinit var btnClearHistory: TextView
    private lateinit var chipRecent1: TextView
    private lateinit var chipRecent2: TextView
    private lateinit var chipRecent3: TextView
    private lateinit var chipRecent4: TextView
    private lateinit var chipRecent5: TextView
    private lateinit var chipRecent6: TextView

    // Inventory Header & RecyclerView
    private lateinit var tvInventoryCount: TextView
    private lateinit var btnAddNewWord: TextView
    private lateinit var rvWords: RecyclerView
    private lateinit var tvEmptyState: TextView

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private var tts: TextToSpeech? = null
    private val wordRepository = WordRepository()
    private val reviewRepository = ReviewRepository()

    private var allWords: List<Word> = emptyList()
    private var currentFilteredWords: List<Word> = emptyList()
    private var selectedPosFilter: String = "ALL"
    private var bookmarkedIds: MutableSet<String> = mutableSetOf()
    private lateinit var wordAdapter: WordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống Status Bar theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_lookup)

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

        tts = TextToSpeech(this, this)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadDataFromRepository()
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo Status Bar luôn được ẩn khi quay lại màn hình
        hideSystemStatusBar()
    }

    private fun initViews() {
        etSearchBox = findViewById(R.id.etSearchBox)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        btnCameraSearch = findViewById(R.id.btnCameraSearch)

        tabDictAll = findViewById(R.id.tabDictAll)
        tabDictEn = findViewById(R.id.tabDictEn)
        tabDictJp = findViewById(R.id.tabDictJp)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterNoun = findViewById(R.id.chipFilterNoun)
        chipFilterVerb = findViewById(R.id.chipFilterVerb)
        chipFilterAdj = findViewById(R.id.chipFilterAdj)
        chipFilterIdiom = findViewById(R.id.chipFilterIdiom)

        btnClearHistory = findViewById(R.id.btnClearHistory)
        chipRecent1 = findViewById(R.id.chipRecent1)
        chipRecent2 = findViewById(R.id.chipRecent2)
        chipRecent3 = findViewById(R.id.chipRecent3)
        chipRecent4 = findViewById(R.id.chipRecent4)
        chipRecent5 = findViewById(R.id.chipRecent5)
        chipRecent6 = findViewById(R.id.chipRecent6)

        tvInventoryCount = findViewById(R.id.tvInventoryCount)
        btnAddNewWord = findViewById(R.id.btnAddNewWord)
        rvWords = findViewById(R.id.rvWords)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupRecyclerView() {
        wordAdapter = WordAdapter(
            words = emptyList(),
            bookmarkedWordIds = bookmarkedIds,
            onWordClick = { word ->
                // [SỬA]: Mở WordDetailActivity với EXTRA_WORD là slug (ví dụ: serendipity) theo PROMPT 6.1
                val intent = Intent(this, WordDetailActivity::class.java).apply {
                    putExtra("EXTRA_WORD", word.id)
                }
                startActivity(intent)
            },
            onAudioClick = { word ->
                speakWord(word.word)
            },
            onBookmarkClick = { word ->
                handleToggleBookmark(word)
            }
        )
        rvWords.layoutManager = LinearLayoutManager(this)
        rvWords.adapter = wordAdapter
    }

    private fun setupListeners() {
        // Tìm kiếm tức thì khi gõ
        etSearchBox.doAfterTextChanged { text ->
            performSearch(text?.toString().orEmpty())
        }

        etSearchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchBox.text.toString())
                true
            } else false
        }

        btnVoiceSearch.setOnClickListener {
            Toast.makeText(this, "Tìm kiếm bằng giọng nói AI", Toast.LENGTH_SHORT).show()
        }

        btnCameraSearch.setOnClickListener {
            Toast.makeText(this, "Quét văn bản OCR qua Camera", Toast.LENGTH_SHORT).show()
        }

        // Chip lọc loại từ
        chipFilterAll.setOnClickListener { applyPosFilter("ALL") }
        chipFilterNoun.setOnClickListener { applyPosFilter("Danh từ") }
        chipFilterVerb.setOnClickListener { applyPosFilter("Động từ") }
        chipFilterAdj.setOnClickListener { applyPosFilter("Tính từ") }
        chipFilterIdiom.setOnClickListener { applyPosFilter("Idiom") }

        // Chip tìm kiếm gần đây
        chipRecent1.setOnClickListener { setSearchQuery("Serendipity") }
        chipRecent2.setOnClickListener { setSearchQuery("Idempotent") }
        chipRecent3.setOnClickListener { setSearchQuery("Ephemeral") }
        chipRecent4.setOnClickListener { setSearchQuery("Pragmatic") }
        chipRecent5.setOnClickListener { setSearchQuery("Ubiquitous") }
        chipRecent6.setOnClickListener { setSearchQuery("Resilience") }

        btnClearHistory.setOnClickListener {
            Toast.makeText(this, "Đã xóa lịch sử tra cứu gần đây", Toast.LENGTH_SHORT).show()
        }

        btnAddNewWord.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this, "Đăng nhập để thêm từ vựng mới", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, AddWordActivity::class.java))
            }
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        navLookup.setOnClickListener {
            Toast.makeText(this, "Đang ở màn hình Tra cứu", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    // [THÊM]: Đọc danh sách từ vựng approved thật từ Firestore theo PROMPT 6.1
    private fun loadDataFromRepository() {
        lifecycleScope.launch {
            // Tự động seed nếu kho Firestore còn trống
            try {
                wordRepository.seedInitialWordsIfEmpty()
            } catch (_: Exception) {}

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                lifecycleScope.launch {
                    reviewRepository.getAllUserReviews(currentUid).collectLatest { reviews ->
                        bookmarkedIds = reviews.filter { it.isBookmarked }.map { it.wordId }.toMutableSet()
                        wordAdapter.notifyDataSetChanged()
                    }
                }
            }

            wordRepository.getApprovedWords().collectLatest { words ->
                allWords = words
                applyFilters()
            }
        }
    }

    private fun setSearchQuery(word: String) {
        etSearchBox.setText(word)
        etSearchBox.setSelection(word.length)
        performSearch(word)
    }

    private fun performSearch(query: String) {
        applyFilters()
    }

    private fun applyPosFilter(pos: String) {
        selectedPosFilter = pos
        applyFilters()
    }

    private fun applyFilters() {
        val query = etSearchBox.text?.toString()?.trim()?.lowercase().orEmpty()
        var filtered = allWords

        if (selectedPosFilter != "ALL") {
            filtered = filtered.filter { it.pos.equals(selectedPosFilter, ignoreCase = true) }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.word.lowercase().contains(query) || it.meaning.lowercase().contains(query)
            }
        }

        currentFilteredWords = filtered
        wordAdapter.updateData(filtered)

        tvInventoryCount.text = "Tất cả từ vựng trong kho (${filtered.size})"

        if (filtered.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvWords.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvWords.visibility = View.VISIBLE
        }
    }

    private fun handleToggleBookmark(word: Word) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Đăng nhập để lưu từ vào Sổ tay", Toast.LENGTH_SHORT).show()
            return
        }

        val isCurrentlyBookmarked = bookmarkedIds.contains(word.id)
        val newBookmark = !isCurrentlyBookmarked
        if (newBookmark) {
            bookmarkedIds.add(word.id)
            Toast.makeText(this, "Đã lưu '${word.word}' vào Sổ tay", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkedIds.remove(word.id)
            Toast.makeText(this, "Đã bỏ lưu '${word.word}'", Toast.LENGTH_SHORT).show()
        }
        wordAdapter.notifyDataSetChanged()

        lifecycleScope.launch {
            reviewRepository.toggleBookmark(uid, word.id, newBookmark)
        }
    }

    private fun speakWord(word: String) {
        tts?.let {
            it.language = Locale.US
            it.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_$word")
            Toast.makeText(this, "Phát âm: $word", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Phát âm: $word", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
