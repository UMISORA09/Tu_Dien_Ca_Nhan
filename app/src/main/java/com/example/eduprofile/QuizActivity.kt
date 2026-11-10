package com.example.eduprofile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class QuizQuestion(
        val id: Int,
        val word: String,
        val phonetic: String,
        val correctAnswerIndex: Int, // 0 for A, 1 for B, 2 for C, 3 for D
        val options: List<String>
    )

    // Header Views
    private lateinit var btnBack: FrameLayout
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView
    private lateinit var tabModeFlashcard: LinearLayout
    private lateinit var tabModeQuiz: LinearLayout
    private lateinit var progressBarSession: ProgressBar

    // Quiz Content Views
    private lateinit var layoutActiveQuiz: LinearLayout
    private lateinit var tvScoreXP: TextView
    private lateinit var tvQuestionWord: TextView
    private lateinit var tvQuestionPhonetic: TextView
    private lateinit var btnSpeakWord: ImageView
    private lateinit var cardQuestion: CardView

    // 4 Option Cards
    private lateinit var cardOptionA: CardView
    private lateinit var cardOptionB: CardView
    private lateinit var cardOptionC: CardView
    private lateinit var cardOptionD: CardView

    private lateinit var layoutOptionA: LinearLayout
    private lateinit var layoutOptionB: LinearLayout
    private lateinit var layoutOptionC: LinearLayout
    private lateinit var layoutOptionD: LinearLayout

    private lateinit var badgeContainerA: FrameLayout
    private lateinit var badgeContainerB: FrameLayout
    private lateinit var badgeContainerC: FrameLayout
    private lateinit var badgeContainerD: FrameLayout

    private lateinit var tvBadgeA: TextView
    private lateinit var tvBadgeB: TextView
    private lateinit var tvBadgeC: TextView
    private lateinit var tvBadgeD: TextView

    private lateinit var tvOptionA: TextView
    private lateinit var tvOptionB: TextView
    private lateinit var tvOptionC: TextView
    private lateinit var tvOptionD: TextView

    // Completion Views
    private lateinit var cardCompletion: CardView
    private lateinit var tvFinalXP: TextView
    private lateinit var tvFinalAccuracy: TextView
    private lateinit var btnRestartQuiz: AppCompatButton
    private lateinit var btnBackHome: AppCompatButton

    // Bottom Navigation
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout

    // State & Data
    private val questionList = ArrayList<QuizQuestion>()
    private var currentIndex = 0
    private var scoreXP = 0
    private var correctCount = 0
    private var isAnswerSelected = false
    private val handler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)

        setupWindowInsets()
        initViews()
        initData()
        setupListeners()

        tts = TextToSpeech(this, this)

        loadQuestion(currentIndex)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainQuiz)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (8 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)
        tabModeFlashcard = findViewById(R.id.tabModeFlashcard)
        tabModeQuiz = findViewById(R.id.tabModeQuiz)
        progressBarSession = findViewById(R.id.progressBarSession)

        layoutActiveQuiz = findViewById(R.id.layoutActiveQuiz)
        tvScoreXP = findViewById(R.id.tvScoreXP)
        tvQuestionWord = findViewById(R.id.tvQuestionWord)
        tvQuestionPhonetic = findViewById(R.id.tvQuestionPhonetic)
        btnSpeakWord = findViewById(R.id.btnSpeakWord)
        cardQuestion = findViewById(R.id.cardQuestion)

        cardOptionA = findViewById(R.id.cardOptionA)
        cardOptionB = findViewById(R.id.cardOptionB)
        cardOptionC = findViewById(R.id.cardOptionC)
        cardOptionD = findViewById(R.id.cardOptionD)

        layoutOptionA = findViewById(R.id.layoutOptionA)
        layoutOptionB = findViewById(R.id.layoutOptionB)
        layoutOptionC = findViewById(R.id.layoutOptionC)
        layoutOptionD = findViewById(R.id.layoutOptionD)

        badgeContainerA = findViewById(R.id.badgeContainerA)
        badgeContainerB = findViewById(R.id.badgeContainerB)
        badgeContainerC = findViewById(R.id.badgeContainerC)
        badgeContainerD = findViewById(R.id.badgeContainerD)

        tvBadgeA = findViewById(R.id.tvBadgeA)
        tvBadgeB = findViewById(R.id.tvBadgeB)
        tvBadgeC = findViewById(R.id.tvBadgeC)
        tvBadgeD = findViewById(R.id.tvBadgeD)

        tvOptionA = findViewById(R.id.tvOptionA)
        tvOptionB = findViewById(R.id.tvOptionB)
        tvOptionC = findViewById(R.id.tvOptionC)
        tvOptionD = findViewById(R.id.tvOptionD)

        cardCompletion = findViewById(R.id.cardCompletion)
        tvFinalXP = findViewById(R.id.tvFinalXP)
        tvFinalAccuracy = findViewById(R.id.tvFinalAccuracy)
        btnRestartQuiz = findViewById(R.id.btnRestartQuiz)
        btnBackHome = findViewById(R.id.btnBackHome)

        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
    }

    private fun initData() {
        questionList.clear()

        // 1. Serendipity
        questionList.add(
            QuizQuestion(
                id = 1,
                word = "Serendipity",
                phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                correctAnswerIndex = 0,
                options = listOf(
                    "Sự tình cờ may mắn; khả năng phát hiện ra những điều thú vị, có giá trị một cách ngẫu nhiên.",
                    "Kiên cường, có khả năng phục hồi nhanh chóng sau khó khăn, biến cố hoặc thay đổi (trong IT: hệ thống chịu lỗi).",
                    "(Công nghệ thông tin / Toán) Lũy đẳng; tính chất của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho cùng kết quả.",
                    "Cắn răng chịu đựng; quyết tâm đối mặt với một tình huống khó khăn không thể tránh khỏi."
                )
            )
        )

        // 2. Resilient
        questionList.add(
            QuizQuestion(
                id = 2,
                word = "Resilient",
                phonetic = "/rɪˈzɪl.i.ənt/",
                correctAnswerIndex = 0,
                options = listOf(
                    "Kiên cường, mau hồi phục sau khó khăn, biến cố hoặc khả năng đàn hồi co giãn của vật chất.",
                    "Phù du, chóng tàn, chỉ tồn tại hoặc kéo dài trong một thời gian rất ngắn ngủi.",
                    "Có mặt ở khắp mọi nơi, phổ biến rộng rãi khắp chốn.",
                    "Thực tế, thực dụng; giải quyết vấn đề dựa trên hiệu quả thực tế thay vì lý thuyết suông."
                )
            )
        )

        // 3. Idempotent
        questionList.add(
            QuizQuestion(
                id = 3,
                word = "Idempotent",
                phonetic = "/ˌaɪ.dɛmˈpoʊ.tənt/",
                correctAnswerIndex = 1,
                options = listOf(
                    "Hùng biện, lưu loát; có tài diễn đạt ý nghĩ và cảm xúc một cách thuyết phục và truyền cảm.",
                    "(Toán / IT) Lũy đẳng; đặc tính của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho kết quả giống hệt nhau.",
                    "Bền bỉ, ngoan cường, kiên trì theo đuổi mục tiêu đến cùng mà không bao giờ bỏ cuộc.",
                    "Sự tình cờ may mắn ngoài mong đợi trong cuộc sống."
                )
            )
        )

        // 4. Ephemeral
        questionList.add(
            QuizQuestion(
                id = 4,
                word = "Ephemeral",
                phonetic = "/ɪˈfem.ər.əl/",
                correctAnswerIndex = 2,
                options = listOf(
                    "Có mặt ở khắp mọi nơi, phổ biến trên toàn cầu.",
                    "Cắn răng chấp nhận hoàn cảnh khó khăn để vượt qua.",
                    "Phù du, chóng tàn, chỉ tồn tại hoặc kéo dài trong một thời gian rất ngắn ngủi.",
                    "Tái cấu trúc mã nguồn mà không thay đổi chức năng vốn có."
                )
            )
        )

        // 5. Pragmatic
        questionList.add(
            QuizQuestion(
                id = 5,
                word = "Pragmatic",
                phonetic = "/præɡˈmæt.ɪk/",
                correctAnswerIndex = 0,
                options = listOf(
                    "Thực tế, thực dụng; giải quyết vấn đề dựa trên hiệu quả thực tế thay vì lý thuyết suông.",
                    "Lời chúc may mắn mang tính biểu tượng trước buổi biểu diễn nghệ thuật.",
                    "Đặc tính bất biến khi thực thi liên tục nhiều phiên làm việc.",
                    "Khả năng đàn hồi và phục hồi trạng thái ổn định ban đầu."
                )
            )
        )

        // 6. Ubiquitous
        questionList.add(
            QuizQuestion(
                id = 6,
                word = "Ubiquitous",
                phonetic = "/juːˈbɪk.wə.təs/",
                correctAnswerIndex = 3,
                options = listOf(
                    "Bền bỉ, kiên định theo đuổi chí hướng đến cùng.",
                    "Vẻ đẹp mong manh thoáng chốc như giọt sương mai.",
                    "Sự tình cờ khám phá ra quy luật khoa học mới.",
                    "Có mặt ở khắp mọi nơi, phổ biến rộng rãi khắp chốn."
                )
            )
        )

        // 7. Eloquent
        questionList.add(
            QuizQuestion(
                id = 7,
                word = "Eloquent",
                phonetic = "/ˈel.ə.kwənt/",
                correctAnswerIndex = 1,
                options = listOf(
                    "Chú trọng thực tiễn, loại bỏ các chi tiết hàn lâm không cần thiết.",
                    "Hùng biện, lưu loát; có tài diễn đạt ý nghĩ và cảm xúc một cách thuyết phục và truyền cảm.",
                    "Tính chất bền bỉ trước áp lực công việc và thách thức kỹ thuật.",
                    "Lũy đẳng; bảo đảm tính toàn vẹn dữ liệu khi gửi trùng lặp gói tin."
                )
            )
        )

        // 8. Tenacious
        questionList.add(
            QuizQuestion(
                id = 8,
                word = "Tenacious",
                phonetic = "/təˈneɪ.ʃəs/",
                correctAnswerIndex = 0,
                options = listOf(
                    "Bền bỉ, ngoan cường, kiên trì theo đuổi mục tiêu đến cùng mà không bao giờ bỏ cuộc.",
                    "Xuất hiện phổ biến ở tất cả các nền tảng thiết bị di động.",
                    "May mắn tình cờ nảy sinh trong hoàn cảnh ngẫu nhiên.",
                    "Thích nghi nhanh với những biến động thời tiết khắc nghiệt."
                )
            )
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Mode Switcher: Click on Flashcard 3D -> launch FlashcardActivity
        tabModeFlashcard.setOnClickListener {
            val intent = Intent(this, FlashcardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Mode Switcher: Click on Quiz Nhanh -> already active
        tabModeQuiz.setOnClickListener {
            Toast.makeText(this, "Đang trong chế độ Quiz Trắc nghiệm", Toast.LENGTH_SHORT).show()
        }

        // Speak word
        btnSpeakWord.setOnClickListener {
            speakCurrentWord()
        }
        cardQuestion.setOnClickListener {
            speakCurrentWord()
        }

        // Options Click Listeners
        cardOptionA.setOnClickListener { onOptionSelected(0) }
        cardOptionB.setOnClickListener { onOptionSelected(1) }
        cardOptionC.setOnClickListener { onOptionSelected(2) }
        cardOptionD.setOnClickListener { onOptionSelected(3) }

        // Restart Quiz
        btnRestartQuiz.setOnClickListener {
            restartQuiz()
        }

        // Back Home
        btnBackHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Bottom Navigation
        navLookup.setOnClickListener {
            startActivity(Intent(this, LookupActivity::class.java))
        }

        navNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
        }

        navSRS.setOnClickListener {
            // Already in SRS mode
            Toast.makeText(this, "Bạn đang trong phòng Ôn tập SRS (Quiz Nhanh)", Toast.LENGTH_SHORT).show()
        }

        navStats.setOnClickListener {
            Toast.makeText(this, "Thống kê từ vựng: $scoreXP XP đã tích lũy trong phiên này", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadQuestion(index: Int) {
        if (index < 0 || index >= questionList.size) {
            showCompletionScreen()
            return
        }

        isAnswerSelected = false
        resetOptionStyles()

        val q = questionList[index]

        // Update Header & Progress
        tvHeaderSubtitle.text = "Thuật toán lặp lại ngắt quãng SM-2 • Thẻ ${index + 1}/${questionList.size}"
        val oldProgress = progressBarSession.progress
        val targetProgress = index + 1
        ObjectAnimator.ofInt(progressBarSession, "progress", oldProgress, targetProgress).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Update Question Word
        tvQuestionWord.text = q.word
        tvQuestionPhonetic.text = q.phonetic

        // Update Options
        tvOptionA.text = q.options[0]
        tvOptionB.text = q.options[1]
        tvOptionC.text = q.options[2]
        tvOptionD.text = q.options[3]

        // Animate question card entry
        cardQuestion.alpha = 0f
        cardQuestion.translationY = -20f
        cardQuestion.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()

        // Animate options cascade entry
        val optionCards = listOf(cardOptionA, cardOptionB, cardOptionC, cardOptionD)
        optionCards.forEachIndexed { i, card ->
            card.alpha = 0f
            card.translationY = 30f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setStartDelay((i * 45).toLong())
                .start()
        }

        // Optional: Speak word upon question appearance
        speakCurrentWord()
    }

    private fun onOptionSelected(selectedIndex: Int) {
        if (isAnswerSelected) return
        isAnswerSelected = true

        val q = questionList[currentIndex]
        val isCorrect = (selectedIndex == q.correctAnswerIndex)

        val layoutList = listOf(layoutOptionA, layoutOptionB, layoutOptionC, layoutOptionD)
        val badgeContainerList = listOf(badgeContainerA, badgeContainerB, badgeContainerC, badgeContainerD)
        val tvBadgeList = listOf(tvBadgeA, tvBadgeB, tvBadgeC, tvBadgeD)
        val cardList = listOf(cardOptionA, cardOptionB, cardOptionC, cardOptionD)

        if (isCorrect) {
            // Apply Correct Style to selected
            layoutList[selectedIndex].setBackgroundResource(R.drawable.bg_option_correct)
            badgeContainerList[selectedIndex].setBackgroundResource(R.drawable.bg_badge_option_correct)
            tvBadgeList[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            // Pulse animation on correct card
            val pulseX = ObjectAnimator.ofFloat(cardList[selectedIndex], "scaleX", 1f, 1.03f, 1f)
            val pulseY = ObjectAnimator.ofFloat(cardList[selectedIndex], "scaleY", 1f, 1.03f, 1f)
            pulseX.duration = 300
            pulseY.duration = 300
            pulseX.start()
            pulseY.start()

            // Update XP
            scoreXP += 10
            correctCount++
            updateScoreDisplay(animated = true)

            Toast.makeText(this, "Chính xác! +10 XP 🎉", Toast.LENGTH_SHORT).show()

            // Next question after 1200ms
            handler.postDelayed({
                currentIndex++
                loadQuestion(currentIndex)
            }, 1200)

        } else {
            // Apply Wrong Style to selected
            layoutList[selectedIndex].setBackgroundResource(R.drawable.bg_option_wrong)
            badgeContainerList[selectedIndex].setBackgroundResource(R.drawable.bg_badge_option_wrong)
            tvBadgeList[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

            // Shake animation on wrong card
            val shake = ObjectAnimator.ofFloat(cardList[selectedIndex], "translationX", 0f, 25f, -25f, 20f, -20f, 10f, 0f)
            shake.duration = 500
            shake.interpolator = CycleInterpolator(1f)
            shake.start()

            // Automatically highlight the correct answer in green
            val correctIndex = q.correctAnswerIndex
            layoutList[correctIndex].setBackgroundResource(R.drawable.bg_option_correct)
            badgeContainerList[correctIndex].setBackgroundResource(R.drawable.bg_badge_option_correct)
            tvBadgeList[correctIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            Toast.makeText(this, "Chưa chính xác! Hãy ghi nhớ đáp án đúng nhé", Toast.LENGTH_SHORT).show()

            // Next question after 1800ms
            handler.postDelayed({
                currentIndex++
                loadQuestion(currentIndex)
            }, 1800)
        }
    }

    private fun resetOptionStyles() {
        val layoutList = listOf(layoutOptionA, layoutOptionB, layoutOptionC, layoutOptionD)
        val badgeContainerList = listOf(badgeContainerA, badgeContainerB, badgeContainerC, badgeContainerD)
        val tvBadgeList = listOf(tvBadgeA, tvBadgeB, tvBadgeC, tvBadgeD)
        val defaultPurple = ContextCompat.getColor(this, R.color.primary_purple)

        for (i in 0..3) {
            layoutList[i].setBackgroundResource(R.drawable.bg_option_default)
            badgeContainerList[i].setBackgroundResource(R.drawable.bg_badge_option_default)
            tvBadgeList[i].setTextColor(defaultPurple)
        }
    }

    private fun updateScoreDisplay(animated: Boolean) {
        tvScoreXP.text = "Điểm XP: $scoreXP"
        if (animated) {
            val scaleX = ObjectAnimator.ofFloat(tvScoreXP, "scaleX", 1f, 1.35f, 1f)
            val scaleY = ObjectAnimator.ofFloat(tvScoreXP, "scaleY", 1f, 1.35f, 1f)
            scaleX.duration = 350
            scaleY.duration = 350
            scaleX.start()
            scaleY.start()
        }
    }

    private fun showCompletionScreen() {
        layoutActiveQuiz.visibility = View.GONE
        cardCompletion.visibility = View.VISIBLE
        cardCompletion.alpha = 0f
        cardCompletion.scaleX = 0.9f
        cardCompletion.scaleY = 0.9f

        cardCompletion.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        tvFinalXP.text = "+$scoreXP XP"
        val total = questionList.size
        val percent = if (total > 0) (correctCount * 100) / total else 0
        tvFinalAccuracy.text = "$correctCount/$total ($percent%)"
        tvHeaderSubtitle.text = "Đã hoàn thành toàn bộ 8 câu hỏi trắc nghiệm"
    }

    private fun restartQuiz() {
        currentIndex = 0
        scoreXP = 0
        correctCount = 0
        updateScoreDisplay(animated = false)

        cardCompletion.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardCompletion.visibility = View.GONE
                    layoutActiveQuiz.visibility = View.VISIBLE
                    loadQuestion(currentIndex)
                }
            })
            .start()
    }

    private fun speakCurrentWord() {
        if (!isTtsReady || currentIndex >= questionList.size) return
        val word = questionList[currentIndex].word
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "QuizWord_${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
