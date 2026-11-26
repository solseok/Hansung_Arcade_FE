package hansung.arcade.ingame;

import hansung.arcade.ingame.airplane.FallingItem;
import hansung.arcade.ingame.airplane.FlightPlan;
import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.BalloonField.ServerBurst;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.block.BoxPusher;
import hansung.arcade.ingame.block.BoxPusher.PushRequest;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
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
 *   <li>아이템이 있는 칸에 들어서면 ITEM_PICKUP을 한 번 보낸다. 먹는 것·능력치·손에 든 쓰는 아이템은 ITEM_TAKEN을 따른다.
 *       블럭에서 나온 아이템과 터진 사람이 흩뿌린 아이템도 서버가 알려 준 칸에 놓는다.</li>
 *   <li>비행기는 AIRPLANE_APPEARED가 오면 알려 준 줄(row)을 따라 띄우고(스스로 출현 시각을 세지 않는다),
 *       아이템은 ITEM_DROPPED로 받은 것만 떨어뜨려 닿으면 놓는다. 떨어지는 사이 그 아이템이 물줄기에 사라지거나 누가 먹으면 놓지 않는다.</li>
 *   <li>아이템 키를 누르면 손에 든 쓰는 아이템을 ITEM_USE로 보낸다. 바늘·보호막·스프링 신발의 효과와 남은 개수는
 *       ITEM_USED를 따른다.</li>
 *   <li>상자에 막힌 채 {@link BoxPusher#PUSH_SECONDS}초를 밀면 상자를 옮기지 않고 멈춘 자리(MOVE)와 BOX_PUSH를 보낸다.
 *       상자는 BOX_MOVED가 오면 옮겨 미끄러뜨린다. 서버가 밀지 않았으면 계속 밀 때 다시 보낸다.</li>
 *   <li>갇힘·구출·탈락·나감은 PLAYER_TRAPPED, PLAYER_RESCUED, PLAYER_POPPED, PLAYER_LEFT를 따르고, 승패는 GAME_OVER로 정한다.</li>
 *   <li>화면을 연 뒤 ROUND_BEGIN이 올 때까지는 검은 화면에서 기다린다. ROUND_BEGIN의 카운트다운이 끝나는 순간에
 *       시작 연출("Ready... Start!")도 끝나도록 연출을 늦춰 시작하고, 그때부터 조작과 경기 시계가 움직인다.</li>
 * </ul>
 *
 * <p>서버가 아직 다루지 않는 것(물풍선 밀기·던지기)은 온라인에서 하지 않는다.
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
    /** 비행기에서 떨어지는 중인 아이템 (서버 itemId → 떨어지는 아이템). 닿으면 맵에 놓는다 */
    private final Map<Integer, FallingItem> fallingItems = new HashMap<>();
    private double sinceMove;
    private boolean roundBegun;
    /** ROUND_BEGIN 뒤 경기가 시작될 때까지 남은 시간(초) */
    private double untilPlay;
    private boolean introStarted;
    private boolean over;

    ServerRules(GameWorld world, ServerOutbox outbox) {
        this.world = Objects.requireNonNull(world);
        this.outbox = Objects.requireNonNull(outbox);
        world.airDrop().followServer();
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
            if (world.gear(player).isAirborne()) {
                // 스프링 신발로 뛰는 동안은 점프가 자리를 옮긴다. 내려앉으면 다시 받은 자리를 따라간다
                player.followServer(player.x(), player.y(), player.facing(), false, dt);
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
        if (!isPlaying() || !player.canPlaceBalloon() || world.gear(player).isAirborne()
                || !map.isInside(col, row) || map.isSolid(col, row)
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
            if (player.state() != PlayerState.NORMAL && player.state() != PlayerState.ESCAPING
                    || world.gear(player).isAirborne()) {
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

    /**
     * player가 손에 든 쓰는 아이템을 써 달라고 서버에 보낸다. 쓸 수 있는지는 서버가 정한다 (못 쓰면 조용히 버린다).
     * 스프링 신발은 서버가 마지막 MOVE의 칸·방향으로 뛰므로 지금 자리를 먼저 보낸다.
     */
    void requestItemUse(Player player) {
        world.gear(player).activeItem().ifPresent(item -> {
            // 공중에서는 어떤 아이템도 쓸 수 없고, 중간 자리를 MOVE로 보내면 안 된다
            if (!isPlaying() || !player.isAlive() || world.gear(player).isAirborne()) {
                return;
            }
            sendMove(player);
            outbox.useItem(player.id(), item.id());
        });
    }

    /**
     * player가 상자에 막힌 채 다 밀었다 ({@link BoxPusher#requestPush}). 상자는 옮기지 않고 밀어 달라고 서버에 보낸다.
     * 서버는 마지막 MOVE가 멈춘 채 미는 쪽을 보고 상자 바로 앞 칸에 있을 때만 미므로, 지금 자리(멈춤, 미는 쪽을 봄)를 먼저 보낸다.
     * 상자는 BOX_MOVED가 오면 옮긴다 ({@link #boxMoved}).
     */
    void requestBoxPush(Player player, PushRequest request) {
        if (!isPlaying() || !player.isAlive() || world.gear(player).isAirborne()) {
            return;
        }
        sendMove(player);
        outbox.pushBox(player.id(), request.col(), request.row(), request.direction());
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
            // 뛰는 동안은 착지 칸을 이미 보냈다. 중간 자리를 보내면 서버가 공중 시간이 끝날 때 그 칸에서 가둘 수 있다
            if (player.isAlive() && !world.gear(player).isAirborne() && (last == null || !last.same(player))) {
                sendMove(player);
            }
        }
    }

    private void sendMove(Player player) {
        sendMoveAt(player, player.x(), player.y(), player.isMoving());
    }

    private void sendMoveAt(Player player, double x, double y, boolean moving) {
        outbox.move(player.id(), x, y, player.facing(), moving);
        sent.put(player.id(), new Sent(x, y, player.facing(), moving));
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
            // 서버는 터진 사람의 손을 비우고 따로 알리지 않는다
            world.gear(player).holdFromServer(null, 0);
            world.addSound(GameSound.DIE);
        }
        if (killerId != null && world.player(killerId.intValue()) != null) {
            world.match().recordKill(killerId.intValue());
        }
    }

    /**
     * ITEM_TAKEN. 누군가 아이템을 먹었다. 능력치와 손에 든 쓰는 아이템은 서버가 알려 준 값으로 맞춘다.
     *
     * @param heldItem  먹은 뒤 손에 든 쓰는 아이템 id. 없으면 null
     * @param heldCount 손에 든 개수. 없으면 0
     */
    public void itemTaken(int itemId, long playerId, String item, Stats stats, String heldItem, int heldCount) {
        removeItem(itemId);
        Player player = world.player((int) playerId);
        if (player == null) {
            return;
        }
        if (stats != null) {
            player.syncStats(stats.balloons(), stats.waterRange(), stats.speedLevel());
        }
        world.gear(player).holdFromServer(itemType(heldItem), heldCount);
        if (world.localPlayers().contains(player)) {
            world.addSound(GameSound.ITEM_EATEN);
        }
    }

    /** ITEM_SCATTERED. 터진 사람이 먹었던 아이템이 맵 곳곳에 흩어졌다 */
    public void itemsScattered(long playerId, int col, int row, List<Drop> items) {
        placeItems(items);
    }

    /**
     * ITEM_USED. 누군가 쓰는 아이템을 썼다. 효과를 보이고 손에 남은 아이템을 맞춘다.
     * 바늘은 갇힌 물풍선을 깨고 빠져나오고, 보호막은 durationMs 동안 물줄기를 막는 모습을, 스프링 신발은 (col, row)로 뛴다.
     */
    public void itemUsed(long playerId, String item, Long durationMs, Integer col, Integer row, String heldItem,
                         int heldCount) {
        Player player = world.player((int) playerId);
        ItemType used = itemType(item);
        if (player == null || used == null) {
            return;
        }
        PlayerGear gear = world.gear(player);
        gear.holdFromServer(itemType(heldItem), heldCount);
        switch (used) {
            case NEEDLE -> player.escape();
            case SHIELD -> {
                gear.raiseShieldFromServer(durationMs == null ? PlayerGear.SHIELD_SECONDS : durationMs / 1000.0);
                world.addSound(GameSound.SHIELD);
            }
            case SPRING_SHOE -> {
                if (col != null && row != null) {
                    gear.jumpFromServer(player, col, row);
                    world.addSound(GameSound.SPRING);
                    land(player, TileMap.centerX(col), TileMap.centerY(row));
                }
            }
            default -> log.warn("쓰는 아이템이 아닌데 썼다고 왔습니다: {}", item);
        }
    }

    /**
     * 스프링 신발로 뛴 player가 (x, y)에 내려앉는다고 서버·화면 양쪽에 맞춘다. 이 컴퓨터의 플레이어면 착지 자리를 바로 MOVE로 보낸다.
     * 서버는 공중 시간이 끝날 때 마지막 MOVE 칸에서 물줄기를 보므로, 뛰는 중간 자리가 남아 있으면 엉뚱한 칸에서 갇힐 수 있다.
     * 다른 컴퓨터의 플레이어면 따라갈 자리를 착지 칸으로 둔다. 그래야 내려앉은 뒤 늦게 받은 중간 자리로 끌려가지 않는다.
     */
    private void land(Player player, double x, double y) {
        Remote remote = remotes.get(player.id());
        if (remote == null) {
            sendMoveAt(player, x, y, false);
            return;
        }
        remote.x = x;
        remote.y = y;
        remote.moving = false;
    }

    /**
     * BOX_MOVED. 밀린 상자가 (fromCol, fromRow)에서 (toCol, toRow)로 갔다. 맵 칸은 바로 옮기고(원래 칸은 바닥이 드러난다)
     * 그림만 {@link BoxPusher#SLIDE_SECONDS}초 동안 미끄러뜨린다. 보강된 상자는 남은 맞을 횟수째 옮겨진다.
     */
    public void boxMoved(int fromCol, int fromRow, int toCol, int toRow) {
        if (world.boxPusher().moveFromServer(fromCol, fromRow, toCol, toRow, world.localPlayers()).isEmpty()) {
            log.warn("밀리는 상자가 없거나 들어갈 칸이 비어 있지 않아 옮기지 않습니다: ({}, {}) → ({}, {})",
                    fromCol, fromRow, toCol, toRow);
        }
    }

    /**
     * AIRPLANE_APPEARED. 비행기가 heading(RIGHT·LEFT) 쪽으로 맵의 row 줄을 따라 가로지르기 시작했다. 그 줄의 위쪽 끝 높이로 그린다.
     * 떨어뜨릴 칸(모두 그 줄)은 서버가 ITEM_DROPPED로 따로 알린다. row를 보내지 않는 옛 서버에서는 0으로 읽혀
     * 비행기만 맨 윗줄로 띄운다.
     */
    public void airplaneAppeared(int airplaneId, String heading, int row) {
        Direction direction = direction(heading, null);
        if (direction == null || !direction.isHorizontal()) {
            log.warn("모르는 비행기 방향이라 띄우지 않습니다: {}", heading);
            return;
        }
        if (row < 0 || row >= world.map().rows()) {
            log.warn("맵에 없는 줄이라 비행기를 띄우지 않습니다: {}", row);
            return;
        }
        world.airDrop().launch(new FlightPlan(direction, row, List.of()));
        world.addSound(GameSound.AIRPLANE);
    }

    /**
     * ITEM_DROPPED. 비행기가 (col, row) 칸에 아이템을 떨어뜨렸다. 떨어지는 모습을 보인 뒤 닿으면 놓는다.
     * 서버는 이때부터 아이템이 있다고 보므로, 떨어지는 사이 removedItems·ITEM_TAKEN으로 없어지면 놓지 않는다.
     *
     * @param fallMs 쓰지 않는다. 서버는 늘 129ms(비행기 배에서 칸까지 20px)를 보내고,
     *               {@link FallingItem}도 같은 높이·가속도로 떨어져 같은 때 닿는다
     */
    public void itemDropped(int airplaneId, int itemId, int col, int row, String item, long fallMs) {
        ItemType type = itemType(item);
        if (type == null) {
            log.warn("모르는 아이템이라 떨어뜨리지 않습니다: {}", item);
            return;
        }
        FallingItem falling = world.airDrop().dropFromServer(col, row, type, () -> {
            fallingItems.remove(itemId);
            placeItems(List.of(new Drop(itemId, col, row, item)));
        });
        fallingItems.put(itemId, falling);
        world.addSound(GameSound.AIR_DROP);
    }

    /**
     * PLAYER_LEFT. 나가기·끊김·로딩 시간 초과로 경기에서 빠졌다. 맵에서 바로 사라진다.
     *
     * @return 이 컴퓨터의 플레이어가 빠졌는지. 서버는 로딩 시간 초과로 뺄 때만 빠진 본인에게도 알리고, 방에서도 뺀다
     */
    public boolean playersLeft(List<Long> playerIds, String reason) {
        boolean mine = false;
        for (long id : playerIds) {
            Player player = world.player((int) id);
            if (player != null) {
                player.leaveMatch();
                world.gear(player).holdFromServer(null, 0);
                mine |= world.localPlayers().contains(player);
            }
        }
        return mine;
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
        FallingItem falling = fallingItems.remove(itemId);
        if (falling != null) {
            world.airDrop().cancel(falling);
        }
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

    /** 아이템 id의 종류. 없거나 모르면 null */
    private static ItemType itemType(String id) {
        if (id == null) {
            return null;
        }
        try {
            return ItemType.fromId(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Direction direction(String name, Direction fallback) {
        try {
            return Direction.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallback;
        }
    }
}
