package org.YAPM;

import org.backend.BackendError;
import org.backend.RegisterUser;

import javax.swing.*;

public class BackgroundRegistration extends SwingWorker<Void, Void> {
  private final RegisterUser registerUser;
  private final RegisterPanel registerPanel;
  private final String uname;
  private final String email;
  private final String passwd;
  private final LoadingOverlay overlay;
  private final JButton registerButton;

  public BackgroundRegistration(RegisterPanel rPanel, RegisterUser rUser, String uname, String email, String passwd,
      LoadingOverlay overlay, JButton registerButton) {
    this.registerUser = rUser;
    this.registerPanel = rPanel;
    this.uname = uname;
    this.email = email;
    this.passwd = passwd;
    this.overlay = overlay;
    this.registerButton = registerButton;
  }

  private void showUserFriendlyError(JPanel registerPanel, String title, String message) {
    JOptionPane.showMessageDialog(
        registerPanel,
        "<html><div style='width: 300px; padding: 5px;'><h3 style='margin-top: 0;'>" + title + "</h3>" + message
            + "</div></html>",
        title,
        JOptionPane.ERROR_MESSAGE);
  }

  private String getRegistrationErrorMessage(BackendError error) {
    return switch (error.getErrorType()) {
      case InvalidUserName -> "Invalid username.<br><br>" +
          "Please check your credentials and try again.";
      case InvalidEmail -> "Invalid email.<br><br>" +
          "Please check your credentials and try again.";
      case PasswordNeedsToBeAtleast8Chars -> "Password needs to be at least 8 characters long.<br><br>" +
          "Please check your credentials and try again.";
      case PasswordContainsUnallowedChars ->
        "Password can only contain lowercase or uppercase alphabets, digits, and special characters.<br><br>" +
            "Please check your credentials and try again.";
      case PasswordNeedsAtleast1Lowercase -> "Password needs to have at least one lowercase character.<br><br>" +
          "Please check your credentials and try again.";
      case PasswordNeedsAtleast1Uppercase -> "Password needs to have at least one uppercase character.<br><br>" +
          "Please check your credentials and try again.";
      case PasswordNeedsAtleast1Number -> "Password needs to have at least one digit.<br><br>" +
          "Please check your credentials and try again.";
      case PasswordNeedsAtleast1SpecialChar -> "Password needs to have at least one special character.<br><br>" +
          "Please check your credentials and try again.";
      case DbTransactionError -> "Database error during login.<br><br>" +
          "Please try again later or contact support.";
      case UserNotLoggedIn -> "Authentication error.<br><br>" +
          "Please try logging in again.";
      default -> "An unknown error occurred during login.<br><br>" +
          "Please try again or contact support.";
    };
  }

  @Override
  public Void doInBackground() {
    BackendError registerErr;

    registerErr = registerUser.setUsername(this.uname);
    if (registerErr != null) {
      showUserFriendlyError(this.registerPanel, "Username Error", getRegistrationErrorMessage(registerErr));
      this.registerPanel.mainUI.showPage("register");

      return null;
    }

    registerErr = registerUser.setEmail(this.email);
    if (registerErr != null) {
      showUserFriendlyError(this.registerPanel, "Email Error", getRegistrationErrorMessage(registerErr));
      this.registerPanel.mainUI.showPage("register");

      return null;
    }

    registerErr = registerUser.setPassword(this.passwd);
    if (registerErr != null) {
      showUserFriendlyError(this.registerPanel, "Password Error", getRegistrationErrorMessage(registerErr));
      this.registerPanel.mainUI.showPage("register");

      return null;
    }

    registerErr = this.registerUser.register();
    if (registerErr != null) {
      showUserFriendlyError(this.registerPanel, "Registration Error", getRegistrationErrorMessage(registerErr));
      this.registerPanel.mainUI.showPage("register");

      return null;
    }

    JOptionPane.showMessageDialog(
        this.registerPanel,
        "<html><div style='width: 300px;'>" +
            "<h3 style='margin-top: 0;'>Registration Successful!</h3>" +
            "<p>Now you may login with your credentials.</p>" +
            "</div></html>",
        "Success",
        JOptionPane.INFORMATION_MESSAGE);

    this.registerPanel.usernameField.setText("");
    this.registerPanel.emailField.setText("");
    this.registerPanel.passField.setText("");
    this.registerPanel.mainUI.showPage("login");

    return null;
  }

  @Override
  public void done() {
    overlay.setVisible(false);
    registerButton.setEnabled(true);
  }
}
