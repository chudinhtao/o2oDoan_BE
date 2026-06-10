package com.fnb.inventory.service;

import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.UomConversionRequest;
import com.fnb.inventory.dto.response.UomConversionResponse;
import com.fnb.inventory.entity.InventoryItem;
import com.fnb.inventory.entity.ItemUomConversion;
import com.fnb.inventory.entity.Uom;
import com.fnb.inventory.repository.InventoryItemRepository;
import com.fnb.inventory.repository.ItemUomConversionRepository;
import com.fnb.inventory.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UomConversionService {

    private final ItemUomConversionRepository conversionRepository;
    private final InventoryItemRepository itemRepository;
    private final UomRepository uomRepository;
    private final UomService uomService;

    public List<UomConversionResponse> findByItemId(UUID itemId) {
        return conversionRepository.findByItemId(itemId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public UomConversionResponse create(UomConversionRequest request) {
        InventoryItem item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu: " + request.getItemId()));
        Uom fromUom = uomRepository.findById(request.getFromUomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy UoM nguồn: " + request.getFromUomId()));
        Uom toUom = uomRepository.findById(request.getToUomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy UoM đích: " + request.getToUomId()));

        ItemUomConversion conversion = ItemUomConversion.builder()
                .item(item).fromUom(fromUom).toUom(toUom)
                .conversionRate(request.getConversionRate()).build();
        return toResponse(conversionRepository.save(conversion));
    }

    @Transactional
    public UomConversionResponse update(UUID id, UomConversionRequest request) {
        ItemUomConversion c = conversionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy đổi: " + id));
        Uom fromUom = uomRepository.findById(request.getFromUomId())
                .orElseThrow(() -> new ResourceNotFoundException("UoM nguồn: " + request.getFromUomId()));
        Uom toUom = uomRepository.findById(request.getToUomId())
                .orElseThrow(() -> new ResourceNotFoundException("UoM đích: " + request.getToUomId()));
        c.setFromUom(fromUom);
        c.setToUom(toUom);
        c.setConversionRate(request.getConversionRate());
        return toResponse(conversionRepository.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        if (!conversionRepository.existsById(id)) throw new ResourceNotFoundException("Quy đổi: " + id);
        conversionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateBaseQuantity(InventoryItem item, UUID inputUomId, java.math.BigDecimal quantity) {
        if (item.getBaseUom() != null && item.getBaseUom().getId().equals(inputUomId)) {
            return quantity;
        }
        
        ItemUomConversion conversion = conversionRepository.findByItemId(item.getId()).stream()
                .filter(c -> c.getFromUom().getId().equals(inputUomId) && 
                            c.getToUom().getId().equals(item.getBaseUom().getId()))
                .findFirst()
                .orElseThrow(() -> new com.fnb.common.exception.BusinessException(
                        "Không có quy đổi từ " + inputUomId + " sang base UoM cho nguyên liệu: " + item.getName()));
                        
        return quantity.multiply(conversion.getConversionRate());
    }

    private UomConversionResponse toResponse(ItemUomConversion c) {
        return UomConversionResponse.builder()
                .id(c.getId()).itemId(c.getItem().getId()).itemName(c.getItem().getName())
                .fromUom(uomService.toResponse(c.getFromUom()))
                .toUom(uomService.toResponse(c.getToUom()))
                .conversionRate(c.getConversionRate()).build();
    }
}
