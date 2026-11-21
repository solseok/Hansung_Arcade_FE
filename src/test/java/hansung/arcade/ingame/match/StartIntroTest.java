package hansung.arcade.ingame.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartIntroTest {

    private static final double TICK = 1.0 / 60;

    @Test
    void 만들면_연출_없이_바로_경기한다() {
        StartIntro intro = new StartIntro();
        assertEquals(StartIntro.Phase.DONE, intro.phase());
        assertFalse(intro.isRunning());
        assertTrue(intro.isClockRunning());
        assertFalse(intro.showsArrow());
        assertEquals(0, intro.blackout());
        intro.advance(5);
        assertFalse(intro.showsArrow());
    }

    @Test
    void READY는_완전히_검은_화면이고_글자도_화살표도_시계도_없다() {
        StartIntro intro = new StartIntro();
        intro.begin();

        assertEquals(StartIntro.Phase.READY, intro.phase());
        assertEquals(1, intro.blackout());
        assertFalse(intro.isClockRunning());
        assertFalse(intro.showsArrow());

        intro.advance(StartIntro.READY_SECONDS * 0.9);
        assertEquals(1, intro.blackout());
    }

    @Test
    void START_글자가_뜬_채로_검은_화면이_걷힌다() {
        StartIntro intro = new StartIntro();
        intro.begin();
        intro.advance(StartIntro.READY_SECONDS);
        assertEquals(1, intro.blackout(), 1e-9);

        intro.advance(StartIntro.FADE_IN_SECONDS / 2);
        assertEquals(0.5, intro.blackout(), 1e-9);
        intro.advance(StartIntro.FADE_IN_SECONDS / 2);
        assertEquals(0, intro.blackout(), 1e-9);
        assertEquals(StartIntro.Phase.START, intro.phase());
    }

    @Test
    void START는_START_글자가_머물다_흩어지는_동안이고_시계와_화살표가_함께_시작한다() {
        StartIntro intro = new StartIntro();
        intro.begin();
        intro.advance(StartIntro.READY_SECONDS);

        assertEquals(StartIntro.Phase.START, intro.phase());
        assertTrue(intro.isClockRunning());
        assertTrue(intro.showsArrow());
        assertTrue(intro.isRunning());

        intro.advance(StartIntro.START_SECONDS);
        assertEquals(StartIntro.Phase.DONE, intro.phase());
        assertFalse(intro.isRunning());
    }

    @Test
    void 화살표는_START_글자가_사라진_뒤_조금_더_남았다가_사라진다() {
        StartIntro intro = new StartIntro();
        intro.begin();
        intro.advance(StartIntro.TOTAL_SECONDS);
        assertFalse(intro.isRunning());
        assertTrue(intro.showsArrow());

        intro.advance(StartIntro.ARROW_LINGER_SECONDS - 0.01);
        assertTrue(intro.showsArrow());
        intro.advance(0.02);
        assertFalse(intro.showsArrow());
    }

    @Test
    void 한_틱씩_더해도_경계에서_정확히_넘어간다() {
        StartIntro intro = new StartIntro();
        intro.begin();
        int readyTicks = (int) Math.round(StartIntro.READY_SECONDS / TICK);
        int totalTicks = (int) Math.round(StartIntro.TOTAL_SECONDS / TICK);
        for (int i = 0; i < readyTicks - 1; i++) {
            intro.advance(TICK);
        }
        assertEquals(StartIntro.Phase.READY, intro.phase());
        intro.advance(TICK);
        assertEquals(StartIntro.Phase.START, intro.phase());
        for (int i = readyTicks; i < totalTicks; i++) {
            assertTrue(intro.isRunning());
            intro.advance(TICK);
        }
        assertFalse(intro.isRunning());
    }

    @Test
    void 크아_영상에서_잰_시간을_따른다() {
        // 검은 화면 0.5초, START!! 1.5초(밝아짐 0.5초 포함, 0.75초 뒤부터 0.75초 동안 흩어짐)
        assertEquals(0.5, StartIntro.READY_SECONDS, 1e-9);
        assertEquals(1.5, StartIntro.START_SECONDS, 1e-9);
        assertTrue(StartIntro.FADE_IN_SECONDS <= StartIntro.START_HOLD_SECONDS, "다 밝아진 뒤에 흩어진다");
    }
}
