CREATE DATABASE IF NOT EXISTS hr_payroll_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_payroll_management;

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS payroll_details;
DROP TABLE IF EXISTS payroll_periods;
DROP TABLE IF EXISTS decisions;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS positions;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS payroll_settings;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE departments (
    department_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

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

CREATE TABLE employees (
    employee_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    date_of_birth DATE NULL,
    gender ENUM('MALE','FEMALE','OTHER','UNSPECIFIED') NOT NULL DEFAULT 'UNSPECIFIED',
    phone VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    department_id INT UNSIGNED NULL,
    position_id INT UNSIGNED NULL,
    hire_date DATE NOT NULL,
    employment_status ENUM('ACTIVE','RESIGNED') NOT NULL DEFAULT 'ACTIVE',
    resigned_at DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_department
      FOREIGN KEY (department_id) REFERENCES departments(department_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_employees_position
      FOREIGN KEY (position_id) REFERENCES positions(position_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_employees_name (full_name),
    INDEX idx_employees_status (employment_status),
    CONSTRAINT chk_employee_resigned_date
      CHECK (resigned_at IS NULL OR resigned_at >= hire_date),
    CONSTRAINT chk_employee_code_format
      CHECK (employee_code REGEXP '^NV[0-9]{3}$' AND BINARY employee_code = BINARY UPPER(employee_code))
) ENGINE=InnoDB;

CREATE TABLE accounts (
    account_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_id INT UNSIGNED NULL,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','HR','ACCOUNTANT') NOT NULL,
    account_status ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    CONSTRAINT fk_accounts_employee
      FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_accounts_role_status (role, account_status)
) ENGINE=InnoDB;

CREATE TABLE attendance (
    attendance_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_id INT UNSIGNED NOT NULL,
    work_date DATE NOT NULL,
    time_in TIME NULL,
    time_out TIME NULL,
    work_hours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    day_type ENUM('WEEKDAY','WEEKEND','HOLIDAY') NOT NULL DEFAULT 'WEEKDAY',
    attendance_status ENUM('PRESENT','PAID_LEAVE','UNPAID_LEAVE','ABSENT') NOT NULL DEFAULT 'PRESENT',
    approval_status ENUM('DRAFT','CONFIRMED') NOT NULL DEFAULT 'DRAFT',
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_employee
      FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
      ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_employee_date UNIQUE (employee_id, work_date),
    CONSTRAINT chk_attendance_work_hours CHECK (work_hours >= 0 AND work_hours <= 24),
    CONSTRAINT chk_attendance_overtime CHECK (overtime_hours >= 0 AND overtime_hours <= 24),
    CONSTRAINT chk_attendance_time_order CHECK (time_in IS NULL OR time_out IS NULL OR time_out >= time_in),
    INDEX idx_attendance_date_status (work_date, approval_status),
    INDEX idx_attendance_employee_date (employee_id, work_date)
) ENGINE=InnoDB;

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

CREATE TABLE payroll_details (
    payroll_detail_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payroll_period_id INT UNSIGNED NOT NULL,
    employee_id INT UNSIGNED NOT NULL,
    base_salary_snapshot DECIMAL(15,2) NOT NULL,
    allowance_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    paid_workdays DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    overtime_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reward_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    deduction_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    insurance_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
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
      AND overtime_amount >= 0 AND reward_amount >= 0
      AND deduction_amount >= 0 AND insurance_amount >= 0
      AND income_tax_amount >= 0 AND salary_by_attendance >= 0
      AND gross_salary >= 0 AND net_salary >= 0
    ),
    INDEX idx_payroll_employee (employee_id),
    INDEX idx_payroll_status (payroll_status)
) ENGINE=InnoDB;
