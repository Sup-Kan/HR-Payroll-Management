# Dữ liệu mẫu

Các file `.csv` **không có dòng tiêu đề** (để phpMyAdmin ở cấp bảng không nhập nhầm dòng tiêu đề thành dữ liệu). `\N` nghĩa là NULL. Mã hóa UTF-8, xuống dòng LF.

Thứ tự nhập và cột của từng file (điền vào ô "Column names" của phpMyAdmin):

| Thứ tự | File | Bảng | Column names |
|---:|---|---|---|
| 1 | `departments.csv` | departments | `department_code,department_name,description` |
| 2 | `positions.csv` | positions | `position_code,position_name,base_salary,allowance_amount,description,department_id` |
| 3 | `employees.csv` | employees | `employee_code,full_name,date_of_birth,gender,phone,address,department_id,position_id,hire_date,employment_status` |
| 4 | `accounts.sql` | accounts | (file SQL, không cần điền) |
| 5 | `payroll_settings.csv` | payroll_settings | `setting_id,standard_workdays_per_month,standard_hours_per_day,insurance_rate,income_tax_rate` |
| 6 | `attendance.csv` | attendance | `employee_id,work_date,time_in,time_out,work_hours,overtime_hours,day_type,attendance_status,approval_status,note` |
| 7 | `decisions.csv` | decisions | `decision_code,employee_id,decision_type,decision_date,reason,amount,approval_status,created_by,approved_by,approved_at` |
| 8 | `payroll_periods.csv` | payroll_periods | `payroll_year,payroll_month,period_status,opened_by,opened_at` |
| 9 | `payroll_details.csv` | payroll_details | (file rỗng, chưa có dữ liệu mẫu, bỏ qua) |

Thêm/sửa/xóa dữ liệu: sửa trực tiếp file CSV tương ứng, giữ đúng thứ tự cột như bảng trên.
