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
import hansung.arcade.outgame.audio.Sound;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static hansung.arcade.outgame.ui.WaitingRoomLayout.*;

/**
 * 대기실 (참고 화면 구조). 왼쪽 판에 방 제목줄·8자리·채팅, 오른쪽 판에 캐릭터·팀·맵·준비를 둔다.
 * 자리마다 캐릭터를 팀 색으로 다시 칠해 보여 주고, 상태 줄에 방장·준비(불 켜짐·꺼짐)를 보여 준다.
 * 방장은 맵 선택(랜덤 포함)과 게임 시작을, 손님은 준비를 한다. 맵은 최대 인원(빌리지 6명)에 맞춰 정원이 바뀐다.
 * 모든 구역의 위치·크기는 {@link WaitingRoomLayout}, 색은 {@link OutgamePalette}를 따른다.
 *
 * <p>2P(한 컴퓨터 2인)로 들어오면 캐릭터 칸과 팀 칸을 왼쪽 클릭하면 1P의 것을, 오른쪽 클릭(맥의 Ctrl+클릭 포함)하면 2P의 것을 고른다.
 * 칸에는 고른 사람의 "1P"(빨강)·"2P"(파랑) 표시가 붙고, 위 배너는 마지막으로 고른 사람의 캐릭터를 보여 준다.
 * 1P(혼자)면 오른쪽 클릭은 아무 일도 하지 않고, 고른 칸에는 체크만 붙는다.</p>
 *
 * <p>방장이 게임을 시작하면 크레이지아케이드처럼 대기실이 {@link #START_FADE_MS}ms 동안 검게 어두워진 뒤 게임 화면으로 넘어간다
 * (게임은 검은 화면에서 시작한다). 어두워지는 동안에는 마우스로 누르거나 키를 쳐도 아무 일도 없고,
 * 그 사이 대기실을 떠나면 게임으로 넘어가지 않는다.
 * 화면에 보이지 않는 대기실(테스트 등)은 기다리지 않고 바로 넘어간다.</p>
 *
 * <p>온라인이면 자리·방장·준비는 서버의 ROOM_STATE로 다시 그리고, 채팅은 서버가 돌려준 CHAT_MESSAGE로 보여 준다
 * (입장·퇴장·방장 안내 포함). 방장이 게임을 시작하면 GAME_STARTED를 받은 모두가 서버가 정한 맵·참가자로 게임 화면에 들어가고
 * LOADED를 보낸다. 서버에서 경기가 아직 진행 중이면(게임 중) 고르기·준비·맵을 막는다.</p>
 */
public final class WaitingRoomPanel extends JPanel implements Scene {
    private static final String NOTICE_TEXT = "모두 준비되면 방장이 게임을 시작할 수 있어요.";
    /** 2P로 들어왔을 때 알림 줄 */
    static final String NOTICE_TEXT_2P = "캐릭터·팀: 왼쪽 클릭은 1P, 오른쪽 클릭은 2P";
    /** 캐릭터를 고를 때 나는 소리 */
    static final String SELECT_SOUND = "Bt_c_SelectAIBattleChar";
    /** 팀·자동 준비를 고를 때 나는 소리 */
    static final String CHECK_SOUND = "Bt_c_CheckBox";
    /** 게임을 시작할 때 대기실이 검게 어두워지는 시간(ms). 크아 플레이 영상에서 잰 값 */
    static final int START_FADE_MS = 300;
    /** 1P·2P 표시 색 */
    static final Color PLAYER_1_MARK = new Color(232, 40, 36);
    static final Color PLAYER_2_MARK = new Color(24, 118, 255);
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
    private final Map<Mascot, CharacterCell> characterCells = new EnumMap<>(Mascot.class);
    private CharacterCell randomCell;
    private final Map<Team, TeamSwatch> teamSwatches = new EnumMap<>(Team.class);
    private final TextLabel notice = new TextLabel(NOTICE_TEXT);
    /** 배너에 보여 줄 사람: 마지막으로 캐릭터를 고른 사람 (0이 1P) */
    private int bannerLocal;
    /** 게임을 시작할 때 대기실을 덮으며 어두워지는 검은 막 */
    private final Curtain curtain = new Curtain();
    private final RoundPanel mapSection = new RoundPanel(OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
    private final RoundPanel previewPanel = new RoundPanel(OutgamePalette.MAP_PREVIEW_BACKGROUND, 4);
    private MapPreview thumbnail;
    private String thumbnailMapId;
    private final TextLabel[] mapInfo = new TextLabel[MAP_INFO_ROWS];
    private final GlossButton mapSelect = new GlossButton("맵 선택", GlossButton.MAP_SELECT);
    private final GlossButton ready = new GlossButton("준비", GlossButton.READY);
    private final JCheckBox autoReady = new AutoReadyBox();
    /** 이 화면이 떠 있는지. 서버 알림은 떠 있을 때만 화면에 반영한다 */
    private boolean showing;
    /** 마지막으로 그린 방 상태. 게임 중 → 대기 중으로 바뀌면 자동 준비를 다시 켠다 */
    private Room.Status lastStatus;

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
        curtain.setBounds(0, 0, WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        add(curtain);
        // 모든 구역보다 위에 그리고, 마우스도 먼저 받는다
        setComponentZOrder(curtain, 0);
        session.online().ifPresent(this::listen);
    }

    /** 서버에서 온 일을 화면에 반영한다 (EDT) */
    private void listen(ServerLobbyService server) {
        server.listen(new ServerLobbyService.Listener() {
            @Override
            public void roomStateChanged(RoomState state) {
                if (!showing) {
                    return;
                }
                if (lastStatus == Room.Status.PLAYING && state.room().status() == Room.Status.WAITING
                        && autoReady.isSelected() && !session.isHost()) {
                    session.setReady(true);
                }
                refresh();
            }

            @Override
            public void chatReceived(ServerLobbyService.ChatLine line) {
                // 게임 화면에 있는 동안 온 채팅·안내도 쌓아 두어, 대기실로 돌아오면 보인다
                if (line.notice()) {
                    chat.systemMessage(line.text());
                } else {
                    chat.message(line.sender(), line.text());
                }
            }

            @Override
            public void serverError(String message) {
                chat.systemMessage(message);
            }

            @Override
            public void gameOver(hansung.arcade.outgame.GameResult result) {
                // 시작 페이드 중에 경기가 끝났으면(상대가 바로 나감 등) 게임 화면으로 가지 않는다
                if (showing && curtain.isFading()) {
                    curtain.clear();
                    chat.systemMessage("경기가 시작되자마자 끝났습니다.");
                }
            }

            @Override
            public void gameStarted(ServerLobbyService.GameStarted started) {
                GameStartInfo start;
                try {
                    start = session.startInfo(started);
                } catch (RuntimeException e) {
                    // 게임을 열 수 없으면 서버 경기에서 빠진다 (LOADED를 못 보내 방에서 빠지기를 기다리지 않게)
                    session.exitGame();
                    chat.clear();
                    scenes.show("lobby", "게임을 열지 못했습니다: " + e.getMessage());
                    return;
                }
                if (!showing) {
                    // 결과 모달 등 다른 화면에 있으면 페이드 없이 바로 게임으로 간다
                    scenes.show("game", start);
                    server.loaded();
                    return;
                }
                if (!session.isHost()) {
                    Sound.play("Bt_c_GameStart");
                }
                // 방장이 혼자 시작할 때처럼 어두워진 뒤 게임 화면으로 가고, 다 띄웠다고 서버에 알린다 (10초 안)
                fadeOutThen(() -> {
                    scenes.show("game", start);
                    server.loaded();
                });
            }
        });
    }

    // ── 화면 짜기 ───────────────────────────────────────────

    private void buildTopBar() {
        RoundPanel top = section("top-bar", OutgamePalette.WAITING_FRAME, 0);
        place(this, null, top, TOP_BAR);
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
        roomTitle.ellipsis();
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
        Mascot[] mascots = Mascot.values();
        for (int i = 0; i < CHARACTER_CELLS; i++) {
            CharacterCell cell;
            if (i < mascots.length) {
                Mascot mascot = mascots[i];
                cell = new CharacterCell(head(mascot), null);
                cell.setName("character-" + mascot.id());
                cell.setToolTipText(CharacterProfiles.name(mascot));
                onPick(cell, local -> {
                    session.select(local, mascot);
                    bannerLocal = local;
                    Sound.play(SELECT_SOUND);
                    refresh();
                });
                characterCells.put(mascot, cell);
            } else if (i == mascots.length) {
                cell = new CharacterCell(null, "?");
                cell.setName("character-random");
                cell.setToolTipText("랜덤 캐릭터");
                onPick(cell, local -> {
                    session.selectRandomCharacter(local);
                    bannerLocal = local;
                    Sound.play(SELECT_SOUND);
                    refresh();
                });
                randomCell = cell;
            } else {
                cell = new CharacterCell(null, "X");
                cell.setName("character-locked-" + i);
                cell.setEnabled(false);
            }
            place(characters, CHARACTER_SECTION, cell, characterCell(i));
        }

        TextLabel teamLabel = new TextLabel("팀선택");
        teamLabel.setName("team-label");
        teamLabel.setForeground(OutgamePalette.SECTION_LABEL);
        place(right, RIGHT_PANEL, teamLabel, TEAM_LABEL);
        RoundPanel teams = section("team-section", OutgamePalette.WAITING_SECTION, SECTION_RADIUS);
        place(right, RIGHT_PANEL, teams, TEAM_SECTION);
        for (Team team : Team.values()) {
            TeamSwatch swatch = new TeamSwatch(team);
            swatch.setName("team-" + team.name());
            swatch.setToolTipText(team.label());
            onPick(swatch, local -> {
                session.selectTeam(local, team);
                Sound.play(CHECK_SOUND);
                refresh();
            });
            teamSwatches.put(team, swatch);
            place(teams, TEAM_SECTION, swatch, teamSwatch(team.ordinal()));
        }

        TextLabel mapLabel = new TextLabel("맵");
        mapLabel.setName("map-label");
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
        // 준비·시작은 누를 때마다 다른 소리를 낸다
        ready.setClickSound(null);
        ready.addActionListener(event -> {
            if (curtain.isFading()) {
                return;
            }
            if (session.isHost()) {
                GameStartInfo start;
                try {
                    start = session.start();
                } catch (IllegalStateException e) {
                    chat.systemMessage(e.getMessage());
                    Sound.play("Bt_c_GameStartNotAble");
                    return;
                }
                Sound.play("Bt_c_GameStart");
                if (start != null) {
                    fadeOutThen(() -> scenes.show("game", start));
                } else {
                    // 온라인: GAME_STARTED를 받을 때 모두 함께 어두워진 뒤 게임 화면으로 간다. 그 사이 다시 누르지 못하게 끈다
                    ready.setEnabled(false);
                }
                return;
            }
            session.setReady(!session.ready());
            Sound.play(session.ready() ? "Bt_c_GameReadyOn" : "Bt_c_GameReadyOff");
            refresh();
        });
        place(readyBox, READY_SECTION, ready, READY_BUTTON);
        autoReady.setName("auto-ready");
        autoReady.addActionListener(event -> {
            Sound.play(CHECK_SOUND);
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
        leave.setClickSound("Bt_c_Out");
        leave.addActionListener(event -> {
            // 서버가 거절해도(이미 방에서 빠짐 등) 로비로 간다. 이유는 로비 안내 줄에 보인다
            String failure = null;
            try {
                session.leave();
            } catch (IllegalStateException e) {
                failure = e.getMessage();
            }
            chat.clear();
            scenes.show("lobby", failure);
        });
        place(bottom, BOTTOM_BAR, leave, LEAVE);
    }

    /** 대기실을 {@link #START_FADE_MS}ms 동안 검게 어둡게 한 뒤 next를 한다. 화면에 보이지 않으면 바로 한다 */
    private void fadeOutThen(Runnable next) {
        if (!isShowing()) {
            next.run();
            return;
        }
        curtain.fade(next);
    }

    /**
     * 칸을 왼쪽 클릭하면 1P(0)로, 2P로 들어왔으면 오른쪽 클릭하면 2P(1)로 pick을 부른다.
     * 왼쪽 클릭은 버튼의 눌림(키보드 Space 포함)으로 받고, 오른쪽 클릭은 칸 안에서 뗄 때 받는다.
     * Ctrl+클릭은 {@link PickCell}이 오른쪽 클릭으로 바꿔 넘긴다.
     */
    private void onPick(JToggleButton cell, IntConsumer pick) {
        cell.addActionListener(event -> pick.accept(0));
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && cell.isEnabled() && cell.contains(e.getPoint())
                        && session.localPlayers() > 1) {
                    pick.accept(1);
                }
            }
        });
    }

    // ── 상태 반영 ───────────────────────────────────────────

    private void sendChat(String message) {
        if (session.isOnline()) {
            // 서버가 방 전체에 돌려주는 CHAT_MESSAGE로 뜬다
            session.chat(message);
            return;
        }
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

        Map<Integer, OutgameSession.SeatView> seats = new HashMap<>();
        session.seats().forEach(seat -> seats.put(seat.slot(), seat));
        boolean meHost = session.isHost();
        for (int i = 0; i < SLOTS; i++) {
            OutgameSession.SeatView seat = seats.get(i);
            if (seat != null) {
                BufferedImage figure = seat.character() == null
                        ? randomFigure()
                        : figure(seat.character(), seat.team().characterColor());
                slots[i].display(seat.state(), seat.nickname(), figure);
            } else {
                slots[i].display(i < room.capacity() ? SlotState.EMPTY : SlotState.CLOSED, "", null);
            }
        }

        // 서버에서 경기가 아직 끝나지 않았으면(게임 중) 고르기·준비·맵을 막는다
        boolean waiting = room.status() == Room.Status.WAITING;
        lastStatus = room.status();
        ready.setText(meHost ? "게임 시작" : session.ready() ? "준비 취소" : "준비");
        // 방장은 다른 참가자가 모두 준비했고 방이 대기 중일 때만 시작할 수 있다
        ready.setEnabled(waiting && (!meHost || session.canStart()));
        mapSelect.setEnabled(meHost && waiting);
        autoReady.setEnabled(!meHost);
        characterCells.values().forEach(cell -> cell.setEnabled(waiting));
        randomCell.setEnabled(waiting);
        teamSwatches.values().forEach(swatch -> swatch.setEnabled(waiting));
        // 칸마다 그 칸을 고른 사람(1P·2P)을 표시한다
        int locals = session.localPlayers();
        boolean two = locals > 1;
        if (bannerLocal >= locals) {
            bannerLocal = 0;
        }
        for (Map.Entry<Mascot, CharacterCell> entry : characterCells.entrySet()) {
            entry.getValue().mark(pickers(locals, l -> !session.randomCharacter(l) && session.character(l) == entry.getKey()), two);
        }
        randomCell.mark(pickers(locals, session::randomCharacter), two);
        for (Map.Entry<Team, TeamSwatch> entry : teamSwatches.entrySet()) {
            entry.getValue().mark(pickers(locals, l -> session.team(l) == entry.getKey()), two);
        }
        notice.setText(two ? NOTICE_TEXT_2P : NOTICE_TEXT);
        banner.display(session.randomCharacter(bannerLocal) ? null : session.character(bannerLocal),
                two ? bannerLocal : -1);
        showMap();
        repaint();
    }

    /** 고른 맵을 보여 준다. 실제 맵은 그림이 미리보기 칸을 꽉 채우고, 랜덤 맵은 보라 액자 안에 물음표 카드를 둔다 */
    private void showMap() {
        GameMaps.GameMap map = GameMaps.byIdOrDefault(session.mapId());
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
        showing = true;
        curtain.clear();
        if (data instanceof Room room) {
            session.enter(room);
            chat.clear();
        } else {
            // 경기를 마치고 돌아왔다: 방을 다시 대기 중으로 돌린다
            session.returnToWaiting();
        }
        // 새 방에 들어왔든 경기를 마치고 돌아왔든, 자동 준비면 바로 준비한다
        session.setReady(autoReady.isSelected() && !session.isHost());
        refresh();
    }

    /** 대기실을 떠나면 짙어지던 막을 걷는다. 그래야 막이 다 짙어졌을 때 떠난 방의 게임을 시작하지 않는다 */
    @Override
    public void exit() {
        showing = false;
        curtain.clear();
    }

    /**
     * 검은 막이 덮여 있는 동안에는 자식끼리 겹친다고 알린다. 그래야 막 아래의 버튼이 눌린 모양 등으로 혼자 다시 그려질 때도
     * 막을 그 위에 함께 다시 그려, 버튼만 밝게 튀어나와 보이지 않는다.
     */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return curtain == null || !curtain.isVisible();
    }

    @Override
    public JComponent view() { return this; }

    // ── 부품 ──────────────────────────────────────────────

    /** 이 컴퓨터 플레이어(0이 1P) 가운데 picked인 사람들의 비트 묶음. 1P면 1, 2P면 2 */
    private static int pickers(int locals, IntPredicate picked) {
        int bits = 0;
        for (int local = 0; local < locals; local++) {
            if (picked.test(local)) {
                bits |= 1 << local;
            }
        }
        return bits;
    }

    /** 고른 사람 표시 ("1P"·"2P" 작은 딱지). (x, y)는 딱지 왼쪽 위 */
    private static void drawPlayerMark(Graphics2D g, int local, int x, int y) {
        String text = (local + 1) + "P";
        Font font = OutgameFonts.label(10);
        int w = 19;
        int h = 13;
        Color fill = local == 0 ? PLAYER_1_MARK : PLAYER_2_MARK;
        g.drawImage(Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, 4)), fill,
                d -> Shapes.edge(d, w, h, 4, Color.WHITE, 1.2f)), x, y, null);
        Shapes.text(g, text, font, Color.WHITE, null, 0, null, x, y, w, h, SwingConstants.CENTER);
    }

    /**
     * 게임을 시작할 때 대기실 전체를 덮으며 {@link #START_FADE_MS}ms 동안 투명에서 검정으로 짙어지는 막.
     * 짙어지는 동안에는 마우스(클릭·마우스 올림)를 받아 버리고, 키도 모두 버려 Space·Enter로 아래 버튼을 누르거나
     * 채팅을 칠 수 없게 한다.
     */
    static final class Curtain extends JComponent {
        private final Timer timer = new Timer(15, event -> step());
        /** 짙어지는 동안 모든 키를 버린다. 키가 어느 부품으로 가든 그보다 먼저 받는다 */
        private final KeyEventDispatcher keyBlocker = event -> true;
        /** keyBlocker를 붙인 곳. 붙이지 않았으면 null */
        private KeyboardFocusManager blocking;
        private long startedAt;
        private float alpha;
        private Runnable next;

        Curtain() {
            setName("start-curtain");
            setOpaque(false);
            setVisible(false);
            // 덮인 동안 아래 칸·버튼으로 클릭이 가지 않게 받아서 버린다. 마우스 리스너가 있으면 마우스 이동도 막이 받아
            // 아래 버튼이 마우스 올림으로 밝아지지 않는다
            addMouseListener(new MouseAdapter() { });
            timer.setCoalesce(true);
        }

        /** 짙어지는 동안 키를 버리는지 */
        boolean blocksKeys() {
            return blocking != null;
        }

        /** 시작한 뒤 elapsedMs가 지났을 때 막의 불투명도(0~1) */
        static float alphaAt(long elapsedMs) {
            return (float) Math.max(0, Math.min(1, elapsedMs / (double) START_FADE_MS));
        }

        boolean isFading() {
            return timer.isRunning();
        }

        void fade(Runnable then) {
            next = then;
            startedAt = System.nanoTime();
            alpha = 0;
            setVisible(true);
            blockKeys();
            timer.restart();
        }

        /** 막을 걷는다. 짙어지던 중이면 멈추고 다 짙어져도 아무것도 하지 않는다. 대기실을 떠나거나 다시 들어올 때 부른다 */
        void clear() {
            stop();
            next = null;
            alpha = 0;
            setVisible(false);
        }

        private void blockKeys() {
            if (blocking == null) {
                blocking = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                blocking.addKeyEventDispatcher(keyBlocker);
            }
        }

        private void stop() {
            timer.stop();
            if (blocking != null) {
                blocking.removeKeyEventDispatcher(keyBlocker);
                blocking = null;
            }
        }

        private void step() {
            alpha = alphaAt((System.nanoTime() - startedAt) / 1_000_000);
            repaint();
            if (alpha >= 1) {
                stop();
                Runnable then = next;
                next = null;
                if (then != null) {
                    then.run();
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (alpha <= 0) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g.dispose();
            }
        }
    }

    /** 고른 캐릭터 배너: 주황 바탕에 캐릭터(1배)와 이름. 랜덤이면 물음표와 "랜덤" */
    private final class Banner extends JComponent {
        private final TextLabel name = new TextLabel();
        private BufferedImage figure;
        /** 2P로 들어왔을 때 배너가 보여 주는 사람(0이 1P). 혼자면 -1 */
        private int local = -1;

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

        /**
         * @param mascot 고른 캐릭터. 랜덤이면 null
         * @param local  2P로 들어왔으면 이 캐릭터를 고른 사람(0이 1P). 혼자면 -1
         */
        void display(Mascot mascot, int local) {
            this.local = local;
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
                if (local >= 0) {
                    drawPlayerMark(g, local, 5, 5);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * 1P·2P가 고르는 칸(캐릭터·팀). Ctrl을 쥐고 왼쪽 버튼으로 누른 클릭은 오른쪽 클릭으로 바꿔 넘긴다.
     * 맥은 Ctrl+클릭이 오른쪽 클릭인데 Java에는 Ctrl을 쥔 왼쪽 버튼으로 와서, 그대로 두면 2P가 아니라 1P가 고르게 된다.
     * 누를 때 Ctrl을 쥐었는지로 정하고, 뗄 때까지 같은 버튼으로 넘긴다 (누른 뒤 Ctrl을 떼거나 쥐어도 칸이 눌린 채 남지 않게).
     */
    private abstract static class PickCell extends JToggleButton {
        private boolean ctrlPress;

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    ctrlPress = e.isControlDown();
                }
                if (ctrlPress) {
                    e = asRightButton(e);
                }
            }
            super.processMouseEvent(e);
        }

        /** 같은 자리·같은 때의 오른쪽 버튼 이벤트 */
        static MouseEvent asRightButton(MouseEvent e) {
            int modifiers = e.getModifiersEx() & ~(InputEvent.BUTTON1_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                modifiers |= InputEvent.BUTTON3_DOWN_MASK;
            }
            return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), modifiers, e.getX(), e.getY(),
                    e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(), MouseEvent.BUTTON3);
        }
    }

    /**
     * 캐릭터 칸: 얼굴 그림 또는 글자(? 랜덤, X 잠김). 고르면 밝은 테두리와 체크.
     * 2P로 들어왔으면 체크 대신 고른 사람의 "1P"(왼쪽 위)·"2P"(오른쪽 위) 딱지를 붙인다.
     */
    private static final class CharacterCell extends PickCell {
        private final BufferedImage face;
        private final String mark;
        /** 이 칸을 고른 사람들 (1P 1, 2P 2) */
        private int pickers;
        private boolean twoPlayers;

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

        /** 이 칸을 고른 사람들을 표시한다. 누군가 골랐으면 골라진 칸이다 */
        void mark(int pickers, boolean twoPlayers) {
            this.pickers = pickers;
            this.twoPlayers = twoPlayers;
            setSelected(pickers != 0);
            repaint();
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
                if (on && twoPlayers) {
                    if ((pickers & 1) != 0) {
                        drawPlayerMark(g, 0, 2, 2);
                    }
                    if ((pickers & 2) != 0) {
                        drawPlayerMark(g, 1, w - 21, 2);
                    }
                } else if (on) {
                    drawCheck(g, w - 17, 3);
                }
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * 팀 색 칸. 고르면 흰 테두리와 체크.
     * 2P로 들어왔으면 체크 대신 고른 사람의 "1P"(위)·"2P"(아래) 딱지를 붙인다.
     */
    private static final class TeamSwatch extends PickCell {
        private final Team team;
        private int pickers;
        private boolean twoPlayers;

        TeamSwatch(Team team) {
            this.team = team;
            setText("");
            setFont(OutgameFonts.label(12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        void mark(int pickers, boolean twoPlayers) {
            this.pickers = pickers;
            this.twoPlayers = twoPlayers;
            setSelected(pickers != 0);
            repaint();
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
                if (on && twoPlayers) {
                    if ((pickers & 1) != 0) {
                        drawPlayerMark(g, 0, (w - 19) / 2, 1);
                    }
                    if ((pickers & 2) != 0) {
                        drawPlayerMark(g, 1, (w - 19) / 2, h - 14);
                    }
                } else if (on) {
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
