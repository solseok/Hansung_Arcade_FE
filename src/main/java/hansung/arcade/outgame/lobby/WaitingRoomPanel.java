package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.chat.ChatPanel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/** 로비 입장 결과를 표시하는 최소 대기실. */
public final class WaitingRoomPanel extends JPanel implements Scene {
    private final LobbyService service;
    private final JLabel title = new JLabel();
    private final JLabel details = new JLabel();
    private final ChatPanel chat = new ChatPanel(this::sendChat);

    public WaitingRoomPanel(LobbyService service, SceneManager scenes) {
        super(new BorderLayout(12, 12));
        this.service = service;
        setPreferredSize(new Dimension(800, 600));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24));
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.add(title, BorderLayout.CENTER);
        JButton leave = new JButton("로비로 나가기");
        leave.addActionListener(event -> scenes.show("lobby", null));
        header.add(leave, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
        add(chat, BorderLayout.SOUTH);
    }

    private void sendChat(String message) {
        chat.message("나", message);
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void enter(Object data) {
        Room room = (Room) data;
        title.setText("대기실 · " + room.number() + "번 방 · " + (service.isHost() ? "방장" : "참가자"));
        details.setText(room.title() + "  |  " + room.map() + "  |  " + room.players() + " / " + room.capacity() + "명");
        chat.clear();
        chat.systemMessage("방에 입장했습니다.");
    }

    @Override
    public void exit() {
        service.leave();
        chat.clear();
    }
}
