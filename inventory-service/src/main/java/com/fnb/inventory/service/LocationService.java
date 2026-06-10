package com.fnb.inventory.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.LocationRequest;
import com.fnb.inventory.dto.response.LocationResponse;
import com.fnb.inventory.entity.Location;
import com.fnb.inventory.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public List<LocationResponse> findAll() {
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LocationResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new BusinessException("Vị trí '" + request.getName() + "' đã tồn tại");
        }
        Location location = Location.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = getOrThrow(id);
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public LocationResponse toggleActive(UUID id) {
        Location location = getOrThrow(id);
        location.setActive(!location.isActive());
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public void delete(UUID id) {
        Location location = getOrThrow(id);
        locationRepository.delete(location);
    }

    private Location getOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí với ID: " + id));
    }

    private LocationResponse toResponse(Location l) {
        return LocationResponse.builder()
                .id(l.getId())
                .name(l.getName())
                .description(l.getDescription())
                .isActive(l.isActive())
                .build();
    }
}
