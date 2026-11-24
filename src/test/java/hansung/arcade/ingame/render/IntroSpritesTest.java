package hansung.arcade.ingame.render;

import hansung.arcade.core.ui.Oklch;
import hansung.arcade.ingame.entity.CharacterColor;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 시작 연출 그림: game_start.bmp의 START!! 줄을 글자마다 자르고, 화살표를 팀 색으로 칠한다 */
class IntroSpritesTest {

    private final IntroSprites sprites = IntroSprites.load();

    @Test
    void START_줄만_잘라_일곱_글자로_나눈다() {
        // game_start.bmp는 350 x 113이고 위 GAME, 아래 START!! 두 줄이다
        assertEquals(350, sprites.startWidth());
        assertEquals(57, sprites.startHeight());
        List<IntroSprites.Glyph> glyphs = sprites.startGlyphs();
        assertEquals(7, glyphs.size(), "S, T, A, R, T, !, ! 일곱 자");

        int right = -1;
        for (IntroSprites.Glyph glyph : glyphs) {
            assertTrue(glyph.x() > right, "글자가 겹칩니다: " + glyph.x());
            assertEquals(sprites.startHeight(), glyph.image().getHeight());
            right = glyph.x() + glyph.image().getWidth() - 1;
        }
        // 맞닿아 한 덩어리였던 S·T도 비슷한 폭으로 나뉜다
        int s = glyphs.get(0).image().getWidth();
        int t = glyphs.get(1).image().getWidth();
        assertTrue(Math.abs(s - t) < 15, "S %d, T %d".formatted(s, t));
    }

    @Test
    void 그림에서_뒤집혀_있던_느낌표를_바로_세운다() {
        List<IntroSprites.Glyph> glyphs = sprites.startGlyphs();
        for (int i = 0; i < glyphs.size(); i++) {
            assertEquals(i >= 5, glyphs.get(i).exclamation(), "글자 " + i);
        }
        for (IntroSprites.Glyph glyph : glyphs.subList(5, 7)) {
            BufferedImage image = glyph.image();
            // 점(작은 덩어리)이 아래, 막대(큰 덩어리)가 위: 위 절반의 잉크가 아래 절반보다 많다
            int half = image.getHeight() / 2;
            assertTrue(ink(image, 0, half) > ink(image, half, image.getHeight()),
                    "느낌표가 아직 뒤집혀 있습니다 (x=%d)".formatted(glyph.x()));
        }
        // S·T·A·R·T는 그대로다
        BufferedImage sheet = SpriteSheet.find(IntroSprites.START_PATH).orElseThrow().image();
        BufferedImage line = IntroSprites.lastLine(sheet);
        IntroSprites.Glyph a = glyphs.get(2);
        for (int y = 0; y < a.image().getHeight(); y++) {
            for (int x = 0; x < a.image().getWidth(); x++) {
                assertEquals(line.getRGB(a.x() + x, y), a.image().getRGB(x, y));
            }
        }
    }

    @Test
    void 화살표는_45_x_56이고_마젠타_배경은_투명하다() {
        BufferedImage arrow = sprites.arrow();
        assertEquals(45, arrow.getWidth());
        assertEquals(56, arrow.getHeight());
        assertTrue(transparent(arrow) > 0);
    }

    @Test
    void 팀_색을_고르지_않으면_원래_주황_화살표다() {
        assertSame(sprites.arrow(), sprites.arrow(null));
    }

    @Test
    void 팀_색_화살표는_모양과_흰_테두리를_두고_몸통만_그_색이다() {
        BufferedImage source = sprites.arrow();
        for (CharacterColor color : CharacterColor.values()) {
            BufferedImage tinted = sprites.arrow(color);
            assertEquals(transparent(source), transparent(tinted), color.id());
            // 몸통 한가운데 픽셀의 색상각이 팀 색과 같다
            int body = tinted.getRGB(source.getWidth() / 2, source.getHeight() / 3) & 0xFFFFFF;
            double hue = Oklch.fromRgb(body).h();
            double wanted = color.oklch().h();
            double diff = Math.abs(((hue - wanted) % 360 + 540) % 360 - 180);
            assertTrue(diff < 12, "%s 화살표 몸통 색상각 %.1f, 팀 색 %.1f".formatted(color.id(), hue, wanted));
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    if ((source.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                        assertEquals(source.getRGB(x, y), tinted.getRGB(x, y), "흰 테두리 " + color.id());
                    }
                }
            }
        }
    }

    private static int ink(BufferedImage image, int fromY, int toY) {
        int count = 0;
        for (int y = fromY; y < toY; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int transparent(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
