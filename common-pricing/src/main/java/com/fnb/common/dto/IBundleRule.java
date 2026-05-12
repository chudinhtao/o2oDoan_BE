package com.fnb.common.dto;

import java.util.List;
import java.util.UUID;

public interface IBundleRule extends IPromotionRule {
    List<? extends IBundleItem> getBundleItems();
}
