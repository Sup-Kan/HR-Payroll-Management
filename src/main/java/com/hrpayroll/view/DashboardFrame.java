package com.hrpayroll.view;

import com.hrpayroll.model.Feature;
import com.hrpayroll.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class DashboardFrame extends JFrame {

    /** Mỗi chức năng chỉ mở một cửa sổ; bấm lại sẽ đưa cửa sổ đó lên trước. */
    private final Map<Feature, JFrame> openFrames =
            new EnumMap<>(Feature.class);

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

        JButton logoutButton = new JButton("Đăng xuất");
        JButton exitButton = new JButton("Thoát");

        JPanel sessionButtons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        sessionButtons.add(logoutButton);
        sessionButtons.add(exitButton);

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(sessionButtons, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Các nút chức năng, bật/tắt theo vai trò đang đăng nhập
        JPanel menuPanel = new JPanel(new GridLayout(3, 2, 16, 16));
        menuPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        for (Feature feature : Feature.values()) {
            addMenuButton(menuPanel, feature);
        }

        mainPanel.add(menuPanel, BorderLayout.CENTER);

        // Thanh trạng thái
        JLabel statusLabel = new JLabel(buildStatusText());
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có muốn đăng xuất không?",
                    "Xác nhận đăng xuất",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                closeModuleWindows();
                Session.logout();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc chắn muốn thoát chương trình?",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                closeModuleWindows();
                dispose();
            }
        });
    }

    private String buildStatusText() {
        if (!Session.isLoggedIn()) {
            return "Chưa đăng nhập";
        }

        return "Người dùng: " + Session.getUsername()
                + "  |  Vai trò: " + Session.getRole().getLabel();
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
