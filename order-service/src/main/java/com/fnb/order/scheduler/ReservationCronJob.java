package com.fnb.order.scheduler;

import com.fnb.order.entity.Reservation;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.repository.ReservationRepository;
import com.fnb.order.repository.TableRepository;
import com.fnb.order.dto.event.TableStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCronJob {

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Quét các Booking quá giờ đến 30 phút mà chưa check-in.
     * Chạy 5 phút một lần (300,000 ms).
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoCancelNoShowReservations() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(30);
        
        // Quét các booking đã CONFIRMED (đã gán bàn) nhưng không tới
        List<Reservation> noShowList = reservationRepository.findByStatusAndBookingTimeBefore("CONFIRMED", thresholdTime);
        // Quét thêm các booking PENDING (chưa gán bàn)
        noShowList.addAll(reservationRepository.findByStatusAndBookingTimeBefore("PENDING", thresholdTime));

        if (noShowList.isEmpty()) {
            return;
        }

        log.info("Reservation CronJob: Tìm thấy {} booking quá giờ (No-show). Bắt đầu xử lý giải phóng bàn.", noShowList.size());

        for (Reservation res : noShowList) {
            res.setStatus("NO_SHOW");
            
            // Giải phóng các bàn đã được gán (nếu có)
            List<TableInfo> reservedTables = res.getTables();
            for (TableInfo table : reservedTables) {
                if ("RESERVED".equals(table.getStatus())) {
                    table.setStatus("FREE");
                    tableRepository.save(table);
                    
                    // Gửi event để cập nhật trên POS realtime
                    applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                            .tableId(table.getId())
                            .status("FREE")
                            .build());
                }
            }
            
            reservationRepository.save(res);
            log.info("Đã chuyển booking của khách {} sang NO_SHOW và giải phóng bàn.", res.getCustomerName());
        }
    }
}
