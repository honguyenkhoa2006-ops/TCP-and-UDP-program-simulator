import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {
    private int arcWidth = 15;
    private int arcHeight = 15;

    public RoundedButton(String text) {
        super(text);
        this.setOpaque(false);
        this.setFocusPainted(false);
        this.setContentAreaFilled(false);
        this.setBorder(null);
    }

    public RoundedButton(String text, int arcWidth, int arcHeight) {
        this(text);
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcHeight);

        // Draw border
        g2.setColor(getForeground());
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcHeight);

        // Draw text
        super.paintComponent(g);
    }

    @Override
    public void paintBorder(Graphics g) {
        // Don't paint border
    }
}
