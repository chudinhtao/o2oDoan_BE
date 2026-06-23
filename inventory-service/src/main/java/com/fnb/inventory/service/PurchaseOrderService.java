package com.fnb.inventory.service;

import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.GoodsReceiptRequest;
import com.fnb.inventory.dto.request.PurchaseOrderRequest;
import com.fnb.inventory.dto.request.StockTransactionRequest;
import com.fnb.inventory.dto.response.PurchaseOrderResponse;
import com.fnb.inventory.entity.InventoryItem;
import com.fnb.inventory.entity.PurchaseOrder;
import com.fnb.inventory.entity.PurchaseOrderItem;
import com.fnb.inventory.entity.Supplier;
import com.fnb.inventory.entity.Uom;
import com.fnb.inventory.enums.POStatus;
import com.fnb.inventory.enums.POType;
import com.fnb.inventory.enums.TransactionType;
import com.fnb.inventory.repository.InventoryItemRepository;
import com.fnb.inventory.repository.PurchaseOrderRepository;
import com.fnb.inventory.repository.SupplierRepository;
import com.fnb.inventory.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository itemRepository;
    private final UomRepository uomRepository;
    private final UomConversionService uomConversionService;
    private final StockTransactionService stockTransactionService;
    private final UserResolverService userResolverService;

    // =====================================================================
    //  CRUD ON DRAFT
    // =====================================================================

    @Transactional
    public PurchaseOrderResponse createPo(PurchaseOrderRequest request) {
        Supplier supplier = resolveSupplier(request);

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .supplier(supplier)
                .type(request.getType())
                .status(POStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .expectedDate(request.getExpectedDate())
                .notes(request.getNotes())
                .build();

        List<PurchaseOrderItem> items = buildItems(request, po);
        po.getItems().addAll(items);
        po.setTotalAmount(calculateTotal(items));
        po = poRepository.save(po);

        // QUICK_GRN: bỏ qua CONFIRMED, nhập kho thẳng
        if (POType.QUICK_GRN.equals(request.getType())) {
            return receiveAllAndComplete(po, request.getLocationId());
        }

        return mapToResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse updateDraftPo(UUID poId, PurchaseOrderRequest request) {
        PurchaseOrder po = findPoOrThrow(poId);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể chỉnh sửa phiếu ở trạng thái Nháp (DRAFT)");
        }

        // Update supplier and metadata
        Supplier supplier = resolveSupplier(request);
        po.setSupplier(supplier);
        po.setExpectedDate(request.getExpectedDate());
        po.setNotes(request.getNotes());

        // Replace items
        po.getItems().clear();
        List<PurchaseOrderItem> items = buildItems(request, po);
        po.getItems().addAll(items);
        po.setTotalAmount(calculateTotal(items));

        return mapToResponse(poRepository.save(po));
    }

    // =====================================================================
    //  STATE TRANSITIONS
    // =====================================================================

    /**
     * DRAFT -> CONFIRMED
     * Khóa phiếu, gửi đơn cho nhà cung cấp. Không cho sửa items nữa.
     */
    @Transactional
    public PurchaseOrderResponse confirmPo(UUID poId) {
        PurchaseOrder po = findPoOrThrow(poId);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể chốt phiếu đang ở trạng thái Nháp (DRAFT)");
        }
        if (po.getItems().isEmpty()) {
            throw new BusinessException("Phiếu phải có ít nhất 1 mặt hàng trước khi chốt");
        }

        po.setStatus(POStatus.CONFIRMED);
        po.setConfirmedAt(LocalDateTime.now());
        log.info("PO {} confirmed by user, status -> CONFIRMED", po.getPoNumber());
        return mapToResponse(poRepository.save(po));
    }

    /**
     * CONFIRMED/PARTIAL_RECEIVED -> Nhận hàng một phần hoặc toàn bộ
     * Tự động chuyển sang PARTIAL_RECEIVED hoặc COMPLETED dựa trên số lượng đã nhận.
     */
    @Transactional
    public PurchaseOrderResponse receivePo(UUID poId, GoodsReceiptRequest request) {
        PurchaseOrder po = findPoOrThrow(poId);

        if (po.getStatus() != POStatus.CONFIRMED && po.getStatus() != POStatus.PARTIAL_RECEIVED) {
            throw new BusinessException(
                "Chỉ có thể nhận hàng cho phiếu ở trạng thái Đã chốt (CONFIRMED) hoặc Nhận một phần (PARTIAL_RECEIVED)"
            );
        }

        // Map poItemId -> PoItem để tìm nhanh
        Map<UUID, PurchaseOrderItem> itemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        for (GoodsReceiptRequest.ReceiptLineRequest line : request.getItems()) {
            PurchaseOrderItem poItem = itemMap.get(line.getPoItemId());
            if (poItem == null) {
                throw new ResourceNotFoundException("PO Item không tìm thấy: " + line.getPoItemId());
            }

            BigDecimal remaining = poItem.getRemainingQuantity();
            if (line.getReceivedQuantity().compareTo(remaining) > 0) {
                throw new BusinessException(String.format(
                    "Số lượng nhận (%s) vượt quá số lượng còn thiếu (%s) của mặt hàng: %s",
                    line.getReceivedQuantity(), remaining, poItem.getItem().getName()
                ));
            }

            // Cộng dồn received qty
            poItem.setReceivedQuantity(poItem.getReceivedQuantity().add(line.getReceivedQuantity()));

            // Ghi stock transaction: nhập vào kho
            BigDecimal baseQty = uomConversionService.calculateBaseQuantity(
                    poItem.getItem(), poItem.getUom().getId(), line.getReceivedQuantity());

            StockTransactionRequest txReq = StockTransactionRequest.builder()
                    .itemId(poItem.getItem().getId())
                    .transactionType(TransactionType.IN_PO)
                    .quantityChange(baseQty)
                    .unitPriceAtTransaction(poItem.getUnitPrice())
                    .referenceId(po.getId())
                    .reason("Nhận hàng từ PO: " + po.getPoNumber())
                    .lotNumber(poItem.getBatchNumber())
                    .expiryDate(poItem.getExpiryDate())
                    .locationId(line.getLocationId())
                    .build();
            stockTransactionService.recordTransaction(txReq);
        }

        // Đánh giá trạng thái mới dựa trên tổng đã nhận
        boolean allReceived = po.getItems().stream()
                .allMatch(item -> item.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0);

        po.setStatus(allReceived ? POStatus.COMPLETED : POStatus.PARTIAL_RECEIVED);
        log.info("PO {} after receiving -> status: {}", po.getPoNumber(), po.getStatus());

        return mapToResponse(poRepository.save(po));
    }

    /**
     * PARTIAL_RECEIVED -> COMPLETED (Đóng phiếu dù chưa nhận đủ)
     * Dùng khi NCC không giao thêm, muốn chốt sổ.
     */
    @Transactional
    public PurchaseOrderResponse forceCompletePo(UUID poId) {
        PurchaseOrder po = findPoOrThrow(poId);
        if (po.getStatus() != POStatus.PARTIAL_RECEIVED) {
            throw new BusinessException("Chỉ có thể đóng phiếu đang ở trạng thái Nhận một phần (PARTIAL_RECEIVED)");
        }
        po.setStatus(POStatus.COMPLETED);
        log.info("PO {} force-completed by user (partial receipt)", po.getPoNumber());
        return mapToResponse(poRepository.save(po));
    }

    /**
     * DRAFT/CONFIRMED -> CANCELLED
     */
    @Transactional
    public PurchaseOrderResponse cancelPo(UUID poId) {
        PurchaseOrder po = findPoOrThrow(poId);
        if (po.getStatus() != POStatus.DRAFT && po.getStatus() != POStatus.CONFIRMED) {
            throw new BusinessException("Chỉ có thể hủy phiếu ở trạng thái Nháp hoặc Đã chốt");
        }
        po.setStatus(POStatus.CANCELLED);
        log.info("PO {} cancelled", po.getPoNumber());
        return mapToResponse(poRepository.save(po));
    }

    // =====================================================================
    //  QUERIES
    // =====================================================================

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getPos(POStatus status, POType type, String poNumber,
                                                      LocalDateTime startDate, LocalDateTime endDate,
                                                      int page, int size) {
        Page<PurchaseOrder> result = poRepository.findWithFilter(
                status != null ? status.name() : null,
                type != null ? type.name() : null,
                poNumber, startDate, endDate, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at")));
        return new PageResponse<>(
                result.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPo(UUID id) {
        return poRepository.findById(id).map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PO không tồn tại"));
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private PurchaseOrderResponse receiveAllAndComplete(PurchaseOrder po, UUID locationId) {
        // Internal: dùng cho QUICK_GRN, nhập kho ngay toàn bộ
        for (PurchaseOrderItem item : po.getItems()) {
            item.setReceivedQuantity(item.getOrderedQuantity());
            BigDecimal baseQty = uomConversionService.calculateBaseQuantity(
                    item.getItem(), item.getUom().getId(), item.getOrderedQuantity());

            StockTransactionRequest txReq = StockTransactionRequest.builder()
                    .itemId(item.getItem().getId())
                    .transactionType(TransactionType.IN_QUICK)
                    .quantityChange(baseQty)
                    .unitPriceAtTransaction(item.getUnitPrice())
                    .referenceId(po.getId())
                    .reason("Nhập nhanh từ PO: " + po.getPoNumber())
                    .lotNumber(item.getBatchNumber())
                    .expiryDate(item.getExpiryDate())
                    .locationId(locationId)
                    .build();
            stockTransactionService.recordTransaction(txReq);
        }
        po.setStatus(POStatus.COMPLETED);
        return mapToResponse(poRepository.save(po));
    }

    private Supplier resolveSupplier(PurchaseOrderRequest request) {
        if (request.getSupplierId() != null) {
            return supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại"));
        }
        if (POType.STANDARD.equals(request.getType())) {
            throw new BusinessException("STANDARD PO phải có Nhà cung cấp");
        }
        return null;
    }

    private List<PurchaseOrderItem> buildItems(PurchaseOrderRequest request, PurchaseOrder po) {
        return request.getItems().stream().map(itemReq -> {
            InventoryItem item = itemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("NL không tồn tại: " + itemReq.getItemId()));
            Uom uom = uomRepository.findById(itemReq.getUomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UoM không tồn tại"));
            uomConversionService.calculateBaseQuantity(item, uom.getId(), BigDecimal.ONE);

            return PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .item(item)
                    .orderedQuantity(itemReq.getOrderedQuantity())
                    .quantity(itemReq.getOrderedQuantity()) // Set explicitly for legacy constraint
                    .receivedQuantity(BigDecimal.ZERO)
                    .uom(uom)
                    .unitPrice(itemReq.getUnitPrice())
                    .batchNumber(itemReq.getBatchNumber())
                    .expiryDate(itemReq.getExpiryDate())
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<PurchaseOrderItem> items) {
        return items.stream()
                .map(i -> i.getOrderedQuantity().multiply(
                        i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseOrder findPoOrThrow(UUID poId) {
        return poRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("PO không tồn tại: " + poId));
    }

    private String generatePoNumber() {
        String datePart = java.time.format.DateTimeFormatter.ofPattern("yyMMdd")
                .format(LocalDateTime.now());
        int randomPart = new java.util.Random().nextInt(9000) + 1000;
        return "PO" + datePart + randomPart;
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .type(po.getType())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .expectedDate(po.getExpectedDate())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .createdBy(userResolverService.resolveName(po.getCreatedBy()))
                .confirmedAt(po.getConfirmedAt())
                .items(po.getItems().stream().map(i -> PurchaseOrderResponse.PurchaseOrderItemResponse.builder()
                        .id(i.getId())
                        .itemId(i.getItem().getId())
                        .itemName(i.getItem().getName())
                        .itemSku(i.getItem().getSku())
                        .orderedQuantity(i.getOrderedQuantity())
                        .receivedQuantity(i.getReceivedQuantity())
                        .remainingQuantity(i.getRemainingQuantity())
                        .uomId(i.getUom().getId())
                        .uomName(i.getUom().getName())
                        .unitPrice(i.getUnitPrice())
                        .totalLineAmount(i.getOrderedQuantity().multiply(
                                i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO))
                        .batchNumber(i.getBatchNumber())
                        .expiryDate(i.getExpiryDate())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
