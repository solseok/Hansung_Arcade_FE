package hansung.arcade.ingame.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackBackgroundTest {

    private static final int WATER = 0x40C0FF;
    private static final int DARK_FRINGE = 0x3C3246;

    private static BufferedImage blackImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // TYPE_INT_RGB는 처음부터 모두 검정이다
        return image;
    }

    private static void fill(BufferedImage image, int x0, int y0, int size, int rgb) {
        for (int y = y0; y < y0 + size; y++) {
            for (int x = x0; x < x0 + size; x++) {
                image.setRGB(x, y, rgb);
            }
        }
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @Test
    void 검정_바탕과_거기에_이어진_잡티는_지우고_물줄기_안의_같은_색은_남긴다() {
        BufferedImage frame = blackImage(10, 10);
        fill(frame, 2, 2, 6, WATER);
        // 바탕에 붙은 잡티 한 점과, 물줄기 안에 갇힌 같은 색 한 점
        frame.setRGB(1, 4, DARK_FRINGE);
        frame.setRGB(4, 4, DARK_FRINGE);

        BufferedImage cleared = BlackBackground.clear(frame);

        assertEquals(0, alpha(cleared, 0, 0));
        assertEquals(0, alpha(cleared, 1, 4));
        assertEquals(0xFF, alpha(cleared, 4, 4));
        assertEquals(0xFF000000 | DARK_FRINGE, cleared.getRGB(4, 4));
        assertEquals(0xFF000000 | WATER, cleared.getRGB(3, 3));
        assertEquals(0xFF000000, frame.getRGB(0, 0) & 0xFF000000, "원본은 그대로 둔다");
    }

    @Test
    void 떨어져_떠다니는_작은_점은_지운다() {
        BufferedImage frame = blackImage(20, 20);
        fill(frame, 1, 1, 2, 0xFFFFFF);
        fill(frame, 10, 10, 4, 0xFFFFFF);

        BufferedImage cleared = BlackBackground.clear(frame);

        assertEquals(0, alpha(cleared, 1, 1), "4픽셀짜리 점");
        assertEquals(0xFF, alpha(cleared, 10, 10), "16픽셀짜리 덩어리");
    }

    @Test
    void 어둡거나_회색빛이거나_분홍빛이면_잡티다() {
        assertTrue(BlackBackground.isFringe(0x050304));
        assertTrue(BlackBackground.isFringe(0x887788));
        assertTrue(BlackBackground.isFringe(0xB975A6));
        assertFalse(BlackBackground.isFringe(0x96F0FF), "밝은 하늘색 물");
        assertFalse(BlackBackground.isFringe(0xFFFFFF), "물보라 흰색");
        assertFalse(BlackBackground.isFringe(0x0064C8), "짙은 파랑 물");
    }
}
