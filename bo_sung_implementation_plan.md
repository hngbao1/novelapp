Bản kế hoạch implementation_plan_v2.md hiện tại đã rất tốt và khá sát với thứ tôi muốn. Hãy giữ toàn bộ hướng đi hiện tại, đặc biệt là:

* Ngữ cảnh thông minh
* Prompt Builder tiếng Việt
* Timeline + Memory
* Chế độ nhập vai
* Khoảnh khắc yêu thích
* Character correction
* Backup local + key rotation

Tuy nhiên tôi muốn chỉnh thêm vài điểm quan trọng:

1. Ưu tiên trải nghiệm trên điện thoại Android trước
   Không cố hiển thị 3 cột cố định trên điện thoại vì sẽ rất chật và khó dùng.

Layout điện thoại dọc nên là:

* Trung tâm màn hình = vùng đọc/viết truyện
* Dưới cùng = ô nhập ý tưởng ngắn
* Drawer bên trái = chương / phân cảnh / timeline
* Bottom sheet hoặc panel trượt = Prompt Builder, trạng thái nhân vật, quan hệ, ký ức, mood

Chỉ tablet hoặc màn hình đủ rộng mới dùng layout 2-3 cột.

2. UI cần cực kỳ dễ hiểu cho người không biết kỹ thuật
   Tôi không quen thuật ngữ chuyên môn hay tiếng Anh.
   Toàn bộ giao diện, setting, nút bấm, mô tả phải dùng tiếng Việt tự nhiên và dễ hiểu.

Ví dụ:

* Không ghi “Context Builder”
  → ghi “Ngữ cảnh thông minh”

* Không ghi “Style Reference”
  → ghi “Đoạn mẫu yêu thích”

* Không ghi “Memory Extraction”
  → ghi “AI tự ghi nhớ chi tiết quan trọng”

3. Cần ưu tiên cảm giác “đọc novel thật”
   Khi AI viết xong, phần hiển thị truyện phải:

* đẹp
* thoáng
* dễ immersion
* ít cảm giác chatbot

Tôi muốn cảm giác giống đang đọc webnovel thật chứ không phải đọc chat AI.

4. Prompt Builder phải là trọng tâm chính của app
   Vì tôi khó viết prompt dài và khó diễn tả ý tưởng thành chữ.
   Prompt Builder cần đủ mạnh để chỉ cần:

* chọn mood
* chọn vibe
* chọn nhân vật
* viết vài dòng ý tưởng ngắn
  là AI đã hiểu khá tốt.

5. App phải hỗ trợ kiểu “người nghĩ bằng hình ảnh”
   Tức là:

* user chỉ nhớ cảnh
* cảm xúc
* hình ảnh
* vài câu thoại rời rạc

AI phải là bên nối và hoàn thiện scene thành novel hoàn chỉnh.

6. Khi AI viết scene
   Cần ưu tiên:

* giữ đúng vibe nhân vật
* giữ nhịp cảm xúc
* giữ continuity giữa các scene
  hơn là cố viết quá hoa mỹ.

7. Chế độ đọc lại truyện rất quan trọng
   Tôi muốn có cảm giác:

* đọc lại dễ
* tìm lại scene dễ
* bookmark scene dễ
* xem timeline dễ
* xem các “khoảnh khắc yêu thích” dễ

Vì tôi thường quên nội dung cũ sau khi viết.

8. Về AI
   Hiện tại dùng Gemini là ổn.
   Nhưng kiến trúc cần mở để sau này có thể:

* dùng Gemini để nhớ/context
* dùng model khác để viết scene cảm xúc tốt hơn

9. Ưu tiên tính ổn định và workflow thực tế
   Tôi không cần app quá “enterprise” hay quá kỹ thuật.
   Quan trọng là:

* dễ dùng
* ít thao tác
* viết nhanh
* đọc lại sướng
* không bị rối UI
* không bị ngợp tính năng

Hãy tối ưu app theo hướng “creative companion cho người viết fanmade bằng AI”, không phải công cụ viết novel chuyên nghiệp kiểu studio.
