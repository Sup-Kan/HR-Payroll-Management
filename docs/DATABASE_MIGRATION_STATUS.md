# Tình trạng chuyển dữ liệu sang MariaDB

## Đã cập nhật trong bản này
- Đăng nhập truy vấn tài khoản từ `hr_payroll_primary.accounts`, kiểm tra `account_status` và BCrypt hash; không còn dùng `MockAuthService` để gán vai trò.
- Màn hình nhân viên nạp danh sách từ `employees` JOIN `departments` và `positions` của database đang chọn. Không còn hai dòng NV001/NV002 viết cứng.
- Bổ sung `EmployeeDAO.findAllRows()`.

- Chấm công: `AttendanceFrame` đọc/ghi bảng `attendance` qua `AttendanceDAO` (thêm, sửa, xóa, xác nhận công; chỉ bản ghi DRAFT mới sửa/xóa được, kiểm tra ngay trong SQL). Chưa hỗ trợ ca qua đêm và ngày lễ (`day_type` chỉ WEEKDAY/WEEKEND).
- Khen thưởng/Kỷ luật: `RewardDisciplineFrame` đọc/ghi bảng `decisions` qua `DecisionDAO` (lập, sửa, phê duyệt có ghi người duyệt, hủy). Trạng thái được kiểm tra ngay trong SQL; danh sách nhân viên lấy từ DB.
- Tính lương: `PayrollFrame` lấy đầu vào từ DB qua `PayrollDAO.loadInputs` (nhân viên đang làm, chức vụ, chấm công, quyết định đã duyệt của kỳ), lưu bảng lương vào `payroll_details`, chốt/mở lại kỳ trong `payroll_periods`. Kỳ đã lưu hiển thị lại đúng số đã lưu. Bảng `payroll_details` có thêm cột giờ tăng ca theo loại ngày và tỷ lệ bảo hiểm/thuế để truy vết.
- Báo cáo: `ReportFrame` dùng `ReportDAO` (nhân viên, phòng ban, ngày công) và `PayrollDAO` (tiền lương: số đã lưu nếu kỳ đã có bảng lương, nếu chưa thì tính dự kiến). `DemoData` đã bị xóa.
- Kỳ mặc định của Chấm công, Tính lương, Báo cáo là tháng có chấm công gần nhất (`AttendanceDAO.latestPeriod`), không phải tháng hiện tại.

## Cần hoàn thiện tiếp trước khi coi là triển khai đầy đủ
- CRUD nhân viên hiện tại trong `EmployeeFrame` cần được chuyển toàn bộ sang INSERT/UPDATE/DELETE có kiểm tra quyền; lần cập nhật này mới chuyển nguồn nạp danh sách.
- Các module chấm công, khen thưởng/kỷ luật, tiền lương và báo cáo đã đọc/ghi DB; vẫn cần kiểm thử tích hợp trên DB thật (chưa chạy tự động).
- Cần triển khai `PrimaryWriteGuard` ở tất cả thao tác ghi Primary, bao gồm xác nhận riêng của IT. Không được dựa riêng vào việc ẩn/khóa nút.
- Cần kiểm thử tích hợp trên cả `hr_payroll_primary` và `hr_payroll_sandbox` với từng vai trò.

## Đăng nhập
Việc xác thực được thực hiện trên Primary để dùng chung tài khoản giữa hai môi trường; sau đăng nhập người dùng chọn database làm việc. Vì vậy hai bảng `accounts` cần được đồng bộ khi quản trị tài khoản.
