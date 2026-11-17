package hansung.arcade.outgame.ui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

public class RoundPanel extends JPanel {
    private final int radius;

    public RoundPanel(Color color, int radius) {
        super(null);
        this.radius = radius;
        setOpaque(false);
        setBackground(color);
    }

    public int radius() {
        return radius;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        g.setColor(getBackground());
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 2 * radius, 2 * radius));
        g.dispose();
    }
}
