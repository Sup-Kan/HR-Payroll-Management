-- Cấu trúc bảng decisions. Chạy theo thứ tự trong install.sql.
CREATE TABLE decisions (
    decision_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    decision_code VARCHAR(30) NOT NULL UNIQUE,
    employee_id INT UNSIGNED NOT NULL,
    decision_type ENUM('REWARD','DISCIPLINE') NOT NULL,
    decision_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    amount DECIMAL(15,2) NULL,
    approval_status ENUM('DRAFT','PENDING','APPROVED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    created_by INT UNSIGNED NULL,
    approved_by INT UNSIGNED NULL,
    approved_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_decisions_employee
      FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
      ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_decisions_created_by
      FOREIGN KEY (created_by) REFERENCES accounts(account_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_decisions_approved_by
      FOREIGN KEY (approved_by) REFERENCES accounts(account_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_decisions_amount CHECK (amount IS NULL OR amount >= 0),
    INDEX idx_decisions_employee_date (employee_id, decision_date),
    INDEX idx_decisions_status_date (approval_status, decision_date)
) ENGINE=InnoDB;
