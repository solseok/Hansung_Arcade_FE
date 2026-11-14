package hansung.arcade.ingame.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MascotTest {

    @Test
    void 캐릭터마다_물풍선_물줄기_이동속도의_시작값과_최댓값이_정해져_있다() {
        //                                  물풍선      물줄기     이동속도
        assertStats(Mascot.SANGSANGBUGI, 1, 6, 1, 7, 5, 9);
        assertStats(Mascot.HANSUNGNYANGI, 2, 7, 1, 9, 4, 8);
        assertStats(Mascot.KKOKKO, 1, 8, 1, 5, 5, 8);
        assertStats(Mascot.KKUKKU, 1, 10, 1, 8, 4, 8);
        assertStats(Mascot.SANGZZI, 1, 6, 2, 7, 5, 8);
    }

    @Test
    void 기본_캐릭터는_상상부기다() {
        assertEquals(Mascot.SANGSANGBUGI, Mascot.DEFAULT);
    }

    private static void assertStats(Mascot mascot, int balloonBase, int balloonMax, int waterBase, int waterMax,
                                    int speedBase, int speedMax) {
        assertEquals(new StatRange(balloonBase, balloonMax), mascot.balloons(), mascot + " 물풍선");
        assertEquals(new StatRange(waterBase, waterMax), mascot.waterRange(), mascot + " 물줄기");
        assertEquals(new StatRange(speedBase, speedMax), mascot.speed(), mascot + " 이동속도");
    }
}
