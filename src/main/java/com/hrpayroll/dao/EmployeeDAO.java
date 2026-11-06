package com.hrpayroll.dao;

import com.hrpayroll.model.Role;
import com.hrpayroll.util.DBConnection;
import com.hrpayroll.util.UsernameUtils;
import java.sql.*;
import java.util.*;

/** Truy vấn hồ sơ nhân viên từ database được chọn trong Session. */
public final class EmployeeDAO {
    private EmployeeDAO() {}
    public static List<Object[]> findAllRows() throws SQLException {
        String sql = "SELECT e.employee_code,e.full_name,e.date_of_birth,e.gender,e.phone,e.address,"
          + "d.department_name,p.position_name,e.hire_date,e.employment_status "
          + "FROM employees e LEFT JOIN departments d ON d.department_id=e.department_id "
          + "LEFT JOIN positions p ON p.position_id=e.position_id ORDER BY e.employee_code";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()) {
            while(rs.next()) rows.add(new Object[]{rs.getString(1),rs.getString(2),fmt(rs.getDate(3)),gender(rs.getString(4)),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),fmt(rs.getDate(9)),
                "RESIGNED".equals(rs.getString(10)) ? "Nghỉ việc" : "Đang làm việc"});
        }
        return rows;
    }
    // ------------------------------------------------------------------
    // Danh mục cho JComboBox
    // ------------------------------------------------------------------

    /** Tên các phòng ban đang hoạt động. */
    public static List<String> departmentNames() throws SQLException {
        return names("SELECT department_name FROM departments "
                   + "WHERE is_active = TRUE ORDER BY department_name");
    }

    /** Tên các chức vụ đang hoạt động. */
    public static List<String> positionNames() throws SQLException {
        return names("SELECT position_name FROM positions "
                   + "WHERE is_active = TRUE ORDER BY position_name");
    }

    private static List<String> names(String sql) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Thêm / sửa / nghỉ việc. Dữ liệu theo đúng thứ tự cột của bảng trên
    // giao diện: mã, tên, ngày sinh, giới tính, SĐT, địa chỉ, phòng ban,
    // chức vụ, ngày vào làm, trạng thái (ngày dạng dd/MM/yyyy).
    // ------------------------------------------------------------------

    private static final int DUPLICATE_KEY = 1062;
    private static final java.time.format.DateTimeFormatter DMY =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Mật khẩu mẫu của tài khoản tạo tự động; nên đổi sau lần đăng nhập đầu. */
    public static final String DEFAULT_PASSWORD = "HrDemo@2026";

    private static final int MAX_CODE_NUMBER = 999;

    /** Kết quả thêm nhân viên: mã được cấp và tài khoản tạo kèm (có thể null). */
    public static final class Created {
        public final String code;
        public final String username;
        public final Role role;

        Created(String code, String username, Role role) {
            this.code = code;
            this.username = username;
            this.role = role;
        }
    }

    /** Mã nhân viên kế tiếp: số lớn nhất hiện có + 1, dạng NV###. */
    public static String nextCode() throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return nextCode(c);
        }
    }

    private static String nextCode(Connection c) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(employee_code, 3) AS UNSIGNED)), 0) "
                   + "FROM employees";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            int next = rs.getInt(1) + 1;
            if (next > MAX_CODE_NUMBER) {
                throw new SQLException("Đã dùng hết mã nhân viên (NV999).");
            }
            return String.format("NV%03d", next);
        }
    }

    /** Tên các chức vụ đang hoạt động của một phòng ban (kèm chức vụ dùng chung). */
    public static List<String> positionNamesByDepartment(String departmentName) throws SQLException {
        String sql = "SELECT p.position_name FROM positions p "
                   + "LEFT JOIN departments d ON d.department_id = p.department_id "
                   + "WHERE p.is_active = TRUE "
                   + "AND (p.department_id IS NULL OR d.department_name = ?) "
                   + "ORDER BY p.position_name";
        List<String> list = new ArrayList<>();
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, departmentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1));
            }
        }
        return list;
    }

    /**
     * Thêm nhân viên mới. Mã nhân viên do hệ thống cấp (d[0] bị bỏ qua).
     * Nếu nhân viên đang làm việc, tạo luôn tài khoản: tên đăng nhập từ họ
     * tên không dấu, mật khẩu mẫu, vai trò theo phòng ban. Cả hai thao tác
     * nằm trong một giao dịch.
     */
    public static Created insert(Object[] d) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Created created = insert(c, d);
                c.commit();
                return created;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static Created insert(Connection c, Object[] d) throws SQLException {
        String sql = "INSERT INTO employees (employee_code, full_name, date_of_birth, gender, "
            + "phone, address, department_id, position_id, hire_date, employment_status, resigned_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, "
            + "(SELECT department_id FROM departments WHERE department_name = ?), "
            + "(SELECT position_id FROM positions WHERE position_name = ?), ?, ?, "
            + "IF(? = 'RESIGNED', GREATEST(CURDATE(), ?), NULL))";
        String status = status(d[9]);
        String code = null;
        int employeeId = 0;

        // Hai người cùng thêm một lúc có thể nhận cùng mã: thử lại với mã mới.
        for (int attempt = 0; attempt < 5 && code == null; attempt++) {
            String candidate = nextCode(c);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, candidate);
                ps.setString(2, str(d[1]));
                ps.setDate(3, date(d[2]));
                ps.setString(4, genderCode(d[3]));
                ps.setString(5, nullIfEmpty(d[4]));
                ps.setString(6, nullIfEmpty(d[5]));
                ps.setString(7, str(d[6]));
                ps.setString(8, str(d[7]));
                ps.setDate(9, date(d[8]));
                ps.setString(10, status);
                ps.setString(11, status);
                ps.setDate(12, date(d[8]));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    employeeId = keys.getInt(1);
                }
                code = candidate;
            } catch (SQLException e) {
                if (e.getErrorCode() != DUPLICATE_KEY || attempt == 4) {
                    throw friendly(e);
                }
            }
        }

        if (!"ACTIVE".equals(status)) {
            return new Created(code, null, null);
        }

        Role role = roleForDepartment(c, str(d[6]));
        String username = createAccount(c, employeeId, str(d[1]), role);
        return new Created(code, username, role);
    }

    /** Vai trò mặc định theo mã phòng ban; không có phòng ban thì là EMPLOYEE. */
    private static Role roleForDepartment(Connection c, String departmentName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT department_code FROM departments WHERE department_name = ?")) {
            ps.setString(1, departmentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Role.EMPLOYEE;
                switch (rs.getString(1)) {
                    case "HR":  return Role.HR;
                    case "ACC": return Role.ACCOUNTANT;
                    case "IT":  return Role.IT;
                    case "SEC": return Role.SECURITY;
                    case "FAC": return Role.CLEANER;
                    case "OPS": return Role.OPERATOR;
                    default:    return Role.EMPLOYEE;
                }
            }
        }
    }

    /** Tạo tài khoản cho nhân viên; trùng tên đăng nhập thì thêm số 2, 3, ... */
    private static String createAccount(Connection c, int employeeId, String fullName, Role role)
            throws SQLException {
        String base = UsernameUtils.fromName(fullName);
        String username = base;

        try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM accounts WHERE username = ?")) {
            for (int n = 2; ; n++) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) break;
                }
                if (n > 1000) throw new SQLException("Không tạo được tên đăng nhập duy nhất cho " + fullName);
                username = base + n;
            }
        }

        String hash = AccountDAO.hashPassword(DEFAULT_PASSWORD.toCharArray());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO accounts (employee_id, username, password_hash, role, account_status) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE')")) {
            ps.setInt(1, employeeId);
            ps.setString(2, username);
            ps.setString(3, hash);
            ps.setString(4, role.name());
            ps.executeUpdate();
        }
        return username;
    }

    /** Sửa nhân viên theo mã (mã không đổi). */
    public static void update(Object[] d) throws SQLException {
        String sql = "UPDATE employees SET full_name=?, date_of_birth=?, gender=?, phone=?, address=?, "
            + "department_id=(SELECT department_id FROM departments WHERE department_name=?), "
            + "position_id=(SELECT position_id FROM positions WHERE position_name=?), "
            + "hire_date=?, employment_status=?, "
            + "resigned_at=IF(? = 'RESIGNED', COALESCE(resigned_at, GREATEST(CURDATE(), hire_date)), NULL) "
            + "WHERE employee_code=?";
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            String status = status(d[9]);
            ps.setString(1, str(d[1]));
            ps.setDate(2, date(d[2]));
            ps.setString(3, genderCode(d[3]));
            ps.setString(4, nullIfEmpty(d[4]));
            ps.setString(5, nullIfEmpty(d[5]));
            ps.setString(6, str(d[6]));
            ps.setString(7, str(d[7]));
            ps.setDate(8, date(d[8]));
            ps.setString(9, status);
            ps.setString(10, status);
            ps.setString(11, str(d[0]));
            if (ps.executeUpdate() == 0 && !exists(c, str(d[0]))) {
                throw new SQLException("Không tìm thấy nhân viên " + d[0] + ". Hãy bấm Làm mới.");
            }
        } catch (SQLException e) {
            throw friendly(e);
        }
    }

    /** Chuyển sang nghỉ việc; hồ sơ vẫn được giữ lại. */
    public static void markResigned(String code) throws SQLException {
        String sql = "UPDATE employees SET employment_status='RESIGNED', "
            + "resigned_at=COALESCE(resigned_at, GREATEST(CURDATE(), hire_date)) "
            + "WHERE employee_code=?";
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, code);
            if (ps.executeUpdate() == 0 && !exists(c, code)) {
                throw new SQLException("Không tìm thấy nhân viên " + code + ". Hãy bấm Làm mới.");
            }
        }
    }

    // MySQL/MariaDB trả 0 dòng khi giá trị không đổi, nên phải kiểm tra tồn tại riêng.
    private static boolean exists(Connection c, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM employees WHERE employee_code=?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static SQLException friendly(SQLException e) {
        if (e.getErrorCode() == DUPLICATE_KEY) return new SQLException("Mã nhân viên đã tồn tại.", e);
        return e;
    }

    private static String str(Object o) { return o == null ? "" : o.toString().trim(); }
    private static String nullIfEmpty(Object o) { String s = str(o); return s.isEmpty() ? null : s; }
    private static java.sql.Date date(Object o) {
        String s = str(o);
        return s.isEmpty() ? null : java.sql.Date.valueOf(java.time.LocalDate.parse(s, DMY));
    }
    private static String status(Object o) { return "Nghỉ việc".equals(str(o)) ? "RESIGNED" : "ACTIVE"; }
    private static String genderCode(Object o) {
        String g = str(o);
        if ("Nam".equals(g)) return "MALE";
        if ("Nữ".equals(g)) return "FEMALE";
        return "OTHER";
    }

    private static String fmt(java.sql.Date d) {
        return d==null ? "" : new java.text.SimpleDateFormat("dd/MM/yyyy").format(d);
    }
    private static String gender(String g) { if("MALE".equals(g))return "Nam"; if("FEMALE".equals(g))return "Nữ"; return "Khác"; }
}
