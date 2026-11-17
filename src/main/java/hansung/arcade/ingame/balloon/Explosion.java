package hansung.arcade.ingame.balloon;

import java.awt.Point;
import java.util.List;

/**
 * 물풍선 하나가 터진 것. 터진 칸에서 상하좌우로 뻗은 물줄기 칸들이 {@link #SECONDS}초 동안 보이고,
 * 그 동안 그 칸에 있는 플레이어는 물줄기에 맞는다. 그 뒤에는 아무것도 남지 않는다.
 */
public final class Explosion {

    /** 물줄기 그림 한 장을 보여 주는 시간(초) */
    public static final double FRAME_SECONDS = 0.15;
    /** 물줄기 그림 장 수 */
    public static final int FRAMES = 3;
    /** 물줄기가 보이는 시간(초). 이 시간이 지나면 잔상도 남지 않는다 */
    public static final double SECONDS = FRAME_SECONDS * FRAMES;
    /** 1/60초씩 더한 나이가 경계에서 살짝 못 미치는 것을 메운다 */
    private static final double TIME_SLACK = 1e-6;

    private final int ownerId;
    private final int col;
    private final int row;
    private final List<WaterCell> cells;
    private final List<Point> blockedCells;
    private double age;

    Explosion(int ownerId, int col, int row, List<WaterCell> cells, List<Point> blockedCells) {
        this.ownerId = ownerId;
        this.col = col;
        this.row = row;
        this.cells = List.copyOf(cells);
        this.blockedCells = List.copyOf(blockedCells);
    }

    /** 터진 물풍선을 놓은 플레이어 번호 */
    public int ownerId() {
        return ownerId;
    }

    /** 물풍선이 터진 칸 */
    public int col() {
        return col;
    }

    public int row() {
        return row;
    }

    /** 물줄기가 덮은 칸들. 터진 칸이 맨 앞이다 */
    public List<WaterCell> cells() {
        return cells;
    }

    /**
     * 물줄기를 멈춰 세운 막힌 칸들 (맵 안쪽만). 물줄기는 이 칸에 들어가지 않는다.
     * 블럭을 부수는 규칙은 이 칸들을 보고 정한다.
     */
    public List<Point> blockedCells() {
        return blockedCells;
    }

    /** 터진 뒤 흐른 시간(초) */
    public double age() {
        return age;
    }

    /** 지금 보여 줄 물줄기 그림 번호 (0부터 {@link #FRAMES} - 1) */
    public int frame() {
        return Math.min(FRAMES - 1, (int) Math.floor(age / FRAME_SECONDS + TIME_SLACK));
    }

    /** (col, row) 칸을 물줄기가 덮었는지 */
    public boolean covers(int col, int row) {
        for (WaterCell cell : cells) {
            if (cell.col() == col && cell.row() == row) {
                return true;
            }
        }
        return false;
    }

    void advance(double dt) {
        age += dt;
    }

    boolean isFinished() {
        return age >= SECONDS - TIME_SLACK;
    }
}
