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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fnb.ai.feign.MenuFeignClient;
import java.util.ArrayList;
import java.util.Arrays;

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
    private final MenuFeignClient menuFeignClient;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    // ─── MENU TOOLS ─────────────────────────────────────────────────────────

    @Tool("Tìm kiếm món ăn theo từ khóa hoặc sở thích của khách. Luôn dùng tool này khi khách hỏi về món ăn, " +
          "đồ uống, đề xuất món, hoặc muốn xem menu. Trả về danh sách tối đa 5 món phù hợp nhất.")
    public String searchMenu(String keyword) {
        log.debug("[TOOL] searchMenu: keyword={}", keyword);

        List<UUID> ids = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                // 1. Dùng EmbeddingModel tạo vector cho keyword
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(keyword).content();
                String vectorString = Arrays.toString(embedding.vector());

                // 2. Query dùng pgvector `<=>` (cosine distance) để tận dụng HNSW index
                String sql = """
                    SELECT mi.id
                    FROM menu.menu_items mi
                    WHERE mi.is_active = true
                      AND mi.is_available = true
                      AND mi.embedding IS NOT NULL
                      AND (mi.embedding <=> ?::vector) < 0.55
                    ORDER BY mi.embedding <=> ?::vector
                    LIMIT 5
                    """;

                ids = jdbc.queryForList(sql, UUID.class, vectorString, vectorString);
            } catch (Exception e) {
                log.error("[TOOL] RAG Vector search failed, falling back to full-text: {}", e.getMessage());
            }
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

        if (ids.isEmpty()) {
            // FALLBACK CẤP CUỐI: Nếu vẫn không có món nào khớp từ khóa (ví dụ khách hỏi "thời tiết này ăn gì" hoặc "món ngon")
            // Thì lấy 3 món nổi bật để AI có cái mà gợi ý (thay vì bó tay và bịa món)
            String finalFallback = "SELECT mi.id FROM menu.menu_items mi WHERE mi.is_active = true AND mi.is_available = true ORDER BY mi.is_featured DESC LIMIT 3";
            ids = jdbc.queryForList(finalFallback, UUID.class);
            
            if (ids.isEmpty()) return "SYSTEM_COMMAND: BÁO CHO KHÁCH LÀ KHÔNG CÓ MÓN NÀY HOẶC ĐÃ HẾT HÀNG. TUYỆT ĐỐI KHÔNG TỰ BỊA MÓN.";
            
            String result = fetchPricesViaFeign(ids);
            return result.isEmpty() ? "SYSTEM_COMMAND: HỆ THỐNG LỖI" : "SYSTEM_COMMAND: TỪ KHÓA TÌM KIẾM CỦA KHÁCH KHÔNG KHỚP VỚI MÓN NÀO CỤ THỂ, NHƯNG HÃY GỢI Ý CÁC MÓN SAU ĐÂY ĐỂ GIỮ CHÂN KHÁCH:\n" + result;
        }
        String result = fetchPricesViaFeign(ids);
        return result.isEmpty() ? "SYSTEM_COMMAND: HỆ THỐNG ĐANG LỖI, VUI LÒNG BÁO KHÁCH ĐỢI LÁT NỮA HOẶC GỌI NHÂN VIÊN." : result;
    }

    @Tool("Liệt kê tất cả các danh mục món ăn của quán. Dùng khi khách muốn xem quán bán những thể loại gì (vd: đồ nướng, lẩu, nước uống).")
    public String getMenuCategories() {
        log.debug("[TOOL] getMenuCategories");
        String sql = """
            SELECT id, name
            FROM menu.categories
            WHERE is_active = true
            ORDER BY display_order ASC
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ HIỆN TẠI QUÁN CHƯA CÓ DANH MỤC NÀO.";
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
        if (ids.isEmpty()) {
            // FALLBACK: Lấy 5 món bất kỳ đang bán nếu không có món nào được đánh dấu featured
            String fallbackSql = "SELECT mi.id FROM menu.menu_items mi WHERE mi.is_active = true AND mi.is_available = true LIMIT 5";
            ids = jdbc.queryForList(fallbackSql, UUID.class);
        }
        if (ids.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ HIỆN QUÁN CHƯA CÓ MÓN NÀO TRONG MENU.";
        
        String result = fetchPricesViaFeign(ids);
        return result.isEmpty() ? "SYSTEM_COMMAND: HỆ THỐNG ĐANG BẬN, VUI LÒNG BÁO KHÁCH ĐỢI LÁT NỮA HOẶC GỌI NHÂN VIÊN." : result;
    }

    @Tool("Lấy danh sách tùy chọn (size, topping, đường, đá...) của một món ăn cụ thể. " +
          "Dùng khi khách hỏi 'có size không', 'topping gì', 'ít đường được không'. " +
          "Yêu cầu itemId (UUID) của món.")
    public String getItemOptions(String itemId) {
        log.debug("[TOOL] getItemOptions: itemId={}", itemId);
        try {
            UUID.fromString(itemId);
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
        if (rows.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ MÓN NÀY KHÔNG CÓ TÙY CHỌN (KHÔNG CÓ SIZE/TOPPING/...).";
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
                    // Không tiết lộ các mã COUPON vì có thể là mã nội bộ/nhạy cảm
                    if ("COUPON".equals(promo.triggerType())) continue;

                    sb.append("- ").append(promo.name()).append("\n")
                      .append("  Áp dụng: Tự động (Không cần nhập mã)\n");

                    // Scope & Targets
                    if ("ORDER".equals(promo.scope())) {
                        sb.append("  Áp dụng cho: Toàn bộ hóa đơn\n");
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "% trên tổng bill\n" : "đ trực tiếp vào bill\n");
                    } else if ("PRODUCT".equals(promo.scope())) {
                        sb.append("  Áp dụng cho: ");
                        if (promo.targets() != null && !promo.targets().isEmpty()) {
                            int count = 0;
                            int maxPrint = 2; // Rút gọn tối đa
                            for (var t : promo.targets()) {
                                if (count > 0) sb.append(", ");
                                if (count >= maxPrint) {
                                    sb.append("và ").append(promo.targets().size() - maxPrint).append(" mục khác");
                                    break;
                                }
                                String tName = t.targetName() != null ? t.targetName() : "...";
                                sb.append(tName);
                                count++;
                            }
                            sb.append("\n");
                        } else {
                            sb.append("Chưa chỉ định rõ món\n");
                        }
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "%\n" : ("EXACT_PRICE".equals(promo.discountType()) ? "đ (đồng giá)\n" : "đ\n"));
                    } else if ("BUNDLE".equals(promo.scope())) {
                        sb.append("  Áp dụng khi mua theo Combo (").append(promo.bundleItems().size()).append(" món)\n");
                        if (promo.bundleItems() != null && !promo.bundleItems().isEmpty()) {
                            int count = 0;
                            int maxPrint = 2; // Rút gọn tối đa
                            for (var b : promo.bundleItems()) {
                                if (count >= maxPrint) {
                                    sb.append("    + ... và ").append(promo.bundleItems().size() - maxPrint).append(" món khác\n");
                                    break;
                                }
                                String iName = b.itemName() != null ? b.itemName() : "...";
                                String roleDesc = "MAIN".equals(b.role()) || "CONDITION".equals(b.role()) || "BUY".equals(b.role()) ? "Mua" : "Tặng/Giảm";
                                
                                sb.append("    + ").append(b.quantity()).append(" phần ").append(iName).append(" (").append(roleDesc).append(")\n");
                                count++;
                            }
                        }
                        sb.append("  Mức giảm: ").append(promo.discountValue()).append("PERCENT".equals(promo.discountType()) ? "% trên tổng giá trị của bộ Combo này" : "đ trừ vào tổng giá trị của bộ Combo này");
                    }

                    if ("PERCENT".equals(promo.discountType()) && promo.maxDiscount() != null && promo.maxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(" (Tối đa ").append(promo.maxDiscount()).append("đ)");
                    }
                    sb.append("\n");

                    // Requirement
                    if (promo.requirement() != null) {
                        if (promo.requirement().minOrderAmount() != null && promo.requirement().minOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
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
                String finalStr = sb.toString();
                if (finalStr.equals("Các chương trình khuyến mãi đang hoạt động:\n")) {
                    return "SYSTEM_COMMAND: BÁO KHÁCH LÀ HIỆN TẠI QUÁN KHÔNG CÓ KHUYẾN MÃI TỰ ĐỘNG NÀO ĐANG DIỄN RA.";
                }
                return finalStr;
            }
        } catch (Exception e) {
            log.error("[TOOL] getActivePromotions failed: {}", e.getMessage());
            return "SYSTEM_COMMAND: BÁO KHÁCH LÀ HỆ THỐNG KIỂM TRA KHUYẾN MÃI ĐANG LỖI.";
        }
        return "SYSTEM_COMMAND: BÁO KHÁCH LÀ HIỆN TẠI QUÁN KHÔNG CÓ KHUYẾN MÃI NÀO ĐANG DIỄN RA.";
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
        if (rows.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ MÃ KHUYẾN MÃI NÀY KHÔNG TỒN TẠI HOẶC ĐÃ HẾT HẠN.";
        return formatRows(rows);
    }

    @Tool("Lấy thông tin nhà hàng: tên, địa chỉ, số điện thoại hotline, slogan. " +
          "Dùng khi khách hỏi về nhà hàng, địa chỉ, số điện thoại.")
    public String getRestaurantInfo() {
        log.debug("[TOOL] getRestaurantInfo");
        String sql = "SELECT name, slogan, address, phone, open_time, close_time, local_culture_notes FROM menu.restaurant_profile LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ CHƯA CÓ THÔNG TIN NHÀ HÀNG TRÊN HỆ THỐNG.";
        return formatRows(rows);
    }

    // ─── ORDER TOOLS ─────────────────────────────────────────────────────────

    @Tool("Kiểm tra toàn bộ thông tin đơn hàng hiện tại: bao gồm tổng tiền, hóa đơn, VÀ trạng thái bếp làm món. " +
          "Dùng khi khách hỏi 'tính tiền', 'bill bao nhiêu', 'món đâu rồi', 'đơn đang ở đâu'.")
    public String getCustomerOrderDetails() {
        log.debug("[TOOL] getOrderDetails");
        String sessionToken = extractSessionToken();
        if (sessionToken.isEmpty()) return "SYSTEM_COMMAND: YÊU CẦU KHÁCH QUÉT LẠI MÃ QR TẠI BÀN ĐỂ XÁC NHẬN BÀN.";

        StringBuilder result = new StringBuilder();

        // 1. Kiểm tra xem có đơn hàng không
        String billSql = """
            SELECT o.id, o.status, o.subtotal, o.discount, o.tax, o.service_fee, o.total, o.promotion_code
            FROM orders.orders o
            JOIN orders.table_sessions ts ON o.session_id = ts.id
            WHERE ts.session_token = ? AND o.status NOT IN ('PAID', 'CANCELLED')
            ORDER BY o.created_at DESC LIMIT 1
            """;
        List<Map<String, Object>> billRows = jdbc.queryForList(billSql, sessionToken);
        if (billRows.isEmpty()) return "SYSTEM_COMMAND: BÁO KHÁCH LÀ BÀN CHƯA CÓ ĐƠN HÀNG NÀO HOẶC ĐÃ THANH TOÁN XONG.";
        
        UUID orderId = (UUID) billRows.get(0).get("id");

        // 2. Kiểm tra xem ĐÃ GỌI món nào chưa (có thể đơn hàng tồn tại nhưng chưa có ticket món ăn)
        String countSql = "SELECT COUNT(oti.id) FROM orders.order_ticket_items oti JOIN orders.order_tickets ot ON oti.ticket_id = ot.id WHERE ot.order_id = ? AND oti.status != 'CANCELLED'";
        Integer totalItems = jdbc.queryForObject(countSql, Integer.class, orderId);
        
        if (totalItems == null || totalItems == 0) {
            return "SYSTEM_COMMAND: BÁO KHÁCH LÀ BÀN CHƯA GỌI MÓN NÀO (GIỎ HÀNG TRỐNG) DÙ ĐÃ MỞ BÀN.";
        }

        result.append("--- HÓA ĐƠN ---\n").append(formatRows(billRows)).append("\n\n");

        // 3. Trạng thái bếp (chỉ lấy món chưa SERVED)
        String foodSql = """
            SELECT oti.item_name, oti.quantity, oti.status, oti.station
            FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            WHERE ot.order_id = ? AND oti.status NOT IN ('SERVED', 'CANCELLED')
            ORDER BY oti.created_at ASC
            """;
        List<Map<String, Object>> foodRows = jdbc.queryForList(foodSql, orderId);
        if (foodRows.isEmpty()) {
            result.append("--- BẾP ---\nTất cả ").append(totalItems).append(" món đã được làm xong và mang ra bàn.");
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
            return "Đã gọi nhân viên thành công. Hãy báo cho khách biết là nhân viên đang ra hỗ trợ.";
        } catch (Exception e) {
            log.error("[TOOL] callStaff failed: {}", e.getMessage());
            return "Không thể gọi nhân viên lúc này, vui lòng vẫy tay hoặc nhấn chuông tại bàn nhé!";
        }
    }

    // ─── NEW FEATURES ────────────────────────────────────────────────────────
    
    @Tool("Lấy danh sách các nguyên liệu (thành phần) của một món ăn để báo cho khách, giúp khách tránh dị ứng hoặc biết rõ món có gì. " +
          "Yêu cầu truyền vào itemId (UUID) của món ăn.")
    public String getMenuItemIngredients(String itemId) {
        log.debug("[TOOL] getMenuItemIngredients: {}", itemId);
        try {
            UUID.fromString(itemId);
        } catch (Exception e) {
            return "LỖI TỪ HỆ THỐNG: itemId không hợp lệ.";
        }
        
        String sql = """
            SELECT ii.name
            FROM inventory.recipes r
            JOIN inventory.recipe_items ri ON r.id = ri.recipe_id
            JOIN inventory.inventory_items ii ON ri.inventory_item_id = ii.id
            WHERE r.sale_item_id = ?::uuid
            """;
        List<String> ingredients = jdbc.queryForList(sql, String.class, itemId);
        if (ingredients.isEmpty()) return "Món này hiện chưa có thông tin chi tiết về thành phần nguyên liệu.";
        return "Thành phần món ăn gồm có: " + String.join(", ", ingredients);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String fetchPricesViaFeign(List<UUID> ids) {
        try {
            var res = menuFeignClient.getBulkItems(ids);
            if (res != null && res.getData() != null && !res.getData().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (var item : res.getData()) {
                    String desc = item.description() != null ? item.description() : "";
                    if (desc.length() > 50) desc = desc.substring(0, 47) + "...";
                    
                    sb.append("- ").append(item.name()).append(" (").append(item.categoryName()).append(")\n");
                    if (!desc.isEmpty()) sb.append("  Mô tả: ").append(desc).append("\n");
                    
                    if (item.salePrice() != null && item.salePrice().compareTo(item.basePrice()) < 0) {
                        sb.append("  Giá: ").append(item.salePrice()).append("đ (Gốc: ").append(item.basePrice()).append("đ)\n");
                    } else {
                        sb.append("  Giá: ").append(item.basePrice()).append("đ\n");
                    }
                    if (!item.isAvailable()) sb.append("  Tình trạng: Hết hàng\n");
                    sb.append("\n");
                }
                return sb.toString().trim();
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
        sb.append("[SYSTEM_NOTE: CÁC CON SỐ TIỀN TỆ TRONG DATA LÀ VNĐ. KHÔNG TỰ QUY ĐỔI SANG USD. TRẠNG THÁI TIẾNG ANH GIỮ NGUYÊN Ý NGHĨA.]\n");
        for (Map<String, Object> row : rows) {
            sb.append(row.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}
