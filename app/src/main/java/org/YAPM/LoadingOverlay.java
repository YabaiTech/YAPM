package org.YAPM;

import javax.swing.*;
import java.awt.*;

public class LoadingOverlay extends JPanel {
  private final Timer timer;
  private float angle = 0f;

  public LoadingOverlay() {
    setOpaque(false);
    setLayout(new BorderLayout());

    // Start rotation timer (60 FPS)
    timer = new Timer(16, e -> {
      angle += 4f;
      if (angle >= 360)
        angle -= 360;
      repaint();
    });
    // timer.start();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Semi-transparent black overlay
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setColor(new Color(0, 0, 0, 170));
    g2d.fillRect(0, 0, getWidth(), getHeight());

    // Enable anti-aliasing
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Spinner parameters
    int size = 60;
    int thickness = 6;
    int x = (getWidth() - size) / 2;
    int y = (getHeight() - size) / 2;

    // Spinner background (transparent circle)
    g2d.setColor(new Color(255, 255, 255, 40));
    g2d.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2d.drawOval(x, y, size, size);

    // Rotating arc
    g2d.setColor(new Color(100, 200, 255));
    g2d.rotate(Math.toRadians(angle), getWidth() / 2, getHeight() / 2);
    g2d.drawArc(x, y, size, size, 0, 90);

    g2d.dispose();
  }

  @Override
  public void setVisible(boolean aFlag) {
    super.setVisible(aFlag);
    if (aFlag) {
      timer.start();
    } else {
      timer.stop();
    }
  }
}
