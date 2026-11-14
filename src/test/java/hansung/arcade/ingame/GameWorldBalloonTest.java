package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.MatchOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 게임 상태에서 물풍선 놓기·갇힘·구출·처치가 어떻게 이어지는지 */
class GameWorldBalloonTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);
    /** 1P와 2P가 한 칸 띄어 같은 줄에 선다 */
    private static final TileMap DUO = MapLoader.parse("fixture/duo", List.of(".1.2..", "......"));

    private static GameWorld duo(MatchMode mode, int team1, int team2) {
        return new GameWorld(DUO, List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI, team1),
                new PlayerProfile("냥이", Mascot.HANSUNGNYANGI, team2)), mode);
    }

    private static void ticks(GameWorld world, int count) {
        for (int i = 0; i < count; i++) {
            world.update(TICK, Map.of());
        }
    }

    private static void walk(GameWorld world, Player player, Direction direction, int count) {
        for (int i = 0; i < count; i++) {
            world.update(TICK, Map.of(player.id(), PlayerCommand.moving(direction)));
        }
    }

    /** 1P가 제자리에 물풍선을 놓고 2초 뒤 자기 물줄기에 갇히게 한다 */
    private static void trapFirstByOwnBalloon(GameWorld world) {
        Player first = world.localPlayers().get(0);
        world.update(TICK, Map.of(first.id(), BALLOON));
        ticks(world, 120);
        assertEquals(PlayerState.TRAPPED, first.state());
    }

    @Test
    void 물풍선_키를_막_누른_틱에_선_칸에_물풍선을_놓는다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);

        world.update(TICK, Map.of(first.id(), BALLOON));

        assertEquals(1, world.balloonField().balloons().size());
        assertEquals(1, world.balloonField().balloons().get(0).col());
        assertEquals(first.id(), world.balloonField().balloons().get(0).ownerId());
    }

    @Test
    void 물줄기에_맞으면_자기_물줄기여도_갇히고_옆_칸_사람도_갇힌다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        // 2P를 1P 바로 옆 칸으로 붙인다 (3열 → 2열)
        walk(world, second, Direction.LEFT, 13);
        assertEquals(2, second.col());

        world.update(TICK, Map.of(first.id(), BALLOON));
        ticks(world, 119);
        assertEquals(PlayerState.NORMAL, first.state());

        ticks(world, 1);
        assertEquals(PlayerState.TRAPPED, first.state());
        assertEquals(PlayerState.TRAPPED, second.state());
        assertEquals(first.id(), second.trapperId().orElseThrow());
    }

    @Test
    void 개인전에서_갇힌_사람에게_상대가_닿으면_터지고_닿은_사람이_kill을_얻는다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        trapFirstByOwnBalloon(world);
        // 물줄기가 다 사라진 뒤에 다가간다
        ticks(world, 30);

        // 냥이는 초당 160px라 25틱이면 1P와 반 칸 안으로 겹친다
        walk(world, second, Direction.LEFT, 25);

        assertEquals(PlayerState.DYING, first.state());
        assertFalse(first.isAlive());
        assertEquals(1, world.match().kills(second.id()));
        assertEquals(0, world.match().kills(first.id()));
        // 둘 중 하나만 남아 그 틱에 경기가 끝난다
        assertTrue(world.match().isOver());
        assertEquals(MatchOutcome.WIN, world.match().result().orElseThrow().outcomeOf(second.id()));
    }

    @Test
    void 팀전에서_같은_편이_닿으면_구해_save를_얻고_구한_사람은_잠깐_못_움직인_뒤_돌아온다() {
        GameWorld world = duo(MatchMode.TEAM, 1, 1);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        trapFirstByOwnBalloon(world);
        ticks(world, 30);

        // 2P가 닿는 틱까지 걸어간다
        int steps = 0;
        while (first.state() == PlayerState.TRAPPED && steps++ < 60) {
            walk(world, second, Direction.LEFT, 1);
        }

        assertEquals(PlayerState.ESCAPING, first.state());
        assertTrue(first.isAlive());
        assertEquals(1, world.match().saves(second.id()));
        assertTrue(Math.abs(second.x() - first.x()) < TileMap.TILE_SIZE / 2.0);
        // 구한 틱부터 0.5초(30틱) 동안 움직이지 못한다
        double x = first.x();
        walk(world, first, Direction.RIGHT, 29);
        assertEquals(PlayerState.ESCAPING, first.state());
        assertEquals(x, first.x(), 1e-9, "빠져나오는 동안은 움직이지 못한다");
        walk(world, first, Direction.RIGHT, 1);
        assertEquals(PlayerState.NORMAL, first.state());
        assertTrue(first.x() > x);
    }

    @Test
    void 아무도_건드리지_않으면_5초_뒤_탈락하고_가둔_사람이_상대면_kill을_얻는다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        // 2P가 1P 옆에 물풍선을 놓고 피한다
        walk(world, second, Direction.LEFT, 13);
        world.update(TICK, Map.of(second.id(), BALLOON));
        walk(world, second, Direction.DOWN, 20);
        walk(world, second, Direction.RIGHT, 30);
        // 물풍선은 놓은 틱부터 120틱 뒤에 터진다 (놓은 뒤 50틱을 걸었다)
        ticks(world, 120 - 50);
        assertEquals(PlayerState.TRAPPED, first.state());
        assertEquals(PlayerState.NORMAL, second.state());

        ticks(world, 299);
        assertEquals(PlayerState.TRAPPED, first.state());
        ticks(world, 1);

        assertEquals(PlayerState.DYING, first.state());
        assertEquals(1, world.match().kills(second.id()));
        // 탈락 연출 2초가 끝나면 사라진다
        ticks(world, 120);
        assertEquals(PlayerState.GONE, first.state());
    }

    @Test
    void 자기_물줄기에_갇혀_시간이_다_되면_아무도_kill을_얻지_않는다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);
        trapFirstByOwnBalloon(world);

        ticks(world, 300);

        assertFalse(first.isAlive());
        assertEquals(0, world.match().kills(first.id()));
        assertEquals(0, world.match().kills(world.localPlayers().get(1).id()));
    }

    @Test
    void 놓인_물풍선은_다른_사람을_막는다() {
        GameWorld world = duo(MatchMode.SOLO, 1, 1);
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        world.update(TICK, Map.of(first.id(), BALLOON));
        walk(world, first, Direction.DOWN, 20);

        walk(world, second, Direction.LEFT, 40);

        // 한 번에 1px 안쪽으로 잘게 움직이므로 물풍선 칸 경계 1px 앞에서 멈춘다
        double leftEdge = second.x() - 18;
        assertTrue(leftEdge >= 2 * TileMap.TILE_SIZE && leftEdge < 2 * TileMap.TILE_SIZE + 1, "왼쪽 끝: " + leftEdge);
    }

    @Test
    void 경기가_끝난_뒤에는_물풍선을_놓지_못하고_물줄기도_가두지_않는다() {
        GameWorld world = duo(MatchMode.TEAM, 1, 1);
        Player first = world.localPlayers().get(0);
        // 끝나기 1초 전에 놓은 물풍선은 끝난 뒤에 터진다
        ticks(world, MatchClock.DURATION_SECONDS * 60 - 60);
        world.update(TICK, Map.of(first.id(), BALLOON));
        ticks(world, 59);
        assertTrue(world.match().isOver());

        world.update(TICK, Map.of(first.id(), BALLOON));
        assertEquals(1, world.balloonField().balloons().size(), "끝난 뒤에는 새로 놓지 못한다");
        ticks(world, 60);

        assertTrue(world.balloonField().balloons().isEmpty(), "놓여 있던 물풍선은 그대로 터진다");
        assertFalse(world.balloonField().explosions().isEmpty());
        assertEquals(PlayerState.NORMAL, first.state());
    }
}
