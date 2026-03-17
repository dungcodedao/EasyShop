# 🛒 EasyShop - Ứng dụng Thương mại điện tử Android

**EasyShop** là một ứng dụng di động mua sắm trực tuyến chuyên nghiệp được xây dựng trên nền tảng Android. Ứng dụng cung cấp giải pháp toàn diện cho cả người mua hàng và người quản lý, với giao diện hiện đại, mượt mà và tối ưu hóa trải nghiệm người dùng.

---

## ✨ Chức năng chính

### 👤 Đối với Người dùng
- **Khám phá sản phẩm:** Duyệt sản phẩm theo danh mục, tìm kiếm thông minh và lọc sản phẩm.
- **🤖 Trợ lý ảo AI (Gemini):**
    - Hỗ trợ tư vấn sản phẩm thông minh dựa trên ngữ cảnh cuộc hội thoại.
    - **Multi-turn Memory:** Ghi nhớ nội dung đã trao đổi để tư vấn chính xác hơn.
    - **Smart Search:** Tự động đề xuất sản phẩm liên quan từ dữ liệu Firestore dựa trên từ khóa câu hỏi.
    - **Tư vấn mua hàng:** Giải đáp thắc mắc về sản phẩm, so sánh giá, gợi ý món phù hợp với nhu cầu.
    - **Hỗ trợ đơn hàng:** Hướng dẫn quy trình đặt hàng, thanh toán và theo dõi đơn hàng.
    - **Lịch sử chat:** Lưu toàn bộ cuộc hội thoại trên Firestore, đồng bộ thời gian thực trên mọi thiết bị.
    - **Xóa & Làm mới:** Người dùng có thể xóa lịch sử và bắt đầu hội thoại mới bất cứ lúc nào.
- **Quản lý mua sắm:** Giỏ hàng tiện lợi, tính toán đơn hàng thời gian thực và áp dụng mã giảm giá.
- **Theo dõi đơn hàng:** Xem lịch sử mua hàng và chi tiết các sản phẩm đã đặt.
- **Cá nhân hóa:** Quản lý hồ sơ, chọn ảnh đại diện đồng bộ hệ thống và danh sách yêu thích.

### 🛡️ Đối với Quản trị viên
- **Dashboard thông minh:** Theo dõi doanh thu, tăng trưởng người dùng và thống kê đơn hàng.
- **Quản lý kho hàng:** CRUD sản phẩm, quản lý danh mục và tồn kho.
- **Xử lý đơn hàng:** Tiếp nhận và cập nhật trạng thái đơn hàng của khách hàng.
- **Quản lý khách hàng:** Xem chi tiết thông tin và thống kê mua sắm của từng user.

---

## 🛠️ Công nghệ sử dụng
- **Ngôn ngữ:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Backend:** Firebase (Auth, Firestore, Storage)
- **AI:** Google Gemini AI (Generative AI SDK)
- **Bảo mật:** Quản lý API Key an toàn qua `local.properties` và `BuildConfig`.
- **Kiến trúc:** MVVM (Model-View-ViewModel) + Repository Pattern.
- **Thư viện:** Coil (Image Loading), Compose Navigation, Razorpay (Payment).

---
*Dự án tập trung vào tính thẩm mỹ, bảo mật và hiệu năng cao cho trải nghiệm thương mại điện tử di động.*
