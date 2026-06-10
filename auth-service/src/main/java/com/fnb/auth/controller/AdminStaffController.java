package com.fnb.auth.controller;

import com.fnb.auth.dto.staff.ShiftTemplateDto;
import com.fnb.auth.dto.staff.WorkScheduleDto;
import com.fnb.auth.service.StaffService;
import com.fnb.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final StaffService staffService;

    // --- Shift Templates ---
    @GetMapping("/shifts")
    public ApiResponse<?> getAllShifts() {
        return ApiResponse.ok("Lấy danh sách ca mẫu thành công", staffService.getAllShiftTemplates());
    }

    @PostMapping("/shifts")
    public ApiResponse<?> saveShift(@RequestBody ShiftTemplateDto dto) {
        return ApiResponse.ok("Lưu ca mẫu thành công", staffService.saveShiftTemplate(dto));
    }

    @DeleteMapping("/shifts/{id}")
    public ApiResponse<?> deleteShift(@PathVariable UUID id) {
        staffService.deleteShiftTemplate(id);
        return ApiResponse.ok("Xoá ca mẫu thành công");
    }

    // --- Schedules ---
    @GetMapping("/schedules")
    public ApiResponse<?> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy lịch làm việc thành công", staffService.getSchedules(from, to));
    }

    @PostMapping("/schedules/assign")
    public ApiResponse<?> assignShift(@RequestBody WorkScheduleDto dto) {
        return ApiResponse.ok("Phân ca thành công", staffService.assignShift(dto));
    }

    @GetMapping("/attendance")
    public ApiResponse<?> getAttendanceLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy nhật ký chấm công thành công", staffService.getAttendanceLogs(from, to != null ? to : from));
    }

    @DeleteMapping("/schedules/{id}")
    public ApiResponse<?> deleteSchedule(@PathVariable UUID id) {
        staffService.deleteSchedule(id);
        return ApiResponse.ok("Xoá lịch làm việc thành công");
    }

    @GetMapping("/attendance/summary")
    public ApiResponse<?> getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy tổng hợp công thành công", staffService.getAttendanceSummary(from, to != null ? to : from));
    }
}
