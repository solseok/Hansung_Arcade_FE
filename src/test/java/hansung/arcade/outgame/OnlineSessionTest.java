package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.network.FakeServer;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerConnection;
import hansung.arcade.outgame.lobby.Room;
import hansung.arcade.outgame.lobby.ServerLobbyService;
import hansung.arcade.outgame.lobby.SlotState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 온라인 대기실: 자리·방장·준비·내 선택은 서버의 방 상태를 따르고, 고르기·준비는 서버로 보낸다 */
class OnlineSessionTest {

    /** 2P(12, 13)가 방장이고 손님(20)이 랜덤 캐릭터로 준비한 방 */
    private static final String HOST_STATE = """
            {"room":{"number":3,"title":"한 판","capacity":8,"mapId":"random","status":"WAITING"},"hostId":12,
             "players":[{"playerId":12,"nickname":"상상부기","slot":0,"character":"kkukku","team":"MINT","ready":false},
                        {"playerId":13,"nickname":"꾸꾸","slot":1,"character":"kkukku","team":"MINT","ready":false},
                        {"playerId":20,"nickname":"손님","slot":4,"character":"random","team":"BLUE","ready":%s}]}"""
            .replaceAll("\\n\\s*", "");

    private FakeServer server;
    private ServerClient client;
    private OutgameSession session;
    /** 이 컴퓨터에 기억한 값. 기본은 HOST_STATE의 1P·2P 자리와 같게 두어 방에 들어가도 맞춰 달라고 보내지 않는다 */
    private final LocalPicks picks = LocalPicks.inMemory();

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(3_000, 5_000, 5_000, 15_000)));
        for (int local = 0; local < 2; local++) {
            picks.saveCharacter(local, "kkukku");
            picks.saveTeam(local, Team.MINT);
        }
        session = new OutgameSession(new ServerLobbyService(client, Runnable::run), picks);
        session.signedIn(List.of(new LoginService.LoginPlayer(12, "상상부기"), new LoginService.LoginPlayer(13, "꾸꾸")));
        client.connect().get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    private void enterAsHost(boolean guestReady) {
        server.respond(request -> FakeServer.ok(request, HOST_STATE.formatted(guestReady)));
        session.enter(session.create("한 판", 8));
    }

    @Test
    void 자리는_서버의_자리_번호대로_보이고_방장과_같은_컴퓨터의_2P는_준비로_보인다() {
        enterAsHost(true);

        assertTrue(session.isOnline());
        assertTrue(session.isHost());
        List<OutgameSession.SeatView> seats = session.seats();
        assertEquals(List.of(0, 1, 4), seats.stream().map(OutgameSession.SeatView::slot).toList());
        assertEquals(SlotState.HOST, seats.get(0).state());
        assertEquals(SlotState.READY, seats.get(1).state());
        assertEquals(SlotState.READY, seats.get(2).state());
        assertNull(seats.get(2).character(), "랜덤을 고른 손님은 ?로 보인다");
        assertEquals(Team.BLUE, seats.get(2).team());
        assertEquals(new Room(3, "한 판", 3, 8, "랜덤 맵", Room.Status.WAITING), session.room());
        assertEquals("random", session.mapId());
    }

    @Test
    void 내_캐릭터와_팀은_서버가_알려_준_내_자리를_따른다() {
        enterAsHost(true);

        assertEquals(Mascot.KKUKKU, session.character());
        assertFalse(session.randomCharacter());
        assertEquals(Team.MINT, session.team());
    }

    @Test
    void 다른_사람이_모두_준비해야_시작할_수_있다() {
        enterAsHost(false);
        assertFalse(session.canStart());

        session.online().orElseThrow();
        server.respond(request -> null);
        session.applyRoomState(hansung.arcade.outgame.lobby.RoomState.from(read(HOST_STATE.formatted(true))));

        assertTrue(session.canStart());
    }

    @Test
    void 혼자_남으면_시작할_수_없다() {
        server.respond(request -> FakeServer.ok(request, """
                {"room":{"number":3,"title":"한 판","capacity":8,"mapId":"random","status":"WAITING"},"hostId":12,
                 "players":[{"playerId":12,"nickname":"상상부기","slot":0,"character":"kkukku","team":"MINT","ready":false}]}"""
                .replaceAll("\\n\\s*", "")));
        session.enter(session.create("한 판", 8));

        assertFalse(session.canStart());
    }

    @Test
    void 방장이_빈_자리를_누르면_CHANGE_SLOT을_보내고_닫힌_자리는_ROOM_STATE대로_보인다() throws Exception {
        enterAsHost(true);
        server.next("ROOM_CREATE");
        server.respond(request -> FakeServer.ok(request, "{}"));

        assertTrue(session.toggleSlot(5));
        var close = server.next("CHANGE_SLOT").path("body");
        assertEquals(5, close.path("slot").asInt());
        assertFalse(close.path("open").asBoolean());
        assertFalse(session.isSlotClosed(5), "닫힌 자리는 서버가 알려 줄 때 바뀐다");

        session.applyRoomState(hansung.arcade.outgame.lobby.RoomState.from(read(HOST_STATE.formatted(true)
                .replace("\"status\":\"WAITING\"}", "\"status\":\"WAITING\",\"closedSlots\":[5]}"))));
        assertTrue(session.isSlotClosed(5));
        assertTrue(session.toggleSlot(5));
        assertTrue(server.next("CHANGE_SLOT").path("body").path("open").asBoolean());

        assertFalse(session.toggleSlot(4), "손님이 앉은 자리는 누르지 않는다");
        assertFalse(session.toggleSlot(1), "2P가 앉은 자리도 그대로다");
    }

    @Test
    void 손님은_자리를_눌러도_보내지_않는다() throws Exception {
        server.respond(request -> FakeServer.ok(request, HOST_STATE.formatted(true).replace("\"hostId\":12",
                "\"hostId\":20")));
        session.enter(session.join(3));
        server.next("ROOM_JOIN");

        assertFalse(session.toggleSlot(5));
        session.chat("확인");
        assertEquals("CHAT", server.nextMessage().path("type").asText(), "CHANGE_SLOT은 나가지 않았다");
    }

    @Test
    void 고르기는_서버로_보내고_방장은_READY를_보내지_않는다() throws Exception {
        enterAsHost(true);

        server.next("ROOM_CREATE");
        session.select(Mascot.KKOKKO);
        session.selectRandomCharacter();
        session.selectTeam(Team.PINK);
        session.setReady(true);
        session.chat("안녕");

        // 다른 type을 건너뛰지 않고 차례대로 본다: 방장의 READY는 나가지 않는다
        assertEquals("kkokko", server.nextMessage().path("body").path("character").asText());
        assertEquals("random", server.nextMessage().path("body").path("character").asText());
        assertEquals("PINK", server.nextMessage().path("body").path("team").asText());
        assertEquals("CHAT", server.nextMessage().path("type").asText());
    }

    @Test
    void 이P가_고른_것은_2P_번호로_보내고_2P_자리는_서버_값을_따른다() throws Exception {
        enterAsHost(true);

        session.select(1, Mascot.SANGZZI);
        session.selectTeam(1, Team.YELLOW);

        var character = server.next("SELECT_CHARACTER").path("body");
        assertEquals(13, character.path("playerId").asLong());
        assertEquals("sangzzi", character.path("character").asText());
        assertEquals(13, server.next("SELECT_TEAM").path("body").path("playerId").asLong());
        // 서버가 다른 값을 돌려주면 세션은 서버 값을 따른다
        session.applyRoomState(hansung.arcade.outgame.lobby.RoomState.from(read(HOST_STATE.formatted(true))));
        assertEquals(Mascot.KKUKKU, session.character(1));
        assertEquals(Team.MINT, session.team(1));
    }

    @Test
    void 방에_들어가면_기억한_값과_다른_자리만_playerId를_붙여_맞춰_달라고_보낸다() throws Exception {
        picks.saveCharacter(1, "sangzzi");
        picks.saveTeam(1, Team.YELLOW);

        enterAsHost(true);

        assertEquals("ROOM_CREATE", server.nextMessage().path("type").asText());
        // 1P는 서버 자리(kkukku/MINT)와 같아 보내지 않는다
        var character = server.nextMessage();
        assertEquals("SELECT_CHARACTER", character.path("type").asText());
        assertEquals(13, character.path("body").path("playerId").asLong());
        assertEquals("sangzzi", character.path("body").path("character").asText());
        var team = server.nextMessage();
        assertEquals("SELECT_TEAM", team.path("type").asText());
        assertEquals(13, team.path("body").path("playerId").asLong());
        assertEquals("YELLOW", team.path("body").path("team").asText());
        // 방 만들기에 이어 대기실 화면이 같은 방으로 또 들어와도 다시 보내지 않는다
        session.chat("다음");
        assertEquals("CHAT", server.nextMessage().path("type").asText());
        assertEquals(Mascot.KKUKKU, session.character(0));
        assertEquals(Mascot.SANGZZI, session.character(1));
        assertEquals(Team.YELLOW, session.team(1));
    }

    @Test
    void 서버_값을_따라도_기억은_내가_고른_값이고_나갔다_다시_들어오면_다시_맞춘다() throws Exception {
        enterAsHost(true);
        server.next("ROOM_CREATE");
        session.select(1, Mascot.SANGZZI);
        server.next("SELECT_CHARACTER");

        // 서버가 예전 값(kkukku)을 알려 와도 화면만 따르고 기억은 그대로다
        session.applyRoomState(hansung.arcade.outgame.lobby.RoomState.from(read(HOST_STATE.formatted(true))));
        assertEquals(Mascot.KKUKKU, session.character(1));
        assertEquals(java.util.Optional.of("sangzzi"), picks.character(1));

        session.leave();
        session.enter(session.join(3));

        server.next("ROOM_JOIN");
        var character = server.nextMessage();
        assertEquals("SELECT_CHARACTER", character.path("type").asText());
        assertEquals(13, character.path("body").path("playerId").asLong());
        assertEquals("sangzzi", character.path("body").path("character").asText());
        assertEquals(Mascot.SANGZZI, session.character(1));
    }

    @Test
    void 기억한_랜덤도_방에_들어가면_맞춰_달라고_보낸다() throws Exception {
        picks.saveCharacter(0, "random");

        enterAsHost(true);

        server.next("ROOM_CREATE");
        var character = server.nextMessage();
        assertEquals(12, character.path("body").path("playerId").asLong());
        assertEquals("random", character.path("body").path("character").asText());
        assertTrue(session.randomCharacter(0));
    }

    @Test
    void 고른_적이_없으면_1P_2P_모두_상상부기이고_팀은_1P_빨강_2P_파랑으로_맞춘다() throws Exception {
        session = new OutgameSession(new ServerLobbyService(client, Runnable::run));
        session.signedIn(List.of(new LoginService.LoginPlayer(12, "상상부기"), new LoginService.LoginPlayer(13, "꾸꾸")));

        enterAsHost(true);

        server.next("ROOM_CREATE");
        List<String> sent = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var message = server.nextMessage();
            var body = message.path("body");
            sent.add(body.path("playerId").asLong() + ":" + body.path(
                    message.path("type").asText().equals("SELECT_CHARACTER") ? "character" : "team").asText());
        }
        assertEquals(List.of("12:sangsangbugi", "12:RED", "13:sangsangbugi", "13:BLUE"), sent);
    }

    @Test
    void 온라인에서는_고르기만으로_준비를_풀지_않고_서버_값을_따른다() {
        server.respond(request -> FakeServer.ok(request, HOST_STATE.formatted(true)
                .replace("\"hostId\":12", "\"hostId\":20")
                .replace("\"slot\":0,\"character\":\"kkukku\",\"team\":\"MINT\",\"ready\":false",
                        "\"slot\":0,\"character\":\"kkukku\",\"team\":\"MINT\",\"ready\":true")));
        session.enter(session.join(3));
        assertTrue(session.ready());

        session.select(Mascot.KKUKKU);

        assertTrue(session.ready(), "같은 캐릭터를 다시 골라도 서버는 준비를 그대로 둔다");
    }

    @Test
    void 나가기가_거절돼도_방을_떠난_상태가_된다() throws Exception {
        enterAsHost(true);
        server.respond(request -> FakeServer.fail(request, "WRONG_STATE", "방에 있지 않습니다."));

        IllegalStateException failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                session::leave);

        assertEquals("방에 있지 않습니다.", failure.getMessage());
        assertNull(session.room());
        assertTrue(session.seats().isEmpty());
    }

    @Test
    void 게임에서_나가면_GAME_EXIT를_보내고_방을_떠난다() throws Exception {
        enterAsHost(true);

        session.exitGame();

        server.next("GAME_EXIT");
        assertNull(session.room());
    }

    @Test
    void 모르는_맵으로_시작하면_열지_않는다() {
        enterAsHost(true);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> session.startInfo(
                new ServerLobbyService.GameStarted("moon/moon01",
                        List.of(new ServerLobbyService.StartedPlayer(12, "상상부기", "kkukku", "MINT", 0)))));
    }

    @Test
    void 게임_시작_정보는_이_컴퓨터의_플레이어를_앞에_둔다() {
        enterAsHost(true);

        GameStartInfo info = session.startInfo(new ServerLobbyService.GameStarted("village/village10", List.of(
                new ServerLobbyService.StartedPlayer(20, "손님", "sangzzi", "BLUE", 0),
                new ServerLobbyService.StartedPlayer(13, "꾸꾸", "kkukku", "MINT", 1),
                new ServerLobbyService.StartedPlayer(12, "상상부기", "hansungnyangi", "MINT", 2))));

        assertEquals("village/village10", info.mapId());
        assertEquals(2, info.localPlayers());
        assertEquals(List.of(12, 13, 20), info.players().stream().map(GameStartInfo.PlayerInfo::id).toList());
        assertEquals(Mascot.HANSUNGNYANGI, info.players().get(0).character(), "랜덤은 서버가 정한 캐릭터로 온다");
        assertEquals("청록", info.players().get(0).team());
    }

    private static com.fasterxml.jackson.databind.JsonNode read(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
