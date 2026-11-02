package com.hrpayroll.util;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Hộp thoại thông báo dùng chung để mọi form xử lý lỗi và thông báo giống nhau. */
public final class Dialogs {

    private static final String TITLE = "Thông báo";

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent, message, TITLE, JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent, message, TITLE, JOptionPane.WARNING_MESSAGE
        );
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent, message, "Lỗi", JOptionPane.ERROR_MESSAGE
        );
    }

    /** @return true nếu người dùng chọn Có */
    public static boolean confirm(
            Component parent, String message, String title) {

        return JOptionPane.showConfirmDialog(
            parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }
}
