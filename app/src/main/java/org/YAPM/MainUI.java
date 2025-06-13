package org.YAPM;
import org.vault.*;

import javax.swing.*;
import java.awt.*;

public class MainUI extends JFrame {
    private String dbPath;
    private String password;
    private VaultManager vaultManager;

    public void setVaultCredentials(String dbPath, String password) {
        this.dbPath = dbPath;
        this.password = password;
        this.vaultManager = new VaultManager(dbPath, password);
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public MainUI() {
        setTitle("YAPM");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 700);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
//        setUndecorated(true);

        setLocationRelativeTo(null);


        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        LoginPanel loginPanel = new LoginPanel(this);
        RegisterPanel registerPanel = new RegisterPanel(this);
        HomePanel homePanel = new HomePanel(this);

        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");
        cardPanel.add(homePanel, "home");

        add(cardPanel);
        cardLayout.show(cardPanel, "login"); // show login by default
    }

    public void showPage(String name) {
        cardLayout.show(cardPanel, name);
    }
}
