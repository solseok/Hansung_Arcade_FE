package hansung.arcade.outgame.ui;

import java.awt.*;

import javax.swing.*;

public class ArrowButton extends JButton {
    public enum Direction {
        LEFT,
        RIGHT
    }

    private final Direction direction;

    public ArrowButton(Direction direction) {
        super(direction == Direction.LEFT ? "이전쪽" : "다음쪽");
        this.direction = direction;
        setFont(OutgameFonts.label(12));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    public Direction direction() {
        return direction;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        if (direction == Direction.RIGHT) {
            g.translate(getWidth(), 0);
            g.scale(-1, 1);
        }
        g.setPaint(
                new GradientPaint(
                        0, 0, new Color(140, 225, 255), 0, getHeight(), new Color(0, 130, 235)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
        g.setColor(isEnabled() ? new Color(40, 115, 200) : new Color(95, 165, 210));
        int x = getWidth() / 2, y = getHeight() / 2;
        g.fillPolygon(new int[] {x - 5, x + 4, x + 4}, new int[] {y, y - 7, y + 7}, 3);
        g.dispose();
    }
}
