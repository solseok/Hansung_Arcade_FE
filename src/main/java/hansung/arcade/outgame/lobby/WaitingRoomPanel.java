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
    private final JComboBox<String> team = new JComboBox<>(new String[]{"빨강", "노랑", "주황", "초록", "하늘", "파랑", "보라", "마젠타"});
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
        character.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Mascot mascot) {
                    setText(switch (mascot) {
                        case SANGSANGBUGI -> "상상부기";
                        case KKUKKU -> "꾸꾸";
                        case KKOKKO -> "꼬꼬";
                        case SANGZZI -> "상찌";
                        case HANSUNGNYANGI -> "한성냥이";
                    });
                }
                return this;
            }
        });
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(0x08BDEB));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0869AF));
        header.setBorder(LobbyArtwork.frame(5));
        header.add(title, BorderLayout.CENTER);
        JButton leave = LobbyArtwork.button("로비로 나가기", false);
        leave.addActionListener(event -> {
            session.leave();
            hansung.arcade.outgame.audio.Sound.play("Ntc_GameEnd");
            chat.clear();
            scenes.show("lobby", null);
        });
        header.add(leave, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(8, 8));
        middle.setOpaque(false);
        slots.setOpaque(false);
        middle.add(slots, BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.setBackground(new Color(0x078FCE));
        controls.setBorder(LobbyArtwork.frame(6));
        JPanel choices = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        choices.setOpaque(false);
        choices.add(new JLabel("캐릭터"));
        choices.add(character);
        choices.add(new JLabel("팀"));
        choices.add(team);
        choices.add(new JLabel("맵"));
        choices.add(map);
        for (JComboBox<?> choice : new JComboBox<?>[]{character, team, map}) {
            choice.setBackground(new Color(0xA6F0FF));
            choice.setForeground(new Color(0x004B7F));
            choice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(ready);
        buttons.add(start);
        buttons.setPreferredSize(new Dimension(280, 38));
        controls.add(choices, BorderLayout.NORTH);
        controls.add(buttons, BorderLayout.SOUTH);
        middle.add(controls, BorderLayout.SOUTH);
        add(middle, BorderLayout.CENTER);
        chat.setPreferredSize(new Dimension(760, 138));
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
            JPanel slot = LobbyArtwork.panel(new BorderLayout(4, 4), new Color(0xD6FAFF), new Color(0x52CCF0));
            slot.setBackground(new Color(0xB4EDFF));
            slot.setBorder(LobbyArtwork.frame(5));
            if (index < players.size()) {
                var player = players.get(index);
                boolean host = session.service.isHost() ? index == 0 : index == 1;
                JLabel name = new JLabel(player.nickname() + (host ? " · 방장" : ""), JLabel.CENTER);
                name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                name.setForeground(host ? new Color(0xFFE46B) : Color.WHITE);
                name.setOpaque(true);
                name.setBackground(new Color(0x0878BA));
                slot.add(name, BorderLayout.NORTH);
                var image = sprites.frame(player.character(), Direction.DOWN, false, 0);
                slot.add(new JLabel(new ImageIcon(image.getScaledInstance(56, 64, java.awt.Image.SCALE_SMOOTH))), BorderLayout.CENTER);
                String state = host ? "방장" : !session.isReady(player.id()) ? "대기중" : "준비 완료";
                slot.add(new JLabel(player.team() + " · " + state, JLabel.CENTER), BorderLayout.SOUTH);
            } else {
                slot.add(new JLabel(index < room.capacity() ? "빈 자리" : "닫힌 자리", JLabel.CENTER));
            }
            slots.add(slot);
        }
        ready.setEnabled(!session.service.isHost());
        ready.setText(session.ready() ? "준비 취소" : "준비");
        start.setEnabled(session.canStart());
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
            session.returnToWaiting();
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
