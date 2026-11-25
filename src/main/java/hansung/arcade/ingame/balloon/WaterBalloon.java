package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.map.TileMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 맵 칸 가운데에 놓인 물풍선 하나. 놓은 뒤 {@link #FUSE_SECONDS}초가 지나면 터진다.
 * 놓인 칸은 막힌 칸이지만, 놓는 순간 그 칸에 몸이 걸쳐 있던 플레이어는 그 칸을 벗어날 때까지 지나갈 수 있다.
 *
 * <p>신발 아이템으로 밀면 한 방향으로 미끄러지고({@link #isSliding()}), 장갑 아이템으로 던지면 공중을 날아
 * 다른 칸에 내려앉는다({@link #isFlying()}). 움직이는 동안에도 터질 때까지 남은 시간은 그대로 흐른다.
 * 어떻게 움직일지는 {@link BalloonField}가 정하고, 여기서는 지금 어디쯤 있는지만 센다.</p>
 */
public final class WaterBalloon {

    /** 놓은 뒤 터질 때까지의 시간(초) */
    public static final double FUSE_SECONDS = 2.0;
    /** 놓인 물풍선 그림 한 장을 보여 주는 시간(초). 4장을 되풀이한다 */
    public static final double FRAME_SECONDS = 0.2;
    /** 던진 물풍선이 날아가며 가장 높이 뜨는 높이(px). 한 칸 */
    public static final double FLIGHT_HEIGHT = TileMap.TILE_SIZE;
    /** 1/60초씩 더한 나이가 경계에서 살짝 못 미치는 것을 메운다 */
    private static final double TIME_SLACK = 1e-6;

    private final int ownerId;
    /** 지금 있는 칸. 미끄러지는 중이면 가운데가 가장 가까운 칸이고, 나는 중이면 내려앉을 칸이다 */
    private int col;
    private int row;
    private final int range;
    /** 아직 이 칸을 벗어나지 않아 지나갈 수 있는 플레이어 번호 */
    private final Set<Integer> passers;
    private double age;
    /** 미끄러지는 방향. 미끄러지지 않으면 null */
    private Direction slideDirection;
    /** 칸 가운데에서 미끄러지는 방향으로 간 거리(px). 칸 가운데로 다가오는 중이면 음수다 */
    private double slideOffset;
    /** 날아가는 중이면 그 비행. 아니면 null */
    private Flight flight;
    /** 마지막으로 던져진 방향. 내려앉을 칸이 막혔을 때 같은 방향으로 더 날아간다 */
    private Direction flightDirection;
    /** 언제 터질지 서버가 정하는지 (온라인). 그러면 시간이 지나도 스스로 터지지 않고 서버가 알릴 때 터진다 */
    private final boolean serverTimed;

    WaterBalloon(int ownerId, int col, int row, int range, Collection<Integer> passers) {
        this(ownerId, col, row, range, passers, false);
    }

    WaterBalloon(int ownerId, int col, int row, int range, Collection<Integer> passers, boolean serverTimed) {
        this.serverTimed = serverTimed;
        this.ownerId = ownerId;
        this.col = col;
        this.row = row;
        this.range = range;
        this.passers = new HashSet<>(passers);
    }

    /** 놓은 플레이어 번호 */
    public int ownerId() {
        return ownerId;
    }

    /** 지금 있는 칸. 날아가는 중이면 내려앉을 칸이다 */
    public int col() {
        return col;
    }

    public int row() {
        return row;
    }

    /** 터질 때 상하좌우로 뻗는 물줄기 길이(칸). 놓을 때 주인의 물줄기 길이로 정해진다 */
    public int range() {
        return range;
    }

    /** 놓은 뒤 흐른 시간(초). 그림을 고를 때 쓴다 */
    public double age() {
        return age;
    }

    /** player가 이 칸을 지나갈 수 있는지. 놓는 순간 몸이 걸쳐 있었고 아직 벗어나지 않았으면 지나갈 수 있다 */
    public boolean lets(Player player) {
        return passers.contains(player.id());
    }

    /** 밀려 미끄러지는 중인지 */
    public boolean isSliding() {
        return slideDirection != null;
    }

    /** 던져져 공중을 나는 중인지. 나는 동안은 어느 칸에도 놓이지 않아서 길을 막지 않고 물줄기에 맞지도 않는다 */
    public boolean isFlying() {
        return flight != null;
    }

    /** 칸에 가만히 놓여 있는지 (미끄러지거나 날지 않는지) */
    public boolean isResting() {
        return !isSliding() && !isFlying();
    }

    /** 땅 위 위치의 x(px, 맵 왼쪽 위 기준). 미끄러지거나 나는 중이면 칸 사이에 있다 */
    public double x() {
        if (flight != null) {
            return flight.x();
        }
        double x = TileMap.centerX(col);
        return slideDirection == null ? x : x + slideDirection.dx() * slideOffset;
    }

    /** 땅 위 위치의 y(px). 나는 중이면 그림자가 있는 자리다 */
    public double y() {
        if (flight != null) {
            return flight.y();
        }
        double y = TileMap.centerY(row);
        return slideDirection == null ? y : y + slideDirection.dy() * slideOffset;
    }

    /** 땅에서 떠 있는 높이(px). 날아가는 동안 포물선을 그리고 그 밖에는 0이다 */
    public double height() {
        return flight == null ? 0 : flight.height();
    }

    void advance(double dt) {
        age += dt;
    }

    boolean isDue() {
        return !serverTimed && age >= FUSE_SECONDS - TIME_SLACK;
    }

    /** 언제 터질지·어디로 움직일지 서버가 정하는 물풍선인지 (온라인). 그러면 이 컴퓨터의 규칙으로는 터뜨리지 않는다 */
    boolean isServerTimed() {
        return serverTimed;
    }

    /** 이 칸에서 몸이 완전히 빠져나간 플레이어는 다시 들어오지 못한다 */
    void forgetLeavers(List<Player> players) {
        for (Player player : players) {
            if (!player.overlapsTile(col, row)) {
                passers.remove(player.id());
            }
        }
    }

    /** player가 이 칸에서 빠져나갈 때까지 지나갈 수 있게 한다 (물풍선이 사람 있는 칸으로 들어왔을 때) */
    void admit(Player player) {
        passers.add(player.id());
    }

    Direction slideDirection() {
        return slideDirection;
    }

    double slideOffset() {
        return slideOffset;
    }

    void startSliding(Direction direction) {
        slideDirection = direction;
        slideOffset = 0;
    }

    void setSlideOffset(double offset) {
        slideOffset = offset;
    }

    /** 미끄러지는 방향으로 한 칸 옮긴다. 위치는 그대로라서 새 칸 가운데보다 한 칸만큼 뒤에서 다가오는 셈이 된다 */
    void stepCell() {
        col += slideDirection.dx();
        row += slideDirection.dy();
        slideOffset -= TileMap.TILE_SIZE;
    }

    void stopSliding() {
        slideDirection = null;
        slideOffset = 0;
    }

    /** (col, row) 칸 가운데에 바로 세운다. 미끄러지던 것은 멈춘다. 서버가 알린 칸으로 맞출 때 쓴다 */
    void settleAt(int col, int row) {
        stopSliding();
        this.col = col;
        this.row = row;
    }

    /** 지금 땅 위 위치에서 direction 쪽 (toCol, toRow) 칸으로 seconds초 동안 날아간다 */
    void startFlying(Direction direction, int toCol, int toRow, double seconds) {
        flightDirection = direction;
        flight = new Flight(x(), y(), TileMap.centerX(toCol), TileMap.centerY(toRow), seconds);
        slideDirection = null;
        slideOffset = 0;
        col = toCol;
        row = toRow;
    }

    Direction flightDirection() {
        return flightDirection;
    }

    /** 나는 시간을 dt초 흘린다. @return 이번에 내려앉았는지 */
    boolean fly(double dt) {
        flight.elapsed = Math.min(flight.seconds, flight.elapsed + dt);
        if (flight.elapsed < flight.seconds - TIME_SLACK) {
            return false;
        }
        flight = null;
        return true;
    }

    /** 공중을 나는 한 번. 땅 위 위치는 곧게 옮겨 가고, 높이는 포물선을 그린다 */
    private static final class Flight {
        private final double fromX;
        private final double fromY;
        private final double toX;
        private final double toY;
        private final double seconds;
        private double elapsed;

        Flight(double fromX, double fromY, double toX, double toY, double seconds) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.seconds = seconds;
        }

        private double progress() {
            return seconds <= 0 ? 1 : elapsed / seconds;
        }

        double x() {
            return fromX + (toX - fromX) * progress();
        }

        double y() {
            return fromY + (toY - fromY) * progress();
        }

        double height() {
            double t = progress();
            return FLIGHT_HEIGHT * 4 * t * (1 - t);
        }
    }
}
