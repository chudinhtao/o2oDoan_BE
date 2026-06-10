package com.fnb.inventory.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.UomRequest;
import com.fnb.inventory.dto.response.UomResponse;
import com.fnb.inventory.entity.Uom;
import com.fnb.inventory.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fnb.common.dto.PageResponse;

@Service
@RequiredArgsConstructor
public class UomService {

    private final UomRepository uomRepository;

    public List<UomResponse> findAll() {
        return uomRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")).stream().map(this::toResponse).toList();
    }

    public PageResponse<UomResponse> search(String keyword, Pageable pageable) {
        Page<Uom> page;
        if (keyword == null || keyword.trim().isEmpty()) {
            page = uomRepository.findAll(pageable);
        } else {
            page = uomRepository.searchByKeyword(keyword, pageable);
        }
        return PageResponse.of(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    public UomResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public UomResponse create(UomRequest request) {
        if (uomRepository.existsByName(request.getName())) {
            throw new BusinessException("Đơn vị tính '" + request.getName() + "' đã tồn tại");
        }
        Uom uom = Uom.builder()
                .name(request.getName())
                .shortName(request.getShortName())
                .build();
        return toResponse(uomRepository.save(uom));
    }

    @Transactional
    public UomResponse update(UUID id, UomRequest request) {
        Uom uom = getOrThrow(id);
        uom.setName(request.getName());
        uom.setShortName(request.getShortName());
        return toResponse(uomRepository.save(uom));
    }

    @Transactional
    public void delete(UUID id) {
        Uom uom = getOrThrow(id);
        uomRepository.delete(uom);
    }

    private Uom getOrThrow(UUID id) {
        return uomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị tính với ID: " + id));
    }

    public UomResponse toResponse(Uom uom) {
        return UomResponse.builder()
                .id(uom.getId())
                .name(uom.getName())
                .shortName(uom.getShortName())
                .build();
    }
}
