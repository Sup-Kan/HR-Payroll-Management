package com.hrpayroll.service;

import com.hrpayroll.model.Role;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * TẠM THỜI: xác thực giả lập cho Giai đoạn 3, chỉ nhận ba tài khoản mẫu
 * trong sample_data.sql và chấp nhận mọi mật khẩu không rỗng.
 * Sẽ thay bằng AuthService đọc bảng accounts và kiểm tra BCrypt khi nối DB.
 */
public final class MockAuthService {

    public static final String DEMO_HINT =
            "admin_demo, hr_demo, accountant_demo";

    private static final Map<String, Role> DEMO_ACCOUNTS = new HashMap<>();

    static {
        DEMO_ACCOUNTS.put("admin_demo", Role.ADMIN);
        DEMO_ACCOUNTS.put("hr_demo", Role.HR);
        DEMO_ACCOUNTS.put("accountant_demo", Role.ACCOUNTANT);
    }

    private MockAuthService() {
    }

    /** @return vai trò của tài khoản, hoặc null nếu không đăng nhập được */
    public static Role authenticate(String username, char[] password) {
        if (username == null || password == null || password.length == 0) {
            return null;
        }
        return DEMO_ACCOUNTS.get(username.trim().toLowerCase(Locale.ROOT));
    }

    /** Tên đăng nhập chuẩn hóa dùng cho phiên làm việc. */
    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
