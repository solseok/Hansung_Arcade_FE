package hansung.arcade.ingame.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchClockTest {

    /** 게임 루프 한 번 */
    private static final double TICK = 1.0 / 60;

    private final MatchClock clock = new MatchClock();

    /** 게임 루프처럼 1/60초씩 n번 흘린다 */
    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            clock.advance(TICK);
        }
    }

    @Test
    void 시작하면_3분이_남아_있다() {
        assertEquals(180, clock.remainingSeconds());
        assertEquals(0, clock.elapsedSeconds());
        assertFalse(clock.isTimeUp());
    }

    @Test
    void 첫_1초_동안은_180이고_1초가_되는_틱에_179가_된다() {
        tick(30);
        assertEquals(180, clock.remainingSeconds());
        tick(29);
        assertEquals(180, clock.remainingSeconds());

        tick(1);
        assertEquals(179, clock.remainingSeconds());
        assertEquals(60, clock.elapsedTicks());
    }

    @Test
    void 매초_경계가_정확히_60틱마다_떨어진다() {
        for (int second = 1; second <= 180; second++) {
            tick(59);
            assertEquals(181 - second, clock.remainingSeconds(), second + "초 직전");
            tick(1);
            assertEquals(180 - second, clock.remainingSeconds(), second + "초");
        }
    }

    @Test
    void 마지막_1초_동안은_1이고_180초가_되는_틱에_0이_되며_시간이_다_된다() {
        tick(179 * 60 + 30);
        assertEquals(1, clock.remainingSeconds());
        tick(29);
        assertEquals(1, clock.remainingSeconds());
        assertFalse(clock.isTimeUp());

        tick(1);
        assertEquals(0, clock.remainingSeconds());
        assertTrue(clock.isTimeUp());
        assertTrue(clock.isStopped());
        assertEquals(180.0, clock.elapsedSeconds());
    }

    @Test
    void 시간이_다_되면_더_흐르지_않는다() {
        tick(180 * 60);
        tick(120);
        clock.advance(10);

        assertEquals(0, clock.remainingSeconds());
        assertEquals(180 * 60, clock.elapsedTicks());
    }

    @Test
    void 한_번에_긴_시간을_흘려도_틱_단위로_맞게_센다() {
        clock.advance(0.5);
        assertEquals(180, clock.remainingSeconds());
        clock.advance(0.5);
        assertEquals(179, clock.remainingSeconds());

        clock.advance(178.5);
        assertEquals(1, clock.remainingSeconds());
        clock.advance(1000);
        assertEquals(0, clock.remainingSeconds());
        assertTrue(clock.isTimeUp());
    }

    @Test
    void 한_틱보다_짧은_시간은_모아_두었다가_센다() {
        for (int i = 0; i < 120; i++) {
            clock.advance(TICK / 2);
        }
        assertEquals(60, clock.elapsedTicks());
        assertEquals(179, clock.remainingSeconds());
    }

    @Test
    void 표시가_바뀐_갱신에서만_바뀌었다고_알린다() {
        tick(59);
        assertFalse(clock.secondChanged());

        tick(1);
        assertTrue(clock.secondChanged());

        tick(1);
        assertFalse(clock.secondChanged());
    }

    @Test
    void 표시가_어떤_값이_된_순간은_그_갱신에서만_참이다() {
        // 61초가 되는 틱에 1:59
        tick(61 * 60 - 1);
        assertEquals(120, clock.remainingSeconds());
        assertFalse(clock.justReached(119));

        tick(1);
        assertEquals(119, clock.remainingSeconds());
        assertTrue(clock.justReached(119));
        assertFalse(clock.justReached(118));
        assertFalse(clock.justReached(120));

        tick(1);
        assertFalse(clock.justReached(119));
    }

    @Test
    void 여러_초를_건너뛰어도_지나친_값은_도달한_것으로_친다() {
        clock.advance(10);

        assertEquals(170, clock.remainingSeconds());
        assertTrue(clock.justReached(175));
        assertTrue(clock.justReached(170));
        assertFalse(clock.justReached(169));
    }

    @Test
    void 멈춘_시계는_흐르지_않고_바뀌었다는_알림도_한_번뿐이다() {
        tick(60);
        assertTrue(clock.secondChanged());
        clock.stop();

        tick(600);

        assertEquals(179, clock.remainingSeconds());
        assertFalse(clock.secondChanged());
        assertFalse(clock.justReached(179));
        assertTrue(clock.isStopped());
        assertFalse(clock.isTimeUp());
    }
}
