package com.hrpayroll.model;

/** Các chức năng (module) mở được từ Dashboard. */
public enum Feature {
    EMPLOYEE("Quản lý nhân viên", "Hồ sơ và thông tin nhân sự"),
    ATTENDANCE("Quản lý chấm công", "Ngày công và giờ làm"),
    REWARD_DISCIPLINE("Khen thưởng / Kỷ luật", "Theo dõi các quyết định"),
    PAYROLL("Quản lý tiền lương", "Theo dõi bảng lương"),
    REPORT("Báo cáo, thống kê", "Tổng hợp dữ liệu hệ thống"),
    ACCOUNT("Quản lý tài khoản", "Tài khoản người dùng");

    private final String title;
    private final String description;

    Feature(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
