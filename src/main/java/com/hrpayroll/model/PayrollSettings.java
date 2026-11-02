package com.hrpayroll.model;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Cấu hình tính lương. Tỷ lệ bảo hiểm và thuế do người dùng cấu hình
 * (lưu dạng phân số, ví dụ 0.105 là 10,5%); công chuẩn và giờ chuẩn
 * là quy tắc đã chốt.
 */
public final class PayrollSettings {

    public static final int STANDARD_WORKDAYS = 22;
    public static final int STANDARD_HOURS = 8;

    private static final Pattern PERCENT = Pattern.compile("\\d{1,3}(\\.\\d{1,3})?");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal insuranceRate;
    private final BigDecimal taxRate;

    public PayrollSettings(BigDecimal insuranceRate, BigDecimal taxRate) {
        this.insuranceRate = insuranceRate;
        this.taxRate = taxRate;
    }

    /**
     * Tạo cấu hình từ chuỗi phần trăm người dùng nhập (ví dụ "10.5" hoặc "10,5").
     *
     * @throws IllegalArgumentException với thông báo tiếng Việt nếu sai
     */
    public static PayrollSettings fromPercentText(
            String insuranceText, String taxText) {

        return new PayrollSettings(
            parsePercent(insuranceText, "bảo hiểm"),
            parsePercent(taxText, "thuế thu nhập")
        );
    }

    private static BigDecimal parsePercent(String text, String name) {
        String t = text == null ? "" : text.trim().replace(',', '.');

        if (t.isEmpty()) {
            throw new IllegalArgumentException(
                "Tỷ lệ " + name + " không được để trống (nhập 0 nếu không áp dụng)."
            );
        }

        if (!PERCENT.matcher(t).matches()) {
            throw new IllegalArgumentException(
                "Tỷ lệ " + name + " phải là số từ 0 đến 100, "
                + "tối đa 3 chữ số thập phân."
            );
        }

        BigDecimal percent = new BigDecimal(t);

        if (percent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(
                "Tỷ lệ " + name + " không được vượt quá 100%."
            );
        }

        return percent.divide(HUNDRED);
    }

    public BigDecimal getInsuranceRate() {
        return insuranceRate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /** Tỷ lệ dạng phần trăm để hiển thị, ví dụ "10.5" hoặc "0". */
    public static String toPercentText(BigDecimal rate) {
        BigDecimal percent = rate.multiply(HUNDRED).stripTrailingZeros();
        return percent.signum() == 0 ? "0" : percent.toPlainString();
    }

    public boolean sameRatesAs(PayrollSettings other) {
        return other != null
            && insuranceRate.compareTo(other.insuranceRate) == 0
            && taxRate.compareTo(other.taxRate) == 0;
    }
}
