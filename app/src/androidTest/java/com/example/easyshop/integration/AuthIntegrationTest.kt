package com.example.easyshop.integration

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import com.example.easyshop.screen.LoginScreen
import com.example.easyshop.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController
    private lateinit var authViewModel: AuthViewModel
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockDb: FirebaseFirestore = mockk(relaxed = true)

    @Before
    fun setUp() {
        // Sử dụng Mockk cho NavController để tránh lỗi Navigation Graph
        navController = mockk(relaxed = true)
        authViewModel = AuthViewModel(mockAuth, mockDb)
    }

    @Test
    fun loginFlow_integration_test() {
        // 1. Gán nội dung màn hình Login cho Test
        composeTestRule.setContent {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        // 2. Nhập email và password giả lập
        composeTestRule.onNodeWithText("Địa chỉ email", ignoreCase = true).performTextInput("test@example.com")
        
        // Tìm đúng ô có nhãn "Mật khẩu" (Exact Match, không phải Substring nên sẽ không trùng "Quên mật khẩu?")
        composeTestRule.onNodeWithText("Mật khẩu", ignoreCase = true).performTextInput("password123")

        // 3. Kiểm tra nút Đăng nhập đã sẵn sàng (enabled)
        composeTestRule.onNodeWithText("Đăng nhập", ignoreCase = true).assertIsEnabled()

        // 4. Nhấn nút Đăng nhập
        composeTestRule.onNodeWithText("Đăng nhập", ignoreCase = true).performClick()

        // 5. Xác nhận rằng Firebase đã được gọi lệnh đăng nhập với đúng dữ liệu
        verify {
            mockAuth.signInWithEmailAndPassword("test@example.com", "password123")
        }
    }

    @Test
    fun signupNavigation_integration_test() {
        composeTestRule.setContent {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        // Link đăng ký có khoảng trắng ở đầu: " Đăng ký"
        composeTestRule.onNodeWithText(" Đăng ký", ignoreCase = true).performClick()

        // Xác nhận NavController đã nhận lệnh điều hướng tới màn hình signup
        verify { navController.navigate("signup") }
    }
}
