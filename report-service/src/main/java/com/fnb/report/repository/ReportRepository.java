package com.fnb.report.repository;

import com.fnb.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
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
                   COUNT(*) as orders,
                   ROUND(AVG(total), 0) as avg_order_value
            FROM orders
            WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) BETWEEN ? AND ?
            GROUP BY DATE(COALESCE(paid_at, updated_at))
            ORDER BY day
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> RevenueDto.builder()
                .day(rs.getDate("day").toLocalDate())
                .revenue(rs.getBigDecimal("revenue"))
                .totalOrders(rs.getLong("orders"))
                .avgOrderValue(rs.getBigDecimal("avg_order_value"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // F2: Thêm sortBy param (QUANTITY hoặc REVENUE)
    public List<TopItemDto> getTopItems(int limit, LocalDate from, LocalDate to, String sortBy) {
        String orderClause = "REVENUE".equalsIgnoreCase(sortBy) ? "revenue DESC" : "total_sold DESC";
        String sql = """
            SELECT oti.item_name, SUM(oti.quantity) as total_sold, SUM(oti.unit_price * oti.quantity) as revenue
            FROM order_ticket_items oti
            JOIN order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders o ON ot.order_id = o.id
            WHERE o.status = 'PAID'
              AND oti.status NOT IN ('CANCELLED', 'RETURNED')
              AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            GROUP BY oti.item_name
            ORDER BY
            """ + orderClause + """
            \nLIMIT ?
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> TopItemDto.builder()
                .itemName(rs.getString("item_name"))
                .totalSold(rs.getLong("total_sold"))
                .revenue(rs.getBigDecimal("revenue"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to),
            limit
        );
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

    // F5: Thêm zone, capacity, avgSessionMinutes qua JOIN tables + table_sessions
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
            FROM orders o
            JOIN tables t ON o.table_id = t.id
            LEFT JOIN table_sessions ts ON o.session_id = ts.id
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
    public ShiftReportDto getCashierShiftReport(LocalDate shiftDate) {
        // Paid orders
        Long totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) = ?",
            Long.class, Date.valueOf(shiftDate));
        if (totalOrders == null) totalOrders = 0L;

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) = ?",
            BigDecimal.class, Date.valueOf(shiftDate));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Cancelled orders stats
        Long cancelledOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE status = 'CANCELLED' AND DATE(COALESCE(updated_at, created_at)) = ?",
            Long.class, Date.valueOf(shiftDate));
        if (cancelledOrders == null) cancelledOrders = 0L;

        BigDecimal cancelledRevenue = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status = 'CANCELLED' AND DATE(COALESCE(updated_at, created_at)) = ?",
            BigDecimal.class, Date.valueOf(shiftDate));
        if (cancelledRevenue == null) cancelledRevenue = BigDecimal.ZERO;

        // Payment method breakdown
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        Map<String, Long> ordersMap = new HashMap<>();
        jdbcTemplate.query("""
            SELECT payment_method, COUNT(id) as transaction_count, SUM(total) as amount
            FROM orders
            WHERE status = 'PAID' AND DATE(COALESCE(paid_at, updated_at)) = ?
            GROUP BY payment_method
            """, rs -> {
            String method = rs.getString("payment_method");
            if (method == null) method = "CASH";
            revenueMap.put(method, rs.getBigDecimal("amount"));
            ordersMap.put(method, rs.getLong("transaction_count"));
        }, Date.valueOf(shiftDate));

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
    public List<PromotionEffectivenessDto> getPromotionEffectiveness(LocalDate from, LocalDate to) {
        String sql = """
            SELECT o.promotion_code,
                   COUNT(o.id) as order_count,
                   COALESCE(SUM(o.discount), 0) as total_discount_given,
                   SUM(o.total) as gross_revenue,
                   ROUND(AVG(o.total), 0) as avg_order_value
            FROM orders o
            WHERE o.status = 'PAID'
              AND o.promotion_code IS NOT NULL
              AND DATE(COALESCE(o.paid_at, o.updated_at)) BETWEEN ? AND ?
            GROUP BY o.promotion_code
            ORDER BY order_count DESC
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> PromotionEffectivenessDto.builder()
                .promotionCode(rs.getString("promotion_code"))
                .orderCount(rs.getLong("order_count"))
                .totalDiscountGiven(rs.getBigDecimal("total_discount_given"))
                .grossRevenue(rs.getBigDecimal("gross_revenue"))
                .avgOrderValue(rs.getBigDecimal("avg_order_value"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // N3: Thống kê bàn gọi nhân viên
    public List<StaffCallStatsDto> getStaffCallStats(LocalDate from, LocalDate to) {
        String sql = """
            SELECT t.number as table_number,
                   sc.call_type,
                   COUNT(sc.id) as call_count,
                   ROUND(AVG(
                       EXTRACT(EPOCH FROM (sc.resolved_at - sc.called_at)) / 60
                   ), 1) as avg_resolve_minutes
            FROM staff_calls sc
            JOIN tables t ON sc.table_id = t.id
            WHERE DATE(sc.called_at) BETWEEN ? AND ?
            GROUP BY t.number, sc.call_type
            ORDER BY call_count DESC
            LIMIT 20
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> StaffCallStatsDto.builder()
                .tableNumber(rs.getString("table_number"))
                .callType(rs.getString("call_type"))
                .callCount(rs.getLong("call_count"))
                .avgResolveMinutes(rs.getBigDecimal("avg_resolve_minutes"))
                .build(),
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // 1.4: Hiệu suất bếp — đo thời gian làm món và tỷ lệ trễ ticket
    public List<KitchenPerformanceDto> getKitchenPerformance(LocalDate from, LocalDate to) {
        String sql = """
            SELECT oti.item_name,
                   COUNT(oti.id) as total_tickets,
                   ROUND(AVG(
                       EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at)) / 60
                   ), 1) as avg_prep_minutes,
                   COUNT(CASE WHEN
                       EXTRACT(EPOCH FROM (oti.completed_at - oti.created_at)) / 60 > 15
                       THEN 1 END) as late_tickets
            FROM order_ticket_items oti
            JOIN order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders o ON ot.order_id = o.id
            WHERE oti.status = 'DONE'
              AND oti.completed_at IS NOT NULL
              AND DATE(o.created_at) BETWEEN ? AND ?
            GROUP BY oti.item_name
            ORDER BY avg_prep_minutes DESC NULLS LAST
            LIMIT 20
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                long total = rs.getLong("total_tickets");
                long late = rs.getLong("late_tickets");
                double lateRate = total > 0 ? Math.round((double) late / total * 1000.0) / 10.0 : 0;
                return KitchenPerformanceDto.builder()
                    .itemName(rs.getString("item_name"))
                    .totalTickets(total)
                    .avgPrepMinutes(rs.getBigDecimal("avg_prep_minutes"))
                    .lateTickets(late)
                    .lateRate(lateRate)
                    .build();
            },
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }

    // 1.4: Chi tiết đơn hủy theo lý do
    public List<CancelledOrderDrilldownDto> getCancelledOrderDrilldown(LocalDate from, LocalDate to) {
        // Tổng đơn trong kỳ (để tính tỷ lệ)
        Long totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE DATE(COALESCE(created_at, updated_at)) BETWEEN ? AND ?",
            Long.class, Date.valueOf(from), Date.valueOf(to)
        );
        long total = totalOrders != null ? totalOrders : 1L;

        String sql = """
            SELECT COALESCE(cancellation_reason, 'UNKNOWN') as cancellation_reason,
                   COUNT(id) as cancel_count,
                   COALESCE(SUM(total), 0) as cancelled_revenue
            FROM orders
            WHERE status = 'CANCELLED'
              AND DATE(COALESCE(updated_at, created_at)) BETWEEN ? AND ?
            GROUP BY COALESCE(cancellation_reason, 'UNKNOWN')
            ORDER BY cancel_count DESC
            """;

        final long totalFinal = total;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                long count = rs.getLong("cancel_count");
                double rate = Math.round((double) count / totalFinal * 1000.0) / 10.0;
                return CancelledOrderDrilldownDto.builder()
                    .cancellationReason(rs.getString("cancellation_reason"))
                    .cancelCount(count)
                    .cancelledRevenue(rs.getBigDecimal("cancelled_revenue"))
                    .cancelRate(rate)
                    .build();
            },
            Date.valueOf(from),
            Date.valueOf(to)
        );
    }
}
