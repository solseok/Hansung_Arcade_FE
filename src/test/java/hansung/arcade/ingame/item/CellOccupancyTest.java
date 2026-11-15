package hansung.arcade.ingame.item;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.CellOccupancy.Occupant;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 9 x 2 빈 맵. 1P는 (0, 0), 2P는 (8, 0)에 선다 */
class CellOccupancyTest {

    private static final TileMap MAP = MapLoader.parse("fixture/occupancy", List.of("1........", "........."));

    private final BalloonField balloons = new BalloonField(MAP);
    private final FieldItems items = new FieldItems();
    private final Player first = new Player(1, 0, 0);
    private final Player second = new Player(2, 8, 0);
    private final List<Player> players = List.of(first, second);
    private final Map<Integer, PlayerGear> gears = Map.of(1, new PlayerGear(), 2, new PlayerGear());
    private final CellOccupancy occupancy = new CellOccupancy(balloons, items, players, player -> gears.get(player.id()));

    @Test
    void 물풍선은_놓인_칸과_던져져_내려앉을_칸을_차지한다() {
        WaterBalloon balloon = balloons.place(second, players).orElseThrow();
        assertTrue(occupancy.has(8, 0, Occupant.BALLOON));

        // 6칸 앞 (2, 0)에 내려앉는다
        assertTrue(balloons.throwBalloon(balloon, Direction.LEFT));

        assertTrue(balloon.isFlying());
        assertTrue(occupancy.has(2, 0, Occupant.BALLOON));
        assertFalse(occupancy.has(8, 0, Occupant.BALLOON), "날아가는 동안 떠난 칸은 비어 있다");
        assertFalse(occupancy.has(5, 0, Occupant.BALLOON), "날아가는 길은 차지하지 않는다");
    }

    @Test
    void 아이템은_놓인_칸만_차지한다() {
        items.place(3, 1, ItemType.BUBBLE);

        assertTrue(occupancy.has(3, 1, Occupant.ITEM));
        assertFalse(occupancy.has(3, 0, Occupant.ITEM));
    }

    @Test
    void 캐릭터는_몸이_조금이라도_걸친_칸을_모두_차지하고_맵에서_사라지면_비운다() {
        // 몸 가운데는 (3, 0)에 있고 오른쪽으로 (4, 0)에도 걸친다
        first.placeAt(TileMap.centerX(3) + 10, TileMap.centerY(0));

        assertTrue(occupancy.has(3, 0, Occupant.BODY));
        assertTrue(occupancy.has(4, 0, Occupant.BODY));
        assertFalse(occupancy.has(2, 0, Occupant.BODY));

        first.eliminate();
        assertTrue(occupancy.has(3, 0, Occupant.BODY), "쓰러지는 연출 중에는 아직 맵에 있다");
        first.update(null, Player.DYING_JUMP_SECONDS + Player.DYING_BLINK_SECONDS, MAP);
        assertEquals(PlayerState.GONE, first.state());
        assertFalse(occupancy.has(3, 0, Occupant.BODY));
    }

    @Test
    void 점프해_내려앉을_칸을_차지한다() {
        gears.get(first.id()).startJump(first, 2, 0);

        assertTrue(occupancy.has(2, 0, Occupant.JUMP_LANDING));
        assertFalse(occupancy.has(1, 0, Occupant.JUMP_LANDING));
    }

    @Test
    void 물줄기는_보이는_동안_덮은_칸을_차지하고_물풍선과는_따로_본다() {
        balloons.place(first, players);
        balloons.detonate(0, 0);

        assertTrue(occupancy.has(1, 0, Occupant.WATER));
        assertFalse(occupancy.has(2, 0, Occupant.WATER));
        assertFalse(occupancy.has(1, 0, Occupant.BALLOON));
    }

    @Test
    void 고른_것_가운데_하나라도_있으면_있다고_본다() {
        items.place(5, 1, ItemType.SHOE);
        balloons.place(first, players);
        balloons.detonate(0, 0);

        assertTrue(occupancy.hasAny(5, 1, EnumSet.of(Occupant.BALLOON, Occupant.ITEM)));
        assertFalse(occupancy.hasAny(5, 1, EnumSet.of(Occupant.BALLOON, Occupant.BODY)));
        assertFalse(occupancy.hasAny(5, 1, EnumSet.noneOf(Occupant.class)));
        // 물줄기만 남은 칸: 비행기가 보는 것들은 없지만, 상자는 받지 않는다
        assertFalse(occupancy.hasAny(1, 0, EnumSet.of(Occupant.BALLOON, Occupant.ITEM, Occupant.BODY)));
        assertTrue(occupancy.hasAny(1, 0, EnumSet.allOf(Occupant.class)));
    }
}
