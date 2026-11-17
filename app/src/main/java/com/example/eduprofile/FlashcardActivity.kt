package com.example.eduprofile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class FlashcardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class FlashcardItem(
        val id: Int,
        val word: String,
        val phonetic: String,
        val pos: String,
        val cefr: String,
        val topic: String,
        val meaning: String,
        val exampleEn: String,
        val exampleVi: String,
        val mnemonic: String,
        var easeFactor: Float = 2.5f,
        var intervalDays: Int = 1,
        var repetitions: Int = 0,
        var nextReviewMillis: Long = 0L,
        var isBookmarked: Boolean = false
    )

    // Views - Header & Progress
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnBack: FrameLayout
    private lateinit var btnShuffle: FrameLayout
    private lateinit var tabModeFlashcard: LinearLayout
    private lateinit var tabModeQuiz: LinearLayout
    private lateinit var tvCardCounter: TextView
    private lateinit var tvQueueRemaining: TextView
    private lateinit var progressBarSession: ProgressBar

    // Views - Card 3D
    private lateinit var cardContainer: FrameLayout
    private lateinit var cardFront: CardView
    private lateinit var cardBack: CardView
    private lateinit var cardCompletion: CardView

    // Front Card Views
    private lateinit var badgePosFront: TextView
    private lateinit var badgeCefrFront: TextView
    private lateinit var badgeTopicFront: TextView
    private lateinit var btnBookmarkFront: ImageView
    private lateinit var tvWordFront: TextView
    private lateinit var tvPhoneticFront: TextView
    private lateinit var btnSpeakFront: LinearLayout

    // Back Card Views
    private lateinit var scrollCardBack: ScrollView
    private lateinit var layoutCardBackContent: LinearLayout
    private lateinit var tvWordBack: TextView
    private lateinit var tvPhoneticBack: TextView
    private lateinit var btnSpeakBack: FrameLayout
    private lateinit var tvMeaningBack: TextView
    private lateinit var tvExampleEnBack: TextView
    private lateinit var tvExampleViBack: TextView
    private lateinit var tvMnemonicBack: TextView
    private lateinit var tvSrsStatsBack: TextView

    // Action: Remember Prompt & Show Answer
    private lateinit var layoutAnswerAction: LinearLayout
    private lateinit var tvRememberPrompt: TextView
    private lateinit var btnShowAnswer: AppCompatButton

    // Rating Views
    private lateinit var ratingPrompt: TextView
    private lateinit var layoutRatingSection: LinearLayout
    private lateinit var layoutRatingButtonsRow: LinearLayout
    private lateinit var btnRatingAgain: LinearLayout
    private lateinit var btnRatingHard: LinearLayout
    private lateinit var btnRatingGood: LinearLayout
    private lateinit var btnRatingEasy: LinearLayout
    private lateinit var btnRetryCurrent: AppCompatButton

    // Completion Views
    private lateinit var tvCompletionXp: TextView
    private lateinit var tvCompletionStreak: TextView
    private lateinit var btnRestartSession: AppCompatButton
    private lateinit var btnBackHome: AppCompatButton

    // Bottom Navigation Views
    private lateinit var bottomNavContainer: LinearLayout
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    // Data & State
    private val masterDeck = ArrayList<FlashcardItem>()
    private val flashcardList = ArrayList<FlashcardItem>()
    private var currentIndex = 0
    private var isFlipped = false
    private var isAnimating = false
    private var reviewedCount = 0
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_flashcard)

        setupWindowInsets()
        prefs = getSharedPreferences("sm2_storage_prefs", Context.MODE_PRIVATE)

        initViews()
        initMasterDeck()

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        } else {
            buildSessionQueue()
        }

        setupListeners()
        setupCameraDistance()

        tts = TextToSpeech(this, this)

        showCard(currentIndex)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainFlashcard)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (6 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }
    }

    private fun initMasterDeck() {
        masterDeck.clear()
        masterDeck.add(
            FlashcardItem(
                id = 1,
                word = "Serendipity",
                phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                pos = "Danh từ",
                cefr = "C1",
                topic = "IELTS Academic",
                meaning = "Sự tình cờ may mắn; khả năng tình cờ phát hiện ra những điều thú vị, có giá trị ngoài mong đợi.",
                exampleEn = "\"Finding this charming coffee shop was pure serendipity on a rainy morning.\"",
                exampleVi = "→ Việc tìm thấy quán cà phê xinh đẹp này là một sự tình cờ may mắn thuần túy vào một buổi sáng mưa.",
                mnemonic = "Mẹo nhớ: \"Se-ren-đi-pi-ty\" -> Đi dạo (đi) gặp chuyện vui vẻ, bất ngờ.",
                easeFactor = 2.5f,
                intervalDays = 4,
                repetitions = 2,
                isBookmarked = true
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 2,
                word = "Ephemeral",
                phonetic = "/ɪˈfem.ər.əl/",
                pos = "Tính từ",
                cefr = "C1",
                topic = "IELTS Academic",
                meaning = "Phù du, chóng tàn, chỉ tồn tại hoặc kéo dài trong một thời gian rất ngắn ngủi.",
                exampleEn = "\"The ephemeral beauty of cherry blossoms lasts for just a few days each spring.\"",
                exampleVi = "→ Vẻ đẹp phù du của hoa anh đào chỉ kéo dài vài ngày ngắn ngủi mỗi độ xuân về.",
                mnemonic = "Mẹo nhớ: \"E-phe-me-ral\" -> Phù du như một giấc mơ thoáng qua.",
                easeFactor = 2.4f,
                intervalDays = 1,
                repetitions = 1
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 3,
                word = "Idempotent",
                phonetic = "/ˌaɪ.dɛmˈpoʊ.tənt/",
                pos = "Tính từ",
                cefr = "C2",
                topic = "Chuyên ngành IT",
                meaning = "(Toán / IT) Lũy đẳng; đặc tính của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho kết quả giống hệt nhau.",
                exampleEn = "\"HTTP GET and PUT methods are designed to be idempotent in RESTful architecture.\"",
                exampleVi = "→ Các phương thức HTTP GET và PUT được thiết kế lũy đẳng trong kiến trúc RESTful.",
                mnemonic = "Mẹo nhớ: \"Idem\" (giống nhau) + \"potent\" (năng lực) -> Bao lần làm cũng y nguyên.",
                easeFactor = 2.6f,
                intervalDays = 6,
                repetitions = 3
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 4,
                word = "Resilient",
                phonetic = "/rɪˈzɪl.i.ənt/",
                pos = "Tính từ",
                cefr = "B2",
                topic = "Giao tiếp hàng ngày",
                meaning = "Kiên cường, mau hồi phục sau khó khăn, biến cố hoặc khả năng đàn hồi co giãn của vật chất.",
                exampleEn = "\"She showed a remarkably resilient attitude after facing the startup failure.\"",
                exampleVi = "→ Cô ấy thể hiện tinh thần kiên cường đáng kinh ngạc sau khi đối mặt thất bại khởi nghiệp.",
                mnemonic = "Mẹo nhớ: \"Re\" (lại) + \"silient\" (bật dậy) -> Bị đẩy ngã vẫn bật dậy kiên cường.",
                easeFactor = 2.5f,
                intervalDays = 3,
                repetitions = 2,
                isBookmarked = true
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 5,
                word = "Pragmatic",
                phonetic = "/præɡˈmæt.ɪk/",
                pos = "Tính từ",
                cefr = "C1",
                topic = "Giao tiếp hàng ngày",
                meaning = "Thực tế, thực dụng; giải quyết vấn đề dựa trên hiệu quả thực tế thay vì lý thuyết suông.",
                exampleEn = "\"We need a pragmatic approach to complete the product launch on time.\"",
                exampleVi = "→ Chúng ta cần một cách tiếp cận thực tế để hoàn thành việc ra mắt sản phẩm đúng hạn.",
                mnemonic = "Mẹo nhớ: \"Pragmatic\" -> Thực hành (practice) là trên hết, không lý thuyết suông.",
                easeFactor = 2.5f,
                intervalDays = 2,
                repetitions = 1
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 6,
                word = "Ubiquitous",
                phonetic = "/juːˈbɪk.wə.təs/",
                pos = "Tính từ",
                cefr = "C1",
                topic = "Chuyên ngành IT",
                meaning = "Có mặt ở khắp mọi nơi, phổ biến rộng rãi khắp chốn.",
                exampleEn = "\"Smartphones and high-speed mobile internet have become ubiquitous worldwide.\"",
                exampleVi = "→ Điện thoại thông minh và internet di động tốc độ cao đã trở nên phổ biến khắp mọi nơi.",
                mnemonic = "Mẹo nhớ: \"U-bi-qui-tous\" -> Đi đâu cũng gặp quầy tạp hóa quen thuộc.",
                easeFactor = 2.7f,
                intervalDays = 7,
                repetitions = 3
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 7,
                word = "Eloquent",
                phonetic = "/ˈel.ə.kwənt/",
                pos = "Tính từ",
                cefr = "B2",
                topic = "IELTS Academic",
                meaning = "Hùng biện, lưu loát; có tài diễn đạt ý nghĩ và cảm xúc một cách thuyết phục và truyền cảm.",
                exampleEn = "\"Her eloquent speech inspired thousands of young engineers in the conference.\"",
                exampleVi = "→ Bài phát biểu hùng biện của cô đã truyền cảm hứng cho hàng ngàn kỹ sư trẻ trong hội nghị.",
                mnemonic = "Mẹo nhớ: \"E-loquent\" -> Lời nói lưu loát, thanh thoát, chạm đến trái tim người nghe.",
                easeFactor = 2.5f,
                intervalDays = 4,
                repetitions = 2
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 8,
                word = "Tenacious",
                phonetic = "/təˈneɪ.ʃəs/",
                pos = "Tính từ",
                cefr = "C1",
                topic = "IELTS Academic",
                meaning = "Bền bỉ, ngoan cường, kiên trì theo đuổi mục tiêu đến cùng mà không bao giờ bỏ cuộc.",
                exampleEn = "\"His tenacious investigation finally brought the truth to light after years.\"",
                exampleVi = "→ Cuộc điều tra kiên trì của anh cuối cùng đã đưa sự thật ra ánh sáng sau nhiều năm.",
                mnemonic = "Mẹo nhớ: \"Te-na-cious\" -> Bền bỉ như mầm cây bám chặt trên vách đá.",
                easeFactor = 2.4f,
                intervalDays = 2,
                repetitions = 1
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 9,
                word = "Synchronous",
                phonetic = "/ˈsɪŋ.krə.nəs/",
                pos = "Tính từ",
                cefr = "B2",
                topic = "Chuyên ngành IT",
                meaning = "(Công nghệ) Đồng bộ; các tiến trình xảy ra cùng một thời điểm hoặc tuần tự chờ kết quả của nhau.",
                exampleEn = "\"Synchronous network calls can block the main UI thread in Android applications.\"",
                exampleVi = "→ Các lệnh gọi mạng đồng bộ có thể làm nghẽn luồng giao diện chính trong ứng dụng Android.",
                mnemonic = "Mẹo nhớ: \"Syn\" (cùng) + \"chron\" (thời gian) -> Cùng nhịp thời gian đồng bộ.",
                easeFactor = 2.5f,
                intervalDays = 3,
                repetitions = 2
            )
        )
        masterDeck.add(
            FlashcardItem(
                id = 10,
                word = "Ambivalent",
                phonetic = "/æmˈbɪv.ə.lənt/",
                pos = "Tính từ",
                cefr = "C1",
                topic = "Idioms & Phrasal",
                meaning = "Mâu thuẫn trong cảm xúc; vừa thích lại vừa không thích, lưỡng lự chưa dứt khoát.",
                exampleEn = "\"He felt ambivalent about taking the promotion because it required frequent travel.\"",
                exampleVi = "→ Anh cảm thấy vừa mừng vừa lo về việc thăng chức vì công việc yêu cầu công tác liên tục.",
                mnemonic = "Mẹo nhớ: \"Ambi\" (hai phía) + \"valent\" -> Tâm trí giằng xé giữa hai ngả đường.",
                easeFactor = 2.5f,
                intervalDays = 1,
                repetitions = 1
            )
        )

        // Tải trạng thái SM-2 đã lưu từ SharedPreferences cho từng từ
        for (item in masterDeck) {
            loadItemState(item)
        }
    }

    /**
     * Lọc các từ đến hạn (nextReviewMillis <= now hoặc chưa từng ôn repetitions == 0)
     */
    private fun buildSessionQueue() {
        val now = System.currentTimeMillis()
        val dueWords = masterDeck.filter { it.repetitions == 0 || it.nextReviewMillis <= now }

        flashcardList.clear()
        if (dueWords.isNotEmpty()) {
            flashcardList.addAll(dueWords)
        } else {
            // Nếu toàn bộ đã ôn và chưa tới hạn, cho phép người dùng ôn tập toàn bộ deck
            flashcardList.addAll(masterDeck)
        }

        currentIndex = 0
        reviewedCount = 0
        progressBarSession.max = flashcardList.size
    }

    private fun loadItemState(item: FlashcardItem) {
        val prefix = "sm2_${item.id}_"
        if (prefs.contains("${prefix}ef")) {
            item.easeFactor = prefs.getFloat("${prefix}ef", item.easeFactor)
            item.intervalDays = prefs.getInt("${prefix}interval", item.intervalDays)
            item.repetitions = prefs.getInt("${prefix}rep", item.repetitions)
            item.nextReviewMillis = prefs.getLong("${prefix}next_review", item.nextReviewMillis)
            item.isBookmarked = prefs.getBoolean("${prefix}bookmark", item.isBookmarked)
        }
    }

    private fun saveItemState(item: FlashcardItem) {
        val prefix = "sm2_${item.id}_"
        prefs.edit().apply {
            putFloat("${prefix}ef", item.easeFactor)
            putInt("${prefix}interval", item.intervalDays)
            putInt("${prefix}rep", item.repetitions)
            putLong("${prefix}next_review", item.nextReviewMillis)
            putBoolean("${prefix}bookmark", item.isBookmarked)
            apply()
        }
    }

    private fun initViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnBack = findViewById(R.id.btnBack)
        btnShuffle = findViewById(R.id.btnShuffle)
        tabModeFlashcard = findViewById(R.id.tabModeFlashcard)
        tabModeQuiz = findViewById(R.id.tabModeQuiz)
        tvCardCounter = findViewById(R.id.tvCardCounter)
        tvQueueRemaining = findViewById(R.id.tvQueueRemaining)
        progressBarSession = findViewById(R.id.progressBarSession)

        cardContainer = findViewById(R.id.cardContainer)
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        cardCompletion = findViewById(R.id.cardCompletion)

        // Front views
        badgePosFront = findViewById(R.id.badgePosFront)
        badgeCefrFront = findViewById(R.id.badgeCefrFront)
        badgeTopicFront = findViewById(R.id.badgeTopicFront)
        btnBookmarkFront = findViewById(R.id.btnBookmarkFront)
        tvWordFront = findViewById(R.id.tvWordFront)
        tvPhoneticFront = findViewById(R.id.tvPhoneticFront)
        btnSpeakFront = findViewById(R.id.btnSpeakFront)

        // Back views
        scrollCardBack = findViewById(R.id.scrollCardBack)
        layoutCardBackContent = findViewById(R.id.layoutCardBackContent)
        tvWordBack = findViewById(R.id.tvWordBack)
        tvPhoneticBack = findViewById(R.id.tvPhoneticBack)
        btnSpeakBack = findViewById(R.id.btnSpeakBack)
        tvMeaningBack = findViewById(R.id.tvMeaningBack)
        tvExampleEnBack = findViewById(R.id.tvExampleEnBack)
        tvExampleViBack = findViewById(R.id.tvExampleViBack)
        tvMnemonicBack = findViewById(R.id.tvMnemonicBack)
        tvSrsStatsBack = findViewById(R.id.tvSrsStatsBack)

        // Action views
        layoutAnswerAction = findViewById(R.id.layoutAnswerAction)
        tvRememberPrompt = findViewById(R.id.tvRememberPrompt)
        btnShowAnswer = findViewById(R.id.btnShowAnswer)

        // Rating views
        ratingPrompt = findViewById(R.id.ratingPrompt)
        layoutRatingSection = findViewById(R.id.layoutRatingSection)
        layoutRatingButtonsRow = findViewById(R.id.layoutRatingButtonsRow)
        btnRatingAgain = findViewById(R.id.btnRatingAgain)
        btnRatingHard = findViewById(R.id.btnRatingHard)
        btnRatingGood = findViewById(R.id.btnRatingGood)
        btnRatingEasy = findViewById(R.id.btnRatingEasy)
        btnRetryCurrent = findViewById(R.id.btnRetryCurrent)

        // Completion views
        tvCompletionXp = findViewById(R.id.tvCompletionXp)
        tvCompletionStreak = findViewById(R.id.tvCompletionStreak)
        btnRestartSession = findViewById(R.id.btnRestartSession)
        btnBackHome = findViewById(R.id.btnBackHome)

        // Bottom Navigation
        bottomNavContainer = findViewById(R.id.bottomNavContainer)
        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupCameraDistance() {
        val scale = resources.displayMetrics.density
        cardFront.cameraDistance = 8000f * scale
        cardBack.cameraDistance = 8000f * scale
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        tabModeFlashcard.setOnClickListener {
            Toast.makeText(this, "Đang ở chế độ Flashcard 3D", Toast.LENGTH_SHORT).show()
        }

        // Lỗi 16: Không gọi finish(), chuyển tab với REORDER_TO_FRONT
        tabModeQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // Lỗi 10: Shuffle chỉ xáo trộn các thẻ chưa ôn
        btnShuffle.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            if (currentIndex >= flashcardList.size) {
                showCompletionState()
                return@setOnClickListener
            }

            val unreviewed = flashcardList.subList(currentIndex, flashcardList.size).toMutableList()
            unreviewed.shuffle()
            for (i in unreviewed.indices) {
                flashcardList[currentIndex + i] = unreviewed[i]
            }

            isFlipped = false
            showCard(currentIndex)
            Toast.makeText(this, "Đã xáo trộn các thẻ chưa ôn!", Toast.LENGTH_SHORT).show()
        }

        // Tap vào thẻ để lật 3D (Mặt trước -> Mặt sau)
        cardFront.setOnClickListener {
            if (!isFlipped && !isAnimating) flipCard3D()
        }

        // Tap vào thẻ để lật lại 3D (Mặt sau -> Mặt trước)
        val flipBackAction = {
            if (isFlipped && !isAnimating) {
                flipCard3D()
            }
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                flipBackAction()
                return true
            }
        })

        scrollCardBack.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.onTouchEvent(event)
        }

        cardBack.setOnClickListener { flipBackAction() }
        layoutCardBackContent.setOnClickListener { flipBackAction() }
        tvWordBack.setOnClickListener { flipBackAction() }
        tvPhoneticBack.setOnClickListener { flipBackAction() }
        tvMeaningBack.setOnClickListener { flipBackAction() }
        tvExampleEnBack.setOnClickListener { flipBackAction() }
        tvExampleViBack.setOnClickListener { flipBackAction() }
        tvMnemonicBack.setOnClickListener { flipBackAction() }
        tvSrsStatsBack.setOnClickListener { flipBackAction() }

        // Lỗi 5: Nút "Xem đáp án"
        btnShowAnswer.setOnClickListener {
            flipCard3D()
        }

        // Lỗi 5: Nút "Làm lại"
        btnRetryCurrent.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            if (flashcardList.isNotEmpty()) {
                currentIndex = 0
                isFlipped = false
                showCard(currentIndex)
                Toast.makeText(this, "Đã quay về thẻ đầu tiên của phiên!", Toast.LENGTH_SHORT).show()
            }
        }

        // Lỗi 15: Loa phát âm & Bookmark an toàn với kiểm tra index
        btnSpeakFront.setOnClickListener {
            if (flashcardList.isEmpty() || currentIndex !in flashcardList.indices) return@setOnClickListener
            speakWord(flashcardList[currentIndex].word)
        }

        btnSpeakBack.setOnClickListener {
            if (flashcardList.isEmpty() || currentIndex !in flashcardList.indices) return@setOnClickListener
            speakWord(flashcardList[currentIndex].word)
        }

        btnBookmarkFront.setOnClickListener {
            if (flashcardList.isEmpty() || currentIndex !in flashcardList.indices) return@setOnClickListener
            val item = flashcardList[currentIndex]
            item.isBookmarked = !item.isBookmarked
            updateBookmarkIcon(item.isBookmarked)
            saveItemState(item)
            val msg = if (item.isBookmarked) "Đã lưu vào danh sách yêu thích" else "Đã bỏ đánh dấu"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Lỗi 1: 4 nút đánh giá chuẩn SM-2
        btnRatingAgain.setOnClickListener { handleRating(ratingScore = 1) }
        btnRatingHard.setOnClickListener { handleRating(ratingScore = 3) }
        btnRatingGood.setOnClickListener { handleRating(ratingScore = 4) }
        btnRatingEasy.setOnClickListener { handleRating(ratingScore = 5) }

        // Lỗi 13: "Ôn lại phiên này" nạp lại từ đầu mà không xóa SM-2 đã lưu
        btnRestartSession.setOnClickListener {
            restartSession()
        }

        // Lỗi 9: Nút "Về Trang chủ" về đúng HomeActivity
        btnBackHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Lỗi 4: Xử lý thanh Bottom Navigation 6 mục
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
        }

        navNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        navSRS.setOnClickListener {
            Toast.makeText(this, "Bạn đang trong phòng Ôn tập Flashcard 3D", Toast.LENGTH_SHORT).show()
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

    /**
     * Khóa/Mở khóa 4 nút đánh giá SM-2 (Lỗi 6)
     */
    private fun setRatingButtonsEnabled(enabled: Boolean) {
        btnRatingAgain.isEnabled = enabled
        btnRatingHard.isEnabled = enabled
        btnRatingGood.isEnabled = enabled
        btnRatingEasy.isEnabled = enabled
        layoutRatingButtonsRow.alpha = if (enabled) 1.0f else 0.45f
    }

    /**
     * Hiển thị dữ liệu từ vựng cho thẻ hiện tại
     */
    private fun showCard(index: Int) {
        // Lỗi 15: Kiểm tra danh sách rỗng
        if (flashcardList.isEmpty()) {
            Toast.makeText(this, "Không có từ nào cần ôn hôm nay", Toast.LENGTH_SHORT).show()
            showCompletionState()
            return
        }

        if (index < 0 || index >= flashcardList.size) {
            showCompletionState()
            return
        }

        val item = flashcardList[index]

        // Cập nhật Header & Progress đúng chuẩn
        tvHeaderTitle.text = getString(R.string.title_flashcard_3d)
        tvCardCounter.text = "Thẻ ${index + 1} / ${flashcardList.size}"
        val remaining = flashcardList.size - index
        tvQueueRemaining.text = "$remaining từ cần ôn"
        progressBarSession.max = flashcardList.size
        progressBarSession.progress = index + 1

        // Đặt lại mặt trước hiển thị
        isFlipped = false
        cardFront.visibility = View.VISIBLE
        cardFront.alpha = 1f
        cardFront.rotationY = 0f
        cardFront.translationX = 0f

        cardBack.visibility = View.GONE
        cardBack.alpha = 1f
        cardBack.rotationY = 0f
        cardBack.translationX = 0f

        // Lỗi 5: Hiện nút "Xem đáp án" khi thẻ chưa lật
        layoutAnswerAction.visibility = View.VISIBLE
        btnShowAnswer.visibility = View.VISIBLE
        tvRememberPrompt.visibility = View.VISIBLE

        // Lỗi 6: Khóa nút đánh giá khi chưa lật thẻ
        setRatingButtonsEnabled(false)

        // Đổ dữ liệu Mặt Trước
        badgePosFront.text = item.pos
        badgeCefrFront.text = item.cefr
        badgeTopicFront.text = item.topic
        tvWordFront.text = item.word
        tvPhoneticFront.text = item.phonetic
        updateBookmarkIcon(item.isBookmarked)

        // Đổ dữ liệu Mặt Sau
        tvWordBack.text = item.word
        tvPhoneticBack.text = item.phonetic
        tvMeaningBack.text = item.meaning
        tvExampleEnBack.text = item.exampleEn
        tvExampleViBack.text = item.exampleVi
        tvMnemonicBack.text = item.mnemonic

        // Lỗi 12: Đổi "Lặp lại" thành "Khoảng cách"
        tvSrsStatsBack.text = "Lần ôn: ${item.repetitions} • Hệ số EF: ${String.format(Locale.US, "%.1f", item.easeFactor)} • Khoảng cách: ${item.intervalDays} ngày"
    }

    private fun updateBookmarkIcon(isBookmarked: Boolean) {
        if (isBookmarked) {
            btnBookmarkFront.setImageResource(R.drawable.ic_bookmark_gold)
        } else {
            btnBookmarkFront.setImageResource(R.drawable.ic_bookmark)
        }
    }

    /**
     * Hiệu ứng Lật thẻ 3D (Flip 3D) quanh trục Y mượt mà
     */
    private fun flipCard3D() {
        if (isAnimating) return
        isAnimating = true

        val halfDuration = 200L

        if (!isFlipped) {
            // LẬT TỪ TRƯỚC SANG SAU
            val animFrontOut = ObjectAnimator.ofFloat(cardFront, "rotationY", 0f, 90f).apply {
                duration = halfDuration
                interpolator = AccelerateInterpolator()
            }

            animFrontOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                    cardBack.rotationY = -90f

                    val animBackIn = ObjectAnimator.ofFloat(cardBack, "rotationY", -90f, 0f).apply {
                        duration = halfDuration
                        interpolator = DecelerateInterpolator()
                    }
                    animBackIn.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isFlipped = true
                            isAnimating = false

                            // Lỗi 5 & 6: Ẩn nút "Xem đáp án" và mở khóa nút đánh giá
                            layoutAnswerAction.visibility = View.GONE
                            setRatingButtonsEnabled(true)
                        }
                    })
                    animBackIn.start()
                }
            })
            animFrontOut.start()

        } else {
            // LẬT TỪ SAU TRỞ LẠI TRƯỚC
            val animBackOut = ObjectAnimator.ofFloat(cardBack, "rotationY", 0f, 90f).apply {
                duration = halfDuration
                interpolator = AccelerateInterpolator()
            }

            animBackOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardBack.visibility = View.GONE
                    cardFront.visibility = View.VISIBLE
                    cardFront.rotationY = -90f

                    val animFrontIn = ObjectAnimator.ofFloat(cardFront, "rotationY", -90f, 0f).apply {
                        duration = halfDuration
                        interpolator = DecelerateInterpolator()
                    }
                    animFrontIn.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isFlipped = false
                            isAnimating = false

                            // Lỗi 5 & 6: Hiện lại nút "Xem đáp án" và khóa nút đánh giá
                            layoutAnswerAction.visibility = View.VISIBLE
                            setRatingButtonsEnabled(false)
                        }
                    })
                    animFrontIn.start()
                }
            })
            animBackOut.start()
        }
    }

    /**
     * Xử lý đánh giá theo đúng chuẩn thuật toán Spaced Repetition SM-2 (Lỗi 1, 2, 6, 15)
     */
    private fun handleRating(ratingScore: Int) {
        // Lỗi 15: Kiểm tra an toàn
        if (flashcardList.isEmpty() || currentIndex !in flashcardList.indices) return

        // Lỗi 6: Chỉ cho phép đánh giá sau khi đã lật thẻ
        if (!isFlipped) {
            Toast.makeText(this, "Vui lòng xem đáp án trước khi tự đánh giá!", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAnimating) return

        val currentItem = flashcardList[currentIndex]
        val q = ratingScore
        val now = System.currentTimeMillis()

        // Tính toán lại hệ số EF theo công thức chuẩn SuperMemo SM-2
        val newEf = currentItem.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        currentItem.easeFactor = maxOf(1.3f, newEf)

        if (q < 3) {
            // Lỗi 1: Khi Quên (q = 1): reset repetitions về 0, ôn lại sau 10 phút, đưa thẻ xuống cuối hàng đợi
            currentItem.repetitions = 0
            currentItem.intervalDays = 0
            currentItem.nextReviewMillis = now + 10 * 60 * 1000L // 10 phút
            // Đưa bản sao của thẻ vào cuối hàng đợi phiên này để ôn lại
            flashcardList.add(currentItem.copy())
            progressBarSession.max = flashcardList.size
        } else {
            // Khó (q=3), Ổn (q=4), Dễ (q=5): tăng repetitions, tính interval theo số lần
            currentItem.repetitions += 1
            currentItem.intervalDays = when (currentItem.repetitions) {
                1 -> 1
                2 -> 6
                else -> maxOf(1, Math.round(currentItem.intervalDays * currentItem.easeFactor))
            }
            currentItem.nextReviewMillis = now + currentItem.intervalDays * 24L * 60 * 60 * 1000L
        }

        // Lỗi 2: Lưu bền vững vào SharedPreferences ngay sau khi đánh giá
        saveItemState(currentItem)
        reviewedCount++

        val activeCard = if (isFlipped) cardBack else cardFront

        // Hiệu ứng trượt thẻ ra ngoài (Slide Out) sang trái
        isAnimating = true
        activeCard.animate()
            .translationX(-500f)
            .alpha(0f)
            .setDuration(200L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeCard.translationX = 0f
                    activeCard.alpha = 1f
                    isAnimating = false

                    currentIndex++
                    if (currentIndex < flashcardList.size) {
                        showCard(currentIndex)
                        // Hiệu ứng thẻ mới trượt vào từ phải
                        cardFront.translationX = 500f
                        cardFront.alpha = 0f
                        cardFront.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(220L)
                            .start()
                    } else {
                        // Đã hoàn thành toàn bộ hàng đợi ôn tập hôm nay
                        showCompletionState()
                    }
                }
            })
            .start()
    }

    /**
     * Lỗi 17: Hiển thị màn hoàn thành với XP và streak thực tế
     */
    private fun showCompletionState() {
        cardFront.visibility = View.GONE
        cardBack.visibility = View.GONE
        cardCompletion.visibility = View.VISIBLE
        layoutAnswerAction.visibility = View.GONE
        ratingPrompt.visibility = View.GONE
        layoutRatingSection.visibility = View.GONE

        tvCardCounter.text = "Hoàn thành 100%"
        tvQueueRemaining.text = "0 từ còn lại"
        progressBarSession.progress = flashcardList.size

        // Tính XP: mỗi thẻ đã ôn tích lũy +5 XP
        val earnedXp = maxOf(5, reviewedCount * 5)
        tvCompletionXp.text = "+$earnedXp XP tích lũy"

        // Đọc streak từ SharedPreferences hoặc hiển thị 18 ngày khớp với Dashboard
        val savedStreak = prefs.getInt("user_learning_streak", 18)
        tvCompletionStreak.text = "$savedStreak ngày streak"

        Toast.makeText(this, "Chúc mừng! Bạn đã hoàn thành phiên ôn tập Flashcard 3D! 🎉", Toast.LENGTH_LONG).show()
    }

    /**
     * Lỗi 13: "Ôn lại phiên này" nạp lại hàng đợi ban đầu mà không xóa hay reset dữ liệu SM-2
     */
    private fun restartSession() {
        buildSessionQueue()
        isFlipped = false
        cardCompletion.visibility = View.GONE
        cardContainer.visibility = View.VISIBLE
        ratingPrompt.visibility = View.VISIBLE
        layoutRatingSection.visibility = View.VISIBLE
        showCard(currentIndex)
    }

    /**
     * Lỗi 14: Phát âm TextToSpeech chuẩn US với thông báo hợp lý
     */
    private fun speakWord(word: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "FlashcardTTS")
        } else {
            Toast.makeText(this, "Đang tải giọng đọc, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    // Lỗi 3: Xử lý xoay màn hình (onSaveInstanceState & onRestoreInstanceState)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("KEY_CURRENT_INDEX", currentIndex)
        outState.putBoolean("KEY_IS_FLIPPED", isFlipped)
        outState.putInt("KEY_REVIEWED_COUNT", reviewedCount)
        outState.putIntegerArrayList("KEY_SESSION_CARD_IDS", ArrayList(flashcardList.map { it.id }))
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        currentIndex = savedInstanceState.getInt("KEY_CURRENT_INDEX", 0)
        isFlipped = savedInstanceState.getBoolean("KEY_IS_FLIPPED", false)
        reviewedCount = savedInstanceState.getInt("KEY_REVIEWED_COUNT", 0)

        val cardIds = savedInstanceState.getIntegerArrayList("KEY_SESSION_CARD_IDS")
        flashcardList.clear()
        if (cardIds != null && cardIds.isNotEmpty()) {
            val masterMap = masterDeck.associateBy { it.id }
            for (id in cardIds) {
                masterMap[id]?.let { flashcardList.add(it.copy()) }
            }
        }
        if (flashcardList.isEmpty()) {
            buildSessionQueue()
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
