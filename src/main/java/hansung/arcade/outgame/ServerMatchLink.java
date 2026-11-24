package hansung.arcade.outgame;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.ingame.ServerOutbox;
import hansung.arcade.ingame.ServerRules;
import hansung.arcade.ingame.ServerRules.Blast;
import hansung.arcade.ingame.ServerRules.Drop;
import hansung.arcade.ingame.ServerRules.Stats;
import hansung.arcade.ingame.ServerRules.TileChange;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.network.Message;
import hansung.arcade.network.ServerClient;
import hansung.arcade.outgame.lobby.ServerLobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 온라인 경기와 서버를 잇는다. 인게임 푸시를 읽어 지금 띄운 경기({@link ServerRules})에 넣고,
 * 이 컴퓨터의 플레이어가 한 일(MOVE, BALLOON_PLACE, ITEM_PICKUP, ITEM_USE, BOX_PUSH, CHAT)을 서버로 보낸다.
 * 나가기(GAME_EXIT)는 방 API({@link ServerLobbyService#exitGame})가 보낸다.
 *
 * <ul>
 *   <li>푸시: ROUND_BEGIN, PLAYER_MOVED, BALLOON_PLACED, BALLOON_EXPLODED, PLAYER_TRAPPED, PLAYER_RESCUED,
 *       PLAYER_POPPED, ITEM_TAKEN, ITEM_USED, ITEM_SCATTERED, AIRPLANE_APPEARED, ITEM_DROPPED, BOX_MOVED, PLAYER_LEFT,
 *       CHAT_MESSAGE, GAME_OVER. 게임 화면을 여는 GAME_STARTED는
 *       방 API({@link ServerLobbyService})가 받는다.</li>
 *   <li>받는 스레드에서 오므로 {@code ui}(EDT, 게임 루프와 같은 스레드)로 넘겨 넣는다.</li>
 *   <li>GAME_STARTED를 받은 뒤 게임 화면을 띄울 때까지(대기실이 어두워지는 동안) 온 푸시는 모아 두었다가 {@link #bind} 때
 *       받은 차례대로 넣는다. 서버는 시작할 때 이미 끊긴 사람이 있으면 GAME_STARTED 바로 뒤에 PLAYER_LEFT·GAME_OVER를 보내므로,
 *       버리면 게임 화면이 검은 채로 멈춘다. 경기를 띄우지도 기다리지도 않을 때 온 푸시는 버린다.</li>
 * </ul>
 */
public final class ServerMatchLink implements ServerOutbox {

    public static final String MOVE = "MOVE";
    public static final String BALLOON_PLACE = "BALLOON_PLACE";
    public static final String ITEM_PICKUP = "ITEM_PICKUP";
    public static final String ITEM_USE = "ITEM_USE";
    public static final String BOX_PUSH = "BOX_PUSH";
    public static final String BOX_MOVED = "BOX_MOVED";
    public static final String CHAT = "CHAT";
    public static final String ROUND_BEGIN = "ROUND_BEGIN";
    public static final String PLAYER_MOVED = "PLAYER_MOVED";
    public static final String BALLOON_PLACED = "BALLOON_PLACED";
    public static final String BALLOON_EXPLODED = "BALLOON_EXPLODED";
    public static final String PLAYER_TRAPPED = "PLAYER_TRAPPED";
    public static final String PLAYER_RESCUED = "PLAYER_RESCUED";
    public static final String PLAYER_POPPED = "PLAYER_POPPED";
    public static final String ITEM_TAKEN = "ITEM_TAKEN";
    public static final String ITEM_USED = "ITEM_USED";
    public static final String AIRPLANE_APPEARED = "AIRPLANE_APPEARED";
    public static final String ITEM_DROPPED = "ITEM_DROPPED";
    public static final String ITEM_SCATTERED = "ITEM_SCATTERED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";

    private static final Logger log = LoggerFactory.getLogger(ServerMatchLink.class);

    private record RoundBegin(long countdownMs, long timeLimitMs) {
    }

    private record PlayerMoved(long playerId, double x, double y, String facing, boolean moving) {
    }

    private record BalloonPlaced(int balloonId, long ownerId, int col, int row, int range, long fuseMs) {
    }

    private record BalloonExploded(List<Blast> balloons, List<TileChange> tiles, List<Drop> items,
                                   List<Integer> removedItems) {
    }

    private record PlayerTrapped(long playerId, int balloonId, long popMs) {
    }

    private record PlayerRescued(long playerId, long rescuerId) {
    }

    private record PlayerPopped(long playerId, Long killerId, String reason) {
    }

    /** @param heldCount 서버가 쓰는 아이템을 다루기 전에는 오지 않는다 (없으면 0) */
    private record ItemTaken(int itemId, long playerId, String item, Stats stats, String heldItem, Integer heldCount) {
    }

    /** 값이 없는 필드는 오지 않는다 (null) */
    private record ItemUsed(long playerId, String item, Long durationMs, Integer col, Integer row, String heldItem,
                            Integer heldCount) {
    }

    private record AirplaneAppeared(int airplaneId, String heading) {
    }

    private record BoxMoved(int fromCol, int fromRow, int toCol, int toRow) {
    }

    private record ItemDropped(int airplaneId, int itemId, int col, int row, String item, long fallMs) {
    }

    private record ItemScattered(long playerId, int col, int row, List<Drop> items) {
    }

    private record PlayerLeft(List<Long> playerIds, String reason) {
    }

    /**
     * 지금 띄운 경기와 그 화면에 알릴 곳.
     *
     * @param chat     경기 중 받은 채팅을 올릴 곳 (맵 위 채팅·말풍선)
     * @param gameOver 서버가 경기를 끝냈다 (GAME_OVER)
     * @param kicked   이 컴퓨터의 플레이어가 경기에서 빠졌다 (로딩 시간 초과). 서버는 방에서도 뺐다
     */
    private record Bound(ServerRules rules, Consumer<ChatMessage> chat, Consumer<GameResult> gameOver, Runnable kicked) {
    }

    private final ServerClient client;
    private final Executor ui;
    /** 지금 띄운 온라인 경기. EDT에서만 바꾸고 읽는다 */
    private Bound bound;
    /** GAME_STARTED를 받고 아직 띄우지 않은 경기에 넣을 일. 기다리는 경기가 없으면 null. EDT에서만 쓴다 */
    private List<Consumer<Bound>> waiting;

    /** @param ui 푸시를 넘길 곳. 게임에서는 {@code SwingUtilities::invokeLater} */
    public ServerMatchLink(ServerClient client, Executor ui) {
        this.client = Objects.requireNonNull(client);
        this.ui = Objects.requireNonNull(ui);
        // 방 API가 같은 GAME_STARTED로 게임 화면을 연다. 그 화면이 bind할 때까지 오는 푸시를 모은다
        client.onPush(ServerLobbyService.GAME_STARTED, body -> ui.execute(() -> {
            if (bound == null) {
                waiting = new ArrayList<>();
            }
        }));
        onPush(ROUND_BEGIN, RoundBegin.class, (r, b) -> r.roundBegin(b.countdownMs(), b.timeLimitMs()));
        onPush(PLAYER_MOVED, PlayerMoved.class,
                (r, b) -> r.playerMoved(b.playerId(), b.x(), b.y(), b.facing(), b.moving()));
        onPush(BALLOON_PLACED, BalloonPlaced.class,
                (r, b) -> r.balloonPlaced(b.balloonId(), b.ownerId(), b.col(), b.row(), b.range(), b.fuseMs()));
        onPush(BALLOON_EXPLODED, BalloonExploded.class, (r, b) -> r.balloonExploded(listOf(b.balloons()),
                listOf(b.tiles()), listOf(b.items()), listOf(b.removedItems())));
        onPush(PLAYER_TRAPPED, PlayerTrapped.class, (r, b) -> r.playerTrapped(b.playerId(), b.balloonId(), b.popMs()));
        onPush(PLAYER_RESCUED, PlayerRescued.class, (r, b) -> r.playerRescued(b.playerId(), b.rescuerId()));
        onPush(PLAYER_POPPED, PlayerPopped.class, (r, b) -> r.playerPopped(b.playerId(), b.killerId(), b.reason()));
        onPush(ITEM_TAKEN, ItemTaken.class, (r, b) -> r.itemTaken(b.itemId(), b.playerId(), b.item(), b.stats(),
                b.heldItem(), count(b.heldCount())));
        onPush(ITEM_USED, ItemUsed.class, (r, b) -> r.itemUsed(b.playerId(), b.item(), b.durationMs(), b.col(), b.row(),
                b.heldItem(), count(b.heldCount())));
        onPush(AIRPLANE_APPEARED, AirplaneAppeared.class, (r, b) -> r.airplaneAppeared(b.airplaneId(), b.heading()));
        onPush(BOX_MOVED, BoxMoved.class, (r, b) -> r.boxMoved(b.fromCol(), b.fromRow(), b.toCol(), b.toRow()));
        onPush(ITEM_DROPPED, ItemDropped.class,
                (r, b) -> r.itemDropped(b.airplaneId(), b.itemId(), b.col(), b.row(), b.item(), b.fallMs()));
        onPush(ITEM_SCATTERED, ItemScattered.class,
                (r, b) -> r.itemsScattered(b.playerId(), b.col(), b.row(), listOf(b.items())));
        client.onPush(PLAYER_LEFT, body -> ui.execute(() -> {
            PlayerLeft left = read(PLAYER_LEFT, body, PlayerLeft.class);
            if (left != null) {
                deliver(PLAYER_LEFT, target -> {
                    if (target.rules().playersLeft(listOf(left.playerIds()), left.reason())) {
                        target.kicked().run();
                    }
                });
            }
        }));
        client.onPush(ServerLobbyService.GAME_OVER, body -> ui.execute(() -> {
            GameResult result;
            try {
                result = ServerLobbyService.readGameOver(body);
            } catch (RuntimeException e) {
                log.error("GAME_OVER를 읽지 못했습니다: {}", body, e);
                return;
            }
            deliver(ServerLobbyService.GAME_OVER, target -> target.gameOver().accept(result));
        }));
        client.onPush(CHAT_MESSAGE, body -> ui.execute(() -> {
            // 입장·퇴장 같은 서버 안내(senderId 0)는 경기 중에는 띄우지 않는다
            long senderId = body.path("senderId").asLong();
            if (senderId != 0) {
                ChatMessage message = new ChatMessage((int) senderId, body.path("sender").asText(""),
                        body.path("text").asText(""));
                deliver(CHAT_MESSAGE, target -> target.chat().accept(message));
            }
        }));
    }

    @FunctionalInterface
    private interface Apply<T> {
        void to(ServerRules rules, T body);
    }

    private <T> void onPush(String type, Class<T> bodyType, Apply<T> apply) {
        client.onPush(type, body -> ui.execute(() -> {
            T parsed = read(type, body, bodyType);
            if (parsed != null) {
                deliver(type, target -> apply.to(target.rules(), parsed));
            }
        }));
    }

    private static <T> T read(String type, JsonNode body, Class<T> bodyType) {
        try {
            return Message.read(body, bodyType);
        } catch (RuntimeException e) {
            log.error("{}를 읽지 못했습니다: {}", type, body, e);
            return null;
        }
    }

    /** 띄운 경기에 넣는다. 띄울 경기를 기다리는 중이면 모아 두고, 둘 다 아니면 버린다. EDT에서 부른다 */
    private void deliver(String type, Consumer<Bound> action) {
        if (bound != null) {
            apply(type, action, bound);
        } else if (waiting != null) {
            waiting.add(target -> apply(type, action, target));
        }
    }

    private static void apply(String type, Consumer<Bound> action, Bound target) {
        try {
            action.accept(target);
        } catch (RuntimeException e) {
            log.error("{}를 경기에 넣지 못했습니다.", type, e);
        }
    }

    private static int count(Integer count) {
        return count == null ? 0 : count;
    }

    private static <T> List<T> listOf(List<T> list) {
        return list == null ? List.of() : list;
    }

    /**
     * 띄운 경기에 서버 푸시를 넣기 시작한다. 화면을 띄우는 동안 모아 둔 푸시를 먼저 받은 차례대로 넣는다. EDT에서 부른다.
     *
     * @param chat     경기 중 받은 채팅을 올릴 곳 (맵 위 채팅·말풍선)
     * @param gameOver 서버가 경기를 끝냈을 때 (GAME_OVER)
     * @param kicked   이 컴퓨터의 플레이어가 경기에서 빠졌을 때 (로딩 시간 초과). 서버는 방에서도 뺐다
     */
    public void bind(ServerRules rules, Consumer<ChatMessage> chat, Consumer<GameResult> gameOver, Runnable kicked) {
        Bound target = new Bound(Objects.requireNonNull(rules), Objects.requireNonNull(chat),
                Objects.requireNonNull(gameOver), Objects.requireNonNull(kicked));
        bound = target;
        List<Consumer<Bound>> queued = waiting == null ? List.of() : waiting;
        waiting = null;
        for (Consumer<Bound> action : queued) {
            // 넣다가 화면이 닫히면(빠짐 → 로비) 나머지는 버린다
            if (bound != target) {
                return;
            }
            action.accept(target);
        }
    }

    /** 경기 화면을 닫았다. 이제 오는 인게임 푸시는 버린다 */
    public void unbind() {
        bound = null;
        waiting = null;
    }

    @Override
    public void move(int playerId, double x, double y, Direction facing, boolean moving) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", playerId);
        body.put("x", x);
        body.put("y", y);
        body.put("facing", facing.name());
        body.put("moving", moving);
        client.send(MOVE, body);
    }

    @Override
    public void placeBalloon(int playerId, int col, int row) {
        client.send(BALLOON_PLACE, Map.of("playerId", playerId, "col", col, "row", row));
    }

    @Override
    public void pickUpItem(int playerId, int itemId) {
        client.send(ITEM_PICKUP, Map.of("playerId", playerId, "itemId", itemId));
    }

    @Override
    public void useItem(int playerId, String item) {
        client.send(ITEM_USE, Map.of("playerId", playerId, "item", item));
    }

    @Override
    public void pushBox(int playerId, int col, int row, Direction direction) {
        client.send(BOX_PUSH, Map.of("playerId", playerId, "col", col, "row", row, "direction", direction.name()));
    }

    @Override
    public void chat(int playerId, String text) {
        client.send(CHAT, Map.of("playerId", playerId, "text", text));
    }
}
