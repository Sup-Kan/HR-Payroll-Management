package com.hrpayroll.dao;

import com.hrpayroll.model.AttendanceRecord;
import com.hrpayroll.model.AttendanceType;
import com.hrpayroll.service.AttendanceCalculator;
import com.hrpayroll.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cập bảng attendance của database đang chọn trong Session.
 *
 * - Một bản ghi được nhận diện bằng (mã nhân viên, ngày làm); schema có
 *   UNIQUE (employee_id, work_date).
 * - Chỉ bản ghi DRAFT mới được sửa/xóa; điều kiện này nằm ngay trong câu
 *   SQL, không chỉ dựa vào việc khóa nút ở giao diện.
 * - Cột day_type chỉ phân biệt WEEKDAY/WEEKEND theo thứ trong tuần.
 *   Ngày lễ (HOLIDAY) chưa được hỗ trợ vì chưa có bảng ngày lễ.
 */
public final class AttendanceDAO {

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    private static final String KEY_FILTER =
        "WHERE e.employee_code = ? AND a.work_date = ? "
        + "AND a.approval_status = 'DRAFT'";

    private AttendanceDAO() {
    }

    /** Toàn bộ bản ghi, sắp theo ngày rồi mã nhân viên. */
    public static List<AttendanceRecord> findAll() throws SQLException {
        String sql =
            "SELECT e.employee_code, e.full_name, a.work_date, "
            + "a.time_in, a.time_out, a.attendance_status, "
            + "a.work_hours, a.overtime_hours, a.note, a.approval_status "
            + "FROM attendance a JOIN employees e "
            + "ON e.employee_id = a.employee_id "
            + "ORDER BY a.work_date, e.employee_code";

        List<AttendanceRecord> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Time in = rs.getTime(4);
                Time out = rs.getTime(5);

                AttendanceRecord record = new AttendanceRecord(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getDate(3).toLocalDate(),
                    in == null ? null : in.toLocalTime(),
                    out == null ? null : out.toLocalTime(),
                    AttendanceType.valueOf(rs.getString(6)),
                    rs.getDouble(7),
                    rs.getDouble(8),
                    rs.getString(9)
                );

                if ("CONFIRMED".equals(rs.getString(10))) {
                    record.confirm();
                }

                list.add(record);
            }
        }

        return list;
    }

    /**
     * Kỳ (tháng/năm) có bản ghi chấm công gần nhất; nếu chưa có dữ liệu thì
     * trả về tháng hiện tại. Dùng làm kỳ mặc định cho các màn hình theo kỳ.
     */
    public static YearMonth latestPeriod() throws SQLException {
        String sql = "SELECT MAX(work_date) FROM attendance";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getDate(1) != null) {
                return YearMonth.from(rs.getDate(1).toLocalDate());
            }
        }

        return YearMonth.now();
    }

    /** Ngày có bản ghi chấm công gần nhất; hôm nay nếu chưa có dữ liệu. */
    public static LocalDate latestDateOrToday() {
        String sql = "SELECT MAX(work_date) FROM attendance";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getDate(1) != null) {
                return rs.getDate(1).toLocalDate();
            }
        } catch (SQLException ex) {
            // dùng ngày hôm nay
        }

        return LocalDate.now();
    }

    /** Như latestPeriod nhưng không ném lỗi: lỗi DB thì dùng tháng hiện tại. */
    public static YearMonth latestPeriodOrNow() {
        try {
            return latestPeriod();
        } catch (SQLException ex) {
            return YearMonth.now();
        }
    }

    /** Thêm bản ghi mới ở trạng thái DRAFT. */
    public static void insert(AttendanceRecord r) throws SQLException {
        String sql =
            "INSERT INTO attendance (employee_id, work_date, time_in, "
            + "time_out, work_hours, overtime_hours, day_type, "
            + "attendance_status, approval_status, note) "
            + "SELECT employee_id, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ? "
            + "FROM employees "
            + "WHERE employee_code = ? AND employment_status = 'ACTIVE'";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(r.getDate()));
            setTime(ps, 2, r.getCheckIn());
            setTime(ps, 3, r.getCheckOut());
            ps.setDouble(4, r.getWorkHours());
            ps.setDouble(5, r.getOvertimeHours());
            ps.setString(6, dayType(r.getDate()));
            ps.setString(7, r.getType().name());
            ps.setString(8, emptyToNull(r.getNote()));
            ps.setString(9, r.getEmployeeCode());

            if (ps.executeUpdate() == 0) {
                throw new SQLException(
                    "Mã nhân viên " + r.getEmployeeCode()
                    + " không tồn tại hoặc đã nghỉ việc."
                );
            }
        } catch (SQLException e) {
            throw friendly(e);
        }
    }

    /** Sửa bản ghi DRAFT; mã nhân viên không đổi, chỉ ngày/giờ/loại/ghi chú. */
    public static void update(AttendanceRecord oldRecord, AttendanceRecord n)
            throws SQLException {

        String sql =
            "UPDATE attendance a JOIN employees e "
            + "ON e.employee_id = a.employee_id "
            + "SET a.work_date = ?, a.time_in = ?, a.time_out = ?, "
            + "a.work_hours = ?, a.overtime_hours = ?, a.day_type = ?, "
            + "a.attendance_status = ?, a.note = ? "
            + KEY_FILTER;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(n.getDate()));
            setTime(ps, 2, n.getCheckIn());
            setTime(ps, 3, n.getCheckOut());
            ps.setDouble(4, n.getWorkHours());
            ps.setDouble(5, n.getOvertimeHours());
            ps.setString(6, dayType(n.getDate()));
            ps.setString(7, n.getType().name());
            ps.setString(8, emptyToNull(n.getNote()));
            ps.setString(9, oldRecord.getEmployeeCode());
            ps.setDate(10, Date.valueOf(oldRecord.getDate()));

            if (ps.executeUpdate() == 0) {
                throw staleRecord();
            }
        } catch (SQLException e) {
            throw friendly(e);
        }
    }

    /** Xóa bản ghi DRAFT. */
    public static void delete(AttendanceRecord r) throws SQLException {
        String sql =
            "DELETE a FROM attendance a JOIN employees e "
            + "ON e.employee_id = a.employee_id " + KEY_FILTER;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, r.getEmployeeCode());
            ps.setDate(2, Date.valueOf(r.getDate()));

            if (ps.executeUpdate() == 0) {
                throw staleRecord();
            }
        }
    }

    /**
     * Xác nhận công cho các bản ghi DRAFT; tất cả hoặc không gì cả.
     *
     * @return số bản ghi thực sự được xác nhận
     */
    public static int confirm(List<AttendanceRecord> records)
            throws SQLException {

        String sql =
            "UPDATE attendance a JOIN employees e "
            + "ON e.employee_id = a.employee_id "
            + "SET a.approval_status = 'CONFIRMED' " + KEY_FILTER;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int total = 0;

                for (AttendanceRecord r : records) {
                    ps.setString(1, r.getEmployeeCode());
                    ps.setDate(2, Date.valueOf(r.getDate()));
                    total += ps.executeUpdate();
                }

                c.commit();
                return total;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------

    private static String dayType(LocalDate date) {
        return AttendanceCalculator.isWeekend(date) ? "WEEKEND" : "WEEKDAY";
    }

    private static void setTime(PreparedStatement ps, int index, LocalTime t)
            throws SQLException {

        if (t == null) {
            ps.setNull(index, Types.TIME);
        } else {
            ps.setTime(index, Time.valueOf(t));
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static SQLException staleRecord() {
        return new SQLException(
            "Bản ghi không còn ở trạng thái nháp hoặc đã bị thay đổi/xóa. "
            + "Hãy bấm Làm mới để tải lại dữ liệu."
        );
    }

    private static SQLException friendly(SQLException e) {
        if (e.getErrorCode() == MYSQL_DUPLICATE_KEY) {
            return new SQLException(
                "Nhân viên này đã có bản ghi trong ngày đã chọn.", e
            );
        }
        return e;
    }
}
