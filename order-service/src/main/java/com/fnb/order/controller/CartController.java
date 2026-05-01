package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.redis.CartDto;
import com.fnb.order.dto.request.TicketItemRequest;
import com.fnb.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Xem giỏ hàng chung
    @GetMapping
    public ApiResponse<CartDto> getCart(@RequestHeader("X-Session-Token") String sessionToken) {
        return ApiResponse.ok("Lấy giỏ hàng thành công", cartService.getCart(sessionToken));
    }

    // Thêm 1 món vào giỏ hàng chung
    @PostMapping("/items")
    public ApiResponse<CartDto> addItem(
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody TicketItemRequest request) {
        return ApiResponse.ok("Đã thêm món vào nhóm chung", cartService.addItemToCart(sessionToken, request));
    }

    // Tăng giảm số lượng (Dùng RequestBody để đồng bộ với FE POS)
    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartDto> updateItemQuantity(
            @RequestHeader("X-Session-Token") String sessionToken,
            @PathVariable String cartItemId,
            @Valid @RequestBody com.fnb.order.dto.request.UpdateCartRequest request) {
        return ApiResponse.ok("Đã cập nhật số lượng", cartService.updateItemQuantity(sessionToken, cartItemId, request.getQuantity(), request.getNote()));
    }

    // Bỏ món ra khỏi giỏ
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartDto> removeItem(
            @RequestHeader("X-Session-Token") String sessionToken,
            @PathVariable String cartItemId) {
        return ApiResponse.ok("Đã xóa món khỏi nhóm", cartService.removeItemFromCart(sessionToken, cartItemId));
    }

    // Làm trắng giỏ
    @DeleteMapping
    public ApiResponse<String> clearCart(@RequestHeader("X-Session-Token") String sessionToken) {
        cartService.clearCart(sessionToken);
        return ApiResponse.ok("Đã xóa sạch giỏ hàng", null);
    }
}
