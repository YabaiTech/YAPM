package org.YAPM;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.backend.*;
import org.vault.*;
import org.YAPM.components.*;
import org.misc.*;

public class HomePanel extends JPanel {

  private final MainUI mainUI;
  private VaultManager vm;
  private final JTable table;
  private final ArrayList<Entry> credentials = new ArrayList<>();
  private final LoadingOverlay overlay;

  public HomePanel(MainUI mainUI) {
    this.mainUI = mainUI;
    setLayout(new BorderLayout());

    // Initialize VaultManager
    LoginUser loginUser = App.currentLoginUser;
    String dbPath = loginUser.getDbFilePath();
    String pwd = loginUser.getPlaintextPassword();
    this.vm = new VaultManager(dbPath, pwd);

    // Connect and load entries
    if (vm.connectToDB() != VaultStatus.DBConnectionSuccess) {
      System.out.println("Failed to connect to DB");
    }
    if (vm.openVault(credentials) == VaultStatus.DBOpenVaultFailure) {
      System.out.println("Failed to open vault");
    }

    Color darkBg = UIManager.getColor("Panel.background");
    Color textColor = UIManager.getColor("Label.foreground");

    JLabel header = new JLabel("YAPM", SwingConstants.CENTER);
    header.setFont(new Font("Segoe UI", Font.BOLD, 24));
    header.setOpaque(true);
    header.setBackground(darkBg.darker());
    header.setForeground(textColor);
    header.setBorder(new EmptyBorder(20, 0, 20, 0));
    add(header, BorderLayout.NORTH);

    JPanel centerWrapper = new JPanel(new BorderLayout());
    centerWrapper.setBackground(darkBg);

    // Prepare table
    String[] columnNames = { "Username", "URL", "Password" };
    String[][] rowData = getRowData();

    table = new JTable(new DefaultTableModel(rowData, columnNames));
    table.setFillsViewportHeight(true);
    table.setRowHeight(30);
    table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    table.setBackground(darkBg);
    table.setForeground(textColor);
    table.setGridColor(darkBg.brighter());
    table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
    table.getTableHeader().setBackground(darkBg.darker());
    table.getTableHeader().setForeground(textColor);

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    scrollPane.getViewport().setBackground(darkBg);
    centerWrapper.add(scrollPane, BorderLayout.CENTER);

    // Initialize the spinner overlay and set invisible by default
    overlay = new LoadingOverlay();
    overlay.setVisible(false);

    // Create layered pane to hold centerWrapper and overlay
    JLayeredPane layeredPane = new JLayeredPane();
    layeredPane.setLayout(new OverlayLayout(layeredPane));
    layeredPane.add(centerWrapper, JLayeredPane.DEFAULT_LAYER);
    layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER);

    layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        overlay.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
      }
    });

    // Add layeredPane instead of centerWrapper directly
    add(layeredPane, BorderLayout.CENTER);

    // Set copy icon cell renderer/editor
    TableCellRenderer renderer = new CopyButtonCellRenderer();
    TableCellEditor editor = new CopyButtonCellEditor();
    for (int i = 0; i < table.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setCellRenderer(renderer);
      table.getColumnModel().getColumn(i).setCellEditor(editor);
    }

    JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 0));
    buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    buttonPanel.setBackground(darkBg);

    JButton addButton = new JButton("Add");
    JButton refreshButton = new JButton("Refresh");
    JButton logoutButton = new JButton("Log Out");
    JButton editButton = new JButton("Edit");
    JButton deleteButton = new JButton("Delete");

    Font btnFont = new Font("Segoe UI", Font.PLAIN, 14);
    // JButton[] buttons = { refreshButton, addButton, editButton, deleteButton, logoutButton };
    JButton[] buttons = { addButton, editButton, deleteButton, logoutButton };
    for (JButton btn : buttons) {
      btn.setFont(btnFont);
      btn.setBackground(darkBg.darker());
      btn.setForeground(textColor);
      btn.setFocusPainted(false);
      buttonPanel.add(btn);
    }

    // Add entry
    addButton.addActionListener(e -> {
      JTextField urlField = new JTextField();
      JTextField usernameField = new JTextField();
      JTextField passwordField = new JTextField();

      JLabel strengthLabel = new JLabel(" ");
      strengthLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
      JProgressBar strengthBar = new JProgressBar(0, 100);
      strengthBar.setBorder(new EmptyBorder(0, 0, 5, 0));
      JLabel checkIcon = new JLabel(); // empty label
      attachPasswordStrengthListeners(passwordField, strengthLabel, strengthBar, checkIcon);

      // generating random password with button Generate

      JButton generateButton = new JButton("Generate");
      generateButton.addActionListener(ev -> {
        PasswordGenerator pg = new PasswordGenerator(true, true, true, true);
        passwordField.setText(pg.generate(16)); // 16-character password
      });

      JPanel panel = new JPanel(new GridLayout(0, 1));
      panel.add(new JLabel("URL:"));
      panel.add(urlField);
      panel.add(new JLabel("Username:"));
      panel.add(usernameField);
      panel.add(new JLabel("Password:"));

      // adding new panel

      JPanel passRow = new JPanel(new BorderLayout());
      passRow.add(passwordField, BorderLayout.CENTER);
      passRow.add(generateButton, BorderLayout.EAST);
      panel.add(passRow);
      panel.add(strengthLabel);
      panel.add(strengthBar);

      int result = JOptionPane.showConfirmDialog(this, panel, "Add New Entry",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

      if (result == JOptionPane.OK_OPTION) {
        VaultStatus status = vm.addEntry(
            urlField.getText().trim(),
            usernameField.getText().trim(),
            passwordField.getText().trim());
        if (status == VaultStatus.DBAddEntrySuccess) {
          JOptionPane.showMessageDialog(this, "Entry added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
          refreshEntryTable();
        } else {
          JOptionPane.showMessageDialog(this, "Failed to add entry: " + status, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    // Refresh
//    refreshButton.addActionListener(e -> {
//      refreshEntryTable();
//    });

    // Logout
    logoutButton.addActionListener(e -> {
      this.vm.closeDB();
      BackendError err = App.currentLoginUser.logout();
      if (err != null) {
        JOptionPane.showMessageDialog(this, "Failed to properly log out", "Error", JOptionPane.ERROR_MESSAGE);
        System.err.println("[HomePanel.HomePanel] FATAL: Failed to properly log out: " + err.getErrorType() + " -> "
            + err.getContext());
      }
      App.currentLoginUser = null;
      mainUI.showPage("login");
    });

    // Edit entry
    editButton.addActionListener(e -> {
      int row = table.getSelectedRow();
      if (row < 0 || row >= credentials.size()) {
        JOptionPane.showMessageDialog(this, "Select an entry to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
      }

      Entry ent = credentials.get(row);
      JTextField urlField = new JTextField(ent.getURL());
      JTextField usernameField = new JTextField(ent.getUsername());
      JTextField passwordField = new JTextField(ent.getPasswd());

      JLabel strengthLabel = new JLabel(" ");
      strengthLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
      JProgressBar strengthBar = new JProgressBar(0, 100);
      strengthBar.setBorder(new EmptyBorder(0, 0, 5, 0));
      JLabel checkIcon = new JLabel(); // empty label

      attachPasswordStrengthListeners(passwordField, strengthLabel, strengthBar, checkIcon);

      // button for generating random password
      JButton generateButton = new JButton("Generate");
      generateButton.addActionListener(ev -> {
        PasswordGenerator pg = new PasswordGenerator(true, true, true, true);
        passwordField.setText(pg.generate(16));
      });

      JPanel panel = new JPanel(new GridLayout(0, 1));
      panel.add(new JLabel("URL:"));
      panel.add(urlField);
      panel.add(new JLabel("Username:"));
      panel.add(usernameField);
      panel.add(new JLabel("Password:"));

      // adding new panel
      JPanel passRow = new JPanel(new BorderLayout());
      passRow.add(passwordField, BorderLayout.CENTER);
      passRow.add(generateButton, BorderLayout.EAST);
      panel.add(passRow);
      panel.add(strengthLabel);
      panel.add(strengthBar);

      int result = JOptionPane.showConfirmDialog(this, panel, "Edit Entry",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

      if (result == JOptionPane.OK_OPTION) {
        VaultStatus st = vm.editEntry(
            ent.getID(),
            urlField.getText().trim(),
            usernameField.getText().trim(),
            passwordField.getText().trim());
        if (st == VaultStatus.DBEditEntrySuccess) {
          JOptionPane.showMessageDialog(this, "Entry updated successfully!", "Success",
              JOptionPane.INFORMATION_MESSAGE);
          refreshEntryTable();
        } else {
          JOptionPane.showMessageDialog(this, "Failed to update entry: " + st, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    // Delete entry
    deleteButton.addActionListener(e -> {
      int row = table.getSelectedRow();
      if (row < 0 || row >= credentials.size()) {
        JOptionPane.showMessageDialog(this, "Select an entry to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
      }

      Entry ent = credentials.get(row);
      int confirm = JOptionPane.showConfirmDialog(this,
          "Are you sure you want to delete '" + ent.getUsername() + "'?",
          "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

      if (confirm == JOptionPane.YES_OPTION) {
        VaultStatus st = vm.deleteEntry(ent.getID());
        if (st == VaultStatus.DBDeleteEntrySuccess) {
          JOptionPane.showMessageDialog(this, "Entry deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
          refreshEntryTable();
        } else {
          JOptionPane.showMessageDialog(this, "Delete failed: " + st, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    JLabel footer = new JLabel("\u00A9 2025 All rights reserved.", SwingConstants.CENTER);
    footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    footer.setOpaque(true);
    footer.setBackground(darkBg.darker());
    footer.setForeground(textColor);
    footer.setBorder(new EmptyBorder(20, 0, 20, 0));

    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(buttonPanel, BorderLayout.NORTH);
    southPanel.add(footer, BorderLayout.SOUTH);
    southPanel.setBackground(darkBg.darker());
    add(southPanel, BorderLayout.SOUTH);
  }

  private String[][] getRowData() {
    String[][] rowData = new String[credentials.size()][3];
    for (int i = 0; i < credentials.size(); i++) {
      Entry e = credentials.get(i);
      rowData[i][0] = e.getUsername();
      rowData[i][1] = e.getURL();
      rowData[i][2] = e.getPasswd();
    }
    return rowData;
  }

  private void refreshEntryTable() {
    credentials.clear();
    vm.close();
    overlay.setVisible(true);

    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        BackendError err = App.currentLoginUser.sync();
        if (err != null) {
          JOptionPane.showMessageDialog(null, "Failed to sync with cloud!", "Error", JOptionPane.ERROR_MESSAGE);
          System.err.println(
              "[HomePanel.refreshEntryTable] Failed to sync with cloud: " + err.getErrorType() + " -> "
                  + err.getContext());
        }

        return null;
      }

      @Override
      protected void done() {
        overlay.setVisible(false);

        String dbPath = App.currentLoginUser.getDbFilePath();
        String pwd = App.currentLoginUser.getPlaintextPassword();
        vm = new VaultManager(dbPath, pwd);

        VaultStatus resp = vm.connectToDB();
        if (resp != VaultStatus.DBConnectionSuccess) {
          JOptionPane.showMessageDialog(null, "Failed to open vault!", "Error", JOptionPane.ERROR_MESSAGE);
          System.err.println("[HomePanel.refreshEntryTable] Failed to open vault: " + resp);
        }

        VaultStatus status = vm.openVault(credentials);
        if (status != VaultStatus.DBOpenVaultSuccess) {
          JOptionPane.showMessageDialog(null, "Failed to reload entries: " + status, "Error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        String[][] rowData = getRowData();
        DefaultTableModel model = new DefaultTableModel(rowData, new String[] { "Username", "URL", "Password" });
        table.setModel(model);

        // reapply renderer/editor
        TableCellRenderer renderer = new CopyButtonCellRenderer();
        TableCellEditor editor = new CopyButtonCellEditor();
        for (int i = 0; i < table.getColumnCount(); i++) {
          table.getColumnModel().getColumn(i).setCellRenderer(renderer);
          table.getColumnModel().getColumn(i).setCellEditor(editor);
        }

      }
    };
    worker.execute();

  }

  // adding live entropy calculator
  // At top of HomePanel.java — inside the class
  private void attachPasswordStrengthListeners(JTextField passwordField, JLabel strengthLabel, JProgressBar strengthBar,
      JLabel checkIcon) {
    passwordField.getDocument().addDocumentListener(new DocumentListener() {
      private void updateStrength() {
        String pwd = passwordField.getText();
        double entropy = PasswordEntropyCalculator.calculateEntropy(pwd);
        PasswordStrength strength = PasswordEntropyCalculator.getPasswdStrength(pwd);

        strengthLabel.setText("Strength: " + strength.name());
        checkIcon.setIcon(null); // Always keep icon hidden

        if (entropy < 28) {
          strengthBar.setValue(20);
          strengthBar.setForeground(Color.RED);
        } else if (entropy < 36) {
          strengthBar.setValue(40);
          strengthBar.setForeground(Color.ORANGE);
        } else if (entropy < 60) {
          strengthBar.setValue(60);
          strengthBar.setForeground(Color.YELLOW.darker());
        } else if (entropy < 128) {
          strengthBar.setValue(80);
          strengthBar.setForeground(new Color(0, 180, 0));
        } else {
          strengthBar.setValue(100);
          strengthBar.setForeground(new Color(0, 150, 255));
          // Do not show icon even on 100%
        }
      }

      public void insertUpdate(DocumentEvent e) {
        updateStrength();
      }

      public void removeUpdate(DocumentEvent e) {
        updateStrength();
      }

      public void changedUpdate(DocumentEvent e) {
        updateStrength();
      }
    });
  }

}
