package com.hrpayroll.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.EmployeeCodeUtils;
import com.hrpayroll.util.TableStyler;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class AttendanceFrame extends JFrame {

    private JComboBox<Integer> monthCombo;
    private JComboBox<Integer> yearCombo;

    private JTextField employeeCodeField;
    private JTextField employeeNameField;
    private JTextField dateField;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JComboBox<String> statusCombo;

    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton addButton;
    private JButton saveButton;
    private JButton updateButton;
    private JButton deleteButton;

    private boolean addingNew = false;

    private static final String[] COLUMNS = {
        "Mã NV", "Họ tên", "Ngày", "Giờ vào",
        "Giờ ra", "Trạng thái", "Số giờ"
    };

    public AttendanceFrame() {
        setTitle("Quản lý chấm công");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        applyPeriodFilter();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("QUẢN LÝ CHẤM CÔNG");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel subtitleLabel = new JLabel(
            "Theo dõi ngày công và thời gian làm việc"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Bộ lọc kỳ chấm công
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

        JButton filterButton = new JButton("Lọc kỳ");
        JButton refreshButton = new JButton("Làm mới");

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
        filterPanel.add(refreshButton);

        // Form nhập liệu
        employeeCodeField = new JTextField(12);
        employeeNameField = new JTextField(16);
        dateField = new JTextField(12);
        checkInField = new JTextField(8);
        checkOutField = new JTextField(8);

        statusCombo = new JComboBox<>(new String[]{
            "Có mặt",
            "Đi muộn",
            "Vắng mặt",
            "Nghỉ phép"
        });

        employeeCodeField.setToolTipText("Ví dụ: NV001");
        dateField.setToolTipText("Định dạng " + DateUtils.DISPLAY_PATTERN);
        checkInField.setToolTipText("Định dạng HH:mm");
        checkOutField.setToolTipText("Định dạng HH:mm");

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
            new int[] {90, 170, 120, 100, 100, 120, 90},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,   // Mã NV, Họ tên
                SwingConstants.CENTER,                        // Ngày
                SwingConstants.CENTER, SwingConstants.CENTER, // Giờ vào, Giờ ra
                SwingConstants.CENTER,                        // Trạng thái
                SwingConstants.RIGHT                          // Số giờ
            }
        );

        sorter = new TableRowSorter<>(tableModel);
        // Cột ngày dạng dd/MM/yyyy phải sắp xếp theo thời gian, không theo chuỗi.
        sorter.setComparator(2, DateUtils.COMPARATOR);
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
        addButton = new JButton("Thêm mới");
        saveButton = new JButton("Lưu");
        updateButton = new JButton("Cập nhật");
        deleteButton = new JButton("Xóa bản ghi");
        JButton backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );

        buttonPanel.add(addButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.setEnabled(false);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
        setFormEditable(false, false);

        // Dữ liệu minh họa trong bộ nhớ.
        // Ngày sử dụng định dạng dd/MM/yyyy.
        tableModel.addRow(new Object[]{
            "NV001", "Nguyễn Văn An",
            DateUtils.format(LocalDate.now().withDayOfMonth(1)),
            "08:00", "17:00", "Có mặt", "8.00"
        });

        tableModel.addRow(new Object[]{
            "NV002", "Trần Thị Bình",
            DateUtils.format(LocalDate.now().withDayOfMonth(2)),
            "08:15", "17:00", "Đi muộn", "7.75"
        });
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

    private void initEvents() {
        addButton.addActionListener(e -> {
            if (!confirmDiscardCurrentInput()) {
                return;
            }

            attendanceTable.clearSelection();
            addingNew = true;
            clearForm();

            setFormEditable(true, true);
            saveButton.setEnabled(true);
            updateButton.setEnabled(false);
            deleteButton.setEnabled(false);

            dateField.setText(DateUtils.format(LocalDate.now()));
            employeeCodeField.requestFocusInWindow();
        });

        saveButton.addActionListener(e -> saveAttendance());
        updateButton.addActionListener(e -> updateAttendance());
        deleteButton.addActionListener(e -> deleteAttendance());

        JButton filterButton = findButton("Lọc kỳ");
        if (filterButton != null) {
            filterButton.addActionListener(e -> applyPeriodFilter());
        }

        JButton refreshButton = findButton("Làm mới");
        if (refreshButton != null) {
            refreshButton.addActionListener(e -> {
                monthCombo.setSelectedItem(LocalDate.now().getMonthValue());
                yearCombo.setSelectedItem(LocalDate.now().getYear());

                applyPeriodFilter();
                attendanceTable.clearSelection();
                clearForm();

                addingNew = false;
                setFormEditable(false, false);
                saveButton.setEnabled(false);
                updateButton.setEnabled(false);
                deleteButton.setEnabled(false);
            });
        }

        monthCombo.addActionListener(e -> applyPeriodFilter());
        yearCombo.addActionListener(e -> applyPeriodFilter());

        attendanceTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int viewRow = attendanceTable.getSelectedRow();

            if (viewRow >= 0) {
                int modelRow =
                    attendanceTable.convertRowIndexToModel(viewRow);

                addingNew = false;
                loadForm(modelRow);
                setFormEditable(true, false);
                saveButton.setEnabled(false);
                updateButton.setEnabled(true);
                deleteButton.setEnabled(true);
            } else if (!addingNew) {
                clearForm();
                setFormEditable(false, false);
                saveButton.setEnabled(false);
                updateButton.setEnabled(false);
                deleteButton.setEnabled(false);
            }
        });

        JButton backButton = findButton("Quay lại");
        if (backButton != null) {
            backButton.addActionListener(e -> dispose());
        }

        getRootPane().setDefaultButton(saveButton);
    }

    private JButton findButton(String text) {
        return findButtonRecursively(getContentPane(), text);
    }

    private JButton findButtonRecursively(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton
                    && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }

            if (component instanceof Container) {
                JButton result = findButtonRecursively(
                    (Container) component, text
                );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void applyPeriodFilter() {
        if (sorter == null || monthCombo == null || yearCombo == null) {
            return;
        }

        Integer month = (Integer) monthCombo.getSelectedItem();
        Integer year = (Integer) yearCombo.getSelectedItem();

        if (month == null || year == null) {
            sorter.setRowFilter(null);
            return;
        }

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel,
                    ? extends Integer> entry) {

                try {
                    LocalDate date = DateUtils.parse(
                        String.valueOf(entry.getValue(2))
                    );
                    return date.getYear() == year
                        && date.getMonthValue() == month;
                } catch (DateTimeParseException ex) {
                    return false;
                }
            }
        });
    }

    private void saveAttendance() {
        Object[] data = readFormData();

        if (data == null) {
            return;
        }

        if (findDuplicate(
                String.valueOf(data[0]),
                String.valueOf(data[2]),
                -1) >= 0) {

            showMessage(
                "Nhân viên này đã có bản ghi trong ngày đã chọn.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        tableModel.addRow(data);
        applyPeriodFilter();

        showMessage(
            "Đã thêm bản ghi trong bộ nhớ. "
            + "Dữ liệu chưa được lưu vào cơ sở dữ liệu.",
            JOptionPane.INFORMATION_MESSAGE
        );

        clearForm();
        addingNew = false;
        setFormEditable(false, false);
        saveButton.setEnabled(false);
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

        Object[] data = readFormData();

        if (data == null) {
            return;
        }

        int modelRow =
            attendanceTable.convertRowIndexToModel(viewRow);
        Object[] oldData = new Object[tableModel.getColumnCount()];
        for (int i = 0; i < oldData.length; i++) {
            oldData[i] = tableModel.getValueAt(modelRow, i);
        }

        if (findDuplicate(
                String.valueOf(data[0]),
                String.valueOf(data[2]),
                modelRow) >= 0) {

            showMessage(
                "Nhân viên này đã có bản ghi trong ngày đã chọn.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String comparison = buildUpdateComparison(COLUMNS, oldData, data);
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

        for (int i = 0; i < data.length; i++) {
            tableModel.setValueAt(data[i], modelRow, i);
        }

        applyPeriodFilter();

        showMessage(
            "Đã cập nhật bản ghi trong bộ nhớ.",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String buildUpdateComparison(String[] columns, Object[] oldData, Object[] newData) {
        StringBuilder message = new StringBuilder("Nội dung trước và sau khi cập nhật:\n\n");
        for (int i = 0; i < columns.length; i++) {
            String before = oldData[i] == null ? "" : oldData[i].toString();
            String after = newData[i] == null ? "" : newData[i].toString();
            if (!before.equals(after)) {
                message.append(columns[i]).append(":\n")
                    .append("  Trước: ").append(before).append("\n")
                    .append("  Sau:   ").append(after).append("\n\n");
            }
        }
        if (message.toString().equals("Nội dung trước và sau khi cập nhật:\n\n")) {
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

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc muốn xóa bản ghi chấm công thử nghiệm này?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            int modelRow =
                attendanceTable.convertRowIndexToModel(viewRow);

            tableModel.removeRow(modelRow);
            clearForm();
            addingNew = false;
            setFormEditable(false, false);

            saveButton.setEnabled(false);
            updateButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
    }

    private Object[] readFormData() {
        String code = EmployeeCodeUtils.normalize(employeeCodeField.getText());
        String name = employeeNameField.getText().trim();
        String dateText = dateField.getText().trim();
        String inText = checkInField.getText().trim();
        String outText = checkOutField.getText().trim();
        String status = String.valueOf(statusCombo.getSelectedItem());

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

        try {
            LocalDate date = DateUtils.parse(dateText);
            double hours = 0.0;

            if ("Có mặt".equals(status) || "Đi muộn".equals(status)) {
                if (inText.isEmpty() || outText.isEmpty()) {
                    showMessage(
                        "Vui lòng nhập giờ vào và giờ ra.",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }

                LocalTime checkIn = LocalTime.parse(inText);
                LocalTime checkOut = LocalTime.parse(outText);

                if (!checkOut.isAfter(checkIn)) {
                    showMessage(
                        "Giờ ra phải sau giờ vào. "
                        + "Bản thử nghiệm chưa hỗ trợ ca qua đêm.",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }

                hours = Duration.between(checkIn, checkOut).toMinutes()
                    / 60.0;

                // Trừ giờ nghỉ trưa 1 tiếng trong dữ liệu thử nghiệm.
                hours = Math.max(0, hours - 1.0);
            } else {
                inText = "";
                outText = "";
            }

            return new Object[]{
                code,
                name,
                DateUtils.format(date),
                inText,
                outText,
                status,
                String.format(java.util.Locale.US, "%.2f", hours)
            };

        } catch (DateTimeParseException ex) {
            showMessage(
                "Ngày phải là ngày hợp lệ, dạng dd/MM/yyyy; "
                + "giờ phải có dạng HH:mm.",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private int findDuplicate(
            String employeeCode,
            String date,
            int excludedRow) {

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (row == excludedRow) {
                continue;
            }

            String existingCode =
                String.valueOf(tableModel.getValueAt(row, 0));
            String existingDate =
                String.valueOf(tableModel.getValueAt(row, 2));

            if (employeeCode.equalsIgnoreCase(existingCode)
                    && date.equals(existingDate)) {
                return row;
            }
        }

        return -1;
    }

    private void loadForm(int row) {
        employeeCodeField.setText(
            String.valueOf(tableModel.getValueAt(row, 0))
        );
        employeeNameField.setText(
            String.valueOf(tableModel.getValueAt(row, 1))
        );
        dateField.setText(
            String.valueOf(tableModel.getValueAt(row, 2))
        );
        checkInField.setText(
            String.valueOf(tableModel.getValueAt(row, 3))
        );
        checkOutField.setText(
            String.valueOf(tableModel.getValueAt(row, 4))
        );
        statusCombo.setSelectedItem(tableModel.getValueAt(row, 5));
    }

    private void clearForm() {
        employeeCodeField.setText("");
        employeeNameField.setText("");
        dateField.setText("");
        checkInField.setText("");
        checkOutField.setText("");
        statusCombo.setSelectedIndex(0);
    }

    private void showMessage(String message, int type) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", type);
    }
    
    /** Bật/tắt vùng nhập liệu; mã nhân viên chỉ mở khi thêm bản ghi mới. */
    private void setFormEditable(boolean editable, boolean codeEditable) {
        employeeCodeField.setEditable(editable && codeEditable);
        employeeNameField.setEditable(editable);
        dateField.setEditable(editable);
        checkInField.setEditable(editable);
        checkOutField.setEditable(editable);
        statusCombo.setEnabled(editable);
    }

    private boolean hasFormInput() {
        return !employeeCodeField.getText().trim().isEmpty()
                || !employeeNameField.getText().trim().isEmpty()
                || !dateField.getText().trim().isEmpty()
                || !checkInField.getText().trim().isEmpty()
                || !checkOutField.getText().trim().isEmpty();
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