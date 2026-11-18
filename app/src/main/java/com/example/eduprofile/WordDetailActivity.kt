package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.eduprofile.data.ReviewRepository
import com.example.eduprofile.data.Word
import com.example.eduprofile.data.WordRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class WordDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // [ĐÃ XÓA]: Toàn bộ danh sách 6 từ hard-code wordDatabase theo PROMPT 6.1

    private val wordRepository = WordRepository()
    private val reviewRepository = ReviewRepository()

    private var currentWord: Word = Word()
    private var isFavorite: Boolean = false

    // Views
    private lateinit var scrollContainer: NestedScrollView
    // [SỬA]: btnBack là LinearLayout trong activity_word_detail.xml, sửa kiểu dữ liệu để tránh ClassCastException
    private lateinit var btnBack: LinearLayout
    private lateinit var btnFolderSelect: LinearLayout
    private lateinit var tvFolderName: TextView
    private lateinit var btnFavorite: FrameLayout
    private lateinit var ivFavoriteIcon: ImageView

    // Word Header
    private lateinit var badgePos: TextView
    private lateinit var badgeCefr: TextView
    private lateinit var badgeStatus: TextView
    private lateinit var tvWordTitle: TextView
    private lateinit var tvWordIpa: TextView
    private lateinit var btnAudioUs: LinearLayout
    private lateinit var btnAudioUk: LinearLayout

    // Detail Tabs
    private lateinit var tabMeaning: TextView
    private lateinit var tabSynAnt: TextView
    private lateinit var tabCollocations: TextView
    private lateinit var tabNotes: TextView

    // Tab Contents
    private lateinit var layoutContentMeaning: LinearLayout
    private lateinit var layoutContentSynAnt: LinearLayout
    private lateinit var layoutContentCollocations: LinearLayout
    private lateinit var layoutContentNotes: LinearLayout

    // Meaning Content
    private lateinit var tvWordMeaning: TextView
    private lateinit var tvHeaderExamples: TextView
    private lateinit var tvExample1En: TextView
    private lateinit var tvExample1Vi: TextView
    // [SỬA]: btnPlayEx1 và btnPlayEx2 là ImageView trong XML
    private lateinit var btnPlayEx1: ImageView
    private lateinit var tvExample2En: TextView
    private lateinit var tvExample2Vi: TextView
    private lateinit var btnPlayEx2: ImageView

    // Other Contents
    private lateinit var containerSynonyms: LinearLayout
    private lateinit var containerAntonyms: LinearLayout
    private lateinit var containerCollocations: LinearLayout
    private lateinit var tvMnemonicContent: TextView
    private lateinit var tvGrammarNote: TextView

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // [THÊM]: Ẩn thanh trạng thái hệ thống theo yêu cầu "không hiện phần này"
        hideSystemStatusBar()
        setContentView(R.layout.activity_word_detail)

        setupWindowInsets()
        initViews()
        setupListeners()
        loadWordDataFromFirestore()

        tts = TextToSpeech(this, this)
    }

    override fun onResume() {
        super.onResume()
        // [THÊM]: Đảm bảo Status Bar luôn ẩn
        hideSystemStatusBar()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainWordDetail)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // [SỬA]: padTop = 12dp để không bị cấn khi ẩn Status Bar
            val padTop = (12 * resources.displayMetrics.density).toInt()
            scrollContainer.setPadding(0, padTop, 0, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val navPadTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, navPadTop, padHoriz, systemBars.bottom + navPadTop)
            insets
        }
    }

    private fun initViews() {
        scrollContainer = findViewById(R.id.scrollContainer)
        btnBack = findViewById(R.id.btnBack)
        btnFolderSelect = findViewById(R.id.btnFolderSelect)
        tvFolderName = findViewById(R.id.tvFolderName)
        btnFavorite = findViewById(R.id.btnFavorite)
        ivFavoriteIcon = findViewById(R.id.ivFavoriteIcon)

        badgePos = findViewById(R.id.badgePos)
        badgeCefr = findViewById(R.id.badgeCefr)
        badgeStatus = findViewById(R.id.badgeStatus)
        tvWordTitle = findViewById(R.id.tvWordTitle)
        tvWordIpa = findViewById(R.id.tvWordIpa)
        btnAudioUs = findViewById(R.id.btnAudioUs)
        btnAudioUk = findViewById(R.id.btnAudioUk)

        tabMeaning = findViewById(R.id.tabMeaning)
        tabSynAnt = findViewById(R.id.tabSynAnt)
        tabCollocations = findViewById(R.id.tabCollocations)
        tabNotes = findViewById(R.id.tabNotes)

        layoutContentMeaning = findViewById(R.id.layoutContentMeaning)
        layoutContentSynAnt = findViewById(R.id.layoutContentSynAnt)
        layoutContentCollocations = findViewById(R.id.layoutContentCollocations)
        layoutContentNotes = findViewById(R.id.layoutContentNotes)

        tvWordMeaning = findViewById(R.id.tvWordMeaning)
        tvHeaderExamples = findViewById(R.id.tvHeaderExamples)
        tvExample1En = findViewById(R.id.tvExample1En)
        tvExample1Vi = findViewById(R.id.tvExample1Vi)
        btnPlayEx1 = findViewById(R.id.btnPlayEx1)
        tvExample2En = findViewById(R.id.tvExample2En)
        tvExample2Vi = findViewById(R.id.tvExample2Vi)
        btnPlayEx2 = findViewById(R.id.btnPlayEx2)

        containerSynonyms = findViewById(R.id.containerSynonyms)
        containerAntonyms = findViewById(R.id.containerAntonyms)
        containerCollocations = findViewById(R.id.containerCollocations)
        tvMnemonicContent = findViewById(R.id.tvMnemonicContent)
        tvGrammarNote = findViewById(R.id.tvGrammarNote)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    // [THÊM]: Tải dữ liệu từ vựng thật từ Firestore words/{slug} theo PROMPT 6.1
    private fun loadWordDataFromFirestore() {
        val rawParam = intent.getStringExtra("EXTRA_WORD") ?: intent.getStringExtra("word") ?: "serendipity"
        val slug = rawParam.trim().lowercase().replace("[^a-z0-9\\s-]".toRegex(), "").replace("\\s+".toRegex(), "-")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch {
                val review = reviewRepository.getReview(uid, slug)
                isFavorite = review?.isBookmarked ?: false
                updateFavoriteIcon()
            }
        }

        lifecycleScope.launch {
            wordRepository.getWord(slug).collectLatest { word ->
                if (word != null) {
                    currentWord = word
                    bindData(word)
                } else {
                    // Fallback thử tìm trong danh sách seed chuẩn
                    val fallback = WordRepository.getSeedWordsList().find { it.id == slug }
                    if (fallback != null) {
                        currentWord = fallback
                        bindData(fallback)
                    }
                }
            }
        }
    }

    private fun bindData(word: Word) {
        tvFolderName.text = if (word.topic.isNotEmpty()) word.topic else "Từ vựng chung"
        badgePos.text = word.pos
        badgeCefr.text = word.cefr
        badgeStatus.text = if (word.status == "approved") "Đã duyệt" else "Chờ duyệt"
        tvWordTitle.text = word.word
        tvWordIpa.text = word.ipa
        tvWordMeaning.text = word.meaning

        updateFavoriteIcon()

        // Examples
        val exList = word.examples
        if (exList.isNotEmpty()) {
            tvHeaderExamples.text = "VÍ DỤ TRONG NGỮ CẢNH (${exList.size})"
            tvExample1En.text = exList[0]["en"] ?: ""
            tvExample1Vi.text = exList[0]["vi"] ?: ""
            if (exList.size > 1) {
                tvExample2En.text = exList[1]["en"] ?: ""
                tvExample2Vi.text = exList[1]["vi"] ?: ""
            } else {
                tvExample2En.text = ""
                tvExample2Vi.text = ""
            }
        } else if (word.exampleEn.isNotEmpty()) {
            tvHeaderExamples.text = "VÍ DỤ TRONG NGỮ CẢNH (1)"
            tvExample1En.text = word.exampleEn
            tvExample1Vi.text = word.exampleVi
            tvExample2En.text = ""
            tvExample2Vi.text = ""
        } else {
            tvHeaderExamples.text = "VÍ DỤ TRONG NGỮ CẢNH (0)"
            tvExample1En.text = "Chưa có câu ví dụ."
            tvExample1Vi.text = ""
            tvExample2En.text = ""
            tvExample2Vi.text = ""
        }

        // Synonyms
        containerSynonyms.removeAllViews()
        val synRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }
        word.synonyms.forEach { syn ->
            val tv = TextView(this).apply {
                text = syn
                setBackgroundResource(R.drawable.bg_chip_synonym)
                setTextColor(ContextCompat.getColor(context, R.color.admin_green_text))
                textSize = 12f
                val ph = (10 * resources.displayMetrics.density).toInt()
                val pv = (5 * resources.displayMetrics.density).toInt()
                setPadding(ph, pv, ph, pv)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                }
                layoutParams = lp
            }
            synRow.addView(tv)
        }
        containerSynonyms.addView(synRow)

        // Antonyms
        containerAntonyms.removeAllViews()
        val antRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }
        word.antonyms.forEach { ant ->
            val tv = TextView(this).apply {
                text = ant
                setBackgroundResource(R.drawable.bg_chip_antonym)
                setTextColor(ContextCompat.getColor(context, R.color.admin_red_text))
                textSize = 12f
                val ph = (10 * resources.displayMetrics.density).toInt()
                val pv = (5 * resources.displayMetrics.density).toInt()
                setPadding(ph, pv, ph, pv)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
                }
                layoutParams = lp
            }
            antRow.addView(tv)
        }
        containerAntonyms.addView(antRow)

        // Collocations
        containerCollocations.removeAllViews()
        word.collocations.forEach { col ->
            val tv = TextView(this).apply {
                text = "• $col"
                setTextColor(ContextCompat.getColor(context, R.color.admin_slate_text))
                textSize = 13f
                val pv = (4 * resources.displayMetrics.density).toInt()
                setPadding(0, pv, 0, pv)
            }
            containerCollocations.addView(tv)
        }

        // Mnemonic & Grammar Notes
        tvMnemonicContent.text = if (word.mnemonic.isNotEmpty()) word.mnemonic else word.personalNote.ifEmpty { "Chưa có mẹo nhớ." }
        tvGrammarNote.text = word.grammarNote.ifEmpty { "Không có ghi chú ngữ pháp đặc biệt." }
    }

    private fun updateFavoriteIcon() {
        ivFavoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // [SỬA]: Nút yêu thích lưu trạng thái thật vào Firestore reviews/{uid}_{slug}.isBookmarked
        btnFavorite.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Đăng nhập để lưu từ vào danh sách yêu thích", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isFavorite = !isFavorite
            updateFavoriteIcon()

            val msg = if (isFavorite) "Đã lưu từ vào Sổ tay yêu thích ⭐" else "Đã bỏ lưu khỏi danh sách yêu thích"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                reviewRepository.toggleBookmark(uid, currentWord.id, isFavorite)
            }
        }

        // Folder selection dialog
        btnFolderSelect.setOnClickListener {
            val folders = arrayOf(
                "Chuyên ngành IT",
                "Giao tiếp hàng ngày",
                "IELTS Reading & Academic",
                "Idioms & Phrasal Verbs"
            )
            MaterialAlertDialogBuilder(this)
                .setTitle("Chọn sổ tay lưu từ")
                .setItems(folders) { _, which ->
                    tvFolderName.text = folders[which]
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        lifecycleScope.launch {
                            wordRepository.addToNotebook(uid, currentWord.id, folders[which])
                        }
                    }
                    Toast.makeText(this, "Đã chuyển từ vào: ${folders[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Audio pronunciations
        btnAudioUs.setOnClickListener { playWordAudio(Locale.US) }
        btnAudioUk.setOnClickListener { playWordAudio(Locale.UK) }

        // Example audio pronunciations
        btnPlayEx1.setOnClickListener {
            val exText = tvExample1En.text.toString()
            if (exText.isNotEmpty()) playText(exText)
        }
        btnPlayEx2.setOnClickListener {
            val exText = tvExample2En.text.toString()
            if (exText.isNotEmpty()) playText(exText)
        }

        // Detail Tabs Switcher
        tabMeaning.setOnClickListener { switchTab(0) }
        tabSynAnt.setOnClickListener { switchTab(1) }
        tabCollocations.setOnClickListener { switchTab(2) }
        tabNotes.setOnClickListener { switchTab(3) }

        // Bottom Navigation
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        navLookup.setOnClickListener {
            val intent = Intent(this, LookupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
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
            startActivity(Intent(this, StatsActivity::class.java))
            finish()
        }

        navAccount.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun switchTab(tabIndex: Int) {
        val colorActive = ContextCompat.getColor(this, R.color.primary_purple)
        val colorInactive = ContextCompat.getColor(this, R.color.text_secondary)

        tabMeaning.setTextColor(if (tabIndex == 0) colorActive else colorInactive)
        tabSynAnt.setTextColor(if (tabIndex == 1) colorActive else colorInactive)
        tabCollocations.setTextColor(if (tabIndex == 2) colorActive else colorInactive)
        tabNotes.setTextColor(if (tabIndex == 3) colorActive else colorInactive)

        layoutContentMeaning.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        layoutContentSynAnt.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        layoutContentCollocations.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        layoutContentNotes.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE
    }

    private fun playWordAudio(locale: Locale) {
        tts?.let {
            it.language = locale
            it.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, "tts_word")
            val locName = if (locale == Locale.US) "US (Mỹ)" else "UK (Anh)"
            Toast.makeText(this, "Phát âm ${currentWord.word} chuẩn $locName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playText(text: String) {
        tts?.let {
            it.language = Locale.US
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_ex")
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
