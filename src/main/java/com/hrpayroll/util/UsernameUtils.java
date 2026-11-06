package com.hrpayroll.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Sinh tên đăng nhập từ họ tên: bỏ dấu tiếng Việt, viết thường, nối bằng "_".
 * Ví dụ "Lê Quốc Bình" thành "le_quoc_binh".
 */
public final class UsernameUtils {

    public static final int MAX_LENGTH = 60;

    private UsernameUtils() {
    }

    /** @return tên đăng nhập gốc; "user" nếu họ tên không còn ký tự hợp lệ */
    public static String fromName(String fullName) {
        if (fullName == null) {
            return "user";
        }

        String s = fullName.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (s.isEmpty()) {
            return "user";
        }
        // chừa chỗ cho hậu tố số khi trùng tên
        return s.length() > MAX_LENGTH - 4 ? s.substring(0, MAX_LENGTH - 4) : s;
    }
}
