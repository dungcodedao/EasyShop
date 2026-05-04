# Emulator QA Summary (2026-05-04)

## 1) Kết luận ngắn
- Reproduce thành công flow mở app `com.example.easyshop` trên `emulator-5554`.
- Không thấy crash app trong phiên capture (`logcat-crash.txt` rỗng).
- Có lỗi môi trường lặp lại từ Google Play Services: `SecurityException: Unknown calling package name 'com.google.android.gms'`.

## 2) Bảng thống kê artifact

| Artifact | Mục đích | Kết quả chính | Issue/Ghi chú |
|---|---|---|---|
| `screen-initial.png` | Ảnh trạng thái ban đầu | Capture thành công (1,076,996 bytes) | Mở bằng Image Preview, không mở text |
| `screen-after-coldstart.png` | Ảnh sau cold start | Capture thành công (688,784 bytes) | File hợp lệ, trước đó chỉ mở sai mode |
| `ui-state-initial.xml` | UI dump ban đầu qua `exec-out` | Thất bại, chỉ 120 bytes | `ERROR: null root node returned by UiTestAutomationBridge` |
| `ui-state.xml` | UI state fallback qua `/sdcard` | Thành công (19,443 bytes) | Dùng file này để phân tích UI |
| `logcat-coldstart.txt` | Log runtime cold start | Có `SecurityException` liên quan `com.google.android.gms` | Chưa thấy `FATAL EXCEPTION` của app |
| `logcat-crash.txt` | Buffer crash | Rỗng (0 bytes) | Không ghi nhận crash trong phiên này |
| `gfxinfo.txt` | Snapshot frame/jank | 146 frames, 86 janky (58.90%), p50=18ms, p99=250ms | Có dấu hiệu jank trên emulator |
| `gfxinfo-framestats.txt` | Framestats chi tiết | Capture thành công (58,834 bytes) | Dùng khi cần phân tích sâu frame timeline |
| `meminfo.txt` | Snapshot bộ nhớ | TOTAL PSS=181,816 KB, Java Heap=15,568 KB, Native Heap=12,760 KB | Ảnh chụp 1 thời điểm, chưa đủ kết luận leak |

## 3) Lưu ý/rủi ro
- Perfetto trace chưa thu được do binary `perfetto` trên emulator không hỗ trợ cờ mới (`--background-wait`, `--txt`).
- Số liệu jank trên emulator thường nhiễu; nên đối chiếu thêm trên máy thật nếu cần kết luận hiệu năng production.
