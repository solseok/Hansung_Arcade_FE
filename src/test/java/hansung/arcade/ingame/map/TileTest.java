package hansung.arcade.ingame.map;

import hansung.arcade.ingame.entity.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileTest {

    @Test
    void 세_번_맞아야_하는_블럭은_한_단계씩_약해지다가_부서진다() {
        assertEquals(Tile.HARD_BLOCK_2, Tile.HARD_BLOCK_3.afterHit());
        assertEquals(Tile.HARD_BLOCK_1, Tile.HARD_BLOCK_2.afterHit());
        assertNull(Tile.HARD_BLOCK_1.afterHit());
    }

    @Test
    void 한_번에_부서지는_블럭은_맞으면_남는_것이_없다() {
        for (Tile tile : new Tile[]{Tile.BLOCK, Tile.BLOCK_2, Tile.SIDE_BLOCK, Tile.PUSH_BOX, Tile.ONE_WAY_LEFT,
                Tile.ONE_WAY_RIGHT, Tile.ONE_WAY_UP, Tile.ONE_WAY_DOWN}) {
            assertNull(tile.afterHit(), tile.name());
        }
    }

    @Test
    void 부서지지_않는_칸은_맞아도_그대로다() {
        for (Tile tile : Tile.values()) {
            if (!tile.isDestructible()) {
                assertEquals(tile, tile.afterHit(), tile.name());
            }
        }
    }

    @Test
    void 바닥_타일은_막지도_부서지지도_않는다() {
        for (Tile tile : Tile.values()) {
            if (tile.isGround()) {
                assertFalse(tile.isSolid(), tile.name());
                assertFalse(tile.isDestructible(), tile.name());
            }
        }
        assertFalse(Tile.BUSH.isGround());
        assertTrue(Tile.SPIKE.isGround());
    }

    @Test
    void 밀리는_블럭은_상자와_철로_보강된_상자다() {
        for (Tile tile : Tile.values()) {
            boolean box = tile == Tile.PUSH_BOX || tile == Tile.HARD_BLOCK_3 || tile == Tile.HARD_BLOCK_2
                    || tile == Tile.HARD_BLOCK_1;
            assertEquals(box, tile.isPushable(), tile.name());
        }
    }

    @Test
    void 가시_블럭의_기호는_가시가_난_면이다() {
        assertEquals(Direction.LEFT, Tile.ONE_WAY_LEFT.thornSide());
        assertEquals(Direction.RIGHT, Tile.ONE_WAY_RIGHT.thornSide());
        assertEquals(Direction.UP, Tile.ONE_WAY_UP.thornSide());
        assertEquals(Direction.DOWN, Tile.ONE_WAY_DOWN.thornSide());
        assertNull(Tile.BLOCK.thornSide());
        assertNull(Tile.SIDE_BLOCK.thornSide());
    }

    @Test
    void 왼쪽에_가시가_난_블럭은_오른쪽으로_밀려_온_물풍선만_터뜨린다() {
        assertTrue(Tile.ONE_WAY_LEFT.popsBalloonMoving(Direction.RIGHT));
        assertFalse(Tile.ONE_WAY_LEFT.popsBalloonMoving(Direction.LEFT));
        assertFalse(Tile.ONE_WAY_LEFT.popsBalloonMoving(Direction.UP));
        assertFalse(Tile.ONE_WAY_LEFT.popsBalloonMoving(Direction.DOWN));
    }

    @Test
    void 가시_블럭마다_가시_면으로_들어오는_방향에서만_터진다() {
        assertTrue(Tile.ONE_WAY_RIGHT.popsBalloonMoving(Direction.LEFT));
        assertTrue(Tile.ONE_WAY_UP.popsBalloonMoving(Direction.DOWN));
        assertTrue(Tile.ONE_WAY_DOWN.popsBalloonMoving(Direction.UP));
        for (Direction travel : Direction.values()) {
            assertFalse(Tile.BLOCK.popsBalloonMoving(travel), travel.name());
            assertFalse(Tile.SIDE_BLOCK.popsBalloonMoving(travel), travel.name());
        }
    }
}
