# Môi trường cơ sở dữ liệu

## Hai database

- `primary/hr_payroll_primary.sql`: database dữ liệu gốc (tương ứng A trong mô tả).
- `sandbox/hr_payroll_sandbox.sql`: database thử nghiệm, là bản sao dữ liệu khởi tạo từ primary tại cùng thời điểm (tương ứng B).

Hai script có cùng cấu trúc và cùng bộ dữ liệu ban đầu; chỉ khác tên database. Code DAO phải gọi `DBConnection.getConnection()` để tự động thao tác trên môi trường đã chọn trong phiên, không viết logic riêng cho primary hoặc sandbox.

## Cách khởi tạo

1. Mở phpMyAdmin hoặc MariaDB client.
2. Import `primary/hr_payroll_primary.sql`.
3. Import `sandbox/hr_payroll_sandbox.sql`.
4. Tạo `src/main/resources/db.properties` từ file `.example` và cấu hình thông tin MariaDB.
5. Chạy ứng dụng, đăng nhập, sau đó chọn môi trường.

**Cảnh báo:** mỗi script là script khởi tạo/reset, có lệnh `DROP TABLE`. Chỉ chạy khi database tương ứng chưa có dữ liệu cần giữ. Không chạy lại trên dữ liệu đang sử dụng nếu chưa sao lưu.

## Tài khoản mẫu

Có 50 tài khoản: 1 Admin, 5 HR, 5 Kế toán, 20 IT, 9 Bảo vệ, 6 Lao công, 4 Vận hành. Tất cả tài khoản mẫu dùng mật khẩu `HrDemo@2026` trong hash BCrypt. Chỉ dùng cho demo; đổi mật khẩu trước khi dùng dữ liệu thật.

Danh sách username: `admin_demo`, `hr01`–`hr05`, `accountant01`–`accountant05`, `it01`–`it20`, `security01`–`security09`, `cleaner01`–`cleaner06`, `operations01`–`operations04`.

## Lưu ý bảo mật quan trọng

Mật khẩu `db.primary.access.password` là lớp chặn ở giao diện ứng dụng, phù hợp mô phỏng yêu cầu chọn môi trường trong đồ án. Nếu cần bảo vệ dữ liệu gốc thực sự, phải tạo user MariaDB riêng cho primary và cấp quyền riêng (sandbox user không được có quyền trên primary). Không dùng chung tài khoản MariaDB `root` cho môi trường vận hành thật.
