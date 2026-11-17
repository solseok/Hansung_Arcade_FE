package hansung.arcade.outgame.ui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

public class TextLabel extends JLabel {
    private int radius;
    private Color outline;
    private int outlineWidth;

    public TextLabel(String text) {
        super(text);
        setFont(OutgameFonts.label(12));
        setForeground(Color.WHITE);
    }

    public void setOutline(Color color, int width) {
        outlined(color, width);
    }

    public void setName(String name) {
        super.setName(name);
        if (java.util.Set.of("room-title", "lobby-nickname", "slot-name").contains(name))
            setFont(OutgameFonts.gulimChe(12));
    }

    public int radius() {
        return radius;
    }

    public Color outline() {
        return outline;
    }

    public int outlineWidth() {
        return outlineWidth;
    }

    public TextLabel rounded(int r, Color bg) {
        radius = r;
        setBackground(bg);
        return this;
    }

    public TextLabel outlined(Color color, int width) {
        outline = color;
        outlineWidth = width;
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        if (radius > 0) {
            g.setPaint(
                    new GradientPaint(
                            0, 0, new Color(195, 240, 255), 0, getHeight() / 2f, getBackground()));
            g.fill(
                    new RoundRectangle2D.Double(
                            0, 0, getWidth(), getHeight(), radius * 2, radius * 2));
        } else if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (getText() != null && !getText().isEmpty()) {
            g.setFont(getFont());
            var fm = g.getFontMetrics();
            float x =
                    getHorizontalAlignment() == CENTER
                            ? (getWidth() - fm.stringWidth(getText())) / 2f
                            : 3;
            float y = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
            var shape =
                    getFont()
                            .createGlyphVector(g.getFontRenderContext(), getText())
                            .getOutline(x, y);
            if (outline != null) {
                g.setColor(outline);
                g.setStroke(
                        new BasicStroke(
                                outlineWidth * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(shape);
            }
            g.setColor(getForeground());
            g.fill(shape);
        }
        g.dispose();
    }
}
