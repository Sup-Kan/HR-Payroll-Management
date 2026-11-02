package com.hrpayroll.service;

import com.hrpayroll.model.Role;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Xác thực mô phỏng trong giai đoạn demo; mọi mật khẩu không rỗng đều được chấp nhận. */
public final class MockAuthService {
    public static final String DEMO_HINT = "admin_demo, hr01..hr05, accountant01..accountant05, it01..it20, security01..security09, cleaner01..cleaner06, operations01..operations04";
    private static final Map<String, Role> DEMO_ACCOUNTS = new HashMap<>();
    static {
        DEMO_ACCOUNTS.put("admin_demo", Role.ADMIN);
        for (int i=1; i<=5; i++) DEMO_ACCOUNTS.put(String.format("hr%02d", i), Role.HR);
        for (int i=1; i<=5; i++) DEMO_ACCOUNTS.put(String.format("accountant%02d", i), Role.ACCOUNTANT);
        for (int i=1; i<=20; i++) DEMO_ACCOUNTS.put(String.format("it%02d", i), Role.EMPLOYEE);
        for (int i=1; i<=9; i++) DEMO_ACCOUNTS.put(String.format("security%02d", i), Role.EMPLOYEE);
        for (int i=1; i<=6; i++) DEMO_ACCOUNTS.put(String.format("cleaner%02d", i), Role.EMPLOYEE);
        for (int i=1; i<=4; i++) DEMO_ACCOUNTS.put(String.format("operations%02d", i), Role.EMPLOYEE);
    }
    private MockAuthService() {}
    public static Role authenticate(String username, char[] password) {
        if (username == null || password == null || password.length == 0) return null;
        return DEMO_ACCOUNTS.get(username.trim().toLowerCase(Locale.ROOT));
    }
    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
