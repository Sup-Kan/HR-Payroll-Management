package com.hrpayroll.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Vai trò người dùng và các chức năng được phép truy cập.
 * Tên hằng khớp với cột accounts.role trong schema.sql
 * (ADMIN, HR, ACCOUNTANT) nên có thể dùng Role.valueOf(...) khi nối DB.
 */
public enum Role {
    ADMIN("Admin", EnumSet.allOf(Feature.class)),
    HR("HR", EnumSet.of(
            Feature.EMPLOYEE,
            Feature.ATTENDANCE,
            Feature.REWARD_DISCIPLINE,
            Feature.REPORT)),
    ACCOUNTANT("Kế toán", EnumSet.of(
            Feature.PAYROLL,
            Feature.REPORT));

    private final String label;
    private final Set<Feature> features;

    Role(String label, Set<Feature> features) {
        this.label = label;
        this.features = Collections.unmodifiableSet(features);
    }

    public boolean canAccess(Feature feature) {
        return features.contains(feature);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
