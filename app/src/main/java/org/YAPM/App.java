package org.YAPM;

import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import org.backend.LoginUser;

import javax.swing.*;

public class App {
  public static LoginUser currentLoginUser;
  public static void main(String[] args) {
    try {
      FlatNordIJTheme.setup();
    } catch (Exception e) {
      System.err.println("Failed to apply theme");
    }

    SwingUtilities.invokeLater(() -> {
      new MainUI().setVisible(true);
    });
  }
}
