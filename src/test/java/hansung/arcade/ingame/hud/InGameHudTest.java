package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 창 없이 HUD를 그림에 그려 자리마다 디자인 색이 칠해졌는지, 층 순서가 맞는지 확인한다.
 */
class InGameHudTest {

    private static final Color ITEM_PICTURE = new Color(0xFF00AA);

    private static HudSkin skin;
    private static TileMap map;
    private static GameWorld world;
    private static InGameHud hud;

    @BeforeAll
    static void setUp() {
        skin = HudSkin.load(HudFonts.load());
        map = MapLoader.parse("fixture/test01", List.of("1BB..", "....."));
        world = new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
        hud = new InGameHud(skin, map, Tileset.forTheme("fixture"));
    }

    @Test
    void 바탕층에_각_상자가_디자인_색으로_칠해진다() {
        BufferedImage screen = render(false);

        assertColor(HudPalette.BACKGROUND, screen, 3, 300);
        // 프레임: 바깥 검은 띠 4px → 회색 띠 3px → 맵 영역
        assertColor(HudPalette.FRAME_OUTER, screen, FRAME.x(), 300);
        assertColor(HudPalette.FRAME_OUTER, screen, FRAME.x() + 3, 300);
        assertColor(HudPalette.FRAME_INNER, screen, FRAME.x() + 4, 300);
        assertColor(HudPalette.FRAME_INNER, screen, FIELD.x() - 1, 300);
        assertColor(HudPalette.FRAME_INNER, screen, 300, FIELD.bottom());

        assertColor(HudPalette.PLAYER_LIST, screen, PLAYER_LIST.x() + 3, PLAYER_LIST.y() + 100);
        assertColor(HudPalette.PLAYER_SLOT, screen, playerSlot(7).x() + 60, playerSlot(7).y() + 15);
        assertColor(HudPalette.ITEM_PANEL, screen, ITEM_PANEL.x() + 3, ITEM_PANEL.y() + 50);
        assertColor(HudPalette.ITEM_INNER, screen, ITEM_INNER.right() - 5, ITEM_INNER.y() + 30);
        assertColor(HudPalette.ITEM_VIEW, screen, ITEM_VIEW.x() + 30, ITEM_VIEW.y() + 25);
        assertColor(HudPalette.EXIT, screen, EXIT.x() + 2, EXIT.y() + 18);
        assertColor(HudPalette.EXIT_BUTTON_EDGE, screen, EXIT_BUTTON.x(), EXIT_BUTTON.y() + 15);
        assertColor(HudPalette.ITEM_BAR, screen, ITEM_BAR.x() + 3, ITEM_BAR.y() + 17);
        assertColor(HudPalette.ITEM_SLOT, screen, itemSlot(5).x() + 30, itemSlot(5).y() + 20);
    }

    @Test
    void 내_아이템_바는_맵_프레임_위에_겹쳐_그린다() {
        BufferedImage screen = render(false);
        int y = FRAME.bottom() - 2;

        assertColor(HudPalette.ITEM_BAR, screen, ITEM_BAR.x() + 20, y);
        assertColor(HudPalette.FRAME_OUTER, screen, ITEM_BAR.x() - 5, y);
    }

    @Test
    void ITEM_글씨는_상자_A_바로_위_3px에_아래를_붙여_그린다() {
        BufferedImage screen = render(false);
        int lastTextRow = -1;
        for (int y = ITEM_PANEL.y(); y < ITEM_INNER.y(); y++) {
            for (int x = ITEM_LABEL.x(); x < ITEM_LABEL.x() + 50; x++) {
                int rgb = screen.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) > 200 && ((rgb >> 8) & 0xFF) > 200 && (rgb & 0xFF) > 200) {
                    lastTextRow = y;
                }
            }
        }
        assertEquals(ITEM_INNER.y() - ITEM_LABEL_GAP - 1, lastTextRow);
    }

    @Test
    void 나가기_버튼은_위쪽_절반이_아래쪽보다_어둡다() {
        BufferedImage screen = render(false);
        // 글자가 없는 왼쪽 끝 세로줄에서 비교한다
        int x = EXIT_BUTTON.x() + 4;
        int upper = brightness(screen.getRGB(x, EXIT_BUTTON.y() + EXIT_HIGHLIGHT + 4));
        int lower = brightness(screen.getRGB(x, EXIT_BUTTON.bottom() - 5));
        assertTrue(upper < lower, "위 %d, 아래 %d".formatted(upper, lower));
    }

    @Test
    void 나가기_버튼은_두_색으로_나뉘지_않고_줄마다_조금씩_밝아진다() {
        BufferedImage screen = render(false);
        int x = EXIT_BUTTON.x() + 4;
        int from = EXIT_BUTTON.y() + 1 + EXIT_HIGHLIGHT + 1;
        int to = EXIT_BUTTON.bottom() - 4;
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        int previous = brightness(screen.getRGB(x, from));
        for (int y = from; y <= to; y++) {
            int rgb = screen.getRGB(x, y);
            colors.add(rgb);
            assertTrue(brightness(rgb) >= previous, "y " + y);
            previous = brightness(rgb);
        }
        assertTrue(colors.size() > 8, "색 단계 " + colors.size());
    }

    @Test
    void 단축키_번호는_아이템_그림보다_위에_그린다() {
        BufferedImage screen = render(true);
        Box badge = keyBadge(0);
        Box slot = itemSlot(0);

        assertColor(HudPalette.KEY_BADGE, screen, badge.x() + 1, badge.y() + 1);
        assertColor(ITEM_PICTURE, screen, slot.x() + 30, slot.y() + 20);
    }

    @Test
    void 맵_모서리는_바닥을_그린_뒤에도_둥글게_덮인다() {
        BufferedImage screen = render(true);
        // 바닥 대신 칠한 색이 영역 귀퉁이 픽셀에는 그대로 남지 않는다
        assertNotEquals(ITEM_PICTURE.getRGB(), screen.getRGB(FIELD.x(), FIELD.y()));
        assertColor(ITEM_PICTURE, screen, FIELD.x() + 5, FIELD.y() + 5);
    }

    @Test
    void 플레이어가_있는_칸에는_얼굴과_닉네임이_그려진다() {
        BufferedImage screen = render(false);
        Box filled = playerSlot(0);
        Box empty = playerSlot(1);
        assertTrue(countChanged(screen, filled, HudPalette.PLAYER_SLOT) > countChanged(screen, empty, HudPalette.PLAYER_SLOT) + 50);
    }

    @Test
    void 경기가_끝나기_전에는_결과_배너가_없다() {
        BufferedImage screen = render(false);

        assertEquals(0, count(screen, RESULT_BAND, HudPalette.RESULT_BAND_EDGE));
        assertEquals(0, count(screen, RESULT_BAND, HudPalette.WIN_BOTTOM));
    }

    @Test
    void 이기면_맵_가운데_띠_위에_WIN_색_글씨를_띄운다() {
        GameWorld won = new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
        playToEnd(won);
        BufferedImage screen = render(won);

        // 띠 위아래 끝의 밝은 선은 맵 영역 폭 전체에 걸친다
        assertColor(HudPalette.RESULT_BAND_EDGE, screen, FIELD.x(), RESULT_BAND.y());
        assertColor(HudPalette.RESULT_BAND_EDGE, screen, FIELD.right() - 1, RESULT_BAND.bottom() - 1);
        assertTrue(count(screen, RESULT_BAND, HudPalette.WIN_BOTTOM) > 100);
        assertTrue(count(screen, RESULT_BAND, HudPalette.WIN_OUTLINE) > 100);
        assertEquals(0, count(screen, RESULT_BAND, HudPalette.DRAW_BOTTOM));
        // 글씨는 띠 가운데에 놓인다
        Box text = bounds(screen, RESULT_BAND, HudPalette.WIN_OUTLINE);
        int middleX = text.x() + text.width() / 2;
        int middleY = text.y() + text.height() / 2;
        assertTrue(Math.abs(middleX - (RESULT_BAND.x() + RESULT_BAND.width() / 2)) <= RESULT_DOT, "가로 " + text);
        assertTrue(Math.abs(middleY - (RESULT_BAND.y() + RESULT_BAND.height() / 2)) <= RESULT_DOT, "세로 " + text);
        // 테두리를 뺀 글씨 높이: 맨 윗줄은 흰 줄, 맨 아랫줄은 가장 짙은 색이다. 글꼴 크기를 0.5 단위로 맞춰 1도트쯤 어긋날 수 있다
        int capHeight = bounds(screen, RESULT_BAND, HudPalette.WIN_BOTTOM).bottom()
                - bounds(screen, RESULT_BAND, HudPalette.RESULT_HIGHLIGHT).y();
        assertTrue(Math.abs(capHeight - RESULT_TEXT_HEIGHT) <= 2 * RESULT_DOT, "글씨 높이 " + capHeight);
    }

    @Test
    void 비기면_DRAW_색_글씨를_띄운다() {
        GameWorld drawn = new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
        drawn.localPlayer().eliminate();
        playToEnd(drawn);
        BufferedImage screen = render(drawn);

        assertTrue(count(screen, RESULT_BAND, HudPalette.DRAW_BOTTOM) > 100);
        assertEquals(0, count(screen, RESULT_BAND, HudPalette.WIN_BOTTOM));
        assertEquals(0, count(screen, RESULT_BAND, HudPalette.LOSE_BOTTOM));
    }

    /** 혼자 하는 판이라 시간이 다 될 때까지 갱신한다 */
    private static void playToEnd(GameWorld world) {
        for (int i = 0; i < MatchClock.DURATION_SECONDS * MatchClock.TICKS_PER_SECOND; i++) {
            world.update(1.0 / MatchClock.TICKS_PER_SECOND, null);
        }
        assertTrue(world.match().isOver());
    }

    private static BufferedImage render(GameWorld shown) {
        BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        hud.paintBelowWorld(g);
        hud.paintAboveWorld(g, shown);
        g.dispose();
        return screen;
    }

    private static int count(BufferedImage screen, Box box, Color color) {
        int count = 0;
        for (int y = box.y(); y < box.bottom(); y++) {
            for (int x = box.x(); x < box.right(); x++) {
                if ((screen.getRGB(x, y) & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** box 안에서 color 픽셀을 모두 담는 영역 */
    private static Box bounds(BufferedImage screen, Box box, Color color) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = box.y(); y < box.bottom(); y++) {
            for (int x = box.x(); x < box.right(); x++) {
                if ((screen.getRGB(x, y) & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertTrue(maxX >= 0, "색이 없다");
        return new Box(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** @param fakeWorld 맵과 아이템 자리에 맵·아이템 대신 단색을 칠할지 */
    private static BufferedImage render(boolean fakeWorld) {
        BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        hud.paintBelowWorld(g);
        if (fakeWorld) {
            g.setColor(ITEM_PICTURE);
            g.fillRect(FIELD.x(), FIELD.y(), FIELD.width(), FIELD.height());
            hud.paintFieldCorners(g);
            Box slot = itemSlot(0);
            g.fillRect(slot.x(), slot.y(), slot.width(), slot.height());
        }
        hud.paintAboveWorld(g, world);
        g.dispose();
        return screen;
    }

    private static int countChanged(BufferedImage screen, Box box, Color base) {
        int count = 0;
        for (int y = box.y() + 2; y < box.bottom() - 2; y++) {
            for (int x = box.x() + 2; x < box.right() - 2; x++) {
                if ((screen.getRGB(x, y) & 0xFFFFFF) != (base.getRGB() & 0xFFFFFF)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int brightness(int rgb) {
        return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
    }

    private static void assertColor(Color expected, BufferedImage screen, int x, int y) {
        assertEquals(String.format("#%06X", expected.getRGB() & 0xFFFFFF),
                String.format("#%06X", screen.getRGB(x, y) & 0xFFFFFF), "(%d, %d)".formatted(x, y));
    }
}
