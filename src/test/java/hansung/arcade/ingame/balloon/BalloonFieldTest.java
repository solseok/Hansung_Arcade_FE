package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.balloon.WaterCell.Part;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalloonFieldTest {

    private static final double TICK = 1.0 / 60;

    private static Player playerAt(int id, Mascot mascot, int col, int row) {
        return new Player(id, new PlayerProfile("P" + id, mascot), col, row);
    }

    private static void ticks(BalloonField field, List<Player> players, int count) {
        for (int i = 0; i < count; i++) {
            field.update(TICK, players);
        }
    }

    /** 이 폭발의 칸들을 "col,row:모양" 글자로 */
    private static Set<String> cellsOf(Explosion explosion) {
        return explosion.cells().stream().map(c -> c.col() + "," + c.row() + ":" + c.part()).collect(Collectors.toSet());
    }

    @Test
    void 선_칸_가운데에_놓이고_2초가_되는_틱에_터진다() {
        TileMap map = MapLoader.parse("fixture/open", List.of(".....", "..1..", "....."));
        BalloonField field = new BalloonField(map);
        Player me = playerAt(1, Mascot.SANGSANGBUGI, 2, 1);
        List<Player> players = List.of(me);

        WaterBalloon balloon = field.place(me, players).orElseThrow();
        assertEquals(2, balloon.col());
        assertEquals(1, balloon.row());
        assertEquals(1, balloon.range());

        ticks(field, players, 119);
        assertEquals(1, field.balloons().size());
        assertTrue(field.explosions().isEmpty());

        ticks(field, players, 1);
        assertTrue(field.balloons().isEmpty());
        Explosion explosion = field.explosions().get(0);
        assertEquals(1, explosion.ownerId());
        assertEquals(Set.of("2,1:CENTER", "1,1:END", "3,1:END", "2,0:END", "2,2:END"), cellsOf(explosion));
    }

    @Test
    void 같은_칸에는_하나만_놓고_한_사람은_물풍선_개수만큼만_놓아_둔다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1...."));
        BalloonField field = new BalloonField(map);
        // 한성냥이는 처음부터 물풍선 2개
        Player me = playerAt(1, Mascot.HANSUNGNYANGI, 0, 0);
        List<Player> players = List.of(me);

        assertTrue(field.place(me, players).isPresent());
        assertTrue(field.place(me, players).isEmpty(), "같은 칸에 두 번");
        for (int i = 0; i < 12; i++) {
            me.update(Direction.RIGHT, TICK, (col, row) -> map.isSolid(col, row));
        }
        assertEquals(1, me.col());
        assertTrue(field.place(me, players).isPresent());
        for (int i = 0; i < 12; i++) {
            me.update(Direction.RIGHT, TICK, (col, row) -> map.isSolid(col, row));
        }
        assertTrue(field.place(me, players).isEmpty(), "개수가 꽉 찼다");
        assertEquals(2, field.placedBy(me));

        // 터지면 다시 놓을 수 있다
        ticks(field, players, 120);
        assertEquals(0, field.placedBy(me));
        assertTrue(field.place(me, players).isPresent());
    }

    @Test
    void 갇혔거나_탈락한_플레이어는_놓지_못한다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1.2.."));
        BalloonField field = new BalloonField(map);
        Player trapped = playerAt(1, Mascot.SANGSANGBUGI, 0, 0);
        Player out = playerAt(2, Mascot.SANGSANGBUGI, 2, 0);
        trapped.trap(2);
        out.eliminate();

        assertTrue(field.place(trapped, List.of(trapped, out)).isEmpty());
        assertTrue(field.place(out, List.of(trapped, out)).isEmpty());
    }

    @Test
    void 물줄기는_막힌_칸_앞과_맵_끝에서_멈추고_멈춘_블럭_칸을_알려준다() {
        // 상지는 물줄기 2칸. 오른쪽은 1칸 뒤 블럭, 위쪽은 맵 끝, 왼쪽은 2칸 다 뻗는다
        TileMap map = MapLoader.parse("fixture/walls", List.of(
                "..1.B",
                "....."));
        BalloonField field = new BalloonField(map);
        Player me = playerAt(1, Mascot.SANGZZI, 2, 0);
        field.place(me, List.of(me));
        ticks(field, List.of(me), 120);

        Explosion explosion = field.explosions().get(0);
        assertEquals(Set.of("2,0:CENTER", "3,0:END", "1,0:MIDDLE", "0,0:END", "2,1:END"), cellsOf(explosion));
        assertEquals(List.of(new Point(4, 0)), explosion.blockedCells());
    }

    @Test
    void 물줄기에_닿은_물풍선은_같은_틱에_이어서_터진다() {
        TileMap map = MapLoader.parse("fixture/chain", List.of("1.2....."));
        BalloonField field = new BalloonField(map);
        Player first = playerAt(1, Mascot.SANGZZI, 0, 0);
        Player second = playerAt(2, Mascot.SANGZZI, 2, 0);
        List<Player> players = List.of(first, second);
        field.place(first, players);
        ticks(field, players, 60);
        field.place(second, players);

        ticks(field, players, 60);

        // 먼저 놓은 물풍선이 2초에 터지면서 1초밖에 안 된 물풍선도 함께 터진다
        assertTrue(field.balloons().isEmpty());
        List<Explosion> explosions = field.explosions();
        assertEquals(2, explosions.size());
        assertEquals(Set.of("0,0:CENTER", "1,0:MIDDLE"), cellsOf(explosions.get(0)),
                "이어 터지는 물풍선 쪽 물줄기는 끝 모양 없이 그 칸에 잇닿는다");
        assertEquals(Set.of("2,0:CENTER", "1,0:MIDDLE", "0,0:END", "3,0:MIDDLE", "4,0:END"), cellsOf(explosions.get(1)));
        assertEquals(2, field.takeNewExplosions().size());
        assertTrue(field.takeNewExplosions().isEmpty(), "한 번 돌려준 폭발은 다시 주지 않는다");
    }

    @Test
    void 물줄기는_3장을_0_15초씩_보여_주고_0_45초가_되면_남지_않는다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1...."));
        BalloonField field = new BalloonField(map);
        Player me = playerAt(1, Mascot.SANGSANGBUGI, 0, 0);
        field.place(me, List.of(me));
        ticks(field, List.of(me), 120);
        Explosion explosion = field.explosions().get(0);

        assertEquals(0, explosion.frame());
        assertTrue(field.isWet(1, 0));
        ticks(field, List.of(me), 9);
        assertEquals(1, explosion.frame());
        ticks(field, List.of(me), 9);
        assertEquals(2, explosion.frame());
        ticks(field, List.of(me), 8);
        assertEquals(1, field.explosions().size());
        assertEquals(1, field.wetOwnerAt(0, 0).orElseThrow());

        ticks(field, List.of(me), 1);
        assertTrue(field.explosions().isEmpty());
        assertFalse(field.isWet(0, 0));
    }

    @Test
    void 놓은_순간_칸에_있던_사람만_빠져나갈_수_있고_한번_나가면_막힌다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1....", ".2..."));
        BalloonField field = new BalloonField(map);
        Player me = playerAt(1, Mascot.SANGSANGBUGI, 0, 0);
        Player other = playerAt(2, Mascot.SANGSANGBUGI, 1, 1);
        List<Player> players = List.of(me, other);
        field.place(me, players);

        assertFalse(field.blocks(me, 0, 0));
        assertTrue(field.blocks(other, 0, 0), "놓을 때 그 칸에 없던 사람에게는 막힌 칸이다");

        // 오른쪽으로 완전히 빠져나간 뒤에는 되돌아가지 못한다
        for (int i = 0; i < 20; i++) {
            me.update(Direction.RIGHT, TICK, (col, row) -> map.isSolid(col, row) || field.blocks(me, col, row));
            field.update(TICK, players);
        }
        assertFalse(me.overlapsTile(0, 0));
        assertTrue(field.blocks(me, 0, 0));
        double x = me.x();
        for (int i = 0; i < 20; i++) {
            me.update(Direction.LEFT, TICK, (col, row) -> map.isSolid(col, row) || field.blocks(me, col, row));
        }
        assertEquals(TileMap.TILE_SIZE + 18, me.x(), 1e-6, "물풍선 칸 앞에서 멈춘다");
        assertTrue(me.x() <= x);
    }

    @Test
    void 물줄기가_남은_칸에_놓은_물풍선은_바로_터진다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1.2.."));
        BalloonField field = new BalloonField(map);
        Player first = playerAt(1, Mascot.SANGSANGBUGI, 0, 0);
        Player second = playerAt(2, Mascot.SANGSANGBUGI, 1, 0);
        List<Player> players = List.of(first, second);
        field.place(first, players);
        ticks(field, players, 120);
        field.takeNewExplosions();

        assertTrue(field.place(second, players).isPresent());

        assertTrue(field.balloons().isEmpty());
        List<Explosion> burst = field.takeNewExplosions();
        assertEquals(1, burst.size());
        assertEquals(2, burst.get(0).ownerId());
    }

    @Test
    void 물풍선을_바로_터뜨릴_수_있다() {
        TileMap map = MapLoader.parse("fixture/open", List.of("1...."));
        BalloonField field = new BalloonField(map);
        Player me = playerAt(1, Mascot.SANGSANGBUGI, 0, 0);
        field.place(me, List.of(me));

        field.detonate(1, 0);
        assertEquals(1, field.balloons().size(), "물풍선이 없는 칸은 그대로");
        field.detonate(0, 0);
        assertTrue(field.balloons().isEmpty());
        assertTrue(field.isWet(1, 0));
    }
}
