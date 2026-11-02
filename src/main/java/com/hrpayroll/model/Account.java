package com.hrpayroll.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tài khoản người dùng. Mật khẩu không được giữ trong đối tượng này:
 * khi nối DB, mật khẩu được băm BCrypt và lưu ở accounts.password_hash.
 */
public final class Account {

    private final String username;
    private final LocalDate createdAt;
    private String employeeCode;
    private Role role;
    private boolean locked;
    private LocalDateTime lastLogin;

    public Account(
            String username,
            String employeeCode,
            Role role,
            boolean locked,
            LocalDate createdAt,
            LocalDateTime lastLogin) {

        this.username = username;
        this.employeeCode = employeeCode;
        this.role = role;
        this.locked = locked;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public String getUsername() {
        return username;
    }

    /** Có thể null: tài khoản không gắn với nhân viên nào. */
    public String getEmployeeCode() {
        return employeeCode;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    /** Có thể null: chưa đăng nhập lần nào. */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}
