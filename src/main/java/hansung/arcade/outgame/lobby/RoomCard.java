package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.Optional;

import javax.swing.*;

public final class RoomCard extends JButton {
    private final Room room;
    private final MapPreview map;
    private final RoundPanel header;
    private final TextLabel number, title, status, count;
    private final JComponent entry;

    public static RoomCard of(Room room) {
        return new RoomCard(room);
    }

    public static RoomCard skeleton() {
        return new RoomCard(null);
    }

    public Optional<Room> room() {
        return Optional.ofNullable(room);
    }

    public boolean isSkeleton() {
        return room == null;
    }

    public int radius() {
        return 5;
    }

    private RoomCard(Room room) {
        this.room = room;
        setLayout(null);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        map = new MapPreview(room == null ? null : room.map());
        map.setName("map-preview");
        add(map);
        header = new RoundPanel(OutgamePalette.ROOM_HEADER, 3);
        header.setName("room-header");
        add(header);
        number =
                new TextLabel(room == null ? "" : "%03d".formatted(room.number()))
                        .rounded(3, OutgamePalette.ROOM_NUMBER_BOX);
        number.setHorizontalAlignment(JLabel.CENTER);
        number.setName("room-number");
        header.add(number);
        title = new TextLabel(room == null ? "" : room.title()).outlined(Color.BLACK, 1);
        title.setForeground(OutgamePalette.p3(215, 208, 76));
        title.setName("room-title");
        title.setFont(OutgameFonts.gulimChe(14).deriveFont(Font.BOLD));
        header.add(title);
        status =
                new TextLabel(room == null ? "" : room.status().name())
                        .outlined(new Color(90, 52, 0), 1);
        status.setForeground(new Color(255, 248, 159));
        status.setFont(OutgameFonts.label(17));
        status.setName("room-status");
        add(status);
        count = new TextLabel(room == null ? "" : room.players() + "/" + room.capacity());
        count.setOpaque(true);
        count.setBackground(OutgamePalette.PLAYER_COUNT);
        count.setName("player-count");
        add(count);
        entry =
                new JComponent() {
                    protected void paintComponent(Graphics g) {
                        ((Graphics2D) g)
                                .setPaint(
                                        new GradientPaint(
                                                0,
                                                0,
                                                new Color(190, 240, 255),
                                                0,
                                                getHeight() / 2f,
                                                OutgamePalette.PLAYER_COUNT));
                        g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g.setColor(
                                room != null && room.canJoin() ? Color.YELLOW : Color.LIGHT_GRAY);
                        g.fillOval(8, 3, 7, 7);
                        g.fillRoundRect(6, 10, 11, 11, 3, 3);
                    }
                };
        entry.setName("room-entry");
        add(entry);
    }

    @Override
    public void doLayout() {
        map.setBounds(8, 7, 76, getHeight() - 14);
        header.setBounds(94, 8, getWidth() - 102, 27);
        number.setBounds(4, 4, 42, 19);
        title.setBounds(49, 1, header.getWidth() - 52, 25);
        status.setBounds(94, 44, 135, 30);
        count.setBounds(getWidth() - 75, 49, 40, 25);
        entry.setBounds(getWidth() - 30, 49, 24, 25);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        Color bg = OutgamePalette.ROOM_ITEM;
        if (getModel().isPressed()) bg = bg.darker();
        else if (getModel().isRollover()) bg = bg.brighter();
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
        g.setColor(new Color(255, 255, 255, 150));
        g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 6, 6);
        g.dispose();
    }

    @Override
    public Component findComponentAt(int x, int y) {
        return contains(x, y) ? this : null;
    }
}
