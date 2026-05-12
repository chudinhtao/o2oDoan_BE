package com.fnb.common.util;

import com.fnb.common.dto.IBundleItem;
import com.fnb.common.dto.IBundleRule;

import java.math.BigDecimal;
import java.util.*;

public class BundleMatcher {

    public static class BundleMatchResult<T extends IBundleRule> {
        private final T rule;
        private final int count;
        private final BigDecimal totalDiscount;

        public BundleMatchResult(T rule, int count, BigDecimal totalDiscount) {
            this.rule = rule;
            this.count = count;
            this.totalDiscount = totalDiscount;
        }

        public T getRule() { return rule; }
        public int getCount() { return count; }
        public BigDecimal getTotalDiscount() { return totalDiscount; }
    }

    /**
     * Thuật toán nhận diện Combo (Greedy approach).
     * @param cart Map chứa các món ăn hiện có (itemID -> quantity)
     * @param availableRules Danh sách các quy tắc Combo
     * @param itemPrices Map chứa giá gốc của các món ăn (để tính discount)
     * @return Danh sách các Combo đã khớp
     */
    public static <T extends IBundleRule> List<BundleMatchResult<T>> matchBundles(
            Map<UUID, Integer> cart,
            List<T> availableRules,
            Map<UUID, BigDecimal> itemPrices) {

        List<BundleMatchResult<T>> matchedBundles = new ArrayList<>();
        
        // Sắp xếp rules theo Priority giảm dần để chọn các bộ có lợi/quan trọng nhất trước
        List<T> sortedRules = new ArrayList<>(availableRules);
        sortedRules.sort((a, b) -> (b.getPriority() != null ? b.getPriority() : 0) 
                                 - (a.getPriority() != null ? a.getPriority() : 0));

        Map<UUID, Integer> remainingCart = new HashMap<>(cart);

        for (T rule : sortedRules) {
            int bundlesOfThisRule = 0;
            // Tính tổng số lượng yêu cầu của từng món trong 1 bộ Combo
            Map<UUID, Integer> requiredPerBundle = new HashMap<>();
            if (rule.getBundleItems() != null) {
                for (IBundleItem bi : rule.getBundleItems()) {
                    requiredPerBundle.put(bi.getItemId(), requiredPerBundle.getOrDefault(bi.getItemId(), 0) + bi.getQuantity());
                }
            }

            boolean possible = true;
            if (requiredPerBundle.isEmpty()) {
                possible = false; // Ngăn chặn vòng lặp vô hạn nếu Combo rỗng
            }

            while (possible) {
                // Kiểm tra xem giỏ hàng có đủ số lượng tổng của TẤT CẢ các item yêu cầu trong 1 bộ không
                for (Map.Entry<UUID, Integer> entry : requiredPerBundle.entrySet()) {
                    if (remainingCart.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                        possible = false;
                        break;
                    }
                }

                if (possible) {
                    // Trừ lùi số lượng trong giỏ hàng
                    for (Map.Entry<UUID, Integer> entry : requiredPerBundle.entrySet()) {
                        remainingCart.put(entry.getKey(), remainingCart.get(entry.getKey()) - entry.getValue());
                    }
                    bundlesOfThisRule++;
                }
            }

            if (bundlesOfThisRule > 0) {
                // Tính toán Discount cho Combo này
                BigDecimal discount = calculateBundleDiscount(rule, bundlesOfThisRule, itemPrices);
                matchedBundles.add(new BundleMatchResult<>(rule, bundlesOfThisRule, discount));
            }
        }

        // Cập nhật lại cart gốc sau khi đã bóc tách Combo
        cart.clear();
        cart.putAll(remainingCart);

        return matchedBundles;
    }

    private static BigDecimal calculateBundleDiscount(IBundleRule rule, int count, Map<UUID, BigDecimal> itemPrices) {
        BigDecimal totalCalculatedBase = BigDecimal.ZERO;
        BigDecimal getItemsBase = BigDecimal.ZERO;
        boolean hasGetItems = false;

        for (IBundleItem bi : rule.getBundleItems()) {
            BigDecimal price = itemPrices.getOrDefault(bi.getItemId(), BigDecimal.ZERO);
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(bi.getQuantity()));
            
            totalCalculatedBase = totalCalculatedBase.add(lineTotal);
            
            if ("GET".equalsIgnoreCase(bi.getRole())) {
                getItemsBase = getItemsBase.add(lineTotal);
                hasGetItems = true;
            }
        }

        // Nếu Combo có món GET (Mua X tặng Y), discount CHỈ áp dụng lên giá trị món GET
        // Nếu toàn bộ là món BUY (Combo giảm giá thông thường), discount áp dụng lên toàn bộ Combo
        BigDecimal targetBasePrice = hasGetItems ? getItemsBase : totalCalculatedBase;

        BigDecimal rawDiscountPerBundle = PricingEngine.calculateRawDiscount(
                targetBasePrice, 
                rule.getDiscountType(), 
                rule.getDiscountValue(), 
                rule.getMaxDiscount()
        );

        return rawDiscountPerBundle.multiply(BigDecimal.valueOf(count));
    }
}
