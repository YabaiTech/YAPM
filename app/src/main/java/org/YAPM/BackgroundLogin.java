package org.YAPM;

import org.backend.BackendError;
import org.backend.LoginUser;

import javax.swing.*;

public class BackgroundLogin extends SwingWorker<Void, Void> {
  private final LoginUser loginUser;
  private final LoginPanel loginPanel;
  private final LoadingOverlay overlay;
  private final JButton loginButton;

  public BackgroundLogin(LoginPanel lPanel, LoginUser lUser, LoadingOverlay overlay, JButton loginButton) {
    this.loginUser = lUser;
    this.loginPanel = lPanel;
    this.overlay = overlay;
    this.loginButton = loginButton;
  }

  private void showUserFriendlyError(JPanel loginPanel, String title, String message) {
    JOptionPane.showMessageDialog(
        loginPanel,
        "<html><div style='width: 300px; padding: 5px;'><h3 style='margin-top: 0;'>" + title + "</h3>" + message
            + "</div></html>",
        title,
        JOptionPane.ERROR_MESSAGE);
  }

  private String getLoginErrorMessage(BackendError error) {
    return switch (error.getErrorType()) {
      case InvalidLoginCredentials -> "Invalid username/email or password.<br><br>" +
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
    BackendError loginErr = loginUser.login();
    if (loginErr != null) {
      showUserFriendlyError(this.loginPanel, "Login Error", getLoginErrorMessage(loginErr));
      this.loginPanel.mainUI.showPage("login");

      return null;
    }

    // Successful login
    JOptionPane.showMessageDialog(
        this.loginPanel,
        "<html><div style='width: 300px;'>" +
            "<h3 style='margin-top: 0;'>Login Successful!</h3>" +
            "<p>Welcome back to YAPM.</p>" +
            "</div></html>",
        "Success",
        JOptionPane.INFORMATION_MESSAGE);

    App.currentLoginUser = loginUser;
    this.loginPanel.usernameEmailField.setText("");
    this.loginPanel.passField.setText("");
    this.loginPanel.mainUI.showPage("home");

    return null;
  }

  @Override
  public void done() {
    overlay.setVisible(false);
    loginButton.setEnabled(true);
  }
}
