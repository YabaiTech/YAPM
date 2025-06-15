package org.YAPM;

import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;

import org.backend.BackendError;
import org.backend.CloudDbConnection;
import org.backend.DBConnection;
import org.backend.DBOperations;
import org.backend.LoginUser;
import org.backend.RegisterUser;

import javax.swing.*;

public class App {
  public static LoginUser currentLoginUser;

  public static void main(String[] args) {
    // try {
    // FlatNordIJTheme.setup();
    // } catch (Exception e) {
    // System.err.println("Failed to apply theme");
    // }
    //
    // SwingUtilities.invokeLater(() -> {
    // new MainUI().setVisible(true);
    // });

    DBConnection dbLocal = new DBConnection();
    CloudDbConnection dbCloud = new CloudDbConnection();

    // String uname = "justAtest" + System.currentTimeMillis();
    // String email = "justAtest@gmail.com" + System.currentTimeMillis();
    // String pwd = "Abc123@!";
    //
    // RegisterUser reg = new RegisterUser(dbLocal, dbCloud);
    // reg.setUsername(uname);
    // reg.setEmail(email);
    // reg.setPassword(pwd);
    //
    // BackendError rsp = reg.register();
    // if (rsp != null) {
    // System.err.println("Failed to register user: " + rsp.getErrorType());
    // System.exit(1);
    // }

    LoginUser auth = new LoginUser(dbLocal, dbCloud, "syncTest@gmail.com83474", "xYZ123#!");
    BackendError rsp = auth.login();
    if (rsp != null) {
      System.err.println("Failed to login: " + rsp.getErrorType());
      System.exit(1);
    } else {
      System.out.println("Successfully logged in the user!");
    }

  }
}
