package hansung.arcade.ingame;

import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWorldTest {

    private final GameWorld world = new GameWorld(MapLoader.parse("fixture/chat", List.of("1....", ".....")),
            new PlayerProfile("부기", Mascot.SANGSANGBUGI));
    private final int myId = world.localPlayer().id();

    @Test
    void 내가_친_채팅은_앞뒤_공백을_떼고_내_번호와_닉네임으로_올라온다() {
        world.sendChat("  안녕하세요 ");

        assertEquals(List.of(new ChatMessage(myId, "부기", "안녕하세요")), world.chatLog().messages());
    }

    @Test
    void 여러_줄이거나_너무_긴_글은_한_줄_64바이트로_다듬어_올린다() {
        world.sendChat("첫 줄\n둘째 줄");
        world.sendChat("가".repeat(40));

        assertEquals(List.of("첫 줄 둘째 줄", "가".repeat(32)),
                world.chatLog().messages().stream().map(ChatMessage::text).toList());
    }

    @Test
    void 내용이_없는_채팅은_올리지_않는다() {
        world.sendChat("");
        world.sendChat("   ");

        assertTrue(world.chatLog().messages().isEmpty());
        assertNull(world.speechBubbles().textOf(myId));
    }

    @Test
    void 다른_플레이어의_채팅도_같은_목록에_올라온다() {
        world.sendChat("안녕");
        world.receiveChat(new ChatMessage(myId + 1, "꼬꼬", "반가워"));

        assertEquals(List.of("안녕", "반가워"), world.chatLog().messages().stream().map(ChatMessage::text).toList());
    }

    @Test
    void 채팅을_치면_친_플레이어의_말풍선이_뜬다() {
        world.sendChat("안녕");
        world.receiveChat(new ChatMessage(myId + 1, "꼬꼬", "반가워"));

        assertEquals("안녕", world.speechBubbles().textOf(myId));
        assertEquals("반가워", world.speechBubbles().textOf(myId + 1));
    }

    @Test
    void 말풍선이_먼저_내려가고_채팅_목록은_조금_더_남는다() {
        world.sendChat("안녕");

        world.update(SpeechBubbles.VISIBLE_SECONDS, null);
        assertNull(world.speechBubbles().textOf(myId));
        assertEquals(1, world.chatLog().messages().size());

        world.update(ChatLog.VISIBLE_SECONDS - SpeechBubbles.VISIBLE_SECONDS, null);
        assertTrue(world.chatLog().messages().isEmpty());
    }
}
