package hansung.arcade.outgame.ui;

import java.awt.*;

public class StarRating extends TextLabel {
    public StarRating() {
        super("인기도:");
    }

    public int stars() {
        return 5;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D c = (Graphics2D) g.create();
        Shapes.hints(c);
        c.setColor(new Color(100, 225, 255));
        for (int i = 0; i < 5; i++) {
            java.awt.geom.Path2D star = new java.awt.geom.Path2D.Double();
            for (int j = 0; j < 10; j++) {
                double a = -Math.PI / 2 + j * Math.PI / 5, r = j % 2 == 0 ? 5 : 2;
                double x = 55 + i * 13 + Math.cos(a) * r, y = getHeight() / 2.0 + Math.sin(a) * r;
                if (j == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.closePath();
            c.fill(star);
        }
        c.dispose();
    }
}
