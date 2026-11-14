package hansung.arcade.ingame.block;

import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

/**
 * 밀려서 한 칸 미끄러지는 중인 블럭. 맵 칸은 미는 순간 이미 도착 칸으로 옮겨져 있고,
 * 이것은 그림을 떠난 칸에서 도착 칸까지 {@link BoxPusher#SLIDE_SECONDS}초 동안 옮겨 그리기 위한 정보다.
 */
public final class SlidingBlock {

    private static final double TIME_SLACK = 1e-6;

    private final Tile tile;
    private final int fromCol;
    private final int fromRow;
    private final int toCol;
    private final int toRow;
    private double age;

    SlidingBlock(Tile tile, int fromCol, int fromRow, int toCol, int toRow) {
        this.tile = tile;
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
    }

    /** 미끄러지는 블럭 종류 */
    public Tile tile() {
        return tile;
    }

    /** 도착 칸 (맵에서 지금 블럭이 있는 칸) */
    public int toCol() {
        return toCol;
    }

    public int toRow() {
        return toRow;
    }

    /** 0(떠난 칸)부터 1(도착 칸)까지 미끄러진 정도 */
    public double progress() {
        return Math.min(1, age / BoxPusher.SLIDE_SECONDS);
    }

    /** 그림을 도착 칸 자리에서 가로로 옮겨 그릴 거리(px). 떠난 칸 쪽이라 미는 방향의 반대 부호다 */
    public int offsetX() {
        return (int) Math.round((fromCol - toCol) * TileMap.TILE_SIZE * (1 - progress()));
    }

    /** 그림을 도착 칸 자리에서 세로로 옮겨 그릴 거리(px) */
    public int offsetY() {
        return (int) Math.round((fromRow - toRow) * TileMap.TILE_SIZE * (1 - progress()));
    }

    void advance(double dt) {
        age += dt;
    }

    boolean isFinished() {
        return age >= BoxPusher.SLIDE_SECONDS - TIME_SLACK;
    }
}
