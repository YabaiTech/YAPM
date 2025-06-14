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

    public HomePanel(MainUI mainUI) {
        this.mainUI = mainUI;
        setLayout(new BorderLayout());

        LoginUser loginUser = App.currentLoginUser;
        String dbPath = loginUser.getDbFilePath();
        String pwd = loginUser.getPlaintextPassword();

        VaultManager vm = new VaultManager(dbPath, pwd);

        VaultStatus resp = vm.connectToDB();
        if (resp != VaultStatus.DBConnectionSuccess) {
            System.out.println("Failed to connect to local DB: " + resp);
            // You can handle this error more gracefully if needed
        }

        ArrayList<Entry> credentials = new ArrayList<>();
        resp = vm.openVault(credentials);
        if (resp == VaultStatus.DBOpenVaultFailure) {
            System.out.println("Failed to open local DB: " + resp);
            // You can handle this error more gracefully if needed
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

        // Prepare data for table
        String[] columnNames = {"Username", "URL", "Password"};
        String[][] rowData = new String[credentials.size()][3];
        for (int i = 0; i < credentials.size(); i++) {
            Entry e = credentials.get(i);
            rowData[i][0] = e.getUsername();
            rowData[i][1] = e.getURL();
            rowData[i][2] = e.getPasswd();
        }

        // Create table with data
        JTable table = new JTable(new DefaultTableModel(rowData, columnNames));
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

        // Buttons panel above footer
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // 10 px gap between buttons
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // padding around buttons
        buttonPanel.setBackground(darkBg);

        JButton addButton = new JButton("Add");
        JButton logoutButton = new JButton("Log Out");

// Set fonts/colors to match theme
        Font btnFont = new Font("Segoe UI", Font.PLAIN, 14);
        addButton.setFont(btnFont);
        logoutButton.setFont(btnFont);

        addButton.setBackground(darkBg.darker());
        addButton.setForeground(textColor);
        logoutButton.setBackground(darkBg.darker());
        logoutButton.setForeground(textColor);

// Remove button focus painting for cleaner look (optional)
        addButton.setFocusPainted(false);
        logoutButton.setFocusPainted(false);

// Dummy action listeners
        addButton.addActionListener(e -> {
            // TODO: Implement Add button action
        });

        logoutButton.addActionListener(e -> {
            mainUI.showPage("login");
        });

        buttonPanel.add(addButton);
        buttonPanel.add(logoutButton);

// Footer label
        JLabel footer = new JLabel("\u00A9 2025 All rights reserved.", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setOpaque(true);
        footer.setBackground(darkBg.darker());
        footer.setForeground(textColor);
        footer.setBorder(new EmptyBorder(20, 0, 20, 0));

// Add buttons panel first, then footer to SOUTH with a wrapper panel
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(footer, BorderLayout.SOUTH);
        southPanel.setBackground(darkBg.darker());

        add(southPanel, BorderLayout.SOUTH);

    }
}
