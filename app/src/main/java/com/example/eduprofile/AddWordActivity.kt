package com.example.eduprofile

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class AddWordActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBack: FrameLayout
    private lateinit var btnSaveTop: TextView
    private lateinit var btnSubmitWord: AppCompatButton

    // Inputs
    private lateinit var tilWord: TextInputLayout
    private lateinit var etWord: TextInputEditText
    private lateinit var btnSpeakWord: FrameLayout
    private lateinit var tilPhonetic: TextInputLayout
    private lateinit var etPhonetic: TextInputEditText

    // Part of speech chips
    private lateinit var chipPosNoun: TextView
    private lateinit var chipPosVerb: TextView
    private lateinit var chipPosAdj: TextView
    private lateinit var chipPosAdv: TextView
    private lateinit var chipPosIdiom: TextView

    // CEFR level chips
    private lateinit var chipLevelA: TextView
    private lateinit var chipLevelB: TextView
    private lateinit var chipLevelC: TextView

    // Meaning & Context
    private lateinit var tilMeaning: TextInputLayout
    private lateinit var etMeaning: TextInputEditText
    private lateinit var tilExample: TextInputLayout
    private lateinit var etExample: TextInputEditText
    private lateinit var tilMnemonic: TextInputLayout
    private lateinit var etMnemonic: TextInputEditText

    // Topic Cards
    private lateinit var cardTopicIT: LinearLayout
    private lateinit var cardTopicComm: LinearLayout
    private lateinit var cardTopicIELTS: LinearLayout
    private lateinit var cardTopicIdioms: LinearLayout

    private var selectedPos = "Danh từ"
    private var selectedLevel = "C1 - C2"
    private var selectedTopic = "IELTS Academic"

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_word)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSaveTop = findViewById(R.id.btnSaveTop)
        btnSubmitWord = findViewById(R.id.btnSubmitWord)

        tilWord = findViewById(R.id.tilWord)
        etWord = findViewById(R.id.etWord)
        btnSpeakWord = findViewById(R.id.btnSpeakWord)
        tilPhonetic = findViewById(R.id.tilPhonetic)
        etPhonetic = findViewById(R.id.etPhonetic)

        chipPosNoun = findViewById(R.id.chipPosNoun)
        chipPosVerb = findViewById(R.id.chipPosVerb)
        chipPosAdj = findViewById(R.id.chipPosAdj)
        chipPosAdv = findViewById(R.id.chipPosAdv)
        chipPosIdiom = findViewById(R.id.chipPosIdiom)

        chipLevelA = findViewById(R.id.chipLevelA)
        chipLevelB = findViewById(R.id.chipLevelB)
        chipLevelC = findViewById(R.id.chipLevelC)

        tilMeaning = findViewById(R.id.tilMeaning)
        etMeaning = findViewById(R.id.etMeaning)
        tilExample = findViewById(R.id.tilExample)
        etExample = findViewById(R.id.etExample)
        tilMnemonic = findViewById(R.id.tilMnemonic)
        etMnemonic = findViewById(R.id.etMnemonic)

        cardTopicIT = findViewById(R.id.cardTopicIT)
        cardTopicComm = findViewById(R.id.cardTopicComm)
        cardTopicIELTS = findViewById(R.id.cardTopicIELTS)
        cardTopicIdioms = findViewById(R.id.cardTopicIdioms)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSpeakWord.setOnClickListener {
            val text = etWord.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AddWordTTS")
            } else {
                Toast.makeText(this, "Nhập từ vựng tiếng Anh để nghe phát âm", Toast.LENGTH_SHORT).show()
            }
        }

        // Part of speech
        val posChips = listOf(chipPosNoun, chipPosVerb, chipPosAdj, chipPosAdv, chipPosIdiom)
        posChips.forEach { chip ->
            chip.setOnClickListener {
                selectedPos = chip.text.toString()
                posChips.forEach { c ->
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_purple)
                        c.setTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_white)
                        c.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    }
                }
            }
        }

        // CEFR levels
        val levelChips = listOf(chipLevelA, chipLevelB, chipLevelC)
        levelChips.forEach { chip ->
            chip.setOnClickListener {
                selectedLevel = chip.text.toString()
                levelChips.forEach { c ->
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_purple)
                        c.setTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_white)
                        c.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    }
                }
            }
        }

        // Topic Cards
        val topics = mapOf(
            cardTopicIT to "Chuyên ngành IT",
            cardTopicComm to "Giao tiếp hằng ngày",
            cardTopicIELTS to "IELTS Academic",
            cardTopicIdioms to "Idioms & Phrasal"
        )

        topics.forEach { (card, name) ->
            card.setOnClickListener {
                selectedTopic = name
                topics.forEach { (c, _) ->
                    if (c == card) {
                        c.setBackgroundResource(R.drawable.bg_topic_card_selected)
                        val tv = c.getChildAt(1) as? TextView
                        tv?.setTextColor(ContextCompat.getColor(this, R.color.primary_purple))
                    } else {
                        c.setBackgroundResource(R.drawable.bg_topic_card)
                        val tv = c.getChildAt(1) as? TextView
                        tv?.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    }
                }
            }
        }

        btnSaveTop.setOnClickListener {
            validateAndSave()
        }

        btnSubmitWord.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val word = etWord.text?.toString()?.trim().orEmpty()
        val meaning = etMeaning.text?.toString()?.trim().orEmpty()
        val phonetic = etPhonetic.text?.toString()?.trim().orEmpty()
        val example = etExample.text?.toString()?.trim().orEmpty()
        val mnemonic = etMnemonic.text?.toString()?.trim().orEmpty()

        tilWord.error = null
        tilMeaning.error = null

        if (word.isEmpty()) {
            tilWord.error = getString(R.string.err_word_empty)
            etWord.requestFocus()
            Toast.makeText(this, R.string.err_word_empty, Toast.LENGTH_SHORT).show()
            return
        }

        if (meaning.isEmpty()) {
            tilMeaning.error = getString(R.string.err_meaning_empty)
            etMeaning.requestFocus()
            Toast.makeText(this, R.string.err_meaning_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent().apply {
            putExtra("WORD", word)
            putExtra("MEANING", meaning)
            putExtra("PHONETIC", phonetic)
            putExtra("POS", selectedPos)
            putExtra("LEVEL", selectedLevel)
            putExtra("TOPIC", selectedTopic)
            putExtra("EXAMPLE", example)
            putExtra("MNEMONIC", mnemonic)
        }

        setResult(RESULT_OK, resultIntent)
        Toast.makeText(
            this,
            "Đã thêm từ \"$word\" ($selectedPos) vào Sổ tay \"$selectedTopic\"!",
            Toast.LENGTH_LONG
        ).show()

        finish()
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

