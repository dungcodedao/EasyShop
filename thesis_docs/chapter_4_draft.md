# Chương 4: XÂY DỰNG & TRIỂN KHAI

## 4.1. Cài đặt hệ thống

### 4.1.2. Cấu trúc thư mục dự án

Hệ thống được tổ chức theo mô hình MVVM (Model-View-ViewModel), tách biệt rõ ràng giữa logic nghiệp vụ và giao diện Android:

- `com.example.easyshop.ai`: Module xử lý thông minh tích hợp Gemini.
- `com.example.easyshop.admin`: Các màn hình và quản trị dành cho chủ cửa hàng.
- `com.example.easyshop.repository`: Tầng truy xuất dữ liệu từ Firebase và API.
- `com.example.easyshop.viewmodel`: Quản lý trạng thái và luồng dữ liệu ứng dụng.

### 4.1.3. Thiết lập dịch vụ đám mây và Bảo mật

- **Firebase Core:** Tích hợp `google-services.json` để kết nối các dịch vụ Cloud Firestore, Auth và Messaging (FCM).
- **Security:** Các khóa API nhạy cảm (Gemini, SePay) được bảo mật trong `local.properties`. Hệ thống sử dụng tệp `fcm_service_account.json` để xác thực quyền gửi thông báo đẩy an toàn.
- **Cloudinary:** Sử dụng dịch vụ lưu trữ đám mây Cloudinary để tối ưu hóa việc lưu trữ ảnh sản phẩm qua CDN.

## 4.2. Các module chức năng

### 4.2.1. Module Đăng nhập và Đăng ký

Hỗ trợ xác thực bằng Google và Email. Hệ thống tự động khởi tạo hồ sơ người dùng trên Firestore.

- **[Hình 4.1: Giao diện Đăng nhập và Đăng ký]**

### 4.2.2. Module Quản lý Sản phẩm

Hiển thị danh sách sản phẩm, tìm kiếm và phân loại theo danh mục.

- **[Hình 4.2: Giao diện Trang chủ và Quản lý Catalog]**

### 4.2.3. Module Giỏ hàng và Thanh toán

Xử lý logic giỏ hàng, áp dụng Voucher và tích hợp MBBank QR qua SePay Webhook.

- **[Hình 4.3: Giao diện Giỏ hàng và Thanh toán QR]**

### 4.2.4. Module Trợ lý thông minh AI

Tư vấn sản phẩm thông minh dựa trên kỹ thuật RAG và mô hình Gemini 2.5 Flash.

- **[Hình 4.4: Giao diện Trợ lý AI hội thoại]**

### 4.2.5. Module Quản trị viên

Các chức năng nhập hàng, duyệt đơn, gửi thông báo và thống kê doanh thu.

- **[Hình 4.5: Giao diện Admin App]**

## 4.3. Mã nguồn then chốt

Trong phần này, luận văn trình bày các đoạn mã nguồn cốt lõi thực thi các chức năng nghiệp vụ chính của hệ thống EasyShop.

### 4.3.1. Chức năng Đăng nhập (`AuthViewModel.kt`)
Hệ thống sử dụng Firebase Authentication kết hợp với Firestore để quản lý phiên đăng nhập và phân quyền người dùng.

```kotlin
fun login(email: String, password: String, callback: (Boolean, String?, String?) -> Unit) {
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val userId = auth.currentUser?.uid ?: ""
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(UserModel::class.java)
                    val role = user?.role ?: "user"
                    callback(true, null, role)
                }
        } else {
            callback(false, task.exception?.message, null)
        }
    }
}
```

### 4.3.2. Chức năng Đăng ký (`AuthViewModel.kt`)
Khi đăng ký, hệ thống hỗ trợ cấp quyền Admin tự động thông qua mã bảo mật (`adminCode`).

```kotlin
fun signup(email: String, name: String, password: String, adminCode: String) {
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Tự động gán quyền Admin nếu nhập đúng mã bí mật của Shop
            val userRole = if (adminCode == AppConfig.ADMIN_SECRET_CODE) "admin" else "user"
            val userModel = UserModel(uid = auth.uid, name = name, role = userRole)
            
            firestore.collection("users").document(auth.uid).set(userModel)
        }
    }
}
```

### 4.3.3. Hiển thị sản phẩm (`HomeViewModel.kt`)
Dữ liệu sản phẩm và danh mục được tải đồng bộ từ Firestore để hiển thị lên giao diện người dùng.

```kotlin
private suspend fun initData() {
    try {
        // Tải danh mục và sản phẩm song song để tối ưu tốc độ
        val categoriesDeferred = async { firestore.collection("categories").get().await() }
        val productsDeferred = async { firestore.collection("products").get().await() }
        
        _categories.value = categoriesDeferred.await().toObjects(CategoryModel::class.java)
        _products.value = productsDeferred.await().toObjects(ProductModel::class.java)
    } catch (e: Exception) {
        _screenState.value = ScreenState.ERROR
    }
}
```

### 4.3.4. Tìm kiếm sản phẩm (`SeacherView.kt`)
Tính năng tìm kiếm được tối ưu hóa bằng `LaunchedEffect` để lọc dữ liệu ngay khi người dùng nhập liệu (Real-time Filtering).

```kotlin
LaunchedEffect(searchQuery, allProducts) {
    filteredProducts = if (searchQuery.isBlank()) {
        allProducts
    } else {
        allProducts.filter { product ->
            val title = product.title.lowercase()
            title.contains(searchQuery.lowercase())
        }
    }
}
```

### 4.3.5. Quy trình Thanh toán (`CheckoutViewModel.kt`)
Hệ thống sử dụng Giao dịch Firestore (`runTransaction`) để đảm bảo việc thanh toán và trừ kho diễn ra đồng thời, tránh lỗi lệch tồn kho.

```kotlin
fun placeOrder(paymentMethod: String) {
    db.runTransaction { transaction ->
        // 1. Trừ số lượng trong kho hàng
        user.cartItems.forEach { (pid, qty) ->
            val prodRef = db.collection("products").document(pid)
            val stock = transaction.get(prodRef).getLong("stockCount") ?: 0
            transaction.update(prodRef, "stockCount", stock - qty)
        }
        // 2. Tạo bản ghi đơn hàng mới
        transaction.set(db.collection("orders").document(orderId), order)
        // 3. Xóa giỏ hàng của người dùng
        transaction.update(db.collection("users").document(uid), "cartItems", emptyMap())
    }
}
```

### 4.3.6. Trợ lý AI Tư vấn (RAG Logic)
Đây là tính năng cốt lõi của đề tài, kết hợp dữ liệu thực tế từ Shop với mô hình Gemini AI.

```kotlin
suspend fun sendMessageStream(userMessage: String) = flow {
    val products = fetchAllProducts() // Lấy ngữ cảnh sản phẩm
    val prompt = buildComplexInstruction(userMessage, products) // Tạo Prompt
    
    generativeModel.generateContentStream(prompt).collect { response ->
        emit(response.text)
    }
}
```

## 4.4. Triển khai thực nghiệm

### 4.4.1. Kịch bản kiểm thử (Test Cases)

#### A. Kịch bản kiểm thử phía Người dùng (User)

| STT | Chức năng           | Thao tác thực hiện                 | Kết quả mong đợi                         | Trạng thái | Minh chứng |
| :-- | :-------------------- | :------------------------------------ | :------------------------------------------- | :----------- | :---------- |
| 1   | **Xác thực**  | Đăng nhập bằng tài khoản Google | Đăng nhập thành công, hiện Profile     | ✅ Đạt     | Hình 4.6   |
| 2   | **Tư vấn AI** | Hỏi "Tư vấn máy đồ họa cao"    | AI gợi ý đúng sản phẩm từ kho hàng   | ✅ Đạt     | Hình 4.7   |
| 3   | **Giỏ hàng**  | Thêm sản phẩm vào giỏ            | Giỏ hàng cập nhật đúng số lượng     | ✅ Đạt     | Hình 4.8   |
| 4   | **Giảm giá**  | Áp dụng Voucher giảm giá 10%      | Tổng hóa đơn giảm trừ đúng giá trị | ✅ Đạt     | Hình 4.9   |
| 5   | **Thanh toán** | Quét mã QR ngân hàng MBBank       | Đơn hàng tự nhận diện thanh toán      | ✅ Đạt     | Hình 4.10  |
| 6   | **Hội thoại** | Nhắn tin hỏi Shop hỗ trợ          | Tin nhắn gửi đi tức thì tới Admin      | ✅ Đạt     | Hình 4.11  |

#### B. Kịch bản kiểm thử phía Quản trị (Admin)

| STT | Chức năng               | Thao tác thực hiện                          | Kết quả mong đợi                                  | Trạng thái | Minh chứng |
| :-- | :------------------------ | :--------------------------------------------- | :---------------------------------------------------- | :----------- | :---------- |
| 1   | **Quản lý Kho**   | Admin thêm 1 danh mục sản phẩm mới        | Danh mục mới xuất hiện ngay trên App khách      | ✅ Đạt     | Hình 4.12  |
| 2   | **Phát hành Mã** | Admin tạo mã giảm giá mới (`WELCOME50`) | Mã được hiển thị tại "Ví Voucher" của khách | ✅ Đạt     | Hình 4.13  |
| 3   | **Cập nhật ảnh** | Chụp và Tải ảnh lên Cloudinary            | Ảnh sản phẩm hiển thị sắc nét qua link CDN     | ✅ Đạt     | Hình 4.14  |
| 4   | **Duyệt đơn**    | Chuyển đơn sang "Đã giao hàng"           | Khách nhận được thông báo FCM                  | ✅ Đạt     | Hình 4.15  |
| 5   | **CSKH**            | Trả lời tin nhắn khách hàng               | Khách nhận phản hồi thời gian thực              | ✅ Đạt     | Hình 4.16  |
| 6   | **Thống kê**      | Xem biểu đồ doanh thu tuần                 | Biểu đồ vẽ đúng hiệu quả kinh doanh           | ✅ Đạt     | Hình 4.17  |

### 4.4.2. Đánh giá kết quả thực nghiệm AI

#### a) Phương pháp đánh giá

Nhóm xây dựng tập câu hỏi kiểm thử gồm **40 câu** thuộc 4 nhóm chủ đề thực tế, phản ánh các nhu cầu mua sắm phổ biến của người dùng. Mỗi câu trả lời của AI được đánh giá theo hai tiêu chí:

- **Chính xác:** Thông tin sản phẩm (tên, giá, tồn kho) khớp với dữ liệu thực tế trong Firestore.
- **Tự nhiên:** Câu trả lời mạch lạc, phù hợp ngữ cảnh và không lặp lại thông tin không liên quan.

#### b) Kết quả theo từng nhóm câu hỏi

| Nhóm câu hỏi           | Mô tả                              |   Số câu   | Kết quả đúng |    Tỉ lệ    |
| :------------------------ | :----------------------------------- | :----------: | :--------------: | :-----------: |
| Tìm kiếm sản phẩm     | "Shop có [tên sản phẩm] không?" |      10      |        10        |     100%     |
| So sánh & tư vấn       | "Nên chọn A hay B?"                |      10      |        9        |      90%      |
| Chính sách cửa hàng   | Hỏi về ship, đổi trả, voucher   |      10      |        10        |     100%     |
| Câu hỏi ngoài phạm vi | Câu hỏi không liên quan Shop     |      10      |        9        |      90%      |
| **Tổng cộng**     |                                      | **40** |   **38**   | **95%** |

- **[Hình 4.18: Minh họa đoạn hội thoại AI tư vấn sản phẩm chính xác theo kho hàng thực tế]**

#### c) Nhận xét

- **Điểm mạnh:** AI xử lý tốt hầu hết các kịch bản tìm kiếm và tư vấn sản phẩm nhờ kỹ thuật RAG, không bịa đặt thông tin ngoài kho hàng thực tế.
- **Hạn chế:** Độ chính xác giảm nhẹ (90%) ở nhóm câu hỏi so sánh phức tạp hoặc câu hỏi có nhiều điều kiện lồng nhau (*ví dụ: "Máy nào cân bằng nhất giữa pin, hiệu năng và giá"*). Đây là giới hạn tự nhiên của mô hình ngôn ngữ Gemini 2.5 Flash và là hướng cải thiện trong các phiên bản tiếp theo.
- **Trải nghiệm người dùng:** Nhờ cơ chế phản hồi **Streaming**, nội dung trả lời xuất hiện dần theo thời gian thực thay vì chờ toàn bộ văn bản, tạo cảm giác hội thoại tự nhiên và mượt mà.

### 4.4.3. Đánh giá tổng kết

Qua quá trình kiểm thử thực tế trên thiết bị vật lý, ứng dụng EasyShop đã chứng minh được sự hoàn thiện ở cả hai phía Người dùng và Quản trị viên. Các tính năng then chốt — Trợ lý AI (RAG), Thanh toán QR tự động (SePay Webhook), và Thông báo đẩy (FCM) — đều vận hành ổn định và nhất quán. Kết quả thực nghiệm khẳng định ứng dụng đáp ứng đầy đủ mục tiêu đề ra, sẵn sàng phục vụ nhu cầu thực tế của mô hình thương mại điện tử thông minh.
