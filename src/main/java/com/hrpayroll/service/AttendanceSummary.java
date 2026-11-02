package com.hrpayroll.service;

import com.hrpayroll.model.AttendanceRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Tổng hợp bảng công theo nhân viên từ danh sách bản ghi chấm công. */
public final class AttendanceSummary {

    /** Một dòng của bảng công: tổng hợp của một nhân viên. */
    public static final class Row {

        private final String employeeCode;
        private final String employeeName;
        private double presentDays;
        private int paidLeaveDays;
        private int unpaidLeaveDays;
        private int absentDays;
        private double overtimeHours;
        private int unconfirmedRecords;

        private Row(String employeeCode, String employeeName) {
            this.employeeCode = employeeCode;
            this.employeeName = employeeName;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        /** Số công làm việc thực tế (đã quy đổi từ giờ làm). */
        public double getPresentDays() {
            return presentDays;
        }

        public int getPaidLeaveDays() {
            return paidLeaveDays;
        }

        public int getUnpaidLeaveDays() {
            return unpaidLeaveDays;
        }

        public int getAbsentDays() {
            return absentDays;
        }

        public double getOvertimeHours() {
            return overtimeHours;
        }

        public int getUnconfirmedRecords() {
            return unconfirmedRecords;
        }

        /** Tổng công tính lương = công làm việc + nghỉ phép năm có lương. */
        public double getTotalDays() {
            return presentDays + paidLeaveDays;
        }
    }

    private AttendanceSummary() {
    }

    /** Kết quả sắp xếp theo mã nhân viên. */
    public static List<Row> summarize(List<AttendanceRecord> records) {
        Map<String, Row> rows = new TreeMap<>();

        for (AttendanceRecord record : records) {
            Row row = rows.get(record.getEmployeeCode());

            if (row == null) {
                row = new Row(
                        record.getEmployeeCode(), record.getEmployeeName()
                );
                rows.put(record.getEmployeeCode(), row);
            }

            switch (record.getType()) {
                case PRESENT:
                    row.presentDays += AttendanceCalculator.workDays(record);
                    row.overtimeHours += record.getOvertimeHours();
                    break;
                case PAID_LEAVE:
                    row.paidLeaveDays++;
                    break;
                case UNPAID_LEAVE:
                    row.unpaidLeaveDays++;
                    break;
                default:
                    row.absentDays++;
                    break;
            }

            if (!record.isConfirmed()) {
                row.unconfirmedRecords++;
            }
        }

        return new ArrayList<>(rows.values());
    }
}
