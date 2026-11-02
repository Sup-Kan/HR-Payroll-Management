package com.hrpayroll.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quy ước tiền tệ: số nguyên đồng, hiển thị dấu chấm ngăn cách hàng nghìn
 * (9.000.000). Khi nhập chấp nhận 500000, 500.000 hoặc 500,000;
 * không chấp nhận số thập phân hay số âm.
 */
public final class MoneyUtils {

    /** DECIMAL(15,2) trong DB cho tối đa 13 chữ số phần nguyên. */
    public static final int MAX_DIGITS = 13;

    private static final Pattern GROUPED =
            Pattern.compile("\\d{1,3}([.,]\\d{3})+");
    private static final Pattern PLAIN = Pattern.compile("\\d+");

    /** So sánh hai chuỗi tiền đã định dạng theo giá trị số. */
    public static final Comparator<Object> COMPARATOR = (a, b) ->
            Long.compare(toNumber(a), toNumber(b));

    private MoneyUtils() {
    }

    public static String format(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount);
    }

    /** @throws NumberFormatException nếu không phải số tiền hợp lệ */
    public static long parse(String text) {
        String t = text == null ? "" : text.trim();

        if (!PLAIN.matcher(t).matches() && !GROUPED.matcher(t).matches()) {
            throw new NumberFormatException(
                "Số tiền chỉ gồm chữ số, có thể ngăn cách hàng nghìn "
                + "bằng dấu chấm (ví dụ 500.000)."
            );
        }

        String digits = t.replace(".", "").replace(",", "");

        if (digits.length() > MAX_DIGITS) {
            throw new NumberFormatException(
                "Số tiền tối đa " + MAX_DIGITS + " chữ số."
            );
        }

        return Long.parseLong(digits);
    }

    public static boolean isValid(String text) {
        try {
            parse(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static long toNumber(Object value) {
        String t = String.valueOf(value).trim().replace(".", "");

        try {
            return Long.parseLong(t);
        } catch (NumberFormatException ex) {
            return Long.MIN_VALUE;
        }
    }
}
