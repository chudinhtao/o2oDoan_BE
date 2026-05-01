package com.fnb.common.dto;

import java.util.UUID;

public interface ITarget {
    String getTargetType(); // GLOBAL, CATEGORY, ITEM
    UUID getTargetId();
}
