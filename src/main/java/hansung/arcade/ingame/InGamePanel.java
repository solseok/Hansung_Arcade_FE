package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.hud.InGameHud;
import hansung.arcade.ingame.input.DirectionInput;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.WorldRenderer;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;

/**
 * 인게임 화면(800 x 600). 아웃게임에서는 이 패널을 프레임에 붙이고 {@link #start()}만 호출하면 된다.
 * 맵은 {@link HudLayout#FIELD} 자리에 그리고, 나머지 화면 장식은 {@link InGameHud}가 그린다.
 *
 * <p>게임 루프는 Swing Timer로 EDT에서 돌기 때문에 입력·갱신·그리기가 모두 같은 스레드에서 일어난다.
 * 갱신은 고정 간격(1/60초)으로 해서 프레임이 흔들려도 이동 속도가 일정하다.</p>
 *
 * <p>방향키는 이 패널이 포커스를 가지고 있을 때 받는다. 키 바인딩(InputMap)은 수정 키까지 일치해야 해서
 * Shift·Ctrl을 누른 채 방향키를 떼면 뗌 이벤트를 놓치므로, 키 코드만 보는 KeyListener를 쓴다.</p>
 */
public final class InGamePanel extends JPanel {

    private static final double FIXED_STEP = 1.0 / 60;
    /** 창을 끌거나 멈췄다 돌아왔을 때 한꺼번에 몰아서 갱신하지 않도록 상한을 둔다. */
    private static final double MAX_FRAME_TIME = 0.25;
    /** 맵 프레임 테두리 두께. 충돌 박스보다 넓은 캐릭터 그림은 좌우로 이만큼 맵 영역 밖까지 보인다. */
    private static final int FRAME_EDGE = HudLayout.FRAME_INNER_EDGE + HudLayout.FRAME_OUTER_EDGE;

    private final GameWorld world;
    private final DirectionInput input = new DirectionInput();
    private final WorldRenderer worldRenderer;
    private final InGameHud hud;
    private final Timer loopTimer;
    private final KeyAdapter directionKeys = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            Direction direction = DirectionInput.directionOf(e.getKeyCode());
            if (direction != null) {
                input.press(direction);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            Direction direction = DirectionInput.directionOf(e.getKeyCode());
            if (direction != null) {
                input.release(direction);
            }
        }
    };
    /** 다른 창이나 다른 컴포넌트로 포커스가 넘어가면 키 뗌 이벤트가 오지 않으므로 누른 키를 비운다. */
    private final FocusAdapter focusLossHandler = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            input.clear();
        }
    };

    private long lastTickNanos;
    private double accumulator;

    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters) {
        this.world = world;
        this.worldRenderer = new WorldRenderer(tileset, characters);
        this.hud = new InGameHud(skin, world.map(), tileset);
        setPreferredSize(new Dimension(HudLayout.WIDTH, HudLayout.HEIGHT));
        // 바탕은 HUD 그림이 화면 전체를 덮으므로 따로 지우지 않는다.
        setOpaque(true);
        setFocusable(true);
        addKeyListener(directionKeys);
        addFocusListener(focusLossHandler);
        loopTimer = new Timer(1000 / 60, e -> tick());
        loopTimer.setCoalesce(true);
    }

    /** 캐릭터 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin) {
        this(world, tileset, skin, CharacterSprites.load());
    }

    public void start() {
        lastTickNanos = System.nanoTime();
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

    @Override
    public void removeNotify() {
        stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            hud.paintBelowWorld(g);
            paintWorld(g);
            hud.paintAboveWorld(g, world);
        } finally {
            g.dispose();
        }
        // 리눅스에서 화면 갱신이 밀려 끊겨 보이는 현상 방지
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * 맵을 그린다. 바닥은 맵 영역 안에만 깔고 둥근 귀퉁이를 프레임색으로 다시 덮은 뒤,
     * 블록·캐릭터는 위쪽을 자르지 않고 그려서 맨 윗줄 그림이 칸 위로 솟은 부분이 프레임 테두리 위에 겹쳐 보이게 한다.
     * 맵이 15 x 13칸보다 작으면 영역 가운데에 둔다.
     */
    private void paintWorld(Graphics2D g) {
        Box field = HudLayout.FIELD;
        int originX = field.x() + (field.width() - world.map().pixelWidth()) / 2;
        int originY = field.y() + (field.height() - world.map().pixelHeight()) / 2;
        // HUD는 도트 느낌으로 그리지만, 아직 도형으로 그리는 캐릭터는 매끈하게 둔다 (스프라이트로 바꾸면 끈다).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        inMap(g, originX, originY, field.y(), 0, () -> worldRenderer.drawGround(g, world));
        hud.paintFieldCorners(g);
        // 위쪽은 창 끝까지 열어 두고 아래는 맵 영역으로 자른다. 좌우는 프레임 두께만큼 열어서,
        // 충돌 박스보다 넓은 캐릭터 그림이 맵 끝에서 잘리지 않고 프레임 위에 겹쳐 보이게 한다
        inMap(g, originX, originY, 0, FRAME_EDGE, () -> worldRenderer.drawObjects(g, world));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    /**
     * 원점을 맵 왼쪽 위로 옮기고, 맵 영역을 좌우로 sideMargin만큼 넓힌 범위의 좌우·아래와 clipTop 위쪽을 잘라
     * draw를 부른 뒤 되돌린다.
     */
    private static void inMap(Graphics2D g, int originX, int originY, int clipTop, int sideMargin, Runnable draw) {
        Box field = HudLayout.FIELD;
        AffineTransform transform = g.getTransform();
        Shape clip = g.getClip();
        g.clipRect(field.x() - sideMargin, clipTop, field.width() + 2 * sideMargin, field.bottom() - clipTop);
        g.translate(originX, originY);
        draw.run();
        g.setTransform(transform);
        g.setClip(clip);
    }
}

