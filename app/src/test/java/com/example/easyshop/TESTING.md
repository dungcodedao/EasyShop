# Hướng dẫn Kiểm thử (Testing Guide) - EasyShop

Tài liệu này hướng dẫn cách chạy các bộ Unit Test tự động để đảm bảo logic ứng dụng luôn chính xác.

## 1. Lệnh chạy Test quan trọng nhất

Để chạy **toàn bộ** các bài test trong dự án, hãy mở Terminal (PowerShell hoặc CMD) tại thư mục gốc và gõ:

```powershell
./gradlew :app:testDebugUnitTest
```

## 2. Chạy từng bộ Test riêng lẻ

Nếu bạn chỉ muốn kiểm tra một phần cụ thể:

*   **Test AI:**
    ```powershell
    ./gradlew :app:testDebugUnitTest --tests "com.example.easyshop.ai.repository.AiRepositoryTest"
    ```
*   **Test Logic Thanh toán (Checkout):**
    ```powershell
    ./gradlew :app:testDebugUnitTest --tests "com.example.easyshop.viewmodel.CheckoutViewModelTest"
    ```
*   **Test Tiện ích (Giá tiền, Lỗi...):**
    ```powershell
    ./gradlew :app:testDebugUnitTest --tests "com.example.easyshop.AppUtilTest"
    ```

## 3. Xem báo cáo kiểm thử trực quan (HTML)

Sau khi chạy lệnh test thành công, Android Studio sẽ sinh ra một file báo cáo đẹp mắt. Bạn có thể mở nó bằng trình duyệt theo đường dẫn:

`app/build/reports/tests/testDebugUnitTest/index.html`

## 4. Các lệnh hỗ trợ khác

*   **Lấy mã SHA-1 (Để fix lỗi Google Login):**
    ```powershell
    ./gradlew signingReport
    ```
*   **Dọn dẹp bản build cũ (Nếu gặp lỗi lạ):**
    ```powershell
    ./gradlew clean
    ```

---
*Ghi chú: Đảm bảo bạn đã cài đặt Java JDK 17+ và biến môi trường ANDROID_HOME đã được thiết lập.*
