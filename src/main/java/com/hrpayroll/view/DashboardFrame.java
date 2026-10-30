package com.hrpayroll.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

//import com.hrpayroll.view.RewardDisciplineFrame;

public class DashboardFrame extends JFrame {

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

        JLabel subtitleLabel = new JLabel(
                "Dashboard - Màn hình chính"
        );
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 6));
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        JButton logoutButton = new JButton("Đăng xuất");

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Các nút chức năng
        JPanel menuPanel = new JPanel(new GridLayout(3, 2, 16, 16));
        menuPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        addMenuButton(
                menuPanel,
                "Quản lý nhân viên",
                "Hồ sơ và thông tin nhân sự",
                "EmployeeFrame"
        );

        addMenuButton(
                menuPanel,
                "Quản lý chấm công",
                "Ngày công và giờ làm",
                "AttendanceFrame"
        );

        addMenuButton(
                menuPanel,
                "Khen thưởng / Kỷ luật",
                "Theo dõi các quyết định",
                "RewardDisciplineFrame"
        );

        addMenuButton(
                menuPanel,
                "Quản lý tiền lương",
                "Theo dõi bảng lương",
                "PayrollFrame"
        );

        addMenuButton(
                menuPanel,
                "Báo cáo, thống kê",
                "Tổng hợp dữ liệu hệ thống",
                "ReportFrame"
        );

        addMenuButton(
                menuPanel,
                "Quản lý tài khoản",
                "Tài khoản người dùng",
                "AccountFrame"
        );

        mainPanel.add(menuPanel, BorderLayout.CENTER);

        // Thanh trạng thái
        JLabel statusLabel = new JLabel(
                "Trạng thái: Giao diện thử nghiệm"
        );
        statusLabel.setBorder(
                new EmptyBorder(8, 0, 0, 0)
        );

        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // Đăng xuất về màn hình đăng nhập
        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có muốn đăng xuất không?",
                    "Xác nhận đăng xuất",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private void addMenuButton(
            JPanel menuPanel,
            String title,
            String description,
            String frameName
    ) {
        JButton button = new JButton(
                "<html><div style='text-align:center;'>"
                + "<b>" + title + "</b><br><br>"
                + description
                + "</div></html>"
        );

        button.setFont(new Font("SansSerif", Font.PLAIN, 15));
        button.setFocusPainted(false);

        button.addActionListener(e ->
                openPlaceholderFrame(title, frameName)
        );

        menuPanel.add(button);
    }

    private void openPlaceholderFrame(
            String title,
            String frameName
    ) {
        if ("EmployeeFrame".equals(frameName)) {
            new EmployeeFrame().setVisible(true);
            return;
        }
        
        if ("AttendanceFrame".equals(frameName)) {
            new AttendanceFrame().setVisible(true);
            return;
        }
        
//        if ("RewardDisciplineFrame".equals(frameName)) {
//            new RewardDisciplineFrame().setVisible(true);
//            return;
//        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(650, 420);
        frame.setLocationRelativeTo(this);

        JLabel message = new JLabel(
                "<html><div style='text-align:center;'>"
                + "<h2>" + title + "</h2>"
                + "<p>Màn hình này sẽ được triển khai "
                + "ở bước tiếp theo.</p>"
                + "<p>Hiện tại đây là giao diện giữ chỗ.</p>"
                + "</div></html>",
                SwingConstants.CENTER
        );

        frame.add(message, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}