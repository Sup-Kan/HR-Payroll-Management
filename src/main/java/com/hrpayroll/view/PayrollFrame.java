package com.hrpayroll.view;

import com.hrpayroll.model.PayrollInput;
import com.hrpayroll.model.PayrollIssue;
import com.hrpayroll.model.PayrollResult;
import com.hrpayroll.model.PayrollSettings;
import com.hrpayroll.model.Role;
import com.hrpayroll.service.DemoData;
import com.hrpayroll.service.PayrollCalculator;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.MoneyUtils;
import com.hrpayroll.util.Session;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tính lương theo kỳ (mục 3.6): chọn kỳ, kiểm tra dữ liệu, tính lương,
 * xem chi tiết, lưu và chốt bảng lương. Chốt kỳ chặn tính lại; chỉ Admin
 * được mở lại kỳ đã chốt. Dữ liệu đầu vào hiện là dữ liệu mẫu trong bộ nhớ.
 */
public class PayrollFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Mã NV", "Họ tên", "Lương cơ bản", "Ngày công", "Tăng ca (giờ)",
        "Phụ cấp", "Thưởng", "Khấu trừ", "Thực lĩnh", "Trạng thái"
    };

    private static final String[] DETAIL_COLUMNS = {
        "Khoản mục", "Cách tính", "Số tiền (đồng)"
    };

    private static final int MAX_ISSUE_LINES = 12;

    private static final Comparator<Object> NUMBER_COMPARATOR = (a, b) -> {
        return Double.compare(toNumber(a), toNumber(b));
    };

    /** Trạng thái của một kỳ lương trong bộ nhớ. */
    private static final class PeriodState {
        private List<PayrollResult> results;
        private PayrollSettings usedSettings;
        private boolean saved;
        private boolean closed;
    }

    private final List<PayrollInput> inputs = DemoData.payrollInputs();
    private final Map<YearMonth, PeriodState> periods = new HashMap<>();

    private JComboBox<Integer> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JTextField insuranceField;
    private JTextField taxField;
    private JLabel statusLabel;

    private JTable payrollTable;
    private DefaultTableModel tableModel;

    private JButton selectPeriodButton;
    private JButton calculateButton;
    private JButton checkButton;
    private JButton detailButton;
    private JButton saveButton;
    private JButton finalizeButton;
    private JButton reopenButton;
    private JButton refreshButton;
    private JButton backButton;

    public PayrollFrame() {
        setTitle("Quản lý tiền lương");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1150, 700);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        loadPeriod();
    }

    // ------------------------------------------------------------------
    // Dựng giao diện
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("QUẢN LÝ TIỀN LƯƠNG");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Kiểm tra dữ liệu, tính, lưu và chốt bảng lương theo kỳ"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Kỳ lương và cấu hình tỷ lệ
        monthCombo = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthCombo.addItem(month);
        }

        yearCombo = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 2; year <= currentYear + 1; year++) {
            yearCombo.addItem(year);
        }

        monthCombo.setSelectedItem(LocalDate.now().getMonthValue());
        yearCombo.setSelectedItem(currentYear);

        monthCombo.setPreferredSize(new Dimension(65, 24));
        monthCombo.setMinimumSize(new Dimension(65, 24));
        yearCombo.setPreferredSize(new Dimension(85, 24));
        yearCombo.setMinimumSize(new Dimension(85, 24));

        insuranceField = new JTextField("0", 6);
        taxField = new JTextField("0", 6);
        insuranceField.setToolTipText(
            "Phần trăm trên lương cơ bản, ví dụ 10.5; nhập 0 nếu không áp dụng"
        );
        taxField.setToolTipText(
            "Phần trăm trên (tổng thu nhập - bảo hiểm); nhập 0 nếu không áp dụng"
        );

        selectPeriodButton = new JButton("Chọn kỳ lương");

        JPanel periodPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        periodPanel.setBorder(
            BorderFactory.createTitledBorder("Kỳ lương và cấu hình tỷ lệ")
        );
        periodPanel.add(new JLabel("Tháng:"));
        periodPanel.add(monthCombo);
        periodPanel.add(new JLabel("Năm:"));
        periodPanel.add(yearCombo);
        periodPanel.add(selectPeriodButton);
        periodPanel.add(Box.createHorizontalStrut(16));
        periodPanel.add(new JLabel("Tỷ lệ bảo hiểm (%):"));
        periodPanel.add(insuranceField);
        periodPanel.add(new JLabel("Tỷ lệ thuế thu nhập (%):"));
        periodPanel.add(taxField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.add(periodPanel, BorderLayout.NORTH);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        // Bảng
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        payrollTable = new JTable(tableModel);
        payrollTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableStyler.apply(
            payrollTable,
            new int[] {70, 140, 105, 80, 95, 95, 90, 100, 115, 95},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,   // Mã NV, Họ tên
                SwingConstants.RIGHT, SwingConstants.RIGHT,   // Lương, Công
                SwingConstants.RIGHT, SwingConstants.RIGHT,   // Tăng ca, Phụ cấp
                SwingConstants.RIGHT, SwingConstants.RIGHT,   // Thưởng, Khấu trừ
                SwingConstants.RIGHT, SwingConstants.CENTER   // Thực lĩnh, Trạng thái
            }
        );

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(tableModel);
        for (int column : new int[] {2, 5, 6, 7, 8}) {
            sorter.setComparator(column, MoneyUtils.COMPARATOR);
        }
        sorter.setComparator(3, NUMBER_COMPARATOR);
        sorter.setComparator(4, NUMBER_COMPARATOR);
        payrollTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(payrollTable);
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Bảng lương")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Nút thao tác
        calculateButton = new JButton("Tính lương");
        checkButton = new JButton("Kiểm tra dữ liệu");
        detailButton = new JButton("Xem chi tiết");
        saveButton = new JButton("Lưu bảng lương");
        finalizeButton = new JButton("Chốt bảng lương");
        reopenButton = new JButton("Mở lại kỳ");
        refreshButton = new JButton("Làm mới");
        backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );
        buttonPanel.add(checkButton);
        buttonPanel.add(calculateButton);
        buttonPanel.add(detailButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(finalizeButton);
        buttonPanel.add(reopenButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initEvents() {
        selectPeriodButton.addActionListener(e -> loadPeriod());
        monthCombo.addActionListener(e -> loadPeriod());
        yearCombo.addActionListener(e -> loadPeriod());

        calculateButton.addActionListener(e -> calculate());
        checkButton.addActionListener(e -> checkData());
        detailButton.addActionListener(e -> showDetail());
        saveButton.addActionListener(e -> savePayroll());
        finalizeButton.addActionListener(e -> finalizePayroll());
        reopenButton.addActionListener(e -> reopenPeriod());
        backButton.addActionListener(e -> dispose());

        refreshButton.addActionListener(e -> {
            monthCombo.setSelectedItem(LocalDate.now().getMonthValue());
            yearCombo.setSelectedItem(LocalDate.now().getYear());
            loadPeriod();
            payrollTable.clearSelection();
        });

        DocumentListener rateListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshStateView();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshStateView();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshStateView();
            }
        };
        insuranceField.getDocument().addDocumentListener(rateListener);
        taxField.getDocument().addDocumentListener(rateListener);

        payrollTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
    }

    // ------------------------------------------------------------------
    // Kỳ lương và hiển thị
    // ------------------------------------------------------------------

    private YearMonth selectedPeriod() {
        return YearMonth.of(
            (Integer) yearCombo.getSelectedItem(),
            (Integer) monthCombo.getSelectedItem()
        );
    }

    private PeriodState state() {
        YearMonth period = selectedPeriod();
        PeriodState state = periods.get(period);

        if (state == null) {
            state = new PeriodState();
            periods.put(period, state);
        }

        return state;
    }

    /** Nạp kỳ đang chọn: tỷ lệ đã dùng (nếu có), bảng và trạng thái. */
    private void loadPeriod() {
        if (payrollTable == null) {
            return;
        }

        PeriodState state = state();

        if (state.usedSettings != null) {
            insuranceField.setText(PayrollSettings.toPercentText(
                state.usedSettings.getInsuranceRate()
            ));
            taxField.setText(PayrollSettings.toPercentText(
                state.usedSettings.getTaxRate()
            ));
        }

        refreshTable();
        refreshStateView();
    }

    private void refreshTable() {
        PeriodState state = state();

        payrollTable.clearSelection();
        tableModel.setRowCount(0);

        String status = state.closed
            ? "Đã chốt"
            : state.results != null ? "Đã tính" : "Chưa tính";

        for (int i = 0; i < inputs.size(); i++) {
            PayrollInput in = inputs.get(i);
            PayrollResult result = state.results == null
                ? null : state.results.get(i);

            tableModel.addRow(new Object[]{
                in.getEmployeeCode(),
                in.getEmployeeName(),
                MoneyUtils.format(in.getBaseSalary()),
                formatDecimal(in.getPaidWorkdays()),
                formatDecimal(in.getOvertimeTotal()),
                MoneyUtils.format(in.getAllowance()),
                MoneyUtils.format(in.getRewardAmount()),
                result == null
                    ? "" : MoneyUtils.format(result.getTotalDeduction()),
                result == null
                    ? "" : MoneyUtils.format(result.getNetSalary()),
                status
            });
        }
    }

    /** Cập nhật dòng trạng thái, ô tỷ lệ và các nút theo trạng thái kỳ. */
    private void refreshStateView() {
        if (statusLabel == null || calculateButton == null) {
            return;
        }

        PeriodState state = state();
        YearMonth period = selectedPeriod();

        StringBuilder text = new StringBuilder();
        text.append(String.format(
            "Kỳ %02d/%d  |  %s  |  %s  |  %s",
            period.getMonthValue(), period.getYear(),
            state.closed ? "Đã chốt" : "Đang mở",
            state.results == null ? "Chưa tính" : "Đã tính",
            state.saved ? "Đã lưu" : "Chưa lưu"
        ));

        if (isStale(state)) {
            text.append("  |  Tỷ lệ đã thay đổi, cần tính lại");
        }

        statusLabel.setText(text.toString());

        insuranceField.setEditable(!state.closed);
        taxField.setEditable(!state.closed);

        updateButtons();
    }

    /** Kết quả đã tính nhưng tỷ lệ trên form không còn khớp tỷ lệ đã dùng. */
    private boolean isStale(PeriodState state) {
        if (state.results == null) {
            return false;
        }

        PayrollSettings current = readSettingsQuietly();

        return current == null || !current.sameRatesAs(state.usedSettings);
    }

    private void updateButtons() {
        if (calculateButton == null) {
            return;
        }

        PeriodState state = state();
        boolean hasResults = state.results != null;
        boolean stale = isStale(state);
        boolean isAdmin = Session.getRole() == Role.ADMIN;

        calculateButton.setEnabled(!state.closed);
        detailButton.setEnabled(
            hasResults && payrollTable.getSelectedRow() >= 0
        );
        saveButton.setEnabled(
            hasResults && !state.closed && !state.saved && !stale
        );
        finalizeButton.setEnabled(
            hasResults && state.saved && !state.closed && !stale
        );
        reopenButton.setEnabled(state.closed && isAdmin);
        reopenButton.setToolTipText(
            state.closed && !isAdmin
                ? "Chỉ Admin được mở lại kỳ lương đã chốt" : null
        );
    }

    // ------------------------------------------------------------------
    // Thao tác
    // ------------------------------------------------------------------

    private void calculate() {
        PeriodState state = state();

        if (state.closed) {
            Dialogs.warn(
                this,
                "Kỳ lương đã chốt, không thể tính lại.\n"
                + "Chỉ Admin được mở lại kỳ để điều chỉnh."
            );
            return;
        }

        PayrollSettings settings = readSettings();

        if (settings == null) {
            return;
        }

        List<PayrollIssue> issues =
            PayrollCalculator.check(inputs, settings, null);

        if (PayrollCalculator.hasError(issues)) {
            Dialogs.warn(
                this,
                "Chưa thể tính lương, cần xử lý các lỗi sau:\n\n"
                + describe(issues, true)
            );
            return;
        }

        if (!issues.isEmpty() && !Dialogs.confirm(
                this,
                "Dữ liệu có cảnh báo:\n\n" + describe(issues, false)
                + "\n\nVẫn tiếp tục tính lương?",
                "Xác nhận tính lương")) {
            return;
        }

        if (state.results != null && !Dialogs.confirm(
                this,
                "Kỳ này đã có kết quả tính lương.\n"
                + "Tính lại sẽ ghi đè kết quả cũ và cần lưu lại. Tiếp tục?",
                "Xác nhận tính lại")) {
            return;
        }

        state.results = PayrollCalculator.calculateAll(inputs, settings);
        state.usedSettings = settings;
        state.saved = false;

        refreshTable();
        refreshStateView();

        List<PayrollIssue> after =
            PayrollCalculator.check(inputs, settings, state.results);

        String message = "Đã tính lương cho " + inputs.size()
            + " nhân viên.\nTổng thực lĩnh: "
            + MoneyUtils.format(PayrollCalculator.totalNet(state.results))
            + " đồng.\nBấm \"Lưu bảng lương\" để lưu kết quả.";

        if (PayrollCalculator.hasError(after)) {
            Dialogs.warn(
                this,
                message + "\n\nCần xử lý trước khi lưu:\n\n"
                + describe(after, true)
            );
        } else {
            Dialogs.info(this, message);
        }
    }

    private void checkData() {
        PayrollSettings settings = readSettings();

        if (settings == null) {
            return;
        }

        PeriodState state = state();
        List<PayrollResult> results = isStale(state) ? null : state.results;
        List<PayrollIssue> issues =
            PayrollCalculator.check(inputs, settings, results);

        if (issues.isEmpty()) {
            Dialogs.info(
                this, "Dữ liệu hợp lệ, không có vấn đề cần xử lý."
            );
        } else if (PayrollCalculator.hasError(issues)) {
            Dialogs.warn(this, describe(issues, false));
        } else {
            Dialogs.info(this, describe(issues, false));
        }
    }

    private void showDetail() {
        PeriodState state = state();
        int viewRow = payrollTable.getSelectedRow();

        if (state.results == null || viewRow < 0) {
            Dialogs.warn(
                this, "Vui lòng tính lương và chọn một nhân viên để xem."
            );
            return;
        }

        PayrollResult r = state.results.get(
            payrollTable.convertRowIndexToModel(viewRow)
        );
        PayrollInput in = r.getInput();
        PayrollSettings used = state.usedSettings;

        String insurancePercent =
            PayrollSettings.toPercentText(used.getInsuranceRate());
        String taxPercent =
            PayrollSettings.toPercentText(used.getTaxRate());

        Object[][] rows = {
            {"Lương cơ bản", "Theo chức vụ: " + in.getPosition(),
                MoneyUtils.format(in.getBaseSalary())},
            {"Số công tính lương",
                formatDecimal(in.getPaidWorkdays()) + " công "
                + "(công làm việc + nghỉ phép năm; công chuẩn "
                + PayrollSettings.STANDARD_WORKDAYS + ")", ""},
            {"Lương theo công", "Lương cơ bản / 22 x số công",
                MoneyUtils.format(r.getSalaryByAttendance())},
            {"Phụ cấp", "Cố định theo chức vụ",
                MoneyUtils.format(in.getAllowance())},
            {"Thưởng", "Quyết định khen thưởng đã duyệt",
                MoneyUtils.format(in.getRewardAmount())},
            {"Tiền tăng ca", "Lương cơ bản / (22 x 8) x giờ x hệ số",
                MoneyUtils.format(r.getOvertimeAmount())},
            {"   Giờ tăng ca",
                formatDecimal(in.getOvertimeWeekday()) + "h ngày thường (150%), "
                + formatDecimal(in.getOvertimeWeekend()) + "h cuối tuần (200%), "
                + formatDecimal(in.getOvertimeHoliday()) + "h ngày lễ (300%)",
                ""},
            {"Tổng thu nhập",
                "Lương theo công + phụ cấp + thưởng + tăng ca",
                MoneyUtils.format(r.getGrossSalary())},
            {"Trừ: Bảo hiểm",
                insurancePercent + "% x lương cơ bản",
                MoneyUtils.format(r.getInsuranceAmount())},
            {"Trừ: Thuế thu nhập",
                taxPercent + "% x (tổng thu nhập - bảo hiểm)",
                MoneyUtils.format(r.getIncomeTaxAmount())},
            {"Trừ: Phạt kỷ luật", "Quyết định kỷ luật đã duyệt có số tiền",
                MoneyUtils.format(in.getDisciplineAmount())},
            {"THỰC LĨNH", "Tổng thu nhập - tổng khấu trừ",
                MoneyUtils.format(r.getNetSalary())}
        };

        DefaultTableModel detailModel =
            new DefaultTableModel(rows, DETAIL_COLUMNS) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

        JTable detailTable = new JTable(detailModel);
        TableStyler.apply(
            detailTable,
            new int[] {150, 520, 130},
            new int[] {
                SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.RIGHT
            }
        );

        JButton closeButton = new JButton("Đóng");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeButton);

        JDialog dialog = new JDialog(
            this,
            "Chi tiết lương " + r.getEmployeeCode() + " - "
            + r.getEmployeeName(),
            true
        );
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(12, 14, 12, 14));
        content.add(new JScrollPane(detailTable), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setSize(900, 400);
        dialog.setLocationRelativeTo(this);
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void savePayroll() {
        PeriodState state = state();

        List<PayrollIssue> issues = PayrollCalculator.check(
            inputs, state.usedSettings, state.results
        );

        if (PayrollCalculator.hasError(issues)) {
            Dialogs.warn(
                this,
                "Chưa thể lưu bảng lương, cần xử lý các lỗi sau:\n\n"
                + describe(issues, true)
            );
            return;
        }

        state.saved = true;
        refreshStateView();

        Dialogs.info(
            this,
            "Đã lưu bảng lương trong bộ nhớ.\n"
            + "Dữ liệu chưa được ghi vào cơ sở dữ liệu."
        );
    }

    private void finalizePayroll() {
        PeriodState state = state();

        List<PayrollIssue> issues = PayrollCalculator.check(
            inputs, state.usedSettings, state.results
        );

        if (PayrollCalculator.hasError(issues)) {
            Dialogs.warn(
                this,
                "Chưa thể chốt bảng lương, cần xử lý các lỗi sau:\n\n"
                + describe(issues, true)
            );
            return;
        }

        YearMonth period = selectedPeriod();

        if (!Dialogs.confirm(
                this,
                String.format(
                    "Chốt bảng lương kỳ %02d/%d?\n"
                    + "Sau khi chốt không thể tính lại, không sửa được công "
                    + "và quyết định khen thưởng/kỷ luật của kỳ này.\n"
                    + "Chỉ Admin được mở lại kỳ.",
                    period.getMonthValue(), period.getYear()
                ),
                "Xác nhận chốt bảng lương")) {
            return;
        }

        state.closed = true;
        refreshTable();
        refreshStateView();

        Dialogs.info(this, "Đã chốt bảng lương kỳ " + periodText() + ".");
    }

    private void reopenPeriod() {
        if (Session.getRole() != Role.ADMIN) {
            Dialogs.warn(this, "Chỉ Admin được mở lại kỳ lương đã chốt.");
            return;
        }

        if (!Dialogs.confirm(
                this,
                "Mở lại kỳ " + periodText() + " để điều chỉnh?\n"
                + "Bảng lương cần được lưu và chốt lại sau khi chỉnh.",
                "Xác nhận mở lại kỳ")) {
            return;
        }

        PeriodState state = state();
        state.closed = false;
        state.saved = false;

        refreshTable();
        refreshStateView();
    }

    // ------------------------------------------------------------------
    // Hỗ trợ
    // ------------------------------------------------------------------

    /** Đọc tỷ lệ trên form; hiện thông báo và trả về null nếu sai. */
    private PayrollSettings readSettings() {
        try {
            return PayrollSettings.fromPercentText(
                insuranceField.getText(), taxField.getText()
            );
        } catch (IllegalArgumentException ex) {
            Dialogs.warn(this, ex.getMessage());
            return null;
        }
    }

    /** Như readSettings nhưng không hiện thông báo. */
    private PayrollSettings readSettingsQuietly() {
        try {
            return PayrollSettings.fromPercentText(
                insuranceField.getText(), taxField.getText()
            );
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String periodText() {
        YearMonth period = selectedPeriod();
        return String.format(
            "%02d/%d", period.getMonthValue(), period.getYear()
        );
    }

    /** Liệt kê lỗi và cảnh báo; rút gọn khi quá dài. */
    private static String describe(
            List<PayrollIssue> issues, boolean errorsOnly) {

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        int errorCount = 0;
        int warningCount = 0;
        int lines = 0;
        int omitted = 0;

        for (PayrollIssue issue : issues) {
            if (!issue.isError() && errorsOnly) {
                continue;
            }

            if (lines >= MAX_ISSUE_LINES) {
                omitted++;
                continue;
            }

            lines++;
            if (issue.isError()) {
                errorCount++;
                errors.append(" - ").append(issue.getMessage()).append("\n");
            } else {
                warningCount++;
                warnings.append(" - ").append(issue.getMessage())
                    .append("\n");
            }
        }

        StringBuilder text = new StringBuilder();

        if (errorCount > 0) {
            text.append("Lỗi (cần xử lý):\n").append(errors);
        }

        if (warningCount > 0) {
            if (errorCount > 0) {
                text.append("\n");
            }
            text.append("Cảnh báo:\n").append(warnings);
        }

        if (omitted > 0) {
            text.append("... và ").append(omitted).append(" mục khác.\n");
        }

        return text.toString().trim();
    }

    private static String formatDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static double toNumber(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
