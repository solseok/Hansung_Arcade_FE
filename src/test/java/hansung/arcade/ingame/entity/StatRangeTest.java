package hansung.arcade.ingame.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatRangeTest {

    @Test
    void 시작값은_1_이상이고_최댓값보다_크지_않아야_한다() {
        assertThrows(IllegalArgumentException.class, () -> new StatRange(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new StatRange(3, 2));
    }

    @Test
    void 시작값과_최댓값이_같아도_된다() {
        assertDoesNotThrow(() -> new StatRange(2, 2));
    }

    @Test
    void 값을_시작값과_최댓값_사이로_맞춘다() {
        StatRange range = new StatRange(2, 7);

        assertEquals(2, range.clamp(-3));
        assertEquals(5, range.clamp(5));
        assertEquals(7, range.clamp(8));
    }

    @Test
    void int_범위를_넘는_값도_맞춘다() {
        StatRange range = new StatRange(2, 7);

        assertEquals(7, range.clamp((long) Integer.MAX_VALUE + 1));
        assertEquals(2, range.clamp((long) Integer.MIN_VALUE - 1));
    }
}
