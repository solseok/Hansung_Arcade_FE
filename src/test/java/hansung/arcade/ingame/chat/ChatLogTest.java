package hansung.arcade.ingame.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLogTest {

    @Test
    void 먼저_온_줄이_앞에_온다() {
        ChatLog log = new ChatLog();
        log.add(new ChatMessage(1, "부기", "안녕"), 0);
        log.add(new ChatMessage(2, "꼬꼬", "반가워"), 1);

        assertEquals(List.of("안녕", "반가워"), texts(log));
    }

    @Test
    void 줄_수가_넘치면_가장_오래된_줄부터_밀려난다() {
        ChatLog log = new ChatLog();
        for (int i = 1; i <= ChatLog.MAX_LINES + 2; i++) {
            log.add(new ChatMessage(1, "부기", "줄" + i), 0);
        }

        assertEquals(ChatLog.MAX_LINES, log.messages().size());
        assertEquals("줄3", texts(log).get(0));
        assertEquals("줄" + (ChatLog.MAX_LINES + 2), texts(log).get(ChatLog.MAX_LINES - 1));
    }

    @Test
    void 올라온_지_정해진_시간이_지난_줄만_사라진다() {
        ChatLog log = new ChatLog();
        log.add(new ChatMessage(1, "부기", "먼저"), 0);
        log.add(new ChatMessage(1, "부기", "나중"), 3);

        log.expire(ChatLog.VISIBLE_SECONDS - 0.1);
        assertEquals(2, log.messages().size());

        log.expire(ChatLog.VISIBLE_SECONDS);
        assertEquals(List.of("나중"), texts(log));

        log.expire(3 + ChatLog.VISIBLE_SECONDS);
        assertTrue(log.messages().isEmpty());
    }

    private static List<String> texts(ChatLog log) {
        return log.messages().stream().map(ChatMessage::text).toList();
    }
}
