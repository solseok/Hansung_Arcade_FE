package hansung.arcade.ingame.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 맵 위에 올라와 있는 채팅 줄들. 줄 수가 넘치면 가장 오래된 줄부터 밀려나고,
 * 올라온 지 {@link #VISIBLE_SECONDS}초가 지난 줄은 사라져서 맵을 계속 가리지 않는다.
 *
 * <p>시간은 게임 진행 시간(초)을 받아 쓰므로 Swing이나 시계에 의존하지 않는다.</p>
 */
public final class ChatLog {

    /** 한 번에 보이는 줄 수 */
    public static final int MAX_LINES = 5;
    /** 한 줄이 올라와 있는 시간 */
    public static final double VISIBLE_SECONDS = 8;

    private final Deque<Entry> entries = new ArrayDeque<>();
    /** 올라와 있는 줄. 화면이 매 프레임 읽어 가므로 줄이 올라오거나 내려갈 때만 새로 만든다 */
    private List<ChatMessage> messages = List.of();

    /** @param now 지금의 게임 진행 시간(초) */
    public void add(ChatMessage message, double now) {
        entries.addLast(new Entry(message, now + VISIBLE_SECONDS));
        if (entries.size() > MAX_LINES) {
            entries.removeFirst();
        }
        refresh();
    }

    /** 보이는 시간이 끝난 줄을 지운다. */
    public void expire(double now) {
        boolean removed = false;
        while (!entries.isEmpty() && entries.peekFirst().expiresAt() <= now) {
            entries.removeFirst();
            removed = true;
        }
        if (removed) {
            refresh();
        }
    }

    /** 올라와 있는 줄. 먼저 온 줄이 앞이다. 줄이 바뀌기 전까지는 같은 목록(바꿀 수 없음)을 돌려준다. */
    public List<ChatMessage> messages() {
        return messages;
    }

    private void refresh() {
        messages = entries.stream().map(Entry::message).toList();
    }

    private record Entry(ChatMessage message, double expiresAt) {
    }
}
