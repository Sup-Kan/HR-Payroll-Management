package com.hrpayroll.view;

import com.hrpayroll.dao.CatalogDAO;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.FormUtils;
import com.hrpayroll.util.MoneyUtils;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Quản lý danh mục phụ: phòng ban và chức vụ (thêm, sửa, xóa).
 * Mỗi tab dùng chung một khung xử lý (Section); chỉ khác trường nhập và DAO.
 */
public class CatalogFrame extends JFrame {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{1,20}");

    public CatalogFrame() {
        setTitle("Quản lý danh mục - Phòng ban & Chức vụ");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 640);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Phòng ban", new DepartmentSection());
        tabs.addTab("Chức vụ", new PositionSection());

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel title = new JLabel("QUẢN LÝ DANH MỤC");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(6, 0, 0, 0));
        main.add(title, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);

        JButton back = new JButton("Quay lại");
        back.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(back);
        main.add(south, BorderLayout.SOUTH);
        setContentPane(main);
    }

    /** Khung chung: form nhập, bảng, các nút Thêm mới/Lưu/Xóa/Làm mới. */
    private abstract class Section extends JPanel {
        final JTextField codeField = new JTextField(14);
        final JTextField nameField = new JTextField(20);
        final JTextArea descField = FormUtils.textArea(2, 20);
        final JCheckBox activeBox = new JCheckBox("Đang hoạt động", true);
        final DefaultTableModel model;
        final JTable table;
        final JPanel form = new JPanel(new GridBagLayout());
        final String[] columns;
        boolean adding;
        int formRow;

        Section(String[] columns, int[] widths) {
            super(new BorderLayout(8, 8));
            this.columns = columns;
            setBorder(new EmptyBorder(8, 4, 4, 4));

            model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            int[] align = new int[columns.length];
            java.util.Arrays.fill(align, SwingConstants.CENTER);
            TableStyler.apply(table, widths, align);
            table.setRowSorter(new TableRowSorter<>(model));

            form.setBorder(BorderFactory.createTitledBorder("Thông tin"));
            addField("Mã:", codeField);
            addField("Tên:", nameField);
            addExtraFields();
            addField("Mô tả:", FormUtils.scroll(descField));
            addField("", activeBox);

            JButton newButton = new JButton("Thêm mới");
            JButton saveButton = new JButton("Lưu");
            JButton deleteButton = new JButton("Xóa");
            JButton refreshButton = new JButton("Làm mới");
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            buttons.add(newButton);
            buttons.add(saveButton);
            buttons.add(deleteButton);
            buttons.add(refreshButton);

            JPanel top = new JPanel(new BorderLayout());
            top.add(form, BorderLayout.CENTER);
            top.add(buttons, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createTitledBorder("Danh sách"));
            add(scroll, BorderLayout.CENTER);

            newButton.addActionListener(e -> startAdding());
            saveButton.addActionListener(e -> save());
            deleteButton.addActionListener(e -> remove());
            refreshButton.addActionListener(e -> reload());
            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                int v = table.getSelectedRow();
                if (v >= 0) {
                    adding = false;
                    fillForm(table.convertRowIndexToModel(v));
                    codeField.setEditable(false);
                }
            });
        }

        /** Gọi sau khi lớp con khởi tạo xong các trường của nó. */
        void init() {
            codeField.setEditable(false);
            reload();
        }

        void addField(String label, JComponent field) {
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(4, 6, 4, 6);
            g.gridy = formRow++;
            g.gridx = 0;
            g.anchor = GridBagConstraints.WEST;
            form.add(new JLabel(label), g);
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            form.add(field, g);
        }

        abstract void addExtraFields();
        abstract List<Object[]> load() throws SQLException;
        abstract void fillForm(int modelRow);
        abstract void clearExtra();
        /** @return thông báo lỗi nhập liệu, hoặc null nếu hợp lệ */
        abstract String validateExtra();
        abstract void insert(String code, String name, String desc, boolean active) throws SQLException;
        abstract void update(String code, String name, String desc, boolean active) throws SQLException;
        abstract void delete(String code) throws SQLException;

        void reload() {
            try {
                model.setRowCount(0);
                for (Object[] row : load()) model.addRow(row);
            } catch (SQLException ex) {
                Dialogs.error(this, "Không thể tải dữ liệu: " + ex.getMessage());
            }
        }

        void startAdding() {
            table.clearSelection();
            adding = true;
            codeField.setText("");
            nameField.setText("");
            descField.setText("");
            activeBox.setSelected(true);
            clearExtra();
            codeField.setEditable(true);
            codeField.requestFocusInWindow();
        }

        void save() {
            String code = codeField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();

            if (!adding && table.getSelectedRow() < 0) {
                Dialogs.warn(this, "Hãy chọn một dòng để sửa hoặc bấm Thêm mới.");
                return;
            }
            if (!CODE.matcher(code).matches()) {
                Dialogs.warn(this, "Mã chỉ gồm chữ in hoa, số hoặc dấu gạch dưới (tối đa 20 ký tự).");
                return;
            }
            if (name.isEmpty()) {
                Dialogs.warn(this, "Tên không được để trống.");
                return;
            }
            String problem = validateExtra();
            if (problem != null) {
                Dialogs.warn(this, problem);
                return;
            }

            try {
                if (adding) {
                    insert(code, name, descField.getText(), activeBox.isSelected());
                } else {
                    if (!Dialogs.confirm(this, "Lưu thay đổi cho " + code + "?", "Xác nhận")) return;
                    update(code, name, descField.getText(), activeBox.isSelected());
                }
            } catch (SQLException ex) {
                Dialogs.error(this, "Không thể lưu: " + ex.getMessage());
                return;
            }

            adding = false;
            reload();
            Dialogs.info(this, "Đã lưu " + code + " vào cơ sở dữ liệu.");
        }

        void remove() {
            int v = table.getSelectedRow();
            if (v < 0) {
                Dialogs.warn(this, "Hãy chọn một dòng để xóa.");
                return;
            }
            String code = String.valueOf(model.getValueAt(table.convertRowIndexToModel(v), 0));
            if (!Dialogs.confirm(this, "Bạn có chắc chắn xóa " + code + " không?", "Xác nhận xóa")) return;
            try {
                delete(code);
            } catch (SQLException ex) {
                Dialogs.error(this, ex.getMessage());
                return;
            }
            reload();
            startAdding();
            adding = false;
            codeField.setEditable(false);
        }

        String cell(int row, int col) { return String.valueOf(model.getValueAt(row, col)); }
    }

    private final class DepartmentSection extends Section {
        DepartmentSection() {
            super(new String[] {"Mã", "Tên phòng ban", "Mô tả", "Trạng thái", "Số nhân viên"},
                  new int[] {90, 200, 280, 120, 100});
            init();
        }
        @Override void addExtraFields() { }
        @Override void clearExtra() { }
        @Override String validateExtra() { return null; }
        @Override List<Object[]> load() throws SQLException { return CatalogDAO.departments(); }
        @Override void fillForm(int r) {
            codeField.setText(cell(r, 0));
            nameField.setText(cell(r, 1));
            descField.setText(cell(r, 2));
            activeBox.setSelected("Hoạt động".equals(cell(r, 3)));
        }
        @Override void insert(String c, String n, String d, boolean a) throws SQLException { CatalogDAO.insertDepartment(c, n, d, a); }
        @Override void update(String c, String n, String d, boolean a) throws SQLException { CatalogDAO.updateDepartment(c, n, d, a); }
        @Override void delete(String code) throws SQLException { CatalogDAO.deleteDepartment(code); }
    }

    private final class PositionSection extends Section {
        private JTextField baseField;
        private JTextField allowanceField;
        private JComboBox<String> departmentCombo;

        PositionSection() {
            super(new String[] {"Mã", "Tên chức vụ", "Phòng ban", "Lương cơ bản", "Phụ cấp", "Mô tả", "Trạng thái", "Số nhân viên"},
                  new int[] {100, 150, 140, 100, 80, 160, 100, 80});
            init();
        }
        @Override void addExtraFields() {
            baseField = new JTextField(14);
            allowanceField = new JTextField(14);
            departmentCombo = new JComboBox<>();
            departmentCombo.addItem(CatalogDAO.SHARED);
            try {
                for (Object[] row : CatalogDAO.departments()) {
                    departmentCombo.addItem(String.valueOf(row[1]));
                }
            } catch (SQLException ex) {
                Dialogs.error(CatalogFrame.this, "Không tải được danh sách phòng ban: " + ex.getMessage());
            }
            addField("Phòng ban:", departmentCombo);
            addField("Lương cơ bản (đồng):", baseField);
            addField("Phụ cấp (đồng):", allowanceField);
        }
        @Override void clearExtra() {
            baseField.setText("0");
            allowanceField.setText("0");
            departmentCombo.setSelectedIndex(0);
        }
        @Override String validateExtra() {
            if (parseMoney(baseField.getText()) < 0 || parseMoney(allowanceField.getText()) < 0) {
                return "Lương cơ bản và phụ cấp phải là số không âm.";
            }
            return null;
        }
        @Override List<Object[]> load() throws SQLException { return CatalogDAO.positions(); }
        @Override void fillForm(int r) {
            codeField.setText(cell(r, 0));
            nameField.setText(cell(r, 1));
            departmentCombo.setSelectedItem(cell(r, 2));
            baseField.setText(cell(r, 3));
            allowanceField.setText(cell(r, 4));
            descField.setText(cell(r, 5));
            activeBox.setSelected("Hoạt động".equals(cell(r, 6)));
        }
        @Override void insert(String c, String n, String d, boolean a) throws SQLException {
            CatalogDAO.insertPosition(c, n, String.valueOf(departmentCombo.getSelectedItem()), parseMoney(baseField.getText()), parseMoney(allowanceField.getText()), d, a);
        }
        @Override void update(String c, String n, String d, boolean a) throws SQLException {
            CatalogDAO.updatePosition(c, n, String.valueOf(departmentCombo.getSelectedItem()), parseMoney(baseField.getText()), parseMoney(allowanceField.getText()), d, a);
        }
        @Override void delete(String code) throws SQLException { CatalogDAO.deletePosition(code); }

        /** Chấp nhận "9.500.000" hoặc "9500000"; trả -1 nếu không hợp lệ. */
        private long parseMoney(String text) {
            String digits = text.trim().replaceAll("[.,\\s]", "");
            if (!digits.matches("\\d{1,13}")) return -1;
            return Long.parseLong(digits);
        }
    }
}
