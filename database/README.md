# Môi trường cơ sở dữ liệu

## Hai database

- `primary`: database dữ liệu gốc (A). `sandbox`: database thử nghiệm (B), cùng cấu trúc và cùng dữ liệu khởi tạo.
- Dữ liệu mẫu và cấu trúc chỉ có **một nguồn chung** cho cả hai môi trường: `schema/` (cấu trúc) và `data/` (dữ liệu). Muốn thêm/sửa/xóa dữ liệu mẫu, chỉ sửa file trong `data/`.
- Code DAO phải gọi `DBConnection.getConnection()` để tự động thao tác trên môi trường đã chọn trong phiên, không viết logic riêng cho primary hoặc sandbox.

## Cấu trúc thư mục

- `schema/`: 9 file `CREATE TABLE`, một file mỗi bảng.
- `data/`: dữ liệu mẫu: 8 file `.csv` và `accounts.sql` (chứa hash BCrypt). CSV không có dòng tiêu đề; tên cột từng file ghi ở `data/README.md`. `\N` nghĩa là NULL. `payroll_details.csv` rỗng.
- `primary/hr_payroll_primary.sql`, `sandbox/hr_payroll_sandbox.sql`: chỉ tạo database và 9 bảng (có `DROP TABLE` trước), **không có dữ liệu**. Phần `CREATE TABLE` trong hai file này sinh từ `schema/`; khi sửa cấu trúc bảng, sửa cả `schema/` và hai file này.
- `primary/seed.sql`, `sandbox/seed.sql`: dữ liệu mẫu dạng `INSERT`, sinh tự động từ `data/` bằng `tools/build_seed.py`.
- `tools/build_seed.py`: script sinh `seed.sql` (Python 3, không cần thư viện ngoài).
- `primary/install.sql`, `sandbox/install.sql`: tạo database, bảng, nhập dữ liệu từ `data/`, kiểm tra và báo kết quả (dòng lệnh).
- `primary/reset.sql`, `sandbox/reset.sql`: chỉ xóa các bảng trong đúng database của môi trường đó; không xóa database, không động tới môi trường còn lại.
- `legacy/`: bản script gộp cũ có cả dữ liệu `INSERT`, giữ để đối chiếu; có thể xóa sau khi đã khởi tạo thành công.

Thứ tự tạo bảng: departments, positions, employees, accounts, attendance, decisions, payroll_settings, payroll_periods, payroll_details.
Thứ tự nhập dữ liệu: departments, positions, employees, accounts, payroll_settings, attendance, decisions, payroll_periods, payroll_details.

## Cách khởi tạo bằng phpMyAdmin (khuyên dùng)

Nguồn dữ liệu duy nhất là `data/`. File `primary/seed.sql` và `sandbox/seed.sql` được **sinh tự động** từ đó bằng `tools/build_seed.py`, nên không sửa tay hai file này.

Khởi tạo (làm cho từng môi trường):
1. Tab **Import** ở trang chủ server (không chọn database): import `primary/hr_payroll_primary.sql` (tạo database và 9 bảng rỗng).
2. Import tiếp `primary/seed.sql` (nhập toàn bộ dữ liệu mẫu theo đúng thứ tự khóa ngoại).
3. Lặp lại với `sandbox/hr_payroll_sandbox.sql` rồi `sandbox/seed.sql`.

Khi sửa/thêm/xóa dữ liệu mẫu:
1. Sửa file trong `data/` (CSV mở được bằng Excel; giữ nguyên thứ tự cột, xem `data/README.md`; NULL viết là `\N`; lưu UTF-8).
2. Chạy `python database/tools/build_seed.py` (cần Python 3).
3. Import lại script cấu trúc rồi `seed.sql` của môi trường cần cập nhật. Script cấu trúc có `DROP TABLE` nên sẽ xóa dữ liệu hiện có.

Lưu ý khi sửa dữ liệu: khóa ngoại trong CSV dùng số thứ tự dòng (ví dụ `employee_id = 1` là dòng đầu của `employees.csv`). Chỉ thêm dòng mới ở cuối, không chèn giữa hoặc đổi thứ tự dòng của `departments`, `positions`, `employees`.

Cách thay thế: nhập từng CSV vào từng bảng bằng phpMyAdmin (tab Import của bảng, định dạng CSV, escape `"`, điền ô "Column names" theo `data/README.md`). Cách này nhiều bước hơn và phải kiểm tra NULL sau khi nhập.

## Cách khởi tạo bằng dòng lệnh (install.sql)

Chạy từ thư mục `database/` (các đường dẫn `schema/` và `data/` tính từ đó):

```
mysql --local-infile=1 -u root -p < primary/install.sql
mysql --local-infile=1 -u root -p < sandbox/install.sql
```

Làm lại từ đầu: chạy `reset.sql` của môi trường đó rồi chạy lại `install.sql`.

Yêu cầu:
- `install.sql` dùng `LOAD DATA LOCAL INFILE`, nên cần bật `local_infile` ở server (`SET GLOBAL local_infile = 1;`) và truyền `--local-infile=1` cho client. phpMyAdmin không chạy được `SOURCE`; nếu chỉ có phpMyAdmin, dùng mục phía trên.
- File CSV phải giữ ký tự xuống dòng LF (đã có `.gitattributes`). Nếu bị đổi sang CRLF, cột cuối mỗi dòng sẽ dính ký tự thừa.
- `install.sql` không xóa bảng sẵn có. Nếu bảng đã tồn tại, nó sẽ báo lỗi; chạy `reset.sql` trước.

Cuối `install.sql` có bước kiểm tra: số dòng từng bảng, 11 khóa ngoại, số giá trị NULL ở `decisions.amount` và tên tiếng Việt (UTF-8). Dòng cuối ghi "KHOI TAO THANH CONG" nếu mọi hạng mục đều OK.

## Tài khoản mẫu

Có 50 tài khoản: 1 Admin, 5 HR, 5 Kế toán, 20 IT, 9 Bảo vệ, 6 Lao công, 4 Vận hành. Tất cả tài khoản mẫu dùng mật khẩu `HrDemo@2026` trong hash BCrypt. Chỉ dùng cho demo; đổi mật khẩu trước khi dùng dữ liệu thật.

Danh sách username: `admin_demo`, `hr01`–`hr05`, `accountant01`–`accountant05`, `it01`–`it20`, `security01`–`security09`, `cleaner01`–`cleaner06`, `operations01`–`operations04`.

## Lưu ý bảo mật quan trọng

Mật khẩu `db.primary.access.password` là lớp chặn ở giao diện ứng dụng, phù hợp mô phỏng yêu cầu chọn môi trường trong đồ án. Nếu cần bảo vệ dữ liệu gốc thực sự, phải tạo user MariaDB riêng cho primary và cấp quyền riêng (sandbox user không được có quyền trên primary). Không dùng chung tài khoản MariaDB `root` cho môi trường vận hành thật.
