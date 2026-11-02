package com.hrpayroll.service;

import com.hrpayroll.model.Account;
import com.hrpayroll.model.Role;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quy tắc quản lý tài khoản. Mỗi hàm trả về thông báo lỗi tiếng Việt,
 * hoặc null nếu hợp lệ.
 */
public final class AccountService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_.]{3,60}");
    private static final int MIN_PASSWORD = 8;
    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu. */
    private static final int MAX_PASSWORD = 72;

    private AccountService() {
    }

    public static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String validateUsername(String raw, List<Account> existing) {
        String username = normalizeUsername(raw);

        if (username.isEmpty()) {
            return "Tên đăng nhập không được để trống.";
        }

        if (!USERNAME.matcher(username).matches()) {
            return "Tên đăng nhập gồm 3 đến 60 ký tự: chữ thường, số, "
                + "dấu gạch dưới hoặc dấu chấm.";
        }

        for (Account account : existing) {
            if (account.getUsername().equals(username)) {
                return "Tên đăng nhập đã tồn tại.";
            }
        }

        return null;
    }

    public static String validatePassword(
            String password, String confirm, String username) {

        if (password == null || password.length() < MIN_PASSWORD) {
            return "Mật khẩu tối thiểu " + MIN_PASSWORD + " ký tự.";
        }

        if (password.length() > MAX_PASSWORD) {
            return "Mật khẩu tối đa " + MAX_PASSWORD + " ký tự.";
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            hasLetter |= Character.isLetter(c);
            hasDigit |= Character.isDigit(c);
        }

        if (!hasLetter || !hasDigit) {
            return "Mật khẩu phải gồm cả chữ và số.";
        }

        if (username != null
                && password.equalsIgnoreCase(username.trim())) {
            return "Mật khẩu không được trùng tên đăng nhập.";
        }

        if (!password.equals(confirm)) {
            return "Mật khẩu nhập lại không khớp.";
        }

        return null;
    }

    public static String checkRoleChange(
            List<Account> all,
            Account target,
            Role newRole,
            String currentUsername) {

        if (target.getRole() == newRole) {
            return "Vai trò mới trùng với vai trò hiện tại.";
        }

        if (target.getUsername().equals(currentUsername)) {
            return "Không thể đổi vai trò của chính tài khoản đang đăng nhập.";
        }

        if (isActiveAdmin(target) && activeAdmins(all) <= 1) {
            return "Phải còn ít nhất một tài khoản Admin đang hoạt động.";
        }

        return null;
    }

    /** Kiểm tra việc đổi trạng thái khóa; chỉ việc khóa mới bị hạn chế. */
    public static String checkLockToggle(
            List<Account> all, Account target, String currentUsername) {

        if (target.isLocked()) {
            return null;
        }

        if (target.getUsername().equals(currentUsername)) {
            return "Không thể khóa chính tài khoản đang đăng nhập.";
        }

        if (isActiveAdmin(target) && activeAdmins(all) <= 1) {
            return "Phải còn ít nhất một tài khoản Admin đang hoạt động.";
        }

        return null;
    }

    public static int activeAdmins(List<Account> all) {
        int count = 0;

        for (Account account : all) {
            if (isActiveAdmin(account)) {
                count++;
            }
        }

        return count;
    }

    private static boolean isActiveAdmin(Account account) {
        return account.getRole() == Role.ADMIN && !account.isLocked();
    }
}
