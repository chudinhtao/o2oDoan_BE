package com.fnb.auth.repository;

import com.fnb.auth.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    List<WorkSchedule> findAllByWorkDateBetween(LocalDate start, LocalDate end);
    List<WorkSchedule> findAllByUserIdAndWorkDateBetween(UUID userId, LocalDate start, LocalDate end);
    List<WorkSchedule> findAllByUserIdAndWorkDate(UUID userId, LocalDate workDate);
    List<WorkSchedule> findAllByWorkDateBeforeAndStatus(LocalDate date, String status);
    boolean existsByShiftIdAndWorkDateGreaterThanEqual(UUID shiftId, LocalDate date);
    boolean existsByShiftId(UUID shiftId);
    void deleteAllByUserIdAndWorkDateGreaterThanEqual(UUID userId, LocalDate date);
    boolean existsByUserId(UUID userId);
}
