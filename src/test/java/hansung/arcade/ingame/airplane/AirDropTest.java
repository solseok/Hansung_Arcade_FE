package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.CellOccupancy;
import hansung.arcade.ingame.item.CellOccupancy.Occupant;
import hansung.arcade.ingame.item.FieldItems;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.item.ScriptedRandom;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.Match;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
 * <p>게임 상태 없이, 비행기가 보는 창구({@link DropZone})를 경기·물풍선·바닥 아이템·플레이어로 직접 만들어 넘긴다.
 * 맵은 15 x 13칸이고 1P는 (0, 12), 2P는 (14, 12)에서 시작한다.</p>
 */
class AirDropTest {

    private static final double TICK = 1.0 / 60;
    private static final int COLUMNS = 15;
    private static final int ROWS = 13;

    /** 15 x 13 맵. rows에 준 줄로 위쪽부터 채우고 나머지는 빈 바닥이며, 1P·2P는 맨 아랫줄 양 끝에 선다 */
    private static TileMap map(String... rows) {
        List<String> lines = new ArrayList<>(List.of(rows));
        while (lines.size() < ROWS - 1) {
            lines.add(".".repeat(COLUMNS));
        }
        lines.add("1" + ".".repeat(COLUMNS - 2) + "2");
        return MapLoader.parse("fixture/airdrop", lines);
    }

    /** 비행기가 보는 게임. 경기 시계는 실제 경기로 흘리고, 칸에 무엇이 있는지는 실제 물풍선·아이템·플레이어로 본다 */
    private static final class Field implements DropZone {

        final TileMap map;
        final List<Player> players = new ArrayList<>();
        final BalloonField balloons;
        final FieldItems items = new FieldItems();
        final Match match;
        private final Map<Integer, PlayerGear> gears = new HashMap<>();
        private final CellOccupancy occupancy;

        /** map의 스폰에 플레이어 count명을 세운 개인전 */
        Field(TileMap map, int count) {
            this.map = map;
            for (int id = 1; id <= count; id++) {
                Point spawn = map.spawnPoints().get(id - 1);
                players.add(new Player(id, new PlayerProfile("p" + id, Mascot.KKUKKU), spawn.x, spawn.y));
                gears.put(id, new PlayerGear());
            }
            this.balloons = new BalloonField(map);
            this.occupancy = new CellOccupancy(balloons, items, players, player -> gears.get(player.id()));
            this.match = new Match(MatchMode.SOLO, players);
        }

        Player player(int id) {
            return players.get(id - 1);
        }

        @Override
        public MatchClock clock() {
            return match.clock();
        }

        @Override
        public boolean isMatchOver() {
            return match.isOver();
        }

        @Override
        public boolean holdsAny(int col, int row, Set<Occupant> occupants) {
            return occupancy.hasAny(col, row, occupants);
        }

        @Override
        public void placeItem(int col, int row, ItemType item) {
            items.place(col, row, item);
        }

        /** 바닥에 놓인 아이템들을 떨어뜨린 칸·아이템으로 */
        List<Drop> placed() {
            return items.all().stream().map(item -> new Drop(item.col(), item.row(), item.type())).toList();
        }
    }

    /** 게임 상태처럼 경기 시계를 먼저 흘린 뒤 비행기를 진행하기를 n번 한다 */
    private static void run(Field field, AirDrop airDrop, int ticks) {
        for (int i = 0; i < ticks; i++) {
            field.match.update(TICK);
            airDrop.update(TICK, field);
        }
    }

    /** 비행기가 떨어뜨리는 자리가 col 열 가운데에 닿기 직전까지 날린다 */
    private static void flyUntilJustBefore(Field field, AirDrop airDrop, int col) {
        Flight flight = airDrop.flights().get(0);
        double target = TileMap.centerX(col);
        while (Math.abs(target - flight.x()) > Flight.SPEED * TICK) {
            run(field, airDrop, 1);
        }
    }

    private static FlightPlan eastward(Drop... drops) {
        return new FlightPlan(Direction.RIGHT, List.of(drops));
    }

    @Test
    void 남은_시간이_1분59초_1분36초_1분14초_51초_29초_7초가_되는_순간_나온다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        List<Integer> appearedAt = new ArrayList<>();
        List<Direction> headings = new ArrayList<>();

        while (!field.match.isOver()) {
            run(field, airDrop, 1);
            for (Flight flight : airDrop.flights()) {
                if (flight.age() == TICK) {
                    appearedAt.add(field.match.clock().remainingSeconds());
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
    void 남은_시간이_120초일_때는_아직_나오지_않는다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));

        run(field, airDrop, 60 * 60);
        assertEquals(120, field.match.clock().remainingSeconds());
        assertTrue(airDrop.flights().isEmpty());

        run(field, airDrop, 60);
        assertEquals(119, field.match.clock().remainingSeconds());
        assertEquals(1, airDrop.flights().size());
    }

    @Test
    void 남은_시간이_출현_시각이_되는_틱에_경기가_끝나면_나오지_않는다() {
        Field field = new Field(map(), 2);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        run(field, airDrop, 61 * 60 - 1);
        assertEquals(120, field.match.clock().remainingSeconds());

        // 1:59가 되는 바로 그 틱에 마지막 상대가 탈락해 경기가 끝난다
        field.player(2).eliminate();
        run(field, airDrop, 1);

        assertTrue(field.match.isOver());
        assertTrue(field.match.clock().justReached(119));
        assertTrue(airDrop.flights().isEmpty());
        // 끝난 경기의 시계는 멈춰 있어서 그 뒤로도 나오지 않는다
        run(field, airDrop, 60 * 60);
        assertTrue(airDrop.flights().isEmpty());
    }

    @Test
    void 비행기는_맵_바깥에서_들어와_반대쪽_바깥까지_날면_사라진다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(new FlightPlan(Direction.LEFT, List.of()));
        Flight flight = airDrop.flights().get(0);
        assertEquals(field.map.pixelWidth() + Flight.OFFSTAGE, flight.x());

        run(field, airDrop, 1);
        assertEquals(field.map.pixelWidth() + Flight.OFFSTAGE - Flight.SPEED * TICK, flight.x(), 1e-9);

        // 600 + 80 x 2 = 760px를 초당 220px로 가면 3.45초. 한 틱에 3.67px이라 208번째 틱에 끝에 닿는다
        run(field, airDrop, 206);
        assertFalse(flight.isGone());
        run(field, airDrop, 1);
        assertTrue(flight.isGone());
        assertEquals(-Flight.OFFSTAGE, flight.x());
        assertTrue(airDrop.flights().isEmpty());
    }

    @Test
    void 고른_칸의_열_가운데를_지나는_순간_떨어뜨린다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 6, ItemType.SHOE)));

        flyUntilJustBefore(field, airDrop, 7);
        assertTrue(airDrop.fallingItems().isEmpty());

        run(field, airDrop, 1);
        assertEquals(List.of(new Drop(7, 6, ItemType.SHOE)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());
    }

    @Test
    void 오른쪽에서_올_때는_오른쪽_칸부터_떨어뜨린다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(new FlightPlan(Direction.LEFT,
                List.of(new Drop(2, 3, ItemType.BUBBLE), new Drop(11, 8, ItemType.FLUID))));

        flyUntilJustBefore(field, airDrop, 11);
        run(field, airDrop, 1);
        assertEquals(List.of(new Drop(11, 8, ItemType.FLUID)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());

        flyUntilJustBefore(field, airDrop, 2);
        assertTrue(airDrop.fallingItems().isEmpty(), "물약 병은 이미 칸에 닿았다");
        run(field, airDrop, 1);
        assertEquals(List.of(new Drop(2, 3, ItemType.BUBBLE)),
                airDrop.fallingItems().stream().map(FallingItem::drop).toList());
    }

    @Test
    void 띄운_비행기와_떨어뜨린_아이템_수는_사라진_뒤에도_줄지_않는다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        assertEquals(0, airDrop.launches());
        assertEquals(0, airDrop.releases());

        airDrop.launch(eastward(new Drop(3, 5, ItemType.SHOE), new Drop(9, 6, ItemType.BUBBLE)));
        while (!airDrop.flights().isEmpty() || !airDrop.fallingItems().isEmpty()) {
            run(field, airDrop, 1);
        }

        assertEquals(1, airDrop.launches());
        assertEquals(2, airDrop.releases());
        assertEquals(2, field.placed().size());
    }

    @Test
    void 떨어진_아이템은_칸에_닿는_순간_맵에_놓인다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 12, ItemType.FLUID)));
        flyUntilJustBefore(field, airDrop, 7);
        run(field, airDrop, 1);

        FallingItem item = airDrop.fallingItems().get(0);
        // 맨 아랫줄 가운데(520 - 20)까지 비행기 높이 46에서 떨어진다
        assertEquals(500 - Flight.CRUISE_Y, item.startHeight());
        while (!item.hasLanded()) {
            assertEquals(Optional.empty(), field.items.at(7, 12));
            run(field, airDrop, 1);
        }

        assertEquals(Optional.of(ItemType.FLUID), field.items.at(7, 12).map(i -> i.type()));
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
        Field field = new Field(map("...............", "...B#T........."), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        field.items.place(6, 1, ItemType.GLOVE);
        Player me = field.player(1);

        assertFalse(airDrop.canDropAt(field, 3, 1), "블럭");
        assertFalse(airDrop.canDropAt(field, 4, 1), "부서지지 않는 블럭");
        assertTrue(airDrop.canDropAt(field, 5, 1), "덤불은 지나갈 수 있는 칸이다");
        assertFalse(airDrop.canDropAt(field, 6, 1), "이미 놓인 아이템");
        assertFalse(airDrop.canDropAt(field, me.col(), me.row()), "캐릭터");
        assertTrue(airDrop.canDropAt(field, 7, 7), "빈 바닥");
        assertFalse(airDrop.canDropAt(field, 15, 7), "맵 밖");

        // 물풍선을 놓고 위 칸으로 비켜선다
        field.balloons.place(me, field.players);
        me.placeAt(TileMap.centerX(0), TileMap.centerY(11));
        assertFalse(me.overlapsTile(0, 12));
        assertFalse(airDrop.canDropAt(field, 0, 12), "물풍선");
        assertTrue(field.balloons.balloonAt(0, 12).isPresent());
    }

    @Test
    void 던진_물풍선이_내려앉을_칸에는_떨어뜨리지_않는다() {
        Field field = new Field(map(), 2);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        // 2P가 제자리 (14, 12)에 놓은 물풍선을 왼쪽으로 던지면 6칸 앞 (8, 12)에 내려앉는다
        WaterBalloon balloon = field.balloons.place(field.player(2), field.players).orElseThrow();
        assertTrue(field.balloons.throwBalloon(balloon, Direction.LEFT));
        assertTrue(balloon.isFlying());

        assertFalse(airDrop.canDropAt(field, 8, 12));
        assertTrue(airDrop.canDropAt(field, 11, 12), "날아가는 길에는 떨어뜨린다");
    }

    @Test
    void 캐릭터는_몸_가운데_칸뿐_아니라_몸이_걸친_칸에도_떨어뜨리지_않는다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        // 몸 가운데는 (3, 5)에 있고 오른쪽으로 (4, 5)에도 걸친다
        field.player(1).placeAt(TileMap.centerX(3) + 10, TileMap.centerY(5));

        assertFalse(airDrop.canDropAt(field, 3, 5));
        assertFalse(airDrop.canDropAt(field, 4, 5));
        assertTrue(airDrop.canDropAt(field, 2, 5));
        assertTrue(airDrop.canDropAt(field, 5, 5));
    }

    @Test
    void 맵에서_사라진_플레이어의_칸에는_떨어뜨린다() {
        Field field = new Field(map(), 2);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        Player second = field.player(2);
        second.eliminate();
        assertFalse(airDrop.canDropAt(field, second.col(), second.row()), "쓰러지는 연출 중에는 아직 맵에 있다");

        second.update(null, Player.DYING_JUMP_SECONDS + Player.DYING_BLINK_SECONDS, field.map);

        assertEquals(PlayerState.GONE, second.state());
        assertTrue(airDrop.canDropAt(field, second.col(), second.row()));
    }

    @Test
    void 막힌_칸은_건너뛰고_빈_칸에만_떨어뜨린다() {
        Field field = new Field(map("...............", "...B.T........."), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(3, 1, ItemType.BUBBLE), new Drop(5, 1, ItemType.SHOE)));

        run(field, airDrop, 5 * 60);

        assertTrue(airDrop.flights().isEmpty());
        assertEquals(List.of(new Drop(5, 1, ItemType.SHOE)), field.placed());
    }

    @Test
    void 떨어지는_동안_밀린_블럭이_들어와_칸이_막히면_놓지_않는다() {
        Field field = new Field(map("...............", ".......P......."), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 9, ItemType.BUBBLE)));
        flyUntilJustBefore(field, airDrop, 7);
        run(field, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        field.map.moveBlock(7, 1, 7, 9);
        run(field, airDrop, 60);

        assertTrue(airDrop.fallingItems().isEmpty());
        assertTrue(field.items.all().isEmpty());
    }

    @Test
    void 떨어지는_동안_칸에_다른_아이템이_놓이면_그_아이템을_바꾸지_않는다() {
        Field field = new Field(map(), 1);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(7, 9, ItemType.BUBBLE)));
        flyUntilJustBefore(field, airDrop, 7);
        run(field, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        field.items.place(7, 9, ItemType.ROLLER_SHOE);
        run(field, airDrop, 60);

        assertTrue(airDrop.fallingItems().isEmpty());
        assertEquals(Optional.of(ItemType.ROLLER_SHOE), field.items.at(7, 9).map(i -> i.type()));
    }

    @Test
    void 날다가_경기가_끝나면_더_떨어뜨리지_않고_마저_지나간다() {
        Field field = new Field(map(), 2);
        AirDrop airDrop = new AirDrop(field.map, ScriptedRandom.of(0.5));
        airDrop.launch(eastward(new Drop(2, 5, ItemType.BUBBLE), new Drop(12, 5, ItemType.SHOE)));
        flyUntilJustBefore(field, airDrop, 2);
        run(field, airDrop, 1);
        assertEquals(1, airDrop.fallingItems().size());

        field.player(2).eliminate();
        run(field, airDrop, 1);
        assertTrue(field.match.isOver());
        run(field, airDrop, 4 * 60);

        assertTrue(airDrop.flights().isEmpty());
        // 끝나기 전에 떨어뜨린 물풍선만 놓였다
        assertEquals(List.of(new Drop(2, 5, ItemType.BUBBLE)), field.placed());
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
