# 📊 Phân Tích Dự Án EasyShop

## Tổng Quan

**EasyShop** là ứng dụng Android thương mại điện tử (e-commerce) được xây dựng bằng **Kotlin + Jetpack Compose**, sử dụng **Firebase** làm backend. Dự án có kiến trúc **MVVM** (Model-View-ViewModel) rõ ràng.

- **Repository**: https://github.com/dungcodedao/EasyShop.git
- **Package**: `com.example.easyshop`
- **Min SDK**: Android

---

## Cấu Trúc Thư Mục

```
EasyShop/
├── app/src/main/java/com/example/easyshop/
│   ├── MainActivity.kt              # Entry point
│   ├── Navigation.kt                # Điều hướng (Bottom Nav + NavHost)
│   ├── FCMService.kt               # Firebase Cloud Messaging Service
│   ├── model/                       # Data models (9 class)
│   │   ├── Product.kt               # Sản phẩm
│   │   ├── Order.kt                 # Đơn hàng
│   │   ├── CartItem.kt              # Giỏ hàng
│   │   ├── Category.kt              # Danh mục
│   │   ├── User.kt                  # Người dùng
│   │   ├── Address.kt               # Địa chỉ
│   │   ├── Voucher.kt               # Mã giảm giá
│   │   ├── Review.kt                # Đánh giá
│   │   ├── Banner.kt                # Banner quảng cáo
│   │   ├── ChatMessage.kt           # Tin nhắn chat
│   │   └── Notification.kt          # Thông báo
│   ├── viewmodel/                   # ViewModels (13 class)
│   │   ├── AuthViewModel.kt         # Xác thực
│   │   ├── HomeViewModel.kt         # Trang chủ
│   │   ├── ProductViewModel.kt      # Sản phẩm
│   │   ├── CartViewModel.kt         # Giỏ hàng
│   │   ├── CheckoutViewModel.kt     # Thanh toán
│   │   ├── OrderViewModel.kt        # Đơn hàng
│   │   ├── FavoriteViewModel.kt     # Yêu thích
│   │   ├── ProfileViewModel.kt      # Hồ sơ
│   │   ├── AddressViewModel.kt      # Địa chỉ
│   │   ├── SearchViewModel.kt       # Tìm kiếm
│   │   ├── CategoryViewModel.kt     # Danh mục
│   │   ├── ChatViewModel.kt         # Chat
│   │   ├── NotificationViewModel.kt # Thông báo
│   │   ├── VoucherViewModel.kt      # Voucher
│   │   ├── ReviewViewModel.kt       # Đánh giá
│   │   └── AdminViewModel.kt        # Quản trị
│   ├── screens/                     # UI Screens
│   │   ├── auth/                    # Đăng nhập, Đăng ký
│   │   │   ├── LoginScreen.kt
│   │   │   └── RegisterScreen.kt
│   │   ├── home/                    # Trang chủ
│   │   │   └── HomeScreen.kt
│   │   ├── product/                 # Chi tiết sản phẩm
│   │   │   └── ProductDetailScreen.kt
│   │   ├── category/                # Danh mục
│   │   │   └── CategoryScreen.kt
│   │   ├── search/                  # Tìm kiếm
│   │   │   └── SearchScreen.kt
│   │   ├── cart/                    # Giỏ hàng
│   │   │   └── CartScreen.kt
│   │   ├── checkout/                # Thanh toán
│   │   │   └── CheckoutScreen.kt
│   │   ├── order/                   # Đơn hàng
│   │   │   ├── OrderScreen.kt
│   │   │   └── OrderDetailScreen.kt
│   │   ├── favorite/                # Yêu thích
│   │   │   └── FavoriteScreen.kt
│   │   ├── profile/                 # Hồ sơ
│   │   │   ├── ProfileScreen.kt
│   │   │   ├── EditProfileScreen.kt
│   │   │   └── ChangePasswordScreen.kt
│   │   ├── address/                 # Địa chỉ
│   │   │   └── AddressScreen.kt
│   │   ├── chat/                    # Chat
│   │   │   └── ChatScreen.kt
│   │   ├── notification/            # Thông báo
│   │   │   └── NotificationScreen.kt
│   │   ├── voucher/                 # Voucher
│   │   │   └── VoucherScreen.kt
│   │   ├── review/                  # Đánh giá
│   │   │   └── ReviewScreen.kt
│   │   └── admin/                   # Admin Panel
│   │       ├── AdminScreen.kt           # Dashboard chính
│   │       ├── AdminDashboardScreen.kt  # Tổng quan thống kê
│   │       ├── AdminProductScreen.kt    # Quản lý sản phẩm
│   │       ├── AddEditProductScreen.kt  # Thêm/Sửa sản phẩm
│   │       ├── AdminCategoryScreen.kt   # Quản lý danh mục
│   │       ├── AdminOrderScreen.kt      # Quản lý đơn hàng
│   │       ├── AdminUserScreen.kt       # Quản lý người dùng
│   │       ├── AdminBannerScreen.kt     # Quản lý banner
│   │       ├── AdminVoucherScreen.kt    # Quản lý voucher
│   │       ├── AdminReviewScreen.kt     # Quản lý đánh giá
│   │       ├── AdminChatScreen.kt       # Quản lý chat
│   │       ├── AdminNotificationScreen.kt # Quản lý thông báo
│   │       └── AdminStatisticScreen.kt  # Thống kê doanh thu
│   ├── ui/theme/                    # Material Theme
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── utils/                       # Utilities
│       ├── NotificationHelper.kt    # Helper hiển thị notification
│       └── FCMTokenManager.kt       # Quản lý FCM token
├── app/src/main/res/                # Resources
│   ├── values/                      # strings.xml, colors.xml, themes.xml
│   ├── drawable/                    # Icons, hình ảnh
│   └── mipmap-*/                    # App icons
├── functions/                       # Firebase Cloud Functions (Node.js)
│   ├── index.js                     # Cloud Functions logic
│   └── package.json                 # Dependencies
├── gradle/                          # Build configuration
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/                     # Gradle wrapper
├── build.gradle.kts                 # Root build file
├── app/build.gradle.kts             # App build file
└── settings.gradle.kts              # Project settings
```

---

## Các Chức Năng Chính

### 1. 🔐 Xác Thực Người Dùng
- **Đăng ký** tài khoản mới bằng Email & Mật khẩu (Firebase Auth)
- **Đăng nhập** bằng Email & Mật khẩu
- **Quên mật khẩu** (gửi email reset qua Firebase)
- **Đổi mật khẩu** cho người dùng đã đăng nhập
- **Phân quyền**: Người dùng thường (`user`) vs Quản trị viên (`admin`)

### 2. 🏠 Trang Chủ & Duyệt Sản Phẩm
- Hiển thị **Banner quảng cáo** (slider tự động từ Firestore)
- **Danh mục sản phẩm** hiển thị dạng lưới (Grid)
- **Sản phẩm nổi bật / mới nhất**
- **Tìm kiếm sản phẩm** theo tên với kết quả real-time
- **Lọc sản phẩm theo danh mục**

### 3. 📦 Chi Tiết Sản Phẩm
- Xem thông tin chi tiết: giá, mô tả, hình ảnh, tồn kho
- Hiển thị **đánh giá & bình luận** từ người dùng khác
- **Đánh giá sao** (1-5 sao)
- **Thêm vào giỏ hàng** với số lượng tùy chọn
- **Thêm vào danh sách yêu thích**

### 4. 🛒 Giỏ Hàng
- **Thêm / Xóa / Cập nhật số lượng** sản phẩm trong giỏ
- Hiển thị **tổng tiền** tự động tính toán
- **Áp dụng mã giảm giá (Voucher)** để giảm giá
- Kiểm tra tồn kho trước khi thanh toán

### 5. 💳 Thanh Toán (Checkout)
- Chọn **địa chỉ giao hàng** từ danh sách địa chỉ đã lưu
- Xem lại **tóm tắt đơn hàng** (sản phẩm, số lượng, tổng tiền)
- **Xác nhận đặt hàng** → tạo đơn hàng trên Firestore
- Trừ tồn kho sản phẩm

### 6. 📋 Quản Lý Đơn Hàng
- Xem **lịch sử đơn hàng** của người dùng
- Xem **chi tiết đơn hàng**: danh sách sản phẩm, địa chỉ giao hàng, tổng tiền, trạng thái
- Theo dõi **trạng thái đơn hàng**:
  - `Chờ xử lý` (Pending)
  - `Đang xử lý` (Processing)
  - `Đang giao` (Shipping)
  - `Đã giao` (Delivered)
  - `Đã hủy` (Cancelled)

### 7. ❤️ Sản Phẩm Yêu thích
- **Thêm / Xóa** sản phẩm khỏi danh sách yêu thích
- Hiển thị **danh sách sản phẩm đã lưu** dạng lưới
- Truy cập nhanh từ tab riêng trong Bottom Navigation

### 8. 👤 Hồ Sơ Người Dùng
- Xem **thông tin cá nhân** (tên, email, ảnh đại diện)
- **Chỉnh sửa hồ sơ** (tên, ảnh đại diện - upload lên Firebase Storage)
- **Đổi mật khẩu**
- **Đăng xuất**

### 9. 📍 Quản Lý Địa Chỉ
- **Thêm / Sửa / Xóa** nhiều địa chỉ giao hàng
- Đặt **địa chỉ mặc định**
- Thông tin bao gồm: tên, số điện thoại, địa chỉ chi tiết
- Sử dụng khi đặt hàng tại Checkout

### 10. 💬 Chat (Tin Nhắn)
- **Chat trực tiếp với Admin** thông qua Firestore
- Lưu **lịch sử tin nhắn** real-time
- Hiển thị thời gian gửi tin nhắn
- Admin có thể trả lời từ panel quản trị

### 11. 🔔 Thông Báo (Push Notification)
- Nhận **thông báo đẩy** qua Firebase Cloud Messaging (FCM)
- Quản lý & hiển thị **lịch sử thông báo** trong app
- Các loại thông báo:
  - Thông báo khi **trạng thái đơn hàng thay đổi**
  - Thông báo khi **có tin nhắn mới**
  - Thông báo **khuyến mãi / quảng cáo** từ Admin
- Cloud Functions tự động gửi notification khi có sự thay đổi

### 12. 🎫 Mã Giảm Giá (Voucher)
- Người dùng **xem danh sách voucher** có sẵn
- **Áp dụng voucher** khi thanh toán (giảm % theo giá trị đơn hàng)
- Admin tạo voucher với: mã, % giảm, điều kiện tối thiểu, ngày hết hạn

### 13. ⭐ Đánh Giá Sản Phẩm
- Người dùng **đánh giá sản phẩm đã mua** (1-5 sao + bình luận)
- **Xem đánh giá** từ người khác trên trang chi tiết sản phẩm
- Admin quản lý đánh giá (xem & xóa đánh giá không phù hợp)

### 14. 👨‍💻 Quản Trị Viên (Admin Panel)

Admin có quyền truy cập panel quản trị với các chức năng:

| Chức năng | Mô tả |
|---|---|
| **Dashboard** | Tổng quan thống kê (tổng doanh thu, đơn hàng, sản phẩm, người dùng) |
| **Quản lý sản phẩm** | Thêm / Sửa / Xóa sản phẩm (upload ảnh lên Firebase Storage) |
| **Quản lý danh mục** | Thêm / Sửa / Xóa danh mục sản phẩm |
| **Quản lý đơn hàng** | Xem danh sách & Cập nhật trạng thái đơn hàng |
| **Quản lý người dùng** | Xem danh sách người dùng, phân quyền admin/user |
| **Quản lý Banner** | Thêm / Sửa / Xóa banner quảng cáo trên trang chủ |
| **Quản lý Voucher** | Tạo / Chỉnh sửa / Xóa mã giảm giá |
| **Quản lý đánh giá** | Xem & Xóa đánh giá không phù hợp |
| **Quản lý Chat** | Trả lời tin nhắn từ người dùng |
| **Quản lý Thông báo** | Gửi thông báo push đến người dùng |
| **Thống kê** | Biểu đồ doanh thu theo thời gian |

---

## Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| **Ngôn ngữ** | Kotlin |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Kiến trúc** | MVVM (Model-View-ViewModel) |
| **Backend** | Firebase |
| **Database** | Cloud Firestore (NoSQL) |
| **Authentication** | Firebase Auth (Email/Password) |
| **Storage** | Firebase Storage (hình ảnh sản phẩm, avatar) |
| **Push Notification** | Firebase Cloud Messaging (FCM) |
| **Server Logic** | Node.js Cloud Functions (tự động gửi notification) |
| **Navigation** | Jetpack Navigation Compose |
| **State Management** | Kotlin StateFlow + LiveData |
| **Image Loading** | Coil (Compose) |
| **Dependency Injection** | Manual (ViewModelProvider) |

---

## Luồng Hoạt Động Chính

```
┌─────────────────────────────────────────────────────────┐
│                    NGƯỜI DÙNG                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Đăng nhập/Đăng ký ──→ Trang chủ (Banner + SP)         │
│                              │                          │
│                    ┌─────────┼─────────┐                │
│                    ▼         ▼         ▼                │
│              Danh mục   Tìm kiếm   Chi tiết SP          │
│                    │                   │                │
│                    └─────────┬─────────┘                │
│                              ▼                          │
│                         Giỏ hàng                        │
│                              │                          │
│                              ▼                          │
│                    Chọn địa chỉ giao hàng               │
│                              │                          │
│                              ▼                          │
│                    Xác nhận thanh toán                   │
│                              │                          │
│                              ▼                          │
│                    Tạo đơn hàng (Firestore)              │
│                              │                          │
│                              ▼                          │
│                    Theo dõi trạng thái                   │
│                              │                          │
│                    ┌─────────┼─────────┐                │
│                    ▼         ▼         ▼                │
│              Đánh giá    Chat Admin   Thông báo          │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    QUẢN TRỊ VIÊN                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Dashboard ──→ Quản lý toàn bộ hệ thống                │
│     │                                                   │
│     ├── Sản phẩm (CRUD)                                │
│     ├── Danh mục (CRUD)                                │
│     ├── Đơn hàng (xem + cập nhật trạng thái)           │
│     ├── Người dùng (xem + phân quyền)                  │
│     ├── Banner (CRUD)                                  │
│     ├── Voucher (CRUD)                                 │
│     ├── Đánh giá (xem + xóa)                          │
│     ├── Chat (trả lời người dùng)                      │
│     ├── Thông báo (gửi push notification)              │
│     └── Thống kê (biểu đồ doanh thu)                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Firebase Collections (Firestore)

| Collection | Mô tả |
|---|---|
| `users` | Thông tin người dùng (tên, email, role, avatar) |
| `products` | Sản phẩm (tên, giá, mô tả, hình ảnh, tồn kho, danh mục) |
| `categories` | Danh mục sản phẩm |
| `carts/{userId}/items` | Giỏ hàng của từng người dùng |
| `orders` | Đơn hàng (sản phẩm, địa chỉ, trạng thái, tổng tiền) |
| `addresses/{userId}/list` | Địa chỉ giao hàng |
| `favorites/{userId}/items` | Sản phẩm yêu thích |
| `reviews` | Đánh giá sản phẩm |
| `banners` | Banner quảng cáo |
| `vouchers` | Mã giảm giá |
| `chats` | Tin nhắn chat |
| `notifications` | Thông báo |

---

## Tổng Kết

EasyShop là một ứng dụng **e-commerce hoàn chỉnh** với đầy đủ tính năng:

- **13+ chức năng chính** từ mua sắm, đặt hàng, thanh toán đến chat và thông báo
- **Admin Panel** mạnh mẽ với khả năng quản lý toàn bộ hệ thống
- **Kiến trúc MVVM** sạch sẽ, dễ bảo trì và mở rộng
- Sử dụng **toàn bộ hệ sinh thái Firebase** giúp giảm thiểu việc tự xây dựng backend
- **Push Notification** tự động thông qua Cloud Functions
- **Real-time updates** cho chat và thông báo

### Điểm mạnh:
- ✅ Code organized theo kiến trúc MVVM rõ ràng
- ✅ Sử dụng Jetpack Compose (modern UI toolkit)
- ✅ Tích hợp đầy đủ Firebase services
- ✅ Phân quyền Admin/User
- ✅ Push Notification tự động

### Có thể cải thiện:
- 🔧 Thêm Dependency Injection (Hilt/Dagger)
- 🔧 Thêm unit tests & UI tests
- 🔧 Thêm error handling & loading states nhất quán hơn
- 🔧 Hỗ trợ thanh toán online (VNPay, MoMo...)
- 🔧 Thêm đa ngôn ngữ (i18n)
- 🔧 Offline support với Room database