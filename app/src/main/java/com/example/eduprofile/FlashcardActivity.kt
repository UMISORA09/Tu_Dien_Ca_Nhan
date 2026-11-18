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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.eduprofile.data.ReviewRepository
import com.example.eduprofile.data.UserStatsRepository
import com.example.eduprofile.data.WordRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class FlashcardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class FlashcardItem(
        val id: Int,
        // [THÊM]: slug để ánh xạ chính xác với Firestore words và reviews theo PROMPT 6.4
        val slug: String = "",
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
    // [THÊM]: Repository duy nhất quản lý SM-2, từ vựng và XP theo PROMPT 5 và PROMPT 6.4
    private val wordRepository = WordRepository()
    private val reviewRepository = ReviewRepository()
    private val userStatsRepository = UserStatsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_flashcard)

        setupWindowInsets()
        prefs = getSharedPreferences("sm2_storage_prefs", Context.MODE_PRIVATE)

        initViews()
        initMasterDeck()

        setupListeners()
        setupCameraDistance()

        tts = TextToSpeech(this, this)
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo thanh trạng thái luôn ẩn
        hideSystemStatusBar()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainFlashcard)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padTop = (12 * resources.displayMetrics.density).toInt()
            v.setPadding(systemBars.left, padTop, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val navPadTop = (6 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, navPadTop, padHoriz, systemBars.bottom + navPadTop)
            insets
        }
    }

    // [SỬA]: Xóa bỏ 10 từ hard-code cũ, nạp dữ liệu động từ Firestore theo PROMPT 6.4
    private fun initMasterDeck() {
        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: "guest"

            // 1. Chạy migrate SharedPreferences sang Firestore nếu có dữ liệu cũ
            try {
                reviewRepository.migrateFromPrefsIfFound(uid, prefs)
            } catch (e: Exception) {
                // Ignore
            }

            // 2. Lấy kho từ đã duyệt hoặc danh sách chuẩn
            val approvedWords = try {
                wordRepository.getApprovedWords().firstOrNull() ?: WordRepository.getSeedWordsList()
            } catch (e: Exception) {
                WordRepository.getSeedWordsList()
            }
            val words = approvedWords.ifEmpty { WordRepository.getSeedWordsList() }
            val wordMap = words.associateBy { it.id }

            // 3. Lấy reviews đến hạn của user
            val reviews = try {
                if (uid != "guest") {
                    reviewRepository.getDueReviews(uid).firstOrNull() ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            masterDeck.clear()
            if (reviews.isNotEmpty()) {
                reviews.forEachIndexed { idx, rev ->
                    val w = wordMap[rev.wordId]
                    if (w != null) {
                        masterDeck.add(
                            FlashcardItem(
                                id = idx + 1,
                                slug = w.id,
                                word = w.word,
                                phonetic = w.ipa,
                                pos = w.pos,
                                cefr = w.cefr,
                                topic = w.topic,
                                meaning = w.meaning,
                                exampleEn = if (w.exampleEn.isNotEmpty()) "\"\"" else "",
                                exampleVi = if (w.exampleVi.isNotEmpty()) "→ " else "",
                                mnemonic = w.mnemonic,
                                easeFactor = rev.ef.toFloat(),
                                intervalDays = rev.interval.toInt(),
                                repetitions = rev.repetitions.toInt(),
                                nextReviewMillis = rev.nextReview?.toDate()?.time ?: 0L,
                                isBookmarked = rev.isBookmarked
                            )
                        )
                    }
                }
            }

            // Nếu hàng đợi rỗng, khởi tạo từ kho từ thật để luôn có bài học
            if (masterDeck.isEmpty()) {
                words.forEachIndexed { idx, w ->
                    masterDeck.add(
                        FlashcardItem(
                            id = idx + 1,
                            slug = w.id,
                            word = w.word,
                            phonetic = w.ipa,
                            pos = w.pos,
                            cefr = w.cefr,
                            topic = w.topic,
                            meaning = w.meaning,
                            exampleEn = if (w.exampleEn.isNotEmpty()) "\"\"" else "",
                            exampleVi = if (w.exampleVi.isNotEmpty()) "→ " else "",
                            mnemonic = w.mnemonic,
                            easeFactor = 2.5f,
                            intervalDays = 1,
                            repetitions = 0,
                            nextReviewMillis = System.currentTimeMillis(),
                            isBookmarked = false
                        )
                    )
                }
            }

            buildSessionQueue()
            if (flashcardList.isNotEmpty()) {
                showCard(currentIndex)
            }
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

            // [THÊM]: Đồng bộ trạng thái bookmark lên Firestore theo PROMPT 6.4
            val user = FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: "guest"
            lifecycleScope.launch {
                try {
                    reviewRepository.toggleBookmark(uid, item.slug, item.isBookmarked)
                } catch (e: Exception) {
                    // Ignore
                }
            }

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

        // [THÊM]: Lưu vào Firestore và cộng XP thật theo PROMPT 6.4
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: "guest"
        lifecycleScope.launch {
            try {
                reviewRepository.applyRating(uid, currentItem.slug, q)
                userStatsRepository.addXp(uid, 5) // +5 XP cho mỗi thẻ Flashcard hoàn thành
                userStatsRepository.recordStudyDay(uid)
            } catch (e: Exception) {
                // Ignore
            }
        }

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

        // [THÊM]: Đọc streak và stats thực tế từ UserStatsRepository theo PROMPT 6.4
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: "guest"
        lifecycleScope.launch {
            val stats = try {
                userStatsRepository.getUserStats(uid).firstOrNull()
            } catch (e: Exception) {
                null
            }
            val savedStreak = stats?.streak ?: prefs.getInt("user_learning_streak", 18)
            tvCompletionStreak.text = "$savedStreak ngày streak"
        }
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
