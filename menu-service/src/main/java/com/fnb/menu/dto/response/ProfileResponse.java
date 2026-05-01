package com.fnb.menu.dto.response;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String name,
        String slogan,
        String logoUrl,
        String bannerUrl,
        String address,
        String phone
) {}
