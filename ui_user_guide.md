# 📖 Hướng Dẫn Sử Dụng Novel Assistant – Dành Cho Người Mới

---

## 1. Novel Assistant là gì?

**Novel Assistant** là một ứng dụng viết truyện có trợ lý AI tích hợp trực tiếp.  
Bạn chỉ cần *mô tả cảnh mình tưởng tượng*, AI sẽ viết thành đoạn văn hoàn chỉnh đúng phong cách và tính cách nhân vật.

---

## 2. Màn Hình Chính (Home)

Khi mở app lên, bạn thấy danh sách các **tiểu thuyết** (novel cards) của mình.

- **Nhấn vào một thẻ truyện** → vào màn hình Viết (Writing Screen).
- **Nhấn nút "+" hoặc FAB** → tạo tiểu thuyết mới.
- Mỗi thẻ hiện tên truyện và số chương đã có.

---

## 3. Màn Hình Viết (Writing Screen) – Nơi Tạo Ra Truyện

Đây là trái tim của app. Giao diện gồm 3 phần chính:

```
┌──────────────────────────────────┐
│  [← Quay lại]  Tên truyện  [≡]  │  ← Thanh tiêu đề (TopAppBar)
├──────────────────────────────────┤
│                                  │
│   Vùng hiển thị văn bản AI       │  ← Nội dung cuộn được
│   (auto scroll khi AI đang viết) │
│                                  │
├──────────────────────────────────┤
│  [🎨] [__Nhập ý tưởng...__] [▶] │  ← Thanh nhập liệu (Input Bar)
└──────────────────────────────────┘
```

### 3.1 Cách Tạo Một Đoạn Truyện

1. **Gõ ý tưởng cảnh** vào ô nhập liệu dưới cùng.
   - Ví dụ: `"Đêm mưa, Minh đứng trước cửa nhà Linh, chần chừ không dám gõ cửa"`
2. **Nhấn nút Gửi (▶)** — AI sẽ bắt đầu viết ngay lập tức, văn xuất hiện từng dòng (streaming).
3. **Văn bản tự cuộn xuống** để bạn đọc theo kịp.
4. Khi AI viết xong, các nút hành động xuất hiện:
   - 💾 **Lưu phân cảnh** → lưu vào cơ sở dữ liệu.
   - 🕐 **Lịch sử phiên bản** → xem/phục hồi bản cũ.
   - ⭐ **Yêu thích** → đánh dấu đoạn hay.
5. **Chips chỉnh nhanh** (ví dụ: "Buồn hơn", "Thêm khoảng lặng"): nhấn để AI chỉnh sửa theo hướng đó mà không cần gõ lại.

### 3.2 Nhập Vai (Role-play Mode)

Mở **Menu (≡ góc trên phải)** → chọn **Nhập vai**.

- Khi bật: ô nhập liệu chuyển thành `"Nhập hành động/suy nghĩ của nhân vật…"`
- Prompt sẽ được bọc trong `[TÊN_NHÂN_VẬT]: ...` thay vì gửi như mô tả cảnh.
- Dùng khi bạn muốn *nhập vai* đối thoại trực tiếp thay vì chỉ đạo cảnh.

### 3.3 Các Nút Trong Menu (≡)

| Mục | Chức năng |
|-----|-----------|
| **Nhập vai** | Bật/tắt chế độ roleplay |
| **Đọc truyện** | Chuyển sang màn hình đọc (Reader mode) |
| **Trạng thái** | Xem tổng quan câu chuyện, nhân vật, mối quan hệ |
| **Dòng thời gian** | Xem timeline các sự kiện đã xảy ra trong truyện |
| **Nhân vật** | Quản lý danh sách nhân vật |
| **Cài đặt AI** | Mở màn hình Cài đặt |

---

## 4. Màn Hình Cài Đặt (Settings)

### 4.1 Chọn Model AI

- Mặc định: **Gemini 2.5 Flash** (nhanh, ổn định).
- Bạn có thể nhập **tên model thủ công** nếu muốn dùng model khác (ví dụ: `gemini-1.5-pro`).
- Thay đổi có hiệu lực ngay lần gọi API tiếp theo — **không cần khởi động lại app**.

### 4.2 Các Thanh Trượt Vibe (Phong Cách Truyện)

Đây là các thông số kiểm soát "hồn" của đoạn văn AI viết ra:

| Thanh | Ý nghĩa |
|-------|---------|
| **Năng lượng (Energy)** | `0` = tĩnh lặng, `3` = bùng nổ, căng thẳng |
| **Tính khó đoán (Unpredictability)** | Thấp = đúng ý bạn, Cao = AI sáng tạo hơn |
| **Tính liên tục (Continuity)** | Cao = bám sát cốt truyện, Thấp = thoải mái hơn |
| **Góc nhìn điện ảnh (Cinematic)** | Cao = tả cảnh như quay phim, Thấp = kể chuyện thuần |
| **Nội tâm (Introspection)** | Cao = đào sâu suy nghĩ nhân vật |
| **Melancholy** | Bật = thêm khoảng lặng, vibe u buồn |

### 4.3 Preset Vibe Nhanh

Ngoài thanh trượt, bạn có thể chọn **Preset** có sẵn để áp dụng trọn bộ phong cách:

- **Visual novel Hàn** – Chemistry ngầm, ánh đèn đường, cảm xúc vi tế
- **Slow burn** – Xây dựng cảm xúc cực chậm, giữ khoảng cách
- **Healing** – Ấm áp, sinh hoạt đời thường, yên bình
- **Drama nhẹ** – Xung đột vừa phải, đối thoại sát thương
- **Điện ảnh đời thường** – Rất thực, âm thanh môi trường sống động

### 4.4 Chủ Đề Đọc Truyện (Reader Theme)

Thay đổi màu nền và chữ khi đọc/viết:

| Theme | Vibe |
|-------|------|
| **WarmDark** (mặc định) | Nền tối ấm kiểu café đêm |
| **WarmCream** | Giấy kem, như đọc sách thật |
| **Sepia** | Màu nâu cổ điển |
| **NightBlue** | Xanh đêm, sâu và tĩnh |

### 4.5 Quản Lý API Key

- App dùng **Key Rotation** (tự xoay vòng key): nếu một key bị giới hạn tốc độ (rate limit), app tự động chuyển sang key tiếp theo.
- Bạn có thể thêm nhiều key Gemini để tăng quota.
- Mỗi key có trạng thái: 🟢 Hoạt động, 🟡 Hạ nhiệt, 🔴 Chết, ⚪ Chưa kiểm tra.

---

## 5. ⚙️ Cơ Chế Gọi API – Khi Nào AI Được Gọi?

> **Tóm gọn: App KHÔNG bao giờ tự động gọi AI. Chỉ gọi khi BẠN nhấn nút Gửi.**

Các trường hợp gọi API:

| Hành động của bạn | API được gọi không? |
|-------------------|---------------------|
| Nhấn **Gửi (▶)** | ✅ Có – tạo đoạn truyện mới |
| Nhấn **Chips chỉnh nhanh** ("Buồn hơn",...) | ✅ Có – tinh chỉnh đoạn hiện tại |
| **Lưu** đoạn truyện xong | ✅ Có – AI **tự động phân tích ký ức ngầm** |
| Chỉ đọc truyện, scroll | ❌ Không |
| Thay đổi Cài đặt | ❌ Không |
| Mở app, vào màn hình | ❌ Không |

---

## 6. 🧠 Cách App Xây Dựng Prompt Gửi Lên AI

Khi bạn nhấn Gửi, app **ghép 3 lớp** lại thành một prompt hoàn chỉnh:

```
┌────────────────────────────────────────────────────┐
│  LAYER 1: SYSTEM PROMPT (Hướng dẫn cho AI)          │
│  - Bộ quy tắc chống AI generic (anti-cliché)        │
│  - Phong cách từ Vibe sliders + Preset               │
│  - Ví dụ: "NĂNG LƯỢNG CĂNG NGẦM: Thoại lửng lơ..." │
│  - Ví dụ: "KHÔNG triết lý sáo rỗng, Show don't tell"│
├────────────────────────────────────────────────────┤
│  LAYER 2: CONTEXT PROMPT (Bối cảnh truyện)          │
│  - Tên & mô tả tiểu thuyết                          │
│  - Danh sách nhân vật + tính cách                   │
│  - Các mối quan hệ giữa nhân vật                    │
│  - Tóm tắt các phân cảnh gần nhất (để liên tục)     │
│  - Ký ức ngầm (memories) được AI đúc kết            │
├────────────────────────────────────────────────────┤
│  LAYER 3: USER PROMPT (Ý tưởng của bạn)             │
│  - Đúng những gì bạn gõ vào ô nhập liệu             │
│  - Hoặc [NHÂN VẬT]: hành động... (nếu roleplay)     │
└────────────────────────────────────────────────────┘
```

Tất cả 3 lớp được gộp lại → gửi lên **Gemini API** qua kết nối **SSE (Server-Sent Events)** để stream kết quả về từng đoạn nhỏ.

---

## 7. 🔄 Hệ Thống Ký Ức Ngầm (Memory System)

Sau khi bạn **lưu** một phân cảnh, app sẽ tự động (chạy nền):

1. Gọi AI phân tích đoạn truyện vừa lưu.
2. Trích xuất các sự kiện quan trọng (thay đổi cảm xúc, lời hứa, chấn thương, phát triển nhân vật...).
3. Lưu vào cơ sở dữ liệu dưới dạng **Ký ức** (Memory).
4. Các ký ức này sẽ được đưa vào **Layer 2** của prompt ở các lần viết tiếp theo → AI luôn "nhớ" chuyện đã xảy ra.

---

## 8. ❗ Xử Lý Lỗi Thường Gặp

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| "Không có API key nào khả dụng" | Tất cả key đang bị rate limit | Chờ vài phút hoặc thêm key mới trong Cài đặt |
| AI tạo ra văn rất ngắn | Token limit thấp hoặc model yếu | Chọn model mạnh hơn hoặc tăng Cinematic level |
| Bàn phím che mất ô nhập | Đã được fix – cập nhật app | Cập nhật lên phiên bản mới nhất |
| Văn bản không cuộn tự động | Đã được fix | Cập nhật lên phiên bản mới nhất |

---

## 9. 🎯 Quy Trình Làm Việc Gợi Ý (Workflow)

```
① Tạo tiểu thuyết mới → đặt tên, mô tả tổng thể
         ↓
② Thêm nhân vật (màn hình Nhân vật) → ghi rõ tính cách
         ↓
③ Chọn Preset Vibe phù hợp với thể loại truyện
         ↓
④ Vào Writing Screen → gõ ý tưởng cảnh → nhấn Gửi
         ↓
⑤ Đọc kết quả → dùng Chips chỉnh nhanh nếu cần
         ↓
⑥ Hài lòng → Lưu phân cảnh
         ↓
⑦ App tự phân tích ký ức ngầm (background)
         ↓
⑧ Lặp lại từ ④ cho cảnh tiếp theo
```

---

## 10. ⚡ Bảng Phím Tắt Nhanh

| Hành động | Cách làm |
|-----------|----------|
| Gửi prompt | Nhấn nút **▶** (hoặc Enter nếu bàn phím có nút Xong) |
| Dừng AI đang viết | Nhấn nút **⬛ (Stop)** thay cho nút Gửi |
| Bật/tắt Nhập vai | Menu **≡** → Nhập vai |
| Đổi chủ đề màu | Cài đặt → kéo xuống → mục **Chủ đề đọc truyện** |
| Thêm API key | Cài đặt → kéo xuống → **Nâng cao** → Quản lý Key |
| Xem phiên bản cũ | Nút 🕐 xuất hiện sau khi AI viết xong |
| Xem dòng thời gian | Menu **≡** → Dòng thời gian |

---

_Chúc bạn viết truyện vui vẻ trong không gian sáng tạo ấm cúng! ☕✍️_
