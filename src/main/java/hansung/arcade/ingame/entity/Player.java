package hansung.arcade.ingame.entity;

import hansung.arcade.ingame.map.TileMap;

import java.util.Objects;

/**
 * 플레이어 캐릭터. 위치 (x, y)는 충돌 박스 중심의 픽셀 좌표다.
 */
public final class Player {

    /** 기본 이동 속도 (초당 픽셀) */
    public static final double BASE_SPEED = TileMap.TILE_SIZE * 3.0;
    /** 충돌 박스 절반 크기. 타일보다 약간 작아서 한 칸 통로를 지날 때 여유가 조금 있다. */
    static final double HALF_HITBOX = TileMap.TILE_SIZE / 2.0 - 2;
    /** 한 번에 움직이는 최대 거리. 벽을 뚫고 지나가지 않도록 잘게 나눠 이동한다. */
    private static final double MAX_SUB_STEP = 1.0;
    private static final double EPSILON = 1e-6;

    private final int id;
    private final PlayerProfile profile;
    private double x;
    private double y;
    private double speed = BASE_SPEED;
    private Direction facing = Direction.DOWN;
    private boolean moving;
    private double walkTime;

    /** 닉네임·캐릭터를 아직 모를 때(테스트 등). {@link PlayerProfile#placeholder} 정보를 쓴다. */
    public Player(int id, int spawnCol, int spawnRow) {
        this(id, PlayerProfile.placeholder(id), spawnCol, spawnRow);
    }

    public Player(int id, PlayerProfile profile, int spawnCol, int spawnRow) {
        this.id = id;
        this.profile = Objects.requireNonNull(profile);
        this.x = TileMap.centerX(spawnCol);
        this.y = TileMap.centerY(spawnRow);
    }

    /**
     * 입력 방향으로 dt초만큼 이동한다. 막히면 모서리 보정으로 통로 쪽으로 미끄러진다.
     *
     * @param input 누르고 있는 방향. 없으면 null
     */
    public void update(Direction input, double dt, TileMap map) {
        moving = false;
        if (input == null) {
            walkTime = 0;
            return;
        }
        facing = input;
        double remaining = speed * dt;
        while (remaining > EPSILON) {
            double step = Math.min(remaining, MAX_SUB_STEP);
            if (!tryStep(input, step, map)) {
                break;
            }
            moving = true;
            remaining -= step;
        }
        walkTime = moving ? walkTime + dt : 0;
    }

    private boolean tryStep(Direction dir, double step, TileMap map) {
        double nx = x + dir.dx() * step;
        double ny = y + dir.dy() * step;
        if (!collides(nx, ny, map)) {
            x = nx;
            y = ny;
            return true;
        }
        return slideAroundCorner(dir, step, map);
    }

    /**
     * 진행 방향이 막혔을 때, 내가 속한 줄(행/열)의 앞칸이 비어 있으면
     * 그 줄 중심 쪽으로 미끄러져 정렬한다. 모서리에 걸려 멈추는 답답함을 줄인다.
     */
    private boolean slideAroundCorner(Direction dir, double step, TileMap map) {
        int col = TileMap.colAt(x);
        int row = TileMap.rowAt(y);
        if (map.isSolid(col + dir.dx(), row + dir.dy())) {
            return false;
        }
        double offset = dir.isHorizontal() ? y - TileMap.centerY(row) : x - TileMap.centerX(col);
        if (Math.abs(offset) < EPSILON) {
            return false;
        }
        double slide = Math.min(step, Math.abs(offset)) * Math.signum(offset);
        double sx = dir.isHorizontal() ? x : x - slide;
        double sy = dir.isHorizontal() ? y - slide : y;
        if (collides(sx, sy, map)) {
            return false;
        }
        x = sx;
        y = sy;
        return true;
    }

    private static boolean collides(double cx, double cy, TileMap map) {
        int left = TileMap.colAt(cx - HALF_HITBOX);
        int right = TileMap.colAt(cx + HALF_HITBOX - EPSILON);
        int top = TileMap.rowAt(cy - HALF_HITBOX);
        int bottom = TileMap.rowAt(cy + HALF_HITBOX - EPSILON);
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                if (map.isSolid(col, row)) {
                    return true;
                }
            }
        }
        return false;
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

    public boolean isMoving() {
        return moving;
    }

    /** 걷기 애니메이션용 누적 시간(초). 멈추면 0으로 돌아간다. */
    public double walkTime() {
        return walkTime;
    }

    public double speed() {
        return speed;
    }
}
