package com.fnb.inventory.enums;

public enum TransactionType {
    IN_PO,           // Nhập từ Purchase Order
    IN_QUICK,        // Nhập kho nhanh
    IN_TRANSFER,     // Nhập chuyển kho
    OUT_SALE,        // Xuất bán qua POS
    OUT_WASTE,       // Xuất hủy / hao hụt
    OUT_TRANSFER,    // Xuất chuyển kho
    HOLD,            // Tạm giữ (khi có order chưa thanh toán)
    REFUND,          // Hoàn trả lại kho
    ADJUSTMENT,      // Điều chỉnh sau kiểm kê
    MANUAL_BLOCK     // Khóa thủ công
}
