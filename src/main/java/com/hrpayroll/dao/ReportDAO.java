package com.hrpayroll.dao;

import com.hrpayroll.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn số liệu cho màn hình báo cáo từ database đang chọn. */
public final class ReportDAO {

    /** Phòng ban dùng cho nhân viên chưa được gán phòng ban. */
    public static final String NO_DEPARTMENT = "(Chưa có phòng ban)";

    /** Một nhân viên trong báo cáo. */
    public static final class EmployeeRecord {

        private final String code;
        private final String name;
        private final String department;
        private final String gender;
        private final boolean active;

        private EmployeeRecord(String code, String name, String department,
                String gender, boolean active) {
            this.code = code;
            this.name = name;
            this.department = department;
            this.gender = gender;
            this.active = active;
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

        /** "Nam", "Nữ" hoặc "Khác". */
        public String getGender() {
            return gender;
        }

        public boolean isActive() {
            return active;
        }
    }

    /** Tổng hợp chấm công của một nhân viên trong một kỳ. */
    public static final class AttendanceStat {

        private final String employeeCode;
        private final double presentDays;
        private final int paidLeaveDays;
        private final int unpaidLeaveDays;
        private final int absentDays;
        private final double overtimeHours;

        private AttendanceStat(String employeeCode, double presentDays,
                int paidLeaveDays, int unpaidLeaveDays, int absentDays,
                double overtimeHours) {
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

        /** Công làm việc thực tế (8 giờ = 1 công, tối đa 1 công/ngày). */
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

    private ReportDAO() {
    }

    /** Tên các phòng ban theo thứ tự mã phòng ban trong DB. */
    public static List<String> findDepartments() throws SQLException {
        String sql = "SELECT department_name FROM departments "
            + "ORDER BY department_id";
        List<String> names = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }

        return names;
    }

    /** Toàn bộ nhân viên (cả đã nghỉ việc), theo mã nhân viên. */
    public static List<EmployeeRecord> findEmployees() throws SQLException {
        String sql =
            "SELECT e.employee_code, e.full_name, d.department_name, "
            + "e.gender, e.employment_status "
            + "FROM employees e "
            + "LEFT JOIN departments d ON d.department_id = e.department_id "
            + "ORDER BY e.employee_code";
        List<EmployeeRecord> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String department = rs.getString(3);

                list.add(new EmployeeRecord(
                    rs.getString(1),
                    rs.getString(2),
                    department == null ? NO_DEPARTMENT : department,
                    genderLabel(rs.getString(4)),
                    "ACTIVE".equals(rs.getString(5))
                ));
            }
        }

        return list;
    }

    /** Tổng hợp chấm công theo nhân viên trong kỳ (chỉ nhân viên có bản ghi). */
    public static List<AttendanceStat> attendanceStats(YearMonth period)
            throws SQLException {

        String sql =
            "SELECT e.employee_code, "
            + "SUM(CASE WHEN a.attendance_status = 'PRESENT' "
            + "    THEN LEAST(a.work_hours, 8) / 8 ELSE 0 END), "
            + "SUM(a.attendance_status = 'PAID_LEAVE'), "
            + "SUM(a.attendance_status = 'UNPAID_LEAVE'), "
            + "SUM(a.attendance_status = 'ABSENT'), "
            + "SUM(CASE WHEN a.attendance_status = 'PRESENT' "
            + "    THEN a.overtime_hours ELSE 0 END) "
            + "FROM attendance a JOIN employees e "
            + "ON e.employee_id = a.employee_id "
            + "WHERE a.work_date >= ? AND a.work_date < ? "
            + "GROUP BY e.employee_code ORDER BY e.employee_code";
        List<AttendanceStat> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(period.atDay(1)));
            ps.setDate(2, Date.valueOf(period.plusMonths(1).atDay(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AttendanceStat(
                        rs.getString(1),
                        rs.getDouble(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getDouble(6)
                    ));
                }
            }
        }

        return list;
    }

    private static String genderLabel(String gender) {
        if ("MALE".equals(gender)) {
            return "Nam";
        }

        return "FEMALE".equals(gender) ? "Nữ" : "Khác";
    }
}
