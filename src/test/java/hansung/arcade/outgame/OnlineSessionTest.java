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

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(1_000, 1_000, 5_000, 15_000)));
        session = new OutgameSession(new ServerLobbyService(client, Runnable::run));
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
    void 고르기는_서버로_보내고_방장은_READY를_보내지_않는다() throws Exception {
        enterAsHost(true);

        session.select(Mascot.KKOKKO);
        session.selectRandomCharacter();
        session.selectTeam(Team.PINK);
        session.setReady(true);

        assertEquals("kkokko", server.next("SELECT_CHARACTER").path("body").path("character").asText());
        assertEquals("random", server.next("SELECT_CHARACTER").path("body").path("character").asText());
        assertEquals("PINK", server.next("SELECT_TEAM").path("body").path("team").asText());
        session.chat("안녕");
        assertEquals("안녕", server.next("CHAT").path("body").path("text").asText(), "READY는 건너뛰고 CHAT이 온다");
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
        // 지금 서버는 연결 단위로 적용해 1P·2P가 같은 값으로 돌아온다. 세션은 서버가 돌려준 값을 따른다
        session.applyRoomState(hansung.arcade.outgame.lobby.RoomState.from(read(HOST_STATE.formatted(true))));
        assertEquals(Mascot.KKUKKU, session.character(1));
        assertEquals(Team.MINT, session.team(1));
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
