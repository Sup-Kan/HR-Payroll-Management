package com.hrpayroll.model;

import java.math.BigDecimal;

/**
 * Dữ liệu đầu vào để tính lương một nhân viên trong một kỳ:
 * công, giờ tăng ca theo loại ngày, thưởng và khấu trừ từ quyết định đã duyệt.
 */
public final class PayrollInput {

    private final String employeeCode;
    private final String employeeName;
    private final String position;
    private final long baseSalary;
    private final long allowance;
    private final BigDecimal paidWorkdays;
    private final BigDecimal overtimeWeekday;
    private final BigDecimal overtimeWeekend;
    private final BigDecimal overtimeHoliday;
    private final long rewardAmount;
    private final long disciplineAmount;
    private final int unconfirmedAttendance;
    private final int pendingDecisions;

    public PayrollInput(
            String employeeCode,
            String employeeName,
            String position,
            long baseSalary,
            long allowance,
            BigDecimal paidWorkdays,
            BigDecimal overtimeWeekday,
            BigDecimal overtimeWeekend,
            BigDecimal overtimeHoliday,
            long rewardAmount,
            long disciplineAmount,
            int unconfirmedAttendance,
            int pendingDecisions) {

        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.position = position;
        this.baseSalary = baseSalary;
        this.allowance = allowance;
        this.paidWorkdays = paidWorkdays;
        this.overtimeWeekday = overtimeWeekday;
        this.overtimeWeekend = overtimeWeekend;
        this.overtimeHoliday = overtimeHoliday;
        this.rewardAmount = rewardAmount;
        this.disciplineAmount = disciplineAmount;
        this.unconfirmedAttendance = unconfirmedAttendance;
        this.pendingDecisions = pendingDecisions;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getPosition() {
        return position;
    }

    public long getBaseSalary() {
        return baseSalary;
    }

    /** Phụ cấp cố định theo chức vụ. */
    public long getAllowance() {
        return allowance;
    }

    /** Số công tính lương (đã gồm nghỉ phép năm có lương). */
    public BigDecimal getPaidWorkdays() {
        return paidWorkdays;
    }

    public BigDecimal getOvertimeWeekday() {
        return overtimeWeekday;
    }

    public BigDecimal getOvertimeWeekend() {
        return overtimeWeekend;
    }

    public BigDecimal getOvertimeHoliday() {
        return overtimeHoliday;
    }

    public BigDecimal getOvertimeTotal() {
        return overtimeWeekday.add(overtimeWeekend).add(overtimeHoliday);
    }

    /** Tổng tiền thưởng từ các quyết định khen thưởng đã duyệt. */
    public long getRewardAmount() {
        return rewardAmount;
    }

    /** Tổng tiền phạt từ các quyết định kỷ luật đã duyệt có số tiền. */
    public long getDisciplineAmount() {
        return disciplineAmount;
    }

    public int getUnconfirmedAttendance() {
        return unconfirmedAttendance;
    }

    public int getPendingDecisions() {
        return pendingDecisions;
    }
}
