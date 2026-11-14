package hansung.arcade.ingame.hud;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelCanvasTest {

    private static final Color RED = new Color(0xFF0000);
    private static final Color BLUE = new Color(0x0000FF);

    @Test
    void 덮인_비율은_비움_반투명_채움_세_단계로_나뉜다() {
        assertEquals(0, PixelCanvas.quantize(0.2));
        assertEquals(0.5, PixelCanvas.quantize(0.25));
        assertEquals(0.5, PixelCanvas.quantize(0.7));
        assertEquals(1, PixelCanvas.quantize(0.75));
        assertEquals(1, PixelCanvas.quantize(1));
    }

    @Test
    void 각진_사각형은_영역을_빈틈없이_칠한다() {
        PixelCanvas canvas = new PixelCanvas(10, 10);
        canvas.fill(new Box(2, 3, 4, 5), Corners.SQUARE, RED);

        assertEquals(RED.getRGB(), canvas.image().getRGB(2, 3));
        assertEquals(RED.getRGB(), canvas.image().getRGB(5, 7));
        assertEquals(0, canvas.image().getRGB(6, 7));
        assertEquals(0, canvas.image().getRGB(2, 8));
    }

    @Test
    void 둥근_모서리는_계단과_반투명_한_단계로_칠한다() {
        PixelCanvas canvas = new PixelCanvas(20, 20);
        canvas.fill(new Box(0, 0, 20, 20), Corners.all(6), RED);

        assertEquals(0, alpha(canvas, 0, 0));
        assertEquals(255, alpha(canvas, 6, 0));
        assertEquals(255, alpha(canvas, 0, 6));
        boolean half = false;
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                int a = alpha(canvas, x, y);
                assertTrue(a == 0 || a == 128 || a == 255, "(%d, %d) alpha %d".formatted(x, y, a));
                half |= a == 128;
            }
        }
        // 원호가 지나는 칸 중 반투명 칸이 있다
        assertTrue(half);
    }

    @Test
    void 띠는_안쪽_픽셀을_건드리지_않는다() {
        PixelCanvas canvas = new PixelCanvas(10, 10);
        Box outer = new Box(0, 0, 10, 10);
        canvas.fill(outer, Corners.SQUARE, BLUE);
        canvas.ring(outer, Corners.SQUARE, outer.inset(2), Corners.SQUARE, RED);

        assertEquals(RED.getRGB(), canvas.image().getRGB(1, 5));
        assertEquals(BLUE.getRGB(), canvas.image().getRGB(2, 5));
        assertEquals(BLUE.getRGB(), canvas.image().getRGB(7, 7));
        assertEquals(RED.getRGB(), canvas.image().getRGB(8, 8));
    }

    @Test
    void 테두리_있는_상자는_가장자리가_테두리색이고_속이_채움색이다() {
        PixelCanvas canvas = new PixelCanvas(10, 10);
        canvas.fillOutlined(new Box(0, 0, 10, 10), Corners.all(3), 1, BLUE, RED);

        assertEquals(BLUE.getRGB(), canvas.image().getRGB(5, 0));
        assertEquals(BLUE.getRGB(), canvas.image().getRGB(0, 5));
        assertEquals(RED.getRGB(), canvas.image().getRGB(5, 5));
        assertEquals(0, alpha(canvas, 0, 0));
    }

    private static int alpha(PixelCanvas canvas, int x, int y) {
        return canvas.image().getRGB(x, y) >>> 24;
    }
}
