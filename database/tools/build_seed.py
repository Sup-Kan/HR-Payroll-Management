#!/usr/bin/env python3
"""Sinh primary/seed.sql và sandbox/seed.sql từ data/*.csv và data/accounts.sql.

Cách dùng (cần Python 3, không cần cài thêm thư viện):
    python database/tools/build_seed.py

Sau khi sửa file trong database/data/, chạy lại script rồi import seed.sql của
từng môi trường bằng phpMyAdmin (sau khi đã import script cấu trúc
primary/hr_payroll_primary.sql hoặc sandbox/hr_payroll_sandbox.sql).

CSV không có dòng tiêu đề; \\N là NULL; thứ tự cột phải đúng như COLUMNS bên dưới
(và như database/data/README.md). Khóa ngoại trong dữ liệu dùng số thứ tự dòng
(ví dụ employee_id = 1 là dòng đầu của employees.csv), vì vậy KHÔNG đổi thứ tự
dòng của departments, positions, employees; chỉ thêm dòng mới ở cuối.
"""
import csv
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DB_DIR = os.path.dirname(HERE)
DATA_DIR = os.path.join(DB_DIR, 'data')

# Thứ tự nhập dữ liệu (mục 4.2): khóa ngoại cần bảng cha có trước.
ORDER = ['departments', 'positions', 'employees', 'accounts', 'payroll_settings',
         'attendance', 'decisions', 'payroll_periods', 'payroll_details']

COLUMNS = {
    'departments': ['department_code', 'department_name', 'description'],
    'positions': ['position_code', 'position_name', 'base_salary', 'allowance_amount', 'description'],
    'employees': ['employee_code', 'full_name', 'date_of_birth', 'gender', 'phone', 'address',
                  'department_id', 'position_id', 'hire_date', 'employment_status'],
    'payroll_settings': ['setting_id', 'standard_workdays_per_month', 'standard_hours_per_day',
                         'insurance_rate', 'income_tax_rate'],
    'attendance': ['employee_id', 'work_date', 'time_in', 'time_out', 'work_hours', 'overtime_hours',
                   'day_type', 'attendance_status', 'approval_status', 'note'],
    'decisions': ['decision_code', 'employee_id', 'decision_type', 'decision_date', 'reason', 'amount',
                  'approval_status', 'created_by', 'approved_by', 'approved_at'],
    'payroll_periods': ['payroll_year', 'payroll_month', 'period_status', 'opened_by', 'opened_at'],
    'payroll_details': ['payroll_period_id', 'employee_id', 'base_salary_snapshot', 'allowance_snapshot',
                        'paid_workdays', 'overtime_hours', 'overtime_weekday_hours',
                        'overtime_weekend_hours', 'overtime_holiday_hours', 'overtime_amount',
                        'reward_amount', 'deduction_amount', 'insurance_amount', 'insurance_rate',
                        'income_tax_rate', 'income_tax_amount',
                        'salary_by_attendance', 'gross_salary', 'net_salary', 'payroll_status',
                        'calculated_at'],
}

ENVIRONMENTS = [('primary', 'hr_payroll_primary'), ('sandbox', 'hr_payroll_sandbox')]


def literal(value):
    if value == r'\N':
        return 'NULL'
    return "'" + value.replace('\\', '\\\\').replace("'", "''") + "'"


def read_rows(table):
    path = os.path.join(DATA_DIR, table + '.csv')
    cols = COLUMNS[table]
    rows = []
    with open(path, encoding='utf-8', newline='') as f:
        for number, row in enumerate(csv.reader(f), 1):
            if not row:
                continue
            if len(row) != len(cols):
                sys.exit('LOI %s.csv dong %d: co %d cot, can %d (%s)'
                         % (table, number, len(row), len(cols), ', '.join(cols)))
            rows.append(row)
    return rows


def build_sql(db, blocks):
    out = ['-- ============================================================',
           '-- seed.sql - dữ liệu mẫu cho %s' % db,
           '-- FILE ĐƯỢC SINH TỰ ĐỘNG từ database/data/ bằng tools/build_seed.py.',
           '-- Đừng sửa tay; hãy sửa file trong data/ rồi chạy lại script.',
           '-- Import SAU script cấu trúc (các bảng phải rỗng), nếu không sẽ lỗi trùng khóa.',
           '-- ============================================================',
           '',
           'USE %s;' % db, '', 'SET NAMES utf8mb4;', '']
    out.extend(blocks)
    return '\n'.join(out) + '\n'


def main():
    blocks, counts = [], {}
    for table in ORDER:
        if table == 'accounts':
            path = os.path.join(DATA_DIR, 'accounts.sql')
            with open(path, encoding='utf-8') as f:
                blocks.append(f.read().rstrip() + '\n')
            counts[table] = 'accounts.sql'
            continue
        rows = read_rows(table)
        counts[table] = len(rows)
        if not rows:
            continue
        lines = ',\n'.join('(' + ', '.join(literal(v) for v in row) + ')' for row in rows)
        blocks.append('INSERT INTO %s (%s) VALUES\n%s;\n'
                      % (table, ', '.join(COLUMNS[table]), lines))
    for folder, db in ENVIRONMENTS:
        target = os.path.join(DB_DIR, folder, 'seed.sql')
        with open(target, 'w', encoding='utf-8', newline='\n') as f:
            f.write(build_sql(db, blocks))
        print('Da ghi', os.path.relpath(target, DB_DIR))
    print('So dong:', ', '.join('%s=%s' % (t, counts[t]) for t in ORDER))


if __name__ == '__main__':
    main()
