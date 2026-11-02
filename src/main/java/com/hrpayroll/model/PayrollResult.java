package com.hrpayroll.model;

import java.math.BigDecimal;

/** Kết quả tính lương một nhân viên; mọi khoản tiền là số nguyên đồng. */
public final class PayrollResult {

    private final PayrollInput input;
    private final long salaryByAttendance;
    private final long overtimeAmount;
    private final long grossSalary;
    private final long insuranceAmount;
    private final long incomeTaxAmount;
    private final long netSalary;

    public PayrollResult(
            PayrollInput input,
            long salaryByAttendance,
            long overtimeAmount,
            long grossSalary,
            long insuranceAmount,
            long incomeTaxAmount,
            long netSalary) {

        this.input = input;
        this.salaryByAttendance = salaryByAttendance;
        this.overtimeAmount = overtimeAmount;
        this.grossSalary = grossSalary;
        this.insuranceAmount = insuranceAmount;
        this.incomeTaxAmount = incomeTaxAmount;
        this.netSalary = netSalary;
    }

    public PayrollInput getInput() {
        return input;
    }

    public String getEmployeeCode() {
        return input.getEmployeeCode();
    }

    public String getEmployeeName() {
        return input.getEmployeeName();
    }

    public BigDecimal getPaidWorkdays() {
        return input.getPaidWorkdays();
    }

    /** Lương theo công = lương cơ bản / 22 x số công. */
    public long getSalaryByAttendance() {
        return salaryByAttendance;
    }

    public long getOvertimeAmount() {
        return overtimeAmount;
    }

    /** Tổng thu nhập = lương theo công + phụ cấp + thưởng + tiền tăng ca. */
    public long getGrossSalary() {
        return grossSalary;
    }

    public long getInsuranceAmount() {
        return insuranceAmount;
    }

    public long getIncomeTaxAmount() {
        return incomeTaxAmount;
    }

    /** Tổng khấu trừ = bảo hiểm + thuế + tiền phạt kỷ luật. */
    public long getTotalDeduction() {
        return insuranceAmount + incomeTaxAmount + input.getDisciplineAmount();
    }

    /** Thực lĩnh = tổng thu nhập − tổng khấu trừ (có thể âm nếu khấu trừ quá lớn). */
    public long getNetSalary() {
        return netSalary;
    }
}
