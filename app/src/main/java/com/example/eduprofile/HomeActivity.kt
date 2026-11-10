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

class HomeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnMenu: FrameLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvGreetingSub: TextView
    private lateinit var ivUserAvatar: ImageView

    // Dictionary tabs
    private lateinit var tabDictAll: LinearLayout
    private lateinit var tabDictEnVi: TextView
    private lateinit var tabDictJp: TextView
    private lateinit var tabDictKr: TextView

    // Search bar
    private lateinit var etSearchBox: EditText
    private lateinit var btnVoiceSearch: FrameLayout
    private lateinit var btnCameraSearch: FrameLayout

    // Quick Actions
    private lateinit var qaLookup: LinearLayout
    private lateinit var qaSRS: LinearLayout
    private lateinit var qaQuiz: LinearLayout
    private lateinit var qaNotebook: LinearLayout
    private lateinit var qaAddWord: LinearLayout
    private lateinit var cardSrsQueue: androidx.cardview.widget.CardView

    // Word of the Day
    private lateinit var btnAudioUS: LinearLayout
    private lateinit var btnAudioUK: LinearLayout
    private lateinit var btnBookmarkWotd: ImageView

    // Bottom Navigation (6 tabs)
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private var tts: TextToSpeech? = null
    private var isWotdBookmarked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

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
        btnMenu = findViewById(R.id.btnMenu)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvGreetingSub = findViewById(R.id.tvGreetingSub)
        ivUserAvatar = findViewById(R.id.ivUserAvatar)

        tabDictAll = findViewById(R.id.tabDictAll)
        tabDictEnVi = findViewById(R.id.tabDictEnVi)
        tabDictJp = findViewById(R.id.tabDictJp)
        tabDictKr = findViewById(R.id.tabDictKr)

        etSearchBox = findViewById(R.id.etSearchBox)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        btnCameraSearch = findViewById(R.id.btnCameraSearch)

        qaLookup = findViewById(R.id.qaLookup)
        qaSRS = findViewById(R.id.qaSRS)
        qaQuiz = findViewById(R.id.qaQuiz)
        qaNotebook = findViewById(R.id.qaNotebook)
        qaAddWord = findViewById(R.id.qaAddWord)
        cardSrsQueue = findViewById(R.id.cardSrsQueue)

        btnAudioUS = findViewById(R.id.btnAudioUS)
        btnAudioUK = findViewById(R.id.btnAudioUK)
        btnBookmarkWotd = findViewById(R.id.btnBookmarkWotd)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupListeners() {
        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.stay)
        }

        ivUserAvatar.setOnClickListener {
            Toast.makeText(this, "Hồ sơ người dùng: Nguyễn Xuân Hòa", Toast.LENGTH_SHORT).show()
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

        // Quick Actions
        qaLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
        }

        qaSRS.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        cardSrsQueue.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        qaQuiz.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        qaNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        qaAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        // Audio pronunciation
        btnAudioUS.setOnClickListener {
            speakWord("Serendipity", Locale.US)
        }

        btnAudioUK.setOnClickListener {
            speakWord("Serendipity", Locale.UK)
        }

        // Bookmark toggle
        btnBookmarkWotd.setOnClickListener {
            isWotdBookmarked = !isWotdBookmarked
            if (isWotdBookmarked) {
                btnBookmarkWotd.setImageResource(R.drawable.ic_bookmark_gold)
                Toast.makeText(this, "Đã lưu 'Serendipity' vào Sổ tay", Toast.LENGTH_SHORT).show()
            } else {
                btnBookmarkWotd.setImageResource(R.drawable.ic_bookmark_white)
                Toast.makeText(this, "Đã bỏ lưu khỏi Sổ tay", Toast.LENGTH_SHORT).show()
            }
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            Toast.makeText(this, "Đang ở Trang chủ", Toast.LENGTH_SHORT).show()
        }

        navLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
        }

        navNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        navSRS.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        navStats.setOnClickListener {
            Toast.makeText(this, "Xem Thống kê học tập", Toast.LENGTH_SHORT).show()
        }

        navAccount.setOnClickListener {
            Toast.makeText(this, "Mở Tài khoản cá nhân & Cài đặt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) {
            Toast.makeText(this, "Đang tra từ điển: $q", Toast.LENGTH_SHORT).show()
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
