# Chương 2: CƠ SỞ LÝ THUYẾT & CÔNG NGHỆ

## 2.1. Kiến thức nền

### Cơ sở dữ liệu NoSQL (Document-based Database)
Khác với mô hình Cơ sở dữ liệu quan hệ (RDBMS) sử dụng bảng, hàng và cột truyền thống, hệ thống TMĐT hiện đại đòi hỏi khả năng co giãn (scale) lớn và thay đổi cấu trúc dữ liệu linh hoạt. Document-based NoSQL (như Firestore) lưu trữ thông tin dưới dạng các Collection và Document. Sự linh hoạt trong schema cho phép dễ dàng lưu trữ các cấu trúc phức tạp (như danh sách địa chỉ, mảng đơn hàng) ngay trong một tài liệu (Embedded Pattern), giảm thiểu thao tác Join giúp tốc độ truy vấn tăng cao.

### Lập trình với Giao diện khai báo (Declarative UI)
Mô hình Imperative UI truyền thống yêu cầu lập trình viên phải thao tác trực tiếp lên từng View (như findViewByID) mỗi khi dữ liệu thay đổi để cập nhật trạng thái. Ngược lại, mô hình Declarative UI định nghĩa UI là "kết quả của một trạng thái - State". Lập trình viên chỉ cần mô tả giao diện nên trông như thế nào với những state cụ thể, hệ thống sẽ tự động vẽ lại (re-composition) chỉ những phần giao diện bị ảnh hưởng khi State thay đổi.

### Trí tuệ nhân tạo tạo sinh (Generative AI)
Generative AI là tập con của Deep Learning, chứa các mô hình ngôn ngữ lớn (LLM) được huấn luyện trên lượng dữ liệu khổng lồ. Ứng dụng tích hợp AI này không phải tự quy định từng câu trả lời rập khuôn (như chatbot theo kịch bản), mà sử dụng API để tự động hiểu dụng ý câu hỏi (Natural Language Understanding) và sinh ra văn bản trả lời dựa vào ngữ cảnh (Context) sản phẩm được cung cấp, hoạt động như một Trợ lý thực thụ.

## 2.2. Công nghệ sử dụng

### Ngôn ngữ lập trình: Kotlin
Kotlin là ngôn ngữ thống trị trên nền tảng lập trình Android hiện đại với sự ủng hộ chính thức từ Google. Trong đồ án này, các đặc tính cốt lõi của Kotlin được tận dụng triệt để:
- **Coroutines & Flow:** Được sử dụng để xử lý các logic bất đồng bộ (gọi mạng, truy vấn CSDL, API AI) một cách an toàn mà không làm nghẽn luồng chính (Main Thread), giúp ứng dụng cực kỳ mượt mà.

### Framework & Thư viện
- **Jetpack Compose:** Là bộ thư viện UI hiện đại hoàn toàn bằng Kotlin, đóng vai trò thay thế toàn bộ hệ thống XML cũ. Cung cấp hệ sinh thái component đa dạng đáp ứng tiêu chuẩn Material Design 3.
- **Kiến trúc MVVM:** Sử dụng lớp `ViewModel` bảo toàn dữ liệu trạng thái khi vòng đời của phần mềm thay đổi (chẳng hạn khi người dùng xoay màn hình), giúp tách bạch hoàn toàn giao diện và logic.
- **Firebase BaaS:** 
  - *Firebase Authentication:* Giải quyết bài toán bảo mật đăng nhập người dùng.
  - *Cloud Firestore:* Là trung tâm dữ liệu thời gian thực (Real-time). Khi admin thay đổi giá hoặc trạng thái đơn hàng, dữ liệu lập tức cập nhật (push-based) về điện thoại người mua.
- **Coil:** Thư viện tối ưu hóa hiệu năng chuyên biệt dành cho Compose, chịu trách nhiệm tải hình ảnh (Image Loading) bất đồng bộ và bộ nhớ đệm (Caching).
- **Google Generative AI SDK:** Giao tiếp trực tiếp với bộ não mô hình Gemini 2.5 Flash chuyên xử lý tác vụ tư vấn khách hàng.

### Nghiên cứu giải pháp Thanh toán số (Digital Payment Research)
Trong khuôn khổ đồ án, hệ thống tập trung nghiên cứu và thử nghiệm khả năng tích hợp các phương thức thanh toán hiện đại để minh chứng cho tính khả thi của mô hình:
- **VietQR Standard (Thử nghiệm):** Nghiên cứu ứng dụng tiêu chuẩn mã QR chung để sinh mã thanh toán động. Việc thử nghiệm này nhằm đánh giá khả năng tự động hóa việc điền nội dung giao dịch, giúp tối ưu hóa quy trình thanh toán trên thiết bị di động.
- **Cơ chế Webhook và Giải pháp SePay (Mô hình thực nghiệm):** Đồ án thực nghiệm việc kết nối bất đồng bộ giữa ứng dụng và hệ thống trung gian. Thông qua giải pháp SePay (phiên bản thử nghiệm), hệ thống mô phỏng quy trình "bắn" tín hiệu Webhook để cập nhật đơn hàng tự động, giúp chứng minh khả năng vận hành của một hệ thống TMĐT thông minh không cần đối soát thủ công.
- **Thanh toán tiền mặt (COD - Cash on Delivery):** Là phương thức truyền thống nhưng vẫn đóng vai trò quan trọng trong việc xây dựng niềm tin với khách hàng. Đồ án tích hợp phương thức này để đảm bảo tính thực tiễn và đáp ứng đa dạng thói quen mua sắm.

## 2.3. Công cụ phát triển
- **Môi trường:** Android Studio phiên bản mới nhất, hệ thống build nội bộ bằng Gradle (phiên bản KTS - Kotlin Script).
- **Thiết kế & Sơ đồ:** Sử dụng ngôn ngữ Mermaid để chuẩn hóa hệ thống biểu đồ kỹ thuật.
- **Quản lý phiên bản:** Hệ thống Git và kho lưu trữ mã nguồn.
