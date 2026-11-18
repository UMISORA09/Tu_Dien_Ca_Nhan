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
import androidx.lifecycle.lifecycleScope
import com.example.eduprofile.data.UserStatsRepository
import com.example.eduprofile.data.Word
import com.example.eduprofile.data.WordRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

// [SỬA] Cập nhật QuizActivity kết nối kho từ Firestore và đồng bộ điểm XP chuẩn
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

    // [THÊM] Repositories và Firebase Auth
    private val wordRepository = WordRepository()
    private val userStatsRepository = UserStatsRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)

        // [THÊM] Ẩn thanh trạng thái hệ thống
        hideSystemStatusBar()

        setupWindowInsets()
        initViews()
        setupListeners()

        tts = TextToSpeech(this, this)

        // [SỬA] Nạp câu hỏi động từ Firestore / Seed data thay vì danh sách tĩnh
        loadQuizQuestions()
    }

    override fun onResume() {
        super.onResume()
        // [THÊM] Đảm bảo ẩn thanh trạng thái khi quay lại màn hình
        hideSystemStatusBar()
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

    // [ĐÃ XÓA] initData() hard-code 8 câu hỏi tĩnh
    // [THÊM] loadQuizQuestions sinh câu hỏi ngẫu nhiên từ kho từ thật
    private fun loadQuizQuestions() {
        lifecycleScope.launch {
            try {
                val flowWords = try {
                    wordRepository.getApprovedWords().first()
                } catch (_: Exception) {
                    emptyList()
                }

                val words: List<Word> = if (flowWords.size >= 4) {
                    flowWords
                } else {
                    WordRepository.getSeedWordsList()
                }

                val targetList = words.shuffled()
                val questions = ArrayList<QuizQuestion>()
                val numQuestions = minOf(8, targetList.size)

                for (i in 0 until numQuestions) {
                    val target = targetList[i]
                    // Lấy 3 nghĩa sai từ các từ khác
                    val otherMeanings = words
                        .filter { it.id != target.id && it.word != target.word }
                        .map { it.meaning }
                        .shuffled()
                        .take(3)

                    // Gộp đáp án đúng và xáo trộn 4 đáp án
                    val allOptions = (otherMeanings + target.meaning).shuffled()
                    val correctIdx = allOptions.indexOf(target.meaning)

                    questions.add(
                        QuizQuestion(
                            id = i + 1,
                            word = target.word,
                            phonetic = target.ipa,
                            correctAnswerIndex = correctIdx,
                            options = allOptions
                        )
                    )
                }

                questionList.clear()
                questionList.addAll(questions)
                currentIndex = 0
                scoreXP = 0
                correctCount = 0
                progressBarSession.max = questionList.size
                updateScoreDisplay(animated = false)

                if (questionList.isNotEmpty()) {
                    loadQuestion(currentIndex)
                } else {
                    Toast.makeText(this@QuizActivity, "Chưa có đủ từ vựng để tạo bài kiểm tra", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Fallback nếu có lỗi
                val fallbackWords = WordRepository.getSeedWordsList().shuffled()
                val questions = ArrayList<QuizQuestion>()
                val num = minOf(8, fallbackWords.size)
                for (i in 0 until num) {
                    val target = fallbackWords[i]
                    val others = fallbackWords.filter { it.id != target.id }.map { it.meaning }.shuffled().take(3)
                    val opts = (others + target.meaning).shuffled()
                    questions.add(
                        QuizQuestion(
                            id = i + 1,
                            word = target.word,
                            phonetic = target.ipa,
                            correctAnswerIndex = opts.indexOf(target.meaning),
                            options = opts
                        )
                    )
                }
                questionList.clear()
                questionList.addAll(questions)
                currentIndex = 0
                scoreXP = 0
                correctCount = 0
                progressBarSession.max = questionList.size
                updateScoreDisplay(animated = false)
                loadQuestion(currentIndex)
            }
        }
    }

    private fun setupListeners() {
        // Chuyển sang Flashcard 3D
        tabModeFlashcard.setOnClickListener {
            val intent = Intent(this, FlashcardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        tabModeQuiz.setOnClickListener {
            Toast.makeText(this, "Đang trong chế độ Quiz Nhanh", Toast.LENGTH_SHORT).show()
        }

        // Phát âm khi chạm vào từ
        tvQuestionWord.setOnClickListener { speakCurrentWord() }
        tvQuestionPhonetic.setOnClickListener { speakCurrentWord() }

        // Lựa chọn đáp án
        layoutOption1.setOnClickListener { onOptionSelected(0) }
        layoutOption2.setOnClickListener { onOptionSelected(1) }
        layoutOption3.setOnClickListener { onOptionSelected(2) }
        layoutOption4.setOnClickListener { onOptionSelected(3) }

        // Làm lại bài Quiz
        btnRestartQuiz.setOnClickListener {
            restartQuiz()
        }

        // Quay lại trang chủ
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

        // Cập nhật phụ đề và thanh tiến trình
        tvHeaderSubtitle.text = "Thuật toán lặp lại ngắt quãng SM-2 • Câu ${index + 1}/${questionList.size}"
        val oldProgress = progressBarSession.progress
        val targetProgress = index + 1
        ObjectAnimator.ofInt(progressBarSession, "progress", oldProgress, targetProgress).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Cập nhật từ vựng câu hỏi
        tvQuestionWord.text = q.word
        tvQuestionPhonetic.text = q.phonetic

        // Cập nhật các lựa chọn
        tvOption1.text = q.options[0]
        tvOption2.text = q.options[1]
        tvOption3.text = q.options[2]
        tvOption4.text = q.options[3]

        // Hiệu ứng chuyển động mượt mà
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
            // Đúng: Hiển thị nền xanh
            optionLayouts[selectedIndex].setBackgroundResource(R.drawable.bg_option_item_correct)
            optionTexts[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            // Hiệu ứng phóng to nhẹ
            val pulseX = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "scaleX", 1f, 1.025f, 1f)
            val pulseY = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "scaleY", 1f, 1.025f, 1f)
            pulseX.duration = 280
            pulseY.duration = 280
            pulseX.start()
            pulseY.start()

            // [SỬA] Thang điểm chuẩn: Mỗi câu đúng +10 XP
            scoreXP += 10
            correctCount++
            updateScoreDisplay(animated = true)

            // [THÊM] Lưu điểm XP thật vào Firestore và ghi nhận ngày học
            val uid = auth.currentUser?.uid
            if (uid != null) {
                lifecycleScope.launch {
                    try {
                        userStatsRepository.addXp(uid, 10L)
                        userStatsRepository.recordStudyDay(uid)
                    } catch (_: Exception) {}
                }
            }

            Toast.makeText(this, "Chính xác! +10 XP 🎉", Toast.LENGTH_SHORT).show()

            // Sang câu tiếp theo sau 1200ms
            handler.postDelayed({
                currentIndex++
                loadQuestion(currentIndex)
            }, 1200)

        } else {
            // Sai: Hiển thị nền đỏ và rung
            optionLayouts[selectedIndex].setBackgroundResource(R.drawable.bg_option_item_wrong)
            optionTexts[selectedIndex].setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

            val shake = ObjectAnimator.ofFloat(optionLayouts[selectedIndex], "translationX", 0f, 20f, -20f, 15f, -15f, 8f, 0f)
            shake.duration = 450
            shake.interpolator = CycleInterpolator(1f)
            shake.start()

            // Tự động làm nổi bật đáp án đúng
            val correctIdx = q.correctAnswerIndex
            optionLayouts[correctIdx].setBackgroundResource(R.drawable.bg_option_item_correct)
            optionTexts[correctIdx].setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

            Toast.makeText(this, "Chưa chính xác! Hãy ghi nhớ đáp án đúng nhé", Toast.LENGTH_SHORT).show()

            // Sang câu tiếp theo sau 1800ms
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
        tvHeaderSubtitle.text = "Đã hoàn thành toàn bộ $total câu hỏi trắc nghiệm"
    }

    private fun restartQuiz() {
        cardCompletion.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    cardCompletion.visibility = View.GONE
                    cardQuizContainer.visibility = View.VISIBLE
                    // [SỬA] Nạp lại bộ câu hỏi ngẫu nhiên mới khi restart
                    loadQuizQuestions()
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
