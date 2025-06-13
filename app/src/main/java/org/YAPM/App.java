package org.YAPM;

import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import javax.swing.*;
import org.backend.*;

public class App {
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
