-- ============================================================
-- install.sql - Sandbox (hr_payroll_sandbox)
-- Khởi tạo cấu trúc + dữ liệu mẫu theo thứ tự khóa ngoại.
-- CHẠY TỪ THƯ MỤC database/ (đường dẫn schema/ và data/ tính từ đó):
--   mysql --local-infile=1 -u root -p < sandbox/install.sql
-- Cần local_infile bật ở cả client và server (xem database/README.md).
-- Không xóa bảng sẵn có: nếu bảng đã tồn tại, hãy chạy reset.sql trước.
-- ============================================================

CREATE DATABASE IF NOT EXISTS hr_payroll_sandbox
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_payroll_sandbox;

SET NAMES utf8mb4;

-- 1. Cấu trúc bảng (4.1)
SOURCE schema/departments.sql;
SOURCE schema/positions.sql;
SOURCE schema/employees.sql;
SOURCE schema/accounts.sql;
SOURCE schema/attendance.sql;
SOURCE schema/decisions.sql;
SOURCE schema/payroll_settings.sql;
SOURCE schema/payroll_periods.sql;
SOURCE schema/payroll_details.sql;

-- 2. Dữ liệu mẫu (4.2)
LOAD DATA LOCAL INFILE 'data/departments.csv' INTO TABLE departments
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (department_code, department_name, description);

LOAD DATA LOCAL INFILE 'data/positions.csv' INTO TABLE positions
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (position_code, position_name, base_salary, allowance_amount, description, department_id);

LOAD DATA LOCAL INFILE 'data/employees.csv' INTO TABLE employees
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (employee_code, full_name, date_of_birth, gender, phone, address, department_id, position_id, hire_date, employment_status);

SOURCE data/accounts.sql;

LOAD DATA LOCAL INFILE 'data/payroll_settings.csv' INTO TABLE payroll_settings
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (setting_id, standard_workdays_per_month, standard_hours_per_day, insurance_rate, income_tax_rate);

LOAD DATA LOCAL INFILE 'data/attendance.csv' INTO TABLE attendance
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (employee_id, work_date, time_in, time_out, work_hours, overtime_hours, day_type, attendance_status, approval_status, note);

LOAD DATA LOCAL INFILE 'data/decisions.csv' INTO TABLE decisions
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (decision_code, employee_id, decision_type, decision_date, reason, amount, approval_status, created_by, approved_by, approved_at);

LOAD DATA LOCAL INFILE 'data/payroll_periods.csv' INTO TABLE payroll_periods
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (payroll_year, payroll_month, period_status, opened_by, opened_at);

LOAD DATA LOCAL INFILE 'data/payroll_details.csv' INTO TABLE payroll_details
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    (payroll_period_id, employee_id, base_salary_snapshot, allowance_snapshot, paid_workdays, overtime_hours, overtime_weekday_hours, overtime_weekend_hours, overtime_holiday_hours, overtime_amount, reward_amount, deduction_amount, insurance_amount, insurance_rate, income_tax_rate, income_tax_amount, salary_by_attendance, gross_salary, net_salary, payroll_status, calculated_at);

-- 3. Kiểm tra số dòng, khóa ngoại, NULL và UTF-8
DROP TEMPORARY TABLE IF EXISTS install_checks;
CREATE TEMPORARY TABLE install_checks AS
SELECT 'departments' AS hang_muc, COUNT(*) AS thuc_te, 6 AS ky_vong, IF(COUNT(*) = 6, 'OK', 'LOI') AS ket_qua FROM departments
UNION ALL
SELECT 'positions' AS hang_muc, COUNT(*) AS thuc_te, 10 AS ky_vong, IF(COUNT(*) = 10, 'OK', 'LOI') AS ket_qua FROM positions
UNION ALL
SELECT 'employees' AS hang_muc, COUNT(*) AS thuc_te, 50 AS ky_vong, IF(COUNT(*) = 50, 'OK', 'LOI') AS ket_qua FROM employees
UNION ALL
SELECT 'accounts' AS hang_muc, COUNT(*) AS thuc_te, 50 AS ky_vong, IF(COUNT(*) = 50, 'OK', 'LOI') AS ket_qua FROM accounts
UNION ALL
SELECT 'payroll_settings' AS hang_muc, COUNT(*) AS thuc_te, 1 AS ky_vong, IF(COUNT(*) = 1, 'OK', 'LOI') AS ket_qua FROM payroll_settings
UNION ALL
SELECT 'attendance' AS hang_muc, COUNT(*) AS thuc_te, 150 AS ky_vong, IF(COUNT(*) = 150, 'OK', 'LOI') AS ket_qua FROM attendance
UNION ALL
SELECT 'decisions' AS hang_muc, COUNT(*) AS thuc_te, 15 AS ky_vong, IF(COUNT(*) = 15, 'OK', 'LOI') AS ket_qua FROM decisions
UNION ALL
SELECT 'payroll_periods' AS hang_muc, COUNT(*) AS thuc_te, 1 AS ky_vong, IF(COUNT(*) = 1, 'OK', 'LOI') AS ket_qua FROM payroll_periods
UNION ALL
SELECT 'payroll_details' AS hang_muc, COUNT(*) AS thuc_te, 0 AS ky_vong, IF(COUNT(*) = 0, 'OK', 'LOI') AS ket_qua FROM payroll_details
UNION ALL
SELECT 'khoa_ngoai', COUNT(*), 11, IF(COUNT(*) = 11, 'OK', 'LOI') FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY'
UNION ALL
SELECT 'decisions_amount_null', COUNT(*), 7, IF(COUNT(*) = 7, 'OK', 'LOI') FROM decisions WHERE amount IS NULL
UNION ALL
SELECT 'utf8_ten_nhan_vien', COUNT(*), 1, IF(COUNT(*) = 1, 'OK', 'LOI') FROM employees WHERE employee_code = 'NV001' AND full_name = 'Nguyễn Minh An';

SELECT * FROM install_checks;

-- 4. Kết quả
SELECT IF(SUM(ket_qua <> 'OK') = 0,
          'KHOI TAO THANH CONG: moi hang muc deu OK',
          CONCAT('KHOI TAO CO LOI: ', SUM(ket_qua <> 'OK'), ' hang muc sai, xem bang tren')) AS thong_bao
FROM install_checks;
