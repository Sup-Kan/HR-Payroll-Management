package com.hrpayroll.model;

/** Trạng thái phê duyệt. Tên hằng khớp decisions.approval_status trong schema.sql. */
public enum DecisionStatus {
    DRAFT("Bản nháp"),
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    CANCELLED("Đã hủy");

    private final String label;

    DecisionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
