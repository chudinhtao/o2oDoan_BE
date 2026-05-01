package com.fnb.menu.controller;

import com.fnb.menu.dto.request.PromotionRequest;
import com.fnb.menu.dto.response.PromotionResponse;
import com.fnb.menu.service.PromotionService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only promotion management — CRUD 5 scopes chuẩn.
 */
@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.listForAdmin(keyword, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> create(
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo khuyến mãi thành công", promotionService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", promotionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạm dừng khuyến mãi", null));
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable UUID id) {
        promotionService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa vĩnh viễn khuyến mãi", null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PromotionResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Thay đổi trạng thái thành công",
                promotionService.toggleStatus(id)));
    }
}
