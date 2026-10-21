# FocusGuard

Ứng dụng Android hỗ trợ giới hạn thời gian dùng ứng dụng, viết bằng Kotlin và Jetpack Compose. Mã được phục dựng từ yêu cầu trong báo cáo, không phải mã nguồn gốc đã mất.

## Chạy trong Android Studio

1. Mở thư mục FocusGuard, chờ Gradle Sync.
2. Chọn JDK đi kèm Android Studio nếu IDE yêu cầu. Giữ các phiên bản công cụ của project mới: AGP 9.4.1, Gradle 9.6.0, Kotlin Compose plugin 2.2.10, Compile/Target SDK 37, Min SDK 24.
3. Chọn điện thoại hoặc emulator rồi nhấn Run.
4. Trong ứng dụng, vào Cài đặt và cấp lần lượt quyền thời gian sử dụng, hiển thị trên ứng dụng khác, dịch vụ hỗ trợ tiếp cận FocusGuard.
5. Vào Ứng dụng, chọn ứng dụng cần quản lý, chỉnh giới hạn 5–180 phút. Thay đổi được lưu tự động.
6. Bật Bảo vệ thời gian trong Tổng quan.

Nếu cài APK trực tiếp và Android chặn dịch vụ hỗ trợ tiếp cận: mở Thông tin ứng dụng của FocusGuard → menu ⋮ → Cho phép cài đặt bị hạn chế, sau đó quay lại cấp quyền. Tùy phiên bản Android, tên và vị trí mục có thể khác. Không cần cấp quyền Internet.

## Chức năng

- Giao diện tiếng Việt gồm Tổng quan, Ứng dụng, Thống kê và Cài đặt.
- Liệt kê ứng dụng có màn hình khởi chạy. Loại FocusGuard, launcher, Cài đặt và bộ quản lý quyền khỏi danh sách để tránh khóa các thành phần thiết yếu.
- Tự chọn Facebook/TikTok đang cài trong lần chạy đầu; không thêm ứng dụng chưa cài. Giới hạn mặc định 30 phút/ngày.
- Cấu hình SharedPreferences/JSON lưu trên máy, tự lưu khi thay đổi; không cần máy chủ.
- UsageEvents được ghép thành khoảng sử dụng trong khung giờ 00:00 đến hiện tại. Thống kê 7 ngày lấy dữ liệu lịch sử còn được Android lưu và áp dụng danh sách ứng dụng hiện tại.
- Accessibility Service chỉ nhận sự kiện tên ứng dụng, không đọc cây nội dung màn hình. Kiểm tra lặp mỗi giây; làm mới thời gian Android khoảng mỗi 2 giây trên luồng nền. Giới hạn vẫn được kiểm tra khi ở nguyên trong một ứng dụng.
- Màn hình khóa dùng WindowManager Overlay. Có nút về màn hình chính và hai thử thách mở khóa.
- Lắc điện thoại 30 lần: ngưỡng cao 1,25g, ngưỡng trở về 1,08g, tối thiểu 250ms giữa hai lần. Đây là đếm chuyển động thiết bị, không xác nhận số bước chạy thực tế.
- Bộ CSV có 322 từ vựng Anh–Việt viết mới. Mỗi từ có thể dùng theo hai chiều, với 4 đáp án khác nhau. Không có bộ dữ liệu 1.400 câu gốc. Trả lời sai đặt lại chuỗi đúng; đúng liên tiếp 5 câu sẽ hoàn thành.
- Thưởng 5 phút theo đồng hồ cho đúng ứng dụng đang khóa. Khoảng thưởng bắt đầu khi hoàn thành thử thách, vẫn trôi nếu chuyển ứng dụng hoặc tắt màn hình; kết thúc chậm nhất lúc nửa đêm. Không cộng dồn vào ngân sách hằng ngày.
- Giới hạn hằng ngày và thưởng tự chuyển sang ngày mới bằng kiểm tra ngày khi đọc dữ liệu và trong vòng lặp dịch vụ; không yêu cầu báo thức chính xác hay AlarmManager.
- Chế độ nghiêm ngặt ngăn thay đổi giới hạn và bật/tắt bảo vệ trong giao diện; có lời nhắc khi mở Cài đặt Android. Luôn có nút tạm mở Cài đặt 60 giây hoặc tắt chế độ. Đây là công cụ tự quản lý, có thể vượt qua và không chống gỡ tuyệt đối.
- Quản trị thiết bị là tùy chọn, được người dùng bật/tắt qua giao diện Android. Không yêu cầu quyền khóa/xóa thiết bị. Để gỡ ứng dụng khi đã bật quản trị, tắt quản trị thiết bị trước.
- Có chế độ chạy thử thử thách trong Cài đặt, không thưởng thời gian.

## Mã nguồn chính

- `MainActivity.kt`, `FocusUi.kt`: giao diện và trạng thái quyền.
- `FocusRepository.kt`, `FocusPolicy.kt`: cấu hình, giới hạn và thưởng.
- `UsageReader.kt`: đọc và ghép UsageEvents thành thời gian.
- `FocusAccessibilityService.kt`: nhận biết ứng dụng và kiểm tra giới hạn liên tục.
- `OverlayLockManager.kt`: màn hình khóa và lời nhắc Cài đặt.
- `ChallengeActivity.kt`, `Vocabulary.kt`: hai thử thách.
- `FocusAdminReceiver.kt`: quản trị thiết bị tùy chọn.
- `assets/english_vocabulary.csv`: từ vựng, UTF-8. Mỗi dòng có 3 cột không chứa dấu phẩy: English,Vietnamese,Part of speech.

## Kiểm tra trên thiết bị

1. Cấp/thu hồi từng quyền, quay lại ứng dụng và xác nhận trạng thái cập nhật.
2. Chọn ứng dụng đã dùng hơn 5 phút hôm nay, đặt giới hạn 5 phút và bật bảo vệ: khi mở ứng dụng phải hiện màn hình khóa.
3. Với ứng dụng chưa hết giờ, giữ nguyên ứng dụng đến lúc đạt giới hạn; xác nhận bị khóa mà không cần chuyển cửa sổ.
4. Trắc nghiệm: trả lời sai làm chuỗi về 0; đúng liên tiếp 5 câu mở thưởng. Bấm tiếp tục và kiểm tra ứng dụng không bị khóa trong khoảng thưởng, rồi khóa lại khi thưởng hết.
5. Lắc nhẹ máy 30 lần; xác nhận không đếm liên tục khi chỉ giữ máy nghiêng. Emulator không thay thế kiểm tra cảm biến trên điện thoại thật.
6. Trở về màn hình chính từ màn hình khóa; kiểm tra overlay biến mất và có thể dùng ứng dụng khác.
7. Tắt bảo vệ: không khóa. Bật nghiêm ngặt: có lời nhắc ở Cài đặt, nút tạm mở/tắt vẫn sử dụng được.
8. Giữ ứng dụng qua 00:00, kiểm tra chuyển ngày, dữ liệu hôm nay và thời gian thưởng hết hiệu lực.
9. Thử khi tắt màn hình, bật lại, đổi ứng dụng, xoay màn hình thử thách và khởi động lại thiết bị.

Unit test tập trung vào ranh giới giới hạn/thưởng, thưởng không vượt nửa đêm, bộ đếm chuyển động và tính duy nhất của đáp án. Build thành công không thay thế kiểm tra các mục trên trên điện thoại thật. Dịch vụ phụ thuộc chính sách pin/OEM và quyền Android. Đếm thời gian trên chia đôi màn hình/PiP không được bảo đảm; sai lệch nhỏ có thể xảy ra do cách Android phát sự kiện và độ trễ kiểm tra khoảng 2 giây.

## Điều chỉnh báo cáo

Ghi đúng phiên bản công cụ hiện tại; đổi “chạy bộ 30 bước” thành “lắc điện thoại 30 lần”; ghi 322 từ vựng thay vì hơn 1.400 câu; mô tả cơ chế chuyển ngày bằng ngày cục bộ thay vì AlarmManager; danh sách ứng dụng tự lưu; quản trị thiết bị là tùy chọn. Chỉ ghi kết quả thực nghiệm mà bạn đã kiểm tra, không tái sử dụng các con số build/lint/APK cũ.

Không thay đổi file báo cáo Word gốc.


## Demo khi đi thi

1. Vào Tổng quan, cuộn tới **Demo khi thuyết trình**.
2. Bấm **Demo TikTok / Facebook** → **Bắt đầu demo**. Ứng dụng tự bật bảo vệ, chọn các gói Facebook/TikTok đã cài và xóa thưởng còn lại của chúng.
3. Ra màn hình chính, mở TikTok/Facebook: màn hình khóa hiện ngay dù chưa dùng đủ 30 phút. Trên màn hình có chữ DEMO; thời gian dùng thật không bị sửa.
4. Chọn **Học từ vựng · Đúng 5 câu**. Có thể trả lời sai để minh họa chuỗi về 0, rồi trả lời đúng liên tiếp 5 câu.
5. Bấm **Tiếp tục dùng ứng dụng**: ứng dụng đích mở lại trong khoảng thưởng 5 phút theo đồng hồ. Hết thưởng sẽ bị khóa lại khi đang ở app đó.
6. Muốn trình diễn lần nữa ngay: về FocusGuard → **Khóa lại để demo** → **Bắt đầu demo**, rồi mở lại mạng xã hội. Không cần chờ hết thưởng.
7. Kết thúc: bấm **Kết thúc demo**. Giới hạn 30 phút/ngày và cấu hình bảo vệ trước khi demo được áp dụng lại. Demo tự hết hiệu lực khi sang ngày mới.

Nếu máy không có TikTok/Facebook: chọn Chrome hoặc một ứng dụng trong mục Ứng dụng rồi bấm **Demo ứng dụng đã chọn**. Không chọn ứng dụng đang chứa bài trình chiếu của bạn.


Cần cấp đủ ba quyền trước khi bấm Demo. Demo không sửa số liệu sử dụng thật.
