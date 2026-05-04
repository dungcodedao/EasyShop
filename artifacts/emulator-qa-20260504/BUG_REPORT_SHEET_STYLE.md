# Bug Report (Sheet Style)

| Mô tả | Status (Dev) | Test check | Steps | Kết quả thực tế | Kết quả mong đợi | Device | Hình ảnh | Dev note |
|---|---|---|---|---|---|---|---|---|
| Cold start jank cao trên màn hình đầu | To do | Đang làm | 1) Force-stop app 2) Mở lại app 3) Quan sát frame stats | 146 frames; 86 janky (58.90%); p50=18ms; p99=250ms | Janky frames < 10%, p99 frame time < 50ms | emulator-5554 | `artifacts/emulator-qa-20260504/screen-after-coldstart.png` | Ưu tiên Medium. Cần verify máy thật để loại nhiễu emulator |
| Google Play Services SecurityException khi khởi động | To do | Đang làm | 1) Xóa logcat 2) Khởi động app 3) Đọc logcat | Lặp lỗi: SecurityException Unknown calling package name com.google.android.gms | Không có SecurityException liên quan GMS trong startup flow | emulator-5554 | `artifacts/emulator-qa-20260504/logcat-coldstart.txt` | Ưu tiên Medium-High nếu ảnh hưởng feature đăng nhập/map/push |
| UI dump qua exec-out thất bại | To do | Done | 1) Chạy uiautomator dump qua exec-out | Lỗi: null root node returned by UiTestAutomationBridge | UI dump thành công qua adb exec-out | emulator-5554 | `artifacts/emulator-qa-20260504/ui-state-initial.xml` | Đang dùng workaround dump qua /sdcard (`ui-state.xml`) |
| Crash app trong phiên test | Done | Done | 1) Capture crash buffer sau cold start | `logcat-crash.txt` rỗng (0 bytes), chưa thấy crash | Không có crash | emulator-5554 | `artifacts/emulator-qa-20260504/logcat-crash.txt` | Trạng thái pass |
