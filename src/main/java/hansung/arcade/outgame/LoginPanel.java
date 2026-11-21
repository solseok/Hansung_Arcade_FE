package hansung.arcade.outgame;

import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.ui.GlossButton;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.TextLabel;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * 로그인 (참고 로그인 화면 640x477 배치를 800x600으로 옮김: 가로 x1.25, 세로 x1.258).
 * 배경은 800x600 그림(resources/etc/hsu_arcade.png)을 늘이거나 줄이지 않고 창 전체에 깔고, 그 위 아래쪽에 올린다.
 * <pre>
 * ┌ 배경 그림 800x600 (마스코트) ──────────────────────────────────┐
 * │                                                              │
 * │        (1P)[ 주황 입력 영역: 탭 + 이름표 + 입력 칸 ](2P)        │
 * │                  [시작하기] [종   료]                         │
 * └──────────────────────────────────────────────────────────────┘
 * </pre>
 * 입력 영역에는 노란 "닉네임" 탭 아래 어두운 회색 칸이 있고, 그 안에 파란 이름표와 흰 입력 칸을 둔다.
 * 입력 영역은 닉네임 칸과 그 아래 오류 문구 한 줄만 담을 만큼만 높다. 닉네임 입력 칸은 가장 넓은 8자가 꽉 차는 폭이고,
 * 입력 영역은 그 칸 하나(2P면 둘)를 감쌀 만큼만 넓다. 규칙 안내 문구와 로고는 두지 않는다.
 * 1P면 칸 1개, 2P면 2개이고 2P면 입력 영역이 넓어지며 1P·2P 버튼이 바깥으로 간다.
 * 아이디·비밀번호·서버 선택은 없다 (서버 주소·포트는 설정 파일에서 읽는다).
 */
public final class LoginPanel extends JPanel implements Scene {

    /** 배경 그림: 창 전체 */
    private static final Box BACKGROUND = new Box(0, 0, 800, 600);
    private static final String BACKGROUND_RESOURCE = "/etc/hsu_arcade.png";
    /** 입력 영역 위치·높이: 닉네임 칸과 오류 한 줄만 담고 아래 버튼 줄 바로 위에 둔다. 폭은 닉네임 칸에 맞춘다 */
    private static final int FORM_Y = 448;
    private static final int FORM_HEIGHT = 100;
    private static final int CENTER_X = 400;
    /** 1P·2P 버튼 크기 (참고 화면 62x79)와 입력 영역과의 틈 */
    private static final int MODE_WIDTH = 77;
    private static final int MODE_HEIGHT = 99;
    private static final int MODE_GAP = 8;
    /** 버튼을 끼우는 회색 받침이 버튼 둘레로 나오는 폭 */
    private static final int SOCKET_MARGIN = 9;
    /** 입력 영역 아래 버튼 줄 (입력 영역과 8px 틈) */
    private static final int BUTTON_ROW_Y = 556;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 35;
    private static final int BUTTON_GAP = 10;
    // 입력 영역 기준
    private static final int SECTION_TOP = 8;
    private static final int SECTION_HEIGHT = 64;
    private static final int SECTION_SIDE = 14;
    private static final int SECTION_GAP = 8;
    /** 오류 문구 한 줄: 닉네임 칸 바로 아래 */
    private static final int MESSAGE_TOP = 74;
    private static final int MESSAGE_HEIGHT = 20;
    /** 닉네임 칸 안: 이름표·입력 칸 둘레 여백, 이름표 글자 좌우 여백, 입력 칸 커서 자리 */
    private static final int SECTION_PADDING = 6;
    private static final int LABEL_PADDING = 8;
    private static final int CARET_ROOM = 2;
    /** 탭 높이, 탭과 칸이 겹치는 높이, 탭 폭 */
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_OVERLAP = 2;
    private static final int TAB_WIDTH = 110;

    private final OutgameSession session;
    private final SceneManager scenes;
    private final JToggleButton one = modeButton("1P", "mode-1p", OutgamePalette.PLAYER_ONE_TOP,
            OutgamePalette.PLAYER_ONE_BOTTOM, OutgamePalette.PLAYER_ONE_TEXT_OUTLINE);
    private final JToggleButton two = modeButton("2P", "mode-2p", OutgamePalette.PLAYER_TWO_TOP,
            OutgamePalette.PLAYER_TWO_BOTTOM, OutgamePalette.PLAYER_TWO_TEXT_OUTLINE);
    private final NicknameSection first = new NicknameSection(1);
    private final NicknameSection second = new NicknameSection(2);
    private final TextLabel message = new TextLabel(" ", SwingConstants.CENTER);
    private final RoundPanel form = new RoundPanel(OutgamePalette.LOGIN_PANEL_TOP, 10)
            .gradient(OutgamePalette.LOGIN_PANEL_BOTTOM)
            .shine(OutgamePalette.LOGIN_TAB_TOP, 1)
            .edge(OutgamePalette.LOGIN_PANEL_EDGE, 2);
    private final GlossButton enter = new GlossButton("시작하기", GlossButton.GOLD);
    private final GlossButton exit = new GlossButton("종   료", GlossButton.BLUE);
    private final RoundPanel socketOne = socket("mode-socket-1p");
    private final RoundPanel socketTwo = socket("mode-socket-2p");

    public LoginPanel(OutgameSession session, SceneManager scenes) {
        super(null);
        this.session = session;
        this.scenes = scenes;
        setPreferredSize(new Dimension(800, 600));
        setBackground(OutgamePalette.LOGIN_BACKGROUND);
        setOpaque(true);

        add(one);
        add(two);

        form.setName("login-form");
        add(form);
        first.addTo(form);
        second.addTo(form);
        message.setName("login-message");
        message.setForeground(OutgamePalette.ERROR_TEXT);
        form.add(message);

        enter.setFont(OutgameFonts.label(16));
        // 시작하기는 로그인이 되는지에 따라 다른 소리를 낸다
        enter.setClickSound(null);
        enter.addActionListener(event -> login());
        add(enter);
        first.field.addActionListener(event -> enter.doClick());
        second.field.addActionListener(event -> enter.doClick());
        exit.setFont(OutgameFonts.label(16));
        exit.addActionListener(event -> {
            var window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
        add(exit);

        ButtonGroup modes = new ButtonGroup();
        modes.add(one);
        modes.add(two);
        one.setSelected(true);
        one.addActionListener(event -> showMode());
        two.addActionListener(event -> showMode());

        // 1P·2P 받침은 버튼과 입력 영역 뒤에, 배경 그림은 맨 뒤에 깐다 (나중에 넣은 것이 아래에 그려진다)
        add(socketOne);
        add(socketTwo);
        place(this, new LoginBackground(), BACKGROUND);
        showMode();
    }

    private void login() {
        try {
            if (two.isSelected()) {
                session.login(first.field.getText(), second.field.getText());
            } else {
                session.login(first.field.getText());
            }
        } catch (IllegalArgumentException exception) {
            message.setText(exception.getMessage());
            Sound.play("Bt_c_GameStartNotAble");
            return;
        }
        message.setText(" ");
        Sound.play("Bt_c_Success");
        scenes.show("lobby", null);
    }

    /**
     * 1P면 가운데 칸 하나, 2P면 왼쪽·오른쪽 두 칸. 2P면 입력 영역이 넓어지고 1P·2P 버튼과 받침이 바깥으로 간다.
     * 시작하기·종료는 입력 영역 가운데 아래 한 줄에 둔다.
     */
    private void showMode() {
        boolean pair = two.isSelected();
        int section = first.width();
        int width = 2 * SECTION_SIDE + (pair ? 2 * section + SECTION_GAP : section);
        Box box = new Box(CENTER_X - width / 2, FORM_Y, width, FORM_HEIGHT);
        form.setBounds(box.x(), box.y(), box.width(), box.height());
        message.setBounds(10, MESSAGE_TOP, box.width() - 20, MESSAGE_HEIGHT);
        first.setBounds(new Box(SECTION_SIDE, SECTION_TOP, section, SECTION_HEIGHT));
        if (pair) {
            second.setBounds(new Box(SECTION_SIDE + section + SECTION_GAP, SECTION_TOP, section, SECTION_HEIGHT));
        }
        second.setVisible(pair);

        int modeY = box.y() + (box.height() - MODE_HEIGHT) / 2;
        int oneX = box.x() - MODE_GAP - MODE_WIDTH;
        int twoX = box.right() + MODE_GAP;
        one.setBounds(oneX, modeY, MODE_WIDTH, MODE_HEIGHT);
        two.setBounds(twoX, modeY, MODE_WIDTH, MODE_HEIGHT);
        // 받침은 버튼을 감싸고 입력 영역 쪽으로 이어져 입력 영역 밑에 들어간다
        socketOne.setBounds(oneX - SOCKET_MARGIN, modeY - SOCKET_MARGIN,
                MODE_WIDTH + SOCKET_MARGIN + MODE_GAP + 6, MODE_HEIGHT + 2 * SOCKET_MARGIN);
        socketTwo.setBounds(twoX - MODE_GAP - 6, modeY - SOCKET_MARGIN,
                MODE_WIDTH + SOCKET_MARGIN + MODE_GAP + 6, MODE_HEIGHT + 2 * SOCKET_MARGIN);

        int center = box.x() + box.width() / 2;
        int rowLeft = center - (2 * BUTTON_WIDTH + BUTTON_GAP) / 2;
        enter.setBounds(rowLeft, BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        exit.setBounds(rowLeft + BUTTON_WIDTH + BUTTON_GAP, BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        message.setText(" ");
        revalidate();
        repaint();
    }

    private static RoundPanel socket(String name) {
        RoundPanel socket = new RoundPanel(OutgamePalette.LOGIN_SOCKET, 26).edge(OutgamePalette.LOGIN_SOCKET_EDGE, 2);
        socket.setName(name);
        return socket;
    }

    /** 1P·2P 유리 버튼: 위가 진하고 아래가 밝은 몸통, 위쪽 광택, 흰 테두리. 고르지 않은 쪽은 흐리다 */
    private static JToggleButton modeButton(String text, String name, Color top, Color bottom, Color textOutline) {
        JToggleButton button = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                int w = getWidth();
                int h = getHeight();
                boolean on = isSelected();
                Color from = on ? top : OutgamePalette.mix(top, Color.GRAY, 0.55);
                Color to = on ? bottom : OutgamePalette.mix(bottom, Color.GRAY, 0.55);
                BufferedImage body = Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, 18)),
                        Shapes.vertical(h, from, to), d -> {
                            int shine = (int) (h * 0.45);
                            d.setPaint(new GradientPaint(0, 0, new Color(1f, 1f, 1f, on ? 0.7f : 0.4f),
                                    0, shine, new Color(1f, 1f, 1f, 0.08f)));
                            d.fill(Shapes.round(5, 4, w - 10, shine, 13));
                            Shapes.edge(d, w, h, 18, OutgamePalette.LOGIN_RING, on ? 3.5f : 2f);
                            Shapes.edge(d, w, h, 18, OutgamePalette.LOGIN_SOCKET_EDGE, 1f);
                        });
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    Shapes.hints(g);
                    g.drawImage(body, 0, 0, null);
                    String text = getText();
                    if (text != null && !text.isEmpty()) {
                        Point shift = inkCenterShift(g, text, getFont(), w, h);
                        Shapes.text(g, text, getFont(), Color.WHITE, textOutline, 2, null,
                                shift.x, shift.y, w, h, CENTER);
                    }
                } finally {
                    g.dispose();
                }
            }
        };
        button.setName(name);
        button.setFont(OutgameFonts.label(30));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        return button;
    }

    /**
     * Shapes.text는 글꼴의 위·아래 여유(ascent·descent)로 세로 가운데를 잡아서 "1P"처럼 아래로 내려가는 획이 없는 글자는
     * 가운데보다 아래에 놓인다. 실제로 칠해지는 글자 모양이 w x h 한가운데에 오도록 옮길 만큼을 돌려준다.
     */
    private static Point inkCenterShift(Graphics2D g, String text, Font font, int w, int h) {
        TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
        Rectangle2D ink = layout.getBounds();
        double left = (w - layout.getAdvance()) / 2.0;
        double baseline = (h - (layout.getAscent() + layout.getDescent())) / 2.0 + layout.getAscent();
        return new Point((int) Math.round(w / 2.0 - (left + ink.getCenterX())),
                (int) Math.round(h / 2.0 - (baseline + ink.getCenterY())));
    }

    private static void place(JComponent parent, JComponent child, Box box) {
        child.setBounds(box.x(), box.y(), box.width(), box.height());
        parent.add(child);
    }

    /** 창 전체에 까는 배경 그림 (800x600, 늘이거나 줄이지 않는다). 그림이 없으면 로그인 바탕색만 칠한다 */
    private static final class LoginBackground extends JComponent {
        private static BufferedImage art;

        LoginBackground() {
            setName("login-background");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            BufferedImage image = art();
            if (image != null) {
                graphics.drawImage(image, 0, 0, null);
            } else {
                graphics.setColor(OutgamePalette.LOGIN_BACKGROUND);
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        private static BufferedImage art() {
            if (art == null) {
                try (var input = LoginPanel.class.getResourceAsStream(BACKGROUND_RESOURCE)) {
                    art = input == null ? null : javax.imageio.ImageIO.read(input);
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException("로그인 배경 그림을 읽지 못했습니다: " + BACKGROUND_RESOURCE, e);
                }
            }
            return art;
        }
    }

    /** 닉네임으로 받는 글자(완성 한글 가~힣, 영문, 숫자) 가운데 가장 넓은 글자 8개 폭 */
    private static int widestNickname(FontMetrics metrics) {
        int widest = 0;
        for (char c = '가'; c <= '힣'; c++) {
            widest = Math.max(widest, metrics.charWidth(c));
        }
        for (char c = '0'; c <= 'z'; c++) {
            if (Character.isLetterOrDigit(c)) {
                widest = Math.max(widest, metrics.charWidth(c));
            }
        }
        return widest * NicknameRule.MAX_LENGTH;
    }

    /**
     * 노란 탭 + 어두운 회색 칸(파란 이름표, 흰 입력 칸) 한 묶음.
     * 입력 칸은 가장 넓은 8자가 꽉 차는 폭, 이름표는 글자에 맞춘 폭이고 칸 폭은 둘을 더해 정한다.
     */
    private static final class NicknameSection {
        private final TextLabel tab = new TextLabel("닉네임", SwingConstants.CENTER);
        private final RoundPanel section = new RoundPanel(OutgamePalette.LOGIN_SECTION, 6)
                .edge(OutgamePalette.LOGIN_SECTION_EDGE, 2);
        private final PlayerLabel label;
        private final JTextField field = new JTextField();
        private final int fieldWidth;
        private final int labelWidth;

        NicknameSection(int player) {
            tab.setName("nickname-" + player + "-tab");
            tab.setFont(OutgameFonts.label(13));
            tab.setForeground(OutgamePalette.LOGIN_TAB_TEXT);
            tab.setOutline(OutgamePalette.LOGIN_TAB_TOP, 1);
            tab.setFill(OutgamePalette.LOGIN_TAB_TOP, 6);
            tab.setFillBottom(OutgamePalette.LOGIN_TAB_BOTTOM);
            tab.setEdge(OutgamePalette.LOGIN_TAB_EDGE, 1f);
            section.setName("nickname-" + player + "-section");
            label = new PlayerLabel(player + "P", " 닉네임");
            label.setName("nickname-" + player + "-label");
            field.setName("nickname-" + player);
            field.setFont(OutgameFonts.gulimChe(14));
            field.setForeground(OutgamePalette.LOBBY_TEXT);
            field.setBackground(Color.WHITE);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            section.add(label);
            section.add(field);
            Insets insets = field.getInsets();
            fieldWidth = widestNickname(field.getFontMetrics(field.getFont())) + CARET_ROOM + insets.left + insets.right;
            labelWidth = Shapes.metrics(label.getFont()).stringWidth(label.getText()) + 2 * LABEL_PADDING;
        }

        /** 이름표 + 입력 칸 + 둘레 여백 */
        int width() {
            return SECTION_PADDING + labelWidth + SECTION_PADDING + fieldWidth + SECTION_PADDING;
        }

        void addTo(JComponent form) {
            // 칸을 탭보다 먼저 넣어 칸이 탭 아래쪽 끝을 덮게 한다
            form.add(section);
            form.add(tab);
        }

        void setBounds(Box box) {
            tab.setBounds(box.x(), box.y(), TAB_WIDTH, TAB_HEIGHT);
            int top = box.y() + TAB_HEIGHT - TAB_OVERLAP;
            int height = box.height() - TAB_HEIGHT + TAB_OVERLAP;
            section.setBounds(box.x(), top, box.width(), height);
            int inner = height - 2 * (SECTION_PADDING + 1);
            label.setBounds(SECTION_PADDING, SECTION_PADDING + 1, labelWidth, inner);
            field.setBounds(SECTION_PADDING + labelWidth + SECTION_PADDING, SECTION_PADDING + 1,
                    box.width() - labelWidth - 3 * SECTION_PADDING, inner);
        }

        void setVisible(boolean visible) {
            tab.setVisible(visible);
            section.setVisible(visible);
        }
    }

    /** 파란 이름표. "1P"는 노랑, 나머지는 흰 글자 (참고 화면 "1P 아이디") */
    private static final class PlayerLabel extends TextLabel {
        private final String player;
        private final String rest;

        PlayerLabel(String player, String rest) {
            super(player + rest, SwingConstants.CENTER);
            this.player = player;
            this.rest = rest;
            setFont(OutgameFonts.label(12));
            setFill(OutgamePalette.LOGIN_LABEL_BOX, 4);
            setEdge(OutgamePalette.LOGIN_LABEL_EDGE, 1f);
            setGloss(0.3f);
        }

        @Override
        protected void paintText(Graphics2D g, int w, int h) {
            Font font = getFont();
            int playerWidth = Shapes.metrics(font).stringWidth(player);
            int total = Shapes.metrics(font).stringWidth(player + rest);
            int left = (w - total) / 2;
            Shapes.text(g, player, font, OutgamePalette.LOGIN_LABEL_PLAYER, OutgamePalette.LOGIN_LABEL_EDGE, 1,
                    null, left, 0, playerWidth, h, LEFT);
            Shapes.text(g, rest, font, Color.WHITE, OutgamePalette.LOGIN_LABEL_EDGE, 1,
                    null, left + playerWidth, 0, total - playerWidth, h, LEFT);
        }
    }

    @Override
    public void enter(Object data) {
        message.setText(" ");
        // 창이 막 뜬 때는 아직 포커스를 받기 전일 수 있어, 화면이 다 붙은 뒤에 닉네임 칸으로 커서를 옮긴다
        SwingUtilities.invokeLater(first.field::requestFocusInWindow);
    }

    @Override
    public JComponent view() { return this; }
}
