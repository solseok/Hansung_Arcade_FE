package hansung.arcade.ingame;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.chat.ChatField;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudPalette;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_BAR;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_LINE_PITCH;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_LOG_BOTTOM;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_TEXT_HEIGHT;
import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 창 없이 인게임 화면을 그림에 그려 맵과 HUD가 겹치는 순서를 확인한다.
 * fixture 테마의 집(H) 그림은 빨간 단색이고 칸보다 20px 높다.
 *
 * <p>채팅은 창이 없어 포커스가 움직이지 않으므로 키·포커스 리스너를 직접 불러 확인한다.
 * 글자 픽셀은 글꼴과 OS에 따라 조금씩 달라서, 세로 위치는 돋움체가 있으면 1px, 다른 글꼴로 대신한 환경에서는 2px
 * 여유를 두고 본다 (자리 자체의 수치는 HudLayoutTest가 정확히 확인한다).
 * 가로는 글자마다 칸 안에서 시작하는 자리가 달라 {@link #assertStartsAtBarLeft 몇 px 여유}를 둔다.</p>
 */
@ExtendWith(RunOnEdt.class)
class InGamePanelTest {

    private static final Color HOUSE = new Color(0xC82828);

    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;
    /** 채팅 목록에서 내가 친 줄의 색 */
    private static final int MINE = 0xDFDB86;
    /** 다른 플레이어의 번호. 맵에는 나(1번)만 있어서 이 플레이어의 말풍선은 뜨지 않는다 */
    private static final int OTHER = 2;
    /** 채팅 글자를 찾아볼 곳: 맵 영역의 아래쪽 절반. 캐릭터는 맨 윗줄에 있어 여기에 흰 픽셀은 채팅 글자뿐이다 */
    private static final Box CHAT_AREA = new Box(FIELD.x(), FIELD.y() + FIELD.height() / 2,
            FIELD.width(), FIELD.height() / 2);

    private static HudSkin skin;
    /** 글자 픽셀의 세로 위치에 두는 여유 */
    private static int slack;

    @BeforeAll
    static void setUp() {
        HudFonts fonts = HudFonts.load();
        skin = HudSkin.load(fonts);
        slack = fonts.missing().contains(HudFonts.DOTUMCHE) ? 2 : 1;
    }

    @Test
    void 맨_윗줄의_키_큰_그림은_맵_테두리_위에_겹쳐_그린다() {
        BufferedImage screen = render("H.............1");

        // 회색 테두리 띠(맵 영역 바로 위)와 프레임 위 빈 곳까지 집 그림이 올라온다
        assertColor(HOUSE, screen, FIELD.x() + 20, FIELD.y() - 2);
        assertColor(HOUSE, screen, FIELD.x() + 20, HudLayout.FRAME.y() - 2);
        // 집이 없는 옆 칸 위는 테두리 그대로다
        assertColor(HudPalette.FRAME_INNER, screen, FIELD.x() + 60, FIELD.y() - 2);
    }

    @Test
    void 맵_이름표는_솟아오른_그림에_가려지지_않는다() {
        BufferedImage screen = render("H.............1");
        int x = FIELD.x() + 20;
        int y = HudLayout.MAP_LABEL.bottom() - 4;

        assertNotEquals(HOUSE.getRGB() & 0xFFFFFF, screen.getRGB(x, y) & 0xFFFFFF);
    }

    @Test
    void Enter를_누르면_채팅_입력칸이_열리고_다시_Enter를_치면_채팅이_올라가며_닫힌다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        ChatField chat = panel.chatField();
        assertFalse(chat.isVisible());

        tap(panel, KeyEvent.VK_ENTER);
        assertTrue(chat.isVisible());

        chat.setText("안녕하세요");
        KeyEvent enter = tap(chat, KeyEvent.VK_ENTER);

        assertEquals(List.of(new ChatMessage(world.localPlayer().id(), "tester", "안녕하세요")),
                world.chatLog().messages());
        assertFalse(chat.isVisible());
        assertEquals("", chat.getText());
        // 쓴 키는 소비해서 다른 단축키로 이어지지 않는다
        assertTrue(enter.isConsumed());
    }

    @Test
    void 빈_채팅은_올리지_않고_입력칸만_닫는다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        tap(panel, KeyEvent.VK_ENTER);

        panel.chatField().setText("   ");
        tap(panel.chatField(), KeyEvent.VK_ENTER);

        assertTrue(world.chatLog().messages().isEmpty());
        assertFalse(panel.chatField().isVisible());
    }

    @Test
    void Enter를_누르고_있는_동안_되풀이되는_누름은_받지_않는다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        ChatField chat = panel.chatField();

        // 누른 채로 있으면 포커스가 옮겨 간 입력칸으로 누름 이벤트가 계속 온다
        press(panel, KeyEvent.VK_ENTER);
        press(chat, KeyEvent.VK_ENTER);
        press(chat, KeyEvent.VK_ENTER);
        assertTrue(chat.isVisible());

        // 떼고 나서 다시 눌러야 올라간다
        release(chat, KeyEvent.VK_ENTER);
        chat.setText("gg");
        press(chat, KeyEvent.VK_ENTER);
        press(panel, KeyEvent.VK_ENTER);
        assertFalse(chat.isVisible());
        assertEquals(1, world.chatLog().messages().size());
    }

    @Test
    void 다른_창으로_넘어가면_누르고_있던_Enter도_잊는다() {
        InGamePanel panel = panel(world("..............1"));
        press(panel, KeyEvent.VK_ENTER);
        assertTrue(panel.chatField().isVisible());

        // 뗌 이벤트를 받지 못한 채 창을 떠났다 돌아와도 다음 Enter가 먹는다
        loseFocus(panel.chatField(), null);
        press(panel.chatField(), KeyEvent.VK_ENTER);
        assertFalse(panel.chatField().isVisible());
    }

    @Test
    void Esc를_누르면_치던_글을_올리지_않고_닫는다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        tap(panel, KeyEvent.VK_ENTER);
        panel.chatField().setText("치다 만 글");

        press(panel.chatField(), KeyEvent.VK_ESCAPE);

        assertTrue(world.chatLog().messages().isEmpty());
        assertFalse(panel.chatField().isVisible());
        // 다시 열면 비어 있다
        tap(panel, KeyEvent.VK_ENTER);
        assertEquals("", panel.chatField().getText());
    }

    @Test
    void 입력칸에서도_방향키는_커서_대신_캐릭터를_움직인다() {
        InGamePanel panel = panel(world("..............1"));
        press(panel, KeyEvent.VK_ENTER);
        ChatField chat = panel.chatField();
        chat.setText("가나다");

        KeyEvent left = press(chat, KeyEvent.VK_LEFT);

        assertEquals(Direction.LEFT, panel.heldDirection());
        // 소비된 키는 JTextField의 커서 이동으로 이어지지 않는다
        assertTrue(left.isConsumed());

        release(chat, KeyEvent.VK_LEFT);
        assertNull(panel.heldDirection());
    }

    @Test
    void 패널과_입력칸_사이로_포커스가_옮겨_가면_누른_방향키를_유지하고_밖으로_나가면_비운다() {
        InGamePanel panel = panel(world("..............1"));
        ChatField chat = panel.chatField();

        press(panel, KeyEvent.VK_RIGHT);
        loseFocus(panel, chat);
        assertEquals(Direction.RIGHT, panel.heldDirection());
        loseFocus(chat, panel);
        assertEquals(Direction.RIGHT, panel.heldDirection());

        // 다른 창으로 넘어가면 넘어간 곳을 알 수 없다 (null)
        loseFocus(chat, null);
        assertNull(panel.heldDirection());
    }

    @Test
    void 올라온_채팅은_맵_아래에서_105px_위에_새_줄이_아래로_오게_쌓인다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        // 위아래로 가장 큰 글자들이라 글자 칸 높이를 꽉 채운다
        world.receiveChat(new ChatMessage(OTHER, "other", "뷁 [gjy] 먼저 온 긴 줄"));
        int bottomLineTop = CHAT_LOG_BOTTOM - CHAT_TEXT_HEIGHT;

        Box one = whiteBounds(paint(panel), CHAT_AREA);
        assertNear(bottomLineTop, one.y());
        assertNear(CHAT_LOG_BOTTOM, one.bottom());
        // 왼쪽 끝은 입력 바와 같다
        assertStartsAtBarLeft(one);

        world.receiveChat(new ChatMessage(OTHER, "other", "뷁"));
        BufferedImage screen = paint(panel);
        Box two = whiteBounds(screen, CHAT_AREA);
        assertNear(bottomLineTop - CHAT_LINE_PITCH, two.y());
        assertNear(CHAT_LOG_BOTTOM, two.bottom());
        // 짧은 새 줄이 아래, 긴 먼저 온 줄이 위
        Box upper = whiteBounds(screen, lineBox(bottomLineTop - CHAT_LINE_PITCH));
        Box lower = whiteBounds(screen, lineBox(bottomLineTop));
        assertTrue(lower.width() < upper.width(), "아래 %d, 위 %d".formatted(lower.width(), upper.width()));
    }

    @Test
    void 채팅_글자는_흰색에_검은_1px_테두리이고_배경은_맵이_그대로_보인다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);
        BufferedImage before = paint(panel);

        world.receiveChat(new ChatMessage(OTHER, "other", "가나다 abc"));
        press(panel, KeyEvent.VK_ENTER);
        panel.chatField().setText("한글 xyz");
        BufferedImage after = paint(panel);

        int text = 0;
        int untouched = 0;
        for (int y = CHAT_AREA.y() + 1; y < CHAT_AREA.bottom() - 1; y++) {
            for (int x = CHAT_AREA.x() + 1; x < CHAT_AREA.right() - 1; x++) {
                int rgb = after.getRGB(x, y) & 0xFFFFFF;
                if (rgb == WHITE) {
                    text++;
                    assertOutlined(after, x, y);
                } else if (rgb != BLACK) {
                    // 글자도 테두리도 아닌 곳은 채팅을 그리기 전과 같다
                    assertEquals(before.getRGB(x, y), after.getRGB(x, y), "(%d, %d)".formatted(x, y));
                    untouched++;
                }
            }
        }
        assertTrue(text > 100, "글자 픽셀 " + text);
        // 입력 바 자리도 글자가 없는 곳은 맵 바닥 그대로다
        assertEquals(before.getRGB(CHAT_BAR.right() - 3, CHAT_BAR.y() + 5), after.getRGB(CHAT_BAR.right() - 3, CHAT_BAR.y() + 5));
        assertTrue(untouched > CHAT_AREA.width() * CHAT_AREA.height() * 9 / 10);
    }

    @Test
    void 채팅_목록에서_남의_줄은_흰색이고_내가_친_줄은_색이_다르다() {
        GameWorld world = world("..............1");
        InGamePanel panel = panel(world);

        world.receiveChat(new ChatMessage(OTHER, "other", "welcome"));
        BufferedImage othersOnly = paint(panel);
        assertTrue(count(othersOnly, CHAT_AREA, WHITE) > 50);
        assertEquals(0, count(othersOnly, CHAT_AREA, MINE));

        world.sendChat("Hi");
        BufferedImage both = paint(panel);
        // 남의 줄은 글자 모양 그대로 한 줄 위로 올라가 흰색으로 남고, 내 줄에는 흰 픽셀이 없다.
        // 돋움체가 아닌 글꼴은 글자가 칸 위아래로 1~2px 나갈 수 있어서, 칸 안의 픽셀 수를 세지 않고 픽셀 자리를 그대로 견준다.
        for (int y = CHAT_AREA.y() + CHAT_LINE_PITCH; y < CHAT_AREA.bottom(); y++) {
            for (int x = CHAT_AREA.x(); x < CHAT_AREA.right(); x++) {
                assertEquals(isColor(othersOnly, x, y, WHITE), isColor(both, x, y - CHAT_LINE_PITCH, WHITE),
                        "(%d, %d)".formatted(x, y));
            }
        }
        assertEquals(count(othersOnly, CHAT_AREA, WHITE), count(both, CHAT_AREA, WHITE));
        // 내 줄은 새 줄 자리(남의 줄 아래)에 내 색으로 놓인다
        assertTrue(count(both, CHAT_AREA, MINE) > 50);
        Box mine = colorBounds(both, CHAT_AREA, MINE);
        int bottomLineTop = CHAT_LOG_BOTTOM - CHAT_TEXT_HEIGHT;
        assertTrue(mine.y() >= bottomLineTop - slack && mine.bottom() <= CHAT_LOG_BOTTOM + slack, "내 줄 " + mine);
        assertTrue(mine.y() > whiteBounds(both, CHAT_AREA).y(), "내 줄 " + mine);
    }

    @Test
    void 채팅을_치면_머리_위에_말풍선이_뜨고_시간이_지나면_내려간다() {
        // 맵 한가운데(8번째 칸, 7번째 줄)에 서 있다
        GameWorld world = worldWithSpawnAt(7, 6);
        InGamePanel panel = panel(world);
        int centerX = FIELD.x() + 7 * 40 + 20;
        int bubbleBottom = FIELD.y() + 6 * 40 + 20 - HudLayout.BUBBLE_ABOVE_PLAYER;
        // 한 줄짜리 말풍선 높이: 위 9 + 글자 칸 12 + 아래 9
        int bubbleTop = bubbleBottom - (9 + CHAT_TEXT_HEIGHT + 9);
        BufferedImage before = paint(panel);

        world.sendChat("Hi");
        BufferedImage after = paint(panel);

        // 위아래 흰 테두리 1px, 그 바깥은 그대로
        assertEquals(WHITE, after.getRGB(centerX, bubbleTop) & 0xFFFFFF);
        assertEquals(WHITE, after.getRGB(centerX, bubbleBottom - 1) & 0xFFFFFF);
        assertEquals(before.getRGB(centerX, bubbleTop - 1), after.getRGB(centerX, bubbleTop - 1));
        assertEquals(before.getRGB(centerX, bubbleBottom), after.getRGB(centerX, bubbleBottom));
        // 안쪽은 맵이 비치되 검은색 20%만큼 어두워진다
        assertDarkenedByFifth(before.getRGB(centerX, bubbleTop + 2), after.getRGB(centerX, bubbleTop + 2));

        world.update(hansung.arcade.ingame.chat.SpeechBubbles.VISIBLE_SECONDS, null);
        BufferedImage later = paint(panel);
        assertEquals(before.getRGB(centerX, bubbleTop), later.getRGB(centerX, bubbleTop));
        assertEquals(before.getRGB(centerX, bubbleTop + 2), later.getRGB(centerX, bubbleTop + 2));
    }

    @Test
    void 치고_있는_글은_채팅_바_자리에_그려지고_닫으면_사라진다() {
        InGamePanel panel = panel(world("..............1"));
        press(panel, KeyEvent.VK_ENTER);
        panel.chatField().setText("[가g|]{}");

        Box typed = whiteBounds(paint(panel), CHAT_AREA);
        // 맵 영역 아래에서 17px 위, 높이 12px
        assertNear(CHAT_BAR.y(), typed.y());
        assertNear(CHAT_BAR.bottom(), typed.bottom());
        assertStartsAtBarLeft(typed);
        assertTrue(typed.right() <= CHAT_BAR.right(), "오른쪽 끝 " + typed.right());

        press(panel.chatField(), KeyEvent.VK_ESCAPE);
        assertEquals(0, count(paint(panel), CHAT_AREA, WHITE));
    }

    @Test
    void 바보다_긴_글을_쳐도_글자와_커서가_채팅_바_밖으로_나가지_않는다() {
        InGamePanel panel = panel(world("..............1"));
        press(panel, KeyEvent.VK_ENTER);
        panel.chatField().setText("가나다라마바사아자차카타파하가나다라마바사아자차카타파하");

        Box typed = whiteBounds(paint(panel), CHAT_AREA);

        assertTrue(typed.x() >= CHAT_BAR.x() - 1 - slack, "왼쪽 끝 " + typed.x());
        assertTrue(typed.right() <= CHAT_BAR.right(), "오른쪽 끝 " + typed.right());
        // 바의 오른쪽 끝 가까이까지 채운다
        assertTrue(typed.right() > CHAT_BAR.right() - 16, "오른쪽 끝 " + typed.right());
    }

    /** 15 x 13칸 맵의 첫 줄만 firstRow로 하고 나머지는 빈 바닥 */
    private static BufferedImage render(String firstRow) {
        return paint(panel(world(firstRow)));
    }

    private static GameWorld world(String firstRow) {
        List<String> rows = new ArrayList<>();
        rows.add(firstRow);
        for (int i = 1; i < 13; i++) {
            rows.add(".".repeat(15));
        }
        TileMap map = MapLoader.parse("fixture/tall", rows);
        return new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
    }

    /** 빈 바닥뿐인 15 x 13칸 맵. 내 캐릭터는 col번째 칸, row번째 줄(0부터)에 선다 */
    private static GameWorld worldWithSpawnAt(int col, int row) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            rows.add(i == row ? ".".repeat(col) + "1" + ".".repeat(14 - col) : ".".repeat(15));
        }
        return new GameWorld(MapLoader.parse("fixture/open", rows), new PlayerProfile("tester", Mascot.KKUKKU));
    }

    private static InGamePanel panel(GameWorld world) {
        InGamePanel panel = new InGamePanel(world, Tileset.forTheme("fixture"), skin);
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        return panel;
    }

    private static BufferedImage paint(InGamePanel panel) {
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        panel.paint(g);
        g.dispose();
        return screen;
    }

    private static KeyEvent press(Component target, int keyCode) {
        KeyEvent event = new KeyEvent(target, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : target.getKeyListeners()) {
            listener.keyPressed(event);
        }
        return event;
    }

    /** 눌렀다 뗀다. 누른 이벤트를 돌려준다. */
    private static KeyEvent tap(Component target, int keyCode) {
        KeyEvent pressed = press(target, keyCode);
        release(target, keyCode);
        return pressed;
    }

    private static void release(Component target, int keyCode) {
        KeyEvent event = new KeyEvent(target, KeyEvent.KEY_RELEASED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : target.getKeyListeners()) {
            listener.keyReleased(event);
        }
    }

    /** from이 포커스를 잃고 to가 받는다. to가 null이면 다른 창으로 넘어간 것이다. */
    private static void loseFocus(Component from, Component to) {
        FocusEvent event = new FocusEvent(from, FocusEvent.FOCUS_LOST, false, to);
        for (FocusListener listener : from.getFocusListeners()) {
            listener.focusLost(event);
        }
    }

    /** 올라온 채팅 한 줄이 놓이는 자리 (칸 위쪽이 top). 폭은 맵 영역 전체다. */
    private static Box lineBox(int top) {
        return new Box(FIELD.x(), top, FIELD.width(), CHAT_TEXT_HEIGHT);
    }

    /** area 안의 흰 픽셀(채팅 글자와 커서)을 모두 담는 영역 */
    private static Box whiteBounds(BufferedImage screen, Box area) {
        return colorBounds(screen, area, WHITE);
    }

    /** area 안에서 색이 rgb인 픽셀을 모두 담는 영역 */
    private static Box colorBounds(BufferedImage screen, Box area, int rgb) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = area.y(); y < area.bottom(); y++) {
            for (int x = area.x(); x < area.right(); x++) {
                if (isColor(screen, x, y, rgb)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertTrue(maxX >= 0, "#%06X 픽셀이 없다: %s".formatted(rgb, area));
        return new Box(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isColor(BufferedImage screen, int x, int y, int rgb) {
        return (screen.getRGB(x, y) & 0xFFFFFF) == rgb;
    }

    /** area 안에서 색이 rgb인 픽셀 수 */
    private static int count(BufferedImage screen, Box area, int rgb) {
        int count = 0;
        for (int y = area.y(); y < area.bottom(); y++) {
            for (int x = area.x(); x < area.right(); x++) {
                if ((screen.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    count++;
                }
            }
        }
        return count;
    }

    /** after가 before 위에 검은색을 20% 덮은 색인지 (색마다 80%, 반올림 오차 1) */
    private static void assertDarkenedByFifth(int before, int after) {
        for (int shift = 16; shift >= 0; shift -= 8) {
            int from = (before >> shift) & 0xFF;
            int to = (after >> shift) & 0xFF;
            assertTrue(Math.abs(to - from * 0.8) <= 1.5, "#%06X → #%06X".formatted(before & 0xFFFFFF, after & 0xFFFFFF));
        }
    }

    /** 흰 픽셀 둘레 8칸은 흰색(글자)이거나 검은색(테두리)이다 */
    private static void assertOutlined(BufferedImage screen, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int rgb = screen.getRGB(x + dx, y + dy) & 0xFFFFFF;
                assertTrue(rgb == WHITE || rgb == BLACK, "(%d, %d) 옆 (%d, %d) #%06X".formatted(x, y, x + dx, y + dy, rgb));
            }
        }
    }

    /** 글자 칸은 바 왼쪽 끝에서 시작한다. 글자 픽셀은 칸 안에서 조금 들어가 있거나 1px 삐져나올 수 있다 */
    private static void assertStartsAtBarLeft(Box text) {
        assertTrue(text.x() >= CHAT_BAR.x() - 1 - slack && text.x() <= CHAT_BAR.x() + 3 + slack,
                "왼쪽 끝 %d (바 %d)".formatted(text.x(), CHAT_BAR.x()));
    }

    private static void assertNear(int expected, int actual) {
        assertTrue(Math.abs(expected - actual) <= slack, "기대 %d (여유 %d), 실제 %d".formatted(expected, slack, actual));
    }

    private static void assertColor(Color expected, BufferedImage screen, int x, int y) {
        assertEquals(String.format("#%06X", expected.getRGB() & 0xFFFFFF),
                String.format("#%06X", screen.getRGB(x, y) & 0xFFFFFF), "(%d, %d)".formatted(x, y));
    }
}
