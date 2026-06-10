package com.fnb.ai.tools;

import com.fnb.ai.feign.InventoryFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component("adminInventoryTools")
@RequiredArgsConstructor
public class AdminInventoryTools {

    private final InventoryFeignClient inventoryFeignClient;
    private final ObjectMapper objectMapper;

    @Tool("Lấy danh sách nguyên vật liệu sắp hết hạn (Expiring Items). " +
          "Dùng khi admin hỏi: 'có nguyên liệu nào sắp hết hạn không', 'cần đẩy bán những món nào để tránh phí'. " +
          "Tham số days là số ngày còn lại để hết hạn (mặc định 7).")
    public String getExpiringStockItems(@P("Số ngày cần cảnh báo hết hạn") int days) {
        log.info("[INVENTORY-TOOL] getExpiringStockItems days={}", days);
        try {
            var res = inventoryFeignClient.getExpiringStockItems(days);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "✅ Hiện tại không có nguyên vật liệu nào sắp hết hạn trong " + days + " ngày tới.";
            }

            StringBuilder sb = new StringBuilder("⚠️ CẢNH BÁO HẾT HẠN (trong ").append(days).append(" ngày):\n\n");
            for (var row : res.getData()) {
                sb.append("• [").append(row.itemSku()).append("] ").append(row.itemName())
                  .append("\n  Lô hàng: ").append(row.lotNumber())
                  .append(" | Ngày hết hạn: ").append(row.expiryDate())
                  .append(" (còn ").append(row.daysRemaining()).append(" ngày)")
                  .append("\n  Tồn hiện tại: ").append(row.currentStock()).append(" ").append(row.uomName())
                  .append(" | Trạng thái: ").append(row.status()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[INVENTORY-TOOL] getExpiringStockItems error: {}", e.getMessage());
            return "Lỗi khi lấy thông tin hàng hết hạn.";
        }
    }

    @Tool("Phân tích Hao hụt Kho (Variance Report). So sánh giữa tồn kho lý thuyết và tồn kho thực tế qua kiểm kê. " +
          "Dùng khi admin hỏi: 'tháng này hao hụt kho nhiều không', 'có mất cắp nguyên liệu không'. " +
          "Tham số from/to là datetime yyyy-MM-dd'T'HH:mm:ss")
    public String getInventoryVarianceReport(@P("Ngày bắt đầu (yyyy-MM-dd'T'HH:mm:ss)") String from,
                                             @P("Ngày kết thúc (yyyy-MM-dd'T'HH:mm:ss)") String to) {
        log.info("[INVENTORY-TOOL] getInventoryVarianceReport from={} to={}", from, to);
        try {
            LocalDateTime startDate = LocalDateTime.parse(from);
            LocalDateTime endDate   = LocalDateTime.parse(to);
            var res = inventoryFeignClient.getVarianceReport(startDate, endDate);
            if (res == null || res.getData() == null) {
                return "Không có dữ liệu hao hụt trong thời gian này.";
            }

            var data = res.getData();
            StringBuilder sb = new StringBuilder("📉 BÁO CÁO HAO HỤT KHO từ ").append(from).append(" đến ").append(to).append(":\n\n");
            sb.append("Tổng giá trị hao hụt ước tính: ").append(formatVnd(data.totalEstimatedLossValue())).append("\n\n");

            if (data.items() != null && !data.items().isEmpty()) {
                sb.append("Chi tiết các món hao hụt nặng nhất:\n");
                for (var item : data.items()) {
                    sb.append("• ").append(item.itemName())
                      .append(" | Chênh lệch: ").append(item.variance())
                      .append(" | Thiệt hại: ").append(formatVnd(item.estimatedLossValue())).append("\n");
                }
            } else {
                sb.append("✅ Không có chênh lệch đáng kể nào được ghi nhận qua các kỳ kiểm kê.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[INVENTORY-TOOL] getInventoryVarianceReport error: {}", e.getMessage());
            return "Lỗi khi lấy báo cáo hao hụt.";
        }
    }

    @Tool("Lấy danh sách đề xuất nhập hàng tự động bằng AI của hệ thống kho (Purchase Suggestions). " +
          "Dùng khi admin hỏi: 'hôm nay cần nhập hàng gì', 'lên đơn mua hàng thế nào'.")
    public String getPurchaseSuggestions() {
        log.info("[INVENTORY-TOOL] getPurchaseSuggestions");
        try {
            var res = inventoryFeignClient.getPurchaseSuggestions();
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "✅ Kho hiện tại đang ổn định, chưa cần nhập thêm nguyên vật liệu mới.";
            }

            StringBuilder sb = new StringBuilder("🛒 ĐỀ XUẤT NHẬP HÀNG (Auto-Procurement):\n\n");
            for (var row : res.getData()) {
                sb.append("• [").append(row.itemSku()).append("] ").append(row.itemName())
                  .append("\n  Số lượng đề xuất: ").append(row.suggestedQuantity()).append(" ").append(row.uomName())
                  .append("\n  Tồn hiện tại: ").append(row.currentStock()).append(" (Safety: ").append(row.safetyStock()).append(")")
                  .append("\n  Nhà cung cấp: ").append(row.supplierName() != null ? row.supplierName() : "Chưa rõ")
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getPurchaseSuggestions error: {}", e.getMessage());
            return "Lỗi khi lấy đề xuất mua hàng: " + e.getMessage();
        }
    }

    @Tool("Truy vết giao dịch (Nhập/Xuất/Hủy) của 1 nguyên liệu cụ thể. " +
          "Dùng khi admin hỏi: 'Tại sao cà phê dạo này hao nhiều', 'Lịch sử nhập xuất của món này'.")
    public String trackItemTransactions(
            @P("ID của nguyên liệu (tùy chọn)") String itemId,
            @P("Ngày bắt đầu ISO (tùy chọn)") String startDate,
            @P("Ngày kết thúc ISO (tùy chọn)") String endDate) {
        log.info("[ADMIN-TOOL] trackItemTransactions item={} start={} end={}", itemId, startDate, endDate);
        try {
            UUID id = (itemId != null && !itemId.isEmpty()) ? UUID.fromString(itemId) : null;
            var res = inventoryFeignClient.getStockTransactions(id, null, startDate, endDate, 0, 50);
            if (res == null || res.getData() == null) return "Không có dữ liệu giao dịch kho.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] trackItemTransactions error: {}", e.getMessage());
            return "Lỗi khi lấy lịch sử giao dịch kho.";
        }
    }

    @Tool("Kiểm tra danh sách Đơn đặt hàng (Purchase Orders) nhập nguyên liệu. " +
          "Dùng khi admin hỏi: 'Lấy lịch sử nhập hàng', 'Tháng này nhập hàng hết bao nhiêu'.")
    public String getRecentPurchaseOrders(
            @P("Trạng thái (COMPLETED, PENDING) (tùy chọn)") String status,
            @P("Ngày bắt đầu ISO (tùy chọn)") String startDate,
            @P("Ngày kết thúc ISO (tùy chọn)") String endDate) {
        log.info("[ADMIN-TOOL] getRecentPurchaseOrders status={} start={} end={}", status, startDate, endDate);
        try {
            var res = inventoryFeignClient.getPurchaseOrders(status, startDate, endDate, 0, 20);
            if (res == null || res.getData() == null) return "Không có dữ liệu đơn nhập hàng.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRecentPurchaseOrders error: {}", e.getMessage());
            return "Lỗi khi lấy đơn nhập hàng.";
        }
    }

    @Tool("Xem biên bản kiểm kê kho (Stocktakes). " +
          "Dùng khi admin hỏi: 'Xem kết quả kiểm kê', 'Tuần trước kiểm kho có lệch không'.")
    public String getRecentStocktakes(
            @P("Trạng thái (COMPLETED, DRAFT) (tùy chọn)") String status,
            @P("Ngày bắt đầu ISO (tùy chọn)") String startDate,
            @P("Ngày kết thúc ISO (tùy chọn)") String endDate) {
        log.info("[ADMIN-TOOL] getRecentStocktakes status={} start={} end={}", status, startDate, endDate);
        try {
            var res = inventoryFeignClient.getStocktakes(status, startDate, endDate, 0, 10);
            if (res == null || res.getData() == null) return "Không có dữ liệu kiểm kê kho.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRecentStocktakes error: {}", e.getMessage());
            return "Lỗi khi lấy biên bản kiểm kê.";
        }
    }

    @Tool("Lấy tổng quan (Dashboard) hiện tại của Kho: tổng giá trị, số món hết hàng, đơn nhập hàng đang chờ. " +
          "Dùng để trả lời nhanh: 'tình hình kho thế nào', 'tổng vốn đang nằm trong kho'.")
    public String getInventoryDashboard() {
        log.info("[INVENTORY-TOOL] getInventoryDashboard");
        try {
            var res = inventoryFeignClient.getDashboardSummary(null, null);
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu tổng quan kho.";
            }

            var data = res.getData();
            StringBuilder sb = new StringBuilder("📊 TỔNG QUAN KHO HIỆN TẠI:\n\n");
            sb.append("💰 Tổng giá trị tồn kho: ").append(formatVnd(data.totalInventoryValue())).append("\n");
            sb.append("🔻 Nguyên liệu sắp hết (Low Stock): ").append(data.lowStockCount()).append(" món\n");
            sb.append("⚠️ Nguyên liệu sắp hết hạn: ").append(data.expiringItemsCount()).append(" món\n");
            sb.append("🚚 Đơn nhập hàng (PO) đang chờ xử lý: ").append(data.pendingPurchaseOrders()).append(" đơn\n");
            sb.append("📉 Tổng giá trị hàng hủy (Waste) tháng này: ").append(formatVnd(data.wasteValueThisMonth())).append("\n");
            
            return sb.toString();
        } catch (Exception e) {
            log.error("[INVENTORY-TOOL] getInventoryDashboard error: {}", e.getMessage());
            return "Lỗi khi tổng hợp dữ liệu kho.";
        }
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }
}
