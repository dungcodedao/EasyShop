package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyshop.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Signup function với auto-assign admin role
     */
    fun signup(
        email: String,
        name: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    // ✅ Auto set admin nếu email có đuôi @easyshop.com
                    val userRole = if (email.lowercase().endsWith("@easyshop.com")) {
                        "admin"
                    } else {
                        "user"
                    }

                    val userModel = UserModel(
                        name = name,
                        email = email,
                        uid = userId,
                        role = userRole,
                        cartItems = emptyMap(),
                        address = ""
                    )

                    // Save user to Firestore
                    firestore.collection("users")
                        .document(userId)
                        .set(userModel)
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message)
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Login function
     */
    /**
     * Login function with role check
     */
    fun login(
        email: String,
        password: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    firestore.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            val user = document.toObject(UserModel::class.java)
                            val role = user?.role ?: "user"
                            callback(true, null, role)
                        }
                        .addOnFailureListener {
                            callback(true, null, "user")
                        }
                } else {
                    callback(false, task.exception?.message, null)
                }
            }
    }

    /**
     * Firebase Google Login
     */
    fun signInWithGoogle(idToken: String, callback: (Boolean, String?, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: ""
                    
                    // Kiểm tra xem User đã tồn tại trên Firestore chưa
                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // Lấy thông tin từ Google gán vào UserModel mới
                                val newUser = UserModel(
                                    uid = userId,
                                    email = user?.email ?: "",
                                    name = user?.displayName ?: "Google User",
                                    role = "user", // Luôn mặc định là user khi đăng nhập Google
                                    profileImg = user?.photoUrl?.toString() ?: ""
                                )
                                firestore.collection("users").document(userId).set(newUser)
                                    .addOnSuccessListener { callback(true, null, "user") }
                            } else {
                                val role = document.getString("role") ?: "user"
                                callback(true, null, role)
                            }
                        }
                } else {
                    callback(false, task.exception?.message, null)
                }
            }
    }

    /**
     * Reset Password (Forgot Password)
     */
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Logout function
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Get current user
     */
    fun getCurrentUser() = auth.currentUser
}