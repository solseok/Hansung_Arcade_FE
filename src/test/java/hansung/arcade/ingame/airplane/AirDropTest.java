package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.ScriptedRandom;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 비행기의 출현 시각, 떨어뜨릴 칸 고르기, 떨어뜨릴지 판단, 떨어진 아이템 놓기.
 *
 * <p>게임 상태는 시계와 칸 점유를 보여 주는 환경으로만 쓰고, 비행기는 따로 만든 {@link AirDrop}으로 진행한다.
 * 맵은 15 x 13칸이고 1P는 (0, 12)에서 시작한다.</p>
 */
class AirDropTest {

    private static final double TICK = 1.0 / 60;
    private static final int COLUMNS = 15;
    private static final int ROWS = 13;

    /** 15 x 13 맵. rows에 준 줄로 위쪽부터 채우고 나머지는 빈 바닥이며, 1P는 맨 아랫줄 왼쪽 끝에 선다 */
    private static TileMap map(String... rows) {
        List<String> lines = new ArrayList<>(List.of(rows));
        while (lines.size() < ROWS - 1) {
            lines.add(".".repeat(COLUMNS));
        }
        lines.add("1" + ".".repeat(COLUMNS - 2) + "2");
        return MapLoader.parse("fixture/airdrop", lines);
    }

    private static GameWorld world(TileMap map, int players) {
        List<PlayerProfile> profiles = new ArrayList<>();
        for (int i = 1; i <= players; i++) {
            profiles.add(new PlayerProfile("p" + i, Mascot.KKUKKU));
        }
        return new GameWorld(map, profiles, MatchMode.SOLO, ScriptedRandom.noDrops());
    }

    /** 게임과 비행기를 한 틱씩 n번 진행한다 */
    private static void run(GameWorld world, AirDrop airDrop, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.update(TICK, Map.of());
            airDrop.update(TICK, world);
        }
    }

    /** 비행기가 떨어뜨리는 자리가 col 열 가운데에 닿기 직전까지 날린다 */
    private static void flyUntilJustBefore(GameWorld world, AirDrop airDrop, int col) {
        Flight flight = airDrop.flights().get(0);
        double target = TileMap.centerX(col);
        while (Math.abs(target - flight.x()) > Flight.SPEED * TICK) {
            run(world, airDrop, 1);
        }
    }

    private static FlightPlan eastward(Drop... drops) {
        return new FlightPlan(Direction.RIGHT, List.of(drops));
    }

    @Test
    void 남은_시간이_1분59초_1분36초_1분14초_51초_29초_7초가_되는_순간_나온다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        List<Integer> appearedAt = new ArrayList<>();
        List<Direction> headings = new ArrayList<>();

        while (!world.match().isOver()) {
            run(world, airDrop, 1);
            for (Flight flight : airDrop.flights()) {
                if (flight.age() == TICK) {
                    appearedAt.add(world.match().clock().remainingSeconds());
                    headings.add(flight.heading());
                }
            }
        }

        assertEquals(List.of(119, 96, 74, 51, 29, 7), appearedAt);
        // 첫 비행기는 왼쪽에서 오른쪽으로, 다음부터 번갈아
        assertEquals(List.of(Direction.RIGHT, Direction.LEFT, Direction.RIGHT, Direction.LEFT, Direction.RIGHT,
                Direction.LEFT), headings);
    }

    @Test
    void 게임을_진행하면_1분59초에_비행기가_나와_빈_칸에_아이템을_떨어뜨린다() {
        // 난수 0.5로는 (7, 6)과 (8, 6)에 신발을 떨어뜨린다
        GameWorld world = new GameWorld(map(), List.of(new PlayerProfile("p1", Mascot.KKUKKU)), MatchMode.SOLO,
                ScriptedRandom.of(0.5));
        for (int i = 0; i < 61 * 60; i++) {
            world.update(TICK, Map.of());
        }
        assertEquals(1, world.airDrop().flights().size());

        for (int i = 0; i < 4 * 60; i++) {
            world.update(TICK, Map.of());
        }

        assertTrue(world.airDrop().flights().isEmpty());
        assertEquals(List.of(new Drop(7, 6, ItemType.SHOE), new Drop(8, 6, ItemType.SHOE)), world.items().all().stream()
                .map(item -> new Drop(item.col(), item.row(), item.type())).toList());
    }

    @Test
    void 남은_시간이_120초일_때는_아직_나오지_않는다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));

        run(world, airDrop, 60 * 60);
        assertEquals(120, world.match().clock().remainingSeconds());
        assertTrue(airDrop.flights().isEmpty());

        run(world, airDrop, 60);
        assertEquals(119, world.match().clock().remainingSeconds());
        assertEquals(1, airDrop.flights().size());
    }

    @Test
    void 남은_시간이_출현_시각이_되는_틱에_경기가_끝나면_나오지_않는다() {
        GameWorld world = world(map(), 2);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        run(world, airDrop, 61 * 60 - 1);
        assertEquals(120, world.match().clock().remainingSeconds());

        // 1:59가 되는 바로 그 틱에 마지막 상대가 탈락해 경기가 끝난다
        world.players().get(1).eliminate();
        run(world, airDrop, 1);

        assertTrue(world.match().isOver());
        assertTrue(world.match().clock().justReached(119));
        assertTrue(airDrop.flights().isEmpty());
        // 끝난 경기의 시계는 멈춰 있어서 그 뒤로도 나오지 않는다
        run(world, airDrop, 60 * 60);
        assertTrue(airDrop.flights().isEmpty());
    }

    @Test
    void 비행기는_맵_바깥에서_들어와_반대쪽_바깥까지_날면_사라진다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(new FlightPlan(Direction.LEFT, List.of()));
        Flight flight = airDrop.flights().get(0);
        assertEquals(world.map().pixelWidth() + Flight.OFFSTAGE, flight.x());

        run(world, airDrop, 1);
        assertEquals(world.map().pixelWidth() + Flight.OFFSTAGE - Flight.SPEED * TICK, flight.x(), 1e-9);

        // 600 + 80 x 2 = 760px를 초당 220px로 가면 3.45초. 한 틱에 3.67px이라 208번째 틱에 끝에 닿는다
        run(world, airDrop, 206);
        assertFalse(flight.isGone());
        run(world, airDrop, 1);
        assertTrue(flight.isGone());
        assertEquals(-Flight.OFFSTAGE, flight.x());
        assertTrue(airDrop.flights().isEmpty());
    }

    @Test
    void 고른_칸의_열_가운데를_지나는_순간_떨어뜨린다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 6, ItemType.SHOE)));

        flyUntilJustBefore(world, airDrop, 7);
        assertTrue(airDrop.fallingItems().isEmpty());

        run(world, airDrop, 1);
        assertEquals(List.of(new Drop(7, 6, ItemType.SHOE)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());
    }

    @Test
    void 오른쪽에서_올_때는_오른쪽_칸부터_떨어뜨린다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(new FlightPlan(Direction.LEFT,
                List.of(new Drop(2, 3, ItemType.BUBBLE), new Drop(11, 8, ItemType.FLUID))));

        flyUntilJustBefore(world, airDrop, 11);
        run(world, airDrop, 1);
        assertEquals(List.of(new Drop(11, 8, ItemType.FLUID)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());

        flyUntilJustBefore(world, airDrop, 2);
        assertTrue(airDrop.fallingItems().isEmpty(), "물약 병은 이미 칸에 닿았다");
        run(world, airDrop, 1);
        assertEquals(List.of(new Drop(2, 3, ItemType.BUBBLE)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());
    }

    @Test
    void 떨어진_아이템은_칸에_닿는_순간_맵에_놓인다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 12, ItemType.FLUID)));
        flyUntilJustBefore(world, airDrop, 7);
        run(world, airDrop, 1);

        FallingItem item = airDrop.fallingItems().get(0);
        // 맨 아랫줄 가운데(520 - 20)까지 비행기 높이 46에서 떨어진다
        assertEquals(500 - Flight.CRUISE_Y, item.startHeight());
        while (!item.hasLanded()) {
            assertEquals(Optional.empty(), world.items().at(7, 12));
            run(world, airDrop, 1);
        }

        assertEquals(Optional.of(ItemType.FLUID), world.items().at(7, 12).map(i -> i.type()));
        assertTrue(airDrop.fallingItems().isEmpty());
    }

    @Test
    void 맨_아랫줄까지_떨어지는_데_0_6초쯤_걸린다() {
        FallingItem item = new FallingItem(new Drop(0, 12, ItemType.BUBBLE), 500 - Flight.CRUISE_Y);
        int ticks = 0;
        while (!item.hasLanded()) {
            item.advance(TICK);
            ticks++;
        }
        // 루트(2 x 454 / 2400) = 0.615초
        assertEquals(37, ticks);
    }

    @Test
    void 비행기_배보다_위인_칸도_조금은_떨어진다() {
        FallingItem item = new FallingItem(new Drop(0, 0, ItemType.BUBBLE), 20 - Flight.CRUISE_Y);

        assertEquals(FallingItem.MIN_HEIGHT, item.startHeight());
        assertFalse(item.hasLanded());
    }

    @Test
    void 블럭_캐릭터_물풍선_아이템이_있는_칸에는_떨어뜨리지_않는다() {
        // (3, 1) 블럭, (4, 1) 부서지지 않는 석상, (5, 1) 덤불
        GameWorld world = world(map("...............", "...B#T........."), 1);
        world.items().place(6, 1, ItemType.GLOVE);
        Player me = world.localPlayer();

        assertFalse(AirDrop.canDropAt(world, 3, 1), "블럭");
        assertFalse(AirDrop.canDropAt(world, 4, 1), "부서지지 않는 블럭");
        assertTrue(AirDrop.canDropAt(world, 5, 1), "덤불은 지나갈 수 있는 칸이다");
        assertFalse(AirDrop.canDropAt(world, 6, 1), "이미 놓인 아이템");
        assertFalse(AirDrop.canDropAt(world, me.col(), me.row()), "캐릭터");
        assertTrue(AirDrop.canDropAt(world, 7, 7), "빈 바닥");

        // 물풍선을 놓고 위로 비켜선다
        world.update(TICK, Map.of(me.id(), new PlayerCommand(null, true, false)));
        for (int i = 0; i < 20; i++) {
            world.update(TICK, Map.of(me.id(), PlayerCommand.moving(Direction.UP)));
        }
        assertEquals(11, me.row());
        assertFalse(AirDrop.canDropAt(world, 0, 12), "물풍선");
        assertTrue(world.balloonField().balloonAt(0, 12).isPresent());
    }

    @Test
    void 맵에서_사라진_플레이어의_칸에는_떨어뜨린다() {
        GameWorld world = world(map(), 2);
        Player second = world.players().get(1);
        second.eliminate();
        assertFalse(AirDrop.canDropAt(world, second.col(), second.row()), "쓰러지는 연출 중에는 아직 맵에 있다");

        for (int i = 0; i < 2 * 60 + 1; i++) {
            world.update(TICK, Map.of());
        }

        assertEquals(PlayerState.GONE, second.state());
        assertTrue(AirDrop.canDropAt(world, second.col(), second.row()));
    }

    @Test
    void 막힌_칸은_건너뛰고_빈_칸에만_떨어뜨린다() {
        GameWorld world = world(map("...............", "...B.T........."), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(3, 1, ItemType.BUBBLE), new Drop(5, 1, ItemType.SHOE)));

        run(world, airDrop, 5 * 60);

        assertTrue(airDrop.flights().isEmpty());
        assertEquals(List.of(new Drop(5, 1, ItemType.SHOE)), world.items().all().stream()
                .map(item -> new Drop(item.col(), item.row(), item.type())).toList());
    }

    @Test
    void 떨어지는_동안_밀린_블럭이_들어와_칸이_막히면_놓지_않는다() {
        GameWorld world = world(map("...............", ".......P......."), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 9, ItemType.BUBBLE)));
        flyUntilJustBefore(world, airDrop, 7);
        run(world, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        world.map().moveBlock(7, 1, 7, 9);
        run(world, airDrop, 60);

        assertTrue(airDrop.fallingItems().isEmpty());
        assertTrue(world.items().all().isEmpty());
    }

    @Test
    void 떨어지는_동안_칸에_다른_아이템이_놓이면_그_아이템을_바꾸지_않는다() {
        GameWorld world = world(map(), 1);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 9, ItemType.BUBBLE)));
        flyUntilJustBefore(world, airDrop, 7);
        run(world, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        world.items().place(7, 9, ItemType.ROLLER_SHOE);
        run(world, airDrop, 60);

        assertTrue(airDrop.fallingItems().isEmpty());
        assertEquals(Optional.of(ItemType.ROLLER_SHOE), world.items().at(7, 9).map(i -> i.type()));
    }

    @Test
    void 날다가_경기가_끝나면_더_떨어뜨리지_않고_마저_지나간다() {
        GameWorld world = world(map(), 2);
        AirDrop airDrop = new AirDrop(world.map(), ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(2, 5, ItemType.BUBBLE), new Drop(12, 5, ItemType.SHOE)));
        flyUntilJustBefore(world, airDrop, 2);
        run(world, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        world.players().get(1).eliminate();
        run(world, airDrop, 1);
        assertTrue(world.match().isOver());
        run(world, airDrop, 4 * 60);

        assertTrue(airDrop.flights().isEmpty());
        // 끝나기 전에 떨어뜨린 물풍선만 놓였다
        assertEquals(List.of(ItemType.BUBBLE), world.items().all().stream().map(i -> i.type()).toList());
    }

    @Test
    void 나올_때마다_난수로_서로_다른_두_칸과_떨어뜨릴_아이템을_고른다() {
        TileMap map = map();
        // 칸 0번(0, 0), 아이템 0번(물풍선), 남은 칸 중 0번 → 이미 고른 0번을 건너뛰어 (1, 0), 아이템 2번(물약 병)
        FlightPlan first = new AirDrop(map, ScriptedRandom.of(0.0, 0.0, 0.0, 0.99)).plan();
        assertEquals(List.of(new Drop(0, 0, ItemType.BUBBLE), new Drop(1, 0, ItemType.FLUID)), first.drops());

        // 같은 난수가 나와도 두 번째는 다른 칸이다: 195칸의 97번 (7, 6), 194칸의 97번 → 98번 (8, 6)
        FlightPlan same = new AirDrop(map, ScriptedRandom.of(0.5)).plan();
        assertEquals(List.of(new Drop(7, 6, ItemType.SHOE), new Drop(8, 6, ItemType.SHOE)), same.drops());

        // 가장 큰 난수로는 마지막 칸들을 고른다
        FlightPlan last = new AirDrop(map, ScriptedRandom.of(0.9999)).plan();
        assertEquals(List.of(new Drop(14, 12, ItemType.FLUID), new Drop(13, 12, ItemType.FLUID)), last.drops());
    }

    @Test
    void 많이_고르면_모든_칸과_세_가지_아이템이_고르게_나온다() {
        TileMap map = map();
        AirDrop airDrop = new AirDrop(map, new SplittableRandom(20261115));
        Set<List<Integer>> cells = new HashSet<>();
        Set<ItemType> items = EnumSet.noneOf(ItemType.class);
        for (int i = 0; i < 5000; i++) {
            List<Drop> drops = airDrop.plan().drops();
            assertEquals(2, drops.size());
            assertNotEquals(List.of(drops.get(0).col(), drops.get(0).row()), List.of(drops.get(1).col(), drops.get(1).row()));
            for (Drop drop : drops) {
                assertTrue(map.isInside(drop.col(), drop.row()));
                cells.add(List.of(drop.col(), drop.row()));
                items.add(drop.item());
            }
        }

        assertEquals(COLUMNS * ROWS, cells.size());
        assertEquals(EnumSet.of(ItemType.BUBBLE, ItemType.SHOE, ItemType.FLUID), items);
    }

    @Test
    void 계획은_가로_방향_서로_다른_칸_두_개까지만_받는다() {
        Drop a = new Drop(1, 1, ItemType.BUBBLE);
        Drop b = new Drop(2, 1, ItemType.SHOE);

        assertThrows(IllegalArgumentException.class, () -> new FlightPlan(Direction.UP, List.of(a)));
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(Direction.RIGHT, List.of(a, b, new Drop(3, 1, ItemType.FLUID))));
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(Direction.RIGHT, List.of(a, new Drop(1, 1, ItemType.SHOE))));
    }
}
