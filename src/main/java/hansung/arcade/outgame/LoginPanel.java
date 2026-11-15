package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.outgame.lobby.LobbyArtwork;
import hansung.arcade.core.game.SceneManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public final class LoginPanel extends JPanel implements Scene {
    public LoginPanel(OutgameSession session, SceneManager scenes, Consumer<String> onLogin) {
        super(new java.awt.GridBagLayout());
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(0x00BCEB));
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setPreferredSize(new Dimension(440, 550));
        form.setBorder(BorderFactory.createEmptyBorder(26, 35, 26, 35));
        JLabel title = new JLabel("HANSUNG ARCADE");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        form.add(title);
        form.add(new JLabel("오프라인 목업 · 서버 설정은 향후 연결에 사용됩니다"));
        JTextField nickname = new JTextField(18);
        JTextField host = new JTextField(session.host());
        JTextField port = new JTextField(Integer.toString(session.port()));
        form.add(new JLabel("닉네임 (1~12자)"));
        form.add(nickname);
        form.add(new JLabel("서버 주소"));
        form.add(host);
        form.add(new JLabel("포트"));
        form.add(port);
        JLabel message = new JLabel(" ");
        JButton enter = LobbyArtwork.button("로비 입장", true);
        enter.addActionListener(event -> {
            try {
                session.login(nickname.getText(), host.getText(), port.getText());
                onLogin.accept(session.nickname());
                scenes.show("lobby", null);
            } catch (IllegalArgumentException exception) {
                message.setText(exception.getMessage());
            }
        });
        nickname.addActionListener(event -> enter.doClick());
        form.add(enter);
        JButton exit = LobbyArtwork.button("종료", false);
        exit.addActionListener(event -> {
            var window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
        form.add(exit);
        form.add(message);
        add(form);
    }

    @Override
    public JComponent view() { return this; }
}
