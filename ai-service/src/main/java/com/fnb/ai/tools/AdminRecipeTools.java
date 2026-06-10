package com.fnb.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.ai.feign.InventoryFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("adminRecipeTools")
@RequiredArgsConstructor
@Slf4j
public class AdminRecipeTools {

    private final InventoryFeignClient inventoryFeignClient;
    private final ObjectMapper objectMapper;

    @Tool("Tra cứu danh bạ Nhà cung cấp (Suppliers) của nguyên vật liệu. Lấy tên, số điện thoại, công nợ. " +
          "Dùng khi admin hỏi: 'Tìm số điện thoại nhà cung cấp thịt', 'Thông tin nhà cung cấp ABC'.")
    public String getSupplierInfo(@P("Tên nhà cung cấp cần tìm (hoặc để trống)") String search) {
        log.info("[ADMIN-TOOL] getSupplierInfo search={}", search);
        try {
            var res = inventoryFeignClient.getSuppliers(search, 0, 50);
            if (res == null || res.getData() == null) return "Không tìm thấy dữ liệu nhà cung cấp.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getSupplierInfo error", e);
            return "Lỗi khi lấy thông tin nhà cung cấp: " + e.getMessage();
        }
    }

    @Tool("Lấy công thức (Recipe/BOM) và định lượng của 1 món ăn dựa trên ID món. Từ đó biết được món này tốn những nguyên liệu gì. " +
          "Dùng khi admin hỏi: 'Món Bít tết làm bằng nguyên liệu gì', 'Định lượng của ly trà sữa này ra sao'.")
    public String getRecipeDetails(@P("ID của món ăn (saleItemId)") String saleItemId) {
        log.info("[ADMIN-TOOL] getRecipeDetails saleItemId={}", saleItemId);
        try {
            var res = inventoryFeignClient.getRecipeBySaleItem(UUID.fromString(saleItemId));
            if (res == null || res.getData() == null) return "Không tìm thấy định lượng công thức cho món này.";
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. THỜI GIAN LÀ CHUẨN ISO. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(res.getData());
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRecipeDetails error", e);
            return "Lỗi khi lấy công thức: " + e.getMessage();
        }
    }
}
