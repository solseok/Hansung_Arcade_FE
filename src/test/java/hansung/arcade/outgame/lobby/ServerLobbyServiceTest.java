package hansung.arcade.outgame.lobby;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.network.FakeServer;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerConnection;
import hansung.arcade.outgame.GameResult;
import hansung.arcade.outgame.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 가짜 서버로 방 API의 요청 모양과 응답·푸시 반영을 본다. 푸시는 받는 스레드에서 바로 처리한다 */
class ServerLobbyServiceTest {

    static final String STATE = """
            {"room":{"number":3,"title":"한 판","capacity":6,"mapId":"village/village10","status":"WAITING"},
             "hostId":12,
             "players":[{"playerId":20,"nickname":"꾸꾸","slot":2,"character":"random","team":"BLUE","ready":true},
                        {"playerId":12,"nickname":"상상부기","slot":0,"character":"sangsangbugi","team":"RED","ready":false}]}""";
    static final String ROOMS = """
            {"rooms":[{"number":1,"title":"대기방","players":2,"capacity":8,"mapId":"camp/camp08","status":"WAITING"},
                      {"number":2,"title":"게임방","players":4,"capacity":6,"mapId":"random","status":"PLAYING"}]}""";

    private FakeServer server;
    private ServerClient client;
    private ServerLobbyService lobby;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(3_000, 5_000, 5_000, 15_000)));
        lobby = new ServerLobbyService(client, Runnable::run);
        lobby.signedIn(List.of(12L));
        client.connect().get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    private static String compact(String json) {
        return json.replaceAll("\\n\\s*", "");
    }

    private void until(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 60 && !condition.getAsBoolean(); i++) {
            Thread.sleep(50);
        }
        assertTrue(condition.getAsBoolean(), "기다린 일이 일어나지 않았습니다.");
    }

    @Test
    void 방_상태_body를_읽는다() throws Exception {
        RoomState state = RoomState.from(new com.fasterxml.jackson.databind.ObjectMapper().readTree(STATE));

        assertEquals(new Room(3, "한 판", 2, 6, "빌리지 10", Room.Status.WAITING), state.room());
        assertEquals("village/village10", state.mapId());
        assertEquals(12, state.hostId());
        assertEquals(List.of(0, 2), state.seats().stream().map(RoomState.Seat::slot).toList(), "자리 번호 순서");
        RoomState.Seat guest = state.seatOf(20);
        assertTrue(guest.randomCharacter());
        assertNull(guest.mascot());
        assertEquals(Team.BLUE, guest.team());
        assertTrue(guest.ready());
    }

    @Test
    void 방_상태의_닫힌_자리를_읽고_없으면_정원_이상인_자리로_본다() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        RoomState old = RoomState.from(mapper.readTree(STATE));
        RoomState closed = RoomState.from(mapper.readTree(STATE.replace("\"status\":\"WAITING\"}",
                "\"status\":\"WAITING\",\"closedSlots\":[7,1,6]}").replace("\"capacity\":6", "\"capacity\":5")));

        assertEquals(List.of(6, 7), old.closedSlots(), "closedSlots가 없는 서버");
        assertEquals(List.of(1, 6, 7), closed.closedSlots());
        assertTrue(closed.isClosed(1));
        assertFalse(closed.isClosed(2));
        assertEquals(5, closed.room().capacity());
        assertEquals(List.of(1, 6, 7), closed.withStatus(Room.Status.PLAYING).closedSlots());
    }

    @Test
    void 방장이_자리를_누르면_CHANGE_SLOT을_보내고_닫힌_자리는_방_상태를_따른다() throws Exception {
        server.respond(request -> FakeServer.ok(request, "CHANGE_SLOT".equals(request.path("type").asText())
                ? "{}" : compact(STATE)));
        Room room = lobby.create("한 판", 6, 1);
        assertEquals(List.of(6, 7), lobby.closedSlots(room));

        lobby.setSlotOpen(3, false);
        lobby.setSlotOpen(6, true);

        JsonNode close = server.next("CHANGE_SLOT").path("body");
        assertEquals(3, close.path("slot").asInt());
        assertFalse(close.path("open").asBoolean());
        assertTrue(close.path("open").isBoolean());
        JsonNode open = server.next("CHANGE_SLOT").path("body");
        assertEquals(6, open.path("slot").asInt());
        assertTrue(open.path("open").asBoolean());

        server.push("{\"type\":\"ROOM_STATE\",\"body\":" + compact(STATE.replace("\"status\":\"WAITING\"}",
                "\"status\":\"WAITING\",\"closedSlots\":[3,7]}")) + "}");
        until(() -> lobby.state().isClosed(3));
        assertEquals(List.of(3, 7), lobby.closedSlots(room));
        assertEquals(List.of(4, 5, 6, 7), lobby.closedSlots(new Room(9, "다른 방", 1, 4, "캠프 08", Room.Status.WAITING)),
                "들어가 있지 않은 방은 정원으로 센다");
    }

    @Test
    void 자리_바꾸기를_서버가_거절하면_서버_문구로_실패한다() throws Exception {
        server.respond(request -> "CHANGE_SLOT".equals(request.path("type").asText())
                ? FakeServer.fail(request, "SLOT_OCCUPIED", "사람이 있는 자리는 닫을 수 없습니다.")
                : FakeServer.ok(request, compact(STATE)));
        lobby.create("한 판", 6, 1);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> lobby.setSlotOpen(2, false));

        assertEquals("사람이 있는 자리는 닫을 수 없습니다.", failure.getMessage());
        lobby.leave();
        assertThrows(IllegalStateException.class, () -> lobby.setSlotOpen(2, false), "방 밖에서는 보내지 않는다");
    }

    @Test
    void 로비에_들어갈_때_ROOM_LIST로_목록을_받는다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(ROOMS)));

        lobby.reload();

        assertEquals(List.of(
                new Room(1, "대기방", 2, 8, "캠프 08", Room.Status.WAITING),
                new Room(2, "게임방", 4, 6, "랜덤 맵", Room.Status.PLAYING)), lobby.rooms());
    }

    @Test
    void 서버가_보내는_LOBBY_ROOMS로_목록을_바꾸고_알린다() throws Exception {
        CompletableFuture<Void> changed = new CompletableFuture<>();
        lobby.listen(new ServerLobbyService.Listener() {
            @Override
            public void roomsChanged() {
                changed.complete(null);
            }
        });

        server.push("{\"type\":\"LOBBY_ROOMS\",\"body\":" + compact(ROOMS) + "}");

        changed.get(3, TimeUnit.SECONDS);
        assertEquals(2, lobby.rooms().size());
    }

    @Test
    void 방을_만들면_응답의_방_상태로_들어가고_방장이다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));

        Room room = lobby.create("한 판", 6, 1);

        JsonNode sent = server.next("ROOM_CREATE");
        assertEquals("한 판", sent.path("body").path("title").asText());
        assertEquals(6, sent.path("body").path("capacity").asInt());
        assertEquals(3, room.number());
        assertTrue(lobby.isHost());
    }

    @Test
    void 입장과_바로시작은_방_번호와_빈_body를_보낸다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));

        lobby.join(3, 1);
        assertEquals(3, server.next("ROOM_JOIN").path("body").path("roomNumber").asInt());
        lobby.leave();
        server.next("ROOM_LEAVE");
        assertNull(lobby.state());

        lobby.quickJoin(1);
        assertTrue(server.next("ROOM_QUICK_JOIN").path("body").isEmpty());
    }

    @Test
    void 서버가_거절하면_서버_문구로_실패한다() {
        server.respond(request -> FakeServer.fail(request, "ROOM_FULL", "자리가 없습니다."));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> lobby.join(3, 1));

        assertEquals("자리가 없습니다.", failure.getMessage());
        assertNull(lobby.state());
    }

    @Test
    void 들어간_방의_ROOM_STATE만_반영한다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));
        List<RoomState> seen = new CopyOnWriteArrayList<>();
        lobby.listen(new ServerLobbyService.Listener() {
            @Override
            public void roomStateChanged(RoomState state) {
                seen.add(state);
            }
        });
        lobby.join(3, 1);

        server.push("{\"type\":\"ROOM_STATE\",\"body\":" + compact(STATE.replace("\"number\":3", "\"number\":9")) + "}");
        server.push("{\"type\":\"ROOM_STATE\",\"body\":" + compact(STATE.replace("\"hostId\":12", "\"hostId\":20")) + "}");

        until(() -> !seen.isEmpty());
        Thread.sleep(100);
        assertEquals(1, seen.size(), "다른 방(9번) 상태는 버린다");
        assertEquals(20, lobby.state().hostId());
        assertFalse(lobby.isHost(), "방장이 넘어갔다");
    }

    @Test
    void 대기실에서_고르기_준비_채팅은_응답_없이_보낸다() throws Exception {
        lobby.selectCharacter(12, "kkukku");
        lobby.selectCharacter(13, RoomState.RANDOM_CHARACTER);
        lobby.selectTeam(13, Team.MINT);
        lobby.ready(true);
        lobby.chat(12, "안녕");
        lobby.loaded();

        JsonNode character = server.next("SELECT_CHARACTER").path("body");
        assertEquals("kkukku", character.path("character").asText());
        assertEquals(12, character.path("playerId").asLong());
        assertEquals("random", server.next("SELECT_CHARACTER").path("body").path("character").asText());
        JsonNode team = server.next("SELECT_TEAM").path("body");
        assertEquals("MINT", team.path("team").asText());
        assertEquals(13, team.path("playerId").asLong(), "2P가 고른 것은 2P 번호로 보낸다");
        JsonNode ready = server.next("READY");
        assertTrue(ready.path("body").path("ready").asBoolean());
        assertFalse(ready.has("id"));
        JsonNode chat = server.next("CHAT");
        assertEquals(12, chat.path("body").path("playerId").asLong());
        assertEquals("안녕", chat.path("body").path("text").asText());
        server.next("LOADED");
    }

    @Test
    void 방장이_맵을_바꾸면_CHANGE_MAP을_보내고_같은_맵이면_보내지_않는다() throws Exception {
        server.respond(request -> FakeServer.ok(request, "CHANGE_MAP".equals(request.path("type").asText())
                ? "{}" : compact(STATE)));
        lobby.create("한 판", 6, 1);

        lobby.updateRoom("캠프 08", Room.Status.WAITING);
        lobby.updateRoom("빌리지 10", Room.Status.WAITING);

        assertEquals("camp/camp08", server.next("CHANGE_MAP").path("body").path("mapId").asText());
        Thread.sleep(100);
        lobby.updateRoom("랜덤 맵", Room.Status.WAITING);
        assertEquals("random", server.next("CHANGE_MAP").path("body").path("mapId").asText());
    }

    @Test
    void 채팅_게임_시작_결과_오류_푸시를_알린다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));
        lobby.join(3, 1);
        List<Object> events = new CopyOnWriteArrayList<>();
        lobby.listen(new ServerLobbyService.Listener() {
            @Override
            public void chatReceived(ServerLobbyService.ChatLine line) {
                events.add(line);
            }

            @Override
            public void gameStarted(ServerLobbyService.GameStarted started) {
                events.add(started);
            }

            @Override
            public void gameOver(GameResult result) {
                events.add(result);
            }

            @Override
            public void serverError(String message) {
                events.add(message);
            }
        });

        server.push("{\"type\":\"CHAT_MESSAGE\",\"body\":{\"senderId\":0,\"sender\":\"\",\"text\":\"꾸꾸님이 들어왔습니다.\"}}");
        server.push("{\"type\":\"GAME_STARTED\",\"body\":{\"mapId\":\"camp/camp08\",\"players\":"
                + "[{\"playerId\":12,\"nickname\":\"상상부기\",\"character\":\"kkokko\",\"team\":\"RED\",\"spawn\":3}]}}");
        server.push("{\"type\":\"GAME_OVER\",\"body\":{\"reason\":\"LAST_TEAM\",\"standings\":"
                + "[{\"playerId\":12,\"nickname\":\"상상부기\",\"outcome\":\"WIN\",\"kills\":2,\"saves\":1}]}}");
        server.push("{\"type\":\"ERROR\",\"body\":{\"code\":\"WRONG_STATE\",\"message\":\"게임 중에는 할 수 없습니다.\",\"type\":\"READY\"}}");

        until(() -> events.size() == 4);
        ServerLobbyService.ChatLine notice = (ServerLobbyService.ChatLine) events.get(0);
        assertTrue(notice.notice());
        assertEquals("꾸꾸님이 들어왔습니다.", notice.text());
        ServerLobbyService.GameStarted started = (ServerLobbyService.GameStarted) events.get(1);
        assertEquals("kkokko", started.players().getFirst().character());
        assertEquals(3, started.players().getFirst().spawn());
        GameResult result = (GameResult) events.get(2);
        assertEquals(new GameResult.Standing(12, "상상부기", GameResult.Outcome.WIN, 2, 1), result.standings().getFirst());
        assertEquals("게임 중에는 할 수 없습니다.", events.get(3));
    }

    @Test
    void GAME_STARTED를_받으면_방을_게임_중으로_두고_먼저_알린다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));
        lobby.join(3, 1);
        List<String> order = new CopyOnWriteArrayList<>();
        lobby.listen(new ServerLobbyService.Listener() {
            @Override
            public void roomStateChanged(RoomState state) {
                order.add("state:" + state.room().status().name());
            }

            @Override
            public void gameStarted(ServerLobbyService.GameStarted started) {
                order.add("started");
            }
        });

        server.push("{\"type\":\"GAME_STARTED\",\"body\":{\"mapId\":\"camp/camp08\",\"players\":[]}}");

        until(() -> order.size() == 2);
        assertEquals(List.of("state:PLAYING", "started"), order);
        assertEquals(Room.Status.PLAYING, lobby.state().room().status());
    }

    @Test
    void 게임_중에_나가면_GAME_EXIT를_보낸다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE.replace("WAITING", "PLAYING"))));
        lobby.join(3, 1);

        lobby.leave();

        server.next("GAME_EXIT");
        assertNull(lobby.state());
    }

    @Test
    void 이미_방에_있다고_거절되면_나간_뒤_한_번_다시_들어간다() throws Exception {
        java.util.concurrent.atomic.AtomicInteger joins = new java.util.concurrent.atomic.AtomicInteger();
        server.respond(request -> switch (request.path("type").asText()) {
            case "ROOM_JOIN" -> joins.incrementAndGet() == 1
                    ? FakeServer.fail(request, "ALREADY_IN_ROOM", "이미 방에 입장해 있습니다.")
                    : FakeServer.ok(request, compact(STATE));
            case "ROOM_LEAVE" -> FakeServer.ok(request, "{}");
            default -> null;
        });

        Room room = lobby.join(3, 1);

        assertEquals(3, room.number());
        server.next("ROOM_JOIN");
        server.next("ROOM_LEAVE");
        server.next("ROOM_JOIN");
    }

    @Test
    void 목록을_받을_때_서버가_아직_방에_두고_있으면_빼낸_뒤_다시_받는다() throws Exception {
        java.util.concurrent.atomic.AtomicInteger lists = new java.util.concurrent.atomic.AtomicInteger();
        server.respond(request -> switch (request.path("type").asText()) {
            case "ROOM_LIST" -> lists.incrementAndGet() == 1
                    ? FakeServer.fail(request, "WRONG_STATE", "방 안에서는 목록을 볼 수 없습니다.")
                    : FakeServer.ok(request, compact(ROOMS));
            case "ROOM_LEAVE" -> FakeServer.ok(request, "{}");
            default -> null;
        });

        lobby.reload();

        assertEquals(2, lobby.rooms().size());
        server.next("ROOM_LEAVE");
    }

    @Test
    void 입장_응답이_늦으면_나가기를_보내_두고_실패한다() throws Exception {
        ServerClient slow = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(3_000, 200, 5_000, 15_000)));
        try {
            ServerLobbyService slowLobby = new ServerLobbyService(slow, Runnable::run);
            slow.connect().get(3, TimeUnit.SECONDS);

            assertThrows(IllegalStateException.class, () -> slowLobby.join(3, 1));

            JsonNode leave = server.next("ROOM_LEAVE");
            assertFalse(leave.has("id"), "응답을 기다리지 않는 나가기");
            assertNull(slowLobby.state());
        } finally {
            slow.close();
        }
    }

    @Test
    void 끊기면_목록과_방을_잊는다() throws Exception {
        server.respond(request -> FakeServer.ok(request, compact(STATE)));
        lobby.join(3, 1);

        server.dropClients();

        until(() -> lobby.state() == null);
        assertTrue(lobby.rooms().isEmpty());
    }
}
