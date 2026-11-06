package com.hrpayroll.view;

import com.hrpayroll.dao.ProfileDAO;
import com.hrpayroll.util.Dialogs;
import com.hrpayroll.util.Session;
import com.hrpayroll.util.TableStyler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Thông tin cá nhân (chỉ đọc) của tài khoản đang đăng nhập: hồ sơ, chấm
 * công, khen thưởng/kỷ luật đã duyệt và phiếu lương các kỳ đã chốt.
 */
public class ProfileFrame extends JFrame {

    private static final String[] ATT_COLS = {
        "Ngày", "Giờ vào", "Giờ ra", "Loại", "Giờ làm", "Giờ OT",
        "Trạng thái", "Ghi chú"
    };
    private static final String[] DEC_COLS = {
        "Mã quyết định", "Loại", "Ngày", "Lý do", "Số tiền"
    };
    private static final String[] PAY_COLS = {
        "Kỳ", "Lương cơ bản", "Phụ cấp", "Công", "Giờ OT",
        "Lương theo công", "Tiền OT", "Thưởng", "Bảo hiểm", "Thuế TNCN",
        "Khấu trừ kỷ luật", "Lương gộp", "Thực nhận"
    };

    public ProfileFrame() {
        setTitle("Thông tin cá nhân");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(14, 16, 14, 16));
        setContentPane(main);

        JButton back = new JButton("Quay lại");
        back.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(back);
        main.add(south, BorderLayout.SOUTH);

        String code;
        try {
            code = ProfileDAO.findEmployeeCode(Session.getUsername());
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể đọc tài khoản: " + ex.getMessage());
            code = null;
        }

        if (code == null) {
            main.add(message("Tài khoản chưa liên kết hồ sơ nhân viên."),
                     BorderLayout.CENTER);
            return;
        }

        try {
            List<String[]> profile = ProfileDAO.profile(code);
            if (profile.isEmpty()) {
                main.add(message("Không tìm thấy hồ sơ " + code
                        + " trong môi trường dữ liệu đang chọn."),
                        BorderLayout.CENTER);
                return;
            }

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Hồ sơ", profilePanel(profile));
            tabs.addTab("Chấm công", tablePanel(ATT_COLS, ProfileDAO.attendance(code),
                    new int[] {90, 70, 70, 110, 70, 70, 100, 220}));
            tabs.addTab("Khen thưởng / Kỷ luật", tablePanel(DEC_COLS,
                    ProfileDAO.decisions(code), new int[] {120, 100, 90, 360, 110}));
            tabs.addTab("Phiếu lương", tablePanel(PAY_COLS, ProfileDAO.payslips(code),
                    new int[] {70, 100, 90, 60, 60, 110, 90, 90, 90, 90, 110, 100, 110}));
            main.add(tabs, BorderLayout.CENTER);
        } catch (Exception ex) {
            Dialogs.error(this, "Không thể tải dữ liệu cá nhân: " + ex.getMessage());
        }
    }

    private static JLabel message(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        return label;
    }

    private static JPanel profilePanel(List<String[]> rows) {
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 10));
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));
        for (String[] row : rows) {
            JLabel name = new JLabel(row[0] + ":");
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            grid.add(name);
            grid.add(new JLabel(row[1]));
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(grid, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent tablePanel(String[] cols, List<Object[]> rows, int[] widths) {
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Object[] row : rows) {
            model.addRow(row);
        }

        JTable table = new JTable(model);
        int[] align = new int[cols.length];
        java.util.Arrays.fill(align, SwingConstants.CENTER);
        TableStyler.apply(table, widths, align);

        if (rows.isEmpty()) {
            JPanel p = new JPanel(new BorderLayout());
            p.add(new JScrollPane(table), BorderLayout.CENTER);
            p.add(new JLabel("Chưa có dữ liệu.", SwingConstants.CENTER), BorderLayout.SOUTH);
            return p;
        }
        return new JScrollPane(table);
    }
}
