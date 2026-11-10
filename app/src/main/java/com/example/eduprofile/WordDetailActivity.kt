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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class WordDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Models for rich word details
    data class ExampleItem(val en: String, val vi: String)
    data class WordDetail(
        val id: String,
        val word: String,
        val ipa: String,
        val pos: String,
        val cefr: String,
        val status: String,
        val folder: String,
        val meaning: String,
        val examples: List<ExampleItem>,
        val synonyms: List<String>,
        val antonyms: List<String>,
        val collocations: List<String>,
        val grammarNote: String,
        val personalNote: String,
        var isFavorite: Boolean = true
    )

    private val wordDatabase = listOf(
        WordDetail(
            id = "w1",
            word = "Serendipity",
            ipa = "/ˌser.ənˈdɪp.ə.ti/",
            pos = "Danh từ",
            cefr = "CEFR C1",
            status = "Đang học",
            folder = "IELTS Reading & Academic",
            meaning = "Sự tình cờ may mắn; khả năng phát hiện ra những điều thú vị, có giá trị một cách ngẫu nhiên.",
            examples = listOf(
                ExampleItem("Finding this cozy café was pure serendipity.", "Việc tìm thấy quán cà phê ấm cúng này hoàn toàn là một sự tình cờ may mắn."),
                ExampleItem("Many scientific discoveries are a result of serendipity.", "Nhiều khám phá khoa học là kết quả của sự tình cờ may mắn.")
            ),
            synonyms = listOf("Chance", "Fluke", "Happy accident", "Good luck"),
            antonyms = listOf("Misfortune", "Bad luck", "Plan"),
            collocations = listOf("Pure serendipity (Hoàn toàn tình cờ)", "A moment of serendipity (Khoảnh khắc kỳ diệu)"),
            grammarNote = "Danh từ không đếm được (Uncountable noun). Thường đi kèm với tính từ \"pure\" hoặc \"happy\".",
            personalNote = "Mẹo nhớ: \"Se-ren-di-pi-ty\" -> Đi dạo (đi) gặp chuyện vui bất ngờ. Gặp lại từ này trong phim Serendipity (2001)."
        ),
        WordDetail(
            id = "w2",
            word = "Resilient",
            ipa = "/rɪˈzɪl.i.ənt/",
            pos = "Tính từ",
            cefr = "CEFR B2",
            status = "Đã thuộc",
            folder = "Chuyên ngành IT",
            meaning = "Kiên cường, có khả năng phục hồi nhanh chóng sau khó khăn, biến cố hoặc thay đổi (trong IT: hệ thống chịu lỗi tốt).",
            examples = listOf(
                ExampleItem("We need to build a resilient microservices architecture.", "Chúng ta cần xây dựng một kiến trúc microservices có khả năng chịu lỗi và phục hồi tốt."),
                ExampleItem("Children are often much more resilient than adults think.", "Trẻ em thường kiên cường hơn nhiều so với những gì người lớn nghĩ.")
            ),
            synonyms = listOf("Tough", "Flexible", "Strong", "Adaptable"),
            antonyms = listOf("Fragile", "Vulnerable", "Weak"),
            collocations = listOf("Resilient system (Hệ thống chịu lỗi)", "Emotionally resilient (Tinh thần kiên cường)"),
            grammarNote = "Danh từ tương ứng là Resilience (/rɪˈzɪl.jəns/).",
            personalNote = "Hay dùng trong tài liệu System Design để chỉ hệ thống tự hồi phục khi sập node."
        ),
        WordDetail(
            id = "w3",
            word = "Idempotent",
            ipa = "/ˌaɪ.dəmˈpəʊ.tənt/",
            pos = "Tính từ",
            cefr = "CEFR C2",
            status = "Đang học",
            folder = "Chuyên ngành IT",
            meaning = "(Công nghệ thông tin / Toán) Lũy đẳng; tính chất của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho ra cùng một kết quả mà không gây tác dụng phụ.",
            examples = listOf(
                ExampleItem("HTTP GET, PUT, and DELETE methods should be idempotent.", "Các phương thức HTTP GET, PUT và DELETE cần đảm bảo tính lũy đẳng."),
                ExampleItem("An idempotent payment API prevents double charging.", "Một API thanh toán có tính idempotent giúp ngăn chặn việc trừ tiền hai lần.")
            ),
            synonyms = listOf("Unchanging", "Consistent", "Repeatable"),
            antonyms = listOf("Non-idempotent", "Mutating"),
            collocations = listOf("Idempotent API", "Idempotent operation", "Idempotency key"),
            grammarNote = "Danh từ là Idempotence hoặc Idempotency.",
            personalNote = "Câu hỏi phỏng vấn Backend cực kỳ phổ biến! Nhớ ví dụ: Bấm nút gọi thang máy nhiều lần cũng chỉ gọi 1 thang máy."
        ),
        WordDetail(
            id = "w4",
            word = "Bite the bullet",
            ipa = "/baɪt ðə ˈbʊl.ɪt/",
            pos = "Idiom",
            cefr = "CEFR C1",
            status = "Mới thêm",
            folder = "Idioms & Phrasal Verbs",
            meaning = "Cắn răng chịu đựng; quyết tâm đối mặt với một tình huống khó khăn không thể tránh khỏi.",
            examples = listOf(
                ExampleItem("I had to bite the bullet and refactor the entire legacy codebase.", "Tôi đành phải cắn răng quyết tâm viết lại toàn bộ đống code cũ kỹ đó.")
            ),
            synonyms = listOf("Face the music", "Grin and bear it"),
            antonyms = listOf("Avoid", "Chicken out"),
            collocations = listOf("Have to bite the bullet", "Decide to bite the bullet"),
            grammarNote = "Thành ngữ (Idiom). Nguồn gốc từ thời xưa khi binh lính phải cắn viên đạn chì lúc phẫu thuật không có thuốc tê.",
            personalNote = "Hình ảnh cắn viên đạn khi đau -> Chấp nhận làm việc khó."
        ),
        WordDetail(
            id = "w5",
            word = "Ubiquitous",
            ipa = "/juːˈbɪk.wɪ.təs/",
            pos = "Tính từ",
            cefr = "CEFR C1",
            status = "Đang học",
            folder = "IELTS Reading & Academic",
            meaning = "Có mặt ở khắp mọi nơi, phổ biến khắp chốn cùng một lúc.",
            examples = listOf(
                ExampleItem("Smartphones have become ubiquitous in modern daily life.", "Điện thoại thông minh đã trở nên phổ biến ở khắp mọi nơi trong cuộc sống hiện đại.")
            ),
            synonyms = listOf("Omnipresent", "Everywhere", "Pervasive", "Universal"),
            antonyms = listOf("Rare", "Scarce"),
            collocations = listOf("Become ubiquitous", "Ubiquitous computing (Điện toán mọi nơi)"),
            grammarNote = "Danh từ: Ubiquity.",
            personalNote = "Hay xuất hiện trong bài đọc IELTS Reading chủ đề Công nghệ & Môi trường."
        ),
        WordDetail(
            id = "w6",
            word = "Articulate",
            ipa = "/ɑːˈtɪk.jə.lət/",
            pos = "Động từ",
            cefr = "CEFR C1",
            status = "Đã thuộc",
            folder = "Giao tiếp hàng ngày",
            meaning = "Diễn đạt, bày tỏ ý tưởng hoặc cảm xúc một cách rõ ràng, mạch lạc, lưu loát.",
            examples = listOf(
                ExampleItem("She struggled to articulate her thoughts during the interview.", "Cô ấy gặp khó khăn trong việc diễn đạt mạch lạc suy nghĩ của mình trong buổi phỏng vấn.")
            ),
            synonyms = listOf("Express", "Communicate", "Eloquent", "Fluent"),
            antonyms = listOf("Mumble", "Inarticulate", "Hesitant"),
            collocations = listOf("Clearly articulate (Diễn đạt rõ ràng)", "Articulate a vision (Trình bày tầm nhìn)"),
            grammarNote = "Tính từ phát âm là /ɑːˈtɪk.jə.lət/, Động từ phát âm đuôi là /ɑːˈtɪk.jə.leɪt/.",
            personalNote = "Từ rất đắt giá để viết vào CV: \"Ability to articulate complex technical concepts\"."
        )
    )

    private lateinit var currentWord: WordDetail
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Views
    private lateinit var scrollContainer: NestedScrollView
    private lateinit var btnBack: LinearLayout
    private lateinit var btnFolderSelect: LinearLayout
    private lateinit var tvFolderName: TextView
    private lateinit var btnFavorite: FrameLayout
    private lateinit var ivFavoriteIcon: ImageView

    private lateinit var badgePos: TextView
    private lateinit var badgeCefr: TextView
    private lateinit var badgeStatus: TextView
    private lateinit var tvWordTitle: TextView
    private lateinit var tvWordIpa: TextView
    private lateinit var btnAudioUs: LinearLayout
    private lateinit var btnAudioUk: LinearLayout

    private lateinit var tabMeaning: TextView
    private lateinit var tabSynAnt: TextView
    private lateinit var tabCollocations: TextView
    private lateinit var tabNotes: TextView

    private lateinit var layoutContentMeaning: LinearLayout
    private lateinit var layoutContentSynAnt: LinearLayout
    private lateinit var layoutContentCollocations: LinearLayout
    private lateinit var layoutContentNotes: LinearLayout

    private lateinit var tvWordMeaning: TextView
    private lateinit var tvHeaderExamples: TextView
    private lateinit var tvExample1En: TextView
    private lateinit var tvExample1Vi: TextView
    private lateinit var btnPlayEx1: ImageView
    private lateinit var tvExample2En: TextView
    private lateinit var tvExample2Vi: TextView
    private lateinit var btnPlayEx2: ImageView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_word_detail)

        setupWindowInsets()
        initViews()
        resolveWordData()
        bindData()
        setupListeners()

        tts = TextToSpeech(this, this)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainWordDetail)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            scrollContainer.setPadding(0, systemBars.top, 0, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
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

    private fun resolveWordData() {
        val wordParam = intent.getStringExtra("EXTRA_WORD") ?: intent.getStringExtra("word") ?: "Serendipity"
        val found = wordDatabase.find { it.word.equals(wordParam, ignoreCase = true) }
        currentWord = found ?: wordDatabase[0]
    }

    private fun bindData() {
        tvFolderName.text = currentWord.folder
        badgePos.text = currentWord.pos
        badgeCefr.text = currentWord.cefr
        badgeStatus.text = currentWord.status
        tvWordTitle.text = currentWord.word
        tvWordIpa.text = currentWord.ipa
        tvWordMeaning.text = currentWord.meaning

        // Favorite icon
        ivFavoriteIcon.setImageResource(
            if (currentWord.isFavorite) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark
        )

        // Examples
        tvHeaderExamples.text = "VÍ DỤ TRONG NGỮ CẢNH (${currentWord.examples.size})"
        if (currentWord.examples.isNotEmpty()) {
            tvExample1En.text = currentWord.examples[0].en
            tvExample1Vi.text = currentWord.examples[0].vi
        }
        if (currentWord.examples.size > 1) {
            tvExample2En.text = currentWord.examples[1].en
            tvExample2Vi.text = currentWord.examples[1].vi
        }

        // Synonyms
        containerSynonyms.removeAllViews()
        val synRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }
        currentWord.synonyms.forEach { syn ->
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
        currentWord.antonyms.forEach { ant ->
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
        currentWord.collocations.forEach { col ->
            val tv = TextView(this).apply {
                text = "• $col"
                setTextColor(ContextCompat.getColor(context, R.color.admin_slate_text))
                textSize = 13f
                val pv = (4 * resources.displayMetrics.density).toInt()
                setPadding(0, pv, 0, pv)
            }
            containerCollocations.addView(tv)
        }

        // Mnemonic & Notes
        tvMnemonicContent.text = currentWord.personalNote
        tvGrammarNote.text = currentWord.grammarNote
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Favorite Toggle
        btnFavorite.setOnClickListener {
            currentWord.isFavorite = !currentWord.isFavorite
            ivFavoriteIcon.setImageResource(
                if (currentWord.isFavorite) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark
            )
            val msg = if (currentWord.isFavorite) "Đã lưu từ vào Sổ tay yêu thích ⭐" else "Đã bỏ lưu khỏi danh sách yêu thích"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Đã chuyển từ vào: ${folders[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Audio pronunciations
        btnAudioUs.setOnClickListener { playWordAudio(Locale.US) }
        btnAudioUk.setOnClickListener { playWordAudio(Locale.UK) }

        // Example audio pronunciations
        btnPlayEx1.setOnClickListener {
            if (currentWord.examples.isNotEmpty()) playText(currentWord.examples[0].en)
        }
        btnPlayEx2.setOnClickListener {
            if (currentWord.examples.size > 1) playText(currentWord.examples[1].en)
        }

        // Tab selection
        tabMeaning.setOnClickListener { selectTab(0) }
        tabSynAnt.setOnClickListener { selectTab(1) }
        tabCollocations.setOnClickListener { selectTab(2) }
        tabNotes.setOnClickListener { selectTab(3) }

        // Bottom Navigation
        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
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
            startActivity(Intent(this, StatsActivity::class.java))
            finish()
        }
        navAccount.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun selectTab(index: Int) {
        val colorActive = ContextCompat.getColor(this, R.color.white)
        val colorInactive = ContextCompat.getColor(this, R.color.admin_slate_sub)

        val bgActive = R.drawable.bg_tab_detail_active
        val bgInactive = R.drawable.bg_tab_detail_inactive

        tabMeaning.setBackgroundResource(if (index == 0) bgActive else bgInactive)
        tabMeaning.setTextColor(if (index == 0) colorActive else colorInactive)

        tabSynAnt.setBackgroundResource(if (index == 1) bgActive else bgInactive)
        tabSynAnt.setTextColor(if (index == 1) colorActive else colorInactive)

        tabCollocations.setBackgroundResource(if (index == 2) bgActive else bgInactive)
        tabCollocations.setTextColor(if (index == 2) colorActive else colorInactive)

        tabNotes.setBackgroundResource(if (index == 3) bgActive else bgInactive)
        tabNotes.setTextColor(if (index == 3) colorActive else colorInactive)

        layoutContentMeaning.visibility = if (index == 0) View.VISIBLE else View.GONE
        layoutContentSynAnt.visibility = if (index == 1) View.VISIBLE else View.GONE
        layoutContentCollocations.visibility = if (index == 2) View.VISIBLE else View.GONE
        layoutContentNotes.visibility = if (index == 3) View.VISIBLE else View.GONE
    }

    private fun playWordAudio(locale: Locale) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "Đang phát âm: ${currentWord.word}", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.language = locale
        tts?.speak(currentWord.word, TextToSpeech.QUEUE_FLUSH, null, "WORD_TTS")
    }

    private fun playText(text: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "Đang đọc câu: $text", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.language = Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EX_TTS")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
