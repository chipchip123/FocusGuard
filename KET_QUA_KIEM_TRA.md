# Kết quả kiểm tra FocusGuard

Ngày kiểm tra: 20/10/2026. Đây là kết quả của mã nguồn phục dựng hiện tại.

## Biên dịch và kiểm tra tự động

- `assembleDebug testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
- 12 bài unit test đạt, 0 thất bại, 0 lỗi. Trong đó có 11 bài kiểm tra logic FocusGuard và 1 bài mẫu Android Studio.
- Logic được kiểm tra: chặn đúng tại ngưỡng, hết thưởng khóa lại, tắt bảo vệ không khóa, thưởng không vượt nửa đêm, chống đếm lặp chuyển động, 4 đáp án khác nhau, tính thời gian qua nửa đêm/chuyển ứng dụng/tắt màn hình.
- Android Lint: 0 lỗi, 19 cảnh báo. Chủ yếu là gợi ý dùng KTX, tài nguyên màu mẫu chưa sử dụng, thông báo có phiên bản thư viện mới hơn và thuộc tính accessibility chỉ áp dụng trên API mới.
- APK debug: 12.669.276 byte.
- SHA-256 APK: `e0dc6721413bd212f5d60f6130b801ac3c04b85be0bc9a99984c3aa87fa15707`.
- CSV: 322 từ tiếng Anh duy nhất, tất cả dòng có đủ 3 cột.
- ZIP mã nguồn đã kiểm tra tính toàn vẹn. Không chứa cache build, local.properties hoặc báo cáo Word của người dùng.

## Đã chạy trên máy ảo Android API 37

- Cài APK, mở giao diện Tổng quan, Ứng dụng; chọn ứng dụng và xác nhận cấu hình tự lưu.
- Cấp quyền trong máy ảo và xác nhận Accessibility Service được Android kết nối. Dịch vụ khai báo khả năng đọc nội dung bằng 0.
- Thiết lập cấu hình thử nghiệm Chrome 5 phút, bật bảo vệ. Giữ Chrome ở foreground đến hết giới hạn: màn hình khóa tự xuất hiện khi không chuyển ứng dụng.
- Mở thử thách trắc nghiệm từ màn hình khóa.
- Trả lời sai một câu: có thông báo chuỗi đúng về 0.
- Trả lời đúng liên tiếp 5 câu: hiện màn hình hoàn thành và +5 phút.
- Kiểm tra SharedPreferences: thưởng được lưu cho `com.android.chrome`, thời điểm hết hạn khoảng 5 phút từ khi hoàn thành.
- Nhấn tiếp tục: trở lại Chrome, màn hình khóa không xuất hiện trong lần kiểm tra ngay sau thưởng.
- Không thấy crash của FocusGuard trong log AndroidRuntime ở các bước trên.

## Cần kiểm tra thêm trên điện thoại thật

- Cảm biến gia tốc và trải nghiệm lắc điện thoại 30 lần. Unit test kiểm tra bộ đếm nhưng không thay thế cảm biến thật.
- Hành vi quyền Overlay/Accessibility, quản trị thiết bị và chế độ nghiêm ngặt trên từng hãng máy.
- Tiết kiệm pin, khởi động lại, đi qua 00:00, chuyển múi giờ, chia đôi màn hình/PiP.
- Khoảng thưởng ở chế độ thường trên nhiều thiết bị; Demo đã chờ đủ 5 phút và xác nhận khóa lại trên emulator.
- Android 7–16 chưa có thiết bị kiểm tra; mã được biên dịch với Min SDK 24 nhưng chạy thực tế mới xác nhận trên API 37.

Các mục chưa kiểm tra không được ghi “Đạt” trong báo cáo thực nghiệm.


## Bổ sung kiểm tra Demo

- Build, 12 unit test và 5 bài test Android trên emulator API 37 đều đạt. Trong đó có 1 unit test và 1 bài Android mẫu.
- Android Lint: 0 lỗi, 19 cảnh báo.
- Đã thử giao diện bật Demo cho Chrome ở mức khoảng 9/30 phút, khóa ngay, trắc nghiệm sai rồi đúng 5 câu, lưu thưởng đúng gói và trở lại Chrome.
- Đã chờ hết toàn bộ khoảng thưởng 5 phút trong Demo, xác nhận tự hiện khóa lại.
- Đã thử kết thúc Demo, khôi phục bảo vệ và giữ giới hạn 30 phút.
- Chưa thực nghiệm app TikTok/Facebook chính thức trên điện thoại thật; máy ảo dùng Chrome để kiểm tra cơ chế chung. Chưa thử cảm biến lắc thật, tất cả hãng máy/phiên bản Android, khởi động lại và chuyển qua 00:00 thực tế.

Không nên nói “đã test hết mọi trường hợp”. Khi đi thi nên dùng trắc nghiệm cho luồng demo đã được kiểm chứng; chỉ trình diễn lắc sau khi bạn đã thử cảm biến trên máy thật.
