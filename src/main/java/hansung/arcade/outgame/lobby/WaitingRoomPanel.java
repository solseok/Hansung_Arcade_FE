package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.PixelImages;
import hansung.arcade.ingame.render.CharacterRecolor;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.GameStartInfo;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.chat.ChatPanel;
import hansung.arcade.outgame.ui.GlossButton;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.StarRating;
import hansung.arcade.outgame.ui.TextLabel;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hansung.arcade.outgame.ui.WaitingRoomLayout.*;

/**
 * 대기실 (참고 화면 구조). 왼쪽 판에 방 제목줄·8자리·채팅, 오른쪽 판에 캐릭터·팀·맵·준비를 둔다.
 * 자리마다 캐릭터를 팀 색으로 다시 칠해 보여 주고, 상태 줄에 방장·준비(불 켜짐·꺼짐)를 보여 준다.
 * 방장은 맵 선택(랜덤 포함)과 게임 시작을, 손님은 준비를 한다. 맵은 최대 인원(빌리지 6명)에 맞춰 정원이 바뀐다.
 * 모든 구역의 위치·크기는 {@link WaitingRoomLayout}, 색은 {@link OutgamePalette}를 따른다.
 */
public final class WaitingRoomPanel extends JPanel implements Scene {
    private static final String NOTICE_TEXT = "모두 준비되면 방장이 게임을 시작할 수 있어요.";
    private static final String RANDOM_NAME = "랜덤";

    private final OutgameSession session;
    private final SceneManager scenes;
    private final MapPicker picker;
    private final CharacterSprites sprites = CharacterSprites.load();
    private final Map<String, BufferedImage> recolored = new HashMap<>();
    private final TextLabel roomNumber = new TextLabel("", SwingConstants.CENTER);
    private final TextLabel roomTitle = new TextLabel("");
    private final TextLabel roomCount = new TextLabel("", SwingConstants.CENTER);
    private final PlayerSlot[] slots = new PlayerSlot[SLOTS];
    private final ChatPanel chat = new ChatPanel(this::sendChat);
    private final Banner banner = new Banner();
    private final Map<Mascot, JToggleButton> characterCells = new EnumMap<>(Mascot.class);
    private JToggleButton randomCell;
    private final Map<Team, JToggleButton> teamSwatches = new EnumMap<>(Team.class);
    private final RoundPanel mapSection = new RoundPanel(OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
    private final RoundPanel previewPanel = new RoundPanel(OutgamePalette.MAP_PREVIEW_BACKGROUND, 4);
    private MapPreview thumbnail;
    private String thumbnailMapId;
    private final TextLabel[] mapInfo = new TextLabel[MAP_INFO_ROWS];
    private final GlossButton mapSelect = new GlossButton("맵 선택", GlossButton.MAP_SELECT);
    private final GlossButton ready = new GlossButton("준비", GlossButton.READY);
    private final JCheckBox autoReady = new AutoReadyBox();

    public WaitingRoomPanel(OutgameSession session, SceneManager scenes) {
        this(session, scenes, MapPicker.dialog());
    }

    public WaitingRoomPanel(OutgameSession session, SceneManager scenes, MapPicker picker) {
        super(null);
        this.session = session;
        this.scenes = scenes;
        this.picker = picker;
        setPreferredSize(new Dimension(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT));
        setBackground(OutgamePalette.WAITING_FRAME);
        setOpaque(true);
        buildTopBar();
        buildLeftPanel();
        buildRightPanel();
        buildBottomBar();
    }

    // ── 화면 짜기 ───────────────────────────────────────────

    private void buildTopBar() {
        RoundPanel top = section("top-bar", OutgamePalette.WAITING_FRAME, 0);
        place(this, null, top, TOP_BAR);
        TextLabel notice = new TextLabel(NOTICE_TEXT);
        notice.setName("notice");
        notice.setForeground(OutgamePalette.MAP_INFO_TEXT);
        notice.setFill(OutgamePalette.WAITING_NOTICE, ROW_RADIUS);
        notice.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        place(top, TOP_BAR, notice, NOTICE);
    }

    private void buildLeftPanel() {
        RoundPanel left = section("left-panel", OutgamePalette.WAITING_LEFT_PANEL, PANEL_RADIUS);
        place(this, null, left, LEFT_PANEL);

        RoundPanel header = section("room-header", OutgamePalette.WAITING_HEADER, SECTION_RADIUS);
        place(left, LEFT_PANEL, header, ROOM_HEADER);
        roomNumber.setName("room-number");
        roomNumber.setFont(OutgameFonts.label(14));
        roomNumber.setForeground(OutgamePalette.ROOM_NUMBER_TEXT);
        roomNumber.setOutline(OutgamePalette.ROOM_NUMBER_BADGE, 1);
        roomNumber.setFill(OutgamePalette.ROOM_NUMBER_BADGE, ROW_RADIUS);
        place(header, ROOM_HEADER, roomNumber, ROOM_NUMBER);
        roomTitle.setName("room-title");
        roomTitle.setFont(OutgameFonts.gulimCheBold(14));
        roomTitle.setForeground(Color.WHITE);
        roomTitle.setOutline(OutgamePalette.WAITING_SECTION, 1);
        place(header, ROOM_HEADER, roomTitle, ROOM_TITLE);
        roomCount.setName("room-count");
        roomCount.setForeground(Color.WHITE);
        roomCount.setFill(OutgamePalette.WAITING_SECTION, ROW_RADIUS);
        place(header, ROOM_HEADER, roomCount, ROOM_COUNT);

        RoundPanel grid = section("slot-grid", OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
        place(left, LEFT_PANEL, grid, SLOT_GRID);
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = new PlayerSlot(i);
            place(grid, SLOT_GRID, slots[i], slot(i));
        }

        chat.setName("chat");
        place(left, LEFT_PANEL, chat, CHAT);
    }

    private void buildRightPanel() {
        RoundPanel right = section("right-panel", OutgamePalette.WAITING_RIGHT_PANEL, PANEL_RADIUS);
        place(this, null, right, RIGHT_PANEL);

        RoundPanel characters = section("character-section", OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
        place(right, RIGHT_PANEL, characters, CHARACTER_SECTION);
        place(characters, CHARACTER_SECTION, banner, CHARACTER_BANNER);
        ButtonGroup characterGroup = new ButtonGroup();
        Mascot[] mascots = Mascot.values();
        for (int i = 0; i < CHARACTER_CELLS; i++) {
            JToggleButton cell;
            if (i < mascots.length) {
                Mascot mascot = mascots[i];
                cell = new CharacterCell(head(mascot), null);
                cell.setName("character-" + mascot.id());
                cell.setToolTipText(CharacterProfiles.name(mascot));
                cell.addActionListener(event -> {
                    session.select(mascot);
                    refresh();
                });
                characterCells.put(mascot, cell);
            } else if (i == mascots.length) {
                cell = new CharacterCell(null, "?");
                cell.setName("character-random");
                cell.setToolTipText("랜덤 캐릭터");
                cell.addActionListener(event -> {
                    session.selectRandomCharacter();
                    refresh();
                });
                randomCell = cell;
            } else {
                cell = new CharacterCell(null, "X");
                cell.setName("character-locked-" + i);
                cell.setEnabled(false);
            }
            characterGroup.add(cell);
            place(characters, CHARACTER_SECTION, cell, characterCell(i));
        }

        TextLabel teamLabel = new TextLabel("팀선택");
        teamLabel.setForeground(OutgamePalette.SECTION_LABEL);
        place(right, RIGHT_PANEL, teamLabel, TEAM_LABEL);
        RoundPanel teams = section("team-section", OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
        place(right, RIGHT_PANEL, teams, TEAM_SECTION);
        ButtonGroup teamGroup = new ButtonGroup();
        for (Team team : Team.values()) {
            JToggleButton swatch = new TeamSwatch(team);
            swatch.setName("team-" + team.name());
            swatch.setToolTipText(team.label());
            swatch.addActionListener(event -> {
                session.selectTeam(team);
                refresh();
            });
            teamGroup.add(swatch);
            teamSwatches.put(team, swatch);
            place(teams, TEAM_SECTION, swatch, teamSwatch(team.ordinal()));
        }

        TextLabel mapLabel = new TextLabel("맵");
        mapLabel.setForeground(OutgamePalette.SECTION_LABEL);
        place(right, RIGHT_PANEL, mapLabel, MAP_LABEL);
        mapSection.setName("map-section");
        place(right, RIGHT_PANEL, mapSection, MAP_SECTION);
        previewPanel.setName("map-preview");
        place(mapSection, MAP_SECTION, previewPanel, MAP_PREVIEW);
        for (int i = 0; i < MAP_INFO_ROWS; i++) {
            int align = i == 0 ? SwingConstants.CENTER : SwingConstants.LEFT;
            mapInfo[i] = i == MAP_INFO_ROWS - 1 ? new StarRating("인기도:", StarRating.MAX_STARS) : new TextLabel("", align);
            mapInfo[i].setName("map-info-" + i);
            mapInfo[i].setForeground(OutgamePalette.MAP_INFO_TEXT);
            mapInfo[i].setFill(OutgamePalette.MAP_INFO, ROW_RADIUS);
            mapInfo[i].setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            place(mapSection, MAP_SECTION, mapInfo[i], mapInfo(i));
        }
        mapSelect.setName("map-select");
        mapSelect.setFont(OutgameFonts.label(20));
        mapSelect.addActionListener(event -> picker.pick(this, session.mapId()).ifPresent(this::selectMap));
        place(mapSection, MAP_SECTION, mapSelect, MAP_SELECT);

        RoundPanel readyBox = section("ready-section", OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
        place(right, RIGHT_PANEL, readyBox, READY_SECTION);
        ready.setName("ready");
        ready.setFont(OutgameFonts.label(24));
        ready.addActionListener(event -> {
            if (session.isHost()) {
                GameStartInfo start = session.start();
                scenes.show("game", start);
                return;
            }
            session.setReady(!session.ready());
            refresh();
        });
        place(readyBox, READY_SECTION, ready, READY_BUTTON);
        autoReady.setName("auto-ready");
        autoReady.addActionListener(event -> {
            if (autoReady.isSelected() && !session.isHost()) {
                session.setReady(true);
            }
            refresh();
        });
        place(readyBox, READY_SECTION, autoReady, AUTO_READY);
    }

    private void buildBottomBar() {
        RoundPanel bottom = section("bottom-bar", OutgamePalette.BOTTOM_BAR, 0);
        place(this, null, bottom, BOTTOM_BAR);
        GlossButton leave = new GlossButton("로비로 나가기", GlossButton.BLUE);
        leave.setName("leave");
        leave.addActionListener(event -> {
            session.leave();
            chat.clear();
            scenes.show("lobby", null);
        });
        place(bottom, BOTTOM_BAR, leave, LEAVE);
    }

    // ── 상태 반영 ───────────────────────────────────────────

    private void sendChat(String message) {
        chat.message(session.nickname(), message);
    }

    private void selectMap(String mapId) {
        try {
            session.selectMap(mapId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            chat.systemMessage(e.getMessage());
        }
        refresh();
    }

    private void refresh() {
        var room = session.room();
        if (room == null) {
            return;
        }
        roomNumber.setText("%03d".formatted(room.number()));
        roomTitle.setText(room.title());
        roomCount.setText(room.players() + "/" + room.capacity());

        List<GameStartInfo.PlayerInfo> players = session.players();
        boolean meHost = session.isHost();
        int host = session.hostIndex();
        for (int i = 0; i < SLOTS; i++) {
            if (i < players.size()) {
                GameStartInfo.PlayerInfo player = players.get(i);
                boolean local = i < session.localPlayers();
                BufferedImage figure = local && session.randomCharacter()
                        ? randomFigure()
                        : figure(player.character(), Team.fromLabel(player.team()).characterColor());
                slots[i].display(stateOf(i, host, meHost), player.nickname(), figure);
            } else {
                slots[i].display(i < room.capacity() ? SlotState.EMPTY : SlotState.CLOSED, "", null);
            }
        }

        ready.setText(meHost ? "게임 시작" : session.ready() ? "준비 취소" : "준비");
        // 방장은 다른 참가자가 모두 준비했고 방이 대기 중일 때만 시작할 수 있다
        ready.setEnabled(!meHost || session.canStart());
        mapSelect.setEnabled(meHost);
        autoReady.setEnabled(!meHost);
        if (session.randomCharacter()) {
            randomCell.setSelected(true);
        } else {
            characterCells.get(session.character()).setSelected(true);
        }
        teamSwatches.get(session.team()).setSelected(true);
        banner.display(session.randomCharacter() ? null : session.character());
        showMap();
        repaint();
    }

    /** index번째 자리의 상태. 이 컴퓨터의 플레이어는 내 준비 상태를, 목업 참가자는 세션이 들고 있는 준비 상태를 따른다 */
    private SlotState stateOf(int index, int host, boolean meHost) {
        if (index == host) {
            return SlotState.HOST;
        }
        if (index < session.localPlayers() && meHost) {
            // 방장과 같은 컴퓨터의 2P는 방장이 시작할 때 함께 들어가므로 늘 준비한 것으로 보인다
            return SlotState.READY;
        }
        return session.isReady(index + 1) ? SlotState.READY : SlotState.WAITING;
    }

    /** 고른 맵을 보여 준다. 실제 맵은 그림이 미리보기 칸을 꽉 채우고, 랜덤 맵은 보라 액자 안에 물음표 카드를 둔다 */
    private void showMap() {
        GameMaps.GameMap map = GameMaps.byId(session.mapId());
        if (!map.id().equals(thumbnailMapId)) {
            if (thumbnail != null) {
                thumbnail.getParent().remove(thumbnail);
            }
            thumbnail = new MapPreview(map.name(), LobbyArtwork.map(map.name()), 4);
            thumbnail.setName("map-thumbnail");
            if (map.isRandom()) {
                place(previewPanel, MAP_PREVIEW, thumbnail, MAP_THUMBNAIL);
            } else {
                place(mapSection, MAP_SECTION, thumbnail, MAP_PREVIEW);
                // 액자 판보다 위에 그려지게 맨 앞에 둔다
                mapSection.setComponentZOrder(thumbnail, 0);
            }
            previewPanel.setVisible(map.isRandom());
            thumbnailMapId = map.id();
        }
        mapInfo[0].setText(map.name());
        mapInfo[1].setText("인  원: " + map.maxPlayers());
        mapInfo[2].setText("테  마: " + map.theme());
    }

    /** 팀 색으로 고정색을 칠한 캐릭터 정면 그림 (1배). 같은 캐릭터·색은 다시 칠하지 않는다 */
    private BufferedImage figure(Mascot mascot, CharacterColor color) {
        return recolored.computeIfAbsent(mascot.id() + "/" + color.id(),
                key -> CharacterRecolor.recolor(sprites.frame(mascot, Direction.DOWN, false, 0), mascot, color));
    }

    /** 랜덤 캐릭터를 고른 자리의 물음표 그림 (캐릭터 그림과 같은 70x70) */
    private BufferedImage randomFigure() {
        return recolored.computeIfAbsent("random", key -> {
            int size = CharacterSprites.FRAME_SIZE;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            Shapes.text(g, "?", OutgameFonts.label(54), Color.WHITE, OutgamePalette.SECTION_LABEL,
                    OutgamePalette.WAITING_SECTION, 3, null, 0, 0, size, size, SwingConstants.CENTER);
            g.dispose();
            return image;
        });
    }

    /** 캐릭터 칸에 넣는 얼굴: 그림에서 실제 픽셀이 있는 곳의 위쪽 46줄 (1배) */
    private BufferedImage head(Mascot mascot) {
        BufferedImage frame = sprites.frame(mascot, Direction.DOWN, false, 0);
        Box ink = PixelImages.opaqueBounds(frame);
        return frame.getSubimage(ink.x(), ink.y(), ink.width(), Math.min(ink.height(), 46));
    }

    private static RoundPanel section(String name, Color fill, int radius) {
        RoundPanel panel = new RoundPanel(fill, radius);
        panel.setName(name);
        return panel;
    }

    /** child를 절대 좌표 box에 놓는다. parentBox는 parent의 절대 좌표 (화면 자체면 null) */
    private static void place(JComponent parent, Box parentBox, JComponent child, Box box) {
        int x = parentBox == null ? box.x() : box.x() - parentBox.x();
        int y = parentBox == null ? box.y() : box.y() - parentBox.y();
        child.setBounds(x, y, box.width(), box.height());
        parent.add(child);
    }

    @Override
    public void enter(Object data) {
        if (data instanceof Room room) {
            session.enter(room);
            chat.clear();
            chat.systemMessage("다른 참가자는 준비된 목업입니다. 실제 서버 연결 전입니다.");
        } else {
            // 경기를 마치고 돌아왔다: 방을 다시 대기 중으로 돌리고, 자동 준비면 바로 준비한다
            session.returnToWaiting();
            session.setReady(autoReady.isSelected() && !session.isHost());
        }
        refresh();
    }

    @Override
    public JComponent view() { return this; }

    // ── 부품 ──────────────────────────────────────────────

    /** 고른 캐릭터 배너: 주황 바탕에 캐릭터(1배)와 이름. 랜덤이면 물음표와 "랜덤" */
    private final class Banner extends JComponent {
        private final TextLabel name = new TextLabel();
        private BufferedImage figure;

        Banner() {
            setName("character-banner");
            setLayout(null);
            name.setName("character-name");
            name.setFont(OutgameFonts.label(26));
            name.setForeground(Color.WHITE);
            name.setGradient(OutgamePalette.LOGIN_TAB_BOTTOM);
            name.setOutline(OutgamePalette.READY_TEXT_SHADOW, 2);
            name.setBounds(90, 18, 180, 48);
            add(name);
        }

        /** @param mascot 고른 캐릭터. 랜덤이면 null */
        void display(Mascot mascot) {
            if (mascot == null) {
                figure = randomFigure();
                name.setText(RANDOM_NAME);
            } else {
                BufferedImage frame = sprites.frame(mascot, Direction.DOWN, false, 0);
                Box ink = PixelImages.opaqueBounds(frame);
                figure = frame.getSubimage(ink.x(), ink.y(), ink.width(), ink.height());
                name.setText(CharacterProfiles.name(mascot));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int w = getWidth();
            int h = getHeight();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Shapes.hints(g);
                g.drawImage(Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, 5)),
                        Shapes.vertical(h, OutgamePalette.BANNER_TOP, OutgamePalette.BANNER_BOTTOM), d -> {
                            Shapes.gloss(d, w, h, 0.3f);
                            Shapes.edge(d, w, h, 5, OutgamePalette.SELECTED_EDGE, 2f);
                        }), 0, 0, null);
                if (figure != null) {
                    g.drawImage(figure, 16 + (60 - figure.getWidth()) / 2, h - 6 - figure.getHeight(), null);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /** 캐릭터 칸: 얼굴 그림 또는 글자(? 랜덤, X 잠김). 고르면 밝은 테두리와 체크 */
    private static final class CharacterCell extends JToggleButton {
        private final BufferedImage face;
        private final String mark;

        CharacterCell(BufferedImage face, String mark) {
            this.face = face;
            this.mark = mark;
            setText("");
            setFont(OutgameFonts.label(32));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int w = getWidth();
            int h = getHeight();
            boolean on = isSelected();
            Color fill = isEnabled() ? OutgamePalette.CHARACTER_CELL : OutgamePalette.WAITING_RIGHT_PANEL;
            if (isEnabled() && getModel().isRollover() && !on) {
                fill = OutgamePalette.mix(fill, Color.WHITE, 0.15);
            }
            Color body = fill;
            Color top = OutgamePalette.mix(fill, Color.WHITE, 0.2);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Shapes.hints(g);
                g.drawImage(Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, 5)), Shapes.vertical(h, top, body),
                        d -> Shapes.edge(d, w, h, 5, on ? OutgamePalette.SELECTED_EDGE : OutgamePalette.WAITING_SECTION,
                                on ? 2.5f : 1f)), 0, 0, null);
                if (face != null) {
                    g.drawImage(face, (w - face.getWidth()) / 2, h - 4 - face.getHeight(), null);
                } else if (mark != null) {
                    Color color = isEnabled() ? Color.WHITE : OutgamePalette.CHARACTER_CELL;
                    Shapes.text(g, mark, getFont(), color, isEnabled() ? OutgamePalette.SECTION_LABEL : null,
                            isEnabled() ? OutgamePalette.WAITING_SECTION : null, 2, null, 0, 0, w, h, CENTER);
                }
                if (on) {
                    drawCheck(g, w - 17, 3);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /** 팀 색 칸. 고르면 흰 테두리와 체크 */
    private static final class TeamSwatch extends JToggleButton {
        private final Team team;

        TeamSwatch(Team team) {
            this.team = team;
            setText("");
            setFont(OutgameFonts.label(12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int w = getWidth();
            int h = getHeight();
            boolean on = isSelected();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Shapes.hints(g);
                g.drawImage(Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, 4)),
                        Shapes.vertical(h, OutgamePalette.mix(team.color(), Color.WHITE, 0.3), team.color()), d -> {
                            Shapes.gloss(d, w, h, 0.35f);
                            Shapes.edge(d, w, h, 4, on ? Color.WHITE : OutgamePalette.WAITING_FRAME, on ? 2.5f : 1f);
                        }), 0, 0, null);
                if (on) {
                    drawCheck(g, w - 15, 1);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /** 자동 준비: "자동/준비" 글자와 작은 칸. 켜면 칸이 체크색으로 찬다 */
    private static final class AutoReadyBox extends JCheckBox {
        AutoReadyBox() {
            super("자동 준비");
            setFont(OutgameFonts.label(11));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Shapes.hints(g);
                Color text = isEnabled() ? OutgamePalette.SECTION_LABEL : OutgamePalette.STATUS_IDLE;
                Font font = getFont();
                Shapes.text(g, "자동", font, text, null, 0, null, 0, 1, getWidth(), 13, CENTER);
                Shapes.text(g, "준비", font, text, null, 0, null, 0, 14, getWidth(), 13, CENTER);
                int size = 16;
                int x = (getWidth() - size) / 2;
                Color box = isSelected() ? OutgamePalette.CHECK_MARK : OutgamePalette.CHAT_INPUT;
                g.drawImage(Shapes.paint(size, size, m -> m.fill(Shapes.round(0, 0, size, size, 3)), box,
                        d -> Shapes.edge(d, size, size, 3, OutgamePalette.SLOT_EDGE, 1f)), x, 30, null);
            } finally {
                g.dispose();
            }
        }
    }

    private static void drawCheck(Graphics2D g, int x, int y) {
        int[] xs = {x + 1, x + 5, x + 13};
        int[] ys = {y + 6, y + 11, y + 1};
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(OutgamePalette.WAITING_SECTION);
        g.drawPolyline(xs, ys, 3);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(OutgamePalette.CHECK_MARK);
        g.drawPolyline(xs, ys, 3);
    }
}
