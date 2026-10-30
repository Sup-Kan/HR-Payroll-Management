package com.hrpayroll.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quy ước mã nhân viên: "NV" + 3 chữ số (NV001 đến NV999).
 * Quy ước này cũng được ràng buộc ở cơ sở dữ liệu (chk_employee_code_format
 * trong schema.sql); nếu cần quá 999 nhân viên phải đổi cả hai nơi.
 */
public final class EmployeeCodeUtils {

    public static final String EXAMPLE = "NV001";

    public static final String INVALID_MESSAGE =
            "Mã nhân viên phải gồm \"NV\" và 3 chữ số (ví dụ " + EXAMPLE + ").";

    private static final Pattern PATTERN = Pattern.compile("NV[0-9]{3}");

    private EmployeeCodeUtils() {
    }

    /** Cắt khoảng trắng và viết hoa (nv001 thành NV001). */
    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String code) {
        return PATTERN.matcher(normalize(code)).matches();
    }
}
