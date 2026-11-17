package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcealmentTest {

    /** 덤불 두 개((1, 0), (3, 0))와 빈 바닥 */
    private static final TileMap MAP = MapLoader.parse("fixture/bush", List.of(".T.T.", "1...."));

    @Test
    void 아무도_들어가지_않은_덤불_칸은_숨기고_다른_칸은_숨기지_않는다() {
        Concealment concealment = Concealment.forViewers(MAP, List.of());

        assertTrue(concealment.hides(1, 0));
        assertTrue(concealment.hides(3, 0));
        assertFalse(concealment.hides(0, 0));
        assertFalse(concealment.hides(1, 1));
        assertFalse(concealment.hides(-1, 0));
        assertFalse(concealment.isRevealedBush(1, 0));
    }

    @Test
    void 화면을_보는_플레이어가_들어간_덤불만_드러난다() {
        Player me = new Player(1, 1, 0);
        Concealment concealment = Concealment.forViewers(MAP, List.of(me));

        assertTrue(concealment.isRevealedBush(1, 0));
        assertFalse(concealment.hides(1, 0));
        assertFalse(concealment.hides(me));
        assertTrue(concealment.hides(3, 0));
        assertFalse(concealment.isRevealedBush(0, 0));
    }

    @Test
    void 다른_덤불에_숨은_상대는_보이지_않는다() {
        Player me = new Player(1, 1, 0);
        Player other = new Player(2, 3, 0);

        Concealment concealment = Concealment.forViewers(MAP, List.of(me));

        assertTrue(concealment.hides(other));
    }

    @Test
    void 같은_덤불에_함께_들어간_상대는_보인다() {
        Player me = new Player(1, 1, 0);
        Player other = new Player(2, 1, 0);

        assertFalse(Concealment.forViewers(MAP, List.of(me)).hides(other));
    }

    @Test
    void 갇혀도_화면을_보는_플레이어는_자기_덤불을_드러낸다() {
        Player me = new Player(1, 1, 0);
        me.trap(1);

        assertTrue(Concealment.forViewers(MAP, List.of(me)).isRevealedBush(1, 0));
    }

    @Test
    void 사라진_플레이어는_덤불을_드러내지_않는다() {
        Player me = new Player(1, 1, 0);
        me.eliminate();
        for (int i = 0; i < 200; i++) {
            me.update(null, 1.0 / 60, MAP);
        }

        assertTrue(Concealment.forViewers(MAP, List.of(me)).hides(1, 0));
    }

    @Test
    void 한_키보드_2인_모드에서는_두_사람_모두_화면을_보는_플레이어라_숨지_않는다() {
        TileMap map = MapLoader.parse("fixture/bush2", List.of(".T.T.", "[spawn]", ".1.2."));
        GameWorld world = new GameWorld(map, List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI),
                new PlayerProfile("냥이", Mascot.HANSUNGNYANGI)));
        world.update(1.0 / 60, Map.of());

        Concealment concealment = Concealment.of(world);

        assertTrue(concealment.isRevealedBush(1, 0));
        assertTrue(concealment.isRevealedBush(3, 0));
        for (Player player : world.players()) {
            assertFalse(concealment.hides(player));
        }
    }
}
