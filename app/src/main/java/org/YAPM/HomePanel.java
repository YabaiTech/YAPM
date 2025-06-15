package org.YAPM;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import org.vault.*;
import org.backend.*;

public class HomePanel extends JPanel {

    private final MainUI mainUI;
    private final VaultManager vm;
    private final JTable table;
    private final ArrayList<Entry> credentials = new ArrayList<>();

    public HomePanel(MainUI mainUI) {
        this.mainUI = mainUI;
        setLayout(new BorderLayout());

        // Initialize VaultManager with current login user's DB path and password
        LoginUser loginUser = App.currentLoginUser;
        String dbPath = loginUser.getDbFilePath();
        String pwd = loginUser.getPlaintextPassword();
        this.vm = new VaultManager(dbPath, pwd);

        // Connect and open vault
        VaultStatus resp = vm.connectToDB();
        if (resp != VaultStatus.DBConnectionSuccess) {
            System.out.println("Failed to connect to local DB: " + resp);
        }

        resp = vm.openVault(credentials);
        if (resp == VaultStatus.DBOpenVaultFailure) {
            System.out.println("Failed to open local DB: " + resp);
        }

        // UI Colors
        Color darkBg = UIManager.getColor("Panel.background");
        Color textColor = UIManager.getColor("Label.foreground");

        // Header label
        JLabel header = new JLabel("YAPM", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setOpaque(true);
        header.setBackground(darkBg.darker());
        header.setForeground(textColor);
        header.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(header, BorderLayout.NORTH);

        // Center panel wrapper
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(darkBg);

        // Prepare table data
        String[] columnNames = {"Username", "URL", "Password"};
        String[][] rowData = new String[credentials.size()][3];
        for (int i = 0; i < credentials.size(); i++) {
            Entry e = credentials.get(i);
            rowData[i][0] = e.getUsername();
            rowData[i][1] = e.getURL();
            rowData[i][2] = e.getPasswd();
        }

        // Create table with model
        table = new JTable(new DefaultTableModel(rowData, columnNames));

        // Password cell click copies password to clipboard
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (col == 2 && row >= 0) {
                    String password = (String) table.getValueAt(row, col);
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(password), null);

                    JOptionPane.showMessageDialog(
                            HomePanel.this,
                            "Password copied to clipboard!",
                            "Copied",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    table.clearSelection();
                }
            }
        });

        // Table appearance settings
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(darkBg);
        table.setForeground(textColor);
        table.setGridColor(darkBg.brighter());
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(darkBg.darker());
        table.getTableHeader().setForeground(textColor);

        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.getViewport().setBackground(darkBg);

        centerWrapper.add(scrollPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Button panel with Add, Refresh, Edit, Delete, Logout buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.setBackground(darkBg);

        JButton addButton = new JButton("Add");
        JButton refreshButton = new JButton("Refresh");
        JButton logoutButton = new JButton("Log Out");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        Font btnFont = new Font("Segoe UI", Font.PLAIN, 14);
        JButton[] buttons = {refreshButton, addButton, editButton, deleteButton, logoutButton};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setBackground(darkBg.darker());
            btn.setForeground(textColor);
            btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        // Add button action: show dialog to add new entry
        addButton.addActionListener(e -> {
            JTextField urlField = new JTextField();
            JTextField usernameField = new JTextField();
            JTextField passwordField = new JTextField();

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("URL:"));
            panel.add(urlField);
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Add New Entry",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                VaultStatus status = vm.addEntry(
                        urlField.getText().trim(),
                        usernameField.getText().trim(),
                        passwordField.getText().trim()
                );
                if (status == VaultStatus.DBAddEntrySuccess) {
                    JOptionPane.showMessageDialog(this, "Entry added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshEntryTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add entry: " + status, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Refresh button reloads table data
        refreshButton.addActionListener(e -> refreshEntryTable());

        // Logout button returns to login page
        logoutButton.addActionListener(e -> mainUI.showPage("login"));

        // Edit button: edit selected entry with dialog
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

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("URL:"));
            panel.add(urlField);
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Entry",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                VaultStatus st = vm.editEntry(
                        ent.getID(),
                        urlField.getText().trim(),
                        usernameField.getText().trim(),
                        passwordField.getText().trim()
                );
                if (st == VaultStatus.DBEditEntrySuccess) {
                    JOptionPane.showMessageDialog(this, "Entry updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshEntryTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update entry: " + st, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Delete button: confirm and delete selected entry
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

        // Footer label
        JLabel footer = new JLabel("\u00A9 2025 All rights reserved.", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setOpaque(true);
        footer.setBackground(darkBg.darker());
        footer.setForeground(textColor);
        footer.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Bottom panel with buttons and footer
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(footer, BorderLayout.SOUTH);
        southPanel.setBackground(darkBg.darker());
        add(southPanel, BorderLayout.SOUTH);
    }

    // Reload the entries from vault and update the table model
    private void refreshEntryTable() {
        credentials.clear(); // Clear old entries
        VaultStatus status = vm.openVault(credentials);
        if (status != VaultStatus.DBOpenVaultSuccess) {
            JOptionPane.showMessageDialog(this, "Failed to reload entries: " + status, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[][] rowData = new String[credentials.size()][3];
        for (int i = 0; i < credentials.size(); i++) {
            Entry e = credentials.get(i);
            rowData[i][0] = e.getUsername();
            rowData[i][1] = e.getURL();
            rowData[i][2] = e.getPasswd();
        }

        DefaultTableModel model = new DefaultTableModel(rowData, new String[]{"Username", "URL", "Password"});
        table.setModel(model);
    }
}
