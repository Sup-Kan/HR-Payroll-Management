-- ============================================================
-- reset.sql - Primary (hr_payroll_primary)
-- CẢNH BÁO: xóa toàn bộ bảng và dữ liệu trong DUY NHẤT database hr_payroll_primary.
-- Không động tới database của môi trường còn lại; không xóa database.
-- Sau khi reset có thể chạy lại install.sql.
-- ============================================================

USE hr_payroll_primary;

DROP TABLE IF EXISTS payroll_details;
DROP TABLE IF EXISTS payroll_periods;
DROP TABLE IF EXISTS payroll_settings;
DROP TABLE IF EXISTS decisions;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS positions;
DROP TABLE IF EXISTS departments;

SELECT 'DA XOA CAC BANG CUA hr_payroll_primary' AS thong_bao;
