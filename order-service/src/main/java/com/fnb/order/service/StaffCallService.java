package com.fnb.order.service;

import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.StaffCallCreatedEvent;
import com.fnb.order.dto.request.StaffCallRequest;
import com.fnb.order.dto.response.StaffCallResponse;
import com.fnb.order.entity.StaffCall;
import com.fnb.order.entity.TableSession;
import com.fnb.order.repository.TableSessionRepository;
import com.fnb.order.repository.StaffCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffCallService {

        private final StaffCallRepository staffCallRepository;
        private final TableSessionRepository sessionRepository;
        private final ApplicationEventPublisher applicationEventPublisher;

        @Transactional
        public void createCall(String sessionToken, StaffCallRequest request) {
                TableSession session = sessionRepository.findBySessionToken(sessionToken)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Session không hợp lệ hoặc đã hết hạn"));

                StaffCall call = StaffCall.builder()
                                .session(session)
                                .table(session.getTable())
                                .callType(request.getCallType())
                                .status("PENDING")
                                .build();

                staffCallRepository.save(call);

                StaffCallCreatedEvent event = StaffCallCreatedEvent
                                .builder()
                                .callId(call.getId())
                                .sessionId(session.getId())
                                .tableId(session.getTable() != null ? session.getTable().getId() : null)
                                .tableNumber(session.getTable() != null ? session.getTable().getNumber() : null)
                                .callType(call.getCallType())
                                .calledAt(call.getCreatedAt() != null ? call.getCreatedAt() : LocalDateTime.now())
                                .build();
                applicationEventPublisher.publishEvent(event);

                log.info("Bàn/Mang về {} đang gọi phục vụ. Yêu cầu: {}", session.getTable() != null ? session.getTable().getNumber() : "Mang về", request.getCallType());
        }

        @Transactional(readOnly = true)
        public List<StaffCallResponse> getActiveCalls() {
                return staffCallRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void resolveCall(UUID id, UUID resolvedBy) {
                StaffCall call = staffCallRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Ủa, gọi phục vụ này không tồn tại!"));

                call.setStatus("RESOLVED");
                call.setResolvedAt(LocalDateTime.now());
                call.setResolvedBy(resolvedBy);
                staffCallRepository.save(call);
                log.info("Đã phục vụ xong yêu cầu {} cho bàn/mang về {} bởi nhân viên {}", 
                    call.getCallType(), call.getTable() != null ? call.getTable().getNumber() : "Mang về", resolvedBy);
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
                                .build();
        }
}
