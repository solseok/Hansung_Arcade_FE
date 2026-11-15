package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerGearTest {

    private static final double TICK = 1.0 / 60;

    private static void advance(PlayerGear gear, Player player, double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += TICK) {
            gear.advance(TICK, player);
        }
    }

    @Test
    void 같은_액티브_아이템은_개수가_쌓이고_다른_것을_먹으면_바꿔_낀다() {
        PlayerGear gear = new PlayerGear();

        gear.equip(ItemType.SHIELD);
        gear.equip(ItemType.SHIELD);
        assertEquals(Optional.of(ItemType.SHIELD), gear.activeItem());
        assertEquals(2, gear.activeCount());

        gear.equip(ItemType.NEEDLE);
        assertEquals(Optional.of(ItemType.NEEDLE), gear.activeItem());
        assertEquals(1, gear.activeCount());
    }

    @Test
    void 쓸_때마다_하나씩_줄고_다_쓰면_칸이_빈다() {
        PlayerGear gear = new PlayerGear();
        gear.equip(ItemType.SPRING_SHOE);
        gear.equip(ItemType.SPRING_SHOE);

        gear.useOne();
        assertEquals(1, gear.activeCount());
        gear.useOne();

        assertTrue(gear.activeItem().isEmpty());
        assertEquals(0, gear.activeCount());
    }

    @Test
    void 쌓을_수_있는_개수는_99개까지다() {
        PlayerGear gear = new PlayerGear();
        for (int i = 0; i < 120; i++) {
            gear.equip(ItemType.NEEDLE);
        }

        assertEquals(PlayerGear.MAX_ACTIVE_COUNT, gear.activeCount());
    }

    @Test
    void 패시브_아이템은_아이템_칸에_끼지_않는다() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerGear().equip(ItemType.TANK));
    }

    @Test
    void 보호막은_3초_동안_물줄기를_막는다() {
        PlayerGear gear = new PlayerGear();
        Player player = new Player(1, 0, 0);

        gear.raiseShield();
        advance(gear, player, 2.9);
        assertTrue(gear.isShielded());
        assertTrue(gear.ignoresWater());

        advance(gear, player, 0.1);
        assertFalse(gear.isShielded());
        assertFalse(gear.ignoresWater());
    }

    @Test
    void 탱크가_부서지면_1초_동안_다시_맞지_않는다() {
        PlayerGear gear = new PlayerGear();
        Player player = new Player(1, 0, 0);
        gear.boardTank();

        assertTrue(gear.breakTank());
        assertFalse(gear.hasTank());
        assertTrue(gear.ignoresWater());
        assertFalse(gear.breakTank(), "탱크는 한 번만 부서진다");

        advance(gear, player, PlayerGear.TANK_GRACE_SECONDS);
        assertFalse(gear.ignoresWater());
    }

    @Test
    void 점프하면_공중을_지나_내려앉을_칸_가운데에_선다() {
        PlayerGear gear = new PlayerGear();
        Player player = new Player(1, 0, 0);

        gear.startJump(player, 2, 0);
        assertTrue(gear.isAirborne());
        assertTrue(gear.isLandingOn(2, 0));
        advance(gear, player, PlayerGear.JUMP_SECONDS / 2);
        assertEquals(TileMap.centerX(1), player.x(), 1e-6);
        assertEquals(0.5, gear.jumpProgress(), 1e-6);

        boolean landed = false;
        for (int i = 0; i < 60 && !landed; i++) {
            landed = gear.advance(TICK, player);
        }

        assertTrue(landed);
        assertFalse(gear.isAirborne());
        assertEquals(TileMap.centerX(2), player.x(), 1e-9);
        assertEquals(TileMap.centerY(0), player.y(), 1e-9);
    }
}
