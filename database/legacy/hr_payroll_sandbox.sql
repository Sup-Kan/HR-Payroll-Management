-- ============================================================
-- MÔI TRƯỜNG THỬ NGHIỆM
-- Script khởi tạo đầy đủ cấu trúc + dữ liệu mẫu.
-- CẢNH BÁO: script xóa các bảng hiện có trong đúng database này
-- trước khi tạo lại. Không chạy trên database đang chứa dữ liệu cần giữ.
-- Bản sandbox là bản sao dữ liệu khởi tạo từ primary tại cùng thời điểm.
-- Mật khẩu đăng nhập demo của 50 tài khoản: HrDemo@2026
-- ============================================================

CREATE DATABASE IF NOT EXISTS hr_payroll_sandbox
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_payroll_sandbox;

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
    role ENUM('ADMIN','IT','HR','ACCOUNTANT','EMPLOYEE','SECURITY','CLEANER','OPERATOR') NOT NULL,
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


SET NAMES utf8mb4;

INSERT INTO departments (department_code, department_name, description) VALUES
    ('HR', 'Nhân sự', 'Quản lý nhân sự và hồ sơ'),
    ('ACC', 'Kế toán', 'Quản lý tài chính và tiền lương'),
    ('IT', 'Công nghệ thông tin', 'Phát triển và hỗ trợ hệ thống'),
    ('SEC', 'An ninh - Bảo vệ', 'Đảm bảo an ninh và an toàn'),
    ('FAC', 'Hành chính - Vệ sinh', 'Vệ sinh và hậu cần văn phòng'),
    ('OPS', 'Vận hành', 'Vận hành và hỗ trợ hoạt động');

INSERT INTO positions (position_code, position_name, base_salary, allowance_amount, description) VALUES
    ('ADMIN', 'Quản trị hệ thống', 18000000, 2500000, 'Quản trị hệ thống và tài khoản'),
    ('HR_MANAGER', 'Trưởng nhóm nhân sự', 16000000, 2000000, 'Phụ trách nhân sự'),
    ('HR_STAFF', 'Nhân viên nhân sự', 9500000, 700000, 'Nghiệp vụ nhân sự'),
    ('ACC_MANAGER', 'Kế toán trưởng', 17000000, 2200000, 'Phụ trách kế toán'),
    ('ACCOUNTANT', 'Kế toán viên', 11000000, 900000, 'Nghiệp vụ kế toán'),
    ('IT_MANAGER', 'Trưởng nhóm IT', 20000000, 3000000, 'Quản lý công nghệ thông tin'),
    ('IT_ENGINEER', 'Kỹ sư công nghệ thông tin', 14500000, 1500000, 'Phát triển và vận hành hệ thống'),
    ('SECURITY', 'Nhân viên bảo vệ', 7500000, 500000, 'Đảm bảo an ninh'),
    ('CLEANER', 'Nhân viên vệ sinh', 6800000, 400000, 'Vệ sinh văn phòng'),
    ('OPERATIONS', 'Nhân viên vận hành', 9000000, 600000, 'Hỗ trợ vận hành');

INSERT INTO employees
(employee_code, full_name, date_of_birth, gender, phone, address, department_id, position_id, hire_date, employment_status) VALUES
('NV001', 'Nguyễn Minh An', '1986-02-02', 'MALE', '0900000001', 'Hà Nội', 1, 1, '2022-02-02', 'ACTIVE'),
('NV002', 'Trần Thu Hà', '1987-03-03', 'FEMALE', '0900000002', 'Đà Nẵng', 1, 2, '2023-03-03', 'ACTIVE'),
('NV003', 'Phạm Ngọc Mai', '1988-04-04', 'MALE', '0900000003', 'Bình Dương', 1, 3, '2024-04-04', 'ACTIVE'),
('NV004', 'Lê Thị Thanh', '1989-05-05', 'FEMALE', '0900000004', 'Đồng Nai', 1, 3, '2025-05-05', 'ACTIVE'),
('NV005', 'Nguyễn Hoàng Yến', '1990-06-06', 'MALE', '0900000005', 'TP. Hồ Chí Minh', 1, 3, '2021-06-06', 'ACTIVE'),
('NV006', 'Đỗ Minh Châu', '1991-07-07', 'FEMALE', '0900000006', 'Hà Nội', 1, 3, '2022-07-07', 'ACTIVE'),
('NV007', 'Lê Quốc Bình', '1992-08-08', 'MALE', '0900000007', 'Đà Nẵng', 2, 4, '2023-08-08', 'ACTIVE'),
('NV008', 'Vũ Thị Lan', '1993-09-09', 'FEMALE', '0900000008', 'Bình Dương', 2, 5, '2024-09-09', 'ACTIVE'),
('NV009', 'Phan Đức Long', '1994-10-10', 'MALE', '0900000009', 'Đồng Nai', 2, 5, '2025-10-10', 'ACTIVE'),
('NV010', 'Đặng Thu Trang', '1995-11-11', 'FEMALE', '0900000010', 'TP. Hồ Chí Minh', 2, 5, '2021-11-11', 'ACTIVE'),
('NV011', 'Bùi Thành Đạt', '1996-12-12', 'MALE', '0900000011', 'Hà Nội', 2, 5, '2022-12-12', 'ACTIVE'),
('NV012', 'Trần Văn Khoa', '1997-01-13', 'FEMALE', '0900000012', 'Đà Nẵng', 3, 6, '2023-01-13', 'ACTIVE'),
('NV013', 'Nguyễn Đức Huy', '1998-02-14', 'MALE', '0900000013', 'Bình Dương', 3, 6, '2024-02-14', 'ACTIVE'),
('NV014', 'Phạm Thành Nam', '1999-03-15', 'FEMALE', '0900000014', 'Đồng Nai', 3, 7, '2025-03-15', 'ACTIVE'),
('NV015', 'Hoàng Minh Quân', '2000-04-16', 'MALE', '0900000015', 'TP. Hồ Chí Minh', 3, 7, '2021-04-16', 'ACTIVE'),
('NV016', 'Võ Quốc Bảo', '1985-05-17', 'FEMALE', '0900000016', 'Hà Nội', 3, 7, '2022-05-17', 'ACTIVE'),
('NV017', 'Đặng Anh Tuấn', '1986-06-18', 'MALE', '0900000017', 'Đà Nẵng', 3, 7, '2023-06-18', 'ACTIVE'),
('NV018', 'Bùi Nhật Minh', '1987-07-19', 'FEMALE', '0900000018', 'Bình Dương', 3, 7, '2024-07-19', 'ACTIVE'),
('NV019', 'Lý Thanh Tùng', '1988-08-20', 'MALE', '0900000019', 'Đồng Nai', 3, 7, '2025-08-20', 'ACTIVE'),
('NV020', 'Đỗ Quang Hưng', '1989-09-21', 'FEMALE', '0900000020', 'TP. Hồ Chí Minh', 3, 7, '2021-09-21', 'ACTIVE'),
('NV021', 'Nguyễn Thành Đạt', '1990-10-22', 'MALE', '0900000021', 'Hà Nội', 3, 7, '2022-10-22', 'ACTIVE'),
('NV022', 'Phan Minh Đức', '1991-11-23', 'FEMALE', '0900000022', 'Đà Nẵng', 3, 7, '2023-11-23', 'ACTIVE'),
('NV023', 'Trương Gia Bảo', '1992-12-24', 'MALE', '0900000023', 'Bình Dương', 3, 7, '2024-12-24', 'ACTIVE'),
('NV024', 'Lê Hoàng Phúc', '1993-01-25', 'FEMALE', '0900000024', 'Đồng Nai', 3, 7, '2025-01-25', 'ACTIVE'),
('NV025', 'Vũ Minh Khôi', '1994-02-26', 'MALE', '0900000025', 'TP. Hồ Chí Minh', 3, 7, '2021-02-26', 'ACTIVE'),
('NV026', 'Đinh Quốc Việt', '1995-03-01', 'FEMALE', '0900000026', 'Hà Nội', 3, 7, '2022-03-01', 'ACTIVE'),
('NV027', 'Hồ Anh Dũng', '1996-04-02', 'MALE', '0900000027', 'Đà Nẵng', 3, 7, '2023-04-02', 'ACTIVE'),
('NV028', 'Cao Tuấn Kiệt', '1997-05-03', 'FEMALE', '0900000028', 'Bình Dương', 3, 7, '2024-05-03', 'ACTIVE'),
('NV029', 'Mai Đức Anh', '1998-06-04', 'MALE', '0900000029', 'Đồng Nai', 3, 7, '2025-06-04', 'ACTIVE'),
('NV030', 'Tạ Minh Tâm', '1999-07-05', 'FEMALE', '0900000030', 'TP. Hồ Chí Minh', 3, 7, '2021-07-05', 'ACTIVE'),
('NV031', 'Dương Hải Đăng', '2000-08-06', 'MALE', '0900000031', 'Hà Nội', 3, 7, '2022-08-06', 'ACTIVE'),
('NV032', 'Nguyễn Văn Sơn', '1985-09-07', 'FEMALE', '0900000032', 'Đà Nẵng', 4, 8, '2023-09-07', 'ACTIVE'),
('NV033', 'Trần Hữu Phước', '1986-10-08', 'MALE', '0900000033', 'Bình Dương', 4, 8, '2024-10-08', 'ACTIVE'),
('NV034', 'Lê Thành Công', '1987-11-09', 'FEMALE', '0900000034', 'Đồng Nai', 4, 8, '2025-11-09', 'ACTIVE'),
('NV035', 'Phạm Văn Hùng', '1988-12-10', 'MALE', '0900000035', 'TP. Hồ Chí Minh', 4, 8, '2021-12-10', 'ACTIVE'),
('NV036', 'Võ Minh Tài', '1989-01-11', 'FEMALE', '0900000036', 'Hà Nội', 4, 8, '2022-01-11', 'ACTIVE'),
('NV037', 'Đặng Quốc Thịnh', '1990-02-12', 'MALE', '0900000037', 'Đà Nẵng', 4, 8, '2023-02-12', 'ACTIVE'),
('NV038', 'Bùi Văn Lộc', '1991-03-13', 'FEMALE', '0900000038', 'Bình Dương', 4, 8, '2024-03-13', 'ACTIVE'),
('NV039', 'Đỗ Thành Nhân', '1992-04-14', 'MALE', '0900000039', 'Đồng Nai', 4, 8, '2025-04-14', 'ACTIVE'),
('NV040', 'Hoàng Văn Phú', '1993-05-15', 'FEMALE', '0900000040', 'TP. Hồ Chí Minh', 4, 8, '2021-05-15', 'ACTIVE'),
('NV041', 'Nguyễn Thị Hoa', '1994-06-16', 'MALE', '0900000041', 'Hà Nội', 5, 9, '2022-06-16', 'ACTIVE'),
('NV042', 'Trần Thị Hạnh', '1995-07-17', 'FEMALE', '0900000042', 'Đà Nẵng', 5, 9, '2023-07-17', 'ACTIVE'),
('NV043', 'Lê Thị Hồng', '1996-08-18', 'MALE', '0900000043', 'Bình Dương', 5, 9, '2024-08-18', 'ACTIVE'),
('NV044', 'Phạm Thị Dung', '1997-09-19', 'FEMALE', '0900000044', 'Đồng Nai', 5, 9, '2025-09-19', 'ACTIVE'),
('NV045', 'Võ Thị Nga', '1998-10-20', 'MALE', '0900000045', 'TP. Hồ Chí Minh', 5, 9, '2021-10-20', 'ACTIVE'),
('NV046', 'Đặng Thị Vân', '1999-11-21', 'FEMALE', '0900000046', 'Hà Nội', 5, 9, '2022-11-21', 'ACTIVE'),
('NV047', 'Phan Văn Tín', '2000-12-22', 'MALE', '0900000047', 'Đà Nẵng', 6, 10, '2023-12-22', 'ACTIVE'),
('NV048', 'Nguyễn Quốc Cường', '1985-01-23', 'FEMALE', '0900000048', 'Bình Dương', 6, 10, '2024-01-23', 'ACTIVE'),
('NV049', 'Trần Minh Trí', '1986-02-24', 'MALE', '0900000049', 'Đồng Nai', 6, 10, '2025-02-24', 'ACTIVE'),
('NV050', 'Lê Anh Tuấn', '1987-03-25', 'FEMALE', '0900000050', 'TP. Hồ Chí Minh', 6, 10, '2021-03-25', 'ACTIVE');

-- Tài khoản mẫu dùng chung mật khẩu demo: HrDemo@2026.
-- Bắt buộc đổi mật khẩu trước khi dùng dữ liệu thật.
INSERT INTO accounts (employee_id, username, password_hash, role, account_status) VALUES
(1, 'admin_demo', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ADMIN', 'ACTIVE'),
(2, 'hr01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'HR', 'ACTIVE'),
(3, 'hr02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'HR', 'ACTIVE'),
(4, 'hr03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'HR', 'ACTIVE'),
(5, 'hr04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'HR', 'ACTIVE'),
(6, 'hr05', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'HR', 'ACTIVE'),
(7, 'accountant01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ACCOUNTANT', 'ACTIVE'),
(8, 'accountant02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ACCOUNTANT', 'ACTIVE'),
(9, 'accountant03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ACCOUNTANT', 'ACTIVE'),
(10, 'accountant04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ACCOUNTANT', 'ACTIVE'),
(11, 'accountant05', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'ACCOUNTANT', 'ACTIVE'),
(12, 'it01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(13, 'it02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(14, 'it03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(15, 'it04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(16, 'it05', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(17, 'it06', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(18, 'it07', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(19, 'it08', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(20, 'it09', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(21, 'it10', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(22, 'it11', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(23, 'it12', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(24, 'it13', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(25, 'it14', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(26, 'it15', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(27, 'it16', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(28, 'it17', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(29, 'it18', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(30, 'it19', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(31, 'it20', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'IT', 'ACTIVE'),
(32, 'security01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(33, 'security02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(34, 'security03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(35, 'security04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(36, 'security05', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(37, 'security06', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'SECURITY', 'ACTIVE'),
(38, 'security07', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(39, 'security08', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(40, 'security09', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(41, 'cleaner01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(42, 'cleaner02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(43, 'cleaner03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(44, 'cleaner04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(45, 'cleaner05', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(46, 'cleaner06', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(47, 'operations01', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(48, 'operations02', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(49, 'operations03', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE'),
(50, 'operations04', '$2a$10$Iag3yw3j1OeYMEP/R65STeBDUw8EYRks9t0jfIuq.vQocuCdUDFlq', 'EMPLOYEE', 'ACTIVE');

INSERT INTO payroll_settings (setting_id, standard_workdays_per_month, standard_hours_per_day, insurance_rate, income_tax_rate) VALUES (1, 22.00, 8.00, 0.00000, 0.00000);

INSERT INTO attendance (employee_id, work_date, time_in, time_out, work_hours, overtime_hours, day_type, attendance_status, approval_status, note) VALUES
(1, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(1, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(1, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(2, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(2, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(2, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(3, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(3, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(3, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(4, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(4, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(4, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(5, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(5, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(5, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(6, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(6, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(6, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(7, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(7, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(7, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(8, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(8, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(8, '2026-10-03', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(9, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(9, '2026-10-02', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(9, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(10, '2026-10-01', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(10, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(10, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(11, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(11, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(11, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(12, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(12, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(12, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(13, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(13, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(13, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(14, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(14, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(14, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(15, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(15, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(15, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(16, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(16, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(16, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(17, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(17, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(17, '2026-10-03', NULL, NULL, 0.00, 0.00, 'WEEKDAY', 'PAID_LEAVE', 'CONFIRMED', 'Dữ liệu minh họa'),
(18, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(18, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(18, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(19, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(19, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(19, '2026-10-03', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(20, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(20, '2026-10-02', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(20, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(21, '2026-10-01', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(21, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(21, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(22, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(22, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(22, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(23, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(23, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(23, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(24, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(24, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(24, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(25, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(25, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(25, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(26, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(26, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(26, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(27, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(27, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(27, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(28, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(28, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(28, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(29, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(29, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(29, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(30, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(30, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(30, '2026-10-03', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(31, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(31, '2026-10-02', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(31, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(32, '2026-10-01', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(32, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(32, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(33, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(33, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(33, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(34, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(34, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(34, '2026-10-03', NULL, NULL, 0.00, 0.00, 'WEEKDAY', 'PAID_LEAVE', 'CONFIRMED', 'Dữ liệu minh họa'),
(35, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(35, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(35, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(36, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(36, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(36, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(37, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(37, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(37, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(38, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(38, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(38, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(39, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(39, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(39, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(40, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(40, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(40, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(41, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(41, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(41, '2026-10-03', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(42, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(42, '2026-10-02', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(42, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(43, '2026-10-01', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(43, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(43, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(44, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(44, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(44, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(45, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(45, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(45, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(46, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(46, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(46, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(47, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(47, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(47, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(48, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(48, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(48, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(49, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(49, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(49, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(50, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(50, '2026-10-02', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa'),
(50, '2026-10-03', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu minh họa');

INSERT INTO decisions (decision_code, employee_id, decision_type, decision_date, reason, amount, approval_status, created_by, approved_by, approved_at) VALUES
('QD-KT-2026-001', 1, 'REWARD', '2026-10-11', 'Thành tích/ghi nhận mẫu', 325000.00, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KL-2026-002', 2, 'DISCIPLINE', '2026-10-12', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KT-2026-003', 3, 'REWARD', '2026-10-13', 'Thành tích/ghi nhận mẫu', 375000.00, 'PENDING', 1, NULL, NULL),
('QD-KL-2026-004', 4, 'DISCIPLINE', '2026-10-14', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KT-2026-005', 5, 'REWARD', '2026-10-15', 'Thành tích/ghi nhận mẫu', 425000.00, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KL-2026-006', 6, 'DISCIPLINE', '2026-10-16', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'PENDING', 1, NULL, NULL),
('QD-KT-2026-007', 7, 'REWARD', '2026-10-17', 'Thành tích/ghi nhận mẫu', 475000.00, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KL-2026-008', 8, 'DISCIPLINE', '2026-10-18', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KT-2026-009', 9, 'REWARD', '2026-10-19', 'Thành tích/ghi nhận mẫu', 525000.00, 'PENDING', 1, NULL, NULL),
('QD-KL-2026-010', 10, 'DISCIPLINE', '2026-10-20', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KT-2026-011', 11, 'REWARD', '2026-10-21', 'Thành tích/ghi nhận mẫu', 575000.00, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KL-2026-012', 12, 'DISCIPLINE', '2026-10-22', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'PENDING', 1, NULL, NULL),
('QD-KT-2026-013', 13, 'REWARD', '2026-10-23', 'Thành tích/ghi nhận mẫu', 625000.00, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KL-2026-014', 14, 'DISCIPLINE', '2026-10-24', 'Ghi nhận nhắc nhở, không mặc định phạt tiền', NULL, 'APPROVED', 1, 1, '2026-10-25 10:00:00'),
('QD-KT-2026-015', 15, 'REWARD', '2026-10-10', 'Thành tích/ghi nhận mẫu', 675000.00, 'PENDING', 1, NULL, NULL);

INSERT INTO payroll_periods (payroll_year, payroll_month, period_status, opened_by) VALUES (2026, 10, 'OPEN', 1);
