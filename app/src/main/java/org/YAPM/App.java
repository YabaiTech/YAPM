
package org.YAPM;

import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import javax.swing.*;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

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
