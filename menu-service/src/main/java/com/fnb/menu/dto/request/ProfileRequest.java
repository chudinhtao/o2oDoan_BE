package com.fnb.menu.dto.request;

public record ProfileRequest(
        String name,
        String slogan,
        String logoUrl,
        String bannerUrl,
        String address,
        String phone
) {}
