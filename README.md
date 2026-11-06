# Human-Resource-and-Payroll-Management-System
A Java Swing desktop application for employee management, attendance tracking, payroll calculation, rewards and disciplinary records, using JDBC and MySQL/MariaDB.


## Môi trường dữ liệu Primary / Sandbox

Sau khi đăng nhập, ứng dụng yêu cầu chọn môi trường: **Dữ liệu gốc (Primary)** hoặc **Bản sao thử nghiệm (Sandbox)**. Sandbox là bản sao dữ liệu khởi tạo từ Primary; hai môi trường dùng chung mã nguồn và luồng nghiệp vụ. Mặc định chọn Sandbox để giảm nguy cơ thao tác nhầm dữ liệu gốc.

Xem `database/README.md` để khởi tạo hai database và danh sách 50 tài khoản demo.

Mật khẩu demo của tài khoản: `HrDemo@2026`. Mật khẩu mở Primary trong cấu hình mẫu: `Primary@2026` (được đặt tại `db.properties`).
