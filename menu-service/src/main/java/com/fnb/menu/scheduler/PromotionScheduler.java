package com.fnb.menu.scheduler;

import com.fnb.menu.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionScheduler {

    private final PromotionRepository promotionRepository;

    /**
     * Tự động khóa các khuyến mãi đã hết hạn.
     *
     * Tối ưu: dùng bulk UPDATE thay vì findAll() + Java filter + saveAll().
     * Một câu JPQL UPDATE duy nhất tác động trực tiếp lên DB, không load bất kỳ entity nào vào RAM.
     *
     * Chạy mỗi 5 phút (đủ độ chính xác cho F&B, giảm tải so với 1 phút).
     */
    @Scheduled(fixedRate = 300_000) // 5 phút
    @Transactional
    public void autoExpirePromotions() {
        int updated = promotionRepository.bulkExpire(LocalDateTime.now());
        if (updated > 0) {
            log.info("Scheduler: Đã khóa {} khuyến mãi hết hạn.", updated);
        }
    }
}
