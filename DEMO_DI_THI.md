# Demo FocusGuard khi đi thi

## Chuẩn bị trước buổi thi

1. Cài APK mới hoặc Run project từ Android Studio lên điện thoại sẽ dùng để thi.
2. Cài TikTok hoặc Facebook bản chính thức trên máy. Lần mở đầu FocusGuard tự chọn các bản đã cài với giới hạn 30 phút/ngày. Nếu cài mạng xã hội sau đó, nút Demo cũng tự tìm và thêm vào danh sách.
3. Mở FocusGuard → Cài đặt, cấp đủ 3 quyền: thời gian sử dụng, hiển thị trên ứng dụng khác, hỗ trợ tiếp cận. Chỉ cần chuẩn bị quyền trước buổi thi; Android không cho ứng dụng tự cấp các quyền này.
4. Thử trọn một lượt trên điện thoại thật trước khi đi thi. Cho phép chạy nền nếu máy có tiết kiệm pin mạnh. Không cần bật quản trị thiết bị hoặc chế độ nghiêm ngặt để demo.

## Trình diễn trước giảng viên

1. Vào Tổng quan, cuộn tới **Demo khi thuyết trình**.
2. Bấm **Demo TikTok / Facebook** → **Bắt đầu demo**. Ứng dụng tự bật bảo vệ, chọn các gói Facebook/TikTok đã cài và xóa thưởng còn lại của chúng.
3. Ra màn hình chính, mở TikTok/Facebook: màn hình khóa hiện ngay dù chưa dùng đủ 30 phút. Trên màn hình có chữ DEMO; thời gian dùng thật không bị sửa.
4. Chọn **Học từ vựng · Đúng 5 câu**. Có thể trả lời sai để minh họa chuỗi về 0, rồi trả lời đúng liên tiếp 5 câu.
5. Bấm **Tiếp tục dùng ứng dụng**: ứng dụng đích mở lại trong khoảng thưởng 5 phút theo đồng hồ. Hết thưởng sẽ bị khóa lại khi đang ở app đó.
6. Muốn trình diễn lần nữa ngay: về FocusGuard → **Khóa lại để demo** → **Bắt đầu demo**, rồi mở lại mạng xã hội. Không cần chờ hết thưởng.
7. Kết thúc: bấm **Kết thúc demo**. Giới hạn 30 phút/ngày và cấu hình bảo vệ trước khi demo được áp dụng lại. Demo tự hết hiệu lực khi sang ngày mới.

Nếu máy không có TikTok/Facebook: chọn Chrome hoặc một ứng dụng trong mục Ứng dụng rồi bấm **Demo ứng dụng đã chọn**. Không chọn ứng dụng đang chứa bài trình chiếu của bạn.

## Phạm vi đã kiểm tra

- Build, 12 unit test và 5 bài test Android trên emulator API 37 đều đạt. Trong đó có 1 unit test và 1 bài Android mẫu.
- Android Lint: 0 lỗi, 19 cảnh báo.
- Đã thử giao diện bật Demo cho Chrome ở mức khoảng 9/30 phút, khóa ngay, trắc nghiệm sai rồi đúng 5 câu, lưu thưởng đúng gói và trở lại Chrome.
- Đã chờ hết toàn bộ khoảng thưởng 5 phút trong Demo, xác nhận tự hiện khóa lại.
- Đã thử kết thúc Demo, khôi phục bảo vệ và giữ giới hạn 30 phút.
- Chưa thực nghiệm app TikTok/Facebook chính thức trên điện thoại thật; máy ảo dùng Chrome để kiểm tra cơ chế chung. Chưa thử cảm biến lắc thật, tất cả hãng máy/phiên bản Android, khởi động lại và chuyển qua 00:00 thực tế.

Không nên nói “đã test hết mọi trường hợp”. Khi đi thi nên dùng trắc nghiệm cho luồng demo đã được kiểm chứng; chỉ trình diễn lắc sau khi bạn đã thử cảm biến trên máy thật.
