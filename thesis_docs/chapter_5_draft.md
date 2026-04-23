# Chương 5: ĐÁNH GIÁ & KẾT QUẢ

## 5.1. Kết quả đạt được
Sau quá trình thiết kế, phát triển và thử nghiệm, nhóm thực hiện đã hoàn thiện sản phẩm ứng dụng EasyShop đóng gói theo đúng cấu trúc đề ra. Các kết quả cụ thể chứng minh quá trình thực hiện bao gồm:
- **Ứng dụng di động:** Một ứng dụng Android thương mại điện tử hoạt động trơn tru với đầy đủ các phân hệ từ người dùng (Customer App) đến Quản trị viên (Admin App). Giao diện tối giản mang đậm phong cách Material Design 3.
- **Hạ tầng Dữ liệu:** Quản lý cơ sở phân tán trên nền tảng Cloud Firestore với độ trễ (latency) nội bộ rất thấp. Hệ thống vận hành tốt dựa trên kiến trúc Model-View-ViewModel.
- **Công nghệ mũi nhọn:** Đã tích hợp thành công Generative AI (Gemini) làm trợ lý ảo và hệ thống **Thanh toán tự động (VietQR/Webhook)**, giúp khép kín chu trình vận hành từ tư vấn đến chốt đơn.
- **Tính năng tương tác và Khuyến mãi:** Hoàn thiện hệ thống **Live Chat** thời gian thực giúp kết nối người mua và người bán, cùng hệ thống **Ví Voucher** thông minh giúp tối ưu hóa doanh số.
- **Quản trị thông minh:** Xây dựng hệ thống Dashboard thống kê cho phép Admin theo dõi biến động doanh thu và hiệu quả kinh doanh một cách trực quan.

## 5.2. So sánh với mục tiêu
Đối chiếu với các mục tiêu khởi tạo đã đặt ra trong Chương 1, đồ án hoàn thiện 100% các yêu cầu chức năng nghiệp vụ khép kín. Khả năng thiết lập giỏ hàng, thanh toán tự động, và AI Assistant đều đạt mức kỳ vọng cao về tính thực tiễn.

## 5.3. Đánh giá hiệu năng
- Ứng dụng đáp ứng tốt các nguyên tắc về Android App Architecture, xử lý vòng đời không bị lỗi Crash.
- Jetpack Compose giúp giao diện mượt mà, tốc độ phản hồi nhanh (FPS ổn định).
- Hệ thống AI phản hồi trong vòng 2-4 giây, dữ liệu Firestore cập nhật thời gian thực dưới `300ms`.

## 5.4. Hạn chế
Mặc dù đạt được những kết quả khả quan, hệ thống vẫn tồn tại một số điểm có thể cải thiện:
- **Môi trường thực nghiệm:** Do giới hạn về pháp lý và ngân sách cá nhân, hệ thống thanh toán tự động qua SePay hiện đang vận hành trong môi trường Sandbox/Test, cần đăng ký chính thức để triển khai thương mại thực tế.
- **Độ chính xác AI:** Trợ lý đôi khi vẫn có thể lệch ngữ cảnh (AI Hallucination) nếu người dùng nhập liệu quá phức tạp hoặc cố tình gây nhiễu thông tin.

## 5.5. Hướng phát triển
Những tiền đề của ứng dụng mở ra nhiều hướng phát triển tiềm năng mang tính thương mại sâu rộng trong tương lai:
- **Định hướng mở rộng:** Hoàn thiện tích hợp đa dạng Payment Gateways (VNPay, ZaloPay) để chính thức thương mại hóa. Nâng cấp Chat thành Video Call để tư vấn sản phẩm trực quan hơn.
- **Khả năng ứng dụng thực tế:** Áp dụng phương pháp công nghệ KMP (Kotlin Multiplatform) để phát triển thêm phiên bản iOS. Tích hợp học máy (Machine Learning) để cá nhân hóa đề xuất sản phẩm dựa trên hành vi chat và mua sắm của khách hàng.
