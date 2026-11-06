package com.hrpayroll.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Vai trò tài khoản; tên hằng khớp trực tiếp với accounts.role trong MariaDB. */
public enum Role {
    ADMIN("Quản trị hệ thống", EnumSet.complementOf(EnumSet.of(Feature.PROFILE))),
    IT("Công nghệ thông tin", EnumSet.complementOf(EnumSet.of(Feature.PROFILE))),
    HR("Nhân sự", EnumSet.of(Feature.EMPLOYEE, Feature.ATTENDANCE, Feature.REWARD_DISCIPLINE, Feature.REPORT, Feature.CATALOG, Feature.PROFILE)),
    ACCOUNTANT("Kế toán", EnumSet.of(Feature.PAYROLL, Feature.REPORT, Feature.PROFILE)),
    EMPLOYEE("Nhân viên", EnumSet.of(Feature.PROFILE)),
    SECURITY("An ninh", EnumSet.of(Feature.PROFILE)),
    CLEANER("Vệ sinh", EnumSet.of(Feature.PROFILE)),
    OPERATOR("Vận hành", EnumSet.of(Feature.PROFILE));

    private final String label;
    private final Set<Feature> features;
    Role(String label, Set<Feature> features) {
        this.label = label;
        this.features = Collections.unmodifiableSet(features);
    }
    public boolean canAccess(Feature feature) { return features.contains(feature); }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
