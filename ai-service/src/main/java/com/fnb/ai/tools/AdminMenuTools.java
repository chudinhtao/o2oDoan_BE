package com.fnb.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.ai.feign.MenuFeignClient;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("adminMenuTools")
@RequiredArgsConstructor
@Slf4j
public class AdminMenuTools {

    private final MenuFeignClient menuFeignClient;
    private final ObjectMapper objectMapper;

    @Tool("Kiểm tra toàn bộ danh sách Khuyến mãi (Promotions) trên hệ thống, bao gồm cả điều kiện áp dụng, đối tượng áp dụng. " +
          "Dùng khi admin hỏi: 'Liệt kê các mã giảm giá', 'Mã VIP10 có điều kiện gì'.")
    public String checkAllPromotions(@P("Số lượng bản ghi tối đa (tùy chọn, mặc định 100)") Integer limit) {
        log.info("[ADMIN-TOOL] checkAllPromotions");
        try {
            var res = menuFeignClient.getAllPromotions(0, limit != null ? limit : 100);
            if (res == null || res.getData() == null) return "Không có dữ liệu khuyến mãi.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] checkAllPromotions error", e);
            return "Lỗi khi lấy danh sách khuyến mãi.";
        }
    }

    @Tool("Kiểm tra thông tin cấu hình của Nhà hàng (Restaurant Profile). Bao gồm tên, giờ mở/đóng cửa, địa chỉ, slogan. " +
          "Dùng khi admin hỏi: 'Nhà hàng mở cửa lúc mấy giờ', 'Thông tin nhà hàng hiện tại'.")
    public String getRestaurantConfig() {
        log.info("[ADMIN-TOOL] getRestaurantConfig");
        try {
            var res = menuFeignClient.getRestaurantProfile();
            if (res == null || res.getData() == null) return "Không tìm thấy cấu hình nhà hàng.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRestaurantConfig error", e);
            return "Lỗi khi lấy thông tin nhà hàng.";
        }
    }

    @Tool("Lấy thông tin chi tiết của 1 món ăn trên Menu (bao gồm các Lựa chọn Topping, Size, Thuế VAT, v.v.). " +
          "Dùng khi admin hỏi: 'Món ABC có những lựa chọn gì', 'Thuế suất của món này là bao nhiêu'.")
    public String getMenuItemOptions(@P("ID của món ăn (UUID)") String itemId) {
        log.info("[ADMIN-TOOL] getMenuItemOptions id={}", itemId);
        try {
            var res = menuFeignClient.getItemDetails(java.util.UUID.fromString(itemId));
            if (res == null || res.getData() == null) return "Không tìm thấy thông tin món ăn.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getMenuItemOptions error", e);
            return "Lỗi khi lấy thông tin món ăn: " + e.getMessage();
        }
    }
}
