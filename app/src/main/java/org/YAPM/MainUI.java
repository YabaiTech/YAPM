package org.YAPM;

import javax.swing.*;
import java.awt.*;

public class MainUI extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public MainUI() {
        setTitle("YAPM");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 700);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        LoginPanel loginPanel = new LoginPanel(this);
        RegisterPanel registerPanel = new RegisterPanel(this);

        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");

        add(cardPanel);
        cardLayout.show(cardPanel, "login"); // show login by default
    }

    public void showPage(String name) {
        if (name.equals("home")) {
            HomePanel homePanel = new HomePanel(this); // safe now, user is logged in
            cardPanel.add(homePanel, "home");
        }
        cardLayout.show(cardPanel, name);
    }
}
