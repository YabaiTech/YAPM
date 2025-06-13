package org.YAPM;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Map;
import org.vault.*;

public class HomePanel extends JPanel {

    public HomePanel(MainUI mainUI) {
        setLayout(new BorderLayout());

        Color darkBg = UIManager.getColor("Panel.background");
        Color textColor = UIManager.getColor("Label.foreground");

        JLabel header = new JLabel("YAPM", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setOpaque(true);
        header.setBackground(darkBg.darker());
        header.setForeground(textColor);
        header.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(header, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(darkBg);

        JButton viewVaultButton = new JButton("View Vault Entries");
        viewVaultButton.addActionListener(e -> {
            VaultManager vm = mainUI.getVaultManager();
            ArrayList<Entry> entries = new ArrayList<>();
            VaultStatus status = vm.openVault(entries);
            if (status != VaultStatus.DBOpenVaultSuccess) {
                JOptionPane.showMessageDialog(this, "Failed to open vault: " + status);
                return;
            }
            StringBuilder sb = new StringBuilder("Stored Entries:\n\n");
            for (Entry entry : entries) {
                sb.append("URL: ").append(entry.getURL()).append("\n")
                        .append("Username: ").append(entry.getUsername()).append("\n")
                        .append("Password: ").append(entry.getPasswd()).append("\n\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Vault Entries", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton addEntryButton = new JButton("Add Entry");
        addEntryButton.addActionListener(e -> {
            String url = JOptionPane.showInputDialog(this, "Enter URL:");
            String uname = JOptionPane.showInputDialog(this, "Enter Username:");
            String pwd = JOptionPane.showInputDialog(this, "Enter Password:");

            VaultManager vm = mainUI.getVaultManager();
            VaultStatus status = vm.addEntry(url, uname, pwd);
            if (status == VaultStatus.DBAddEntrySuccess) {
                JOptionPane.showMessageDialog(this, "Entry added successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add entry: " + status);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(darkBg);
        buttonPanel.add(viewVaultButton);
        buttonPanel.add(addEntryButton);

        centerWrapper.add(buttonPanel);
        add(centerWrapper, BorderLayout.CENTER);

        JLabel footer = new JLabel("\u00A9 2025 All rights reserved.", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setOpaque(true);
        footer.setBackground(darkBg.darker());
        footer.setForeground(textColor);
        footer.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(footer, BorderLayout.SOUTH);
    }
}
