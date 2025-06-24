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

public class RegisterPanel extends JPanel {
  final MainUI mainUI;
  final JTextField usernameField;
  final JTextField emailField;
  final JPasswordField passField;

  public RegisterPanel(MainUI mainUI) {
    this.mainUI = mainUI;
    setLayout(new BorderLayout());

    Color darkBg = UIManager.getColor("Panel.background");
    Color formBorderColor = UIManager.getColor("Component.borderColor");
    Color textColor = UIManager.getColor("Label.foreground");
    Color labelColor = UIManager.getColor("Label.disabledForeground");
    Color accentColor = UIManager.getColor("Component.focusColor");

    // Header
    JLabel header = new JLabel("YAPM - Register", SwingConstants.CENTER);
    header.setFont(new Font("Segoe UI", Font.BOLD, 24));
    header.setOpaque(true);
    header.setBackground(darkBg.darker());
    header.setForeground(textColor);
    header.setBorder(new EmptyBorder(20, 0, 20, 0));
    add(header, BorderLayout.NORTH);

    // Center Panel
    JPanel centerWrapper = new JPanel(new GridBagLayout());
    centerWrapper.setBackground(darkBg);

    JPanel formPanel = new JPanel();
    formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
    formPanel.setBackground(darkBg);
    formPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(formBorderColor),
        new EmptyBorder(20, 20, 20, 20)));
    formPanel.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
    formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Username Field
    JLabel usernameLabel = new JLabel("Username:");
    usernameLabel.setForeground(labelColor);
    usernameLabel.setAlignmentX(LEFT_ALIGNMENT);
    usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.PLAIN, 18f));
    usernameLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

    JTextField usernameField = new JTextField();
    this.usernameField = usernameField;
    usernameField.putClientProperty(FlatClientProperties.STYLE, "font: 18");
    usernameField.setFont(usernameField.getFont().deriveFont(18f));
    usernameField.setPreferredSize(new Dimension(450, 40));
    usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    usernameField.setAlignmentX(LEFT_ALIGNMENT);
    usernameField.setBorder(BorderFactory.createEmptyBorder());
    usernameField.setMargin(new Insets(0, 0, 0, 0));

    formPanel.add(usernameLabel);
    formPanel.add(usernameField);
    formPanel.add(Box.createVerticalStrut(10));

    // Email Field
    JLabel emailLabel = new JLabel("Email:");
    emailLabel.setForeground(labelColor);
    emailLabel.setAlignmentX(LEFT_ALIGNMENT);
    emailLabel.setFont(emailLabel.getFont().deriveFont(Font.PLAIN, 18f));
    emailLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

    JTextField emailField = new JTextField();
    this.emailField = emailField;
    emailField.putClientProperty(FlatClientProperties.STYLE, "font: 18");
    emailField.setFont(emailField.getFont().deriveFont(18f));
    emailField.setPreferredSize(new Dimension(450, 40));
    emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    emailField.setAlignmentX(LEFT_ALIGNMENT);
    emailField.setBorder(BorderFactory.createEmptyBorder());
    emailField.setMargin(new Insets(0, 0, 0, 0));

    formPanel.add(emailLabel);
    formPanel.add(emailField);
    formPanel.add(Box.createVerticalStrut(10));

    // Password Field
    JLabel passLabel = new JLabel("Password:");
    passLabel.setForeground(labelColor);
    passLabel.setAlignmentX(LEFT_ALIGNMENT);
    passLabel.setFont(passLabel.getFont().deriveFont(Font.PLAIN, 18f));
    passLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

    JPasswordField passField = new JPasswordField();
    this.passField = passField;
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

    // Password Rules Link
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
            RegisterPanel.this,
            """
                <html><div style='width: 300px;'>
                <h3>Registration Rules</h3>
                <p><b>Username Requirements:</b></p>
                <ul>
                    <li>4-20 characters long</li>
                    <li>Only letters (a-z, A-Z) and numbers (0-9)</li>
                    <li>No spaces or special characters</li>
                </ul>
                <p><b>Password Requirements:</b></p>
                <ul>
                    <li>Minimum 8 characters</li>
                    <li>At least one uppercase letter (A-Z)</li>
                    <li>At least one lowercase letter (a-z)</li>
                    <li>At least one number (0-9)</li>
                    <li>At least one special character (!@#$%^&* etc.)</li>
                </ul>
                </div></html>
                """,
            "Registration Rules",
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

    // Register Button
    JButton registerButton = new JButton("Register");
    registerButton.setAlignmentX(LEFT_ALIGNMENT);
    registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    registerButton.setPreferredSize(new Dimension(450, 35));
    registerButton.setFont(registerButton.getFont().deriveFont(Font.BOLD, 16f));

    registerButton.addActionListener(e -> {
      String uname = usernameField.getText().trim();
      String email = emailField.getText().trim();
      String pwd = new String(passField.getPassword());

      DBConnection db = new DBConnection();
      CloudDbConnection cloudDb = new CloudDbConnection();
      RegisterUser reg = new RegisterUser(db, cloudDb);

      BackendError err;

      err = reg.setUsername(uname);
      if (err != null) {
        showUserFriendlyError("Username Error", getUsernameErrorMessage(err));
        usernameField.requestFocus();
        return;
      }

      err = reg.setEmail(email);
      if (err != null) {
        showUserFriendlyError("Email Error", getEmailErrorMessage(err));
        emailField.requestFocus();
        return;
      }

      err = reg.setPassword(pwd);
      if (err != null) {
        showUserFriendlyError("Password Error", getPasswordErrorMessage(err));
        passField.requestFocus();
        return;
      }

      err = reg.register();
      if (err == null) {
        JOptionPane.showMessageDialog(RegisterPanel.this,
            "<html><div style='width: 300px;'>" +
                "<h3>Registration Successful!</h3>" +
                "<p>You can now login with your credentials.</p>" +
                "</div></html>",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        mainUI.showPage("login");
      } else {
        showUserFriendlyError("Registration Error", getRegistrationErrorMessage(err));
      }
    });

    formPanel.add(registerButton);
    formPanel.add(Box.createVerticalStrut(15));

    // Login Prompt
    JLabel promptLabel = new JLabel("Already have an account?");
    promptLabel.setForeground(labelColor);
    promptLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel.add(promptLabel);

    JLabel loginNowLabel = new JLabel("Login now");
    loginNowLabel.setForeground(accentColor);
    loginNowLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    loginNowLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    loginNowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    loginNowLabel.addMouseListener(new MouseAdapter() {
      Font originalFont = loginNowLabel.getFont();

      @Override
      public void mouseClicked(MouseEvent e) {
        mainUI.showPage("login");
      }

      @Override
      @SuppressWarnings("unchecked")
      public void mouseEntered(MouseEvent e) {
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) originalFont.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        loginNowLabel.setFont(originalFont.deriveFont(attributes));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        loginNowLabel.setFont(originalFont);
      }
    });

    formPanel.add(loginNowLabel);
    centerWrapper.add(formPanel);
    add(centerWrapper, BorderLayout.CENTER);

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

  private String getUsernameErrorMessage(BackendError error) {
    switch (error.getErrorType()) {
      case InvalidUserName:
        return "<p>Invalid username format.</p>" +
            "<p>Username must:</p>" +
            "<ul>" +
            "<li>Be 4-20 characters long</li>" +
            "<li>Contain only letters and numbers</li>" +
            "<li>No spaces or special characters</li>" +
            "</ul>";
      case UsernameAlreadyExists:
        return "<p>This username is already taken.</p>" +
            "<p>Please choose a different username.</p>";
      case UsernameNotProvided:
        return "<p>Username is required.</p>" +
            "<p>Please enter a username.</p>";
      default:
        return "<p>An unknown error occurred with your username.</p>";
    }
  }

  private String getEmailErrorMessage(BackendError error) {
    switch (error.getErrorType()) {
      case InvalidEmail:
        return "<p>Invalid email format.</p>" +
            "<p>Please enter a valid email address in the format:</p>" +
            "<p style='text-align: center;'><b>example@domain.com</b></p>";
      case EmailAlreadyExists:
        return "<p>This email is already registered.</p>" +
            "<p>If this is your email, please try logging in instead.</p>";
      case EmailNotProvided:
        return "<p>Email is required.</p>" +
            "<p>Please enter your email address.</p>";
      default:
        return "<p>An unknown error occurred with your email.</p>";
    }
  }

  private String getPasswordErrorMessage(BackendError error) {
    switch (error.getErrorType()) {
      case PasswordNeedsToBeAtleast8Chars:
        return "<p>Password must be at least 8 characters long.</p>";
      case PasswordNeedsAtleast1Lowercase:
        return "<p>Password must contain at least one lowercase letter (a-z).</p>";
      case PasswordNeedsAtleast1Uppercase:
        return "<p>Password must contain at least one uppercase letter (A-Z).</p>";
      case PasswordNeedsAtleast1Number:
        return "<p>Password must contain at least one number (0-9).</p>";
      case PasswordNeedsAtleast1SpecialChar:
        return "<p>Password must contain at least one special character (!@#$%^&* etc.).</p>";
      case PasswordContainsUnallowedChars:
        return "<p>Password contains invalid characters.</p>" +
            "<p>Only letters, numbers, and standard special characters are allowed.</p>";
      case PasswordNotProvided:
        return "<p>Password is required.</p>";
      default:
        return "<p>An unknown error occurred with your password.</p>";
    }
  }

  private String getRegistrationErrorMessage(BackendError error) {
    switch (error.getErrorType()) {
      case LocalDBCreationFailed:
        return "<p>Failed to create your secure storage.</p>" +
            "<p>Please try again or contact support if the problem persists.</p>";
      case FailedToCreateDbDir:
        return "<p>System error: Could not create storage directory.</p>" +
            "<p>Please check your system permissions or contact support.</p>";
      case DbTransactionError:
        return "<p>Database error during registration.</p>" +
            "<p>Please try again later or contact support.</p>";
      case HashedPasswordNotGenerated:
      case SaltForHashNotGenerated:
        return "<p>Security error during registration.</p>" +
            "<p>This is likely a system problem. Please contact support.</p>";
      default:
        return "<p>An unknown error occurred during registration.</p>" +
            "<p>Please try again or contact support if the problem persists.</p>";
    }
  }
}
