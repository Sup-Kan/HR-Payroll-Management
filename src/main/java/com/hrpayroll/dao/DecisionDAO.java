package com.hrpayroll.dao;

import com.hrpayroll.model.Decision;
import com.hrpayroll.model.DecisionStatus;
import com.hrpayroll.model.DecisionType;
import com.hrpayroll.service.DecisionService;
import com.hrpayroll.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cập bảng decisions (khen thưởng/kỷ luật) của database đang chọn.
 *
 * - Chỉ quyết định DRAFT/PENDING mới được sửa hoặc phê duyệt; điều kiện nằm
 *   trong câu SQL nên vẫn đúng khi hai người cùng thao tác.
 * - Quyết định đã duyệt không sửa được, chỉ hủy được; đã hủy thì cố định.
 * - Số tiền để trống (NULL) nghĩa là không ảnh hưởng đến lương.
 */
public final class DecisionDAO {

    private static final int MYSQL_DUPLICATE_KEY = 1062;
    private static final String EDITABLE = "('DRAFT','PENDING')";

    private DecisionDAO() {
    }

    public static List<Decision> findAll() throws SQLException {
        String sql =
            "SELECT d.decision_code, d.decision_type, e.employee_code, "
            + "e.full_name, d.decision_date, d.reason, d.amount, "
            + "d.approval_status, ap.username, d.approved_at "
            + "FROM decisions d "
            + "JOIN employees e ON e.employee_id = d.employee_id "
            + "LEFT JOIN accounts ap ON ap.account_id = d.approved_by "
            + "ORDER BY d.decision_date, d.decision_code";

        List<Decision> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                BigDecimal amount = rs.getBigDecimal(7);
                DecisionStatus status =
                    DecisionStatus.valueOf(rs.getString(8));

                Decision decision = new Decision(
                    rs.getString(1),
                    DecisionType.valueOf(rs.getString(2)),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getDate(5).toLocalDate(),
                    rs.getString(6),
                    amount == null ? null : amount.longValue(),
                    status
                );

                if (status == DecisionStatus.APPROVED) {
                    String by = rs.getString(9);
                    Timestamp at = rs.getTimestamp(10);
                    decision.approve(
                        by == null ? "(không xác định)" : by,
                        at == null ? null : at.toLocalDateTime()
                    );
                }

                list.add(decision);
            }
        }

        return list;
    }

    /** Mã kế tiếp theo loại và năm, tính từ dữ liệu hiện có trong DB. */
    public static String nextCode(DecisionType type, int year)
            throws SQLException {

        String prefix = DecisionService.codePrefix(type, year);
        String sql = "SELECT decision_code FROM decisions "
            + "WHERE decision_code LIKE ?";
        int max = 0;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, prefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        max = Math.max(max, Integer.parseInt(
                            rs.getString(1).substring(prefix.length())));
                    } catch (NumberFormatException ex) {
                        // Mã sai dạng không ảnh hưởng việc đánh số.
                    }
                }
            }
        }

        return prefix + String.format("%03d", max + 1);
    }

    /** Thêm quyết định mới; createdBy là tên đăng nhập người lập (có thể null). */
    public static void insert(Decision d, String createdBy)
            throws SQLException {

        String sql =
            "INSERT INTO decisions (decision_code, employee_id, "
            + "decision_type, decision_date, reason, amount, "
            + "approval_status, created_by) "
            + "SELECT ?, employee_id, ?, ?, ?, ?, ?, "
            + "(SELECT account_id FROM accounts WHERE username = ?) "
            + "FROM employees WHERE employee_code = ?";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getCode());
            ps.setString(2, d.getType().name());
            ps.setDate(3, Date.valueOf(d.getDate()));
            ps.setString(4, d.getReason());
            setAmount(ps, 5, d.getAmount());
            ps.setString(6, d.getStatus().name());
            ps.setString(7, createdBy);
            ps.setString(8, d.getEmployeeCode());

            if (ps.executeUpdate() == 0) {
                throw new SQLException(
                    "Mã nhân viên " + d.getEmployeeCode()
                    + " không tồn tại."
                );
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_KEY) {
                throw new SQLException(
                    "Mã quyết định " + d.getCode() + " đã tồn tại "
                    + "(có thể người khác vừa lập). "
                    + "Hãy bấm Làm mới rồi thử lại.", e
                );
            }
            throw e;
        }
    }

    /** Sửa nhân viên, ngày lập, lý do, số tiền của quyết định chưa duyệt. */
    public static void update(Decision d) throws SQLException {
        String sql =
            "UPDATE decisions SET "
            + "employee_id = (SELECT employee_id FROM employees "
            + "WHERE employee_code = ?), "
            + "decision_date = ?, reason = ?, amount = ? "
            + "WHERE decision_code = ? AND approval_status IN " + EDITABLE;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getEmployeeCode());
            ps.setDate(2, Date.valueOf(d.getDate()));
            ps.setString(3, d.getReason());
            setAmount(ps, 4, d.getAmount());
            ps.setString(5, d.getCode());

            requireChanged(ps.executeUpdate());
        }
    }

    /** Phê duyệt quyết định chưa duyệt; ghi người duyệt và thời điểm duyệt. */
    public static void approve(String code, String approverUsername)
            throws SQLException {

        String sql =
            "UPDATE decisions SET approval_status = 'APPROVED', "
            + "approved_by = (SELECT account_id FROM accounts "
            + "WHERE username = ?), approved_at = NOW() "
            + "WHERE decision_code = ? AND approval_status IN " + EDITABLE;

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, approverUsername);
            ps.setString(2, code);
            requireChanged(ps.executeUpdate());
        }
    }

    /** Hủy quyết định (chưa duyệt hoặc đã duyệt); không khôi phục được. */
    public static void cancel(String code) throws SQLException {
        String sql =
            "UPDATE decisions SET approval_status = 'CANCELLED' "
            + "WHERE decision_code = ? AND approval_status <> 'CANCELLED'";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, code);
            requireChanged(ps.executeUpdate());
        }
    }

    private static void setAmount(PreparedStatement ps, int index, Long amount)
            throws SQLException {

        if (amount == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, BigDecimal.valueOf(amount));
        }
    }

    private static void requireChanged(int rows) throws SQLException {
        if (rows == 0) {
            throw new SQLException(
                "Quyết định đã thay đổi trạng thái hoặc không còn tồn tại. "
                + "Hãy bấm Làm mới để tải lại dữ liệu."
            );
        }
    }
}
