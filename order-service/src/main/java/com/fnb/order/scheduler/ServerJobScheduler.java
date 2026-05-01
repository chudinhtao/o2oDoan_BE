package com.fnb.order.scheduler;

import com.fnb.order.dto.event.DeliveryReadyAlertEvent;
import com.fnb.order.dto.event.StaffCallSpilloverEvent;
import com.fnb.order.entity.OrderTicketItem;
import com.fnb.order.entity.StaffCall;
import com.fnb.order.repository.OrderTicketItemRepository;
import com.fnb.order.repository.StaffCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sweeper Job — Fault Tolerance cho Server Role.
 *
 * Chạy mỗi 30 giây để:
 * 1. Tìm các món bếp đã làm xong (DONE) nhưng chưa được bưng → bắn cảnh báo.
 * 2. Tìm các StaffCall PENDING quá 30s mà chưa ai tiếp nhận → bắn spillover.
 *
 * Đây là lưới an toàn: nếu WebSocket bị mất kết nối hoặc message bị drop,
 * Sweeper đảm bảo Server vẫn nhận được cảnh báo sau tối đa 30 giây.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerJobScheduler {

    private final OrderTicketItemRepository ticketItemRepository;
    private final StaffCallRepository staffCallRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Ngưỡng cảnh báo cho món HOT: 240 giây (4 phút) */
    private static final int HOT_URGENCY_SECONDS = 240;
    /** Ngưỡng spillover cho StaffCall: 120 giây (2 phút) (Phase 2.2: ngâm quá 120s → cứu viện chéo) */
    private static final int CALL_SPILLOVER_SECONDS = 120;

    /**
     * Quét các món DONE chưa được bưng và bắn cảnh báo.
     * - HOT station: cảnh báo sau 60 giây
     * - COLD/DRINK: cảnh báo ngay (0 giây) - cần bưng càng sớm càng tốt
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional(readOnly = true)
    public void sweepOverdueDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy tất cả món DONE, chưa SERVED, đã được hoàn thành từ trước :threshold
        // Dùng threshold = now - 0s (tức là lấy tất) để tự xử lý logic theo station
        List<OrderTicketItem> overdueItems = ticketItemRepository.findOverdueDeliveries(now);

        if (overdueItems.isEmpty()) return;

        // Group by Order (hỗ trợ cả Bàn và Takeaway)
        Map<com.fnb.order.entity.Order, List<OrderTicketItem>> byOrder = overdueItems.stream()
                .filter(item -> item.getTicket() != null
                        && item.getTicket().getOrder() != null)
                .collect(Collectors.groupingBy(item -> item.getTicket().getOrder()));

        byOrder.forEach((order, items) -> {
            boolean isTakeawayOrDelivery = "TAKEAWAY".equals(order.getOrderType()) || "DELIVERY".equals(order.getOrderType());
            Integer tableNumber = order.getTable() != null ? order.getTable().getNumber() : null;

            List<UUID> urgentIds = items.stream()
                    .filter(item -> {
                        if (isTakeawayOrDelivery) {
                            boolean hasActiveItems = item.getTicket().getItems().stream()
                                    .anyMatch(i -> "PENDING".equals(i.getStatus()) || "PREPARING".equals(i.getStatus()));
                            if (hasActiveItems) return false; // Không hú còi nếu đơn Takeaway chưa xong 100%
                        }
                        return isUrgent(item, now);
                    })
                    .map(OrderTicketItem::getId)
                    .collect(Collectors.toList());

            if (!urgentIds.isEmpty()) {
                long maxWait = items.stream()
                        .mapToLong(i -> {
                            LocalDateTime readyTime = i.getCompletedAt() != null ? i.getCompletedAt() : i.getCreatedAt();
                            return ChronoUnit.SECONDS.between(readyTime, now);
                        })
                        .max().orElse(0);

                DeliveryReadyAlertEvent alert = DeliveryReadyAlertEvent.builder()
                        .tableNumber(tableNumber)
                        .tableId(order.getTable() != null ? order.getTable().getId() : null)
                        .zone(order.getTable() != null ? order.getTable().getZone() : "Takeaway")
                        .urgentItemIds(urgentIds)
                        .urgencyLevel(maxWait >= 120 ? "CRITICAL" : "WARNING")
                        .alertAt(now)
                        .build();

                kafkaTemplate.send("delivery.ready.alert", tableNumber.toString(), alert);
                log.warn("Sweeper [DELIVERY]: Bàn {} có {} món trễ ({} giây). Level: {}",
                        tableNumber, urgentIds.size(), maxWait, alert.getUrgencyLevel());
            }
        });
    }

    /**
     * Quét các StaffCall PENDING quá 30 giây và bắn spillover.
     * Spillover = broadcast toàn bộ Server (không chỉ zone) để xử lý.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional(readOnly = true)
    public void sweepSpilloverCalls() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(CALL_SPILLOVER_SECONDS);
        List<StaffCall> staleCalls = staffCallRepository.findPendingCallsOlderThan(threshold);

        if (staleCalls.isEmpty()) return;

        staleCalls.forEach(call -> {
            long pendingSeconds = ChronoUnit.SECONDS.between(call.getCreatedAt(), LocalDateTime.now());
            StaffCallSpilloverEvent event = StaffCallSpilloverEvent.builder()
                    .callId(call.getId())
                    .tableNumber(call.getTable() != null ? call.getTable().getNumber() : null)
                    .tableId(call.getTable() != null ? call.getTable().getId() : null)
                    .zone(call.getTable() != null ? call.getTable().getZone() : null)
                    .callType(call.getCallType())
                    .pendingSeconds(pendingSeconds)
                    .alertAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send("staff.call.spillover", call.getId().toString(), event);
            log.warn("Sweeper [SPILLOVER]: StaffCall {} (bàn {}) đã chờ {} giây!",
                    call.getId(), call.getTable() != null ? call.getTable().getNumber() : "N/A",
                    pendingSeconds);
        });
    }

    /** Xác định liệu một món có cần cảnh báo khẩn cấp (Spillover Cứu Viện) dựa trên SLA ngâm đồ */
    private boolean isUrgent(OrderTicketItem item, LocalDateTime now) {
        LocalDateTime readyTime = item.getCompletedAt() != null ? item.getCompletedAt() : item.getCreatedAt();
        long waitSeconds = ChronoUnit.SECONDS.between(readyTime, now);
                
        if ("HOT".equalsIgnoreCase(item.getStation()) || "KITCHEN".equalsIgnoreCase(item.getStation())) {
            return waitSeconds >= HOT_URGENCY_SECONDS;
        }
        return waitSeconds >= 120; // COLD và DRINK chờ 120s thì hú còi
    }
}
