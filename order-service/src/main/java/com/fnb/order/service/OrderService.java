package com.fnb.order.service;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.client.MenuServiceClient;
import com.fnb.order.dto.event.*;
import com.fnb.order.dto.request.*;
import com.fnb.order.dto.response.*;
import com.fnb.order.entity.*;
import com.fnb.order.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTicketRepository ticketRepository;
    private final TableSessionRepository sessionRepository;
    private final TableRepository tableRepository;
    private final StaffCallRepository staffCallRepository;
    private final MenuServiceClient menuServiceClient;
    private final CartService cartService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ServerDeliveryService serverDeliveryService;

    @Transactional
    public void submitTicket(String sessionToken, TicketRequest request) {
        log.info("Nhận yêu cầu đặt món từ session: {}", sessionToken);

        // 1. Lấy Session và Table
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException("Session này đã đóng, không thể gọi món");
        }

        TableInfo table = session.getTable();

        // 1.1 Gia hạn thêm 4 tiếng cho Session mỗi khi khách có thao tác đặt món
        session.setExpiresAt(LocalDateTime.now().plusHours(4));
        sessionRepository.save(session);

        // 2. Lấy Giỏ Hàng chung từ Redis
        var cart = cartService.getCart(sessionToken);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống, không thể đặt món.");
        }

        // 3. Tìm Order hiện tại (OPEN hoặc PAYMENT_REQUESTED). Nếu đã PAID (do Không giải phóng bàn), tạo Order mới
        Order order = orderRepository
                .findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .orElseGet(() -> {
                    log.info("Phiên bàn {} đang mở nhưng order trước đã thanh toán (Không đóng bàn) -> Tạo Order mới.", table != null ? table.getNumber() : "N/A");
                    Order newOrder = Order.builder()
                            .session(session)
                            .table(session.getTable())
                            .source("MANUAL")
                            .orderType(session.getTable() == null ? "TAKEAWAY" : "DINE_IN")
                            .status("OPEN")
                            .subtotal(java.math.BigDecimal.ZERO)
                            .total(java.math.BigDecimal.ZERO)
                            .build();
                    return orderRepository.save(newOrder);
                });

        // Nếu đơn đang chờ thanh toán mà lại gọi thêm món -> Reset về OPEN
        if ("PAYMENT_REQUESTED".equals(order.getStatus())) {
            log.info("Đơn hàng {} đang chờ thanh toán nhưng có món mới -> Chuyển về OPEN", order.getId());
            order.setStatus("OPEN");
            orderRepository.save(order);

            // Cập nhật lại cả trạng thái bàn trong DB (nếu có bàn)
            if (table != null) {
                table.setStatus("OCCUPIED");
                tableRepository.save(table);

                // Bắn event để POS cập nhật sơ đồ bàn (từ màu CAM về màu ĐỎ)
                applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                        .tableId(table.getId())
                        .status("OCCUPIED")
                        .sessionToken(sessionToken)
                        .build());
            }

            // Bắn thêm event đồng bộ trạng thái đơn hàng (OrderDetail)
            applicationEventPublisher.publishEvent(OrderStatusUpdatedEvent.builder()
                    .orderId(order.getId())
                    .status("OPEN")
                    .sessionToken(sessionToken)
                    .tableNumber(table.getNumber() != null ? String.valueOf(table.getNumber()) : null)
                    .build());
        }

        // 4. Tạo Ticket mới
        int currentTicketsCount = ticketRepository.findByOrderId(order.getId()).size();
        OrderTicket ticket = OrderTicket.builder()
                .order(order)
                .seqNumber(currentTicketsCount + 1)
                .status("PENDING")
                .note(request != null ? request.getNote() : null)
                .createdBy("CUSTOMER")
                .build();

        // 5. Lặp qua các món trong Giỏ (đã chuẩn hóa giá & tên)
        BigDecimal ticketTotal = BigDecimal.ZERO;
        List<OrderTicketItem> ticketItems = new ArrayList<>();

        for (var cartItem : cart.getItems()) {
            int qty = cartItem.getQuantity();

            // Tách từng món ra để bếp làm từng cái một (Số lượng luôn là 1)
            for (int i = 0; i < qty; i++) {
                OrderTicketItem ticketItem = OrderTicketItem.builder()
                        .ticket(ticket)
                        .menuItemId(cartItem.getMenuItemId())
                        .itemName(cartItem.getItemName())
                        .unitPrice(cartItem.getUnitPrice())
                        .quantity(1) // Ép về 1 để bếp kiểm soát từng món
                        .note(cartItem.getNote())
                        .station(cartItem.getStation())
                        .status("PENDING")
                        .build();

                List<OrderItemOption> itemOptions = new ArrayList<>();
                BigDecimal optionTotal = BigDecimal.ZERO;

                if (cartItem.getOptions() != null) {
                    for (var optReq : cartItem.getOptions()) {
                        BigDecimal extraPrice = optReq.getExtraPrice() != null ? optReq.getExtraPrice()
                                : BigDecimal.ZERO;

                        OrderItemOption option = OrderItemOption.builder()
                                .ticketItem(ticketItem)
                                .optionName(optReq.getOptionName())
                                .extraPrice(extraPrice)
                                .build();
                        itemOptions.add(option);
                        optionTotal = optionTotal.add(extraPrice);
                    }
                }

                ticketItem.setOptions(itemOptions);
                ticketItems.add(ticketItem);

                BigDecimal itemCost = cartItem.getUnitPrice().add(optionTotal);
                ticketTotal = ticketTotal.add(itemCost);
            }
        }

        ticket.setItems(ticketItems);
        ticketRepository.save(ticket);

        // Đảm bảo quan hệ hai chiều trong bộ nhớ (tránh bị Hibernate OrphanRemoval xóa
        // ticket mới tạo)
        order.getTickets().add(ticket);

        // 6. Cập nhật lại hóa đơn tổng
        recalculateTotal(order);
        orderRepository.save(order);

        // 7. Dọn sạch Giỏ hàng chờ lần order tới
        cartService.clearCart(sessionToken);

        // 8. Bắn Kafka Event lên KDS để báo nhà Bếp có hóa đơn mới
        List<OrderCreatedItemEvent> eventItems = ticketItems.stream().map(ti -> {
            List<OrderCreatedOptionEvent> eventOptions = ti.getOptions().stream()
                    .map(opt -> OrderCreatedOptionEvent.builder()
                            .optionName(opt.getOptionName())
                            .extraPrice(opt.getExtraPrice())
                            .build())
                    .collect(Collectors.toList());

            return OrderCreatedItemEvent.builder()
                    .menuItemId(ti.getMenuItemId())
                    .itemName(ti.getItemName())
                    .quantity(ti.getQuantity())
                    .note(ti.getNote())
                    .station(ti.getStation())
                    .unitPrice(ti.getUnitPrice())
                    .options(eventOptions)
                    .build();
        }).collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .ticketId(ticket.getId())
                .tableNumber(table != null ? table.getNumber() : null)
                .sessionToken(sessionToken)
                .note(ticket.getNote())
                .createdAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now())
                .items(eventItems)
                .build();

        // Push via Spring Event (để TransactionalEventListener xử lý)
        applicationEventPublisher.publishEvent(event);
        log.info("Đã tạo Ticket thành công cho Bàn/Takeaway: {}, Tổng tiền tăng thêm: {}", table != null ? table.getNumber() : "Mang về", ticketTotal);
    }

    @Transactional
    public OrderResponse getOrderBySessionToken(String sessionToken) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        // Cho phép xem đơn hàng nếu session còn ACTIVE hoặc vừa mới CLOSED (để khách xem hóa đơn sau thanh toán)
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("Session {} đã hết hạn, trả về null cho getOrderBySessionToken", sessionToken);
            return null;
        }

        // 1. Cố gắng tìm đơn hàng OPEN hoặc PAYMENT_REQUESTED
        Optional<Order> activeOrderOpt = orderRepository.findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN", "PAYMENT_REQUESTED"));
        if (activeOrderOpt.isPresent()) {
            return mapToOrderResponse(activeOrderOpt.get());
        }

        // Nếu session đã đóng (CLOSED), KHÔNG ĐƯỢC tạo order mới
        if (!"ACTIVE".equals(session.getStatus())) {
            // Trả về order mới nhất của session này (có thể là PAID hoặc CANCELLED) để khách/POS xem lại
            Order lastOrder = orderRepository.findFirstBySessionIdOrderByUpdatedAtDesc(session.getId())
                    .orElse(null);
            return lastOrder != null ? mapToOrderResponse(lastOrder) : null;
        }

        // 2. Nếu không có (có thể do order trước đã PAID và Không giải phóng bàn), tự tạo 1 Order mới
        log.info("Session {} vẫn ACTIVE nhưng không có order OPEN. Đang tạo Order mới (Phiên nối tiếp).", session.getSessionToken());
        Order newOrder = Order.builder()
                .session(session)
                .table(session.getTable())
                .source("MANUAL")
                .orderType(session.getTable() == null ? "TAKEAWAY" : "DINE_IN")
                .status("OPEN")
                .subtotal(java.math.BigDecimal.ZERO)
                .total(java.math.BigDecimal.ZERO)
                .build();
        return mapToOrderResponse(orderRepository.save(newOrder));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderTicketResponse> ticketDTOs = new ArrayList<>();

        if (order.getId() != null && order.getTickets() != null) {
            List<OrderTicket> tickets = order.getTickets();
            for (OrderTicket ticket : tickets) {
                List<OrderTicketItemResponse> itemDTOs = ticket.getItems().stream().map(item -> {
                    List<OrderItemOptionResponse> optDTOs = item.getOptions().stream()
                            .map(opt -> OrderItemOptionResponse.builder()
                                    .id(opt.getId())
                                    .optionName(opt.getOptionName())
                                    .extraPrice(opt.getExtraPrice())
                                    .build())
                            .collect(Collectors.toList());

                    return OrderTicketItemResponse.builder()
                            .id(item.getId())
                            .menuItemId(item.getMenuItemId())
                            .itemName(item.getItemName())
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .note(item.getNote())
                            .status(item.getStatus())
                            .station(item.getStation())
                            .createdAt(item.getCreatedAt())
                            .options(optDTOs)
                            .build();
                }).collect(Collectors.toList());

                ticketDTOs.add(OrderTicketResponse.builder()
                        .id(ticket.getId())
                        .orderId(ticket.getOrder().getId())
                        .seqNumber(ticket.getSeqNumber())
                        .status(ticket.getStatus())
                        .note(ticket.getNote())
                        .createdBy(ticket.getCreatedBy())
                        .createdAt(ticket.getCreatedAt())
                        .items(itemDTOs)
                        .build());
            }
        }

        // --- Logic Gộp món cho Hóa đơn (Bill Summary) ---
        List<OrderSummaryItemResponse> summaryItems = new ArrayList<>();
        if (order.getTickets() != null) {
            order.getTickets().stream()
                    .filter(t -> !"CANCELLED".equals(t.getStatus()))
                    .flatMap(t -> t.getItems().stream())
                    .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"RETURNED".equals(i.getStatus()))
                    .forEach(item -> {
                        // Tìm món đã có trong summary chưa
                        Optional<OrderSummaryItemResponse> existing = summaryItems.stream()
                                .filter(s -> s.getMenuItemId().equals(item.getMenuItemId()))
                                .filter(s -> Objects.equals(s.getNote(), item.getNote()))
                                .filter(s -> isSameOptionsDTO(s.getOptions(), item.getOptions()))
                                .findFirst();

                        if (existing.isPresent()) {
                            existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
                            existing.get().setPriceTotal(existing.get().getPriceTotal().add(
                                    calculateItemTotal(item)));
                        } else {
                            List<OrderItemOptionResponse> optDTOs = item.getOptions().stream()
                                    .map(opt -> OrderItemOptionResponse.builder()
                                            .id(opt.getId())
                                            .optionName(opt.getOptionName())
                                            .extraPrice(opt.getExtraPrice())
                                            .build())
                                    .collect(Collectors.toList());

                            summaryItems.add(OrderSummaryItemResponse.builder()
                                    .menuItemId(item.getMenuItemId())
                                    .itemName(item.getItemName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .priceTotal(calculateItemTotal(item))
                                    .note(item.getNote())
                                    .options(optDTOs)
                                    .build());
                        }
                    });
        }

        return OrderResponse.builder()
                .id(order.getId())
                .sessionId(order.getSession() != null ? order.getSession().getId() : null)
                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                .tableNumber(order.getTable() != null && order.getTable().getNumber() != null
                        ? String.valueOf(order.getTable().getNumber())
                        : null)
                .status(order.getStatus())
                .source(order.getSource())
                .orderType(order.getOrderType())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .promotionId(order.getPromotionId())
                .promotionCode(order.getPromotionCode())
                .createdAt(order.getCreatedAt())
                .tickets(ticketDTOs)
                .summaryItems(summaryItems)
                .build();
    }

    private BigDecimal calculateItemTotal(OrderTicketItem item) {
        BigDecimal extra = item.getOptions().stream()
                .map(o -> o.getExtraPrice() != null ? o.getExtraPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return item.getUnitPrice().add(extra).multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private boolean isSameOptionsDTO(List<OrderItemOptionResponse> sOpts,
            List<OrderItemOption> itemOpts) {
        if (sOpts == null && itemOpts == null)
            return true;
        if (sOpts == null || itemOpts == null)
            return false;
        if (sOpts.size() != itemOpts.size())
            return false;

        Set<UUID> ids1 = sOpts.stream().map(OrderItemOptionResponse::getId).collect(Collectors.toSet());
        Set<UUID> ids2 = itemOpts.stream().map(OrderItemOption::getId).collect(Collectors.toSet());
        return ids1.equals(ids2);
    }

    @Transactional
    public void cancelItem(UUID orderId, UUID itemId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Không thể sửa hóa đơn đã hủy toàn bộ");
        }

        OrderTicketItem itemToCancel = null;
        OrderTicket targetTicket = null;

        for (OrderTicket ticket : order.getTickets()) {
            for (OrderTicketItem item : ticket.getItems()) {
                if (item.getId().equals(itemId)) {
                    itemToCancel = item;
                    targetTicket = ticket;
                    break;
                }
            }
            if (itemToCancel != null)
                break;
        }

        if (itemToCancel == null) {
            throw new ResourceNotFoundException("Không tìm thấy món trong hóa đơn này");
        }

        if ("CANCELLED".equals(itemToCancel.getStatus())) {
            throw new BusinessException("Món này đã bị hủy trước đó");
        }

        if ("DONE".equals(itemToCancel.getStatus()) || "SERVED".equals(itemToCancel.getStatus())
                || "COMPLETED".equals(itemToCancel.getStatus())) {
            throw new BusinessException("Không thể hủy món đã hoàn thành hoặc đã phục vụ");
        }

        itemToCancel.setStatus("CANCELLED");
        if (reason != null && !reason.trim().isEmpty()) {
            itemToCancel.setNote(
                    (itemToCancel.getNote() != null ? itemToCancel.getNote() + " | " : "") + "Huỷ lý do: " + reason);
        }

        recalculateTotal(order);

        // Kiểm tra xem ticket đó có bị hủy/trả toàn bộ món không
        boolean noMoreActiveItems = targetTicket.getItems().stream()
                .allMatch(i -> "CANCELLED".equals(i.getStatus()) || "RETURNED".equals(i.getStatus()));

        if (noMoreActiveItems) {
            targetTicket.setStatus("CANCELLED");
        } else {
            // Update lại ticket status nếu tất cả món còn lại là DONE (hoặc đã hủy/trả)
            boolean everyActiveIsDone = targetTicket.getItems().stream()
                    .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"RETURNED".equals(i.getStatus()))
                    .allMatch(i -> "DONE".equals(i.getStatus()) || "SERVED".equals(i.getStatus())
                            || "COMPLETED".equals(i.getStatus()));
            if (everyActiveIsDone) {
                targetTicket.setStatus("DONE");
            }
        }

        checkAndAutoCancelEmptyOrder(order, reason);
        orderRepository.save(order);

        // Phase 2.3: Nếu món đang DONE/COMPLETED (bếp làm xong chưa bưng) bị hủy
        // → Bắn Cancel Alert để Server nhận cảnh báo đỏ để không bưng nhầm
        boolean wasDoneNotServed = ("DONE".equals(itemToCancel.getStatus())
                || "COMPLETED".equals(itemToCancel.getStatus()))
                && itemToCancel.getServedAt() == null;
        if (wasDoneNotServed) {
            serverDeliveryService.publishCancelAlert(
                    order.getId(),
                    order.getTable() != null ? order.getTable().getNumber() : null,
                    List.of(CancelAlertEvent.CancelledItem.builder()
                            .itemId(itemToCancel.getId())
                            .itemName(itemToCancel.getItemName())
                            .quantity(itemToCancel.getQuantity())
                            .station(itemToCancel.getStation())
                            .build())
            );
        }

        // Bắn event cập nhật KDS để bếp biết
        TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                .ticketId(targetTicket.getId())
                .itemId(itemToCancel.getId())
                .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                .status("CANCELLED")
                .type("ITEM")
                .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                        ? order.getSession().getTable().getNumber()
                        : null)
                .updatedAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        // Bắn thêm event cập nhật sơ đồ bàn POS (luôn bắn khi đổi tiền)
        if (order.getTable() != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(order.getTable().getId())
                    .status(order.getTable().getStatus())
                    .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                    .build());
        }
    }

    @Transactional
    public void returnItem(UUID orderId, UUID itemId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Không thể trả món của hóa đơn đã hủy toàn bộ");
        }

        OrderTicketItem itemToReturn = null;
        OrderTicket targetTicket = null;

        for (OrderTicket ticket : order.getTickets()) {
            for (OrderTicketItem item : ticket.getItems()) {
                if (item.getId().equals(itemId)) {
                    itemToReturn = item;
                    targetTicket = ticket;
                    break;
                }
            }
            if (itemToReturn != null)
                break;
        }

        if (itemToReturn == null) {
            throw new ResourceNotFoundException("Không tìm thấy món trong hóa đơn này");
        }

        if ("RETURNED".equals(itemToReturn.getStatus()) || "CANCELLED".equals(itemToReturn.getStatus())) {
            throw new BusinessException("Món này đã bị hủy hoặc trả lại trước đó");
        }

        if (!("DONE".equals(itemToReturn.getStatus()) || "SERVED".equals(itemToReturn.getStatus())
                || "COMPLETED".equals(itemToReturn.getStatus()))) {
            throw new BusinessException(
                    "Chỉ có thể trả lại món nếu trạng thái đã hoàn thành (DONE) hoặc phục vụ (SERVED). Nếu món chưa làm xong vui lòng dùng chức năng Hủy món.");
        }

        itemToReturn.setStatus("RETURNED");
        if (reason != null && !reason.trim().isEmpty()) {
            itemToReturn.setNote((itemToReturn.getNote() != null ? itemToReturn.getNote() + " | " : "")
                    + "Trả hàng lý do: " + reason);
        }

        recalculateTotal(order);
        orderRepository.save(order);

        TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                .ticketId(targetTicket.getId())
                .itemId(itemToReturn.getId())
                .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                .status("RETURNED")
                .type("ITEM")
                .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                        ? order.getSession().getTable().getNumber()
                        : null)
                .updatedAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        // Bắn thêm event cập nhật sơ đồ bàn POS (luôn bắn khi đổi tiền)
        if (order.getTable() != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(order.getTable().getId())
                    .status(order.getTable().getStatus())
                    .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                    .build());
        }
        log.info("Khách trả lại món: {} trong Order: {}, lý do: {}", itemToReturn.getItemName(), orderId, reason);
    }

    @Transactional
    public void cancelTicket(UUID orderId, UUID ticketId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));

        OrderTicket targetTicket = order.getTickets().stream()
                .filter(t -> t.getId().equals(ticketId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu yêu cầu (ticket) này."));

        if ("CANCELLED".equals(targetTicket.getStatus())) {
            throw new BusinessException("Phiếu yêu cầu này đã được hủy trước đó.");
        }

        // Kiểm tra trạng thái các món trong phiếu
        boolean hasProcessedItems = targetTicket.getItems().stream()
                .anyMatch(i -> !"PENDING".equals(i.getStatus()) && !"CANCELLED".equals(i.getStatus()));

        if (hasProcessedItems) {
            throw new BusinessException(
                    "Phiếu có món đang được xử lý hoặc đã xong, không thể hủy toàn bộ. Vui lòng hủy từng món chưa làm!");
        }

        for (OrderTicketItem item : targetTicket.getItems()) {
            if (!"CANCELLED".equals(item.getStatus())) {
                item.setStatus("CANCELLED");
                if (reason != null && !reason.trim().isEmpty()) {
                    item.setNote((item.getNote() != null ? item.getNote() + " | " : "") + "NV Huỷ: " + reason);
                }
            }
        }
        targetTicket.setStatus("CANCELLED");

        recalculateTotal(order);
        checkAndAutoCancelEmptyOrder(order, reason);
        orderRepository.save(order);

        TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                .ticketId(targetTicket.getId())
                .itemId(null)
                .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                .status("CANCELLED")
                .type("TICKET")
                .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                        ? order.getSession().getTable().getNumber()
                        : null)
                .updatedAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        // Bắn thêm event cập nhật sơ đồ bàn POS (luôn bắn khi hủy phiếu)
        if (order.getTable() != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(order.getTable().getId())
                    .status(order.getTable().getStatus())
                    .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                    .build());
        }
        log.info("Nhân viên hủy phiếu: {} trong Order: {}", targetTicket.getId(), order.getId());
    }

    @Transactional
    public void cancelItemByCustomer(String sessionToken, UUID itemId, String reason) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException("Session đã đóng, không thể hủy món.");
        }

        Order order = orderRepository
                .findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn của session này."));

        OrderTicketItem itemToCancel = null;
        OrderTicket targetTicket = null;

        for (OrderTicket ticket : order.getTickets()) {
            for (OrderTicketItem item : ticket.getItems()) {
                if (item.getId().equals(itemId)) {
                    itemToCancel = item;
                    targetTicket = ticket;
                    break;
                }
            }
            if (itemToCancel != null)
                break;
        }

        if (itemToCancel == null) {
            throw new ResourceNotFoundException("Không tìm thấy món trong hóa đơn của bạn.");
        }

        if ("CANCELLED".equals(itemToCancel.getStatus())) {
            throw new BusinessException("Món này đã được hủy trước đó.");
        }

        if (!"PENDING".equals(itemToCancel.getStatus())) {
            throw new BusinessException(
                    "Không thể hủy món đã được bếp tiếp nhận hoặc đang làm. Vui lòng liên hệ nhân viên!");
        }

        itemToCancel.setStatus("CANCELLED");
        if (reason != null && !reason.trim().isEmpty()) {
            itemToCancel.setNote(
                    (itemToCancel.getNote() != null ? itemToCancel.getNote() + " | " : "") + "KH Hủy: " + reason);
        }

        recalculateTotal(order);

        // Kiểm tra xem ticket đó có bị hủy/trả toàn bộ món không
        boolean noMoreActiveItems = targetTicket.getItems().stream()
                .allMatch(i -> "CANCELLED".equals(i.getStatus()) || "RETURNED".equals(i.getStatus()));

        if (noMoreActiveItems) {
            targetTicket.setStatus("CANCELLED");
        } else {
            // Update lại ticket status nếu tất cả món còn lại là DONE (hoặc đã hủy/trả)
            boolean everyActiveIsDone = targetTicket.getItems().stream()
                    .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"RETURNED".equals(i.getStatus()))
                    .allMatch(i -> "DONE".equals(i.getStatus()) || "SERVED".equals(i.getStatus())
                            || "COMPLETED".equals(i.getStatus()));
            if (everyActiveIsDone) {
                targetTicket.setStatus("DONE");
            }
        }

        checkAndAutoCancelEmptyOrder(order, reason);
        orderRepository.save(order);

        TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                .ticketId(targetTicket.getId())
                .itemId(itemToCancel.getId())
                .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                .status("CANCELLED")
                .type("ITEM")
                .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                        ? order.getSession().getTable().getNumber()
                        : null)
                .updatedAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        // Bắn thêm event cập nhật sơ đồ bàn POS (luôn bắn khi khách tự hủy món)
        if (order.getTable() != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(order.getTable().getId())
                    .status(order.getTable().getStatus())
                    .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                    .build());
        }
        log.info("Khách tự hủy món: {} trong Order: {}", itemToCancel.getItemName(), order.getId());
    }

    @Transactional
    public void cancelTicketByCustomer(String sessionToken, UUID ticketId, String reason) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new BusinessException("Session đã đóng, không thể hủy phiếu.");
        }

        Order order = orderRepository
                .findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn của session này."));

        OrderTicket targetTicket = order.getTickets().stream()
                .filter(t -> t.getId().equals(ticketId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiếu yêu cầu (ticket) này trong session của bạn."));

        if ("CANCELLED".equals(targetTicket.getStatus())) {
            throw new BusinessException("Phiếu yêu cầu này đã được hủy trước đó.");
        }

        boolean canCancelAll = targetTicket.getItems().stream()
                .filter(i -> !"CANCELLED".equals(i.getStatus()))
                .allMatch(i -> "PENDING".equals(i.getStatus()));

        if (!canCancelAll) {
            throw new BusinessException(
                    "Có món trong phiếu đã được tiếp nhận hoặc đang xử lý, không thể tự hủy nguyên phiếu. Vui lòng liên hệ nhân viên!");
        }

        for (OrderTicketItem item : targetTicket.getItems()) {
            if (!"CANCELLED".equals(item.getStatus())) {
                item.setStatus("CANCELLED");
                if (reason != null && !reason.trim().isEmpty()) {
                    item.setNote((item.getNote() != null ? item.getNote() + " | " : "") + "KH Hủy: " + reason);
                }
            }
        }
        targetTicket.setStatus("CANCELLED");

        recalculateTotal(order);
        checkAndAutoCancelEmptyOrder(order, reason);
        orderRepository.save(order);

        TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                .ticketId(targetTicket.getId())
                .itemId(null)
                .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                .status("CANCELLED")
                .type("TICKET")
                .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                        ? order.getSession().getTable().getNumber()
                        : null)
                .updatedAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        // Bắn thêm event cập nhật sơ đồ bàn POS (luôn bắn khi khách tự hủy phiếu)
        if (order.getTable() != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(order.getTable().getId())
                    .status(order.getTable().getStatus())
                    .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                    .build());
        }
        log.info("Khách tự hủy phiếu: {} trong Order: {}", targetTicket.getId(), order.getId());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn có ID: " + id));
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrderHistory(String status, String source, String search,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (source != null && !source.isEmpty()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                // 1. Search by Order ID (UUID AS String)
                Predicate idPredicate = cb.like(cb.lower(root.get("id").as(String.class)), searchPattern);
                
                // 2. Search by Table Number or Table Name
                Join<Order, TableInfo> tableJoin = root.join("table", JoinType.LEFT);
                Predicate tableNumPredicate = cb.like(tableJoin.get("number").as(String.class), searchPattern);
                Predicate tableNamePredicate = cb.like(cb.lower(tableJoin.get("name")), searchPattern);
                
                // 3. Search by Promotion Code
                Predicate promoPredicate = cb.like(cb.lower(root.get("promotionCode")), searchPattern);
                
                // Combine with OR
                predicates.add(cb.or(idPredicate, tableNumPredicate, tableNamePredicate, promoPredicate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional
    public void cancelOrder(UUID id, String reason, String note, UUID cancellerId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn có ID: " + id));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Hóa đơn này đã được hủy trước đó!");
        }

        boolean hasDoneItems = order.getTickets().stream()
                .flatMap(t -> t.getItems().stream())
                .anyMatch(i -> "DONE".equals(i.getStatus()) || "SERVED".equals(i.getStatus())
                        || "COMPLETED".equals(i.getStatus()));

        if (hasDoneItems) {
            throw new BusinessException(
                    "Không thể hủy hóa đơn vì đã có món hoàn thành. Bạn chỉ có thể thanh toán hoặc hủy từng món (nếu chưa làm).");
        }

        order.setStatus("CANCELLED");
        // Phase 2: Ghi nhận ai đã duyệt hủy và lý do
        if (cancellerId != null) order.setCancelledBy(cancellerId);
        if (reason != null) order.setCancelReason(reason);
        // Update all items and tickets to CANCELLED as well
        order.getTickets().forEach(ticket -> {
            boolean onlyPendingOrInProgress = ticket.getItems().stream()
                    .allMatch(i -> "PENDING".equals(i.getStatus()) || "PREPARING".equals(i.getStatus())
                            || "CANCELLED".equals(i.getStatus()) || "RETURNED".equals(i.getStatus()));
            if (onlyPendingOrInProgress) {
                ticket.setStatus("CANCELLED");
                ticket.getItems().forEach(item -> {
                    if (!"CANCELLED".equals(item.getStatus())) {
                        item.setStatus("CANCELLED");
                    }
                });

                // Kích hoạt socket realtime cho Bếp
                TicketUpdatedEvent event = TicketUpdatedEvent.builder()
                        .ticketId(ticket.getId())
                        .itemId(null) // Cập nhật cả ticket
                        .sessionToken(order.getSession() != null ? order.getSession().getSessionToken() : null)
                        .status("CANCELLED")
                        .type("TICKET")
                        .tableNumber(order.getSession() != null && order.getSession().getTable() != null
                                ? order.getSession().getTable().getNumber()
                                : null)
                        .updatedAt(LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }
        });

        recalculateTotal(order);
        handleSessionAndTableWhenOrderCancelled(order);
        orderRepository.save(order);

        log.info("Đã hủy hóa đơn: {}, lý do: {}, note: {}", id, reason, note);
    }

    @Transactional
    public void closeOrder(UUID id, boolean releaseTable, UUID cashierId, String paymentMethod, String paymentDetail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order ID: " + id));

        if ("PAID".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Hóa đơn này đã được đóng hoặc hủy trước đó!");
        }

        // Validate Mixed Payment total against Order total
        if ("MIXED".equals(paymentMethod) && paymentDetail != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(paymentDetail);
                java.math.BigDecimal totalMixed = java.math.BigDecimal.ZERO;
                
                if (rootNode.isObject()) {
                    java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = rootNode.fields();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                        if (field.getValue().isNumber()) {
                            totalMixed = totalMixed.add(new java.math.BigDecimal(field.getValue().asText()));
                        }
                    }
                } else if (rootNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                        if (node.has("amount") && node.get("amount").isNumber()) {
                            totalMixed = totalMixed.add(new java.math.BigDecimal(node.get("amount").asText()));
                        }
                    }
                }
                
                // Compare with 0 so scale doesn't matter (e.g. 50000.00 vs 50000)
                if (totalMixed.compareTo(order.getTotal()) != 0) {
                    throw new BusinessException("Tổng số tiền thanh toán hỗn hợp (" + totalMixed + ") không khớp với tổng hóa đơn (" + order.getTotal() + ")");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Lỗi parse paymentDetail: {}", e.getMessage());
                throw new BusinessException("Định dạng dữ liệu thanh toán không hợp lệ!");
            }
        }

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        
        // Ghi nhan phuong thuc thanh toan
        if (paymentMethod != null) order.setPaymentMethod(paymentMethod);
        if (paymentDetail != null) order.setPaymentDetail(paymentDetail);

        // Phase 2: Ghi nhận thu ngân nào đã chốt bill
        if (cashierId != null) order.setCashierId(cashierId);
        orderRepository.save(order);

        // Tăng số lần sử dụng của Voucher nếu có
        if (order.getPromotionId() != null) {
            try {
                menuServiceClient.incrementPromotionUsage(order.getPromotionId());
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật số lần dùng Voucher {}: {}", order.getPromotionId(), e.getMessage());
            }
        }

        TableInfo table = order.getTable();
        TableSession session = order.getSession();

        if (releaseTable) {
            if (session != null) {
                session.setClosedAt(LocalDateTime.now());
                session.setStatus("CLOSED");
                sessionRepository.save(session);
            }

            if (table != null) {
                table.setStatus("CLEANING");
                tableRepository.save(table);
            }
        }

        OrderPaidEvent paidEvent = OrderPaidEvent.builder()
                .orderId(order.getId())
                .tableId(table != null ? table.getId() : null)
                .tableNumber(table != null ? table.getNumber() : null)
                .sessionToken(session != null ? session.getSessionToken() : null)
                .paidAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(paidEvent);

        if (releaseTable && table != null) {
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(table.getId())
                    .status("CLEANING")
                    .sessionToken(session != null ? session.getSessionToken() : null)
                    .build());

            applicationEventPublisher.publishEvent(OrderStatusUpdatedEvent.builder()
                    .orderId(order.getId())
                    .status("PAID")
                    .sessionToken(session != null ? session.getSessionToken() : null)
                    .tableNumber(table.getNumber() != null ? String.valueOf(table.getNumber()) : null)
                    .build());
        }

        log.info("Đã chốt hóa đơn và giải phóng bàn/mang về: {}", table != null ? table.getNumber() : "Mang về");
    }

    @Transactional
    public void applyPromotion(String sessionToken, String code) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        Order order = orderRepository.findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn để áp dụng mã giảm giá."));

        executeApplyPromotion(order, code);
    }

    @Transactional
    public void applyPromotionById(java.util.UUID orderId, String code) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + orderId));
        
        if ("PAID".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Không thể áp dụng khuyến mãi cho hóa đơn đã thanh toán hoặc đã hủy.");
        }

        executeApplyPromotion(order, code);
    }

    private void executeApplyPromotion(Order order, String code) {
        if (code == null || code.trim().isEmpty()) {
            order.setPromotionId(null);
            order.setDiscountType(null);
            order.setDiscountRate(null);
            order.setMinOrderAmount(null);
            order.setMaxDiscountValue(null);
            order.setDiscount(BigDecimal.ZERO);
            order.setPromotionCode(null);
            order.setIsStackable(true);
            
            recalculateTotal(order);
            orderRepository.save(order);
            log.info("Đã gỡ mã giảm giá cho Order {}", order.getId());
            return;
        }

        // Gọi Menu service để validate mã
        ApiResponse<MenuServiceClient.PromotionDetail> promoResponse = menuServiceClient.validatePromotion(code, order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO);
        if (promoResponse == null || !promoResponse.isSuccess() || promoResponse.getData() == null) {
            String msg = (promoResponse != null && promoResponse.getMessage() != null) 
                         ? promoResponse.getMessage() 
                         : "Mã giảm giá không hợp lệ hoặc không đủ điều kiện áp dụng.";
            throw new BusinessException(msg);
        }

        var promo = promoResponse.getData();
        order.setPromotionId(promo.id());
        order.setPromotionCode(code.toUpperCase());
        // Map field mới: discountType / discountValue / maxDiscount
        order.setDiscountType(promo.discountType());
        order.setDiscountRate(promo.discountValue());
        // minOrderAmount lấy từ requirement (nếu có)
        if (promo.requirement() != null) {
            order.setMinOrderAmount(promo.requirement().minOrderAmount());
        }
        order.setMaxDiscountValue(promo.maxDiscount());
        order.setIsStackable(promo.stackable() != null ? promo.stackable() : true);
        
        // Tính tiền giảm tại chỗ
        recalculateTotal(order);
        orderRepository.save(order);
        
        log.info("Đã áp dụng mã {} cho Order {}, giảm giá: {}", code, order.getId(), order.getDiscount());
    }

    private void recalculateTotal(Order order) {
        BigDecimal rawSubtotal = BigDecimal.ZERO;
        List<OrderTicketItem> activeItems = new ArrayList<>();

        if (order.getTickets() != null) {
            activeItems = order.getTickets().stream()
                    .filter(t -> !"CANCELLED".equals(t.getStatus()))
                    .flatMap(t -> t.getItems().stream())
                    .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"RETURNED".equals(i.getStatus()))
                    .toList();

            rawSubtotal = activeItems.stream()
                    .map(this::calculateItemTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        order.setSubtotal(rawSubtotal);

        BigDecimal automatedDiscount = BigDecimal.ZERO;
        boolean hasAutoDiscount = false;

        try {
            // --- LEVEL 0: Bundle Matching (Combo) ---
            var activeRulesRes = menuServiceClient.getActivePromotions();
            if (activeRulesRes != null && activeRulesRes.isSuccess() && activeRulesRes.getData() != null) {
                List<MenuServiceClient.PromotionDetail> allPromos = activeRulesRes.getData();
                
                List<MenuServiceClient.PromotionDetail> bundleRules = allPromos.stream()
                        .filter(p -> "BUNDLE".equals(p.scope()) && "AUTO".equals(p.triggerType()))
                        .toList();

                Map<UUID, Integer> cartMap = new HashMap<>();
                Map<UUID, BigDecimal> priceMap = new HashMap<>();
                Map<UUID, BigDecimal> flashSaleBenefitMap = new HashMap<>();

                // Pre-calculate individual item Flash Sale benefits
                var productAutoPromos = allPromos.stream()
                        .filter(p -> "AUTO".equals(p.triggerType()) && "PRODUCT".equals(p.scope()))
                        .toList();

                Map<UUID, UUID> itemCategoryMap = new HashMap<>();
                for (OrderTicketItem item : activeItems) {
                    try {
                        var res = menuServiceClient.getMenuItemById(item.getMenuItemId());
                        if (res != null && res.isSuccess() && res.getData() != null) {
                            itemCategoryMap.put(item.getMenuItemId(), res.getData().categoryId());
                        }
                    } catch (Exception e) {}
                }

                for (OrderTicketItem item : activeItems) {
                    UUID itemId = item.getMenuItemId();
                    UUID categoryId = itemCategoryMap.get(itemId);
                    cartMap.put(itemId, cartMap.getOrDefault(itemId, 0) + item.getQuantity());
                    priceMap.put(itemId, item.getUnitPrice());

                    var itemPromos = productAutoPromos.stream()
                            .filter(p -> com.fnb.common.util.PricingEngine.isApplicable(p.targets(), itemId, categoryId))
                            .toList();

                    if (!itemPromos.isEmpty()) {
                        var bestFlash = com.fnb.common.util.PricingEngine.selectBestPromotion(itemPromos, item.getUnitPrice());
                        flashSaleBenefitMap.put(itemId, bestFlash.getDiscountAmount());
                    } else {
                        flashSaleBenefitMap.put(itemId, BigDecimal.ZERO);
                    }
                }

                if (!bundleRules.isEmpty()) {
                    List<MenuServiceClient.PromotionDetail> profitableBundles = bundleRules.stream().filter(rule -> {
                        BigDecimal totalFlashBenefit = BigDecimal.ZERO;
                        for (var bi : rule.getBundleItems()) {
                            BigDecimal perItemFlash = flashSaleBenefitMap.getOrDefault(bi.getItemId(), BigDecimal.ZERO);
                            totalFlashBenefit = totalFlashBenefit.add(perItemFlash.multiply(BigDecimal.valueOf(bi.getQuantity())));
                        }
                        BigDecimal bBase = BigDecimal.ZERO;
                        for (var bi : rule.getBundleItems()) {
                            bBase = bBase.add(priceMap.getOrDefault(bi.getItemId(), BigDecimal.ZERO).multiply(BigDecimal.valueOf(bi.getQuantity())));
                        }
                        BigDecimal bundleDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                                bBase, rule.getDiscountType(), rule.getDiscountValue(), rule.getMaxDiscount());
                        return bundleDiscount.compareTo(totalFlashBenefit) >= 0;
                    }).toList();

                    if (!profitableBundles.isEmpty()) {
                        var matchedResults = com.fnb.common.util.BundleMatcher.matchBundles(cartMap, profitableBundles, priceMap);
                        for (var result : matchedResults) {
                            automatedDiscount = automatedDiscount.add(result.getTotalDiscount());
                            hasAutoDiscount = true;
                            log.info("Đã áp dụng Combo: {} x{}", result.getRule().getName(), result.getCount());
                        }
                    }
                }

                // --- LEVEL 1: Product Auto Discount (Flash Sale) ---
                for (OrderTicketItem item : activeItems) {
                    int remainingQty = cartMap.getOrDefault(item.getMenuItemId(), 0);
                    if (remainingQty <= 0) continue;

                    UUID itemId = item.getMenuItemId();
                    UUID categoryId = itemCategoryMap.get(itemId);
                    var itemPromos = productAutoPromos.stream()
                            .filter(p -> com.fnb.common.util.PricingEngine.isApplicable(p.targets(), itemId, categoryId))
                            .toList();

                    if (!itemPromos.isEmpty()) {
                        var bestResult = com.fnb.common.util.PricingEngine.selectBestPromotion(itemPromos, item.getUnitPrice());
                        if (bestResult.getPromotion() != null) {
                            automatedDiscount = automatedDiscount.add(bestResult.getDiscountAmount().multiply(BigDecimal.valueOf(remainingQty)));
                            hasAutoDiscount = true;
                            cartMap.put(item.getMenuItemId(), 0);
                        }
                    }
                }

                // --- LEVEL 2: Order Auto Discount ---
                BigDecimal subtotalAfterL0L1 = rawSubtotal.subtract(automatedDiscount);
                var orderAutoPromos = allPromos.stream()
                        .filter(p -> "ORDER".equals(p.scope()) && "AUTO".equals(p.triggerType()))
                        .filter(p -> p.requirement() == null || subtotalAfterL0L1.compareTo(p.requirement().minOrderAmount()) >= 0)
                        .toList();

                if (!orderAutoPromos.isEmpty()) {
                    var bestResult = com.fnb.common.util.PricingEngine.selectBestPromotion(orderAutoPromos, subtotalAfterL0L1);
                    if (bestResult.getPromotion() != null) {
                        if (Boolean.FALSE.equals(bestResult.getPromotion().stackable())) {
                            BigDecimal standAloneDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                                rawSubtotal, bestResult.getPromotion().discountType(), 
                                bestResult.getPromotion().discountValue(), bestResult.getPromotion().maxDiscount()
                            );
                            if (standAloneDiscount.compareTo(automatedDiscount) > 0) {
                                automatedDiscount = standAloneDiscount;
                                hasAutoDiscount = true;
                            }
                        } else {
                            automatedDiscount = automatedDiscount.add(bestResult.getDiscountAmount());
                            hasAutoDiscount = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi Pricing Engine (Combo/Auto): {}", e.getMessage(), e);
        }

        // --- LEVEL 3: Coupon Discount ---
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (order.getPromotionId() != null) {
            // Kiểm tra điều kiện đơn tối thiểu cho Coupon
            if (rawSubtotal.compareTo(order.getMinOrderAmount() != null ? order.getMinOrderAmount() : BigDecimal.ZERO) < 0) {
                log.info("Đơn hàng không đủ minAmount ({}), gỡ Coupon.", order.getMinOrderAmount());
                order.setPromotionId(null);
                order.setPromotionCode(null);
            } else {
                if (Boolean.FALSE.equals(order.getIsStackable())) {
                    // NON-STACKABLE: So sánh với Auto Discount, cái nào lớn hơn thì lấy
                    BigDecimal rawCouponDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                        rawSubtotal, order.getDiscountType(), order.getDiscountRate(), order.getMaxDiscountValue()
                    ).min(rawSubtotal);
                    
                    if (rawCouponDiscount.compareTo(automatedDiscount) >= 0) {
                        couponDiscount = rawCouponDiscount;
                        automatedDiscount = BigDecimal.ZERO; // Hủy auto discount
                    } else {
                        // Auto discount có lợi hơn, tự động gỡ coupon
                        log.info("Auto discount ({}) tốt hơn Coupon ({}), gỡ Coupon.", automatedDiscount, rawCouponDiscount);
                        order.setPromotionId(null);
                        order.setPromotionCode(null);
                        couponDiscount = BigDecimal.ZERO;
                    }
                } else {
                    // STACKABLE: Tính trên giá trị phần dư sau khi giảm auto (Tránh lỗ)
                    couponDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                        rawSubtotal.subtract(automatedDiscount), 
                        order.getDiscountType(), order.getDiscountRate(), order.getMaxDiscountValue()
                    ).min(rawSubtotal.subtract(automatedDiscount));
                }
            }
        }

        order.setDiscount(automatedDiscount.add(couponDiscount));
        BigDecimal finalTotal = rawSubtotal.subtract(order.getDiscount()).max(BigDecimal.ZERO);
        order.setTotal(finalTotal);
    }

    // Đã thay thế bởi com.fnb.common.util.PricingEngine

    @Transactional
    public void requestPayment(String sessionToken, String paymentMethod) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không hợp lệ hoặc đã hết hạn"));

        // Lấy hóa đơn đang OPEN
        Order order = orderRepository.findFirstBySessionIdAndStatusIn(session.getId(), Arrays.asList("OPEN"))
                .orElseThrow(() -> new BusinessException("Không có hóa đơn nào đang mở để yêu cầu thanh toán!"));

        if (order.getTotal() == null || order.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Hóa đơn chưa có món nào, không thể thanh toán!");
        }

        order.setStatus("PAYMENT_REQUESTED");
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        TableInfo table = session.getTable();
        if (table != null) {
            table.setStatus("PAYMENT_REQUESTED");
            tableRepository.save(table);
        }

        // Bắn Notification cho Cashier (thông qua StaffCall)
        String finalCallType = "PAYMENT_" + paymentMethod; // e.g. PAYMENT_CASH, PAYMENT_QR
        StaffCall call = StaffCall.builder()
                .session(session)
                .table(table)
                .callType(finalCallType)
                .status("PENDING")
                .build();
        staffCallRepository.save(call);

        // Cần Application Context hoặc Service Bean, nhưng do có EventPublisher ta đẩy
        // event là đủ.
        StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                .callId(call.getId()) // Sẽ có ID sau khi save
                .sessionId(session.getId())
                .tableId(table != null ? table.getId() : null)
                .tableNumber(table != null ? table.getNumber() : null)
                .callType(finalCallType)
                .calledAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(event);

        if (table != null) {
            // Bắn thêm event để đồng bộ Sơ đồ bàn POS
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(table.getId())
                    .status("PAYMENT_REQUESTED")
                    .sessionToken(sessionToken)
                    .build());
        }

        // Bắn thêm event đồng bộ trạng thái đơn hàng (OrderDetail)
        applicationEventPublisher.publishEvent(OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .status("PAYMENT_REQUESTED")
                .sessionToken(sessionToken)
                .tableNumber(table != null && table.getNumber() != null ? String.valueOf(table.getNumber()) : null)
                .build());

        log.info("Khách bàn {} yêu cầu thanh toán bằng {}", table != null ? table.getNumber() : "Mang về", paymentMethod);
    }

    @Transactional
    public void handleBankWebhook(String content, BigDecimal amount, String refCode) {
        log.info("Xử lý thanh toán NH tự động: content={}, amount={}, ref={}", content, amount, refCode);

        // 1. Phân tích nội dung chuyển khoản để lấy Order ID.
        // Quy ước: Khách quét mã sẽ có dạng "FNB [SessionToken/ID]" hoặc ID nằm đâu đó
        // trong chuỗi.
        // Ở đây giả sử mã VietQR sinh ra có nhúng ID của Order (dạng UUID) vào content.
        Pattern pattern = Pattern.compile("([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) {
            log.warn("Không tìm thấy UUID hợp lệ trong nội dung chuyển khoản: {}", content);
            return; // Không ném lỗi vì đây là webhook ngân hàng, có thể ai đó chuyển nhầm
        }

        String extractedOrderId = matcher.group(1);
        UUID orderId = UUID.fromString(extractedOrderId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Webhook: Order ID {} không tồn tại trong hệ thống.", extractedOrderId);
            return;
        }

        if ("PAID".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            log.info("Webhook: Order {} đã thanh toán hoặc đã hủy, bỏ qua.", extractedOrderId);
            return;
        }

        // 2. Đối chiếu số tiền
        if (amount.compareTo(order.getTotal()) >= 0) {
            // Khách chuyển đủ hoặc thừa tiền -> TỰ ĐỘNG CHECKOUT
            log.info("Webhook: Đối soát thành công, tự động checkout Order {}", extractedOrderId);

            // Xóa StaffCall pending nếu có (không cần thiết lắm vì bàn sẽ đóng luôn, nhưng
            // clean code)

            // Thực hiện Checkout (Mượn hàm closeOrder hiện tại)
            order.setPaymentMethod("QR_BANK");
            String paymentDetail = String.format("{\"bank_ref\": \"%s\", \"pay_amount\": %s}", refCode,
                    amount.toString());
            order.setPaymentDetail(paymentDetail);
            orderRepository.save(order);

            // Đóng hóa đơn và bắn event
            closeOrder(orderId, true, null, "QR_BANK", paymentDetail);

            // Bắn một Websocket/StaffCall nhỏ về POS báo là "Bàn xyz đã tự đóng do check
            // Bank thành công"
            TableSession session = order.getSession();
            if (session != null && session.getTable() != null) {
                StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                        .callId(UUID.randomUUID())
                        .sessionId(session.getId())
                        .tableId(session.getTable().getId())
                        .tableNumber(session.getTable().getNumber())
                        .callType("BANK_VERIFIED_AUTO_CHECKOUT")
                        .calledAt(LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }
        } else {
            log.warn("Webhook: Order {} cần {} nhưng khách chỉ chuyển {}", extractedOrderId, order.getTotal(), amount);
            // Có thể tạo một StaffCall cảnh báo cho Thu Ngân xử lý tay
        }
    }

    @Transactional
    public String createTakeawayOrder(TakeawayOrderRequest request, String cashierUsername) {
        log.info("Nhận yêu cầu tạo đơn Takeaway từ thu ngân: {}", cashierUsername);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Danh sách món không được để trống.");
        }

        // 1. Tạo Order (MANUAL + PAID vì khách trả tiền luôn)
        Order order = Order.builder()
                .session(null)
                .table(null)
                .source("MANUAL")
                .orderType("TAKEAWAY")
                .status("PAID")
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();
        orderRepository.save(order);

        OrderTicket ticket = OrderTicket.builder()
                .order(order)
                .seqNumber(1)
                .status("PENDING")
                .note(request.getNote())
                .createdBy("CASHIER")
                .build();

        BigDecimal ticketTotal = BigDecimal.ZERO;
        List<OrderTicketItem> ticketItems = new ArrayList<>();

        // 3. Xử lý Items & xác thực giá/options qua MenuServiceClient
        for (var reqItem : request.getItems()) {
            var res = menuServiceClient.getMenuItemById(reqItem.getMenuItemId());
            if (!res.isSuccess() || res.getData() == null) {
                throw new BusinessException("Món ăn không tồn tại hoặc lỗi đồng bộ menu");
            }
            var menuItemInfo = res.getData();
            if (menuItemInfo.isActive() != null && !menuItemInfo.isActive() ||
                    menuItemInfo.isAvailable() != null && !menuItemInfo.isAvailable()) {
                throw new BusinessException("Rất tiếc, món " + menuItemInfo.name() + " hiện đang ngừng phục vụ.");
            }

            // Giá gốc — discount được tính bởi Pricing Engine sau
            BigDecimal actualUnitPrice = menuItemInfo.basePrice();

            OrderTicketItem ticketItem = OrderTicketItem.builder()
                    .ticket(ticket)
                    .menuItemId(reqItem.getMenuItemId())
                    .itemName(menuItemInfo.name())
                    .unitPrice(actualUnitPrice)
                    .quantity(reqItem.getQuantity())
                    .note(reqItem.getNote())
                    .station(menuItemInfo.station())
                    .status("PENDING")
                    .build();

            List<OrderItemOption> itemOptions = new ArrayList<>();
            BigDecimal optionTotal = BigDecimal.ZERO;

            if (reqItem.getOptions() != null && !reqItem.getOptions().isEmpty()) {
                for (var optReq : reqItem.getOptions()) {
                    MenuServiceClient.OptionDetail matchedOption = null;
                    if (menuItemInfo.optionGroups() != null) {
                        for (var group : menuItemInfo.optionGroups()) {
                            if (group.options() != null) {
                                for (var opt : group.options()) {
                                    if (opt.id().equals(optReq.getOptionId())) {
                                        matchedOption = opt;
                                        break;
                                    }
                                }
                            }
                            if (matchedOption != null)
                                break;
                        }
                    }

                    if (matchedOption == null) {
                        throw new BusinessException("Tùy chọn không tồn tại hoặc đã bị xóa!");
                    }

                    if (matchedOption.isAvailable() != null && !matchedOption.isAvailable()) {
                        throw new BusinessException(
                                "Tùy chọn " + matchedOption.name() + " hiện đang tạm ngưng kinh doanh.");
                    }

                    BigDecimal extraPrice = matchedOption.extraPrice() != null ? matchedOption.extraPrice()
                            : BigDecimal.ZERO;
                    OrderItemOption option = OrderItemOption.builder()
                            .ticketItem(ticketItem)
                            .optionName(matchedOption.name())
                            .extraPrice(extraPrice)
                            .build();

                    itemOptions.add(option);
                    optionTotal = optionTotal.add(extraPrice);
                }
            }

            ticketItem.setOptions(itemOptions);
            ticketItems.add(ticketItem);

            BigDecimal itemCost = actualUnitPrice.add(optionTotal)
                    .multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            ticketTotal = ticketTotal.add(itemCost);
        }

        ticket.setItems(ticketItems);
        ticketRepository.save(ticket);

        // Đảm bảo quan hệ hai chiều trong bộ nhớ
        if (order.getTickets() == null) {
            order.setTickets(new ArrayList<>());
        }
        order.getTickets().add(ticket);

        order.setSubtotal(ticketTotal);
        order.setTotal(ticketTotal);

        if (org.springframework.util.StringUtils.hasText(request.getPromotionCode())) {
            try {
                executeApplyPromotion(order, request.getPromotionCode());
            } catch (Exception e) {
                log.warn("Lỗi khi áp dụng mã cho Takeaway: {}", e.getMessage());
                // Tiếp tục tạo đơn nhưng không có khuyến mãi nếu mã lỗi? 
                // Hoặc throw tùy business. Ở đây ta throw để báo lỗi mã sai cho nhân viên.
                throw new BusinessException("Mã giảm giá không hợp lệ cho đơn Takeaway: " + e.getMessage());
            }
        }

        orderRepository.save(order);

        // 4. Bắn OrderCreatedEvent cho KDS bếp
        List<OrderCreatedItemEvent> eventItems = ticketItems.stream().map(ti -> {
            List<OrderCreatedOptionEvent> eventOptions = ti.getOptions().stream()
                    .map(opt -> OrderCreatedOptionEvent.builder()
                            .optionName(opt.getOptionName())
                            .extraPrice(opt.getExtraPrice())
                            .build())
                    .collect(Collectors.toList());

            return OrderCreatedItemEvent.builder()
                    .menuItemId(ti.getMenuItemId())
                    .itemName(ti.getItemName())
                    .quantity(ti.getQuantity())
                    .note(ti.getNote())
                    .station(ti.getStation())
                    .unitPrice(ti.getUnitPrice())
                    .options(eventOptions)
                    .build();
        }).collect(Collectors.toList());

        OrderCreatedEvent createdEvent = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .ticketId(ticket.getId())
                .tableNumber(null) // Hiển thị trên KDS
                .sessionToken(null)
                .note(ticket.getNote())
                .createdAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now())
                .items(eventItems)
                .build();
        applicationEventPublisher.publishEvent(createdEvent);

        // 5. Bắn OrderPaidEvent để ghi nhận doanh thu
        OrderPaidEvent paidEvent = OrderPaidEvent.builder()
                .orderId(order.getId())
                .tableId(null)
                .tableNumber(null)
                .sessionToken(null)
                .paidAt(LocalDateTime.now())
                .build();
        applicationEventPublisher.publishEvent(paidEvent);

        return "Tạo đơn mang đi thành công";
    }

    private void checkAndAutoCancelEmptyOrder(Order order, String reason) {
        if ("CANCELLED".equals(order.getStatus()) || "PAID".equals(order.getStatus())) {
            return;
        }

        boolean allTicketsCancelledOrEmpty = true;
        if (order.getTickets() != null && !order.getTickets().isEmpty()) {
            allTicketsCancelledOrEmpty = order.getTickets().stream()
                    .allMatch(t -> "CANCELLED".equals(t.getStatus()));
        }

        if (allTicketsCancelledOrEmpty) {
            log.info("Order {} đã trống rỗng do tất cả món/phiếu bị hủy. Tự động chuyển Order sang CANCELLED.", order.getId());
            order.setStatus("CANCELLED");
            handleSessionAndTableWhenOrderCancelled(order);
        }
    }

    private void handleSessionAndTableWhenOrderCancelled(Order order) {
        TableSession session = order.getSession();
        if (session == null) return;

        log.info("Order {} bị hủy. Luôn giữ session {} ACTIVE để khách/nhân viên có thể order lại mà không cần mở lại bàn.", 
                order.getId(), session.getSessionToken());

        // Không đóng session, không dời table sang CLEANING nữa.
        // Frontend sẽ tự động fetch lại và API getOrderBySessionToken sẽ tự đẻ ra Order OPEN mới.

        applicationEventPublisher.publishEvent(OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .status("CANCELLED")
                .sessionToken(session.getSessionToken())
                .tableNumber(order.getTable() != null && order.getTable().getNumber() != null ? String.valueOf(order.getTable().getNumber()) : null)
                .build());
    }

    @Transactional(readOnly = true)
    public List<com.fnb.order.dto.response.PosTableResponse> getActiveTakeawayOrders() {
        return orderRepository.findActiveTakeawayOrders();
    }
}
