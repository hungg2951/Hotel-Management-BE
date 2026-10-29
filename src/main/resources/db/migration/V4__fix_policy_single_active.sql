-- Migration V4: Cập nhật đảm bảo chỉ 1 chính sách mặc định là ACTIVE, các chính sách còn lại là INACTIVE (Đã tắt)
UPDATE cancellation_policy
SET is_active = FALSE
WHERE is_default = FALSE;

UPDATE early_checkout_policy
SET is_active = FALSE
WHERE is_default = FALSE;

