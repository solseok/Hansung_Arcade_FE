package hansung.arcade.ingame;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.BalloonField.ServerBurst;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.StartIntro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 온라인 경기의 규칙: 판정은 서버가 하고, 이 컴퓨터는 서버가 알려 준 대로 그린다. Swing과 네트워크에 의존하지 않는다.
 *
 * <ul>
 *   <li>이 컴퓨터의 플레이어(1P·2P)는 지금처럼 키로 움직이고, 그 자리를 {@link #MOVE_INTERVAL_SECONDS}초마다
 *       바뀌었을 때만 MOVE로 보낸다. 다른 플레이어는 PLAYER_MOVED로 받은 자리로 따라간다 ({@link #CATCH_UP}배 빠르기).</li>
 *   <li>물풍선 키를 누르면 놓지 않고 BALLOON_PLACE만 보낸다. 서버가 받아들이면 BALLOON_PLACED가 와서 그때 놓는다.
 *       놓인 물풍선은 스스로 터지지 않고 BALLOON_EXPLODED가 오면 그 길이만큼 물줄기를 그리고, 블럭은 서버가 알려 준 모습으로 바꾼다.</li>
 *   <li>아이템이 있는 칸에 들어서면 ITEM_PICKUP을 한 번 보낸다. 먹는 것·능력치는 ITEM_TAKEN을 따른다.
 *       블럭에서 나온 아이템과 터진 사람이 흩뿌린 아이템도 서버가 알려 준 칸에 놓는다.</li>
 *   <li>갇힘·구출·탈락·나감은 PLAYER_TRAPPED, PLAYER_RESCUED, PLAYER_POPPED, PLAYER_LEFT를 따르고, 승패는 GAME_OVER로 정한다.</li>
 *   <li>화면을 연 뒤 ROUND_BEGIN이 올 때까지는 검은 화면에서 기다린다. ROUND_BEGIN의 카운트다운이 끝나는 순간에
 *       시작 연출("Ready... Start!")도 끝나도록 연출을 늦춰 시작하고, 그때부터 조작과 경기 시계가 움직인다.</li>
 * </ul>
 *
 * <p>서버가 아직 다루지 않는 것(밀거나 던지기, 액티브 아이템, 밀리는 상자, 비행기)은 온라인에서 하지 않는다.
 * 서버 메시지는 게임 루프와 같은 스레드(EDT)에서 넣는다.</p>
 */
public final class ServerRules {

    /** 내 자리(MOVE)를 보내는 간격(초). 서버 명세: 초당 20번 */
    static final double MOVE_INTERVAL_SECONDS = 0.05;
    /** 다른 플레이어가 서버가 알려 준 자리에서 이보다 멀면 따라가지 않고 바로 옮긴다 (2칸) */
    static final double SNAP_DISTANCE = TileMap.TILE_SIZE * 2.0;
    /** 다른 플레이어를 따라가는 빠르기. 그 플레이어가 걷는 빠르기의 이만큼 배라 늦게 받은 만큼 따라잡는다 */
    static final double CATCH_UP = 1.5;
    private static final double EPSILON = 1e-6;
    private static final Logger log = LoggerFactory.getLogger(ServerRules.class);

    /** BALLOON_EXPLODED의 balloons 한 줄. 물줄기가 상하좌우로 실제로 뻗은 칸 수 */
    public record Blast(int balloonId, int col, int row, int up, int down, int left, int right) {
    }

    /** BALLOON_EXPLODED의 tiles 한 줄. 바뀐 뒤의 맵 기호 (블럭이 부서지면 그 칸 바닥 기호) */
    public record TileChange(int col, int row, String tile) {
    }

    /** 맵에 새로 놓인 아이템 (블럭에서 나옴, 터진 사람이 흩뿌림) */
    public record Drop(int itemId, int col, int row, String item) {
    }

    /** 아이템을 먹은 뒤의 능력치 */
    public record Stats(int balloons, int waterRange, int speedLevel) {
    }

    /** 다른 플레이어가 서버에서 있는 자리 */
    private static final class Remote {
        private double x;
        private double y;
        private Direction facing = Direction.DOWN;
        private boolean moving;

        private Remote(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /** 마지막으로 보낸 MOVE */
    private record Sent(double x, double y, Direction facing, boolean moving) {
        boolean same(Player player) {
            return x == player.x() && y == player.y() && facing == player.facing() && moving == player.isMoving();
        }
    }

    private final GameWorld world;
    private final ServerOutbox outbox;
    /** 놓여 있는 물풍선 (서버 balloonId → 물풍선) */
    private final Map<Integer, WaterBalloon> balloons = new HashMap<>();
    /** 놓였던 물풍선의 주인 (balloonId → 플레이어 번호). 터진 뒤 갇힘을 알릴 때도 쓴다 */
    private final Map<Integer, Integer> balloonOwners = new HashMap<>();
    /** 맵에 놓인 아이템의 칸 (서버 itemId → 칸) */
    private final Map<Integer, Point> itemCells = new HashMap<>();
    /** 다른 컴퓨터의 플레이어 번호 → 서버에서의 자리 */
    private final Map<Integer, Remote> remotes = new HashMap<>();
    /** 이 컴퓨터의 플레이어 번호 → 마지막으로 보낸 MOVE */
    private final Map<Integer, Sent> sent = new HashMap<>();
    /** 이 컴퓨터의 플레이어 번호 → 지금 선 칸, 그 칸에서 줍겠다고 보낸 아이템 */
    private final Map<Integer, Point> pickupCells = new HashMap<>();
    private final Map<Integer, Integer> pickupRequests = new HashMap<>();
    private double sinceMove;
    private boolean roundBegun;
    /** ROUND_BEGIN 뒤 경기가 시작될 때까지 남은 시간(초) */
    private double untilPlay;
    private boolean introStarted;
    private boolean over;

    ServerRules(GameWorld world, ServerOutbox outbox) {
        this.world = Objects.requireNonNull(world);
        this.outbox = Objects.requireNonNull(outbox);
        for (Player player : world.players()) {
            if (!world.localPlayers().contains(player)) {
                remotes.put(player.id(), new Remote(player.x(), player.y()));
            }
        }
    }

    // ── 게임 루프가 틱마다 부르는 것 ─────────────────────────

    /**
     * 카운트다운과 시작 연출을 dt초 흘리고, 경기 중이면 경기 시계를 흘린다.
     *
     * @return 이번 틱에 키 입력을 받는지 (경기 중이고 시작 연출이 끝났다)
     */
    boolean advanceRound(double dt) {
        if (roundBegun && untilPlay > 0) {
            untilPlay = Math.max(0, untilPlay - dt);
        }
        if (introStarted) {
            world.advanceIntro(dt);
        } else if (roundBegun && untilPlay <= StartIntro.TOTAL_SECONDS + EPSILON) {
            // 연출이 카운트다운과 함께 끝나도록 남은 시간이 연출 길이가 됐을 때 시작한다
            world.intro().begin();
            world.addSound(GameSound.READY);
            introStarted = true;
        }
        if (!isPlaying()) {
            return false;
        }
        world.match().update(dt);
        return !world.intro().isRunning();
    }

    /** 서버에서 경기가 진행 중인지 (카운트다운이 끝났고 GAME_OVER가 오지 않았다) */
    public boolean isPlaying() {
        return roundBegun && untilPlay <= 0 && !over;
    }

    /** 다른 컴퓨터의 플레이어를 서버가 알려 준 자리 쪽으로 옮긴다. 멀리 떨어졌으면 바로 옮긴다 */
    void moveRemotePlayers(double dt) {
        for (Player player : world.players()) {
            Remote remote = remotes.get(player.id());
            if (remote == null) {
                continue;
            }
            double dx = remote.x - player.x();
            double dy = remote.y - player.y();
            double distance = Math.hypot(dx, dy);
            double x = remote.x;
            double y = remote.y;
            double step = Math.max(player.speed(), Player.TRAPPED_SPEED) * CATCH_UP * dt;
            if (distance <= SNAP_DISTANCE && distance > step) {
                x = player.x() + dx / distance * step;
                y = player.y() + dy / distance * step;
            }
            // 서버에서 멈췄어도 아직 따라가는 중이면 걷는 모습을 보인다
            boolean catchingUp = Math.hypot(remote.x - x, remote.y - y) > 1;
            player.followServer(x, y, remote.facing, remote.moving || catchingUp, dt);
        }
    }

    /**
     * player가 선 칸에 물풍선을 놓아 달라고 서버에 보낸다. 이 컴퓨터가 보기에도 놓을 수 없는 칸이면 보내지 않는다.
     * 서버는 마지막 MOVE의 칸을 보고 판정하므로 지금 자리를 먼저 보낸다.
     */
    void requestBalloon(Player player) {
        int col = player.col();
        int row = player.row();
        TileMap map = world.map();
        BalloonField field = world.balloonField();
        if (!isPlaying() || !player.canPlaceBalloon() || !map.isInside(col, row) || map.isSolid(col, row)
                || field.holds(col, row) || field.placedBy(player) >= player.balloons()) {
            return;
        }
        sendMove(player);
        outbox.placeBalloon(player.id(), col, row);
    }

    /**
     * 이 컴퓨터의 플레이어가 아이템 칸에 들어섰으면 ITEM_PICKUP을 보낸다. 한 칸에서는 한 번만 보내고, 칸을 벗어났다가
     * 다시 들어오면 또 보낸다. 서버는 마지막 MOVE의 칸을 보므로 지금 자리를 먼저 보낸다.
     */
    void requestPickups() {
        for (Player player : world.localPlayers()) {
            Point cell = new Point(player.col(), player.row());
            if (!cell.equals(pickupCells.get(player.id()))) {
                pickupCells.put(player.id(), cell);
                pickupRequests.remove(player.id());
            }
            if (player.state() != PlayerState.NORMAL && player.state() != PlayerState.ESCAPING) {
                continue;
            }
            Integer itemId = itemAt(cell);
            if (itemId == null || itemId.equals(pickupRequests.get(player.id()))) {
                continue;
            }
            sendMove(player);
            outbox.pickUpItem(player.id(), itemId);
            pickupRequests.put(player.id(), itemId);
        }
    }

    /** 경기 중이면 {@link #MOVE_INTERVAL_SECONDS}초마다, 지난번 보낸 뒤 바뀐 플레이어의 자리만 보낸다 */
    void sendMoves(double dt) {
        if (!isPlaying()) {
            return;
        }
        sinceMove += dt;
        if (sinceMove + EPSILON < MOVE_INTERVAL_SECONDS) {
            return;
        }
        sinceMove = 0;
        for (Player player : world.localPlayers()) {
            Sent last = sent.get(player.id());
            if (player.isAlive() && (last == null || !last.same(player))) {
                sendMove(player);
            }
        }
    }

    private void sendMove(Player player) {
        outbox.move(player.id(), player.x(), player.y(), player.facing(), player.isMoving());
        sent.put(player.id(), new Sent(player.x(), player.y(), player.facing(), player.isMoving()));
    }

    /** 1P가 친 채팅을 서버로 보낸다. 다듬고 나서 비어 있으면 보내지 않는다. 화면에는 서버가 돌려준 글로 뜬다 */
    void sendChat(Player from, String text) {
        String content = ChatMessage.clean(text);
        if (!content.isEmpty()) {
            outbox.chat(from.id(), content);
        }
    }

    // ── 서버가 알린 일 ──────────────────────────────────────

    /** ROUND_BEGIN. 모두 화면을 띄웠다. countdownMs 뒤 경기가 시작되고 timeLimitMs 뒤 끝난다 */
    public void roundBegin(long countdownMs, long timeLimitMs) {
        if (roundBegun) {
            return;
        }
        roundBegun = true;
        untilPlay = Math.max(0, countdownMs) / 1000.0;
        if (timeLimitMs != MatchClock.DURATION_SECONDS * 1000L) {
            log.warn("서버 제한 시간 {}ms가 화면 시계 {}초와 다릅니다. 끝나는 때는 서버의 GAME_OVER를 따릅니다.",
                    timeLimitMs, MatchClock.DURATION_SECONDS);
        }
    }

    /** PLAYER_MOVED. 다른 컴퓨터의 플레이어가 움직였다. 이 컴퓨터의 플레이어나 모르는 번호는 버린다 */
    public void playerMoved(long playerId, double x, double y, String facing, boolean moving) {
        Remote remote = remotes.get((int) playerId);
        if (remote == null) {
            return;
        }
        remote.x = x;
        remote.y = y;
        remote.facing = direction(facing, remote.facing);
        remote.moving = moving;
    }

    /** BALLOON_PLACED. fuseMs는 쓰지 않는다 (언제 터지는지는 BALLOON_EXPLODED가 알린다) */
    public void balloonPlaced(int balloonId, long ownerId, int col, int row, int range, long fuseMs) {
        WaterBalloon balloon = world.balloonField().placeFromServer((int) ownerId, col, row, range, world.players());
        balloons.put(balloonId, balloon);
        balloonOwners.put(balloonId, (int) ownerId);
        world.addSound(GameSound.BALLOON_PLACED);
    }

    /**
     * BALLOON_EXPLODED. 물풍선들이 한꺼번에(연쇄 포함) 터졌다. 물줄기를 그리고, 블럭을 서버가 알려 준 모습으로 바꾸고,
     * 물줄기에 사라진 아이템을 없앤 뒤 부서진 블럭에서 나온 아이템을 놓는다.
     */
    public void balloonExploded(List<Blast> blasts, List<TileChange> tiles, List<Drop> items, List<Integer> removedItems) {
        List<ServerBurst> bursts = new ArrayList<>();
        for (Blast blast : blasts) {
            WaterBalloon balloon = balloons.remove(blast.balloonId());
            int owner = balloonOwners.getOrDefault(blast.balloonId(), 0);
            bursts.add(new ServerBurst(balloon, owner, blast.col(), blast.row(),
                    blast.up(), blast.down(), blast.left(), blast.right()));
        }
        world.balloonField().burstFromServer(bursts);
        for (TileChange change : tiles) {
            Tile tile = change.tile() == null || change.tile().length() != 1 ? null : Tile.fromSymbol(change.tile().charAt(0));
            if (tile == null) {
                log.warn("모르는 맵 기호라 칸을 바꾸지 않습니다: {}", change);
                continue;
            }
            world.map().setBlock(change.col(), change.row(), tile);
        }
        for (int itemId : removedItems) {
            removeItem(itemId);
        }
        placeItems(items);
    }

    /** PLAYER_TRAPPED. balloonId 물풍선의 물줄기에 갇혔다 */
    public void playerTrapped(long playerId, int balloonId, long popMs) {
        Player player = world.player((int) playerId);
        if (player != null && player.trapByServer(balloonOwners.getOrDefault(balloonId, 0))) {
            world.addSound(GameSound.TRAPPED);
        }
    }

    /** PLAYER_RESCUED. 같은 편이 닿아 구했다 */
    public void playerRescued(long playerId, long rescuerId) {
        Player player = world.player((int) playerId);
        if (player != null) {
            player.escape();
        }
        if (world.player((int) rescuerId) != null) {
            world.match().recordSave((int) rescuerId);
        }
    }

    /** PLAYER_POPPED. 상대가 닿았거나(TOUCH) 시간이 다 돼(TIMEOUT) 터졌다. killerId는 TIMEOUT이면 null */
    public void playerPopped(long playerId, Long killerId, String reason) {
        Player player = world.player((int) playerId);
        if (player != null && player.isAlive()) {
            player.eliminate();
            world.addSound(GameSound.DIE);
        }
        if (killerId != null && world.player(killerId.intValue()) != null) {
            world.match().recordKill(killerId.intValue());
        }
    }

    /** ITEM_TAKEN. 누군가 아이템을 먹었다. 능력치는 서버가 알려 준 값으로 맞춘다 */
    public void itemTaken(int itemId, long playerId, String item, Stats stats) {
        removeItem(itemId);
        Player player = world.player((int) playerId);
        if (player == null) {
            return;
        }
        if (stats != null) {
            player.syncStats(stats.balloons(), stats.waterRange(), stats.speedLevel());
        }
        if (world.localPlayers().contains(player)) {
            world.addSound(GameSound.ITEM_EATEN);
        }
    }

    /** ITEM_SCATTERED. 터진 사람이 먹었던 아이템이 맵 곳곳에 흩어졌다 */
    public void itemsScattered(long playerId, int col, int row, List<Drop> items) {
        placeItems(items);
    }

    /** PLAYER_LEFT. 나가기·끊김·로딩 시간 초과로 경기에서 빠졌다. 맵에서 바로 사라진다 */
    public void playersLeft(List<Long> playerIds, String reason) {
        for (long id : playerIds) {
            Player player = world.player((int) id);
            if (player != null) {
                player.leaveMatch();
            }
        }
    }

    /** GAME_OVER. 경기가 끝났다. 시계를 멈추고 더는 입력을 받지 않는다 */
    public void gameOver() {
        over = true;
        world.match().stopClock();
    }

    // ── 도움 ─────────────────────────────────────────────

    private void placeItems(List<Drop> items) {
        boolean placed = false;
        for (Drop drop : items) {
            ItemType type;
            try {
                type = ItemType.fromId(drop.item());
            } catch (IllegalArgumentException e) {
                log.warn("모르는 아이템이라 놓지 않습니다: {}", drop);
                continue;
            }
            // 한 칸에 아이템은 하나뿐이라, 그 칸에 있던 아이템은 새 아이템으로 바뀐다
            itemCells.values().removeIf(cell -> cell.x == drop.col() && cell.y == drop.row());
            world.items().place(drop.col(), drop.row(), type);
            itemCells.put(drop.itemId(), new Point(drop.col(), drop.row()));
            placed = true;
        }
        if (placed) {
            world.addSound(GameSound.ITEM_POP);
        }
    }

    private void removeItem(int itemId) {
        Point cell = itemCells.remove(itemId);
        if (cell != null) {
            world.items().remove(cell.x, cell.y);
        }
    }

    private Integer itemAt(Point cell) {
        for (Map.Entry<Integer, Point> entry : itemCells.entrySet()) {
            if (entry.getValue().equals(cell)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Direction direction(String name, Direction fallback) {
        try {
            return Direction.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallback;
        }
    }
}
