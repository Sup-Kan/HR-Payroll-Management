package com.hrpayroll.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Vai trò tài khoản; tên hằng khớp trực tiếp với accounts.role trong MariaDB. */
public enum Role {
    ADMIN("Quản trị hệ thống", EnumSet.allOf(Feature.class)),
    IT("Công nghệ thông tin", EnumSet.allOf(Feature.class)),
    HR("Nhân sự", EnumSet.of(Feature.EMPLOYEE, Feature.ATTENDANCE, Feature.REWARD_DISCIPLINE, Feature.REPORT)),
    ACCOUNTANT("Kế toán", EnumSet.of(Feature.PAYROLL, Feature.REPORT)),
    EMPLOYEE("Nhân viên", EnumSet.noneOf(Feature.class)),
    SECURITY("An ninh", EnumSet.noneOf(Feature.class)),
    CLEANER("Vệ sinh", EnumSet.noneOf(Feature.class)),
    OPERATOR("Vận hành", EnumSet.noneOf(Feature.class));

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
