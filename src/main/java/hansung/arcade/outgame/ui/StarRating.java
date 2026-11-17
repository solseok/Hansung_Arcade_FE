package hansung.arcade.outgame.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Path2D;

/** "인기도:" 같은 글자 뒤에 별을 늘어놓는 라벨 (참고 화면 8번의 하늘색 별) */
public class StarRating extends TextLabel {

    public static final int MAX_STARS = 5;
    private static final Color STAR = new Color(120, 225, 255);
    private static final Color STAR_EDGE = new Color(20, 90, 170);

    private final int stars;

    public StarRating(String text, int stars) {
        super(text);
        this.stars = Math.clamp(stars, 0, MAX_STARS);
    }

    public int stars() {
        return stars;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Shapes.hints(g);
            Insets in = getInsets();
            int textWidth = Shapes.metrics(getFont()).stringWidth(getText());
            double size = Math.min(getHeight() - 4, 13);
            double gap = 3;
            double x = in.left + textWidth + 6;
            double cy = getHeight() / 2.0;
            for (int i = 0; i < stars; i++) {
                Path2D star = star(x + size / 2, cy, size / 2, size / 4.6);
                g.setColor(STAR);
                g.fill(star);
                g.setColor(STAR_EDGE);
                g.setStroke(new BasicStroke(1f));
                g.draw(star);
                x += size + gap;
            }
        } finally {
            g.dispose();
        }
    }

    private static Path2D star(double cx, double cy, double outer, double inner) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(-90 + i * 36);
            double px = cx + r * Math.cos(angle);
            double py = cy + r * Math.sin(angle);
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        path.closePath();
        return path;
    }
}
