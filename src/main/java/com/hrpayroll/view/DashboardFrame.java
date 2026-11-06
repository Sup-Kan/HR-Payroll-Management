package com.hrpayroll.view;

import com.hrpayroll.dao.PresenceDAO;
import com.hrpayroll.model.Feature;
import com.hrpayroll.model.DatabaseEnvironment;
import com.hrpayroll.model.Role;
import com.hrpayroll.util.DBConnection;
import com.hrpayroll.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class DashboardFrame extends JFrame {

    /** Mỗi chức năng chỉ mở một cửa sổ; bấm lại sẽ đưa cửa sổ đó lên trước. */
    private final Map<Feature, JFrame> openFrames =
            new EnumMap<>(Feature.class);

    private final String presenceUser = Session.getUsername();
    private final javax.swing.Timer heartbeat = new javax.swing.Timer(
            PresenceDAO.HEARTBEAT_SECONDS * 1000, e -> sendPresence(true));
    private final JLabel statusLabel = new JLabel();
    private final JLabel environmentBadge = new JLabel("", SwingConstants.CENTER);

    public DashboardFrame() {
        setTitle("Dashboard - Quản lý Nhân sự & Tiền lương");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(mainPanel);

        // Phần tiêu đề
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));

        JLabel titleLabel = new JLabel(
                "HỆ THỐNG QUẢN LÝ NHÂN SỰ & TIỀN LƯƠNG"
        );
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        // Chừa chỗ phía trên để dấu chồng (Ố, Ề, Ấ) không bị cắt.
        titleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel subtitleLabel = new JLabel(
                "Dashboard - Màn hình chính"
        );
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 6));
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        environmentBadge.setOpaque(true);
        environmentBadge.setForeground(Color.WHITE);
        environmentBadge.setFont(new Font("SansSerif", Font.BOLD, 13));
        environmentBadge.setBorder(new EmptyBorder(6, 12, 6, 12));

        JButton switchButton = new JButton("Đổi môi trường");
        Role role = Session.getRole();
        boolean canSwitch = role == Role.ADMIN || role == Role.IT;
        switchButton.setEnabled(canSwitch);
        if (!canSwitch) {
            switchButton.setToolTipText(
                    "Chỉ Quản trị hệ thống và Công nghệ thông tin được đổi môi trường");
        }

        JButton logoutButton = new JButton("Đăng xuất");
        JButton exitButton = new JButton("Thoát");

        JPanel sessionButtons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        sessionButtons.add(environmentBadge);
        sessionButtons.add(switchButton);
        sessionButtons.add(logoutButton);
        sessionButtons.add(exitButton);

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(sessionButtons, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Các nút chức năng, bật/tắt theo vai trò đang đăng nhập
        // Chỉ hiện các chức năng vai trò hiện tại được dùng.
        JPanel menuPanel = new JPanel(new GridLayout(0, 2, 16, 16));
        menuPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        for (Feature feature : Feature.values()) {
            if (Session.canAccess(feature)) {
                addMenuButton(menuPanel, feature);
            }
        }

        if (menuPanel.getComponentCount() == 0) {
            menuPanel.setLayout(new BorderLayout());
            menuPanel.add(new JLabel(
                    "Tài khoản chưa được cấp chức năng nào.",
                    SwingConstants.CENTER));
        }

        // Ít chức năng thì giữ ô nút vừa phải, không kéo dãn kín màn hình.
        if (menuPanel.getComponentCount() <= 2) {
            JPanel top = new JPanel(new BorderLayout());
            menuPanel.setPreferredSize(new Dimension(0, 170));
            top.add(menuPanel, BorderLayout.NORTH);
            mainPanel.add(top, BorderLayout.CENTER);
        } else {
            mainPanel.add(menuPanel, BorderLayout.CENTER);
        }

        // Thanh trạng thái
        refreshEnvironmentDisplay();
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        switchButton.addActionListener(e -> switchEnvironment());

        // Báo "đang trực tuyến" định kỳ; xóa khi đóng Dashboard (đăng xuất/thoát/đóng cửa sổ).
        sendPresence(true);
        heartbeat.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopPresence();
            }
        });

        logoutButton.addActionListener(e -> logout());
        exitButton.addActionListener(e -> exitApplication());

        setJMenuBar(buildMenuBar(canSwitch));
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có muốn đăng xuất không?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            closeModuleWindows();
            stopPresence();
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn thoát chương trình?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            closeModuleWindows();
            stopPresence();
            dispose();
        }
    }

    /**
     * Thanh menu: Hệ thống, Chức năng (chỉ các mục vai trò được dùng) và
     * Trợ giúp. Các lệnh dùng chung xử lý với các nút trên Dashboard.
     */
    private JMenuBar buildMenuBar(boolean canSwitch) {
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenu systemMenu = new JMenu("Hệ thống");
        systemMenu.setMnemonic(java.awt.event.KeyEvent.VK_H);

        JMenuItem switchItem = new JMenuItem("Đổi môi trường dữ liệu...");
        switchItem.setEnabled(canSwitch);
        switchItem.addActionListener(e -> switchEnvironment());

        JMenuItem logoutItem = new JMenuItem("Đăng xuất");
        logoutItem.setAccelerator(KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_L, ctrl));
        logoutItem.addActionListener(e -> logout());

        JMenuItem exitItem = new JMenuItem("Thoát");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_Q, ctrl));
        exitItem.addActionListener(e -> exitApplication());

        systemMenu.add(switchItem);
        systemMenu.addSeparator();
        systemMenu.add(logoutItem);
        systemMenu.add(exitItem);

        JMenu featureMenu = new JMenu("Chức năng");
        featureMenu.setMnemonic(java.awt.event.KeyEvent.VK_C);

        int shortcut = 0;
        for (Feature feature : Feature.values()) {
            if (!Session.canAccess(feature)) {
                continue;
            }
            JMenuItem item = new JMenuItem(feature.getTitle());
            item.setToolTipText(feature.getDescription());
            if (++shortcut <= 9) {
                item.setAccelerator(KeyStroke.getKeyStroke(
                        java.awt.event.KeyEvent.VK_0 + shortcut, ctrl));
            }
            item.addActionListener(e -> openFeature(feature));
            featureMenu.add(item);
        }
        if (featureMenu.getItemCount() == 0) {
            JMenuItem none = new JMenuItem("(Chưa được cấp chức năng nào)");
            none.setEnabled(false);
            featureMenu.add(none);
        }

        JMenu helpMenu = new JMenu("Trợ giúp");
        helpMenu.setMnemonic(java.awt.event.KeyEvent.VK_T);
        JMenuItem aboutItem = new JMenuItem("Giới thiệu");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Hệ thống Quản lý Nhân sự & Tiền lương\n"
                + "Java Swing + JDBC + MariaDB\n\n"
                + buildStatusText(),
                "Giới thiệu",
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(systemMenu);
        menuBar.add(featureMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /** Ghi trạng thái trên luồng nền để không làm đơ giao diện; lỗi DB bị bỏ qua. */
    private void sendPresence(boolean online) {
        Thread t = new Thread(() -> {
            try {
                if (online) {
                    PresenceDAO.touch(presenceUser);
                } else {
                    PresenceDAO.clear(presenceUser);
                }
            } catch (Exception ex) {
                ex.printStackTrace(); // thường do thiếu cột last_seen_at
            }
        }, "presence");
        t.setDaemon(false);
        t.start();
    }

    private void stopPresence() {
        heartbeat.stop();
        sendPresence(false);
    }

    private void refreshEnvironmentDisplay() {
        DatabaseEnvironment env = Session.getDatabaseEnvironment();
        if (env == DatabaseEnvironment.PRIMARY) {
            environmentBadge.setText("PRIMARY - Dữ liệu gốc");
            environmentBadge.setBackground(new Color(214, 80, 30));
        } else if (env == DatabaseEnvironment.SANDBOX) {
            environmentBadge.setText("SANDBOX - Thử nghiệm");
            environmentBadge.setBackground(new Color(30, 110, 190));
        } else {
            environmentBadge.setText("Chưa chọn DB");
            environmentBadge.setBackground(Color.GRAY);
        }
        statusLabel.setText(buildStatusText());
    }

    /** Chuyển sang môi trường còn lại; chỉ Admin/IT, đóng mọi cửa sổ chức năng. */
    private void switchEnvironment() {
        Role role = Session.getRole();
        if (role != Role.ADMIN && role != Role.IT) {
            JOptionPane.showMessageDialog(this,
                    "Bạn không có quyền đổi môi trường dữ liệu.",
                    "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DatabaseEnvironment current = Session.getDatabaseEnvironment();
        DatabaseEnvironment target = current == DatabaseEnvironment.PRIMARY
                ? DatabaseEnvironment.SANDBOX : DatabaseEnvironment.PRIMARY;

        char[] password = new char[0];
        try {
            if (target == DatabaseEnvironment.PRIMARY) {
                JPasswordField field = new JPasswordField(20);
                int ok = JOptionPane.showConfirmDialog(this,
                        new Object[]{"Nhập mật khẩu truy cập dữ liệu gốc:", field},
                        "Chuyển sang Primary", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                password = field.getPassword();
                field.setText("");
                if (ok != JOptionPane.OK_OPTION) {
                    return;
                }
                if (!DBConnection.verifyPrimaryAccess(password)) {
                    JOptionPane.showMessageDialog(this,
                            "Mật khẩu truy cập dữ liệu gốc không chính xác.",
                            "Không được phép truy cập", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try (Connection ignored = DBConnection.getConnection(target)) {
                // xác nhận DB tồn tại và kết nối được
            }

            boolean hasOpen = openFrames.values().stream()
                    .anyMatch(Window::isDisplayable);
            String msg = "Chuyển sang " + target.getLabel() + "?";
            if (hasOpen) {
                msg += "\nTất cả cửa sổ chức năng đang mở sẽ bị đóng "
                        + "(dữ liệu chưa lưu sẽ mất).";
            }
            int choice = JOptionPane.showConfirmDialog(this, msg,
                    "Xác nhận đổi môi trường", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            closeModuleWindows();
            Session.setDatabaseEnvironment(target);
            refreshEnvironmentDisplay();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Không thể mở môi trường dữ liệu", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String buildStatusText() {
        if (!Session.isLoggedIn()) {
            return "Chưa đăng nhập";
        }

        DatabaseEnvironment env = Session.getDatabaseEnvironment();
        String environment = env == null ? "Chưa chọn DB" : env.getLabel() + " (" + env.getDatabaseName() + ")";
        return "Người dùng: " + Session.getUsername()
                + "  |  Vai trò: " + Session.getRole().getLabel()
                + "  |  Môi trường: " + environment;
    }

    private void addMenuButton(JPanel menuPanel, Feature feature) {
        JButton button = new JButton(
                "<html><div style='text-align:center;'>"
                + "<b>" + feature.getTitle() + "</b><br><br>"
                + feature.getDescription()
                + "</div></html>"
        );

        button.setFont(new Font("SansSerif", Font.PLAIN, 15));
        button.setFocusPainted(false);

        if (Session.canAccess(feature)) {
            button.addActionListener(e -> openFeature(feature));
        } else {
            button.setEnabled(false);
            button.setToolTipText(
                    "Vai trò hiện tại không có quyền truy cập chức năng này"
            );
        }

        menuPanel.add(button);
    }

    private void openFeature(Feature feature) {
        // Kiểm tra lại ở đây phòng khi hàm được gọi từ nơi khác ngoài nút.
        if (!Session.canAccess(feature)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền truy cập chức năng này.",
                    "Không đủ quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFrame frame = openFrames.get(feature);

        if (frame != null && frame.isDisplayable()) {
            if (frame.getState() == Frame.ICONIFIED) {
                frame.setState(Frame.NORMAL);
            }
            frame.toFront();
            frame.requestFocus();
            return;
        }

        frame = createFrame(feature);
        openFrames.put(feature, frame);
        frame.setVisible(true);
    }

    private JFrame createFrame(Feature feature) {
        switch (feature) {
            case EMPLOYEE:
                return new EmployeeFrame();
            case ATTENDANCE:
                return new AttendanceFrame();
            case REWARD_DISCIPLINE:
                return new RewardDisciplineFrame();
            case PAYROLL:
                return new PayrollFrame();
            case REPORT:
                return new ReportFrame();
            case ACCOUNT:
                return new AccountFrame();
            case CATALOG:
                return new CatalogFrame();
            case PROFILE:
                return new ProfileFrame();
            default:
                throw new IllegalArgumentException(
                    "Chưa có màn hình cho chức năng: " + feature
                );
        }
    }

    private void closeModuleWindows() {
        for (JFrame frame : openFrames.values()) {
            frame.dispose();
        }
        openFrames.clear();
    }
}
