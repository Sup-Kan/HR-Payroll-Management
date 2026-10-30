package com.hrpayroll.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Hệ thống Quản lý Nhân sự & Tiền lương");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 330);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(24, 32, 24, 32));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel(
                "ĐĂNG NHẬP HỆ THỐNG",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 21));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Tên đăng nhập:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Mật khẩu:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JButton loginButton = new JButton("Đăng nhập");
        JButton resetButton = new JButton("Làm mới");
        JButton exitButton = new JButton("Thoát");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0)
        );
        buttonPanel.add(loginButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exitButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> handleLogin());

        resetButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            usernameField.requestFocusInWindow();
        });

        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc chắn muốn thoát chương trình?",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
        });

        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        try {
            if (username.isEmpty() || password.length == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.",
                        "Thiếu thông tin",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Chỉ mô phỏng đăng nhập trong Giai đoạn 3.
            // Chưa xác thực thông tin từ cơ sở dữ liệu.
            JOptionPane.showMessageDialog(
                    this,
                    "Đăng nhập mô phỏng thành công!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();
            new DashboardFrame().setVisible(true);

        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}