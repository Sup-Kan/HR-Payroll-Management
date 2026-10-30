package com.hrpayroll.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Định dạng chung cho JTable: tiêu đề cột, căn lề dữ liệu theo từng cột,
 * độ rộng cột. Dùng cho mọi màn hình có bảng để giao diện đồng nhất.
 *
 * Cách dùng:
 *   TableStyler.apply(table,
 *       new int[] {80, 160, 100},
 *       new int[] {SwingConstants.CENTER, SwingConstants.LEFT, SwingConstants.RIGHT});
 */
public final class TableStyler {

    private static final Color HEADER_BG = new Color(220, 235, 250);
    private static final Color HEADER_FG = new Color(35, 55, 75);
    private static final Color GRID_COLOR = new Color(160, 175, 190);
    private static final int ROW_HEIGHT = 25;
    private static final int HEADER_HEIGHT = 30;
    private static final int MIN_COLUMN_WIDTH = 50;

    private TableStyler() {
    }

    /**
     * @param widths     độ rộng ưu tiên của từng cột (px), theo thứ tự cột
     * @param alignments SwingConstants.LEFT / CENTER / RIGHT cho dữ liệu từng cột
     */
    public static void apply(JTable table, int[] widths, int[] alignments) {
        if (widths.length != table.getColumnCount()
                || alignments.length != table.getColumnCount()) {
            throw new IllegalArgumentException(
                    "Số phần tử widths/alignments phải bằng số cột của bảng ("
                    + table.getColumnCount() + ").");
        }

        table.setRowHeight(ROW_HEIGHT);
        table.setFillsViewportHeight(true);
        table.setGridColor(GRID_COLOR);
        table.setShowGrid(true);
        // Các cột co giãn theo khung chứa: không bị cắt cột cuối, không cuộn ngang.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        styleHeader(table);

        for (int i = 0; i < widths.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(MIN_COLUMN_WIDTH);
            column.setPreferredWidth(widths[i]);
            column.setCellRenderer(new AlignedCellRenderer(alignments[i]));
        }
    }

    private static void styleHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);

        // Bọc renderer mặc định của giao diện (Look and Feel) thay vì thay thế,
        // để vẫn giữ mũi tên sắp xếp của TableRowSorter.
        final TableCellRenderer base = header.getDefaultRenderer();
        final Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR),
                BorderFactory.createEmptyBorder(0, 6, 0, 6));

        header.setDefaultRenderer((t, value, isSelected, hasFocus, row, col) -> {
            Component c = base.getTableCellRendererComponent(
                    t, value, isSelected, hasFocus, row, col);
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setBackground(HEADER_BG);
                label.setForeground(HEADER_FG);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setBorder(border);
                label.setOpaque(true);
            }
            return c;
        });

        header.setPreferredSize(new Dimension(
                header.getPreferredSize().width, HEADER_HEIGHT));
    }

    /** Căn lề dữ liệu theo cột và chừa lề trái/phải để chữ không sát đường kẻ. */
    private static final class AlignedCellRenderer extends DefaultTableCellRenderer {

        private final int alignment;

        AlignedCellRenderer(int alignment) {
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alignment);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            return this;
        }
    }
}
