package com.hrpayroll.dao;

import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.PayrollResult;
import com.hrpayroll.model.PayrollSettings;
import com.hrpayroll.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu tính lương trong database đang chọn.
 *
 * - loadInputs: gom đầu vào của một kỳ từ nhân viên, chức vụ, chấm công và
 *   quyết định khen thưởng/kỷ luật (cùng quy tắc với AttendanceCalculator:
 *   8 giờ = 1 công, nghỉ phép năm = 1 công, tăng ca chỉ tính khi có mặt).
 * - save/finalize/reopen: lưu bảng lương vào payroll_details (kèm tỷ lệ và
 *   giờ tăng ca theo loại ngày để truy vết) và chốt/mở lại kỳ trong
 *   payroll_periods. Kỳ đã chốt (CLOSED) không ghi đè được.
 * - Khoản khấu trừ kỷ luật được lưu ở cột deduction_amount; bảo hiểm và thuế
 *   có cột riêng.
 */
public final class PayrollDAO {

    /** Bảng lương đã lưu của một kỳ. */
    public static final class SavedPeriod {

        private final boolean closed;
        private final PayrollSettings settings;
        private final List<PayrollResult> results;

        private SavedPeriod(boolean closed, PayrollSettings settings,
                List<PayrollResult> results) {
            this.closed = closed;
            this.settings = settings;
            this.results = results;
        }

        public boolean isClosed() {
            return closed;
        }

        /** Tỷ lệ bảo hiểm và thuế đã dùng khi lưu. */
        public PayrollSettings getSettings() {
            return settings;
        }

        public List<PayrollResult> getResults() {
            return results;
        }
    }

    private static final String INPUT_SQL =
        "SELECT e.employee_code, e.full_name, COALESCE(p.position_name, ''), "
        + "COALESCE(p.base_salary, 0), COALESCE(p.allowance_amount, 0), "
        + "COALESCE(att.paid_days, 0), COALESCE(att.ot_weekday, 0), "
        + "COALESCE(att.ot_weekend, 0), COALESCE(att.ot_holiday, 0), "
        + "COALESCE(att.unconfirmed, 0), COALESCE(dc.reward, 0), "
        + "COALESCE(dc.discipline, 0), COALESCE(dc.pending, 0) "
        + "FROM employees e "
        + "LEFT JOIN positions p ON p.position_id = e.position_id "
        + "LEFT JOIN (SELECT employee_id, "
        + "  SUM(CASE attendance_status WHEN 'PRESENT' "
        + "      THEN LEAST(work_hours, 8) / 8 "
        + "      WHEN 'PAID_LEAVE' THEN 1 ELSE 0 END) AS paid_days, "
        + "  SUM(CASE WHEN attendance_status = 'PRESENT' "
        + "      AND day_type = 'WEEKDAY' THEN overtime_hours ELSE 0 END) AS ot_weekday, "
        + "  SUM(CASE WHEN attendance_status = 'PRESENT' "
        + "      AND day_type = 'WEEKEND' THEN overtime_hours ELSE 0 END) AS ot_weekend, "
        + "  SUM(CASE WHEN attendance_status = 'PRESENT' "
        + "      AND day_type = 'HOLIDAY' THEN overtime_hours ELSE 0 END) AS ot_holiday, "
        + "  SUM(CASE WHEN approval_status = 'DRAFT' THEN 1 ELSE 0 END) AS unconfirmed "
        + "  FROM attendance WHERE work_date >= ? AND work_date < ? "
        + "  GROUP BY employee_id) att ON att.employee_id = e.employee_id "
        + "LEFT JOIN (SELECT employee_id, "
        + "  SUM(CASE WHEN approval_status = 'APPROVED' "
        + "      AND decision_type = 'REWARD' THEN COALESCE(amount, 0) ELSE 0 END) AS reward, "
        + "  SUM(CASE WHEN approval_status = 'APPROVED' "
        + "      AND decision_type = 'DISCIPLINE' THEN COALESCE(amount, 0) ELSE 0 END) AS discipline, "
        + "  SUM(CASE WHEN approval_status IN ('DRAFT', 'PENDING') THEN 1 ELSE 0 END) AS pending "
        + "  FROM decisions WHERE decision_date >= ? AND decision_date < ? "
        + "  GROUP BY employee_id) dc ON dc.employee_id = e.employee_id "
        + "WHERE e.employment_status = 'ACTIVE' OR att.employee_id IS NOT NULL "
        + "ORDER BY e.employee_code";

    private PayrollDAO() {
    }

    /** Tỷ lệ bảo hiểm và thuế mặc định trong payroll_settings (0 nếu chưa có). */
    public static PayrollSettings loadSettings() throws SQLException {
        String sql = "SELECT insurance_rate, income_tax_rate "
            + "FROM payroll_settings WHERE setting_id = 1";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new PayrollSettings(
                    rs.getBigDecimal(1), rs.getBigDecimal(2));
            }
        }

        return new PayrollSettings(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Đầu vào tính lương của kỳ, lấy từ dữ liệu hiện hành trong DB. */
    public static List<PayrollInput> loadInputs(YearMonth period)
            throws SQLException {

        Date start = Date.valueOf(period.atDay(1));
        Date end = Date.valueOf(period.plusMonths(1).atDay(1));
        List<PayrollInput> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(INPUT_SQL)) {

            ps.setDate(1, start);
            ps.setDate(2, end);
            ps.setDate(3, start);
            ps.setDate(4, end);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PayrollInput(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBigDecimal(4).longValue(),
                        rs.getBigDecimal(5).longValue(),
                        rs.getBigDecimal(6),
                        rs.getBigDecimal(7),
                        rs.getBigDecimal(8),
                        rs.getBigDecimal(9),
                        rs.getBigDecimal(11).longValue(),
                        rs.getBigDecimal(12).longValue(),
                        rs.getInt(10),
                        rs.getInt(13)
                    ));
                }
            }
        }

        return list;
    }

    /** Bảng lương đã lưu của kỳ, hoặc null nếu kỳ chưa có bảng lương. */
    public static SavedPeriod loadSaved(YearMonth period) throws SQLException {
        String sql =
            "SELECT p.period_status, e.employee_code, e.full_name, "
            + "COALESCE(pos.position_name, ''), d.base_salary_snapshot, "
            + "d.allowance_snapshot, d.paid_workdays, d.overtime_weekday_hours, "
            + "d.overtime_weekend_hours, d.overtime_holiday_hours, "
            + "d.reward_amount, d.deduction_amount, d.salary_by_attendance, "
            + "d.overtime_amount, d.gross_salary, d.insurance_amount, "
            + "d.income_tax_amount, d.net_salary, d.insurance_rate, "
            + "d.income_tax_rate "
            + "FROM payroll_periods p "
            + "JOIN payroll_details d ON d.payroll_period_id = p.payroll_period_id "
            + "JOIN employees e ON e.employee_id = d.employee_id "
            + "LEFT JOIN positions pos ON pos.position_id = e.position_id "
            + "WHERE p.payroll_year = ? AND p.payroll_month = ? "
            + "ORDER BY e.employee_code";

        List<PayrollResult> results = new ArrayList<>();
        boolean closed = false;
        PayrollSettings settings = null;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, period.getYear());
            ps.setInt(2, period.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    closed = "CLOSED".equals(rs.getString(1));

                    if (settings == null) {
                        settings = new PayrollSettings(
                            rs.getBigDecimal(19), rs.getBigDecimal(20));
                    }

                    PayrollInput input = new PayrollInput(
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5).longValue(),
                        rs.getBigDecimal(6).longValue(),
                        rs.getBigDecimal(7),
                        rs.getBigDecimal(8), rs.getBigDecimal(9),
                        rs.getBigDecimal(10),
                        rs.getBigDecimal(11).longValue(),
                        rs.getBigDecimal(12).longValue(),
                        0, 0
                    );

                    results.add(new PayrollResult(
                        input,
                        rs.getBigDecimal(13).longValue(),
                        rs.getBigDecimal(14).longValue(),
                        rs.getBigDecimal(15).longValue(),
                        rs.getBigDecimal(16).longValue(),
                        rs.getBigDecimal(17).longValue(),
                        rs.getBigDecimal(18).longValue()
                    ));
                }
            }
        }

        return results.isEmpty()
            ? null : new SavedPeriod(closed, settings, results);
    }

    /**
     * Lưu bảng lương (ghi đè bảng đã lưu trước đó của kỳ, nếu kỳ chưa chốt)
     * và lưu tỷ lệ bảo hiểm/thuế làm mặc định cho các kỳ sau.
     */
    public static void save(
            YearMonth period,
            List<PayrollResult> results,
            PayrollSettings settings,
            String username) throws SQLException {

        String insert =
            "INSERT INTO payroll_details (payroll_period_id, employee_id, "
            + "base_salary_snapshot, allowance_snapshot, paid_workdays, "
            + "overtime_hours, overtime_weekday_hours, overtime_weekend_hours, "
            + "overtime_holiday_hours, overtime_amount, reward_amount, "
            + "deduction_amount, insurance_amount, insurance_rate, "
            + "income_tax_rate, income_tax_amount, salary_by_attendance, "
            + "gross_salary, net_salary, payroll_status, calculated_at) "
            + "SELECT ?, employee_id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, 'CALCULATED', NOW() "
            + "FROM employees WHERE employee_code = ?";

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int periodId = openPeriodForUpdate(c, period, username, true);

                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM payroll_details "
                        + "WHERE payroll_period_id = ?")) {
                    del.setInt(1, periodId);
                    del.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(insert)) {
                    for (PayrollResult r : results) {
                        PayrollInput in = r.getInput();
                        int i = 1;
                        ps.setInt(i++, periodId);
                        ps.setBigDecimal(i++, money(in.getBaseSalary()));
                        ps.setBigDecimal(i++, money(in.getAllowance()));
                        ps.setBigDecimal(i++, in.getPaidWorkdays());
                        ps.setBigDecimal(i++, in.getOvertimeTotal());
                        ps.setBigDecimal(i++, in.getOvertimeWeekday());
                        ps.setBigDecimal(i++, in.getOvertimeWeekend());
                        ps.setBigDecimal(i++, in.getOvertimeHoliday());
                        ps.setBigDecimal(i++, money(r.getOvertimeAmount()));
                        ps.setBigDecimal(i++, money(in.getRewardAmount()));
                        ps.setBigDecimal(i++, money(in.getDisciplineAmount()));
                        ps.setBigDecimal(i++, money(r.getInsuranceAmount()));
                        ps.setBigDecimal(i++, settings.getInsuranceRate());
                        ps.setBigDecimal(i++, settings.getTaxRate());
                        ps.setBigDecimal(i++, money(r.getIncomeTaxAmount()));
                        ps.setBigDecimal(i++, money(r.getSalaryByAttendance()));
                        ps.setBigDecimal(i++, money(r.getGrossSalary()));
                        ps.setBigDecimal(i++, money(r.getNetSalary()));
                        ps.setString(i, in.getEmployeeCode());

                        if (ps.executeUpdate() == 0) {
                            throw new SQLException(
                                "Không tìm thấy nhân viên "
                                + in.getEmployeeCode() + " trong DB.");
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payroll_settings SET insurance_rate = ?, "
                        + "income_tax_rate = ? WHERE setting_id = 1")) {
                    ps.setBigDecimal(1, settings.getInsuranceRate());
                    ps.setBigDecimal(2, settings.getTaxRate());
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Chốt kỳ: bảng lương đã lưu chuyển sang FINALIZED, kỳ sang CLOSED. */
    public static void finalizePeriod(YearMonth period, String username)
            throws SQLException {

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int periodId = openPeriodForUpdate(c, period, username, false);

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payroll_details SET payroll_status = 'FINALIZED' "
                        + "WHERE payroll_period_id = ?")) {
                    ps.setInt(1, periodId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                            "Kỳ này chưa có bảng lương đã lưu để chốt.");
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payroll_periods SET period_status = 'CLOSED', "
                        + "closed_by = (SELECT account_id FROM accounts "
                        + "WHERE username = ?), closed_at = NOW() "
                        + "WHERE payroll_period_id = ?")) {
                    ps.setString(1, username);
                    ps.setInt(2, periodId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Mở lại kỳ đã chốt: kỳ về OPEN, bảng lương về CALCULATED. */
    public static void reopenPeriod(YearMonth period) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int periodId;

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT payroll_period_id FROM payroll_periods "
                        + "WHERE payroll_year = ? AND payroll_month = ? "
                        + "AND period_status = 'CLOSED' FOR UPDATE")) {
                    ps.setInt(1, period.getYear());
                    ps.setInt(2, period.getMonthValue());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Kỳ này không ở trạng thái đã chốt.");
                        }
                        periodId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payroll_details SET payroll_status = 'CALCULATED' "
                        + "WHERE payroll_period_id = ?")) {
                    ps.setInt(1, periodId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payroll_periods SET period_status = 'OPEN', "
                        + "closed_by = NULL, closed_at = NULL "
                        + "WHERE payroll_period_id = ?")) {
                    ps.setInt(1, periodId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Khóa dòng kỳ lương để cập nhật; tạo kỳ OPEN nếu chưa có (khi create).
     * Từ chối nếu kỳ đã chốt.
     */
    private static int openPeriodForUpdate(
            Connection c, YearMonth period, String username, boolean create)
            throws SQLException {

        if (create) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO payroll_periods (payroll_year, "
                    + "payroll_month, period_status, opened_by) VALUES "
                    + "(?, ?, 'OPEN', (SELECT account_id FROM accounts "
                    + "WHERE username = ?))")) {
                ps.setInt(1, period.getYear());
                ps.setInt(2, period.getMonthValue());
                ps.setString(3, username);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT payroll_period_id, period_status FROM payroll_periods "
                + "WHERE payroll_year = ? AND payroll_month = ? FOR UPDATE")) {
            ps.setInt(1, period.getYear());
            ps.setInt(2, period.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                        "Kỳ này chưa có bảng lương đã lưu.");
                }

                if ("CLOSED".equals(rs.getString(2))) {
                    throw new SQLException(
                        "Kỳ lương đã chốt, không thể ghi đè. "
                        + "Chỉ Admin được mở lại kỳ.");
                }

                return rs.getInt(1);
            }
        }
    }

    private static BigDecimal money(long value) {
        return BigDecimal.valueOf(value);
    }
}
