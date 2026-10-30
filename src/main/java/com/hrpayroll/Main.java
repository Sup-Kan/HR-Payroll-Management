package com.hrpayroll;

import com.hrpayroll.view.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                System.err.println(
                        "Khong the thiet lap giao dien: "
                        + e.getMessage()
                );
            }

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}