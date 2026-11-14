package hansung.arcade.ingame.hud;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 투명한 그림에 테두리 글자를 그려 픽셀을 확인한다. 돋움체가 없는 PC와 CI에서도 돌도록
 * 모양 검사는 Java 기본 고정폭 글꼴로 하고, 돋움체 검사는 돋움체가 있을 때만 한다.
 */
class OutlineTextTest {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int CELL = HudLayout.CHAT_TEXT_HEIGHT;
    /** 글자 칸의 왼쪽 위를 둔 자리 */
    private static final int ORIGIN = 10;

    private final OutlineText text = new OutlineText(new Font(Font.MONOSPACED, Font.PLAIN, 12), CELL,
            Color.WHITE, Color.BLACK);

    @Test
    void 글자는_채움색과_테두리색_두_가지로만_찍히고_나머지는_비어_있다() {
        BufferedImage image = draw(text, "가Ag 뷁!");
        int fill = 0;
        int outline = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (argb == WHITE) {
                    fill++;
                } else if (argb == BLACK) {
                    outline++;
                } else {
                    assertEquals(0, argb, "(%d, %d) 중간색 %08X".formatted(x, y, argb));
                }
            }
        }
        assertTrue(fill > 50, "글자 픽셀 " + fill);
        assertTrue(outline > fill, "테두리 픽셀 " + outline);
    }

    @Test
    void 테두리는_글자_픽셀에_맞닿은_1px만_감싼다() {
        BufferedImage image = draw(text, "가Ag 뷁!");
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                int argb = image.getRGB(x, y);
                if (argb == WHITE) {
                    continue;
                }
                // 글자에 닿은 자리는 빠짐없이 테두리이고, 닿지 않은 자리에는 아무것도 없다
                assertEquals(touchesFill(image, x, y) ? BLACK : 0, argb, "(%d, %d)".formatted(x, y));
            }
        }
    }

    @Test
    void 글자_폭은_테두리를_빼고_잰다() {
        assertEquals(0, text.width(""));
        assertEquals(2 * text.width("가"), text.width("가가"));
        assertTrue(text.width("가") >= text.width("a"));
    }

    @Test
    void 빈_글은_아무것도_그리지_않는다() {
        BufferedImage image = draw(text, "");
        assertEquals(new Box(0, 0, image.getWidth(), image.getHeight()), PixelImages.opaqueBounds(image));
        assertEquals(0, image.getRGB(ORIGIN, ORIGIN));
    }

    @Test
    void 커서_막대는_칸_높이의_흰_막대를_검은_1px_테두리가_감싼다() {
        BufferedImage image = new BufferedImage(30, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        text.drawBar(g, ORIGIN, ORIGIN, 1);
        g.dispose();

        assertEquals(new Box(ORIGIN - 1, ORIGIN - 1, 3, CELL + 2), PixelImages.opaqueBounds(image));
        assertEquals(WHITE, image.getRGB(ORIGIN, ORIGIN));
        assertEquals(WHITE, image.getRGB(ORIGIN, ORIGIN + CELL - 1));
        assertEquals(BLACK, image.getRGB(ORIGIN, ORIGIN - 1));
        assertEquals(BLACK, image.getRGB(ORIGIN + 1, ORIGIN + 5));
    }

    @Test
    void 돋움체_12px_글자는_12px_칸에_위아래로_딱_맞는다() {
        HudFonts fonts = HudFonts.load();
        assumeTrue(!fonts.missing().contains(HudFonts.DOTUMCHE), "돋움체가 없는 환경");
        OutlineText dotum = new OutlineText(fonts.dotumChe(CELL), CELL, Color.WHITE, Color.BLACK);

        // 위로 가장 높은 글자와 아래로 가장 내려가는 글자가 섞인 줄
        Box ink = fillBounds(draw(dotum, "가나다 gjy [|]"));
        assertEquals(ORIGIN, ink.y());
        assertEquals(CELL, ink.height());

        // 글자마다 픽셀 높이가 달라도 기준선은 그대로다: 한글은 칸 바닥에서 한 줄 위(g, j, y의 꼬리 자리)에서 끝난다
        assertEquals(ORIGIN + CELL - 1, fillBounds(draw(dotum, "가나다")).bottom());
        assertEquals(ORIGIN + CELL, fillBounds(draw(dotum, "gjy")).bottom());
        // 돋움체는 한글 12px, 영문·숫자 6px 고정폭이다
        assertEquals(12, dotum.width("가"));
        assertEquals(6, dotum.width("a"));
    }

    private static BufferedImage draw(OutlineText art, String string) {
        BufferedImage image = new BufferedImage(160, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        art.draw(g, string, ORIGIN, ORIGIN);
        g.dispose();
        return image;
    }

    private static boolean touchesFill(BufferedImage image, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (image.getRGB(x + dx, y + dy) == WHITE) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 채움색(흰색) 픽셀만 담는 영역 */
    private static Box fillBounds(BufferedImage image) {
        BufferedImage fillOnly = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == WHITE) {
                    fillOnly.setRGB(x, y, WHITE);
                }
            }
        }
        return PixelImages.opaqueBounds(fillOnly);
    }
}
