package hansung.arcade.ingame;

import hansung.arcade.ingame.airplane.Drop;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.item.ScriptedRandom;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 아이템을 먹고 쓰는 일이 한 틱의 물풍선 싸움 규칙과 맞물리는지 */
class GameWorldItemTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);
    private static final PlayerCommand USE_ITEM = new PlayerCommand(null, false, true);

    /** 상상부기 혼자. 1이 스폰 */
    private static GameWorld solo(String... rows) {
        return new GameWorld(MapLoader.parse("fixture/items", List.of(rows)),
                new PlayerProfile("부기", Mascot.SANGSANGBUGI));
    }

    /** 1P가 선 칸에 item을 놓고 한 틱 흘려 먹게 한다 */
    private static void pickUp(GameWorld world, ItemType item) {
        Player me = world.localPlayer();
        world.items().place(me.col(), me.row(), item);
        tick(world, PlayerCommand.IDLE);
    }

    private static void tick(GameWorld world, PlayerCommand command) {
        world.update(TICK, Map.of(world.localPlayer().id(), command));
    }

    private static void ticks(GameWorld world, int count) {
        for (int i = 0; i < count; i++) {
            tick(world, PlayerCommand.IDLE);
        }
    }

    private static void walk(GameWorld world, Direction direction, int count) {
        for (int i = 0; i < count; i++) {
            tick(world, PlayerCommand.moving(direction));
        }
    }

    /** 선 칸에 물풍선을 놓고 터질 때까지 그 자리에 있는다 */
    private static void soakInOwnBalloon(GameWorld world) {
        tick(world, BALLOON);
        ticks(world, 120);
    }

    @Test
    void 액티브_아이템을_먹으면_아이템_칸에_들어간다() {
        GameWorld world = solo(".1...");
        PlayerGear gear = world.gear(world.localPlayer());

        pickUp(world, ItemType.SHIELD);
        pickUp(world, ItemType.SHIELD);

        assertEquals(Optional.of(ItemType.SHIELD), gear.activeItem());
        assertEquals(2, gear.activeCount());
    }

    @Test
    void 신발이_있으면_물풍선_쪽으로_걸어가_부딪힌_물풍선을_민다() {
        GameWorld world = solo(".1......B");
        pickUp(world, ItemType.SHOE);
        tick(world, BALLOON);
        walk(world, Direction.LEFT, 15);
        assertEquals(0, world.localPlayer().col());

        walk(world, Direction.RIGHT, 5);
        ticks(world, 60);

        WaterBalloon balloon = world.balloonField().balloons().get(0);
        assertEquals(7, balloon.col());
        assertTrue(balloon.isResting());
    }

    @Test
    void 신발이_없으면_물풍선에_막혀_밀지_못한다() {
        GameWorld world = solo(".1......B");
        tick(world, BALLOON);
        walk(world, Direction.LEFT, 15);

        walk(world, Direction.RIGHT, 20);

        assertEquals(1, world.balloonField().balloons().get(0).col());
        assertEquals(0, world.localPlayer().col());
    }

    @Test
    void 장갑이_있으면_선_칸의_물풍선을_바라보는_쪽으로_6칸_던진다() {
        GameWorld world = solo(".1..........");
        pickUp(world, ItemType.GLOVE);
        tick(world, BALLOON);
        // 한 틱만 오른쪽으로 걸어 오른쪽을 본다 (아직 같은 칸)
        walk(world, Direction.RIGHT, 1);

        tick(world, BALLOON);

        WaterBalloon balloon = world.balloonField().balloons().get(0);
        assertTrue(balloon.isFlying());
        ticks(world, 30);
        assertEquals(7, balloon.col());
        assertTrue(balloon.isResting());
        assertEquals(1, world.balloonField().balloons().size(), "던졌으니 새로 놓지 않는다");
    }

    @Test
    void 장갑이_있으면_바라보는_앞_칸의_물풍선도_던진다() {
        GameWorld world = solo(".1..........");
        pickUp(world, ItemType.GLOVE);
        tick(world, BALLOON);
        walk(world, Direction.LEFT, 15);
        walk(world, Direction.RIGHT, 1);
        assertEquals(0, world.localPlayer().col());

        tick(world, BALLOON);
        ticks(world, 30);

        // 물풍선이 있던 1열을 0번째로 6칸째
        assertEquals(7, world.balloonField().balloons().get(0).col());
    }

    @Test
    void 장갑이_있어도_던질_물풍선이_없으면_물풍선을_놓는다() {
        GameWorld world = solo(".1...");
        pickUp(world, ItemType.GLOVE);

        tick(world, BALLOON);

        WaterBalloon balloon = world.balloonField().balloons().get(0);
        assertEquals(1, balloon.col());
        assertTrue(balloon.isResting());
    }

    @Test
    void 보호막을_쓰는_동안에는_물줄기에_맞아도_갇히지_않는다() {
        GameWorld world = solo(".1...");
        Player me = world.localPlayer();
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.SHIELD);

        tick(world, USE_ITEM);
        assertTrue(gear.isShielded());
        assertTrue(gear.activeItem().isEmpty());
        soakInOwnBalloon(world);
        assertEquals(PlayerState.NORMAL, me.state());

        // 3초가 지나 보호막이 걷히면 다시 갇힌다
        ticks(world, 60);
        assertFalse(gear.isShielded());
        soakInOwnBalloon(world);
        assertEquals(PlayerState.TRAPPED, me.state());
    }

    @Test
    void 탱크에_타고_있으면_한_번은_갇히는_대신_탱크가_부서진다() {
        GameWorld world = solo(".1...");
        Player me = world.localPlayer();
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.TANK);
        assertTrue(gear.hasTank());

        soakInOwnBalloon(world);
        assertEquals(PlayerState.NORMAL, me.state());
        assertFalse(gear.hasTank());
        // 물줄기가 남은 동안에도 다시 갇히지 않는다
        ticks(world, 30);
        assertEquals(PlayerState.NORMAL, me.state());

        soakInOwnBalloon(world);
        assertEquals(PlayerState.TRAPPED, me.state());
    }

    @Test
    void 갇혔을_때_바늘을_쓰면_바로_빠져나온다() {
        GameWorld world = solo(".1...");
        Player me = world.localPlayer();
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.NEEDLE);
        // 갇히지 않았을 때는 써지지 않고 남는다
        tick(world, USE_ITEM);
        assertEquals(1, gear.activeCount());
        soakInOwnBalloon(world);
        assertEquals(PlayerState.TRAPPED, me.state());

        tick(world, USE_ITEM);

        assertEquals(PlayerState.ESCAPING, me.state());
        assertTrue(gear.activeItem().isEmpty());
        ticks(world, 30);
        assertEquals(PlayerState.NORMAL, me.state());
    }

    @Test
    void 스프링_신발을_쓰면_바라보는_쪽_블럭을_넘어_그_너머_칸에_내려앉는다() {
        GameWorld world = solo(".1B..");
        Player me = world.localPlayer();
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.SPRING_SHOE);
        walk(world, Direction.RIGHT, 3);
        assertEquals(1, me.col());

        tick(world, USE_ITEM);
        assertTrue(gear.isAirborne());
        // 공중에서는 걷지 않는다
        walk(world, Direction.LEFT, 10);
        ticks(world, 20);

        assertFalse(gear.isAirborne());
        assertEquals(TileMap.centerX(3), me.x(), 1e-9);
        assertTrue(gear.activeItem().isEmpty());
    }

    @Test
    void 스프링_신발은_내려앉을_칸이_막혀_있으면_쓰지_않고_남는다() {
        GameWorld world = solo(".1BB.");
        Player me = world.localPlayer();
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.SPRING_SHOE);
        walk(world, Direction.RIGHT, 3);

        tick(world, USE_ITEM);

        assertFalse(gear.isAirborne());
        assertEquals(1, gear.activeCount());
        assertEquals(1, me.col());
    }

    @Test
    void 스프링_신발은_던진_물풍선이_내려앉을_칸으로는_쓰지_않고_남는다() {
        GameWorld world = new GameWorld(MapLoader.parse("fixture/items", List.of(".1.......2")),
                List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI), new PlayerProfile("냥이", Mascot.HANSUNGNYANGI)));
        Player me = world.localPlayer();
        Player other = world.localPlayers().get(1);
        PlayerGear gear = world.gear(me);
        pickUp(world, ItemType.SPRING_SHOE);
        walk(world, Direction.RIGHT, 3);
        assertEquals(1, me.col());
        // 2P가 제자리 (9, 0)에 놓은 물풍선을 왼쪽으로 던져, 1P가 뛰어 내려앉을 (3, 0)에 내려앉게 한다
        WaterBalloon balloon = world.balloonField().place(other, world.players()).orElseThrow();
        assertTrue(world.balloonField().throwBalloon(balloon, Direction.LEFT));
        assertEquals(3, balloon.col());

        tick(world, USE_ITEM);

        assertTrue(balloon.isFlying());
        assertFalse(gear.isAirborne());
        assertEquals(1, gear.activeCount());
    }

    @Test
    void 점프해_공중에_있는_동안에는_물줄기에_맞지_않는다() {
        GameWorld world = solo("..1..");
        Player me = world.localPlayer();
        pickUp(world, ItemType.SPRING_SHOE);
        tick(world, BALLOON);
        ticks(world, 110);
        walk(world, Direction.RIGHT, 1);

        // 물풍선이 터지기 직전에 뛰어 물줄기 위를 지나간다
        tick(world, USE_ITEM);
        ticks(world, 30);

        assertEquals(4, me.col());
        assertEquals(PlayerState.NORMAL, me.state());
    }

    @Test
    void 빨간_악마로_민_물풍선이_가시_난_면에_닿으면_바로_터진다() {
        GameWorld world = solo(".1....<");
        pickUp(world, ItemType.RED_DEVIL);
        tick(world, BALLOON);
        walk(world, Direction.LEFT, 15);

        walk(world, Direction.RIGHT, 3);
        ticks(world, 40);

        assertTrue(world.balloonField().balloons().isEmpty());
        // 터진 물풍선은 가시 블럭 바로 앞 칸에서 터졌고, 물줄기에 맞은 가시 블럭은 부서졌다
        assertTrue(world.map().tileAt(6, 0).isGround());
    }

    @Test
    void 게임을_진행하면_1분59초에_비행기가_나와_빈_칸에_아이템을_떨어뜨린다() {
        // 15 x 13 빈 맵의 맨 아랫줄 왼쪽 끝에 1P가 선다. 난수 0.5로는 (7, 6)과 (8, 6)에 신발을 떨어뜨린다
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < 12; row++) {
            rows.add(".".repeat(15));
        }
        rows.add("1" + ".".repeat(14));
        GameWorld world = new GameWorld(MapLoader.parse("fixture/airdrop", rows),
                List.of(new PlayerProfile("부기", Mascot.KKUKKU)), MatchMode.SOLO, ScriptedRandom.of(0.5));
        ticks(world, 61 * 60);
        assertEquals(1, world.airDrop().flights().size());

        ticks(world, 4 * 60);

        assertTrue(world.airDrop().flights().isEmpty());
        assertEquals(List.of(new Drop(7, 6, ItemType.SHOE), new Drop(8, 6, ItemType.SHOE)), world.items().all().stream()
                .map(item -> new Drop(item.col(), item.row(), item.type())).toList());
    }

    @Test
    void 장갑으로_맵_끝을_보고_던지면_던지지_않고_물풍선도_새로_놓지_않는다() {
        GameWorld world = solo("1....", ".....");
        Player me = world.localPlayer();
        pickUp(world, ItemType.GLOVE);
        pickUp(world, ItemType.BUBBLE);
        walk(world, Direction.LEFT, 1);
        tick(world, BALLOON);

        tick(world, BALLOON);
        tick(world, BALLOON);
        ticks(world, 10);

        assertEquals(1, world.balloonField().balloons().size());
        assertTrue(world.balloonField().balloons().get(0).isResting());
        assertEquals(PlayerState.NORMAL, me.state());
        assertTrue(world.balloonField().explosions().isEmpty());
    }
}
