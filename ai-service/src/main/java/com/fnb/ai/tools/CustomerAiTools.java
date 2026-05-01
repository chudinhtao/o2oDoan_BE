package com.fnb.ai.tools;

import com.fnb.ai.feign.OrderFeignClient;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bộ công cụ (Tools) cho Customer AI.
 * Đọc: JdbcTemplate trực tiếp → tối ưu tốc độ RAG.
 * Ghi: FeignClient → đảm bảo business logic ở order-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerAiTools {

    private final JdbcTemplate jdbc;
    private final OrderFeignClient orderFeignClient;
    private final com.fnb.ai.feign.MenuFeignClient menuFeignClient;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    // ─── MENU TOOLS ─────────────────────────────────────────────────────────

    @Tool("Tìm kiếm món ăn theo từ khóa hoặc sở thích của khách. Luôn dùng tool này khi khách hỏi về món ăn, " +
          "đồ uống, đề xuất món, hoặc muốn xem menu. Trả về danh sách tối đa 5 món phù hợp nhất.")
    public String searchMenu(String keyword) {
        log.debug("[TOOL] searchMenu: keyword={}", keyword);

        List<UUID> ids = new java.util.ArrayList<>();
        try {
            // 1. Dùng EmbeddingModel tạo vector cho keyword
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(keyword).content();
            String vectorString = java.util.Arrays.toString(embedding.vector());

            // 2. Query dùng pgvector `<->` để tìm món ăn có ý nghĩa tương đồng nhất
            String sql = """
                SELECT mi.id
                FROM menu.menu_items mi
                WHERE mi.is_active = true
                  AND mi.is_available = true
                  AND mi.embedding IS NOT NULL
                ORDER BY mi.embedding <-> ?::vector
                LIMIT 5
                """;

            ids = jdbc.queryForList(sql, UUID.class, vectorString);
        } catch (Exception e) {
            log.error("[TOOL] RAG Vector search failed, falling back to full-text: {}", e.getMessage());
        }

        // 3. Fallback sang tìm kiếm văn bản (Full-text) nếu chưa có embedding hoặc lỗi
        if (ids.isEmpty()) {
            String fallbackSql = """
                SELECT mi.id
                FROM menu.menu_items mi
                WHERE mi.is_active = true
                  AND mi.is_available = true
                  AND (lower(mi.name) LIKE lower(?) OR lower(mi.description) LIKE lower(?))
                ORDER BY mi.is_featured DESC
                LIMIT 5
                """;

            String likeKw = "%" + keyword + "%";
            ids = jdbc.queryForList(fallbackSql, UUID.class, likeKw, likeKw);
        }

        if (ids.isEmpty()) return "Không tìm thấy món phù hợp với yêu cầu: " + keyword;
        
        String result = fetchPricesViaFeign(ids);
        return result.isEmpty() ? "Lỗi khi lấy thông tin giá từ hệ thống." : result;
    }

    @Tool("Liệt kê tất cả các danh mục món ăn của quán. Dùng khi khách muốn xem quán bán những thể loại gì (vd: đồ nướng, lẩu, nước uống).")
    public String getMenuCategories() {
        log.debug("[TOOL] getMenuCategories");
        String sql = """
            SELECT id, name, description
            FROM menu.categories
            ORDER BY display_order ASC
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return "Hiện chưa có danh mục món ăn nào.";
        return formatRows(rows);
    }

    @Tool("Lấy danh sách các món bán chạy nhất (best seller) hoặc món đặc trưng nổi bật của quán. Dùng khi khách hỏi 'món nào bán chạy', 'best seller', 'quán có món gì ngon'.")
    public String getBestSellers() {
        log.debug("[TOOL] getBestSellers");
        String sql = """
            SELECT mi.id
            FROM menu.menu_items mi
            WHERE mi.is_active = true
              AND mi.is_available = true
              AND mi.is_featured = true
            LIMIT 5
            """;
        List<UUID> ids = jdbc.queryForList(sql, UUID.class);
        if (ids.isEmpty()) return "Quán chưa thiết lập món best seller nào.";
        
        String result = fetchPricesViaFeign(ids);
        return result.isEmpty() ? "Lỗi khi lấy thông tin giá từ hệ thống." : result;
    }

    @Tool("Lấy danh sách tùy chọn (size, topping, đường, đá...) của một món ăn cụ thể. " +
          "Dùng khi khách hỏi 'có size không', 'topping gì', 'ít đường được không'. " +
          "Yêu cầu itemId (UUID) của món.")
    public String getItemOptions(String itemId) {
        log.debug("[TOOL] getItemOptions: itemId={}", itemId);
        try {
            java.util.UUID.fromString(itemId);
        } catch (IllegalArgumentException e) {
            return "LỖI TỪ HỆ THỐNG: itemId không đúng định dạng UUID. Vui lòng gọi tool searchMenu() trước để tìm ID chính xác của món ăn, sau đó mới gọi tool này.";
        }
        
        String sql = """
            SELECT g.name AS group_name, g.type AS selection_type, g.is_required,
                   o.name AS option_name, o.extra_price
            FROM menu.item_option_groups g
            JOIN menu.item_options o ON o.group_id = g.id
            WHERE g.item_id = ?::uuid
              AND o.is_available = true
            ORDER BY g.display_order, o.name
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, itemId);
        if (rows.isEmpty()) return "Món này không có tùy chọn thêm.";
        return formatRows(rows);
    }

    @Tool("Liệt kê tất cả chương trình khuyến mãi (tự động và coupon) đang hoạt động hôm nay. " +
          "Dùng khi khách hỏi 'có khuyến mãi gì không', 'mã giảm giá', 'có ưu đãi không'.")
    public String getActivePromotions() {
        log.debug("[TOOL] getActivePromotions");
        try {
            var res = menuFeignClient.getActivePromotions();
            if (res != null && res.getData() != null && !res.getData().isEmpty()) {
                StringBuilder sb = new StringBuilder("Các chương trình khuyến mãi đang hoạt động:\n");
                for (var promo : res.getData()) {
                    sb.append("- ").append(promo.name()).append("\n")
                      .append("  Mã Code: ").append("COUPON".equals(promo.triggerType()) ? promo.code() : "Tự động áp dụng (Không cần nhập mã)").append("\n");

                    // Scope & Targets
                    if ("ORDER".equals(promo.scope())) {
                        sb.append("  Áp dụng cho: Toàn bộ hóa đơn\n");
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "% trên tổng bill" : "đ trực tiếp vào bill");
                    } else if ("PRODUCT".equals(promo.scope())) {
                        sb.append("  Áp dụng cho các món/danh mục sau:\n");
                        if (promo.targets() != null && !promo.targets().isEmpty()) {
                            int count = 0;
                            int maxPrint = 5;
                            for (var t : promo.targets()) {
                                if (count >= maxPrint) {
                                    sb.append("    + ... và ").append(promo.targets().size() - maxPrint).append(" mục khác\n");
                                    break;
                                }
                                String tName = t.targetName() != null ? t.targetName() : "(Chưa tải được tên)";
                                sb.append("    + ").append(tName).append(" (").append("CATEGORY".equals(t.targetType()) ? "Danh mục" : "Món ăn").append(")\n");
                                count++;
                            }
                        } else {
                            sb.append("    + (Chưa chỉ định rõ món)\n");
                        }
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "% trên giá món" : ("EXACT_PRICE".equals(promo.discountType()) ? "đ (Bán đồng giá)" : "đ trực tiếp vào giá món"));
                    } else if ("BUNDLE".equals(promo.scope())) {
                        sb.append("  Áp dụng khi mua theo Combo:\n");
                        if (promo.bundleItems() != null && !promo.bundleItems().isEmpty()) {
                            int count = 0;
                            int maxPrint = 5;
                            for (var b : promo.bundleItems()) {
                                if (count >= maxPrint) {
                                    sb.append("    + ... và ").append(promo.bundleItems().size() - maxPrint).append(" món khác\n");
                                    break;
                                }
                                String iName = b.itemName() != null ? b.itemName() : "(Chưa tải được tên)";
                                sb.append("    + ").append(b.quantity()).append("x ").append(iName).append(" (Vai trò: ").append(b.role()).append(")\n");
                                count++;
                            }
                        }
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "% trên tổng giá trị của bộ Combo này" : "đ trừ vào tổng giá trị của bộ Combo này");
                    }

                    if ("PERCENT".equals(promo.discountType()) && promo.maxDiscount() != null && promo.maxDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        sb.append(" (Tối đa ").append(promo.maxDiscount()).append("đ)");
                    }
                    sb.append("\n");

                    // Requirement
                    if (promo.requirement() != null) {
                        if (promo.requirement().minOrderAmount() != null && promo.requirement().minOrderAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            sb.append("  Điều kiện: Đơn tối thiểu ").append(promo.requirement().minOrderAmount()).append("đ\n");
                        }
                        if (promo.requirement().minQuantity() > 0) {
                            sb.append("  Điều kiện: Mua ít nhất ").append(promo.requirement().minQuantity()).append(" sản phẩm\n");
                        }
                    }

                    // Schedules
                    if (promo.schedules() != null && !promo.schedules().isEmpty()) {
                        sb.append("  Khung giờ áp dụng (Happy Hour):\n");
                        for (var sch : promo.schedules()) {
                            sb.append("    + Thứ ").append(sch.dayOfWeek() == 1 ? "Chủ Nhật" : sch.dayOfWeek()).append(": ").append(sch.startTime()).append(" - ").append(sch.endTime()).append("\n");
                        }
                    }

                    // Stackable
                    if (promo.stackable()) {
                        sb.append("  Được dùng chung với các khuyến mãi khác.\n");
                    }

                    sb.append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("[TOOL] getActivePromotions failed: {}", e.getMessage());
            return "Lỗi khi kiểm tra khuyến mãi từ hệ thống.";
        }
        return "Hiện tại không có chương trình khuyến mãi nào đang hoạt động.";
    }

    @Tool("Kiểm tra điều kiện áp dụng của một chương trình khuyến mãi hoặc mã giảm giá. " +
          "Dùng khi khách hỏi 'mã này áp dụng thế nào', 'điều kiện dùng khuyến mãi này'. Yêu cầu truyền code hoặc tên khuyến mãi.")
    public String getPromotionRules(String promoIdentifier) {
        log.debug("[TOOL] getPromotionRules: identifier={}", promoIdentifier);
        String sql = """
            SELECT p.name, p.trigger_type, p.scope, p.discount_type, p.discount_value, p.max_discount,
                   r.min_order_amount, r.min_quantity
            FROM menu.promotions p
            LEFT JOIN menu.promotion_requirements r ON r.promotion_id = p.id
            WHERE (upper(p.code) = upper(?) OR upper(p.name) LIKE upper(?))
              AND p.is_active = true
            """;

        String likeName = "%" + promoIdentifier + "%";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, promoIdentifier, likeName);
        if (rows.isEmpty()) return "Khuyến mãi '" + promoIdentifier + "' không tồn tại hoặc đã hết hạn.";
        return formatRows(rows);
    }

    @Tool("Lấy thông tin nhà hàng: tên, địa chỉ, số điện thoại hotline, slogan. " +
          "Dùng khi khách hỏi về nhà hàng, địa chỉ, số điện thoại.")
    public String getRestaurantInfo() {
        log.debug("[TOOL] getRestaurantInfo");
        String sql = "SELECT name, slogan, address, phone FROM menu.restaurant_profile LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return "Chưa có thông tin nhà hàng.";
        return formatRows(rows);
    }

    // ─── ORDER TOOLS ─────────────────────────────────────────────────────────

    @Tool("Kiểm tra toàn bộ thông tin đơn hàng hiện tại: bao gồm tổng tiền, hóa đơn, VÀ trạng thái bếp làm món. " +
          "Dùng khi khách hỏi 'tính tiền', 'bill bao nhiêu', 'món đâu rồi', 'đơn đang ở đâu'.")
    public String getOrderDetails() {
        log.debug("[TOOL] getOrderDetails");
        String sessionToken = extractSessionToken();
        if (sessionToken.isEmpty()) return "Không tìm thấy thông tin bàn.";

        StringBuilder result = new StringBuilder();

        // 1. Hóa đơn
        String billSql = """
            SELECT o.status, o.subtotal, o.discount, o.tax, o.service_fee, o.total, o.promotion_code
            FROM orders.orders o
            JOIN orders.table_sessions ts ON o.session_id = ts.id
            WHERE ts.session_token = ? AND o.status NOT IN ('PAID', 'CANCELLED')
            ORDER BY o.created_at DESC LIMIT 1
            """;
        List<Map<String, Object>> billRows = jdbc.queryForList(billSql, sessionToken);
        if (billRows.isEmpty()) return "Chưa có đơn hàng nào cho bàn này.";
        
        result.append("--- HÓA ĐƠN ---\n").append(formatRows(billRows)).append("\n\n");

        // 2. Trạng thái bếp
        String foodSql = """
            SELECT oti.item_name, oti.quantity, oti.status, oti.station
            FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders.orders o ON ot.order_id = o.id
            JOIN orders.table_sessions ts ON o.session_id = ts.id
            WHERE ts.session_token = ? AND o.status NOT IN ('PAID', 'CANCELLED')
              AND oti.status NOT IN ('SERVED', 'CANCELLED')
            ORDER BY oti.created_at ASC
            """;
        List<Map<String, Object>> foodRows = jdbc.queryForList(foodSql, sessionToken);
        if (foodRows.isEmpty()) {
            result.append("--- BẾP ---\nTất cả món đã làm xong và mang ra bàn.");
        } else {
            result.append("--- BẾP (Đang làm) ---\n").append(formatRows(foodRows));
        }

        return result.toString();
    }

    @Tool("Gọi nhân viên ra bàn hỗ trợ khách. " +
          "Dùng khi khách nói 'gọi nhân viên', 'cần hỗ trợ', 'lấy khăn giấy', 'dọn bàn'. " +
          "Yêu cầu chọn callType (chỉ được chọn 1 trong 4: WATER, BILL, CLEAN, SUPPORT) và reason (lý do cụ thể bằng tiếng Việt).")
    public String callStaff(String callType, String reason) {
        log.debug("[TOOL] callStaff: callType={}, reason={}", callType, reason);
        try {
            String sessionToken = extractSessionToken();
            if (sessionToken.isEmpty()) return "Không tìm thấy thông tin bàn.";

            String sql = "SELECT id FROM orders.table_sessions WHERE session_token = ?";
            List<UUID> ids = jdbc.queryForList(sql, UUID.class, sessionToken);
            if (ids.isEmpty()) return "Phiên bàn không hợp lệ.";
            UUID sessionId = ids.get(0);

            OrderFeignClient.StaffCallBody body = new OrderFeignClient.StaffCallBody(
                    sessionId, callType, reason
            );
            orderFeignClient.callStaff(sessionToken, body);
            return "Đã báo nhân viên hỗ trợ (" + callType + "). Nhân viên sẽ đến ngay ạ!";
        } catch (Exception e) {
            log.error("[TOOL] callStaff failed: {}", e.getMessage());
            return "Không thể gọi nhân viên lúc này, vui lòng vẫy tay hoặc nhấn chuông tại bàn nhé!";
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String fetchPricesViaFeign(List<UUID> ids) {
        try {
            var res = menuFeignClient.getBulkItems(ids);
            if (res != null && res.getData() != null && !res.getData().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (var item : res.getData()) {
                    sb.append("- ").append(item.name()).append("\n")
                      .append("  ID: ").append(item.id()).append("\n")
                      .append("  Mô tả: ").append(item.description()).append("\n")
                      .append("  Giá gốc: ").append(item.basePrice()).append("\n")
                      .append("  Giá bán (đã áp dụng KM nếu có): ").append(item.salePrice()).append("\n")
                      .append("  Danh mục: ").append(item.categoryName()).append("\n")
                      .append("  Tình trạng: ").append(item.isAvailable() ? "Còn hàng" : "Hết hàng").append("\n\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("[TOOL] fetchPricesViaFeign failed: {}", e.getMessage());
        }
        return "";
    }

    private String extractSessionToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String token = req.getHeader("X-Session-Token");
            if (token != null) return token;
        }
        return "";
    }

    private String formatRows(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            sb.append(row.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}
