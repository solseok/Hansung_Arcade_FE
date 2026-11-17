package hansung.arcade.ingame.entity;

import hansung.arcade.ingame.map.TileMap;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * 플레이어 캐릭터. 위치 (x, y)는 충돌 박스 중심의 픽셀 좌표다.
 * 물풍선 개수·물줄기 길이·이동속도 단계는 고른 캐릭터({@link Mascot})의 시작값으로 시작해 그 캐릭터의 최댓값까지 오른다.
 *
 * <p>물풍선 싸움의 상태({@link PlayerState})도 여기서 센다. 물줄기에 맞으면 {@link #trap 갇히고},
 * 같은 편이 닿으면 {@link #escape 빠져나오고}, 상대가 닿거나 갇힌 채 {@link #TRAP_SECONDS}초가 지나면 {@link #eliminate 탈락}한다.
 * 누가 언제 갇히고 풀리는지는 게임 상태(GameWorld)가 정하고, 여기서는 상태마다 흐르는 시간과 연출 길이를 센다.</p>
 */
public final class Player {

    /** 충돌 박스 절반 크기. 타일보다 약간 작아서 한 칸 통로를 지날 때 여유가 조금 있다. */
    static final double HALF_HITBOX = TileMap.TILE_SIZE / 2.0 - 2;
    /** 물풍선에 갇혀 있는 시간(초). 이 동안 아무도 건드리지 않으면 탈락한다 */
    public static final double TRAP_SECONDS = 5.0;
    /** 갇혀 있는 동안의 이동 속도 (초당 픽셀). 3초에 한 칸 */
    public static final double TRAPPED_SPEED = TileMap.TILE_SIZE / 3.0;
    /** 갇힌 물풍선이 깨져 나오는 연출 길이(초). 바늘 그림 5장을 0.1초씩 보여 준다 */
    public static final double ESCAPE_SECONDS = 0.5;
    /** 탈락 연출 중 쓰러진 채 위로 솟았다 떨어지는 시간(초) */
    public static final double DYING_JUMP_SECONDS = 1.0;
    /** 탈락 연출 중 떨어진 뒤 깜빡이는 시간(초). 0.2초에 한 번씩 5번 깜빡이고 사라진다 */
    public static final double DYING_BLINK_SECONDS = 1.0;
    /** 두 캐릭터가 닿았다고 보는 거리. 중심끼리 가로·세로 모두 반 칸보다 가까우면 닿은 것이다 */
    static final double TOUCH_DISTANCE = TileMap.TILE_SIZE / 2.0;
    /** 한 번에 움직이는 최대 거리. 벽을 뚫고 지나가지 않도록 잘게 나눠 이동한다. */
    private static final double MAX_SUB_STEP = 1.0;
    private static final double EPSILON = 1e-6;
    /**
     * 상태 시간은 1/60초씩 더한 값이라 5초 같은 경계에서 4.9999…처럼 살짝 못 미친다.
     * 그대로 비교하면 한 틱 늦게 넘어가므로 경계에 이만큼 여유를 준다.
     */
    private static final double TIME_SLACK = 1e-6;

    private final int id;
    private final PlayerProfile profile;
    private double x;
    private double y;
    private int balloons;
    private int waterRange;
    private int speedLevel;
    private Direction facing = Direction.DOWN;
    private boolean moving;
    private double walkTime;
    private boolean alive = true;
    private PlayerState state = PlayerState.NORMAL;
    /** 지금 상태가 된 뒤 흐른 시간(초) */
    private double stateTime;
    /** 나를 가둔 물줄기의 주인 번호. 갇힌 적이 없으면 null */
    private Integer trapperId;

    /** 닉네임·캐릭터를 아직 모를 때(테스트 등). {@link PlayerProfile#placeholder} 정보를 쓴다. */
    public Player(int id, int spawnCol, int spawnRow) {
        this(id, PlayerProfile.placeholder(id), spawnCol, spawnRow);
    }

    public Player(int id, PlayerProfile profile, int spawnCol, int spawnRow) {
        this.id = id;
        this.profile = Objects.requireNonNull(profile);
        this.x = TileMap.centerX(spawnCol);
        this.y = TileMap.centerY(spawnRow);
        Mascot mascot = profile.mascot();
        this.balloons = mascot.balloons().base();
        this.waterRange = mascot.waterRange().base();
        this.speedLevel = mascot.speed().base();
    }

    /** 맵의 막힌 칸만 피해서 움직인다. {@link #update(Direction, double, Obstacles)}와 같다. */
    public void update(Direction input, double dt, TileMap map) {
        update(input, dt, map::isSolid);
    }

    /**
     * dt초만큼 상태 시간을 흘리고, 입력 방향으로 움직인다. 막히면 모서리 보정으로 통로 쪽으로 미끄러진다.
     * 갇히면 {@link #TRAPPED_SPEED}로 느리게 움직이고, 빠져나오는 중·탈락한 뒤에는 움직이지 않는다.
     * 빠져나오는 연출과 탈락 연출이 끝나면 각각 보통 상태, 사라진 상태로 넘어간다.
     *
     * @param input     누르고 있는 방향. 없으면 null
     * @param obstacles 들어갈 수 없는 칸 (맵의 막힌 칸과 놓인 물풍선 등)
     */
    public void update(Direction input, double dt, Obstacles obstacles) {
        stateTime += dt;
        if (state == PlayerState.ESCAPING && stateTime >= ESCAPE_SECONDS - TIME_SLACK) {
            // 바늘 그림 마지막 장이 정면으로 서 있는 모습이라 그대로 이어지게 아래를 본다
            changeState(PlayerState.NORMAL);
            facing = Direction.DOWN;
        } else if (state == PlayerState.DYING && stateTime >= DYING_JUMP_SECONDS + DYING_BLINK_SECONDS - TIME_SLACK) {
            changeState(PlayerState.GONE);
        }
        moving = false;
        if (input == null || !canMove()) {
            walkTime = 0;
            return;
        }
        facing = input;
        double remaining = (state == PlayerState.TRAPPED ? TRAPPED_SPEED : speed()) * dt;
        while (remaining > EPSILON) {
            double step = Math.min(remaining, MAX_SUB_STEP);
            if (!tryStep(input, step, obstacles)) {
                break;
            }
            moving = true;
            remaining -= step;
        }
        walkTime = moving ? walkTime + dt : 0;
    }

    private boolean canMove() {
        return state == PlayerState.NORMAL || state == PlayerState.TRAPPED;
    }

    private boolean tryStep(Direction dir, double step, Obstacles obstacles) {
        double nx = x + dir.dx() * step;
        double ny = y + dir.dy() * step;
        if (!collides(nx, ny, obstacles)) {
            x = nx;
            y = ny;
            return true;
        }
        return slideAroundCorner(dir, step, obstacles);
    }

    /**
     * 진행 방향이 막혔을 때, 내가 속한 줄(행/열)의 앞칸이 비어 있으면
     * 그 줄 중심 쪽으로 미끄러져 정렬한다. 모서리에 걸려 멈추는 답답함을 줄인다.
     */
    private boolean slideAroundCorner(Direction dir, double step, Obstacles obstacles) {
        int col = TileMap.colAt(x);
        int row = TileMap.rowAt(y);
        if (obstacles.blocks(col + dir.dx(), row + dir.dy())) {
            return false;
        }
        double offset = dir.isHorizontal() ? y - TileMap.centerY(row) : x - TileMap.centerX(col);
        if (Math.abs(offset) < EPSILON) {
            return false;
        }
        double slide = Math.min(step, Math.abs(offset)) * Math.signum(offset);
        double sx = dir.isHorizontal() ? x : x - slide;
        double sy = dir.isHorizontal() ? y - slide : y;
        if (collides(sx, sy, obstacles)) {
            return false;
        }
        x = sx;
        y = sy;
        return true;
    }

    private static boolean collides(double cx, double cy, Obstacles obstacles) {
        int left = TileMap.colAt(cx - HALF_HITBOX);
        int right = TileMap.colAt(cx + HALF_HITBOX - EPSILON);
        int top = TileMap.rowAt(cy - HALF_HITBOX);
        int bottom = TileMap.rowAt(cy + HALF_HITBOX - EPSILON);
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                if (obstacles.blocks(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 걷지 않고 (x, y) 픽셀 자리로 옮긴다 (스프링 신발 점프처럼 막힌 칸 위를 지나갈 때).
     * 막힌 칸인지는 보지 않으므로 부르는 쪽이 내려설 수 있는 칸인지 확인한다.
     */
    public void placeAt(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** 충돌 박스가 (col, row) 칸에 조금이라도 걸쳐 있는지. 칸 경계에 딱 맞닿은 것은 걸친 것으로 보지 않는다 */
    public boolean overlapsTile(int col, int row) {
        int left = TileMap.colAt(x - HALF_HITBOX);
        int right = TileMap.colAt(x + HALF_HITBOX - EPSILON);
        int top = TileMap.rowAt(y - HALF_HITBOX);
        int bottom = TileMap.rowAt(y + HALF_HITBOX - EPSILON);
        return col >= left && col <= right && row >= top && row <= bottom;
    }

    /** 다른 캐릭터와 닿았는지. 중심끼리 가로·세로 모두 반 칸({@link #TOUCH_DISTANCE})보다 가까우면 닿은 것이다 */
    public boolean touches(Player other) {
        return Math.abs(x - other.x) < TOUCH_DISTANCE && Math.abs(y - other.y) < TOUCH_DISTANCE;
    }

    public int id() {
        return id;
    }

    public PlayerProfile profile() {
        return profile;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public int col() {
        return TileMap.colAt(x);
    }

    public int row() {
        return TileMap.rowAt(y);
    }

    public Direction facing() {
        return facing;
    }

    /** 이번 판에서 아직 살아 있는지. 승패 판정에 쓴다. 갇혀 있거나 빠져나오는 중이어도 살아 있다 */
    public boolean isAlive() {
        return alive;
    }

    /**
     * 이번 판에서 탈락시킨다 (갇힌 채 상대가 닿았거나 시간이 다 됐을 때 등). 한 번 탈락하면 그 판에서는 되살아나지 않는다.
     * 바로 탈락 연출({@link PlayerState#DYING})이 시작되고, 연출이 끝나면 맵에서 사라진다. 이미 탈락했으면 아무 일도 없다.
     */
    public void eliminate() {
        if (!alive) {
            return;
        }
        alive = false;
        changeState(PlayerState.DYING);
    }

    /**
     * 물줄기에 맞아 물풍선에 갇힌다. 보통 상태일 때만 갇히고, 이미 갇혔거나 빠져나오는 중·탈락한 뒤에는 아무 일도 없다.
     *
     * @param ownerId 맞은 물줄기를 만든 물풍선의 주인 번호. 시간이 다 돼 탈락할 때 kill을 누구에게 줄지 정한다
     * @return 이번에 갇혔는지
     */
    public boolean trap(int ownerId) {
        if (state != PlayerState.NORMAL) {
            return false;
        }
        changeState(PlayerState.TRAPPED);
        trapperId = ownerId;
        return true;
    }

    /**
     * 갇힌 물풍선이 깨져 빠져나오기 시작한다 (같은 편이 닿았을 때, 이후 바늘 아이템).
     * {@link #ESCAPE_SECONDS}초 동안 움직이지 못하고, 연출이 끝나면 보통 상태로 돌아간다. 갇혀 있을 때만 된다.
     *
     * @return 이번에 빠져나오기 시작했는지
     */
    public boolean escape() {
        if (state != PlayerState.TRAPPED) {
            return false;
        }
        changeState(PlayerState.ESCAPING);
        return true;
    }

    /** 갇힌 채 {@link #TRAP_SECONDS}초가 지나 탈락할 때가 됐는지 */
    public boolean isTrapTimeUp() {
        return state == PlayerState.TRAPPED && stateTime >= TRAP_SECONDS - TIME_SLACK;
    }

    /** 물풍선을 놓을 수 있는 상태인지. 갇히거나 빠져나오는 중·탈락한 뒤에는 놓지 못한다 */
    public boolean canPlaceBalloon() {
        return state == PlayerState.NORMAL;
    }

    public PlayerState state() {
        return state;
    }

    /** 지금 상태가 된 뒤 흐른 시간(초). 갇힌 그림·탈락 연출을 고를 때 쓴다 */
    public double stateTime() {
        return stateTime;
    }

    /** 마지막으로 나를 가둔 물줄기의 주인 번호. 갇힌 적이 없으면 비어 있다 */
    public OptionalInt trapperId() {
        return trapperId == null ? OptionalInt.empty() : OptionalInt.of(trapperId);
    }

    private void changeState(PlayerState next) {
        state = next;
        stateTime = 0;
        moving = false;
        walkTime = 0;
    }

    public boolean isMoving() {
        return moving;
    }

    /** 걷기 애니메이션용 누적 시간(초). 멈추면 0으로 돌아간다. */
    public double walkTime() {
        return walkTime;
    }

    /** 현재 이동 속도 (초당 픽셀) */
    public double speed() {
        return SpeedLevel.pixelsPerSecond(speedLevel);
    }

    /** 이동속도 단계 ({@link SpeedLevel#MIN}~캐릭터의 최댓값) */
    public int speedLevel() {
        return speedLevel;
    }

    /** 한 번에 놓아 둘 수 있는 물풍선 개수 */
    public int balloons() {
        return balloons;
    }

    /** 물풍선이 터질 때 상하좌우로 뻗는 물줄기 길이(칸) */
    public int waterRange() {
        return waterRange;
    }

    /** 물풍선 개수를 delta만큼 바꾼다. 캐릭터의 시작값~최댓값을 벗어나지 않는다. */
    public void addBalloons(int delta) {
        balloons = profile.mascot().balloons().clamp((long) balloons + delta);
    }

    /** 물줄기 길이를 delta만큼 바꾼다. 캐릭터의 시작값~최댓값을 벗어나지 않는다. */
    public void addWaterRange(int delta) {
        waterRange = profile.mascot().waterRange().clamp((long) waterRange + delta);
    }

    /**
     * 이동속도 단계를 delta만큼 바꾼다. 빨라질 때는 캐릭터의 최댓값까지,
     * 느려질 때는 캐릭터와 상관없이 {@link SpeedLevel#MIN}까지 내려간다.
     */
    public void addSpeedLevel(int delta) {
        int max = profile.mascot().speed().max();
        speedLevel = Math.clamp((long) speedLevel + delta, SpeedLevel.MIN, max);
    }
}
