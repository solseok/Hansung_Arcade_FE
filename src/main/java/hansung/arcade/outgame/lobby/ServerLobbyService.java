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
 *   <li>요청 → 응답: ROOM_LIST, ROOM_CREATE, ROOM_JOIN, ROOM_QUICK_JOIN, ROOM_LEAVE, CHANGE_MAP, CHANGE_SLOT, GAME_START.
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
    public static final String CHANGE_SLOT = "CHANGE_SLOT";
    public static final String GAME_START = "GAME_START";
    public static final String SELECT_CHARACTER = "SELECT_CHARACTER";
    public static final String SELECT_TEAM = "SELECT_TEAM";
    public static final String READY = "READY";
    public static final String CHAT = "CHAT";
    public static final String LOADED = "LOADED";
    public static final String GAME_EXIT = "GAME_EXIT";
    public static final String LOBBY_ROOMS = "LOBBY_ROOMS";
    public static final String ROOM_STATE = "ROOM_STATE";
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String GAME_OVER = "GAME_OVER";
    /** 서버가 이미 방에 있다고 거절할 때의 code */
    static final String ALREADY_IN_ROOM = "ALREADY_IN_ROOM";

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
    private volatile List<Long> playerIds = List.of();
    private volatile List<Room> rooms = List.of();
    private volatile RoomState state;

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
            if (state == null) {
                return;
            }
            GameStarted started = Message.read(body, GameStarted.class);
            // 서버는 게임을 시작해도 방 사람들에게 ROOM_STATE를 보내지 않는다. 끝나면 대기 중 ROOM_STATE가 온다
            RoomState playing = state.withStatus(Room.Status.PLAYING);
            state = playing;
            listeners.forEach(listener -> listener.roomStateChanged(playing));
            listeners.forEach(listener -> listener.gameStarted(started));
        });
        onPush(GAME_OVER, body -> {
            GameResult result = readGameOver(body);
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

    /** ROOM_LIST로 목록을 새로 받는다. 서버가 아직 나를 방에 두고 있으면(나가기가 어긋남) 빼낸 뒤 다시 받는다 */
    @Override
    public void reload() {
        JsonNode body;
        try {
            body = client.call(ROOM_LIST, null);
        } catch (ServerException e) {
            if (!"WRONG_STATE".equals(e.code())) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            leaveQuietly();
            body = call(ROOM_LIST, null);
        }
        rooms = RoomState.rooms(body);
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
        return enter(ROOM_JOIN, Map.of("roomNumber", roomNumber));
    }

    @Override
    public Room create(String title, int capacity, int seats) {
        return create(title, capacity, seats, RoomMode.SOLO);
    }

    /** ROOM_CREATE에 경기 방식(mode)을 함께 보낸다. 팀전이면 고를 수 있는 팀이 ROOM_STATE의 teams로 온다 */
    @Override
    public Room create(String title, int capacity, int seats, RoomMode mode) {
        return enter(ROOM_CREATE, Map.of("title", title, "capacity", capacity, "mode", mode.name()));
    }

    @Override
    public Room quickJoin(int seats) {
        return enter(ROOM_QUICK_JOIN, null);
    }

    /**
     * 방에 들어간다. 서버는 이미 방에 있다고 하면(앞서 나가기가 어긋난 경우) 나가기를 보내고 한 번 다시 들어간다.
     * 응답이 늦어 시간이 지나면, 서버가 이미 앉혔을 수 있으므로 나가기를 보내 두고 실패로 알린다.
     */
    private Room enter(String type, Object body) {
        JsonNode response;
        try {
            response = client.call(type, body);
        } catch (ServerException e) {
            if (ALREADY_IN_ROOM.equals(e.code())) {
                leaveQuietly();
                response = call(type, body);
            } else {
                if (ServerException.TIMEOUT.equals(e.code())) {
                    client.send(ROOM_LEAVE, null);
                }
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        state = RoomState.from(response);
        return state.room();
    }

    /** 서버가 아직 나를 방에 두고 있을 때 빼낸다. 대기 중이면 ROOM_LEAVE, 게임 중이면 GAME_EXIT */
    private void leaveQuietly() {
        try {
            client.call(ROOM_LEAVE, null);
        } catch (ServerException e) {
            client.send(GAME_EXIT, null);
        }
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

    /** 서버가 알려 준 지금 방의 닫힌 자리. 방 밖이면 정원으로 셈한다 */
    @Override
    public List<Integer> closedSlots(Room room) {
        RoomState current = state;
        if (current == null || current.room().number() != room.number()) {
            return LobbyService.super.closedSlots(room);
        }
        return current.closedSlots();
    }

    /**
     * 방장이 빈 자리를 열거나 닫는다 (CHANGE_SLOT). 규칙 검사는 서버가 하고, 바뀐 자리·정원은 뒤따르는 ROOM_STATE로 온다.
     *
     * @return 지금 들고 있는 방 (ROOM_STATE가 오기 전 모습)
     * @throws IllegalStateException 방 밖이거나 서버가 거절하면. 서버 문구를 담는다
     */
    @Override
    public Room setSlotOpen(int slot, boolean open) {
        if (state == null) {
            throw new IllegalStateException("방에 있지 않습니다.");
        }
        call(CHANGE_SLOT, Map.of("slot", slot, "open", open));
        return state.room();
    }

    /**
     * 방에서 나간다. 게임 중이면 GAME_EXIT(응답 없음), 대기 중이면 ROOM_LEAVE를 보낸다.
     * 서버가 거절하거나 응답이 늦어도 이 컴퓨터는 방을 떠난 것으로 한다. 서버가 아직 방에 두고 있으면
     * 로비에서 목록을 받을 때({@link #reload}) 다시 빼낸다.
     *
     * @throws IllegalStateException 서버가 나가기를 거절했으면 (그래도 방은 떠난 상태다)
     */
    @Override
    public void leave() {
        RoomState leaving = state;
        state = null;
        if (leaving == null) {
            return;
        }
        if (leaving.room().status() == Room.Status.PLAYING) {
            client.send(GAME_EXIT, null);
            return;
        }
        call(ROOM_LEAVE, null);
    }

    /** 게임 화면에서 나간다 (GAME_EXIT). 서버는 방에서 빼고, 이 컴퓨터는 로비로 간다 */
    public void exitGame() {
        state = null;
        client.send(GAME_EXIT, null);
    }

    /** 서버가 이미 경기와 방에서 뺐다 (로딩 시간 초과). 아무것도 보내지 않고 들고 있던 방만 잊는다 */
    public void forgetRoom() {
        state = null;
    }

    /**
     * 캐릭터를 고른다. 서버는 playerId의 플레이어(1P·2P 가운데 하나)만 바꾼다.
     *
     * @param playerId  고른 사람 (2P면 2P 번호)
     * @param character 캐릭터 id 또는 {@value RoomState#RANDOM_CHARACTER}
     */
    public void selectCharacter(long playerId, String character) {
        client.send(SELECT_CHARACTER, Map.of("playerId", playerId, "character", character));
    }

    /** 팀을 고른다. playerId는 {@link #selectCharacter}와 같다 */
    public void selectTeam(long playerId, Team team) {
        client.send(SELECT_TEAM, Map.of("playerId", playerId, "team", team.name()));
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

    /** GAME_OVER body를 결과 모달에 넘길 결과로 읽는다 */
    public static GameResult readGameOver(JsonNode json) {
        GameOverBody body = Message.read(json, GameOverBody.class);
        List<GameResult.Standing> standings = body.standings() == null ? List.of() : body.standings().stream()
                .map(standing -> new GameResult.Standing((int) standing.playerId(), standing.nickname(),
                        GameResult.Outcome.valueOf(standing.outcome()), standing.kills(), standing.saves()))
                .toList();
        return new GameResult(standings);
    }
}
