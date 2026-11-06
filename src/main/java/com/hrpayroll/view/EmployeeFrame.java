package com.hrpayroll.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.EmployeeCodeUtils;
import com.hrpayroll.util.TableStyler;
import com.hrpayroll.dao.EmployeeDAO;
import java.sql.SQLException;

public class EmployeeFrame extends JFrame {

    private JTextField codeField;
    private JTextField nameField;
    private JTextField dobField;
    private JComboBox<String> genderCombo;
    private JTextField phoneField;
    private JTextField addressField;
    private JComboBox<String> departmentCombo;
    private JComboBox<String> positionCombo;
    private JTextField startDateField;
    private JComboBox<String> statusCombo;
    private JTextField searchField;

    private JTable employeeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton addButton;
    private JButton saveButton;
    private JButton updateButton;
    private JButton deleteButton;

    private boolean addingNew = false;
    /** true khi đang nạp dữ liệu vào form: không tự lọc lại danh sách chức vụ. */
    private boolean loadingForm = false;

    private static final String[] COLUMNS = {
        "Mã NV", "Họ tên", "Ngày sinh", "Giới tính",
        "SĐT", "Địa chỉ", "Phòng ban", "Chức vụ",
        "Ngày vào làm", "Trạng thái"
    };

    public EmployeeFrame() {
        setTitle("Quản lý nhân viên");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1150, 700);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        loadLookups();
        loadEmployeesFromDatabase();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("QUẢN LÝ NHÂN VIÊN");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Quản lý hồ sơ và thông tin nhân sự"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Khu vực thông tin nhân viên
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Thông tin nhân viên")
        );

        codeField = new JTextField(14);
        nameField = new JTextField(14);
        dobField = new JTextField(14);
        genderCombo = new JComboBox<>(
            new String[]{"Nam", "Nữ", "Khác"}
        );
        phoneField = new JTextField(14);
        addressField = new JTextField(14);

        // Danh sách phòng ban/chức vụ được nạp từ database (loadLookups).
        departmentCombo = new JComboBox<>(new String[]{"Chưa chọn"});
        positionCombo = new JComboBox<>(new String[]{"Chưa chọn"});

        startDateField = new JTextField(14);
        statusCombo = new JComboBox<>(
            new String[]{"Đang làm việc", "Nghỉ việc"}
        );

        codeField.setToolTipText("Hệ thống tự cấp mã theo thứ tự, ví dụ "
            + EmployeeCodeUtils.EXAMPLE);
        dobField.setToolTipText("Định dạng " + DateUtils.DISPLAY_PATTERN);
        startDateField.setToolTipText("Định dạng " + DateUtils.DISPLAY_PATTERN);

        addFormRow(formPanel, 0, "Mã nhân viên (tự động):", codeField,
                   "Họ và tên:", nameField);
        addFormRow(formPanel, 1, "Ngày sinh (dd/MM/yyyy):", dobField,
                   "Giới tính:", genderCombo);
        addFormRow(formPanel, 2, "Số điện thoại:", phoneField,
                   "Địa chỉ:", addressField);
        addFormRow(formPanel, 3, "Phòng ban:", departmentCombo,
                   "Chức vụ:", positionCombo);
        addFormRow(formPanel, 4, "Ngày vào làm (dd/MM/yyyy):", startDateField,
                   "Trạng thái:", statusCombo);

        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.add(formPanel, BorderLayout.CENTER);

        // Khu vực tìm kiếm
        searchField = new JTextField(22);
        JButton searchButton = new JButton("Tìm kiếm");
        JButton refreshButton = new JButton("Làm mới");

        JPanel searchPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 8)
        );
        searchPanel.add(new JLabel("Tìm theo mã hoặc tên:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        // Bảng nhân viên
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        employeeTable = new JTable(tableModel);
        employeeTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        // Tiêu đề, căn lề và độ rộng cột: dùng chung qua TableStyler
        TableStyler.apply(
            employeeTable,
            new int[] {70, 150, 95, 75, 105, 160, 95, 105, 125, 120},
            new int[] {
                SwingConstants.CENTER, SwingConstants.LEFT,   // Mã NV, Họ tên
                SwingConstants.CENTER, SwingConstants.CENTER, // Ngày sinh, Giới tính
                SwingConstants.CENTER, SwingConstants.LEFT,   // SĐT, Địa chỉ
                SwingConstants.LEFT,   SwingConstants.LEFT,   // Phòng ban, Chức vụ
                SwingConstants.CENTER, SwingConstants.CENTER  // Ngày vào làm, Trạng thái
            }
        );

        sorter = new TableRowSorter<>(tableModel);
        // Cột ngày dạng dd/MM/yyyy phải sắp xếp theo thời gian, không theo chuỗi.
        sorter.setComparator(2, DateUtils.COMPARATOR); // Ngày sinh
        sorter.setComparator(8, DateUtils.COMPARATOR); // Ngày vào làm
        employeeTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(employeeTable);
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Danh sách nhân viên")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(formContainer, BorderLayout.NORTH);
        centerPanel.add(searchPanel, BorderLayout.CENTER);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.add(tableScrollPane, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
        contentPanel.add(centerPanel, BorderLayout.NORTH);
        contentPanel.add(tableContainer, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Các nút thao tác
        addButton = new JButton("Thêm mới");
        saveButton = new JButton("Lưu");
        updateButton = new JButton("Cập nhật");
        deleteButton = new JButton("Xóa / Nghỉ việc");
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

        // Dữ liệu được nạp từ database sau khi khởi tạo giao diện.
    }

    private void loadLookups() {
        try {
            for (String name : EmployeeDAO.departmentNames()) departmentCombo.addItem(name);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Không tải được danh sách phòng ban/chức vụ: " + ex.getMessage(),
                "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Nạp lại ô Chức vụ theo phòng ban đang chọn; giữ lại lựa chọn cũ nếu còn hợp lệ. */
    private void refillPositions() {
        Object previous = positionCombo.getSelectedItem();
        positionCombo.removeAllItems();
        positionCombo.addItem("Chưa chọn");

        Object dept = departmentCombo.getSelectedItem();
        if (dept != null && departmentCombo.getSelectedIndex() > 0) {
            try {
                for (String name : EmployeeDAO.positionNamesByDepartment(dept.toString())) {
                    positionCombo.addItem(name);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                    "Không tải được danh sách chức vụ: " + ex.getMessage(),
                    "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (previous != null) {
            positionCombo.setSelectedItem(previous);
        }
    }

    /** Tải lại bảng từ DB rồi chọn lại nhân viên theo mã (nếu có). */
    private void reloadAndSelect(String code) {
        loadEmployeesFromDatabase();
        int row = code == null ? -1 : findRowByCode(code);
        if (row >= 0) {
            int view = employeeTable.convertRowIndexToView(row);
            if (view >= 0) employeeTable.setRowSelectionInterval(view, view);
        }
    }

    private void loadEmployeesFromDatabase() {
        try {
            tableModel.setRowCount(0);
            for (Object[] row : EmployeeDAO.findAllRows()) tableModel.addRow(row);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Không tải được hồ sơ nhân viên từ database đang chọn: " + ex.getMessage(),
                "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
        }
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

            employeeTable.clearSelection();
            addingNew = true;
            clearForm();

            // Mã nhân viên do hệ thống cấp: chỉ hiển thị mã kế tiếp, không nhập tay.
            try {
                codeField.setText(EmployeeDAO.nextCode());
            } catch (SQLException ex) {
                showMessage("Không lấy được mã nhân viên kế tiếp: " + ex.getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                addingNew = false;
                return;
            }

            setFormEditable(true, false);
            saveButton.setEnabled(true);
            updateButton.setEnabled(false);
            deleteButton.setEnabled(false);

            nameField.requestFocusInWindow();
        });

        // Chọn phòng ban thì chỉ hiện chức vụ thuộc phòng ban đó.
        departmentCombo.addActionListener(e -> {
            if (!loadingForm) {
                refillPositions();
            }
        });

        saveButton.addActionListener(e -> saveEmployee());

        updateButton.addActionListener(e -> updateEmployee());

        deleteButton.addActionListener(e -> markAsResigned());

        employeeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = employeeTable.getSelectedRow();

                if (viewRow >= 0) {
                    int modelRow = employeeTable.convertRowIndexToModel(viewRow);
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
            }
        });

        // Nhấp vùng trống bên trong bảng sẽ bỏ chọn hàng hiện tại.
        // Nhấp bên ngoài bảng không làm thay đổi lựa chọn.
        employeeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (employeeTable.rowAtPoint(e.getPoint()) == -1) {
                    employeeTable.clearSelection();
                }
            }
        });

        searchField.addActionListener(e -> searchEmployees());

        // Lấy nút tìm kiếm và làm mới từ khu vực tìm kiếm.
        // Hai nút được gắn listener riêng trong hàm dưới.
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                attachSearchActions((JPanel) component);
            }
        }

        // Nút Quay lại chỉ đóng form nhân viên.
        for (Component component : getContentPane().getComponents()) {
            if (component instanceof JPanel) {
                attachBackAction((JPanel) component);
            }
        }
    }

    private void attachSearchActions(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;

                if ("Tìm kiếm".equals(button.getText())) {
                    button.addActionListener(e -> searchEmployees());
                } else if ("Làm mới".equals(button.getText())) {
                    button.addActionListener(e -> {
                        searchField.setText("");
                        sorter.setRowFilter(null);
                        employeeTable.clearSelection();
                        clearForm();
                        addingNew = false;
                        setFormEditable(false, false);
                        saveButton.setEnabled(false);
                        updateButton.setEnabled(false);
                        deleteButton.setEnabled(false);
                    });
                }
            } else if (component instanceof Container) {
                attachSearchActions((Container) component);
            }
        }
    }

    private void attachBackAction(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton
                    && "Quay lại".equals(((JButton) component).getText())) {

                JButton backButton = (JButton) component;

                backButton.addActionListener(e -> dispose());

            } else if (component instanceof Container) {
                attachBackAction((Container) component);
            }
        }
    }

    /**
     * Kiểm tra định dạng mã nhân viên và các ngày theo quy ước dự án.
     * Hiện thông báo và trả về false nếu có lỗi.
     */
    private boolean validateFormats() {
        if (!EmployeeCodeUtils.isValid(codeField.getText())) {
            showMessage(
                EmployeeCodeUtils.INVALID_MESSAGE,
                JOptionPane.WARNING_MESSAGE
            );
            codeField.requestFocusInWindow();
            return false;
        }

        String dob = dobField.getText().trim();
        if (!dob.isEmpty() && !DateUtils.isValid(dob)) {
            showMessage(
                "Ngày sinh phải là ngày hợp lệ, dạng "
                + DateUtils.DISPLAY_PATTERN + ".",
                JOptionPane.WARNING_MESSAGE
            );
            dobField.requestFocusInWindow();
            return false;
        }

        // hire_date là NOT NULL trong cơ sở dữ liệu nên ngày vào làm bắt buộc.
        if (!DateUtils.isValid(startDateField.getText())) {
            showMessage(
                "Ngày vào làm là bắt buộc, dạng "
                + DateUtils.DISPLAY_PATTERN + ".",
                JOptionPane.WARNING_MESSAGE
            );
            startDateField.requestFocusInWindow();
            return false;
        }

        return true;
    }

    private void saveEmployee() {
        String code = EmployeeCodeUtils.normalize(codeField.getText());
        String name = nameField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            showMessage(
                "Mã nhân viên và họ tên không được để trống.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!validateFormats()) {
            return;
        }

        EmployeeDAO.Created created;
        try {
            created = EmployeeDAO.insert(readFormData());
        } catch (SQLException ex) {
            showMessage("Không thể thêm nhân viên: " + ex.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
            return;
        }

        loadEmployeesFromDatabase();

        String message = "Đã thêm nhân viên " + created.code + " vào cơ sở dữ liệu.";
        if (created.username != null) {
            message += "\n\nĐã tạo tài khoản đăng nhập:"
                + "\n  Tên đăng nhập: " + created.username
                + "\n  Vai trò: " + created.role.getLabel()
                + "\n  Mật khẩu mẫu: " + EmployeeDAO.DEFAULT_PASSWORD
                + "\nHãy đổi mật khẩu trong Quản lý tài khoản.";
        }
        showMessage(message, JOptionPane.INFORMATION_MESSAGE);

        clearForm();
        addingNew = false;
        setFormEditable(false, false);
        saveButton.setEnabled(false);
    }

    private void updateEmployee() {
        int viewRow = employeeTable.getSelectedRow();

        if (viewRow < 0) {
            showMessage(
                "Vui lòng chọn nhân viên cần cập nhật.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String code = EmployeeCodeUtils.normalize(codeField.getText());
        String name = nameField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            showMessage(
                "Mã nhân viên và họ tên không được để trống.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!validateFormats()) {
            return;
        }

        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        Object[] data = readFormData();
        Object[] oldData = new Object[tableModel.getColumnCount()];
        for (int i = 0; i < oldData.length; i++) {
            oldData[i] = tableModel.getValueAt(modelRow, i);
        }

        String comparison = buildUpdateComparison(COLUMNS, oldData, data);
        int choice = JOptionPane.showConfirmDialog(
            this,
            comparison,
            "Xác nhận cập nhật nhân viên",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            EmployeeDAO.update(data);
        } catch (SQLException ex) {
            showMessage("Không thể cập nhật nhân viên: " + ex.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
            return;
        }

        reloadAndSelect(String.valueOf(data[0]));
        showMessage("Đã cập nhật nhân viên trong cơ sở dữ liệu.",
                    JOptionPane.INFORMATION_MESSAGE);
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

    private void markAsResigned() {
        int viewRow = employeeTable.getSelectedRow();

        if (viewRow < 0) {
            showMessage(
                "Vui lòng chọn nhân viên cần chuyển trạng thái.",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Chuyển nhân viên này sang trạng thái Nghỉ việc?\n"
            + "Hồ sơ vẫn được giữ lại.",
            "Xác nhận",
            JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            int modelRow = employeeTable.convertRowIndexToModel(viewRow);
            String code = String.valueOf(tableModel.getValueAt(modelRow, 0));

            try {
                EmployeeDAO.markResigned(code);
            } catch (SQLException ex) {
                showMessage("Không thể cập nhật trạng thái: " + ex.getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                return;
            }

            reloadAndSelect(code);
            showMessage("Đã chuyển nhân viên " + code + " sang Nghỉ việc.",
                        JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void searchEmployees() {
        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(
                RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(keyword),
                    0, 1
                )
            );
        }
    }

    private int findRowByCode(String code) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (code.equalsIgnoreCase(
                    String.valueOf(tableModel.getValueAt(i, 0)))) {
                return i;
            }
        }
        return -1;
    }

    private Object[] readFormData() {
        return new Object[]{
            EmployeeCodeUtils.normalize(codeField.getText()),
            nameField.getText().trim(),
            dobField.getText().trim(),
            genderCombo.getSelectedItem(),
            phoneField.getText().trim(),
            addressField.getText().trim(),
            departmentCombo.getSelectedItem(),
            positionCombo.getSelectedItem(),
            startDateField.getText().trim(),
            statusCombo.getSelectedItem()
        };
    }

    private void loadForm(int row) {
        loadingForm = true;
        try {
            fillForm(row);
        } finally {
            loadingForm = false;
        }
    }

    private void fillForm(int row) {
        codeField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        dobField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        genderCombo.setSelectedItem(tableModel.getValueAt(row, 3));
        phoneField.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        addressField.setText(String.valueOf(tableModel.getValueAt(row, 5)));
        departmentCombo.setSelectedItem(tableModel.getValueAt(row, 6));
        refillPositions();
        positionCombo.setSelectedItem(tableModel.getValueAt(row, 7));
        startDateField.setText(String.valueOf(tableModel.getValueAt(row, 8)));
        statusCombo.setSelectedItem(tableModel.getValueAt(row, 9));
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        dobField.setText("");
        genderCombo.setSelectedIndex(0);
        phoneField.setText("");
        addressField.setText("");
        departmentCombo.setSelectedIndex(0);
        refillPositions();
        positionCombo.setSelectedIndex(0);
        startDateField.setText("");
        statusCombo.setSelectedIndex(0);
    }

    private void showMessage(String message, int type) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", type);
    }
    
    /** Bật/tắt vùng nhập liệu; mã nhân viên chỉ mở khi thêm hồ sơ mới. */
    private void setFormEditable(boolean editable, boolean codeEditable) {
        codeField.setEditable(editable && codeEditable);
        nameField.setEditable(editable);
        dobField.setEditable(editable);
        phoneField.setEditable(editable);
        addressField.setEditable(editable);
        startDateField.setEditable(editable);
        genderCombo.setEnabled(editable);
        departmentCombo.setEnabled(editable);
        positionCombo.setEnabled(editable);
        statusCombo.setEnabled(editable);
    }

    private boolean hasFormInput() {
        return !codeField.getText().trim().isEmpty()
                || !nameField.getText().trim().isEmpty()
                || !dobField.getText().trim().isEmpty()
                || !phoneField.getText().trim().isEmpty()
                || !addressField.getText().trim().isEmpty()
                || !startDateField.getText().trim().isEmpty();
    }

    private boolean confirmDiscardCurrentInput() {
        if (!hasFormInput()) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Thông tin nhân viên đang hiển thị có thể chưa được lưu.\n"
                + "Bạn có muốn bỏ thông tin này và tạo hồ sơ mới không?",
                "Xác nhận tạo mới",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return choice == JOptionPane.YES_OPTION;
    }
}