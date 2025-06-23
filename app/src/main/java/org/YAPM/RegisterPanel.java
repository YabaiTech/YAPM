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
    private final DarkLoadingOverlay overlay;

    public RegisterPanel(MainUI mainUI) {
        setLayout(new BorderLayout());

        Color darkBg = UIManager.getColor("Panel.background");
        Color formBorderColor = UIManager.getColor("Component.borderColor");
        Color textColor = UIManager.getColor("Label.foreground");
        Color labelColor = UIManager.getColor("Label.disabledForeground");
        Color accentColor = UIManager.getColor("Component.focusColor");

        JLabel header = new JLabel("YAPM - Register", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setOpaque(true);
        header.setBackground(darkBg.darker());
        header.setForeground(textColor);
        header.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(header, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(darkBg);

        overlay = new DarkLoadingOverlay();
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

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(labelColor);
        usernameLabel.setAlignmentX(LEFT_ALIGNMENT);
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.PLAIN, 18f));
        usernameLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JTextField usernameField = new JTextField();
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

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(labelColor);
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        emailLabel.setFont(emailLabel.getFont().deriveFont(Font.PLAIN, 18f));
        emailLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JTextField emailField = new JTextField();
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

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(labelColor);
        passLabel.setAlignmentX(LEFT_ALIGNMENT);
        passLabel.setFont(passLabel.getFont().deriveFont(Font.PLAIN, 18f));
        passLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPasswordField passField = new JPasswordField();
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

        JLabel rulesLabel = new JLabel("View username and password rules");
        rulesLabel.setForeground(accentColor);
        rulesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rulesLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rulesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rulesLabel.addMouseListener(new MouseAdapter() {
            Font originalFont = rulesLabel.getFont();

            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(RegisterPanel.this, "Rules...");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
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

        JButton registerButton = new JButton("Register");
        registerButton.setAlignmentX(LEFT_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        registerButton.setPreferredSize(new Dimension(450, 35));
        registerButton.setFont(registerButton.getFont().deriveFont(Font.BOLD, 16f));

        registerButton.addActionListener(e -> {
            String uname = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String pwd = new String(passField.getPassword());

            overlay.setVisible(true);
            registerButton.setEnabled(false);

            new SwingWorker<BackendError, Void>() {
                @Override
                protected BackendError doInBackground() {
                    try (
                        DBConnection localDb = new DBConnection();
                        CloudDbConnection cloudDb = new CloudDbConnection()
                    ) {
                        RegisterUser reg = new RegisterUser(localDb, cloudDb);
                        BackendError err;

                        err = reg.setUsername(uname);
                        if (err != null) return err;

                        err = reg.setEmail(email);
                        if (err != null) return err;

                        err = reg.setPassword(pwd);
                        if (err != null) return err;

                        return reg.register();
                    } catch (Exception ex) {
                        return new BackendError(BackendError.ErrorTypes.DbTransactionError, ex.getMessage());
                    }
                }

                @Override
                protected void done() {
                    overlay.setVisible(false);
                    registerButton.setEnabled(true);

                    try {
                        BackendError err = get();
                        if (err != null) {
                            showUserFriendlyError("Registration Error", getRegistrationErrorMessage(err));
                        } else {
                            JOptionPane.showMessageDialog(RegisterPanel.this, "Registration successful!");
                            mainUI.showPage("login");
                        }
                    } catch (Exception ex) {
                        showUserFriendlyError("Unexpected Error", "<p>Registration failed.</p><p>" + ex.getMessage() + "</p>");
                    }
                }
            }.execute();
        });

        formPanel.add(registerButton);
        formPanel.add(Box.createVerticalStrut(15));

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

        JLabel footer = new JLabel("© 2025 All rights reserved.", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setOpaque(true);
        footer.setBackground(darkBg.darker());
        footer.setForeground(textColor);
        footer.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(footer, BorderLayout.SOUTH);
    }

    private void showUserFriendlyError(String title, String message) {
        JOptionPane.showMessageDialog(this, "<html><div style='width: 300px; padding: 5px;'><h3 style='margin-top: 0;'>" + title + "</h3>" + message + "</div></html>", title, JOptionPane.ERROR_MESSAGE);
    }

    private String getUsernameErrorMessage(BackendError error) {
        return switch (error.getErrorType()) {
            case InvalidUserName -> "<p>Invalid username format.</p><ul><li>4-20 characters</li><li>Only letters/numbers</li><li>No special characters</li></ul>";
            case UsernameAlreadyExists -> "<p>This username is already taken.</p><p>Please choose another.</p>";
            case UsernameNotProvided -> "<p>Username is required.</p>";
            default -> "<p>Unknown username error occurred.</p>";
        };
    }

    private String getEmailErrorMessage(BackendError error) {
        return switch (error.getErrorType()) {
            case InvalidEmail -> "<p>Invalid email format. Use example@domain.com</p>";
            case EmailAlreadyExists -> "<p>Email is already registered.</p>";
            case EmailNotProvided -> "<p>Email is required.</p>";
            default -> "<p>Unknown email error occurred.</p>";
        };
    }

    private String getPasswordErrorMessage(BackendError error) {
        return switch (error.getErrorType()) {
            case PasswordNeedsToBeAtleast8Chars -> "<p>Password must be at least 8 characters long.</p>";
            case PasswordNeedsAtleast1Lowercase -> "<p>Password must contain at least one lowercase letter (a-z).</p>";
            case PasswordNeedsAtleast1Uppercase -> "<p>Password must contain at least one uppercase letter (A-Z).</p>";
            case PasswordNeedsAtleast1Number -> "<p>Password must contain at least one number (0-9).</p>";
            case PasswordNeedsAtleast1SpecialChar -> "<p>Password must contain at least one special character (!@#$%^&* etc.).</p>";
            case PasswordContainsUnallowedChars -> "<p>Password contains invalid characters.</p><p>Only letters, numbers, and standard special characters are allowed.</p>";
            case PasswordNotProvided -> "<p>Password is required.</p>";
            default -> "<p>An unknown error occurred with your password.</p>";
        };
    }

    private String getRegistrationErrorMessage(BackendError error) {
        return switch (error.getErrorType()) {
            case LocalDBCreationFailed -> "<p>Failed to create your secure storage.</p><p>Please try again or contact support if the problem persists.</p>";
            case FailedToCreateDbDir -> "<p>System error: Could not create storage directory.</p><p>Please check your system permissions or contact support.</p>";
            case DbTransactionError -> "<p>Database error during registration.</p><p>Please try again later or contact support.</p>";
            case HashedPasswordNotGenerated, SaltForHashNotGenerated -> "<p>Security error during registration.</p><p>This is likely a system problem. Please contact support.</p>";
            default -> "<p>An unknown error occurred during registration.</p><p>Please try again or contact support if the problem persists.</p>";
        };
    }
}
