package com.example.eduprofile.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// [THÊM]: Tầng Repository quản lý điểm XP, chuỗi học tập (Streak) và hồ sơ học viên theo PROMPT 5
class UserStatsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    /**
     * Lấy thông tin thống kê người dùng (XP, Streak, Goal...) dạng Flow realtime.
     */
    fun getUserStats(userId: String): Flow<UserStats?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val stats = snapshot?.toObject(UserStats::class.java)
                trySend(stats)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Cộng điểm XP cho người dùng bằng FieldValue.increment() để tránh xung đột concurrent.
     * Thang XP thống nhất: Quiz đúng +10 XP/câu, Flashcard chấm xong +5 XP/thẻ.
     */
    suspend fun addXp(userId: String, amount: Long) {
        usersCollection.document(userId).update("xp", FieldValue.increment(amount)).await()
    }

    /**
     * Ghi nhận ngày học và cập nhật chuỗi Streak theo múi giờ Asia/Ho_Chi_Minh.
     * Sử dụng Transaction để đảm bảo tính toàn vẹn dữ liệu.
     */
    suspend fun recordStudyDay(userId: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        val todayStr = sdf.format(Date())

        val userRef = usersCollection.document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) return@runTransaction

            val lastStudyDate = snapshot.getString("lastStudyDate") ?: ""
            if (lastStudyDate == todayStr) {
                // Đã học hôm nay rồi, không tăng streak lần nữa
                return@runTransaction
            }

            val currentStreak = snapshot.getLong("streak") ?: 0L
            val bestStreak = snapshot.getLong("bestStreak") ?: currentStreak

            // Tính ngày hôm qua
            val yesterdayMillis = System.currentTimeMillis() - 86400000L
            val yesterdayStr = sdf.format(Date(yesterdayMillis))

            val newStreak = if (lastStudyDate == yesterdayStr) {
                currentStreak + 1
            } else {
                1L
            }

            val newBestStreak = if (newStreak > bestStreak) newStreak else bestStreak

            transaction.update(userRef, mapOf(
                "streak" to newStreak,
                "bestStreak" to newBestStreak,
                "lastStudyDate" to todayStr
            ))
        }.await()
    }

    /**
     * Lấy toàn bộ danh sách người dùng cho Admin CMS.
     */
    fun getAllUsers(): Flow<List<UserStats>> = callbackFlow {
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val stats = doc.toObject(UserStats::class.java)
                stats?.copy(email = doc.getString("email") ?: stats.email)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (chỉ dành cho Admin).
     */
    suspend fun setUserBlocked(userId: String, blocked: Boolean) {
        usersCollection.document(userId).update("blocked", blocked).await()
    }

    /**
     * Thay đổi vai trò người dùng ("USER" hoặc "ADMIN").
     */
    suspend fun setUserRole(userId: String, newRole: String) {
        usersCollection.document(userId).update("role", newRole).await()
    }
}
