package org.YAPM;

import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class LoginUI extends JFrame {

    CardLayout cardLayout;
    JPanel cardPanel;

    public LoginUI() {
        setTitle("YAPM - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(450, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Title bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titleBar.setBackground(new Color(46, 52, 64));
        JLabel titleLabel = new JLabel("Welcome to YAPM");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(236, 239, 244));
        titleBar.add(titleLabel);

        // Card Panel for Login/Register forms
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(new Color(59, 66, 82));

        cardPanel.add(createLoginPanel(), "login");
        cardPanel.add(createRegisterPanel(), "register");

        // Add to frame
        add(titleBar, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel createLoginPanel() {
        JPanel panel = createFormPanel();

        JLabel userLabel = createLabel("Username:");
        JTextField userField = createTextField();

        JLabel passLabel = createLabel("Password:");
        JPasswordField passField = new JPasswordField();
        styleInput(passField);

        JButton loginButton = createButton("Login");

        JButton toRegister = new JButton("No account? Register here");
        styleLinkButton(toRegister);
        toRegister.addActionListener(e -> cardLayout.show(cardPanel, "register"));

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(loginButton);
        panel.add(toRegister);

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = createFormPanel();

        JLabel userLabel = createLabel("Username:");
        JTextField userField = createTextField();

        JLabel emailLabel = createLabel("Email:");
        JTextField emailField = createTextField();

        JLabel passLabel = createLabel("Password:");
        JPasswordField passField = new JPasswordField();
        styleInput(passField);

        JButton registerButton = createButton("Register");

        JButton toLogin = new JButton("Already have an account? Login");
        styleLinkButton(toLogin);
        toLogin.addActionListener(e -> cardLayout.show(cardPanel, "login"));

        panel.add(userLabel);
        panel.add(userField);
        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(registerButton);
        panel.add(toLogin);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        panel.setBackground(new Color(59, 66, 82));
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(236, 239, 244));
        return label;
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    private void styleInput(JTextComponent field) {
        field.setBackground(new Color(67, 76, 94));
        field.setForeground(new Color(236, 239, 244));
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(76, 86, 106)));
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 35));
        return button;
    }

    private void styleLinkButton(JButton button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setForeground(new Color(129, 161, 193));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static void main(String[] args) {
        try {
            FlatNordIJTheme.setup();
        } catch (Exception e) {
            System.err.println("Failed to apply Nord theme");
        }

        SwingUtilities.invokeLater(() -> {
            LoginUI ui = new LoginUI();
            ui.setVisible(true);
        });
    }
}
