package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.*;

import java.awt.*;

public final class RoomListPanel extends RoundPanel {
    public RoomListPanel() {
        super(OutgamePalette.ROOM_LIST, 6);
    }

    public int mainHeight() {
        return getHeight() - 28;
    }

    public Rectangle tabBounds() {
        return new Rectangle(getWidth() / 2 - 74, mainHeight() - 4, 148, 32);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth(), mainHeight(), 12, 12);
        Rectangle t = tabBounds();
        g.fillRoundRect(t.x, t.y, t.width, t.height, 8, 8);
        g.dispose();
    }
}
