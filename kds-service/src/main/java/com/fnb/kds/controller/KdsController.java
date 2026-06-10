package com.fnb.kds.controller;

import com.fnb.kds.entity.KdsOrderTicket;
import com.fnb.kds.entity.KdsOrderTicketItem;
import com.fnb.kds.repository.KdsOrderTicketItemRepository;
import com.fnb.kds.repository.KdsOrderTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.fnb.kds.dto.event.KdsTicketUpdatedEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.fnb.kds.entity.KdsOrder;
import com.fnb.kds.repository.KdsOrderRepository;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/kds")
@RequiredArgsConstructor
@Transactional
public class KdsController {

    private final KdsOrderTicketRepository kdsTicketRepository;
    private final KdsOrderTicketItemRepository kdsTicketItemRepository;
    private final KdsOrderRepository kdsOrderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @GetMapping("/tickets/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveTickets(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom) {
        List<KdsOrderTicket> tickets = kdsTicketRepository.findByStatusInOrderByCreatedAtAsc(Arrays.asList("PENDING", "PREPARING", "DONE", "SERVED", "CANCELLED", "RETURNED"));
        
        LocalDateTime cutoff = startFrom != null ? startFrom : LocalDateTime.now().minusHours(12);

        List<Map<String, Object>> responses = tickets.stream()
            .filter(t -> {
                boolean isActive = "PENDING".equals(t.getStatus()) || "PREPARING".equals(t.getStatus());
                boolean isRecent = t.getCreatedAt() != null && t.getCreatedAt().isAfter(cutoff);
                return isActive || isRecent;
            })
            .map(ticket -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ticket.getId());
            map.put("ticketId", ticket.getId());
            map.put("orderId", ticket.getOrder() != null ? ticket.getOrder().getId() : null);
            map.put("tableNumber", ticket.getOrder() != null && ticket.getOrder().getTable() != null ? ticket.getOrder().getTable().getNumber() : null);
            map.put("status", ticket.getStatus());
            map.put("createdAt", ticket.getCreatedAt());
            map.put("note", ticket.getNote());
            
            List<Map<String, Object>> items = ticket.getItems().stream()
                .map(item -> {
                Map<String, Object> iMap = new HashMap<>();
                iMap.put("id", item.getId());
                iMap.put("itemId", item.getId());
                iMap.put("itemName", item.getItemName());
                iMap.put("quantity", item.getQuantity());
                iMap.put("status", item.getStatus());
                iMap.put("note", item.getNote());
                iMap.put("station", item.getStation());
                iMap.put("kitchenAlertSent", item.getKitchenAlertSent());
                
                // Add options as simple string array
                if (item.getOptions() != null) {
                    List<String> options = item.getOptions().stream()
                        .map(opt -> opt.getOptionName())
                        .collect(Collectors.toList());
                    iMap.put("options", options);
                }
                
                return iMap;
            }).collect(Collectors.toList());
            
            map.put("items", items);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private int getStatusLevel(String status) {
        if (status == null) return 0;
        switch (status.toUpperCase()) {
            case "PENDING": return 1;
            case "PREPARING": return 2;
            case "READY":
            case "DONE": return 3;
            case "SERVED":
            case "COMPLETED": return 4;
            default: return 0;
        }
    }

    @PutMapping("/items/{id}/status")
    public ResponseEntity<?> updateItemStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id, 
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }
        
        return kdsTicketItemRepository.findById(id).map(item -> {
            String currentStatus = item.getStatus();
            String upperNew = newStatus.toUpperCase();
            
            if ("CANCELLED".equals(upperNew)) {
                if ("DONE".equalsIgnoreCase(currentStatus) || "SERVED".equalsIgnoreCase(currentStatus) || "COMPLETED".equalsIgnoreCase(currentStatus)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy món đã hoàn thành hoặc đã phục vụ"));
                }
                
                // Phase 2: Ghi nhận ai đã hủy món trên KDS
                if (userId != null) {
                    try {
                        item.setCancelledBy(UUID.fromString(userId));
                    } catch (Exception e) { /* ignore invalid uuid */ }
                }
                
                // Subtract money from order
                KdsOrderTicket ticket = item.getTicket();
                if (ticket != null && ticket.getOrder() != null) {
                    KdsOrder order = ticket.getOrder();
                    BigDecimal extra = item.getOptions() != null ? 
                         item.getOptions().stream().map(o -> o.getExtraPrice() != null ? o.getExtraPrice() : BigDecimal.ZERO)
                             .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
                    BigDecimal unit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    BigDecimal itemValue = unit.add(extra).multiply(BigDecimal.valueOf(qty));
                    
                    if (order.getSubtotal() != null) order.setSubtotal(order.getSubtotal().subtract(itemValue));
                    if (order.getTotal() != null) order.setTotal(order.getTotal().subtract(itemValue));
                    kdsOrderRepository.save(order);
                }
                int currentLevel = getStatusLevel(currentStatus);
                int newLevel = getStatusLevel(upperNew);
                if (newLevel > 0 && currentLevel > newLevel) {
                    // Cho phép quay ngược từ DONE/READY về PREPARING/PENDING đề phòng bếp ấn nhầm
                    boolean isUndoFromDone = ("DONE".equalsIgnoreCase(currentStatus) || "READY".equalsIgnoreCase(currentStatus)) 
                            && ("PREPARING".equalsIgnoreCase(upperNew) || "PENDING".equalsIgnoreCase(upperNew));
                    if (!isUndoFromDone) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Cấm quay ngược trạng thái (không thể chuyển từ " + currentStatus + " về " + upperNew + ")"));
                    }
                }
            }

            item.setStatus(upperNew);
            
            // Phase 1 KPI: Ghi nhan ai da lam xong va luc nao
            if ("DONE".equals(upperNew) || "COMPLETED".equals(upperNew)) {
                item.setCompletedAt(LocalDateTime.now());
                if (userId != null) {
                    try {
                        item.setPreparedBy(UUID.fromString(userId));
                    } catch (Exception e) { /* ignore invalid uuid */ }
                }
            }
            
            kdsTicketItemRepository.save(item);
            
            // Check ticket state
            KdsOrderTicket ticket = item.getTicket();
            long totalItems = ticket.getItems().size();
            long cancelledItems = ticket.getItems().stream().filter(i -> "CANCELLED".equals(i.getStatus()) || "RETURNED".equals(i.getStatus())).count();
            
            boolean allCancelled = (totalItems > 0 && cancelledItems == totalItems);
            
            boolean allDone = !allCancelled && ticket.getItems().stream()
                    .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"RETURNED".equals(i.getStatus()))
                    .allMatch(i -> ("DONE".equalsIgnoreCase(i.getStatus()) || "SERVED".equalsIgnoreCase(i.getStatus()) || "COMPLETED".equalsIgnoreCase(i.getStatus())));
            
            boolean hasAnyProgress = ticket.getItems().stream()
                    .anyMatch(i -> "PREPARING".equalsIgnoreCase(i.getStatus()) || "DONE".equalsIgnoreCase(i.getStatus()) || "SERVED".equalsIgnoreCase(i.getStatus()) || "COMPLETED".equalsIgnoreCase(i.getStatus()));

            Integer tableNumber = ticket.getOrder() != null && ticket.getOrder().getTable() != null ? ticket.getOrder().getTable().getNumber() : null;

            String oldTicketStatus = ticket.getStatus();
            boolean ticketChanged = false;

            if (allCancelled && !"CANCELLED".equalsIgnoreCase(oldTicketStatus)) {
                ticket.setStatus("CANCELLED");
                ticketChanged = true;
            } else if (allDone && !"DONE".equalsIgnoreCase(oldTicketStatus)) {
                ticket.setStatus("DONE");
                ticketChanged = true;
            } else if (!allDone && "DONE".equalsIgnoreCase(oldTicketStatus)) {
                // Nếu vé đang ở DONE nhưng giờ có món chưa xong (do Undo món), kéo vé lại về PREPARING
                ticket.setStatus("PREPARING");
                ticketChanged = true;
            } else if (hasAnyProgress && "PENDING".equalsIgnoreCase(oldTicketStatus)) {
                ticket.setStatus("PREPARING");
                ticketChanged = true;
            }

            String sessionToken = ticket.getOrder() != null && ticket.getOrder().getSession() != null
                    ? ticket.getOrder().getSession().getSessionToken() : null;

            if (ticketChanged) {
                kdsTicketRepository.save(ticket);
                publishEvent(ticket.getId(), null, ticket.getStatus(), tableNumber, "TICKET", sessionToken);
            } 
            
            // Always publish item event
            publishEvent(ticket.getId(), item.getId(), upperNew, tableNumber, "ITEM", sessionToken);
            
            return ResponseEntity.ok(Map.of("message", "Item updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<?> updateTicketStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id, 
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }
        
        return kdsTicketRepository.findById(id).map(ticket -> {
            String upperNew = newStatus.toUpperCase();

            if ("CANCELLED".equals(upperNew)) {
                boolean hasDoneItem = ticket.getItems().stream()
                        .anyMatch(i -> "DONE".equalsIgnoreCase(i.getStatus()) || "SERVED".equalsIgnoreCase(i.getStatus()) || "COMPLETED".equalsIgnoreCase(i.getStatus()));
                if (hasDoneItem) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy nguyên vé vì đã có món hoàn thành/phục vụ"));
                }
            }

            ticket.setStatus(upperNew);
            // Also update all items if marking whole ticket
            if ("DONE".equals(upperNew) || "CANCELLED".equals(upperNew)) {
                KdsOrder order = ticket.getOrder();
                boolean needsSave = false;
                
                for (KdsOrderTicketItem i : ticket.getItems()) {
                    if (!"CANCELLED".equalsIgnoreCase(i.getStatus())) {
                        
                        if ("CANCELLED".equals(upperNew) && order != null) {
                            BigDecimal extra = i.getOptions() != null ? 
                                 i.getOptions().stream().map(o -> o.getExtraPrice() != null ? o.getExtraPrice() : BigDecimal.ZERO)
                                     .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
                            BigDecimal unit = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                            int qty = i.getQuantity() != null ? i.getQuantity() : 1;
                            BigDecimal itemValue = unit.add(extra).multiply(BigDecimal.valueOf(qty));
                            
                            if (order.getSubtotal() != null) order.setSubtotal(order.getSubtotal().subtract(itemValue));
                            if (order.getTotal() != null) order.setTotal(order.getTotal().subtract(itemValue));
                            needsSave = true;
                        }
                        
                        i.setStatus(upperNew);
                        if ("CANCELLED".equals(upperNew)) {
                            if (userId != null) {
                                try {
                                    i.setCancelledBy(UUID.fromString(userId));
                                } catch (Exception e) { /* ignore */ }
                            }
                        }
                        if ("DONE".equals(upperNew) || "COMPLETED".equals(upperNew)) {
                            i.setCompletedAt(LocalDateTime.now());
                            if (userId != null) {
                                try {
                                    i.setPreparedBy(UUID.fromString(userId));
                                } catch (Exception e) { /* ignore */ }
                            }
                        }
                    }
                }
                
                if (needsSave && order != null) {
                    kdsOrderRepository.save(order);
                }
            }
            kdsTicketRepository.save(ticket);
            
            Integer tableNumber = ticket.getOrder() != null && ticket.getOrder().getTable() != null ? ticket.getOrder().getTable().getNumber() : null;
            String sessionToken = ticket.getOrder() != null && ticket.getOrder().getSession() != null
                    ? ticket.getOrder().getSession().getSessionToken() : null;
            publishEvent(ticket.getId(), null, upperNew, tableNumber, "TICKET", sessionToken);
            
            return ResponseEntity.ok(Map.of("message", "Ticket updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void publishEvent(UUID ticketId, UUID itemId, String status, Integer tableNumber, String type, String sessionToken) {
        KdsTicketUpdatedEvent event = KdsTicketUpdatedEvent.builder()
                .ticketId(ticketId)
                .itemId(itemId)
                .sessionToken(sessionToken)
                .status(status)
                .tableNumber(tableNumber)
                .type(type)
                .updatedAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send("ticket.updated", ticketId != null ? ticketId.toString() : UUID.randomUUID().toString(), event);
    }
}
