package com.example.eduprofile

import android.content.Intent
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

class LookupActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBack: FrameLayout
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

    // Actions & Cards
    private lateinit var btnAddNewWord: TextView
    private lateinit var btnAudioWord1: FrameLayout
    private lateinit var btnAudioWord2: FrameLayout
    private lateinit var btnAudioWord3: FrameLayout
    private lateinit var btnBookmarkWord1: ImageView
    private lateinit var btnBookmarkWord2: ImageView
    private lateinit var btnBookmarkWord3: ImageView

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private var tts: TextToSpeech? = null
    private var isWord1Bookmarked = true
    private var isWord2Bookmarked = true
    private var isWord3Bookmarked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lookup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }

        tts = TextToSpeech(this, this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
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

        btnAddNewWord = findViewById(R.id.btnAddNewWord)
        btnAudioWord1 = findViewById(R.id.btnAudioWord1)
        btnAudioWord2 = findViewById(R.id.btnAudioWord2)
        btnAudioWord3 = findViewById(R.id.btnAudioWord3)
        btnBookmarkWord1 = findViewById(R.id.btnBookmarkWord1)
        btnBookmarkWord2 = findViewById(R.id.btnBookmarkWord2)
        btnBookmarkWord3 = findViewById(R.id.btnBookmarkWord3)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
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

        // Recent search chips click
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
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        // Audio pronunciation
        btnAudioWord1.setOnClickListener { speakWord("Serendipity") }
        btnAudioWord2.setOnClickListener { speakWord("Resilient") }
        btnAudioWord3.setOnClickListener { speakWord("Idempotent") }

        // Bookmark buttons
        btnBookmarkWord1.setOnClickListener {
            isWord1Bookmarked = !isWord1Bookmarked
            btnBookmarkWord1.setImageResource(if (isWord1Bookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark)
            Toast.makeText(this, if (isWord1Bookmarked) "Đã lưu 'Serendipity'" else "Đã bỏ lưu 'Serendipity'", Toast.LENGTH_SHORT).show()
        }

        btnBookmarkWord2.setOnClickListener {
            isWord2Bookmarked = !isWord2Bookmarked
            btnBookmarkWord2.setImageResource(if (isWord2Bookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark)
            Toast.makeText(this, if (isWord2Bookmarked) "Đã lưu 'Resilient'" else "Đã bỏ lưu 'Resilient'", Toast.LENGTH_SHORT).show()
        }

        btnBookmarkWord3.setOnClickListener {
            isWord3Bookmarked = !isWord3Bookmarked
            btnBookmarkWord3.setImageResource(if (isWord3Bookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark)
            Toast.makeText(this, if (isWord3Bookmarked) "Đã lưu 'Idempotent'" else "Đã bỏ lưu 'Idempotent'", Toast.LENGTH_SHORT).show()
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
        }

        navStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        navAccount.setOnClickListener {
            Toast.makeText(this, "Mở Tài khoản cá nhân & Cài đặt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSearchQuery(word: String) {
        etSearchBox.setText(word)
        etSearchBox.setSelection(word.length)
        performSearch(word)
    }

    private fun performSearch(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) {
            Toast.makeText(this, "Đang tra cứu từ vựng: $q", Toast.LENGTH_SHORT).show()
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

