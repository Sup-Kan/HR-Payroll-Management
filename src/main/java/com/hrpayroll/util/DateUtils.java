package com.hrpayroll.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;

/**
 * Quy ước ngày của dự án: giao diện hiển thị và nhập theo dd/MM/yyyy
 * (ví dụ 15/05/1998). Cơ sở dữ liệu vẫn lưu kiểu DATE chuẩn (yyyy-MM-dd);
 * việc chuyển đổi chỉ diễn ra ở lớp giao diện/DAO qua lớp này.
 */
public final class DateUtils {

    /** Chuỗi mẫu hiển thị cho người dùng (nhãn, tooltip, thông báo lỗi). */
    public static final String DISPLAY_PATTERN = "dd/MM/yyyy";

    // STRICT: từ chối ngày không tồn tại (31/02/2026) và bắt buộc đủ 2 chữ số
    // ngày/tháng. STRICT yêu cầu dùng "uuuu" thay cho "yyyy" khi phân tích.
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    /**
     * So sánh hai giá trị ngày dạng dd/MM/yyyy theo thứ tự thời gian
     * (dùng cho TableRowSorter.setComparator). Giá trị rỗng hoặc sai định
     * dạng được xếp trước các ngày hợp lệ.
     */
    public static final Comparator<Object> COMPARATOR = (a, b) -> {
        LocalDate da = tryParse(a);
        LocalDate db = tryParse(b);
        if (da == null && db == null) {
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
        if (da == null) {
            return -1;
        }
        if (db == null) {
            return 1;
        }
        return da.compareTo(db);
    };

    private DateUtils() {
    }

    /** @throws DateTimeParseException nếu sai định dạng hoặc ngày không tồn tại */
    public static LocalDate parse(String text) {
        return LocalDate.parse(text.trim(), FORMATTER);
    }

    public static boolean isValid(String text) {
        return tryParse(text) != null;
    }

    public static String format(LocalDate date) {
        return date.format(FORMATTER);
    }

    private static LocalDate tryParse(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return parse(String.valueOf(value));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
