package com.fnb.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("adminSqlTools")
@RequiredArgsConstructor
@Slf4j
public class AdminSqlTools {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Tool("Lấy toàn bộ cấu trúc CSDL (Schema, Table, Column, Data Type) của hệ thống F&B. " +
          "BẠN BẮT BUỘC PHẢI GỌI TOOL NÀY TRƯỚC KHI VIẾT CÂU LỆNH SQL ĐỂ TRÁNH TRUY VẤN SAI TÊN CỘT HOẶC SAI TÊN BẢNG. " +
          "LƯU Ý: Khi JOIN giữa cột uuid và varchar, bạn PHẢI thêm ::text để ép kiểu, ví dụ: JOIN auth.users u ON u.id::text = o.cashier_id::text")
    public String getDatabaseSchema() {
        log.info("[ADMIN-TOOL] getDatabaseSchema");
        try {
            String sql = """
                SELECT table_schema, table_name, column_name, data_type 
                FROM information_schema.columns 
                WHERE table_schema IN ('auth', 'menu', 'orders', 'inventory', 'kds')
                ORDER BY table_schema, table_name, ordinal_position
            """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) return "Không thể lấy cấu trúc CSDL.";
            
            StringBuilder sb = new StringBuilder("CẤU TRÚC DATABASE CHUẨN (Tên Bảng và Các Cột):\n");
            sb.append("LƯU Ý QUAN TRỌNG: Khi JOIN cột uuid với cột khác, PHẢI dùng ::text để ép kiểu.\n\n");
            String currentTable = "";
            for (Map<String, Object> row : rows) {
                String table = row.get("table_schema") + "." + row.get("table_name");
                String col = String.valueOf(row.get("column_name"));
                String type = String.valueOf(row.get("data_type"));
                // Chỉ hiển thị type ngắn gọn cho các kiểu quan trọng dễ nhầm
                String typeHint = switch (type) {
                    case "uuid" -> ":uuid";
                    case "character varying" -> ":varchar";
                    case "bigint" -> ":bigint";
                    case "numeric" -> ":numeric";
                    case "timestamp without time zone", "timestamp with time zone" -> ":timestamp";
                    default -> "";
                };
                if (!table.equals(currentTable)) {
                    if (!currentTable.isEmpty()) sb.append(")\n");
                    sb.append("- ").append(table).append("(").append(col).append(typeHint);
                    currentTable = table;
                } else {
                    sb.append(", ").append(col).append(typeHint);
                }
            }
            if (!currentTable.isEmpty()) sb.append(")\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getDatabaseSchema Error", e);
            return "Lỗi khi lấy Schema: " + e.getMessage();
        }
    }

    @Tool("Vũ khí tối thượng: Truy vấn trực tiếp vào Cơ sở dữ liệu (PostgreSQL) bằng lệnh SQL. " +
          "CHỈ SỬ DỤNG khi các tool khác (Report, Order, Inventory) KHÔNG THỂ trả lời được câu hỏi. " +
          "Yêu cầu SQL phải là lệnh SELECT hợp lệ. LƯU Ý TỐI QUAN TRỌNG: BẠN PHẢI GỌI TOOL 'getDatabaseSchema' TRƯỚC ĐỂ LẤY TÊN CỘT VÀ TÊN BẢNG CHUẨN XÁC, SAU ĐÓ MỚI ĐƯỢC PHÉP DÙNG TOOL NÀY. TUYỆT ĐỐI KHÔNG TỰ BỊA TÊN CỘT. " +
          "CẢNH BÁO TYPE CAST: Khi JOIN cột uuid với cột khác, PHẢI dùng ::text ép kiểu (VD: u.id::text = o.cashier_id::text). Không cast sẽ bị lỗi 'operator does not exist: character varying = uuid'. " +
          "CẢNH BÁO ENCODING: TUYỆT ĐỐI KHÔNG dùng tiếng Việt có dấu trong câu SQL (LIKE '%hủy%' sẽ BỊ LỖI ENCODING). " +
          "Thay vào đó hãy: (1) Lọc theo cột enum/status (transaction_type, status), (2) Nếu bắt buộc dùng LIKE thì chỉ dùng ký tự ASCII không dấu, (3) Hoặc bỏ qua điều kiện LIKE và lọc kết quả sau. " +
          "Dùng khi admin hỏi những câu hỏi phức tạp. Vd: 'Lấy top 5 khách hàng VIP', 'Bảng user có bao nhiêu người'.")
    public String executeReadOnlyQuery(@P("Lệnh SQL SELECT hoàn chỉnh") String sqlQuery) {
        log.warn("[ADMIN-TOOL] executeReadOnlyQuery: {}", sqlQuery);
        
        // Anti-CRUD regex to prevent modifications
        String upperQuery = sqlQuery.toUpperCase().trim();
        if (upperQuery.contains("INSERT") || upperQuery.contains("UPDATE") || 
            upperQuery.contains("DELETE") || upperQuery.contains("DROP") || 
            upperQuery.contains("ALTER") || upperQuery.contains("TRUNCATE") || 
            upperQuery.contains("EXEC") || upperQuery.contains("GRANT")) {
            return "Lỗi bảo mật: AI chỉ được phép thực thi lệnh SELECT. Không được phép chỉnh sửa dữ liệu.";
        }

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery);
            if (result.isEmpty()) {
                return "Truy vấn thành công nhưng không có dữ liệu trả về (Empty ResultSet).";
            }
            // Giới hạn kết quả để LLM không bị quá tải token
            if (result.size() > 50) {
                result = result.subList(0, 50);
                return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA). Chỉ hiển thị 50 dòng đầu tiên:\n" + objectMapper.writeValueAsString(result);
            }
            return "DỮ LIỆU JSON (SYSTEM_NOTE: TIỀN TỆ TRONG DATA LÀ VNĐ. KHÔNG ĐƯỢC TỰ SUY DIỄN ĐƠN VỊ ĐO LƯỜNG LỆCH VỚI DATA):\n" + objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] SQL Error", e);
            return "Lỗi khi thực thi SQL: " + e.getMessage();
        }
    }
}
