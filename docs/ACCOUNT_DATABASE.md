# Quản lý tài khoản bằng MariaDB

Màn hình `AccountFrame` không còn lấy danh sách tài khoản từ `DemoData.accounts()`.

- `AccountDAO.findAll()` đọc tài khoản từ database đang chọn trong `Session`.
- `AccountDAO.findActiveEmployees()` tải danh sách nhân viên đang làm việc.
- Tạo tài khoản, đổi mật khẩu, đổi vai trò và khóa/mở khóa đều ghi vào bảng `accounts`.
- Mật khẩu được băm BCrypt trước khi lưu; không lưu mật khẩu thô.
- Nút **Làm mới** truy vấn lại dữ liệu từ MariaDB.

Các cột mà DAO sử dụng: `accounts.employee_id`, `username`, `password_hash`, `role`, `account_status`, `created_at`, `last_login_at`; bảng `employees` gồm `employee_id`, `employee_code`, `full_name`, `employment_status`.

Cần chọn môi trường Primary/Sandbox trước khi mở màn hình, vì `DBConnection` lấy database từ phiên đăng nhập hiện tại.
