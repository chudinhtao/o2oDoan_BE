package com.fnb.ai.agent.admin;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * [PHASE 4.1] LLM Router de dieu huong thong minh (thay the Regex).
 * Phan loai y dinh Admin thanh 1 trong 4 domain.
 */
@AiService(tools = {})
public interface AdminRouterAgent {

    @SystemMessage("""
        Ban la he thong Router thong minh cho Admin Nha hang.
        Nhiem vu cua ban la phan loai cau hoi cua Admin vao DUNG 1 TRONG 4 DOMAIN duoi day:

        1. FINANCE: Chuyen ve dong tien, AOV, loi nhuan, ROI khuyen mai, phan tich kenh ban (QR vs MANUAL), giam gia.
        2. OPS: Chuyen ve van hanh bep, toc do phuc vu, tinh trang menu (het hang), don huy, thong ke goi nhan vien (staff calls), ghep ban.
        3. REPORT: (DEFAULT CHO TRUY XUAT DU LIEU). Cac bao cao doanh thu, top mon, hoac BAT KY cau hoi AD-HOC nao yeu cau: "danh sach", "thong ke", "dem so luong", "co bao nhieu", "ai la nguoi", "ban so may". (Gom tat ca truy van SQL vao day).
        4. OTHER: CHI DUNG cho nhung loi chao vo nghia hoac cau hoi hoan toan khong the truy xuat tu Database nha hang.

        Tra ve CHI 1 TU DUY NHAT thuoc danh sach: FINANCE, OPS, REPORT, OTHER.
        Khong giai thich, khong them dau cau.
        """)
    String routeIntent(@dev.langchain4j.service.MemoryId String memoryId, @UserMessage String userMessage);
}
