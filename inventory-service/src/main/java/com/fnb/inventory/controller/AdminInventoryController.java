package com.fnb.inventory.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.inventory.dto.request.*;
import com.fnb.inventory.dto.response.*;
import com.fnb.inventory.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final UomService uomService;
    private final SupplierService supplierService;
    private final ItemCategoryService categoryService;
    private final LocationService locationService;
    private final InventoryItemService itemService;
    private final UomConversionService conversionService;
    private final RecipeService recipeService;

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Không thể thực hiện thao tác xóa vì dữ liệu này đang được tham chiếu bởi các bản ghi khác (ràng buộc hệ thống)."));
    }

    // ─── UoM ──────────────────────────────────────────────────────────

    @GetMapping("/uoms")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<List<UomResponse>>> listUoms() {
        return ResponseEntity.ok(ApiResponse.ok(uomService.findAll()));
    }

    @GetMapping("/uoms/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<PageResponse<UomResponse>>> searchUoms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(uomService.search(keyword, pageRequest)));
    }

    @PostMapping("/uoms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UomResponse>> createUom(@Valid @RequestBody UomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn vị tính thành công", uomService.create(request)));
    }

    @PutMapping("/uoms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UomResponse>> updateUom(@PathVariable UUID id, @Valid @RequestBody UomRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", uomService.update(id, request)));
    }

    @DeleteMapping("/uoms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUom(@PathVariable UUID id) {
        uomService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa đơn vị tính thành công", null));
    }

    // ─── Suppliers ────────────────────────────────────────────────────

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> listSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.findAll(keyword, isActive, page, size)));
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.findById(id)));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo NCC thành công", supplierService.create(request)));
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật NCC thành công", supplierService.update(id, request)));
    }

    @PatchMapping("/suppliers/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> toggleSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.toggleActive(id)));
    }

    @DeleteMapping("/suppliers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa NCC thành công", null));
    }

    // ─── Item Categories ──────────────────────────────────────────────

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<List<ItemCategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findAll()));
    }

    @GetMapping("/categories/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<PageResponse<ItemCategoryResponse>>> searchCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(categoryService.search(keyword, pageRequest)));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemCategoryResponse>> createCategory(@Valid @RequestBody ItemCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo nhóm NL thành công", categoryService.create(request)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemCategoryResponse>> updateCategory(@PathVariable UUID id, @Valid @RequestBody ItemCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", categoryService.update(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa nhóm NL thành công", null));
    }

    // ─── Locations ────────────────────────────────────────────────────

    @GetMapping("/locations")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> listLocations() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findAll()));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo vị trí thành công", locationService.create(request)));
    }

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", locationService.update(id, request)));
    }

    @PatchMapping("/locations/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> toggleLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.toggleActive(id)));
    }

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa vị trí thành công", null));
    }

    // ─── Inventory Items ──────────────────────────────────────────────

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> listItems(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.findAll(categoryId, type, isActive, keyword, page, size)));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.findById(id)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(@Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo nguyên liệu thành công", itemService.create(request)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(@PathVariable UUID id, @Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", itemService.update(id, request)));
    }

    @PatchMapping("/items/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> toggleItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.toggleActive(id)));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable UUID id) {
        itemService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa nguyên liệu thành công", null));
    }

    // ─── UoM Conversions ──────────────────────────────────────────────

    @GetMapping("/items/{itemId}/conversions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<List<UomConversionResponse>>> listConversions(@PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(conversionService.findByItemId(itemId)));
    }

    @PostMapping("/conversions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UomConversionResponse>> createConversion(@Valid @RequestBody UomConversionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo quy đổi thành công", conversionService.create(request)));
    }

    @PutMapping("/conversions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UomConversionResponse>> updateConversion(@PathVariable UUID id, @Valid @RequestBody UomConversionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật quy đổi thành công", conversionService.update(id, request)));
    }

    @DeleteMapping("/conversions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConversion(@PathVariable UUID id) {
        conversionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa quy đổi thành công", null));
    }

    // ─── Recipes (BOM) ────────────────────────────────────────────────

    @GetMapping("/recipes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecipeResponse>> getRecipe(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.findById(id)));
    }

    @GetMapping("/recipes/by-sale-item/{saleItemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecipeResponse>> getRecipeBySaleItem(@PathVariable UUID saleItemId) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.findBySaleItemId(saleItemId)));
    }

    @GetMapping("/recipes/by-modifier/{modifierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecipeResponse>> getRecipeByModifier(@PathVariable UUID modifierId) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.findByModifierId(modifierId)));
    }

    @GetMapping("/recipes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RecipeResponse>>> listRecipes(@RequestParam(defaultValue = "MAIN_ITEM") RecipeType type) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.findAllByType(type)));
    }

    @PostMapping("/recipes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecipeResponse>> createOrUpdateRecipe(@Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Lưu công thức thành công", recipeService.createOrUpdate(request)));
    }

    @DeleteMapping("/recipes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable UUID id) {
        recipeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa công thức thành công", null));
    }
}
