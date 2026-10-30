USE hr_payroll_management;
SET NAMES utf8mb4;

INSERT INTO departments (department_code, department_name, description) VALUES
('HR', 'Nhân sự', 'Quản lý nhân sự và hồ sơ'),
('ACC', 'Kế toán', 'Quản lý tài chính và tiền lương'),
('IT', 'Công nghệ thông tin', 'Hỗ trợ hệ thống');

INSERT INTO positions (position_code, position_name, base_salary, allowance_amount, description) VALUES
('HR_STAFF', 'Nhân viên nhân sự', 9000000.00, 500000.00, 'Vị trí mẫu'),
('ACCOUNTANT', 'Kế toán viên', 10000000.00, 700000.00, 'Vị trí mẫu'),
('IT_STAFF', 'Nhân viên IT', 12000000.00, 1000000.00, 'Vị trí mẫu'),
('HR_MANAGER', 'Trưởng phòng nhân sự', 16000000.00, 2000000.00, 'Vị trí mẫu');

INSERT INTO employees
(employee_code, full_name, date_of_birth, gender, phone, address, department_id, position_id, hire_date, employment_status)
VALUES
('NV001', 'Nguyễn Minh An', '1998-03-12', 'MALE', '0900000001', 'Hà Nội', 1, 1, '2024-01-15', 'ACTIVE'),
('NV002', 'Trần Thu Hà', '1996-08-21', 'FEMALE', '0900000002', 'Đà Nẵng', 2, 2, '2023-06-01', 'ACTIVE'),
('NV003', 'Lê Quốc Bình', '1999-11-05', 'MALE', '0900000003', 'TP. Hồ Chí Minh', 3, 3, '2025-02-10', 'ACTIVE'),
('NV004', 'Phạm Ngọc Mai', '1992-04-17', 'FEMALE', '0900000004', 'Hà Nội', 1, 4, '2022-09-01', 'ACTIVE');

INSERT INTO accounts (employee_id, username, password_hash, role, account_status) VALUES
(1, 'admin_demo', '$2a$12$REPLACE_WITH_REAL_BCRYPT_HASH', 'ADMIN', 'ACTIVE'),
(1, 'hr_demo', '$2a$12$REPLACE_WITH_REAL_BCRYPT_HASH', 'HR', 'ACTIVE'),
(2, 'accountant_demo', '$2a$12$REPLACE_WITH_REAL_BCRYPT_HASH', 'ACCOUNTANT', 'ACTIVE');

INSERT INTO payroll_settings
(setting_id, standard_workdays_per_month, standard_hours_per_day, insurance_rate, income_tax_rate)
VALUES (1, 22.00, 8.00, 0.00000, 0.00000);

INSERT INTO attendance
(employee_id, work_date, time_in, time_out, work_hours, overtime_hours, day_type, attendance_status, approval_status, note)
VALUES
(1, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu mẫu'),
(1, '2026-10-02', '09:00:00', '19:00:00', 8.00, 2.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu mẫu có tăng ca'),
(2, '2026-10-01', '09:00:00', '17:00:00', 8.00, 0.00, 'WEEKDAY', 'PRESENT', 'CONFIRMED', 'Dữ liệu mẫu'),
(3, '2026-10-01', NULL, NULL, 0.00, 0.00, 'WEEKDAY', 'PAID_LEAVE', 'CONFIRMED', 'Nghỉ phép năm có lương');

INSERT INTO decisions
(decision_code, employee_id, decision_type, decision_date, reason, amount, approval_status)
VALUES
('QD-KT-2026-001', 1, 'REWARD', '2026-10-15', 'Hoàn thành tốt nhiệm vụ (mẫu)', 500000.00, 'APPROVED'),
('QD-KL-2026-001', 2, 'DISCIPLINE', '2026-10-16', 'Quyết định kỷ luật mẫu, chưa xác định phạt tiền', NULL, 'APPROVED');

INSERT INTO payroll_periods (payroll_year, payroll_month, period_status)
VALUES (2026, 10, 'OPEN');