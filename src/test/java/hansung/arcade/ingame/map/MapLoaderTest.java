package hansung.arcade.ingame.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLoaderTest {

    @Test
    void 기본_맵을_리소스에서_읽는다() {
        TileMap map = MapLoader.loadResource("camp/camp08");

        assertEquals("캠프 08", map.name());
        assertEquals("camp", map.theme());
        assertEquals(15, map.columns());
        assertEquals(13, map.rows());
        assertEquals(List.of(new Point(0, 6), new Point(14, 6)), map.spawnPoints());
    }

    @Test
    void 기본_맵의_스폰_지점은_바닥이고_상하좌우_중_한_곳_이상_비어있다() {
        TileMap map = MapLoader.loadResource("camp/camp08");

        for (Point spawn : map.spawnPoints()) {
            assertFalse(map.isSolid(spawn.x, spawn.y), "스폰 지점이 막혀 있음: " + spawn);
            boolean hasExit = !map.isSolid(spawn.x - 1, spawn.y) || !map.isSolid(spawn.x + 1, spawn.y)
                    || !map.isSolid(spawn.x, spawn.y - 1) || !map.isSolid(spawn.x, spawn.y + 1);
            assertTrue(hasExit, "스폰 지점에서 나갈 길이 없음: " + spawn);
        }
    }

    @Test
    void 글자를_타일로_바꾸고_스폰_번호순으로_정렬한다() {
        TileMap map = MapLoader.parse("test", List.of(
                "name=테스트",
                "2.#",
                "TB1"));

        assertEquals("테스트", map.name());
        assertEquals(Tile.FLOOR, map.tileAt(0, 0));
        assertEquals(Tile.STATUE, map.tileAt(2, 0));
        assertEquals(Tile.BUSH, map.tileAt(0, 1));
        assertEquals(Tile.BLOCK, map.tileAt(1, 1));
        assertEquals(Tile.FLOOR, map.tileAt(2, 1));
        assertEquals(List.of(new Point(2, 1), new Point(0, 0)), map.spawnPoints());
    }

    @Test
    void 배치_규칙의_모든_기호를_읽는다() {
        TileMap map = MapLoader.parse("test", List.of("1B-TPcba><^v#W!MMHY_|=OJK"));

        List<Tile> expected = List.of(Tile.FLOOR, Tile.BLOCK, Tile.SIDE_BLOCK, Tile.BUSH, Tile.PUSH_BOX,
                Tile.HARD_BLOCK_3, Tile.HARD_BLOCK_2, Tile.HARD_BLOCK_1,
                Tile.ONE_WAY_RIGHT, Tile.ONE_WAY_LEFT, Tile.ONE_WAY_UP, Tile.ONE_WAY_DOWN,
                Tile.STATUE, Tile.WIRE, Tile.SPIKE, Tile.MISSILE, Tile.MISSILE, Tile.HOUSE_1, Tile.TREE,
                Tile.ROAD, Tile.ROAD_LANE, Tile.CROSSWALK, Tile.BLOCK_2, Tile.HOUSE_2, Tile.HOUSE_3);
        for (int col = 0; col < expected.size(); col++) {
            assertEquals(expected.get(col), map.tileAt(col, 0), "열 " + col);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"camp/camp08", "patrit/patrit14", "village/village10"})
    void 리소스의_모든_맵을_읽을_수_있다(String id) {
        TileMap map = MapLoader.loadResource(id);

        assertFalse(map.spawnPoints().isEmpty());
        assertEquals(id.substring(0, id.indexOf('/')), map.theme());
    }

    @Test
    void 덤불과_바닥_가시와_길은_지나갈_수_있다() {
        TileMap map = MapLoader.parse("test", List.of("1T!_|=B"));

        for (int col = 1; col <= 5; col++) {
            assertFalse(map.isSolid(col, 0), "열 " + col);
        }
        assertTrue(map.isSolid(6, 0));
    }

    @Test
    void 맵_id의_폴더가_테마가_된다() {
        assertEquals("camp", MapLoader.parse("camp/test", List.of("1")).theme());
        assertEquals("", MapLoader.parse("test", List.of("1")).theme());
    }

    @Test
    void 미사일은_두_칸씩_짝을_지어_첫_칸에서만_그린다() {
        TileMap map = MapLoader.parse("test", List.of("1MMMM"));

        assertTrue(map.isTileOrigin(1, 0));
        assertFalse(map.isTileOrigin(2, 0));
        assertTrue(map.isTileOrigin(3, 0));
        assertFalse(map.isTileOrigin(4, 0));
        assertTrue(map.isTileOrigin(0, 0));
    }

    @Test
    void 미사일이_한_칸만_있으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.M.")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1MMM")));
    }

    @Test
    void 맵_바깥은_막힌_칸이다() {
        TileMap map = MapLoader.parse("test", List.of("1.", ".."));

        assertTrue(map.isSolid(-1, 0));
        assertTrue(map.isSolid(0, -1));
        assertTrue(map.isSolid(2, 0));
        assertTrue(map.isSolid(0, 2));
    }

    @Test
    void 줄_길이가_다르면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1..", "..")));
    }

    @Test
    void 모르는_글자가_있으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1?")));
    }

    @Test
    void 스폰_지점이_없으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("..")));
    }

    @Test
    void 스폰_번호가_1부터_빠짐없이_이어지지_않으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.3")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of(".2.")));
    }

    @Test
    void 파일_맨_앞의_BOM은_무시한다() {
        TileMap map = MapLoader.parse("test", List.of("\uFEFFname=테스트", "1.#"));

        assertEquals("테스트", map.name());
        assertEquals(3, map.columns());
        assertEquals(1, MapLoader.parse("test", List.of("\uFEFF1")).columns());
    }

    @Test
    void 같은_스폰_번호가_두_번_나오면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.1")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.2", "..2")));
    }
}
