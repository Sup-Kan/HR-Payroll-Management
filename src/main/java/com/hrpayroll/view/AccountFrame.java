package com.hrpayroll.view;

import com.hrpayroll.dao.AccountDAO;
import com.hrpayroll.dao.AccountDAO.EmployeeOption;
import com.hrpayroll.dao.PresenceDAO;
import com.hrpayroll.model.Account;
import com.hrpayroll.model.Feature;
import com.hrpayroll.model.Role;
import com.hrpayroll.service.AccountService;
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
// import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/**
 * Quản lý tài khoản và phân quyền. Dữ liệu được đọc/ghi trực tiếp qua AccountDAO.
 * Mật khẩu được băm BCrypt trước khi lưu vào accounts.password_hash.
 */
public class AccountFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Tên đăng nhập", "Nhân viên", "Vai trò",
        "Trạng thái", "Ngày tạo", "Đăng nhập gần nhất", "Đang đăng nhập"
    };

    private static final String NO_EMPLOYEE = "(Không gắn với nhân viên)";
    private static final String LOCK_TEXT = "Khóa tài khoản";
    private static final String UNLOCK_TEXT = "Mở khóa";

    private final List<Account> accounts = new ArrayList<>();
    private Set<String> onlineUsers = Collections.emptySet();
    private Timer presenceTimer;
    private boolean onlineError;
    private final List<EmployeeOption> employees = new ArrayList<>();

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
    private JButton profileButton;

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
        reloadFromDatabase();
        applyFilter();
        startPresenceUpdates();
    }

    private static final int COL_ONLINE = 6;

    private static boolean canSeePresence() {
        return Session.getRole() == Role.ADMIN || Session.getRole() == Role.IT;
    }

    /** Làm mới cột "Trực tuyến" định kỳ, không đụng tới dòng đang chọn/đang sửa. */
    private void startPresenceUpdates() {
        if (!canSeePresence()) {
            return;
        }
        presenceTimer = new Timer(PresenceDAO.HEARTBEAT_SECONDS * 1000,
                e -> updatePresenceColumn());
        presenceTimer.setInitialDelay(2000);
        presenceTimer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                presenceTimer.stop();
            }
        });
    }

    private void updatePresenceColumn() {
        new SwingWorker<Set<String>, Void>() {
            @Override
            protected Set<String> doInBackground() throws Exception {
                return PresenceDAO.onlineUsernames();
            }

            @Override
            protected void done() {
                try {
                    onlineUsers = get();
                    onlineError = false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    onlineError = true;
                }
                for (int i = 0; i < accounts.size() && i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(onlineText(accounts.get(i)), i, COL_ONLINE);
                }
            }
        }.execute();
    }

    private String onlineText(Account account) {
        if (onlineError) {
            return "Lỗi đọc";
        }
        return onlineUsers.contains(account.getUsername()) ? "● Đang đăng nhập" : "Không";
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
        loadEmployeeOptions();

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
            new int[] {150, 210, 100, 110, 100, 150, 130},
            new int[] {
                SwingConstants.LEFT, SwingConstants.LEFT,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER
            }
        );

        // Chỉ Admin/IT thấy trạng thái trực tuyến (cột vẫn nằm trong model).
        if (!canSeePresence()) {
            accountTable.removeColumn(accountTable.getColumnModel().getColumn(COL_ONLINE));
        }

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
        profileButton = new JButton("Thông tin cá nhân");
        backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );
        buttonPanel.add(createButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(passwordButton);
        buttonPanel.add(roleButton);
        buttonPanel.add(lockButton);
        buttonPanel.add(profileButton);
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
        profileButton.addActionListener(e -> new ProfileFrame().setVisible(true));

        searchButton.addActionListener(e -> applyFilter());
        searchField.addActionListener(e -> applyFilter());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadEmployeeOptions();
            reloadFromDatabase();
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

    private void reloadFromDatabase() {
        try {
            onlineUsers = canSeePresence()
                    ? PresenceDAO.onlineUsernames() : Collections.emptySet();
            onlineError = false;
        } catch (Exception ex) {
            ex.printStackTrace();
            onlineUsers = Collections.emptySet();
            onlineError = true;
        }
        try {
            accounts.clear();
            accounts.addAll(AccountDAO.findAll());
            refreshTable();
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể tải tài khoản từ database: " + ex.getMessage());
        }
    }

    private void loadEmployeeOptions() {
        try {
            // Nạp lại từ DB mỗi lần: nhân viên thêm sau khi mở màn hình này
            // phải xuất hiện trong danh sách.
            employees.clear();
            employees.addAll(AccountDAO.findActiveEmployees());
            employeeCombo.removeAllItems();
            employeeCombo.addItem(NO_EMPLOYEE);
            for (EmployeeOption employee : employees) employeeCombo.addItem(employee);
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể tải danh sách nhân viên: " + ex.getMessage());
        }
    }

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
                    : DateUtils.formatDateTime(account.getLastLogin()),
                onlineText(account)
            });
        }
    }

    private String employeeText(String code) {
        if (code == null) {
            return "-";
        }

        for (EmployeeOption employee : employees) {
            if (employee.getCode().equals(code)) return employee.toString();
        }
        return code;
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

                return account.getUsername().toLowerCase(Locale.ROOT).contains(keyword)
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
        loadEmployeeOptions();
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
        String employeeCode = selected instanceof EmployeeOption
            ? ((EmployeeOption) selected).getCode() : null;
        String username = AccountService.normalizeUsername(usernameField.getText());
        char[] password = passwordField.getPassword();
        try {
            AccountDAO.create(username, employeeCode, (Role) roleCombo.getSelectedItem(), password);
            passwordField.setText("");
            confirmField.setText("");
            creating = false;
            reloadFromDatabase();
            showBlankState();
            Dialogs.info(this, "Đã tạo tài khoản " + username + " trong database.");
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể tạo tài khoản: " + ex.getMessage());
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
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

        char[] password = newField.getPassword();
        try {
            AccountDAO.updatePassword(account.getUsername(), password);
            Dialogs.info(this, "Đã cập nhật mật khẩu cho " + account.getUsername() + ".");
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể đổi mật khẩu: " + ex.getMessage());
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
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

        try {
            AccountDAO.updateRole(account.getUsername(), newRole);
            account.setRole(newRole);
            reloadFromDatabase();
            showBlankState();
            Dialogs.info(this, "Đã đổi vai trò của " + account.getUsername() + " thành " + newRole.getLabel() + ".");
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể cập nhật vai trò: " + ex.getMessage());
        }
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

        try {
            AccountDAO.updateStatus(account.getUsername(), locking);
            account.setLocked(locking);
            reloadFromDatabase();
            showBlankState();
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể cập nhật trạng thái tài khoản: " + ex.getMessage());
        }
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

            if (item instanceof EmployeeOption
                    && ((EmployeeOption) item).getCode()
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
