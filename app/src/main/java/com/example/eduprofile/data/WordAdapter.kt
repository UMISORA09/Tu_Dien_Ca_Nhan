package com.example.eduprofile.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.eduprofile.R

// [THÊM]: Adapter hiển thị danh sách từ vựng đọc động từ Firestore theo PROMPT 6.1
class WordAdapter(
    private var words: List<Word> = emptyList(),
    private val bookmarkedWordIds: Set<String> = emptySet(),
    private val onWordClick: (Word) -> Unit,
    private val onAudioClick: (Word) -> Unit,
    private val onBookmarkClick: (Word) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    fun updateData(newWords: List<Word>) {
        words = newWords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word_card, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(words[position])
    }

    override fun getItemCount(): Int = words.size

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardWord: CardView = itemView.findViewById(R.id.cardWord)
        private val tvWord: TextView = itemView.findViewById(R.id.tvWord)
        private val tvIpa: TextView = itemView.findViewById(R.id.tvIpa)
        private val btnAudio: FrameLayout = itemView.findViewById(R.id.btnAudio)
        private val btnBookmark: ImageView = itemView.findViewById(R.id.btnBookmark)
        private val badgePos: TextView = itemView.findViewById(R.id.badgePos)
        private val badgeCefr: TextView = itemView.findViewById(R.id.badgeCefr)
        private val tvMeaning: TextView = itemView.findViewById(R.id.tvMeaning)

        fun bind(word: Word) {
            tvWord.text = word.word
            tvIpa.text = word.ipa
            badgePos.text = word.pos
            badgeCefr.text = word.cefr
            tvMeaning.text = word.meaning

            val isBookmarked = bookmarkedWordIds.contains(word.id)
            btnBookmark.setImageResource(
                if (isBookmarked) R.drawable.ic_bookmark_gold else R.drawable.ic_bookmark
            )

            cardWord.setOnClickListener { onWordClick(word) }
            btnAudio.setOnClickListener { onAudioClick(word) }
            btnBookmark.setOnClickListener { onBookmarkClick(word) }
        }
    }
}
