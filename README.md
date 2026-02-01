# 🛒 EasyShop - Modern E-Commerce Android Application

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-green.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Integrated-orange.svg)](https://firebase.google.com)

---

### 🇻🇳 Giới thiệu (Vietnamese)
**EasyShop** là một ứng dụng thương mại điện tử Android hiện đại, được thiết kế để mang lại trải nghiệm mua sắm mượt mà nhất. Dự án tập trung vào việc sử dụng các công nghệ mới nhất trong hệ sinh thái Android như **Jetpack Compose**, **Firebase** và kiến trúc **MVVM**.

### 🇺🇸 Introduction (English)
**EasyShop** is a modern Android e-commerce application designed to provide a seamless shopping experience. This project showcases the latest Android development practices, including **Jetpack Compose**, **Firebase**, and **MVVM Architecture**.

---

## ✨ Key Features / Tính năng chính

### 👤 User Flow (Dành cho Người dùng)
- **Modern UI/UX:** Clean, responsive design with a professional **Royal Blue theme**.
- **Product Discovery:** Browse by categories, dynamic search, and smart filtering.
- **Cart & Checkout:** Full cart management with real-time tax and discount calculations.
- **Order Tracking:** Detailed order status and purchase history with product info.
- **Wishlist:** Save favorite products for later purchase.

### 🛡️ Admin Flow (Dành cho Quản trị viên)
- **Analytics Dashboard:** Visual representation of revenue, orders, and user growth.
- **Inventory Management:** Full CRUD (Create, Read, Update, Delete) for products and categories.
- **Order Processing:** Manage customer orders and update shipping statuses.
- **User Management:** Monitor registered users and assign roles (User/Admin).

---

## 🛠️ Tech Stack / Công nghệ sử dụng

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Backend** | Firebase (Auth, Firestore, Cloud Storage) |
| **Image Loading** | Coil |
| **Navigation** | Jetpack Compose Navigation |
| **Payment** | Razorpay & Mock Integration |

---

## 🏗️ Architecture / Kiến trúc dự án
Project này được xây dựng theo mô hình **MVVM**, đảm bảo tách biệt rõ ràng giữa logic và giao diện:
- **Model:** Định nghĩa dữ liệu (Product, Order, User).
- **View:** Các Composable components và Screens (Jetpack Compose).
- **ViewModel:** Xử lý logic nghiệp vụ và quản lý trạng thái UI (State Management).

---

## 🚀 Setup Instructions / Hướng dẫn cài đặt

1. **Clone the project:** 
   ```bash
   git clone https://github.com/your-username/EasyShop.git
   ```
2. **Firebase Configuration:**
   - Create a project on [Firebase Console](https://console.firebase.google.com).
   - Add your Android app (Package name: `com.example.easyshop`).
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Email/Password Auth**, **Firestore**, and **Storage**.
3. **Build & Run:** Open with Android Studio (Ladybug or newer) and sync Gradle.

> [!IMPORTANT]
> **Security Note:** The `google-services.json` file is excluded from this repository for security reasons. Users need to provide their own Firebase configuration to run the app.

---

## 📸 Screenshots / Hình ảnh minh họa
*(Coming Soon - You can add your screenshots here)*

---

## 🤝 Contact / Liên hệ
**Ngô Văn Dũng**
- 📧 Email: [ngovandung1552003@gmail.com](mailto:ngovandung1552003@gmail.com)
- 💼 LinkedIn: [Your Profile Link Here]
- 💻 My Work: [GitHub Portfolio]

---
*Created with ❤️ for Portfolio showcase.*
