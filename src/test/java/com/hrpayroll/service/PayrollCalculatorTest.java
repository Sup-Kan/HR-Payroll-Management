package com.hrpayroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.PayrollIssue;
import com.hrpayroll.model.PayrollResult;
import com.hrpayroll.model.PayrollSettings;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayrollCalculatorTest {

    private static final PayrollSettings S10 =
        PayrollSettings.fromPercentText("10.5", "10");
    private static final PayrollSettings ZERO =
        PayrollSettings.fromPercentText("0", "0");

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static PayrollInput input(long base, long allowance, String days,
            String otWeekday, String otWeekend, String otHoliday,
            long reward, long discipline, int unconfirmed, int pending) {
        return new PayrollInput("NV001", "Test", "Chuc vu", base, allowance,
            bd(days), bd(otWeekday), bd(otWeekend), bd(otHoliday),
            reward, discipline, unconfirmed, pending);
    }

    @Test
    void fullExample() {
        // Số tính tay: OT = 12tr*8*1.5/176 = 818181; gross = 14.318.181;
        // BH = 1.260.000; thuế = floor((14.318.181-1.260.000)*10%) = 1.305.818;
        // net = 14.318.181 - 1.260.000 - 1.305.818 - 200.000 = 11.552.363
        PayrollResult r = PayrollCalculator.calculate(
            input(12_000_000, 1_000_000, "22", "8", "0", "0", 500_000, 200_000, 0, 0), S10);
        assertEquals(11_552_363, r.getNetSalary());
    }

    @Test
    void overtimeExactNotTruncatedByEarlyDivision() {
        // Nhân trước, chia sau: 12tr x (22h x 1.5) / 176 phải ra đúng 2.250.000.
        PayrollResult r = PayrollCalculator.calculate(
            input(12_000_000, 0, "0", "22", "0", "0", 0, 0, 0, 0), ZERO);
        // 12tr * (22*1.5) / 176 = 2.250.000
        assertEquals(2_250_000, r.getNetSalary());
    }

    @Test
    void overtimeRatesByDayType() {
        // base 17.600.000 -> lương giờ = 17.6tr/176 = 100.000
        // 1h ngày thường 150k + 1h cuối tuần 200k + 1h lễ 300k = 650k
        PayrollResult r = PayrollCalculator.calculate(
            input(17_600_000, 0, "0", "1", "1", "1", 0, 0, 0, 0), ZERO);
        assertEquals(650_000, r.getNetSalary());
    }

    @Test
    void salaryByAttendanceIsFlooredToDong() {
        // 10.000.000 / 22 * 1 = 454.545,45 -> 454.545
        PayrollResult r = PayrollCalculator.calculate(
            input(10_000_000, 0, "1", "0", "0", "0", 0, 0, 0, 0), ZERO);
        assertEquals(454_545, r.getNetSalary());
    }

    @Test
    void zeroRatesMeansNoInsuranceNoTax() {
        PayrollResult r = PayrollCalculator.calculate(
            input(22_000_000, 500_000, "22", "0", "0", "0", 100_000, 0, 0, 0), ZERO);
        assertEquals(22_600_000, r.getNetSalary());
    }

    @Test
    void disciplineLargerThanIncomeGivesNegativeNetAndError() {
        PayrollInput in = input(5_000_000, 0, "1", "0", "0", "0", 0, 9_000_000, 0, 0);
        PayrollResult r = PayrollCalculator.calculate(in, ZERO);
        assertTrue(r.getNetSalary() < 0);
        List<PayrollIssue> issues = PayrollCalculator.check(List.of(in), ZERO, List.of(r));
        assertTrue(PayrollCalculator.hasError(issues));
    }

    @Test
    void checkFlagsBadInputs() {
        assertTrue(PayrollCalculator.hasError(
            PayrollCalculator.check(List.of(), S10, null)));
        assertTrue(PayrollCalculator.hasError(PayrollCalculator.check(
            List.of(input(0, 0, "22", "0", "0", "0", 0, 0, 0, 0)), S10, null)));
        assertTrue(PayrollCalculator.hasError(PayrollCalculator.check(
            List.of(input(10_000_000, 0, "32", "0", "0", "0", 0, 0, 0, 0)), S10, null)));
        assertTrue(PayrollCalculator.hasError(PayrollCalculator.check(
            List.of(input(10_000_000, 0, "22", "0", "0", "0", 0, 0, 2, 0)), S10, null)));
    }

    @Test
    void pendingDecisionIsOnlyWarning() {
        List<PayrollIssue> issues = PayrollCalculator.check(
            List.of(input(10_000_000, 0, "22", "0", "0", "0", 0, 0, 0, 3)), S10, null);
        assertFalse(issues.isEmpty());
        assertFalse(PayrollCalculator.hasError(issues));
    }

    @Test
    void percentParsing() {
        assertEquals(0, bd("0.105").compareTo(
            PayrollSettings.fromPercentText("10,5", "0").getInsuranceRate()));
        for (String bad : new String[] {"", "abc", "101", "-1", "1.2345"}) {
            try {
                PayrollSettings.fromPercentText(bad, "0");
                throw new AssertionError("Phải từ chối: " + bad);
            } catch (IllegalArgumentException expected) {
                // đúng
            }
        }
    }
}
