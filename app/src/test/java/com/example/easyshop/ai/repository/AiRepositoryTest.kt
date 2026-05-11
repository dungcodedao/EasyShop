package com.example.easyshop.ai.repository

import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.model.CategoryModel
import com.example.easyshop.model.ProductModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiRepositoryTest {

    private lateinit var repository: AiRepository
    private val mockDb = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockHttpClient = mockk<OkHttpClient>()

    @Before
    fun setup() {
        // Khởi tạo repository với các mock dependencies
        repository = AiRepository(mockDb, mockAuth, mockHttpClient)
    }

    @Test
    fun `detectIntent should return promo when message contains voucher or giam gia`() {
        // Chúng ta dùng reflection để truy cập private method detectIntent (hoặc đổi nó thành internal/public)
        // Trong trường hợp này, vì đây là logic quan trọng, tôi sẽ dùng reflection hoặc test gián tiếp.
        // Tuy nhiên, để đơn giản cho demo, tôi sẽ giả định chúng ta đã chuyển detectIntent sang internal.
        
        val method = repository.javaClass.getDeclaredMethod("detectIntent", String::class.java)
        method.isAccessible = true

        assertEquals("promo", method.invoke(repository, "Cho mình xin mã giảm giá với"))
        assertEquals("promo", method.invoke(repository, "Shop có voucher nào không?"))
        assertEquals("promo", method.invoke(repository, "Săn coupon cực hời"))
    }

    @Test
    fun `detectIntent should return payment when message contains chuyen khoan or momo`() {
        val method = repository.javaClass.getDeclaredMethod("detectIntent", String::class.java)
        method.isAccessible = true

        assertEquals("payment", method.invoke(repository, "Thanh toán bằng MoMo được không?"))
        assertEquals("payment", method.invoke(repository, "Cách chuyển khoản ngân hàng"))
    }

    @Test
    fun `calculateProductScore should give high score for exact title match`() {
        val product = ProductModel(
            id = "p1",
            title = "Laptop Gaming ASUS ROG",
            price = "30000000",
            category = "Laptop"
        )
        
        val method = repository.javaClass.getDeclaredMethod(
            "calculateProductScore", 
            ProductModel::class.java, String::class.java, List::class.java, List::class.java
        )
        method.isAccessible = true

        // Params: product, query, allTokens, primaryTokens
        val score = method.invoke(repository, product, "laptop gaming", listOf("laptop", "gaming"), listOf("laptop", "gaming")) as Int
        
        assertTrue("Score should be high for exact match", score >= 10)
    }

    @Test
    fun `selectRelevantProducts should return sorted products based on relevance`() {
        val p1 = ProductModel(id = "1", title = "iPhone 15 Pro", category = "Phone")
        val p2 = ProductModel(id = "2", title = "MacBook Air M2", category = "Laptop")
        val p3 = ProductModel(id = "3", title = "iPhone 13", category = "Phone")
        
        val products = listOf(p1, p2, p3)
        
        val method = repository.javaClass.getDeclaredMethod(
            "selectRelevantProducts", 
            String::class.java, List::class.java, List::class.java
        )
        method.isAccessible = true

        // Tìm kiếm "iphone"
        @Suppress("UNCHECKED_CAST")
        val results = method.invoke(repository, "iphone", emptyList<ChatMessage>(), products) as List<ProductModel>
        
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "1" })
        assertTrue(results.any { it.id == "3" })
    }

    @Test
    fun `detectIntent should recognize various intents and handle accents`() {
        val method = repository.javaClass.getDeclaredMethod("detectIntent", String::class.java)
        method.isAccessible = true

        // Comparison intent
        assertEquals("comparison", method.invoke(repository, "So sánh iPhone 15 và 14"))
        
        // Budget intent
        assertEquals("budget", method.invoke(repository, "Tìm máy nào giá rẻ dưới 10 triệu"))
        
        // Payment with accents and mixed case
        assertEquals("payment", method.invoke(repository, "TRẢ GÓP qua thẻ tín dụng"))
        
        // Advice (default)
        assertEquals("advice", method.invoke(repository, "Chào shop, chúc một ngày tốt lành"))
    }

    @Test
    fun `foldText should normalize complex Vietnamese accents`() {
        val method = repository.javaClass.getDeclaredMethod("foldText", String::class.java)
        method.isAccessible = true

        val input = "Đồ điện tử gia dụng giá rẻ"
        val expected = "do dien tu gia dung gia re"
        assertEquals(expected, method.invoke(repository, input))
    }

    @Test
    fun `calculateProductScore should prioritize title over category`() {
        val product = ProductModel(id = "p1", title = "Tủ lạnh Samsung", category = "Gia dụng")
        val method = repository.javaClass.getDeclaredMethod(
            "calculateProductScore", 
            ProductModel::class.java, String::class.java, List::class.java, List::class.java
        )
        method.isAccessible = true

        // Query match title "tủ lạnh"
        val titleScore = method.invoke(repository, product, "tu lanh", listOf("tu", "lanh"), listOf("tu", "lanh")) as Int
        // Query match category "gia dụng"
        val categoryScore = method.invoke(repository, product, "gia dung", listOf("gia", "dung"), listOf("gia", "dung")) as Int

        assertTrue("Title match should have higher score ($titleScore) than category match ($categoryScore)", titleScore > categoryScore)
    }

    @Test
    fun `selectRelevantProducts should utilize history tokens for context-aware search`() {
        val p1 = ProductModel(id = "1", title = "iPhone 15 Pro", category = "Phone")
        val p2 = ProductModel(id = "2", title = "Samsung Galaxy S24", category = "Phone")
        val products = listOf(p1, p2)
        
        val method = repository.javaClass.getDeclaredMethod(
            "selectRelevantProducts", 
            String::class.java, List::class.java, List::class.java
        )
        method.isAccessible = true

        // Lịch sử: User hỏi về iPhone
        val history = listOf(
            ChatMessage(content = "Shop có iPhone không?", isUser = true),
            ChatMessage(content = "Có ạ, shop có iPhone 15 Pro.", isUser = false)
        )

        // Query hiện tại: "Nó giá bao nhiêu?" -> "Nó" không chứa thông tin sản phẩm, 
        // nhưng "iPhone" từ history sẽ giúp tìm đúng sản phẩm.
        @Suppress("UNCHECKED_CAST")
        val results = method.invoke(repository, "Nó giá bao nhiêu?", history, products) as List<ProductModel>
        
        assertTrue("Should find iPhone based on history", results.any { it.id == "1" })
        assertTrue("iPhone should be the top result", results.first().id == "1")
    }
}
