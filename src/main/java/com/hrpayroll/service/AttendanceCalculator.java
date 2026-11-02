package com.hrpayroll.service;

import com.hrpayroll.model.AttendanceRecord;
import com.hrpayroll.model.AttendanceType;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Quy tắc tính công đã chốt ở Giai đoạn 1:
 * - 8 giờ = 1 công, thiếu giờ tính công lẻ theo tỷ lệ;
 * - giờ làm trừ thời gian nghỉ trưa (12:00-13:00 là khung nghỉ trưa);
 * - giờ tăng ca = giờ làm vượt 8 giờ chuẩn;
 * - nghỉ phép năm tính 1 công có lương; nghỉ không lương và vắng mặt 0 công.
 *
 * Chưa xử lý: ngày lễ (sẽ dùng bảng ngày lễ trong DB khi nối DB).
 * Hệ số tăng ca (150%/200%/300%) áp dụng ở bước tính lương, không ở đây.
 */
public final class AttendanceCalculator {

    public static final double STANDARD_HOURS = 8.0;
    public static final int STANDARD_WORKDAYS_PER_MONTH = 22;
    public static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    private AttendanceCalculator() {
    }

    /** Giờ làm thực tế = (giờ ra − giờ vào) − phần trùng với giờ nghỉ trưa. */
    public static double workedHours(LocalTime checkIn, LocalTime checkOut) {
        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        long lunch = overlapMinutes(
                checkIn, checkOut, LUNCH_START, LUNCH_END
        );
        return round2(Math.max(0, minutes - lunch) / 60.0);
    }

    public static double overtimeHours(double workedHours) {
        return round2(Math.max(0, workedHours - STANDARD_HOURS));
    }

    /** Số công (chưa làm tròn) theo loại chấm công và giờ làm. */
    public static double workDays(AttendanceType type, double workedHours) {
        switch (type) {
            case PRESENT:
                return Math.min(workedHours, STANDARD_HOURS) / STANDARD_HOURS;
            case PAID_LEAVE:
                return 1.0;
            default:
                return 0.0;
        }
    }

    public static double workDays(AttendanceRecord record) {
        return workDays(record.getType(), record.getWorkHours());
    }

    /** Tạo bản ghi với giờ làm và giờ tăng ca đã tính sẵn. */
    public static AttendanceRecord createRecord(
            String employeeCode,
            String employeeName,
            LocalDate date,
            LocalTime checkIn,
            LocalTime checkOut,
            AttendanceType type,
            String note) {

        boolean hasTime = type.requiresTime();
        LocalTime in = hasTime ? checkIn : null;
        LocalTime out = hasTime ? checkOut : null;

        double worked = hasTime ? workedHours(in, out) : 0.0;

        return new AttendanceRecord(
                employeeCode, employeeName, date, in, out, type,
                worked, overtimeHours(worked), note
        );
    }

    /** Thứ 7 hoặc Chủ nhật (ngày lễ chưa được xét ở đây). */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String format2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static long overlapMinutes(
            LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2) {

        LocalTime start = start1.isAfter(start2) ? start1 : start2;
        LocalTime end = end1.isBefore(end2) ? end1 : end2;

        return end.isAfter(start)
                ? Duration.between(start, end).toMinutes()
                : 0;
    }
}
