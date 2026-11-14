package hansung.arcade.ingame.map;

import java.awt.Point;
import java.util.List;

/**
 * 격자 맵. 좌표는 (col, row) 타일 좌표와 픽셀 좌표 두 가지를 쓴다.
 * 맵 바깥은 모두 막힌 칸으로 취급한다.
 *
 * <p>칸마다 바닥({@link #groundAt})과 그 위에 놓인 타일({@link #tileAt}) 두 층이 있다. 아무것도 놓이지 않은 칸은
 * 두 층이 같다. 블럭이 부서지거나({@link #hitBlock}) 밀려 나가면({@link #moveBlock}) 그 칸의 바닥이 다시 드러난다.
 * 맵 파일에는 블럭 밑의 바닥을 적을 수 없어 처음에는 일반 바닥({@link Tile#FLOOR})으로 둔다.</p>
 */
public final class TileMap {

    public static final int TILE_SIZE = 40;

    private final String name;
    private final String theme;
    private final Tile[][] tiles;
    /** 칸마다 블럭·덤불 밑에 깔린 바닥 타일 */
    private final Tile[][] ground;
    private final List<Point> spawnPoints;

    TileMap(String name, String theme, Tile[][] tiles, List<Point> spawnPoints) {
        this.name = name;
        this.theme = theme;
        this.tiles = new Tile[tiles.length][];
        this.ground = new Tile[tiles.length][];
        for (int row = 0; row < tiles.length; row++) {
            this.tiles[row] = tiles[row].clone();
            this.ground[row] = new Tile[tiles[row].length];
            for (int col = 0; col < tiles[row].length; col++) {
                Tile tile = tiles[row][col];
                this.ground[row][col] = tile.isGround() ? tile : Tile.FLOOR;
            }
        }
        this.spawnPoints = List.copyOf(spawnPoints);
    }

    public String name() {
        return name;
    }

    /** 그림 묶음을 고르는 테마 이름 (예: camp). 맵 id의 폴더 이름이며, 폴더가 없으면 빈 문자열이다. */
    public String theme() {
        return theme;
    }

    public int columns() {
        return tiles[0].length;
    }

    public int rows() {
        return tiles.length;
    }

    public int pixelWidth() {
        return columns() * TILE_SIZE;
    }

    public int pixelHeight() {
        return rows() * TILE_SIZE;
    }

    public boolean isInside(int col, int row) {
        return col >= 0 && col < columns() && row >= 0 && row < rows();
    }

    public Tile tileAt(int col, int row) {
        return tiles[row][col];
    }

    /** (col, row) 칸 맨 밑의 바닥 타일. 위에 블럭·덤불이 없으면 {@link #tileAt}과 같다 */
    public Tile groundAt(int col, int row) {
        return ground[row][col];
    }

    /**
     * (col, row) 칸의 블럭을 물줄기로 한 번 맞힌다. 부서지는 블럭은 {@link Tile#afterHit()}로 바뀌고,
     * 다 부서지면 바닥이 드러난다. 부서지지 않는 칸이나 맵 밖은 아무 일도 없다.
     *
     * @return 이번에 블럭이 다 부서져 칸이 비었는지
     */
    public boolean hitBlock(int col, int row) {
        if (!isInside(col, row) || !tiles[row][col].isDestructible()) {
            return false;
        }
        Tile left = tiles[row][col].afterHit();
        tiles[row][col] = left != null ? left : ground[row][col];
        return left == null;
    }

    /**
     * (col, row) 칸에 놓인 밀리는 블럭({@link Tile#isPushable()})을 (toCol, toRow) 칸으로 옮긴다.
     * 떠난 칸에는 바닥이 드러나고, 도착한 칸의 바닥은 블럭 밑에 그대로 남는다.
     *
     * @throws IllegalArgumentException 옮길 칸에 밀리는 블럭이 없거나, 도착할 칸이 맵 밖이거나 바닥이 아닌 경우
     */
    public void moveBlock(int col, int row, int toCol, int toRow) {
        if (!isInside(col, row) || !tiles[row][col].isPushable()) {
            throw new IllegalArgumentException("(%d, %d) 칸에 밀리는 블럭이 없습니다.".formatted(col, row));
        }
        if (!isInside(toCol, toRow) || !tiles[toRow][toCol].isGround()) {
            throw new IllegalArgumentException("(%d, %d) 칸은 블럭을 옮겨 놓을 바닥이 아닙니다.".formatted(toCol, toRow));
        }
        tiles[toRow][toCol] = tiles[row][col];
        tiles[row][col] = ground[row][col];
    }

    /**
     * 이 칸이 타일의 왼쪽 첫 칸인지. 미사일처럼 여러 칸짜리 타일은 첫 칸에서만 한 번 그린다.
     * 한 칸짜리 타일은 항상 true.
     */
    public boolean isTileOrigin(int col, int row) {
        Tile tile = tiles[row][col];
        int sameOnLeft = 0;
        for (int c = col - 1; c >= 0 && tiles[row][c] == tile; c--) {
            sameOnLeft++;
        }
        return sameOnLeft % tile.width() == 0;
    }

    public boolean isSolid(int col, int row) {
        return !isInside(col, row) || tiles[row][col].isSolid();
    }

    /** 스폰 지점(타일 좌표). 맵 파일의 1, 2, 3 ... 순서로 정렬되어 있다. */
    public List<Point> spawnPoints() {
        return spawnPoints;
    }

    public static int colAt(double pixelX) {
        return (int) Math.floor(pixelX / TILE_SIZE);
    }

    public static int rowAt(double pixelY) {
        return (int) Math.floor(pixelY / TILE_SIZE);
    }

    public static double centerX(int col) {
        return col * TILE_SIZE + TILE_SIZE / 2.0;
    }

    public static double centerY(int row) {
        return row * TILE_SIZE + TILE_SIZE / 2.0;
    }
}
