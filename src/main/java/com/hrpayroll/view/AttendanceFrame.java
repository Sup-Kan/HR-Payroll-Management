package com.hrpayroll.view;

import com.hrpayroll.model.AttendanceRecord;
import com.hrpayroll.model.AttendanceType;
import com.hrpayroll.service.AttendanceCalculator;
import com.hrpayroll.service.AttendanceSummary;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.EmployeeCodeUtils;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AttendanceFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Mã NV", "Họ tên", "Ngày", "Giờ vào", "Giờ ra",
        "Số công", "Tăng ca (giờ)", "Trạng thái", "Xác nhận", "Ghi chú"
    };

    private static final String[] SUMMARY_COLUMNS = {
        "Mã NV", "Họ tên", "Công làm việc", "Nghỉ phép năm", "Tổng công",
        "Tăng ca (giờ)", "Nghỉ không lương", "Vắng mặt", "Chưa xác nhận"
    };

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final int MAX_NOTE_LENGTH = 255;

    private static final Comparator<Object> NUMBER_COMPARATOR =
            (a, b) -> Double.compare(toNumber(a), toNumber(b));

    /** Dữ liệu trong bộ nhớ; chỉ số trong danh sách trùng chỉ số dòng của model. */
    private final List<AttendanceRecord> records = new ArrayList<>();

    private JComboBox<Integer> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JTextField searchField;

    private JTextField employeeCodeField;
    private JTextField employeeNameField;
    private JTextField dateField;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JComboBox<AttendanceType> statusCombo;
    private JTextField noteField;
    private JLabel noticeLabel;

    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton filterButton;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton addButton;
    private JButton saveButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton confirmButton;
    private JButton summaryButton;
    private JButton backButton;

    private boolean addingNew = false;
    private boolean formEditable = false;

    public AttendanceFrame() {
        setTitle("Quản lý chấm công");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        loadSampleData();
        refreshTable();
        applyFilters();
    }

    // ------------------------------------------------------------------
    // Dựng giao diện
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("QUẢN LÝ CHẤM CÔNG");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Theo dõi ngày công và thời gian làm việc"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Kỳ chấm công và tìm kiếm
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

        searchField = new JTextField(14);
        searchField.setToolTipText("Nhập mã hoặc tên nhân viên");

        filterButton = new JButton("Lọc kỳ");
        searchButton = new JButton("Tìm kiếm");
        refreshButton = new JButton("Làm mới");

        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        filterPanel.setBorder(
            BorderFactory.createTitledBorder("Kỳ chấm công")
        );

        filterPanel.add(new JLabel("Tháng:"));
        filterPanel.add(monthCombo);
        filterPanel.add(new JLabel("Năm:"));
        filterPanel.add(yearCombo);
        filterPanel.add(filterButton);
        filterPanel.add(Box.createHorizontalStrut(16));
        filterPanel.add(new JLabel("Tìm theo mã hoặc tên:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        // Form nhập liệu
        employeeCodeField = new JTextField(12);
        employeeNameField = new JTextField(16);
        dateField = new JTextField(12);
        checkInField = new JTextField(8);
        checkOutField = new JTextField(8);
        noteField = new JTextField(30);
        statusCombo = new JComboBox<>(AttendanceType.values());

        noticeLabel = new JLabel(" ");
        noticeLabel.setForeground(new Color(160, 60, 0));

        employeeCodeField.setToolTipText(
            "Ví dụ: " + EmployeeCodeUtils.EXAMPLE
        );
        dateField.setToolTipText("Định dạng " + DateUtils.DISPLAY_PATTERN);
        checkInField.setToolTipText("Định dạng HH:mm");
        checkOutField.setToolTipText("Định dạng HH:mm");
        noteField.setToolTipText("Tối đa " + MAX_NOTE_LENGTH + " ký tự");

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Thông tin chấm công")
        );

        addFormRow(
            formPanel, 0,
            "Mã nhân viên:", employeeCodeField,
            "Họ tên:", employeeNameField
        );
        addFormRow(
            formPanel, 1,
            "Ngày (dd/MM/yyyy):", dateField,
            "Trạng thái:", statusCombo
        );
        addFormRow(
            formPanel, 2,
            "Giờ vào (HH:mm):", checkInField,
            "Giờ ra (HH:mm):", checkOutField
        );
        addWideRow(formPanel, 3, "Ghi chú:", noteField);
        addWideRow(formPanel, 4, "", noticeLabel);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(formPanel, BorderLayout.CENTER);

        // Bảng dữ liệu
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        attendanceTable = new JTable(tableModel);
        attendanceTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        // Tiêu đề, căn lề và độ rộng cột: dùng chung qua TableStyler
        TableStyler.apply(
            attendanceTable,
            new int[] {70, 140, 95, 70, 70, 80, 110, 115, 115, 135},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,   // Mã NV, Họ tên
                SwingConstants.CENTER,                        // Ngày
                SwingConstants.CENTER, SwingConstants.CENTER, // Giờ vào, ra
                SwingConstants.RIGHT, SwingConstants.RIGHT,   // Số công, Tăng ca
                SwingConstants.CENTER, SwingConstants.CENTER, // Trạng thái, Xác nhận
                SwingConstants.LEFT                           // Ghi chú
            }
        );

        sorter = new TableRowSorter<>(tableModel);
        // Ngày dd/MM/yyyy và số dạng chuỗi phải sắp xếp theo giá trị thật.
        sorter.setComparator(2, DateUtils.COMPARATOR);
        sorter.setComparator(5, NUMBER_COMPARATOR);
        sorter.setComparator(6, NUMBER_COMPARATOR);
        attendanceTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(attendanceTable);
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Danh sách chấm công")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Nút thao tác
        addButton = new JButton("Chấm công");
        saveButton = new JButton("Lưu");
        updateButton = new JButton("Cập nhật");
        deleteButton = new JButton("Xóa bản ghi");
        confirmButton = new JButton("Xác nhận công");
        summaryButton = new JButton("Xem bảng công");
        backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );

        buttonPanel.add(addButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(confirmButton);
        buttonPanel.add(summaryButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        showBlankState();
    }

    private void addFormRow(
            JPanel panel,
            int row,
            String label1,
            JComponent field1,
            String label2,
            JComponent field2) {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label1), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(label2), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        panel.add(field2, gbc);
    }

    /** Một hàng chỉ có một trường, trải rộng hết các cột còn lại. */
    private void addWideRow(
            JPanel panel, int row, String label, JComponent field) {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void initEvents() {
        addButton.addActionListener(e -> startAddingNew());
        saveButton.addActionListener(e -> saveAttendance());
        updateButton.addActionListener(e -> updateAttendance());
        deleteButton.addActionListener(e -> deleteAttendance());
        confirmButton.addActionListener(e -> confirmAttendance());
        summaryButton.addActionListener(e -> showSummary());
        backButton.addActionListener(e -> dispose());

        filterButton.addActionListener(e -> applyFilters());
        searchButton.addActionListener(e -> applyFilters());
        searchField.addActionListener(e -> applyFilters());
        monthCombo.addActionListener(e -> applyFilters());
        yearCombo.addActionListener(e -> applyFilters());

        refreshButton.addActionListener(e -> {
            monthCombo.setSelectedItem(LocalDate.now().getMonthValue());
            yearCombo.setSelectedItem(LocalDate.now().getYear());
            searchField.setText("");

            applyFilters();
            attendanceTable.clearSelection();
            showBlankState();
        });

        statusCombo.addActionListener(e -> updateTimeFieldState());

        attendanceTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int viewRow = attendanceTable.getSelectedRow();

            if (viewRow >= 0) {
                int modelRow =
                    attendanceTable.convertRowIndexToModel(viewRow);
                showRecord(records.get(modelRow));
            } else if (!addingNew) {
                showBlankState();
            }
        });

        getRootPane().setDefaultButton(saveButton);
    }

    // ------------------------------------------------------------------
    // Dữ liệu và bảng
    // ------------------------------------------------------------------

    /** Dữ liệu minh họa trong bộ nhớ, thuộc tháng hiện tại. */
    private void loadSampleData() {
        LocalDate day1 = LocalDate.now().withDayOfMonth(1);
        LocalDate day2 = LocalDate.now().withDayOfMonth(2);

        records.add(AttendanceCalculator.createRecord(
            "NV001", "Nguyễn Văn An", day1,
            LocalTime.of(8, 0), LocalTime.of(17, 0),
            AttendanceType.PRESENT, ""
        ));
        records.add(AttendanceCalculator.createRecord(
            "NV002", "Trần Thị Bình", day1,
            null, null,
            AttendanceType.PAID_LEAVE, "Nghỉ phép năm"
        ));
        records.add(AttendanceCalculator.createRecord(
            "NV001", "Nguyễn Văn An", day2,
            LocalTime.of(8, 0), LocalTime.of(19, 0),
            AttendanceType.PRESENT, "Tăng ca làm báo cáo"
        ));
        records.add(AttendanceCalculator.createRecord(
            "NV002", "Trần Thị Bình", day2,
            LocalTime.of(8, 15), LocalTime.of(17, 0),
            AttendanceType.PRESENT, "Đi muộn 15 phút"
        ));
    }

    /** Dựng lại toàn bộ bảng từ danh sách bản ghi. */
    private void refreshTable() {
        attendanceTable.clearSelection();
        tableModel.setRowCount(0);

        for (AttendanceRecord record : records) {
            tableModel.addRow(rowOf(record));
        }
    }

    private Object[] rowOf(AttendanceRecord record) {
        return new Object[]{
            record.getEmployeeCode(),
            record.getEmployeeName(),
            DateUtils.format(record.getDate()),
            formatTime(record.getCheckIn()),
            formatTime(record.getCheckOut()),
            AttendanceCalculator.format2(
                AttendanceCalculator.workDays(record)
            ),
            AttendanceCalculator.format2(record.getOvertimeHours()),
            record.getType().getLabel(),
            record.isConfirmed() ? "Đã xác nhận" : "Chưa xác nhận",
            record.getNote()
        };
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static double toNumber(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    /** Áp dụng đồng thời bộ lọc kỳ và từ khóa tìm kiếm. */
    private void applyFilters() {
        if (sorter == null || monthCombo == null || yearCombo == null) {
            return;
        }

        final Integer month = (Integer) monthCombo.getSelectedItem();
        final Integer year = (Integer) yearCombo.getSelectedItem();
        final String keyword = searchField.getText().trim()
            .toLowerCase(Locale.ROOT);

        if (month == null || year == null) {
            sorter.setRowFilter(null);
            return;
        }

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel,
                    ? extends Integer> entry) {

                AttendanceRecord record = records.get(entry.getIdentifier());

                if (record.getDate().getYear() != year
                        || record.getDate().getMonthValue() != month) {
                    return false;
                }

                if (keyword.isEmpty()) {
                    return true;
                }

                return record.getEmployeeCode().toLowerCase(Locale.ROOT)
                        .contains(keyword)
                    || record.getEmployeeName().toLowerCase(Locale.ROOT)
                        .contains(keyword);
            }
        });
    }

    // ------------------------------------------------------------------
    // Thao tác
    // ------------------------------------------------------------------

    private void startAddingNew() {
        if (!confirmDiscardCurrentInput()) {
            return;
        }

        attendanceTable.clearSelection();
        addingNew = true;
        clearForm();

        setFormEditable(true, true);
        setButtons(true, false, false);
        noticeLabel.setText(" ");

        dateField.setText(DateUtils.format(LocalDate.now()));
        employeeCodeField.requestFocusInWindow();
    }

    private void saveAttendance() {
        AttendanceRecord record = readFormData();

        if (record == null) {
            return;
        }

        if (findDuplicate(record, -1) >= 0) {
            showMessage(
                "Nhân viên này đã có bản ghi trong ngày đã chọn.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        records.add(record);
        refreshTable();

        showMessage(
            "Đã thêm bản ghi trong bộ nhớ. "
            + "Dữ liệu chưa được lưu vào cơ sở dữ liệu."
            + periodHint(record.getDate()),
            JOptionPane.INFORMATION_MESSAGE
        );

        addingNew = false;
        showBlankState();
    }

    private void updateAttendance() {
        int viewRow = attendanceTable.getSelectedRow();

        if (viewRow < 0) {
            showMessage(
                "Vui lòng chọn bản ghi cần cập nhật.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = attendanceTable.convertRowIndexToModel(viewRow);
        AttendanceRecord oldRecord = records.get(modelRow);

        if (oldRecord.isConfirmed()) {
            showMessage(
                "Bản ghi đã xác nhận công, không thể cập nhật.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        AttendanceRecord newRecord = readFormData();

        if (newRecord == null) {
            return;
        }

        if (findDuplicate(newRecord, modelRow) >= 0) {
            showMessage(
                "Nhân viên này đã có bản ghi trong ngày đã chọn.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String comparison = buildUpdateComparison(
            COLUMNS, rowOf(oldRecord), rowOf(newRecord)
        );
        int choice = JOptionPane.showConfirmDialog(
            this,
            comparison,
            "Xác nhận cập nhật chấm công",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        records.set(modelRow, newRecord);
        refreshTable();

        showMessage(
            "Đã cập nhật bản ghi trong bộ nhớ."
            + periodHint(newRecord.getDate()),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String buildUpdateComparison(
            String[] columns, Object[] oldData, Object[] newData) {

        String header = "Nội dung trước và sau khi cập nhật:\n\n";
        StringBuilder message = new StringBuilder(header);

        for (int i = 0; i < columns.length; i++) {
            String before = String.valueOf(oldData[i]);
            String after = String.valueOf(newData[i]);

            if (!before.equals(after)) {
                message.append(columns[i]).append(":\n")
                    .append("  Trước: ").append(before).append("\n")
                    .append("  Sau:   ").append(after).append("\n\n");
            }
        }

        if (message.toString().equals(header)) {
            message.append("Không có trường nào thay đổi.\n\n");
        }

        message.append("Bạn có xác nhận cập nhật các thay đổi này không?");
        return message.toString();
    }

    private void deleteAttendance() {
        int viewRow = attendanceTable.getSelectedRow();

        if (viewRow < 0) {
            return;
        }

        int modelRow = attendanceTable.convertRowIndexToModel(viewRow);

        if (records.get(modelRow).isConfirmed()) {
            showMessage(
                "Bản ghi đã xác nhận công, không thể xóa.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc muốn xóa bản ghi chấm công thử nghiệm này?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            records.remove(modelRow);
            refreshTable();
            addingNew = false;
            showBlankState();
        }
    }

    /**
     * Xác nhận công cho các bản ghi đang hiển thị (theo kỳ và từ khóa).
     * Bản ghi đã xác nhận không thể sửa hoặc xóa.
     */
    private void confirmAttendance() {
        if (attendanceTable.getRowCount() == 0) {
            showMessage(
                "Không có bản ghi nào trong danh sách đang hiển thị.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<AttendanceRecord> targets = new ArrayList<>();

        for (int viewRow = 0;
                viewRow < attendanceTable.getRowCount(); viewRow++) {
            int modelRow = attendanceTable.convertRowIndexToModel(viewRow);
            AttendanceRecord record = records.get(modelRow);

            if (!record.isConfirmed()) {
                targets.add(record);
            }
        }

        if (targets.isEmpty()) {
            showMessage(
                "Tất cả bản ghi đang hiển thị đã được xác nhận.",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Xác nhận công cho " + targets.size()
            + " bản ghi đang hiển thị (kỳ " + periodText() + ")?\n"
            + "Sau khi xác nhận, các bản ghi này không thể sửa hoặc xóa.",
            "Xác nhận công",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        for (AttendanceRecord record : targets) {
            record.confirm();
        }

        refreshTable();
        addingNew = false;
        showBlankState();

        showMessage(
            "Đã xác nhận công cho " + targets.size() + " bản ghi.",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /** Bảng công tổng hợp theo nhân viên của kỳ đang chọn. */
    private void showSummary() {
        int month = (Integer) monthCombo.getSelectedItem();
        int year = (Integer) yearCombo.getSelectedItem();

        List<AttendanceRecord> period = new ArrayList<>();
        for (AttendanceRecord record : records) {
            if (record.getDate().getYear() == year
                    && record.getDate().getMonthValue() == month) {
                period.add(record);
            }
        }

        if (period.isEmpty()) {
            showMessage(
                "Kỳ " + periodText() + " chưa có dữ liệu chấm công.",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        DefaultTableModel summaryModel =
            new DefaultTableModel(SUMMARY_COLUMNS, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

        int unconfirmedTotal = 0;

        for (AttendanceSummary.Row row : AttendanceSummary.summarize(period)) {
            summaryModel.addRow(new Object[]{
                row.getEmployeeCode(),
                row.getEmployeeName(),
                AttendanceCalculator.format2(row.getPresentDays()),
                row.getPaidLeaveDays(),
                AttendanceCalculator.format2(row.getTotalDays()),
                AttendanceCalculator.format2(row.getOvertimeHours()),
                row.getUnpaidLeaveDays(),
                row.getAbsentDays(),
                row.getUnconfirmedRecords()
            });
            unconfirmedTotal += row.getUnconfirmedRecords();
        }

        JTable summaryTable = new JTable(summaryModel);
        TableStyler.apply(
            summaryTable,
            new int[] {70, 140, 115, 120, 90, 110, 135, 90, 120},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,
                SwingConstants.RIGHT, SwingConstants.CENTER,
                SwingConstants.RIGHT, SwingConstants.RIGHT,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER
            }
        );

        String footer = "Công chuẩn của kỳ: "
            + AttendanceCalculator.STANDARD_WORKDAYS_PER_MONTH + " ngày.";
        if (unconfirmedTotal > 0) {
            footer += "  Còn " + unconfirmedTotal
                + " bản ghi chưa xác nhận, cần xác nhận trước khi tính lương.";
        }

        JLabel footerLabel = new JLabel(footer);
        footerLabel.setBorder(new EmptyBorder(8, 2, 8, 2));

        JButton closeButton = new JButton("Đóng");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(footerLabel, BorderLayout.CENTER);
        bottomPanel.add(closeButton, BorderLayout.EAST);

        JDialog dialog = new JDialog(
            this, "Bảng công kỳ " + periodText(), true
        );
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(12, 14, 12, 14));
        content.add(new JScrollPane(summaryTable), BorderLayout.CENTER);
        content.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setSize(1080, 360);
        dialog.setLocationRelativeTo(this);
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Đọc và hiển thị form
    // ------------------------------------------------------------------

    /** Đọc form thành bản ghi; hiện thông báo và trả về null nếu sai. */
    private AttendanceRecord readFormData() {
        String code = EmployeeCodeUtils.normalize(employeeCodeField.getText());
        String name = employeeNameField.getText().trim();
        String dateText = dateField.getText().trim();
        String inText = checkInField.getText().trim();
        String outText = checkOutField.getText().trim();
        String note = noteField.getText().trim();
        AttendanceType type = (AttendanceType) statusCombo.getSelectedItem();

        if (code.isEmpty() || name.isEmpty() || dateText.isEmpty()) {
            showMessage(
                "Mã nhân viên, họ tên và ngày là bắt buộc.",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        if (!EmployeeCodeUtils.isValid(code)) {
            showMessage(
                EmployeeCodeUtils.INVALID_MESSAGE,
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        if (note.length() > MAX_NOTE_LENGTH) {
            showMessage(
                "Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự.",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        try {
            LocalDate date = DateUtils.parse(dateText);
            LocalTime checkIn = null;
            LocalTime checkOut = null;

            if (type.requiresTime()) {
                if (inText.isEmpty() || outText.isEmpty()) {
                    showMessage(
                        "Vui lòng nhập giờ vào và giờ ra.",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }

                checkIn = LocalTime.parse(inText, TIME_FORMAT);
                checkOut = LocalTime.parse(outText, TIME_FORMAT);

                if (!checkOut.isAfter(checkIn)) {
                    showMessage(
                        "Giờ ra phải sau giờ vào. "
                        + "Bản thử nghiệm chưa hỗ trợ ca qua đêm.",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }

                if (AttendanceCalculator.workedHours(checkIn, checkOut) <= 0) {
                    showMessage(
                        "Khoảng thời gian này nằm hoàn toàn trong giờ nghỉ "
                        + "trưa (12:00 - 13:00), không có giờ làm.",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }
            }

            return AttendanceCalculator.createRecord(
                code, name, date, checkIn, checkOut, type, note
            );

        } catch (DateTimeParseException ex) {
            showMessage(
                "Ngày phải là ngày hợp lệ, dạng dd/MM/yyyy; "
                + "giờ phải có dạng HH:mm.",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private int findDuplicate(AttendanceRecord candidate, int excludedIndex) {
        for (int i = 0; i < records.size(); i++) {
            if (i == excludedIndex) {
                continue;
            }

            AttendanceRecord existing = records.get(i);

            if (existing.getEmployeeCode().equalsIgnoreCase(
                    candidate.getEmployeeCode())
                    && existing.getDate().equals(candidate.getDate())) {
                return i;
            }
        }

        return -1;
    }

    /** Hiện bản ghi lên form; bản ghi đã xác nhận chỉ được xem. */
    private void showRecord(AttendanceRecord record) {
        addingNew = false;
        loadForm(record);

        if (record.isConfirmed()) {
            setFormEditable(false, false);
            setButtons(false, false, false);
            noticeLabel.setText(
                "Bản ghi đã xác nhận công, không thể sửa hoặc xóa."
            );
        } else {
            setFormEditable(true, false);
            setButtons(false, true, true);
            noticeLabel.setText(" ");
        }
    }

    private void showBlankState() {
        clearForm();
        setFormEditable(false, false);
        setButtons(false, false, false);
        noticeLabel.setText(" ");
    }

    private void setButtons(boolean save, boolean update, boolean delete) {
        saveButton.setEnabled(save);
        updateButton.setEnabled(update);
        deleteButton.setEnabled(delete);
    }

    private void loadForm(AttendanceRecord record) {
        employeeCodeField.setText(record.getEmployeeCode());
        employeeNameField.setText(record.getEmployeeName());
        dateField.setText(DateUtils.format(record.getDate()));
        checkInField.setText(formatTime(record.getCheckIn()));
        checkOutField.setText(formatTime(record.getCheckOut()));
        statusCombo.setSelectedItem(record.getType());
        noteField.setText(record.getNote());
    }

    private void clearForm() {
        employeeCodeField.setText("");
        employeeNameField.setText("");
        dateField.setText("");
        checkInField.setText("");
        checkOutField.setText("");
        noteField.setText("");
        statusCombo.setSelectedIndex(0);
    }

    /** Bật/tắt vùng nhập liệu; mã nhân viên chỉ mở khi thêm bản ghi mới. */
    private void setFormEditable(boolean editable, boolean codeEditable) {
        formEditable = editable;

        employeeCodeField.setEditable(editable && codeEditable);
        employeeNameField.setEditable(editable);
        dateField.setEditable(editable);
        noteField.setEditable(editable);
        statusCombo.setEnabled(editable);

        updateTimeFieldState();
    }

    /** Giờ vào/ra chỉ nhập được khi loại chấm công là "Có mặt". */
    private void updateTimeFieldState() {
        AttendanceType type = (AttendanceType) statusCombo.getSelectedItem();
        boolean needsTime = type != null && type.requiresTime();

        checkInField.setEditable(formEditable && needsTime);
        checkOutField.setEditable(formEditable && needsTime);

        if (formEditable && !needsTime) {
            checkInField.setText("");
            checkOutField.setText("");
        }
    }

    private String periodText() {
        return String.format(
            "%02d/%d",
            (Integer) monthCombo.getSelectedItem(),
            (Integer) yearCombo.getSelectedItem()
        );
    }

    /** Nhắc người dùng nếu bản ghi vừa lưu nằm ngoài kỳ đang lọc. */
    private String periodHint(LocalDate date) {
        if (date.getMonthValue() == (Integer) monthCombo.getSelectedItem()
                && date.getYear() == (Integer) yearCombo.getSelectedItem()) {
            return "";
        }

        return String.format(
            "\nBản ghi thuộc kỳ %02d/%d nên không hiển thị trong kỳ đang lọc.",
            date.getMonthValue(), date.getYear()
        );
    }

    private void showMessage(String message, int type) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", type);
    }

    private boolean hasFormInput() {
        return !employeeCodeField.getText().trim().isEmpty()
                || !employeeNameField.getText().trim().isEmpty()
                || !dateField.getText().trim().isEmpty()
                || !checkInField.getText().trim().isEmpty()
                || !checkOutField.getText().trim().isEmpty()
                || !noteField.getText().trim().isEmpty();
    }

    private boolean confirmDiscardCurrentInput() {
        if (!hasFormInput()) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Thông tin đang hiển thị có thể chưa được lưu.\n"
            + "Bạn có muốn bỏ thông tin này và tạo bản ghi mới không?",
            "Xác nhận tạo mới",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        return choice == JOptionPane.YES_OPTION;
    }
}
