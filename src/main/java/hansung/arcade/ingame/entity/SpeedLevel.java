package hansung.arcade.ingame.entity;

import hansung.arcade.ingame.map.TileMap;

/**
 * 이동속도 단계(1~9)를 실제 속도로 바꾼다.
 * 5단계가 초당 4.5타일이고 한 단계마다 0.5타일씩 달라진다 (1단계 2.5타일, 9단계 6.5타일).
 */
public final class SpeedLevel {

    public static final int MIN = 1;
    public static final int MAX = 9;

    private static final int REFERENCE_LEVEL = 5;
    private static final double REFERENCE_TILES_PER_SECOND = 4.5;
    private static final double TILES_PER_SECOND_PER_LEVEL = 0.5;

    private SpeedLevel() {
    }

    public static double tilesPerSecond(int level) {
        if (level < MIN || level > MAX) {
            throw new IllegalArgumentException("이동속도 단계는 %d~%d 이어야 합니다: %d".formatted(MIN, MAX, level));
        }
        return REFERENCE_TILES_PER_SECOND + (level - REFERENCE_LEVEL) * TILES_PER_SECOND_PER_LEVEL;
    }

    public static double pixelsPerSecond(int level) {
        return tilesPerSecond(level) * TileMap.TILE_SIZE;
    }
}
