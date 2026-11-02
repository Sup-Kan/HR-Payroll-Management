package com.hrpayroll.service;

import com.hrpayroll.model.Account;
import com.hrpayroll.model.Decision;
import com.hrpayroll.model.DecisionStatus;
import com.hrpayroll.model.DecisionType;
import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TẠM THỜI (Giai đoạn 3): dữ liệu mẫu chỉ đọc cho các màn hình Khen thưởng,
 * Tính lương, Báo cáo và Tài khoản. Mỗi màn dùng bản sao riêng nên sửa ở màn
 * này không ảnh hưởng màn khác. Xóa lớp này khi đã có DAO (Giai đoạn 4-7).
 * Chức vụ, lương cơ bản và phụ cấp lấy theo bảng positions trong sample_data.sql.
 */
public final class DemoData {

    public static final List<String> DEPARTMENTS = Arrays.asList(
        "Nhân sự", "Kế toán", "Công nghệ thông tin"
    );

    /** Nhân viên mẫu. */
    public static final class Employee {

        private final String code;
        private final String name;
        private final String department;
        private final String position;
        private final String gender;
        private final boolean active;
        private final long baseSalary;
        private final long allowance;

        private Employee(
                String code, String name, String department, String position,
                String gender, boolean active, long baseSalary, long allowance) {

            this.code = code;
            this.name = name;
            this.department = department;
            this.position = position;
            this.gender = gender;
            this.active = active;
            this.baseSalary = baseSalary;
            this.allowance = allowance;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDepartment() {
            return department;
        }

        public String getPosition() {
            return position;
        }

        public String getGender() {
            return gender;
        }

        public boolean isActive() {
            return active;
        }

        public long getBaseSalary() {
            return baseSalary;
        }

        public long getAllowance() {
            return allowance;
        }

        /** Dạng "NV001 - Nguyễn Văn An" cho ô chọn nhân viên. */
        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    /** Thống kê ngày công của một nhân viên trong kỳ mẫu. */
    public static final class AttendanceStat {

        private final String employeeCode;
        private final double presentDays;
        private final int paidLeaveDays;
        private final int unpaidLeaveDays;
        private final int absentDays;
        private final double overtimeHours;

        private AttendanceStat(
                String employeeCode, double presentDays, int paidLeaveDays,
                int unpaidLeaveDays, int absentDays, double overtimeHours) {

            this.employeeCode = employeeCode;
            this.presentDays = presentDays;
            this.paidLeaveDays = paidLeaveDays;
            this.unpaidLeaveDays = unpaidLeaveDays;
            this.absentDays = absentDays;
            this.overtimeHours = overtimeHours;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public double getPresentDays() {
            return presentDays;
        }

        public int getPaidLeaveDays() {
            return paidLeaveDays;
        }

        public int getUnpaidLeaveDays() {
            return unpaidLeaveDays;
        }

        public int getAbsentDays() {
            return absentDays;
        }

        public double getOvertimeHours() {
            return overtimeHours;
        }
    }

    // Mã, công làm việc, phép năm, nghỉ không lương, vắng,
    // giờ tăng ca (thường, cuối tuần, lễ), thưởng, phạt,
    // công chưa xác nhận, quyết định chờ duyệt.
    private static final Object[][] MONTH = {
        {"NV001", 21.0, 1, 0, 0, 2.0, 0.0, 0.0, 500000L, 0L, 0, 0},
        {"NV002", 20.5, 1, 0, 0, 0.0, 0.0, 0.0, 0L, 0L, 0, 0},
        {"NV003", 22.0, 0, 0, 0, 4.0, 8.0, 0.0, 0L, 0L, 0, 0},
        {"NV004", 20.0, 0, 2, 0, 0.0, 0.0, 0.0, 0L, 0L, 0, 1},
        {"NV005", 22.0, 0, 0, 0, 0.0, 0.0, 8.0, 0L, 0L, 0, 0},
        {"NV006", 21.0, 0, 0, 1, 0.0, 0.0, 0.0, 0L, 200000L, 0, 0}
    };

    private DemoData() {
    }

    /** Kỳ có dữ liệu mẫu: tháng hiện tại. */
    public static YearMonth samplePeriod() {
        return YearMonth.now();
    }

    public static List<Employee> employees() {
        List<Employee> list = new ArrayList<>();
        list.add(new Employee("NV001", "Nguyễn Văn An", "Nhân sự",
            "Nhân viên nhân sự", "Nam", true, 9000000L, 500000L));
        list.add(new Employee("NV002", "Trần Thị Bình", "Kế toán",
            "Kế toán viên", "Nữ", true, 10000000L, 700000L));
        list.add(new Employee("NV003", "Lê Quốc Bình", "Công nghệ thông tin",
            "Nhân viên IT", "Nam", true, 12000000L, 1000000L));
        list.add(new Employee("NV004", "Phạm Ngọc Mai", "Nhân sự",
            "Trưởng phòng nhân sự", "Nữ", true, 16000000L, 2000000L));
        list.add(new Employee("NV005", "Hoàng Minh Đức", "Công nghệ thông tin",
            "Nhân viên IT", "Nam", true, 12000000L, 1000000L));
        list.add(new Employee("NV006", "Vũ Thu Hà", "Kế toán",
            "Kế toán viên", "Nữ", true, 10000000L, 700000L));
        list.add(new Employee("NV007", "Đặng Văn Hùng", "Công nghệ thông tin",
            "Nhân viên IT", "Nam", false, 12000000L, 1000000L));
        return list;
    }

    public static List<Employee> activeEmployees() {
        List<Employee> list = new ArrayList<>();

        for (Employee employee : employees()) {
            if (employee.isActive()) {
                list.add(employee);
            }
        }

        return list;
    }

    public static Employee findEmployee(String code) {
        for (Employee employee : employees()) {
            if (employee.getCode().equals(code)) {
                return employee;
            }
        }

        return null;
    }

    /** Đầu vào tính lương của các nhân viên đang làm việc trong kỳ mẫu. */
    public static List<PayrollInput> payrollInputs() {
        List<PayrollInput> list = new ArrayList<>();

        for (Object[] row : MONTH) {
            Employee employee = findEmployee((String) row[0]);

            double present = (Double) row[1];
            int paidLeave = (Integer) row[2];

            list.add(new PayrollInput(
                employee.getCode(),
                employee.getName(),
                employee.getPosition(),
                employee.getBaseSalary(),
                employee.getAllowance(),
                BigDecimal.valueOf(present + paidLeave),
                BigDecimal.valueOf((Double) row[5]),
                BigDecimal.valueOf((Double) row[6]),
                BigDecimal.valueOf((Double) row[7]),
                (Long) row[8],
                (Long) row[9],
                (Integer) row[10],
                (Integer) row[11]
            ));
        }

        return list;
    }

    public static List<AttendanceStat> attendanceStats() {
        List<AttendanceStat> list = new ArrayList<>();

        for (Object[] row : MONTH) {
            double overtime = (Double) row[5] + (Double) row[6]
                + (Double) row[7];

            list.add(new AttendanceStat(
                (String) row[0],
                (Double) row[1],
                (Integer) row[2],
                (Integer) row[3],
                (Integer) row[4],
                overtime
            ));
        }

        return list;
    }

    public static List<Decision> decisions() {
        List<Decision> list = new ArrayList<>();

        Decision reward = new Decision(
            "QD-KT-2026-001", DecisionType.REWARD, "NV001", "Nguyễn Văn An",
            LocalDate.of(2026, 10, 15), "Hoàn thành tốt nhiệm vụ (mẫu)",
            500000L, DecisionStatus.PENDING
        );
        reward.approve("admin_demo", LocalDateTime.of(2026, 10, 16, 9, 0));
        list.add(reward);

        Decision discipline = new Decision(
            "QD-KL-2026-001", DecisionType.DISCIPLINE, "NV002", "Trần Thị Bình",
            LocalDate.of(2026, 10, 16),
            "Quyết định kỷ luật mẫu, chưa xác định phạt tiền",
            null, DecisionStatus.PENDING
        );
        discipline.approve("hr_demo", LocalDateTime.of(2026, 10, 17, 14, 30));
        list.add(discipline);

        list.add(new Decision(
            "QD-KT-2026-002", DecisionType.REWARD, "NV003", "Lê Quốc Bình",
            LocalDate.now(), "Hoàn thành dự án đúng hạn (mẫu)",
            1000000L, DecisionStatus.PENDING
        ));

        return list;
    }

    public static List<Account> accounts() {
        List<Account> list = new ArrayList<>();
        LocalDate created = LocalDate.of(2026, 10, 1);

        list.add(new Account(
            "admin_demo", "NV001", Role.ADMIN, false, created, null
        ));
        list.add(new Account(
            "hr_demo", "NV001", Role.HR, false, created, null
        ));
        list.add(new Account(
            "accountant_demo", "NV002", Role.ACCOUNTANT, false, created, null
        ));

        return list;
    }
}
