package com.hrpayroll.model;

/** Một vấn đề phát hiện khi kiểm tra dữ liệu trước khi tính hoặc chốt lương. */
public final class PayrollIssue {

    /** ERROR chặn việc tính/lưu/chốt; WARNING chỉ cảnh báo. */
    public enum Severity { ERROR, WARNING }

    private final Severity severity;
    private final String message;

    public PayrollIssue(Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public String getMessage() {
        return message;
    }
}
