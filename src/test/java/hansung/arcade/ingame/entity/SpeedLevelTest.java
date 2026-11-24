package hansung.arcade.ingame.entity;

import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpeedLevelTest {

    @Test
    void 단계5는_초당_4점5타일이고_1단계는_2점5_9단계는_6점5타일이다() {
        assertEquals(4.5, SpeedLevel.tilesPerSecond(5), 1e-9);
        assertEquals(2.5, SpeedLevel.tilesPerSecond(1), 1e-9);
        assertEquals(6.5, SpeedLevel.tilesPerSecond(9), 1e-9);
    }

    @Test
    void 한_단계마다_초당_0점5타일씩_빨라진다() {
        for (int level = SpeedLevel.MIN; level < SpeedLevel.MAX; level++) {
            assertEquals(0.5, SpeedLevel.tilesPerSecond(level + 1) - SpeedLevel.tilesPerSecond(level), 1e-9,
                    "단계 " + level);
        }
    }

    @Test
    void 픽셀_속도는_타일_속도에_타일_크기를_곱한_값이다() {
        assertEquals(4.5 * TileMap.TILE_SIZE, SpeedLevel.pixelsPerSecond(5), 1e-9);
    }

    @Test
    void 단계는_1부터_9까지만_있다() {
        assertEquals(1, SpeedLevel.MIN);
        assertEquals(9, SpeedLevel.MAX);
        assertThrows(IllegalArgumentException.class, () -> SpeedLevel.tilesPerSecond(0));
        assertThrows(IllegalArgumentException.class, () -> SpeedLevel.tilesPerSecond(10));
    }
}
