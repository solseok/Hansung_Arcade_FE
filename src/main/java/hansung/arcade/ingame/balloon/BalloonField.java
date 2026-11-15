package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.balloon.WaterCell.Part;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 맵 위의 물풍선과 물줄기. Swing에 의존하지 않는다.
 *
 * <ul>
 *   <li>물풍선은 플레이어가 선 칸 가운데에 놓는다. 한 칸에 하나뿐이고, 한 사람이 동시에 놓아 둘 수 있는 개수는
 *       {@link Player#balloons()}다. 갇히거나 탈락한 플레이어는 놓지 못한다.</li>
 *   <li>놓은 뒤 {@link WaterBalloon#FUSE_SECONDS}초가 지나면 터져, 놓을 때 주인의 물줄기 길이만큼 상하좌우로 물줄기가 뻗는다.</li>
 *   <li>물줄기는 막힌 칸(블럭·맵 끝)에 들어가지 않고 그 앞에서 멈춘다. 다른 물풍선에 닿으면 그 물풍선도 같은 순간 터진다.
 *       물줄기가 남아 있는 칸에 물풍선을 놓아도 바로 터진다.</li>
 *   <li>물줄기는 {@link Explosion#SECONDS}초 동안 남는다.</li>
 *   <li>{@link #push 밀린} 물풍선은 그 방향으로 미끄러져, 통과할 수 없는 블럭(맵 끝 포함) 바로 앞 칸에 멈춘다.
 *       다른 물풍선·캐릭터·덤불은 막지 않고 지나간다. 다만 멈출 칸에 다른 물풍선이 있으면 그 앞의 빈 칸에 멈춘다.
 *       블럭의 가시 난 면({@link Tile#popsBalloonMoving})에 닿아 멈추면 그 자리에서 바로 터지고,
 *       미끄러지다 물줄기가 남은 칸에 들어가도 바로 터진다.</li>
 *   <li>{@link #throwBalloon 던진} 물풍선은 공중을 날아 원래 칸에서 {@link #THROW_DISTANCE}칸째에 내려앉는다
 *       ({@link #landingCell} 참고). 나는 동안에는 길을 막지 않고 물줄기에도 맞지 않으며, 그 사이 터질 때가 되면
 *       내려앉자마자 터진다.</li>
 *   <li>물풍선이 사람이 있는 칸에 미끄러져 들어오거나 내려앉으면, 그 사람은 놓는 순간처럼 그 칸을 벗어날 때까지 지나갈 수 있다.</li>
 * </ul>
 *
 * <p>물줄기에 맞은 플레이어를 어떻게 할지는 게임 상태(GameWorld)가 {@link #wetOwnerAt}을 보고 정한다.
 * 블럭 파괴·아이템처럼 터질 때마다 할 일은 {@link #takeNewExplosions}로 이번에 터진 것을 받아 처리한다.</p>
 */
public final class BalloonField {

    /** 밀린 물풍선이 미끄러지는 빠르기 (초당 칸) */
    public static final double SLIDE_TILES_PER_SECOND = 10;
    /** 던진 물풍선이 내려앉는 거리. 원래 칸을 0번째로 세어 이만큼째 칸이다 */
    public static final int THROW_DISTANCE = 6;
    /** 던진 물풍선이 한 칸을 나는 시간(초) */
    public static final double FLIGHT_SECONDS_PER_TILE = 0.06;
    /** 아주 가까이 내려앉아도 이만큼은 난다(초) */
    public static final double MIN_FLIGHT_SECONDS = 0.2;
    private static final double EPSILON = 1e-6;
    private static final double HALF_TILE = TileMap.TILE_SIZE / 2.0;

    private final TileMap map;
    /** 놓인 물풍선. 놓은 순서 */
    private final List<WaterBalloon> balloons = new ArrayList<>();
    /** 물줄기가 아직 보이는 폭발. 터진 순서 */
    private final List<Explosion> explosions = new ArrayList<>();
    /** 마지막으로 {@link #takeNewExplosions}를 부른 뒤에 터진 것 */
    private final List<Explosion> fresh = new ArrayList<>();

    public BalloonField(TileMap map) {
        this.map = map;
    }

    /**
     * dt초를 흘린다. 다 보인 물줄기를 지우고, 놓인 칸에서 빠져나간 플레이어를 막고, 밀리거나 던져진 물풍선을 옮기고,
     * 때가 된 물풍선을 터뜨린다. 날아가는 물풍선은 때가 지나도 내려앉을 때까지 기다렸다 터진다.
     *
     * @param players 맵 위의 모든 플레이어. 놓인 칸을 벗어났는지 볼 때 쓴다
     */
    public void update(double dt, List<Player> players) {
        for (Explosion explosion : explosions) {
            explosion.advance(dt);
        }
        explosions.removeIf(Explosion::isFinished);
        List<WaterBalloon> due = new ArrayList<>();
        for (WaterBalloon balloon : List.copyOf(balloons)) {
            balloon.forgetLeavers(players);
            balloon.advance(dt);
            if (balloon.isSliding()) {
                slide(balloon, dt, players, due);
            } else if (balloon.isFlying()) {
                Direction direction = balloon.flightDirection();
                if (balloon.fly(dt)) {
                    land(balloon, direction, players, due);
                }
            }
            if (!balloon.isFlying() && balloon.isDue()) {
                due.add(balloon);
            }
        }
        for (WaterBalloon balloon : due) {
            explode(balloon);
        }
    }

    /**
     * 가만히 놓인 balloon을 direction 쪽으로 민다. 미끄러질 수 있으면 미끄러지기 시작한다.
     * 바로 앞이 막혀 있으면 움직이지 않는데, 그 블럭의 가시 난 면을 향해 밀었으면 그 자리에서 터진다.
     *
     * @return 미끄러지기 시작했거나 가시에 터졌는지. 이미 움직이는 중이거나 바로 앞이 막혀 그대로면 false
     */
    public boolean push(WaterBalloon balloon, Direction direction) {
        if (!balloons.contains(balloon) || !balloon.isResting()) {
            return false;
        }
        balloon.startSliding(direction);
        return switch (nextMove(balloon)) {
            case CONTINUE -> true;
            case POP -> {
                balloon.stopSliding();
                explode(balloon);
                yield true;
            }
            case STOP -> {
                balloon.stopSliding();
                yield false;
            }
        };
    }

    /**
     * 가만히 놓인 balloon을 direction 쪽으로 던진다. 내려앉을 칸은 {@link #landingCell}로 정하고,
     * 거리에 맞춰 {@link #FLIGHT_SECONDS_PER_TILE}초씩(적어도 {@link #MIN_FLIGHT_SECONDS}초) 난다.
     *
     * @return 던졌는지. 이미 움직이는 중이면 false
     */
    public boolean throwBalloon(WaterBalloon balloon, Direction direction) {
        if (!balloons.contains(balloon) || !balloon.isResting()) {
            return false;
        }
        Point landing = landingCell(balloon.col(), balloon.row(), direction, THROW_DISTANCE, balloon);
        fly(balloon, direction, landing);
        return true;
    }

    /**
     * (col, row)에서 direction 쪽으로 distance칸째부터 찾은, 던진 물풍선이 내려앉을 칸.
     *
     * <ol>
     *   <li>distance칸째가 비어 있으면 거기. 비어 있지 않으면(통과할 수 없는 블럭이거나 이미 물풍선이 있으면)
     *       같은 방향으로 다음 빈 칸까지 더 간다. 캐릭터·덤불이 있는 칸은 빈 칸이다.</li>
     *   <li>맵 끝까지 빈 칸이 없으면(distance칸째가 맵 밖인 경우도) 맵 끝에서 거꾸로 돌아오며 처음 만난 빈 칸.</li>
     *   <li>그것도 없으면 (col, row) 그대로.</li>
     * </ol>
     *
     * @param self 내려앉을 물풍선 자신. 자기가 있는 칸은 비어 있는 것으로 본다
     */
    Point landingCell(int col, int row, Direction direction, int distance, WaterBalloon self) {
        int last = 0;
        for (int step = 1; map.isInside(col + direction.dx() * step, row + direction.dy() * step); step++) {
            last = step;
        }
        for (int step = distance; step <= last; step++) {
            if (canLand(col + direction.dx() * step, row + direction.dy() * step, self)) {
                return new Point(col + direction.dx() * step, row + direction.dy() * step);
            }
        }
        for (int step = Math.min(last, distance - 1); step >= 1; step--) {
            if (canLand(col + direction.dx() * step, row + direction.dy() * step, self)) {
                return new Point(col + direction.dx() * step, row + direction.dy() * step);
            }
        }
        return new Point(col, row);
    }

    /** 물풍선이 내려앉을 수 있는 칸인지: 맵 안의 통과할 수 있는 칸이고, 다른 물풍선이 없고 다른 물풍선이 내려앉으려는 칸도 아니다 */
    private boolean canLand(int col, int row, WaterBalloon self) {
        if (map.isSolid(col, row)) {
            return false;
        }
        for (WaterBalloon other : balloons) {
            if (other != self && other.col() == col && other.row() == row) {
                return false;
            }
        }
        return true;
    }

    private void fly(WaterBalloon balloon, Direction direction, Point landing) {
        int tiles = Math.abs(landing.x - balloon.col()) + Math.abs(landing.y - balloon.row());
        balloon.startFlying(direction, landing.x, landing.y,
                Math.max(MIN_FLIGHT_SECONDS, tiles * FLIGHT_SECONDS_PER_TILE));
    }

    /**
     * 날던 물풍선이 내려앉는다. 나는 사이 그 칸이 막혔거나 다른 물풍선이 놓였으면 같은 방향의 다음 빈 칸으로 더 날아간다.
     * 내려앉은 칸에 사람이 있으면 그 칸을 벗어날 때까지 지나가게 두고, 물줄기가 남은 칸이면 바로 터진다.
     */
    private void land(WaterBalloon balloon, Direction direction, List<Player> players, List<WaterBalloon> burst) {
        if (!canLand(balloon.col(), balloon.row(), balloon)) {
            Point next = landingCell(balloon.col(), balloon.row(), direction, 1, balloon);
            if (next.x != balloon.col() || next.y != balloon.row()) {
                fly(balloon, direction, next);
                return;
            }
            // 어디에도 내려앉을 수 없으면 그 자리에서 터진다
            burst.add(balloon);
            return;
        }
        admitOverlapping(balloon, players);
        if (isWet(balloon.col(), balloon.row())) {
            burst.add(balloon);
        }
    }

    /**
     * 미끄러지는 balloon을 dt초만큼 옮긴다. 칸 가운데에 올 때마다 {@link #nextMove}로 더 갈지 정하고,
     * 새 칸에 들어가면 그 칸에 있던 사람을 지나가게 두며, 물줄기가 남은 칸이면 터뜨린다.
     */
    private void slide(WaterBalloon balloon, double dt, List<Player> players, List<WaterBalloon> burst) {
        double remaining = SLIDE_TILES_PER_SECOND * TileMap.TILE_SIZE * dt;
        while (remaining > EPSILON && balloon.isSliding()) {
            double offset = balloon.slideOffset();
            if (offset < -EPSILON) {
                // 새 칸 가운데로 다가가는 중
                double step = Math.min(remaining, -offset);
                remaining -= step;
                balloon.setSlideOffset(offset + step > -EPSILON ? 0 : offset + step);
                continue;
            }
            if (offset <= EPSILON) {
                Move move = nextMove(balloon);
                if (move != Move.CONTINUE) {
                    balloon.stopSliding();
                    if (move == Move.POP) {
                        burst.add(balloon);
                    }
                    return;
                }
            }
            double step = Math.min(remaining, HALF_TILE - offset);
            remaining -= step;
            balloon.setSlideOffset(offset + step);
            if (offset + step >= HALF_TILE - EPSILON) {
                balloon.setSlideOffset(HALF_TILE);
                balloon.stepCell();
                admitOverlapping(balloon, players);
                if (isWet(balloon.col(), balloon.row())) {
                    balloon.stopSliding();
                    burst.add(balloon);
                    return;
                }
            }
        }
    }

    /**
     * 칸 가운데에 온 미끄러지는 balloon이 다음 칸으로 갈지. 앞쪽으로 통과할 수 없는 블럭(맵 끝 포함)이 나오기 전까지의 칸 중
     * 다른 물풍선이 없는 가장 먼 칸이 멈출 칸이고, 그 칸이 지금 칸보다 앞이면 더 간다.
     * 바로 앞 칸이 블럭이라 멈추는데 그 블럭의 가시 난 면을 향해 왔으면 터진다.
     */
    private Move nextMove(WaterBalloon balloon) {
        Direction direction = balloon.slideDirection();
        int nextCol = balloon.col() + direction.dx();
        int nextRow = balloon.row() + direction.dy();
        if (map.isSolid(nextCol, nextRow)) {
            boolean thorn = map.isInside(nextCol, nextRow) && map.tileAt(nextCol, nextRow).popsBalloonMoving(direction);
            return thorn ? Move.POP : Move.STOP;
        }
        for (int step = 1; ; step++) {
            int col = balloon.col() + direction.dx() * step;
            int row = balloon.row() + direction.dy() * step;
            if (map.isSolid(col, row)) {
                return Move.STOP;
            }
            if (canLand(col, row, balloon)) {
                return Move.CONTINUE;
            }
        }
    }

    /** 미끄러지는 물풍선이 칸 가운데에서 할 일 */
    private enum Move {
        CONTINUE,
        STOP,
        POP
    }

    /** balloon이 있는 칸에 몸이 걸친 사람은 그 칸을 벗어날 때까지 지나갈 수 있다 */
    private static void admitOverlapping(WaterBalloon balloon, List<Player> players) {
        for (Player player : players) {
            if (player.overlapsTile(balloon.col(), balloon.row())) {
                balloon.admit(player);
            }
        }
    }

    /**
     * player가 몸이 걸친 칸의 물풍선을 그 칸을 벗어날 때까지 지나갈 수 있게 한다.
     * 점프처럼 걷지 않고 물풍선이 있는 칸에 내려선 사람이 갇히지 않게 할 때 쓴다.
     */
    public void letLeave(Player player) {
        for (WaterBalloon balloon : balloons) {
            if (!balloon.isFlying() && player.overlapsTile(balloon.col(), balloon.row())) {
                balloon.admit(player);
            }
        }
    }

    /**
     * player가 선 칸(충돌 박스 가운데가 있는 칸)에 물풍선을 놓는다. 놓을 수 없으면 아무 일도 없다.
     * 놓는 순간 그 칸에 몸이 걸쳐 있는 플레이어(놓은 사람 포함)는 그 칸을 벗어날 때까지 지나갈 수 있다.
     *
     * @param players 맵 위의 모든 플레이어
     * @return 놓은 물풍선. 갇혀 있거나, 이미 놓아 둔 개수가 꽉 찼거나, 그 칸에 물풍선이 있으면 비어 있다
     */
    public Optional<WaterBalloon> place(Player player, List<Player> players) {
        int col = player.col();
        int row = player.row();
        if (!player.canPlaceBalloon() || map.isSolid(col, row) || balloonAt(col, row).isPresent()
                || placedBy(player) >= player.balloons()) {
            return Optional.empty();
        }
        List<Integer> passers = players.stream().filter(p -> p.overlapsTile(col, row)).map(Player::id).toList();
        WaterBalloon balloon = new WaterBalloon(player.id(), col, row, player.waterRange(), passers);
        balloons.add(balloon);
        if (isWet(col, row)) {
            explode(balloon);
        }
        return Optional.of(balloon);
    }

    /** (col, row) 칸의 물풍선을 바로 터뜨린다. 물풍선이 없으면 아무 일도 없다 */
    public void detonate(int col, int row) {
        balloonAt(col, row).ifPresent(this::explode);
    }

    /** player가 지금 놓아 둔(아직 안 터진) 물풍선 개수 */
    public int placedBy(Player player) {
        return (int) balloons.stream().filter(balloon -> balloon.ownerId() == player.id()).count();
    }

    /** (col, row) 칸이 player에게 막힌 칸인지. 물풍선이 놓여 있고, player가 놓는 순간부터 그 칸에 있던 사람이 아니면 막혔다 */
    public boolean blocks(Player player, int col, int row) {
        return balloonAt(col, row).map(balloon -> !balloon.lets(player)).orElse(false);
    }

    /** (col, row) 칸에 놓인 물풍선. 미끄러지는 물풍선은 가운데가 가장 가까운 칸에 있고, 날아가는 물풍선은 어느 칸에도 없다 */
    public Optional<WaterBalloon> balloonAt(int col, int row) {
        for (WaterBalloon balloon : balloons) {
            if (!balloon.isFlying() && balloon.col() == col && balloon.row() == row) {
                return Optional.of(balloon);
            }
        }
        return Optional.empty();
    }

    /** (col, row) 칸에 물줄기가 보이는지 */
    public boolean isWet(int col, int row) {
        return wetOwnerAt(col, row).isPresent();
    }

    /** (col, row) 칸을 덮은 물줄기의 주인 번호. 여럿이면 가장 늦게 터진 것이고, 물줄기가 없으면 비어 있다 */
    public OptionalInt wetOwnerAt(int col, int row) {
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion explosion = explosions.get(i);
            if (explosion.covers(col, row)) {
                return OptionalInt.of(explosion.ownerId());
            }
        }
        return OptionalInt.empty();
    }

    /** 놓인 물풍선들(미끄러지거나 날아가는 것 포함). 놓은 순서 */
    public List<WaterBalloon> balloons() {
        return Collections.unmodifiableList(balloons);
    }

    /** 물줄기가 아직 보이는 폭발들. 터진 순서 */
    public List<Explosion> explosions() {
        return Collections.unmodifiableList(explosions);
    }

    /** 지난번에 부른 뒤로 새로 터진 폭발들. 한 번 돌려준 폭발은 다시 돌려주지 않는다 */
    public List<Explosion> takeNewExplosions() {
        List<Explosion> taken = List.copyOf(fresh);
        fresh.clear();
        return taken;
    }

    /** first를 터뜨리고, 물줄기에 닿은 물풍선을 이어서 터뜨린다 */
    private void explode(WaterBalloon first) {
        Deque<WaterBalloon> queue = new ArrayDeque<>();
        queue.add(first);
        while (!queue.isEmpty()) {
            WaterBalloon balloon = queue.poll();
            if (balloons.remove(balloon)) {
                Explosion explosion = burst(balloon, queue);
                explosions.add(explosion);
                fresh.add(explosion);
            }
        }
    }

    /** balloon이 터진 물줄기 칸을 정한다. 물줄기가 닿은 다른 물풍선은 chain에 넣는다 */
    private Explosion burst(WaterBalloon balloon, Deque<WaterBalloon> chain) {
        List<WaterCell> cells = new ArrayList<>();
        List<Point> blocked = new ArrayList<>();
        cells.add(new WaterCell(balloon.col(), balloon.row(), Part.CENTER, null));
        for (Direction direction : Direction.values()) {
            List<WaterCell> arm = new ArrayList<>();
            boolean joined = false;
            for (int step = 1; step <= balloon.range(); step++) {
                int col = balloon.col() + direction.dx() * step;
                int row = balloon.row() + direction.dy() * step;
                if (map.isSolid(col, row)) {
                    if (map.isInside(col, row)) {
                        blocked.add(new Point(col, row));
                    }
                    break;
                }
                Optional<WaterBalloon> other = balloonAt(col, row);
                if (other.isPresent()) {
                    // 닿은 물풍선이 같은 순간 터져 그 칸을 덮으므로, 이 물줄기는 끝 모양 없이 그 칸에 이어진다
                    chain.add(other.get());
                    joined = true;
                    break;
                }
                arm.add(new WaterCell(col, row, Part.MIDDLE, direction));
            }
            if (!arm.isEmpty() && !joined) {
                WaterCell last = arm.removeLast();
                arm.add(new WaterCell(last.col(), last.row(), Part.END, direction));
            }
            cells.addAll(arm);
        }
        return new Explosion(balloon.ownerId(), balloon.col(), balloon.row(), cells, blocked);
    }
}
