package hansung.arcade.outgame;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
 * </ul>
 */
@ExtendWith(RunOnEdt.class)
class ResultOverlayTest {

    /** 일부러 진 사람을 앞에 둔다. 모달은 승패·킬 순으로 줄을 세운다. */
    private static final GameResult RESULT = new GameResult(List.of(
            new GameResult.Standing(2, "꾸꾸", LOSE, 0, 2),
            new GameResult.Standing(1, "부기", WIN, 6, 0)));

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

    private BufferedImage render() {
        BufferedImage image = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        overlay.paint(g);
        g.dispose();
        return image;
    }
}
