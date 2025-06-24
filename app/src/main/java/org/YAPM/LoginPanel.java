package org.YAPM;

import com.formdev.flatlaf.FlatClientProperties;
import org.backend.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;

public class LoginPanel extends JPanel {
  final MainUI mainUI;
  final JTextField usernameEmailField;
  final JPasswordField passField;
  private LoadingOverlay overlay;

  public LoginPanel(MainUI mainUI) {
    this.mainUI = mainUI;
    setLayout(new BorderLayout());

    Color darkBg = UIManager.getColor("Panel.background");
    Color formBorderColor = UIManager.getColor("Component.borderColor");
    Color textColor = UIManager.getColor("Label.foreground");
    Color labelColor = UIManager.getColor("Label.disabledForeground");
    Color accentColor = UIManager.getColor("Component.focusColor");

    // Header
    JLabel header = new JLabel("YAPM - Login", SwingConstants.CENTER);
    header.setFont(new Font("Segoe UI", Font.BOLD, 24));
    header.setOpaque(true);
    header.setBackground(darkBg.darker());
    header.setForeground(textColor);
    header.setBorder(new EmptyBorder(20, 0, 20, 0));
    add(header, BorderLayout.NORTH);

    // Center Panel
    JPanel centerWrapper = new JPanel(new GridBagLayout());
    centerWrapper.setBackground(darkBg);

    overlay = new LoadingOverlay();
    overlay.setVisible(false);
    overlay.setOpaque(false);
    overlay.setAlignmentX(Component.CENTER_ALIGNMENT);
    overlay.setAlignmentY(Component.CENTER_ALIGNMENT);
    overlay.setPreferredSize(new Dimension(600, 700));

    JPanel formPanel = new JPanel();
    formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
    formPanel.setBackground(darkBg);
    formPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(formBorderColor),
        new EmptyBorder(20, 20, 20, 20)));
    formPanel.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
    formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Username/Email Field
    JLabel emailLabel = new JLabel("Username/Email:");
    emailLabel.setForeground(labelColor);
    emailLabel.setAlignmentX(LEFT_ALIGNMENT);
    emailLabel.setFont(emailLabel.getFont().deriveFont(Font.PLAIN, 18f));
    emailLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

    this.usernameEmailField = new JTextField();
    usernameEmailField.putClientProperty(FlatClientProperties.STYLE, "font: 18");
    usernameEmailField.setFont(usernameEmailField.getFont().deriveFont(18f));
    usernameEmailField.setPreferredSize(new Dimension(450, 40));
    usernameEmailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    usernameEmailField.setAlignmentX(LEFT_ALIGNMENT);
    usernameEmailField.setBorder(BorderFactory.createEmptyBorder());
    usernameEmailField.setMargin(new Insets(0, 0, 0, 0));

    formPanel.add(emailLabel);
    formPanel.add(usernameEmailField);
    formPanel.add(Box.createVerticalStrut(10));

    // Password Field
    JLabel passLabel = new JLabel("Password:");
    passLabel.setForeground(labelColor);
    passLabel.setAlignmentX(LEFT_ALIGNMENT);
    passLabel.setFont(passLabel.getFont().deriveFont(Font.PLAIN, 18f));
    passLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

    this.passField = new JPasswordField();
    passField.putClientProperty(FlatClientProperties.STYLE, "font: 18");
    passField.setFont(passField.getFont().deriveFont(18f));
    passField.setPreferredSize(new Dimension(450, 40));
    passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    passField.setAlignmentX(LEFT_ALIGNMENT);
    passField.setBorder(BorderFactory.createEmptyBorder());
    passField.setMargin(new Insets(0, 0, 0, 0));

    formPanel.add(passLabel);
    formPanel.add(passField);
    formPanel.add(Box.createVerticalStrut(20));

    // Rules Link
    JLabel rulesLabel = new JLabel("View username and password rules");
    rulesLabel.setForeground(accentColor);
    rulesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    rulesLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    rulesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    rulesLabel.addMouseListener(new MouseAdapter() {
      Font originalFont = rulesLabel.getFont();

      @Override
      public void mouseClicked(MouseEvent e) {
        JOptionPane.showMessageDialog(
            LoginPanel.this,
            """
                <html><div style='width: 300px;'>
                <h3>Login Requirements</h3>
                <p><b>Username/Email Rules:</b></p>
                <ul>
                    <li>Cannot be empty</li>
                    <li>Username: Only letters (a-z, A-Z) and numbers (0-9)</li>
                    <li>Email: Must be in valid format (user@domain.com)</li>
                </ul>
                <p><b>Password Rules:</b></p>
                <ul>
                    <li>Cannot be empty</li>
                    <li>Minimum 8 characters</li>
                </ul>
                </div></html>
                """,
            "Login Rules",
            JOptionPane.INFORMATION_MESSAGE);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) originalFont.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        rulesLabel.setFont(originalFont.deriveFont(attributes));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        rulesLabel.setFont(originalFont);
      }
    });

    formPanel.add(rulesLabel);
    formPanel.add(Box.createVerticalStrut(10));

    // Login Button
    JButton loginButton = new JButton("Login");
    loginButton.setAlignmentX(LEFT_ALIGNMENT);
    loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    loginButton.setPreferredSize(new Dimension(450, 35));
    loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 16f));

    loginButton.addActionListener(e -> {
      String accountIdentifier = usernameEmailField.getText().trim();
      String password = new String(passField.getPassword());

      // Input validation
      if (accountIdentifier.isEmpty()) {
        showUserFriendlyError("Input Error", "Username/Email cannot be empty.");
        usernameEmailField.requestFocus();
        return;
      }

      if (!accountIdentifier.contains("@") && !accountIdentifier.matches("^[a-zA-Z0-9]+$")) {
        showUserFriendlyError("Input Error",
            "Username must be alphanumeric (letters and digits only).");
        usernameEmailField.requestFocus();
        return;
      }

      if (accountIdentifier.contains("@") && !accountIdentifier.matches("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
        showUserFriendlyError("Input Error",
            "Invalid email format. Please enter a valid email address.");
        usernameEmailField.requestFocus();
        return;
      }

      if (password.isEmpty()) {
        showUserFriendlyError("Input Error", "Password cannot be empty.");
        passField.requestFocus();
        return;
      }

      if (password.length() < 8) {
        showUserFriendlyError("Input Error",
            "Password must be at least 8 characters long.");
        passField.requestFocus();
        return;
      }

      overlay.setVisible(true);
      loginButton.setEnabled(false);

      // Backend login process
      DBConnection dbConnection = new DBConnection();
      CloudDbConnection cloudDbConnection = new CloudDbConnection();
      LoginUser loginUser = new LoginUser(dbConnection, cloudDbConnection, accountIdentifier, password);

      BackgroundLogin blWorker = new BackgroundLogin(this, loginUser, this.overlay, loginButton);
      blWorker.execute();
    });

    formPanel.add(loginButton);
    formPanel.add(Box.createVerticalStrut(15));

    // Register Prompt
    JLabel promptLabel = new JLabel("Don't have an account?");
    promptLabel.setForeground(labelColor);
    promptLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel.add(promptLabel);

    JLabel registerNowLabel = new JLabel("Register now");
    registerNowLabel.setForeground(accentColor);
    registerNowLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    registerNowLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    registerNowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    registerNowLabel.addMouseListener(new MouseAdapter() {
      Font originalFont = registerNowLabel.getFont();

      @Override
      public void mouseClicked(MouseEvent e) {
        mainUI.showPage("register");
      }

      @Override
      @SuppressWarnings("unchecked")
      public void mouseEntered(MouseEvent e) {
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) originalFont.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        registerNowLabel.setFont(originalFont.deriveFont(attributes));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        registerNowLabel.setFont(originalFont);
      }
    });

    formPanel.add(registerNowLabel);
    centerWrapper.add(formPanel);
    add(centerWrapper, BorderLayout.CENTER);

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

    add(layeredPane, BorderLayout.CENTER);

    // Footer
    JLabel footer = new JLabel("© 2025 All rights reserved.", SwingConstants.CENTER);
    footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    footer.setOpaque(true);
    footer.setBackground(darkBg.darker());
    footer.setForeground(textColor);
    footer.setBorder(new EmptyBorder(20, 0, 20, 0));
    add(footer, BorderLayout.SOUTH);
  }

  private void showUserFriendlyError(String title, String message) {
    JOptionPane.showMessageDialog(
        this,
        "<html><div style='width: 300px; padding: 5px;'><h3 style='margin-top: 0;'>" + title + "</h3>" + message
            + "</div></html>",
        title,
        JOptionPane.ERROR_MESSAGE);
  }

  private String getLoginErrorMessage(BackendError error) {
    switch (error.getErrorType()) {
      case InvalidLoginCredentials:
        return "Invalid username/email or password.<br><br>" +
            "Please check your credentials and try again.";
      case DbTransactionError:
        return "Database error during login.<br><br>" +
            "Please try again later or contact support.";
      case UserNotLoggedIn:
        return "Authentication error.<br><br>" +
            "Please try logging in again.";
      default:
        return "An unknown error occurred during login.<br><br>" +
            "Please try again or contact support.";
    }
  }
}
