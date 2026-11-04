package com.hrpayroll.service;

import com.hrpayroll.dao.ReportDAO;
import com.hrpayroll.model.PayrollResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tính số liệu thống kê cho màn hình báo cáo từ dữ liệu do ReportDAO và
 * PayrollDAO cung cấp; lớp này không truy cập DB trực tiếp.
 */
public final class ReportService {

    /** Giá trị "không lọc" của ô chọn phòng ban. */
    public static final String ALL = "Tất cả";

    private ReportService() {
    }

    /** Một dòng thống kê nhân viên theo phòng ban. */
    public static final class DepartmentStat {

        private final String department;
        private int total;
        private int active;
        private int resigned;
        private int male;
        private int female;

        private DepartmentStat(String department) {
            this.department = department;
        }

        public String getDepartment() {
            return department;
        }

        public int getTotal() {
            return total;
        }

        public int getActive() {
            return active;
        }

        public int getResigned() {
            return resigned;
        }

        public int getMale() {
            return male;
        }

        public int getFemale() {
            return female;
        }

        private void add(ReportDAO.EmployeeRecord employee) {
            total++;

            if (employee.isActive()) {
                active++;
            } else {
                resigned++;
            }

            if ("Nam".equals(employee.getGender())) {
                male++;
            } else if ("Nữ".equals(employee.getGender())) {
                female++;
            }
        }
    }

    /** Một dòng thống kê ngày công. */
    public static final class AttendanceRow {

        private final String code;
        private final String name;
        private final String department;
        private final double presentDays;
        private final int paidLeaveDays;
        private final int unpaidLeaveDays;
        private final int absentDays;
        private final double overtimeHours;

        private AttendanceRow(
                ReportDAO.EmployeeRecord employee,
                ReportDAO.AttendanceStat stat) {

            this.code = employee.getCode();
            this.name = employee.getName();
            this.department = employee.getDepartment();
            this.presentDays = stat.getPresentDays();
            this.paidLeaveDays = stat.getPaidLeaveDays();
            this.unpaidLeaveDays = stat.getUnpaidLeaveDays();
            this.absentDays = stat.getAbsentDays();
            this.overtimeHours = stat.getOvertimeHours();
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

        public double getTotalDays() {
            return presentDays + paidLeaveDays;
        }
    }

    /** Một dòng thống kê tiền lương. */
    public static final class PayrollRow {

        private final String code;
        private final String name;
        private final String department;
        private final long baseSalary;
        private final long allowance;
        private final long reward;
        private final long overtime;
        private final long deduction;
        private final long net;

        private PayrollRow(
                ReportDAO.EmployeeRecord employee, PayrollResult result) {
            this.code = employee.getCode();
            this.name = employee.getName();
            this.department = employee.getDepartment();
            this.baseSalary = result.getInput().getBaseSalary();
            this.allowance = result.getInput().getAllowance();
            this.reward = result.getInput().getRewardAmount();
            this.overtime = result.getOvertimeAmount();
            this.deduction = result.getTotalDeduction();
            this.net = result.getNetSalary();
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

        public long getBaseSalary() {
            return baseSalary;
        }

        public long getAllowance() {
            return allowance;
        }

        public long getReward() {
            return reward;
        }

        public long getOvertime() {
            return overtime;
        }

        public long getDeduction() {
            return deduction;
        }

        public long getNet() {
            return net;
        }
    }

    /** Thống kê nhân viên theo phòng ban, theo thứ tự của danh sách phòng ban. */
    public static List<DepartmentStat> employeeStats(
            List<ReportDAO.EmployeeRecord> employees,
            List<String> departments,
            String department,
            String keyword) {

        List<DepartmentStat> stats = new ArrayList<>();

        for (String name : departments) {
            if (!matchesDepartment(name, department)) {
                continue;
            }

            DepartmentStat stat = new DepartmentStat(name);

            for (ReportDAO.EmployeeRecord employee : employees) {
                if (name.equals(employee.getDepartment())
                        && matchesKeyword(employee, keyword)) {
                    stat.add(employee);
                }
            }

            if (stat.getTotal() > 0) {
                stats.add(stat);
            }
        }

        return stats;
    }

    public static List<AttendanceRow> attendanceStats(
            List<ReportDAO.EmployeeRecord> employees,
            List<ReportDAO.AttendanceStat> stats,
            String department,
            String keyword) {

        List<AttendanceRow> rows = new ArrayList<>();

        for (ReportDAO.EmployeeRecord employee : employees) {
            if (!matchesDepartment(employee.getDepartment(), department)
                    || !matchesKeyword(employee, keyword)) {
                continue;
            }

            for (ReportDAO.AttendanceStat stat : stats) {
                if (stat.getEmployeeCode().equals(employee.getCode())) {
                    rows.add(new AttendanceRow(employee, stat));
                }
            }
        }

        return rows;
    }

    public static List<PayrollRow> payrollStats(
            List<ReportDAO.EmployeeRecord> employees,
            List<PayrollResult> results,
            String department,
            String keyword) {

        List<PayrollRow> rows = new ArrayList<>();

        for (ReportDAO.EmployeeRecord employee : employees) {
            if (!matchesDepartment(employee.getDepartment(), department)
                    || !matchesKeyword(employee, keyword)) {
                continue;
            }

            for (PayrollResult result : results) {
                if (result.getEmployeeCode().equals(employee.getCode())) {
                    rows.add(new PayrollRow(employee, result));
                }
            }
        }

        return rows;
    }

    /** Tổng các cột tiền: lương cơ bản, phụ cấp, thưởng, tăng ca, khấu trừ, thực lĩnh. */
    public static long[] payrollTotals(List<PayrollRow> rows) {
        long[] totals = new long[6];

        for (PayrollRow row : rows) {
            totals[0] += row.getBaseSalary();
            totals[1] += row.getAllowance();
            totals[2] += row.getReward();
            totals[3] += row.getOvertime();
            totals[4] += row.getDeduction();
            totals[5] += row.getNet();
        }

        return totals;
    }

    private static boolean matchesDepartment(String actual, String filter) {
        return filter == null || ALL.equals(filter) || filter.equals(actual);
    }

    private static boolean matchesKeyword(
            ReportDAO.EmployeeRecord employee, String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String key = keyword.trim().toLowerCase(Locale.ROOT);

        return employee.getCode().toLowerCase(Locale.ROOT).contains(key)
            || employee.getName().toLowerCase(Locale.ROOT).contains(key);
    }
}
