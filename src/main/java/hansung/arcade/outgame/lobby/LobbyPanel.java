package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.*;
import hansung.arcade.outgame.*;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Supplier;

import javax.swing.*;

public final class LobbyPanel extends JPanel implements Scene {
    private final OutgameSession session;
    private final SceneManager scenes;
    private final EntryPrompt prompt;
    private final RoomCreator creator;
    private final RoomListPanel list = new RoomListPanel();
    private final ArrowButton prev = new ArrowButton(ArrowButton.Direction.LEFT),
            next = new ArrowButton(ArrowButton.Direction.RIGHT);
    private final JLabel feedback = new TextLabel(""), nickname = new TextLabel("");
    private int page;

    public LobbyPanel(LobbyService service, SceneManager scenes) {
        this(new OutgameSession(service), scenes);
    }

    public LobbyPanel(OutgameSession session, SceneManager scenes) {
        this(session, scenes, EntryPrompt.dialog());
    }

    public LobbyPanel(OutgameSession session, SceneManager scenes, EntryPrompt prompt) {
        this(session, scenes, prompt, RoomCreator.dialog());
    }

    public LobbyPanel(
            OutgameSession session, SceneManager scenes, EntryPrompt prompt, RoomCreator creator) {
        super(null);
        this.session = session;
        this.scenes = scenes;
        this.prompt = prompt;
        this.creator = creator;
        setPreferredSize(new Dimension(800, 600));
        setBackground(OutgamePalette.LOBBY_BACKGROUND);
        JButton create = LobbyArtwork.button("방만들기", true),
                quick = LobbyArtwork.button("바로시작", true);
        create.setName("create-room");
        quick.setName("quick-join");
        create.setBounds(25, 18, 155, 62);
        quick.setBounds(190, 18, 155, 62);
        add(create);
        add(quick);
        create.addActionListener(
                e ->
                        creator.create(this)
                                .ifPresent(
                                        r ->
                                                enterRoom(
                                                        () ->
                                                                session.create(
                                                                        r.title(), r.capacity()))));
        quick.addActionListener(e -> enterRoom(session::quickJoin));
        nickname.setName("lobby-nickname");
        nickname.setBounds(365, 28, 400, 32);
        add(nickname);
        list.setName("room-list");
        list.setBounds(16, 95, 768, 430);
        add(list);
        prev.setName("page-prev");
        next.setName("page-next");
        prev.addActionListener(
                e -> {
                    page--;
                    refresh();
                });
        next.addActionListener(
                e -> {
                    page++;
                    refresh();
                });
        feedback.setName("lobby-message");
        feedback.setBounds(24, 528, 750, 25);
        add(feedback);
        JButton menu = LobbyArtwork.button("메뉴", false), exit = LobbyArtwork.button("나가기", false);
        menu.setBounds(20, 563, 85, 27);
        exit.setBounds(695, 563, 85, 27);
        add(menu);
        add(exit);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem reload = new JMenuItem("방 목록 새로고침");
        reload.addActionListener(e -> refresh());
        popup.add(reload);
        JCheckBoxMenuItem mute = new JCheckBoxMenuItem("음소거", Sound.isMuted());
        mute.addActionListener(e -> Sound.setMuted(mute.isSelected()));
        popup.add(mute);
        menu.addActionListener(e -> popup.show(menu, 0, -popup.getPreferredSize().height));
        exit.addActionListener(
                e -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                });
        refresh();
    }

    public void setNickname(String name) {
        nickname.setText(name + " 님");
    }

    private void enterRoom(Supplier<Room> action) {
        try {
            Room room = action.get();
            feedback.setText("");
            scenes.show("waiting", room);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            feedback.setText(ex.getMessage());
            refresh();
        }
    }

    private void refresh() {
        var rooms = RoomOrder.sort(session.service.rooms());
        int pages = Math.max(1, (rooms.size() + 7) / 8);
        page = Math.max(0, Math.min(page, pages - 1));
        list.removeAll();
        for (int i = 0; i < 8; i++) {
            int index = page * 8 + i;
            Room room = index < rooms.size() ? rooms.get(index) : null;
            RoomCard card = room == null ? RoomCard.skeleton() : RoomCard.of(room);
            card.setName(room == null ? "room-skeleton-" + i : "room-card-" + room.number());
            card.setBounds(10 + i % 2 * 378, 9 + i / 2 * 97, 370, 90);
            if (room != null)
                card.addActionListener(
                        e -> {
                            if (room.status() == Room.Status.PLAYING) {
                                feedback.setText("게임 중인 방입니다.");
                                return;
                            }
                            if (room.players() + session.localPlayers() > room.capacity()) {
                                feedback.setText("방이 가득 찼습니다. 빈 자리가 부족합니다.");
                                return;
                            }
                            if (prompt.confirm(this, room))
                                enterRoom(() -> session.join(room.number()));
                        });
            list.add(card);
        }
        prev.setBounds(320, 402, 60, 24);
        next.setBounds(388, 402, 60, 24);
        list.add(prev);
        list.add(next);
        prev.setEnabled(page > 0);
        next.setEnabled(page < pages - 1);
        nickname.setText(String.join(" / ", session.nicknames()) + " 님");
        if (rooms.isEmpty()) feedback.setText("표시할 방이 없습니다.");
        list.revalidate();
        list.repaint();
    }

    @Override
    public void enter(Object data) {
        refresh();
    }

    @Override
    public JComponent view() {
        return this;
    }
}
