package com.hrpayroll.view;

import com.hrpayroll.model.DatabaseEnvironment;
import com.hrpayroll.util.DBConnection;
import com.hrpayroll.util.Session;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/** Chọn database sau khi đăng nhập thành công. Mặc định chọn sandbox an toàn hơn. */
public class EnvironmentSelectionFrame extends JFrame {
    private final JRadioButton primaryButton = new JRadioButton("Dữ liệu gốc (Primary)");
    private final JRadioButton sandboxButton = new JRadioButton("Bản sao thử nghiệm (Sandbox)", true);
    private final JPasswordField primaryPassword = new JPasswordField(20);

    public EnvironmentSelectionFrame() {
        setTitle("Lựa chọn môi trường dữ liệu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(540, 340);
        setResizable(false);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12,12));
        root.setBorder(new EmptyBorder(22,26,22,26));
        setContentPane(root);
        JLabel title = new JLabel("CHỌN CƠ SỞ DỮ LIỆU", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif",Font.BOLD,20));
        root.add(title,BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(0,1,6,6));
        ButtonGroup group = new ButtonGroup(); group.add(primaryButton); group.add(sandboxButton);
        options.add(primaryButton);
        JLabel primaryInfo = new JLabel("  Dữ liệu gốc — yêu cầu mật khẩu truy cập riêng.");
        primaryInfo.setForeground(new Color(150,70,30)); options.add(primaryInfo);
        options.add(sandboxButton);
        JLabel sandboxInfo = new JLabel("  Bản sao của dữ liệu gốc tại thời điểm khởi tạo; không yêu cầu mật khẩu riêng.");
        sandboxInfo.setForeground(new Color(40,100,150)); options.add(sandboxInfo);
        JPanel pass = new JPanel(new BorderLayout(8,0)); pass.add(new JLabel("Mật khẩu dữ liệu gốc:"),BorderLayout.WEST); pass.add(primaryPassword,BorderLayout.CENTER);
        options.add(pass);
        root.add(options,BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton continueButton = new JButton("Tiếp tục"); JButton cancelButton = new JButton("Đăng xuất");
        buttons.add(cancelButton); buttons.add(continueButton); root.add(buttons,BorderLayout.SOUTH);
        primaryButton.addActionListener(e -> primaryPassword.setEnabled(true));
        sandboxButton.addActionListener(e -> { primaryPassword.setText(""); primaryPassword.setEnabled(false); });
        primaryPassword.setEnabled(false);
        continueButton.addActionListener(e -> chooseEnvironment());
        cancelButton.addActionListener(e -> { Session.logout(); dispose(); new LoginFrame().setVisible(true); });
        getRootPane().setDefaultButton(continueButton);
    }

    private void chooseEnvironment() {
        DatabaseEnvironment env = primaryButton.isSelected() ? DatabaseEnvironment.PRIMARY : DatabaseEnvironment.SANDBOX;
        char[] password = primaryPassword.getPassword();
        try {
            if (env == DatabaseEnvironment.PRIMARY && !DBConnection.verifyPrimaryAccess(password)) {
                JOptionPane.showMessageDialog(this,"Mật khẩu truy cập dữ liệu gốc không chính xác.","Không được phép truy cập",JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (Connection ignored = DBConnection.getConnection(env)) { /* xác nhận DB tồn tại và kết nối được */ }
            Session.setDatabaseEnvironment(env);
            dispose();
            new DashboardFrame().setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,ex.getMessage(),"Không thể mở môi trường dữ liệu",JOptionPane.ERROR_MESSAGE);
        } finally { Arrays.fill(password,'\0'); primaryPassword.setText(""); }
    }
}
