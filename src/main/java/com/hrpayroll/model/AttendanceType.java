package com.hrpayroll.model;

/**
 * Loại chấm công trong một ngày. Tên hằng khớp với cột
 * attendance.attendance_status trong schema.sql.
 * "Đi muộn" không phải một loại: đi muộn chỉ làm giảm số giờ làm
 * và số công được tính lẻ theo tỷ lệ.
 */
public enum AttendanceType {
    PRESENT("Có mặt", true),
    PAID_LEAVE("Nghỉ phép năm", false),
    UNPAID_LEAVE("Nghỉ không lương", false),
    ABSENT("Vắng mặt", false);

    private final String label;
    private final boolean requiresTime;

    AttendanceType(String label, boolean requiresTime) {
        this.label = label;
        this.requiresTime = requiresTime;
    }

    public String getLabel() {
        return label;
    }

    /** Có bắt buộc nhập giờ vào/ra hay không. */
    public boolean requiresTime() {
        return requiresTime;
    }

    @Override
    public String toString() {
        return label;
    }
}
