package com.hrpayroll.dao;

import com.hrpayroll.model.Account;
import com.hrpayroll.model.Role;
import com.hrpayroll.model.DatabaseEnvironment;
import com.hrpayroll.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
// import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Đọc/ghi tài khoản trực tiếp trong database đang được chọn ở Session. */
public final class AccountDAO {
    private AccountDAO() {}

    public static final class EmployeeOption {
        private final int id;
        private final String code;
        private final String name;
        public EmployeeOption(int id, String code, String name) { this.id=id; this.code=code; this.name=name; }
        public int getId() { return id; }
        public String getCode() { return code; }
        @Override public String toString() { return code + " - " + name; }
    }

    /** Xác thực tài khoản thật từ database đang được chọn. */
    public static Role authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) return null;
        String sql = "SELECT password_hash, role, account_status FROM accounts WHERE username=?";
        try (Connection c=DBConnection.getConnection(DatabaseEnvironment.PRIMARY); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next() || !"ACTIVE".equals(rs.getString("account_status"))) return null;
                String hash=rs.getString("password_hash");
                if (hash == null || !at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password, hash).verified) return null;
                try (PreparedStatement update=c.prepareStatement("UPDATE accounts SET last_login_at=NOW() WHERE username=?")) {
                    update.setString(1, username.trim()); update.executeUpdate();
                }
                return Role.valueOf(rs.getString("role"));
            }
        }
    }

    public static List<Account> findAll() throws SQLException {
        String sql = "SELECT a.username, e.employee_code, a.role, a.account_status, "
                + "DATE(a.created_at) AS created_date, a.last_login_at "
                + "FROM accounts a LEFT JOIN employees e ON e.employee_id=a.employee_id "
                + "ORDER BY a.username";
        List<Account> result = new ArrayList<>();
        try (Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()) {
            while (rs.next()) {
                Date created=rs.getDate("created_date");
                Timestamp last=rs.getTimestamp("last_login_at");
                result.add(new Account(rs.getString("username"), rs.getString("employee_code"),
                    Role.valueOf(rs.getString("role")), "LOCKED".equals(rs.getString("account_status")),
                    created == null ? LocalDate.now() : created.toLocalDate(),
                    last == null ? null : last.toLocalDateTime()));
            }
        }
        return result;
    }

    public static List<EmployeeOption> findActiveEmployees() throws SQLException {
        String sql="SELECT employee_id, employee_code, full_name FROM employees WHERE employment_status='ACTIVE' ORDER BY employee_code";
        List<EmployeeOption> result=new ArrayList<>();
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()) {
            while(rs.next()) result.add(new EmployeeOption(rs.getInt("employee_id"),rs.getString("employee_code"),rs.getString("full_name")));
        }
        return result;
    }

    public static void create(String username, String employeeCode, Role role, char[] password) throws SQLException {
        String sql="INSERT INTO accounts(employee_id, username, password_hash, role, account_status) "
                + "VALUES ((SELECT employee_id FROM employees WHERE employee_code=?), ?, ?, ?, 'ACTIVE')";
        String hash=hashPassword(password);
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            if(employeeCode==null) ps.setNull(1, Types.INTEGER); else ps.setString(1,employeeCode);
            ps.setString(2,username); ps.setString(3,hash); ps.setString(4,role.name());
            if(ps.executeUpdate()!=1) throw new SQLException("Không tạo được tài khoản.");
        }
    }

    public static void updateRole(String username, Role role) throws SQLException {
        update("UPDATE accounts SET role=? WHERE username=?", role.name(), username);
    }
    public static void updateStatus(String username, boolean locked) throws SQLException {
        update("UPDATE accounts SET account_status=? WHERE username=?", locked ? "LOCKED" : "ACTIVE", username);
    }
    public static void updatePassword(String username, char[] password) throws SQLException {
        update("UPDATE accounts SET password_hash=? WHERE username=?", hashPassword(password), username);
    }
    private static void update(String sql, String value, String username) throws SQLException {
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1,value); ps.setString(2,username);
            if(ps.executeUpdate()!=1) throw new SQLException("Không tìm thấy tài khoản hoặc dữ liệu không thay đổi.");
        }
    }
    private static String hashPassword(char[] password) {
        return at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, password);
    }
}
