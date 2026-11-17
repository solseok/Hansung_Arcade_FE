package hansung.arcade.ingame.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagentaSpillTest {

    private static final int WATER = 0xFF40C0FF;
    private static final int PINK = 0xFFFFDBFF;

    private static BufferedImage filled(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    @Test
    void 보라_분홍_쪽으로_기운_색만_번진_자국이다() {
        assertTrue(MagentaSpill.isSpill(0xFFDBFF), "옅은 분홍");
        assertTrue(MagentaSpill.isSpill(0xC18FB2), "자줏빛 회색");
        assertTrue(MagentaSpill.isSpill(0xD78AAC), "탁한 분홍");
        assertTrue(MagentaSpill.isSpill(0x9174CE), "보라");
        assertTrue(MagentaSpill.isSpill(0xE38095), "붉은 분홍");
        assertTrue(MagentaSpill.isSpill(0x978CD8), "남보라");

        assertFalse(MagentaSpill.isSpill(0x0073F8), "짙은 파랑 물");
        assertFalse(MagentaSpill.isSpill(0x149EFF), "파랑 물");
        assertFalse(MagentaSpill.isSpill(0xC6FFFF), "밝은 하늘색 물");
        assertFalse(MagentaSpill.isSpill(0x9099E8), "보라에 못 미친 남파랑");
        assertFalse(MagentaSpill.isSpill(0xDBDBFF), "빨강과 초록이 같은 연파랑");
        assertFalse(MagentaSpill.isSpill(0xFFFFFF), "물보라 흰색");
        assertFalse(MagentaSpill.isSpill(0xFFF8E3), "노르스름한 하이라이트");
        assertFalse(MagentaSpill.isSpill(0xFFF9FF), "거의 흰색");
    }

    @Test
    void 번진_한_점은_둘레_물_색으로_메우고_원본은_그대로_둔다() {
        BufferedImage frame = filled(5, 5, WATER);
        frame.setRGB(2, 2, PINK);

        BufferedImage cleaned = MagentaSpill.clean(frame);

        assertEquals(WATER, cleaned.getRGB(2, 2));
        assertEquals(WATER, cleaned.getRGB(0, 0));
        assertEquals(PINK, frame.getRGB(2, 2), "원본은 그대로 둔다");
    }

    @Test
    void 이웃이_여러_색이면_평균으로_메운다() {
        BufferedImage frame = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        frame.setRGB(0, 0, 0xFF2060A0);
        frame.setRGB(1, 0, PINK);
        frame.setRGB(2, 0, 0xFF60A0E0);

        assertEquals(0xFF4080C0, MagentaSpill.clean(frame).getRGB(1, 0));
    }

    @Test
    void 여러_줄이_물들면_물_쪽에서부터_채우고_투명한_칸은_건드리지_않는다() {
        BufferedImage frame = filled(6, 6, WATER);
        for (int x = 0; x < 5; x++) {
            frame.setRGB(x, 0, PINK);
            frame.setRGB(x, 1, 0xFF9174CE);
        }
        // 오른쪽 끝 줄은 투명. 알파 0인 마젠타는 그대로 둔다
        for (int y = 0; y < 6; y++) {
            frame.setRGB(5, y, 0x00FF00FF);
        }

        BufferedImage cleaned = MagentaSpill.clean(frame);

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 5; x++) {
                assertEquals(WATER, cleaned.getRGB(x, y), "(%d, %d)".formatted(x, y));
            }
            assertEquals(0x00FF00FF, cleaned.getRGB(5, y), "투명한 칸");
        }
    }

    @Test
    void 깨끗한_이웃이_없으면_빨강을_초록만큼_낮춰_파랑_쪽으로_돌린다() {
        BufferedImage frame = filled(3, 3, PINK);

        BufferedImage cleaned = MagentaSpill.clean(frame);

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals(0xFFDBDBFF, cleaned.getRGB(x, y));
                assertFalse(MagentaSpill.isSpill(cleaned.getRGB(x, y)));
            }
        }
    }
}
