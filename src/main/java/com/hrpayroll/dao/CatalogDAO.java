package com.hrpayroll.dao;

import com.hrpayroll.util.DBConnection;
import com.hrpayroll.util.MoneyUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Danh mục phụ: phòng ban (departments) và chức vụ (positions).
 *
 * Xóa chỉ được phép khi không còn nhân viên nào tham chiếu; nếu đang được
 * dùng thì phải chuyển sang "Ngừng hoạt động" (is_active = FALSE). Quy tắc
 * này nằm trong DAO để không phụ thuộc vào việc khóa nút ở giao diện.
 */
public final class CatalogDAO {

    private static final int DUPLICATE_KEY = 1062;

    private CatalogDAO() {
    }

    // ------------------------------ Phòng ban ------------------------------

    /** Mã, tên, mô tả, trạng thái, số nhân viên. */
    public static List<Object[]> departments() throws SQLException {
        String sql =
            "SELECT d.department_code, d.department_name, d.description, "
            + "d.is_active, COUNT(e.employee_id) "
            + "FROM departments d LEFT JOIN employees e "
            + "ON e.department_id = d.department_id "
            + "GROUP BY d.department_id ORDER BY d.department_code";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[] {
                    rs.getString(1), rs.getString(2), text(rs.getString(3)),
                    status(rs.getBoolean(4)), rs.getInt(5)
                });
            }
        }
        return rows;
    }

    public static void insertDepartment(String code, String name,
            String description, boolean active) throws SQLException {
        execute("INSERT INTO departments (department_code, department_name, "
              + "description, is_active) VALUES (?, ?, ?, ?)",
                code, name, empty(description), active);
    }

    public static void updateDepartment(String code, String name,
            String description, boolean active) throws SQLException {
        int n = execute("UPDATE departments SET department_name = ?, "
              + "description = ?, is_active = ? WHERE department_code = ?",
                name, empty(description), active, code);
        if (n == 0) throw notFound(code);
    }

    public static void deleteDepartment(String code) throws SQLException {
        delete("departments", "department_code", "department_id",
               "phòng ban", code);
    }

    // ------------------------------- Chức vụ -------------------------------

    public static final String SHARED = "(Dùng chung)";

    /** Mã, tên, phòng ban, lương cơ bản, phụ cấp, mô tả, trạng thái, số nhân viên. */
    public static List<Object[]> positions() throws SQLException {
        String sql =
            "SELECT p.position_code, p.position_name, d.department_name, "
            + "p.base_salary, p.allowance_amount, p.description, p.is_active, "
            + "COUNT(e.employee_id) "
            + "FROM positions p "
            + "LEFT JOIN departments d ON d.department_id = p.department_id "
            + "LEFT JOIN employees e ON e.position_id = p.position_id "
            + "GROUP BY p.position_id ORDER BY p.position_code";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[] {
                    rs.getString(1), rs.getString(2),
                    rs.getString(3) == null ? SHARED : rs.getString(3),
                    MoneyUtils.format(rs.getBigDecimal(4).longValue()),
                    MoneyUtils.format(rs.getBigDecimal(5).longValue()),
                    text(rs.getString(6)), status(rs.getBoolean(7)),
                    rs.getInt(8)
                });
            }
        }
        return rows;
    }

    public static void insertPosition(String code, String name, String department,
            long baseSalary, long allowance, String description, boolean active)
            throws SQLException {
        execute("INSERT INTO positions (position_code, position_name, "
              + "base_salary, allowance_amount, description, is_active, department_id) "
              + "VALUES (?, ?, ?, ?, ?, ?, "
              + "(SELECT department_id FROM departments WHERE department_name = ?))",
                code, name, baseSalary, allowance, empty(description), active,
                departmentOrNull(department));
    }

    public static void updatePosition(String code, String name, String department,
            long baseSalary, long allowance, String description, boolean active)
            throws SQLException {
        int n = execute("UPDATE positions SET position_name = ?, base_salary = ?, "
              + "allowance_amount = ?, description = ?, is_active = ?, "
              + "department_id = (SELECT department_id FROM departments WHERE department_name = ?) "
              + "WHERE position_code = ?",
                name, baseSalary, allowance, empty(description), active,
                departmentOrNull(department), code);
        if (n == 0) throw notFound(code);
    }

    public static void deletePosition(String code) throws SQLException {
        delete("positions", "position_code", "position_id", "chức vụ", code);
    }

    // ------------------------------------------------------------------

    private static void delete(String table, String codeCol, String idCol,
            String label, String code) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM employees e JOIN " + table
            + " t ON t." + idCol + " = e." + idCol + " WHERE t." + codeCol + " = ?";
        String deleteSql = "DELETE FROM " + table + " WHERE " + codeCol + " = ?";

        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(countSql)) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new SQLException("Không thể xóa " + label + " "
                            + code + " vì đang có " + rs.getInt(1)
                            + " nhân viên thuộc " + label
                            + " này. Hãy chuyển sang \"Ngừng hoạt động\".");
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(deleteSql)) {
                ps.setString(1, code);
                if (ps.executeUpdate() == 0) throw notFound(code);
            }
        }
    }

    private static int execute(String sql, Object... params) throws SQLException {
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_KEY) {
                throw new SQLException("Mã hoặc tên đã tồn tại.", e);
            }
            throw e;
        }
    }

    private static String departmentOrNull(String department) {
        return department == null || SHARED.equals(department) ? null : department;
    }

    private static SQLException notFound(String code) {
        return new SQLException("Không tìm thấy " + code + ". Hãy bấm Làm mới.");
    }

    private static String status(boolean active) {
        return active ? "Hoạt động" : "Ngừng hoạt động";
    }

    private static String text(String s) { return s == null ? "" : s; }
    private static String empty(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }
}
