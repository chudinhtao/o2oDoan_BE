package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.request.TableRequest;
import com.fnb.order.dto.response.TableResponse;
import com.fnb.order.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTableController {

    private final TableService tableService;

    /** POST /api/admin/tables – Tạo bàn mới */
    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @Valid @RequestBody TableRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo bàn thành công", tableService.create(request)));
    }

    /** PUT /api/admin/tables/{id} – Cập nhật thông tin bàn */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable UUID id,
            @Valid @RequestBody TableRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật bàn thành công", tableService.update(id, request)));
    }

    /** POST /api/admin/tables/{id}/qr – Tạo / Tái tạo QR code */
    @PostMapping("/{id}/qr")
    public ResponseEntity<ApiResponse<TableResponse>> generateQr(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo QR mới thành công", tableService.generateQrCode(id)));
    }

    /** PATCH /api/admin/tables/{id}/disable-qr – Vô hiệu hóa QR */
    @PatchMapping("/{id}/disable-qr")
    public ResponseEntity<ApiResponse<TableResponse>> disableQr(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Khóa QR bàn thành công", tableService.disableQrCode(id)));
    }

    /** DELETE /api/admin/tables/{id} – Xóa mềm bàn */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable UUID id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa bàn thành công", null));
    }

    /** DELETE /api/admin/tables/{id}/hard – Xóa cứng bàn vĩnh viễn */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteTable(@PathVariable UUID id) {
        tableService.hardDeleteTable(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa vĩnh viễn", null));
    }

    /** PATCH /api/admin/tables/{id}/toggle-active – Bật/tắt trạng thái hoạt động */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<TableResponse>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", tableService.toggleActiveStatus(id)));
    }
}
