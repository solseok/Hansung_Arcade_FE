package hansung.arcade.ingame;

import hansung.arcade.ingame.block.BoxPusher;
import hansung.arcade.ingame.block.SlidingBlock;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 상자(P)를 몸으로 밀어 한 칸씩 옮기는 규칙 */
class GameWorldPushTest {

    private static final double TICK = 1.0 / 60;
    private static final int T = TileMap.TILE_SIZE;
    /** 상자에 닿는 데 걸리는 틱 (상상부기는 한 틱에 3px, 칸 가운데에서 상자까지 2px) 과 넉넉한 여유 */
    private static final int REACH_TICKS = 2;
    /** 상자가 밀리는 데 필요한 틱 수 */
    private static final int PUSH_TICKS = (int) Math.ceil(BoxPusher.PUSH_SECONDS / TICK);

    private static GameWorld solo(String... rows) {
        return new GameWorld(MapLoader.parse("fixture/push", List.of(rows)), new PlayerProfile("부기", Mascot.SANGSANGBUGI));
    }

    private static GameWorld duo(String... rows) {
        return new GameWorld(MapLoader.parse("fixture/push", List.of(rows)),
                List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI), new PlayerProfile("냥이", Mascot.KKOKKO)));
    }

    private static void walk(GameWorld world, Player player, Direction direction, int count) {
        for (int i = 0; i < count; i++) {
            world.update(TICK, Map.of(player.id(), PlayerCommand.moving(direction)));
        }
    }

    private static void ticks(GameWorld world, int count) {
        for (int i = 0; i < count; i++) {
            world.update(TICK, Map.of());
        }
    }

    @Test
    void 상자에_막힌_채_잠깐_밀어서는_움직이지_않고_계속_밀면_한_칸_밀린다() {
        GameWorld world = solo("1P..");
        Player me = world.localPlayer();

        walk(world, me, Direction.RIGHT, REACH_TICKS + PUSH_TICKS - 3);
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));

        walk(world, me, Direction.RIGHT, 3);
        assertEquals(Tile.FLOOR, world.map().tileAt(1, 0));
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
    }

    @Test
    void 밀다가_손을_떼면_민_시간이_처음부터_다시_간다() {
        GameWorld world = solo("1P..");
        Player me = world.localPlayer();

        walk(world, me, Direction.RIGHT, REACH_TICKS + PUSH_TICKS - 5);
        ticks(world, 1);
        walk(world, me, Direction.RIGHT, PUSH_TICKS - 5);

        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));
    }

    @Test
    void 계속_밀면_따라가며_한_칸씩_또_민다() {
        GameWorld world = solo("1P...");
        Player me = world.localPlayer();

        // 한 번 밀 때마다 한 칸 걸어가 닿고, 미끄러짐이 끝난 뒤 다시 0.25초를 민다
        walk(world, me, Direction.RIGHT, 120);

        assertEquals(Tile.PUSH_BOX, world.map().tileAt(4, 0));
        assertEquals(Tile.FLOOR, world.map().tileAt(3, 0));
        assertEquals(3, me.col());
    }

    @Test
    void 세로로도_민다() {
        GameWorld world = solo("1", "P", ".");
        Player me = world.localPlayer();

        walk(world, me, Direction.DOWN, REACH_TICKS + PUSH_TICKS);

        assertEquals(Tile.FLOOR, world.map().tileAt(0, 1));
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(0, 2));
    }

    @Test
    void 밀려_갈_칸이_블럭이거나_덤불이거나_맵_밖이면_밀리지_않는다() {
        for (String row : new String[]{"1PB", "1PP", "1PT", "1P#", "1P"}) {
            GameWorld world = solo(row);

            walk(world, world.localPlayer(), Direction.RIGHT, 60);

            assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0), row);
        }
    }

    @Test
    void 밀려_갈_칸에_아이템이_있으면_밀리지_않는다() {
        GameWorld world = solo("1P.");
        world.items().place(2, 0, ItemType.BUBBLE);

        walk(world, world.localPlayer(), Direction.RIGHT, 60);

        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));
    }

    @Test
    void 밀려_갈_칸에_다른_플레이어가_있으면_밀리지_않고_비켜나면_바로_밀린다() {
        GameWorld world = duo("1P2", "...");
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);

        walk(world, first, Direction.RIGHT, 60);
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));

        // 2P가 아래 칸으로 완전히 비켜날 때까지 1P는 계속 민다
        for (int i = 0; i < 30; i++) {
            world.update(TICK, Map.of(first.id(), PlayerCommand.moving(Direction.RIGHT),
                    second.id(), PlayerCommand.moving(Direction.DOWN)));
        }
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
    }

    @Test
    void 밀려_갈_칸에_물풍선이_있으면_밀리지_않는다() {
        GameWorld world = duo("1P.", "..2");
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        // 2P가 (2, 0)에 물풍선을 놓고 다시 아래로 비킨다
        walk(world, second, Direction.UP, 14);
        assertEquals(0, second.row());
        world.update(TICK, Map.of(second.id(), new PlayerCommand(null, true, false)));
        walk(world, second, Direction.DOWN, 14);
        assertTrue(world.balloonField().balloonAt(2, 0).isPresent());

        walk(world, first, Direction.RIGHT, 40);

        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));
    }

    @Test
    void 밀려_갈_칸에_물줄기가_남아_있으면_밀리지_않고_물줄기가_사라지면_바로_밀린다() {
        GameWorld world = duo("1P.2", "....");
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        // 2P가 (3, 0)에 물풍선을 놓고 물줄기가 닿지 않는 (1, 1)로 비킨다
        world.update(TICK, Map.of(second.id(), new PlayerCommand(null, true, false)));
        second.placeAt(TileMap.centerX(1), TileMap.centerY(1));
        walk(world, first, Direction.RIGHT, REACH_TICKS + PUSH_TICKS - 3);
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));

        // 물풍선이 터져 밀려 갈 칸 (2, 0)에 물줄기가 남는다
        world.balloonField().detonate(3, 0);
        walk(world, first, Direction.RIGHT, 10);
        assertTrue(world.balloonField().isWet(2, 0));
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0), "물줄기가 남은 칸으로는 밀리지 않는다");

        // 물줄기는 밀기를 본 뒤에 사라지므로, 사라진 다음 틱에 민 시간이 이미 차 있어 바로 밀린다
        while (world.balloonField().isWet(2, 0)) {
            walk(world, first, Direction.RIGHT, 1);
        }
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(1, 0));
        walk(world, first, Direction.RIGHT, 1);
        assertEquals(Tile.FLOOR, world.map().tileAt(1, 0));
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
    }

    @Test
    void 밀린_상자는_떠난_칸에서_도착_칸까지_미끄러져_보인다() {
        GameWorld world = solo("1P..");
        Player me = world.localPlayer();
        BoxPusher pusher = world.boxPusher();

        walk(world, me, Direction.RIGHT, REACH_TICKS + PUSH_TICKS);
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
        SlidingBlock slide = pusher.slidingInto(2, 0).orElseThrow();
        // 도착 칸 기준으로 왼쪽(떠난 칸 쪽)에 그린다
        assertTrue(slide.offsetX() < 0 && slide.offsetX() > -T, "offsetX " + slide.offsetX());
        assertEquals(0, slide.offsetY());

        ticks(world, (int) Math.ceil(BoxPusher.SLIDE_SECONDS / TICK));
        assertTrue(pusher.slides().isEmpty());
    }

    @Test
    void 철로_보강된_상자도_밀리고_남은_맞을_횟수째_옮겨진다() {
        GameWorld world = solo("1b..");
        Player me = world.localPlayer();

        walk(world, me, Direction.RIGHT, REACH_TICKS + PUSH_TICKS);

        assertEquals(Tile.FLOOR, world.map().tileAt(1, 0));
        assertEquals(Tile.HARD_BLOCK_2, world.map().tileAt(2, 0));
    }

    @Test
    void 미끄러지는_중인_상자는_다시_밀리지_않는다() {
        GameWorld world = solo("1P...");
        Player me = world.localPlayer();

        walk(world, me, Direction.RIGHT, REACH_TICKS + PUSH_TICKS);
        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
        // 미는 동안 따라온 몸이 다시 상자에 닿아도 미끄러지는 동안은 시간을 쌓지 않는다
        walk(world, me, Direction.RIGHT, (int) (BoxPusher.SLIDE_SECONDS / TICK) - 1);

        assertEquals(Tile.PUSH_BOX, world.map().tileAt(2, 0));
    }
}
