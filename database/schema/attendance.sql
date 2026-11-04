-- Cấu trúc bảng attendance. Chạy theo thứ tự trong install.sql.
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
