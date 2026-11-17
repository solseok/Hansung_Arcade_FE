package hansung.arcade.outgame.ui;

import java.awt.*;

import javax.swing.*;

/** 캐릭터 또는 팀 선택 상태를 밝은 테두리로 보여 준다. */
public final class ChoiceButton extends JToggleButton {
    public ChoiceButton(String text) {
        super(text);
        setup();
    }

    public ChoiceButton(Icon icon) {
        super(icon);
        setup();
    }

    private void setup() {
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setFont(OutgameFonts.label(18));
        setBackground(OutgamePalette.CHARACTER_CELL);
        setForeground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        Color base = isEnabled() ? getBackground() : new Color(65, 89, 125);
        g.setColor(base);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
        g.setPaint(
                new GradientPaint(
                        0,
                        0,
                        new Color(255, 255, 255, 130),
                        0,
                        getHeight() / 2f,
                        new Color(255, 255, 255, 0)));
        g.fillRoundRect(1, 1, getWidth() - 2, getHeight() / 2, 5, 5);
        g.setColor(isSelected() ? Color.WHITE : base.brighter());
        g.setStroke(new BasicStroke(isSelected() ? 3 : 1));
        g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 5, 5);
        g.dispose();
        super.paintComponent(graphics);
    }
}
