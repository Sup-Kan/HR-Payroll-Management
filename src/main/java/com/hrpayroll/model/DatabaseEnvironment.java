package com.hrpayroll.model;

/** Hai môi trường dùng chung một bộ nghiệp vụ; chỉ khác database đích. */
public enum DatabaseEnvironment {
    PRIMARY("Dữ liệu gốc", "hr_payroll_primary"),
    SANDBOX("Bản sao thử nghiệm", "hr_payroll_sandbox");

    private final String label;
    private final String databaseName;

    DatabaseEnvironment(String label, String databaseName) {
        this.label = label;
        this.databaseName = databaseName;
    }

    public String getLabel() { return label; }
    public String getDatabaseName() { return databaseName; }
}
