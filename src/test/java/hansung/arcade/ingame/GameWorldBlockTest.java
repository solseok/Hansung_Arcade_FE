package hansung.arcade.ingame;

import hansung.arcade.ingame.balloon.Explosion;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.FieldItems.FieldItem;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.ScriptedRandom;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 물줄기가 블럭을 부수고, 부서진 자리에서 아이템이 나오고, 아이템을 먹는 흐름 */
class GameWorldBlockTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);
    /** 물풍선이 놓인 틱부터 터지는 틱까지 */
    private static final int FUSE_TICKS = 120;
    /** 물줄기가 보이는 틱 수 */
    private static final int WATER_TICKS = (int) Math.round(Explosion.SECONDS / TICK);

    private static GameWorld world(Mascot mascot, RandomGenerator random, String... rows) {
        return new GameWorld(MapLoader.parse("fixture/block", List.of(rows)),
                List.of(new PlayerProfile("부기", mascot)), MatchMode.SOLO, random);
    }

    private static GameWorld duo(RandomGenerator random, String... rows) {
        return new GameWorld(MapLoader.parse("fixture/block", List.of(rows)),
                List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI), new PlayerProfile("냥이", Mascot.KKOKKO)),
                MatchMode.SOLO, random);
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

    /** 1P가 선 칸에 물풍선을 놓고 터질 때까지 기다린다 */
    private static void placeAndExplode(GameWorld world) {
        world.update(TICK, Map.of(world.localPlayer().id(), BALLOON));
        ticks(world, FUSE_TICKS);
        assertTrue(world.balloonField().balloons().isEmpty());
    }

    @Test
    void 블럭이_두_개_이어져_있으면_물줄기가_남아도_앞_블럭_하나만_부서진다() {
        // 상찌는 물줄기 2칸으로 시작한다. 물풍선 바로 왼쪽에 블럭 둘
        GameWorld world = world(Mascot.SANGZZI, ScriptedRandom.noDrops(), "..BB1..");
        TileMap map = world.map();

        placeAndExplode(world);

        assertEquals(Tile.FLOOR, map.tileAt(3, 0));
        assertEquals(Tile.BLOCK, map.tileAt(2, 0));
        Explosion explosion = world.balloonField().explosions().get(0);
        assertFalse(explosion.covers(3, 0));
        assertFalse(explosion.covers(2, 0));
        // 반대쪽은 막힘 없이 2칸 뻗는다
        assertTrue(explosion.covers(6, 0));
    }

    @Test
    void 부서진_자리는_그_물줄기가_사라질_때까지도_다시_맞지_않는다() {
        GameWorld world = world(Mascot.SANGZZI, ScriptedRandom.noDrops(), "..BB1..");

        placeAndExplode(world);
        ticks(world, WATER_TICKS);

        assertEquals(Tile.BLOCK, world.map().tileAt(2, 0));
    }

    @Test
    void 같은_틱에_두_물줄기가_한_블럭을_맞혀도_한_번만_맞는다() {
        // 두 사람이 세 번 맞아야 하는 블럭 양쪽에서 같은 틱에 물풍선을 놓는다
        GameWorld world = duo(ScriptedRandom.noDrops(), "1c2", "...");
        List<Player> players = world.localPlayers();

        world.update(TICK, Map.of(players.get(0).id(), BALLOON, players.get(1).id(), BALLOON));
        ticks(world, FUSE_TICKS);

        assertEquals(2, world.balloonField().explosions().size());
        assertEquals(Tile.HARD_BLOCK_2, world.map().tileAt(1, 0));
    }

    @Test
    void 다른_틱에_터진_물줄기는_각자_블럭을_맞힌다() {
        GameWorld world = duo(ScriptedRandom.noDrops(), "1c2", "...");
        List<Player> players = world.localPlayers();

        world.update(TICK, Map.of(players.get(0).id(), BALLOON));
        ticks(world, 30);
        world.update(TICK, Map.of(players.get(1).id(), BALLOON));
        ticks(world, FUSE_TICKS);

        assertEquals(Tile.HARD_BLOCK_1, world.map().tileAt(1, 0));
    }

    @Test
    void 물줄기는_덤불을_지나가고_덤불과_부서지지_않는_블럭은_그대로다() {
        GameWorld world = world(Mascot.SANGZZI, ScriptedRandom.noDrops(), "#1T.");

        placeAndExplode(world);

        Explosion explosion = world.balloonField().explosions().get(0);
        assertTrue(explosion.covers(2, 0));
        assertTrue(explosion.covers(3, 0));
        assertEquals(Tile.BUSH, world.map().tileAt(2, 0));
        assertEquals(Tile.STATUE, world.map().tileAt(0, 0));
    }

    @Test
    void 부서진_블럭_자리에_아이템이_나오고_그_물줄기에는_사라지지_않는다() {
        GameWorld world = world(Mascot.SANGSANGBUGI, ScriptedRandom.alwaysDrops(ItemType.FLUID), "B1.");

        placeAndExplode(world);
        assertEquals(Optional.of(new FieldItem(0, 0, ItemType.FLUID)), world.items().at(0, 0));

        ticks(world, WATER_TICKS + 1);
        assertTrue(world.balloonField().explosions().isEmpty());
        assertEquals(Optional.of(new FieldItem(0, 0, ItemType.FLUID)), world.items().at(0, 0));
    }

    @Test
    void 아이템이_안_나오는_난수면_빈_바닥만_남는다() {
        GameWorld world = world(Mascot.SANGSANGBUGI, ScriptedRandom.noDrops(), "B1.");

        placeAndExplode(world);

        assertEquals(Tile.FLOOR, world.map().tileAt(0, 0));
        assertTrue(world.items().all().isEmpty());
    }

    @Test
    void 여러_번_맞아야_하는_블럭은_다_부서질_때만_아이템이_나온다() {
        GameWorld world = duo(ScriptedRandom.alwaysDrops(ItemType.BUBBLE), "1b2", "...");
        List<Player> players = world.localPlayers();

        world.update(TICK, Map.of(players.get(0).id(), BALLOON));
        ticks(world, FUSE_TICKS);
        assertEquals(Tile.HARD_BLOCK_1, world.map().tileAt(1, 0));
        assertTrue(world.items().all().isEmpty());

        world.update(TICK, Map.of(players.get(1).id(), BALLOON));
        ticks(world, FUSE_TICKS);
        assertEquals(Tile.FLOOR, world.map().tileAt(1, 0));
        assertEquals(Optional.of(new FieldItem(1, 0, ItemType.BUBBLE)), world.items().at(1, 0));
    }

    @Test
    void 이미_놓여_있던_아이템은_물줄기에_맞으면_사라진다() {
        GameWorld world = world(Mascot.SANGSANGBUGI, ScriptedRandom.noDrops(), "1..");
        world.items().place(1, 0, ItemType.BUBBLE);
        world.items().place(2, 0, ItemType.FLUID);

        placeAndExplode(world);

        // 물줄기 1칸이 닿은 칸만 사라진다
        assertEquals(List.of(new FieldItem(2, 0, ItemType.FLUID)), world.items().all());
    }

    @Test
    void 아이템_칸으로_걸어_들어가면_먹고_능력치가_오른다() {
        GameWorld world = world(Mascot.KKUKKU, ScriptedRandom.noDrops(), "1....");
        Player me = world.localPlayer();
        world.items().place(1, 0, ItemType.BUBBLE);
        world.items().place(2, 0, ItemType.FLUID);
        world.items().place(3, 0, ItemType.ROLLER_SHOE);

        walk(world, me, Direction.RIGHT, 80);

        assertEquals(4, me.col());
        assertTrue(world.items().all().isEmpty());
        assertEquals(2, me.balloons());
        assertEquals(2, me.waterRange());
        assertEquals(5, me.speedLevel());
    }

    @Test
    void 몸이_걸치기만_하고_가운데가_들어오지_않으면_먹지_않는다() {
        GameWorld world = world(Mascot.KKUKKU, ScriptedRandom.noDrops(), "1.");
        Player me = world.localPlayer();
        world.items().place(1, 0, ItemType.BUBBLE);

        // 가운데가 칸 경계를 넘기 직전까지 (20px 안쪽)만 걷는다. 꾸꾸는 초당 4타일 = 160px
        walk(world, me, Direction.RIGHT, 7);

        assertTrue(me.overlapsTile(1, 0));
        assertEquals(0, me.col());
        assertEquals(1, world.items().all().size());
    }

    @Test
    void 갇힌_사람은_아이템을_먹지_못한다() {
        GameWorld world = world(Mascot.SANGSANGBUGI, ScriptedRandom.noDrops(), "1..");
        Player me = world.localPlayer();
        placeAndExplode(world);
        assertEquals(PlayerState.TRAPPED, me.state());
        ticks(world, WATER_TICKS + 1);

        world.items().place(0, 0, ItemType.BUBBLE);
        ticks(world, 5);

        assertEquals(1, world.items().all().size());
        assertEquals(1, me.balloons());
    }
}
