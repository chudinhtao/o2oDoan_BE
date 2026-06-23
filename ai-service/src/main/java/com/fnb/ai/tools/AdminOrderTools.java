package com.fnb.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.ai.feign.OrderFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("adminOrderTools")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderTools {

    private final OrderFeignClient orderFeignClient;
    private final ObjectMapper objectMapper;

    @Tool("Tìm kiếm lịch sử đơn hàng. Cung cấp startDate/endDate (yyyy-MM-ddTHH:mm:ss) hoặc status (COMPLETED, CANCELLED, PENDING). " +
          "Dùng khi admin hỏi: 'Lấy cho tôi lịch sử đơn hàng', 'Hôm qua có bao nhiêu đơn bị hủy'. " +
          "CẢNH BÁO: Tuyệt đối KHÔNG dùng tool này để tính tổng hoặc thống kê. Chỉ dùng để tra cứu chi tiết danh sách.")
    public String searchOrderHistory(
            @P("Trạng thái đơn hàng (tùy chọn)") String status,
            @P("Ngày bắt đầu ISO (tùy chọn)") String startDate,
            @P("Ngày kết thúc ISO (tùy chọn)") String endDate,
            @P("Số lượng bản ghi tối đa (tùy chọn, mặc định 100)") Integer limit) {
        log.info("[ADMIN-TOOL] searchOrderHistory status={} start={} end={}", status, startDate, endDate);
        try {
            int finalLimit = (limit != null && limit > 0) ? Math.min(limit, 500) : 100;
            var res = orderFeignClient.getOrderHistory(status, startDate, endDate, 0, finalLimit);
            if (res == null || res.getData() == null) return "Không tìm thấy dữ liệu đơn hàng.";
            // Trả về JSON rút gọn cho LLM
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] searchOrderHistory error", e);
            return "Lỗi khi lấy lịch sử đơn hàng: " + e.getMessage();
        }
    }

    @Tool("Lấy toàn bộ thông tin chi tiết của 1 đơn hàng dựa vào ID. Bao gồm thông tin các món ăn, giá tiền, phương thức thanh toán. " +
          "Dùng khi admin hỏi: 'Đơn hàng XYZ có những món gì', 'Chi tiết bill này'.")
    public String getOrderDetails(@P("ID của đơn hàng (UUID)") String orderId) {
        log.info("[ADMIN-TOOL] getOrderDetails id={}", orderId);
        try {
            UUID id;
            try {
                id = UUID.fromString(orderId);
            } catch (IllegalArgumentException ex) {
                return "Lỗi: ID đơn hàng ('" + orderId + "') không hợp lệ. Vui lòng yêu cầu người dùng cung cấp chính xác UUID của đơn hàng.";
            }
            var res = orderFeignClient.getOrderById(id);
            if (res == null || res.getData() == null) return "Không tìm thấy đơn hàng với ID: " + orderId;
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getOrderDetails error", e);
            return "Lỗi khi lấy chi tiết đơn hàng: " + e.getMessage();
        }
    }

    @Tool("Điều tra dòng thời gian (Timeline) của 1 đơn hàng. Từ lúc đặt, phục vụ món, in bill, thanh toán. " +
          "Dùng khi admin hỏi: 'Tại sao đơn này làm lâu', 'Đơn XYZ bị hủy lúc mấy giờ', 'Lịch sử phục vụ đơn này'.")
    public String investigateOrderTimeline(@P("ID của đơn hàng (UUID)") String orderId) {
        log.info("[ADMIN-TOOL] investigateOrderTimeline id={}", orderId);
        try {
            UUID id;
            try {
                id = UUID.fromString(orderId);
            } catch (IllegalArgumentException ex) {
                return "Lỗi: ID đơn hàng ('" + orderId + "') không hợp lệ. Vui lòng yêu cầu người dùng cung cấp chính xác UUID của đơn hàng.";
            }
            var res = orderFeignClient.getOrderTimeline(id);
            if (res == null || res.getData() == null) return "Không tìm thấy timeline cho đơn hàng ID: " + orderId;
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] investigateOrderTimeline error", e);
            return "Lỗi khi lấy timeline đơn hàng: " + e.getMessage();
        }
    }
}
