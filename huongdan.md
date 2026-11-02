# Hướng dẫn chạy HR-Payroll-Management

## 1. Chuẩn bị

- JDK 11 trở lên.
- NetBeans hoặc IDE Java có hỗ trợ Maven.
- XAMPP/MariaDB đang chạy.

## 2. Khởi tạo dữ liệu

Import hai script sau trong phpMyAdmin hoặc MariaDB client:

1. `database/primary/hr_payroll_primary.sql` — dữ liệu gốc.
2. `database/sandbox/hr_payroll_sandbox.sql` — bản sao thử nghiệm có cùng cấu trúc và dữ liệu ban đầu.

Hai script có lệnh xóa bảng cũ trong đúng database đích trước khi tạo lại. Chỉ import vào database mới hoặc sau khi đã sao lưu dữ liệu cần giữ.

## 3. Cấu hình kết nối

- Mở `src/main/resources/db.properties.example`.
- Sao chép thành `src/main/resources/db.properties`.
- Điền host, port, user và mật khẩu MariaDB.
- Đặt `db.primary.access.password` trong `db.properties`. Mật khẩu mẫu để mở dữ liệu gốc là `Primary@2026`; có thể thay đổi.
- Không commit `db.properties` lên GitHub.

## 4. Đăng nhập demo

Ứng dụng hiện vẫn dùng `MockAuthService`: mọi mật khẩu không rỗng được chấp nhận cho username hợp lệ. Sau đăng nhập, chọn môi trường dữ liệu. Môi trường Sandbox được chọn mặc định; Primary yêu cầu mật khẩu `db.primary.access.password`.

Tài khoản mẫu: `admin_demo`, `hr01`–`hr05`, `accountant01`–`accountant05`, `it01`–`it20`, `security01`–`security09`, `cleaner01`–`cleaner06`, `operations01`–`operations04`.

Mật khẩu mẫu được băm trong SQL: `HrDemo@2026`. Khi chạy ứng dụng demo, `MockAuthService` chỉ kiểm tra username và chấp nhận mật khẩu không rỗng; việc xác thực BCrypt từ bảng `accounts` sẽ được tích hợp ở giai đoạn AuthService/JDBC.

## 5. Lưu ý về trạng thái triển khai

Lựa chọn Primary/Sandbox và định tuyến kết nối JDBC đã được bổ sung. `DBConnection.getConnection()` tự dùng môi trường đã chọn, do đó các DAO có thể dùng chung một đoạn mã cho cả hai database. Các màn hình nghiệp vụ hiện tại vẫn còn dùng dữ liệu demo trong bộ nhớ; cần triển khai DAO và thay `DemoData` theo từng module để CRUD thực sự được lưu vào database đã chọn.

Mật khẩu mở Primary hiện là lớp kiểm soát ở giao diện ứng dụng, chưa thay thế quyền truy cập MariaDB. Muốn bảo vệ dữ liệu gốc thực sự, cần tạo user MariaDB riêng và cấp quyền phù hợp; không dùng tài khoản `root` trong môi trường vận hành thật.
