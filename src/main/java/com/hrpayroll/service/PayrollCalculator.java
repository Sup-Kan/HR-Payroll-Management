package com.hrpayroll.service;

import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.PayrollIssue;
import com.hrpayroll.model.PayrollIssue.Severity;
import com.hrpayroll.model.PayrollResult;
import com.hrpayroll.model.PayrollSettings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Quy tắc tính lương đã chốt ở Giai đoạn 1:
 * - Lương theo công = lương cơ bản / 22 x số công;
 * - Tiền tăng ca = lương cơ bản / (22 x 8) x số giờ x hệ số
 *   (ngày thường 150%, cuối tuần 200%, lễ 300%);
 * - Phụ cấp cố định theo chức vụ; thưởng và tiền phạt lấy từ quyết định đã duyệt;
 * - Bảo hiểm = tỷ lệ x lương cơ bản; thuế = tỷ lệ x (tổng thu nhập - bảo hiểm);
 * - Thực lĩnh = lương theo công + phụ cấp + thưởng + tăng ca - khấu trừ;
 * - Mỗi khoản tiền bỏ phần thập phân (làm tròn xuống đến đồng).
 *
 * Hai điểm quy tắc chưa nêu rõ, đang giả định và cần xác nhận ở Giai đoạn 7:
 * cơ sở tính thuế (tổng thu nhập trừ bảo hiểm) và cách làm tròn (xuống).
 */
public final class PayrollCalculator {

    public static final BigDecimal RATE_WEEKDAY = new BigDecimal("1.5");
    public static final BigDecimal RATE_WEEKEND = new BigDecimal("2.0");
    public static final BigDecimal RATE_HOLIDAY = new BigDecimal("3.0");

    private static final BigDecimal WORKDAYS =
            BigDecimal.valueOf(PayrollSettings.STANDARD_WORKDAYS);
    private static final BigDecimal HOURS =
            BigDecimal.valueOf(PayrollSettings.STANDARD_HOURS);
    private static final MathContext MC = MathContext.DECIMAL128;

    private PayrollCalculator() {
    }

    public static PayrollResult calculate(
            PayrollInput in, PayrollSettings settings) {

        BigDecimal base = BigDecimal.valueOf(in.getBaseSalary());

        long salaryByAttendance = floor(
            base.multiply(in.getPaidWorkdays()).divide(WORKDAYS, MC)
        );

        long overtime = floor(overtimeExact(in, base));

        long gross = salaryByAttendance
            + in.getAllowance()
            + in.getRewardAmount()
            + overtime;

        long insurance = floor(base.multiply(settings.getInsuranceRate()));

        long taxable = Math.max(0, gross - insurance);
        long tax = floor(
            BigDecimal.valueOf(taxable).multiply(settings.getTaxRate())
        );

        long net = gross - insurance - tax - in.getDisciplineAmount();

        return new PayrollResult(
            in, salaryByAttendance, overtime, gross, insurance, tax, net
        );
    }

    public static List<PayrollResult> calculateAll(
            List<PayrollInput> inputs, PayrollSettings settings) {

        List<PayrollResult> results = new ArrayList<>();

        for (PayrollInput input : inputs) {
            results.add(calculate(input, settings));
        }

        return results;
    }

    /**
     * Kiểm tra dữ liệu trước khi tính/lưu/chốt lương.
     *
     * @param results kết quả đã tính, hoặc null nếu chưa tính
     */
    public static List<PayrollIssue> check(
            List<PayrollInput> inputs,
            PayrollSettings settings,
            List<PayrollResult> results) {

        List<PayrollIssue> issues = new ArrayList<>();

        if (inputs.isEmpty()) {
            issues.add(error("Không có nhân viên nào để tính lương."));
            return issues;
        }

        boolean anyAttendance = false;

        for (PayrollInput in : inputs) {
            if (in.getPaidWorkdays().signum() > 0) {
                anyAttendance = true;
            }
        }

        if (!anyAttendance) {
            issues.add(error(
                "Kỳ này chưa có dữ liệu chấm công nào, không thể tính lương."
            ));
        }

        for (PayrollInput in : inputs) {
            String who = in.getEmployeeCode() + ": ";

            if (in.getBaseSalary() <= 0) {
                issues.add(error(who + "lương cơ bản chưa hợp lệ."));
            }

            if (in.getPaidWorkdays().signum() < 0
                    || in.getPaidWorkdays().compareTo(BigDecimal.valueOf(31)) > 0) {
                issues.add(error(who + "số công nằm ngoài khoảng 0 đến 31."));
            }

            if (in.getUnconfirmedAttendance() > 0) {
                issues.add(error(
                    who + "còn " + in.getUnconfirmedAttendance()
                    + " bản ghi công chưa xác nhận."
                ));
            }

            if (in.getPendingDecisions() > 0) {
                issues.add(warning(
                    who + "còn " + in.getPendingDecisions()
                    + " quyết định chờ duyệt, sẽ không được tính vào lương."
                ));
            }
        }

        if (results != null) {
            for (PayrollResult result : results) {
                if (result.getNetSalary() < 0) {
                    issues.add(error(
                        result.getEmployeeCode()
                        + ": thực lĩnh âm, khấu trừ vượt tổng thu nhập."
                    ));
                }
            }
        }

        if (settings.getInsuranceRate().signum() == 0) {
            issues.add(warning("Tỷ lệ bảo hiểm đang là 0%."));
        }

        if (settings.getTaxRate().signum() == 0) {
            issues.add(warning("Tỷ lệ thuế thu nhập đang là 0%."));
        }

        return issues;
    }

    public static boolean hasError(List<PayrollIssue> issues) {
        for (PayrollIssue issue : issues) {
            if (issue.isError()) {
                return true;
            }
        }
        return false;
    }

    public static long totalNet(List<PayrollResult> results) {
        long total = 0;
        for (PayrollResult result : results) {
            total += result.getNetSalary();
        }
        return total;
    }

    /**
     * Tiền tăng ca chính xác (chưa làm tròn) của cả ba loại ngày.
     * Nhân trước, chia một lần ở cuối: chia lương giờ trước sẽ sinh số lẻ
     * vô hạn (12.000.000 / 176) và làm kết quả đúng 1.500.000 bị cắt
     * xuống 1.499.999.
     */
    private static BigDecimal overtimeExact(PayrollInput in, BigDecimal base) {
        BigDecimal weightedHours = in.getOvertimeWeekday().multiply(RATE_WEEKDAY)
            .add(in.getOvertimeWeekend().multiply(RATE_WEEKEND))
            .add(in.getOvertimeHoliday().multiply(RATE_HOLIDAY));

        return base.multiply(weightedHours)
            .divide(WORKDAYS.multiply(HOURS), MC);
    }

    private static long floor(BigDecimal value) {
        return value.setScale(0, RoundingMode.DOWN).longValueExact();
    }

    private static PayrollIssue error(String message) {
        return new PayrollIssue(Severity.ERROR, message);
    }

    private static PayrollIssue warning(String message) {
        return new PayrollIssue(Severity.WARNING, message);
    }
}
