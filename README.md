# 🛒 EasyShop - Ứng dụng Android Thương mại Điện tử Hiện đại

EasyShop là một ứng dụng Thương mại điện tử (E-Commerce) hiệu năng cao, đầy đủ tính năng được xây dựng bằng **Jetpack Compose** và **Firebase**. Ứng dụng mang lại trải nghiệm mua sắm mượt mà cho người dùng và cung cấp bảng điều khiển quản trị toàn diện cho quản trị viên để quản lý sản phẩm, đơn hàng và người dùng.

---

## ✨ Tính năng nổi bật

### 👤 Dành cho Người dùng
*   **Giao diện UI/UX trực quan:** Sử dụng Jetpack Compose với Material 3, kết hợp phong cách glassmorphism và thẩm mỹ hiện đại.
*   **Khám phá sản phẩm:** Duyệt sản phẩm theo danh mục, tìm kiếm theo tên và lọc kết quả thông minh.
*   **Giỏ hàng & Thanh toán nâng cao:** Quản lý giỏ hàng, áp dụng mã khuyến mãi, tính toán tổng đơn hàng (tạm tính, thuế, giảm giá).
*   **Thanh toán bảo mật:** Tích hợp cả thanh toán giả lập (để kiểm thử) và thanh toán thực tế qua cổng Razorpay.
*   **Theo dõi đơn hàng:** Xem lịch sử và trạng thái đơn hàng ngay trong mục hồ sơ cá nhân.
*   **Danh sách yêu thích (Wishlist):** Lưu lại các sản phẩm yêu thích để mua sau.
*   **Quản lý hồ sơ:** Cập nhật thông tin cá nhân, địa chỉ và ảnh đại diện.

### 🛡️ Dành cho Quản trị viên (Admin)
*   **Bảng điều khiển phân tích:** Thống kê thời gian thực về tổng doanh thu, số lượng sản phẩm và tăng trưởng người dùng.
*   **Quản lý sản phẩm:** Đầy đủ các thao tác CRUD (Thêm, Xem, Sửa, Xóa) cho sản phẩm và danh mục.
*   **Quản lý đơn hàng:** Theo dõi và xử lý đơn hàng của người dùng một cách hiệu quả.
*   **Quản lý người dùng:** Xem và quản lý danh sách người dùng đã đăng ký.
*   **Quản lý danh mục:** Thêm hoặc sửa đổi các danh mục sản phẩm linh hoạt, hiển thị trực tiếp bằng icon trên trang chủ.

---

## 🛠️ Công nghệ sử dụng

*   **Ngôn ngữ:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Điều hướng:** Compose Navigation
*   **Backend:** Firebase (Authentication, Firestore, Cloud Storage)
*   **Tải ảnh:** Coil
*   **Kiến trúc:** MVVM (Model-View-ViewModel) / State Management
*   **Cổng thanh toán:** Razorpay & Mock Integration

---

## 🎨 Tối ưu hóa giao diện (UI)
Ứng dụng đã được tối ưu hóa để tận dụng tối đa không gian màn hình:
*   **Thiết kế gọn gàng:** Giảm khoảng cách (padding chỉ 8dp) để hiển thị được nhiều nội dung hơn trên một màn hình.
*   **Header thông minh:** Sử dụng `CenterAlignedTopAppBar` giúp thanh điều hướng phía trên trông hiện đại, tinh tế và tiết kiệm diện tích.
*   **Thanh điều hướng dưới (Bottom Navigation):** Chuyển đổi mượt mà giữa các mục chính của ứng dụng.

---

## 🚀 Hướng dẫn cài đặt

### 1. Điều kiện tiên quyết
*   Android Studio Ladybug trở lên.
*   JDK 17 hoặc cao hơn.
*   Một dự án Firebase đã được tạo.

### 2. Cấu hình Firebase
1.  Tạo project trên [Firebase Console](https://console.firebase.google.com/).
2.  Bật **Email/Password Authentication**.
3.  Thiết lập **Cloud Firestore** và **Firebase Storage**.
4.  Tải tệp `google-services.json` về và đặt vào thư mục `app/`.

### 3. Cài đặt ứng dụng
1.  Clone repository này về máy.
2.  Mở project bằng Android Studio.
3.  Đồng bộ (Sync) project với Gradle.
4.  Chạy ứng dụng trên Trình giả lập (Emulator) hoặc thiết bị thật.

---

## 📁 Cấu trúc dự án (Modules chính)

*   `pages/`: Các màn hình chính (Trang chủ, Chi tiết sản phẩm, Giỏ hàng, Thanh toán, Hồ sơ).
*   `admin/`: Các màn hình dành riêng cho quản trị viên và phân tích dữ liệu.
*   `components/`: Các thành phần UI có thể tái sử dụng (BannerView, SearchView, Thẻ sản phẩm).
*   `model/`: Các lớp dữ liệu đại diện cho Sản phẩm, Người dùng và Đơn hàng.
*   `sale/`: Logic và các thành phần liên quan đến khuyến mãi và thanh toán.
*   `AppUtil.kt`: Các tiện ích toàn cục để xử lý logic giỏ hàng, định dạng tiền tệ và thông báo.

---

## 🔐 Bảo mật & Bảo trì (Dành cho Đồ án)

Phần này giúp bạn ghi nhớ các thiết lập bảo mật quan trọng để giải trình với hội đồng:

### 1. Quản lý mã nguồn (Git)
*   **File `.gitignore`**: Đã được cấu hình để ẩn các file nhạy cảm như `google-services.json` và `local.properties`.
*   **Lưu ý**: Nếu bạn nộp bài qua GitHub, hãy nén và gửi riêng các file này cho thầy cô vì chúng không có trên mạng.

### 2. Bảo mật phía Server (Firebase)
*   **Firestore Rules**: Hãy đảm bảo bạn đã chuyển từ "Test Mode" sang "Locked Mode" hoặc cấu hình chỉ cho phép người dùng đã Log-in mới được sửa dữ liệu.
*   **Xác thực**: Sử dụng Firebase Auth để quản trị quyền Admin.

### 3. Bí kíp trả lời Hội đồng (Q&A)
*   **Hỏi**: *"Làm sao em bảo vệ API Key của mình?"* 
    *   **Trả lời**: *"Em lưu chúng trong file `local.properties` và dùng `secrets-gradle-plugin` để truy xuất, tránh việc hardcode trực tiếp vào mã nguồn."*
*   **Hỏi**: *"App này có chống lại việc bị người khác xem trộm code không?"*
    *   **Trả lời**: *"Dạ có, em đã cấu hình R8/ProGuard để mã hóa (obfuscate) code khi xuất file APK, giúp gây khó khăn cho việc dịch ngược ứng dụng."*

---

## 📝 Giấy phép
Dự án này được thực hiện vì mục đích giáo dục và đồ án tốt nghiệp. Bạn có thể tự do khám phá và phát triển thêm!
