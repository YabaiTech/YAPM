package org.YAPM.components;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class CopyButtonCellRenderer extends JPanel implements TableCellRenderer {

  private final JLabel textLabel;
  private final JButton copyButton;

  public CopyButtonCellRenderer() {
    setLayout(new BorderLayout());
    setOpaque(true);

    textLabel = new JLabel();

    Icon copyIcon = UIManager.getIcon("FileView.fileIcon");
    copyButton = new JButton(copyIcon);
    copyButton.setFocusable(false);
    copyButton.setBorderPainted(false);
    copyButton.setContentAreaFilled(false);

    add(textLabel, BorderLayout.CENTER);
    add(copyButton, BorderLayout.EAST);
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

    textLabel.setText(value == null ? "" : value.toString());

    Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
    Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();

    setBackground(bg);
    textLabel.setForeground(fg);
    copyButton.setForeground(fg);

    // Reset icon every time to prevent FlatLaf from resetting it
    Icon copyIcon = UIManager.getIcon("FileView.fileIcon");
    copyButton.setIcon(copyIcon);

    return this;
  }
}
