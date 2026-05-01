package com.fnb.order.service;

import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.order.dto.event.TableStatusUpdatedEvent;
import com.fnb.order.dto.request.TableRequest;
import com.fnb.order.dto.response.PosTableResponse;
import com.fnb.order.dto.response.TableResponse;
import com.fnb.order.entity.TableInfo;
import com.fnb.order.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.qr-base-url:http://localhost:5173/?qr=}")
    private String qrBaseUrl;

    public PageResponse<TableResponse> getAllTables(String keyword, String status, Boolean isActive, int page,
            int size) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "";
        }
        Page<TableInfo> result = tableRepository.findAllWithFilter(
                keyword, status, isActive,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "number")));
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    public List<PosTableResponse> getAllForPos() {
        return tableRepository.findAllForPos();
    }

    /** Lấy danh sách Zone duy nhất cho Dropdown Server App. */
    public List<String> getDistinctZones() {
        return tableRepository.findDistinctZones();
    }

    @Cacheable(value = "order:tables", key = "#id")
    public TableResponse getTable(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    @CacheEvict(value = "order:tables", allEntries = true)
    public TableResponse create(TableRequest request) {
        if (tableRepository.findByNumber(request.getNumber()).isPresent()) {
            throw new BusinessException("Số bàn đã tồn tại");
        }

        TableInfo table = TableInfo.builder()
                .number(request.getNumber())
                .name(request.getName())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 4)
                .zone(request.getZone())
                .build();

        return toResponse(tableRepository.save(table));
    }

    @Transactional
    @CacheEvict(value = "order:tables", key = "#id")
    public TableResponse update(UUID id, TableRequest request) {
        TableInfo table = findById(id);

        if (!table.getNumber().equals(request.getNumber())
                && tableRepository.findByNumber(request.getNumber()).isPresent()) {
            throw new BusinessException("Số bàn đã tồn tại");
        }

        table.setNumber(request.getNumber());
        if (request.getName() != null)
            table.setName(request.getName());
        if (request.getCapacity() != null)
            table.setCapacity(request.getCapacity());
        if (request.getZone() != null)
            table.setZone(request.getZone());

        return toResponse(tableRepository.save(table));
    }

    @Transactional
    @CacheEvict(value = "order:tables", key = "#id")
    public TableResponse generateQrCode(UUID id) {
        TableInfo table = findById(id);

        // Tạo token ngẫu nhiên mới mỗi lần tạo QR (tránh khách quét QR cũ)
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        table.setQrToken(newToken);
        table.setQrUrl(qrBaseUrl + newToken);

        return toResponse(tableRepository.save(table));
    }

    @Transactional
    @CacheEvict(value = "order:tables", key = "#id")
    public TableResponse disableQrCode(UUID id) {
        TableInfo table = findById(id);
        table.setQrToken(null);
        table.setQrUrl(null);
        return toResponse(tableRepository.save(table));
    }

    @Transactional
    @CacheEvict(value = "order:tables", key = "#id")
    public void markAsCleaned(UUID id) {
        TableInfo table = findById(id);
        if ("CLEANING".equals(table.getStatus())) {
            table.setStatus("FREE");
            tableRepository.save(table);

            // Tự động giải phóng tất cả các bàn con (nếu có) đang được ghép vào bàn này
            int freedChildren = tableRepository.freeChildTables(table.getId());
            if (freedChildren > 0) {
                log.info("Bàn {} dọn xong, đã tự động giải phóng {} bàn con đang ghép.", table.getNumber(), freedChildren);
                // Bạn có thể cân nhắc bắn WebSocket event để refresh lại mảng TableMap trên UI
                applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(table.getId()) // Gửi tạm id bàn cha, frontend nên fetch lại toàn bộ map
                    .status("REFRESH_ALL")
                    .build());
            }

            // Bắn event để POS biết bàn đã sạch (FREE)
            applicationEventPublisher.publishEvent(TableStatusUpdatedEvent.builder()
                    .tableId(table.getId())
                    .status("FREE")
                    .build());
        } else {
            throw new BusinessException("Bàn không ở trạng thái dọn dẹp");
        }
    }

    @Transactional
    @CacheEvict(value = "order:tables", allEntries = true)
    public void deleteTable(UUID id) {
        TableInfo table = findById(id);
        table.setActive(false); // Soft delete
        tableRepository.save(table);
    }

    @Transactional
    @CacheEvict(value = "order:tables", allEntries = true)
    public void hardDeleteTable(UUID id) {
        if (!tableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bàn không tồn tại");
        }
        tableRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "order:tables", allEntries = true)
    public TableResponse toggleActiveStatus(UUID id) {
        TableInfo table = findById(id);
        table.setActive(!table.isActive());
        return toResponse(tableRepository.save(table));
    }

    public TableInfo findById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bàn không tồn tại"));
    }

    private TableResponse toResponse(TableInfo table) {
        return TableResponse.builder()
                .id(table.getId())
                .number(table.getNumber())
                .name(table.getName())
                .status(table.getStatus())
                .capacity(table.getCapacity())
                .qrUrl(table.getQrUrl())
                .isActive(table.isActive())
                .zone(table.getZone())
                .build();
    }
}
