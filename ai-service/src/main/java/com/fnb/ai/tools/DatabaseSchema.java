package com.fnb.ai.tools;

/**
 * [PHASE 4.3 — Level 2] Database Schema Reference cho Text-to-SQL.
 *
 * Hằng số này chứa cấu trúc tối giản của toàn bộ database FnB,
 * được cấp cho AI qua tool getDatabaseSchema() để tránh hallucination SQL.
 *
 * Nguồn: db_schema_utf8.sql (PostgreSQL 16, 4 schemas: auth, menu, orders, kds)
 * Không bao gồm: flyway_schema_history, refresh_tokens, table_info (bảng kỹ thuật)
 */
public final class DatabaseSchema {

    private DatabaseSchema() {}

    public static final String GET_CORE_SCHEMA = """
        [SCHEMA: auth]
        users: id(UUID)PK, username(STR)UNIQ, full_name(STR), phone(STR), pin_code(STR:HIDDEN),
               role(STR:'ADMIN'|'CASHIER'|'STAFF'|'KITCHEN'), is_active(BOOL), created_at(TS)
        -- PIN: chỉ dung de verify, KHONG bao gio SELECT pin_code ra output

        [SCHEMA: menu]
        categories: id(UUID)PK, name(STR), display_order(INT), is_active(BOOL)
        menu_items: id(UUID)PK, category_id(UUID)FK>categories, name(STR), base_price(NUM), sale_price(NUM), station(STR:'HOT'|'COLD'|'DRINK'), is_active(BOOL)
        item_option_groups: id(UUID)PK, item_id(UUID)FK>menu_items, name(STR), type(STR:'SINGLE'|'MULTI')
        item_options: id(UUID)PK, group_id(UUID)FK>item_option_groups, name(STR), extra_price(NUM)
        promotions: id(UUID)PK, code(STR), name(STR), scope(STR:'PRODUCT'|'ORDER'|'BUNDLE'), trigger_type(STR:'AUTO'|'COUPON'), discount_type(STR:'PERCENT'|'FIX_AMOUNT'|'FIX_PRICE'), discount_value(NUM), max_discount(NUM), usage_limit(INT), used_count(INT), is_active(BOOL)
        promotion_requirements: promotion_id(UUID)FK, min_order_amount(NUM), min_quantity(INT)
        promotion_targets: promotion_id(UUID)FK, target_type(STR:'ITEM'|'CATEGORY'|'GLOBAL'), target_id(UUID)
        promotion_bundle_items: promotion_id(UUID)FK, item_id(UUID)FK, role(STR:'BUY'|'GET')
        promotion_schedules: promotion_id(UUID)FK, day_of_week(INT:0-6), start_time(TIME), end_time(TIME)

        [SCHEMA: orders]
        tables: id(UUID)PK, number(INT)UNIQ, name(STR), status(STR:'FREE'|'OCCUPIED'|'CLEANING'|'PAYMENT_REQUESTED'|'MERGED'), capacity(INT), zone(STR), parent_table_id(UUID)FK>tables
        table_sessions: id(UUID)PK, table_id(UUID)FK>tables, status(STR:'ACTIVE'|'CLOSED'|'MERGED'), opened_at(TS)
        orders: id(UUID)PK, session_id(UUID)FK>table_sessions, table_id(UUID)FK>tables,
                source(STR:'QR'|'MANUAL'),
                order_type(STR:'DINE_IN'|'TAKEAWAY'|'DELIVERY'),
                status(STR:'OPEN'|'PAYMENT_REQUESTED'|'PAID'|'CANCELLED'|'MERGED'),
                subtotal(NUM), discount(NUM), total(NUM), promotion_id(UUID), paid_at(TS)-USE_FOR_REVENUE,
                payment_method(STR:'CASH'|'PayOS'|'MIXED'), -- MIXED: Tra mot phan tien mat, mot phan QR
                payment_detail(JSONB), -- Luu breakdown: {"cash": 50000, "qr": 50000}
                cashier_id(UUID)FK>auth.users   -- Phase1: Thu ngan da chot bill nay
                cancelled_by(UUID)FK>auth.users -- Phase1: Manager da duyet huy don
                cancel_reason(STR),             -- Phase1: Ly do huy don
                created_at(TS)
        order_tickets: id(UUID)PK, order_id(UUID)FK>orders, status(STR:'PENDING'|'COMPLETED'|'CANCELLED'), created_at(TS)
        order_ticket_items: id(UUID)PK, ticket_id(UUID)FK>order_tickets,
                menu_item_id(UUID), item_name(STR), unit_price(NUM), quantity(INT),
                status(STR:'PENDING'|'SERVED'|'CANCELLED'), station(STR:'HOT'|'COLD'|'DRINK'),
                served_by(UUID)FK>auth.users,   -- Phase1: Nhan vien da bung mon ra ban
                cancelled_by(UUID)FK>auth.users  -- Phase1: Ai da duyet huy mon le nay
        order_item_options: ticket_item_id(UUID)FK, option_name(STR), extra_price(NUM)
        staff_calls: id(UUID)PK, session_id(UUID)FK, table_id(UUID)FK,
                call_type(STR:'STAFF'|'PAYMENT'), status(STR:'PENDING'|'RESOLVED'),
                resolved_by(UUID)FK>auth.users, -- Phase1: Nhan vien da xu ly chuong goi nay
                message(STR), created_at(TS), resolved_at(TS)
        audit_logs: action_name(STR), user_id(STR), target_id(STR), created_at(TS)

        [SCHEMA: kds]
        kds_tickets: id(UUID)PK, order_id(UUID), status(STR:'PENDING'|'IN_PROGRESS'|'DONE')
        kds_ticket_items: id(UUID)PK, kds_ticket_id(UUID)FK, item_name(STR), quantity(INT), station(STR),
                prepared_by(UUID)FK>auth.users, -- Phase1: Dau bep da lam xong mon nay
                completed_at(TS)                -- Phase1: Thoi diem hoan thanh mon (tinh KPI toc do bep)

        ── SQL RULES (BẪT BUỘC TUÂN THỦ) ──────────────────────────────────
        1. Luon dung schema prefix: orders.orders, menu.menu_items, kds.kds_tickets
        2. Dung paid_at (khong phai created_at) cho bao cao doanh thu / tai chinh
        3. Loc status = 'PAID' khi thong ke don da hoan thanh
        4. source = 'DINE_IN' | 'TAKEAWAY' (go dung, khong tuy y)
        5. Luon them LIMIT 20 de tranh tran bo nho
        6. Noi bang dung: order_ticket_items JOIN order_tickets JOIN orders (khong nhay coc)
        7. TUYET DOI KHONG hoi nguoi dung cung cap UUID. Tim theo ten dung `item_name ILIKE '%ten%'`
        8. [Phase1 KPI] JOIN auth.users CHI DUOC PHEP khi tinh KPI nhan su:
           - cashier_id -> u.full_name: Thong ke so don da chot cua thu ngan
           - cancelled_by -> u.full_name: Danh sach don/mon da huy theo nguoi duyet
           - served_by -> u.full_name: So mon da bung cua tung nhan vien
           - resolved_by -> u.full_name: So cuoc goi NV da xu ly
           - prepared_by -> u.full_name: Toc do bep theo dau bep (KPI)
        9. TUYET DOI KHONG SELECT pin_code tu bat ky bang nao
       10. Neu du lieu tracking con NULL (chua co), bao cao dieu do ro rang thay vi bao loi
        ============================================================
        """;
}
