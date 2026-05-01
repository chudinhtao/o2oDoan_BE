package com.fnb.order.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.TableStatusUpdatedEvent;
import com.fnb.order.dto.response.SessionResponse;
import com.fnb.order.entity.Order;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.entity.TableSession;
import com.fnb.order.repository.OrderRepository;
import com.fnb.order.repository.TableRepository;
import com.fnb.order.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final OrderRepository orderRepository;
    private final TableSessionRepository sessionRepository;
    private final TableRepository tableRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public SessionResponse openSession(String qrToken) {
        // Lock bàn bằng PESSIMISTIC_WRITE → request thứ 2 phải chờ request 1 commit
        // xong
        TableInfo table = tableRepository.findByQrTokenForUpdate(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Mã QR không hợp lệ hoặc đã hết hạn"));
        return processOpenSession(table, "QR");
    }

    @Transactional
    public SessionResponse openSessionByTableId(UUID tableId) {
        // Lock bàn bằng PESSIMISTIC_WRITE → tránh 2 POS/customer mở cùng lúc
        TableInfo table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn"));
        return processOpenSession(table, "MANUAL");
    }

    private SessionResponse processOpenSession(TableInfo table, String source) {
        if (!table.isActive()) {
            throw new BusinessException("Bàn này hiện không hoạt động");
        }

        if ("CLEANING".equals(table.getStatus())) {
            throw new BusinessException("Bàn đang được dọn dẹp, xin vui lòng chờ");
        }

        // --- MERGED REDIRECTION LOGIC ---
        // Nếu bàn đang được ghép vào bàn khác, tự động trỏ về bàn gốc (Bàn cha)
        if ("MERGED".equals(table.getStatus()) && table.getParentTableId() != null) {
            log.info("Bàn {} đang được ghép. Redirecting sang bàn gốc {}", table.getNumber(), table.getParentTableId());
            table = tableRepository.findByIdForUpdate(table.getParentTableId())
                    .orElseThrow(() -> new BusinessException("Bàn gốc đã bị xóa hoặc không hợp lệ"));
        }

        // Bàn đã bị lock (FOR UPDATE) → lúc này chỉ có 1 thread duy nhất chạy tới đây
        // Nếu bàn đang OCCUPIED hoặc PAYMENT_REQUESTED, trả về session hiện tại để
        // chung mâm
        Optional<TableSession> activeSession = sessionRepository.findActiveSessionByTableId(table.getId());
        if (activeSession.isPresent()) {
            log.info("Khách/Thu ngân join vào bàn {}, session {}", table.getNumber(),
                    activeSession.get().getSessionToken());

            // Xử lý lỗi lệch đồng bộ: Nếu session ACTIVE mà bàn vẫn FREE, cập nhật lại
            // thành OCCUPIED
            if ("FREE".equals(table.getStatus())) {
                table.setStatus("OCCUPIED");
                tableRepository.save(table);
            }

            return toResponse(activeSession.get());
        }

        // Tạo mới session (đổi bàn sang OCCUPIED)
        String sessionToken = UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();

        TableSession session = TableSession.builder()
                .table(table)
                .sessionToken(sessionToken)
                .expiresAt(LocalDateTime.now().plusHours(4))
                .build();

        table.setStatus("OCCUPIED");
        tableRepository.save(table);

        TableSession savedSession = sessionRepository.save(session);

        // TẠO LUÔN ORDER NGAY KHI MỞ BÀN (Theo kiến trúc mới POS/Customer-friendly)
        Order order = Order.builder()
                .session(savedSession)
                .table(table)
                .source(source)
                .orderType("DINE_IN")
                .status("OPEN")
                .subtotal(java.math.BigDecimal.ZERO)
                .total(java.math.BigDecimal.ZERO)
                .build();
        orderRepository.save(order);

        // Bắn event để POS biết bàn đã đổi trạng thái (WS)
        applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                .tableId(table.getId())
                .status("OCCUPIED")
                .sessionToken(sessionToken)
                .build());

        return toResponse(savedSession);
    }

    @Transactional
    public SessionResponse openTakeawaySession() {
        String sessionToken = UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();

        TableSession session = TableSession.builder()
                .table(null) // Không gắn bàn
                .sessionToken(sessionToken)
                .expiresAt(LocalDateTime.now().plusHours(4))
                .status("ACTIVE")
                .build();
        TableSession savedSession = sessionRepository.save(session);

        // Tạo Order trống ngay lập tức
        Order order = Order.builder()
                .session(savedSession)
                .table(null)
                .source("MANUAL")
                .orderType("TAKEAWAY")
                .status("OPEN")
                .subtotal(java.math.BigDecimal.ZERO)
                .total(java.math.BigDecimal.ZERO)
                .build();
        orderRepository.save(order);

        log.info("Đã tạo phiên mạng đi (Takeaway) thành công: {}", sessionToken);
        return toResponse(savedSession);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "order:sessions", key = "#sessionToken")
    public SessionResponse getSessionCurrent(String sessionToken) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại hoặc đã hết hạn"));
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getActiveSessionByTableId(UUID tableId) {
        return sessionRepository.findActiveSessionByTableId(tableId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    @CacheEvict(value = "order:sessions", key = "#sessionToken")
    public void closeSession(String sessionToken) {
        TableSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private SessionResponse toResponse(TableSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .tableId(session.getTable() != null ? session.getTable().getId() : null)
                .tableNumber(session.getTable() != null ? session.getTable().getNumber() : 0)
                .sessionToken(session.getSessionToken())
                .status(session.getStatus())
                .openedAt(session.getOpenedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}
