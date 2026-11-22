package hansung.arcade.outgame.lobby;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.network.Message;
import hansung.arcade.network.Protocol;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerException;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.GameResult;
import hansung.arcade.outgame.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 로비·대기실을 서버의 방 API로 한다 (온라인 모드). 목업({@link MockLobbyService}) 자리에 끼운다.
 *
 * <ul>
 *   <li>요청 → 응답: ROOM_LIST, ROOM_CREATE, ROOM_JOIN, ROOM_QUICK_JOIN, ROOM_LEAVE, CHANGE_MAP, GAME_START.
 *       응답을 기다리는 동안 부른 스레드(EDT)가 멈춘다. 서버가 거절하면 서버 문구를 담은 {@link IllegalStateException}을 던진다</li>
 *   <li>응답 없음: SELECT_CHARACTER, SELECT_TEAM, READY, CHAT, LOADED. 실패는 ERROR 푸시로 온다</li>
 *   <li>푸시: LOBBY_ROOMS(방 목록 전체), ROOM_STATE, CHAT_MESSAGE, GAME_STARTED, GAME_OVER, ERROR.
 *       받는 스레드에서 오므로 {@code ui}(EDT)로 넘겨 상태를 바꾸고 {@link Listener}에 알린다</li>
 * </ul>
 *
 * <p>상태(방 목록, 지금 방)는 EDT에서만 바꾸고 읽는다. 응답이 푸시보다 먼저 오고 푸시는 EDT 큐에 차례로 쌓이므로,
 * 요청을 마친 뒤에 그 요청이 일으킨 푸시가 반영된다. 로그인 직후에는 서버가 목록을 먼저 보내지 않으므로
 * 로비에 들어갈 때 {@link #reload()}로 받는다.</p>
 */
public final class ServerLobbyService implements LobbyService {

    public static final String ROOM_LIST = "ROOM_LIST";
    public static final String ROOM_CREATE = "ROOM_CREATE";
    public static final String ROOM_JOIN = "ROOM_JOIN";
    public static final String ROOM_QUICK_JOIN = "ROOM_QUICK_JOIN";
    public static final String ROOM_LEAVE = "ROOM_LEAVE";
    public static final String CHANGE_MAP = "CHANGE_MAP";
    public static final String GAME_START = "GAME_START";
    public static final String SELECT_CHARACTER = "SELECT_CHARACTER";
    public static final String SELECT_TEAM = "SELECT_TEAM";
    public static final String READY = "READY";
    public static final String CHAT = "CHAT";
    public static final String LOADED = "LOADED";
    public static final String LOBBY_ROOMS = "LOBBY_ROOMS";
    public static final String ROOM_STATE = "ROOM_STATE";
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String GAME_OVER = "GAME_OVER";

    private static final Logger log = LoggerFactory.getLogger(ServerLobbyService.class);

    /** 서버에서 온 일을 화면에 알린다. 모두 EDT에서 불린다 */
    public interface Listener {
        /** 로비 목록이 바뀌었다 (LOBBY_ROOMS) */
        default void roomsChanged() {
        }

        /** 지금 방의 상태가 바뀌었다 (ROOM_STATE) */
        default void roomStateChanged(RoomState state) {
        }

        /** 방 채팅 (CHAT_MESSAGE). 입장·퇴장·방장 안내는 {@link ChatLine#notice()} */
        default void chatReceived(ChatLine line) {
        }

        /** 경기가 열렸다 (GAME_STARTED) */
        default void gameStarted(GameStarted started) {
        }

        /** 경기가 끝났다 (GAME_OVER). 이어서 ROOM_STATE(대기 중)가 온다 */
        default void gameOver(GameResult result) {
        }

        /** 응답 없는 메시지(준비·고르기·채팅)가 거절됐다 (ERROR) */
        default void serverError(String message) {
        }
    }

    /** @param senderId 0이면 서버 안내 (sender는 빈 글자) */
    public record ChatLine(long senderId, String sender, String text) {
        public boolean notice() {
            return senderId == 0;
        }
    }

    /** GAME_STARTED body. 캐릭터·맵의 랜덤은 서버가 실제 값으로 정해 보낸다 */
    public record GameStarted(String mapId, List<StartedPlayer> players) {
    }

    public record StartedPlayer(long playerId, String nickname, String character, String team, int spawn) {
    }

    private record StandingBody(long playerId, String nickname, String outcome, int kills, int saves) {
    }

    private record GameOverBody(String reason, List<StandingBody> standings) {
    }

    private final ServerClient client;
    private final Executor ui;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private List<Long> playerIds = List.of();
    private List<Room> rooms = List.of();
    private RoomState state;

    /**
     * @param ui 푸시를 넘길 곳. 게임에서는 {@code SwingUtilities::invokeLater}
     */
    public ServerLobbyService(ServerClient client, Executor ui) {
        this.client = Objects.requireNonNull(client);
        this.ui = Objects.requireNonNull(ui);
        onPush(LOBBY_ROOMS, body -> {
            rooms = RoomState.rooms(body);
            listeners.forEach(Listener::roomsChanged);
        });
        onPush(ROOM_STATE, body -> {
            RoomState next = RoomState.from(body);
            // 방을 나간 뒤 늦게 도착한 다른 방 상태는 버린다
            if (state == null || state.room().number() != next.room().number()) {
                log.debug("들어가 있지 않은 방의 상태를 버립니다: {}번", next.room().number());
                return;
            }
            state = next;
            listeners.forEach(listener -> listener.roomStateChanged(next));
        });
        onPush(CHAT_MESSAGE, body -> {
            if (state == null) {
                return;
            }
            ChatLine line = new ChatLine(body.path("senderId").asLong(), body.path("sender").asText(""),
                    body.path("text").asText(""));
            listeners.forEach(listener -> listener.chatReceived(line));
        });
        onPush(GAME_STARTED, body -> {
            GameStarted started = Message.read(body, GameStarted.class);
            listeners.forEach(listener -> listener.gameStarted(started));
        });
        onPush(GAME_OVER, body -> {
            GameResult result = result(Message.read(body, GameOverBody.class));
            listeners.forEach(listener -> listener.gameOver(result));
        });
        onPush(Protocol.ERROR, body -> {
            String message = body.path("message").asText("");
            if (!message.isBlank()) {
                listeners.forEach(listener -> listener.serverError(message));
            }
        });
        client.onDisconnected(reason -> ui.execute(this::forget));
    }

    private void onPush(String type, Consumer<JsonNode> handler) {
        client.onPush(type, body -> ui.execute(() -> {
            try {
                handler.accept(body);
            } catch (RuntimeException e) {
                log.error("{}를 처리하지 못했습니다: {}", type, body, e);
            }
        }));
    }

    /** 화면이 서버에서 온 일을 받는다. 먼저 등록한 것부터 불린다 */
    public void listen(Listener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void signedIn(List<Long> playerIds) {
        this.playerIds = List.copyOf(playerIds);
        forget();
    }

    /** 끊기거나 다시 로그인하면 들고 있던 목록과 방을 잊는다 */
    private void forget() {
        rooms = List.of();
        state = null;
    }

    /** ROOM_LIST로 목록을 새로 받는다 */
    @Override
    public void reload() {
        rooms = RoomState.rooms(call(ROOM_LIST, null));
    }

    @Override
    public List<Room> rooms() {
        return rooms;
    }

    /** 지금 들어가 있는 방의 상태. 방 밖이면 null */
    public RoomState state() {
        return state;
    }

    /** @param seats 쓰지 않는다. 함께 들어가는 인원은 서버가 로그인한 인원으로 안다 */
    @Override
    public Room join(int roomNumber, int seats) {
        return enter(call(ROOM_JOIN, Map.of("roomNumber", roomNumber)));
    }

    @Override
    public Room create(String title, int capacity, int seats) {
        return enter(call(ROOM_CREATE, Map.of("title", title, "capacity", capacity)));
    }

    @Override
    public Room quickJoin(int seats) {
        return enter(call(ROOM_QUICK_JOIN, null));
    }

    private Room enter(JsonNode body) {
        state = RoomState.from(body);
        return state.room();
    }

    /** 이 컴퓨터의 플레이어가 방장인지 (ROOM_STATE의 hostId) */
    @Override
    public boolean isHost() {
        return state != null && playerIds.contains(state.hostId());
    }

    /**
     * 방장이 맵을 바꾼다 (CHANGE_MAP). 정원·인원 검사는 서버가 하고, 바뀐 방은 뒤따르는 ROOM_STATE로 온다.
     *
     * @param status 쓰지 않는다. 게임 중·대기 중은 서버가 정한다
     * @return 지금 들고 있는 방 (ROOM_STATE가 오기 전 모습)
     */
    @Override
    public Room updateRoom(String map, Room.Status status) {
        if (state == null) {
            throw new IllegalStateException("방에 있지 않습니다.");
        }
        String mapId = GameMaps.byName(map).id();
        if (!mapId.equals(state.mapId())) {
            call(CHANGE_MAP, Map.of("mapId", mapId));
        }
        return state.room();
    }

    @Override
    public void leave() {
        if (state == null) {
            return;
        }
        call(ROOM_LEAVE, null);
        state = null;
    }

    /** @param character 캐릭터 id 또는 {@value RoomState#RANDOM_CHARACTER} */
    public void selectCharacter(String character) {
        client.send(SELECT_CHARACTER, Map.of("character", character));
    }

    public void selectTeam(Team team) {
        client.send(SELECT_TEAM, Map.of("team", team.name()));
    }

    public void ready(boolean ready) {
        client.send(READY, Map.of("ready", ready));
    }

    /** @param playerId 말한 사람 (2P면 2P 번호) */
    public void chat(long playerId, String text) {
        client.send(CHAT, Map.of("playerId", playerId, "text", text));
    }

    /** 방장이 게임을 시작한다. 성공하면 곧 방 전체에 GAME_STARTED가 온다 */
    public void startGame() {
        call(GAME_START, null);
    }

    /** 게임 화면을 다 띄웠다 (서버는 모두 LOADED를 보내면 카운트다운을 시작하고, 10초 안에 안 보내면 뺀다) */
    public void loaded() {
        client.send(LOADED, null);
    }

    private JsonNode call(String type, Object body) {
        try {
            return client.call(type, body);
        } catch (ServerException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static GameResult result(GameOverBody body) {
        List<GameResult.Standing> standings = body.standings() == null ? List.of() : body.standings().stream()
                .map(standing -> new GameResult.Standing((int) standing.playerId(), standing.nickname(),
                        GameResult.Outcome.valueOf(standing.outcome()), standing.kills(), standing.saves()))
                .toList();
        return new GameResult(standings);
    }
}
