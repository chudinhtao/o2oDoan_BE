package com.fnb.report.repository;

import com.fnb.common.dto.PageResponse;
import com.fnb.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    // F1: Thêm avg_order_value
    public List<RevenueDto> getRevenueReport(LocalDate from, LocalDate to) {
        String sql = """
            SELECT DATE(COALESCE(paid_at, updated_at)) as day,
                   SUM(total) as revenue,
                   SUM(tax) as tax_amount,
                   COUNT(*) as orders,
                   ROUND(AVG(total), 0) as avg_order_value
            FROM orders
            WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) BETWEEN ? AND ?
            GROUP BY DATE(COALESCE(paid_at, updated_at))
            ORDER BY day
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                BigDecimal gross = rs.getBigDecimal("revenue");
                BigDecimal tax = rs.getBigDecimal("tax_amount");
                return RevenueDto.builder()
                    .day(rs.getDate("day").toLocalDate())
                    .revenue(gross)
                    .taxAmount(tax)
                    .netRevenue(gross.subtract(tax != null ? tax : BigDecimal.ZERO))
                    .totalOrders(rs.getLong("orders"))
                    .avgOrderValue(rs.getBigDecimal("avg_order_value"))
                    .build();
            },
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // F2: Thêm sortBy param (QUANTITY hoặc REVENUE)
    public PageResponse<TopItemDto> getTopItems(LocalDate from, LocalDate to, String sortBy, int page, int size) {
        String orderClause = "REVENUE".equalsIgnoreCase(sortBy) ? "revenue DESC" : "total_sold DESC";

        String countSql = """
            SELECT COUNT(DISTINCT oti.item_name)
            FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders o ON ot.order_id = o.id
            WHERE o.status = 'PAID'
              AND oti.status NOT IN ('CANCELLED', 'RETURNED')
              AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            """;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = """
            SELECT oti.item_name, SUM(oti.quantity) as total_sold, SUM(oti.unit_price * oti.quantity) as revenue
            FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders o ON ot.order_id = o.id
            WHERE o.status = 'PAID'
              AND oti.status NOT IN ('CANCELLED', 'RETURNED')
              AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            GROUP BY oti.item_name
            ORDER BY
            """ + orderClause + """
            \nOFFSET ? LIMIT ?
            """;

        List<TopItemDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> TopItemDto.builder()
                .itemName(rs.getString("item_name"))
                .totalSold(rs.getLong("total_sold"))
                .revenue(rs.getBigDecimal("revenue"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public List<ItemSalesDto> getItemSalesSummary(LocalDate from, LocalDate to) {
        String sql = """
            SELECT item_id, SUM(quantity) as total_sold
            FROM (
                SELECT oti.menu_item_id as item_id, oti.quantity
                FROM orders.order_ticket_items oti
                JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
                JOIN orders.orders o ON ot.order_id = o.id
                WHERE o.status = 'PAID'
                  AND oti.status NOT IN ('CANCELLED', 'RETURNED')
                  AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
                
                UNION ALL
                
                SELECT oio.menu_item_id as item_id, oti.quantity
                FROM orders.order_item_options oio
                JOIN orders.order_ticket_items oti ON oio.ticket_item_id = oti.id
                JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
                JOIN orders.orders o ON ot.order_id = o.id
                WHERE o.status = 'PAID'
                  AND oti.status NOT IN ('CANCELLED', 'RETURNED')
                  AND oio.menu_item_id IS NOT NULL
                  AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            ) as combined_sales
            GROUP BY item_id
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                String idStr = rs.getString("item_id");
                return ItemSalesDto.builder()
                    .itemId(idStr != null ? java.util.UUID.fromString(idStr) : null)
                    .quantitySold(rs.getBigDecimal("total_sold"))
                    .build();
            },
            Date.valueOf(from),
            Date.valueOf(to),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    public ProfitLossDto getInventorySummary(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT
                COALESCE(ABS(SUM(CASE WHEN st.transaction_type = 'OUT_SALE' THEN st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0) ELSE 0 END)), 0) as cogs,
                COALESCE(ABS(SUM(CASE WHEN (st.transaction_type = 'OUT_WASTE' OR (st.transaction_type = 'ADJUSTMENT' AND st.quantity_change < 0)) THEN st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0) ELSE 0 END)), 0) as waste
            FROM inventory.stock_transactions st
            JOIN inventory.inventory_items i ON st.item_id = i.id
            WHERE st.created_at >= ? AND st.created_at <= ?
            """;

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> ProfitLossDto.builder()
                .totalCogs(rs.getBigDecimal("cogs"))
                .totalWaste(rs.getBigDecimal("waste"))
                .build(),
            java.sql.Timestamp.valueOf(from),
            java.sql.Timestamp.valueOf(to)
        );
    }

    public PageResponse<InventoryVarianceDto> getInventoryVariance(LocalDateTime from, LocalDateTime to, int page, int size) {
        String baseSql = """
            WITH sales AS (
                SELECT o.order_type, oio.menu_item_id as sale_item_id, SUM(oti.quantity) as qty
                FROM orders.order_item_options oio
                JOIN orders.order_ticket_items oti ON oio.ticket_item_id = oti.id
                JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
                JOIN orders.orders o ON ot.order_id = o.id
                WHERE o.status = 'PAID' AND oti.status NOT IN ('CANCELLED', 'RETURNED')
                  AND o.created_at >= ? AND o.created_at <= ?
                GROUP BY o.order_type, oio.menu_item_id
                UNION ALL
                SELECT o.order_type, oti.menu_item_id as sale_item_id, SUM(oti.quantity) as qty
                FROM orders.order_ticket_items oti
                JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
                JOIN orders.orders o ON ot.order_id = o.id
                WHERE o.status = 'PAID' AND oti.status NOT IN ('CANCELLED', 'RETURNED')
                  AND o.created_at >= ? AND o.created_at <= ?
                GROUP BY o.order_type, oti.menu_item_id
            ),
            grouped_sales AS (
                SELECT order_type, sale_item_id, SUM(qty) as total_qty
                FROM sales
                GROUP BY order_type, sale_item_id
            ),
            theoretical AS (
                SELECT ri.inventory_item_id, 
                       SUM(
                           gs.total_qty * ri.quantity 
                           * (1.0 + COALESCE(ri.wastage_percent, 0) / 100.0) 
                           * COALESCE(uc.conversion_rate, 1)
                       ) as expected_qty
                FROM grouped_sales gs
                JOIN inventory.recipes r ON (r.sale_item_id = gs.sale_item_id OR r.modifier_id = gs.sale_item_id)
                JOIN inventory.recipe_items ri ON ri.recipe_id = r.id
                JOIN inventory.inventory_items ii ON ri.inventory_item_id = ii.id
                LEFT JOIN inventory.item_uom_conversions uc 
                    ON uc.item_id = ri.inventory_item_id 
                    AND uc.from_uom_id = ri.uom_id 
                    AND uc.to_uom_id = ii.base_uom_id
                WHERE COALESCE(ri.scope, 'ALWAYS') = 'ALWAYS'
                   OR (ri.scope = 'TAKEAWAY_ONLY' AND REPLACE(gs.order_type, '_', '') IN ('TAKEAWAY', 'DELIVERY'))
                   OR (ri.scope = 'DINE_IN_ONLY' AND REPLACE(gs.order_type, '_', '') = 'DINEIN')
                GROUP BY ri.inventory_item_id
            ),
            actual AS (
                SELECT st.item_id as inventory_item_id, -SUM(st.quantity_change) as actual_qty
                FROM inventory.stock_transactions st
                LEFT JOIN orders.orders o ON st.reference_id = o.id AND st.transaction_type IN ('OUT_SALE', 'REFUND')
                WHERE st.transaction_type IN ('OUT_SALE', 'OUT_WASTE', 'ADJUSTMENT', 'REFUND')
                  AND COALESCE(o.created_at, st.created_at) >= ? AND COALESCE(o.created_at, st.created_at) <= ?
                GROUP BY st.item_id
            )
            SELECT 
                COALESCE(t.inventory_item_id, a.inventory_item_id) as ingredient_id,
                ii.name as ingredient_name,
                u.name as uom_name,
                ii.avg_cost_price as avg_cost,
                COALESCE(t.expected_qty, 0) as theoretical_usage,
                COALESCE(a.actual_qty, 0) as actual_usage,
                COALESCE(a.actual_qty, 0) - COALESCE(t.expected_qty, 0) as variance
            FROM theoretical t
            FULL OUTER JOIN actual a ON t.inventory_item_id = a.inventory_item_id
            JOIN inventory.inventory_items ii ON COALESCE(t.inventory_item_id, a.inventory_item_id) = ii.id
            LEFT JOIN inventory.uoms u ON ii.base_uom_id = u.id
            """;

        java.sql.Timestamp start = java.sql.Timestamp.valueOf(from);
        java.sql.Timestamp end = java.sql.Timestamp.valueOf(to);

        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, start, end, start, end, start, end);

        String sql = baseSql + "\nOFFSET ? LIMIT ?";
        List<InventoryVarianceDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                BigDecimal variance = rs.getBigDecimal("variance");
                BigDecimal avgCost = rs.getBigDecimal("avg_cost");
                if (avgCost == null) avgCost = BigDecimal.ZERO;
                return InventoryVarianceDto.builder()
                    .ingredientId(java.util.UUID.fromString(rs.getString("ingredient_id")))
                    .ingredientName(rs.getString("ingredient_name"))
                    .uomName(rs.getString("uom_name"))
                    .theoreticalUsage(rs.getBigDecimal("theoretical_usage"))
                    .actualUsage(rs.getBigDecimal("actual_usage"))
                    .variance(variance)
                    .varianceValue(variance.multiply(avgCost))
                    .build();
            },
            start, end, start, end, start, end, page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<TopWastedItemDto> getTopWastedItems(LocalDateTime from, LocalDateTime to, int page, int size) {
        String baseSql = """
            SELECT st.item_id,
                   ii.name as ingredient_name,
                   u.name as uom_name,
                   ABS(SUM(st.quantity_change)) as wasted_quantity,
                   ABS(SUM(st.quantity_change * COALESCE(st.unit_price_at_transaction, ii.avg_cost_price, 0))) as wasted_value
            FROM inventory.stock_transactions st
            JOIN inventory.inventory_items ii ON st.item_id = ii.id
            LEFT JOIN inventory.uoms u ON ii.base_uom_id = u.id
            WHERE (st.transaction_type = 'OUT_WASTE' OR (st.transaction_type = 'ADJUSTMENT' AND st.quantity_change < 0))
              AND st.created_at >= ? AND st.created_at <= ?
            GROUP BY st.item_id, ii.name, u.name
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") as sub";
        Long total = jdbcTemplate.queryForObject(
            countSql, Long.class, 
            java.sql.Timestamp.valueOf(from), java.sql.Timestamp.valueOf(to)
        );

        String sql = baseSql + "\nORDER BY wasted_value DESC\nOFFSET ? LIMIT ?";
        List<TopWastedItemDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> TopWastedItemDto.builder()
                .ingredientId(UUID.fromString(rs.getString("item_id")))
                .ingredientName(rs.getString("ingredient_name"))
                .uomName(rs.getString("uom_name"))
                .wastedQuantity(rs.getBigDecimal("wasted_quantity"))
                .wastedValue(rs.getBigDecimal("wasted_value"))
                .build(),
            java.sql.Timestamp.valueOf(from),
            java.sql.Timestamp.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }
    
    public PageResponse<StaffPerformanceDto> getStaffPerformance(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            WITH sales AS (
                SELECT cashier_id, 
                       COUNT(id) as total_orders, 
                       SUM(total) as total_revenue
                FROM orders.orders
                WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) BETWEEN ? AND ?
                  AND cashier_id IS NOT NULL
                GROUP BY cashier_id
            ),
            cancellations AS (
                SELECT o.cancelled_by as cashier_id, 
                       COUNT(DISTINCT o.id) as cancelled_orders, 
                       COALESCE(SUM(
                           oti.quantity * (
                               oti.unit_price + 
                               COALESCE((SELECT SUM(oio.extra_price) FROM orders.order_item_options oio WHERE oio.ticket_item_id = oti.id), 0)
                           )
                       ), 0) as cancelled_revenue
                FROM orders.orders o
                LEFT JOIN orders.order_tickets ot ON o.id = ot.order_id
                LEFT JOIN orders.order_ticket_items oti ON ot.id = oti.ticket_id AND (oti.status = 'CANCELLED' OR oti.status = 'RETURNED')
                WHERE o.status = 'CANCELLED' AND DATE(COALESCE(o.updated_at, o.created_at)) BETWEEN ? AND ?
                  AND o.cancelled_by IS NOT NULL
                GROUP BY o.cancelled_by
            )
            SELECT 
                COALESCE(s.cashier_id, c.cashier_id) as staff_id,
                u.full_name as staff_name,
                COALESCE(s.total_orders, 0) as total_orders,
                COALESCE(s.total_revenue, 0) as total_revenue,
                COALESCE(c.cancelled_orders, 0) as cancelled_orders,
                COALESCE(c.cancelled_revenue, 0) as cancelled_revenue
            FROM sales s
            FULL OUTER JOIN cancellations c ON s.cashier_id = c.cashier_id
            LEFT JOIN auth.users u ON COALESCE(s.cashier_id, c.cashier_id) = u.id
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(
            countSql, Long.class, 
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to)
        );

        String sql = baseSql + "\nORDER BY total_revenue DESC\nOFFSET ? LIMIT ?";
        List<StaffPerformanceDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                long totalOrders = rs.getLong("total_orders");
                long cancelledOrders = rs.getLong("cancelled_orders");
                long allOrders = totalOrders + cancelledOrders;
                double cancelRate = allOrders > 0 ? (double) cancelledOrders / allOrders * 100.0 : 0.0;
                
                String staffName = rs.getString("staff_name");
                if (staffName == null || staffName.isEmpty()) staffName = "Unknown Staff";

                return StaffPerformanceDto.builder()
                    .cashierId(UUID.fromString(rs.getString("staff_id")))
                    .staffName(staffName)
                    .totalOrders(totalOrders)
                    .totalRevenue(rs.getBigDecimal("total_revenue"))
                    .cancelledOrders(cancelledOrders)
                    .cancelledRevenue(rs.getBigDecimal("cancelled_revenue"))
                    .cancelRate(Math.round(cancelRate * 10.0) / 10.0)
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    // F3: Thêm percentage window function và totalAllRevenue
    public List<SourceDto> getRevenueBySource(LocalDate from, LocalDate to) {
        String sql = """
            SELECT source,
                   COUNT(*) as orders,
                   SUM(total) as revenue,
                   SUM(SUM(total)) OVER () as total_all_revenue
            FROM orders
            WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) BETWEEN ? AND ?
            GROUP BY source
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                BigDecimal rev = rs.getBigDecimal("revenue");
                BigDecimal totalAll = rs.getBigDecimal("total_all_revenue");
                double pct = 0;
                if (totalAll != null && totalAll.compareTo(BigDecimal.ZERO) > 0) {
                    pct = rev.divide(totalAll, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                             .setScale(1, RoundingMode.HALF_UP)
                             .doubleValue();
                }
                return SourceDto.builder()
                    .source(rs.getString("source"))
                    .totalOrders(rs.getLong("orders"))
                    .revenue(rev)
                    .percentage(pct)
                    .totalAllRevenue(totalAll)
                    .build();
            },
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // F4: Thêm avgOrderValue
    public List<HourlyTrafficDto> getHourlyTraffic(LocalDate from, LocalDate to) {
        String sql = """
            SELECT EXTRACT(HOUR FROM created_at) as hour_of_day,
                   COUNT(id) as order_count,
                   SUM(total) as revenue,
                   ROUND(AVG(total), 0) as avg_order_value
            FROM orders
            WHERE status = 'PAID' AND DATE(created_at) BETWEEN ? AND ?
            GROUP BY EXTRACT(HOUR FROM created_at)
            ORDER BY hour_of_day
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> HourlyTrafficDto.builder()
                .hourOfDay(rs.getInt("hour_of_day"))
                .orderCount(rs.getLong("order_count"))
                .revenue(rs.getBigDecimal("revenue"))
                .avgOrderValue(rs.getBigDecimal("avg_order_value"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // F5: Thêm zone, capacity, avgSessionMinutes qua JOIN orders.tables + table_sessions
    public List<TableUsageDto> getTableUsage(LocalDate from, LocalDate to) {
        String sql = """
            SELECT t.number as table_number,
                   t.name as table_name,
                   t.zone,
                   t.capacity,
                   COUNT(o.id) as sessions_count,
                   COALESCE(SUM(o.total), 0) as total_revenue,
                   ROUND(
                       AVG(
                           EXTRACT(EPOCH FROM (ts.closed_at - ts.opened_at)) / 60
                       ), 1
                   ) as avg_session_minutes
            FROM orders.orders o
            JOIN orders.tables t ON o.table_id = t.id
            LEFT JOIN orders.table_sessions ts ON o.session_id = ts.id
            WHERE o.status = 'PAID' AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            GROUP BY t.id, t.number, t.name, t.zone, t.capacity
            ORDER BY total_revenue DESC
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> TableUsageDto.builder()
                .tableNumber(rs.getString("table_number"))
                .tableName(rs.getString("table_name"))
                .zone(rs.getString("zone"))
                .capacity(rs.getObject("capacity", Integer.class))
                .sessionsCount(rs.getLong("sessions_count"))
                .totalRevenue(rs.getBigDecimal("total_revenue"))
                .avgSessionMinutes(rs.getBigDecimal("avg_session_minutes"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // F6: Thêm cancelledOrders và cancelledRevenue
    public ShiftReportDto getCashierShiftReport(LocalDate shiftDate, UUID cashierId, UUID attendanceId) {
        String baseCond = cashierId != null ? " AND cashier_id = '" + cashierId + "'" : "";
        String cancelCond = cashierId != null ? " AND cancelled_by = '" + cashierId + "'" : "";

        String timeCondPaid = " AND DATE(COALESCE(paid_at, updated_at)) = ?";
        String timeCondCancel = " AND DATE(COALESCE(updated_at, created_at)) = ?";
        Object[] timeArgs = new Object[] { Date.valueOf(shiftDate) };
        
        if (attendanceId != null) {
            try {
                Map<String, Object> log = jdbcTemplate.queryForMap(
                    "SELECT check_in, COALESCE(check_out, CURRENT_TIMESTAMP) as check_out FROM auth.attendance_logs WHERE id = ?", attendanceId);
                
                java.sql.Timestamp checkIn = (java.sql.Timestamp) log.get("check_in");
                java.sql.Timestamp checkOut = (java.sql.Timestamp) log.get("check_out");
                
                timeCondPaid = " AND COALESCE(paid_at, updated_at) BETWEEN ? AND ?";
                timeCondCancel = " AND COALESCE(updated_at, created_at) BETWEEN ? AND ?";
                timeArgs = new Object[] { checkIn, checkOut };
            } catch (Exception e) {
                // fallback to shiftDate if attendance not found
            }
        }

        // Paid orders
        Long totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders.orders WHERE status = 'PAID'" + timeCondPaid + baseCond,
            Long.class, timeArgs);
        if (totalOrders == null) totalOrders = 0L;

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total), 0) FROM orders.orders WHERE status = 'PAID'" + timeCondPaid + baseCond,
            BigDecimal.class, timeArgs);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Cancelled orders stats
        Long cancelledOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders.orders o WHERE o.status = 'CANCELLED'" + timeCondCancel.replace("created_at", "o.created_at").replace("updated_at", "o.updated_at") + cancelCond.replace("cancelled_by", "o.cancelled_by"),
            Long.class, timeArgs);
        if (cancelledOrders == null) cancelledOrders = 0L;

        BigDecimal cancelledRevenue = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(" +
            "  oti.quantity * (" +
            "    oti.unit_price + " +
            "    COALESCE((SELECT SUM(oio.extra_price) FROM orders.order_item_options oio WHERE oio.ticket_item_id = oti.id), 0)" +
            "  )" +
            "), 0) " +
            "FROM orders.orders o " +
            "LEFT JOIN orders.order_tickets ot ON o.id = ot.order_id " +
            "LEFT JOIN orders.order_ticket_items oti ON ot.id = oti.ticket_id AND (oti.status = 'CANCELLED' OR oti.status = 'RETURNED') " +
            "WHERE o.status = 'CANCELLED'" + 
            timeCondCancel.replace("created_at", "o.created_at").replace("updated_at", "o.updated_at") + 
            cancelCond.replace("cancelled_by", "o.cancelled_by"),
            BigDecimal.class, timeArgs);
        if (cancelledRevenue == null) cancelledRevenue = BigDecimal.ZERO;

        // Revenue by payment method
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        Map<String, Long> ordersMap = new HashMap<>();
        jdbcTemplate.query(
            "SELECT payment_method, COUNT(id) as transaction_count, SUM(total) as amount " +
            "FROM orders.orders " +
            "WHERE status = 'PAID'" + timeCondPaid + baseCond +
            " GROUP BY payment_method",
            (rs) -> {
                String method = rs.getString("payment_method");
                if (method == null) method = "UNKNOWN";
                revenueMap.put(method, rs.getBigDecimal("amount"));
                ordersMap.put(method, rs.getLong("transaction_count"));
            }, timeArgs);

        return ShiftReportDto.builder()
                .shiftDate(shiftDate)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .revenueByPaymentMethod(revenueMap)
                .ordersByPaymentMethod(ordersMap)
                .cancelledOrders(cancelledOrders)
                .cancelledRevenue(cancelledRevenue)
                .build();
    }

    // N2: Hiệu quả khuyến mãi
    public PageResponse<PromotionEffectivenessDto> getPromotionEffectiveness(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            SELECT o.promotion_code,
                   COUNT(o.id) as order_count,
                   COALESCE(SUM(o.discount), 0) as total_discount_given,
                   SUM(o.total) as gross_revenue,
                   ROUND(AVG(o.total), 0) as avg_order_value
            FROM orders.orders o
            WHERE o.status = 'PAID'
              AND o.promotion_code IS NOT NULL
              AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            GROUP BY o.promotion_code
            """;

        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY order_count DESC\nOFFSET ? LIMIT ?";
        List<PromotionEffectivenessDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> PromotionEffectivenessDto.builder()
                .promotionCode(rs.getString("promotion_code"))
                .orderCount(rs.getLong("order_count"))
                .totalDiscountGiven(rs.getBigDecimal("total_discount_given"))
                .grossRevenue(rs.getBigDecimal("gross_revenue"))
                .avgOrderValue(rs.getBigDecimal("avg_order_value"))
                .build(),
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    // N3: Thống kê bàn gọi nhân viên
    public PageResponse<StaffCallStatsDto> getStaffCallStats(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            SELECT t.number as table_number,
                   sc.call_type,
                   COUNT(sc.id) as call_count,
                   COALESCE(ROUND(AVG(
                       EXTRACT(EPOCH FROM (sc.resolved_at - sc.created_at)) / 60
                   ), 1), 0) as avg_resolve_minutes
            FROM orders.staff_calls sc
            JOIN orders.tables t ON sc.table_id = t.id
            WHERE DATE(sc.created_at) BETWEEN ? AND ?
            GROUP BY t.number, sc.call_type
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY call_count DESC\nOFFSET ? LIMIT ?";
        List<StaffCallStatsDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> StaffCallStatsDto.builder()
                .tableNumber(rs.getString("table_number"))
                .callType(rs.getString("call_type"))
                .callCount(rs.getLong("call_count"))
                .avgResolveMinutes(rs.getBigDecimal("avg_resolve_minutes"))
                .build(),
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    // 1.4: Hiệu suất bếp — đo thời gian làm món và tỷ lệ trễ ticket
    public PageResponse<KitchenPerformanceDto> getKitchenPerformance(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            SELECT oti.item_name,
                   COUNT(oti.id) as total_tickets,
                   COALESCE(ROUND(AVG(
                       EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at)) / 60
                   ), 1), 0) as avg_prep_minutes,
                   COUNT(CASE WHEN
                       EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at)) / 60 > 15
                       THEN 1 END) as late_tickets
            FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders.orders o ON ot.order_id = o.id
            WHERE oti.status IN ('DONE', 'SERVED')
              AND oti.completed_at IS NOT NULL
              AND DATE(o.created_at) BETWEEN ? AND ?
            GROUP BY oti.item_name
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY avg_prep_minutes DESC NULLS LAST\nOFFSET ? LIMIT ?";
        List<KitchenPerformanceDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                long totalTickets = rs.getLong("total_tickets");
                long late = rs.getLong("late_tickets");
                double lateRate = totalTickets > 0 ? Math.round((double) late / totalTickets * 1000.0) / 10.0 : 0;
                return KitchenPerformanceDto.builder()
                    .itemName(rs.getString("item_name"))
                    .totalTickets(totalTickets)
                    .avgPrepMinutes(rs.getBigDecimal("avg_prep_minutes"))
                    .lateTickets(late)
                    .lateRate(lateRate)
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<CancelledOrderDrilldownDto> getCancelledOrderDrilldown(LocalDate from, LocalDate to, int page, int size) {
        // Tổng đơn trong kỳ (để tính tỷ lệ)
        Long totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders.orders WHERE DATE(COALESCE(created_at, updated_at)) BETWEEN ? AND ?",
            Long.class, Date.valueOf(from), Date.valueOf(to)
        );
        final long totalFinal = totalOrders != null && totalOrders > 0 ? totalOrders : 1L;

        String baseSql = """
            SELECT COALESCE(o.cancel_reason, 'UNKNOWN') as cancel_reason,
                   COUNT(DISTINCT o.id) as cancel_count,
                   COALESCE(SUM(
                       oti.quantity * (
                           oti.unit_price + 
                           COALESCE((SELECT SUM(oio.extra_price) FROM orders.order_item_options oio WHERE oio.ticket_item_id = oti.id), 0)
                       )
                   ), 0) as cancelled_revenue
            FROM orders.orders o
            LEFT JOIN orders.order_tickets ot ON o.id = ot.order_id
            LEFT JOIN orders.order_ticket_items oti ON ot.id = oti.ticket_id AND (oti.status = 'CANCELLED' OR oti.status = 'RETURNED')
            WHERE o.status = 'CANCELLED'
              AND DATE(COALESCE(o.updated_at, o.created_at)) BETWEEN ? AND ?
            GROUP BY COALESCE(o.cancel_reason, 'UNKNOWN')
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY cancel_count DESC\nOFFSET ? LIMIT ?";
        
        List<CancelledOrderDrilldownDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                long count = rs.getLong("cancel_count");
                double rate = Math.round((double) count / totalFinal * 1000.0) / 10.0;
                return CancelledOrderDrilldownDto.builder()
                    .cancellationReason(rs.getString("cancel_reason"))
                    .cancelCount(count)
                    .cancelledRevenue(rs.getBigDecimal("cancelled_revenue"))
                    .cancelRate(rate)
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<ChefPerformanceDto> getChefPerformance(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            SELECT 
                oti.prepared_by as chef_id,
                COALESCE(u.full_name, 'Unknown Chef') as chef_name,
                SUM(oti.quantity) as total_items,
                COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at))/60.0), 1), 0) as avg_prep_minutes,
                COUNT(CASE WHEN EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at))/60.0 > 15 THEN 1 END) as late_items
            FROM orders.order_ticket_items oti
            LEFT JOIN auth.users u ON oti.prepared_by = u.id
            WHERE DATE(oti.completed_at) BETWEEN ? AND ?
              AND oti.status IN ('DONE', 'SERVED')
            GROUP BY oti.prepared_by, u.full_name
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY total_items DESC\nOFFSET ? LIMIT ?";
        List<ChefPerformanceDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                int totalItems = rs.getInt("total_items");
                int lateItems = rs.getInt("late_items");
                double lateRate = totalItems > 0 ? Math.round((double) lateItems / totalItems * 1000.0) / 10.0 : 0.0;
                return ChefPerformanceDto.builder()
                    .chefId((UUID) rs.getObject("chef_id"))
                    .chefName(rs.getString("chef_name"))
                    .totalItemsPrepared(totalItems)
                    .avgPrepMinutes(rs.getDouble("avg_prep_minutes"))
                    .lateItemCount(lateItems)
                    .lateRate(lateRate)
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<ServerPerformanceDto> getServerPerformance(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            WITH calls AS (
                SELECT 
                    sc.resolved_by as server_id,
                    COUNT(sc.id) as total_calls,
                    COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM (sc.accepted_at - sc.created_at))), 1), 0) as avg_response_seconds,
                    COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM (sc.resolved_at - sc.created_at))/60.0), 1), 0) as avg_resolution_minutes
                FROM orders.staff_calls sc
                WHERE DATE(sc.resolved_at) BETWEEN ? AND ?
                  AND sc.status = 'RESOLVED'
                GROUP BY sc.resolved_by
            ),
            deliveries AS (
                SELECT 
                    oti.served_by as server_id,
                    COUNT(oti.id) as total_items_served,
                    COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM (oti.served_at - COALESCE(oti.completed_at, oti.created_at)))), 1), 0) as avg_delivery_seconds
                FROM orders.order_ticket_items oti
                WHERE DATE(oti.served_at) BETWEEN ? AND ?
                  AND oti.status = 'SERVED'
                GROUP BY oti.served_by
            )
            SELECT 
                COALESCE(c.server_id, d.server_id) as server_id,
                COALESCE(u.full_name, 'Unknown Server') as server_name,
                COALESCE(c.total_calls, 0) as total_calls,
                COALESCE(c.avg_response_seconds, 0) as avg_response_seconds,
                COALESCE(c.avg_resolution_minutes, 0) as avg_resolution_minutes,
                COALESCE(d.total_items_served, 0) as total_items_served,
                COALESCE(d.avg_delivery_seconds, 0) as avg_delivery_seconds
            FROM calls c
            FULL OUTER JOIN deliveries d ON c.server_id = d.server_id
            LEFT JOIN auth.users u ON COALESCE(c.server_id, d.server_id) = u.id
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY (COALESCE(total_calls, 0) + COALESCE(total_items_served, 0)) DESC\nOFFSET ? LIMIT ?";
        List<ServerPerformanceDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> ServerPerformanceDto.builder()
                .serverId((UUID) rs.getObject("server_id"))
                .serverName(rs.getString("server_name"))
                .totalCallsResolved(rs.getInt("total_calls"))
                .avgResponseSeconds(rs.getDouble("avg_response_seconds"))
                .avgResolutionMinutes(rs.getDouble("avg_resolution_minutes"))
                .totalItemsServed(rs.getInt("total_items_served"))
                .avgDeliverySeconds(rs.getDouble("avg_delivery_seconds"))
                .build(),
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<CategorySalesDto> getCategorySales(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            WITH CategoryTotals AS (
                SELECT 
                    c.id as category_id,
                    c.name as category_name,
                    SUM(oti.quantity) as total_quantity,
                    SUM(oti.quantity * oti.unit_price) as total_revenue,
                    SUM(oti.tax_amount) as total_tax
                FROM orders.order_ticket_items oti
                JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
                JOIN orders.orders o ON ot.order_id = o.id
                JOIN menu.menu_items mi ON oti.menu_item_id = mi.id
                JOIN menu.categories c ON mi.category_id = c.id
                WHERE o.status = 'PAID'
                  AND oti.status NOT IN ('CANCELLED', 'RETURNED')
                  AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
                GROUP BY c.id, c.name
            ),
            TotalRevenue AS (
                SELECT NULLIF(SUM(total_revenue), 0) as grand_total FROM CategoryTotals
            )
            SELECT 
                ct.*,
                ROUND(CAST(ct.total_revenue / tr.grand_total * 100 AS numeric), 2) as percentage
            FROM CategoryTotals ct, TotalRevenue tr
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, Date.valueOf(from), Date.valueOf(to));

        String sql = baseSql + "\nORDER BY ct.total_revenue DESC\nOFFSET ? LIMIT ?";
        List<CategorySalesDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                BigDecimal gross = rs.getBigDecimal("total_revenue");
                BigDecimal tax = rs.getBigDecimal("total_tax");
                if (tax == null) tax = BigDecimal.ZERO;
                return CategorySalesDto.builder()
                    .categoryId((UUID) rs.getObject("category_id"))
                    .categoryName(rs.getString("category_name"))
                    .totalQuantitySold(rs.getInt("total_quantity"))
                    .totalRevenue(gross)
                    .totalTax(tax)
                    .totalNetRevenue(gross.subtract(tax))
                    .revenuePercentage(rs.getDouble("percentage"))
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public PageResponse<StaffTimesheetDto> getStaffTimesheet(LocalDate from, LocalDate to, int page, int size) {
        String baseSql = """
            WITH ShiftStats AS (
                SELECT 
                    user_id,
                    COUNT(id) as total_shifts,
                    SUM(EXTRACT(EPOCH FROM (COALESCE(check_out, CURRENT_TIMESTAMP) - check_in))/3600.0) as total_hours
                FROM auth.attendance_logs
                WHERE DATE(check_in) BETWEEN ? AND ?
                GROUP BY user_id
            ),
            SalesStats AS (
                SELECT 
                    cashier_id,
                    SUM(total) as total_revenue
                FROM orders.orders
                WHERE status = 'PAID'
                  AND DATE(COALESCE(paid_at, updated_at)) BETWEEN ? AND ?
                GROUP BY cashier_id
            ),
            KitchenStats AS (
                SELECT 
                    oti.prepared_by as user_id,
                    SUM(oti.quantity) as items_prepared
                FROM orders.order_ticket_items oti
                WHERE oti.status IN ('DONE', 'SERVED')
                  AND oti.prepared_by IS NOT NULL
                  AND DATE(oti.completed_at) BETWEEN ? AND ?
                GROUP BY oti.prepared_by
            ),
            ServerStats AS (
                SELECT 
                    sc.resolved_by as user_id,
                    COUNT(sc.id) as calls_resolved
                FROM orders.staff_calls sc
                WHERE sc.status = 'RESOLVED'
                  AND sc.resolved_by IS NOT NULL
                  AND DATE(sc.resolved_at) BETWEEN ? AND ?
                GROUP BY sc.resolved_by
            )
            SELECT 
                u.id as staff_id,
                u.full_name as staff_name,
                u.role,
                COALESCE(ss.total_shifts, 0) as total_shifts,
                COALESCE(ss.total_hours, 0) as total_hours,
                CASE 
                    WHEN u.role = 'CASHIER' THEN COALESCE(sa.total_revenue, 0)
                    ELSE 0
                END as total_revenue,
                COALESCE(ks.items_prepared, 0) as items_prepared,
                COALESCE(svs.calls_resolved, 0) as calls_resolved
            FROM auth.users u
            LEFT JOIN ShiftStats ss ON u.id = ss.user_id
            LEFT JOIN SalesStats sa ON u.id = sa.cashier_id
            LEFT JOIN KitchenStats ks ON u.id = ks.user_id
            LEFT JOIN ServerStats svs ON u.id = svs.user_id
            WHERE u.role IN ('CASHIER', 'SERVER', 'KITCHEN') 
              AND (ss.total_shifts > 0 OR sa.total_revenue > 0 OR ks.items_prepared > 0 OR svs.calls_resolved > 0)
            """;
            
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS sub";
        Long total = jdbcTemplate.queryForObject(
            countSql, Long.class, 
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to),
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to)
        );

        String sql = baseSql + "\nORDER BY total_hours DESC, total_revenue DESC\nOFFSET ? LIMIT ?";
        List<StaffTimesheetDto> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                double hours = rs.getDouble("total_hours");
                BigDecimal revenue = rs.getBigDecimal("total_revenue");
                String role = rs.getString("role");
                long itemsPrepared = rs.getLong("items_prepared");
                long callsResolved = rs.getLong("calls_resolved");
                
                BigDecimal rph = BigDecimal.ZERO;
                if ("CASHIER".equals(role) && hours > 0 && revenue.compareTo(BigDecimal.ZERO) > 0) {
                    rph = revenue.divide(BigDecimal.valueOf(hours), 2, RoundingMode.HALF_UP);
                }
                return StaffTimesheetDto.builder()
                    .staffId((UUID) rs.getObject("staff_id"))
                    .staffName(rs.getString("staff_name"))
                    .role(role)
                    .totalShifts(rs.getInt("total_shifts"))
                    .totalWorkingHours(Math.round(hours * 10.0) / 10.0)
                    .totalRevenue(revenue)
                    .revenuePerHour(rph)
                    .itemsPrepared(itemsPrepared)
                    .callsResolved(callsResolved)
                    .build();
            },
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to),
            Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to),
            page * size, size
        );
        return PageResponse.of(list, page, size, total != null ? total : 0);
    }

    public ReservationReportDto getReservationReport(LocalDate from, LocalDate to) {
        String sql = """
            SELECT 
                COUNT(*) as total_reservations,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as total_completed,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as total_cancelled,
                COUNT(CASE WHEN status = 'NO_SHOW' THEN 1 END) as total_no_show,
                COALESCE(SUM(deposit_amount), 0) as total_deposits,
                COALESCE(SUM(CASE WHEN status = 'CANCELLED' AND refund_status = 'PENDING_REFUND' THEN deposit_amount ELSE 0 END), 0) as pending_refund,
                COALESCE(SUM(CASE WHEN status = 'CANCELLED' AND refund_status = 'REFUNDED' THEN deposit_amount ELSE 0 END), 0) as refunded,
                COALESCE(SUM(CASE WHEN (status = 'CANCELLED' OR status = 'NO_SHOW') AND refund_status = 'NOT_REQUIRED' THEN deposit_amount ELSE 0 END), 0) as forfeited
            FROM orders.reservations
            WHERE DATE(booking_time) BETWEEN ? AND ?
            """;

        ReservationReportDto report = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> ReservationReportDto.builder()
                .totalReservations(rs.getLong("total_reservations"))
                .totalCompleted(rs.getLong("total_completed"))
                .totalCancelled(rs.getLong("total_cancelled"))
                .totalNoShow(rs.getLong("total_no_show"))
                .totalDeposits(rs.getBigDecimal("total_deposits"))
                .pendingRefund(rs.getBigDecimal("pending_refund"))
                .refunded(rs.getBigDecimal("refunded"))
                .forfeited(rs.getBigDecimal("forfeited"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );

        String trendSql = """
            SELECT 
                DATE(booking_time) as booking_date,
                COUNT(*) as total_reservations,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as total_completed,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as total_cancelled
            FROM orders.reservations
            WHERE DATE(booking_time) BETWEEN ? AND ?
            GROUP BY DATE(booking_time)
            ORDER BY DATE(booking_time)
            """;
            
        List<DailyReservationTrendDto> trend = jdbcTemplate.query(
            trendSql,
            (rs, rowNum) -> DailyReservationTrendDto.builder()
                .day(rs.getDate("booking_date").toString())
                .totalReservations(rs.getLong("total_reservations"))
                .totalCompleted(rs.getLong("total_completed"))
                .totalCancelled(rs.getLong("total_cancelled"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
        
        if (report != null) {
            report.setDailyTrend(trend);
        }
        
        return report;
    }
}
