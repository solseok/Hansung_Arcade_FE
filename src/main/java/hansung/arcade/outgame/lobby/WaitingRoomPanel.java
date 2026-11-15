package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.chat.ChatPanel;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.render.CharacterSprites;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public final class WaitingRoomPanel extends JPanel implements Scene {
    private final OutgameSession session;
    private final JLabel title = new JLabel();
    private final JPanel slots = new JPanel(new GridLayout(2, 4, 8, 8));
    private final ChatPanel chat = new ChatPanel(this::sendChat);
    private final JComboBox<Mascot> character = new JComboBox<>(Mascot.values());
    private final JComboBox<String> team = new JComboBox<>(new String[]{"빨강", "파랑"});
    private final JComboBox<String> map = new JComboBox<>(new String[]{"캠프 08", "패트릿 14", "빌리지 10"});
    private final JButton ready = LobbyArtwork.button("준비", false);
    private final JButton start = LobbyArtwork.button("게임 시작", true);
    private final CharacterSprites sprites = CharacterSprites.load();
    private boolean updating;

    public WaitingRoomPanel(LobbyService service, SceneManager scenes) {
        this(new OutgameSession(service), scenes);
    }

    public WaitingRoomPanel(OutgameSession session, SceneManager scenes) {
        super(new BorderLayout(10, 10));
        this.session = session;
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(0x08BDEB));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.CENTER);
        JButton leave = LobbyArtwork.button("로비로 나가기", false);
        leave.addActionListener(event -> {
            session.leave();
            chat.clear();
            scenes.show("lobby", null);
        });
        header.add(leave, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(8, 8));
        middle.setOpaque(false);
        slots.setOpaque(false);
        middle.add(slots, BorderLayout.CENTER);
        JPanel controls = new JPanel(new java.awt.FlowLayout());
        controls.add(new JLabel("캐릭터"));
        controls.add(character);
        controls.add(new JLabel("팀"));
        controls.add(team);
        controls.add(new JLabel("맵"));
        controls.add(map);
        controls.add(ready);
        controls.add(start);
        middle.add(controls, BorderLayout.SOUTH);
        add(middle, BorderLayout.CENTER);
        chat.setPreferredSize(new Dimension(760, 160));
        add(chat, BorderLayout.SOUTH);
        character.addActionListener(event -> select());
        team.addActionListener(event -> select());
        map.addActionListener(event -> {
            if (!updating) {
                session.selectMap(new String[]{"camp/camp08", "patrit/patrit14", "village/village10"}[map.getSelectedIndex()]);
            }
        });
        ready.addActionListener(event -> {
            session.setReady(!session.ready());
            refresh();
        });
        start.addActionListener(event -> scenes.show("game", session.start()));
    }

    private void select() {
        if (!updating) {
            session.select((Mascot) character.getSelectedItem(), (String) team.getSelectedItem());
            refresh();
        }
    }

    private void sendChat(String message) {
        chat.message(session.nickname(), message);
    }

    private void refresh() {
        Room room = session.room();
        title.setText(room.number() + "번 방 · " + room.title() + "  " + room.players() + "/" + room.capacity());
        slots.removeAll();
        var players = session.players();
        for (int index = 0; index < 8; index++) {
            JPanel slot = new JPanel(new BorderLayout(4, 4));
            slot.setBackground(new Color(0xB4EDFF));
            slot.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x1277AE), 3),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            if (index < players.size()) {
                var player = players.get(index);
                boolean host = session.service.isHost() ? index == 0 : index == 1;
                slot.add(new JLabel(player.nickname() + (host ? " · 방장" : ""), JLabel.CENTER), BorderLayout.NORTH);
                var image = sprites.frame(player.character(), Direction.DOWN, false, 0);
                slot.add(new JLabel(new ImageIcon(image.getScaledInstance(56, 64, java.awt.Image.SCALE_SMOOTH))), BorderLayout.CENTER);
                String state = host ? "방장" : index == 0 && !session.ready() ? "대기중" : "준비 완료";
                slot.add(new JLabel(player.team() + " · " + state, JLabel.CENTER), BorderLayout.SOUTH);
            } else {
                slot.add(new JLabel(index < room.capacity() ? "빈 자리" : "닫힌 자리", JLabel.CENTER));
            }
            slots.add(slot);
        }
        ready.setEnabled(!session.service.isHost());
        ready.setText(session.ready() ? "준비 취소" : "준비");
        start.setEnabled(session.service.isHost());
        map.setEnabled(session.service.isHost());
        slots.revalidate();
        slots.repaint();
    }

    @Override
    public void enter(Object data) {
        if (data instanceof Room room) {
            session.enter(room);
            chat.clear();
            chat.systemMessage("다른 참가자는 준비된 목업입니다. 실제 서버 연결 전입니다.");
        } else {
            session.setReady(false);
        }
        updating = true;
        character.setSelectedItem(session.character());
        team.setSelectedItem(session.team());
        map.setSelectedIndex(java.util.List.of("camp/camp08", "patrit/patrit14", "village/village10").indexOf(session.mapId()));
        updating = false;
        refresh();
    }

    @Override
    public JComponent view() { return this; }
}
