package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.ui.OutgameFonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import static hansung.arcade.outgame.GameResult.Outcome.LOSE;
import static hansung.arcade.outgame.GameResult.Outcome.WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 11. 게임 결과는 따로 화면을 넘기지 않고 게임 화면 위에 반투명 모달로 띄운다.
 * <ul>
 *   <li>머리줄은 | NAME | KILL | SAVE | 만, 줄마다 [ 순위 ] 승패 이름 | 킬 | 세이브 | (캐릭터·EXP·LUCCI·플레이 시간 없음)</li>
 *   <li>마지막 플레이어가 죽고 1.5초 뒤 맵 프레임 아래에서 모달이 올라온다</li>
 *   <li>8.5초에 화면이 완전히 검어질 때까지 어두워지고, 그다음 원래 대기방으로 돌아간다</li>
 *   <li>모달은 인원과 상관없이 가장 많은 8명이 들어가는 고정 크기다</li>
 *   <li>이 컴퓨터의 플레이어(2P면 둘 다) 줄은 노란 글자로 강조하고, 이름은 굵은 굴림체로 쓴다</li>
 *   <li>글자는 미리 그린 1배 그림을 늘리지 않고 화면 배율(레티나 2배)대로 바로 그려 깨지지 않는다</li>
 * </ul>
 */
@ExtendWith(RunOnEdt.class)
class ResultOverlayTest {

    /** 일부러 진 사람을 앞에 둔다. 모달은 승패·킬 순으로 줄을 세운다. */
    private static final GameResult RESULT = new GameResult(List.of(
            new GameResult.Standing(2, "꾸꾸", LOSE, 0, 2),
            new GameResult.Standing(1, "부기", WIN, 6, 0)));

    /** 다 올라온 때 */
    private static final long SHOWN_MS = ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS;
    private static final int NAME = 2;
    private static final int KILL = 3;
    private static final int SAVE = 4;

    private ResultOverlay overlay;
    private final AtomicInteger finished = new AtomicInteger();

    @BeforeEach
    void setUp() {
        overlay = new ResultOverlay();
        overlay.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        overlay.onFinished(finished::incrementAndGet);
        overlay.show(RESULT, 1);
    }

    @Test
    void 결과에_플레이_시간이_없고_킬_세이브_승패가_있다() {
        List<String> result = Arrays.stream(GameResult.class.getRecordComponents()).map(RecordComponent::getName).toList();
        List<String> standing = Arrays.stream(GameResult.Standing.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertFalse(result.contains("playSeconds"), result.toString());
        assertTrue(standing.containsAll(List.of("nickname", "outcome", "kills", "saves")), standing.toString());
    }

    @Test
    void 머리줄은_NAME_KILL_SAVE_뿐이다() {
        assertEquals(List.of("NAME", "KILL", "SAVE"), overlay.headers());
    }

    @Test
    void 줄은_순위_승패_이름_킬_세이브() {
        assertEquals(List.of(
                List.of("[ 1 ]", "WIN", "부기", "06", "00"),
                List.of("[ 2 ]", "LOSE", "꾸꾸", "00", "02")), overlay.rows());
    }

    @Test
    void 모달은_불투명도를_낮춘_바탕이다() {
        int alpha = ResultOverlay.MODAL_BACKGROUND.getAlpha();
        assertTrue(alpha >= 64 && alpha <= 200, "모달 바탕 알파 " + alpha);

        overlay.tick(ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS);
        BufferedImage image = render();
        Rectangle panel = overlay.panelBounds();
        int shown = image.getRGB(panel.x + 12, panel.y + panel.height - 4) >>> 24;
        assertTrue(Math.abs(shown - alpha) <= 1, "모달 바탕이 반투명하지 않습니다: " + shown);
    }

    @Test
    void 처음_1점5초는_아무것도_없다() {
        overlay.tick(0);
        assertFalse(overlay.panelVisible());
        overlay.tick(ResultOverlay.RISE_DELAY_MS - 1);
        assertFalse(overlay.panelVisible());
        assertEquals(1500, ResultOverlay.RISE_DELAY_MS);
    }

    @Test
    void 일점오초부터_맵_프레임_아래에서_위로_올라온다() {
        Box field = HudLayout.FIELD;

        overlay.tick(ResultOverlay.RISE_DELAY_MS);
        assertTrue(overlay.panelVisible());
        assertEquals(field.bottom(), overlay.panelBounds().y, "맵 프레임 안쪽 아래 끝에서 시작해야 합니다.");

        int previous = Integer.MAX_VALUE;
        for (long t = ResultOverlay.RISE_DELAY_MS; t <= ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS; t += 50) {
            overlay.tick(t);
            int y = overlay.panelBounds().y;
            assertTrue(y <= previous, t + "ms 에 다시 내려갔습니다: " + y);
            previous = y;
        }
        Rectangle done = overlay.panelBounds();
        assertTrue(new Rectangle(field.x(), field.y(), field.width(), field.height()).contains(done),
                "다 올라온 모달은 맵 안에 있어야 합니다: " + done);
        assertTrue(done.y < field.bottom() - done.height / 2);
    }

    @Test
    void 올라오는_동안_맵_프레임_아래로는_그려지지_않는다() {
        overlay.tick(ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS / 3);
        BufferedImage image = render();
        Box field = HudLayout.FIELD;

        for (int y = field.bottom(); y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, image.getRGB(x, y) >>> 24, "(" + x + "," + y + ") 맵 프레임 아래에 모달이 보입니다.");
            }
        }
        Rectangle panel = overlay.panelBounds();
        assertTrue(panel.y < field.bottom());
        assertTrue((image.getRGB(panel.x + 12, panel.y + 2) >>> 24) > 0, "올라온 부분이 보이지 않습니다.");
    }

    @Test
    void 팔점오초에_어두워지기_시작해_완전히_검어지면_대기방으로_돌아간다() {
        assertEquals(8500, ResultOverlay.FADE_START_MS);

        overlay.tick(ResultOverlay.FADE_START_MS - 1);
        assertEquals(0, overlay.fadeAlpha());
        overlay.tick(ResultOverlay.FADE_START_MS);
        assertEquals(0, overlay.fadeAlpha());
        overlay.tick(ResultOverlay.FADE_START_MS + ResultOverlay.FADE_MS / 2);
        assertTrue(overlay.fadeAlpha() > 0 && overlay.fadeAlpha() < 255, "중간 " + overlay.fadeAlpha());
        assertEquals(0, finished.get());

        overlay.tick(ResultOverlay.END_MS);
        assertEquals(255, overlay.fadeAlpha());
        assertEquals(1, finished.get());
        BufferedImage black = render();
        for (int y = 0; y < black.getHeight(); y += 7) {
            for (int x = 0; x < black.getWidth(); x += 7) {
                assertEquals(0xFF000000, black.getRGB(x, y), "(" + x + "," + y + ") 완전히 검지 않습니다.");
            }
        }

        overlay.tick(ResultOverlay.END_MS + 1000);
        assertEquals(1, finished.get(), "대기방으로 한 번만 돌아가야 합니다.");
    }

    @Test
    void 모달은_인원과_상관없이_8명이_들어가는_고정_크기다() {
        int most = GameMaps.RANDOM.maxPlayers();
        overlay.tick(SHOWN_MS);
        Dimension size = overlay.panelBounds().getSize();

        for (int count : new int[]{1, 4, most}) {
            overlay.show(players(count), 1);
            overlay.tick(SHOWN_MS);
            assertEquals(size, overlay.panelBounds().getSize(), count + "명일 때 모달 크기가 바뀌었습니다.");
        }
        Rectangle panel = overlay.panelBounds();
        for (int row = 0; row < most; row++) {
            for (int column = 0; column <= SAVE; column++) {
                Rectangle cell = overlay.cellBounds(row, column);
                assertTrue(panel.contains(cell), (row + 1) + "번째 줄 " + column + "번 칸이 모달 밖입니다: " + cell);
            }
        }
    }

    @Test
    void 내_플레이어_줄은_노란_글자로_강조한다() {
        overlay.tick(SHOWN_MS);
        BufferedImage image = render();

        // 1등 줄이 내 플레이어(부기, 1번), 2등 줄은 다른 사람(꾸꾸)
        for (int column : new int[]{NAME, KILL, SAVE}) {
            assertTrue(count(image, overlay.cellBounds(0, column), ResultOverlayTest::yellow) >= 8,
                    "내 줄 " + overlay.headers().get(column - NAME) + " 글자가 노랗지 않습니다.");
            assertEquals(0, count(image, overlay.cellBounds(1, column), ResultOverlayTest::yellow),
                    "다른 사람 줄 " + overlay.headers().get(column - NAME) + " 글자가 노랗습니다.");
            assertTrue(count(image, overlay.cellBounds(1, column), ResultOverlayTest::white) >= 8,
                    "다른 사람 줄 " + overlay.headers().get(column - NAME) + " 글자가 흰색이 아닙니다.");
        }
    }

    @Test
    void 한_컴퓨터에서_둘이_하면_두_사람_줄을_모두_강조한다() {
        overlay.show(RESULT, Set.of(1, 2));
        overlay.tick(SHOWN_MS);
        BufferedImage image = render();

        for (int row = 0; row < 2; row++) {
            assertTrue(count(image, overlay.cellBounds(row, NAME), ResultOverlayTest::yellow) >= 8,
                    (row + 1) + "등 줄 이름이 노랗지 않습니다.");
        }
    }

    @Test
    void 게임_화면은_이_컴퓨터의_플레이어를_모두_내_플레이어로_넘긴다() {
        GamePreviewScene game = new GamePreviewScene(new SceneManager(view -> { }),
                HudSkin.load(HudFonts.load()), CharacterSprites.load());
        game.enter(new GameStartInfo("camp/camp08", List.of(
                new GameStartInfo.PlayerInfo(1, "부기", Mascot.SANGSANGBUGI, "빨강"),
                new GameStartInfo.PlayerInfo(2, "꾸꾸", Mascot.KKUKKU, "빨강"),
                new GameStartInfo.PlayerInfo(3, "목업 3", Mascot.KKOKKO, "파랑")), 2));
        try {
            SwingTree.button(game.view(), "목업 경기 끝내기").doClick();
            assertEquals(Set.of(1, 2), game.resultOverlay().localPlayerIds());
        } finally {
            game.exit();
        }
    }

    @Test
    void 이름은_굵은_굴림체로_쓴다() {
        Font font = ResultOverlay.nameFont();

        assertEquals(OutgameFonts.gulimChe(14).getFamily(), font.getFamily());
        assertTrue(font.isBold(), "이름 글꼴이 굵지 않습니다: " + font);
        assertTrue(font.getSize2D() >= 14, "이름 글꼴이 작습니다: " + font);
    }

    @Test
    void 글자는_1배_그림을_늘리지_않고_화면_배율대로_그린다() {
        overlay.tick(SHOWN_MS);
        // 레티나처럼 2배로 그린다. 그림을 늘리면 가까운 픽셀을 그대로 써서 2x2 묶음이 한 색이 된다
        BufferedImage image = new BufferedImage(HudLayout.WIDTH * 2, HudLayout.HEIGHT * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.scale(2, 2);
        overlay.paint(g);
        g.dispose();

        Rectangle cell = overlay.cellBounds(0, NAME);
        int detailed = 0;
        for (int y = cell.y * 2; y < (cell.y + cell.height) * 2; y += 2) {
            for (int x = cell.x * 2; x < (cell.x + cell.width) * 2; x += 2) {
                int c = image.getRGB(x, y);
                if (c != image.getRGB(x + 1, y) || c != image.getRGB(x, y + 1) || c != image.getRGB(x + 1, y + 1)) {
                    detailed++;
                }
            }
        }
        assertTrue(detailed >= 20, "2배 화면에서 이름이 1배 그림을 늘린 것처럼 뭉개집니다: " + detailed);
    }

    private static GameResult players(int count) {
        List<GameResult.Standing> standings = new ArrayList<>();
        for (int id = 1; id <= count; id++) {
            standings.add(new GameResult.Standing(id, "플레이어" + id, id == 1 ? WIN : LOSE, count - id, 0));
        }
        return new GameResult(standings);
    }

    private static int count(BufferedImage image, Rectangle cell, IntPredicate test) {
        int found = 0;
        for (int y = cell.y; y < cell.y + cell.height; y++) {
            for (int x = cell.x; x < cell.x + cell.width; x++) {
                if (test.test(image.getRGB(x, y))) {
                    found++;
                }
            }
        }
        return found;
    }

    private static boolean yellow(int argb) {
        int a = argb >>> 24;
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        return a > 200 && r >= 220 && g >= 180 && b <= 120;
    }

    private static boolean white(int argb) {
        int a = argb >>> 24;
        return a > 200 && (argb >> 16 & 0xFF) >= 225 && (argb >> 8 & 0xFF) >= 225 && (argb & 0xFF) >= 225;
    }

    private BufferedImage render() {
        BufferedImage image = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        overlay.paint(g);
        g.dispose();
        return image;
    }
}
