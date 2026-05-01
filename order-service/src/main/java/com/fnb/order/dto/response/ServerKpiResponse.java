package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * KPI ngày hôm nay của một Server: hiển thị trên Tab Cá Nhân.
 */
@Data
@Builder
public class ServerKpiResponse {
    /** Tổng số món đã bưng ra bàn hôm nay */
    private long totalServed;
    /** Tổng số yêu cầu gọi phục vụ đã xử lý hôm nay */
    private long totalResolved;
    /** Thời gian xử lý trung bình một yêu cầu (giây) */
    private long avgResponseSeconds;
}
