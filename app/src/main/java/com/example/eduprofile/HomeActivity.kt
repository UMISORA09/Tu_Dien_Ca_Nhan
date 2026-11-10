package com.example.eduprofile

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class HomeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var etSearchBox: EditText
    private lateinit var btnVoiceSearch: FrameLayout
    private lateinit var btnCameraSearch: FrameLayout
    private lateinit var btnMenu: FrameLayout
    private lateinit var btnDarkMode: FrameLayout

    // Dictionary tabs
    private lateinit var tabDictAll: LinearLayout
    private lateinit var tabDictEnVi: TextView
    private lateinit var tabDictJp: TextView
    private lateinit var tabDictKr: TextView

    // Filters
    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterNoun: TextView
    private lateinit var chipFilterVerb: TextView
    private lateinit var chipFilterAdj: TextView
    private lateinit var chipFilterIdiom: TextView

    // Word of the Day
    private lateinit var btnAudioUS: LinearLayout
    private lateinit var btnAudioUK: LinearLayout
    private lateinit var btnBookmarkWotd: ImageView
    private lateinit var btnViewDetail: LinearLayout

    // Trending chips
    private lateinit var chipTrend1: LinearLayout
    private lateinit var chipTrend2: LinearLayout
    private lateinit var chipTrend3: LinearLayout
    private lateinit var chipTrend4: LinearLayout
    private lateinit var btnCollapseTrending: TextView

    // Personal card
    private lateinit var btnAddNewWord: TextView
    private lateinit var btnPersonalAudio: FrameLayout
    private lateinit var btnPersonalBookmark: ImageView

    // Bottom Navigation
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout

    private var tts: TextToSpeech? = null
    private var isWotdBookmarked = false
    private var isPersonalBookmarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (8 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }

        tts = TextToSpeech(this, this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etSearchBox = findViewById(R.id.etSearchBox)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        btnCameraSearch = findViewById(R.id.btnCameraSearch)
        btnMenu = findViewById(R.id.btnMenu)
        btnDarkMode = findViewById(R.id.btnDarkMode)

        tabDictAll = findViewById(R.id.tabDictAll)
        tabDictEnVi = findViewById(R.id.tabDictEnVi)
        tabDictJp = findViewById(R.id.tabDictJp)
        tabDictKr = findViewById(R.id.tabDictKr)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterNoun = findViewById(R.id.chipFilterNoun)
        chipFilterVerb = findViewById(R.id.chipFilterVerb)
        chipFilterAdj = findViewById(R.id.chipFilterAdj)
        chipFilterIdiom = findViewById(R.id.chipFilterIdiom)

        btnAudioUS = findViewById(R.id.btnAudioUS)
        btnAudioUK = findViewById(R.id.btnAudioUK)
        btnBookmarkWotd = findViewById(R.id.btnBookmarkWotd)
        btnViewDetail = findViewById(R.id.btnViewDetail)

        chipTrend1 = findViewById(R.id.chipTrend1)
        chipTrend2 = findViewById(R.id.chipTrend2)
        chipTrend3 = findViewById(R.id.chipTrend3)
        chipTrend4 = findViewById(R.id.chipTrend4)
        btnCollapseTrending = findViewById(R.id.btnCollapseTrending)

        btnAddNewWord = findViewById(R.id.btnAddNewWord)
        btnPersonalAudio = findViewById(R.id.btnPersonalAudio)
        btnPersonalBookmark = findViewById(R.id.btnPersonalBookmark)

        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
    }

    private fun setupListeners() {
        btnMenu.setOnClickListener {
            Toast.makeText(this, "Mở thanh Menu điều hướng", Toast.LENGTH_SHORT).show()
        }

        btnDarkMode.setOnClickListener {
            Toast.makeText(this, "Chuyển đổi giao diện Sáng / Tối", Toast.LENGTH_SHORT).show()
        }

        etSearchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchBox.text.toString())
                true
            } else false
        }

        btnVoiceSearch.setOnClickListener {
            Toast.makeText(this, "Tính năng tìm kiếm bằng giọng nói AI", Toast.LENGTH_SHORT).show()
        }

        btnCameraSearch.setOnClickListener {
            Toast.makeText(this, "Tính năng quét văn bản OCR qua Camera", Toast.LENGTH_SHORT).show()
        }

        // Audio pronunciation
        btnAudioUS.setOnClickListener {
            speakWord("Serendipity", Locale.US)
        }

        btnAudioUK.setOnClickListener {
            speakWord("Serendipity", Locale.UK)
        }

        btnPersonalAudio.setOnClickListener {
            speakWord("Serendipity", Locale.US)
        }

        // Bookmark buttons
        btnBookmarkWotd.setOnClickListener {
            isWotdBookmarked = !isWotdBookmarked
            if (isWotdBookmarked) {
                Toast.makeText(this, "Đã lưu 'Serendipity' vào Sổ tay cá nhân", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Đã bỏ lưu khỏi Sổ tay", Toast.LENGTH_SHORT).show()
            }
        }

        btnPersonalBookmark.setOnClickListener {
            isPersonalBookmarked = !isPersonalBookmarked
            if (isPersonalBookmarked) {
                Toast.makeText(this, "Đã ghim từ vựng ưu tiên ôn tập", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Đã bỏ ghim từ vựng", Toast.LENGTH_SHORT).show()
            }
        }

        btnViewDetail.setOnClickListener {
            Toast.makeText(this, "Xem phân tích chi tiết từ vựng 'Serendipity'", Toast.LENGTH_SHORT).show()
        }

        // Trending chips click
        chipTrend1.setOnClickListener { setTrendingSearch("Serendipity") }
        chipTrend2.setOnClickListener { setTrendingSearch("Idempotent") }
        chipTrend3.setOnClickListener { setTrendingSearch("Resilient") }
        chipTrend4.setOnClickListener { setTrendingSearch("Bite the bullet") }

        btnAddNewWord.setOnClickListener {
            Toast.makeText(this, "Mở biểu mẫu thêm từ mới vào Từ điển cá nhân", Toast.LENGTH_SHORT).show()
        }

        // Bottom nav
        navLookup.setOnClickListener {
            Toast.makeText(this, "Đang ở trang Tra cứu", Toast.LENGTH_SHORT).show()
        }
        navNotebook.setOnClickListener {
            Toast.makeText(this, "Mở Sổ tay từ vựng cá nhân", Toast.LENGTH_SHORT).show()
        }
        navSRS.setOnClickListener {
            Toast.makeText(this, "Bắt đầu phiên Ôn tập ngắt quãng SRS", Toast.LENGTH_SHORT).show()
        }
        navStats.setOnClickListener {
            Toast.makeText(this, "Xem Thống kê học tập & Chuỗi streak", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setTrendingSearch(word: String) {
        etSearchBox.setText(word)
        etSearchBox.setSelection(word.length)
        performSearch(word)
    }

    private fun performSearch(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) {
            Toast.makeText(this, "Đang tra cứu từ: $q", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakWord(word: String, locale: Locale) {
        tts?.let {
            it.language = locale
            it.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_$word")
            Toast.makeText(this, "Phát âm [$locale]: $word", Toast.LENGTH_SHORT).show()
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

