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

    public HomePanel(MainUI mainUI) {
        this.mainUI = mainUI;
        setLayout(new BorderLayout());

        LoginUser loginUser = App.currentLoginUser;
        String dbPath = loginUser.getDbFilePath();
        String pwd = loginUser.getPlaintextPassword();

        this.vm = new VaultManager(dbPath, pwd);

        VaultStatus resp = vm.connectToDB();
        if (resp != VaultStatus.DBConnectionSuccess) {
            System.out.println("Failed to connect to local DB: " + resp);
        }

        ArrayList<Entry> credentials = new ArrayList<>();
        resp = vm.openVault(credentials);
        if (resp == VaultStatus.DBOpenVaultFailure) {
            System.out.println("Failed to open local DB: " + resp);
        }

        Color darkBg = UIManager.getColor("Panel.background");
        Color textColor = UIManager.getColor("Label.foreground");

        // Header
        JLabel header = new JLabel("YAPM", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setOpaque(true);
        header.setBackground(darkBg.darker());
        header.setForeground(textColor);
        header.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(header, BorderLayout.NORTH);

        // Center wrapper panel
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

        table = new JTable(new DefaultTableModel(rowData, columnNames));

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                // Check if password column is clicked
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

                    // Clear selection to remove visual highlight
                    table.clearSelection();
                }
            }
        });

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
        add(centerWrapper, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0)); // Change 2 → 3
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.setBackground(darkBg);

        JButton addButton = new JButton("Add");
        JButton logoutButton = new JButton("Log Out");
        JButton refreshButton = new JButton("Refresh");

        Font btnFont = new Font("Segoe UI", Font.PLAIN, 14);
        addButton.setFont(btnFont);
        logoutButton.setFont(btnFont);

        addButton.setBackground(darkBg.darker());
        addButton.setForeground(textColor);
        logoutButton.setBackground(darkBg.darker());
        logoutButton.setForeground(textColor);

        refreshButton.setFont(btnFont);
        refreshButton.setBackground(darkBg.darker());
        refreshButton.setForeground(textColor);
        refreshButton.setFocusPainted(false);

        addButton.setFocusPainted(false);
        logoutButton.setFocusPainted(false);

        // Add button action: opens a modal to add a new entry
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
                String url = urlField.getText().trim();
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                VaultStatus status = vm.addEntry(url, username, password);
                if (status == VaultStatus.DBAddEntrySuccess) {
                    JOptionPane.showMessageDialog(this, "Entry added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshEntryTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add entry: " + status, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Logout button: goes back to login page
        logoutButton.addActionListener(e -> {
            mainUI.showPage("login");
        });

        refreshButton.addActionListener(e -> refreshEntryTable());

        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);

        // Footer
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

    private void refreshEntryTable() {
        ArrayList<Entry> updatedEntries = new ArrayList<>();
        VaultStatus status = vm.openVault(updatedEntries);
        if (status != VaultStatus.DBOpenVaultSuccess) {
            JOptionPane.showMessageDialog(this, "Failed to reload entries: " + status, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[][] rowData = new String[updatedEntries.size()][3];
        for (int i = 0; i < updatedEntries.size(); i++) {
            Entry e = updatedEntries.get(i);
            rowData[i][0] = e.getUsername();
            rowData[i][1] = e.getURL();
            rowData[i][2] = e.getPasswd();
        }

        DefaultTableModel model = new DefaultTableModel(rowData, new String[]{"Username", "URL", "Password"});
        table.setModel(model);
    }
}
