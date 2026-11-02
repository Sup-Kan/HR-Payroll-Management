package com.hrpayroll.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một bản ghi chấm công của một nhân viên trong một ngày.
 * Giờ làm và giờ tăng ca do AttendanceCalculator tính sẵn;
 * chỉ trạng thái xác nhận được thay đổi sau khi tạo.
 */
public final class AttendanceRecord {

    private final String employeeCode;
    private final String employeeName;
    private final LocalDate date;
    private final LocalTime checkIn;
    private final LocalTime checkOut;
    private final AttendanceType type;
    private final double workHours;
    private final double overtimeHours;
    private final String note;
    private boolean confirmed;

    public AttendanceRecord(
            String employeeCode,
            String employeeName,
            LocalDate date,
            LocalTime checkIn,
            LocalTime checkOut,
            AttendanceType type,
            double workHours,
            double overtimeHours,
            String note) {

        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.type = type;
        this.workHours = workHours;
        this.overtimeHours = overtimeHours;
        this.note = note == null ? "" : note;
        this.confirmed = false;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public LocalDate getDate() {
        return date;
    }

    /** Có thể null khi nghỉ phép hoặc vắng mặt. */
    public LocalTime getCheckIn() {
        return checkIn;
    }

    /** Có thể null khi nghỉ phép hoặc vắng mặt. */
    public LocalTime getCheckOut() {
        return checkOut;
    }

    public AttendanceType getType() {
        return type;
    }

    public double getWorkHours() {
        return workHours;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public String getNote() {
        return note;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void confirm() {
        this.confirmed = true;
    }
}
