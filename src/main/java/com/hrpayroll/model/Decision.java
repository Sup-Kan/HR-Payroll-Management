package com.hrpayroll.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Quyết định khen thưởng hoặc kỷ luật. Số tiền có thể để trống: kỷ luật
 * không mặc định là phạt tiền khi chưa có quy định. Chỉ quyết định đã duyệt
 * và có số tiền mới được tính vào lương.
 */
public final class Decision {

    private final String code;
    private final DecisionType type;
    private String employeeCode;
    private String employeeName;
    private LocalDate date;
    private String reason;
    private Long amount;
    private DecisionStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;

    public Decision(
            String code,
            DecisionType type,
            String employeeCode,
            String employeeName,
            LocalDate date,
            String reason,
            Long amount,
            DecisionStatus status) {

        this.code = code;
        this.type = type;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.date = date;
        this.reason = reason;
        this.amount = amount;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public DecisionType getType() {
        return type;
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

    public String getReason() {
        return reason;
    }

    /** Có thể null: quyết định không kèm số tiền. */
    public Long getAmount() {
        return amount;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    /** Chỉ sửa được khi chưa duyệt và chưa hủy. */
    public boolean isEditable() {
        return status == DecisionStatus.DRAFT
                || status == DecisionStatus.PENDING;
    }

    /** Mã và loại quyết định không đổi; các trường còn lại được cập nhật. */
    public void update(
            String employeeCode,
            String employeeName,
            LocalDate date,
            String reason,
            Long amount) {

        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.date = date;
        this.reason = reason;
        this.amount = amount;
    }

    public void approve(String by, LocalDateTime at) {
        this.status = DecisionStatus.APPROVED;
        this.approvedBy = by;
        this.approvedAt = at;
    }

    public void cancel() {
        this.status = DecisionStatus.CANCELLED;
    }
}
