package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
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
import androidx.lifecycle.lifecycleScope
import com.example.eduprofile.data.ReviewRepository
import com.example.eduprofile.data.UserStatsRepository
import com.example.eduprofile.data.Word
import com.example.eduprofile.data.WordRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
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

    // Word of the Day views
    private lateinit var tvWotdWord: TextView
    private lateinit var tvWotdPhonetic: TextView
    private lateinit var tvWotdMeaning: TextView
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
    private var isWotdBookmarked = false

    private val wordRepository = WordRepository()
    private val reviewRepository = ReviewRepository()
    private val userStatsRepository = UserStatsRepository()

    private var currentWotd: Word = Word(
        id = "serendipity",
        word = "Serendipity",
        ipa = "/ˌser.ənˈdɪp.ə.ti/",
        pos = "Danh từ",
        meaning = "Sự tình cờ may mắn; khả năng phát hiện ra những điều thú vị, có giá trị một cách ngẫu nhiên."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống Status Bar theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_home)

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
        setupListeners()
        loadWordOfTheDay()
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo Status Bar luôn ẩn khi quay lại màn hình chính
        hideSystemStatusBar()
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

        tvWotdWord = findViewById(R.id.tvWotdWord)
        tvWotdPhonetic = findViewById(R.id.tvWotdPhonetic)
        tvWotdMeaning = findViewById(R.id.tvWotdMeaning)
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
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Search action
        etSearchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearchBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, LookupActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", query)
                    }
                    startActivity(intent)
                }
                true
            } else false
        }

        btnVoiceSearch.setOnClickListener {
            Toast.makeText(this, "Tìm kiếm bằng giọng nói AI", Toast.LENGTH_SHORT).show()
        }

        btnCameraSearch.setOnClickListener {
            Toast.makeText(this, "Quét văn bản OCR qua Camera", Toast.LENGTH_SHORT).show()
        }

        // Quick Actions
        qaLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
        }

        qaSRS.setOnClickListener {
            if (checkGuestBlockAction()) return@setOnClickListener
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        qaQuiz.setOnClickListener {
            if (checkGuestBlockAction()) return@setOnClickListener
            startActivity(Intent(this, QuizActivity::class.java))
        }

        qaNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        qaAddWord.setOnClickListener {
            if (checkGuestBlockAction()) return@setOnClickListener
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        cardSrsQueue.setOnClickListener {
            if (checkGuestBlockAction()) return@setOnClickListener
            startActivity(Intent(this, FlashcardActivity::class.java))
        }

        // [SỬA]: Phát âm Từ vựng của ngày
        btnAudioUS.setOnClickListener {
            speakWord(currentWotd.word, Locale.US)
        }

        btnAudioUK.setOnClickListener {
            speakWord(currentWotd.word, Locale.UK)
        }

        // [SỬA]: Bookmark Từ vựng của ngày lưu thật vào Firestore
        btnBookmarkWotd.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Đăng nhập để lưu tiến độ của bạn", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }

            isWotdBookmarked = !isWotdBookmarked
            btnBookmarkWotd.setImageResource(
                if (isWotdBookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark_white
            )

            lifecycleScope.launch {
                wordRepository.addToNotebook(uid, currentWotd.id, "Từ vựng của ngày")
                reviewRepository.toggleBookmark(uid, currentWotd.id, isWotdBookmarked)
            }

            val msg = if (isWotdBookmarked) "Đã lưu '${currentWotd.word}' vào Sổ tay" else "Đã bỏ lưu khỏi Sổ tay"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // [SỬA]: Chạm vào Từ vựng của ngày mở WordDetailActivity với slug thật
        findViewById<View>(R.id.cardWotd)?.setOnClickListener {
            val intent = Intent(this, WordDetailActivity::class.java).apply {
                putExtra("EXTRA_WORD", currentWotd.id)
            }
            startActivity(intent)
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
            startActivity(Intent(this, StatsActivity::class.java))
        }

        navAccount.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // [THÊM]: Chặn thao tác ghi đối với Chế độ Khách theo PROMPT 2
    private fun checkGuestBlockAction(): Boolean {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Đăng nhập để lưu tiến độ của bạn", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return true
        }
        return false
    }

    // [THÊM]: Tải Từ vựng của ngày từ Firestore theo ngày trong năm theo PROMPT 6.1
    private fun loadWordOfTheDay() {
        lifecycleScope.launch {
            wordRepository.getApprovedWords().collectLatest { words ->
                if (words.isNotEmpty()) {
                    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    val wotd = words[dayOfYear % words.size]
                    currentWotd = wotd
                    tvWotdWord.text = wotd.word
                    tvWotdPhonetic.text = "${wotd.ipa} • ${wotd.pos}"
                    tvWotdMeaning.text = wotd.meaning

                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val review = reviewRepository.getReview(uid, wotd.id)
                        isWotdBookmarked = review?.isBookmarked ?: false
                        btnBookmarkWotd.setImageResource(
                            if (isWotdBookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark_white
                        )
                    }
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            userStatsRepository.getUserStats(uid).collectLatest { stats ->
                if (stats != null && stats.displayName.isNotEmpty()) {
                    tvGreeting.text = "Chào buổi sáng, ${stats.displayName} \uD83D\uDC4B"
                }
            }
        }
    }

    private fun speakWord(word: String, locale: Locale) {
        tts?.let {
            it.language = locale
            it.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_$word")
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
