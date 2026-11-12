package hansung.arcade.ingame.map;

import java.awt.Point;
import java.util.List;

/**
 * 격자 맵. 좌표는 (col, row) 타일 좌표와 픽셀 좌표 두 가지를 쓴다.
 * 맵 바깥은 모두 막힌 칸으로 취급한다.
 */
public final class TileMap {

    public static final int TILE_SIZE = 40;

    private final String name;
    private final String theme;
    private final Tile[][] tiles;
    private final List<Point> spawnPoints;

    TileMap(String name, String theme, Tile[][] tiles, List<Point> spawnPoints) {
        this.name = name;
        this.theme = theme;
        this.tiles = tiles;
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
