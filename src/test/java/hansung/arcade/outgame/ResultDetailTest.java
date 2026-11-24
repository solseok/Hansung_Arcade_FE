package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.render.CharacterSprites;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static hansung.arcade.outgame.GameResult.Outcome.LOSE;
import static hansung.arcade.outgame.GameResult.Outcome.WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 결과 모달에서 기존 테스트가 묶지 않던 모양과 동작.
 * 테스트만 보고 만든 최소 구현(모달을 게임 화면에 붙이지 않고 시계도 없음, 머리줄 글자·둥근 모서리가 없음,
 * 승패·킬이 같으면 받은 순서 그대로)을 잡는다.
 * 실제 시계가 도는지 보려고 일부 테스트는 EDT 밖에서 기다린다.
 */
class ResultDetailTest {

    private static final GameResult RESULT = new GameResult(List.of(
            new GameResult.Standing(2, "꾸꾸", LOSE, 0, 2),
            new GameResult.Standing(1, "부기", WIN, 6, 0)));
    private static final long SHOWN_MS = ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS;

    @Test
    void 목업_경기를_끝내면_모달이_게임_화면_위에_붙고_시계가_돌아_올라온다() throws Exception {
        AtomicReference<GamePreviewScene> game = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            GamePreviewScene scene = new GamePreviewScene(new SceneManager(view -> { }),
                    HudSkin.load(HudFonts.load()), CharacterSprites.load(),
                hansung.arcade.ingame.render.BalloonSprites.load(), hansung.arcade.ingame.render.ItemSprites.load(),
                hansung.arcade.ingame.render.AirplaneSprites.load());
            scene.enter(new GameStartInfo("camp/camp08", List.of(
                    new GameStartInfo.PlayerInfo(1, "부기", Mascot.SANGSANGBUGI, "빨강"),
                    new GameStartInfo.PlayerInfo(2, "꾸꾸", Mascot.KKUKKU, "파랑")), 1));
            SwingTree.button(scene.view(), "목업 경기 끝내기").doClick();
            game.set(scene);
        });
        try {
            Thread.sleep(ResultOverlay.RISE_DELAY_MS + 500);
            AtomicReference<String> problem = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                ResultOverlay overlay = game.get().resultOverlay();
                if (!SwingUtilities.isDescendingFrom(overlay, game.get().view())) {
                    problem.set("결과 모달이 게임 화면에 붙어 있지 않습니다.");
                } else if (!overlay.isVisible() || overlay.getWidth() < HudLayout.WIDTH || overlay.getHeight() < HudLayout.HEIGHT) {
                    problem.set("결과 모달이 게임 화면 전체를 덮지 않습니다: " + overlay.getBounds());
                } else if (!overlay.panelVisible()) {
                    problem.set("1.5초가 지났는데 모달이 올라오지 않았습니다 (시계가 돌지 않습니다).");
                }
            });
            assertEquals(null, problem.get(), problem.get());
        } finally {
            SwingUtilities.invokeAndWait(() -> game.get().exit());
        }
    }

    @Test
    void 머리줄_NAME_KILL_SAVE_글자를_그린다() throws Exception {
        onEdt(() -> {
            ResultOverlay overlay = shown();
            BufferedImage image = render(overlay);
            Rectangle panel = overlay.panelBounds();
            for (int column = 2; column <= 4; column++) {
                Rectangle row = overlay.cellBounds(0, column);
                Rectangle header = new Rectangle(row.x, panel.y, row.width, row.y - panel.y);
                int white = 0;
                for (int y = header.y; y < header.y + header.height; y++) {
                    for (int x = header.x; x < header.x + header.width; x++) {
                        int argb = image.getRGB(x, y);
                        if ((argb >>> 24) > 200 && (argb >> 16 & 0xFF) > 225 && (argb >> 8 & 0xFF) > 225 && (argb & 0xFF) > 225) {
                            white++;
                        }
                    }
                }
                assertTrue(white >= 8, overlay.headers().get(column - 2) + " 머리줄 글자가 없습니다.");
            }
        });
    }

    @Test
    void 모달_모서리는_둥글다() throws Exception {
        onEdt(() -> {
            ResultOverlay overlay = shown();
            BufferedImage image = render(overlay);
            Rectangle panel = overlay.panelBounds();

            assertEquals(0, image.getRGB(panel.x, panel.y) >>> 24, "왼쪽 위 모서리가 각져 있습니다.");
            assertEquals(0, image.getRGB(panel.x + panel.width - 1, panel.y + panel.height - 1) >>> 24,
                    "오른쪽 아래 모서리가 각져 있습니다.");
            assertTrue((image.getRGB(panel.x + 8, panel.y + panel.height / 2) >>> 24) > 0);
        });
    }

    @Test
    void 승패와_킬이_같으면_세이브_많은_순_그다음_플레이어_번호_순() throws Exception {
        GameResult result = new GameResult(List.of(
                new GameResult.Standing(3, "셋", LOSE, 1, 0),
                new GameResult.Standing(2, "둘", LOSE, 1, 2),
                new GameResult.Standing(4, "넷", LOSE, 1, 2),
                new GameResult.Standing(1, "하나", LOSE, 1, 0)));
        onEdt(() -> {
            ResultOverlay overlay = new ResultOverlay();
            overlay.show(result, 1);
            assertEquals(List.of("둘", "넷", "하나", "셋"), overlay.rows().stream().map(row -> row.get(2)).toList());
        });
    }

    private static ResultOverlay shown() {
        ResultOverlay overlay = new ResultOverlay();
        overlay.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        overlay.show(RESULT, 1);
        overlay.tick(SHOWN_MS);
        return overlay;
    }

    private static BufferedImage render(ResultOverlay overlay) {
        BufferedImage image = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        overlay.paint(g);
        g.dispose();
        return image;
    }

    private static void onEdt(Runnable test) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                test.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() instanceof AssertionError error) {
            throw error;
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
