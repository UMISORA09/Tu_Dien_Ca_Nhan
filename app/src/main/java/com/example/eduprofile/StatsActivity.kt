package com.example.eduprofile

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StatsActivity : AppCompatActivity() {

    // Header
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView

    // Streak Card
    private lateinit var cardStreak: CardView
    private lateinit var tvStreakBadge: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var tvStreakRecord: TextView
    private lateinit var tvStreakProgressDesc: TextView
    private lateinit var ivFlame: ImageView
    private lateinit var flameContainer: FrameLayout

    // Memory Distribution Card
    private lateinit var cardMemoryDistribution: CardView
    private lateinit var tvMemoryTitle: TextView
    private lateinit var boxNewWords: LinearLayout
    private lateinit var boxLearningWords: LinearLayout
    private lateinit var boxMasteredWords: LinearLayout
    private lateinit var tvCountNew: TextView
    private lateinit var tvCountLearning: TextView
    private lateinit var tvCountMastered: TextView
    private lateinit var barSegmentNew: View
    private lateinit var barSegmentLearning: View
    private lateinit var barSegmentMastered: View

    // Weekly Chart Card
    private lateinit var cardWeeklyReview: CardView
    private lateinit var tvWeeklyTotal: TextView
    private lateinit var colMon: LinearLayout
    private lateinit var colTue: LinearLayout
    private lateinit var colWed: LinearLayout
    private lateinit var colThu: LinearLayout
    private lateinit var colFri: LinearLayout
    private lateinit var colSat: LinearLayout
    private lateinit var colSun: LinearLayout

    private lateinit var barMon: View
    private lateinit var barTue: View
    private lateinit var barWed: View
    private lateinit var barThu: View
    private lateinit var barFri: View
    private lateinit var barSat: View
    private lateinit var barSun: View

    // Bottom Navigation (6 Tabs)
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    // Data Stats Mock
    private val streakDays = 18
    private val streakRecord = 24
    private val todayReviewed = 14
    private val todayTarget = 20

    private val countNew = 2
    private val countLearning = 4
    private val countMastered = 2

    private val weeklyCounts = listOf(
        Pair("Thứ 2", 15),
        Pair("Thứ 3", 22),
        Pair("Thứ 4", 12),
        Pair("Thứ 5", 25),
        Pair("Thứ 6", 18),
        Pair("Thứ 7", 20),
        Pair("Chủ nhật (Hôm nay)", 14)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)

        setupWindowInsets()
        initViews()
        setupListeners()
        bindData()
        startAnimations()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainStats)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.scrollContainer).setPadding(
                0,
                systemBars.top,
                0,
                0
            )
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

        cardStreak = findViewById(R.id.cardStreak)
        tvStreakBadge = findViewById(R.id.tvStreakBadge)
        tvStreakDays = findViewById(R.id.tvStreakDays)
        tvStreakRecord = findViewById(R.id.tvStreakRecord)
        tvStreakProgressDesc = findViewById(R.id.tvStreakProgressDesc)
        ivFlame = findViewById(R.id.ivFlame)
        flameContainer = findViewById(R.id.flameContainer)

        cardMemoryDistribution = findViewById(R.id.cardMemoryDistribution)
        tvMemoryTitle = findViewById(R.id.tvMemoryTitle)
        boxNewWords = findViewById(R.id.boxNewWords)
        boxLearningWords = findViewById(R.id.boxLearningWords)
        boxMasteredWords = findViewById(R.id.boxMasteredWords)
        tvCountNew = findViewById(R.id.tvCountNew)
        tvCountLearning = findViewById(R.id.tvCountLearning)
        tvCountMastered = findViewById(R.id.tvCountMastered)
        barSegmentNew = findViewById(R.id.barSegmentNew)
        barSegmentLearning = findViewById(R.id.barSegmentLearning)
        barSegmentMastered = findViewById(R.id.barSegmentMastered)

        cardWeeklyReview = findViewById(R.id.cardWeeklyReview)
        tvWeeklyTotal = findViewById(R.id.tvWeeklyTotal)

        colMon = findViewById(R.id.colMon)
        colTue = findViewById(R.id.colTue)
        colWed = findViewById(R.id.colWed)
        colThu = findViewById(R.id.colThu)
        colFri = findViewById(R.id.colFri)
        colSat = findViewById(R.id.colSat)
        colSun = findViewById(R.id.colSun)

        barMon = findViewById(R.id.barMon)
        barTue = findViewById(R.id.barTue)
        barWed = findViewById(R.id.barWed)
        barThu = findViewById(R.id.barThu)
        barFri = findViewById(R.id.barFri)
        barSat = findViewById(R.id.barSat)
        barSun = findViewById(R.id.barSun)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun bindData() {
        // 1. Streak
        tvStreakDays.text = "$streakDays Ngày"
        tvStreakRecord.text = getString(R.string.streak_record_format, streakRecord)
        tvStreakProgressDesc.text = getString(R.string.streak_today_progress_format, todayReviewed, todayTarget)

        // 2. Memory Distribution
        val totalWords = countNew + countLearning + countMastered
        tvMemoryTitle.text = getString(R.string.label_memory_distribution_format, totalWords)
        tvCountNew.text = countNew.toString()
        tvCountLearning.text = countLearning.toString()
        tvCountMastered.text = countMastered.toString()

        // 3. Weekly Total (Matching Figma: Tổng: 112 từ)
        tvWeeklyTotal.text = "Tổng: 112 từ"
    }

    private fun setupListeners() {
        // Streak Card Tap
        cardStreak.setOnClickListener {
            Toast.makeText(
                this,
                "🔥 Bạn đang duy trì chuỗi học tập $streakDays ngày liên tục! Cố lên nhé!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Memory Status Boxes Taps
        boxNewWords.setOnClickListener {
            Toast.makeText(this, "📚 Bạn có $countNew từ vựng mới thêm đang chờ học", Toast.LENGTH_SHORT).show()
        }
        boxLearningWords.setOnClickListener {
            Toast.makeText(this, "⚡ Bạn có $countLearning từ vựng đang ghi nhớ trong chu trình SM-2", Toast.LENGTH_SHORT).show()
        }
        boxMasteredWords.setOnClickListener {
            Toast.makeText(this, "⭐ Xuất sắc! Bạn đã thuộc lòng $countMastered từ vựng", Toast.LENGTH_SHORT).show()
        }

        // Weekly Bar Columns Taps
        val cols = listOf(colMon, colTue, colWed, colThu, colFri, colSat, colSun)
        val bars = listOf(barMon, barTue, barWed, barThu, barFri, barSat, barSun)

        cols.forEachIndexed { index, col ->
            col.setOnClickListener {
                val dayData = weeklyCounts[index]
                Toast.makeText(
                    this,
                    "📊 ${dayData.first}: Đã ôn ${dayData.second} từ vựng",
                    Toast.LENGTH_SHORT
                ).show()

                // Bounce animation on clicked bar
                val bounceBar = bars[index]
                val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                    bounceBar,
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1f),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1f)
                ).apply {
                    duration = 300
                    interpolator = OvershootInterpolator()
                }
                scaleUp.start()
            }
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
            finish()
        }

        navNotebook.setOnClickListener {
            startActivity(Intent(this, NotebookActivity::class.java))
            finish()
        }

        navSRS.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
            finish()
        }

        navStats.setOnClickListener {
            Toast.makeText(this, "Bạn đang ở màn hình Thống kê học tập", Toast.LENGTH_SHORT).show()
        }

        navAccount.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun startAnimations() {
        // 1. Animated Days Counter (0 -> streakDays)
        val daysAnimator = ValueAnimator.ofInt(0, streakDays)
        daysAnimator.duration = 800
        daysAnimator.interpolator = AccelerateDecelerateInterpolator()
        daysAnimator.addUpdateListener { anim ->
            tvStreakDays.text = "${anim.animatedValue} Ngày"
        }
        daysAnimator.start()

        // 2. Flame Pulse Animation
        val flamePulse = ObjectAnimator.ofPropertyValuesHolder(
            flameContainer,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f, 1f)
        ).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        flamePulse.start()

        // 3. Chart Bars Grow-up Animation
        val bars = listOf(barMon, barTue, barWed, barThu, barFri, barSat, barSun)
        bars.forEachIndexed { i, bar ->
            bar.pivotY = 105f * resources.displayMetrics.density
            val growAnim = ObjectAnimator.ofFloat(bar, "scaleY", 0f, 1f).apply {
                duration = 600 + (i * 70L)
                interpolator = OvershootInterpolator(1.2f)
            }
            growAnim.start()
        }
    }
}

