package org.YAPM;

import javax.swing.*;
import java.awt.*;

public class MainUI extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public MainUI() {
        setTitle("YAPM");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        LoginPanel loginPanel = new LoginPanel(this);
        RegisterPanel registerPanel = new RegisterPanel(this);
        HomePanel homePanel = new HomePanel();

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
