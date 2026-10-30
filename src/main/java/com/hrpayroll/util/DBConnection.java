package com.hrpayroll.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Tạo kết nối JDBC từ file db.properties trên classpath.
 * Mỗi lần gọi getConnection() trả về một kết nối mới;
 * nơi gọi phải đóng bằng try-with-resources.
 */
public final class DBConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static Properties config;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = loadConfig();

        try {
            return DriverManager.getConnection(
                    props.getProperty("db.url").trim(),
                    props.getProperty("db.user").trim(),
                    props.getProperty("db.password", "")
            );
        } catch (SQLException e) {
            throw new SQLException(
                    "Không thể kết nối cơ sở dữ liệu. "
                    + "Hãy kiểm tra MariaDB/XAMPP đã chạy, database đã được "
                    + "tạo bằng schema.sql và thông tin trong db.properties.",
                    e
            );
        }
    }

    private static synchronized Properties loadConfig() throws SQLException {
        if (config != null) {
            return config;
        }

        try (InputStream in = DBConnection.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (in == null) {
                throw new SQLException(
                        "Không tìm thấy db.properties. Hãy sao chép "
                        + "db.properties.example thành db.properties "
                        + "trong src/main/resources rồi nhập thông tin kết nối."
                );
            }

            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));

            requireKey(props, "db.url");
            requireKey(props, "db.user");

            config = props;
            return config;

        } catch (IOException e) {
            throw new SQLException("Không đọc được db.properties.", e);
        }
    }

    private static void requireKey(Properties props, String key)
            throws SQLException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("db.properties thiếu giá trị: " + key);
        }
    }

    /** Chạy riêng để kiểm tra kết nối; có thể xóa sau khi có DAO. */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Ket noi thanh cong: "
                    + conn.getMetaData().getDatabaseProductName() + " "
                    + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Nguyen nhan: " + e.getCause().getMessage());
            }
        }
    }
}
