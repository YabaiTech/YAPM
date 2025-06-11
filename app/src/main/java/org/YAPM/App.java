package org.YAPM;

import javax.swing.*;
        import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        try {
            FlatNordIJTheme.setup(); // Apply the Nord theme
        } catch (Exception e) {
            System.err.println("Failed to apply theme.");
        }

        SwingUtilities.invokeLater(() -> {
            new LoginUI().setVisible(true); // Show login UI by default
        });
    }
}
