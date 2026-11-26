package hansung.arcade.ingame.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 비행기 그림을 맵의 어느 높이에 그리는지 ({@link AirDropRenderer#planeTop}). 맵은 13줄, 520px이다 */
class AirDropRendererTest {

    private static final int ROWS = 13;

    @Test
    void 셋째_줄부터는_배가_지나가는_줄의_위쪽_끝에_오게_그린다() {
        // 그림 위쪽 끝 = 줄 × 40 - 44
        assertEquals(36, AirDropRenderer.planeTop(80));
        assertEquals(316, AirDropRenderer.planeTop(360));
        assertEquals(436, AirDropRenderer.planeTop(480), "맨 아랫줄");
    }

    @Test
    void 맨_윗줄과_둘째_줄은_그림이_맵_위로_넘지_않게_맵_맨_위에_붙여_그린다() {
        assertEquals(0, AirDropRenderer.planeTop(0), "44px 넘는 만큼 내린다");
        assertEquals(0, AirDropRenderer.planeTop(40), "4px 넘는 만큼 내린다");
    }

    @Test
    void 어느_줄이든_그림_전체가_맵_안에_있고_지나가는_줄을_덮는다() {
        for (int row = 0; row < ROWS; row++) {
            int top = AirDropRenderer.planeTop(row * 40);
            assertTrue(top >= 0, "줄 %d: 위쪽 끝 %d".formatted(row, top));
            assertTrue(top + AirplaneSprites.FRAME_HEIGHT <= ROWS * 40, "줄 %d: 아래쪽 끝".formatted(row));
            // 그림이 지나가는 줄을 덮는다: 그림 위쪽 끝이 줄 위쪽 끝보다 위거나 같고, 아래쪽 끝은 줄 가운데보다 아래다
            assertTrue(top <= row * 40 && top + AirplaneSprites.FRAME_HEIGHT > row * 40 + 20, "줄 " + row);
        }
    }
}
