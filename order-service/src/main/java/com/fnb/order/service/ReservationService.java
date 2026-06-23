package com.fnb.order.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.order.dto.request.AssignTableRequest;
import com.fnb.order.dto.request.ReservationRequest;
import com.fnb.order.dto.response.ReservationResponse;
import com.fnb.order.entity.Reservation;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.repository.ReservationRepository;
import com.fnb.order.repository.TableRepository;
import com.fnb.order.dto.event.TableStatusUpdatedEvent;
import com.fnb.order.entity.Order;
import com.fnb.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final SessionService sessionService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public boolean checkCapacity(LocalDateTime time, int partySize) {
        // Tương lai: Cập nhật thuật toán tính toán Capacity dựa vào tổng sức chứa (sum capacity) của TableRepository
        // Hiện tại trả về true để flow booking hoạt động.
        return true; 
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest req) {
        int adult = req.getAdultCount() != null ? req.getAdultCount() : (req.getPartySize() != null ? req.getPartySize() : 2);
        int child = req.getChildrenCount() != null ? req.getChildrenCount() : 0;
        int party = adult + child;

        Reservation res = Reservation.builder()
                .customerName(req.getCustomerName())
                .customerPhone(req.getCustomerPhone())
                .partySize(party)
                .adultCount(adult)
                .childrenCount(child)
                .bookingTime(req.getBookingTime())
                .preOrderDraft(req.getPreOrderDraft())
                .note(req.getNote())
                .depositAmount(req.getDepositAmount() != null ? req.getDepositAmount() : java.math.BigDecimal.ZERO)
                .status("PENDING")
                .build();
        return toResponse(reservationRepository.save(res));
    }

    @Transactional(readOnly = true)
    public com.fnb.common.dto.PageResponse<ReservationResponse> getStaffReservations(
            String status, String keyword, LocalDateTime start, LocalDateTime end,
            org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.jpa.domain.Specification<Reservation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (StringUtils.hasText(keyword)) {
                String searchKw = "%" + keyword.toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate namePredicate = cb.like(cb.lower(root.get("customerName")), searchKw);
                jakarta.persistence.criteria.Predicate phonePredicate = cb.like(root.get("customerPhone"), searchKw);
                predicates.add(cb.or(namePredicate, phonePredicate));
            }
            
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingTime"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingTime"), end));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        org.springframework.data.domain.Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        List<ReservationResponse> list = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        
        return new com.fnb.common.dto.PageResponse<>(
                list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
    }

    @Transactional
    public ReservationResponse updateReservation(UUID id, com.fnb.order.dto.request.UpdateReservationRequest req) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
        if (req.getCustomerName() != null) res.setCustomerName(req.getCustomerName());
        if (req.getCustomerPhone() != null) res.setCustomerPhone(req.getCustomerPhone());
        
        if (req.getAdultCount() != null) res.setAdultCount(req.getAdultCount());
        if (req.getChildrenCount() != null) res.setChildrenCount(req.getChildrenCount());
        
        if (req.getPartySize() != null) {
            res.setPartySize(req.getPartySize());
        } else if (req.getAdultCount() != null || req.getChildrenCount() != null) {
            int a = res.getAdultCount() != null ? res.getAdultCount() : 0;
            int c = res.getChildrenCount() != null ? res.getChildrenCount() : 0;
            res.setPartySize(a + c);
        }
        if (req.getBookingTime() != null) res.setBookingTime(req.getBookingTime());
        if (req.getNote() != null) res.setNote(req.getNote());
        if (req.getPreOrderDraft() != null) res.setPreOrderDraft(req.getPreOrderDraft());
        if (req.getDepositAmount() != null) res.setDepositAmount(req.getDepositAmount());
        
        return toResponse(reservationRepository.save(res));
    }

    @Transactional
    public ReservationResponse assignTables(UUID id, AssignTableRequest req) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
        
        List<TableInfo> oldTables = res.getTables();
        List<TableInfo> newTables = tableRepository.findAllById(req.getTableIds());
        
        // Nhả bàn cũ không còn trong danh sách mới
        for (TableInfo oldTable : oldTables) {
            if (!newTables.contains(oldTable)) {
                oldTable.setStatus("FREE");
                tableRepository.save(oldTable);
                applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                        .tableId(oldTable.getId()).status("FREE").build());
            }
        }
        
        // Gán bàn mới
        for (TableInfo newTable : newTables) {
            if (!oldTables.contains(newTable)) {
                if (!"FREE".equals(newTable.getStatus())) {
                    throw new BusinessException("Bàn " + newTable.getNumber() + " đang không trống!");
                }
                newTable.setStatus("RESERVED");
                tableRepository.save(newTable);
                applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                        .tableId(newTable.getId()).status("RESERVED").build());
            }
        }
        
        res.setTables(newTables);
        res.setStatus("CONFIRMED");
        return toResponse(reservationRepository.save(res));
    }

    @Transactional
    public void cancelReservation(UUID id, com.fnb.order.dto.request.CancelReservationRequest req) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
        
        if (req != null && "NO_SHOW".equals(req.getStatus())) {
            res.setStatus("NO_SHOW");
        } else {
            res.setStatus("CANCELLED");
        }
        
        if (req != null && StringUtils.hasText(req.getReason())) {
            res.setNote((res.getNote() != null ? res.getNote() + " | " : "") + "Lý do hủy: " + req.getReason());
        }

        if (req != null && StringUtils.hasText(req.getRefundStatus())) {
            res.setRefundStatus(req.getRefundStatus());
        }
        
        for (TableInfo t : res.getTables()) {
            if ("RESERVED".equals(t.getStatus())) {
                t.setStatus("FREE");
                tableRepository.save(t);
                applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(t.getId()).status("FREE").build());
            }
        }
        res.getTables().clear();
        reservationRepository.save(res);
    }
    
    @Transactional
    public void checkIn(UUID id) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
        
        if (res.getTables().isEmpty()) {
            throw new BusinessException("Vui lòng xếp bàn (Assign Tables) trước khi Check-in");
        }
        
        res.setStatus("COMPLETED");
        reservationRepository.save(res);
        
        // Mở session trên bàn đầu tiên (Primary table)
        TableInfo primaryTable = res.getTables().get(0);
        
        // Tạm thời trả status về FREE để pass qua logic chặn RESERVED của SessionService
        primaryTable.setStatus("FREE");
        tableRepository.save(primaryTable);
        com.fnb.order.dto.response.SessionResponse sessionRes = sessionService.openSessionByTableId(primaryTable.getId());
        
        // Explicitly publish event to guarantee real-time updates for the primary table on POS
        applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                .tableId(primaryTable.getId())
                .status("OCCUPIED")
                .sessionToken(sessionRes.getSessionToken())
                .build());
        
        // Cập nhật Deposit Amount vào Order nếu có cọc
        if (res.getDepositAmount() != null && res.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            orderRepository.findBySession_SessionToken(sessionRes.getSessionToken()).ifPresent(order -> {
                order.setDepositAmount(res.getDepositAmount());
                // Total calculation usually handled by OrderService, but init total correctly
                orderRepository.save(order);
                log.info("Đã chuyển {} tiền cọc vào Bill của Session {}", res.getDepositAmount(), sessionRes.getSessionToken());
            });
        }
        
        // Xử lý các bàn phụ: Tự động gộp (MERGED) vào bàn chính
        for (int i = 1; i < res.getTables().size(); i++) {
            TableInfo childTable = res.getTables().get(i);
            childTable.setStatus("MERGED");
            childTable.setParentTableId(primaryTable.getId());
            tableRepository.save(childTable);
            
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(childTable.getId()).status("MERGED").build());
        }
        
        // Xử lý đẩy mảng JSON preOrderDraft xuống bếp (nếu khách có chọn món trước)
        if (StringUtils.hasText(res.getPreOrderDraft())) {
            try {
                List<com.fnb.order.dto.request.TicketItemRequest> preOrderItems = objectMapper.readValue(
                        res.getPreOrderDraft(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<com.fnb.order.dto.request.TicketItemRequest>>() {}
                );
                if (preOrderItems != null && !preOrderItems.isEmpty()) {
                    for (com.fnb.order.dto.request.TicketItemRequest itemReq : preOrderItems) {
                        cartService.addItemToCart(sessionRes.getSessionToken(), itemReq);
                    }
                    com.fnb.order.dto.request.TicketRequest ticketReq = new com.fnb.order.dto.request.TicketRequest();
                    ticketReq.setNote("Món đặt trước (Pre-order)");
                    orderService.submitTicket(sessionRes.getSessionToken(), ticketReq);
                    log.info("Đã đẩy tự động {} món pre-order xuống bếp cho Booking {}", preOrderItems.size(), id);
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý đẩy preOrderDraft xuống bếp cho Booking {}", id, e);
            }
        }
    }

    @Transactional(readOnly = true)
    public com.fnb.common.dto.PageResponse<ReservationResponse> getAdminReservations(
            String status, String phone, LocalDateTime from, LocalDateTime to,
            Boolean hasDeposit, String refundStatus,
            org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.jpa.domain.Specification<Reservation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(status)) {
                if (status.contains(",")) {
                    List<String> statusList = java.util.Arrays.asList(status.split(","));
                    predicates.add(root.get("status").in(statusList));
                } else {
                    predicates.add(cb.equal(root.get("status"), status));
                }
            }
            if (StringUtils.hasText(phone)) {
                predicates.add(cb.like(root.get("customerPhone"), "%" + phone + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingTime"), to));
            }
            if (Boolean.TRUE.equals(hasDeposit)) {
                predicates.add(cb.greaterThan(root.get("depositAmount"), java.math.BigDecimal.ZERO));
            }
            if (StringUtils.hasText(refundStatus) && !"ALL".equalsIgnoreCase(refundStatus)) {
                predicates.add(cb.equal(root.get("refundStatus"), refundStatus));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        org.springframework.data.domain.Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        List<ReservationResponse> list = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new com.fnb.common.dto.PageResponse<>(
                list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID id) {
        return reservationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
    }

    @Transactional
    public void updateReservationStatus(UUID id, com.fnb.order.dto.request.UpdateReservationStatusRequest req) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy Booking"));
        res.setStatus(req.getStatus());
        reservationRepository.save(res);
    }

    private ReservationResponse toResponse(Reservation res) {
        return ReservationResponse.builder()
                .id(res.getId())
                .customerName(res.getCustomerName())
                .customerPhone(res.getCustomerPhone())
                .partySize(res.getPartySize())
                .adultCount(res.getAdultCount())
                .childrenCount(res.getChildrenCount())
                .bookingTime(res.getBookingTime())
                .status(res.getStatus())
                .depositAmount(res.getDepositAmount())
                .refundStatus(res.getRefundStatus())
                .preOrderDraft(res.getPreOrderDraft())
                .note(res.getNote())
                .assignedTableNumbers(res.getTables().stream().map(TableInfo::getNumber).collect(Collectors.toList()))
                .build();
    }
}
