-- Cấu trúc bảng payroll_details. Chạy theo thứ tự trong install.sql.
CREATE TABLE payroll_details (
    payroll_detail_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payroll_period_id INT UNSIGNED NOT NULL,
    employee_id INT UNSIGNED NOT NULL,
    base_salary_snapshot DECIMAL(15,2) NOT NULL,
    allowance_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    paid_workdays DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    overtime_weekday_hours DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    overtime_weekend_hours DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    overtime_holiday_hours DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    overtime_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reward_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    deduction_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    insurance_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    insurance_rate DECIMAL(6,5) NOT NULL DEFAULT 0.00000,
    income_tax_rate DECIMAL(6,5) NOT NULL DEFAULT 0.00000,
    income_tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    salary_by_attendance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    gross_salary DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    net_salary DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payroll_status ENUM('DRAFT','CALCULATED','FINALIZED') NOT NULL DEFAULT 'DRAFT',
    calculated_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_details_period
      FOREIGN KEY (payroll_period_id) REFERENCES payroll_periods(payroll_period_id)
      ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_payroll_details_employee
      FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
      ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_payroll_employee_period UNIQUE (payroll_period_id, employee_id),
    CONSTRAINT chk_payroll_detail_amounts CHECK (
      base_salary_snapshot >= 0 AND allowance_snapshot >= 0
      AND paid_workdays >= 0 AND overtime_hours >= 0
      AND overtime_weekday_hours >= 0 AND overtime_weekend_hours >= 0
      AND overtime_holiday_hours >= 0
      AND overtime_amount >= 0 AND reward_amount >= 0
      AND deduction_amount >= 0 AND insurance_amount >= 0
      AND income_tax_amount >= 0 AND salary_by_attendance >= 0
      AND gross_salary >= 0 AND net_salary >= 0
    ),
    INDEX idx_payroll_employee (employee_id),
    INDEX idx_payroll_status (payroll_status)
) ENGINE=InnoDB;
