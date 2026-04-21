package com.example.easyshop.repository

import com.example.easyshop.model.ChatSession
import com.example.easyshop.model.ShopChatMessage
import com.example.easyshop.services.FcmSender
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Lấy danh sách tin nhắn của một cuộc hội thoại (Thời gian thực)
     */
    fun getMessages(userId: String): Flow<List<ShopChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(userId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ShopChatMessage::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Gửi tin nhắn
     */
    suspend fun sendMessage(message: ShopChatMessage, userId: String, userName: String, isAdmin: Boolean, userProfileImage: String? = null) {
        val chatRef = db.collection("chats").document(userId)
        val messageRef = chatRef.collection("messages").document()
        
        val finalMessage = message.copy(id = messageRef.id)
        
        // 1. Lưu tin nhắn vào collection messages
        messageRef.set(finalMessage).await()
        
        // 2. Cập nhật metadata của ChatSession
        val updates = mutableMapOf<String, Any>(
            "lastMessage" to finalMessage.text,
            "lastTimestamp" to finalMessage.timestamp,
            "userId" to userId
        )
        
        // Chỉ cập nhật thông tin user khi chính user đó gửi tin
        if (!isAdmin) {
            updates["userName"] = userName
            userProfileImage?.let { updates["userProfileImage"] = it }
        }
        
        if (isAdmin) {
            updates["unreadCountByUser"] = com.google.firebase.firestore.FieldValue.increment(1)
        } else {
            updates["unreadCountByAdmin"] = com.google.firebase.firestore.FieldValue.increment(1)
        }
        
        chatRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
        
        // 3. Gửi Push Notification
        if (isAdmin) {
            // Admin nhắn cho User
            FcmSender.sendToUser(userId, "Shop đã trả lời", finalMessage.text, "CHAT", excludeUserId = finalMessage.senderId)
        } else {
            // User nhắn cho Admin
            FcmSender.sendToAdmins("Tin nhắn mới từ $userName", finalMessage.text, "CHAT", excludeUserId = finalMessage.senderId)
        }
    }

    /**
     * Đánh dấu tất cả tin nhắn là đã đọc
     */
    suspend fun markAsRead(userId: String, isAdmin: Boolean) {
        val chatRef = db.collection("chats").document(userId)
        val updates = if (isAdmin) {
            mapOf("unreadCountByAdmin" to 0)
        } else {
            mapOf("unreadCountByUser" to 0)
        }
        // Sử dụng set với merge thay cho update để không bị crash nếu tài liệu không tồn tại
        chatRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Lấy danh sách tất cả các cuộc hội thoại (Cho Admin - Thời gian thực)
     */
    fun getAllChatSessions(): Flow<List<ChatSession>> = callbackFlow {
        val listener = db.collection("chats")
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { it.toObject(ChatSession::class.java) } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Delete chat session and its messages
     */
    suspend fun deleteChatSession(userId: String) {
        val chatRef = db.collection("chats").document(userId)
        val messagesRef = chatRef.collection("messages")
        
        try {
            // Delete all message documents in subcollection
            val messagesSnapshot = messagesRef.get().await()
            for (doc in messagesSnapshot.documents) {
                doc.reference.delete().await()
            }
            
            // Delete the main chat session document
            chatRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

