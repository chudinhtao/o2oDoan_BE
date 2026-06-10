package com.fnb.inventory.service;

import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.SupplierRequest;
import com.fnb.inventory.dto.response.SupplierResponse;
import com.fnb.inventory.entity.Supplier;
import com.fnb.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public PageResponse<SupplierResponse> findAll(String keyword, Boolean isActive, int page, int size) {
        Page<Supplier> result = supplierRepository.findAllWithFilter(
                keyword, isActive, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at")));
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                page, size, result.getTotalElements());
    }

    public SupplierResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = generateSupplierCode();
        }

        if (supplierRepository.existsByCode(code)) {
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                code = generateSupplierCode();
            } else {
                throw new BusinessException("Mã NCC '" + code + "' đã tồn tại");
            }
        }

        Supplier supplier = Supplier.builder()
                .code(code)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .taxCode(request.getTaxCode())
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = getOrThrow(id);
        supplier.setName(request.getName());
        supplier.setCode(request.getCode());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse toggleActive(UUID id) {
        Supplier supplier = getOrThrow(id);
        supplier.setActive(!supplier.isActive());
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(UUID id) {
        Supplier supplier = getOrThrow(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private Supplier getOrThrow(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy NCC với ID: " + id));
    }

    private SupplierResponse toResponse(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .taxCode(s.getTaxCode())
                .isActive(s.isActive())
                .build();
    }

    private String generateSupplierCode() {
        int randomNum = new java.util.Random().nextInt(9000) + 1000;
        return "SUP" + randomNum;
    }
}
