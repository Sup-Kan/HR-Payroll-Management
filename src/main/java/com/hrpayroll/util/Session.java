package com.hrpayroll.util;

import com.hrpayroll.model.Feature;
import com.hrpayroll.model.Role;

/** Phiên đăng nhập hiện tại của ứng dụng (một người dùng tại một thời điểm). */
public final class Session {

    private static String username;
    private static Role role;

    private Session() {
    }

    public static void login(String user, Role userRole) {
        username = user;
        role = userRole;
    }

    public static void logout() {
        username = null;
        role = null;
    }

    public static boolean isLoggedIn() {
        return role != null;
    }

    public static String getUsername() {
        return username;
    }

    public static Role getRole() {
        return role;
    }

    public static boolean canAccess(Feature feature) {
        return role != null && role.canAccess(feature);
    }
}
