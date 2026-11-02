# Tình trạng chuyển dữ liệu sang MariaDB

## Đã cập nhật trong bản này
- Đăng nhập truy vấn tài khoản từ `hr_payroll_primary.accounts`, kiểm tra `account_status` và BCrypt hash; không còn dùng `MockAuthService` để gán vai trò.
- Màn hình nhân viên nạp danh sách từ `employees` JOIN `departments` và `positions` của database đang chọn. Không còn hai dòng NV001/NV002 viết cứng.
- Bổ sung `EmployeeDAO.findAllRows()`.

## Cần hoàn thiện tiếp trước khi coi là triển khai đầy đủ
- CRUD nhân viên hiện tại trong `EmployeeFrame` cần được chuyển toàn bộ sang INSERT/UPDATE/DELETE có kiểm tra quyền; lần cập nhật này mới chuyển nguồn nạp danh sách.
- Chấm công, khen thưởng/kỷ luật, tiền lương và báo cáo vẫn còn logic/dữ liệu mẫu trong các frame/service tương ứng; chưa được phép coi là dữ liệu MariaDB hoàn chỉnh.
- Cần triển khai `PrimaryWriteGuard` ở tất cả thao tác ghi Primary, bao gồm xác nhận riêng của IT. Không được dựa riêng vào việc ẩn/khóa nút.
- Cần kiểm thử tích hợp trên cả `hr_payroll_primary` và `hr_payroll_sandbox` với từng vai trò.

## Đăng nhập
Việc xác thực được thực hiện trên Primary để dùng chung tài khoản giữa hai môi trường; sau đăng nhập người dùng chọn database làm việc. Vì vậy hai bảng `accounts` cần được đồng bộ khi quản trị tài khoản.
