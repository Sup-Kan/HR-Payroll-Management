-- Cấu trúc bảng payroll_settings. Chạy theo thứ tự trong install.sql.
CREATE TABLE payroll_settings (
    setting_id TINYINT UNSIGNED PRIMARY KEY,
    standard_workdays_per_month DECIMAL(4,2) NOT NULL DEFAULT 22.00,
    standard_hours_per_day DECIMAL(4,2) NOT NULL DEFAULT 8.00,
    insurance_rate DECIMAL(6,5) NOT NULL DEFAULT 0.00000,
    income_tax_rate DECIMAL(6,5) NOT NULL DEFAULT 0.00000,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_settings_workdays CHECK (standard_workdays_per_month > 0),
    CONSTRAINT chk_settings_hours CHECK (standard_hours_per_day > 0),
    CONSTRAINT chk_settings_insurance CHECK (insurance_rate >= 0 AND insurance_rate <= 1),
    CONSTRAINT chk_settings_tax CHECK (income_tax_rate >= 0 AND income_tax_rate <= 1)
) ENGINE=InnoDB;
