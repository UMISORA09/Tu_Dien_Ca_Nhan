package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotebookActivity : AppCompatActivity() {

    private lateinit var btnAddWord: FrameLayout

    // Topic Cards
    private lateinit var topicCardIT: LinearLayout
    private lateinit var topicCardComm: LinearLayout
    private lateinit var topicCardIELTS: LinearLayout
    private lateinit var topicCardIdioms: LinearLayout

    // Filter Chips
    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterNew: TextView
    private lateinit var chipFilterLearning: TextView
    private lateinit var chipFilterMastered: TextView

    // Vocab Cards
    private lateinit var cardVocab1: LinearLayout
    private lateinit var cardVocab2: LinearLayout
    private lateinit var cardVocab3: LinearLayout

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navLookup: LinearLayout
    private lateinit var navNotebook: LinearLayout
    private lateinit var navSRS: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private var selectedTopic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notebook)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<LinearLayout>(R.id.bottomNavContainer)
            val padTop = (8 * resources.displayMetrics.density).toInt()
            val padHoriz = (4 * resources.displayMetrics.density).toInt()
            bottomNav.setPadding(padHoriz, padTop, padHoriz, systemBars.bottom + padTop)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnAddWord = findViewById(R.id.btnAddWord)

        topicCardIT = findViewById(R.id.topicCardIT)
        topicCardComm = findViewById(R.id.topicCardComm)
        topicCardIELTS = findViewById(R.id.topicCardIELTS)
        topicCardIdioms = findViewById(R.id.topicCardIdioms)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterNew = findViewById(R.id.chipFilterNew)
        chipFilterLearning = findViewById(R.id.chipFilterLearning)
        chipFilterMastered = findViewById(R.id.chipFilterMastered)

        cardVocab1 = findViewById(R.id.cardVocab1)
        cardVocab2 = findViewById(R.id.cardVocab2)
        cardVocab3 = findViewById(R.id.cardVocab3)

        navHome = findViewById(R.id.navHome)
        navLookup = findViewById(R.id.navLookup)
        navNotebook = findViewById(R.id.navNotebook)
        navSRS = findViewById(R.id.navSRS)
        navStats = findViewById(R.id.navStats)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupListeners() {
        btnAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        // Topic Cards selection
        topicCardIT.setOnClickListener {
            toggleTopic("IT", topicCardIT)
        }

        topicCardComm.setOnClickListener {
            toggleTopic("COMM", topicCardComm)
        }

        topicCardIELTS.setOnClickListener {
            toggleTopic("IELTS", topicCardIELTS)
        }

        topicCardIdioms.setOnClickListener {
            toggleTopic("IDIOMS", topicCardIdioms)
        }

        // Status Filter Chips
        chipFilterAll.setOnClickListener {
            selectStatusFilter(chipFilterAll)
            cardVocab1.visibility = View.VISIBLE
            cardVocab2.visibility = View.VISIBLE
            cardVocab3.visibility = View.VISIBLE
        }

        chipFilterNew.setOnClickListener {
            selectStatusFilter(chipFilterNew)
            Toast.makeText(this, "Chưa có từ mới hôm nay; hãy tra từ để thêm vào sổ tay!", Toast.LENGTH_SHORT).show()
        }

        chipFilterLearning.setOnClickListener {
            selectStatusFilter(chipFilterLearning)
            cardVocab1.visibility = View.VISIBLE
            cardVocab2.visibility = View.GONE
            cardVocab3.visibility = View.VISIBLE
        }

        chipFilterMastered.setOnClickListener {
            selectStatusFilter(chipFilterMastered)
            cardVocab1.visibility = View.GONE
            cardVocab2.visibility = View.VISIBLE
            cardVocab3.visibility = View.GONE
        }

        // Vocab cards clicks
        cardVocab1.setOnClickListener {
            Toast.makeText(this, "Serendipity /ˌser.ənˈdɪp.ə.ti/: Đang ôn tập SM-2", Toast.LENGTH_SHORT).show()
        }

        cardVocab2.setOnClickListener {
            Toast.makeText(this, "Resilient /rɪˈzɪl.i.ənt/: Đã thuộc lòng vững chắc", Toast.LENGTH_SHORT).show()
        }

        cardVocab3.setOnClickListener {
            Toast.makeText(this, "Idempotent /ˌaɪ.dɛmˈpoʊ.tənt/: Lặp lại SRS sau 3 ngày", Toast.LENGTH_SHORT).show()
        }

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
            // Already in notebook
        }

        navSRS.setOnClickListener {
            Toast.makeText(this, "Ôn tập Spaced Repetition (SuperMemo SM-2)", Toast.LENGTH_SHORT).show()
        }

        navStats.setOnClickListener {
            Toast.makeText(this, "Thống kê từ vựng & Chuỗi ngày học liên tục", Toast.LENGTH_SHORT).show()
        }

        navAccount.setOnClickListener {
            Toast.makeText(this, "Tài khoản học viên: Nguyễn Minh Quân", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTopic(topicKey: String, clickedCard: LinearLayout) {
        val topicCards = listOf(topicCardIT, topicCardComm, topicCardIELTS, topicCardIdioms)
        if (selectedTopic == topicKey) {
            selectedTopic = null
            topicCards.forEach { it.setBackgroundResource(R.drawable.bg_topic_card) }
            cardVocab1.visibility = View.VISIBLE
            cardVocab2.visibility = View.VISIBLE
            cardVocab3.visibility = View.VISIBLE
            Toast.makeText(this, "Hiển thị tất cả chủ đề", Toast.LENGTH_SHORT).show()
        } else {
            selectedTopic = topicKey
            topicCards.forEach {
                if (it == clickedCard) {
                    it.setBackgroundResource(R.drawable.bg_topic_card_selected)
                } else {
                    it.setBackgroundResource(R.drawable.bg_topic_card)
                }
            }

            when (topicKey) {
                "IT" -> {
                    cardVocab1.visibility = View.GONE
                    cardVocab2.visibility = View.VISIBLE
                    cardVocab3.visibility = View.VISIBLE
                    Toast.makeText(this, "Bộ sưu tập: Chuyên ngành IT (2 từ)", Toast.LENGTH_SHORT).show()
                }
                "IELTS" -> {
                    cardVocab1.visibility = View.VISIBLE
                    cardVocab2.visibility = View.GONE
                    cardVocab3.visibility = View.GONE
                    Toast.makeText(this, "Bộ sưu tập: IELTS Reading & Academic (2 từ)", Toast.LENGTH_SHORT).show()
                }
                "COMM" -> {
                    Toast.makeText(this, "Bộ sưu tập: Giao tiếp hàng ngày (3 từ)", Toast.LENGTH_SHORT).show()
                }
                "IDIOMS" -> {
                    Toast.makeText(this, "Bộ sưu tập: Idioms & Phrasal Verbs (1 từ)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun selectStatusFilter(activeChip: TextView) {
        val chips = listOf(chipFilterAll, chipFilterNew, chipFilterLearning, chipFilterMastered)
        for (chip in chips) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_purple)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_white)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }
}
