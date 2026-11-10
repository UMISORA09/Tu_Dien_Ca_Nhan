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
        val correctAnswerIndex: Int, // 0 for Option 1, 1 for Option 2, etc.
        val options: List<String>
    )

    // Header Views
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView
    private lateinit var tabModeFlashcard: LinearLayout
    private lateinit var tabModeQuiz: LinearLayout
    private lateinit var progressBarSession: ProgressBar

    // Quiz Container & Target Word
    private lateinit var cardQuizContainer: CardView
    private lateinit var tvQuizBadgeScore: TextView
    private lateinit var tvQuestionWord: TextView
    private lateinit var tvQuestionPhonetic: TextView

    // 4 Option Cards
    private lateinit var layoutOption1: LinearLayout
    private lateinit var layoutOption2: LinearLayout
    private lateinit var layoutOption3: LinearLayout
    private lateinit var layoutOption4: LinearLayout

    private lateinit var tvOption1: TextView
    private lateinit var tvOption2: TextView
    private lateinit var tvOption3: TextView
    private lateinit var tvOption4: TextView

    // Completion Views
    private lateinit var cardCompletion: CardView
    private lateinit var tvFinalXP: TextView
    private lateinit var tvFinalAccuracy: TextView
    private lateinit var btnRestartQuiz: AppCompatButton
    private lateinit var btnBackHome: AppCompatButton

    // Bottom Navigation (6 tabs matching Figma)
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

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
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }
    }

    private fun initViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)
        tabModeFlashcard = findViewById(R.id.tabModeFlashcard)
        tabModeQuiz = findViewById(R.id.tabModeQuiz)
        progressBarSession = findViewById(R.id.progressBarSession)

        cardQuizContainer = findViewById(R.id.cardQuizContainer)
        tvQuizBadgeScore = findViewById(R.id.tvQuizBadgeScore)
        tvQuestionWord = findViewById(R.id.tvQuestionWord)
        tvQuestionPhonetic = findViewById(R.id.tvQuestionPhonetic)

        layoutOption1 = findViewById(R.id.layoutOption1)
        layoutOption2 = findViewById(R.id.layoutOption2)
        layoutOption3 = findViewById(R.id.layoutOption3)
        layoutOption4 = findViewById(R.id.layoutOption4)

        tvOption1 = findViewById(R.id.tvOption1)
        tvOption2 = findViewById(R.id.tvOption2)
        tvOption3 = findViewById(R.id.tvOption3)
        tvOption4 = findViewById(R.id.tvOption4)

        cardCompletion = findViewById(R.id.cardCompletion)
        tvFinalXP = findViewById(R.id.tvFinalXP)
        tvFinalAccuracy = findViewById(R.id.tvFinalAccuracy)
        btnRestartQuiz = findViewById(R.id.btnRestartQuiz)
        btnBackHome = findViewById(R.id.btnBackHome)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun initData() {
        questionList.clear()

        // 1. Serendipity (Exact text from Figma)
        questionList.add(
            QuizQuestion(
                id = 1,
                word = "Serendipity",
                phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                correctAnswerIndex = 0,
                options = listOf(
                    "Sự tình cờ may mắn; khả năng phát hiện ra những điều thú vị, có giá trị một cách ngẫu nhiên.",
                    "Kiên cường, có khả năng phục hồi nhanh chóng sau khó khăn, biến cố hoặc thay đổi (trong IT: hệ thống chịu lỗi tốt).",
                    "(Công nghệ thông tin / Toán) Lũy đẳng; tính chất của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho ra cùng một kết quả mà không gây tác dụng phụ.",
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
                    "Kiên cường, có khả năng phục hồi nhanh chóng sau khó khăn, biến cố hoặc thay đổi (trong IT: hệ thống chịu lỗi tốt).",
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
                    "(Công nghệ thông tin / Toán) Lũy đẳng; tính chất của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho ra cùng một kết quả mà không gây tác dụng phụ.",
                    "Bền bỉ, ngoan cường, kiên trì theo đuổi mục tiêu đến cùng mà không bao giờ bỏ cuộc.",
                    "Sự tình cờ may mắn; khả năng phát hiện ra những điều thú vị ngoài mong đợi."
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
                    "Có mặt ở khắp mọi nơi, phổ biến trên quy mô toàn cầu.",
                    "Cắn răng chịu đựng; quyết tâm đối mặt với thử thách khó khăn.",
                    "Phù du, chóng tàn, chỉ tồn tại hoặc kéo dài trong một thời gian rất ngắn ngủi.",
                    "Tái cấu trúc mã nguồn mà không làm thay đổi hành vi bên ngoài của hệ thống."
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
                    "Lời chúc may mắn mang ý nghĩa chúc diễn viên biểu diễn thành công tốt đẹp.",
                    "Tính chất bền vững, duy trì trạng thái nhất quán sau các sự cố gián đoạn.",
                    "Khả năng phục hồi và tái tạo năng lượng sau chuỗi ngày làm việc căng thẳng."
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
                    "Bền bỉ, kiên định theo đuổi mục tiêu chiến lược của dự án.",
                    "Vẻ đẹp mong manh, thoáng chốc như giọt sương mai đầu ngày.",
                    "Sự tình cờ phát hiện ra một phát minh công nghệ có giá trị cao.",
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
                    "Ưu tiên hành động thực tiễn, hạn chế tối đa các tranh luận lý thuyết.",
                    "Hùng biện, lưu loát; có tài diễn đạt ý nghĩ và cảm xúc một cách thuyết phục và truyền cảm.",
                    "Kiên trì vượt qua mọi rào cản và khó khăn để đạt thành quả.",
                    "Lũy đẳng; đảm bảo tính toàn vẹn khi thực hiện lặp lại một lệnh gọi API."
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
                    "Phổ biến rộng khắp, xuất hiện trong mọi ngóc ngách của đời sống xã hội.",
                    "Khả năng nắm bắt cơ hội may mắn và chuyển hóa thành thành công vượt bậc.",
                    "Khả năng thích ứng linh hoạt trước những thay đổi khó lường của môi trường."
                )
            )
        )
    }

    private fun setupListeners() {
        // Mode Switcher: Click on Flashcard 3D -> launch FlashcardActivity
        tabModeFlashcard.setOnClickListener {
            val intent = Intent(this, FlashcardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Mode Switcher: Click on Quiz Nhanh -> already active
        tabModeQuiz.setOnClickListener {
            Toast.makeText(this, "Đang trong chế độ Quiz Nhanh", Toast.LENGTH_SHORT).show()
        }

        // Tap word to pronounce
        tvQuestionWord.setOnClickListener {
            speakCurrentWord()
        }
        tvQuestionPhonetic.setOnClickListener {
            speakCurrentWord()
        }

        // Options Click Listeners
        layoutOption1.setOnClickListener { onOptionSelected(0) }
        layoutOption2.setOnClickListener { onOptionSelected(1) }
        layoutOption3.setOnClickListener { onOptionSelected(2) }
        layoutOption4.setOnClickListener { onOptionSelected(3) }

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

        // Bottom Navigation (6 Tabs)
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
            Toast.makeText(this, "Bạn đang trong phòng Ôn tập SRS (Quiz Nhanh)", Toast.LENGTH_SHORT).show()
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
        tvOption1.text = q.options[0]
        tvOption2.text = q.options[1]
        tvOption3.text = q.options[2]
        tvOption4.text = q.options[3]

        // Smooth entry animation for options
        val optionLayouts = listOf(layoutOption1, layoutOption2, layoutOption3, layoutOption4)
        optionLayouts.forEachIndexed { i, layout ->
            layout.alpha = 0f
            layout.translationY = 24f
            layout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .setStartDelay((i * 40).toLong())
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
        val optionLayouts = listOf(layoutOption1, layoutOption2, layoutOption3, layoutOption4)
        val optionTexts = listOf(tvOption1, tvOption2, tvOption3, tvOption4)

        if (isCorrect) {
            // Correct Style
            optionLayouts[selectedIndex].setBackgroundResource(R.drawable.bg_option_item_correct)
            optionTexts[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            // Pulse animation
            val pulseX = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "scaleX", 1f, 1.025f, 1f)
            val pulseY = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "scaleY", 1f, 1.025f, 1f)
            pulseX.duration = 280
            pulseY.duration = 280
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
            // Wrong Style
            optionLayouts[selectedIndex].setBackgroundResource(R.drawable.bg_option_item_wrong)
            optionTexts[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

            // Shake animation
            val shake = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "translationX", 0f, 20f, -20f, 15f, -15f, 8f, 0f)
            shake.duration = 450
            shake.interpolator = CycleInterpolator(1f)
            shake.start()

            // Automatically highlight correct answer
            val correctIdx = q.correctAnswerIndex
            optionLayouts[correctIdx].setBackgroundResource(R.drawable.bg_option_item_correct)
            optionTexts[correctIdx].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            Toast.makeText(this, "Chưa chính xác! Hãy ghi nhớ đáp án đúng nhé", Toast.LENGTH_SHORT).show()

            // Next question after 1800ms
            handler.postDelayed({
                currentIndex++
                loadQuestion(currentIndex)
            }, 1800)
        }
    }

    private fun resetOptionStyles() {
        val optionLayouts = listOf(layoutOption1, layoutOption2, layoutOption3, layoutOption4)
        val optionTexts = listOf(tvOption1, tvOption2, tvOption3, tvOption4)
        val defaultTextColor = ContextCompat.getColor(this, R.color.text_primary)

        for (i in 0..3) {
            optionLayouts[i].setBackgroundResource(R.drawable.bg_option_item_default)
            optionTexts[i].setTextColor(defaultTextColor)
            optionLayouts[i].scaleX = 1f
            optionLayouts[i].scaleY = 1f
            optionLayouts[i].translationX = 0f
        }
    }

    private fun updateScoreDisplay(animated: Boolean) {
        tvQuizBadgeScore.text = "TRẮC NGHIỆM QUIZ • ĐIỂM: $scoreXP"
        if (animated) {
            val scaleX = ObjectAnimator.ofFloat(tvQuizBadgeScore, "scaleX", 1f, 1.25f, 1f)
            val scaleY = ObjectAnimator.ofFloat(tvQuizBadgeScore, "scaleY", 1f, 1.25f, 1f)
            scaleX.duration = 300
            scaleY.duration = 300
            scaleX.start()
            scaleY.start()
        }
    }

    private fun showCompletionScreen() {
        cardQuizContainer.visibility = View.GONE
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
                    cardQuizContainer.visibility = View.VISIBLE
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
