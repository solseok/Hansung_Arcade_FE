package hansung.arcade.ingame.map;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileMapTest {

    private static TileMap map(String... rows) {
        return MapLoader.parse("fixture/tilemap", List.of(rows));
    }

    @Test
    void 일반_블럭은_한_번_맞으면_부서져_바닥이_된다() {
        TileMap map = map("1B.");

        assertTrue(map.hitBlock(1, 0));

        assertEquals(Tile.FLOOR, map.tileAt(1, 0));
        assertFalse(map.isSolid(1, 0));
    }

    @Test
    void 세_번_맞아야_하는_블럭은_세_번째에_부서진다() {
        TileMap map = map("1c.");

        assertFalse(map.hitBlock(1, 0));
        assertEquals(Tile.HARD_BLOCK_2, map.tileAt(1, 0));
        assertFalse(map.hitBlock(1, 0));
        assertEquals(Tile.HARD_BLOCK_1, map.tileAt(1, 0));
        assertTrue(map.hitBlock(1, 0));
        assertEquals(Tile.FLOOR, map.tileAt(1, 0));
    }

    @Test
    void 부서지지_않는_칸과_덤불과_맵_밖은_맞아도_그대로다() {
        TileMap map = map("1#T");

        assertFalse(map.hitBlock(1, 0));
        assertFalse(map.hitBlock(2, 0));
        assertFalse(map.hitBlock(3, 0));
        assertFalse(map.hitBlock(-1, 0));

        assertEquals(Tile.STATUE, map.tileAt(1, 0));
        assertEquals(Tile.BUSH, map.tileAt(2, 0));
    }

    @Test
    void 블럭이_없는_칸의_바닥은_그_칸_타일이고_블럭과_덤불_밑은_일반_바닥이다() {
        TileMap map = map("1_|=!BT");

        assertEquals(Tile.ROAD, map.groundAt(1, 0));
        assertEquals(Tile.ROAD_LANE, map.groundAt(2, 0));
        assertEquals(Tile.CROSSWALK, map.groundAt(3, 0));
        assertEquals(Tile.SPIKE, map.groundAt(4, 0));
        assertEquals(Tile.FLOOR, map.groundAt(5, 0));
        assertEquals(Tile.FLOOR, map.groundAt(6, 0));
    }

    @Test
    void 상자를_길_위로_밀었다가_다시_빼면_길이_그대로_드러난다() {
        TileMap map = map("1P_.");

        map.moveBlock(1, 0, 2, 0);
        assertEquals(Tile.FLOOR, map.tileAt(1, 0));
        assertEquals(Tile.PUSH_BOX, map.tileAt(2, 0));
        assertEquals(Tile.ROAD, map.groundAt(2, 0));
        assertTrue(map.isSolid(2, 0));

        map.moveBlock(2, 0, 3, 0);
        assertEquals(Tile.ROAD, map.tileAt(2, 0));
        assertEquals(Tile.PUSH_BOX, map.tileAt(3, 0));
    }

    @Test
    void 길_위의_상자가_부서지면_길이_드러난다() {
        TileMap map = map("1P_");
        map.moveBlock(1, 0, 2, 0);

        assertTrue(map.hitBlock(2, 0));

        assertEquals(Tile.ROAD, map.tileAt(2, 0));
    }

    @Test
    void 밀리는_블럭이_아니거나_바닥이_아닌_칸으로는_옮길_수_없다() {
        TileMap map = map("1BP#T.");

        assertThrows(IllegalArgumentException.class, () -> map.moveBlock(1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> map.moveBlock(2, 0, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> map.moveBlock(2, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> map.moveBlock(2, 0, 2, -1));
        assertEquals(Tile.PUSH_BOX, map.tileAt(2, 0));
    }
}
