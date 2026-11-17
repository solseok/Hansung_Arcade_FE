package hansung.arcade.outgame.lobby;

import static hansung.arcade.outgame.ui.WaitingRoomLayout.*;

import hansung.arcade.core.game.*;
import hansung.arcade.ingame.entity.*;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.*;
import hansung.arcade.outgame.ui.*;

import java.awt.*;
import java.util.Optional;

import javax.swing.*;

/** 방 참가자와 선택 상태를 같은 세션에 반영하는 대기실. */
public final class WaitingRoomPanel extends JPanel implements Scene {
    @FunctionalInterface
    public interface MapPicker {
        Optional<String> pick(Component parent, String current);
    }

    private final OutgameSession session;
    private final SceneManager scenes;
    private final MapPicker picker;
    private boolean autoReady;
    private final JTextPane messages = new JTextPane();
    private final JTextField input = new JTextField();
    private final CharacterSprites sprites = CharacterSprites.load();
    private final JButton ready = LobbyArtwork.button("준비", true);

    public WaitingRoomPanel(LobbyService service, SceneManager scenes) {
        this(new OutgameSession(service), scenes);
    }

    public WaitingRoomPanel(OutgameSession session, SceneManager scenes) {
        this(
                session,
                scenes,
                (parent, current) -> {
                    Object choice =
                            JOptionPane.showInputDialog(
                                    parent,
                                    "맵을 선택하세요.",
                                    "맵 선택",
                                    JOptionPane.PLAIN_MESSAGE,
                                    null,
                                    GameMaps.ids().stream()
                                            .map(id -> GameMaps.byId(id).label())
                                            .toArray(),
                                    GameMaps.byId(current).label());
                    return choice == null
                            ? Optional.empty()
                            : Optional.of(GameMaps.byLabel(choice.toString()).id());
                });
    }

    public WaitingRoomPanel(OutgameSession session, SceneManager scenes, MapPicker picker) {
        super(null);
        this.session = session;
        this.scenes = scenes;
        this.picker = picker;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(OutgamePalette.WAITING_FRAME);
        messages.setEditable(false);
        messages.setBackground(OutgamePalette.CHAT_BACKGROUND);
        messages.setForeground(Color.WHITE);
        messages.setFont(OutgameFonts.gulimChe(12));
        input.setFont(OutgameFonts.gulimChe(12));
        input.setBackground(OutgamePalette.CHAT_INPUT);
        input.setForeground(Color.WHITE);
        input.addActionListener(
                e -> {
                    String text = input.getText().strip();
                    if (!text.isEmpty()) {
                        messages.setText(
                                messages.getText() + session.nickname() + ": " + text + "\n");
                        input.setText("");
                    }
                });
        ready.addActionListener(
                e -> {
                    if (session.service.isHost()) scenes.show("game", session.start());
                    else {
                        session.setReady(!session.ready());
                        refresh();
                    }
                });
    }

    private <T extends JComponent> T place(T c, String name, Box b) {
        c.setName(name);
        c.setBounds(b.x(), b.y(), b.width(), b.height());
        add(c, 0);
        return c;
    }

    private void panel(String name, Box b, Color color) {
        place(new RoundPanel(color, 3), name, b);
    }

    private TextLabel label(String name, String text, Box box) {
        return place(new TextLabel(text), name, box);
    }

    private void refresh() {
        removeAll();
        panel("top-bar", TOP_BAR, OutgamePalette.WAITING_NOTICE);
        panel("left-panel", LEFT_PANEL, OutgamePalette.WAITING_LEFT_PANEL);
        panel("right-panel", RIGHT_PANEL, OutgamePalette.WAITING_RIGHT_PANEL);
        panel("bottom-bar", BOTTOM_BAR, OutgamePalette.BOTTOM_BAR);
        label("notice", "즐거운 한 판을 함께해요!", NOTICE);
        panel("room-header", ROOM_HEADER, OutgamePalette.WAITING_HEADER);
        panel("slot-grid", SLOT_GRID, OutgamePalette.WAITING_SECTION);
        panel("character-section", CHARACTER_SECTION, OutgamePalette.WAITING_SECTION);
        RoundPanel banner =
                place(
                        new RoundPanel(OutgamePalette.CHARACTER_CELL, 3),
                        "character-banner",
                        CHARACTER_BANNER);
        panel("team-section", TEAM_SECTION, OutgamePalette.WAITING_SECTION);
        panel("map-section", MAP_SECTION, OutgamePalette.WAITING_SECTION);
        panel("ready-section", READY_SECTION, OutgamePalette.WAITING_SECTION);
        Room room = session.room();
        if (room == null) return;
        label("room-number", "%03d".formatted(room.number()), ROOM_NUMBER);
        label("room-title", room.title(), ROOM_TITLE);
        label("room-count", room.players() + "/" + room.capacity(), ROOM_COUNT);
        var players = session.players();
        for (int i = 0; i < 8; i++) {
            SlotState state =
                    i >= room.capacity()
                            ? SlotState.CLOSED
                            : i >= players.size()
                                    ? SlotState.EMPTY
                                    : (session.service.isHost()
                                                    ? i == 0
                                                    : i == session.localPlayers())
                                            ? SlotState.HOST
                                            : session.isReady(i + 1)
                                                    ? SlotState.READY
                                                    : SlotState.WAITING;
            place(
                    new PlayerSlot(i < players.size() ? players.get(i) : null, state),
                    "slot-" + i,
                    slot(i));
        }
        panel("chat", CHAT, OutgamePalette.CHAT_BACKGROUND);
        label("chat-tab", "전체 채팅", CHAT_TAB);
        place(messages, "chat-log", CHAT_LOG);
        place(input, "chat-input", CHAT_INPUT_ROW);
        TextLabel characterName =
                new TextLabel(session.randomCharacter() ? "랜덤" : name(session.character()));
        characterName.setName("character-name");
        characterName.setBounds(67, 25, 205, 32);
        banner.add(characterName);
        JLabel portrait =
                new JLabel(
                        new ImageIcon(
                                sprites.frame(session.character(), Direction.DOWN, false, 0)));
        portrait.setBounds(5, 5, 60, 73);
        banner.add(portrait);
        ButtonGroup characters = new ButtonGroup();
        int index = 0;
        for (Mascot mascot : Mascot.values()) {
            JToggleButton b =
                    new ChoiceButton(
                            new ImageIcon(sprites.frame(mascot, Direction.DOWN, false, 0)));
            b.setSelected(!session.randomCharacter() && session.character() == mascot);
            b.addActionListener(
                    e -> {
                        session.select(mascot);
                        refresh();
                    });
            characters.add(b);
            place(b, "character-" + mascot.id(), characterCell(index++));
        }
        JToggleButton random = new ChoiceButton("?");
        random.setSelected(session.randomCharacter());
        characters.add(random);
        random.addActionListener(
                e -> {
                    session.selectRandomCharacter();
                    refresh();
                });
        place(random, "character-random", characterCell(index++));
        while (index < 8) {
            JToggleButton locked = new ChoiceButton("×");
            locked.setEnabled(false);
            place(locked, "character-locked-" + index, characterCell(index++));
        }
        label("team-label", "팀선택", TEAM_LABEL);
        for (Team team : Team.values()) {
            JToggleButton b = new ChoiceButton("");
            b.setBackground(team.color());
            b.setSelected(session.team() == team);
            b.setBorder(
                    BorderFactory.createLineBorder(
                            session.team() == team ? Color.WHITE : team.color().darker(), 2));
            b.addActionListener(
                    e -> {
                        session.selectTeam(team);
                        refresh();
                    });
            place(b, "team-" + team.name(), teamSwatch(team.ordinal()));
        }
        label("map-label", "맵", MAP_LABEL);
        panel("map-preview", MAP_PREVIEW, OutgamePalette.MAP_PREVIEW_BACKGROUND);
        boolean randomMap = session.mapId().equals(GameMaps.RANDOM_ID);
        var map = GameMaps.byId(session.mapId());
        place(
                new MapPreview(map.label()),
                "map-thumbnail",
                randomMap ? MAP_THUMBNAIL : MAP_PREVIEW);
        label("map-info-0", map.label(), mapInfo(0));
        label("map-info-1", "인  원: " + room.capacity(), mapInfo(1));
        label("map-info-2", "테  마: " + map.theme(), mapInfo(2));
        place(new StarRating(), "map-info-3", mapInfo(3));
        JButton choose = LobbyArtwork.button("맵 선택", false);
        choose.setEnabled(session.service.isHost());
        choose.addActionListener(
                e ->
                        picker.pick(this, session.mapId())
                                .ifPresent(
                                        id -> {
                                            try {
                                                session.selectMap(id);
                                                refresh();
                                            } catch (IllegalStateException ex) {
                                                messages.setText(
                                                        messages.getText()
                                                                + ex.getMessage()
                                                                + "\n");
                                            }
                                        }));
        place(choose, "map-select", MAP_SELECT);
        ready.setText(session.service.isHost() ? "게임 시작" : session.ready() ? "준비 취소" : "준비");
        ready.setEnabled(!session.service.isHost() || session.canStart());
        place(ready, "ready", READY_BUTTON);
        JCheckBox auto =
                new JCheckBox("자동", autoReady) {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        Shapes.hints(g);
                        g.setColor(isEnabled() ? Color.WHITE : Color.GRAY);
                        g.drawRect(13, 7, 12, 12);
                        if (isSelected()) {
                            g.drawLine(15, 13, 18, 16);
                            g.drawLine(18, 16, 23, 10);
                        }
                        g.setFont(OutgameFonts.label(10));
                        g.drawString("자동", 8, 34);
                        g.dispose();
                    }
                };
        auto.setBorderPainted(false);
        auto.setContentAreaFilled(false);
        auto.setFont(OutgameFonts.label(10));
        auto.setEnabled(!session.service.isHost());
        auto.addActionListener(
                e -> {
                    autoReady = auto.isSelected();
                    session.setReady(autoReady);
                    refresh();
                });
        place(auto, "auto-ready", AUTO_READY);
        JButton leave = LobbyArtwork.button("로비로 나가기", false);
        leave.addActionListener(
                e -> {
                    session.leave();
                    messages.setText("");
                    scenes.show("lobby", null);
                });
        place(leave, "leave", LEAVE);
        revalidate();
        repaint();
    }

    private String name(Mascot m) {
        return switch (m) {
            case SANGSANGBUGI -> "상상부기";
            case KKUKKU -> "꾸꾸";
            case KKOKKO -> "꼬꼬";
            case SANGZZI -> "상찌";
            case HANSUNGNYANGI -> "한성냥이";
        };
    }

    @Override
    public void enter(Object data) {
        if (data instanceof Room room) {
            session.enter(room);
            messages.setText("목업 대기실에 입장했습니다.\n");
        } else session.returnToWaiting();
        if (autoReady && !session.service.isHost()) session.setReady(true);
        refresh();
    }

    @Override
    public JComponent view() {
        return this;
    }
}
