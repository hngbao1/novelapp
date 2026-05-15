package com.novel.assistant.data.remote.ai

/**
 * Các Module Prompt độc lập, dễ dàng tái sử dụng và mở rộng cho nhiều thể loại truyện khác nhau.
 * Phục vụ cho "Visual Novel Co-writing Engine".
 */
object PromptModules {

    /**
     * Trụ cột 1: Energy Rules
     * Điều khiển nhịp điệu và năng lượng của Scene.
     */
    fun getEnergyRules(energyLevel: Int): String {
        return when (energyLevel) {
            0 -> "- NĂNG LƯỢNG TĨNH: Gần như không thoại. Tập trung miêu tả vật thể, không gian, âm thanh nhỏ. Câu văn dài, chậm rãi. Hành động lặp đi lặp lại hoặc đình trệ."
            1 -> "- NĂNG LƯỢNG NHẸ: Nhịp độ từ tốn. Thoại ngắt quãng, tự nhiên. Cảm xúc nhẹ nhàng, không bùng phát. Hành động từ tốn."
            2 -> "- NĂNG LƯỢNG CĂNG NGẦM (Tension): Thoại lửng lơ, giấu giếm. Miêu tả ánh mắt, hơi thở, biểu cảm vi tế. Nhịp điệu dồn dập nhưng kìm nén, ngột ngạt."
            3 -> "- NĂNG LƯỢNG BÙNG NỔ: Thoại ngắn, dứt khoát, trực diện. Cảm xúc bung xoã mạnh mẽ. Câu văn sắc bén, nhịp điệu nhanh."
            else -> "- NĂNG LƯỢNG CÂN BẰNG: Giữ nhịp độ tự nhiên."
        }
    }

    /**
     * Quy tắc Điện ảnh & Cảm xúc (Cinematic & Introspection)
     */
    fun getCinematicAndIntrospectionRules(cinematicLevel: Int, introspectionLevel: Int, melancholyLevel: Int): String {
        return buildString {
            when (cinematicLevel) {
                0 -> appendLine("- GÓC NHÌN VĂN HỌC: Viết như tiểu thuyết truyền thống, tập trung kể chuyện và diễn đạt mạch lạc.")
                1 -> appendLine("- GÓC NHÌN CÂN BẰNG: Kết hợp kể chuyện và miêu tả hình ảnh.")
                2 -> appendLine("- GÓC NHÌN ĐIỆN ẢNH: Viết như một kịch bản/visual novel. Tả góc quay, ánh sáng, âm thanh foley, thiết kế bối cảnh thay vì kể lể.")
            }
            
            when (introspectionLevel) {
                0 -> appendLine("- NỘI TÂM KHÁCH QUAN: Không đi sâu vào suy nghĩ nhân vật. Chỉ tả cái nhìn thấy được.")
                1 -> appendLine("- NỘI TÂM VỪA: Thi thoảng điểm xuyết suy nghĩ ngắn gọn.")
                2 -> appendLine("- NỘI TÂM SÂU SẮC: Khai thác sâu suy nghĩ mâu thuẫn, những lời chưa dám nói, sự chật vật trong tâm trí.")
            }

            when (melancholyLevel) {
                1 -> appendLine("- VIBE BUỒN NHẸ: Thêm vài khoảng lặng, màu sắc u buồn nhẹ.")
                2 -> appendLine("- VIBE MELANCHOLY (Khoảng lặng): Thật nhiều khoảng lặng. Thoại cụt lủn. Miêu tả những chi tiết nhỏ lẻ loi để làm nổi bật sự cô đơn/hoài niệm.")
            }
        }
    }

    /**
     * Smart Presets: Định hình toàn bộ Vibe truyện
     */
    fun getPresetRules(presetName: String): String {
        return when (presetName) {
            "Visual novel Hàn" -> """
                - PRESET VISUAL NOVEL HÀN:
                  + Chú trọng "Chemistry ngầm" giữa các nhân vật (những cái chạm tình cờ, ánh mắt né tránh).
                  + Ánh sáng thường mang tính điện ảnh (đèn đường, hoàng hôn, mưa).
                  + Cảm xúc vi tế, không khoa trương.
            """.trimIndent()
            
            "Slow burn" -> """
                - PRESET SLOW BURN:
                  + Xây dựng mâu thuẫn và tình cảm cực kỳ chậm.
                  + Giữ khoảng cách giữa các nhân vật. Tăng sự ngập ngừng.
                  + Không bao giờ giải quyết xung đột quá nhanh.
            """.trimIndent()
            
            "Healing" -> """
                - PRESET HEALING (Chữa lành):
                  + Không khí ấm áp, bình yên, an toàn.
                  + Xung đột được giải quyết bằng sự thấu hiểu, lắng nghe.
                  + Tập trung vào chi tiết sinh hoạt đời thường (nấu ăn, uống trà, dọn dẹp).
            """.trimIndent()
            
            "Drama nhẹ" -> """
                - PRESET DRAMA:
                  + Xung đột cảm xúc rõ ràng nhưng giữ mức độ thực tế.
                  + Đối thoại có tính sát thương hoặc hiểu lầm.
            """.trimIndent()
            
            "Điện ảnh đời thường" -> """
                - PRESET ĐIỆN ẢNH ĐỜI THƯỜNG (Slice of Life Cinematic):
                  + Rất thực tế, âm thanh môi trường rõ (tiếng xe, chim hót, gió).
                  + Mọi thứ diễn ra tự nhiên, không cố tạo kịch tính.
            """.trimIndent()
            
            else -> ""
        }
    }

    /**
     * Trụ cột cốt lõi: Anti-AI Generic Rules
     */
    fun getAntiAiRules(): String {
        return """
            === LỆNH CẤM TỐI THƯỢNG (ANTI-AI GENERIC RULES) ===
            1. KHÔNG triết lý sáo rỗng, KHÔNG tổng kết đạo lý cuối scene.
            2. KHÔNG tự tiện dán nhãn cảm xúc ("cô ấy cảm thấy buồn vì..."). Thay vào đó, hãy miêu tả hành vi, cử chỉ (Show, Don't Tell).
            3. KHÔNG viết mọi thứ quá hoàn hảo, kịch tính thái quá (overdramatic) hoặc uỷ mị giả tạo. Giữ sự trần trụi, có tỳ vết.
            4. KHÔNG nhắc đi nhắc lại tên nhân vật liên tục. Dùng đại từ (hắn, y, cô, cậu...) hoặc miêu tả ngoại hình để thay thế.
            5. KHÔNG TỰ KẾT THÚC SCENE sớm. KHÔNG tự động giải quyết vấn đề. Luôn để lại khoảng hở (hook) cho scene tiếp theo.
            6. KHÔNG dùng các cụm từ cliché như: "mỉm cười cay đắng", "nụ cười không chạm đến đáy mắt", "thở dài thườn thượt".
            7. Nội tâm phải thật ngắn gọn, cắt cụt, tự nhiên, giống suy nghĩ thật của con người đang rối bời.
        """.trimIndent()
    }
}
