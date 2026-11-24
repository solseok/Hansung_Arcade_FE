package hansung.arcade.ingame;

import hansung.arcade.ingame.ServerRules.Blast;
import hansung.arcade.ingame.ServerRules.Drop;
import hansung.arcade.ingame.ServerRules.Stats;
import hansung.arcade.ingame.ServerRules.TileChange;
import hansung.arcade.ingame.balloon.Explosion;
import hansung.arcade.ingame.balloon.WaterCell;
import hansung.arcade.ingame.balloon.WaterCell.Part;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.StartIntro;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 온라인 경기: 판정은 서버가 하고, 이 컴퓨터는 내 플레이어를 움직여 보내고 서버가 알린 대로 그린다 */
class ServerRulesTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);
    private static final int ME = 12;
    private static final int OTHER = 20;
    /** 1번 스폰은 (1,1), 2번 스폰은 (5,1). (3,0)은 블럭, (4,0)은 세 번 맞아야 부서지는 블럭 */
    private static final List<String> ROWS = List.of(
            "...Bc..",
            ".1...2.",
            ".......");

    /** 서버로 보낸 것을 한 줄씩 적어 둔다 */
    private static final class Outbox implements ServerOutbox {
        final List<String> sent = new ArrayList<>();
        /** 보낸 MOVE의 자리 (x, y) */
        final List<double[]> moves = new ArrayList<>();
        /** 보낸 MOVE의 방향·걷는 중인지 ("RIGHT false" 꼴) */
        final List<String> moveStates = new ArrayList<>();

        @Override
        public void move(int playerId, double x, double y, Direction facing, boolean moving) {
            sent.add("MOVE " + playerId);
            moves.add(new double[]{x, y});
            moveStates.add(facing + " " + moving);
        }

        @Override
        public void pushBox(int playerId, int col, int row, Direction direction) {
            sent.add("BOX_PUSH %d %d,%d %s".formatted(playerId, col, row, direction));
        }

        @Override
        public void placeBalloon(int playerId, int col, int row) {
            sent.add("BALLOON_PLACE %d %d,%d".formatted(playerId, col, row));
        }

        @Override
        public void pickUpItem(int playerId, int itemId) {
            sent.add("ITEM_PICKUP %d %d".formatted(playerId, itemId));
        }

        @Override
        public void useItem(int playerId, String item) {
            sent.add("ITEM_USE %d %s".formatted(playerId, item));
        }

        @Override
        public void chat(int playerId, String text) {
            sent.add("CHAT %d %s".formatted(playerId, text));
        }

        long count(String prefix) {
            return sent.stream().filter(line -> line.startsWith(prefix)).count();
        }
    }

    private final Outbox outbox = new Outbox();
    private final TileMap map = MapLoader.parse("fixture/online", ROWS);
    /** 나(12번)는 2번 스폰, 다른 컴퓨터의 플레이어(20번)는 1번 스폰에 선다 */
    private final GameWorld world = GameWorld.online(map, List.of(
            new OnlineSeat(ME, new PlayerProfile("부기", Mascot.SANGSANGBUGI, 1), 2, true),
            new OnlineSeat(OTHER, new PlayerProfile("냥이", Mascot.HANSUNGNYANGI, 2), 1, false)), outbox);
    private final ServerRules server = world.server().orElseThrow();
    private final Player me = world.localPlayers().getFirst();
    private final Player other = world.players().get(1);

    private void ticks(int count) {
        ticks(count, Map.of());
    }

    private void ticks(int count, Map<Integer, PlayerCommand> commands) {
        for (int i = 0; i < count; i++) {
            world.update(TICK, commands);
        }
    }

    /** 라운드를 열고 카운트다운(3초)이 끝날 때까지 흘린다 */
    private void play() {
        server.roundBegin(3_000, 180_000);
        ticks(181);
        assertTrue(server.isPlaying());
        outbox.sent.clear();
    }

    @Test
    void 서버가_정한_번호와_스폰_자리에_경기_인원_모두를_세운다() {
        assertEquals(List.of(ME, OTHER), world.players().stream().map(Player::id).toList());
        assertEquals(List.of(me), world.localPlayers());
        assertEquals(5, me.col());
        assertEquals(1, me.row());
        assertEquals(1, other.col());
        assertEquals(1, other.row());
    }

    @Test
    void 라운드가_열리기_전에는_검은_화면에서_움직이지_않고_아무것도_보내지_않는다() {
        double x = me.x();

        ticks(120, Map.of(ME, PlayerCommand.moving(Direction.LEFT)));

        assertEquals(x, me.x());
        assertEquals(1.0, world.intro().blackout());
        assertTrue(outbox.sent.isEmpty());
        assertFalse(world.takeSounds().contains(GameSound.READY), "Ready 소리는 연출을 시작할 때 낸다");
    }

    @Test
    void 카운트다운이_끝나는_순간_시작_연출도_끝나고_그때부터_움직이고_시계가_흐른다() {
        server.roundBegin(3_000, 180_000);
        int beforeIntro = (int) Math.round((3.0 - StartIntro.TOTAL_SECONDS) * 60) - 2;
        ticks(beforeIntro);
        assertFalse(world.takeSounds().contains(GameSound.READY));
        ticks(4);
        assertTrue(world.takeSounds().contains(GameSound.READY), "남은 시간이 연출 길이가 되면 연출을 시작한다");

        double x = me.x();
        ticks(170 - beforeIntro - 4, Map.of(ME, PlayerCommand.moving(Direction.LEFT)));
        assertEquals(x, me.x(), "카운트다운 중에는 움직이지 않는다");
        assertEquals(MatchClock.DURATION_SECONDS, world.match().clock().remainingSeconds());

        ticks(20, Map.of(ME, PlayerCommand.moving(Direction.LEFT)));
        assertTrue(me.x() < x, "카운트다운이 끝나면 움직인다");
        assertFalse(world.intro().isRunning());
        assertTrue(world.match().clock().elapsedSeconds() > 0, "경기 시계는 경기가 시작된 뒤부터 흐른다");
    }

    @Test
    void 내_자리는_초당_20번까지_바뀌었을_때만_MOVE로_보낸다() {
        play();
        ticks(60);
        assertTrue(outbox.count("MOVE") <= 1, "가만히 있으면 처음 한 번만 보낸다: " + outbox.sent);

        outbox.sent.clear();
        ticks(60, Map.of(ME, PlayerCommand.moving(Direction.LEFT)));
        assertTrue(outbox.count("MOVE " + ME) >= 19 && outbox.count("MOVE " + ME) <= 21, outbox.sent.toString());
    }

    @Test
    void 물풍선_키는_놓지_않고_지금_자리부터_보낸_뒤_놓아_달라고_보낸다() {
        play();

        ticks(1, Map.of(ME, BALLOON));

        assertEquals(List.of("MOVE " + ME, "BALLOON_PLACE " + ME + " 5,1"), outbox.sent.subList(0, 2));
        assertTrue(world.balloonField().balloons().isEmpty(), "서버가 받아들일 때까지 놓지 않는다");
    }

    @Test
    void 서버가_놓은_물풍선은_시간이_지나도_스스로_터지지_않고_알린_길이만큼_터진다() {
        play();
        server.balloonPlaced(1, OTHER, 2, 2, 3, 2_000);
        assertEquals(1, world.balloonField().balloons().size());
        assertTrue(world.takeSounds().contains(GameSound.BALLOON_PLACED));

        ticks(240);
        assertEquals(1, world.balloonField().balloons().size(), "언제 터지는지는 서버가 알린다");

        server.balloonExploded(List.of(new Blast(1, 2, 2, 1, 0, 2, 3)), List.of(), List.of(), List.of());
        ticks(1);

        assertTrue(world.balloonField().balloons().isEmpty());
        Explosion explosion = world.balloonField().explosions().getFirst();
        assertEquals(OTHER, explosion.ownerId());
        assertEquals(1 + 1 + 2 + 3, explosion.cells().size());
        assertTrue(explosion.cells().contains(new WaterCell(2, 1, Part.END, Direction.UP)));
        assertTrue(explosion.cells().contains(new WaterCell(1, 2, Part.MIDDLE, Direction.LEFT)));
        assertTrue(explosion.cells().contains(new WaterCell(0, 2, Part.END, Direction.LEFT)));
        assertTrue(explosion.cells().contains(new WaterCell(5, 2, Part.END, Direction.RIGHT)));
        assertTrue(world.takeSounds().contains(GameSound.EXPLODE));
    }

    @Test
    void 함께_터진_물풍선에_닿은_물줄기는_끝_모양_없이_이어진다() {
        play();
        server.balloonPlaced(1, OTHER, 1, 2, 2, 2_000);
        server.balloonPlaced(2, ME, 3, 2, 2, 2_000);

        server.balloonExploded(List.of(new Blast(1, 1, 2, 0, 0, 0, 1), new Blast(2, 3, 2, 0, 0, 1, 2)),
                List.of(), List.of(), List.of());

        Explosion first = world.balloonField().explosions().getFirst();
        assertTrue(first.cells().contains(new WaterCell(2, 2, Part.MIDDLE, Direction.RIGHT)));
        assertTrue(world.balloonField().explosions().get(1).cells()
                .contains(new WaterCell(2, 2, Part.MIDDLE, Direction.LEFT)));
    }

    @Test
    void 블럭은_서버가_알려_준_모습으로_바뀌고_나온_아이템이_놓이며_물줄기에_맞은_아이템은_사라진다() {
        play();
        server.itemsScattered(OTHER, 1, 1, List.of(new Drop(4, 6, 2, "shoe")));

        server.balloonExploded(List.of(),
                List.of(new TileChange(3, 0, "."), new TileChange(4, 0, "b")),
                List.of(new Drop(5, 3, 0, "bubble")), List.of(4));

        assertEquals(Tile.FLOOR, map.tileAt(3, 0));
        assertEquals(Tile.HARD_BLOCK_2, map.tileAt(4, 0));
        assertEquals(ItemType.BUBBLE, world.items().at(3, 0).orElseThrow().type());
        assertTrue(world.items().at(6, 2).isEmpty());
        assertTrue(world.takeSounds().contains(GameSound.ITEM_POP));
    }

    @Test
    void 아이템_칸에_들어서면_한_번만_줍겠다고_보내고_먹는_것은_서버를_따른다() {
        play();
        server.itemsScattered(OTHER, 1, 1, List.of(new Drop(7, me.col(), me.row(), "fluid")));

        ticks(30);

        assertEquals(1, outbox.count("ITEM_PICKUP " + ME + " 7"), outbox.sent.toString());
        int pickup = outbox.sent.indexOf("ITEM_PICKUP " + ME + " 7");
        assertEquals("MOVE " + ME, outbox.sent.get(pickup - 1), "서버는 마지막 MOVE 칸을 보므로 자리부터 보낸다");
        assertTrue(world.items().at(me.col(), me.row()).isPresent(), "서버가 받아들일 때까지 그대로 있다");

        server.itemTaken(7, ME, "fluid", new Stats(2, 3, 6), null, 0);

        assertTrue(world.items().all().isEmpty());
        assertEquals(2, me.balloons());
        assertEquals(3, me.waterRange());
        assertEquals(6, me.speedLevel());
        assertTrue(world.takeSounds().contains(GameSound.ITEM_EATEN));
    }

    @Test
    void 갇힘_구출_탈락은_서버가_알린_대로_하고_스스로_터뜨리지_않는다() {
        play();
        server.balloonPlaced(3, ME, 1, 2, 1, 2_000);
        server.balloonExploded(List.of(new Blast(3, 1, 2, 1, 0, 0, 0)), List.of(), List.of(), List.of());

        server.playerTrapped(OTHER, 3, 5_000);
        assertEquals(PlayerState.TRAPPED, other.state());
        assertEquals(ME, other.trapperId().orElseThrow());
        assertTrue(world.takeSounds().contains(GameSound.TRAPPED));

        ticks(6 * 60);
        assertEquals(PlayerState.TRAPPED, other.state(), "5초가 지나도 서버가 알릴 때까지 갇혀 있다");

        server.playerRescued(OTHER, ME);
        assertEquals(PlayerState.ESCAPING, other.state());
        assertEquals(1, world.match().saves(ME));

        server.playerTrapped(OTHER, 3, 5_000);
        assertEquals(PlayerState.TRAPPED, other.state(), "빠져나오는 중이어도 서버가 가두면 갇힌다");
        server.playerPopped(OTHER, (long) ME, "TOUCH");

        assertFalse(other.isAlive());
        assertEquals(PlayerState.DYING, other.state());
        assertEquals(1, world.match().kills(ME));
        assertTrue(world.takeSounds().contains(GameSound.DIE));
        assertFalse(world.match().isOver(), "승패는 서버의 GAME_OVER가 정한다");
    }

    @Test
    void 다른_플레이어는_받은_자리로_따라가고_멀리_떨어졌으면_바로_옮긴다() {
        play();
        double startX = other.x();

        server.playerMoved(OTHER, startX + 20, other.y(), "RIGHT", true);
        ticks(1);
        assertTrue(other.x() > startX && other.x() < startX + 20, "한 틱에 다 가지 않고 따라간다");
        assertEquals(Direction.RIGHT, other.facing());
        assertTrue(other.isMoving());

        ticks(30);
        assertEquals(startX + 20, other.x(), 1e-9);

        server.playerMoved(OTHER, startX + 5 * TileMap.TILE_SIZE, other.y(), "DOWN", false);
        ticks(1);
        assertEquals(startX + 5 * TileMap.TILE_SIZE, other.x(), 1e-9);
        assertFalse(other.isMoving());
    }

    @Test
    void 비행기는_서버가_알릴_때만_띄우고_떨어뜨린_아이템은_닿은_뒤에_놓는다() {
        play();
        ticks(62 * 60);
        assertEquals(0, world.airDrop().launches(), "온라인은 1:59가 돼도 스스로 띄우지 않는다");

        server.airplaneAppeared(1, "RIGHT");
        assertEquals(1, world.airDrop().flights().size());
        assertEquals(Direction.RIGHT, world.airDrop().flights().getFirst().heading());
        assertTrue(world.takeSounds().contains(GameSound.AIRPLANE));

        server.itemDropped(1, 9, 2, 2, "bubble", 200);
        assertEquals(1, world.airDrop().fallingItems().size());
        assertTrue(world.items().at(2, 2).isEmpty(), "떨어지는 동안은 아직 칸에 없다");
        assertTrue(world.takeSounds().contains(GameSound.AIR_DROP));

        ticks(60);
        assertTrue(world.airDrop().fallingItems().isEmpty());
        assertEquals(ItemType.BUBBLE, world.items().at(2, 2).orElseThrow().type());
        assertEquals(0, world.airDrop().launches() - 1, "떨어뜨린 아이템 때문에 비행기를 더 띄우지 않는다");
    }

    @Test
    void 떨어지는_사이_물줄기에_사라지거나_누가_먹은_아이템은_놓지_않는다() {
        play();
        server.itemDropped(1, 9, 2, 2, "bubble", 200);
        server.itemDropped(1, 10, 0, 2, "shoe", 200);

        server.balloonExploded(List.of(), List.of(), List.of(), List.of(9));
        server.itemTaken(10, OTHER, "shoe", new Stats(2, 1, 4), null, 0);
        ticks(60);

        assertTrue(world.airDrop().fallingItems().isEmpty());
        assertTrue(world.items().all().isEmpty());
    }

    @Test
    void 내_발밑에_떨어진_아이템은_닿은_뒤에_줍겠다고_보낸다() {
        play();
        server.itemDropped(1, 11, me.col(), me.row(), "fluid", 200);

        ticks(3);
        assertEquals(0, outbox.count("ITEM_PICKUP"), "떨어지는 동안은 보내지 않는다");
        ticks(60);
        assertEquals(1, outbox.count("ITEM_PICKUP " + ME + " 11"), outbox.sent.toString());
    }

    @Test
    void 내가_선_칸에_떨어진_아이템은_서버가_바로_먹이면_닿기_전에_지우고_줍기를_보내지_않는다() {
        play();
        // 서버는 사람이 서 있어도 떨어뜨리고, 닿는 순간(서버 시각) 그 칸에 선 사람에게 바로 먹인다 (서버 #24).
        // 이 컴퓨터는 받은 뒤부터 떨어뜨리므로 대개 닿기 전에 ITEM_TAKEN이 온다
        server.itemDropped(1, 12, me.col(), me.row(), "roller_shoe", 200);
        ticks(3);
        server.itemTaken(12, ME, "roller_shoe", new Stats(1, 1, 6), null, 0);
        ticks(60);

        assertTrue(world.airDrop().fallingItems().isEmpty());
        assertTrue(world.items().all().isEmpty());
        assertEquals(6, me.speedLevel());
        assertEquals(0, outbox.count("ITEM_PICKUP"), "서버가 이미 먹였으니 보내지 않는다: " + outbox.sent);
    }

    @Test
    void 손에_든_쓰는_아이템은_서버를_따르고_아이템_키는_쓰겠다고_보내기만_한다() {
        play();
        server.itemTaken(5, ME, "shield", new Stats(1, 1, 5), "shield", 2);
        assertEquals(ItemType.SHIELD, world.gear(me).activeItem().orElseThrow());
        assertEquals(2, world.gear(me).activeCount());

        ticks(1, Map.of(ME, new PlayerCommand(null, false, true)));

        assertEquals(List.of("MOVE " + ME, "ITEM_USE " + ME + " shield"), outbox.sent.subList(0, 2));
        assertFalse(world.gear(me).isShielded(), "서버가 받아들일 때까지 쓰지 않는다");
        assertEquals(2, world.gear(me).activeCount());

        server.itemUsed(ME, "shield", 3_000L, null, null, "shield", 1);
        assertTrue(world.gear(me).isShielded());
        assertEquals(1, world.gear(me).activeCount());
        assertTrue(world.takeSounds().contains(GameSound.SHIELD));
        ticks(3 * 60 + 5);
        assertFalse(world.gear(me).isShielded());
    }

    @Test
    void 손에_든_것이_없으면_아이템_키를_눌러도_보내지_않는다() {
        play();

        ticks(1, Map.of(ME, new PlayerCommand(null, false, true)));

        assertEquals(0, outbox.count("ITEM_USE"));
    }

    @Test
    void 바늘을_쓰면_갇힌_물풍선을_깨고_빠져나온다() {
        play();
        server.balloonPlaced(3, OTHER, 5, 2, 1, 2_000);
        server.balloonExploded(List.of(new Blast(3, 5, 2, 1, 0, 0, 0)), List.of(), List.of(), List.of());
        server.playerTrapped(ME, 3, 5_000);
        assertEquals(PlayerState.TRAPPED, me.state());

        server.itemUsed(ME, "needle", null, null, null, null, 0);

        assertEquals(PlayerState.ESCAPING, me.state());
        assertTrue(world.gear(me).activeItem().isEmpty());
    }

    @Test
    void 스프링_신발을_쓰면_서버가_정한_칸으로_뛰어_내려앉는다() {
        play();

        server.itemUsed(ME, "spring_shoe", 400L, 3, 1, null, 0);
        assertTrue(world.gear(me).isAirborne());
        assertTrue(world.takeSounds().contains(GameSound.SPRING));
        // 서버는 공중 시간이 끝날 때 마지막 MOVE 칸을 보므로 착지 자리를 바로 보내고, 뛰는 동안은 더 보내지 않는다
        assertEquals("MOVE " + ME, outbox.sent.getLast());
        assertEquals(TileMap.centerX(3), outbox.moves.getLast()[0], 1e-9);
        assertEquals(TileMap.centerY(1), outbox.moves.getLast()[1], 1e-9);
        outbox.sent.clear();
        ticks(10, Map.of(ME, new PlayerCommand(Direction.DOWN, true, false)));
        assertEquals(1, me.row(), "공중에서는 걷지 않는다");
        assertTrue(outbox.sent.isEmpty(), "뛰는 동안은 MOVE·BALLOON_PLACE를 보내지 않는다: " + outbox.sent);

        ticks(20);
        assertFalse(world.gear(me).isAirborne());
        assertEquals(TileMap.centerX(3), me.x(), 1e-9);
        assertEquals(TileMap.centerY(1), me.y(), 1e-9);
        assertTrue(outbox.sent.isEmpty(), "내려앉은 자리는 이미 보냈다: " + outbox.sent);

        // 다른 컴퓨터의 플레이어도 뛰는 동안은 받은 자리 대신 점프를 따라 움직이고, 내려앉은 뒤 뛰기 전 자리로 끌려가지 않는다
        server.itemUsed(OTHER, "spring_shoe", 400L, 3, 2, null, 0);
        ticks(12);
        assertTrue(world.gear(other).isAirborne());
        assertNotEquals(TileMap.centerX(1), other.x(), "점프가 자리를 옮긴다");
        ticks(30);
        assertEquals(TileMap.centerX(3), other.x(), 1e-9);
        assertEquals(TileMap.centerY(2), other.y(), 1e-9);
    }

    @Test
    void 터지거나_나간_사람은_손에_든_아이템이_비고_아이템_키도_보내지_않는다() {
        play();
        server.itemTaken(5, ME, "needle", new Stats(1, 1, 5), "needle", 3);
        server.itemTaken(6, OTHER, "shield", new Stats(2, 1, 4), "shield", 1);

        server.playerPopped(ME, null, "TIMEOUT");
        server.playersLeft(List.of((long) OTHER), "EXIT");
        ticks(1, Map.of(ME, new PlayerCommand(null, false, true)));

        assertTrue(world.gear(me).activeItem().isEmpty());
        assertTrue(world.gear(other).activeItem().isEmpty());
        assertEquals(0, outbox.count("ITEM_USE"));
    }

    /** 1번 스폰(1,1) 오른쪽 바로 옆 (2,1)에 밀리는 상자가 있고 그 너머 (3,1)은 바닥이다. 나(12번)는 1번 스폰에 선다 */
    private GameWorld boxWorld(Outbox box) {
        GameWorld boxes = GameWorld.online(MapLoader.parse("fixture/box", List.of(
                        ".......",
                        ".1P...2",
                        ".......")), List.of(
                new OnlineSeat(ME, new PlayerProfile("부기", Mascot.SANGSANGBUGI, 1), 1, true),
                new OnlineSeat(OTHER, new PlayerProfile("냥이", Mascot.HANSUNGNYANGI, 2), 2, false)), box);
        boxes.server().orElseThrow().roundBegin(0, 180_000);
        for (int i = 0; i < 130; i++) {
            boxes.update(TICK, Map.of());
        }
        box.sent.clear();
        box.moveStates.clear();
        return boxes;
    }

    @Test
    void 상자에_막혀_계속_밀면_옮기지_않고_멈춘_자리부터_보낸_뒤_밀어_달라고_한다() {
        Outbox box = new Outbox();
        GameWorld boxes = boxWorld(box);
        Map<Integer, PlayerCommand> right = Map.of(ME, PlayerCommand.moving(Direction.RIGHT));

        for (int i = 0; i < 18; i++) {
            boxes.update(TICK, right);
        }

        int push = box.sent.indexOf("BOX_PUSH " + ME + " 2,1 RIGHT");
        assertTrue(push > 0, box.sent.toString());
        assertEquals("MOVE " + ME, box.sent.get(push - 1), "서버는 마지막 MOVE가 멈춰 그 방향을 보는지 본다");
        assertEquals("RIGHT false", box.moveStates.getLast());
        assertEquals(Tile.PUSH_BOX, boxes.map().tileAt(2, 1), "서버가 알릴 때까지 옮기지 않는다");
        assertEquals(1, box.count("BOX_PUSH"));

        for (int i = 0; i < 16; i++) {
            boxes.update(TICK, right);
        }
        assertEquals(2, box.count("BOX_PUSH"), "안 밀렸으면 계속 누를 때 0.25초 뒤 다시 보낸다");
    }

    @Test
    void 서버가_민_상자는_바로_옮기고_미끄러지는_모습을_보인다() {
        Outbox box = new Outbox();
        GameWorld boxes = boxWorld(box);

        boxes.server().orElseThrow().boxMoved(2, 1, 3, 1);

        assertEquals(Tile.FLOOR, boxes.map().tileAt(2, 1));
        assertEquals(Tile.PUSH_BOX, boxes.map().tileAt(3, 1));
        assertEquals(1, boxes.boxPusher().slides().size());
        for (int i = 0; i < 15; i++) {
            boxes.update(TICK, Map.of());
        }
        assertTrue(boxes.boxPusher().slides().isEmpty());

        // 이 컴퓨터의 맵과 어긋난 알림(상자가 없는 칸)은 견디고 맵을 바꾸지 않는다
        boxes.server().orElseThrow().boxMoved(2, 1, 1, 1);
        assertEquals(Tile.FLOOR, boxes.map().tileAt(2, 1));
        assertEquals(Tile.PUSH_BOX, boxes.map().tileAt(3, 1));
    }

    @Test
    void 경기에서_나간_플레이어는_맵에서_바로_사라진다() {
        play();

        assertFalse(server.playersLeft(List.of((long) OTHER), "EXIT"), "다른 컴퓨터의 플레이어다");

        assertEquals(PlayerState.GONE, other.state());
        assertFalse(other.isAlive());
        assertTrue(server.playersLeft(List.of((long) ME), "DISCONNECT"), "이 컴퓨터의 플레이어가 빠졌다");
    }

    @Test
    void 채팅은_다듬어_서버로_보내고_서버가_돌려준_글만_올린다() {
        play();

        world.sendChat("  안녕  ");
        world.sendChat("   ");

        assertEquals(List.of("CHAT " + ME + " 안녕"), outbox.sent.stream().filter(line -> line.startsWith("CHAT")).toList());
        assertTrue(world.chatLog().messages().isEmpty());
    }

    @Test
    void 서버가_경기를_끝내면_더는_움직이지_않고_보내지_않는다() {
        play();
        server.gameOver();
        double x = me.x();
        int remaining = world.match().clock().remainingSeconds();

        ticks(120, Map.of(ME, PlayerCommand.moving(Direction.LEFT)));
        ticks(1, Map.of(ME, BALLOON));

        assertEquals(x, me.x());
        assertTrue(outbox.sent.isEmpty(), outbox.sent.toString());
        assertEquals(remaining, world.match().clock().remainingSeconds(), "시계가 멈춘다");
    }
}
