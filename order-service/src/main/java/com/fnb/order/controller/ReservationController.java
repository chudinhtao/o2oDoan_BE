package com.fnb.order.controller;

import com.fnb.order.dto.request.AssignTableRequest;
import com.fnb.order.dto.request.ReservationRequest;
import com.fnb.order.dto.response.ReservationResponse;
import com.fnb.order.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.fnb.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {
    
    private final ReservationService reservationService;

    // --- Customer APIs ---
    @GetMapping("/customer/reservations/check-capacity")
    public ResponseEntity<ApiResponse<Boolean>> checkCapacity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
            @RequestParam int partySize) {
        return ResponseEntity.ok(ApiResponse.ok("Kiểm tra sức chứa thành công", reservationService.checkCapacity(time, partySize)));
    }

    @PostMapping("/customer/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo đặt bàn thành công", reservationService.createReservation(request)));
    }

    // --- Staff APIs (POS) ---
    @GetMapping("/staff/reservations")
    public ResponseEntity<ApiResponse<com.fnb.common.dto.PageResponse<ReservationResponse>>> getStaffReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đặt bàn thành công", reservationService.getStaffReservations(status, keyword, start, end, pageable)));
    }

    @PutMapping("/staff/reservations/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationByStaff(
            @PathVariable UUID id, @RequestBody com.fnb.order.dto.request.UpdateReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đặt bàn thành công", reservationService.updateReservation(id, request)));
    }

    @PutMapping("/staff/reservations/{id}/assign-tables")
    public ResponseEntity<ApiResponse<ReservationResponse>> assignTables(
            @PathVariable UUID id, @RequestBody AssignTableRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Gán bàn thành công", reservationService.assignTables(id, request)));
    }

    @PostMapping("/staff/reservations/{id}/check-in")
    public ResponseEntity<ApiResponse<Void>> checkIn(@PathVariable UUID id) {
        reservationService.checkIn(id);
        return ResponseEntity.ok(ApiResponse.ok("Check-in thành công", null));
    }

    @PutMapping("/staff/reservations/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable UUID id, @RequestBody(required = false) com.fnb.order.dto.request.CancelReservationRequest request) {
        reservationService.cancelReservation(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đặt bàn thành công", null));
    }

    // --- Admin APIs ---
    @GetMapping("/admin/reservations")
    public ResponseEntity<ApiResponse<com.fnb.common.dto.PageResponse<ReservationResponse>>> getAdminReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean hasDeposit,
            @RequestParam(required = false) String refundStatus,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đặt bàn thành công", reservationService.getAdminReservations(status, phone, startDate, endDate, hasDeposit, refundStatus, pageable)));
    }

    @GetMapping("/admin/reservations/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin đặt bàn thành công", reservationService.getReservationById(id)));
    }

    @PostMapping("/admin/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservationByAdmin(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo đặt bàn thành công", reservationService.createReservation(request)));
    }

    @PutMapping("/admin/reservations/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationByAdmin(
            @PathVariable UUID id, @RequestBody com.fnb.order.dto.request.UpdateReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đặt bàn thành công", reservationService.updateReservation(id, request)));
    }

    @PatchMapping("/admin/reservations/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateReservationStatus(
            @PathVariable UUID id, @RequestBody com.fnb.order.dto.request.UpdateReservationStatusRequest request) {
        reservationService.updateReservationStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", null));
    }
}
