package hansung.arcade.ingame;

import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.MatchOutcome;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWorldTest {

    /** 게임 루프 한 번 */
    private static final double TICK = 1.0 / 60;

    private static final PlayerProfile BUGI = new PlayerProfile("부기", Mascot.SANGSANGBUGI);
    private static final PlayerProfile NYANGI = new PlayerProfile("냥이", Mascot.HANSUNGNYANGI);
    /** 스폰 1이 왼쪽 위, 스폰 2가 오른쪽 아래인 빈 맵 */
    private static final TileMap TWO_SPAWNS = MapLoader.parse("fixture/duo", List.of("1....", ".....", "....2"));

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

        world.update(SpeechBubbles.VISIBLE_SECONDS, Map.of());
        assertNull(world.speechBubbles().textOf(myId));
        assertEquals(1, world.chatLog().messages().size());

        world.update(ChatLog.VISIBLE_SECONDS - SpeechBubbles.VISIBLE_SECONDS, Map.of());
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
            world.update(TICK, Map.of(world.localPlayer().id(), PlayerCommand.moving(Direction.RIGHT)));
        }

        assertEquals(x, me.x());
        assertFalse(me.isMoving());
        assertEquals(elapsed + 1, world.elapsedSeconds(), 1e-9);
        assertEquals(0, world.match().clock().remainingSeconds());
        // 끝난 뒤에 친 채팅도 시간이 지나면 내려간다
        world.update(ChatLog.VISIBLE_SECONDS, Map.of());
        assertTrue(world.chatLog().messages().isEmpty());
    }

    @Test
    void 경기_중에는_방향키대로_움직인다() {
        double x = world.localPlayer().x();

        world.update(TICK, Map.of(world.localPlayer().id(), PlayerCommand.moving(Direction.RIGHT)));

        assertTrue(world.localPlayer().x() > x);
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            world.update(TICK, Map.of());
        }
    }

    @Test
    void 혼자_하면_이_컴퓨터의_플레이어는_1번_스폰의_1P_한_명이다() {
        GameWorld solo = new GameWorld(TWO_SPAWNS, BUGI);

        assertEquals(List.of(solo.localPlayer()), solo.localPlayers());
        assertEquals(solo.localPlayers(), solo.players());
        assertEquals(1, solo.localPlayer().id());
        assertEquals(0, solo.localPlayer().col());
        assertEquals(0, solo.localPlayer().row());
    }

    @Test
    void 두_명이면_1P는_1번_스폰_2P는_2번_스폰에서_각자의_정보로_시작한다() {
        GameWorld duo = new GameWorld(TWO_SPAWNS, List.of(BUGI, NYANGI));

        List<Player> players = duo.localPlayers();
        assertEquals(2, players.size());
        assertEquals(players, duo.players());
        Player first = players.get(0);
        Player second = players.get(1);
        assertEquals(List.of(1, 2), List.of(first.id(), second.id()));
        assertEquals(List.of(BUGI, NYANGI), List.of(first.profile(), second.profile()));
        assertEquals(List.of(0, 0), List.of(first.col(), first.row()));
        assertEquals(List.of(4, 2), List.of(second.col(), second.row()));
        // 고른 캐릭터의 능력치로 시작한다
        assertEquals(Mascot.HANSUNGNYANGI.balloons().base(), second.balloons());
        assertEquals(first, duo.localPlayer());
    }

    @Test
    void 명령은_플레이어_번호대로_따로_들어가고_명령이_없는_플레이어는_가만히_있다() {
        GameWorld duo = new GameWorld(TWO_SPAWNS, List.of(BUGI, NYANGI));
        Player first = duo.localPlayers().get(0);
        Player second = duo.localPlayers().get(1);
        double secondX = second.x();
        double secondY = second.y();

        duo.update(0.1, Map.of(first.id(), PlayerCommand.moving(Direction.RIGHT)));
        assertTrue(first.x() > TileMap.centerX(0));
        assertEquals(secondX, second.x());

        duo.update(0.1, Map.of(second.id(), PlayerCommand.moving(Direction.UP)));
        assertTrue(second.y() < secondY);
        assertEquals(secondX, second.x());
    }

    @Test
    void 두_명이어도_채팅은_1P가_친_것으로_올라간다() {
        GameWorld duo = new GameWorld(TWO_SPAWNS, List.of(BUGI, NYANGI));

        duo.sendChat("안녕");

        assertEquals(List.of(new ChatMessage(1, "부기", "안녕")), duo.chatLog().messages());
    }

    @Test
    void 스폰보다_사람이_많거나_아무도_없으면_만들지_않는다() {
        TileMap oneSpawn = MapLoader.parse("fixture/solo", List.of("1...."));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GameWorld(oneSpawn, List.of(BUGI, NYANGI)));
        assertTrue(e.getMessage().contains("스폰"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new GameWorld(oneSpawn, List.of()));
    }
}
