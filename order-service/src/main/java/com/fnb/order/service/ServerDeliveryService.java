package com.fnb.order.service;

import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.CancelAlertEvent;
import com.fnb.order.dto.event.StaffCallAcceptedEvent;
import com.fnb.order.dto.response.ServerKpiResponse;
import com.fnb.order.dto.response.StaffCallResponse;
import com.fnb.order.dto.response.TicketDeliveryDto;
import com.fnb.order.entity.Order;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    public List<TicketDeliveryDto> getPendingDeliveries(List<String> zones, UUID serverId) {
        // Chuyển zones list thành chuỗi PostgreSQL array literal: {Tầng 1,Tầng 2}
        String zonesParam = buildZonesParam(zones);

        List<OrderTicketItem> items = (zonesParam == null)
                ? ticketItemRepository.findPendingDeliveries(serverId)
                : ticketItemRepository.findPendingDeliveriesByZones(zonesParam, serverId);

        // Group items by Order (hỗ trợ cả Bàn và Takeaway)
        Map<UUID, List<OrderTicketItem>> groupedByOrder = items.stream()
                .filter(item -> item.getTicket() != null
                        && item.getTicket().getOrder() != null)
                .collect(Collectors.groupingBy(item ->
                        item.getTicket().getOrder().getId()
                ));

        LocalDateTime now = LocalDateTime.now();
        return groupedByOrder.values().stream()
                .map(orderItems -> buildDeliveryDto(orderItems, now))
                .filter(dto -> dto != null && dto.getItems() != null && !dto.getItems().isEmpty())
                // Bàn có món urgent (cứu viện đỏ) lên trước
                .sorted(Comparator.comparing(dto -> dto.getItems().stream()
                        .anyMatch(TicketDeliveryDto.DeliveryItem::isUrgent) ? 0 : 1))
                .collect(Collectors.toList());
    }

    /**
     * Server bấm "Nhận bưng": Đổi trạng thái từ DONE -> DELIVERING.
     * Gán người bưng là serverId.
     */
    @Transactional
    public int claimDelivery(List<UUID> itemIds, UUID serverId) {
        int updated = ticketItemRepository.markAsDelivering(itemIds, serverId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể nhận bưng. Có thể nhân viên khác đã nhận hoặc món không còn ở trạng thái chờ bưng.");
        }
        log.info("Server {}: Đã NHẬN BƯNG {} món", serverId, updated);
        // Bắn event để các server khác cập nhật UI
        ticketItemRepository.findByIdIn(itemIds).forEach(item ->
                kafkaTemplate.send("ticket.updated", item.getId().toString(),
                        buildServedEvent(item)) // Dùng chung buildServedEvent gửi type = ITEM, status = DELIVERING
        );
        return updated;
    }

    /**
     * Server bấm "Bỏ nhận": Đổi trạng thái từ DELIVERING -> DONE.
     * Xóa gán người bưng.
     */
    @Transactional
    public int unclaimDelivery(List<UUID> itemIds, UUID serverId) {
        int updated = ticketItemRepository.unclaimDelivery(itemIds, serverId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể bỏ nhận bưng. Có thể món đã được bưng hoặc không thuộc quyền của bạn.");
        }
        log.info("Server {}: Đã BỎ NHẬN BƯNG {} món", serverId, updated);
        // Bắn event để các server khác thấy lại món trên khay đồ chung
        ticketItemRepository.findByIdIn(itemIds).forEach(item ->
                kafkaTemplate.send("ticket.updated", item.getId().toString(),
                        buildServedEvent(item))
        );
        return updated;
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
     * Thu ngân (POS) bấm "Đã giao cho khách" đối với đơn Takeaway.
     * Cập nhật tất cả các món DONE/COMPLETED của Session thành SERVED.
     */
    @Transactional
    public int serveTakeawaySession(UUID sessionId, UUID cashierId) {
        LocalDateTime now = LocalDateTime.now();
        List<OrderTicketItem> doneItems = ticketItemRepository.findDoneItemsBySessionId(sessionId);
        if (doneItems.isEmpty()) {
            log.warn("serveTakeawaySession: Không có món nào DONE cho session={}", sessionId);
            return 0;
        }

        List<UUID> itemIds = doneItems.stream().map(OrderTicketItem::getId).collect(Collectors.toList());
        
        // Vì các món Takeaway không trải qua trạng thái DELIVERING nên dùng markAsDelivering trick hoặc viết query mới.
        // Khoan, hàm markAsServed cũ yêu cầu status phải là DELIVERING.
        // Nên ta sẽ lặp qua save() thủ công hoặc cập nhật query. Do list nhỏ, ta lặp qua save:
        for (OrderTicketItem item : doneItems) {
            item.setStatus("SERVED");
            item.setServedAt(now);
            item.setServedBy(cashierId);
        }
        ticketItemRepository.saveAll(doneItems);

        log.info("Thu ngân {}: Đã giao {} món mang về cho Session {}", cashierId, doneItems.size(), sessionId);
        
        // Bắn event Real-time cho màn hình Khách hàng
        for (OrderTicketItem item : doneItems) {
            kafkaTemplate.send("ticket.updated", item.getId().toString(), buildServedEvent(item));
        }
        
        return doneItems.size();
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
        Map<String, Object> event = new HashMap<>();
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

    private TicketDeliveryDto buildDeliveryDto(List<OrderTicketItem> items,
                                               LocalDateTime now) {
        Order order = items.get(0).getTicket().getOrder();
        TableInfo table = order.getTable();
        Integer tableNumber = table != null ? table.getNumber() : null;
        String orderType = order.getOrderType();
        boolean isTakeawayOrDelivery = "TAKEAWAY".equals(orderType) || "DELIVERY".equals(orderType);

        // Tối ưu 1: ẨN HOÀN TOÀN ĐƠN MANG VỀ (Takeaway) khỏi màn hình của Nhân viên phục vụ (Server).
        // Phục vụ chỉ lo bưng bàn. Thu ngân sẽ lo Takeaway qua chuông báo StaffCall trên POS.
        if (isTakeawayOrDelivery) {
            return null; 
        }

        List<TicketDeliveryDto.DeliveryItem> deliveryItems = items.stream()
                .map(item -> {
                    LocalDateTime readyTime = item.getCompletedAt() != null ? item.getCompletedAt() : item.getCreatedAt();
                    long waitSeconds = ChronoUnit.SECONDS.between(readyTime, now);
                            
                    // SLA Breached (Hết hạn ngâm đồ trên quầy)
                    boolean isSlaBreached = false;
                    if ("HOT".equalsIgnoreCase(item.getStation()) || "KITCHEN".equalsIgnoreCase(item.getStation())) {
                        isSlaBreached = waitSeconds >= 240;
                    } else {
                        isSlaBreached = waitSeconds >= 120;
                    }

                    // Tối ưu 2: isUrgent chỉ áp dụng cho Dine-in, Takeaway không hụ còi
                    boolean isUrgent = isTakeawayOrDelivery ? false : isSlaBreached;

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
                            .deliveryAlertSent(item.getDeliveryAlertSent())
                            .build();
                })
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
        
        if (item.getTicket() != null) {
            event.put("ticketId", item.getTicket().getId());
            if (item.getTicket().getOrder() != null && item.getTicket().getOrder().getSession() != null) {
                event.put("sessionToken", item.getTicket().getOrder().getSession().getSessionToken());
            }
        }
        
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
