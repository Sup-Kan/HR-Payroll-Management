-- Cấu trúc bảng accounts. Chạy theo thứ tự trong install.sql.
CREATE TABLE accounts (
    account_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_id INT UNSIGNED NULL,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','IT','HR','ACCOUNTANT','EMPLOYEE','SECURITY','CLEANER','OPERATOR') NOT NULL,
    account_status ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    last_seen_at DATETIME NULL,
    CONSTRAINT fk_accounts_employee
      FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
      ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_accounts_role_status (role, account_status)
) ENGINE=InnoDB;
