package hansung.arcade.outgame;

import static hansung.arcade.outgame.ui.OutgamePalette.*;

import hansung.arcade.core.game.*;
import hansung.arcade.outgame.lobby.LobbyArtwork;
import hansung.arcade.outgame.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.*;

/** 닉네임으로 시작하는 로컬 1P/2P 로그인 화면. */
public final class LoginPanel extends JPanel implements Scene {
    private final JPanel form;
    private final JPanel[] sections = new JPanel[2];
    private final JLabel[] tabs = new JLabel[2];
    private final JTextField[] fields = {new JTextField(), new JTextField()};
    private final ModeButton one = new ModeButton("1P", new Color(0, 136, 244)),
            two = new ModeButton("2P", new Color(255, 132, 0));
    private final JLabel message = new JLabel("", JLabel.CENTER);
    private final JButton start = LobbyArtwork.button("시작하기", false),
            exit = LobbyArtwork.button("종   료", false);
    private final JComponent background;
    private boolean twoPlayers;

    public LoginPanel(OutgameSession session, SceneManager scenes) {
        this(session, scenes, n -> {});
    }

    public LoginPanel(OutgameSession session, SceneManager scenes, Consumer<String> onLogin) {
        super(null);
        setPreferredSize(new Dimension(800, 600));
        BufferedImage art;
        try (var in = getClass().getResourceAsStream("/etc/hsu_arcade.png")) {
            art = javax.imageio.ImageIO.read(in);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        background =
                new JComponent() {
                    protected void paintComponent(Graphics g) {
                        g.drawImage(art, 0, 0, getWidth(), getHeight(), null);
                    }
                };
        background.setName("login-background");
        form =
                new JPanel(null) {
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        Shapes.hints(g);
                        g.setPaint(
                                new GradientPaint(
                                        0, 0, LOGIN_PANEL_TOP, 0, getHeight(), LOGIN_PANEL_BOTTOM));
                        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g.setColor(LOGIN_PANEL_EDGE);
                        g.setStroke(new BasicStroke(3));
                        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                        g.dispose();
                    }
                };
        form.setOpaque(false);
        form.setName("login-form");
        add(form);
        for (int i = 0; i < 2; i++) {
            final int index = i;
            JTextField field = fields[i];
            field.setName("nickname-" + (i + 1));
            field.setFont(OutgameFonts.gulimChe(12));
            field.setBackground(Color.WHITE);
            JPanel section = new JPanel(null);
            sections[i] = section;
            section.setBackground(LOGIN_SECTION);
            section.setName("nickname-" + (i + 1) + "-section");
            TextLabel label = new TextLabel((i + 1) + "P 닉네임");
            label.setName("nickname-" + (i + 1) + "-label");
            label.setOpaque(true);
            label.setBackground(LOGIN_LABEL_BOX);
            label.setFont(OutgameFonts.label(12));
            label.setLabelFor(field);
            label.setBounds(7, 4, 76, 25);
            field.setBounds(87, 4, 102, 25);
            section.add(label);
            section.add(field);
            form.add(section);
            JLabel tab =
                    new TextLabel("닉네임") {
                        protected void paintComponent(Graphics graphics) {
                            Graphics2D g = (Graphics2D) graphics.create();
                            g.setPaint(
                                    new GradientPaint(
                                            0, 0, LOGIN_TAB_TOP, 0, getHeight(), LOGIN_TAB_BOTTOM));
                            g.fillRect(0, 0, getWidth(), getHeight());
                            g.dispose();
                            super.paintComponent(graphics);
                        }
                    };
            tabs[i] = tab;
            tab.setHorizontalAlignment(JLabel.CENTER);
            tab.setFont(OutgameFonts.label(12));
            tab.setForeground(LOGIN_TAB_TEXT);
            tab.setName("nickname-" + (i + 1) + "-tab");
            form.add(tab);
            field.addActionListener(e -> start.doClick());
        }
        message.setName("login-message");
        message.setFont(OutgameFonts.gulimChe(12));
        message.setForeground(new Color(130, 30, 0));
        form.add(message);
        one.setName("mode-1p");
        two.setName("mode-2p");
        ButtonGroup modes = new ButtonGroup();
        modes.add(one);
        modes.add(two);
        one.setSelected(true);
        add(one);
        add(two);
        one.addActionListener(e -> mode(false));
        two.addActionListener(e -> mode(true));
        start.addActionListener(
                e -> {
                    try {
                        if (twoPlayers) session.login(fields[0].getText(), fields[1].getText());
                        else session.login(fields[0].getText());
                        message.setText("");
                        onLogin.accept(session.nickname());
                        scenes.show("lobby", null);
                    } catch (IllegalArgumentException ex) {
                        message.setText(ex.getMessage());
                    }
                });
        exit.addActionListener(
                e -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                });
        add(start);
        add(exit);
        add(background);
        mode(false);
    }

    private void mode(boolean value) {
        twoPlayers = value;
        sections[1].setVisible(value);
        tabs[1].setVisible(value);
        fields[1].setVisible(value);
        doLayout();
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        int cx = getWidth() / 2, w = twoPlayers ? 420 : 220;
        form.setBounds(cx - w / 2, getHeight() - 152, w, 100);
        background.setBounds(0, 0, getWidth(), getHeight());
        for (int i = 0; i < 2; i++) {
            sections[i].setBounds(12 + i * 200, 33, 196, 40);
            tabs[i].setBounds(12 + i * 200, 10, 196, 23);
        }
        message.setBounds(4, 74, w - 8, 22);
        one.setBounds(cx - w / 2 - 77, getHeight() - 152, 77, 99);
        two.setBounds(cx + w / 2, getHeight() - 152, 77, 99);
        start.setBounds(cx - 125, getHeight() - 46, 122, 35);
        exit.setBounds(cx + 3, getHeight() - 46, 122, 35);
    }

    @Override
    public void enter(Object data) {
        SwingUtilities.invokeLater(fields[0]::requestFocusInWindow);
    }

    @Override
    public JComponent view() {
        return this;
    }

    private static final class ModeButton extends JToggleButton {
        private final Color color;

        ModeButton(String text, Color color) {
            super(text);
            this.color = color;
            setFont(OutgameFonts.label(22));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Shapes.hints(g);
            Color base = isSelected() ? color : new Color(135, 160, 175);
            g.setColor(new Color(45, 65, 75));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);
            g.setPaint(new GradientPaint(0, 5, Color.WHITE, 0, getHeight() / 2f, base));
            g.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);
            g.setFont(getFont());
            var fm = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(
                    getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
