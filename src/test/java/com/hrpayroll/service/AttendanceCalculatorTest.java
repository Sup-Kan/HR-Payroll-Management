package com.hrpayroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hrpayroll.model.AttendanceType;
import java.time.LocalTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AttendanceCalculatorTest {

    private static final double EPS = 0.001;

    @Test
    void fullDayMinusLunch() {
        // 08:00-17:00 = 9h, trừ nghỉ trưa 1h = 8h
        assertEquals(8.0, AttendanceCalculator.workedHours(
            LocalTime.of(8, 0), LocalTime.of(17, 0)), EPS);
    }

    @Test
    void lunchOverlapIsPartial() {
        // 12:30-17:30: chỉ trùng 30 phút nghỉ trưa -> 4.5h
        assertEquals(4.5, AttendanceCalculator.workedHours(
            LocalTime.of(12, 30), LocalTime.of(17, 30)), EPS);
    }

    @Test
    void morningOnlyHasNoLunchDeduction() {
        assertEquals(4.0, AttendanceCalculator.workedHours(
            LocalTime.of(8, 0), LocalTime.of(12, 0)), EPS);
    }

    @Test
    void overtimeOnlyAboveEightHours() {
        assertEquals(0.0, AttendanceCalculator.overtimeHours(8.0), EPS);
        assertEquals(0.0, AttendanceCalculator.overtimeHours(6.5), EPS);
        assertEquals(2.5, AttendanceCalculator.overtimeHours(10.5), EPS);
    }

    @Test
    void workDaysByType() {
        assertEquals(1.0, AttendanceCalculator.workDays(AttendanceType.PRESENT, 8), EPS);
        assertEquals(0.5, AttendanceCalculator.workDays(AttendanceType.PRESENT, 4), EPS);
        // giờ vượt 8 không làm công > 1
        assertEquals(1.0, AttendanceCalculator.workDays(AttendanceType.PRESENT, 10), EPS);
        assertEquals(1.0, AttendanceCalculator.workDays(AttendanceType.PAID_LEAVE, 0), EPS);
        assertEquals(0.0, AttendanceCalculator.workDays(AttendanceType.UNPAID_LEAVE, 0), EPS);
        assertEquals(0.0, AttendanceCalculator.workDays(AttendanceType.ABSENT, 0), EPS);
    }

    /** Ghi lại hành vi hiện tại (lỗi đã biết): ca qua đêm cho 0 giờ. */
    @Test
    void overnightShiftCurrentlyYieldsZero() {
        assertEquals(0.0, AttendanceCalculator.workedHours(
            LocalTime.of(22, 0), LocalTime.of(6, 0)), EPS);
    }

    /** Hành vi mong muốn sau khi sửa (cần chốt quy tắc ca đêm + sửa CHECK trong DB). */
    @Disabled("Chờ chốt quy tắc ca qua đêm")
    @Test
    void overnightShiftShouldBeEightHours() {
        assertEquals(8.0, AttendanceCalculator.workedHours(
            LocalTime.of(22, 0), LocalTime.of(6, 0)), EPS);
    }
}
