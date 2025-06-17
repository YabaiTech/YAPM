package org.YAPM.components;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CopyButtonCellEditor extends AbstractCellEditor implements TableCellEditor {

  private final JPanel panel;
  private final JLabel textLabel;
  private final JButton copyButton;
  private String cellValue;

  public CopyButtonCellEditor() {
    panel = new JPanel(new BorderLayout());
    textLabel = new JLabel();
    copyButton = new JButton("📋");

    copyButton.setFocusable(false);
    copyButton.setBorderPainted(false);
    copyButton.setContentAreaFilled(false);

    panel.add(textLabel, BorderLayout.CENTER);
    panel.add(copyButton, BorderLayout.EAST);

    copyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (cellValue != null) {
          Toolkit.getDefaultToolkit().getSystemClipboard()
              .setContents(new StringSelection(cellValue), null);
          JOptionPane.showMessageDialog(panel, "Copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        }
      }
    });
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    cellValue = value == null ? "" : value.toString();
    textLabel.setText(cellValue);
    return panel;
  }

  @Override
  public Object getCellEditorValue() {
    return cellValue;
  }
}
