package com.hrpayroll.view;

import com.hrpayroll.dao.AttendanceDAO;
import com.hrpayroll.dao.PayrollDAO;
import com.hrpayroll.dao.ReportDAO;
import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.PayrollResult;
import com.hrpayroll.model.PayrollSettings;
import com.hrpayroll.service.AttendanceCalculator;
import com.hrpayroll.service.PayrollCalculator;
import com.hrpayroll.service.ReportService;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.MoneyUtils;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Báo cáo, thống kê (mục 3.7): nhân viên theo phòng ban, ngày công và tiền
 * lương theo kỳ. Số liệu lấy từ DB (ReportDAO, PayrollDAO).
 */
public class ReportFrame extends JFrame {

    private enum ReportType { EMPLOYEE, ATTENDANCE, PAYROLL }

    private static final String TOTAL = "Tổng cộng";

    private ReportType current = ReportType.EMPLOYEE;

    private JToggleButton employeeButton;
    private JToggleButton attendanceButton;
    private JToggleButton payrollButton;

    private JComboBox<Integer> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JComboBox<String> departmentCombo;
    private JTextField keywordField;
    private JButton filterButton;
    private JButton refreshButton;
    private JButton backButton;

    private JScrollPane tableScrollPane;
    private JTable reportTable;
    private JLabel summaryLabel;

    public ReportFrame() {
        setTitle("Báo cáo, thống kê");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setMinimumSize(new Dimension(900, 540));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        generateReport();
    }

    // ------------------------------------------------------------------
    // Dựng giao diện
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("BÁO CÁO, THỐNG KÊ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Thống kê nhân viên, ngày công và tiền lương"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        // Chọn loại báo cáo
        employeeButton = new JToggleButton("Thống kê nhân viên", true);
        attendanceButton = new JToggleButton("Thống kê ngày công");
        payrollButton = new JToggleButton("Thống kê tiền lương");

        ButtonGroup group = new ButtonGroup();
        group.add(employeeButton);
        group.add(attendanceButton);
        group.add(payrollButton);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        typePanel.setBorder(BorderFactory.createTitledBorder("Loại báo cáo"));
        typePanel.add(employeeButton);
        typePanel.add(attendanceButton);
        typePanel.add(payrollButton);

        // Bộ lọc
        monthCombo = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthCombo.addItem(month);
        }

        yearCombo = new JComboBox<>();
        // Kỳ mặc định: tháng có dữ liệu chấm công gần nhất.
        YearMonth defaultPeriod = AttendanceDAO.latestPeriodOrNow();
        int currentYear = LocalDate.now().getYear();
        int firstYear = Math.min(currentYear - 2, defaultPeriod.getYear());
        int lastYear = Math.max(currentYear + 1, defaultPeriod.getYear());
        for (int year = firstYear; year <= lastYear; year++) {
            yearCombo.addItem(year);
        }

        monthCombo.setSelectedItem(defaultPeriod.getMonthValue());
        yearCombo.setSelectedItem(defaultPeriod.getYear());

        monthCombo.setPreferredSize(new Dimension(65, 24));
        monthCombo.setMinimumSize(new Dimension(65, 24));
        yearCombo.setPreferredSize(new Dimension(85, 24));
        yearCombo.setMinimumSize(new Dimension(85, 24));

        departmentCombo = new JComboBox<>();
        departmentCombo.addItem(ReportService.ALL);
        try {
            for (String department : ReportDAO.findDepartments()) {
                departmentCombo.addItem(department);
            }
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được danh sách phòng ban.\n" + ex.getMessage());
        }

        keywordField = new JTextField(14);
        keywordField.setToolTipText("Mã hoặc tên nhân viên");

        filterButton = new JButton("Tìm kiếm / Lọc");
        refreshButton = new JButton("Làm mới");

        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        filterPanel.setBorder(BorderFactory.createTitledBorder("Bộ lọc"));
        filterPanel.add(new JLabel("Tháng:"));
        filterPanel.add(monthCombo);
        filterPanel.add(new JLabel("Năm:"));
        filterPanel.add(yearCombo);
        filterPanel.add(new JLabel("Phòng ban:"));
        filterPanel.add(departmentCombo);
        filterPanel.add(new JLabel("Từ khóa:"));
        filterPanel.add(keywordField);
        filterPanel.add(filterButton);
        filterPanel.add(refreshButton);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.add(typePanel, BorderLayout.NORTH);
        controls.add(filterPanel, BorderLayout.CENTER);
        topPanel.add(controls, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Bảng báo cáo
        tableScrollPane = new JScrollPane();
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Kết quả")
        );
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Chân trang
        summaryLabel = new JLabel(" ");
        backButton = new JButton("Quay lại");

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 4));
        bottomPanel.add(summaryLabel, BorderLayout.CENTER);
        bottomPanel.add(backButton, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void initEvents() {
        employeeButton.addActionListener(e -> selectType(ReportType.EMPLOYEE));
        attendanceButton.addActionListener(
            e -> selectType(ReportType.ATTENDANCE)
        );
        payrollButton.addActionListener(e -> selectType(ReportType.PAYROLL));

        filterButton.addActionListener(e -> generateReport());
        keywordField.addActionListener(e -> generateReport());
        monthCombo.addActionListener(e -> generateReport());
        yearCombo.addActionListener(e -> generateReport());
        departmentCombo.addActionListener(e -> generateReport());

        refreshButton.addActionListener(e -> {
            keywordField.setText("");
            departmentCombo.setSelectedIndex(0);
            YearMonth latest = AttendanceDAO.latestPeriodOrNow();
            monthCombo.setSelectedItem(latest.getMonthValue());
            yearCombo.setSelectedItem(latest.getYear());
            generateReport();
        });

        backButton.addActionListener(e -> dispose());
    }

    private void selectType(ReportType type) {
        current = type;
        generateReport();
    }

    // ------------------------------------------------------------------
    // Sinh báo cáo
    // ------------------------------------------------------------------

    private void generateReport() {
        if (tableScrollPane == null || departmentCombo == null) {
            return;
        }

        boolean periodBased = current != ReportType.EMPLOYEE;
        monthCombo.setEnabled(periodBased);
        yearCombo.setEnabled(periodBased);

        switch (current) {
            case ATTENDANCE:
                showAttendanceReport();
                break;
            case PAYROLL:
                showPayrollReport();
                break;
            default:
                showEmployeeReport();
                break;
        }
    }

    private String department() {
        return (String) departmentCombo.getSelectedItem();
    }

    private String keyword() {
        return keywordField.getText();
    }

    private YearMonth period() {
        return YearMonth.of(
            (Integer) yearCombo.getSelectedItem(),
            (Integer) monthCombo.getSelectedItem()
        );
    }

    private void showEmployeeReport() {
        List<ReportService.DepartmentStat> stats;

        try {
            List<String> departments = new ArrayList<>(
                ReportDAO.findDepartments()
            );
            departments.add(ReportDAO.NO_DEPARTMENT);

            stats = ReportService.employeeStats(
                ReportDAO.findEmployees(), departments, department(), keyword()
            );
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được số liệu nhân viên.\n" + ex.getMessage());
            stats = new ArrayList<>();
        }

        List<Object[]> rows = new ArrayList<>();
        int total = 0;
        int active = 0;
        int resigned = 0;
        int male = 0;
        int female = 0;

        for (ReportService.DepartmentStat stat : stats) {
            rows.add(new Object[]{
                stat.getDepartment(), stat.getTotal(), stat.getActive(),
                stat.getResigned(), stat.getMale(), stat.getFemale()
            });
            total += stat.getTotal();
            active += stat.getActive();
            resigned += stat.getResigned();
            male += stat.getMale();
            female += stat.getFemale();
        }

        if (!rows.isEmpty()) {
            rows.add(new Object[]{
                TOTAL, total, active, resigned, male, female
            });
        }

        setTable(
            new String[] {
                "Phòng ban", "Tổng nhân viên", "Đang làm việc",
                "Đã nghỉ việc", "Nam", "Nữ"
            },
            rows,
            new int[] {220, 130, 130, 130, 90, 90},
            new int[] {
                SwingConstants.LEFT, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.CENTER
            }
        );

        summaryLabel.setText(
            rows.isEmpty()
                ? "Không có nhân viên phù hợp với bộ lọc."
                : "Tổng " + total + " nhân viên: " + active
                  + " đang làm việc, " + resigned + " đã nghỉ việc."
        );
    }

    private void showAttendanceReport() {
        List<Object[]> rows = new ArrayList<>();
        boolean periodHasData = false;

        try {
            List<ReportDAO.AttendanceStat> stats =
                ReportDAO.attendanceStats(period());
            periodHasData = !stats.isEmpty();

            double present = 0;
            int paidLeave = 0;
            int unpaidLeave = 0;
            int absent = 0;
            double overtime = 0;

            for (ReportService.AttendanceRow row
                    : ReportService.attendanceStats(
                        ReportDAO.findEmployees(), stats,
                        department(), keyword())) {

                rows.add(new Object[]{
                    row.getCode(), row.getName(), row.getDepartment(),
                    AttendanceCalculator.format2(row.getPresentDays()),
                    row.getPaidLeaveDays(),
                    AttendanceCalculator.format2(row.getTotalDays()),
                    AttendanceCalculator.format2(row.getOvertimeHours()),
                    row.getUnpaidLeaveDays(), row.getAbsentDays()
                });

                present += row.getPresentDays();
                paidLeave += row.getPaidLeaveDays();
                unpaidLeave += row.getUnpaidLeaveDays();
                absent += row.getAbsentDays();
                overtime += row.getOvertimeHours();
            }

            if (!rows.isEmpty()) {
                rows.add(new Object[]{
                    TOTAL, "", "",
                    AttendanceCalculator.format2(present), paidLeave,
                    AttendanceCalculator.format2(present + paidLeave),
                    AttendanceCalculator.format2(overtime),
                    unpaidLeave, absent
                });
            }
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được số liệu ngày công.\n" + ex.getMessage());
        }

        setTable(
            new String[] {
                "Mã NV", "Họ tên", "Phòng ban", "Công làm việc",
                "Nghỉ phép năm", "Tổng công", "Tăng ca (giờ)",
                "Nghỉ không lương", "Vắng mặt"
            },
            rows,
            new int[] {70, 150, 150, 105, 105, 90, 105, 125, 90},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,
                SwingConstants.LEFT, SwingConstants.RIGHT,
                SwingConstants.CENTER, SwingConstants.RIGHT,
                SwingConstants.RIGHT, SwingConstants.CENTER,
                SwingConstants.CENTER
            }
        );

        summaryLabel.setText(periodSummary(periodHasData, rows.isEmpty(),
            "Công chuẩn của kỳ: "
            + PayrollSettings.STANDARD_WORKDAYS + " ngày."));
    }

    private void showPayrollReport() {
        List<Object[]> rows = new ArrayList<>();
        boolean periodHasData = false;
        String note = "";

        try {
            List<PayrollResult> results;
            PayrollDAO.SavedPeriod saved = PayrollDAO.loadSaved(period());

            if (saved != null) {
                // Bảng lương đã lưu trong DB: báo cáo đúng số đã lưu.
                periodHasData = true;
                results = saved.getResults();
                note = saved.isClosed()
                    ? "Bảng lương đã chốt."
                    : "Bảng lương đã lưu, chưa chốt.";
            } else {
                // Chưa lưu: tính dự kiến từ dữ liệu hiện hành với tỷ lệ mặc định.
                List<PayrollInput> inputs = PayrollDAO.loadInputs(period());
                PayrollSettings settings = PayrollDAO.loadSettings();

                for (PayrollInput input : inputs) {
                    if (input.getPaidWorkdays().signum() > 0) {
                        periodHasData = true;
                    }
                }

                results = periodHasData
                    ? PayrollCalculator.calculateAll(inputs, settings)
                    : new ArrayList<PayrollResult>();
                note = "Số liệu dự kiến, chưa lưu bảng lương (bảo hiểm "
                    + PayrollSettings.toPercentText(settings.getInsuranceRate())
                    + "%, thuế "
                    + PayrollSettings.toPercentText(settings.getTaxRate())
                    + "%). Bảng lương chính thức xem ở màn Quản lý tiền lương.";
            }

            List<ReportService.PayrollRow> data =
                ReportService.payrollStats(
                    ReportDAO.findEmployees(), results,
                    department(), keyword()
                );

            for (ReportService.PayrollRow row : data) {
                rows.add(new Object[]{
                    row.getCode(), row.getName(), row.getDepartment(),
                    MoneyUtils.format(row.getBaseSalary()),
                    MoneyUtils.format(row.getAllowance()),
                    MoneyUtils.format(row.getReward()),
                    MoneyUtils.format(row.getOvertime()),
                    MoneyUtils.format(row.getDeduction()),
                    MoneyUtils.format(row.getNet())
                });
            }

            if (!rows.isEmpty()) {
                long[] totals = ReportService.payrollTotals(data);

                rows.add(new Object[]{
                    TOTAL, "", "",
                    MoneyUtils.format(totals[0]),
                    MoneyUtils.format(totals[1]),
                    MoneyUtils.format(totals[2]),
                    MoneyUtils.format(totals[3]),
                    MoneyUtils.format(totals[4]),
                    MoneyUtils.format(totals[5])
                });
            }
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được số liệu tiền lương.\n" + ex.getMessage());
        }

        setTable(
            new String[] {
                "Mã NV", "Họ tên", "Phòng ban", "Lương cơ bản",
                "Phụ cấp", "Thưởng", "Tăng ca", "Khấu trừ", "Thực lĩnh"
            },
            rows,
            new int[] {70, 140, 140, 110, 95, 90, 95, 95, 115},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,
                SwingConstants.LEFT, SwingConstants.RIGHT,
                SwingConstants.RIGHT, SwingConstants.RIGHT,
                SwingConstants.RIGHT, SwingConstants.RIGHT,
                SwingConstants.RIGHT
            }
        );

        summaryLabel.setText(
            periodSummary(periodHasData, rows.isEmpty(), note));
    }

    private String periodSummary(
            boolean periodHasData, boolean empty, String note) {

        YearMonth period = period();

        if (!periodHasData) {
            return String.format(
                "Kỳ %02d/%d chưa có dữ liệu.",
                period.getMonthValue(), period.getYear()
            );
        }

        return empty
            ? "Không có dữ liệu phù hợp với bộ lọc."
            : String.format(
                "Kỳ %02d/%d. %s", period.getMonthValue(), period.getYear(), note
            );
    }

    /** Dựng bảng mới cho mỗi báo cáo vì số cột và kiểu căn lề khác nhau. */
    private void setTable(
            String[] columns,
            List<Object[]> rows,
            int[] widths,
            int[] alignments) {

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Object[] row : rows) {
            model.addRow(row);
        }

        reportTable = new JTable(model);
        reportTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableStyler.apply(reportTable, widths, alignments);

        tableScrollPane.setViewportView(reportTable);
    }
}
