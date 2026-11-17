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

        // 기본 캐릭터 상상부기는 이동속도 5단계 = 초당 4.5타일
        assertEquals(TileMap.centerX(0) + 4.5 * T * 0.1, player.x(), 1e-9);
        assertEquals(TileMap.centerY(0), player.y(), 1e-9);
        assertTrue(player.isMoving());
        assertEquals(Direction.RIGHT, player.facing());
    }

    @Test
    void 이동속도_단계가_오르면_그만큼_더_멀리_간다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);
        player.addSpeedLevel(1);

        player.update(Direction.RIGHT, 0.1, map);

        assertEquals(6, player.speedLevel());
        assertEquals(TileMap.centerX(0) + 5.0 * T * 0.1, player.x(), 1e-9);
    }

    @Test
    void 능력치는_고른_캐릭터의_시작값으로_시작한다() {
        Player bugi = playerOf(Mascot.SANGSANGBUGI);
        assertEquals(1, bugi.balloons());
        assertEquals(1, bugi.waterRange());
        assertEquals(5, bugi.speedLevel());
        assertEquals(4.5 * T, bugi.speed(), 1e-9);

        Player nyangi = playerOf(Mascot.HANSUNGNYANGI);
        assertEquals(2, nyangi.balloons());
        assertEquals(1, nyangi.waterRange());
        assertEquals(4, nyangi.speedLevel());
        assertEquals(4.0 * T, nyangi.speed(), 1e-9);

        assertEquals(2, playerOf(Mascot.SANGZZI).waterRange());
    }

    @Test
    void 능력치는_캐릭터의_최댓값을_넘지_않는다() {
        Player bugi = playerOf(Mascot.SANGSANGBUGI);
        for (int i = 0; i < 20; i++) {
            bugi.addBalloons(1);
            bugi.addWaterRange(1);
            bugi.addSpeedLevel(1);
        }
        assertEquals(6, bugi.balloons());
        assertEquals(7, bugi.waterRange());
        assertEquals(9, bugi.speedLevel());
        assertEquals(6.5 * T, bugi.speed(), 1e-9);

        Player kkukku = playerOf(Mascot.KKUKKU);
        kkukku.addBalloons(Integer.MAX_VALUE);
        kkukku.addWaterRange(Integer.MAX_VALUE);
        kkukku.addSpeedLevel(Integer.MAX_VALUE);
        assertEquals(10, kkukku.balloons());
        assertEquals(8, kkukku.waterRange());
        assertEquals(8, kkukku.speedLevel());
    }

    @Test
    void 이동속도는_1단계까지_내려가고_물풍선과_물줄기는_시작값_아래로_내려가지_않는다() {
        Player nyangi = playerOf(Mascot.HANSUNGNYANGI);
        nyangi.addSpeedLevel(-100);
        nyangi.addBalloons(-100);
        nyangi.addWaterRange(-100);

        assertEquals(1, nyangi.speedLevel());
        assertEquals(2.5 * T, nyangi.speed(), 1e-9);
        assertEquals(2, nyangi.balloons());
        assertEquals(1, nyangi.waterRange());
    }

    private static Player playerOf(Mascot mascot) {
        return new Player(1, new PlayerProfile("tester", mascot), 0, 0);
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
    void 맵_오른쪽_아래쪽_끝에도_딱_붙어_멈춘다() {
        TileMap map = MapLoader.parse("edge", List.of("1.."));
        Player player = new Player(1, 0, 0);

        run(player, Direction.RIGHT, map, 2.0);
        run(player, Direction.DOWN, map, 1.0);

        double rightEdge = player.x() + Player.HALF_HITBOX;
        double bottomEdge = player.y() + Player.HALF_HITBOX;
        assertTrue(rightEdge <= map.pixelWidth() && rightEdge > map.pixelWidth() - 1.5, "오른쪽 끝: " + rightEdge);
        assertTrue(bottomEdge <= map.pixelHeight() && bottomEdge > map.pixelHeight() - 1.5, "아래쪽 끝: " + bottomEdge);
    }

    @Test
    void 가로로_갈_때도_위로_어긋나_있으면_아래로_미끄러져_들어간다() {
        // 1행에서 오른쪽으로 가려 하는데 플레이어가 0행 쪽으로 치우쳐 있다. (1,1)은 통로, (1,0)은 바위
        TileMap map = MapLoader.parse("corner", List.of(
                ".#.",
                "1..",
                "..."));
        Player player = new Player(1, 0, 1);
        run(player, Direction.UP, map, 0.08); // 5프레임, 15px 위로 치우침
        double offsetBefore = player.y() - TileMap.centerY(1);
        assertTrue(offsetBefore < -10, "테스트 준비 실패: " + offsetBefore);

        run(player, Direction.RIGHT, map, 1.0);

        double tolerance = T / 2.0 - Player.HALF_HITBOX;
        assertEquals(TileMap.centerY(1), player.y(), tolerance);
        assertTrue(player.col() >= 1, "통로로 들어가지 못함: col=" + player.col());
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
        run(player, Direction.RIGHT, map, 0.08); // 5프레임, 15px 오른쪽으로 치우침
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
        run(player, Direction.RIGHT, map, 0.08); // 5프레임, 15px 오른쪽으로 치우침
        double xBefore = player.x();
        assertTrue(player.col() == 0 && xBefore - TileMap.centerX(0) > 10, "테스트 준비 실패: x=" + xBefore);

        run(player, Direction.DOWN, map, 1.0);

        assertEquals(xBefore, player.x(), 1e-9);
        assertEquals(0, player.row());
    }

    @Test
    void 처음에는_살아_있고_탈락시키면_다시_살아나지_않는다() {
        Player player = new Player(1, 0, 0);
        assertTrue(player.isAlive());

        player.eliminate();
        assertFalse(player.isAlive());
        player.eliminate();
        assertFalse(player.isAlive());
    }

    private static void ticks(Player player, Direction input, TileMap map, int count) {
        for (int i = 0; i < count; i++) {
            player.update(input, FRAME, map);
        }
    }

    @Test
    void 물줄기에_맞으면_갇히고_3초에_한_칸_속도로만_움직인다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);

        assertTrue(player.trap(2));
        assertEquals(PlayerState.TRAPPED, player.state());
        assertEquals(2, player.trapperId().orElseThrow());
        assertTrue(player.isAlive());

        ticks(player, Direction.RIGHT, map, 180);

        assertEquals(TileMap.centerX(0) + T, player.x(), 1e-6);
        assertTrue(player.isMoving());
    }

    @Test
    void 갇혀_있으면_물풍선을_놓지_못하고_다시_갇히지도_않는다() {
        Player player = new Player(1, 0, 0);
        assertTrue(player.canPlaceBalloon());

        player.trap(1);

        assertFalse(player.canPlaceBalloon());
        assertFalse(player.trap(2));
        assertEquals(1, player.trapperId().orElseThrow());
    }

    @Test
    void 갇힌_지_5초가_되는_틱에_시간이_다_된다() {
        TileMap map = MapLoader.parse("open", List.of("1...."));
        Player player = new Player(1, 0, 0);
        player.trap(1);

        ticks(player, null, map, 299);
        assertFalse(player.isTrapTimeUp());
        ticks(player, null, map, 1);
        assertTrue(player.isTrapTimeUp());
        // 시간이 다 돼도 탈락은 게임 상태가 정한다 (kill을 누구에게 줄지 같이 정해야 해서)
        assertTrue(player.isAlive());
    }

    @Test
    void 빠져나오는_동안은_움직이지_못하고_0_5초_뒤_아래를_보고_보통_상태로_돌아온다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);
        assertFalse(player.escape(), "갇히지 않았으면 빠져나올 것도 없다");
        player.trap(1);
        ticks(player, Direction.RIGHT, map, 30);
        double x = player.x();

        assertTrue(player.escape());
        ticks(player, Direction.RIGHT, map, 29);
        assertEquals(PlayerState.ESCAPING, player.state());
        assertEquals(x, player.x(), 1e-9);
        assertFalse(player.isMoving());
        assertFalse(player.canPlaceBalloon());
        assertFalse(player.trap(2), "빠져나오는 중에는 다시 갇히지 않는다");

        // 30번째 틱에 풀려나고, 다음 틱부터 다시 걷는다
        ticks(player, null, map, 1);
        assertEquals(PlayerState.NORMAL, player.state());
        assertEquals(Direction.DOWN, player.facing());
        assertTrue(player.canPlaceBalloon());
        ticks(player, Direction.RIGHT, map, 1);
        assertTrue(player.x() > x);
    }

    @Test
    void 탈락하면_그_순간_살아_있지_않고_2초_연출이_끝나면_사라진다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);
        player.trap(2);

        player.eliminate();
        assertFalse(player.isAlive());
        assertEquals(PlayerState.DYING, player.state());
        assertEquals(0, player.stateTime());

        ticks(player, Direction.RIGHT, map, 119);
        assertEquals(PlayerState.DYING, player.state());
        assertEquals(TileMap.centerX(0), player.x(), 1e-9, "탈락하면 움직이지 않는다");
        ticks(player, null, map, 1);
        assertEquals(PlayerState.GONE, player.state());
        assertFalse(player.trap(2));
        assertFalse(player.escape());
    }

    @Test
    void 장애물로_막은_칸에는_들어가지_못한다() {
        TileMap map = MapLoader.parse("open", List.of("1...."));
        Player player = new Player(1, 0, 0);

        for (int i = 0; i < 60; i++) {
            player.update(Direction.RIGHT, FRAME, (col, row) -> map.isSolid(col, row) || col == 2);
        }

        assertEquals(2 * T - Player.HALF_HITBOX, player.x(), 1.5);
    }

    @Test
    void 충돌_박스가_걸친_칸과_닿은_캐릭터를_가린다() {
        TileMap map = MapLoader.parse("open", List.of("1....", "....."));
        Player player = new Player(1, 0, 0);
        assertTrue(player.overlapsTile(0, 0));
        assertFalse(player.overlapsTile(1, 0));

        ticks(player, Direction.RIGHT, map, 3);
        assertTrue(player.overlapsTile(0, 0));
        assertTrue(player.overlapsTile(1, 0));
        assertFalse(player.overlapsTile(0, 1));

        Player near = new Player(2, 0, 0);
        Player next = new Player(3, 1, 0);
        assertTrue(player.touches(near));
        assertFalse(near.touches(next), "옆 칸 가운데끼리는 닿지 않는다");
    }
}
