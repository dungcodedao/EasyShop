# EasyShop - Báo cáo Tiến độ & Cỗ máy Trạng thái (Session Summary)
**Ngày ghi nhận:** 10/04/2026

Báo cáo này tóm tắt các công việc quan trọng đã thực hiện để phục hồi dự án **EasyShop** và chuẩn bị cho hồ sơ bảo vệ Đồ án Tốt nghiệp.

---

## 1. Phục hồi Hệ thống (Build Integrity)
- **Tình trạng:** Đã khắc phục hoàn toàn các lỗi biên dịch (Compilation Errors).
- **Hành động:** 
    - Thực hiện `gradlew clean assembleDebug` thành công.
    - Sửa lỗi tham chiếu `timestamp` sang `createdAt` trong `NotificationViewModel`.
    - Fix các lỗi cú pháp trong `FavoritePage.kt` và `ShimmerEffect.kt`.
    - Thay thế icon Google bị thiếu bằng placeholder ký tự để tránh lỗi Resource Not Found.

## 2. Chuẩn hóa Luồng dữ liệu (State Management)
- **Kiến trúc:** Triển khai **ScreenState** (LOADING, SUCCESS, ERROR, EMPTY) trên toàn bộ dự án.
- **ViewModel:** Cập nhật `HomeViewModel`, `FavoriteViewModel`, `NotificationViewModel`... để quản lý trạng thái tải dữ liệu một cách chuyên nghiệp.
- **UI:** Tích hợp loading shimmer và xử lý trạng thái trống (Empty State) cho các trang quan trọng.

## 3. Hệ thống Sơ đồ Đồ án (UML & Documentation)
- **File lưu trữ:** `view_diagrams.html`
- **Các cập nhật mới nhất:**
    - **Sơ đồ Use Case:** Phân tách rõ 3 Actor (Guest, User, Admin).
    - **Sơ đồ ERD:** 
        - Bổ sung trường `specs` (CPU, RAM, GPU) cho bảng **Products** để AI có thể đọc thông số kỹ thuật.
        - Bổ sung cấu trúc hoàn chỉnh cho bảng **PromoCodes** (code, discount, expiryDate).
    - **Sơ đồ DFD & Sequence:** Chuẩn hóa luồng truy vấn thông số sản phẩm cho Chatbot AI.

## 4. Công việc đang thực hiện
- [ ] Kiểm tra hiển thị Banner trên Home Screen (Đang debug luồng lấy dữ liệu từ Firestore).
- [ ] Kiểm tra tính ổn định của các trang Admin sau khi refactor.

---
**Ghi chú:** Toàn bộ lịch sử này đã được lưu lại để bạn có thể xem lại bất cứ lúc nào trong quá trình làm báo cáo tốt nghiệp.
