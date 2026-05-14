# Tổng quan & Đề xuất tối ưu màn hình Settings (Cài đặt)

Tài liệu này đánh giá hiện trạng của màn hình Cài đặt (`SettingsScreen.kt`) và đề xuất các tính năng cần thêm/tối ưu, đặc biệt là các tuỳ chỉnh chuyên sâu cho trợ lý AI Gemini để ứng dụng linh hoạt và mạnh mẽ hơn.

---

## 1. Hiện trạng màn hình Settings

Hiện tại, ứng dụng đã có một màn hình cài đặt cơ bản bao gồm 4 phần chính:

### Tuỳ chỉnh đọc truyện
- **Cỡ chữ (Font Size):** Tuỳ chỉnh từ 14 - 24.
- **Khoảng cách dòng (Line Height):** Tuỳ chỉnh từ 24 - 40.
*(Khá đầy đủ cho nhu cầu đọc cơ bản).*

### Trợ lý viết (AI Gemini)
- **Mẫu viết (Model Name):** Cho phép nhập tên model (vd: `gemini-2.0-flash`, `gemini-pro`).
*(Phần này hiện tại quá sơ sài, AI cần nhiều thông số tinh chỉnh hơn để điều khiển văn phong).*

### Sao lưu dữ liệu (Backup)
- **Tự động sao lưu:** Công tắc bật/tắt.
- **Xuất dữ liệu:** Export toàn bộ truyện ra file JSON.
- **Khôi phục dữ liệu:** Import file JSON với tính năng xử lý xung đột cực tốt (Ghi đè, Giữ cả hai, Bỏ qua).

### Thông tin
- Hiển thị phiên bản ứng dụng (v1.0.0).

---

## 2. Những tính năng CẦN THÊM / TỐI ƯU

Để đưa app lên mức độ "Pro" (đặc biệt khi bạn đang dùng Gemini 2.0 / REST API mạnh mẽ), phần Settings cần được chia cấu trúc rõ ràng hơn và bổ sung các tính năng sau:

### 2.1. Cài đặt AI & Gemini (Quan trọng nhất)

Phần "Trợ lý viết" hiện tại chỉ cho đổi tên Model. Cần thiết kế lại thành một trang riêng hoặc mở rộng với các thông số điều khiển (có thể thiết kế dạng thanh trượt như phần Cỡ chữ):

*   **1. Nhiệt độ sáng tạo (Temperature):**
    *   *Slider từ 0.0 đến 2.0.*
    *   *Giải thích:* Càng thấp AI viết càng logic, bám sát cốt truyện. Càng cao AI viết càng bay bổng, sáng tạo, nhiều từ ngữ lạ (thích hợp cho thơ hoặc phân cảnh cần phá cách).
*   **2. Quản lý Token & Độ dài (Max Output Tokens):**
    *   *Dropdown hoặc Slider:* Ngắn (512), Vừa (2048), Dài (8192).
    *   *Giải thích:* Tránh việc AI viết quá lê thê hoặc tự cắt đứt câu giữa chừng.
*   **3. Quản lý API Key cá nhân:**
    *   *Tính năng:* Cho phép người dùng tự nhập API Key của riêng họ thay vì dùng Key mặc định của hệ thống (KeyRotationManager). 
    *   *Lợi ích:* Tránh việc hệ thống quá tải (429) do nhiều người dùng chung Key. Nếu họ nhập Key riêng, dùng Key đó để bypass Rotation.
*   **4. Tuỳ chỉnh Prompt Hệ thống (System Instruction):**
    *   *Tính năng:* TextBox cho phép người dùng tự sửa "Tính cách AI mặc định" (Ví dụ: "Bạn là một nhà văn mạng Trung Quốc chuyên viết truyện tiên hiệp, dùng từ Hán Việt...").
*   **5. Bật/Tắt tính năng Memory (Trí nhớ cốt truyện):**
    *   Nếu phân tích Memory tốn token hoặc tốn thời gian, cho phép người dùng Tắt tự động trích xuất Memory để AI chạy nhanh hơn.

### 2.2. Tối ưu Giao diện (UI/UX)
*   **Chế độ Tối/Sáng (Dark/Light Mode):** Hiện tại ứng dụng đang force Dark Mode (màu tối). Cần thêm tuỳ chọn System, Light, Dark.
*   **Kiểu Font (Font Family):** Thêm tuỳ chọn đổi font chữ (Serif để đọc giống sách giấy, Sans-serif để hiện đại).
*   **Phân trang Settings:** Nếu Settings quá dài, có thể chuyển thành các Navigation Menu (Cài đặt chung, Cài đặt AI, Sao lưu & Phục hồi) thay vì để chung một list scroll dài.

### 2.3. Tối ưu Dữ liệu & Backup
*   **Vị trí lưu file Export:** Cho phép dùng `Intent.ACTION_CREATE_DOCUMENT` (System File Picker) để người dùng chọn vị trí lưu file xuất ra (Google Drive, Download), thay vì lưu mặc định vào Data thư mục ẩn. Hiện tại chức năng Export đang ghi cứng vào `backupManager.getBackupDir()` (thường khó tìm trên Android 13+).
*   **Xoá toàn bộ dữ liệu (Wipe Data):** Thêm nút "Xoá tất cả bộ nhớ và truyện" (Kèm cảnh báo đỏ) để reset app.

---

## 3. Lộ trình triển khai (Đề xuất)

Nếu bạn muốn tôi code thêm các chức năng này, chúng ta có thể làm theo thứ tự sau:

1. **Giai đoạn 1 (Dễ - Làm ngay):** Thêm các Slider điều chỉnh `Temperature`, `Max Tokens` vào giao diện hiện tại. Lưu xuống `AppPreferences.kt` và truyền qua `GeminiProvider.kt`.
2. **Giai đoạn 2 (Vừa):** Sửa nút Export để nó mở hộp thoại chọn thư mục lưu (SAF - Storage Access Framework) để người dùng lấy file JSON ra máy dễ hơn.
3. **Giai đoạn 3 (Nâng cao):** Tuỳ chỉnh System Prompt cá nhân hoá và Giao diện Quản lý API Key riêng.
