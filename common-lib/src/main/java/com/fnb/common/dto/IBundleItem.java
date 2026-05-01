package com.fnb.common.dto;

import java.util.UUID;

public interface IBundleItem {
    UUID getItemId();
    int getQuantity();
    String getRole(); // "BUY" or "GET"
}
