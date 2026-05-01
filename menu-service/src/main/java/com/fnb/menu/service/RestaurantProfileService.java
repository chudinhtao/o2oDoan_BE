package com.fnb.menu.service;

import com.fnb.menu.dto.request.ProfileRequest;
import com.fnb.menu.dto.response.ProfileResponse;
import com.fnb.menu.entity.RestaurantProfile;
import com.fnb.menu.repository.RestaurantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantProfileService {

    private final RestaurantProfileRepository repository;

    /** Singleton: lấy bản ghi đầu tiên (luôn chỉ có 1). */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        return repository.findAll()
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    /** Upsert: nếu chưa có thì tạo mới, nếu có thì cập nhật. */
    @Transactional
    public ProfileResponse upsert(ProfileRequest req) {
        RestaurantProfile profile = repository.findAll()
                .stream()
                .findFirst()
                .orElseGet(RestaurantProfile::new);

        if (req.name()      != null) profile.setName(req.name());
        if (req.slogan()    != null) profile.setSlogan(req.slogan());
        if (req.logoUrl()   != null) profile.setLogoUrl(req.logoUrl());
        if (req.bannerUrl() != null) profile.setBannerUrl(req.bannerUrl());
        if (req.address()   != null) profile.setAddress(req.address());
        if (req.phone()     != null) profile.setPhone(req.phone());

        return toResponse(repository.save(profile));
    }

    private ProfileResponse toResponse(RestaurantProfile p) {
        return new ProfileResponse(
                p.getId(), p.getName(), p.getSlogan(),
                p.getLogoUrl(), p.getBannerUrl(), p.getAddress(), p.getPhone()
        );
    }
}
