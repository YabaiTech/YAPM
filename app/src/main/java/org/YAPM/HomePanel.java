package org.YAPM;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
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

    // Added spinner overlay field
    private final DarkLoadingOverlay overlay;

    public HomePanel(MainUI mainUI) {
        this.mainUI = mainUI;
        setLayout(new BorderLayout());

        LoginUser loginUser = App.currentLoginUser;
        String dbPath = loginUser.getDbFilePath();
        String pwd = loginUser.getPlaintextPassword();
        this.vm = new VaultManager(dbPath, pwd);

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

        String[] columnNames = {"Username", "URL", "Password"};
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
        overlay = new DarkLoadingOverlay();
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
        JButton[] buttons = {refreshButton, addButton, editButton, deleteButton, logoutButton};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setBackground(darkBg.darker());
            btn.setForeground(textColor);
            btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        // Adjusted refreshButton action to show/hide spinner overlay (no modal dialog)
        refreshButton.addActionListener(e -> {
            overlay.setVisible(true);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    refreshEntryTable();
                    return null;
                }

                @Override
                protected void done() {
                    overlay.setVisible(false);
                }
            };
            worker.execute();
        });

        logoutButton.addActionListener(e -> {
            this.vm.closeDB();
            BackendError err = App.currentLoginUser.logout();
            if (err != null) {
                JOptionPane.showMessageDialog(this, "Failed to properly log out", "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("[HomePanel.HomePanel] FATAL: Failed to properly log out: " + err.getErrorType() + " -> " + err.getContext());
            }
            App.currentLoginUser = null;
            mainUI.showPage("login");
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

        BackendError err = App.currentLoginUser.sync();
        if (err != null) {
            JOptionPane.showMessageDialog(this, "Failed to sync with cloud!", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("[HomePanel.refreshEntryTable] Failed to sync with cloud: " + err.getErrorType() + " -> " + err.getContext());
        }

        String dbPath = App.currentLoginUser.getDbFilePath();
        String pwd = App.currentLoginUser.getPlaintextPassword();
        this.vm = new VaultManager(dbPath, pwd);
        VaultStatus resp = vm.connectToDB();
        if (resp != VaultStatus.DBConnectionSuccess) {
            JOptionPane.showMessageDialog(this, "Failed to open vault!", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("[HomePanel.refreshEntryTable] Failed to open vault: " + resp);
        }

        VaultStatus status = vm.openVault(credentials);
        if (status != VaultStatus.DBOpenVaultSuccess) {
            JOptionPane.showMessageDialog(this, "Failed to reload entries: " + status, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[][] rowData = getRowData();
        DefaultTableModel model = new DefaultTableModel(rowData, new String[]{"Username", "URL", "Password"});
        table.setModel(model);

        TableCellRenderer renderer = new CopyButtonCellRenderer();
        TableCellEditor editor = new CopyButtonCellEditor();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
            table.getColumnModel().getColumn(i).setCellEditor(editor);
        }
    }
}
