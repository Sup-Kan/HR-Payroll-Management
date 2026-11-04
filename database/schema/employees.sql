-- Cấu trúc bảng employees. Chạy theo thứ tự trong install.sql.
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
