package com.fnb.order.scheduler;

import com.fnb.order.client.MenuServiceClient;
import com.fnb.order.repository.OrderRepository;
import com.fnb.order.repository.OrderTicketRepository;
import com.fnb.order.repository.StaffCallRepository;
import com.fnb.order.service.PayOSPaymentService;
import com.fnb.order.entity.Order;
import com.fnb.order.entity.StaffCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fnb.order.repository.TableRepository;
import com.fnb.order.repository.TableSessionRepository;
import com.fnb.order.dto.event.StaffCallCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.fnb.order.entity.TableSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final StaffCallRepository staffCallRepository;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final MenuServiceClient menuServiceClient;
    private final PayOSPaymentService payOSPaymentService;
    private final TableRepository tableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

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
    @EventListener(ApplicationReadyEvent.class)
    public void syncPendingPaymentsJob() {
        // Chỉ quét các hoá đơn Payment_Requested bằng PayOS trong 24 giờ qua (tránh rate limit).
        List<Order> pendingOrders = orderRepository.findTop50ByStatusAndPaymentMethodAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(
                "PAYMENT_REQUESTED", "PayOS", LocalDateTime.now().minusHours(24));

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
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runDailyMaintenance() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xóa đơn hàng trống tồn đọng quá 1 tiếng
        int deletedOrders = orderRepository.deleteEmptyOrders(now.minusHours(1));
        if (deletedOrders > 0) {
            log.info("Scheduler: Đã xóa {} đơn hàng trống (0đ/không có món).", deletedOrders);
        }

        // 1.1 Dọn bàn cuối ngày (End of Day Process)
        // Hard-reset đã bị gỡ bỏ để tránh đá văng khách nếu quán mở cửa quá khuya.
        // Việc dọn dẹp session hết hạn đã được giao phó toàn quyền cho cleanupExpiredSessions() mỗi 30 phút.

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
     * Giám sát các bàn ăn (Session Watcher) - Nhắc việc cho POS thay vì tự động xóa.
     * Chạy mỗi 5 phút một lần.
     */
    @Scheduled(fixedRate = 300_000)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void sessionWatcherJob() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Bàn rỗng (Mở > 1 tiếng nhưng 0đ)
        List<TableSession> emptySessions = tableSessionRepository.findEmptySessions(now.minusHours(1));
        for (TableSession s : emptySessions) {
            publishStaffCall(s, "EMPTY_SESSION_ALERT");
        }

        // 2. Bàn quên dọn (Thanh toán xong > 30 phút)
        List<TableSession> readySessions = tableSessionRepository.findSessionsReadyForCleanup(now.minusMinutes(30));
        for (TableSession s : readySessions) {
            publishStaffCall(s, "TABLE_CLEANUP_REMINDER");
        }

        // 3. Khách ngồi dai (Mở > 4 tiếng, còn đơn chưa thanh toán)
        List<TableSession> longSessions = tableSessionRepository.findLongRunningSessions(now.minusHours(4));
        for (TableSession s : longSessions) {
            publishStaffCall(s, "LONG_SESSION_ALERT");
        }
    }

    /**
     * Giám sát các đơn treo thanh toán PayOS (Order Watcher).
     * Chạy mỗi 5 phút.
     */
    @Scheduled(fixedRate = 300_000)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void watchPendingOrders() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Cảnh báo Takeaway kẹt > 15 phút
        List<Order> takeawayOverdue = orderRepository.findOverdueTakeawayPayments(now.minusMinutes(15));
        for (Order o : takeawayOverdue) {
            publishStaffCall(o.getSession(), "TAKEAWAY_TIMEOUT");
        }

        // 2. Cảnh báo Dine-in kẹt > 30 phút
        List<Order> dineInOverdue = orderRepository.findOverdueDineInPayments(now.minusMinutes(30));
        for (Order o : dineInOverdue) {
            publishStaffCall(o.getSession(), "DINE_IN_PAYMENT_ALERT");
        }
    }

    private void publishStaffCall(TableSession session, String callType) {
        if (session == null) return;
        
        // Prevent spam: Check if an active call of this type already exists for the session
        if (staffCallRepository.existsBySessionIdAndCallTypeAndStatus(session.getId(), callType, "PENDING") ||
            staffCallRepository.existsBySessionIdAndCallTypeAndStatus(session.getId(), callType, "ACCEPTED")) {
            return;
        }

        StaffCall call = StaffCall.builder()
                .session(session)
                .table(session.getTable())
                .callType(callType)
                .status("PENDING")
                .message("Hệ thống: " + callType)
                .build();
        staffCallRepository.save(call);

        StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                .callId(call.getId())
                .sessionId(session.getId())
                .tableId(session.getTable() != null ? session.getTable().getId() : null)
                .tableNumber(session.getTable() != null ? session.getTable().getNumber() : null)
                .callType(callType)
                .calledAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);
        log.info("Alert Generator: Đã lưu và bắn cảnh báo {} cho bàn {}", callType, session.getTable() != null ? session.getTable().getNumber() : "Mang về");
    }
}
