-- Cấu trúc bảng payroll_periods. Chạy theo thứ tự trong install.sql.
CREATE TABLE payroll_periods (
    payroll_period_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payroll_year SMALLINT UNSIGNED NOT NULL,
    payroll_month TINYINT UNSIGNED NOT NULL,
    period_status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    opened_by INT UNSIGNED NULL,
    closed_by INT UNSIGNED NULL,
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    UNIQUE KEY uq_payroll_year_month (payroll_year, payroll_month),
    CONSTRAINT fk_payroll_period_opened_by
      FOREIGN KEY (opened_by) REFERENCES accounts(account_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_payroll_period_closed_by
      FOREIGN KEY (closed_by) REFERENCES accounts(account_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_payroll_month CHECK (payroll_month BETWEEN 1 AND 12)
) ENGINE=InnoDB;
