package com.example.eduprofile.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// [THÊM]: Data class Word theo schema PROMPT 3 và PROMPT 5
data class Word(
    @DocumentId val id: String = "",
    val word: String = "",
    val ipa: String = "",
    val pos: String = "",
    val cefr: String = "",
    val topic: String = "",
    val meaning: String = "",
    val exampleEn: String = "",
    val exampleVi: String = "",
    val examples: List<Map<String, String>> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val mnemonic: String = "",
    val grammarNote: String = "",
    val personalNote: String = "",
    val status: String = "approved", // "approved" | "pending" | "rejected"
    val createdBy: String = "",
    val approvedBy: String = "",
    val createdAt: Timestamp? = null
)

// [THÊM]: Data class UserWord đại diện từ vựng trong sổ tay cá nhân
data class UserWord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val status: String = "new", // "new" (Mới) | "learning" (Đang học) | "mastered" (Thuộc lòng)
    val note: String = "",
    val folder: String = "",
    val addedAt: Timestamp? = null
)

// [THÊM]: Data class Review lưu trạng thái ôn tập SM-2 trên Firestore
data class Review(
    @DocumentId val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val ef: Double = 2.5,
    val interval: Long = 1,
    val repetitions: Long = 0,
    val nextReview: Timestamp? = null,
    val lastReviewed: Timestamp? = null,
    val isBookmarked: Boolean = false
)

// [THÊM]: Data class UserStats quản lý XP, streak, tiến độ người dùng
data class UserStats(
    val displayName: String = "",
    val email: String = "",
    val role: String = "USER", // "USER" | "ADMIN"
    val xp: Long = 0,
    val streak: Long = 0,
    val bestStreak: Long = 0,
    val lastStudyDate: String = "",
    val level: String = "",
    val dailyGoal: Long = 10,
    val blocked: Boolean = false
)
