package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 신발로 민 물풍선이 미끄러지고, 장갑으로 던진 물풍선이 날아가 내려앉는 규칙 */
class BalloonFieldMoveTest {

    private static final double TICK = 1.0 / 60;

    private static Player playerAt(int id, int col, int row) {
        return new Player(id, new PlayerProfile("P" + id, Mascot.SANGSANGBUGI), col, row);
    }

    private static void ticks(BalloonField field, List<Player> players, int count) {
        for (int i = 0; i < count; i++) {
            field.update(TICK, players);
        }
    }

    /** row줄 한 줄짜리 맵에 col칸마다 플레이어를 세워 물풍선을 놓는다. 놓은 뒤 플레이어는 목록에서 뺀다 */
    private static WaterBalloon placeAt(BalloonField field, int id, int col) {
        Player placer = playerAt(id, col, 0);
        return field.place(placer, List.of(placer)).orElseThrow();
    }

    @Test
    void 밀린_물풍선은_통과할_수_없는_블럭_바로_앞까지_미끄러진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.....B")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        assertTrue(field.push(balloon, Direction.RIGHT));
        assertTrue(balloon.isSliding());
        // 초당 10칸이라 네 칸은 0.4초
        ticks(field, List.of(), 30);

        assertTrue(balloon.isResting());
        assertEquals(5, balloon.col());
        assertEquals(TileMap.centerX(5), balloon.x(), 1e-9);
    }

    @Test
    void 맵_끝도_막힌_칸이라_끝_칸에_멈춘다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1....")));
        WaterBalloon balloon = placeAt(field, 1, 2);

        field.push(balloon, Direction.LEFT);
        ticks(field, List.of(), 30);

        assertEquals(0, balloon.col());
        assertTrue(balloon.isResting());
    }

    @Test
    void 미끄러지는_물풍선은_다른_물풍선과_캐릭터와_덤불을_지나간다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.T......B")));
        WaterBalloon pushed = placeAt(field, 1, 1);
        WaterBalloon other = placeAt(field, 2, 4);
        Player standing = playerAt(3, 8, 0);

        field.push(pushed, Direction.RIGHT);
        ticks(field, List.of(standing), 60);

        assertEquals(8, pushed.col());
        assertTrue(pushed.isResting());
        assertEquals(4, other.col());
        // 물풍선이 멈춘 칸에 서 있던 사람은 놓는 순간처럼 그 칸을 벗어날 때까지 지나갈 수 있다
        assertFalse(field.blocks(standing, 8, 0));
    }

    @Test
    void 멈출_칸에_다른_물풍선이_있으면_그_앞의_빈_칸에_멈춘다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1....B")));
        WaterBalloon pushed = placeAt(field, 1, 1);
        placeAt(field, 2, 4);

        field.push(pushed, Direction.RIGHT);
        ticks(field, List.of(), 30);

        assertEquals(3, pushed.col());
    }

    @Test
    void 바로_앞이_막혀_있으면_밀리지_않는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.B..")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        assertFalse(field.push(balloon, Direction.RIGHT));
        assertTrue(balloon.isResting());
        assertEquals(1, balloon.col());
    }

    @Test
    void 가시_난_면에_닿아_멈추면_그_자리에서_바로_터진다() {
        // < 는 왼쪽 면에 가시가 난 블럭. 오른쪽으로 밀려 온 물풍선이 그 면에 닿는다
        BalloonField field = new BalloonField(MapLoader.parse("fixture/thorn", List.of("1....<")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        field.push(balloon, Direction.RIGHT);
        ticks(field, List.of(), 30);

        assertTrue(field.balloons().isEmpty());
        Explosion explosion = field.explosions().get(0);
        assertEquals(4, explosion.col());
    }

    @Test
    void 가시가_없는_면에_닿으면_보통_블럭처럼_멈춘다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/thorn", List.of("1....>")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        field.push(balloon, Direction.RIGHT);
        ticks(field, List.of(), 30);

        assertEquals(4, balloon.col());
        assertTrue(field.explosions().isEmpty());
    }

    @Test
    void 가시_블럭_바로_앞에서_가시_쪽으로_밀어도_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/thorn", List.of("1.<..")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        assertTrue(field.push(balloon, Direction.RIGHT));

        assertTrue(field.balloons().isEmpty());
        assertEquals(1, field.explosions().get(0).col());
    }

    @Test
    void 미끄러지는_동안에도_터질_시간은_그대로_흘러_지금_칸에서_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1..............")));
        WaterBalloon balloon = placeAt(field, 1, 1);
        ticks(field, List.of(), 108);

        field.push(balloon, Direction.RIGHT);
        // 남은 0.2초 동안 두 칸쯤 미끄러지다 터진다
        ticks(field, List.of(), 12);

        assertTrue(field.balloons().isEmpty());
        assertEquals(3, field.explosions().get(0).col());
    }

    @Test
    void 미끄러지다_물줄기가_남은_칸에_들어가면_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.........")));
        WaterBalloon pushed = placeAt(field, 1, 1);
        placeAt(field, 2, 6);
        ticks(field, List.of(), 60);
        // 6열 물풍선이 먼저 터져 5열까지 물줄기가 남은 채로 민다
        field.detonate(6, 0);

        field.push(pushed, Direction.RIGHT);
        // 5열에 들어서기까지 3.5칸 = 0.35초. 물줄기는 0.45초 남는다
        ticks(field, List.of(), 25);

        assertTrue(field.balloons().isEmpty());
        assertEquals(5, field.explosions().get(1).col());
    }

    @Test
    void 미끄러지다_바닥_가시_칸에_들어가면_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/spike", List.of("1...!....B")));
        WaterBalloon pushed = placeAt(field, 1, 1);

        field.push(pushed, Direction.RIGHT);
        ticks(field, List.of(), 30);

        assertTrue(field.balloons().isEmpty());
        assertEquals(4, field.explosions().get(0).col());
    }

    @Test
    void 던진_물풍선이_바닥_가시_칸에_내려앉으면_바로_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/spike", List.of("1......!...")));
        WaterBalloon thrown = placeAt(field, 1, 1);

        assertTrue(field.throwBalloon(thrown, Direction.RIGHT));
        ticks(field, List.of(), 10);
        // 나는 동안은 가시 위를 지나도 터지지 않는다
        assertTrue(field.explosions().isEmpty());
        ticks(field, List.of(), 20);

        assertTrue(field.balloons().isEmpty());
        assertEquals(7, field.explosions().get(0).col());
    }

    @Test
    void 던진_물풍선은_원래_칸을_0번째로_6칸째에_내려앉는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1..........")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        assertTrue(field.throwBalloon(balloon, Direction.RIGHT));
        assertTrue(balloon.isFlying());
        // 나는 동안은 어느 칸에도 없어서 길을 막지 않는다
        assertTrue(field.balloonAt(1, 0).isEmpty());
        assertTrue(field.balloonAt(7, 0).isEmpty());
        ticks(field, List.of(), 10);
        assertTrue(balloon.height() > 0);

        ticks(field, List.of(), 20);

        assertTrue(balloon.isResting());
        assertEquals(7, balloon.col());
        assertEquals(balloon, field.balloonAt(7, 0).orElseThrow());
    }

    @Test
    void 여섯째_칸이_블럭이나_물풍선이면_같은_방향의_다음_빈_칸까지_간다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1......B....")));
        WaterBalloon thrown = placeAt(field, 1, 0);
        placeAt(field, 2, 8);
        WaterBalloon second = placeAt(field, 3, 1);

        // 0열에서 6칸째인 6열은 빈 칸이라 그대로 6열. 1열에서 던지면 7열(블럭) → 8열(물풍선) → 9열
        assertEquals(new Point(6, 0), field.landingCell(0, 0, Direction.RIGHT, 6, thrown));
        assertEquals(new Point(9, 0), field.landingCell(1, 0, Direction.RIGHT, 6, second));
    }

    @Test
    void 캐릭터와_덤불이_있는_칸에도_내려앉는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1......T...")));
        WaterBalloon balloon = placeAt(field, 1, 1);
        Player standing = playerAt(2, 7, 0);

        assertEquals(new Point(7, 0), field.landingCell(1, 0, Direction.RIGHT, 6, balloon));
        field.throwBalloon(balloon, Direction.RIGHT);
        ticks(field, List.of(standing), 30);

        // 내려앉은 칸에 서 있던 사람은 그 칸을 벗어날 때까지 지나갈 수 있다
        assertEquals(7, balloon.col());
        assertFalse(field.blocks(standing, 7, 0));
    }

    @Test
    void 맵_끝까지_빈_칸이_없으면_맵_끝에서_거꾸로_돌아오며_처음_만난_빈_칸에_내려앉는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1...BB")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        // 7열은 맵 밖이고 4·5열은 블럭이라 3열
        assertEquals(new Point(3, 0), field.landingCell(1, 0, Direction.RIGHT, 6, balloon));
        // 왼쪽은 0열 하나뿐이다
        assertEquals(new Point(0, 0), field.landingCell(1, 0, Direction.LEFT, 6, balloon));
    }

    @Test
    void 내려앉을_곳이_제자리밖에_없으면_던지지_않는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1.BBB")));
        WaterBalloon balloon = placeAt(field, 1, 1);

        assertEquals(new Point(1, 0), field.landingCell(1, 0, Direction.RIGHT, 6, balloon));
        assertFalse(field.throwBalloon(balloon, Direction.RIGHT));
        assertTrue(balloon.isResting());
    }

    @Test
    void 날아가는_동안_터질_때가_되면_내려앉자마자_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1..........")));
        WaterBalloon balloon = placeAt(field, 1, 1);
        ticks(field, List.of(), 110);

        field.throwBalloon(balloon, Direction.RIGHT);
        // 6칸은 0.36초. 2초가 지나도 나는 중이면 터지지 않는다
        ticks(field, List.of(), 15);
        assertTrue(balloon.isFlying());
        assertTrue(field.explosions().isEmpty());

        ticks(field, List.of(), 10);
        assertTrue(field.balloons().isEmpty());
        assertEquals(7, field.explosions().get(0).col());
    }

    @Test
    void 나는_물풍선은_물줄기에_맞지_않는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1..........")));
        WaterBalloon thrown = placeAt(field, 1, 1);
        placeAt(field, 2, 4);
        ticks(field, List.of(), 60);

        field.throwBalloon(thrown, Direction.RIGHT);
        ticks(field, List.of(), 3);
        // 4열 물풍선이 터져 물줄기가 3열 위를 덮어도, 위로 날아가는 물풍선은 함께 터지지 않는다
        field.detonate(4, 0);

        assertTrue(thrown.isFlying());
        assertEquals(1, field.explosions().size());
    }

    @Test
    void 던진_물풍선이_내려앉을_칸은_미리_차지해서_그_칸에는_물풍선을_놓을_수_없다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1..........")));
        WaterBalloon thrown = placeAt(field, 1, 1);

        field.throwBalloon(thrown, Direction.RIGHT);
        ticks(field, List.of(), 5);
        Player waiting = playerAt(2, 7, 0);

        assertTrue(field.holds(7, 0));
        assertTrue(field.place(waiting, List.of(waiting)).isEmpty());
        ticks(field, List.of(waiting), 40);
        assertEquals(7, thrown.col());
        assertTrue(thrown.isResting());
    }

    @Test
    void 두_물풍선을_같은_칸으로_던져도_한_칸에_하나씩만_내려앉는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1..............")));
        WaterBalloon first = placeAt(field, 1, 1);
        WaterBalloon second = placeAt(field, 2, 13);

        field.throwBalloon(first, Direction.RIGHT);
        // 13열에서 왼쪽으로 6칸째도 7열이지만, 먼저 던진 물풍선이 내려앉을 칸이라 그다음 빈 칸인 6열
        field.throwBalloon(second, Direction.LEFT);
        ticks(field, List.of(), 30);

        assertEquals(7, first.col());
        assertEquals(6, second.col());
        assertTrue(first.isResting());
        assertTrue(second.isResting());
    }

    @Test
    void 미끄러지는_물풍선이_다른_물풍선_위를_지나다_터지면_겹친_물풍선도_함께_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.....#")));
        WaterBalloon pushed = placeAt(field, 1, 1);
        ticks(field, List.of(), 60);
        placeAt(field, 2, 2);
        ticks(field, List.of(), 54);

        // 남은 0.1초 동안 한 칸 미끄러져 2열 물풍선 위에서 터진다
        field.push(pushed, Direction.RIGHT);
        ticks(field, List.of(), 6);

        assertTrue(field.balloons().isEmpty());
        assertEquals(2, field.explosions().size());
    }

    @Test
    void 지나는_사이_앞이_막혀_다른_물풍선이_있는_칸에_멈추게_되면_둘_다_터진다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1....B")));
        WaterBalloon pushed = placeAt(field, 1, 1);
        placeAt(field, 2, 3);

        field.push(pushed, Direction.RIGHT);
        // 3열에 막 들어섰을 때 4열에 서 있던 사람이 물풍선을 놓는다. 멈출 곳이 3열밖에 없다
        ticks(field, List.of(), 9);
        assertEquals(3, pushed.col());
        placeAt(field, 3, 4);
        ticks(field, List.of(), 5);

        assertTrue(field.balloons().isEmpty());
        assertTrue(field.explosions().stream().anyMatch(explosion -> explosion.col() == 3));
    }

    @Test
    void 칸_가운데를_지난_사이_앞_칸에_상자가_밀려_들어오면_지금_칸_가운데에_멈춘다() {
        TileMap map = MapLoader.parse("fixture/slide", List.of("1....#", "...P.."));
        BalloonField field = new BalloonField(map);
        WaterBalloon pushed = placeAt(field, 1, 1);

        field.push(pushed, Direction.RIGHT);
        // 2열 가운데를 막 지났다. 다음에 들어갈 3열은 물풍선이 곧 차지할 칸이다
        ticks(field, List.of(), 7);
        assertEquals(2, pushed.col());
        assertTrue(field.holds(3, 0));
        // 그래도 상자가 들어왔다면(게임에서는 막는다) 물풍선은 상자를 지나가지 않는다
        map.moveBlock(3, 1, 3, 0);
        ticks(field, List.of(), 20);

        assertTrue(pushed.isResting());
        assertEquals(2, pushed.col());
        assertEquals(TileMap.centerX(2), pushed.x(), 1e-9);
    }

    // ---------------------------------------------------------------- 온라인: 서버가 놓은 물풍선

    /** row 0의 col칸에 서버가 놓은 물풍선 (스스로 터지지 않는다) */
    private static WaterBalloon serverBalloonAt(BalloonField field, int col) {
        return field.placeFromServer(9, col, 0, 1, List.of());
    }

    @Test
    void 서버_물풍선은_가시_난_면_앞에_멈춰도_스스로_터지지_않고_서버를_기다린다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/thorn", List.of("1....<")));
        WaterBalloon balloon = serverBalloonAt(field, 1);

        field.kickFromServer(balloon, 1, 0, Direction.RIGHT, List.of());
        ticks(field, List.of(), 30);

        assertTrue(balloon.isResting());
        assertEquals(4, balloon.col());
        assertTrue(field.explosions().isEmpty());
    }

    @Test
    void 서버_물풍선은_다른_물풍선이_있는_칸에_멈춰도_스스로_터지지_않는다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1....B")));
        WaterBalloon kicked = serverBalloonAt(field, 1);
        serverBalloonAt(field, 3);

        field.kickFromServer(kicked, 1, 0, Direction.RIGHT, List.of());
        ticks(field, List.of(), 9);
        serverBalloonAt(field, 4);
        ticks(field, List.of(), 5);

        assertEquals(3, field.balloons().size());
        assertTrue(kicked.isResting());
        assertEquals(3, kicked.col());
        assertTrue(field.explosions().isEmpty());
    }

    @Test
    void 서버_물풍선은_내려앉을_칸이_막혀도_더_날지_않고_그_칸에서_서버를_기다린다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1.........")));
        WaterBalloon thrown = serverBalloonAt(field, 1);
        field.throwFromServer(thrown, Direction.RIGHT, 7, 0, 0.36);
        serverBalloonAt(field, 7);

        ticks(field, List.of(), 30);

        assertTrue(thrown.isResting());
        assertEquals(7, thrown.col());
        assertTrue(field.explosions().isEmpty());
    }

    @Test
    void 던질_수_있는지는_가만히_놓였고_내려앉을_곳이_제자리가_아닌지로_본다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/throw", List.of("1.........")));
        WaterBalloon balloon = serverBalloonAt(field, 1);

        assertTrue(field.canThrow(balloon, Direction.RIGHT));
        assertTrue(field.canThrow(balloon, Direction.LEFT), "맵 끝에서 거꾸로 돌아와 0열에 내려앉는다");
        assertFalse(field.canThrow(balloon, Direction.UP), "한 줄짜리 맵이라 내려앉을 곳이 없다");
        field.kickFromServer(balloon, 1, 0, Direction.RIGHT, List.of());
        assertFalse(field.canThrow(balloon, Direction.RIGHT), "미끄러지는 중");
    }

    @Test
    void 찰_수_있는지는_미끄러질_칸이_있거나_가시_난_면을_보는지로_본다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/thorn", List.of("1.<.>.")));
        WaterBalloon nearThorn = serverBalloonAt(field, 1);
        WaterBalloon nearBack = serverBalloonAt(field, 3);

        assertTrue(field.canKick(nearThorn, Direction.RIGHT), "가시 난 면이라 그 자리에서 터진다");
        assertTrue(field.canKick(nearThorn, Direction.LEFT));
        assertFalse(field.canKick(nearBack, Direction.RIGHT), "가시가 없는 면 바로 앞");
        assertFalse(field.canKick(nearBack, Direction.UP));
        field.throwFromServer(nearThorn, Direction.LEFT, 0, 0, 0.2);
        assertFalse(field.canKick(nearThorn, Direction.LEFT), "나는 중");
    }

    @Test
    void 미끄러지는_물풍선은_앞이_비어_있어도_찰_수_없다() {
        BalloonField field = new BalloonField(MapLoader.parse("fixture/slide", List.of("1.........")));
        WaterBalloon balloon = serverBalloonAt(field, 3);
        assertTrue(field.canKick(balloon, Direction.RIGHT));

        field.kickFromServer(balloon, 3, 0, Direction.RIGHT, List.of());

        assertFalse(field.canKick(balloon, Direction.RIGHT));
    }
}
