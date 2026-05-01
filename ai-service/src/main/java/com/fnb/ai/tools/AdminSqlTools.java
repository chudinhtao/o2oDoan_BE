package com.fnb.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * [PHASE 4.3 — Level 2] Safe Text-to-SQL Tools.
 *
 * Cung cấp 2 công cụ cho Admin AI:
 *   1. getDatabaseSchema()    — Lấy cấu trúc bảng để AI soạn SQL chính xác.
 *   2. executeReadOnlyQuery() — Thực thi câu SELECT an toàn, tối đa 20 dòng.
 *
 * Quy trình bắt buộc: getDatabaseSchema() → phân tích → executeReadOnlyQuery()
 *
 * BẢO MẬT:
 *   - Chỉ cho phép câu lệnh bắt đầu bằng SELECT.
 *   - Regex word-boundary chặn keyword injection (DROP, DELETE, UPDATE...).
 *   - Backend tự động enforce LIMIT 20 nếu AI quên.
 */
@Slf4j
@Component("adminSqlTools")
@RequiredArgsConstructor
public class AdminSqlTools {

    private final JdbcTemplate jdbc;

    /** Regex chặn DML/DDL keyword dù AI có cố tình nhúng vào string */
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|REPLACE|MERGE|CALL|EXEC|EXECUTE)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // ─── Tool 1: Schema Provider ───────────────────────────────────────────────

    @Tool("""
        Lấy cấu trúc đầy đủ của Database (tên bảng, cột, kiểu dữ liệu, enum, foreign key).
        BẮT BUỘC phải gọi tool này TRƯỚC KHI gọi executeReadOnlyQuery để viết SQL chính xác.
        Không cần tham số.
        """)
    public String getDatabaseSchema() {
        log.info("[SQL-TOOL] Admin AI yêu cầu Database Schema.");
        return DatabaseSchema.GET_CORE_SCHEMA;
    }

    // ─── Tool 2: Safe Query Executor ──────────────────────────────────────────

    @Tool("""
        Chạy câu lệnh SQL SELECT để trả lời câu hỏi ad-hoc không có tool chuyên dụng.
        Yêu cầu: Phải gọi getDatabaseSchema() trước để đảm bảo SQL đúng cấu trúc.
        Tự động giới hạn 20 dòng kết quả. Chỉ chấp nhận SELECT.
        """)
    public String executeReadOnlyQuery(
            @P("Câu lệnh SQL hợp lệ, bắt đầu bằng SELECT, dùng đúng schema prefix (orders., menu., auth.). Không chứa INSERT/UPDATE/DELETE/DROP.")
            String sql) {

        log.info("[SQL-TOOL] Thực thi truy vấn: {}", sql);

        String cleanSql = sql.strip();

        // Guard 1: phải bắt đầu bằng SELECT
        if (!cleanSql.toUpperCase().startsWith("SELECT")) {
            log.warn("[SQL-TOOL] Từ chối: câu lệnh không phải SELECT.");
            return "LỖI BẢO MẬT: Chỉ được phép chạy câu lệnh SELECT.";
        }

        // Guard 2: regex chặn keyword nguy hiểm (word-boundary)
        if (DANGEROUS_KEYWORDS.matcher(cleanSql).find()) {
            log.warn("[SQL-TOOL] Từ chối: phát hiện từ khóa DML/DDL.");
            return "LỖI BẢO MẬT: Phát hiện từ khóa nguy hiểm. Chỉ được đọc dữ liệu (READ-ONLY).";
        }

        // Guard 3: Chặn Multi-statement (chỉ cho phép dấu ; ở cuối cùng nếu có)
        int semiIdx = cleanSql.indexOf(';');
        if (semiIdx >= 0 && semiIdx < cleanSql.length() - 1) {
            log.warn("[SQL-TOOL] Từ chối: phát hiện multi-statement (;).");
            return "LỖI BẢO MẬT: Không được phép thực thi nhiều câu lệnh cùng lúc.";
        }

        // Guard 4: tự động inject LIMIT nếu AI quên (chỉ xử lý chuỗi)
        String safeSql = enforceLimitClause(cleanSql);

        try {
            return jdbc.execute((java.sql.Connection conn) -> {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.setMaxRows(20);         
                    stmt.setQueryTimeout(5);     
                    
                    try (java.sql.ResultSet rs = stmt.executeQuery(safeSql)) {
                        java.sql.ResultSetMetaData rsmd = rs.getMetaData();
                        int columnCount = rsmd.getColumnCount();
                        
                        List<Map<String, Object>> rows = new java.util.ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new java.util.LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(rsmd.getColumnName(i), rs.getObject(i));
                            }
                            rows.add(row);
                        }
                        
                        if (rows.isEmpty()) {
                            return "Truy vấn thành công nhưng không có dữ liệu nào được trả về.";
                        }

                        StringBuilder sb = new StringBuilder("KẾT QUẢ TRUY VẤN (tối đa 20 dòng):\n");
                        for (Map<String, Object> row : rows) {
                            sb.append(row).append("\n");
                        }
                        return sb.toString();
                    }
                } catch (Exception ex) {
                    return "LỖI SQL KHI THỰC THI: " + ex.getMessage();
                }
            });

        } catch (Exception e) {
            log.error("[SQL-TOOL] Lỗi thực thi SQL: {}", e.getMessage());
            return "LỖI HỆ THỐNG CƠ SỞ DỮ LIỆU: " + e.getMessage()
                    + "\nGợi ý: Kiểm tra lại tên bảng (dùng getDatabaseSchema()) và schema prefix.";
        }
    }

    /**
     * Đảm bảo câu SQL luôn có LIMIT 20.
     * Nếu AI đã có LIMIT nhưng > 20, thay thành 20.
     * Nếu chưa có LIMIT, nối thêm vào cuối.
     */
    private String enforceLimitClause(String sql) {
        String upper = sql.toUpperCase();
        int limitIdx = upper.lastIndexOf("LIMIT");
        if (limitIdx >= 0) {
            // Tìm số sau LIMIT và đảm bảo <= 20
            String after = sql.substring(limitIdx + 5).strip();
            String[] parts = after.split("\\s+", 2);
            try {
                int requested = Integer.parseInt(parts[0]);
                if (requested > 20) {
                    return sql.substring(0, limitIdx) + "LIMIT 20";
                }
                return sql; // đã hợp lệ
            } catch (NumberFormatException ignored) {
                // không parse được, inject an toàn
            }
        }
        // Chưa có LIMIT — xóa dấu chấm phẩy cuối (nếu có) rồi thêm
        String trimmed = sql.stripTrailing();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
        }
        return trimmed + " LIMIT 20";
    }
}
