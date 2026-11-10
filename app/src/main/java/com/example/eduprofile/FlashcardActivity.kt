package com.example.eduprofile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
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
        var isBookmarked: Boolean = false
    )

    // Views - Header & Progress
    private lateinit var btnBack: FrameLayout
    private lateinit var btnShuffle: FrameLayout
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
    private lateinit var tvWordBack: TextView
    private lateinit var tvPhoneticBack: TextView
    private lateinit var btnSpeakBack: FrameLayout
    private lateinit var tvMeaningBack: TextView
    private lateinit var tvExampleEnBack: TextView
    private lateinit var tvExampleViBack: TextView
    private lateinit var tvMnemonicBack: TextView
    private lateinit var tvSrsStatsBack: TextView

    // Rating Views
    private lateinit var ratingPrompt: TextView
    private lateinit var layoutRatingSection: LinearLayout
    private lateinit var btnRatingAgain: LinearLayout
    private lateinit var btnRatingHard: LinearLayout
    private lateinit var btnRatingGood: LinearLayout
    private lateinit var btnRatingEasy: LinearLayout

    // Completion Views
    private lateinit var btnRestartSession: AppCompatButton
    private lateinit var btnBackHome: AppCompatButton

    // Data & State
    private val flashcardList = ArrayList<FlashcardItem>()
    private var currentIndex = 0
    private var isFlipped = false
    private var isAnimating = false
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)

        initData()
        initViews()
        setupListeners()
        setupCameraDistance()

        // Khởi tạo TextToSpeech
        tts = TextToSpeech(this, this)

        // Hiển thị thẻ đầu tiên
        showCard(currentIndex)
    }

    private fun initData() {
        flashcardList.clear()
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
        flashcardList.add(
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
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnShuffle = findViewById(R.id.btnShuffle)
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
        tvWordBack = findViewById(R.id.tvWordBack)
        tvPhoneticBack = findViewById(R.id.tvPhoneticBack)
        btnSpeakBack = findViewById(R.id.btnSpeakBack)
        tvMeaningBack = findViewById(R.id.tvMeaningBack)
        tvExampleEnBack = findViewById(R.id.tvExampleEnBack)
        tvExampleViBack = findViewById(R.id.tvExampleViBack)
        tvMnemonicBack = findViewById(R.id.tvMnemonicBack)
        tvSrsStatsBack = findViewById(R.id.tvSrsStatsBack)

        // Rating views
        ratingPrompt = findViewById(R.id.ratingPrompt)
        layoutRatingSection = findViewById(R.id.layoutRatingSection)
        btnRatingAgain = findViewById(R.id.btnRatingAgain)
        btnRatingHard = findViewById(R.id.btnRatingHard)
        btnRatingGood = findViewById(R.id.btnRatingGood)
        btnRatingEasy = findViewById(R.id.btnRatingEasy)

        // Completion views
        btnRestartSession = findViewById(R.id.btnRestartSession)
        btnBackHome = findViewById(R.id.btnBackHome)

        // Configure progress bar
        progressBarSession.max = flashcardList.size
    }

    private fun setupCameraDistance() {
        // Tăng camera distance để góc xoay 3D có độ sâu tự nhiên, không bị biến dạng hình học
        val scale = resources.displayMetrics.density
        cardFront.cameraDistance = 8000f * scale
        cardBack.cameraDistance = 8000f * scale
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnShuffle.setOnClickListener {
            flashcardList.shuffle()
            currentIndex = 0
            isFlipped = false
            showCard(currentIndex)
            Toast.makeText(this, "Đã xáo trộn thứ tự các thẻ Flashcard 3D!", Toast.LENGTH_SHORT).show()
        }

        // Tap vào thẻ (ở cả mặt trước và mặt sau) để lật 3D
        cardFront.setOnClickListener { flipCard3D() }
        cardBack.setOnClickListener { flipCard3D() }

        // Loa phát âm TextToSpeech
        btnSpeakFront.setOnClickListener {
            val item = flashcardList[currentIndex]
            speakWord(item.word)
        }

        btnSpeakBack.setOnClickListener {
            val item = flashcardList[currentIndex]
            speakWord(item.word)
        }

        // Đánh dấu Bookmark
        btnBookmarkFront.setOnClickListener {
            val item = flashcardList[currentIndex]
            item.isBookmarked = !item.isBookmarked
            updateBookmarkIcon(item.isBookmarked)
            val msg = if (item.isBookmarked) "Đã lưu vào danh sách yêu thích" else "Đã bỏ đánh dấu"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Các nút đánh giá thuật toán SM-2 Spaced Repetition
        btnRatingAgain.setOnClickListener { handleRating(ratingScore = 1, intervalDays = 1) }
        btnRatingHard.setOnClickListener { handleRating(ratingScore = 3, intervalDays = 2) }
        btnRatingGood.setOnClickListener { handleRating(ratingScore = 4, intervalDays = 4) }
        btnRatingEasy.setOnClickListener { handleRating(ratingScore = 5, intervalDays = 7) }

        // Màn hình hoàn thành
        btnRestartSession.setOnClickListener {
            currentIndex = 0
            isFlipped = false
            cardCompletion.visibility = View.GONE
            cardContainer.visibility = View.VISIBLE
            ratingPrompt.visibility = View.VISIBLE
            layoutRatingSection.visibility = View.VISIBLE
            showCard(currentIndex)
        }

        btnBackHome.setOnClickListener {
            finish()
        }
    }

    /**
     * Hiển thị dữ liệu từ vựng cho thẻ hiện tại
     */
    private fun showCard(index: Int) {
        if (index < 0 || index >= flashcardList.size) return

        val item = flashcardList[index]

        // Cập nhật Header & Progress
        tvCardCounter.text = "Thẻ ${index + 1} / ${flashcardList.size}"
        val remaining = flashcardList.size - index
        tvQueueRemaining.text = "$remaining từ cần ôn"
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
        tvSrsStatsBack.text = "Lần ôn: ${item.repetitions} • Hệ số EF: ${String.format(Locale.US, "%.1f", item.easeFactor)} • Lặp lại: ${item.intervalDays} ngày"
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

        val halfDuration = 220L

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
                        }
                    })
                    animFrontIn.start()
                }
            })
            animBackOut.start()
        }
    }

    /**
     * Xử lý đánh giá theo thuật toán Spaced Repetition SM-2
     */
    private fun handleRating(ratingScore: Int, intervalDays: Int) {
        if (isAnimating) return

        val currentItem = flashcardList[currentIndex]
        currentItem.repetitions += 1
        currentItem.intervalDays = intervalDays

        // Cập nhật hệ số EF (Ease Factor) theo công thức SM-2: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val q = ratingScore
        val newEf = currentItem.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        currentItem.easeFactor = maxOf(1.3f, newEf)

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

    private fun showCompletionState() {
        cardFront.visibility = View.GONE
        cardBack.visibility = View.GONE
        cardCompletion.visibility = View.VISIBLE
        ratingPrompt.visibility = View.GONE
        layoutRatingSection.visibility = View.GONE

        tvCardCounter.text = "Hoàn thành 100%"
        tvQueueRemaining.text = "0 từ còn lại"
        progressBarSession.progress = flashcardList.size

        Toast.makeText(this, "Chúc mừng! Bạn đã hoàn thành phiên ôn tập Flashcard 3D! 🎉", Toast.LENGTH_LONG).show()
    }

    /**
     * Phát âm TextToSpeech chuẩn US
     */
    private fun speakWord(word: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "FlashcardTTS")
        } else {
            Toast.makeText(this, "Đang phát âm: $word", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
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

