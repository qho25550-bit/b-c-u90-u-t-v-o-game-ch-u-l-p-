# DUAL AI DEBATE – Android 13

Bộ mã nguồn khởi đầu gồm:
- Ứng dụng Android native viết bằng Kotlin.
- Backend Node.js gọi OpenAI và Google Gemini.
- Hai AI tranh luận qua 2 vòng, sau đó OpenAI tổng hợp kết luận.

## Yêu cầu

- Android Studio và Android SDK.
- JDK 17.
- Node.js 20+ (khuyến nghị).
- OpenAI API key.
- Gemini API key.
- Một máy chủ có HTTPS để chạy backend.

## 1. Cấu hình backend

Mở thư mục `server`:

```bash
npm install
cp .env.example .env
```

Sửa `.env`, điền API key thật:

```env
OPENAI_API_KEY=...
GEMINI_API_KEY=...
OPENAI_MODEL=gpt-6.2-astra
GEMINI_MODEL=gemini-3.8-flash
PORT=3000
```

Chạy thử:

```bash
npm start
```

Kiểm tra `http://localhost:3000/health`.

Lưu ý: API model có thể thay đổi theo tài khoản, khu vực và thời điểm. Nếu model mặc định không được cấp quyền, đổi `OPENAI_MODEL` hoặc `GEMINI_MODEL` sang model đang hiển thị trong tài khoản nhà cung cấp.

## 2. Triển khai backend

Đưa thư mục `server` lên một máy chủ Node.js có HTTPS. Cấu hình biến môi trường trong dashboard máy chủ, không tải file `.env` chứa khóa lên GitHub.

Sau khi triển khai, kiểm tra:
`https://TEN-MIEN-CUA-BAN/health`

## 3. Kết nối ứng dụng Android

Mở dự án `DUAL_AI_DEBATE` bằng Android Studio.

Trong `app/src/main/java/com/carter/dualaidebate/MainActivity.kt`, tìm:

```kotlin
private val backendUrl = "https://YOUR-BACKEND-DOMAIN.example.com/debate"
```

Thay bằng URL HTTPS thật của backend, ví dụ:

```kotlin
private val backendUrl = "https://api.tenmiencuaban.com/debate"
```

Không đặt API key trong ứng dụng Android.

## 4. Tạo APK

Trong Android Studio:
1. Mở thư mục dự án `DUAL_AI_DEBATE`.
2. Chờ Gradle Sync hoàn tất; cài Android SDK 35 nếu Android Studio yêu cầu.
3. Chọn Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Khi build thành công, chọn Locate trong thông báo.
5. APK debug thường nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Để phát hành:
Build > Generate Signed Bundle / APK > APK, tạo hoặc chọn keystore riêng và lưu bản sao an toàn.

## 5. Cài trên điện thoại Android 13

Chép APK sang điện thoại, mở file APK và cho phép cài ứng dụng từ nguồn đang dùng nếu Android hỏi. Chỉ cài APK do bạn tự build hoặc nguồn bạn tin cậy.

## 6. Kiểm tra lỗi

- `Unable to resolve host`: kiểm tra Internet/URL backend.
- `HTTP 502`: kiểm tra log backend, API key, model và hạn mức.
- `401/403`: API key sai, bị thu hồi hoặc chưa có quyền model.
- `404`: kiểm tra endpoint `/debate` và URL trong ứng dụng.
- Lỗi Gemini: kiểm tra model được tài khoản hỗ trợ và phản hồi API thực tế.

## Bảo mật và giới hạn

- Không chia sẻ API key trong ảnh chụp màn hình, chat hay mã nguồn công khai.
- Backend mẫu chưa có đăng nhập người dùng; cần bổ sung xác thực, giới hạn theo tài khoản, giới hạn chi phí và CORS theo domain trước khi phát hành rộng rãi.
- Mỗi lượt tranh luận hiện gọi 5 lần API (3 OpenAI, 2 Gemini), vì vậy có thể phát sinh phí.
- Kết luận AI không đảm bảo đúng tuyệt đối; kiểm tra nguồn độc lập với nội dung quan trọng.
- `targetSdk` và `compileSdk` là 35; `minSdk` là 26. Thiết bị Android 13 (API 33) tương thích.
