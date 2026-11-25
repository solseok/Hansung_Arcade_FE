package hansung.arcade.ingame.map;

import java.awt.Point;
import java.util.List;

/**
 * 격자 맵. 좌표는 (col, row) 타일 좌표와 픽셀 좌표 두 가지를 쓴다.
 * 맵 바깥은 모두 막힌 칸으로 취급한다.
 *
 * <p>칸마다 바닥 층과 블럭 층이 있다. 바닥({@link #groundAt})은 맵 파일의 [floor] 층 그대로 바뀌지 않고,
 * 블럭 층에는 블럭·상자·덤불 등이 놓이거나 비어 있다. {@link #tileAt}은 블럭이 있으면 블럭을, 없으면 바닥을 준다.
 * 블럭이 부서지거나({@link #hitBlock}) 밀려 나가면({@link #moveBlock}) 그 칸의 바닥이 다시 드러난다.</p>
 */
public final class TileMap {

    public static final int TILE_SIZE = 40;

    private final String id;
    private final String name;
    private final String theme;
    /** 바닥 층. 칸마다 바닥 타일({@link Tile#isGround()})이 하나씩 있고 바뀌지 않는다 */
    private final Tile[][] ground;
    /** 블럭 층. 바닥 위에 놓인 바닥이 아닌 타일이고, 빈 칸은 null */
    private final Tile[][] blocks;
    private final List<Point> spawnPoints;

    /**
     * @param id     맵 id (resources/maps 아래 경로, 예: camp/camp08)
     * @param ground 바닥 층. 모든 칸이 바닥 타일이다
     * @param blocks 블럭 층. 바닥과 같은 크기이고, 빈 칸은 null, 나머지는 바닥이 아닌 타일이다
     */
    TileMap(String id, String name, String theme, Tile[][] ground, Tile[][] blocks, List<Point> spawnPoints) {
        this.id = id;
        this.name = name;
        this.theme = theme;
        this.ground = new Tile[ground.length][];
        this.blocks = new Tile[ground.length][];
        for (int row = 0; row < ground.length; row++) {
            this.ground[row] = ground[row].clone();
            this.blocks[row] = blocks[row].clone();
            for (int col = 0; col < ground[row].length; col++) {
                if (!this.ground[row][col].isGround()) {
                    throw new IllegalArgumentException("바닥 층 (%d, %d)에 바닥이 아닌 '%c'가 있습니다."
                            .formatted(col, row, this.ground[row][col].symbol()));
                }
                Tile block = this.blocks[row][col];
                if (block != null && block.isGround()) {
                    throw new IllegalArgumentException("블럭 층 (%d, %d)에 바닥 타일 '%c'가 있습니다."
                            .formatted(col, row, block.symbol()));
                }
            }
        }
        this.spawnPoints = List.copyOf(spawnPoints);
    }

    /** 맵 id. resources/maps 아래 경로다 (예: camp/camp08). 블럭에서 나오는 아이템처럼 맵마다 다른 규칙을 고를 때 쓴다 */
    public String id() {
        return id;
    }

    /** 화면에 쓰는 이름. 맵 파일에 name= 줄이 없으면 맵 id다 */
    public String name() {
        return name;
    }

    /** 그림 묶음을 고르는 테마 이름 (예: camp). 맵 id의 폴더 이름이며, 폴더가 없으면 빈 문자열이다. */
    public String theme() {
        return theme;
    }

    public int columns() {
        return ground[0].length;
    }

    public int rows() {
        return ground.length;
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

    /** (col, row) 칸에서 위에 보이는 타일. 블럭이 놓여 있으면 그 블럭, 없으면 바닥이다 */
    public Tile tileAt(int col, int row) {
        Tile block = blocks[row][col];
        return block != null ? block : ground[row][col];
    }

    /** (col, row) 칸 바닥 층의 타일. 위에 블럭·덤불이 없으면 {@link #tileAt}과 같다 */
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
        if (!isInside(col, row) || blocks[row][col] == null || !blocks[row][col].isDestructible()) {
            return false;
        }
        Tile left = blocks[row][col].afterHit();
        blocks[row][col] = left;
        return left == null;
    }

    /**
     * (col, row) 칸을 서버가 알려 준 모습으로 맞춘다 (온라인에서 블럭이 맞았을 때). 바닥 타일이면 블럭을 치워 바닥을 드러내고,
     * 그 밖의 타일이면 그 블럭을 놓는다. 맵 밖이면 아무 일도 없다.
     */
    public void setBlock(int col, int row, Tile tile) {
        if (!isInside(col, row)) {
            return;
        }
        blocks[row][col] = tile.isGround() ? null : tile;
    }

    /**
     * (col, row) 칸에 놓인 밀리는 블럭({@link Tile#isPushable()})을 (toCol, toRow) 칸으로 옮긴다.
     * 블럭 층에서만 옮기므로 떠난 칸에는 그 칸의 바닥이 드러나고, 블럭 밑은 도착한 칸의 바닥이 된다.
     *
     * @throws IllegalArgumentException 옮길 칸에 밀리는 블럭이 없거나, 도착할 칸이 맵 밖이거나 이미 무언가 놓여 있는 경우
     */
    public void moveBlock(int col, int row, int toCol, int toRow) {
        if (!isInside(col, row) || blocks[row][col] == null || !blocks[row][col].isPushable()) {
            throw new IllegalArgumentException("(%d, %d) 칸에 밀리는 블럭이 없습니다.".formatted(col, row));
        }
        if (!isInside(toCol, toRow) || blocks[toRow][toCol] != null) {
            throw new IllegalArgumentException("(%d, %d) 칸은 블럭을 옮겨 놓을 빈 바닥이 아닙니다.".formatted(toCol, toRow));
        }
        blocks[toRow][toCol] = blocks[row][col];
        blocks[row][col] = null;
    }

    /**
     * 이 칸이 타일의 왼쪽 첫 칸인지. 미사일처럼 여러 칸짜리 타일은 첫 칸에서만 한 번 그린다.
     * 한 칸짜리 타일은 항상 true.
     */
    public boolean isTileOrigin(int col, int row) {
        Tile tile = tileAt(col, row);
        int sameOnLeft = 0;
        for (int c = col - 1; c >= 0 && tileAt(c, row) == tile; c--) {
            sameOnLeft++;
        }
        return sameOnLeft % tile.width() == 0;
    }

    public boolean isSolid(int col, int row) {
        return !isInside(col, row) || tileAt(col, row).isSolid();
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
