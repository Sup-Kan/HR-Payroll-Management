package com.hrpayroll;

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
                    "Không thể thiết lập giao diện: "
                    + e.getMessage()
                );
            }

            System.out.println(
                "HR-Payroll-Management started."
            );
        });
    }
}