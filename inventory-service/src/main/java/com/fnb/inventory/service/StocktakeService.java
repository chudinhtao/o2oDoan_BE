package com.fnb.inventory.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.StockTransactionRequest;
import com.fnb.inventory.dto.request.StocktakeItemUpdateRequest;
import com.fnb.inventory.dto.response.StocktakeResponse;
import com.fnb.inventory.entity.InventoryLevel;
import com.fnb.inventory.entity.Stocktake;
import com.fnb.inventory.entity.StocktakeItem;
import com.fnb.inventory.repository.InventoryLevelRepository;
import com.fnb.inventory.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fnb.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final InventoryLevelRepository levelRepository;
    private final StockTransactionService stockTransactionService;
    private final com.fnb.inventory.repository.LocationRepository locationRepository;
    private final UserResolverService userResolverService;

    @Transactional
    public StocktakeResponse createSnapshot(com.fnb.inventory.dto.request.StocktakeCreateRequest request) {
        if (request == null || request.getLocationId() == null) {
            throw new BusinessException("Vui lòng chọn Kho cần kiểm kê");
        }
        
        com.fnb.inventory.entity.Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Kho không tồn tại"));

        String name = (request.getName() != null && !request.getName().trim().isEmpty()) 
                ? request.getName() 
                : "Kiểm kê " + java.time.format.DateTimeFormatter.ofPattern("yyMMdd-HHmm").format(java.time.LocalDateTime.now());

        Stocktake stocktake = Stocktake.builder()
                .status(StocktakeStatus.DRAFT)
                .name(name)
                .notes(request.getNotes())
                .snapshotTime(java.time.LocalDateTime.now())
                .location(location)
                .build();

        List<InventoryLevel> currentLevels = levelRepository.findByItemIsActiveTrueAndLocationId(request.getLocationId());
        
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        List<StocktakeItem> items = currentLevels.stream()
                .filter(level -> {
                    if (level.getCurrentStock().compareTo(BigDecimal.ZERO) != 0) return true;
                    if (level.getUpdatedAt() != null && level.getUpdatedAt().isAfter(sevenDaysAgo)) return true;
                    if (level.getUpdatedAt() == null && level.getCreatedAt() != null && level.getCreatedAt().isAfter(sevenDaysAgo)) return true;
                    return false;
                })
                .map(level -> StocktakeItem.builder()
                .stocktake(stocktake)
                .item(level.getItem())
                .batch(level.getBatch())
                .systemQuantity(level.getCurrentStock())
                .build()).collect(Collectors.toList());

        stocktake.getItems().addAll(items);
        return mapToResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional
    public StocktakeResponse updateCounts(UUID stocktakeId, List<StocktakeItemUpdateRequest> countRequests) {
        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new ResourceNotFoundException("Kỳ kiểm kê không tồn tại"));

        if (StocktakeStatus.COMPLETED.equals(stocktake.getStatus())) {
            throw new BusinessException("Kỳ kiểm kê đã hoàn thành, không thể sửa");
        }

        Map<UUID, StocktakeItem> itemMap = stocktake.getItems().stream()
                .collect(Collectors.toMap(StocktakeItem::getId, i -> i));

        for (StocktakeItemUpdateRequest req : countRequests) {
            StocktakeItem item = itemMap.get(req.getId());
            if (item != null) {
                item.setCountedQuantity(req.getCountedQuantity());
                item.setVariance(req.getCountedQuantity().subtract(item.getSystemQuantity()));
                item.setAdjustmentReason(req.getAdjustmentReason());
            }
        }

        return mapToResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional
    public StocktakeResponse completeStocktake(UUID stocktakeId) {
        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new ResourceNotFoundException("Kỳ kiểm kê không tồn tại"));

        if (StocktakeStatus.COMPLETED.equals(stocktake.getStatus())) {
            throw new BusinessException("Kỳ kiểm kê đã hoàn thành");
        }

        for (StocktakeItem item : stocktake.getItems()) {
            if (item.getCountedQuantity() == null) {
                // Nếu chưa đếm, coi như không có sai lệch (hoặc ép phải đếm hết)
                item.setCountedQuantity(item.getSystemQuantity());
                item.setVariance(BigDecimal.ZERO);
            }

            if (item.getVariance().compareTo(BigDecimal.ZERO) != 0) {
                // Tạo giao dịch cân bằng kho (ADJUSTMENT)
                StockTransactionRequest txRequest = StockTransactionRequest.builder()
                        .itemId(item.getItem().getId())
                        .transactionType(TransactionType.ADJUSTMENT)
                        .quantityChange(item.getVariance())
                        .locationId(stocktake.getLocation() != null ? stocktake.getLocation().getId() : null)
                        .lotNumber(item.getBatch() != null ? item.getBatch().getLotNumber() : null)
                        .referenceId(stocktake.getId())
                        .reason("Kiểm kê kho lệch: " + item.getVariance() + " (" + item.getAdjustmentReason() + ")")
                        .build();

                stockTransactionService.recordTransaction(txRequest);
            }
        }

        stocktake.setStatus(StocktakeStatus.COMPLETED);
        stocktake.setCompletedAt(java.time.LocalDateTime.now());
        return mapToResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional(readOnly = true)
    public StocktakeResponse getStocktake(UUID id) {
        return stocktakeRepository.findById(id).map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Kỳ kiểm kê không tồn tại"));
    }



    @Transactional
    public StocktakeResponse cancelStocktake(UUID id) {
        Stocktake stocktake = stocktakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stocktake not found"));
        if (stocktake.getStatus() == com.fnb.inventory.enums.StocktakeStatus.COMPLETED) {
            throw new com.fnb.common.exception.BusinessException("Không thể hủy đợt kiểm kê đã chốt");
        }
        stocktake.setStatus(com.fnb.inventory.enums.StocktakeStatus.CANCELLED);
        return mapToResponse(stocktakeRepository.save(stocktake));
    }

    public PageResponse<StocktakeResponse> getStocktakes(String status, String keyword, String startDate, String endDate, int page, int size) {
        Page<Stocktake> stocktakePage = stocktakeRepository.findWithFilter(status, keyword, startDate, endDate, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at")));
        return PageResponse.of(
                stocktakePage.getContent().stream().map(this::mapToResponse).toList(),
                page, size, stocktakePage.getTotalElements());
    }

    private StocktakeResponse mapToResponse(Stocktake s) {
        return StocktakeResponse.builder()
                .id(s.getId())
                .status(s.getStatus())
                .name(s.getName())
                .notes(s.getNotes())
                .snapshotTime(s.getSnapshotTime())
                .completedAt(s.getCompletedAt())
                .createdAt(s.getCreatedAt())
                .createdBy(userResolverService.resolveName(s.getCreatedBy()))
                .updatedAt(s.getUpdatedAt())
                .updatedBy(userResolverService.resolveName(s.getUpdatedBy()))
                .locationId(s.getLocation() != null ? s.getLocation().getId() : null)
                .locationName(s.getLocation() != null ? s.getLocation().getName() : null)
                .items(s.getItems().stream().map(i -> StocktakeResponse.StocktakeItemResponse.builder()
                        .id(i.getId())
                        .itemId(i.getItem().getId())
                        .itemName(i.getItem().getName())
                        .itemSku(i.getItem().getSku())
                        .systemQuantity(i.getSystemQuantity())
                        .countedQuantity(i.getCountedQuantity())
                        .variance(i.getVariance())
                        .adjustmentReason(i.getAdjustmentReason())
                        .batchId(i.getBatch() != null ? i.getBatch().getId() : null)
                        .lotNumber(i.getBatch() != null ? i.getBatch().getLotNumber() : "N/A")
                        .expiryDate(i.getBatch() != null ? i.getBatch().getExpiryDate() : null)
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
