package com.fnb.inventory.service;

import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.StockTransactionRequest;
import com.fnb.inventory.dto.response.StockTransactionResponse;
import com.fnb.inventory.entity.InventoryItem;
import com.fnb.inventory.entity.InventoryLevel;
import com.fnb.inventory.entity.StockTransaction;
import com.fnb.inventory.repository.InventoryItemRepository;
import com.fnb.inventory.repository.InventoryLevelRepository;
import com.fnb.inventory.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fnb.inventory.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final InventoryLevelRepository levelRepository;
    private final InventoryItemRepository itemRepository;
    private final com.fnb.inventory.repository.InventoryBatchRepository batchRepository;
    private final com.fnb.inventory.repository.LocationRepository locationRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * Immutable Audit Ledger - Ghi nhận mọi sự thay đổi kho.
     * Hàm này MUST be @Transactional.
     */
    @Transactional
    public StockTransactionResponse recordTransaction(StockTransactionRequest request) {
        log.info("Recording stock transaction for item {}: {} ({})", 
                request.getItemId(), request.getTransactionType(), request.getQuantityChange());

        InventoryItem item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Nguyên liệu không tồn tại"));

        java.util.List<InventoryLevel> levels = levelRepository.findByItemId(item.getId());

        BigDecimal totalOldStock = levels.stream()
                .map(InventoryLevel::getCurrentStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Ghi sổ cái
        BigDecimal priceAtTransaction = request.getUnitPriceAtTransaction();
        if (priceAtTransaction == null && request.getQuantityChange().compareTo(BigDecimal.ZERO) < 0) {
            priceAtTransaction = item.getAvgCostPrice();
        }

        com.fnb.inventory.entity.InventoryBatch batchLookup = null;
        if (request.getLotNumber() != null && !request.getLotNumber().trim().isEmpty()) {
            batchLookup = batchRepository.findFirstByItemIdAndLotNumber(request.getItemId(), request.getLotNumber()).orElse(null);
        }

        StockTransaction transaction = StockTransaction.builder()
                .item(item)
                .batch(batchLookup)
                .transactionType(request.getTransactionType())
                .quantityChange(request.getQuantityChange())
                .unitPriceAtTransaction(priceAtTransaction)
                .referenceId(request.getReferenceId())
                .orderLineItemId(request.getOrderLineItemId())
                .location(request.getLocationId() != null ? locationRepository.findById(request.getLocationId()).orElse(null) : null)
                .reason(request.getReason())
                .build();
        transaction = transactionRepository.save(transaction);

        BigDecimal remainingQty = request.getQuantityChange();

        // 2. Cập nhật tồn kho theo FEFO (nếu xuất) hoặc cộng dồn (nếu nhập)
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            // Nhập kho
            InventoryLevel targetLevel;
            boolean hasLotNumber = request.getLotNumber() != null && !request.getLotNumber().trim().isEmpty();
            boolean hasExpiryDate = request.getExpiryDate() != null;
            boolean isStandardInbound = TransactionType.IN_PO.equals(request.getTransactionType()) || TransactionType.IN_QUICK.equals(request.getTransactionType());
            
            if (hasLotNumber || hasExpiryDate || isStandardInbound) {
                com.fnb.inventory.entity.InventoryBatch batch = batchLookup;
                if (batch == null) {
                    String finalLotNumber = hasLotNumber ? request.getLotNumber() : 
                        "LOT-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();

                    batch = com.fnb.inventory.entity.InventoryBatch.builder()
                            .item(item)
                            .lotNumber(finalLotNumber)
                            .expiryDate(request.getExpiryDate())
                            .build();
                    batch = batchRepository.save(batch);
                }
                
                transaction.setBatch(batch);
                transaction = transactionRepository.save(transaction);
                
                com.fnb.inventory.entity.Location location = null;
                if (request.getLocationId() != null) {
                    location = locationRepository.findById(request.getLocationId()).orElse(levels.isEmpty() ? null : levels.get(0).getLocation());
                } else {
                    location = levels.isEmpty() ? null : levels.get(0).getLocation();
                }

                final com.fnb.inventory.entity.Location finalLocForBatch = location;
                final com.fnb.inventory.entity.InventoryBatch finalBatch = batch;
                targetLevel = levels.stream()
                        .filter(l -> {
                            if (finalLocForBatch == null) return l.getLocation() == null;
                            return l.getLocation() != null && l.getLocation().getId().equals(finalLocForBatch.getId());
                        })
                        .filter(l -> l.getBatch() != null && l.getBatch().getId().equals(finalBatch.getId()))
                        .findFirst()
                        .orElse(null);

                if (targetLevel == null) {
                    targetLevel = com.fnb.inventory.entity.InventoryLevel.builder()
                            .item(item)
                            .batch(batch)
                            .currentStock(BigDecimal.ZERO)
                            .location(location)
                            .build();
                    targetLevel = levelRepository.save(targetLevel);
                    levels.add(targetLevel);
                }
            } else {
                com.fnb.inventory.entity.Location reqLoc = null;
                if (request.getLocationId() != null) {
                    reqLoc = locationRepository.findById(request.getLocationId()).orElse(null);
                } else if (!levels.isEmpty()) {
                    // Try to find the first valid physical location if none provided
                    reqLoc = levels.stream().filter(l -> l.getLocation() != null).map(InventoryLevel::getLocation).findFirst().orElse(null);
                }

                final com.fnb.inventory.entity.Location finalLoc = reqLoc;
                java.util.stream.Stream<InventoryLevel> streamForLoc = levels.stream()
                        .filter(l -> {
                            if (finalLoc == null) return l.getLocation() == null;
                            return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                        });
                        
                targetLevel = null;

                if (TransactionType.REFUND.equals(request.getTransactionType())) {
                    // [BUGFIX] Hoàn trả đơn hàng (REFUND): Tự động trả về Lô đang sử dụng (FEFO)
                    targetLevel = levels.stream()
                            .filter(l -> {
                                if (finalLoc == null) return l.getLocation() == null;
                                return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                            })
                            .filter(l -> l.getBatch() != null && l.getBatch().getExpiryDate() != null)
                            .min(java.util.Comparator.comparing(l -> l.getBatch().getExpiryDate()))
                            .orElse(null);

                    // NẾU KHÔNG CÓ LÔ NÀO KHẢ DỤNG, TẠO LÔ MỚI MANG TÊN RETURN-[Mã đơn hàng]
                    if (targetLevel == null && request.getReferenceId() != null) {
                        String returnLotNumber = "RETURN-" + request.getReferenceId().toString().substring(0, 6).toUpperCase();
                        
                        com.fnb.inventory.entity.InventoryBatch returnBatch = batchRepository.findFirstByItemIdAndLotNumber(item.getId(), returnLotNumber)
                            .orElseGet(() -> {
                                com.fnb.inventory.entity.InventoryBatch newBatch = com.fnb.inventory.entity.InventoryBatch.builder()
                                    .item(item)
                                    .lotNumber(returnLotNumber)
                                    .build();
                                return batchRepository.save(newBatch);
                            });

                        targetLevel = levels.stream()
                            .filter(l -> {
                                if (finalLoc == null) return l.getLocation() == null;
                                return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                            })
                            .filter(l -> l.getBatch() != null && l.getBatch().getId().equals(returnBatch.getId()))
                            .findFirst()
                            .orElse(null);

                        if (targetLevel == null) {
                            targetLevel = com.fnb.inventory.entity.InventoryLevel.builder()
                                    .item(item)
                                    .batch(returnBatch)
                                    .location(finalLoc)
                                    .currentStock(BigDecimal.ZERO)
                                    .build();
                            targetLevel = levelRepository.save(targetLevel);
                            levels.add(targetLevel);
                        }

                        // Cập nhật lại lô cho giao dịch này
                        transaction.setBatch(returnBatch);
                        transaction = transactionRepository.save(transaction);
                    }
                }

                if (targetLevel == null) {
                    // Fallback mặc định: N/A batch
                    targetLevel = levels.stream()
                            .filter(l -> {
                                if (finalLoc == null) return l.getLocation() == null;
                                return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                            })
                            .filter(l -> l.getBatch() == null)
                            .findFirst()
                            .orElse(null);
                }

                if (targetLevel == null) {
                    targetLevel = com.fnb.inventory.entity.InventoryLevel.builder()
                            .item(item)
                            .location(finalLoc)
                            .currentStock(BigDecimal.ZERO)
                            .build();
                    levels.add(targetLevel);
                }
            }
            
            targetLevel.setCurrentStock(targetLevel.getCurrentStock().add(remainingQty));
            levelRepository.save(targetLevel);
            
            // Tính MAC
            if (TransactionType.IN_PO.equals(request.getTransactionType()) || TransactionType.IN_QUICK.equals(request.getTransactionType())) {
                if (request.getUnitPriceAtTransaction() != null && request.getUnitPriceAtTransaction().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal oldAvgCost = item.getAvgCostPrice() != null ? item.getAvgCostPrice() : BigDecimal.ZERO;
                    BigDecimal oldTotalValue = totalOldStock.max(BigDecimal.ZERO).multiply(oldAvgCost);
                    BigDecimal newTotalValue = request.getQuantityChange().multiply(request.getUnitPriceAtTransaction());
                    
                    BigDecimal newStockTotal = totalOldStock.max(BigDecimal.ZERO).add(request.getQuantityChange());
                    if (newStockTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newAvgCost = oldTotalValue.add(newTotalValue).divide(newStockTotal, 2, java.math.RoundingMode.HALF_UP);
                        item.setAvgCostPrice(newAvgCost);
                        itemRepository.save(item);
                        log.info("Cập nhật giá vốn Moving Average cho {}: {}", item.getName(), newAvgCost);
                    }
                }
            }
        } else if (remainingQty.compareTo(BigDecimal.ZERO) < 0) {
            // Xuất kho (FEFO: ưu tiên còn hạn, ngày hết hạn gần nhất)
            // Nếu có chỉ định locationId, ưu tiên lấy các level thuộc location đó
            java.util.stream.Stream<InventoryLevel> stream = levels.stream()
                    .filter(l -> l.getCurrentStock().compareTo(BigDecimal.ZERO) > 0);
            
            if (request.getLocationId() != null) {
                stream = stream.filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getLocationId()));
            }

            // [BUGFIX] Nếu client chỉ định đích danh 1 lô cụ thể để hủy/xuất, chỉ quét các level thuộc lô đó
            if (request.getLotNumber() != null && !request.getLotNumber().trim().isEmpty()) {
                stream = stream.filter(l -> l.getBatch() != null && l.getBatch().getLotNumber().equals(request.getLotNumber()));
            } else if (TransactionType.ADJUSTMENT.equals(request.getTransactionType())) {
                // Nếu là ADJUSTMENT mà không có lotNumber, nghĩa là đang điều chỉnh lượng của Lô mặc định (N/A)
                stream = stream.filter(l -> l.getBatch() == null);
            }

            java.util.List<InventoryLevel> availableLevels = stream.sorted((l1, l2) -> {
                        java.time.LocalDate d1 = (l1.getBatch() != null) ? l1.getBatch().getExpiryDate() : null;
                        java.time.LocalDate d2 = (l2.getBatch() != null) ? l2.getBatch().getExpiryDate() : null;
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d1.compareTo(d2);
                    })
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal qtyToDeduct = remainingQty.abs();

            for (InventoryLevel lvl : availableLevels) {
                if (qtyToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

                // Log cảnh báo nếu lô này đã hết hạn
                if (lvl.getBatch() != null && lvl.getBatch().getExpiryDate() != null) {
                    if (lvl.getBatch().getExpiryDate().isBefore(java.time.LocalDate.now())) {
                        log.warn("CẢNH BÁO: Đang trừ kho vào lô đã HẾT HẠN (Batch: {}, Expiry: {}) cho nguyên liệu: {}", 
                            lvl.getBatch().getLotNumber(), lvl.getBatch().getExpiryDate(), item.getName());
                    }
                }

                BigDecimal deductible = lvl.getCurrentStock().min(qtyToDeduct);
                lvl.setCurrentStock(lvl.getCurrentStock().subtract(deductible));
                levelRepository.save(lvl);
                qtyToDeduct = qtyToDeduct.subtract(deductible);
            }

            // Nếu vẫn còn thiếu (qtyToDeduct > 0), trừ vào lô mặc định tạo tồn kho âm tại vị trí (location) được chỉ định
            if (qtyToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                java.util.stream.Stream<InventoryLevel> fallbackStream = levels.stream();
                if (request.getLocationId() != null) {
                    fallbackStream = fallbackStream.filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getLocationId()));
                }
                
                InventoryLevel fallbackLevel = fallbackStream.filter(l -> l.getBatch() == null).findFirst().orElse(null);
                
                // Nếu chưa có level nào cho location này, tạo mới để cho phép trừ âm
                if (fallbackLevel == null) {
                    fallbackLevel = com.fnb.inventory.entity.InventoryLevel.builder()
                        .item(item)
                        .currentStock(BigDecimal.ZERO)
                        .location(request.getLocationId() != null ? locationRepository.findById(request.getLocationId()).orElse(null) : null)
                        .build();
                    fallbackLevel = levelRepository.save(fallbackLevel);
                    levels.add(fallbackLevel);
                }

                fallbackLevel.setCurrentStock(fallbackLevel.getCurrentStock().subtract(qtyToDeduct));
                levelRepository.save(fallbackLevel);
                log.warn("Nguyên liệu {} đã bị TRỪ ÂM KHO số lượng: {} tại location: {}", item.getName(), qtyToDeduct, request.getLocationId());
            }
        }

        BigDecimal totalNewStock = totalOldStock.add(request.getQuantityChange());

        // 3. O2O Warnings (Approach 2: No auto-hide, just warning)
        BigDecimal safetyStock = item.getSafetyStock() != null ? item.getSafetyStock() : BigDecimal.ZERO;
        
        if (totalNewStock.compareTo(BigDecimal.ZERO) <= 0 && request.getQuantityChange().signum() < 0) {
            log.warn("CẢNH BÁO: Nguyên liệu {} ĐÃ HẾT TRÊN HỆ THỐNG (Tồn: {}). Tuy nhiên hệ thống không tự động ẩn món để tránh gián đoạn bán hàng.", 
                    item.getName(), totalNewStock);
            // eventPublisher.publishEvent(new com.fnb.inventory.dto.event.InventoryOutOfStockEvent(item.getId(), item.getName()));
        } else if (totalNewStock.compareTo(safetyStock) <= 0 && totalOldStock.compareTo(safetyStock) > 0) {
            log.warn("CẢNH BÁO: Nguyên liệu {} sắp hết (Dưới mức an toàn: {} <= {})", item.getName(), totalNewStock, safetyStock);
        }

        // 4. O2O In-Stock
        boolean wasOutOfStock = totalOldStock.compareTo(BigDecimal.ZERO) <= 0;
        boolean nowInStock = totalNewStock.compareTo(BigDecimal.ZERO) > 0;
        boolean isInboundTransaction = TransactionType.IN_PO.equals(request.getTransactionType())
                || TransactionType.IN_QUICK.equals(request.getTransactionType())
                || TransactionType.ADJUSTMENT.equals(request.getTransactionType());

        if (wasOutOfStock && nowInStock && isInboundTransaction) {
            log.info("Nguyên liệu {} có hàng trở lại ({} → {}), kích hoạt mở lại các món liên quan.",
                    item.getName(), totalOldStock, totalNewStock);
            eventPublisher.publishEvent(new com.fnb.inventory.dto.event.InventoryInStockEvent(item.getId(), item.getName()));
        }

        return mapToResponse(transaction);
    }

    /**
     * Kill-switch thủ công: Set tồn kho về 0 bằng 1 giao dịch MANUAL_BLOCK.
     */
    @Transactional
    public StockTransactionResponse applyKillSwitch(UUID itemId, String reason) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguyên liệu không tồn tại"));

        java.util.List<InventoryLevel> levels = levelRepository.findByItemId(itemId);
        if (levels.isEmpty()) {
            throw new ResourceNotFoundException("Tồn kho chưa được khởi tạo");
        }

        BigDecimal currentStock = levels.stream()
                .map(InventoryLevel::getCurrentStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Kill-switch: {} đã hết hàng (tồn = {}), chỉ fire event.", item.getName(), currentStock);
            eventPublisher.publishEvent(
                    new com.fnb.inventory.dto.event.InventoryOutOfStockEvent(item.getId(), item.getName()));
            StockTransaction dummy = StockTransaction.builder()
                    .item(item)
                    .transactionType(TransactionType.MANUAL_BLOCK)
                    .quantityChange(BigDecimal.ZERO)
                    .reason(reason + " (đã hết từ trước)")
                    .build();
            return mapToResponse(transactionRepository.save(dummy));
        }

        BigDecimal deduction = currentStock.negate();
        StockTransactionRequest req = StockTransactionRequest.builder()
                .itemId(itemId)
                .transactionType(TransactionType.MANUAL_BLOCK)
                .quantityChange(deduction)
                .reason(reason)
                .build();

        log.warn("Kill-switch: {} ({} → 0) - Lý do: {}", item.getName(), currentStock, reason);
        return recordTransaction(req);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> getTransactionHistory(
            UUID itemId, TransactionType transactionType, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Page<StockTransaction> result = transactionRepository.findWithFilter(
                itemId, transactionType != null ? transactionType.name() : null, startDate, endDate, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at")));
        
        return new PageResponse<>(
                result.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    private StockTransactionResponse mapToResponse(StockTransaction tx) {
        return StockTransactionResponse.builder()
                .id(tx.getId())
                .itemId(tx.getItem().getId())
                .itemName(tx.getItem().getName())
                .itemSku(tx.getItem().getSku())
                .baseUomName(tx.getItem().getBaseUom() != null ? tx.getItem().getBaseUom().getName() : "")
                .transactionType(tx.getTransactionType())
                .quantityChange(tx.getQuantityChange())
                .unitPriceAtTransaction(tx.getUnitPriceAtTransaction())
                .referenceId(tx.getReferenceId())
                .orderLineItemId(tx.getOrderLineItemId())
                .reason(tx.getReason())
                .createdAt(tx.getCreatedAt())
                .createdBy(tx.getCreatedBy())
                .lotNumber(tx.getBatch() != null ? tx.getBatch().getLotNumber() : null)
                .expiryDate(tx.getBatch() != null ? tx.getBatch().getExpiryDate() : null)
                .locationId(tx.getLocation() != null ? tx.getLocation().getId() : null)
                .locationName(tx.getLocation() != null ? tx.getLocation().getName() : null)
                .build();
    }

    @Transactional
    public java.util.List<StockTransactionResponse> transferStock(com.fnb.inventory.dto.request.InternalTransferRequest request) {
        if (request.getFromLocationId().equals(request.getToLocationId())) {
            throw new com.fnb.common.exception.BusinessException("Kho xuất và kho nhập không được trùng nhau");
        }
        
        java.util.List<StockTransactionResponse> responses = new java.util.ArrayList<>();
        String transferNote = request.getNotes() != null ? "Chuyển kho: " + request.getNotes() : "Chuyển kho nội bộ";

        com.fnb.inventory.entity.Location fromLoc = locationRepository.findById(request.getFromLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kho xuất"));
        com.fnb.inventory.entity.Location toLoc = locationRepository.findById(request.getToLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kho nhập"));

        for (com.fnb.inventory.dto.request.InternalTransferRequest.TransferItemRequest itemReq : request.getItems()) {
            BigDecimal qtyToTransfer = itemReq.getQuantity();
            if (qtyToTransfer.compareTo(BigDecimal.ZERO) <= 0) continue;

            InventoryItem item = itemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nguyên liệu không tồn tại"));

            // Tìm các lô có sẵn tại kho xuất, áp dụng FEFO (ưu tiên lô cận date)
            java.util.List<InventoryLevel> fromLevels = levelRepository.findByItemId(item.getId()).stream()
                    .filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getFromLocationId()))
                    .filter(l -> l.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
                    .filter(l -> {
                        if (itemReq.getLotNumber() == null || itemReq.getLotNumber().trim().isEmpty()) return true;
                        return l.getBatch() != null && l.getBatch().getLotNumber().equals(itemReq.getLotNumber());
                    })
                    .sorted((l1, l2) -> {
                        java.time.LocalDate d1 = (l1.getBatch() != null) ? l1.getBatch().getExpiryDate() : null;
                        java.time.LocalDate d2 = (l2.getBatch() != null) ? l2.getBatch().getExpiryDate() : null;
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d1.compareTo(d2);
                    })
                    .collect(Collectors.toList());

            BigDecimal remainingToDeduct = qtyToTransfer;

            // Xử lý trừ kho FEFO và gán trực tiếp vào kho đích với CÙNG LÔ
            for (InventoryLevel fromLvl : fromLevels) {
                if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal deductible = fromLvl.getCurrentStock().min(remainingToDeduct);
                fromLvl.setCurrentStock(fromLvl.getCurrentStock().subtract(deductible));
                levelRepository.save(fromLvl);
                
                com.fnb.inventory.entity.InventoryBatch batch = fromLvl.getBatch();
                
                // Ghi nhận lệnh OUT_TRANSFER
                StockTransaction outTx = StockTransaction.builder()
                    .item(item)
                    .batch(batch)
                    .location(fromLoc)
                    .transactionType(TransactionType.OUT_TRANSFER)
                    .quantityChange(deductible.negate())
                    .unitPriceAtTransaction(item.getAvgCostPrice())
                    .reason(transferNote)
                    .build();
                outTx = transactionRepository.save(outTx);
                responses.add(mapToResponse(outTx));

                // Tìm hoặc tạo level tương ứng ở kho đích
                InventoryLevel toLvl = levelRepository.findByItemId(item.getId()).stream()
                    .filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getToLocationId()))
                    .filter(l -> {
                        if (batch == null) return l.getBatch() == null;
                        return l.getBatch() != null && l.getBatch().getId().equals(batch.getId());
                    })
                    .findFirst()
                    .orElse(null);

                if (toLvl == null) {
                    toLvl = InventoryLevel.builder()
                        .item(item)
                        .batch(batch)
                        .location(toLoc)
                        .currentStock(BigDecimal.ZERO)
                        .build();
                }
                toLvl.setCurrentStock(toLvl.getCurrentStock().add(deductible));
                levelRepository.save(toLvl);

                // Ghi nhận lệnh IN_TRANSFER
                StockTransaction inTx = StockTransaction.builder()
                    .item(item)
                    .batch(batch)
                    .location(toLoc)
                    .transactionType(TransactionType.IN_TRANSFER)
                    .quantityChange(deductible)
                    .unitPriceAtTransaction(item.getAvgCostPrice())
                    .reason(transferNote)
                    .build();
                inTx = transactionRepository.save(inTx);
                responses.add(mapToResponse(inTx));

                remainingToDeduct = remainingToDeduct.subtract(deductible);
            }

            // Nếu kho xuất không đủ số lượng (còn dư remainingToDeduct), ép trừ âm vào lô mặc định (null)
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Chuyển kho: Nguyên liệu {} bị TRỪ ÂM {} ở kho xuất {}", item.getName(), remainingToDeduct, request.getFromLocationId());
                
                InventoryLevel fallbackFrom = levelRepository.findByItemId(item.getId()).stream()
                    .filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getFromLocationId()))
                    .filter(l -> l.getBatch() == null)
                    .findFirst()
                    .orElseGet(() -> levelRepository.save(InventoryLevel.builder()
                        .item(item).location(fromLoc).currentStock(BigDecimal.ZERO).build()));
                
                fallbackFrom.setCurrentStock(fallbackFrom.getCurrentStock().subtract(remainingToDeduct));
                levelRepository.save(fallbackFrom);

                StockTransaction outTx = StockTransaction.builder()
                    .item(item).batch(null).location(fromLoc).transactionType(TransactionType.OUT_TRANSFER)
                    .quantityChange(remainingToDeduct.negate()).unitPriceAtTransaction(item.getAvgCostPrice()).reason(transferNote).build();
                outTx = transactionRepository.save(outTx);
                responses.add(mapToResponse(outTx));

                InventoryLevel fallbackTo = levelRepository.findByItemId(item.getId()).stream()
                    .filter(l -> l.getLocation() != null && l.getLocation().getId().equals(request.getToLocationId()))
                    .filter(l -> l.getBatch() == null)
                    .findFirst()
                    .orElseGet(() -> levelRepository.save(InventoryLevel.builder()
                        .item(item).location(toLoc).currentStock(BigDecimal.ZERO).build()));
                
                fallbackTo.setCurrentStock(fallbackTo.getCurrentStock().add(remainingToDeduct));
                levelRepository.save(fallbackTo);

                StockTransaction inTx = StockTransaction.builder()
                    .item(item).batch(null).location(toLoc).transactionType(TransactionType.IN_TRANSFER)
                    .quantityChange(remainingToDeduct).unitPriceAtTransaction(item.getAvgCostPrice()).reason(transferNote).build();
                inTx = transactionRepository.save(inTx);
                responses.add(mapToResponse(inTx));
            }
        }

        return responses;
    }

}
