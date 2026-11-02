package com.hrpayroll.model;

/** Loại quyết định. Tên hằng khớp decisions.decision_type trong schema.sql. */
public enum DecisionType {
    REWARD("Khen thưởng", "KT"),
    DISCIPLINE("Kỷ luật", "KL");

    private final String label;
    private final String codePart;

    DecisionType(String label, String codePart) {
        this.label = label;
        this.codePart = codePart;
    }

    public String getLabel() {
        return label;
    }

    /** Phần viết tắt trong mã quyết định, ví dụ QD-KT-2026-001. */
    public String getCodePart() {
        return codePart;
    }

    @Override
    public String toString() {
        return label;
    }
}
