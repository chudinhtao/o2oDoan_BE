package com.fnb.order.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.TableStatusUpdatedEvent;
import com.fnb.order.entity.Order;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.entity.TableSession;
import com.fnb.order.repository.OrderRepository;
import com.fnb.order.repository.OrderTicketRepository;
import com.fnb.order.repository.TableRepository;
import com.fnb.order.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableActionService {

    private final TableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository ticketRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Chuyển tất cả order từ bàn nguồn (source) sang bàn đích (target).
     * Yêu cầu: Bàn đích phải đang trống (FREE).
     */
    @Transactional
    @CacheEvict(value = "order:sessions", allEntries = true)
    public void transferTable(UUID sourceTableId, UUID targetTableId) {
        TableInfo sourceTable = tableRepository.findById(sourceTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Bàn nguồn không tồn tại"));

        TableInfo targetTable = tableRepository.findById(targetTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Bàn đích không tồn tại"));

        if (!"OCCUPIED".equals(sourceTable.getStatus())) {
            throw new BusinessException("Bàn nguồn phải đang có khách để chuyển");
        }
        if (!"FREE".equals(targetTable.getStatus())) {
            throw new BusinessException("Bàn đích phải còn trống để chuyển tới");
        }

        // 1. Tìm session đang mở của bàn nguồn
        TableSession activeSession = sessionRepository.findActiveSessionByTableId(sourceTableId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiên hoạt động của bàn nguồn"));

        // 2. Cập nhật session sang bàn mới
        activeSession.setTable(targetTable);
        sessionRepository.save(activeSession);

        // 3. Đổi trạng thái bàn
        sourceTable.setStatus("CLEANING"); // Chuyển đi rồi thì dọn bàn cũ
        tableRepository.save(sourceTable);

        // Kéo theo tất cả các bàn con (nếu bàn nguồn là bàn cha) cũng phải dọn dẹp
        int cleanedChildren = tableRepository.cleanChildTables(sourceTableId);
        if (cleanedChildren > 0) {
            log.info("Bàn nguồn {} có {} bàn ghép con. Đã tự động chuyển các bàn ghép sang CLEANING.", sourceTable.getNumber(), cleanedChildren);
            // Gửi event REFRESH_ALL để POS fetch lại toàn bộ map (vì có nhiều bàn thay đổi)
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                .tableId(sourceTableId)
                .status("REFRESH_ALL")
                .build());
        }

        targetTable.setStatus("OCCUPIED"); // Bàn mới có người
        tableRepository.save(targetTable);

        // (Tuỳ chọn cập nhật lại field "table" bên trong Order nếu có lưu cứng table
        // lúc tạo order)
        orderRepository.findFirstBySessionIdAndStatusIn(activeSession.getId(), java.util.Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .ifPresent(order -> {
                    order.setTable(targetTable);
                    orderRepository.save(order);
                });

        log.info("Chuyển bàn thành công: từ Bàn {} sang Bàn {}", sourceTable.getNumber(), targetTable.getNumber());

        // Bắn event cập nhật WS cho tất cả POS
        publishTableStatus(sourceTable.getId(), "CLEANING");
        publishTableStatus(targetTable.getId(), "OCCUPIED");
    }

    private void publishTableStatus(UUID tableId, String status) {
        applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                .tableId(tableId)
                .status(status)
                .build());
    }

    /**
     * Gộp NHIỀU bàn nguồn vào 1 bàn đích.
     * Bàn đích: giữ nguyên, nhận tất cả order/ticket.
     * Các bàn nguồn: chuyển hết order → dọn bàn.
     */
    @Transactional
    @CacheEvict(value = "order:sessions", allEntries = true)
    public void mergeTables(List<UUID> sourceTableIds, UUID targetTableId) {
        // --- Validate bàn đích ---
        TableInfo targetTable = tableRepository.findById(targetTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Bàn đích không tồn tại"));

        if (!"OCCUPIED".equals(targetTable.getStatus())) {
            throw new BusinessException("Bàn đích phải đang có khách mới gộp được");
        }

        TableSession targetSession = sessionRepository.findActiveSessionByTableId(targetTableId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiên của bàn đích"));

        Order targetOrder = orderRepository.findFirstBySessionIdAndStatusIn(targetSession.getId(), java.util.Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                .orElse(null);

        // Đếm ticket hiện tại của bàn đích để tính offset seqNumber
        int runningSeqOffset = targetOrder != null
                ? ticketRepository.findByOrderId(targetOrder.getId()).size()
                : 0;

        // --- Lặp qua từng bàn nguồn ---
        for (UUID sourceTableId : sourceTableIds) {
            if (sourceTableId.equals(targetTableId)) {
                log.warn("Bỏ qua bàn trùng với bàn đích: {}", sourceTableId);
                continue;
            }

            TableInfo sourceTable = tableRepository.findById(sourceTableId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bàn nguồn không tồn tại: " + sourceTableId));

            if (!"OCCUPIED".equals(sourceTable.getStatus())) {
                throw new BusinessException("Bàn " + sourceTable.getNumber() + " phải đang có khách mới được gộp");
            }

            TableSession sourceSession = sessionRepository.findActiveSessionByTableId(sourceTableId)
                    .orElseThrow(
                            () -> new BusinessException("Không tìm thấy phiên của bàn " + sourceTable.getNumber()));

            Order sourceOrder = orderRepository.findFirstBySessionIdAndStatusIn(sourceSession.getId(), java.util.Arrays.asList("OPEN", "PAYMENT_REQUESTED"))
                    .orElse(null);

            // -- Gộp Order --
            if (sourceOrder != null) {
                if (targetOrder == null) {
                    // Bàn đích chưa có order → lấy luôn order nguồn làm order đích
                    sourceOrder.setSession(targetSession);
                    sourceOrder.setTable(targetTable);
                    orderRepository.save(sourceOrder);
                    targetOrder = sourceOrder;
                    runningSeqOffset = ticketRepository.findByOrderId(targetOrder.getId()).size();
                } else {
                    // Cả 2 đều có order → chuyển ticket qua native SQL (bypass orphanRemoval)
                    int sourceTicketCount = ticketRepository.findByOrderId(sourceOrder.getId()).size();
                    ticketRepository.transferTicketsToOrder(
                            sourceOrder.getId(), targetOrder.getId(), runningSeqOffset);
                    runningSeqOffset += sourceTicketCount;

                    // Cộng dồn tiền
                    targetOrder.setSubtotal(targetOrder.getSubtotal().add(sourceOrder.getSubtotal()));
                    targetOrder.setTotal(targetOrder.getTotal().add(sourceOrder.getTotal()));
                    orderRepository.save(targetOrder);

                    // Đánh dấu order nguồn đã gộp
                    sourceOrder.getTickets().clear();
                    sourceOrder.setStatus("MERGED");
                    orderRepository.save(sourceOrder);
                }
            }

            // -- Đóng session bàn nguồn, trả bàn --
            sourceSession.setStatus("MERGED");
            sourceSession.setClosedAt(LocalDateTime.now());
            sessionRepository.save(sourceSession);

            sourceTable.setStatus("MERGED");
            sourceTable.setParentTableId(targetTableId);
            tableRepository.save(sourceTable);

            publishTableStatus(sourceTable.getId(), "MERGED");

            log.info("Gộp bàn: dồn từ Bàn {} sang Bàn {}", sourceTable.getNumber(), targetTable.getNumber());
        }

        publishTableStatus(targetTable.getId(), "OCCUPIED");

        log.info("Hoàn tất gộp {} bàn vào Bàn {}", sourceTableIds.size(), targetTable.getNumber());
    }
}
