package com.fnb.order.service;

import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.CancelAlertEvent;
import com.fnb.order.dto.event.StaffCallAcceptedEvent;
import com.fnb.order.dto.response.ServerKpiResponse;
import com.fnb.order.dto.response.StaffCallResponse;
import com.fnb.order.dto.response.TicketDeliveryDto;
import com.fnb.order.entity.OrderTicketItem;
import com.fnb.order.entity.StaffCall;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.repository.OrderTicketItemRepository;
import com.fnb.order.repository.StaffCallRepository;
import com.fnb.order.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerDeliveryService {

    private final OrderTicketItemRepository ticketItemRepository;
    private final StaffCallRepository staffCallRepository;
    private final TableRepository tableRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Undo window: 30 giây sau khi SERVED mới được phép Undo */
    private static final int UNDO_WINDOW_SECONDS = 120;
    /** Dynamic Batching: HOT station urgent sau 240s */
    private static final int HOT_URGENCY_SECONDS = 240;

    // ==========================================================
    // DELIVERY APIs
    // ==========================================================

    /**
     * Lấy danh sách tất cả món đang chờ bưng (DONE/COMPLETED chưa SERVED).
     * Hỗ trợ filter theo zones (nhiều zone, cách nhau dấu phẩy).
     * Nếu zones null/rỗng → trả về toàn bộ.
     * Group by tableNumber. Ưu tiên hiển thị bàn có món urgent lên trước.
     */
    @Transactional(readOnly = true)
    public List<TicketDeliveryDto> getPendingDeliveries(List<String> zones) {
        // Chuyển zones list thành chuỗi PostgreSQL array literal: {Tầng 1,Tầng 2}
        String zonesParam = buildZonesParam(zones);

        List<OrderTicketItem> items = (zonesParam == null)
                ? ticketItemRepository.findPendingDeliveries()
                : ticketItemRepository.findPendingDeliveriesByZones(zonesParam);

        // Group items by table (thông qua ticket -> order -> table trực tiếp)
        Map<Integer, List<OrderTicketItem>> groupedByTable = items.stream()
                .filter(item -> item.getTicket() != null
                        && item.getTicket().getOrder() != null
                        && item.getTicket().getOrder().getTable() != null)
                .collect(Collectors.groupingBy(item ->
                        item.getTicket().getOrder().getTable().getNumber()
                ));

        LocalDateTime now = LocalDateTime.now();
        return groupedByTable.entrySet().stream()
                .map(e -> buildDeliveryDto(e.getKey(), e.getValue(), now))
                .filter(dto -> dto != null && dto.getItems() != null && !dto.getItems().isEmpty())
                // Bàn có món urgent (cứu viện đỏ) lên trước
                .sorted(Comparator.comparing(dto -> dto.getItems().stream()
                        .anyMatch(TicketDeliveryDto.DeliveryItem::isUrgent) ? 0 : 1))
                .collect(Collectors.toList());
    }

    /**
     * Server bấm "Bưng ra": Mark SERVED cho danh sách món.
     * Bắn Kafka ticket.updated để KDS và POS biết.
     */
    @Transactional
    public int serveItems(List<UUID> itemIds, UUID serverId) {
        LocalDateTime now = LocalDateTime.now();
        int updated = ticketItemRepository.markAsServed(itemIds, now, serverId);
        if (updated == 0) {
            log.warn("serveItems: Không có món nào được cập nhật. itemIds={}", itemIds);
        }
        log.info("Server {}: Đã bưng {} món ra bàn", serverId, updated);
        // Bắn event cho KDS / POS cập nhật status
        ticketItemRepository.findByIdIn(itemIds).forEach(item ->
                kafkaTemplate.send("ticket.updated", item.getId().toString(),
                        buildServedEvent(item))
        );
        return updated;
    }

    /**
     * Undo serve: Chỉ hợp lệ nếu served_at <= 120s trước.
     * Rollback status về DONE.
     */
    @Transactional
    public int undoServe(List<UUID> itemIds, UUID serverId) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(UNDO_WINDOW_SECONDS);
        int updated = ticketItemRepository.undoServe(itemIds, cutoff);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể Undo: đã quá 120 giây hoặc món không ở trạng thái SERVED.");
        }
        log.info("Server {}: Đã Undo serve {} món", serverId, updated);
        // Bắn event rollback cho KDS
        ticketItemRepository.findByIdIn(itemIds).forEach(item ->
                kafkaTemplate.send("ticket.updated", item.getId().toString(),
                        buildServedEvent(item))
        );
        return updated;
    }

    // ==========================================================
    // STAFF CALL APIs
    // ==========================================================

    /**
     * Lấy danh sách StaffCall đang active (PENDING + ACCEPTED) cho màn hình Server.
     * Hỗ trợ filter theo nhiều zones.
     */
    @Transactional(readOnly = true)
    public List<StaffCallResponse> getActiveServerCalls(List<String> zones) {
        String zonesParam = buildZonesParam(zones);
        List<StaffCall> calls = (zonesParam == null)
                ? staffCallRepository.findActiveServerCalls()
                : staffCallRepository.findActiveServerCallsByZones(zonesParam);

        return calls.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Server tiếp nhận yêu cầu — Optimistic Locking.
     * Nếu acceptCall() trả về 0: đã có người accept trước → 409 Conflict.
     */
    @Transactional
    public void acceptCall(UUID callId, UUID serverId, String serverName) {
        LocalDateTime now = LocalDateTime.now();
        int updated = staffCallRepository.acceptCall(callId, serverId, now);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yêu cầu này đã được nhân viên khác tiếp nhận.");
        }

        // Bắn Kafka để lock UI người khác qua WebSocket
        StaffCall call = staffCallRepository.findById(callId).orElseThrow();
        StaffCallAcceptedEvent event = StaffCallAcceptedEvent.builder()
                .callId(callId)
                .acceptedBy(serverId)
                .acceptedByName(serverName != null ? serverName : serverId.toString())
                .tableNumber(call.getTable() != null ? call.getTable().getNumber() : null)
                .callType(call.getCallType())
                .acceptedAt(now)
                .build();
        kafkaTemplate.send("staff.call.accepted", callId.toString(), event);

        log.info("Server {}: Đã tiếp nhận StaffCall {} (bàn {})",
                serverId, callId, call.getTable() != null ? call.getTable().getNumber() : "N/A");
    }

    /**
     * Server hoàn thành tác vụ: chuyển ACCEPTED → RESOLVED.
     */
    @Transactional
    public void resolveCall(UUID callId, UUID serverId) {
        StaffCall call = staffCallRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu gọi phục vụ không tồn tại."));

        if ("RESOLVED".equals(call.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý rồi.");
        }

        call.setStatus("RESOLVED");
        call.setResolvedAt(LocalDateTime.now());
        call.setResolvedBy(serverId);
        staffCallRepository.save(call);

        // Bắn sự kiện qua Kafka để các màn hình tự động làm mới ngay lập tức
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("callId", call.getId());
        event.put("status", "RESOLVED");
        event.put("resolvedBy", serverId);
        
        kafkaTemplate.send("staff.call.resolved", callId.toString(), event);

        log.info("Server {}: Đã hoàn thành yêu cầu {} (bàn {})",
                serverId, callId, call.getTable() != null ? call.getTable().getNumber() : "N/A");
    }

    // ==========================================================
    // CANCEL ALERT (Phase 2.3)
    // ==========================================================

    /**
     * Được gọi từ OrderService khi hủy món đang ở DONE/COMPLETED chưa SERVED.
     * Bắn Kafka cancel.alert để Server nhận cảnh báo đỏ qua WebSocket.
     */
    @Transactional
    public void publishCancelAlert(UUID orderId, Integer tableNumber,
                                   List<CancelAlertEvent.CancelledItem> cancelledItems) {
        if (cancelledItems == null || cancelledItems.isEmpty()) return;

        CancelAlertEvent event = CancelAlertEvent.builder()
                .orderId(orderId)
                .tableNumber(tableNumber)
                .cancelledItems(cancelledItems)
                .cancelledAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("cancel.alert", orderId.toString(), event);
        log.warn("CancelAlert: Bàn {} hủy {} món đang chờ bưng!", tableNumber, cancelledItems.size());
    }

    // ==========================================================
    // METADATA & KPI APIs (Phase 3.3)
    // ==========================================================

    /**
     * Lấy danh sách Zone duy nhất từ bảng tables.
     * Dùng cho Dropdown Multi-select Zone trên Mobile App.
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctZones() {
        return tableRepository.findDistinctZones();
    }

    /**
     * KPI hôm nay của một Server:
     * - Số món đã bưng
     * - Số yêu cầu đã xử lý
     * - Thời gian xử lý trung bình (giây)
     */
    @Transactional(readOnly = true)
    public ServerKpiResponse getKpiToday(UUID serverId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long totalServed = ticketItemRepository.countServedToday(serverId, startOfDay);
        long totalResolved = staffCallRepository.countResolvedToday(serverId, startOfDay);
        double avgSeconds = staffCallRepository.avgResolutionTimeSeconds(serverId, startOfDay);

        return ServerKpiResponse.builder()
                .totalServed(totalServed)
                .totalResolved(totalResolved)
                .avgResponseSeconds((long) avgSeconds)
                .build();
    }

    // ==========================================================
    // Private helpers
    // ==========================================================

    /**
     * Chuyển danh sách zones thành PostgreSQL array literal string cho native query.
     * Trả về null nếu zones rỗng (→ query sẽ bỏ qua filter).
     * VD: ["Tầng 1", "Sân Vườn"] → "{Tầng 1,Sân Vườn}"
     */
    private String buildZonesParam(List<String> zones) {
        if (zones == null || zones.isEmpty()) return null;
        return "{" + String.join(",", zones) + "}";
    }

    private TicketDeliveryDto buildDeliveryDto(Integer tableNumber,
                                               List<OrderTicketItem> items,
                                               LocalDateTime now) {
        TableInfo table = items.get(0).getTicket().getOrder().getTable();
        String orderType = items.get(0).getTicket().getOrder().getOrderType();
        boolean isTakeawayOrDelivery = "TAKEAWAY".equals(orderType) || "DELIVERY".equals(orderType);

        // 1. Check if ANY item on this table is SLA breached
        boolean anySlaBreachedForTable = items.stream().anyMatch(item -> {
            LocalDateTime readyTime = item.getCompletedAt() != null ? item.getCompletedAt() : item.getCreatedAt();
            long waitSeconds = ChronoUnit.SECONDS.between(readyTime, now);
            if ("HOT".equalsIgnoreCase(item.getStation()) || "KITCHEN".equalsIgnoreCase(item.getStation())) {
                return waitSeconds >= 240;
            } else {
                return waitSeconds >= 120;
            }
        });

        List<TicketDeliveryDto.DeliveryItem> deliveryItems = items.stream()
                .map(item -> {
                    LocalDateTime readyTime = item.getCompletedAt() != null ? item.getCompletedAt() : item.getCreatedAt();
                    long waitSeconds = ChronoUnit.SECONDS.between(readyTime, now);
                    boolean hasActiveItems = item.getTicket().getItems().stream()
                            .anyMatch(i -> "PENDING".equals(i.getStatus()) || "PREPARING".equals(i.getStatus()));
                            
                    // SLA Breached (Hết hạn ngâm đồ trên quầy)
                    boolean isSlaBreached = false;
                    if ("HOT".equalsIgnoreCase(item.getStation()) || "KITCHEN".equalsIgnoreCase(item.getStation())) {
                        isSlaBreached = waitSeconds >= 240;
                    } else {
                        isSlaBreached = waitSeconds >= 120;
                    }

                    boolean isReadyToServe;
                    boolean isUrgent;

                    if (isTakeawayOrDelivery) {
                        // O2O Rule: Bắt buộc xong 100% mới báo lấy đồ. Bỏ qua thời gian ngâm.
                        isReadyToServe = !hasActiveItems;
                        isUrgent = false; // Đơn mang về ưu tiên hiển thị chờ shipper, không báo đỏ vội.
                    } else {
                        // Dine-in Rule: Được phép bưng khi đã xong cả mâm HOẶC có món trên bàn bị hết hạn ngâm đồ
                        isReadyToServe = anySlaBreachedForTable || !hasActiveItems;
                        isUrgent = isSlaBreached;
                    }
                    
                    if (!isReadyToServe) return null; // Lọc bỏ ngay nếu chưa được phép bưng

                    return TicketDeliveryDto.DeliveryItem.builder()
                            .itemId(item.getId())
                            .itemName(item.getItemName())
                            .quantity(item.getQuantity())
                            .station(item.getStation())
                            .status(item.getStatus())
                            .readyAt(readyTime)
                            .unitPrice(item.getUnitPrice())
                            .note(item.getNote())
                            .isUrgent(isUrgent)
                            .build();
                })
                .filter(java.util.Objects::nonNull) // LỌC NHỮNG MÓN BỊ ẨN
                .collect(Collectors.toList());

        if (deliveryItems.isEmpty()) {
            return null; // Không trả về bàn nếu không có món nào cần bưng
        }

        return TicketDeliveryDto.builder()
                .tableNumber(tableNumber)
                .tableId(table != null ? table.getId() : null)
                .zone(table != null ? table.getZone() : "Takeaway")
                .items(deliveryItems)
                .build();
    }

    private Map<String, Object> buildServedEvent(OrderTicketItem item) {
        Map<String, Object> event = new HashMap<>();
        event.put("itemId", item.getId());
        event.put("status", item.getStatus());
        event.put("servedAt", item.getServedAt());
        event.put("type", "ITEM");
        return event;
    }

    private StaffCallResponse mapToResponse(StaffCall call) {
        return StaffCallResponse.builder()
                .id(call.getId())
                .sessionId(call.getSession() != null ? call.getSession().getId() : null)
                .tableId(call.getTable() != null ? call.getTable().getId() : null)
                .tableNumber(call.getTable() != null ? call.getTable().getNumber() : null)
                .callType(call.getCallType())
                .status(call.getStatus())
                .createdAt(call.getCreatedAt())
                .resolvedAt(call.getResolvedAt())
                .acceptedBy(call.getAcceptedBy())
                .acceptedAt(call.getAcceptedAt())
                .build();
    }
}
