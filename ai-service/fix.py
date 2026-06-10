import os

replacements = {
    "Dung khi admin/le tan hoi: 'toi mai luc 19h con nhan duoc ban 10 nguoi khong'": "Dùng khi admin/lễ tân hỏi: 'tối mai lúc 19h còn nhận được bàn 10 người không'",
    "Lay danh sach khach dat ban": "Lấy danh sách khách đặt bàn",
    "Loi khi lay danh sach dat ban": "Lỗi khi lấy danh sách đặt bàn",
    "Dung khi admin hoi 'ty le huy ban', 'bao nhieu khach khong den'": "Dùng khi admin hỏi 'tỷ lệ hủy bàn', 'bao nhiêu khách không đến'",
    "Dung khi admin hoi: 'hien dang co bao nhieu ban', 'quan co dong khong', 'tong tien dang an la bao nhieu'": "Dùng khi admin hỏi: 'hiện đang có bao nhiêu bàn', 'quán có đông không', 'tổng tiền đang ăn là bao nhiêu'",
    "Dung khi admin hoi: 'khach thuong tra bang gi', 'hom nay thu tien mat bao nhieu'": "Dùng khi admin hỏi: 'khách thường trả bằng gì', 'hôm nay thu tiền mặt bao nhiêu'",
    "Dung khi admin hoi: 'ai da cham cong', 'co ai di tre khong', 'ai vang mat hom nay'": "Dùng khi admin hỏi: 'ai đã chấm công', 'có ai đi trễ không', 'ai vắng mặt hôm nay'",
    "Dung khi can tham khao chuan muc nganh hoac ly thuyet van hanh nha hang de dua ra loi khuyen": "Dùng khi cần tham khảo chuẩn mực ngành hoặc lý thuyết vận hành nhà hàng để đưa ra lời khuyên",
    "Dung khi can de xuat mon moi, chuong trinh khuyen mai theo trend hoac danh gia xem nha hang co bi tut hau khong": "Dùng khi cần đề xuất món mới, chương trình khuyến mãi theo trend hoặc đánh giá xem nhà hàng có bị tụt hậu không",
    "Dung khi admin hoi: 'bep dang co bao nhieu don', 'bep co dang qua tai khong'": "Dùng khi admin hỏi: 'bếp đang có bao nhiêu đơn', 'bếp có đang quá tải không'",
    "Dung khi admin hoi: 'co mon nao bi cham khong', 'tram bep nao dang bi ket'": "Dùng khi admin hỏi: 'có món nào bị chậm không', 'trạm bếp nào đang bị kẹt'",
    "Dung khi admin hoi: 'thoi gian lam mon trung binh', 'hieu suat bep hom nay', 'ty le tre don'": "Dùng khi admin hỏi: 'thời gian làm món trung bình', 'hiệu suất bếp hôm nay', 'tỷ lệ trễ đơn'",
    "Dung khi admin hoi: 'KM nao hieu qua nhat', 'khuyen mai co loi khong', 'nen giu KM nao'": "Dùng khi admin hỏi: 'KM nào hiệu quả nhất', 'khuyến mãi có lời không', 'nên giữ KM nào'",
    "Dung khi admin hoi: 'AOV dang the nao', 'khach chi tieu bao nhieu', 'gia tri don hang co tang khong'": "Dùng khi admin hỏi: 'AOV đang thế nào', 'khách chi tiêu bao nhiêu', 'giá trị đơn hàng có tăng không'",
    "Dung khi admin hoi: 'kenh nao hieu qua', 'QR chiem bao nhieu', 'nen dau tu kenh nao'": "Dùng khi admin hỏi: 'kênh nào hiệu quả', 'QR chiếm bao nhiêu', 'nên đầu tư kênh nào'",
    "Ban la he thong Router thong minh cho Admin Nha hang": "Bạn là hệ thống Router thông minh cho Admin Nhà hàng",
    "Nhiem vu cua ban la phan loai cau hoi cua Admin vao DUNG 1 TRONG 4 DOMAIN duoi day": "Nhiệm vụ của bạn là phân loại câu hỏi của Admin vào ĐÚNG 1 TRONG 4 DOMAIN dưới đây",
    "Ban la CHUYEN GIA TU VAN CHIEN LUOC KINH DOANH (Virtual COO) danh rieng cho chu nha hang": "Bạn là CHUYÊN GIA TƯ VẤN CHIẾN LƯỢC KINH DOANH (Virtual COO) dành riêng cho chủ nhà hàng",
    "Ban la mot AI tu dong. Ban KHONG DUOC PHEO TRINH BAY KẾ HOẠCH hoac XIN PHÉP": "Bạn là một AI tự động. Bạn KHÔNG ĐƯỢC TRÌNH BÀY KẾ HOẠCH hoặc XIN PHÉP",
    "Chuyen gia Van hanh cua nha hang": "Chuyên gia Vận hành của nhà hàng",
    "Chuyen gia Tai chinh cua nha hang": "Chuyên gia Tài chính của nhà hàng",
    "Chuyen gia phan tich bao cao kinh doanh toan dien cho Admin": "Chuyên gia phân tích báo cáo kinh doanh toàn diện cho Admin",
    "Lay danh sach ve bep (KDS tickets) dang hoat dong hoac moi hoan thanh": "Lấy danh sách vé bếp (KDS tickets) đang hoạt động hoặc mới hoàn thành"
}

dir_path = r'd:\\srcDOAN\\backend\\ai-service\\src\\main\\java\\com\\fnb\\ai'

for root, _, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            modified = False
            for k, v in replacements.items():
                if k in content:
                    content = content.replace(k, v)
                    modified = True
                    
            if modified:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f'Fixed: {file}')
