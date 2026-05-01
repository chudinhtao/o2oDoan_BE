package com.fnb.order.scheduler;

import com.fnb.order.client.MenuServiceClient;
import com.fnb.order.repository.OrderRepository;
import com.fnb.order.repository.OrderTicketRepository;
import com.fnb.order.repository.StaffCallRepository;
import com.fnb.order.service.PayOSPaymentService;
import com.fnb.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final StaffCallRepository staffCallRepository;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final MenuServiceClient menuServiceClient;
    private final PayOSPaymentService payOSPaymentService;
    private final com.fnb.order.repository.TableRepository tableRepository;
    private final com.fnb.order.repository.TableSessionRepository tableSessionRepository;

    /**
     * Dọn dẹp các yêu cầu gọi nhân viên đã xử lý xong quá 7 ngày.
     * Chạy vào lúc 3 giờ sáng mỗi ngày.
     *
     * Tối ưu: dùng bulk DELETE (1 câu SQL WHERE) thay vì findAll() + Java filter + delete từng cái.
     * Không load bất kỳ entity nào vào bộ nhớ.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldStaffCalls() {
        int deleted = staffCallRepository.deleteOldResolvedCalls(
                LocalDateTime.now().minusDays(7)
        );
        if (deleted > 0) {
            log.info("Scheduler: Đã xóa {} bản ghi StaffCall cũ (đã xử lý > 7 ngày).", deleted);
        }
    }

    /**
     * Fallback bảo vệ hệ thống: Đồng bộ trạng thái thanh toán PayOS.
     * Chạy mỗi 5 phút. Quét các đơn đang kẹt ở PAYMENT_REQUESTED để kiểm tra trực tiếp với cổng thanh toán.
     */
    @Scheduled(fixedRate = 300_000)
    public void syncPendingPaymentsJob() {
        // Chỉ quét các hoá đơn Payment_Requested bằng PayOS. (Khách có thể đổi ý sang trả tiền mặt)
        List<Order> pendingOrders = orderRepository.findByStatusAndPaymentMethod("PAYMENT_REQUESTED", "PayOS");

        if (!pendingOrders.isEmpty()) {
            log.info("PayOS Sync Job: Bắt đầu dò tìm {} đơn hàng có nguy cơ miss webhook...", pendingOrders.size());
            for (Order order : pendingOrders) {
                // Ta chỉ sync các đơn đã Request được > 5 phút (tránh request API quá rát khi khách vừa ấn tạo)
                if (order.getUpdatedAt() != null && order.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                    payOSPaymentService.syncPendingPayment(order);
                }
            }
        }
    }

    /**
     * Dọn dẹp hàng ngày: chạy lúc 2h sáng mỗi ngày.
     * 1. Xóa các đơn hàng hoàn toàn trống (không có ticket) để giảm tải DB.
     * 2. Tìm top 5 món bán chạy nhất của ngày hôm trước và cập nhật danh sách isFeatured.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runDailyMaintenance() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xóa đơn hàng trống tồn đọng quá 1 tiếng
        int deletedOrders = orderRepository.deleteEmptyOrders(now.minusHours(1));
        if (deletedOrders > 0) {
            log.info("Scheduler: Đã xóa {} đơn hàng trống (0đ/không có món).", deletedOrders);
        }

        // 1.1 Dọn bàn cuối ngày (End of Day Process)
        // Reset toàn bộ bàn về FREE và đóng tất cả các Session đang ACTIVE
        int closedSessions = tableSessionRepository.closeAllActiveSessions();
        int freedTables = tableRepository.resetAllTablesToFree();
        log.info("Scheduler [EOD]: Đã đóng {} session đang ACTIVE và reset {} bàn về trạng thái FREE.", closedSessions, freedTables);

        // 2. Tìm top món bán chạy (tính từ 00:00 ngày hôm qua)
        LocalDateTime startOfYesterday = now.minusDays(1).toLocalDate().atStartOfDay();
        List<UUID> topItems = orderTicketRepository.findTopSellingItemIds(startOfYesterday, 5);

        if (!topItems.isEmpty()) {
            log.info("Scheduler: Tìm thấy {} món bán chạy nhất hôm qua.", topItems.size());
            try {
                menuServiceClient.updateFeaturedItems(topItems);
                log.info("Scheduler: Đã cập nhật isFeatured cho các món bán chạy sang menu-service.");
            } catch (Exception e) {
                log.error("Scheduler: Lỗi khi gọi menu-service updateFeaturedItems", e);
            }
        } else {
            log.info("Scheduler: Không có món nào bán được hôm qua, bỏ qua cập nhật isFeatured.");
        }
    }

    /**
     * Quét và dọn dẹp các session đã quá hạn 4 tiếng.
     * Chạy mỗi 30 phút một lần.
     */
    @Scheduled(fixedRate = 1800_000)
    @Transactional
    public void cleanupExpiredSessions() {
        int freedTables = tableRepository.resetTablesForExpiredSessions();
        int closedSessions = tableSessionRepository.closeExpiredSessions();
        if (closedSessions > 0 || freedTables > 0) {
            log.info("Scheduler: Đã đóng {} session hết hạn (> 4 tiếng) và giải phóng {} bàn.", closedSessions, freedTables);
        }
    }
}
