package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.input.DirectionInput;
import hansung.arcade.ingame.render.HudRenderer;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.WorldRenderer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;

/**
 * 인게임 화면. 아웃게임에서는 이 패널을 프레임에 붙이고 {@link #start()}만 호출하면 된다.
 *
 * <p>게임 루프는 Swing Timer로 EDT에서 돌기 때문에 입력·갱신·그리기가 모두 같은 스레드에서 일어난다.
 * 갱신은 고정 간격(1/60초)으로 해서 프레임이 흔들려도 이동 속도가 일정하다.</p>
 */
public final class InGamePanel extends JPanel {

    private static final int MARGIN = 20;
    /** 제목 줄 높이. 맨 윗줄 블록 그림이 칸 위로 솟는 높이(캠프 미사일 28px)만큼 제목 아래에 여유를 둔다. */
    private static final int TOP_BAR = 64;
    private static final int HUD_WIDTH = 140;
    private static final double FIXED_STEP = 1.0 / 60;
    /** 창을 끌거나 멈췄다 돌아왔을 때 한꺼번에 몰아서 갱신하지 않도록 상한을 둔다. */
    private static final double MAX_FRAME_TIME = 0.25;

    private static final Color BACKGROUND = new Color(0x1B2836);
    private static final Color FIELD_BORDER = new Color(0x3E5871);
    private static final Color TITLE = new Color(0xE8EEF4);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final GameWorld world;
    private final DirectionInput input = new DirectionInput();
    private final WorldRenderer worldRenderer;
    private final HudRenderer hudRenderer = new HudRenderer();
    private final Timer loopTimer;
    private final WindowAdapter focusLossHandler = new WindowAdapter() {
        @Override
        public void windowLostFocus(WindowEvent e) {
            input.clear();
        }
    };

    private long lastTickNanos;
    private double accumulator;
    private int framesThisSecond;
    private long fpsWindowStart;
    private int fps;

    public InGamePanel(GameWorld world, Tileset tileset) {
        this.world = world;
        this.worldRenderer = new WorldRenderer(tileset);
        int width = MARGIN + world.map().pixelWidth() + MARGIN + HUD_WIDTH + MARGIN;
        int height = TOP_BAR + world.map().pixelHeight() + MARGIN * 2;
        setPreferredSize(new Dimension(width, height));
        setBackground(BACKGROUND);
        setFocusable(true);
        bindDirectionKeys();
        loopTimer = new Timer(1000 / 60, e -> tick());
        loopTimer.setCoalesce(true);
    }

    public void start() {
        lastTickNanos = System.nanoTime();
        fpsWindowStart = lastTickNanos;
        accumulator = 0;
        loopTimer.start();
        requestFocusInWindow();
    }

    public void stop() {
        loopTimer.stop();
        input.clear();
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += Math.min((now - lastTickNanos) / 1e9, MAX_FRAME_TIME);
        lastTickNanos = now;
        while (accumulator >= FIXED_STEP) {
            world.update(FIXED_STEP, input.current());
            accumulator -= FIXED_STEP;
        }
        repaint();
    }

    private void bindDirectionKeys() {
        bindDirection(KeyEvent.VK_UP, Direction.UP);
        bindDirection(KeyEvent.VK_DOWN, Direction.DOWN);
        bindDirection(KeyEvent.VK_LEFT, Direction.LEFT);
        bindDirection(KeyEvent.VK_RIGHT, Direction.RIGHT);
    }

    private void bindDirection(int keyCode, Direction direction) {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        String pressKey = "press-" + direction;
        String releaseKey = "release-" + direction;
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressKey);
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releaseKey);
        actionMap.put(pressKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.press(direction);
            }
        });
        actionMap.put(releaseKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.release(direction);
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.addWindowFocusListener(focusLossHandler);
        }
    }

    @Override
    public void removeNotify() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.removeWindowFocusListener(focusLossHandler);
        }
        stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setFont(TITLE_FONT);
            g.setColor(TITLE);
            g.drawString("HANSUNG ARCADE", MARGIN, 27);

            int fieldWidth = world.map().pixelWidth();
            int fieldHeight = world.map().pixelHeight();
            g.setColor(FIELD_BORDER);
            g.fillRoundRect(MARGIN - 4, TOP_BAR - 4, fieldWidth + 8, fieldHeight + 8, 10, 10);

            AffineTransform base = g.getTransform();
            g.translate(MARGIN, TOP_BAR);
            g.clipRect(0, -TOP_BAR, fieldWidth, fieldHeight + TOP_BAR);
            worldRenderer.draw(g, world);
            g.setTransform(base);
            g.setClip(null);

            int hudX = MARGIN + fieldWidth + MARGIN;
            hudRenderer.draw(g, world, hudX, TOP_BAR - 4, HUD_WIDTH, fieldHeight + 8, fps);
        } finally {
            g.dispose();
        }
        countFrame();
        // 리눅스에서 화면 갱신이 밀려 끊겨 보이는 현상 방지
        Toolkit.getDefaultToolkit().sync();
    }

    private void countFrame() {
        framesThisSecond++;
        long now = System.nanoTime();
        if (now - fpsWindowStart >= 1_000_000_000L) {
            fps = framesThisSecond;
            framesThisSecond = 0;
            fpsWindowStart = now;
        }
    }
}
