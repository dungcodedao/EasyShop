package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyshop.model.UserModel
import com.google.firebase.auth.FirebaseAuth
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
        callback: (Boolean, String?, String?) -> Unit  // ← Thêm role parameter
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    // ✅ Get user role from Firestore
                    firestore.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            val user = document.toObject(UserModel::class.java)
                            val role = user?.role ?: "user"
                            callback(true, null, role)  // ← Return role
                        }
                        .addOnFailureListener {
                            callback(true, null, "user")  // ← Default to user on error
                        }
                } else {
                    callback(false, task.exception?.message, null)
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