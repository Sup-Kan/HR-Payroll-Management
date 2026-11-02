package com.hrpayroll.util;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** Dựng form dạng lưới nhãn - trường dùng chung cho các màn hình. */
public final class FormUtils {

    private FormUtils() {
    }

    /** Một hàng gồm hai cặp nhãn - trường (cột 0-1 và cột 2-3). */
    public static void addRow(
            JPanel panel,
            int row,
            String label1,
            JComponent field1,
            String label2,
            JComponent field2) {

        GridBagConstraints gbc = constraints();

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label1), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(label2), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        panel.add(field2, gbc);
    }

    /** Một hàng chỉ có một trường, trải rộng hết các cột còn lại. */
    public static void addWideRow(
            JPanel panel, int row, String label, JComponent field) {

        GridBagConstraints gbc = constraints();

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    /** Nội dung hộp thoại xác nhận cập nhật: chỉ liệt kê các trường thay đổi. */
    public static String buildUpdateComparison(
            String[] columns, Object[] oldData, Object[] newData) {

        String header = "Nội dung trước và sau khi cập nhật:\n\n";
        StringBuilder message = new StringBuilder(header);

        for (int i = 0; i < columns.length; i++) {
            String before = String.valueOf(oldData[i]);
            String after = String.valueOf(newData[i]);

            if (!before.equals(after)) {
                message.append(columns[i]).append(":\n")
                    .append("  Trước: ").append(before).append("\n")
                    .append("  Sau:   ").append(after).append("\n\n");
            }
        }

        if (message.toString().equals(header)) {
            message.append("Không có trường nào thay đổi.\n\n");
        }

        message.append("Bạn có xác nhận cập nhật các thay đổi này không?");
        return message.toString();
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }
}
