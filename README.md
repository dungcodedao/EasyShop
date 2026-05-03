# 🌟 EasyShop - Android E-Commerce Real-time Application
> **Đồ án tốt nghiệp: Hệ thống thương mại điện tử tích hợp AI & Hỗ trợ trực tuyến**

---

## 📖 Giới thiệu (Overview)

**EasyShop** là một giải pháp thương mại điện tử di động toàn diện, được thiết kế để mang lại trải nghiệm mua sắm mượt mà, phản hồi tức thì (Real-time) và tối ưu hóa hiệu năng trên nền tảng Android. Ứng dụng áp dụng những công nghệ tiên tiến nhất hiện nay như **Jetpack Compose** cho giao diện hiện đại và **Firebase** làm kiến trúc Backend-as-a-Service (BaaS).

### 🎯 Mục tiêu dự án
- Xây dựng hệ thống bán hàng đa vai trò (User & Admin).
- Tối ưu hóa trải nghiệm người dùng với hệ thống thông báo đẩy (Push Notifications).
- Tích hợp **AI Chatbot** (Gemini) và **Hỗ trợ trực tuyến** (Shop Chat) để phục vụ khách hàng.
- Quản lý tài nguyên hình ảnh tối ưu thông qua **Cloudinary**.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Tầng (Layer) | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Giao diện (UI)** | Jetpack Compose, MD3 | Thiết kế UI khai báo, linh hoạt và hiện đại. |
| **Kiến trúc** | MVVM | Đảm bảo logic nghiệp vụ tách biệt với giao diện. |
| **Ngôn ngữ** | Kotlin (Coroutines & Flow) | Xử lý bất đồng bộ và luồng dữ liệu thời gian thực. |
| **Backend** | Firebase Auth, Firestore | Quản lý định danh và cơ sở dữ liệu Real-time. |
| **Hình ảnh** | Cloudinary & OkHttp | Tải và lưu trữ hình ảnh (Sản phẩm & Chat). |
| **AI** | Generative AI SDK | Tích hợp Trợ lý ảo tư vấn khách hàng. |

---

## 🏗️ Kiến Trúc & Tài Liệu (Architecture & Docs)

Để hiểu sâu hơn về kiến trúc hệ thống, sơ đồ luồng dữ liệu và cấu trúc Firestore, vui lòng xem tại:
👉 **[PROJECT_ANALYSIS.md](docs/PROJECT_ANALYSIS.md)**

### Tóm lược kiến trúc Database
- **Snapshot Pattern:** Chụp ảnh thông tin sản phẩm tại thời điểm đặt hàng.
- **Embedded Pattern:** Nhúng mảng địa chỉ trực tiếp vào tài liệu User.
- **Real-time Sync:** Đồng bộ hóa tin nhắn và trạng thái đơn hàng ngay lập tức.

---

## 💎 Tính Năng Nổi Bật (Key Features)

### 1. Hệ thống Chat Kép (Dual-Chat System)
- **AI Assistant:** Tư vấn sản phẩm, giải đáp thắc mắc tự động dựa trên Gemini AI.
- **Shop Support:** Chat trực tiếp với Admin, hỗ trợ gửi hình ảnh (qua Cloudinary) với giao diện Indigo/White hiện đại.

### 2. Thông báo Thông minh (Smart Notifications)
- **In-app Banner:** Trượt từ trên xuống với hiệu ứng mượt mà khi có thay đổi đơn hàng.
- **Push Notification:** Firebase Cloud Messaging giúp nhận tin nhắn ngay cả khi tắt app.

### 3. Quản trị Toàn diện (Admin Dashboard)
- Quản lý sản phẩm, danh mục, mã giảm giá (Promo Code).
- Theo dõi doanh thu và duyệt đơn hàng tập trung.

---

## 🚀 Hướng Dẫn Cài Đặt (Installation)

1. **Clone project:**
   ```bash
   git clone https://github.com/your-username/EasyShop.git
   ```
2. **Cấu hình Firebase:**
   - Tạo project trên [Firebase Console](https://console.firebase.google.com/).
   - Thêm file `google-services.json` vào thư mục `app/`.
   - Bật Authentication (Email & Google) và Firestore.
3. **Cấu hình Cloudinary:**
   - Cập nhật API Key trong `CloudinaryUploader.kt` hoặc file config tương ứng.
4. **Build & Run:** Mở dự án bằng Android Studio và chạy trên Emulator hoặc thiết bị thật.

---

## 📸 Hình ảnh Minh họa (Screenshots)

| Trang Chủ | Chi Tiết Sản Phẩm | Trợ Lý AI |
| :---: | :---: | :---: |
| ![Home](screenshots/home.png) | ![Details](screenshots/details.png) | ![AI](screenshots/ai.png) |

| Chat với Shop | Thông Báo | Quản Trị (Admin) |
| :---: | :---: | :---: |
| ![Chat](screenshots/chat.png) | ![Notif](screenshots/notif.png) | ![Admin](screenshots/admin.png) |

---

## 🤝 Liên hệ
- **Tác giả:** [Tên của bạn]
- **Email:** [Email của bạn]
- **Trường:** [Tên trường của bạn]

---
*© 2026 EasyShop Project - Graduation Thesis.*

