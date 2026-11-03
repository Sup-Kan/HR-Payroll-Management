package com.hrpayroll.view;

import com.hrpayroll.dao.AccountDAO;
import com.hrpayroll.dao.AccountDAO.EmployeeOption;
import com.hrpayroll.dao.DecisionDAO;
import com.hrpayroll.model.Decision;
import com.hrpayroll.model.DecisionStatus;
import com.hrpayroll.model.DecisionType;
import com.hrpayroll.service.DecisionService;
import com.hrpayroll.util.DateUtils;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.FormUtils;
import com.hrpayroll.util.MoneyUtils;
import com.hrpayroll.util.Session;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quản lý quyết định khen thưởng / kỷ luật (mục 3.5).
 * Quyết định mới ở trạng thái "Chờ duyệt"; chỉ quyết định đã duyệt và có
 * số tiền mới được tính vào lương. Dữ liệu đọc/ghi qua DecisionDAO.
 */
public class RewardDisciplineFrame extends JFrame {

    private static final String[] COLUMNS = {
        "Mã quyết định", "Mã NV", "Họ tên", "Loại",
        "Ngày lập", "Số tiền", "Lý do", "Trạng thái"
    };

    private static final int MAX_REASON_LENGTH = 500;
    private static final String ALL = "Tất cả";
    private static final String CHOOSE_EMPLOYEE = "-- Chọn nhân viên --";
    private static final String AUTO_CODE = "(tự tạo khi lưu)";

    private final List<Decision> records = new ArrayList<>();

    private JTextField codeField;
    private JComboBox<Object> employeeCombo;
    private JComboBox<DecisionType> typeCombo;
    private JTextField dateField;
    private JTextField amountField;
    private JTextField reasonField;
    private JTextField statusField;
    private JLabel noticeLabel;

    private JTextField searchField;
    private JComboBox<Object> statusFilter;
    private JComboBox<Object> typeFilter;

    private JTable decisionTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JButton addButton;
    private JButton saveButton;
    private JButton updateButton;
    private JButton approveButton;
    private JButton cancelButton;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton backButton;

    private boolean addingNew = false;

    public RewardDisciplineFrame() {
        setTitle("Quản lý khen thưởng / kỷ luật");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        initComponents();
        initEvents();
        loadFromDatabase();
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

        JLabel titleLabel = new JLabel("QUẢN LÝ KHEN THƯỞNG / KỶ LUẬT");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
            "Lập, phê duyệt và theo dõi các quyết định; "
            + "chỉ khoản đã phê duyệt mới được tính vào lương"
        );

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tìm kiếm và lọc
        searchField = new JTextField(16);
        searchField.setToolTipText(
            "Mã quyết định, mã hoặc tên nhân viên, lý do"
        );

        statusFilter = new JComboBox<>();
        statusFilter.addItem(ALL);
        for (DecisionStatus status : DecisionStatus.values()) {
            statusFilter.addItem(status);
        }

        typeFilter = new JComboBox<>();
        typeFilter.addItem(ALL);
        for (DecisionType type : DecisionType.values()) {
            typeFilter.addItem(type);
        }

        searchButton = new JButton("Tìm kiếm");
        refreshButton = new JButton("Làm mới");

        JPanel filterPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        filterPanel.setBorder(BorderFactory.createTitledBorder("Tìm kiếm"));
        filterPanel.add(new JLabel("Từ khóa:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Loại:"));
        filterPanel.add(typeFilter);
        filterPanel.add(new JLabel("Trạng thái:"));
        filterPanel.add(statusFilter);
        filterPanel.add(searchButton);
        filterPanel.add(refreshButton);

        // Form
        codeField = new JTextField(14);
        codeField.setEditable(false);

        employeeCombo = new JComboBox<>();
        employeeCombo.addItem(CHOOSE_EMPLOYEE);
        loadEmployees();

        typeCombo = new JComboBox<>(DecisionType.values());
        dateField = new JTextField(12);
        amountField = new JTextField(14);
        reasonField = new JTextField(40);
        statusField = new JTextField(14);
        statusField.setEditable(false);

        dateField.setToolTipText("Định dạng " + DateUtils.DISPLAY_PATTERN);
        amountField.setToolTipText(
            "Số tiền đồng, ví dụ 500.000; để trống nếu không kèm tiền"
        );
        reasonField.setToolTipText("Tối đa " + MAX_REASON_LENGTH + " ký tự");

        noticeLabel = new JLabel(" ");
        noticeLabel.setForeground(new Color(160, 60, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
            BorderFactory.createTitledBorder("Thông tin quyết định")
        );

        FormUtils.addRow(
            formPanel, 0,
            "Mã quyết định:", codeField,
            "Nhân viên:", employeeCombo
        );
        FormUtils.addRow(
            formPanel, 1,
            "Loại:", typeCombo,
            "Ngày lập (dd/MM/yyyy):", dateField
        );
        FormUtils.addRow(
            formPanel, 2,
            "Số tiền (đồng):", amountField,
            "Trạng thái:", statusField
        );
        FormUtils.addWideRow(formPanel, 3, "Lý do:", reasonField);
        FormUtils.addWideRow(formPanel, 4, "", noticeLabel);

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

        decisionTable = new JTable(tableModel);
        decisionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableStyler.apply(
            decisionTable,
            new int[] {125, 70, 140, 90, 95, 110, 250, 100},
            new int[] {
                SwingConstants.CENTER, SwingConstants.CENTER, // Mã QĐ, Mã NV
                SwingConstants.LEFT, SwingConstants.CENTER,   // Họ tên, Loại
                SwingConstants.CENTER, SwingConstants.RIGHT,  // Ngày, Số tiền
                SwingConstants.LEFT, SwingConstants.CENTER    // Lý do, Trạng thái
            }
        );

        sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(4, DateUtils.COMPARATOR);
        sorter.setComparator(5, MoneyUtils.COMPARATOR);
        decisionTable.setRowSorter(sorter);

        JScrollPane tableScrollPane = new JScrollPane(decisionTable);
        tableScrollPane.setBorder(
            BorderFactory.createTitledBorder("Danh sách quyết định")
        );

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Nút thao tác
        addButton = new JButton("Thêm quyết định");
        saveButton = new JButton("Lưu");
        updateButton = new JButton("Cập nhật");
        approveButton = new JButton("Phê duyệt");
        cancelButton = new JButton("Hủy quyết định");
        backButton = new JButton("Quay lại");

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );
        buttonPanel.add(addButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(approveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        showBlankState();
    }

    private void initEvents() {
        addButton.addActionListener(e -> startAddingNew());
        saveButton.addActionListener(e -> saveDecision());
        updateButton.addActionListener(e -> updateDecision());
        approveButton.addActionListener(e -> approveDecision());
        cancelButton.addActionListener(e -> cancelDecision());
        backButton.addActionListener(e -> dispose());

        searchButton.addActionListener(e -> applyFilters());
        searchField.addActionListener(e -> applyFilters());
        statusFilter.addActionListener(e -> applyFilters());
        typeFilter.addActionListener(e -> applyFilters());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            typeFilter.setSelectedIndex(0);
            loadEmployees();
            reloadFromDatabase();
            decisionTable.clearSelection();
            addingNew = false;
            showBlankState();
        });

        decisionTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int viewRow = decisionTable.getSelectedRow();

            if (viewRow >= 0) {
                int modelRow =
                    decisionTable.convertRowIndexToModel(viewRow);
                showDecision(records.get(modelRow));
            } else if (!addingNew) {
                showBlankState();
            }
        });

        getRootPane().setDefaultButton(saveButton);
    }

    // ------------------------------------------------------------------
    // Bảng và bộ lọc
    // ------------------------------------------------------------------

    private void refreshTable() {
        decisionTable.clearSelection();
        tableModel.setRowCount(0);

        for (Decision decision : records) {
            tableModel.addRow(rowOf(decision));
        }
    }

    private Object[] rowOf(Decision decision) {
        return new Object[]{
            decision.getCode(),
            decision.getEmployeeCode(),
            decision.getEmployeeName(),
            decision.getType().getLabel(),
            DateUtils.format(decision.getDate()),
            decision.getAmount() == null
                ? "" : MoneyUtils.format(decision.getAmount()),
            decision.getReason(),
            decision.getStatus().getLabel()
        };
    }

    private void applyFilters() {
        if (sorter == null || statusFilter == null || typeFilter == null) {
            return;
        }

        final String keyword = searchField.getText().trim()
            .toLowerCase(Locale.ROOT);
        final Object status = statusFilter.getSelectedItem();
        final Object type = typeFilter.getSelectedItem();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel,
                    ? extends Integer> entry) {

                Decision decision = records.get(entry.getIdentifier());

                if (status instanceof DecisionStatus
                        && decision.getStatus() != status) {
                    return false;
                }

                if (type instanceof DecisionType
                        && decision.getType() != type) {
                    return false;
                }

                if (keyword.isEmpty()) {
                    return true;
                }

                return contains(decision.getCode(), keyword)
                    || contains(decision.getEmployeeCode(), keyword)
                    || contains(decision.getEmployeeName(), keyword)
                    || contains(decision.getReason(), keyword);
            }
        });
    }

    private static boolean contains(String text, String keyword) {
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    // ------------------------------------------------------------------
    // Thao tác
    // ------------------------------------------------------------------

    private void startAddingNew() {
        if (!confirmDiscardCurrentInput()) {
            return;
        }

        decisionTable.clearSelection();
        addingNew = true;
        clearForm();

        codeField.setText(AUTO_CODE);
        statusField.setText(DecisionStatus.PENDING.getLabel());
        dateField.setText(DateUtils.format(LocalDate.now()));

        setFormEditable(true, true);
        setButtons(true, false, false, false);
        noticeLabel.setText(" ");
        employeeCombo.requestFocusInWindow();
    }

    private void saveDecision() {
        Input input = readInput(null);

        if (input == null) {
            return;
        }

        String code;

        try {
            code = DecisionDAO.nextCode(input.type, input.date.getYear());

            Decision decision = new Decision(
                code, input.type,
                input.employee.getCode(), input.employee.getName(),
                input.date, input.reason, input.amount,
                DecisionStatus.PENDING
            );

            DecisionDAO.insert(decision, Session.getUsername());
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        reloadFromDatabase();

        addingNew = false;
        showBlankState();

        Dialogs.info(
            this,
            "Đã lập quyết định " + code + " ở trạng thái Chờ duyệt "
            + "và lưu vào cơ sở dữ liệu."
        );
    }

    private void updateDecision() {
        Decision decision = selectedDecision();

        if (decision == null) {
            Dialogs.warn(this, "Vui lòng chọn quyết định cần cập nhật.");
            return;
        }

        if (!decision.isEditable()) {
            Dialogs.warn(
                this,
                "Chỉ quyết định đang chờ duyệt mới được cập nhật."
            );
            return;
        }

        Input input = readInput(decision);

        if (input == null) {
            return;
        }

        Decision preview = new Decision(
            decision.getCode(), decision.getType(),
            input.employee.getCode(), input.employee.getName(),
            input.date, input.reason, input.amount, decision.getStatus()
        );

        String comparison = FormUtils.buildUpdateComparison(
            COLUMNS, rowOf(decision), rowOf(preview)
        );

        if (!Dialogs.confirm(
                this, comparison, "Xác nhận cập nhật quyết định")) {
            return;
        }

        try {
            DecisionDAO.update(preview);
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        reloadFromDatabase();
        showBlankState();
        Dialogs.info(this, "Đã cập nhật quyết định " + decision.getCode() + ".");
    }

    private void approveDecision() {
        Decision decision = selectedDecision();

        if (decision == null) {
            Dialogs.warn(this, "Vui lòng chọn quyết định cần phê duyệt.");
            return;
        }

        if (!decision.isEditable()) {
            Dialogs.warn(
                this, "Chỉ quyết định đang chờ duyệt mới được phê duyệt."
            );
            return;
        }

        String effect = decision.getAmount() == null
            ? "Quyết định không kèm số tiền nên không ảnh hưởng đến lương."
            : "Khoản " + MoneyUtils.format(decision.getAmount())
              + " đồng sẽ được tính vào lương khi tính lương kỳ tương ứng.";

        if (!Dialogs.confirm(
                this,
                "Phê duyệt quyết định " + decision.getCode() + "?\n"
                + effect + "\nSau khi duyệt không thể sửa, chỉ có thể hủy.",
                "Xác nhận phê duyệt")) {
            return;
        }

        String approver = Session.getUsername() == null
            ? "(không xác định)" : Session.getUsername();

        try {
            DecisionDAO.approve(decision.getCode(), approver);
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        reloadFromDatabase();
        showBlankState();
        Dialogs.info(this, "Đã phê duyệt quyết định " + decision.getCode() + ".");
    }

    private void cancelDecision() {
        Decision decision = selectedDecision();

        if (decision == null) {
            Dialogs.warn(this, "Vui lòng chọn quyết định cần hủy.");
            return;
        }

        if (decision.getStatus() == DecisionStatus.CANCELLED) {
            Dialogs.warn(this, "Quyết định này đã được hủy.");
            return;
        }

        String note = decision.getStatus() == DecisionStatus.APPROVED
            ? "\nQuyết định đã duyệt: hủy sẽ loại khoản này khỏi lương "
              + "các kỳ chưa chốt."
            : "";

        if (!Dialogs.confirm(
                this,
                "Bạn có chắc muốn hủy quyết định " + decision.getCode()
                + "?" + note + "\nQuyết định đã hủy không thể khôi phục.",
                "Xác nhận hủy quyết định")) {
            return;
        }

        try {
            DecisionDAO.cancel(decision.getCode());
        } catch (SQLException ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        reloadFromDatabase();
        showBlankState();
    }

    // ------------------------------------------------------------------
    // Đọc và hiển thị form
    // ------------------------------------------------------------------

    /** Dữ liệu form đã kiểm tra hợp lệ. */
    private static final class Input {
        private EmployeeOption employee;
        private DecisionType type;
        private LocalDate date;
        private Long amount;
        private String reason;
    }

    /**
     * Đọc và kiểm tra form; hiện thông báo và trả về null nếu sai.
     *
     * @param existing quyết định đang sửa (null khi thêm mới)
     */
    private Input readInput(Decision existing) {
        Input input = new Input();

        Object selected = employeeCombo.getSelectedItem();

        if (!(selected instanceof EmployeeOption)) {
            Dialogs.warn(this, "Vui lòng chọn nhân viên.");
            return null;
        }

        input.employee = (EmployeeOption) selected;
        input.type = existing == null
            ? (DecisionType) typeCombo.getSelectedItem()
            : existing.getType();

        try {
            input.date = DateUtils.parse(dateField.getText());
        } catch (DateTimeParseException ex) {
            Dialogs.warn(
                this,
                "Ngày lập phải là ngày hợp lệ, dạng "
                + DateUtils.DISPLAY_PATTERN + "."
            );
            return null;
        }

        String amountText = amountField.getText().trim();

        if (amountText.isEmpty()) {
            input.amount = null;
        } else {
            try {
                input.amount = MoneyUtils.parse(amountText);
            } catch (NumberFormatException ex) {
                Dialogs.warn(this, ex.getMessage());
                return null;
            }
        }

        input.reason = reasonField.getText().trim();

        if (input.reason.isEmpty()) {
            Dialogs.warn(this, "Lý do không được để trống.");
            return null;
        }

        if (input.reason.length() > MAX_REASON_LENGTH) {
            Dialogs.warn(
                this, "Lý do tối đa " + MAX_REASON_LENGTH + " ký tự."
            );
            return null;
        }

        return input;
    }

    private Decision selectedDecision() {
        int viewRow = decisionTable.getSelectedRow();

        if (viewRow < 0) {
            return null;
        }

        return records.get(decisionTable.convertRowIndexToModel(viewRow));
    }

    /** Hiện quyết định lên form; chỉ quyết định chờ duyệt mới sửa được. */
    private void showDecision(Decision decision) {
        addingNew = false;

        codeField.setText(decision.getCode());
        typeCombo.setSelectedItem(decision.getType());
        dateField.setText(DateUtils.format(decision.getDate()));
        amountField.setText(
            decision.getAmount() == null
                ? "" : MoneyUtils.format(decision.getAmount())
        );
        reasonField.setText(decision.getReason());
        statusField.setText(decision.getStatus().getLabel());
        selectEmployee(decision.getEmployeeCode(), decision.getEmployeeName());

        switch (decision.getStatus()) {
            case APPROVED:
                setFormEditable(false, false);
                setButtons(false, false, false, true);
                noticeLabel.setText(
                    "Đã duyệt bởi " + decision.getApprovedBy() + " lúc "
                    + DateUtils.formatDateTime(decision.getApprovedAt())
                    + ". Không thể sửa, chỉ có thể hủy."
                );
                break;
            case CANCELLED:
                setFormEditable(false, false);
                setButtons(false, false, false, false);
                noticeLabel.setText("Quyết định đã hủy, không thể thay đổi.");
                break;
            default:
                setFormEditable(true, false);
                setButtons(false, true, true, true);
                noticeLabel.setText(" ");
                break;
        }
    }

    private void selectEmployee(String code, String name) {
        for (int i = 0; i < employeeCombo.getItemCount(); i++) {
            Object item = employeeCombo.getItemAt(i);

            if (item instanceof EmployeeOption
                    && ((EmployeeOption) item).getCode().equals(code)) {
                employeeCombo.setSelectedIndex(i);
                return;
            }
        }

        // Nhân viên đã nghỉ việc không còn trong danh sách đang làm.
        EmployeeOption former = new EmployeeOption(-1, code, name);
        employeeCombo.addItem(former);
        employeeCombo.setSelectedItem(former);
    }

    /** Nạp danh sách nhân viên đang làm việc vào ô chọn. */
    private void loadEmployees() {
        employeeCombo.removeAllItems();
        employeeCombo.addItem(CHOOSE_EMPLOYEE);

        try {
            for (EmployeeOption employee : AccountDAO.findActiveEmployees()) {
                employeeCombo.addItem(employee);
            }
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được danh sách nhân viên.\n" + ex.getMessage());
        }
    }

    /** Nạp toàn bộ quyết định từ database đang chọn. */
    private void loadFromDatabase() {
        try {
            List<Decision> loaded = DecisionDAO.findAll();
            records.clear();
            records.addAll(loaded);
        } catch (SQLException ex) {
            Dialogs.error(this,
                "Không tải được danh sách quyết định.\n" + ex.getMessage());
        }
    }

    private void reloadFromDatabase() {
        loadFromDatabase();
        refreshTable();
        applyFilters();
    }

    private void showBlankState() {
        clearForm();
        setFormEditable(false, false);
        setButtons(false, false, false, false);
        noticeLabel.setText(" ");
    }

    private void setButtons(
            boolean save, boolean update, boolean approve, boolean cancel) {

        saveButton.setEnabled(save);
        updateButton.setEnabled(update);
        approveButton.setEnabled(approve);
        cancelButton.setEnabled(cancel);
    }

    private void clearForm() {
        codeField.setText("");
        employeeCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        dateField.setText("");
        amountField.setText("");
        reasonField.setText("");
        statusField.setText("");
    }

    /** Loại quyết định chỉ chọn được khi thêm mới vì mã quyết định gắn với loại. */
    private void setFormEditable(boolean editable, boolean typeEditable) {
        employeeCombo.setEnabled(editable);
        typeCombo.setEnabled(editable && typeEditable);
        dateField.setEditable(editable);
        amountField.setEditable(editable);
        reasonField.setEditable(editable);
    }

    private boolean hasFormInput() {
        return employeeCombo.getSelectedIndex() > 0
            || !dateField.getText().trim().isEmpty()
            || !amountField.getText().trim().isEmpty()
            || !reasonField.getText().trim().isEmpty();
    }

    private boolean confirmDiscardCurrentInput() {
        if (!hasFormInput()) {
            return true;
        }

        return Dialogs.confirm(
            this,
            "Thông tin đang hiển thị có thể chưa được lưu.\n"
            + "Bạn có muốn bỏ thông tin này và lập quyết định mới không?",
            "Xác nhận lập quyết định mới"
        );
    }
}
