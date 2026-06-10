package com.fnb.order.repository;

import com.fnb.order.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    // Lấy danh sách booking đang chờ để xử lý quá giờ
    List<Reservation> findByStatusAndBookingTimeBefore(String status, LocalDateTime time);

    // Lọc theo trạng thái và thời gian trong ngày (Dành cho Lễ tân)
    List<Reservation> findByBookingTimeBetweenOrderByBookingTimeAsc(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r JOIN r.tables t WHERE t.id = :tableId AND r.status IN ('PENDING', 'CONFIRMED')")
    java.util.Optional<Reservation> findActiveReservationByTableId(@org.springframework.data.repository.query.Param("tableId") UUID tableId);

    java.util.Optional<Reservation> findByPayosOrderCode(Long payosOrderCode);
}
