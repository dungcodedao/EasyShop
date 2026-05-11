package com.example.easyshop.viewmodel

import com.example.easyshop.model.PromoCodeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CheckoutViewModelTest {

    private lateinit var viewModel: CheckoutViewModel
    private val mockDb = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()

    @Before
    fun setup() {
        io.mockk.mockkObject(com.example.easyshop.AppUtil)
        io.mockk.every { com.example.easyshop.AppUtil.getString(any(), *anyVararg()) } returns "Error"
        io.mockk.every { com.example.easyshop.AppUtil.getString(any()) } returns "Error"
        
        viewModel = CheckoutViewModel(mockDb, mockAuth)
    }

    @org.junit.After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `applyPromoCode with percentage should calculate correct discount`() {
        // Giả lập subtotal là 1.000.000
        viewModel.setDiscountInfo("", 0.0, 1000000.0)
        
        val promo = PromoCodeModel(
            code = "SUMMER10",
            type = "percentage",
            value = 10.0, // 10%
            maxDiscount = 50000.0 // Tối đa 50k
        )

        viewModel.applyPromoCode("SUMMER10", promo)

        // 10% của 1 triệu là 100k, nhưng max là 50k -> Discount nên là 50k
        assertEquals(50000.0, viewModel.discount.value, 0.1)
        assertEquals("SUMMER10", viewModel.promoCode.value)
    }

    @Test
    fun `applyPromoCode with fixed amount should calculate correct discount`() {
        viewModel.setDiscountInfo("", 0.0, 1000000.0)
        
        val promo = PromoCodeModel(
            code = "FIXED50",
            type = "fixed",
            value = 50000.0
        )

        viewModel.applyPromoCode("FIXED50", promo)

        assertEquals(50000.0, viewModel.discount.value, 0.1)
    }

    @Test
    fun `removePromoCode should reset discount and code`() {
        viewModel.setDiscountInfo("CODE", 50000.0, 1000000.0)
        
        viewModel.removePromoCode()

        assertEquals(0.0, viewModel.discount.value, 0.1)
        assertEquals("", viewModel.promoCode.value)
    }

    @Test
    fun `applyPromoCode should not exceed subtotal`() {
        viewModel.setDiscountInfo("", 0.0, 30000.0) // Subtotal chỉ 30k
        
        val promo = PromoCodeModel(
            code = "BIG",
            type = "fixed",
            value = 50000.0 // Giảm 50k
        )

        viewModel.applyPromoCode("BIG", promo)

        // Discount không được vượt quá subtotal
        assertEquals(30000.0, viewModel.discount.value, 0.1)
    }
    @Test
    fun `applyPromoCode with percentage and zero maxDiscount should calculate unlimited discount`() {
        viewModel.setDiscountInfo("", 0.0, 1000000.0)
        
        val promo = PromoCodeModel(
            code = "NOLIMIT",
            type = "percentage",
            value = 15.0, // 15%
            maxDiscount = 0.0 // Không giới hạn
        )

        viewModel.applyPromoCode("NOLIMIT", promo)

        // 15% của 1 triệu là 150k
        assertEquals(150000.0, viewModel.discount.value, 0.1)
    }

    @Test
    fun `applyPromoCode with invalid type should return 0 discount`() {
        viewModel.setDiscountInfo("", 50000.0, 1000000.0) // Đang có discount 50k
        
        val promo = PromoCodeModel(
            code = "WEIRD",
            type = "unknown_type",
            value = 50.0
        )

        viewModel.applyPromoCode("WEIRD", promo)

        // Nếu type sai, discount bị reset về 0
        assertEquals(0.0, viewModel.discount.value, 0.1)
    }

    @Test
    fun `placeOrder without address should return Error`() {
        // Sử dụng Reflection để inject user giả vào ViewModel (vượt qua _userModel.value ?: return)
        val userModelField = CheckoutViewModel::class.java.getDeclaredField("_userModel")
        userModelField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableFlow = userModelField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.easyshop.model.UserModel?>
        mutableFlow.value = com.example.easyshop.model.UserModel(name = "Test", uid = "123")

        // Đảm bảo selectedAddress là null
        viewModel.placeOrder(paymentMethod = "COD")

        val result = viewModel.checkoutResult.value
        assert(result is CheckoutResult.Error)
        // Check if error message is not empty
        assert((result as CheckoutResult.Error).message.isNotEmpty())
    }
}
