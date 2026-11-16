package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.lobby.LobbyArtwork;
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/** 참고 화면의 캐릭터 배경과 하단 로그인 패널을 프로젝트 마스코트로 구성한다. */
public final class LoginPanel extends JPanel implements Scene {
    private final CharacterSprites sprites = CharacterSprites.load();
    private final JTextField nickname = new JTextField(18);

    public LoginPanel(OutgameSession session, SceneManager scenes, Consumer<String> onLogin) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(800, 600));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JPanel hero = new JPanel(new BorderLayout());
        hero.setOpaque(false);
        JLabel title = LobbyArtwork.outlined("HANSUNG ARCADE", 42, new Color(0xD9FFFF), new Color(0x005080));
        title.setPreferredSize(new Dimension(438, 58));
        JPanel logo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        logo.setOpaque(false);
        logo.add(title);
        hero.add(logo, BorderLayout.SOUTH);
        add(hero, BorderLayout.CENTER);

        JPanel form = LobbyArtwork.panel(new BorderLayout(8, 8), new Color(0xFFE586), new Color(0xEB8D08));
        form.setPreferredSize(new Dimension(420, 228));
        form.setBorder(BorderFactory.createCompoundBorder(LobbyArtwork.frame(5),
                BorderFactory.createEmptyBorder(5, 16, 6, 16)));
        JLabel label = new JLabel("1 PLAYER  ·  오프라인 플레이", JLabel.CENTER);
        label.setForeground(new Color(0x784200));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        form.add(label, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(3, 1, 0, 6));
        fields.setOpaque(false);
        JTextField host = new JTextField(session.host());
        JTextField port = new JTextField(Integer.toString(session.port()));
        fields.add(field("닉네임", nickname));
        fields.add(field("서버 주소", host));
        fields.add(field("포트", port));
        form.add(fields, BorderLayout.CENTER);
        JLabel message = new JLabel("닉네임을 입력하고 입장하세요.", JLabel.CENTER);
        message.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        message.setForeground(new Color(0x653800));
        JButton enter = LobbyArtwork.button("로비 입장", false);
        enter.addActionListener(event -> {
            try {
                session.login(nickname.getText(), host.getText(), port.getText());
                onLogin.accept(session.nickname());
                scenes.show("lobby", null);
            } catch (IllegalArgumentException exception) {
                message.setText(exception.getMessage());
            }
        });
        for (JTextField input : new JTextField[]{nickname, host, port}) {
            input.addActionListener(event -> enter.doClick());
        }
        JButton exit = LobbyArtwork.button("종료", false);
        exit.addActionListener(event -> {
            var window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(370, 32));
        buttons.add(enter);
        buttons.add(exit);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(message, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.SOUTH);
        form.add(bottom, BorderLayout.SOUTH);
        JPanel dock = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        dock.setOpaque(false);
        dock.add(form);
        add(dock, BorderLayout.SOUTH);
    }

    private JPanel field(String text, JTextField input) {
        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setBackground(new Color(0x086AA9));
        row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 4));
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(76, 24));
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        label.setLabelFor(input);
        input.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        row.add(label, BorderLayout.WEST);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale(getWidth() / 800.0, getHeight() / 600.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x057BC4), 0, 600, new Color(0x50E8F4)));
        g.fillRect(0, 0, 800, 600);
        for (int ray = 0; ray < 20; ray++) {
            double a = ray * Math.PI / 10;
            g.setColor(new Color(140, 248, 255, 42));
            g.fillPolygon(new int[]{400, 400 + (int)(1000 * Math.cos(a)), 400 + (int)(1000 * Math.cos(a + .12))},
                    new int[]{175, 175 + (int)(1000 * Math.sin(a)), 175 + (int)(1000 * Math.sin(a + .12))}, 3);
        }
        g.setColor(new Color(231, 255, 255, 225));
        g.fillOval(260, 28, 280, 265);
        g.fillOval(175, 130, 230, 160);
        g.fillOval(400, 120, 225, 170);
        g.setColor(new Color(15, 130, 204, 100));
        g.fillOval(-160, 305, 1120, 700);
        for (int i = 0; i < 16; i++) {
            int x = (i * 137 + 19) % 790;
            int y = (i * 59 + 32) % 300;
            int size = 10 + i % 4 * 5;
            g.setColor(new Color(205, 255, 255, 100));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(255, 255, 255, 190));
            g.drawOval(x, y, size, size);
        }
        Mascot[] cast = {Mascot.KKUKKU, Mascot.KKOKKO, Mascot.SANGZZI, Mascot.HANSUNGNYANGI, Mascot.SANGSANGBUGI};
        int[] xs = {160, 255, 475, 575, 326};
        int[] ys = {175, 155, 155, 175, 65};
        for (int i = 0; i < cast.length; i++) {
            int h = i == 4 ? 222 : 110;
            var frame = sprites.frame(cast[i], Direction.DOWN, false, 0);
            int w = frame.getWidth() * h / frame.getHeight();
            g.drawImage(frame, xs[i], ys[i], w, h, null);
        }
        g.dispose();
    }

    @Override
    public void enter(Object data) { SwingUtilities.invokeLater(nickname::requestFocusInWindow); }

    @Override
    public JComponent view() { return this; }
}
