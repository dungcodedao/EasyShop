# Chương 1: GIỚI THIỆU

## 1.1. Lý do chọn đề tài

Trong kỷ nguyên chuyển đổi số hiện nay, thương mại điện tử trên nền tảng di động (M-commerce) đang trải qua sự tăng trưởng bùng nổ, thay đổi hoàn toàn thói quen định hình hành vi mua sắm của người tiêu dùng. Cùng với đó, sự phát triển của Trí tuệ nhân tạo (AI), đặc biệt là các mô hình ngôn ngữ lớn (LLM), đã mở ra những hướng đi mới trong việc cá nhân hóa trải nghiệm khách hàng.

Tuy nhiên, nhiều ứng dụng mua sắm vừa và nhỏ vẫn gặp hạn chế về việc tối ưu hóa giao diện người dùng và thiếu vắng khả năng tương tác, tư vấn thông minh thời gian thực. Nhận thấy vấn đề đó, đề tài **"Xây dựng hệ thống thương mại điện tử đa nền tảng với Jetpack Compose và Trí tuệ nhân tạo"** (ứng dụng EasyShop) được lựa chọn. Đề tài hướng tới việc nghiên cứu và áp dụng các công nghệ lập trình Declarative UI tiên tiến kết hợp với Generative AI để tạo nên một nền tảng bán hàng thông minh, đáp ứng nhu cầu trải nghiệm cao của người tiêu dùng hiện đại.

## 1.2. Mục tiêu

Mục tiêu cốt lõi của đồ án là xây dựng và hoàn thiện một hệ thống phần mềm thương mại điện tử hoạt động trơn tru với các tiêu chí sau:

- **Về mặt kỹ thuật:** Nắm vững và áp dụng thành công kiến trúc MVVM, xây dựng giao diện hoàn toàn bằng Jetpack Compose thay cho XML truyền thống. Tích hợp cơ sở dữ liệu thời gian thực (Real-time Database).
- **Về mặt chức năng:** Hoàn thiện quy trình mua sắm khép kín từ khâu duyệt sản phẩm, thêm ngôn giỏ hàng, đến quản lý đơn hàng. Xây dựng phân hệ Quản trị (Admin) để quản lý hàng hóa và mã khuyến mãi.
- **Về mặt công nghệ AI:** Tích hợp mô hình AI tạo sinh làm "Trợ lý ảo", có khả năng truy xuất dữ liệu sản phẩm để tư vấn tự động 24/7 cho khách hàng.

## 1.3. Đối tượng & phạm vi

- **Đối tượng hướng tới:**
  - *Bộ phận khách hàng (User/Guest):* Những người có nhu cầu tìm kiếm, tham khảo cấu hình và mua sắm các thiết bị công nghệ.
  - *Bộ phận quản lý (Admin):* Ban quản trị cửa hàng cần công cụ quản lý tập trung đơn hàng, doanh thu và kho bãi.
- **Phạm vi ứng dụng:**
  - Đồ án tập trung xây dựng ứng dụng (Client) trên hệ điều hành Android.
  - Sử dụng dịch vụ backend tự động (BaaS - Backend as a Service) thay vì tự xây dựng máy chủ cục bộ.

## 1.4. Phương pháp thực hiện

Để đạt được các mục tiêu trên, đề tài sử dụng phương pháp phát triển linh hoạt với các giai đoạn cụ thể:

1. **Nghiên cứu tài liệu:** Tìm hiểu lý thuyết về thư viện UI Jetpack Compose, cơ chế lưu trữ NoSQL và cách thức gọi API trí tuệ nhân tạo.
2. **Khảo sát và Thiết kế:** Phân tích yêu cầu hệ thống thông qua mockups giao diện và hệ thống các biểu đồ chuẩn (UML, ERD).
3. **Triển khai lập trình:** Xây dựng khung kiến trúc MVVM, tiến hành code từng module chức năng (Authentication, Catalog, Cart, AI Assistant) bằng ngôn ngữ Kotlin.
4. **Kiểm thử và Hoàn thiện:** Sử dụng giả lập (Emulator) và thiết bị Android vật lý để debug, tối ưu hiệu năng và đánh giá lại so với thiết kế ban đầu.

## 1.5. Bố cục đồ án

Nội dung của báo cáo đồ án được kết cấu thành 5 chương chính:

- **Chương 1: Giới thiệu** - Trình bày ngữ cảnh, lý do, mục tiêu và phương pháp thực hiện đề tài.
- **Chương 2: Cơ sở lý thuyết & Công nghệ** - Tổng hợp các kiến thức nền tảng và công cụ được sử dụng để xây dựng ứng dụng.
- **Chương 3: Phân tích & Thiết kế hệ thống** - Đưa ra các biểu đồ đặc tả hệ thống và kiến trúc cơ sở dữ liệu.
- **Chương 4: Xây dựng & Triển khai** - Chi tiết hóa các module chức năng ứng dụng với giao diện và mã nguồn thực tế.
- **Chương 5: Đánh giá & Kết quả** - Đưa ra kết quả đạt được, chỉ ra những hạn chế và hướng phát triển trong tương lai.

## 1.6. Tổng quan nghiên cứu

Qua quá trình khảo sát các ứng dụng thương mại điện tử chuyên biệt (như các ứng dụng của đại lý máy tính, linh kiện điện tử quy mô vừa) và các mẫu ứng dụng bán hàng phổ biến trên Google Play Store, nhóm thực hiện nhận thấy một số hạn chế phổ biến mà người dùng thường gặp phải:

- **Hạn chế về trải nghiệm người dùng (UX/UI):** Nhiều ứng dụng hiện nay vẫn được xây dựng trên nền tảng công nghệ cũ (XML View truyền thống), dẫn đến hiện tượng giật lag khi xử lý danh mục sản phẩm lớn hoặc hiển thị hình ảnh chất lượng cao trên các thiết bị Android tầm trung. Đồng thời, giao diện thường mang tính rập khuôn, thiếu sự mượt mà trong các hiệu ứng chuyển cảnh.
- **Thiếu hụt hỗ trợ kỹ thuật thông minh:** Đối với các mặt hàng công nghệ, người dùng thường gặp khó khăn khi phải tự mình phân tích các thông số kỹ thuật phức tạp (về CPU, RAM, GPU...). Các ứng dụng hiện tại chủ yếu chỉ cung cấp bộ lọc (Filter) cơ bản, chưa có khả năng giải thích hoặc tư vấn sản phẩm dựa trên nhu cầu thực tế của từng khách hàng.
- **Hiệu năng và tối ưu hóa tài nguyên:** Một số ứng dụng không được tối ưu hóa về mặt kiến trúc (thiếu MVVM, quản lý state kém), gây tiêu tốn RAM và bộ nhớ đệm (cache) không cần thiết, làm giảm tuổi thọ trải nghiệm trên các dòng điện thoại không phải là flagship.

Nhận diện được những khoảng trống này, ứng dụng **EasyShop** được xây dựng nhằm mang đến một giải pháp hiện đại hơn: tận dụng sức mạnh của **Jetpack Compose** để tối ưu hóa hiệu năng giao diện và tích hợp **Trợ lý AI** để đóng vai trò như một chuyên gia tư vấn kỹ thuật trực tuyến, giúp thu hẹp khoảng cách giữa thông số máy móc và nhu cầu thực tế của người dùng.

## 1.7. Xuất xứ đề tài

Đề tài được hình thành và phát triển trong quá trình tác giả thực tập tại **Công ty Cổ phần Công nghệ và Dịch vụ ALOBASE**. Qua thời gian quan sát và trực tiếp tham gia vào các hoạt động tại doanh nghiệp, tôi nhận thấy:

- **Nhu cầu thực tế:** Công ty đang tìm kiếm những phương án đổi mới nhằm tối ưu hóa quy trình tương tác với khách hàng, đặc biệt là trong việc giới thiệu và giải đáp thắc mắc về các gói dịch vụ công nghệ phức tạp.
- **Yêu cầu từ doanh nghiệp:** Xuất phát từ yêu cầu thực tế của ALOBASE về việc ứng dụng các công nghệ tiên tiến như AI tạo sinh (Generative AI) vào sản phẩm thương mại để gia tăng lợi thế cạnh tranh và giảm tải cho bộ phận vận hành.

Chính vì vậy, được sự hướng dẫn và tạo điều kiện từ phía nhà trường cùng với sự hỗ trợ về môi trường thực nghiệm từ phía Công ty ALOBASE, tôi đã quyết định thực hiện đề tài xây dựng ứng dụng **EasyShop**. Dự án này không chỉ là kết quả của quá trình học tập lý thuyết mà còn là lời giải cho một bài toán thực tế mà doanh nghiệp đang đối mặt, nhằm tạo ra một hệ thống TMĐT hiện đại, thông minh và có tính thực tiễn cao.
