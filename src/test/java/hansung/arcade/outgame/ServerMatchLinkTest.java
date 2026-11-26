package hansung.arcade.outgame;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.OnlineSeat;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.network.FakeServer;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 가짜 서버로 인게임 푸시가 경기에 들어가는지와, 보내는 메시지가 서버 명세 모양인지 본다. 푸시는 받는 스레드에서 바로 처리한다 */
class ServerMatchLinkTest {

    private FakeServer server;
    private ServerClient client;
    private ServerMatchLink link;
    private GameWorld world;
    private final List<GameResult> results = new CopyOnWriteArrayList<>();
    private final AtomicInteger kicks = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(1_000, 1_000, 5_000, 15_000)));
        link = new ServerMatchLink(client, Runnable::run);
        client.connect().get(3, TimeUnit.SECONDS);
        world = GameWorld.online(MapLoader.parse("fixture/link", List.of("...B.P", ".1..2.", "......")), List.of(
                new OnlineSeat(12, new PlayerProfile("부기", Mascot.SANGSANGBUGI, 1), 1, true),
                new OnlineSeat(20, new PlayerProfile("냥이", Mascot.HANSUNGNYANGI, 2), 2, false)), link);
    }

    /** 게임 화면을 띄운 것처럼 경기를 잇는다 */
    private void bind() {
        link.bind(world.server().orElseThrow(), world::receiveChat, results::add, kicks::incrementAndGet);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    private static void until(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 60 && !condition.getAsBoolean(); i++) {
            Thread.sleep(50);
        }
        assertTrue(condition.getAsBoolean(), "기다린 일이 일어나지 않았습니다.");
    }

    private Player player(int id) {
        return world.players().stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }

    @Test
    void 이동_물풍선_아이템_갇힘_탈락_푸시를_경기에_넣는다() throws Exception {
        bind();
        server.push("{\"type\":\"ROUND_BEGIN\",\"body\":{\"countdownMs\":0,\"timeLimitMs\":180000}}");
        until(() -> world.server().orElseThrow().isPlaying());

        server.push("{\"type\":\"PLAYER_MOVED\",\"body\":{\"playerId\":20,\"x\":190.0,\"y\":60.0,\"facing\":\"LEFT\",\"moving\":true}}");
        server.push("{\"type\":\"BALLOON_PLACED\",\"body\":{\"balloonId\":1,\"ownerId\":20,\"col\":2,\"row\":2,\"range\":1,\"fuseMs\":2000}}");
        until(() -> world.balloonField().balloons().size() == 1);

        server.push("""
                {"type":"BALLOON_EXPLODED","body":{"balloons":[{"balloonId":1,"col":2,"row":2,"up":1,"down":0,"left":1,"right":1}],
                 "tiles":[{"col":3,"row":0,"tile":"."}],"items":[{"itemId":1,"col":3,"row":0,"item":"roller_shoe"}],"removedItems":[]}}"""
                .replaceAll("\\n\\s*", ""));
        until(() -> world.balloonField().balloons().isEmpty());
        assertEquals(1, world.balloonField().explosions().size());
        assertEquals(Tile.FLOOR, world.map().tileAt(3, 0));
        assertEquals(ItemType.ROLLER_SHOE, world.items().at(3, 0).orElseThrow().type());

        server.push("{\"type\":\"ITEM_TAKEN\",\"body\":{\"itemId\":1,\"playerId\":20,\"item\":\"roller_shoe\","
                + "\"stats\":{\"balloons\":2,\"waterRange\":1,\"speedLevel\":5},\"heldItem\":null}}");
        until(() -> world.items().all().isEmpty());
        assertEquals(5, player(20).speedLevel());

        server.push("{\"type\":\"PLAYER_TRAPPED\",\"body\":{\"playerId\":12,\"balloonId\":1,\"popMs\":5000}}");
        until(() -> player(12).state() == PlayerState.TRAPPED);
        server.push("{\"type\":\"PLAYER_POPPED\",\"body\":{\"playerId\":12,\"killerId\":null,\"reason\":\"TIMEOUT\"}}");
        until(() -> !player(12).isAlive());

        server.push("{\"type\":\"ITEM_SCATTERED\",\"body\":{\"playerId\":12,\"col\":1,\"row\":1,"
                + "\"items\":[{\"itemId\":2,\"col\":0,\"row\":2,\"item\":\"bubble\"}]}}");
        until(() -> world.items().at(0, 2).isPresent());

        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[20],\"reason\":\"EXIT\"}}");
        until(() -> player(20).state() == PlayerState.GONE);
    }

    @Test
    void row를_보내지_않는_옛_서버의_비행기는_맨_윗줄로_띄운다() throws Exception {
        bind();
        server.push("{\"type\":\"ROUND_BEGIN\",\"body\":{\"countdownMs\":0,\"timeLimitMs\":180000}}");
        until(() -> world.server().orElseThrow().isPlaying());

        server.push("{\"type\":\"AIRPLANE_APPEARED\",\"body\":{\"airplaneId\":1,\"heading\":\"RIGHT\"}}");
        until(() -> world.airDrop().flights().size() == 1);

        assertEquals(0, world.airDrop().flights().getFirst().row());
    }

    @Test
    void 비행기와_쓰는_아이템_푸시를_경기에_넣는다() throws Exception {
        bind();
        server.push("{\"type\":\"ROUND_BEGIN\",\"body\":{\"countdownMs\":0,\"timeLimitMs\":180000}}");
        until(() -> world.server().orElseThrow().isPlaying());

        server.push("{\"type\":\"AIRPLANE_APPEARED\",\"body\":{\"airplaneId\":1,\"heading\":\"LEFT\",\"row\":2}}");
        server.push("{\"type\":\"ITEM_DROPPED\",\"body\":{\"airplaneId\":1,\"itemId\":4,\"col\":0,\"row\":2,"
                + "\"item\":\"shoe\",\"fallMs\":129}}");
        until(() -> world.airDrop().fallingItems().size() == 1);
        assertEquals(1, world.airDrop().flights().size());
        assertEquals(2, world.airDrop().flights().getFirst().row(), "AIRPLANE_APPEARED의 row를 따라 난다");

        server.push("{\"type\":\"ITEM_TAKEN\",\"body\":{\"itemId\":9,\"playerId\":12,\"item\":\"shield\","
                + "\"stats\":{\"balloons\":1,\"waterRange\":1,\"speedLevel\":5},\"heldItem\":\"shield\",\"heldCount\":2}}");
        until(() -> world.gear(player(12)).activeCount() == 2);
        server.push("{\"type\":\"ITEM_USED\",\"body\":{\"playerId\":12,\"item\":\"shield\",\"durationMs\":3000,"
                + "\"heldItem\":\"shield\",\"heldCount\":1}}");
        until(() -> world.gear(player(12)).isShielded());
        assertEquals(1, world.gear(player(12)).activeCount());

        // 아이템을 쓰기 전 서버(heldCount가 없음)에서 온 ITEM_TAKEN은 손을 비운다
        server.push("{\"type\":\"ITEM_TAKEN\",\"body\":{\"itemId\":4,\"playerId\":12,\"item\":\"shoe\","
                + "\"stats\":{\"balloons\":1,\"waterRange\":1,\"speedLevel\":5},\"heldItem\":null}}");
        until(() -> world.gear(player(12)).activeItem().isEmpty());
        assertTrue(world.airDrop().fallingItems().isEmpty(), "떨어지던 아이템을 누가 먹으면 지운다");
    }

    @Test
    void 상자_이동_푸시를_경기에_넣는다() throws Exception {
        bind();
        server.push("{\"type\":\"ROUND_BEGIN\",\"body\":{\"countdownMs\":0,\"timeLimitMs\":180000}}");
        until(() -> world.server().orElseThrow().isPlaying());

        server.push("{\"type\":\"BOX_MOVED\",\"body\":{\"fromCol\":5,\"fromRow\":0,\"toCol\":5,\"toRow\":1}}");

        // 맵 칸을 옮긴 뒤 미끄러짐을 더하므로, 미끄러짐이 보이면 칸도 다 옮겨졌다
        until(() -> world.boxPusher().slidingInto(5, 1).isPresent());
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(5, 1));
        assertEquals(Tile.FLOOR, world.map().tileAt(5, 0));
    }

    @Test
    void 물풍선_던지기_차기_멈춤_푸시를_경기에_넣는다() throws Exception {
        bind();
        server.push("{\"type\":\"ROUND_BEGIN\",\"body\":{\"countdownMs\":0,\"timeLimitMs\":180000}}");
        until(() -> world.server().orElseThrow().isPlaying());
        server.push("{\"type\":\"BALLOON_PLACED\",\"body\":{\"balloonId\":1,\"ownerId\":12,\"col\":0,\"row\":2,\"range\":1,\"fuseMs\":2000}}");
        until(() -> world.balloonField().balloons().size() == 1);
        WaterBalloon balloon = world.balloonField().balloons().getFirst();

        server.push("{\"type\":\"BALLOON_THROWN\",\"body\":{\"balloonId\":1,\"playerId\":null,\"col\":0,\"row\":2,"
                + "\"toCol\":5,\"toRow\":2,\"direction\":\"RIGHT\",\"flightMs\":300}}");
        until(balloon::isFlying);
        assertEquals(5, balloon.col(), "나는 동안 칸은 내려앉을 칸이다");

        server.push("{\"type\":\"BALLOON_PLACED\",\"body\":{\"balloonId\":2,\"ownerId\":20,\"col\":2,\"row\":0,\"range\":1,\"fuseMs\":2000}}");
        until(() -> world.balloonField().balloons().size() == 2);
        WaterBalloon second = world.balloonField().balloons().get(1);
        server.push("{\"type\":\"BALLOON_KICKED\",\"body\":{\"balloonId\":2,\"playerId\":20,\"col\":2,\"row\":0,"
                + "\"direction\":\"LEFT\"}}");
        until(second::isSliding);
        server.push("{\"type\":\"BALLOON_STOPPED\",\"body\":{\"balloonId\":2,\"col\":1,\"row\":0}}");
        until(second::isResting);
        assertEquals(List.of(1, 0), List.of(second.col(), second.row()));
    }

    @Test
    void 경기_중_채팅은_맵_위에_올리고_서버_안내는_올리지_않는다() throws Exception {
        bind();
        server.push("{\"type\":\"CHAT_MESSAGE\",\"body\":{\"senderId\":0,\"sender\":\"\",\"text\":\"냥이님이 나갔습니다.\"}}");
        server.push("{\"type\":\"CHAT_MESSAGE\",\"body\":{\"senderId\":20,\"sender\":\"냥이\",\"text\":\"안녕\"}}");

        until(() -> !world.chatLog().messages().isEmpty());
        assertEquals(1, world.chatLog().messages().size());
        assertEquals("냥이", world.chatLog().messages().getFirst().sender());
    }

    @Test
    void 경기_화면을_닫으면_푸시를_버린다() throws Exception {
        bind();
        link.unbind();

        server.push("{\"type\":\"BALLOON_PLACED\",\"body\":{\"balloonId\":1,\"ownerId\":20,\"col\":2,\"row\":2,\"range\":1,\"fuseMs\":2000}}");
        server.push("{\"type\":\"CHAT_MESSAGE\",\"body\":{\"senderId\":20,\"sender\":\"냥이\",\"text\":\"안녕\"}}");
        Thread.sleep(300);

        assertTrue(world.balloonField().balloons().isEmpty());
        assertTrue(world.chatLog().messages().isEmpty());
    }

    @Test
    void 시작한_뒤_게임_화면을_띄우기_전에_온_푸시는_모아_두었다가_띄울_때_차례대로_넣는다() throws Exception {
        server.push("{\"type\":\"GAME_STARTED\",\"body\":{\"mapId\":\"camp/camp08\",\"players\":[]}}");
        // 시작할 때 상대가 이미 끊겨 있으면 서버는 GAME_STARTED 바로 뒤에 이렇게 보낸다
        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[20],\"reason\":\"DISCONNECT\"}}");
        server.push("{\"type\":\"GAME_OVER\",\"body\":{\"reason\":\"LAST_TEAM\",\"standings\":["
                + "{\"playerId\":12,\"nickname\":\"부기\",\"outcome\":\"WIN\",\"kills\":0,\"saves\":0},"
                + "{\"playerId\":20,\"nickname\":\"냥이\",\"outcome\":\"LOSE\",\"kills\":0,\"saves\":0}]}}");
        Thread.sleep(300);
        assertEquals(PlayerState.NORMAL, player(20).state(), "띄우기 전에는 넣지 않는다");
        assertTrue(results.isEmpty());

        bind();

        assertEquals(PlayerState.GONE, player(20).state());
        assertEquals(1, results.size());
        assertEquals(GameResult.Outcome.WIN, results.getFirst().standings().getFirst().outcome());
    }

    @Test
    void 경기를_기다리지_않을_때_온_푸시는_띄워도_넣지_않는다() throws Exception {
        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[20],\"reason\":\"DISCONNECT\"}}");
        Thread.sleep(300);

        bind();

        assertEquals(PlayerState.NORMAL, player(20).state());
    }

    @Test
    void 이_컴퓨터의_플레이어가_빠지면_화면에_알린다() throws Exception {
        bind();

        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[20],\"reason\":\"EXIT\"}}");
        until(() -> player(20).state() == PlayerState.GONE);
        assertEquals(0, kicks.get(), "다른 사람이 나간 것은 알리지 않는다");

        server.push("{\"type\":\"PLAYER_LEFT\",\"body\":{\"playerIds\":[12],\"reason\":\"DISCONNECT\"}}");
        until(() -> kicks.get() == 1);
    }

    @Test
    void 보내는_메시지는_서버_명세의_필드_이름을_쓴다() throws Exception {
        link.move(12, 60.5, 61.25, Direction.UP, true);
        JsonNode move = server.next(ServerMatchLink.MOVE).path("body");
        assertEquals(12, move.path("playerId").asLong());
        assertEquals(60.5, move.path("x").asDouble());
        assertEquals(61.25, move.path("y").asDouble());
        assertEquals("UP", move.path("facing").asText());
        assertTrue(move.path("moving").asBoolean());

        link.placeBalloon(12, 1, 1);
        JsonNode balloon = server.next(ServerMatchLink.BALLOON_PLACE).path("body");
        assertEquals(List.of(12, 1, 1), List.of(balloon.path("playerId").asInt(), balloon.path("col").asInt(),
                balloon.path("row").asInt()));

        link.pickUpItem(12, 7);
        JsonNode pickup = server.next(ServerMatchLink.ITEM_PICKUP).path("body");
        assertEquals(List.of(12, 7), List.of(pickup.path("playerId").asInt(), pickup.path("itemId").asInt()));

        link.useItem(12, "spring_shoe");
        JsonNode use = server.next(ServerMatchLink.ITEM_USE).path("body");
        assertEquals(12, use.path("playerId").asInt());
        assertEquals("spring_shoe", use.path("item").asText());

        link.pushBox(12, 6, 7, Direction.RIGHT);
        JsonNode push = server.next(ServerMatchLink.BOX_PUSH).path("body");
        assertEquals(List.of(12, 6, 7), List.of(push.path("playerId").asInt(), push.path("col").asInt(),
                push.path("row").asInt()));
        assertEquals("RIGHT", push.path("direction").asText());

        link.throwBalloon(12, 1, 2, Direction.DOWN);
        JsonNode thrown = server.next(ServerMatchLink.BALLOON_THROW).path("body");
        assertEquals(List.of(12, 1, 2), List.of(thrown.path("playerId").asInt(), thrown.path("col").asInt(),
                thrown.path("row").asInt()));
        assertEquals("DOWN", thrown.path("direction").asText());

        link.kickBalloon(12, 3, 4, Direction.LEFT);
        JsonNode kicked = server.next(ServerMatchLink.BALLOON_KICK).path("body");
        assertEquals(List.of(12, 3, 4), List.of(kicked.path("playerId").asInt(), kicked.path("col").asInt(),
                kicked.path("row").asInt()));
        assertEquals("LEFT", kicked.path("direction").asText());

        link.chat(12, "안녕");
        JsonNode chat = server.next(ServerMatchLink.CHAT).path("body");
        assertEquals("안녕", chat.path("text").asText());

    }
}
