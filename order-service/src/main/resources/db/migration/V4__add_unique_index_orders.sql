-- Đảm bảo mỗi Session (phiên) chỉ có duy nhất 1 Order đang mở (OPEN) hoặc chờ thanh toán (PAYMENT_REQUESTED).
-- Điều này ngăn chặn việc đẻ ra nhiều Order liên tiếp nếu order trước chưa thanh toán xong.
CREATE UNIQUE INDEX IF NOT EXISTS unq_active_order ON orders(session_id) WHERE status IN ('OPEN', 'PAYMENT_REQUESTED');
