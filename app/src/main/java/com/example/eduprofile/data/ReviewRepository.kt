package com.example.eduprofile.data

import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

// [THÊM]: Tầng Repository quản lý tiến độ ôn tập SM-2 theo PROMPT 5
class ReviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reviewsCollection = firestore.collection("reviews")

    /**
     * Lấy các lượt ôn tập đến hạn (nextReview <= now hoặc chưa từng ôn) của người dùng.
     */
    fun getDueReviews(userId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val now = Timestamp.now()
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Review::class.java)
                }?.filter { r ->
                    r.nextReview == null || r.nextReview <= now
                }?.sortedBy { it.nextReview } ?: emptyList()

                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Lấy toàn bộ bản ghi review của người dùng (dùng để đếm thống kê, biểu đồ tuần).
     */
    fun getAllUserReviews(userId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Review::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Đọc thông tin ôn tập 1 từ cụ thể của người dùng.
     */
    suspend fun getReview(userId: String, wordId: String): Review? {
        val docId = "${userId}_$wordId"
        return try {
            val doc = reviewsCollection.document(docId).get().await()
            if (doc.exists()) {
                doc.toObject(Review::class.java)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lưu trạng thái Review lên Firestore.
     */
    suspend fun saveReview(review: Review) {
        val docId = "${review.userId}_${review.wordId}"
        reviewsCollection.document(docId).set(review, SetOptions.merge()).await()
    }

    /**
     * Áp dụng thuật toán SuperMemo SM-2 chuẩn (commit 38f8c3f)
     * Quality: 1=Quên, 3=Khó, 4=Ổn, 5=Dễ
     * EF sàn 1.3, interval 1 -> 6 -> interval * EF.
     */
    suspend fun applyRating(userId: String, wordId: String, quality: Int): Review {
        val currentReview = getReview(userId, wordId) ?: Review(
            id = "${userId}_$wordId",
            userId = userId,
            wordId = wordId,
            ef = 2.5,
            interval = 1,
            repetitions = 0,
            isBookmarked = false
        )

        // Tính EF mới theo công thức SuperMemo SM-2
        val q = quality.coerceIn(1, 5)
        var newEf = currentReview.ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        if (newEf < 1.3) newEf = 1.3

        val nowSeconds = System.currentTimeMillis() / 1000
        val newRepetitions: Long
        val newInterval: Long
        val nextReviewSeconds: Long

        if (q < 3) {
            // Quên: reset repetitions về 0, interval 1 ngày, ôn lại sau 10 phút (600 giây)
            newRepetitions = 0L
            newInterval = 1L
            nextReviewSeconds = nowSeconds + 600
        } else {
            // Nhớ được (Khó, Ổn, Dễ): tăng repetitions, áp dụng interval ngắt quãng
            newRepetitions = currentReview.repetitions + 1
            newInterval = when (newRepetitions) {
                1L -> 1L
                2L -> 6L
                else -> (currentReview.interval * newEf).roundToLong().coerceAtLeast(1L)
            }
            nextReviewSeconds = nowSeconds + (newInterval * 86400)
        }

        val updatedReview = currentReview.copy(
            id = "${userId}_$wordId",
            userId = userId,
            wordId = wordId,
            ef = (newEf * 100.0).roundToLong() / 100.0,
            interval = newInterval,
            repetitions = newRepetitions,
            nextReview = Timestamp(nextReviewSeconds, 0),
            lastReviewed = Timestamp.now()
        )

        saveReview(updatedReview)
        return updatedReview
    }

    /**
     * Đánh dấu yêu thích / bookmark từ vựng.
     */
    suspend fun toggleBookmark(userId: String, wordId: String, bookmarked: Boolean) {
        val docId = "${userId}_$wordId"
        val review = getReview(userId, wordId)
        if (review != null) {
            reviewsCollection.document(docId).update("isBookmarked", bookmarked).await()
        } else {
            val newReview = Review(
                id = docId,
                userId = userId,
                wordId = wordId,
                ef = 2.5,
                interval = 1,
                repetitions = 0,
                nextReview = Timestamp.now(),
                isBookmarked = bookmarked
            )
            saveReview(newReview)
        }
    }

    /**
     * Di chuyển dữ liệu cũ từ SharedPreferences "sm2_storage_prefs" lên Firestore (chạy 1 lần).
     */
    suspend fun migrateFromPrefsIfFound(userId: String, prefs: SharedPreferences) {
        val idToSlugMap = mapOf(
            1 to "serendipity",
            2 to "ephemeral",
            3 to "idempotent",
            4 to "resilient",
            5 to "pragmatic",
            6 to "ubiquitous",
            7 to "eloquent",
            8 to "tenacious",
            9 to "synchronous",
            10 to "ambivalent"
        )

        var hasMigratedAny = false
        for ((id, slug) in idToSlugMap) {
            val efKey = "sm2_${id}_ef"
            if (prefs.contains(efKey)) {
                val ef = prefs.getFloat(efKey, 2.5f).toDouble()
                val interval = prefs.getInt("sm2_${id}_interval", 1).toLong()
                val rep = prefs.getInt("sm2_${id}_rep", 0).toLong()
                val nextReviewMillis = prefs.getLong("sm2_${id}_next_review", 0L)
                val isBookmarked = prefs.getBoolean("sm2_${id}_bookmark", false)

                val nextReviewTimestamp = if (nextReviewMillis > 0) {
                    Timestamp(nextReviewMillis / 1000, 0)
                } else {
                    Timestamp.now()
                }

                val review = Review(
                    id = "${userId}_$slug",
                    userId = userId,
                    wordId = slug,
                    ef = ef,
                    interval = interval,
                    repetitions = rep,
                    nextReview = nextReviewTimestamp,
                    lastReviewed = Timestamp.now(),
                    isBookmarked = isBookmarked
                )
                saveReview(review)
                hasMigratedAny = true
            }
        }

        if (hasMigratedAny) {
            // Xóa sạch key cũ sau khi migrate thành công
            prefs.edit().clear().apply()
        }
    }
}
