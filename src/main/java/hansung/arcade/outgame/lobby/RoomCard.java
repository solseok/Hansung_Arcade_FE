package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.ui.LobbyLayout;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.TextLabel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * 로비의 방 한 칸 (참고 화면 6번). 칸 전체가 버튼이라 누르면 입장을 묻는다 (따로 입장 버튼이 없다).
 * 왼쪽에 맵 미리보기(아래 띠에 맵 이름), 오른쪽 위에 번호 상자와 제목이 있는 제목줄, 아래에 WAITING·PLAYING,
 * 인원 라벨, 들어갈 수 있는지 보여 주는 사람 모양 표시를 둔다. 방이 없는 자리는 같은 모양의 빈 스켈레톤이다.
 * 마우스를 올리면 밝아지고, 누르는 동안 어두워진다.
 */
public final class RoomCard extends JButton {

    private final Room room;

    private RoomCard(Room room, String name, int seats) {
        this.room = room;
        setName(name);
        setLayout(null);
        setText("");
        setFont(OutgameFonts.label(12));
        setBackground(OutgamePalette.ROOM_ITEM);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(room != null);

        MapPreview preview = room == null
                ? MapPreview.skeleton(LobbyLayout.MAP_PREVIEW_RADIUS)
                : new MapPreview(room.map(), LobbyArtwork.map(room.map()), LobbyLayout.MAP_PREVIEW_RADIUS);
        place(this, preview, LobbyLayout.CARD_PREVIEW);

        RoundPanel header = new RoundPanel(OutgamePalette.ROOM_HEADER, LobbyLayout.CARD_PART_RADIUS);
        header.setName("room-header");
        place(this, header, LobbyLayout.CARD_HEADER);
        TextLabel number = new TextLabel(room == null ? "" : "%03d".formatted(room.number()), SwingConstants.CENTER);
        number.setName("room-number");
        number.setFont(OutgameFonts.label(15));
        number.setForeground(OutgamePalette.ROOM_NUMBER_DIGITS);
        number.setOutline(OutgamePalette.ROOM_NUMBER_DIGITS_OUTLINE, 1);
        number.setFill(OutgamePalette.ROOM_NUMBER_BOX, LobbyLayout.CARD_PART_RADIUS);
        number.setEdge(OutgamePalette.ROOM_NUMBER_BOX_EDGE, 1f);
        number.setGloss(0.45f);
        place(header, number, LobbyLayout.CARD_NUMBER);
        TextLabel title = new TextLabel(room == null ? "" : room.title());
        title.setName("room-title");
        title.ellipsis();
        title.setFont(OutgameFonts.gulimCheBold(14));
        title.setForeground(OutgamePalette.ROOM_TITLE_TEXT);
        title.setOutline(OutgamePalette.TEXT_OUTLINE, 1);
        place(header, title, LobbyLayout.CARD_TITLE);

        TextLabel status = new TextLabel(room == null ? "" : statusText(room));
        status.setName("room-status");
        status.setFont(OutgameFonts.label(18));
        boolean playing = room != null && room.status() == Room.Status.PLAYING;
        status.setForeground(playing ? OutgamePalette.PLAYING_LABEL_TOP : OutgamePalette.WAITING_LABEL_TOP);
        status.setGradient(playing ? OutgamePalette.PLAYING_LABEL_BOTTOM : OutgamePalette.WAITING_LABEL_BOTTOM);
        status.setOutline(playing ? OutgamePalette.PLAYING_LABEL_OUTLINE : OutgamePalette.WAITING_LABEL_OUTLINE, 1);
        place(this, status, LobbyLayout.CARD_STATUS);

        TextLabel count = new TextLabel(room == null ? "" : room.players() + "/" + room.capacity(), SwingConstants.CENTER);
        count.setName("player-count");
        count.setFont(OutgameFonts.label(13));
        count.setForeground(OutgamePalette.PLAYER_COUNT_TEXT);
        count.setOutline(OutgamePalette.ROOM_NUMBER_DIGITS_OUTLINE, 1);
        count.setFill(OutgamePalette.PLAYER_COUNT, LobbyLayout.CARD_PART_RADIUS);
        place(this, count, LobbyLayout.CARD_COUNT);

        // 2P면 두 자리가 함께 남아야 들어갈 수 있는 방으로 표시한다
        place(this, new EntryBadge(room != null && room.hasSeats(seats)), LobbyLayout.CARD_ENTRY);

        if (room == null) {
            setEnabled(false);
            getAccessibleContext().setAccessibleName("빈 자리");
        } else {
            getAccessibleContext().setAccessibleName(room.number() + "번 방 " + room.title());
            setToolTipText(room.title());
        }
    }

    public static RoomCard of(Room room) {
        return of(room, 1);
    }

    /** seats는 이 컴퓨터에서 함께 들어가는 사람 수(1P면 1, 2P면 2) */
    public static RoomCard of(Room room, int seats) {
        return new RoomCard(room, "room-card-" + room.number(), seats);
    }

    /** 방이 없는 자리. index는 쪽 안 순서 */
    public static RoomCard skeleton(int index) {
        return new RoomCard(null, "room-skeleton-" + index, 1);
    }

    public Optional<Room> room() {
        return Optional.ofNullable(room);
    }

    public boolean isSkeleton() {
        return room == null;
    }

    public int radius() {
        return LobbyLayout.ROOM_RADIUS;
    }

    private static String statusText(Room room) {
        return room.status() == Room.Status.PLAYING ? "PLAYING" : "WAITING";
    }

    private static void place(JComponent parent, JComponent child, Box box) {
        child.setBounds(box.x(), box.y(), box.width(), box.height());
        parent.add(child);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Color fill = getBackground();
        if (room != null && getModel().isArmed() && getModel().isPressed()) {
            fill = OutgamePalette.mix(fill, Color.BLACK, 0.14);
        } else if (room != null && getModel().isRollover()) {
            fill = OutgamePalette.mix(fill, Color.WHITE, 0.22);
        }
        BufferedImage body = Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, radius())), fill, d -> {
            Shapes.shine(d, w, h, OutgamePalette.ROOM_ITEM_SHINE, 2);
            Shapes.edge(d, w, h, radius(), OutgamePalette.ROOM_ITEM_EDGE, 1f);
        });
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.drawImage(body, 0, 0, null);
            if (room != null && isFocusOwner()) {
                Shapes.hints(g);
                g.setColor(Color.WHITE);
                g.draw(Shapes.round(3.5, 3.5, w - 7, h - 7, radius() - 2));
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * 들어갈 수 있는지 보여 주는 사람 모양 표시 (참고 화면 6번 오른쪽 아래).
     * 들어갈 수 있으면 파란 광택 상자에 노란 사람, 가득 찼거나 게임 중이거나 빈 자리면 회색이다.
     */
    private static final class EntryBadge extends JComponent {
        private final boolean open;

        EntryBadge(boolean open) {
            this.open = open;
            setName("room-entry");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Color top = open ? OutgamePalette.ENTRY_TOP : OutgamePalette.ENTRY_CLOSED_TOP;
            Color bottom = open ? OutgamePalette.ENTRY_BOTTOM : OutgamePalette.ENTRY_CLOSED_BOTTOM;
            Color edge = open ? OutgamePalette.ENTRY_EDGE : OutgamePalette.ENTRY_CLOSED_EDGE;
            BufferedImage body = Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, LobbyLayout.CARD_PART_RADIUS)),
                    Shapes.vertical(h, top, bottom), d -> {
                        Shapes.gloss(d, w, h, 0.5f);
                        Shapes.edge(d, w, h, LobbyLayout.CARD_PART_RADIUS, edge, 1f);
                    });
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Shapes.hints(g);
                g.drawImage(body, 0, 0, null);
                double cx = w / 2.0;
                double headR = h * 0.16;
                Ellipse2D head = new Ellipse2D.Double(cx - headR, h * 0.18, headR * 2, headR * 2);
                Path2D person = new Path2D.Double();
                person.moveTo(cx - headR * 0.6, h * 0.18 + headR * 2);
                person.lineTo(cx + headR * 0.6, h * 0.18 + headR * 2);
                person.lineTo(cx + headR * 1.5, h * 0.82);
                person.lineTo(cx - headR * 1.5, h * 0.82);
                person.closePath();
                if (open) {
                    g.setPaint(new java.awt.GradientPaint(0, (float) (h * 0.18), OutgamePalette.ENTRY_PERSON_TOP,
                            0, (float) (h * 0.82), OutgamePalette.ENTRY_PERSON_BOTTOM));
                } else {
                    g.setColor(OutgamePalette.ENTRY_CLOSED_PERSON);
                }
                g.fill(head);
                g.fill(person);
                g.setColor(open ? OutgamePalette.ROOM_NUMBER_DIGITS_OUTLINE : OutgamePalette.ENTRY_CLOSED_EDGE);
                g.draw(head);
                g.draw(person);
            } finally {
                g.dispose();
            }
        }
    }
}
