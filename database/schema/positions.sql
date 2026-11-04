-- Cấu trúc bảng positions. Chạy theo thứ tự trong install.sql.
CREATE TABLE positions (
    position_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    position_code VARCHAR(20) NOT NULL UNIQUE,
    position_name VARCHAR(100) NOT NULL,
    base_salary DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    allowance_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    description VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_positions_base_salary CHECK (base_salary >= 0),
    CONSTRAINT chk_positions_allowance CHECK (allowance_amount >= 0)
) ENGINE=InnoDB;
