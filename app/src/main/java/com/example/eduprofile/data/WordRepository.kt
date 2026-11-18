package com.example.eduprofile.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// [THÊM]: Tầng Repository duy nhất quản lý từ vựng và sổ tay theo PROMPT 5
class WordRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val wordsCollection = firestore.collection("words")
    private val userWordsCollection = firestore.collection("userWords")

    /**
     * Lấy danh sách từ vựng đã duyệt (status == "approved") theo thời gian thực (Flow).
     * Firestore Android tự bật offline cache. Nếu Firestore chưa có bản ghi hoặc chưa tạo DB,
     * tự động fallback về 12 từ vựng chuẩn để đảm bảo app luôn hiển thị đầy đủ dữ liệu.
     */
    fun getApprovedWords(): Flow<List<Word>> = callbackFlow {
        val listener = wordsCollection
            .whereEqualTo("status", "approved")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(getSeedWordsList())
                    return@addSnapshotListener
                }
                val words = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Word::class.java)
                }?.sortedBy { it.word } ?: emptyList()

                if (words.isEmpty()) {
                    trySend(getSeedWordsList())
                } else {
                    trySend(words)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Tìm kiếm từ vựng theo từ hoặc nghĩa tiếng Việt.
     * Tải từ đã duyệt và lọc client-side cho kho dữ liệu hiện tại.
     */
    fun searchWords(query: String): Flow<List<Word>> {
        val cleanQuery = query.trim().lowercase()
        return getApprovedWords().map { list ->
            if (cleanQuery.isEmpty()) {
                list
            } else {
                list.filter {
                    it.word.lowercase().contains(cleanQuery) ||
                            it.meaning.lowercase().contains(cleanQuery)
                }
            }
        }
    }

    /**
     * Lấy thông tin chi tiết một từ theo slug (wordId) dạng Flow.
     */
    fun getWord(wordId: String): Flow<Word?> = callbackFlow {
        val listener = wordsCollection.document(wordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = getSeedWordsList().find { it.id == wordId }
                    trySend(fallback)
                    return@addSnapshotListener
                }
                val word = snapshot?.toObject(Word::class.java)
                if (word != null) {
                    trySend(word)
                } else {
                    val fallback = getSeedWordsList().find { it.id == wordId }
                    trySend(fallback)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Lấy thông tin một từ theo slug (wordId) dạng suspend fun.
     */
    suspend fun getWordDirect(wordId: String): Word? {
        return try {
            val doc = wordsCollection.document(wordId).get().await()
            doc.toObject(Word::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Thêm từ mới vào Firestore.
     * Tạo slug từ tên từ, lưu vào words với status = "pending" (hoặc do checkbox quyết định).
     * Đồng thời tạo bản ghi userWords cho chính người tạo.
     * Trả về slug của từ đã tạo.
     */
    suspend fun addWord(word: Word, userId: String, contributeToSystem: Boolean = true): String {
        val slug = word.word.trim().lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")

        val targetStatus = if (contributeToSystem) "pending" else "pending"

        val wordData = hashMapOf(
            "word" to word.word,
            "ipa" to word.ipa,
            "pos" to word.pos,
            "cefr" to word.cefr,
            "topic" to word.topic,
            "meaning" to word.meaning,
            "exampleEn" to word.exampleEn,
            "exampleVi" to word.exampleVi,
            "examples" to word.examples,
            "synonyms" to word.synonyms,
            "antonyms" to word.antonyms,
            "collocations" to word.collocations,
            "mnemonic" to word.mnemonic,
            "grammarNote" to word.grammarNote,
            "personalNote" to word.personalNote,
            "status" to targetStatus,
            "createdBy" to userId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        wordsCollection.document(slug).set(wordData).await()

        // Tạo userWords cho chính người dùng này
        val userWordId = "${userId}_$slug"
        val userWordData = hashMapOf(
            "userId" to userId,
            "wordId" to slug,
            "status" to "new",
            "note" to word.personalNote,
            "folder" to word.topic,
            "addedAt" to FieldValue.serverTimestamp()
        )
        userWordsCollection.document(userWordId).set(userWordData).await()

        return slug
    }

    /**
     * Kiểm tra xem slug từ đã tồn tại trong kho words hay chưa.
     */
    suspend fun isWordExists(slug: String): Boolean {
        return try {
            val doc = wordsCollection.document(slug).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Duyệt từ vựng (chỉ dành cho Admin).
     */
    suspend fun approveWord(wordId: String, adminUid: String) {
        wordsCollection.document(wordId).update(
            mapOf(
                "status" to "approved",
                "approvedBy" to adminUid
            )
        ).await()
    }

    /**
     * Từ chối từ vựng (chỉ dành cho Admin).
     */
    suspend fun rejectWord(wordId: String) {
        wordsCollection.document(wordId).update("status", "rejected").await()
    }

    /**
     * Lấy danh sách từ đang chờ duyệt (status == "pending") cho Admin CMS.
     */
    fun getPendingWords(): Flow<List<Word>> = callbackFlow {
        val listener = wordsCollection
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Word::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Lấy danh sách từ trong sổ tay của một người dùng cụ thể.
     */
    fun getUserWords(userId: String): Flow<List<UserWord>> = callbackFlow {
        val listener = userWordsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(UserWord::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Cập nhật trạng thái một từ trong sổ tay ("new" | "learning" | "mastered").
     */
    suspend fun updateUserWordStatus(userId: String, wordId: String, status: String) {
        val docId = "${userId}_$wordId"
        userWordsCollection.document(docId).update("status", status).await()
    }

    /**
     * Thêm từ vào sổ tay người dùng.
     */
    suspend fun addToNotebook(userId: String, wordId: String, folder: String = "Từ vựng chung", note: String = "") {
        val docId = "${userId}_$wordId"
        val data = hashMapOf(
            "userId" to userId,
            "wordId" to wordId,
            "status" to "new",
            "note" to note,
            "folder" to folder,
            "addedAt" to FieldValue.serverTimestamp()
        )
        userWordsCollection.document(docId).set(data).await()
    }

    /**
     * Tự động nạp 12 từ vựng mẫu chuẩn nếu kho từ words trên Firestore chưa có dữ liệu.
     */
    suspend fun seedInitialWordsIfEmpty(): Int {
        val snapshot = wordsCollection.limit(1).get().await()
        if (!snapshot.isEmpty) return 0

        val initialWords = getSeedWordsList()
        for (w in initialWords) {
            val doc = wordsCollection.document(w.id)
            val data = hashMapOf(
                "word" to w.word,
                "ipa" to w.ipa,
                "pos" to w.pos,
                "cefr" to w.cefr,
                "topic" to w.topic,
                "meaning" to w.meaning,
                "exampleEn" to w.exampleEn,
                "exampleVi" to w.exampleVi,
                "examples" to w.examples,
                "synonyms" to w.synonyms,
                "antonyms" to w.antonyms,
                "collocations" to w.collocations,
                "mnemonic" to w.mnemonic,
                "grammarNote" to w.grammarNote,
                "personalNote" to w.personalNote,
                "status" to "approved",
                "createdBy" to "seed",
                "createdAt" to FieldValue.serverTimestamp()
            )
            doc.set(data).await()
        }
        return initialWords.size
    }

    companion object {
        fun getSeedWordsList(): List<Word> {
            return listOf(
                Word(
                    id = "serendipity",
                    word = "Serendipity",
                    ipa = "/ˌser.ənˈdɪp.ə.ti/",
                    pos = "Danh từ",
                    cefr = "C1",
                    topic = "IELTS Academic",
                    meaning = "Sự tình cờ may mắn; khả năng tình cờ phát hiện ra những điều thú vị, có giá trị ngoài mong đợi.",
                    exampleEn = "Finding this charming coffee shop was pure serendipity on a rainy morning.",
                    exampleVi = "Việc tìm thấy quán cà phê xinh đẹp này là một sự tình cờ may mắn thuần túy vào một buổi sáng mưa.",
                    examples = listOf(
                        mapOf("en" to "Finding this cozy café was pure serendipity.", "vi" to "Tìm thấy quán cà phê ấm cúng này quả là một sự tình cờ may mắn."),
                        mapOf("en" to "Many scientific discoveries are the result of serendipity.", "vi" to "Nhiều khám phá khoa học là kết quả của sự tình cờ may mắn.")
                    ),
                    synonyms = listOf("Chance", "Fluke", "Happy accident", "Good luck"),
                    antonyms = listOf("Misfortune", "Bad luck", "Plan"),
                    collocations = listOf("Pure serendipity (Hoàn toàn tình cờ)", "A moment of serendipity (Khoảnh khắc kỳ diệu)"),
                    mnemonic = "Mẹo nhớ: \"Se-ren-đi-pi-ty\" -> Đi dạo (đi) gặp chuyện vui vẻ, bất ngờ.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "ephemeral",
                    word = "Ephemeral",
                    ipa = "/ɪˈfem.ər.əl/",
                    pos = "Tính từ",
                    cefr = "C1",
                    topic = "IELTS Academic",
                    meaning = "Phù du, chóng tàn, chỉ tồn tại hoặc kéo dài trong một thời gian rất ngắn ngủi.",
                    exampleEn = "The ephemeral beauty of cherry blossoms lasts for just a few days each spring.",
                    exampleVi = "Vẻ đẹp phù du của hoa anh đào chỉ kéo dài vài ngày ngắn ngủi mỗi độ xuân về.",
                    synonyms = listOf("Transient", "Fleeting", "Short-lived", "Temporary"),
                    antonyms = listOf("Permanent", "Eternal", "Enduring", "Everlasting"),
                    collocations = listOf("Ephemeral beauty", "Ephemeral nature"),
                    mnemonic = "Mẹo nhớ: \"E-phe-me-ral\" -> Phù du như một giấc mơ thoáng qua.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "idempotent",
                    word = "Idempotent",
                    ipa = "/ˌaɪ.dɛmˈpoʊ.tənt/",
                    pos = "Tính từ",
                    cefr = "C2",
                    topic = "Chuyên ngành IT",
                    meaning = "(Toán / IT) Lũy đẳng; đặc tính của một thao tác mà dù thực hiện 1 lần hay nhiều lần đều cho kết quả giống hệt nhau.",
                    exampleEn = "HTTP GET and PUT methods are designed to be idempotent in RESTful architecture.",
                    exampleVi = "Các phương thức HTTP GET và PUT được thiết kế lũy đẳng trong kiến trúc RESTful.",
                    examples = listOf(
                        mapOf("en" to "HTTP GET, PUT, and DELETE methods should be idempotent.", "vi" to "Các phương thức HTTP GET, PUT và DELETE nên có tính lũy đẳng."),
                        mapOf("en" to "An idempotent payment API prevents double charging.", "vi" to "Một API thanh toán lũy đẳng giúp ngăn chặn việc trừ tiền hai lần.")
                    ),
                    synonyms = listOf("Unchanging", "Consistent", "Repeatable"),
                    antonyms = listOf("Non-idempotent", "Mutating"),
                    collocations = listOf("Idempotent API", "Idempotent operation", "Idempotency key"),
                    mnemonic = "Mẹo nhớ: \"Idem\" (giống nhau) + \"potent\" (năng lực) -> Bao lần làm cũng y nguyên.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "resilient",
                    word = "Resilient",
                    ipa = "/rɪˈzɪl.i.ənt/",
                    pos = "Tính từ",
                    cefr = "B2",
                    topic = "Giao tiếp hàng ngày",
                    meaning = "Kiên cường, mau hồi phục sau khó khăn, biến cố hoặc khả năng đàn hồi co giãn của vật chất.",
                    exampleEn = "She showed a remarkably resilient attitude after facing the startup failure.",
                    exampleVi = "Cô ấy thể hiện tinh thần kiên cường đáng kinh ngạc sau khi đối mặt thất bại khởi nghiệp.",
                    examples = listOf(
                        mapOf("en" to "We need to build a resilient microservices architecture.", "vi" to "Chúng ta cần xây dựng một kiến trúc microservices có khả năng tự phục hồi."),
                        mapOf("en" to "Children are often much more resilient than adults think.", "vi" to "Trẻ em thường kiên cường hơn nhiều so với người lớn nghĩ.")
                    ),
                    synonyms = listOf("Tough", "Flexible", "Strong", "Adaptable"),
                    antonyms = listOf("Fragile", "Vulnerable", "Weak"),
                    collocations = listOf("Resilient system (Hệ thống chịu lỗi)", "Emotionally resilient (Tinh thần kiên cường)"),
                    mnemonic = "Mẹo nhớ: \"Re\" (lại) + \"silient\" (bật dậy) -> Bị đẩy ngã vẫn bật dậy kiên cường.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "pragmatic",
                    word = "Pragmatic",
                    ipa = "/præɡˈmæt.ɪk/",
                    pos = "Tính từ",
                    cefr = "C1",
                    topic = "Giao tiếp hàng ngày",
                    meaning = "Thực tế, thực dụng; giải quyết vấn đề dựa trên hiệu quả thực tế thay vì lý thuyết suông.",
                    exampleEn = "We need a pragmatic approach to complete the product launch on time.",
                    exampleVi = "Chúng ta cần một cách tiếp cận thực tế để hoàn thành việc ra mắt sản phẩm đúng hạn.",
                    synonyms = listOf("Practical", "Realistic", "Sensible", "Down-to-earth"),
                    antonyms = listOf("Idealistic", "Impractical", "Theoretical"),
                    collocations = listOf("Pragmatic approach", "Pragmatic solution"),
                    mnemonic = "Mẹo nhớ: \"Pragmatic\" -> Thực hành (practice) là trên hết, không lý thuyết suông.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "ubiquitous",
                    word = "Ubiquitous",
                    ipa = "/juːˈbɪk.wə.təs/",
                    pos = "Tính từ",
                    cefr = "C1",
                    topic = "Chuyên ngành IT",
                    meaning = "Có mặt ở khắp mọi nơi, phổ biến rộng rãi khắp chốn.",
                    exampleEn = "Smartphones and high-speed mobile internet have become ubiquitous worldwide.",
                    exampleVi = "Điện thoại thông minh và internet di động tốc độ cao đã trở nên phổ biến khắp mọi nơi.",
                    examples = listOf(
                        mapOf("en" to "Smartphones have become ubiquitous in modern daily life.", "vi" to "Điện thoại thông minh đã trở nên phổ biến khắp nơi trong đời sống hiện đại.")
                    ),
                    synonyms = listOf("Omnipresent", "Everywhere", "Pervasive", "Universal"),
                    antonyms = listOf("Rare", "Scarse", "Seldom"),
                    collocations = listOf("Ubiquitous computing", "Ubiquitous presence"),
                    mnemonic = "Mẹo nhớ: \"U-bi-qui-tous\" -> Đi đâu cũng gặp quầy tạp hóa quen thuộc.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "eloquent",
                    word = "Eloquent",
                    ipa = "/ˈel.ə.kwənt/",
                    pos = "Tính từ",
                    cefr = "B2",
                    topic = "IELTS Academic",
                    meaning = "Hùng biện, lưu loát; có tài diễn đạt ý nghĩ và cảm xúc một cách thuyết phục và truyền cảm.",
                    exampleEn = "Her eloquent speech inspired thousands of young engineers in the conference.",
                    exampleVi = "Bài phát biểu hùng biện của cô đã truyền cảm hứng cho hàng ngàn kỹ sư trẻ trong hội nghị.",
                    synonyms = listOf("Fluent", "Articulate", "Expressive", "Persuasive"),
                    antonyms = listOf("Inarticulate", "Hesitant", "Tongue-tied"),
                    collocations = listOf("Eloquent speech", "Eloquent speaker"),
                    mnemonic = "Mẹo nhớ: \"E-loquent\" -> Lời nói lưu loát, thanh thoát, chạm đến trái tim người nghe.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "tenacious",
                    word = "Tenacious",
                    ipa = "/təˈneɪ.ʃəs/",
                    pos = "Tính từ",
                    cefr = "C1",
                    topic = "IELTS Academic",
                    meaning = "Bền bỉ, ngoan cường, kiên trì theo đuổi mục tiêu đến cùng mà không bao giờ bỏ cuộc.",
                    exampleEn = "His tenacious investigation finally brought the truth to light after years.",
                    exampleVi = "Cuộc điều tra kiên trì của anh cuối cùng đã đưa sự thật ra ánh sáng sau nhiều năm.",
                    synonyms = listOf("Persistent", "Determined", "Dogged", "Resolute"),
                    antonyms = listOf("Weak", "Surrendering", "Yielding"),
                    collocations = listOf("Tenacious effort", "Tenacious defender"),
                    mnemonic = "Mẹo nhớ: \"Te-na-cious\" -> Bền bỉ như mầm cây bám chặt trên vách đá.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "synchronous",
                    word = "Synchronous",
                    ipa = "/ˈsɪŋ.krə.nəs/",
                    pos = "Tính từ",
                    cefr = "B2",
                    topic = "Chuyên ngành IT",
                    meaning = "(Công nghệ) Đồng bộ; các tiến trình xảy ra cùng một thời điểm hoặc tuần tự chờ kết quả của nhau.",
                    exampleEn = "Synchronous network calls can block the main UI thread in Android applications.",
                    exampleVi = "Các lệnh gọi mạng đồng bộ có thể làm nghẽn luồng giao diện chính trong ứng dụng Android.",
                    synonyms = listOf("Simultaneous", "Concurrent", "In sync"),
                    antonyms = listOf("Asynchronous", "Non-concurrent"),
                    collocations = listOf("Synchronous communication", "Synchronous operation"),
                    mnemonic = "Mẹo nhớ: \"Syn\" (cùng) + \"chron\" (thời gian) -> Cùng nhịp thời gian đồng bộ.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "ambivalent",
                    word = "Ambivalent",
                    ipa = "/æmˈbɪv.ə.lənt/",
                    pos = "Tính từ",
                    cefr = "C1",
                    topic = "Idioms & Phrasal",
                    meaning = "Mâu thuẫn trong cảm xúc; vừa thích lại vừa không thích, lưỡng lự chưa dứt khoát.",
                    exampleEn = "He felt ambivalent about taking the promotion because it required frequent travel.",
                    exampleVi = "Anh cảm thấy vừa mừng vừa lo về việc thăng chức vì công việc yêu cầu công tác liên tục.",
                    synonyms = listOf("Equivocal", "Uncertain", "Undecided", "Mixed"),
                    antonyms = listOf("Decisive", "Certain", "Unambiguous"),
                    collocations = listOf("Feel ambivalent about", "Ambivalent attitude"),
                    mnemonic = "Mẹo nhớ: \"Ambi\" (hai phía) + \"valent\" -> Tâm trí giằng xé giữa hai ngả đường.",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "bite-the-bullet",
                    word = "Bite the bullet",
                    ipa = "/baɪt ðə ˈbʊl.ɪt/",
                    pos = "Idiom",
                    cefr = "C1",
                    topic = "Idioms & Phrasal Verbs",
                    meaning = "Cắn răng chịu đựng; quyết tâm đối mặt với một tình huống khó khăn không thể tránh khỏi.",
                    exampleEn = "I had to bite the bullet and refactor the entire legacy codebase.",
                    exampleVi = "Tôi đành phải cắn răng quyết tâm viết lại toàn bộ đống code cũ kỹ đó.",
                    examples = listOf(
                        mapOf("en" to "I had to bite the bullet and refactor the entire legacy codebase.", "vi" to "Tôi phải cắn răng chịu đựng và cấu trúc lại toàn bộ mã nguồn cũ.")
                    ),
                    synonyms = listOf("Face the music", "Grin and bear it"),
                    antonyms = listOf("Avoid", "Chicken out"),
                    collocations = listOf("Have to bite the bullet", "Decide to bite the bullet"),
                    mnemonic = "",
                    status = "approved",
                    createdBy = "seed"
                ),
                Word(
                    id = "articulate",
                    word = "Articulate",
                    ipa = "/ɑːˈtɪk.jə.lət/",
                    pos = "Động từ",
                    cefr = "C1",
                    topic = "Giao tiếp hàng ngày",
                    meaning = "Diễn đạt, bày tỏ ý tưởng hoặc cảm xúc một cách rõ ràng, mạch lạc, lưu loát.",
                    exampleEn = "She struggled to articulate her thoughts during the interview.",
                    exampleVi = "Cô ấy gặp khó khăn trong việc diễn đạt mạch lạc suy nghĩ của mình trong buổi phỏng vấn.",
                    examples = listOf(
                        mapOf("en" to "She struggled to articulate her thoughts during the interview.", "vi" to "Cô ấy gặp khó khăn trong việc diễn đạt suy nghĩ của mình trong buổi phỏng vấn.")
                    ),
                    synonyms = listOf("Express clearly", "Vocalize", "Formulate"),
                    antonyms = listOf("Mumble", "Inarticulate", "Hesitant"),
                    collocations = listOf("Clearly articulate (Diễn đạt rõ ràng)", "Articulate a vision (Trình bày tầm nhìn)"),
                    mnemonic = "",
                    grammarNote = "Tính từ phát âm là /ɑːˈtɪk.jə.lət/, Động từ phát âm đuôi là /ɑːˈtɪk.jə.leɪt/.",
                    personalNote = "Từ rất đắt giá để viết vào CV: \"Ability to articulate complex technical concepts\".",
                    status = "approved",
                    createdBy = "seed"
                )
            )
        }
    }
}
