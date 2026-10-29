-- Migration V2: Tăng độ dài cột bed_type để lưu trữ cấu hình kết hợp nhiều loại giường (VD: 1 giường King và 1 giường phụ)
ALTER TABLE room_type MODIFY (bed_type VARCHAR2(255));
