package com.hrpayroll.dao;

import com.hrpayroll.model.DatabaseEnvironment;
import com.hrpayroll.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Trạng thái trực tuyến của tài khoản.
 *
 * Đăng nhập luôn xác thực trên Primary nên trạng thái cũng luôn ghi/đọc ở
 * Primary (accounts.last_seen_at), bất kể phiên đang dùng môi trường nào.
 * Mỗi phiên "chạm" định kỳ (heartbeat); tài khoản được coi là trực tuyến khi
 * lần chạm cuối cách hiện tại không quá ONLINE_WINDOW_SECONDS giây, nên nếu
 * ứng dụng bị tắt đột ngột thì tài khoản tự chuyển offline sau ít phút.
 * Thời gian so sánh bằng NOW() của MariaDB, không phụ thuộc đồng hồ máy khách.
 */
public final class PresenceDAO {

    public static final int HEARTBEAT_SECONDS = 30;
    public static final int ONLINE_WINDOW_SECONDS = 90;

    private PresenceDAO() {
    }

    /** Đánh dấu tài khoản đang trực tuyến ngay lúc này. */
    public static void touch(String username) throws SQLException {
        update("UPDATE accounts SET last_seen_at = NOW() WHERE username = ?",
               username);
    }

    /** Đánh dấu ngoại tuyến (đăng xuất/thoát). */
    public static void clear(String username) throws SQLException {
        update("UPDATE accounts SET last_seen_at = NULL WHERE username = ?",
               username);
    }

    /** Tên đăng nhập của các tài khoản đang trực tuyến. */
    public static Set<String> onlineUsernames() throws SQLException {
        String sql = "SELECT username FROM accounts WHERE last_seen_at >= "
                   + "NOW() - INTERVAL " + ONLINE_WINDOW_SECONDS + " SECOND";
        Set<String> result = new HashSet<>();

        try (Connection c = DBConnection.getConnection(DatabaseEnvironment.PRIMARY);
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    private static void update(String sql, String username) throws SQLException {
        if (username == null) {
            return;
        }
        try (Connection c = DBConnection.getConnection(DatabaseEnvironment.PRIMARY);
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
