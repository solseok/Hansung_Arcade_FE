package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.balloon.WaterCell.Part;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
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
 * </ul>
 *
 * <p>물줄기에 맞은 플레이어를 어떻게 할지는 게임 상태(GameWorld)가 {@link #wetOwnerAt}을 보고 정한다.
 * 블럭 파괴·아이템처럼 터질 때마다 할 일은 {@link #takeNewExplosions}로 이번에 터진 것을 받아 처리한다.</p>
 */
public final class BalloonField {

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
     * dt초를 흘린다. 다 보인 물줄기를 지우고, 놓인 칸에서 빠져나간 플레이어를 막고, 때가 된 물풍선을 터뜨린다.
     *
     * @param players 맵 위의 모든 플레이어. 놓인 칸을 벗어났는지 볼 때 쓴다
     */
    public void update(double dt, List<Player> players) {
        for (Explosion explosion : explosions) {
            explosion.advance(dt);
        }
        explosions.removeIf(Explosion::isFinished);
        List<WaterBalloon> due = new ArrayList<>();
        for (WaterBalloon balloon : balloons) {
            balloon.forgetLeavers(players);
            balloon.advance(dt);
            if (balloon.isDue()) {
                due.add(balloon);
            }
        }
        for (WaterBalloon balloon : due) {
            explode(balloon);
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

    public Optional<WaterBalloon> balloonAt(int col, int row) {
        for (WaterBalloon balloon : balloons) {
            if (balloon.col() == col && balloon.row() == row) {
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

    /** 놓인 물풍선들. 놓은 순서 */
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
