package com.hrpayroll.util;

import com.hrpayroll.model.DatabaseEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

/** Kết nối tới DB được chọn trong phiên; DAO không cần viết riêng cho từng môi trường. */
public final class DBConnection {
    private static final String CONFIG_FILE = "db.properties";
    private static Properties config;
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        DatabaseEnvironment env = Session.getDatabaseEnvironment();
        if (env == null) throw new SQLException("Chưa chọn môi trường dữ liệu. Hãy đăng nhập và chọn môi trường trước.");
        return getConnection(env);
    }

    public static Connection getConnection(DatabaseEnvironment env) throws SQLException {
        if (env == null) throw new SQLException("Môi trường dữ liệu không hợp lệ.");
        Properties p = loadConfig();
        String host = p.getProperty("db.host", "localhost").trim();
        String port = p.getProperty("db.port", "3306").trim();
        String dbUser = required(p, "db.user");
        String dbPassword = p.getProperty("db.password", "").trim();
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + env.getDatabaseName() + "?useUnicode=true&characterEncoding=utf8";
        try { return DriverManager.getConnection(url, dbUser, dbPassword); }
        catch (SQLException e) {
            e.printStackTrace();

            String detail = e.getMessage();

            if (e.getCause() != null) {
                detail += "\nChi tiết: " + e.getCause().getMessage();
            }

            throw new SQLException(
                "Không thể kết nối " + env.getLabel()
                + " (" + env.getDatabaseName() + ")."
                + "\nNguyên nhân: " + detail,
                e
            );
        }
    }

    /** Kiểm tra mật khẩu mở môi trường dữ liệu gốc; đây là lớp chặn trong ứng dụng demo. */
    public static boolean verifyPrimaryAccess(char[] candidate) throws SQLException {
        if (candidate == null || candidate.length == 0) return false;
        String expected = required(loadConfig(), "db.primary.access.password");
        byte[] a = new String(candidate).getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        try { return MessageDigest.isEqual(a, b); }
        finally { Arrays.fill(a, (byte) 0); Arrays.fill(b, (byte) 0); }
    }

    private static synchronized Properties loadConfig() throws SQLException {
        if (config != null) return config;
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) throw new SQLException("Không tìm thấy db.properties. Sao chép db.properties.example thành db.properties.");
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            required(p, "db.user");
            required(p, "db.primary.access.password");
            config = p;
            return config;
        } catch (IOException e) { throw new SQLException("Không đọc được db.properties.", e); }
    }

    private static String required(Properties p, String key) throws SQLException {
        String value = p.getProperty(key);
        if (value == null || value.trim().isEmpty()) throw new SQLException("db.properties thiếu giá trị: " + key);
        return value.trim();
    }
}
