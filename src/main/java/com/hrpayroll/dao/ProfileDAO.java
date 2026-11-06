package com.hrpayroll.dao;

import com.hrpayroll.model.AttendanceType;
import com.hrpayroll.model.DatabaseEnvironment;
import com.hrpayroll.model.DecisionType;
import com.hrpayroll.util.DBConnection;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu cá nhân (chỉ đọc) của một nhân viên.
 *
 * Mọi truy vấn đều lọc theo mã nhân viên ngay trong SQL; mã này lấy từ tài
 * khoản đang đăng nhập (accounts Primary, nơi xác thực), không nhận từ giao
 * diện. Chỉ trả về dữ liệu đã chính thức: khen thưởng/kỷ luật APPROVED và
 * phiếu lương của kỳ CLOSED với bản ghi FINALIZED.
 */
public final class ProfileDAO {

    private ProfileDAO() {
    }

    /** Mã nhân viên gắn với tài khoản; null nếu chưa gắn. */
    public static String findEmployeeCode(String username) throws SQLException {
        String sql = "SELECT e.employee_code FROM accounts a "
                   + "JOIN employees e ON e.employee_id = a.employee_id "
                   + "WHERE a.username = ?";

        try (Connection c = DBConnection.getConnection(DatabaseEnvironment.PRIMARY);
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Các cặp (nhãn, giá trị) của hồ sơ; rỗng nếu không có trong DB đang chọn. */
    public static List<String[]> profile(String code) throws SQLException {
        String sql =
            "SELECT e.employee_code, e.full_name, e.date_of_birth, e.gender, "
            + "e.phone, e.address, d.department_name, p.position_name, "
            + "e.hire_date, e.employment_status "
            + "FROM employees e "
            + "LEFT JOIN departments d ON d.department_id = e.department_id "
            + "LEFT JOIN positions p ON p.position_id = e.position_id "
            + "WHERE e.employee_code = ?";

        List<String[]> rows = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rows.add(new String[] {"Mã nhân viên", rs.getString(1)});
                    rows.add(new String[] {"Họ và tên", rs.getString(2)});
                    rows.add(new String[] {"Ngày sinh", date(rs, 3)});
                    rows.add(new String[] {"Giới tính", gender(rs.getString(4))});
                    rows.add(new String[] {"Điện thoại", text(rs.getString(5))});
                    rows.add(new String[] {"Địa chỉ", text(rs.getString(6))});
                    rows.add(new String[] {"Phòng ban", text(rs.getString(7))});
                    rows.add(new String[] {"Chức vụ", text(rs.getString(8))});
                    rows.add(new String[] {"Ngày vào làm", date(rs, 9)});
                    rows.add(new String[] {"Trạng thái",
                        "ACTIVE".equals(rs.getString(10)) ? "Đang làm việc" : "Đã nghỉ việc"});
                }
            }
        }
        return rows;
    }

    /** Chấm công, mới nhất trước. */
    public static List<Object[]> attendance(String code) throws SQLException {
        String sql =
            "SELECT a.work_date, a.time_in, a.time_out, a.attendance_status, "
            + "a.work_hours, a.overtime_hours, a.approval_status, a.note "
            + "FROM attendance a JOIN employees e ON e.employee_id = a.employee_id "
            + "WHERE e.employee_code = ? ORDER BY a.work_date DESC";

        List<Object[]> rows = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] {
                        date(rs, 1), time(rs.getTime(2)), time(rs.getTime(3)),
                        AttendanceType.valueOf(rs.getString(4)).getLabel(),
                        rs.getBigDecimal(5).stripTrailingZeros().toPlainString(),
                        rs.getBigDecimal(6).stripTrailingZeros().toPlainString(),
                        "CONFIRMED".equals(rs.getString(7)) ? "Đã xác nhận" : "Nháp",
                        text(rs.getString(8))
                    });
                }
            }
        }
        return rows;
    }

    /** Khen thưởng/kỷ luật đã duyệt, mới nhất trước. */
    public static List<Object[]> decisions(String code) throws SQLException {
        String sql =
            "SELECT d.decision_code, d.decision_type, d.decision_date, "
            + "d.reason, d.amount "
            + "FROM decisions d JOIN employees e ON e.employee_id = d.employee_id "
            + "WHERE e.employee_code = ? AND d.approval_status = 'APPROVED' "
            + "ORDER BY d.decision_date DESC, d.decision_code DESC";

        List<Object[]> rows = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] {
                        rs.getString(1),
                        DecisionType.valueOf(rs.getString(2)).getLabel(),
                        date(rs, 3), rs.getString(4), money(rs.getBigDecimal(5))
                    });
                }
            }
        }
        return rows;
    }

    /** Phiếu lương các kỳ đã chốt, kỳ mới nhất trước. */
    public static List<Object[]> payslips(String code) throws SQLException {
        String sql =
            "SELECT p.payroll_month, p.payroll_year, "
            + "d.base_salary_snapshot, d.allowance_snapshot, d.paid_workdays, "
            + "d.overtime_hours, d.salary_by_attendance, d.overtime_amount, "
            + "d.reward_amount, d.insurance_amount, d.income_tax_amount, "
            + "d.deduction_amount, d.gross_salary, d.net_salary "
            + "FROM payroll_details d "
            + "JOIN payroll_periods p ON p.payroll_period_id = d.payroll_period_id "
            + "JOIN employees e ON e.employee_id = d.employee_id "
            + "WHERE e.employee_code = ? AND p.period_status = 'CLOSED' "
            + "AND d.payroll_status = 'FINALIZED' "
            + "ORDER BY p.payroll_year DESC, p.payroll_month DESC";

        List<Object[]> rows = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] {
                        String.format("%02d/%d", rs.getInt(1), rs.getInt(2)),
                        money(rs.getBigDecimal(3)), money(rs.getBigDecimal(4)),
                        rs.getBigDecimal(5).stripTrailingZeros().toPlainString(),
                        rs.getBigDecimal(6).stripTrailingZeros().toPlainString(),
                        money(rs.getBigDecimal(7)), money(rs.getBigDecimal(8)),
                        money(rs.getBigDecimal(9)), money(rs.getBigDecimal(10)),
                        money(rs.getBigDecimal(11)), money(rs.getBigDecimal(12)),
                        money(rs.getBigDecimal(13)), money(rs.getBigDecimal(14))
                    });
                }
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------

    private static String date(ResultSet rs, int col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? "" : DateUtils.format(d.toLocalDate());
    }

    private static String time(Time t) {
        return t == null ? "" : t.toLocalTime().toString();
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    private static String money(BigDecimal v) {
        return v == null ? "" : MoneyUtils.format(v.longValue());
    }

    private static String gender(String g) {
        if ("MALE".equals(g)) return "Nam";
        if ("FEMALE".equals(g)) return "Nữ";
        if ("OTHER".equals(g)) return "Khác";
        return "Chưa xác định";
    }
}
