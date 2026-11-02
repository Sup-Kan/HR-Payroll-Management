package com.hrpayroll.view;

import com.hrpayroll.model.Account;
import com.hrpayroll.model.Feature;
import com.hrpayroll.model.Role;
import com.hrpayroll.service.AccountService;
import com.hrpayroll.service.DemoData;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.FormUtils;
import com.hrpayroll.util.Session;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quản lý tài khoản và phân quyền (mục 3.8), chỉ Admin truy cập.
 * Mật khẩu chỉ được kiểm tra hợp lệ, không được giữ lại: khi nối DB,
 * mật khẩu được băm BCrypt rồi lưu vào accounts.password_hash.
 */
public class AccountFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Tên đăng nhập", "Nhân viên", "Vai trò",
        "Trạng thái", "Ngày tạo", "Đăng nhập gần nhất"
    };

    private static final String NO_EMPLOYEE = "(Không gắn với nhân viên)";
    private static final String LOCK_TEXT = "Khóa tài khoản";
    private static final String UNLOCK_TEXT = "Mở khóa";

    private final List<Account> accounts =
        new ArrayList<>(DemoData.accounts());

    private JTextField usernameField;
    private JComboBox<Object> employeeCombo;
    private JComboBox<Role> roleCombo;
    private JTextField statusField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JLabel noticeLabel;

    private JTextField searchField;

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton createButton;
    private JButton saveButton;
    private JButton passwordButton;
    private JButton roleButton;
    private JButton lockButton;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton backButton;

    private boolean creating = false;

    public AccountFrame() {
        setTitle("Quản lý tài khoản");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        refreshTable();
        applyFilter();
    }

    // ------------------------------------------------------------------
    // Dựng giao diện
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("QUẢN LÝ TÀI KHOẢN VÀ PHÂN QUYỀN");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Tạo tài khoản, đặt lại mật khẩu, phân quyền và khóa/mở khóa"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tìm kiếm
        searchField = new JTextField(18);
        searchField.setToolTipText(
            "Tên đăng nhập, mã hoặc tên nhân viên"
        );
        searchButton = new JButton("Tìm kiếm");
        refreshButton = new JButton("Làm mới");

        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        filterPanel.setBorder(BorderFactory.createTitledBorder("Tìm kiếm"));
        filterPanel.add(new JLabel("Từ khóa:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        // Form
        usernameField = new JTextField(16);
        usernameField.setToolTipText(
            "3 đến 60 ký tự: chữ thường, số, dấu gạch dưới hoặc dấu chấm"
        );

        employeeCombo = new JComboBox<>();
        employeeCombo.addItem(NO_EMPLOYEE);
        for (DemoData.Employee employee : DemoData.activeEmployees()) {
            employeeCombo.addItem(employee);
        }

        roleCombo = new JComboBox<>(Role.values());
        statusField = new JTextField(14);
        statusField.setEditable(false);

        passwordField = new JPasswordField(16);
        confirmField = new JPasswordField(16);
        passwordField.setToolTipText(
            "Tối thiểu 8 ký tự, gồm cả chữ và số"
        );

        noticeLabel = new JLabel(" ");
        noticeLabel.setForeground(new Color(160, 60, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Thông tin tài khoản")
        );

        FormUtils.addRow(
            formPanel, 0,
            "Tên đăng nhập:", usernameField,
            "Nhân viên:", employeeCombo
        );
        FormUtils.addRow(
            formPanel, 1,
            "Vai trò:", roleCombo,
            "Trạng thái:", statusField
        );
        FormUtils.addRow(
            formPanel, 2,
            "Mật khẩu:", passwordField,
            "Nhập lại mật khẩu:", confirmField
        );
        FormUtils.addWideRow(formPanel, 3, "", noticeLabel);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(formPanel, BorderLayout.CENTER);

        // Bảng
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        accountTable = new JTable(tableModel);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableStyler.apply(
            accountTable,
            new int[] {150, 210, 100, 110, 100, 150},
            new int[] {
                SwingConstants.LEFT, SwingConstants.LEFT,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.CENTER
            }
        );

        sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(4, DateUtils.COMPARATOR);
        accountTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(accountTable);
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Danh sách tài khoản")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Nút thao tác
        createButton = new JButton("Tạo tài khoản");
        saveButton = new JButton("Lưu");
        passwordButton = new JButton("Đổi mật khẩu");
        roleButton = new JButton("Phân quyền");
        lockButton = new JButton(LOCK_TEXT);
        backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );
        buttonPanel.add(createButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(passwordButton);
        buttonPanel.add(roleButton);
        buttonPanel.add(lockButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        showBlankState();
    }

    private void initEvents() {
        createButton.addActionListener(e -> startCreating());
        saveButton.addActionListener(e -> saveAccount());
        passwordButton.addActionListener(e -> changePassword());
        roleButton.addActionListener(e -> changeRole());
        lockButton.addActionListener(e -> toggleLock());
        backButton.addActionListener(e -> dispose());

        searchButton.addActionListener(e -> applyFilter());
        searchField.addActionListener(e -> applyFilter());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            applyFilter();
            accountTable.clearSelection();
            creating = false;
            showBlankState();
        });

        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int viewRow = accountTable.getSelectedRow();

            if (viewRow >= 0) {
                int modelRow = accountTable.convertRowIndexToModel(viewRow);
                showAccount(accounts.get(modelRow));
            } else if (!creating) {
                showBlankState();
            }
        });

        getRootPane().setDefaultButton(saveButton);
    }

    // ------------------------------------------------------------------
    // Bảng và tìm kiếm
    // ------------------------------------------------------------------

    private void refreshTable() {
        accountTable.clearSelection();
        tableModel.setRowCount(0);

        for (Account account : accounts) {
            tableModel.addRow(new Object[]{
                account.getUsername(),
                employeeText(account.getEmployeeCode()),
                account.getRole().getLabel(),
                account.isLocked() ? "Đã khóa" : "Hoạt động",
                DateUtils.format(account.getCreatedAt()),
                account.getLastLogin() == null
                    ? "Chưa đăng nhập"
                    : DateUtils.formatDateTime(account.getLastLogin())
            });
        }
    }

    private static String employeeText(String code) {
        if (code == null) {
            return "-";
        }

        DemoData.Employee employee = DemoData.findEmployee(code);

        return employee == null ? code : employee.toString();
    }

    private void applyFilter() {
        if (sorter == null) {
            return;
        }

        final String keyword = searchField.getText().trim()
            .toLowerCase(Locale.ROOT);

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel,
                    ? extends Integer> entry) {

                if (keyword.isEmpty()) {
                    return true;
                }

                Account account = accounts.get(entry.getIdentifier());

                return account.getUsername().contains(keyword)
                    || employeeText(account.getEmployeeCode())
                        .toLowerCase(Locale.ROOT).contains(keyword);
            }
        });
    }

    // ------------------------------------------------------------------
    // Thao tác
    // ------------------------------------------------------------------

    private void startCreating() {
        if (!confirmDiscardCurrentInput()) {
            return;
        }

        accountTable.clearSelection();
        creating = true;
        clearForm();

        statusField.setText("Hoạt động");
        setFormEditable(true);
        setActionButtons(false, null);
        saveButton.setEnabled(true);
        noticeLabel.setText(" ");
        usernameField.requestFocusInWindow();
    }

    private void saveAccount() {
        String error = AccountService.validateUsername(
            usernameField.getText(), accounts
        );

        if (error == null) {
            error = AccountService.validatePassword(
                new String(passwordField.getPassword()),
                new String(confirmField.getPassword()),
                usernameField.getText()
            );
        }

        if (error != null) {
            Dialogs.warn(this, error);
            return;
        }

        Object selected = employeeCombo.getSelectedItem();
        String employeeCode = selected instanceof DemoData.Employee
            ? ((DemoData.Employee) selected).getCode() : null;

        Account account = new Account(
            AccountService.normalizeUsername(usernameField.getText()),
            employeeCode,
            (Role) roleCombo.getSelectedItem(),
            false,
            LocalDate.now(),
            null
        );

        accounts.add(account);

        passwordField.setText("");
        confirmField.setText("");
        creating = false;
        refreshTable();
        showBlankState();

        Dialogs.info(
            this,
            "Đã tạo tài khoản " + account.getUsername() + ".\n"
            + "Mật khẩu chưa được lưu: khi nối cơ sở dữ liệu, mật khẩu sẽ "
            + "được băm BCrypt rồi mới lưu.\n"
            + "Tài khoản này chưa đăng nhập được ở bản thử nghiệm."
        );
    }

    private void changePassword() {
        Account account = selectedAccount();

        if (account == null) {
            Dialogs.warn(this, "Vui lòng chọn tài khoản cần đổi mật khẩu.");
            return;
        }

        JPasswordField newField = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);

        JPanel panel = new JPanel(new GridBagLayout());
        FormUtils.addRow(
            panel, 0, "Mật khẩu mới:", newField, "Nhập lại:", confirm
        );

        int choice = JOptionPane.showConfirmDialog(
            this, panel,
            "Đặt lại mật khẩu cho " + account.getUsername(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String error = AccountService.validatePassword(
            new String(newField.getPassword()),
            new String(confirm.getPassword()),
            account.getUsername()
        );

        if (error != null) {
            Dialogs.warn(this, error);
            return;
        }

        Dialogs.info(
            this,
            "Đã đặt lại mật khẩu cho " + account.getUsername() + ".\n"
            + "Bản thử nghiệm chưa lưu mật khẩu; khi nối cơ sở dữ liệu "
            + "mật khẩu sẽ được băm BCrypt."
        );
    }

    private void changeRole() {
        Account account = selectedAccount();

        if (account == null) {
            Dialogs.warn(this, "Vui lòng chọn tài khoản cần phân quyền.");
            return;
        }

        JComboBox<Role> combo = new JComboBox<>(Role.values());
        combo.setSelectedItem(account.getRole());

        JLabel featuresLabel = new JLabel(featuresText(account.getRole()));
        combo.addActionListener(e ->
            featuresLabel.setText(featuresText((Role) combo.getSelectedItem()))
        );

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.add(new JLabel("Vai trò mới cho " + account.getUsername() + ":"));
        panel.add(combo);
        panel.add(featuresLabel);

        int choice = JOptionPane.showConfirmDialog(
            this, panel, "Phân quyền",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        Role newRole = (Role) combo.getSelectedItem();
        String error = AccountService.checkRoleChange(
            accounts, account, newRole, Session.getUsername()
        );

        if (error != null) {
            Dialogs.warn(this, error);
            return;
        }

        account.setRole(newRole);
        refreshTable();
        showBlankState();

        Dialogs.info(
            this,
            "Đã đổi vai trò của " + account.getUsername() + " thành "
            + newRole.getLabel() + ".\nCó hiệu lực ở lần đăng nhập sau."
        );
    }

    private void toggleLock() {
        Account account = selectedAccount();

        if (account == null) {
            Dialogs.warn(this, "Vui lòng chọn tài khoản cần khóa hoặc mở khóa.");
            return;
        }

        String error = AccountService.checkLockToggle(
            accounts, account, Session.getUsername()
        );

        if (error != null) {
            Dialogs.warn(this, error);
            return;
        }

        boolean locking = !account.isLocked();

        if (!Dialogs.confirm(
                this,
                (locking ? "Khóa" : "Mở khóa") + " tài khoản "
                + account.getUsername() + "?",
                locking ? "Xác nhận khóa tài khoản" : "Xác nhận mở khóa")) {
            return;
        }

        account.setLocked(locking);
        refreshTable();
        showBlankState();
    }

    // ------------------------------------------------------------------
    // Hiển thị form
    // ------------------------------------------------------------------

    private static String featuresText(Role role) {
        StringBuilder text = new StringBuilder("Được truy cập: ");
        boolean first = true;

        for (Feature feature : Feature.values()) {
            if (role.canAccess(feature)) {
                if (!first) {
                    text.append(", ");
                }
                text.append(feature.getTitle());
                first = false;
            }
        }

        return text.toString();
    }

    private Account selectedAccount() {
        int viewRow = accountTable.getSelectedRow();

        if (viewRow < 0) {
            return null;
        }

        return accounts.get(accountTable.convertRowIndexToModel(viewRow));
    }

    private void showAccount(Account account) {
        creating = false;

        usernameField.setText(account.getUsername());
        statusField.setText(account.isLocked() ? "Đã khóa" : "Hoạt động");
        roleCombo.setSelectedItem(account.getRole());
        passwordField.setText("");
        confirmField.setText("");

        employeeCombo.setSelectedIndex(0);
        for (int i = 0; i < employeeCombo.getItemCount(); i++) {
            Object item = employeeCombo.getItemAt(i);

            if (item instanceof DemoData.Employee
                    && ((DemoData.Employee) item).getCode()
                        .equals(account.getEmployeeCode())) {
                employeeCombo.setSelectedIndex(i);
            }
        }

        setFormEditable(false);
        setActionButtons(true, account);

        boolean self = account.getUsername().equals(Session.getUsername());
        noticeLabel.setText(
            self
                ? "Đây là tài khoản đang đăng nhập: không thể tự khóa "
                  + "hoặc đổi vai trò."
                : " "
        );
    }

    private void showBlankState() {
        clearForm();
        setFormEditable(false);
        setActionButtons(false, null);
        saveButton.setEnabled(false);
        noticeLabel.setText(" ");
    }

    /** Bật các nút thao tác trên tài khoản đang chọn; nút Lưu chỉ bật khi tạo mới. */
    private void setActionButtons(boolean enabled, Account account) {
        passwordButton.setEnabled(enabled);
        roleButton.setEnabled(enabled);
        lockButton.setEnabled(enabled);
        saveButton.setEnabled(false);

        lockButton.setText(
            account != null && account.isLocked() ? UNLOCK_TEXT : LOCK_TEXT
        );
    }

    private void clearForm() {
        usernameField.setText("");
        employeeCombo.setSelectedIndex(0);
        roleCombo.setSelectedItem(Role.HR);
        statusField.setText("");
        passwordField.setText("");
        confirmField.setText("");
    }

    private void setFormEditable(boolean editable) {
        usernameField.setEditable(editable);
        employeeCombo.setEnabled(editable);
        roleCombo.setEnabled(editable);
        passwordField.setEditable(editable);
        confirmField.setEditable(editable);
    }

    private boolean hasFormInput() {
        return !usernameField.getText().trim().isEmpty()
            || passwordField.getPassword().length > 0
            || confirmField.getPassword().length > 0;
    }

    private boolean confirmDiscardCurrentInput() {
        if (!creating || !hasFormInput()) {
            return true;
        }

        return Dialogs.confirm(
            this,
            "Thông tin đang nhập chưa được lưu.\n"
            + "Bạn có muốn bỏ thông tin này và tạo tài khoản mới không?",
            "Xác nhận tạo mới"
        );
    }
}
