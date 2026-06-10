package com.fnb.auth.repository;

import com.fnb.auth.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findByScheduleId(UUID scheduleId);
    Optional<AttendanceLog> findByUserIdAndCheckOutIsNull(UUID userId);
    boolean existsByUserId(UUID userId);
    List<AttendanceLog> findBySchedule_WorkDateBetweenOrderBySchedule_WorkDateDesc(LocalDate from, LocalDate to);
}
