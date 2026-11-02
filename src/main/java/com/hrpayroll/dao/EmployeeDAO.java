package com.hrpayroll.dao;

import com.hrpayroll.util.DBConnection;
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
    private static String fmt(java.sql.Date d) {
        return d==null ? "" : new java.text.SimpleDateFormat("dd/MM/yyyy").format(d);
    }
    private static String gender(String g) { if("MALE".equals(g))return "Nam"; if("FEMALE".equals(g))return "Nữ"; return "Khác"; }
}
