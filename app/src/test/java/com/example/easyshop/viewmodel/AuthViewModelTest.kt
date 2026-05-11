package com.example.easyshop.viewmodel

import com.example.easyshop.AppUtil
import com.example.easyshop.model.UserModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val mockDb = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockAuthTask = mockk<Task<AuthResult>>()
    private val mockFirestoreTask = mockk<Task<Void>>()
    private val mockUser = mockk<FirebaseUser>()

    @Before
    fun setup() {
        mockkObject(AppUtil)
        every { AppUtil.syncFcmToken() } just runs
        
        viewModel = AuthViewModel(mockAuth, mockDb)
        
        // Cấu hình mặc định cho các mock
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test_uid"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `signup with correct admin code should assign admin role`() {
        val email = "admin@test.com"
        val name = "Admin User"
        val password = "password123"
        val adminCode = "EASY_ADMIN_2024" // Đúng mã bí mật
        
        var capturedRole: String? = null
        
        // Mock luồng Firebase Auth
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockAuthTask
        val authListenerSlot = slot<OnCompleteListener<AuthResult>>()
        every { mockAuthTask.addOnCompleteListener(capture(authListenerSlot)) } returns mockAuthTask
        every { mockAuthTask.isSuccessful } returns true
        
        // Mock luồng Firestore
        val mockCollection = mockk<CollectionReference>()
        val mockDocument = mockk<DocumentReference>()
        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        
        val userModelSlot = slot<UserModel>()
        every { mockDocument.set(capture(userModelSlot)) } returns mockFirestoreTask
        
        val successListenerSlot = slot<OnSuccessListener<Void>>()
        every { mockFirestoreTask.addOnSuccessListener(capture(successListenerSlot)) } returns mockFirestoreTask
        every { mockFirestoreTask.addOnFailureListener(any()) } returns mockFirestoreTask

        // Chạy hàm signup
        viewModel.signup(email, name, password, adminCode) { success, error, role ->
            capturedRole = role
        }
        
        // Kích hoạt callback giả lập
        authListenerSlot.captured.onComplete(mockAuthTask)
        successListenerSlot.captured.onSuccess(null)

        // Kiểm tra kết quả
        assertEquals("admin", userModelSlot.captured.role)
        assertEquals("admin", capturedRole)
    }

    @Test
    fun `signup with wrong admin code should assign user role`() {
        val email = "user@test.com"
        val name = "Normal User"
        val password = "password123"
        val adminCode = "WRONG_CODE" // Sai mã
        
        var capturedRole: String? = null
        
        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockAuthTask
        val authListenerSlot = slot<OnCompleteListener<AuthResult>>()
        every { mockAuthTask.addOnCompleteListener(capture(authListenerSlot)) } returns mockAuthTask
        every { mockAuthTask.isSuccessful } returns true
        
        val mockCollection = mockk<CollectionReference>()
        val mockDocument = mockk<DocumentReference>()
        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        
        val userModelSlot = slot<UserModel>()
        every { mockDocument.set(capture(userModelSlot)) } returns mockFirestoreTask
        
        val successListenerSlot = slot<OnSuccessListener<Void>>()
        every { mockFirestoreTask.addOnSuccessListener(capture(successListenerSlot)) } returns mockFirestoreTask
        every { mockFirestoreTask.addOnFailureListener(any()) } returns mockFirestoreTask

        viewModel.signup(email, name, password, adminCode) { success, error, role ->
            capturedRole = role
        }
        
        authListenerSlot.captured.onComplete(mockAuthTask)
        successListenerSlot.captured.onSuccess(null)

        // Phải là "user" vì mã sai
        assertEquals("user", userModelSlot.captured.role)
        assertEquals("user", capturedRole)
    }
}
