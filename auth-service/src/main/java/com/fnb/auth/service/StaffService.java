package com.fnb.auth.service;

import com.fnb.auth.dto.staff.ShiftTemplateDto;
import com.fnb.auth.dto.staff.WorkScheduleDto;
import com.fnb.auth.entity.AttendanceLog;
import com.fnb.auth.entity.ShiftTemplate;
import com.fnb.auth.entity.User;
import com.fnb.auth.entity.WorkSchedule;
import com.fnb.auth.repository.AttendanceLogRepository;
import com.fnb.auth.repository.ShiftTemplateRepository;
import com.fnb.auth.repository.UserRepository;
import com.fnb.auth.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // --- Staff Profile Management (Old methods restored) ---
    public com.fnb.common.dto.PageResponse<com.fnb.auth.dto.response.UserResponse> getAllStaff(String keyword, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<User> staffPage = userRepository.searchStaff(
                List.of("CASHIER", "KITCHEN", "SERVER"),
                keyword == null || keyword.trim().isEmpty() ? null : keyword.trim(),
                pageable
        );
        return com.fnb.common.dto.PageResponse.of(
                staffPage.map(this::mapToUserResponse).getContent(),
                page,
                size,
                staffPage.getTotalElements()
        );
    }

    @Transactional
    public com.fnb.auth.dto.response.UserResponse createStaff(com.fnb.auth.dto.request.CreateStaffRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isActive(true)
                .build();
        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public com.fnb.auth.dto.response.UserResponse updateStaff(UUID id, com.fnb.auth.dto.request.CreateStaffRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Staff not found"));
        
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(request.getRole());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        
        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public com.fnb.auth.dto.response.UserResponse toggleActive(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Staff not found"));
        user.setActive(!user.isActive());
        
        if (!user.isActive()) {
            // Tự động xóa tất cả lịch làm việc tương lai của nhân viên bị khóa
            workScheduleRepository.deleteAllByUserIdAndWorkDateGreaterThanEqual(id, LocalDate.now());
        }
        
        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteStaff(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Nhân viên không tồn tại."));

        // Kiểm tra lịch làm việc
        boolean hasSchedules = workScheduleRepository.existsByUserId(id);
        if (hasSchedules) {
            throw new com.fnb.common.exception.BusinessException("Không thể xóa nhân viên đã được gán lịch làm việc. Vui lòng sử dụng tính năng khóa tài khoản.");
        }

        // Kiểm tra nhật ký chấm công
        boolean hasAttendanceLogs = attendanceLogRepository.existsByUserId(id);
        if (hasAttendanceLogs) {
            throw new com.fnb.common.exception.BusinessException("Không thể xóa nhân viên đã có lịch sử chấm công. Vui lòng sử dụng tính năng khóa tài khoản.");
        }

        userRepository.delete(user);
    }

    private com.fnb.auth.dto.response.UserResponse mapToUserResponse(User user) {
        return com.fnb.auth.dto.response.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .build();
    }

    // --- Shift Templates ---
    public List<ShiftTemplateDto> getAllShiftTemplates() {
        return shiftTemplateRepository.findAll().stream()
                .map(this::mapToShiftDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShiftTemplateDto saveShiftTemplate(ShiftTemplateDto dto) {
        ShiftTemplate entity = ShiftTemplate.builder()
                .id(dto.getId())
                .name(dto.getName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .colorCode(dto.getColorCode())
                .active(dto.isActive())
                .build();
        return mapToShiftDto(shiftTemplateRepository.save(entity));
    }

    @Transactional
    public void deleteShiftTemplate(UUID id) {
        ShiftTemplate shift = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Ca làm việc không tồn tại."));

        boolean isAssignedInFuture = workScheduleRepository.existsByShiftIdAndWorkDateGreaterThanEqual(id, LocalDate.now());
        if (isAssignedInFuture) {
            throw new com.fnb.common.exception.BusinessException("Không thể xóa ca làm việc đang được gán lịch trong tương lai.");
        }

        boolean isReferencedByPast = workScheduleRepository.existsByShiftId(id);
        if (isReferencedByPast) {
            // Lưu trữ/Lưu vết lịch sử: ngắt kích hoạt ca thay vì xóa vật lý
            shift.setActive(false);
            shiftTemplateRepository.save(shift);
        } else {
            // Không có bất kỳ ràng buộc nào -> Xóa vĩnh viễn
            shiftTemplateRepository.deleteById(id);
        }
    }

    // --- Work Schedules ---
    public List<WorkScheduleDto> getSchedules(LocalDate start, LocalDate end) {
        return workScheduleRepository.findAllByWorkDateBetween(start, end).stream()
                .map(this::mapToScheduleDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkScheduleDto assignShift(WorkScheduleDto dto) {
        if (dto.getWorkDate().isBefore(LocalDate.now())) {
            throw new com.fnb.common.exception.BusinessException("Không thể phân ca cho ngày trong quá khứ.");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("User not found"));
        ShiftTemplate shift = shiftTemplateRepository.findById(dto.getShiftId())
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Shift template not found"));

        List<WorkSchedule> existingSchedules = workScheduleRepository.findAllByUserIdAndWorkDate(user.getId(), dto.getWorkDate());
        for (WorkSchedule existing : existingSchedules) {
            ShiftTemplate existingShift = existing.getShift();
            if (shift.getStartTime().isBefore(existingShift.getEndTime()) &&
                shift.getEndTime().isAfter(existingShift.getStartTime())) {
                throw new com.fnb.common.exception.BusinessException("Lịch làm việc bị trùng với ca: " + existingShift.getName());
            }
        }

        WorkSchedule schedule = WorkSchedule.builder()
                .user(user)
                .shift(shift)
                .workDate(dto.getWorkDate())
                .notes(dto.getNotes())
                .status("PLANNED")
                .build();

        return mapToScheduleDto(workScheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException("Schedule not found"));
        
        if (schedule.getWorkDate().isBefore(LocalDate.now())) {
            throw new com.fnb.common.exception.BusinessException("Không thể xóa ca làm việc trong quá khứ.");
        }

        if ("ACTIVE".equals(schedule.getStatus()) || "COMPLETED".equals(schedule.getStatus())) {
            throw new com.fnb.common.exception.BusinessException("Không thể xóa ca làm việc đang diễn ra hoặc đã hoàn thành.");
        }
        workScheduleRepository.deleteById(id);
    }

    // --- Attendance ---
    @Transactional
    public void checkIn(UUID userId, String note) {
        LocalDate today = LocalDate.now();
        List<WorkSchedule> schedules = workScheduleRepository.findAllByUserIdAndWorkDate(userId, today);
        if (schedules.isEmpty()) {
            throw new com.fnb.common.exception.BusinessException("No schedule found for today. Please contact admin.");
        }

        java.time.LocalTime nowTime = java.time.LocalTime.now();
        WorkSchedule closestSchedule = schedules.stream()
            .min(java.util.Comparator.comparing(s -> java.time.Duration.between(s.getShift().getStartTime(), nowTime).abs()))
            .orElseThrow(() -> new com.fnb.common.exception.BusinessException("No valid shift found."));

        java.time.LocalTime startTime = closestSchedule.getShift().getStartTime();
        java.time.LocalDateTime scheduledStart = java.time.LocalDateTime.of(today, startTime);
        
        if (java.time.LocalDateTime.now().isBefore(scheduledStart.minusMinutes(30))) {
            throw new com.fnb.common.exception.BusinessException("Chưa đến giờ điểm danh. Chỉ được điểm danh sớm tối đa 30 phút.");
        }

        java.util.Optional<AttendanceLog> existingLog = attendanceLogRepository.findByScheduleId(closestSchedule.getId());
        if (existingLog.isPresent()) {
            AttendanceLog log = existingLog.get();
            java.time.LocalTime endTime = closestSchedule.getShift().getEndTime();
            
            LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now(), endTime);
            if (endTime.isBefore(startTime)) {
                endDateTime = endDateTime.plusDays(1);
            }
            LocalDateTime maxCheckOut = endDateTime.plusHours(2);
            
            if (log.getCheckOut() == null || LocalDateTime.now().isBefore(maxCheckOut)) {
                return;
            } else {
                throw new com.fnb.common.exception.BusinessException("Bạn đã hoàn thành ca làm việc này rồi.");
            }
        }

        int gracePeriod = closestSchedule.getShift().getGracePeriodMinutes();
        int lateMinutes = 0;
        boolean isLate = false;
        
        if (nowTime.isAfter(startTime.plusMinutes(gracePeriod))) {
            isLate = true;
            lateMinutes = (int) java.time.Duration.between(startTime, nowTime).toMinutes();
        }

        AttendanceLog log = AttendanceLog.builder()
                .user(closestSchedule.getUser())
                .schedule(closestSchedule)
                .checkIn(LocalDateTime.now())
                .checkInNote(note)
                .late(isLate)
                .lateMinutes(lateMinutes)
                .build();
        
        attendanceLogRepository.save(log);
        closestSchedule.setStatus("ACTIVE");
        workScheduleRepository.save(closestSchedule);
    }

    @Transactional
    public void checkOut(UUID userId, String note) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        java.time.LocalTime nowTime = now.toLocalTime();

        List<WorkSchedule> allSchedules = workScheduleRepository.findAllByUserIdAndWorkDate(userId, today);
        if (allSchedules.isEmpty()) {
            throw new com.fnb.common.exception.BusinessException("No schedule found for today.");
        }
        allSchedules.sort(java.util.Comparator.comparing(s -> s.getShift().getStartTime()));

        AttendanceLog currentLog = null;
        for (WorkSchedule schedule : allSchedules) {
            java.util.Optional<AttendanceLog> optLog = attendanceLogRepository.findByScheduleId(schedule.getId());
            if (optLog.isPresent()) {
                AttendanceLog log = optLog.get();
                java.time.LocalTime startTime = schedule.getShift().getStartTime();
                java.time.LocalTime endTime = schedule.getShift().getEndTime();
                
                LocalDateTime endDateTime = LocalDateTime.of(today, endTime);
                if (endTime.isBefore(startTime)) {
                    endDateTime = endDateTime.plusDays(1);
                }
                LocalDateTime maxCheckOut = endDateTime.plusHours(2);
                
                if (log.getCheckOut() == null || now.isBefore(maxCheckOut)) {
                    currentLog = log;
                }
            }
        }

        if (currentLog == null) {
            throw new com.fnb.common.exception.BusinessException("Không tìm thấy ca làm việc nào có thể chốt sổ hiện tại.");
        }

        int currentIndex = -1;
        for (int i = 0; i < allSchedules.size(); i++) {
            if (allSchedules.get(i).getId().equals(currentLog.getSchedule().getId())) {
                currentIndex = i;
                break;
            }
        }

        while (currentIndex != -1 && currentIndex < allSchedules.size() - 1) {
            WorkSchedule nextSchedule = allSchedules.get(currentIndex + 1);
            java.time.LocalTime currentEndTime = currentLog.getSchedule().getShift().getEndTime();
            java.time.LocalTime nextStartTime = nextSchedule.getShift().getStartTime();

            long gapMinutes = java.time.Duration.between(currentEndTime, nextStartTime).toMinutes();
            if (Math.abs(gapMinutes) <= 60 && nowTime.isAfter(nextStartTime)) {
                LocalDateTime autoCheckOutTime = LocalDateTime.of(today, currentEndTime);
                if (now.isBefore(autoCheckOutTime)) {
                    autoCheckOutTime = now;
                }
                
                closeLog(currentLog, autoCheckOutTime, "Tự động kết ca do chuyển ca liên tiếp");

                LocalDateTime autoCheckInTime = LocalDateTime.of(today, nextStartTime);
                AttendanceLog nextLog = AttendanceLog.builder()
                        .user(nextSchedule.getUser())
                        .schedule(nextSchedule)
                        .checkIn(autoCheckInTime)
                        .build();
                nextLog = attendanceLogRepository.save(nextLog);
                
                nextSchedule.setStatus("ACTIVE");
                workScheduleRepository.save(nextSchedule);

                currentLog = nextLog;
                currentIndex++;
            } else {
                break;
            }
        }

        closeLog(currentLog, now, note);
    }

    private void closeLog(AttendanceLog log, LocalDateTime checkOutTime, String note) {
        log.setCheckOut(checkOutTime);
        log.setCheckOutNote(note);
        if (log.getSchedule() != null) {
            WorkSchedule schedule = log.getSchedule();
            java.time.LocalTime endTime = schedule.getShift().getEndTime();
            java.time.LocalTime actualOutTime = checkOutTime.toLocalTime();
            int gracePeriod = schedule.getShift().getGracePeriodMinutes();

            if (actualOutTime.isBefore(endTime.minusMinutes(gracePeriod))) {
                log.setEarlyLeave(true);
                log.setEarlyLeaveMinutes((int) java.time.Duration.between(actualOutTime, endTime).toMinutes());
            }

            if (actualOutTime.isAfter(endTime)) {
                log.setOtMinutes((int) java.time.Duration.between(endTime, actualOutTime).toMinutes());
            }

            schedule.setStatus("COMPLETED");
            workScheduleRepository.save(schedule);
        }
        attendanceLogRepository.save(log);
    }

    public List<com.fnb.auth.dto.staff.AttendanceLogDto> getAttendanceLogs(LocalDate from, LocalDate to) {
        // Lấy tất cả logs trong khoảng ngày (dựa trên check_in)
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);
        
        return attendanceLogRepository.findAll().stream()
                .filter(log -> log.getCheckIn() != null && !log.getCheckIn().isBefore(start) && !log.getCheckIn().isAfter(end))
                .map(this::mapToAttendanceDto)
                .collect(Collectors.toList());
    }

    private com.fnb.auth.dto.staff.AttendanceLogDto mapToAttendanceDto(AttendanceLog log) {
        return com.fnb.auth.dto.staff.AttendanceLogDto.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .fullName(log.getUser().getFullName())
                .shiftName(log.getSchedule() != null ? log.getSchedule().getShift().getName() : "N/A")
                .checkIn(log.getCheckIn())
                .checkOut(log.getCheckOut())
                .checkInNote(log.getCheckInNote())
                .checkOutNote(log.getCheckOutNote())
                .isLate(log.isLate())
                .isEarlyLeave(log.isEarlyLeave())
                .lateMinutes(log.getLateMinutes())
                .earlyLeaveMinutes(log.getEarlyLeaveMinutes())
                .otMinutes(log.getOtMinutes())
                .build();
    }

    // --- Mappers ---
    public com.fnb.auth.dto.staff.AttendanceLogDto getCurrentAttendance(UUID userId) {
        LocalDate today = LocalDate.now();
        List<WorkSchedule> schedules = workScheduleRepository.findAllByUserIdAndWorkDate(userId, today);

        if (schedules.isEmpty()) {
            throw new com.fnb.common.exception.BusinessException("NO_SCHEDULE");
        }

        java.time.LocalTime nowTime = java.time.LocalTime.now();

        for (WorkSchedule schedule : schedules) {
            java.util.Optional<AttendanceLog> optLog = attendanceLogRepository.findByScheduleId(schedule.getId());
            if (optLog.isPresent()) {
                AttendanceLog log = optLog.get();
                java.time.LocalTime startTime = schedule.getShift().getStartTime();
                java.time.LocalTime endTime = schedule.getShift().getEndTime();
                
                LocalDateTime endDateTime = LocalDateTime.of(today, endTime);
                if (endTime.isBefore(startTime)) {
                    endDateTime = endDateTime.plusDays(1);
                }
                LocalDateTime maxCheckOut = endDateTime.plusHours(2);
                
                if (log.getCheckOut() == null || LocalDateTime.now().isBefore(maxCheckOut)) {
                    return mapToAttendanceDto(log);
                }
            }
        }

        return null;
    }

    private ShiftTemplateDto mapToShiftDto(ShiftTemplate s) {
        return ShiftTemplateDto.builder()
                .id(s.getId())
                .name(s.getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .colorCode(s.getColorCode())
                .active(s.isActive())
                .build();
    }

    private WorkScheduleDto mapToScheduleDto(WorkSchedule w) {
        return WorkScheduleDto.builder()
                .id(w.getId())
                .userId(w.getUser().getId())
                .fullName(w.getUser().getFullName())
                .shiftId(w.getShift().getId())
                .shiftName(w.getShift().getName())
                .workDate(w.getWorkDate())
                .status(w.getStatus())
                .notes(w.getNotes())
                .build();
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 1 0 * * *") // Chạy lúc 00:01 sáng mỗi ngày
    @Transactional
    public void markNoShows() {
        LocalDate today = LocalDate.now();
        List<WorkSchedule> pastSchedules = workScheduleRepository.findAllByWorkDateBeforeAndStatus(today, "PLANNED");
        
        if (!pastSchedules.isEmpty()) {
            for (WorkSchedule schedule : pastSchedules) {
                schedule.setStatus("NO_SHOW");
            }
            workScheduleRepository.saveAll(pastSchedules);
            log.info("Auto-marked {} unfulfilled schedules as NO_SHOW", pastSchedules.size());
        }
    }

    public java.util.List<com.fnb.auth.dto.attendance.AttendanceSummaryDto> getAttendanceSummary(LocalDate from, LocalDate to) {
        List<AttendanceLog> logs = attendanceLogRepository.findBySchedule_WorkDateBetweenOrderBySchedule_WorkDateDesc(from, to);
        
        java.util.Map<UUID, com.fnb.auth.dto.attendance.AttendanceSummaryDto> summaryMap = new java.util.HashMap<>();
        
        for (AttendanceLog log : logs) {
            UUID userId = log.getUser().getId();
            com.fnb.auth.dto.attendance.AttendanceSummaryDto dto = summaryMap.computeIfAbsent(userId, id -> 
                com.fnb.auth.dto.attendance.AttendanceSummaryDto.builder()
                    .userId(userId)
                    .fullName(log.getUser().getFullName())
                    .totalShifts(0)
                    .totalWorkingHours(0.0)
                    .totalLateMinutes(0)
                    .totalEarlyLeaveMinutes(0)
                    .totalOtMinutes(0)
                    .build()
            );
            
            dto.setTotalShifts(dto.getTotalShifts() + 1);
            dto.setTotalLateMinutes(dto.getTotalLateMinutes() + log.getLateMinutes());
            dto.setTotalEarlyLeaveMinutes(dto.getTotalEarlyLeaveMinutes() + log.getEarlyLeaveMinutes());
            dto.setTotalOtMinutes(dto.getTotalOtMinutes() + log.getOtMinutes());
            
            if (log.getCheckIn() != null && log.getCheckOut() != null) {
                long minutes = java.time.Duration.between(log.getCheckIn(), log.getCheckOut()).toMinutes();
                // Substract break time if applicable, here we assume all duration is working hours
                dto.setTotalWorkingHours(dto.getTotalWorkingHours() + (minutes / 60.0));
            }
        }
        
        // Round working hours to 2 decimal places
        for (com.fnb.auth.dto.attendance.AttendanceSummaryDto dto : summaryMap.values()) {
            dto.setTotalWorkingHours(Math.round(dto.getTotalWorkingHours() * 100.0) / 100.0);
        }
        
        return new java.util.ArrayList<>(summaryMap.values());
    }
}
