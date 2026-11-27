package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEffectsTest {

    /** 꼬꼬: 물풍선 1~8, 물줄기 1~5, 속도 5~8 */
    private static Player kkokko() {
        return new Player(1, new PlayerProfile("꼬꼬", Mascot.KKOKKO), 0, 0);
    }

    @Test
    void 물풍선_아이템은_놓아_둘_수_있는_물풍선을_하나_늘린다() {
        Player player = kkokko();

        ItemEffects.apply(player, new PlayerGear(), ItemType.BUBBLE);

        assertEquals(2, player.balloons());
    }

    @Test
    void 물줄기_아이템은_물줄기를_한_칸_늘린다() {
        Player player = kkokko();

        ItemEffects.apply(player, new PlayerGear(), ItemType.FLUID);

        assertEquals(2, player.waterRange());
    }

    @Test
    void 롤러_신발은_이동속도를_한_단계_올린다() {
        Player player = kkokko();

        ItemEffects.apply(player, new PlayerGear(), ItemType.ROLLER_SHOE);

        assertEquals(6, player.speedLevel());
    }

    @Test
    void 캐릭터_최댓값에서는_더_먹어도_오르지_않는다() {
        Player player = kkokko();
        PlayerGear gear = new PlayerGear();
        for (int i = 0; i < 20; i++) {
            ItemEffects.apply(player, gear, ItemType.BUBBLE);
            ItemEffects.apply(player, gear, ItemType.FLUID);
            ItemEffects.apply(player, gear, ItemType.ROLLER_SHOE);
        }

        assertEquals(8, player.balloons());
        assertEquals(5, player.waterRange());
        assertEquals(8, player.speedLevel());
    }

    @Test
    void 울트라는_물줄기를_캐릭터_최댓값까지_한_번에_올린다() {
        Player player = kkokko();
        PlayerGear gear = new PlayerGear();

        ItemEffects.apply(player, gear, ItemType.ULTRA);

        assertEquals(5, player.waterRange());
        assertEquals(1, player.balloons());
        assertEquals(5, player.speedLevel());
        assertFalse(gear.canPushBalloons());
    }

    @Test
    void 빨간_악마는_이동속도를_최댓값까지_올리고_물풍선을_밀_수_있게_한다() {
        Player player = new Player(1, new PlayerProfile("냥이", Mascot.HANSUNGNYANGI), 0, 0);
        PlayerGear gear = new PlayerGear();
        int waterRange = player.waterRange();

        ItemEffects.apply(player, gear, ItemType.RED_DEVIL);

        // 한성냥이의 이동속도 최댓값은 8. 물줄기는 그대로다
        assertEquals(Mascot.HANSUNGNYANGI.speed().max(), player.speedLevel());
        assertEquals(8, player.speedLevel());
        assertEquals(waterRange, player.waterRange());
        assertTrue(gear.canPushBalloons());
        assertFalse(gear.canThrowBalloons());
    }

    @Test
    void 신발은_물풍선을_밀고_장갑은_던지고_탱크는_태운다() {
        PlayerGear shoe = new PlayerGear();
        PlayerGear glove = new PlayerGear();
        PlayerGear tank = new PlayerGear();

        ItemEffects.apply(kkokko(), shoe, ItemType.SHOE);
        ItemEffects.apply(kkokko(), glove, ItemType.GLOVE);
        ItemEffects.apply(kkokko(), tank, ItemType.TANK);

        assertTrue(shoe.canPushBalloons());
        assertFalse(shoe.canThrowBalloons());
        assertTrue(glove.canThrowBalloons());
        assertFalse(glove.canPushBalloons());
        assertTrue(tank.hasTank());
    }

    @Test
    void 액티브_아이템은_능력치를_바꾸지_않고_아이템_칸에_낀다() {
        for (ItemType item : ItemType.values()) {
            if (!item.isActive()) {
                continue;
            }
            Player player = kkokko();
            PlayerGear gear = new PlayerGear();

            ItemEffects.apply(player, gear, item);

            assertEquals(Optional.of(item), gear.activeItem(), item.name());
            assertEquals(1, gear.activeCount(), item.name());
            assertEquals(1, player.balloons(), item.name());
            assertEquals(1, player.waterRange(), item.name());
            assertEquals(5, player.speedLevel(), item.name());
        }
    }
}
