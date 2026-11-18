package com.example.eduprofile.data

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eduprofile.R

// [THÊM]: Item dữ liệu hiển thị trong Sổ tay từ vựng NotebookActivity theo PROMPT 6.2
data class NotebookItem(
    val word: Word,
    val userWord: UserWord,
    val nextReviewInfo: String = "Ôn tập SM-2"
)

// [THÊM]: Adapter sổ tay từ vựng nạp dữ liệu động từ Firestore theo PROMPT 6.2
class NotebookAdapter(
    private var items: List<NotebookItem> = emptyList(),
    private val onWordClick: (Word) -> Unit
) : RecyclerView.Adapter<NotebookAdapter.NotebookViewHolder>() {

    fun updateData(newItems: List<NotebookItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotebookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notebook_word, parent, false)
        return NotebookViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotebookViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NotebookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: LinearLayout = itemView.findViewById(R.id.cardContainer)
        private val tvWord: TextView = itemView.findViewById(R.id.tvWord)
        private val badgePos: TextView = itemView.findViewById(R.id.badgePos)
        private val layoutStatusBadge: LinearLayout = itemView.findViewById(R.id.layoutStatusBadge)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        private val tvStatusText: TextView = itemView.findViewById(R.id.tvStatusText)
        private val tvPhonetic: TextView = itemView.findViewById(R.id.tvPhonetic)
        private val tvMeaning: TextView = itemView.findViewById(R.id.tvMeaning)
        private val ivTopicIcon: ImageView = itemView.findViewById(R.id.ivTopicIcon)
        private val tvTopicText: TextView = itemView.findViewById(R.id.tvTopicText)
        private val tvReviewInfo: TextView = itemView.findViewById(R.id.tvReviewInfo)

        fun bind(item: NotebookItem) {
            val word = item.word
            val userWord = item.userWord

            tvWord.text = word.word
            badgePos.text = if (word.pos.isNotEmpty()) word.pos else "Từ vựng"
            tvPhonetic.text = word.ipa
            tvMeaning.text = word.meaning

            // Cập nhật trạng thái học tập (Mới / Đang học / Thuộc lòng)
            when (userWord.status.lowercase()) {
                "mastered" -> {
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_badge_mastered)
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    tvStatusText.text = "Thuộc lòng"
                    tvStatusText.setTextColor(Color.parseColor("#16A34A"))
                }
                "learning" -> {
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_badge_learning)
                    ivStatusIcon.setImageResource(R.drawable.ic_hourglass)
                    tvStatusText.text = "Đang học"
                    tvStatusText.setTextColor(Color.parseColor("#D97706"))
                }
                else -> { // "new"
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_badge_new)
                    ivStatusIcon.setImageResource(R.drawable.ic_sparkle)
                    tvStatusText.text = "Mới"
                    tvStatusText.setTextColor(Color.parseColor("#7C3AED"))
                }
            }

            // Gán thông tin chủ đề
            val topic = if (word.topic.isNotEmpty()) word.topic else "IELTS Academic"
            tvTopicText.text = topic
            when {
                topic.contains("IT", ignoreCase = true) || topic.contains("Công nghệ", ignoreCase = true) -> {
                    ivTopicIcon.setImageResource(R.drawable.ic_laptop)
                }
                topic.contains("COMM", ignoreCase = true) || topic.contains("Giao tiếp", ignoreCase = true) -> {
                    ivTopicIcon.setImageResource(R.drawable.ic_chat_bubble)
                }
                topic.contains("IDIOM", ignoreCase = true) || topic.contains("Thành ngữ", ignoreCase = true) -> {
                    ivTopicIcon.setImageResource(R.drawable.ic_sparkle)
                }
                else -> {
                    ivTopicIcon.setImageResource(R.drawable.ic_graduation_cap)
                }
            }

            tvReviewInfo.text = item.nextReviewInfo

            cardContainer.setOnClickListener {
                onWordClick(word)
            }
        }
    }
}
