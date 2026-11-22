package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.StartIntro;
import hansung.arcade.ingame.render.IntroSprites;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_TEXT_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 경기 시작 연출: READY 동안 검은 막, START 동안 START!! 글자 그림(흩어지며 사라짐),
 * START!! 글자가 뜬 뒤부터 조금 뒤까지 팀 색 화살표. 연출이 다 끝나면 아무것도 그리지 않는다.
 */
class IntroViewTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerProfile ONE = new PlayerProfile("one", Mascot.SANGSANGBUGI, 1, CharacterColor.RED);
    private static final PlayerProfile TWO = new PlayerProfile("two", Mascot.HANSUNGNYANGI, 2, CharacterColor.BLUE);
    /** 13줄(맵 영역 높이 그대로) 맵. 2P는 맨 윗줄, 1P는 START!! 글자와 겹치지 않는 아래쪽 줄에 선다 */
    private static final TileMap MAP = MapLoader.parse("fixture/intro-view", rows());
    private static final IntroSprites SPRITES = IntroSprites.shared();

    private final OutlineText text = new OutlineText(new Font(Font.MONOSPACED, Font.PLAIN, 12), CHAT_TEXT_HEIGHT,
            Color.WHITE, Color.BLACK);
    private final IntroView view = new IntroView(text, SPRITES);

    private static List<String> rows() {
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < 13; row++) {
            rows.add(row == 0 ? "......2" : row == 10 ? "...1..." : ".......");
        }
        return rows;
    }

    private static GameWorld duo() {
        return new GameWorld(MAP, List.of(ONE, TWO));
    }

    /** 연출을 켜고 seconds초 흘린다 */
    private static GameWorld introAt(GameWorld world, double seconds) {
        world.beginIntro();
        int ticks = (int) Math.round(seconds / TICK);
        for (int i = 0; i < ticks; i++) {
            world.update(TICK, Map.of());
        }
        return world;
    }

    @Test
    void 연출을_켜지_않았거나_화살표까지_사라지면_아무것도_그리지_않는다() {
        assertEquals(0, countDrawn(paint(duo()), full()));

        GameWorld done = introAt(duo(), StartIntro.TOTAL_SECONDS + StartIntro.ARROW_LINGER_SECONDS + 0.1);
        assertEquals(0, countDrawn(paint(done), full()));
    }

    @Test
    void 시작하면_화면_전체가_검고_START_글자가_뜬_뒤_검은_막이_걷힌다() {
        GameWorld world = duo();
        world.beginIntro();
        assertEquals(HudLayout.WIDTH * HudLayout.HEIGHT, countColor(blackout(world), full(), Color.BLACK));

        GameWorld ready = introAt(duo(), StartIntro.READY_SECONDS - 0.05);
        assertEquals(HudLayout.WIDTH * HudLayout.HEIGHT, countColor(blackout(ready), full(), Color.BLACK));

        GameWorld fading = introAt(duo(), StartIntro.READY_SECONDS + StartIntro.FADE_IN_SECONDS / 2);
        int alpha = blackout(fading).getRGB(10, 10) >>> 24;
        assertTrue(alpha > 100 && alpha < 155, "반쯤 걷힌 막의 불투명도 " + alpha);

        GameWorld bright = introAt(duo(), StartIntro.READY_SECONDS + StartIntro.FADE_IN_SECONDS + 0.05);
        assertEquals(0, countDrawn(blackout(bright), full()));
    }

    @Test
    void START_동안_START_글자_그림을_맵_가운데에_1배로_그린다() {
        GameWorld world = introAt(duo(), StartIntro.READY_SECONDS + StartIntro.FADE_IN_SECONDS + 0.1);
        BufferedImage screen = paint(world);
        Box line = view.startBox();

        for (IntroSprites.Glyph glyph : SPRITES.startGlyphs()) {
            assertSameAsImage(screen, glyph.image(), line.x() + glyph.x(), line.y(), false);
        }
        // 맵 영역 가로 가운데
        assertEquals(HudLayout.FIELD.x() + HudLayout.FIELD.width() / 2, line.x() + line.width() / 2, 1);
    }

    @Test
    void START_글자는_왼쪽부터_한_자씩_위로_튀어_올라_사라진다() {
        GameWorld world = introAt(duo(), StartIntro.READY_SECONDS + StartIntro.START_HOLD_SECONDS + 0.07);
        BufferedImage screen = paint(world);
        Box line = view.startBox();
        List<IntroSprites.Glyph> glyphs = SPRITES.startGlyphs();

        IntroSprites.Glyph first = glyphs.getFirst();
        IntroSprites.Glyph last = glyphs.getLast();
        assertTrue(matchingPixels(screen, first.image(), line.x() + first.x(), line.y()) < opaquePixels(first.image()) / 2,
                "첫 글자는 이미 튀어 오르고 있어야 합니다");
        // 위로 올라간다: 글자 줄 위쪽에 그려진 것이 있고, 아래쪽에는 없다
        assertTrue(countDrawn(screen, new Box(line.x() + first.x(), line.y() - 40, first.image().getWidth(), 40)) > 50);
        // (그 아래 40px쯤에는 1P 화살표의 글자가 있으므로 바로 아래 20px만 본다)
        assertEquals(0, countDrawn(screen, new Box(line.x(), line.bottom(), line.width(), 20)));
        assertSameAsImage(screen, last.image(), line.x() + last.x(), line.y(), false);

        // START 단계가 끝나면 글자는 하나도 남지 않는다 (화살표는 아직 남아 있지만 글자 줄과 겹치지 않는 곳이다)
        GameWorld done = introAt(duo(), StartIntro.TOTAL_SECONDS + 0.05);
        assertTrue(done.intro().showsArrow());
        assertEquals(0, countDrawn(paint(done), line));
    }

    @Test
    void 화살표는_머리_바로_위에_팀_색으로_그리고_맨_윗줄은_뒤집어_발_아래에_그린다() {
        // 화살표가 흔들리지 않고(흔들림 주기의 배수) 검은 막이 다 걷힌 때
        GameWorld world = introAt(duo(), 1.5);
        BufferedImage screen = paint(world);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);

        BufferedImage red = SPRITES.arrow(CharacterColor.RED);
        int firstX = HudLayout.mapOriginX(world.map()) + (int) Math.round(first.x());
        int firstY = HudLayout.mapOriginY(world.map()) + (int) Math.round(first.y());
        assertSameAsImage(screen, red, firstX - red.getWidth() / 2,
                firstY - IntroView.ARROW_TIP_ABOVE_PLAYER - red.getHeight(), false);

        BufferedImage blue = SPRITES.arrow(CharacterColor.BLUE);
        int secondX = HudLayout.mapOriginX(world.map()) + (int) Math.round(second.x());
        int secondY = HudLayout.mapOriginY(world.map()) + (int) Math.round(second.y());
        assertSameAsImage(screen, blue, secondX - blue.getWidth() / 2, secondY + IntroView.ARROW_TIP_BELOW_PLAYER, true);
    }

    @Test
    void 둘이면_화살표에_1P_2P를_적고_혼자면_적지_않는다() {
        GameWorld two = introAt(duo(), 1.5);
        Player first = two.localPlayers().get(0);
        BufferedImage arrow = SPRITES.arrow(CharacterColor.RED);
        int x = HudLayout.mapOriginX(two.map()) + (int) Math.round(first.x());
        int top = HudLayout.mapOriginY(two.map()) + (int) Math.round(first.y())
                - IntroView.ARROW_TIP_ABOVE_PLAYER - arrow.getHeight();
        Box label = new Box(x - 20, top - IntroView.LABEL_GAP - 20, 40, 20);
        assertTrue(countColor(paint(two), label, Color.WHITE) > 20, "1P 글자가 화살표 위에 없습니다");

        GameWorld solo = introAt(new GameWorld(MAP, ONE), 1.5);
        assertEquals(0, countDrawn(paint(solo), label));
    }

    @Test
    void 이어진_느낌표_둘은_함께_튀어_오른다() {
        int[] units = IntroView.flightUnits(SPRITES.startGlyphs());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 5), java.util.Arrays.stream(units).boxed().toList());
    }

    @Test
    void 화살표_표시는_1P부터_차례로_붙인다() {
        assertEquals("1P", IntroView.tagOf(0));
        assertEquals("2P", IntroView.tagOf(1));
    }

    private BufferedImage paint(GameWorld world) {
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        view.paint(g, world);
        view.paintBlackout(g, world);
        g.dispose();
        return screen;
    }

    private BufferedImage blackout(GameWorld world) {
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        view.paintBlackout(g, world);
        g.dispose();
        return screen;
    }

    private static Box full() {
        return new Box(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
    }

    /** image의 불투명한 픽셀이 screen의 (x, y)부터 그대로 찍혔는지. flipped면 위아래를 뒤집어 찍힌 것과 견준다 */
    private static void assertSameAsImage(BufferedImage screen, BufferedImage image, int x, int y, boolean flipped) {
        int compared = 0;
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                int source = image.getRGB(i, flipped ? image.getHeight() - 1 - j : j);
                if ((source >>> 24) != 0xFF) {
                    continue;
                }
                assertEquals(source, screen.getRGB(x + i, y + j), "(%d, %d)".formatted(i, j));
                compared++;
            }
        }
        assertTrue(compared > 100, "견준 픽셀 " + compared);
    }

    private static int matchingPixels(BufferedImage screen, BufferedImage image, int x, int y) {
        int matching = 0;
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                int source = image.getRGB(i, j);
                if ((source >>> 24) == 0xFF && screen.getRGB(x + i, y + j) == source) {
                    matching++;
                }
            }
        }
        return matching;
    }

    private static int opaquePixels(BufferedImage image) {
        int count = 0;
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                if ((image.getRGB(i, j) >>> 24) == 0xFF) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countDrawn(BufferedImage image, Box area) {
        int count = 0;
        for (int y = Math.max(0, area.y()); y < Math.min(image.getHeight(), area.bottom()); y++) {
            for (int x = Math.max(0, area.x()); x < Math.min(image.getWidth(), area.right()); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countColor(BufferedImage image, Box area, Color color) {
        int count = 0;
        for (int y = Math.max(0, area.y()); y < Math.min(image.getHeight(), area.bottom()); y++) {
            for (int x = Math.max(0, area.x()); x < Math.min(image.getWidth(), area.right()); x++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    count++;
                }
            }
        }
        return count;
    }
}
