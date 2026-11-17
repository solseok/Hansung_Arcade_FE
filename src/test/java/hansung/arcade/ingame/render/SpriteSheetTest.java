package hansung.arcade.ingame.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteSheetTest {

    @Test
    void 알파가_있는_PNG는_반투명_알파를_그대로_쓰고_마젠타도_색_그대로_둔다() {
        List<BufferedImage> frames = SpriteSheet.find("/sprites/fixture/alpha_strip.png").orElseThrow().frames(2);

        assertEquals(2, frames.size());
        BufferedImage first = frames.get(0);
        assertEquals(0x80000000, first.getRGB(0, 0), "반투명 그림자");
        assertEquals(0xFFFF00FF, first.getRGB(1, 0), "PNG의 마젠타는 배경이 아니다");
        assertEquals(0, first.getRGB(0, 1) >>> 24);
        assertEquals(0xFF2080FF, first.getRGB(1, 1));
        assertEquals(0x40102030, frames.get(1).getRGB(0, 0));
    }

    @Test
    void 알파가_없는_BMP는_마젠타_배경만_투명하게_바꾸고_나머지는_불투명하다() {
        BufferedImage frame = SpriteSheet.find("/image/fixture/01_block_normal.bmp").orElseThrow().frames(40).get(0);

        int transparent = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int alpha = frame.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 0xFF, "(%d, %d) 알파 %d".formatted(x, y, alpha));
                if (alpha == 0) {
                    transparent++;
                }
            }
        }
        assertTrue(transparent > 0, "마젠타 배경이 투명해져야 한다");
    }
}
