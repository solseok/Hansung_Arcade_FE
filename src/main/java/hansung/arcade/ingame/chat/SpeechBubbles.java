package hansung.arcade.ingame.chat;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅을 친 플레이어 머리 위에 떠 있는 말풍선들. 플레이어마다 가장 최근에 친 말 하나만 띄우고,
 * {@link #VISIBLE_SECONDS}초가 지나면 내린다. 시간은 {@link ChatLog}처럼 게임 진행 시간(초)을 받아 쓴다.
 */
public final class SpeechBubbles {

    /** 말풍선이 떠 있는 시간 */
    public static final double VISIBLE_SECONDS = 5;

    private final Map<Integer, Entry> byPlayer = new HashMap<>();

    /** 보낸 플레이어의 말풍선을 이 채팅으로 바꾼다. */
    public void show(ChatMessage message, double now) {
        byPlayer.put(message.senderId(), new Entry(message.text(), now + VISIBLE_SECONDS));
    }

    public void expire(double now) {
        byPlayer.values().removeIf(entry -> entry.expiresAt() <= now);
    }

    /** 플레이어 머리 위에 떠 있는 말. 없으면 null. */
    public String textOf(int playerId) {
        Entry entry = byPlayer.get(playerId);
        return entry == null ? null : entry.text();
    }

    private record Entry(String text, double expiresAt) {
    }
}
