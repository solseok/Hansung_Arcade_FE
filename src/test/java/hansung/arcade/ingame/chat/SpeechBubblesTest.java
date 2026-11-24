package hansung.arcade.ingame.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpeechBubblesTest {

    private final SpeechBubbles bubbles = new SpeechBubbles();

    @Test
    void 채팅을_친_플레이어의_말만_뜬다() {
        bubbles.show(new ChatMessage(1, "부기", "안녕"), 0);

        assertEquals("안녕", bubbles.textOf(1));
        assertNull(bubbles.textOf(2));
    }

    @Test
    void 다시_치면_새_말로_바뀌고_그때부터_다시_시간을_센다() {
        bubbles.show(new ChatMessage(1, "부기", "먼저"), 0);
        bubbles.show(new ChatMessage(1, "부기", "나중"), 3);

        bubbles.expire(SpeechBubbles.VISIBLE_SECONDS);
        assertEquals("나중", bubbles.textOf(1));

        bubbles.expire(3 + SpeechBubbles.VISIBLE_SECONDS);
        assertNull(bubbles.textOf(1));
    }

    @Test
    void 플레이어마다_따로_내려간다() {
        bubbles.show(new ChatMessage(1, "부기", "안녕"), 0);
        bubbles.show(new ChatMessage(2, "꼬꼬", "반가워"), 2);

        bubbles.expire(SpeechBubbles.VISIBLE_SECONDS);

        assertNull(bubbles.textOf(1));
        assertEquals("반가워", bubbles.textOf(2));
    }
}
