package hansung.arcade.ingame.entity;

import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

    private static final int T = TileMap.TILE_SIZE;
    private static final double FRAME = 1.0 / 60;

    private static void run(Player player, Direction input, TileMap map, double seconds) {
        for (double t = 0; t < seconds; t += FRAME) {
            player.update(input, FRAME, map);
        }
    }

    @Test
    void 빈_칸으로는_속도만큼_이동한다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);

        player.update(Direction.RIGHT, 0.1, map);

        assertEquals(TileMap.centerX(0) + Player.BASE_SPEED * 0.1, player.x(), 1e-9);
        assertEquals(TileMap.centerY(0), player.y(), 1e-9);
        assertTrue(player.isMoving());
        assertEquals(Direction.RIGHT, player.facing());
    }

    @Test
    void 블록_앞에서_멈추고_블록과_겹치지_않는다() {
        TileMap map = MapLoader.parse("wall", List.of("1.#.."));
        Player player = new Player(1, 0, 0);

        run(player, Direction.RIGHT, map, 2.0);

        double rightEdge = player.x() + Player.HALF_HITBOX;
        assertTrue(rightEdge <= 2 * T, "블록을 파고듦: " + rightEdge);
        assertTrue(rightEdge > 2 * T - 1.5, "블록 앞까지 가지 못함: " + rightEdge);
        assertFalse(player.isMoving());
    }

    @Test
    void 맵_끝을_넘어가지_않는다() {
        TileMap map = MapLoader.parse("edge", List.of("1.."));
        Player player = new Player(1, 0, 0);

        run(player, Direction.LEFT, map, 1.0);
        run(player, Direction.UP, map, 1.0);

        assertTrue(player.x() - Player.HALF_HITBOX >= 0);
        assertTrue(player.y() - Player.HALF_HITBOX >= 0);
    }

    @Test
    void 통로_입구에서_어긋나_있으면_모서리를_타고_미끄러져_들어간다() {
        // 0행에서 아래로 내려가려 하는데 플레이어가 1열 쪽으로 반 칸 가까이 치우쳐 있다.
        // (0,1)은 통로, (1,1)은 바위
        TileMap map = MapLoader.parse("corner", List.of(
                "1..",
                ".#.",
                "..."));
        Player player = new Player(1, 0, 0);
        run(player, Direction.RIGHT, map, 0.12); // 8프레임, 16px 오른쪽으로 치우침
        double offsetBefore = player.x() - TileMap.centerX(0);
        assertTrue(offsetBefore > 10, "테스트 준비 실패: " + offsetBefore);

        run(player, Direction.DOWN, map, 1.0);

        // 충돌 박스가 타일보다 작아서, 그 여유만큼 어긋나 있으면 더 정렬하지 않고 바로 통과한다.
        double tolerance = T / 2.0 - Player.HALF_HITBOX;
        assertEquals(TileMap.centerX(0), player.x(), tolerance);
        assertTrue(player.row() >= 1, "통로로 내려가지 못함: row=" + player.row());
    }

    @Test
    void 앞칸이_막혀_있으면_모서리_보정을_하지_않는다() {
        TileMap map = MapLoader.parse("blocked", List.of(
                "1..",
                "##."));
        Player player = new Player(1, 0, 0);
        run(player, Direction.RIGHT, map, 0.12);
        double xBefore = player.x();

        run(player, Direction.DOWN, map, 1.0);

        assertEquals(xBefore, player.x(), 1e-9);
        assertEquals(0, player.row());
    }
}
