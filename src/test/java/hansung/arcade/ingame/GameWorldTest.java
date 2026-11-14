package hansung.arcade.ingame;

import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.MatchOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWorldTest {

    /** 게임 루프 한 번 */
    private static final double TICK = 1.0 / 60;

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
    void 남이_보낸_글도_한_줄_64바이트로_다듬고_빈_글은_올리지_않는다() {
        world.receiveChat(new ChatMessage(myId + 1, "꼬꼬", ""));
        world.receiveChat(new ChatMessage(myId + 1, "꼬꼬", " \n "));
        assertTrue(world.chatLog().messages().isEmpty());
        assertNull(world.speechBubbles().textOf(myId + 1));

        // "첫 줄 "이 6바이트라 뒤에 한글 29자(58바이트)까지 들어간다
        world.receiveChat(new ChatMessage(myId + 1, "꼬꼬", "첫 줄\n" + "가".repeat(40)));
        String cleaned = "첫 줄 " + "가".repeat(29);
        assertEquals(List.of(new ChatMessage(myId + 1, "꼬꼬", cleaned)), world.chatLog().messages());
        assertEquals(cleaned, world.speechBubbles().textOf(myId + 1));
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

    @Test
    void 경기_시계는_3분에서_시작해_게임_루프를_따라_1초씩_줄어든다() {
        assertEquals(180, world.match().clock().remainingSeconds());

        tick(61 * 60);

        assertEquals(119, world.match().clock().remainingSeconds());
        assertFalse(world.match().isOver());
    }

    @Test
    void 따로_정하지_않으면_개인전이고_팀전으로도_시작할_수_있다() {
        assertEquals(MatchMode.SOLO, world.match().mode());

        GameWorld team = new GameWorld(MapLoader.parse("fixture/team", List.of("1....")),
                new PlayerProfile("부기", Mascot.SANGSANGBUGI, 2), MatchMode.TEAM);
        assertEquals(MatchMode.TEAM, team.match().mode());
    }

    @Test
    void 혼자_하는_판은_시간이_다_되면_끝나고_살아_있으면_이긴다() {
        tick(MatchClock.DURATION_SECONDS * 60 - 1);
        assertFalse(world.match().isOver());

        tick(1);

        assertTrue(world.match().isOver());
        assertEquals(MatchOutcome.WIN, world.match().result().orElseThrow().outcomeOf(myId));
    }

    @Test
    void 경기가_끝나면_방향키를_눌러도_움직이지_않지만_시간은_계속_흐른다() {
        Player me = world.localPlayer();
        tick(MatchClock.DURATION_SECONDS * 60);
        assertTrue(world.match().isOver());
        double x = me.x();
        double elapsed = world.elapsedSeconds();
        world.sendChat("gg");

        for (int i = 0; i < 60; i++) {
            world.update(TICK, Direction.RIGHT);
        }

        assertEquals(x, me.x());
        assertFalse(me.isMoving());
        assertEquals(elapsed + 1, world.elapsedSeconds(), 1e-9);
        assertEquals(0, world.match().clock().remainingSeconds());
        // 끝난 뒤에 친 채팅도 시간이 지나면 내려간다
        world.update(ChatLog.VISIBLE_SECONDS, null);
        assertTrue(world.chatLog().messages().isEmpty());
    }

    @Test
    void 경기_중에는_방향키대로_움직인다() {
        double x = world.localPlayer().x();

        world.update(TICK, Direction.RIGHT);

        assertTrue(world.localPlayer().x() > x);
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            world.update(TICK, null);
        }
    }
}
