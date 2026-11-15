package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.ChatField;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.hud.InGameHud;
import hansung.arcade.ingame.input.KeyBindings;
import hansung.arcade.ingame.input.LocalInput;
import hansung.arcade.ingame.render.AirDropRenderer;
import hansung.arcade.ingame.render.AirplaneSprites;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.WorldRenderer;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Component;
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
 * <p>이동·물풍선·아이템 키({@link KeyBindings})는 이 패널이 포커스를 가지고 있을 때 받아, 틱마다 플레이어별 명령으로
 * 바꿔 {@link GameWorld#update}에 넘긴다. 게임에 이 컴퓨터의 플레이어가 둘이면(한 키보드 2인 모드)
 * 1P는 W/A/S/D 쪽, 2P는 방향키 쪽 키로 따로 움직인다. 키 바인딩(InputMap)은 수정 키까지 일치해야 해서
 * Shift·Ctrl을 누른 채 방향키를 떼면 뗌 이벤트를 놓치므로, 키 코드만 보는 KeyListener를 쓴다.</p>
 *
 * <p>Enter를 누르면 맵 왼쪽 아래에 채팅 입력칸({@link ChatField})이 열리고 포커스가 그리로 간다.
 * 다시 Enter를 치면 채팅을 올리고, Esc를 치면 올리지 않고 닫는다. 입력칸이 열려 있어도 방향키는 캐릭터를 움직이고,
 * 나머지 키(글자, Space, Shift 등)는 글을 치는 데 쓴다.</p>
 */
public final class InGamePanel extends JPanel {

    static final double FIXED_STEP = 1.0 / 60;
    /** 창을 끌거나 멈췄다 돌아왔을 때 한꺼번에 몰아서 갱신하지 않도록 상한을 둔다. */
    private static final double MAX_FRAME_TIME = 0.25;
    /** 맵 프레임 테두리 두께. 충돌 박스보다 넓은 캐릭터 그림은 좌우로 이만큼 맵 영역 밖까지 보인다. */
    private static final int FRAME_EDGE = HudLayout.FRAME_INNER_EDGE + HudLayout.FRAME_OUTER_EDGE;

    private final GameWorld world;
    private final LocalInput input;
    private final WorldRenderer worldRenderer;
    private final AirDropRenderer airDropRenderer;
    private final InGameHud hud;
    private final Timer loopTimer;
    private final ChatField chatField;
    /**
     * 패널과 채팅 입력칸 양쪽에 붙는다. 받은 키는 소비해서, 입력칸에서는 방향키가 커서 대신 캐릭터를 움직인다.
     * 입력칸에서 누른 글자 키·Space·Shift 같은 키는 받지 않고 글자로 들어가게 둔다.
     */
    private final KeyAdapter playerKeys = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (input.keyPressed(e, e.getSource() == chatField)) {
                e.consume();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (input.keyReleased(e)) {
                e.consume();
            }
        }
    };
    /**
     * 패널과 채팅 입력칸 양쪽에 붙는다. Enter는 패널에서는 입력칸을 열고 입력칸에서는 친 글을 올리며,
     * Esc는 입력칸을 올리지 않고 닫는다. 쓴 키는 소비해서 다른 단축키로 이어지지 않게 한다.
     *
     * <p>Enter를 누르고 있으면 누름 이벤트가 되풀이해 오는데, 그대로 받으면 열기와 빈 글 올리기(닫기)가
     * 번갈아 일어나 입력칸이 깜빡인다. 그래서 뗄 때까지는 처음 한 번만 받는다.</p>
     */
    private final KeyAdapter chatKeys = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (!enterHeld) {
                    enterHeld = true;
                    if (e.getSource() == chatField) {
                        submitChat();
                    } else {
                        openChat();
                    }
                }
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getSource() == chatField) {
                closeChat();
                e.consume();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                enterHeld = false;
            }
        }
    };
    /**
     * 다른 창으로 포커스가 넘어가면 키 뗌 이벤트가 오지 않으므로 누른 키를 비운다.
     * 패널과 채팅 입력칸 사이를 오갈 때는 옮겨 간 쪽이 키 뗌을 받으므로 그대로 둔다.
     */
    private final FocusAdapter focusLossHandler = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            Component next = e.getOppositeComponent();
            if (next != InGamePanel.this && next != chatField) {
                input.clear();
                enterHeld = false;
            }
        }
    };

    private long lastTickNanos;
    private double accumulator;
    /** Enter를 누른 채인지. 누르고 있는 동안 되풀이되는 누름 이벤트를 거른다 */
    private boolean enterHeld;

    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters,
                       BalloonSprites balloons, ItemSprites items, AirplaneSprites airplanes) {
        this.world = world;
        this.input = new LocalInput(world.localPlayers().stream().map(Player::id).toList());
        this.worldRenderer = new WorldRenderer(tileset, characters, balloons, items);
        this.airDropRenderer = new AirDropRenderer(airplanes, items);
        this.hud = new InGameHud(skin, world.map(), tileset, items,
                KeyBindings.forLocalPlayers(world.localPlayers().size()).stream().map(KeyBindings::itemKeyName).toList());
        setPreferredSize(new Dimension(HudLayout.WIDTH, HudLayout.HEIGHT));
        // 바탕은 HUD 그림이 화면 전체를 덮으므로 따로 지우지 않는다.
        setOpaque(true);
        setFocusable(true);
        // 한글 입력 상태에서는 입력기가 W/A/S/D 같은 글자 키를 먼저 가져가 키 코드가 제대로 오지 않을 수 있다 (윈도우).
        // 글을 받지 않는 패널이라 입력기를 끈다. 채팅 입력칸은 자기 입력기를 그대로 쓴다
        enableInputMethods(false);
        addKeyListener(playerKeys);
        addKeyListener(chatKeys);
        addFocusListener(focusLossHandler);
        // 채팅 입력칸은 HudLayout.CHAT_BAR 자리에 고정이라 배치 관리자를 쓰지 않는다
        setLayout(null);
        chatField = new ChatField(skin.chatText());
        chatField.addKeyListener(playerKeys);
        chatField.addKeyListener(chatKeys);
        chatField.addFocusListener(focusLossHandler);
        add(chatField);
        loopTimer = new Timer(1000 / 60, e -> tick());
        loopTimer.setCoalesce(true);
    }

    /** 비행기 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters,
                       BalloonSprites balloons, ItemSprites items) {
        this(world, tileset, skin, characters, balloons, items, AirplaneSprites.load());
    }

    /** 아이템·비행기 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters,
                       BalloonSprites balloons) {
        this(world, tileset, skin, characters, balloons, ItemSprites.load());
    }

    /** 물풍선·아이템 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public InGamePanel(GameWorld world, Tileset tileset, HudSkin skin, CharacterSprites characters) {
        this(world, tileset, skin, characters, BalloonSprites.load());
    }

    /** 캐릭터·물풍선·아이템 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
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

    /** 테스트에서 입력칸에 글을 넣고 상태를 확인할 때 쓴다. */
    ChatField chatField() {
        return chatField;
    }

    /** 내 캐릭터(1P)가 지금 누르고 있는 이동 방향. 없으면 null. 테스트에서 키가 어디서 눌려도 전달되는지 확인할 때 쓴다. */
    Direction heldDirection() {
        return input.heldDirection(0);
    }

    /** 키 입력 상태. 테스트에서 틱이 돈 뒤 막 눌린 키가 남았는지 확인할 때 쓴다. */
    LocalInput input() {
        return input;
    }

    private void openChat() {
        if (!chatField.isVisible()) {
            // 닫힌 뒤에 뒤늦게 확정된 조합 글자가 남아 있을 수 있어 열 때도 비운다
            chatField.setText("");
            chatField.setVisible(true);
        }
        chatField.requestFocusInWindow();
    }

    private void submitChat() {
        world.sendChat(chatField.getText());
        closeChat();
    }

    private void closeChat() {
        // 포커스를 먼저 패널로 옮긴다. 숨긴 입력칸에 포커스가 남으면 방향키를 받을 곳이 없다
        requestFocusInWindow();
        chatField.setVisible(false);
        chatField.setText("");
    }

    private void tick() {
        long now = System.nanoTime();
        advance(Math.min((now - lastTickNanos) / 1e9, MAX_FRAME_TIME));
        lastTickNanos = now;
        repaint();
    }

    /**
     * 흐른 시간 seconds를 고정 간격 틱으로 나눠 게임을 진행한다. 한 프레임에 틱이 여러 번 돌 수도, 한 번도 안 돌 수도 있다.
     * 명령은 틱마다 새로 만들어서, 막 눌린 물풍선·아이템 키는 누른 뒤 첫 틱에 한 번만 들어가고 틱이 돌 때까지 남아 있다.
     */
    void advance(double seconds) {
        accumulator += seconds;
        while (accumulator >= FIXED_STEP) {
            world.update(FIXED_STEP, input.takeCommands());
            accumulator -= FIXED_STEP;
        }
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
        int originX = HudLayout.mapOriginX(world.map());
        int originY = HudLayout.mapOriginY(world.map());
        // HUD는 도트 느낌으로 그리지만, 아직 도형으로 그리는 캐릭터는 매끈하게 둔다 (스프라이트로 바꾸면 끈다).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        inMap(g, originX, originY, field.y(), 0, () -> worldRenderer.drawGround(g, world));
        hud.paintFieldCorners(g);
        // 위쪽은 창 끝까지 열어 두고 아래는 맵 영역으로 자른다. 좌우는 프레임 두께만큼 열어서,
        // 충돌 박스보다 넓은 캐릭터 그림이 맵 끝에서 잘리지 않고 프레임 위에 겹쳐 보이게 한다
        inMap(g, originX, originY, 0, FRAME_EDGE, () -> worldRenderer.drawObjects(g, world));
        // 비행기와 떨어지는 아이템은 모든 물체 위에 그리되, 맵 영역 밖(테두리와 그 바깥)은 자른다
        inMap(g, originX, originY, field.y(), 0, () -> airDropRenderer.draw(g, world));

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
