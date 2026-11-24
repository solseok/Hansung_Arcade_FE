package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.ui.ArrowButton;
import hansung.arcade.outgame.ui.GlossButton;
import hansung.arcade.outgame.ui.LobbyLayout;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.TextLabel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 로비. 대기 중인 방을 앞에, 게임 중인 방을 뒤에 둔 한 목록을 8칸씩 보여 준다 ({@link RoomOrder}).
 * 방 칸을 누르면 입장을 묻고({@link EntryPrompt}), 게임 중이거나 가득 찬 방은 묻지 않고 안내만 한다.
 * 쪽은 목록 아래로 튀어나온 자리의 ◁ ▷ 버튼으로 넘긴다.
 *
 * <p>온라인이면 로비에 들어올 때 서버에서 목록을 받고(ROOM_LIST), 다른 사람이 방을 만들거나 들어가고 나가면
 * 서버가 보내는 목록(LOBBY_ROOMS)으로 바로 다시 그린다. 만들기·입장·바로시작이 거절되면 서버 문구를 안내 줄에 보여 준다.</p>
 */
public final class LobbyPanel extends JPanel implements Scene {
    private final OutgameSession session;
    private final SceneManager scenes;
    private final EntryPrompt prompt;
    private final RoomCreator creator;
    private final RoomListPanel list = new RoomListPanel();
    private final List<RoomCard> cards = new ArrayList<>();
    private final TextLabel message = new TextLabel(" ", SwingConstants.CENTER);
    private final TextLabel nickname = new TextLabel("", SwingConstants.RIGHT);
    private final ArrowButton previous = new ArrowButton("이전쪽", ArrowButton.Direction.LEFT);
    private final ArrowButton next = new ArrowButton("다음쪽", ArrowButton.Direction.RIGHT);
    private int page;
    /**
     * 이 화면이 떠 있는지. 서버가 보낸 목록은 떠 있을 때만 다시 그리고,
     * 확인 창이 떠 있는 동안 연결이 끊겨 다른 화면으로 갔으면 입장하지 않는다
     */
    private boolean showing;

    public LobbyPanel(OutgameSession session, SceneManager scenes) {
        this(session, scenes, EntryPrompt.dialog());
    }

    public LobbyPanel(OutgameSession session, SceneManager scenes, EntryPrompt prompt) {
        this(session, scenes, prompt, RoomCreator.dialog());
    }

    public LobbyPanel(OutgameSession session, SceneManager scenes, EntryPrompt prompt, RoomCreator creator) {
        super(null);
        this.session = session;
        this.scenes = scenes;
        this.prompt = prompt;
        this.creator = creator;
        setPreferredSize(new Dimension(LobbyLayout.WIDTH, LobbyLayout.HEIGHT));
        setBackground(OutgamePalette.LOBBY_BACKGROUND);
        setOpaque(true);

        JButton create = LobbyArtwork.button("방만들기", true);
        create.setName("create-room");
        JButton quick = LobbyArtwork.button("바로시작", true);
        quick.setName("quick-join");
        create.addActionListener(event -> createRoom());
        quick.addActionListener(event -> enterRoom(session::quickJoin));
        place(this, create, LobbyLayout.CREATE_BUTTON);
        place(this, quick, LobbyLayout.QUICK_BUTTON);
        nickname.setName("lobby-nickname");
        nickname.setForeground(Color.WHITE);
        nickname.setFont(OutgameFonts.gulimCheBold(15));
        nickname.setOutline(OutgamePalette.BUTTON_BLUE_EDGE, 1);
        place(this, nickname, LobbyLayout.NICKNAME);

        place(this, list, LobbyLayout.ROOM_LIST);
        previous.setName("page-prev");
        next.setName("page-next");
        previous.addActionListener(event -> {
            page--;
            refresh();
        });
        next.addActionListener(event -> {
            page++;
            refresh();
        });
        place(list, previous, LobbyLayout.PAGE_PREV);
        place(list, next, LobbyLayout.PAGE_NEXT);

        message.setName("lobby-message");
        message.setFont(OutgameFonts.label(13));
        message.setForeground(Color.WHITE);
        message.setOutline(OutgamePalette.BUTTON_BLUE_EDGE, 1);
        place(this, message, LobbyLayout.MESSAGE);

        RoundPanel footer = new RoundPanel(OutgamePalette.LOBBY_FOOTER, 0);
        footer.setName("footer");
        place(this, footer, LobbyLayout.FOOTER);
        JButton menu = new GlossButton("메뉴", GlossButton.BLUE);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem reload = new JMenuItem("방 목록 새로고침");
        reload.addActionListener(event -> reload());
        popup.add(reload);
        JCheckBoxMenuItem mute = new JCheckBoxMenuItem("음소거", Sound.isMuted());
        mute.addActionListener(event -> Sound.setMuted(mute.isSelected()));
        popup.add(mute);
        menu.addActionListener(event -> popup.show(menu, 0, -popup.getPreferredSize().height));
        JButton exit = new GlossButton("나가기", GlossButton.BLUE);
        exit.addActionListener(event -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
        place(footer, menu, relative(LobbyLayout.MENU, LobbyLayout.FOOTER));
        place(footer, exit, relative(LobbyLayout.EXIT, LobbyLayout.FOOTER));

        session.online().ifPresent(server -> server.listen(new ServerLobbyService.Listener() {
            @Override
            public void roomsChanged() {
                if (showing) {
                    // 다른 사람 때문에 다시 그려도 방금 보여 준 안내(입장 거절 등)는 남긴다
                    String kept = message.getText();
                    refresh();
                    if (!kept.isBlank() && !session.service.rooms().isEmpty()) {
                        message.setText(kept);
                    }
                }
            }
        }));
    }

    /** 목록을 새로 받아 다시 그린다. 받지 못하면 이유를 안내 줄에 보여 준다 */
    private void reload() {
        String failure = null;
        try {
            session.service.reload();
        } catch (IllegalStateException exception) {
            failure = exception.getMessage();
        }
        refresh();
        if (failure != null) {
            message.setText(failure);
        }
    }

    private void createRoom() {
        creator.create(this).ifPresent(request -> {
            if (showing) {
                enterRoom(() -> session.create(request.title(), request.capacity()));
            }
        });
    }

    private void onCardClicked(Room room) {
        if (room.status() == Room.Status.PLAYING) {
            message.setText(room.number() + "번 방은 게임 중입니다. 끝나면 들어갈 수 있어요.");
            return;
        }
        if (!room.hasSeats(session.localPlayers())) {
            message.setText(room.number() + "번 방은 자리가 가득 찼습니다.");
            return;
        }
        if (prompt.confirm(this, room) && showing) {
            enterRoom(() -> session.join(room.number()));
        }
    }

    private void enterRoom(Supplier<Room> action) {
        Room room;
        try {
            room = action.get();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            refresh();
            message.setText(exception.getMessage());
            return;
        }
        scenes.show("waiting", room);
    }

    private void refresh() {
        List<Room> rooms = RoomOrder.sort(session.service.rooms(), session.localPlayers());
        int pages = Math.max(1, (rooms.size() + LobbyLayout.PAGE_SIZE - 1) / LobbyLayout.PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        cards.forEach(list::remove);
        cards.clear();
        for (int slot = 0; slot < LobbyLayout.PAGE_SIZE; slot++) {
            int index = page * LobbyLayout.PAGE_SIZE + slot;
            RoomCard card;
            if (index < rooms.size()) {
                Room room = rooms.get(index);
                card = RoomCard.of(room, session.localPlayers());
                card.addActionListener(event -> onCardClicked(room));
            } else {
                card = RoomCard.skeleton(slot);
            }
            place(list, card, LobbyLayout.room(slot));
            cards.add(card);
        }
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);
        message.setText(rooms.isEmpty() ? "표시할 방이 없습니다." : " ");
        list.revalidate();
        list.repaint();
    }

    private static Box relative(Box child, Box parent) {
        return new Box(child.x() - parent.x(), child.y() - parent.y(), child.width(), child.height());
    }

    private static void place(JComponent parent, JComponent child, Box box) {
        child.setBounds(box.x(), box.y(), box.width(), box.height());
        parent.add(child);
    }

    @Override
    public JComponent view() {
        return this;
    }

    /** @param data 대기실에서 나오다 생긴 안내(String). 없으면 null */
    @Override
    public void enter(Object data) {
        showing = true;
        nickname.setText(String.join(" · ", session.nicknames()) + " 님");
        reload();
        if (data instanceof String notice && !notice.isBlank()) {
            message.setText(notice);
        }
    }

    @Override
    public void exit() {
        showing = false;
    }
}
