package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemEffectsTest {

    /** 꼬꼬: 물풍선 1~8, 물줄기 1~5, 속도 5~8 */
    private static Player kkokko() {
        return new Player(1, new PlayerProfile("꼬꼬", Mascot.KKOKKO), 0, 0);
    }

    @Test
    void 물풍선_아이템은_놓아_둘_수_있는_물풍선을_하나_늘린다() {
        Player player = kkokko();

        ItemEffects.apply(player, ItemType.BUBBLE);

        assertEquals(2, player.balloons());
    }

    @Test
    void 물줄기_아이템은_물줄기를_한_칸_늘린다() {
        Player player = kkokko();

        ItemEffects.apply(player, ItemType.FLUID);

        assertEquals(2, player.waterRange());
    }

    @Test
    void 롤러_신발은_이동속도를_한_단계_올린다() {
        Player player = kkokko();

        ItemEffects.apply(player, ItemType.ROLLER_SHOE);

        assertEquals(6, player.speedLevel());
    }

    @Test
    void 캐릭터_최댓값에서는_더_먹어도_오르지_않는다() {
        Player player = kkokko();
        for (int i = 0; i < 20; i++) {
            ItemEffects.apply(player, ItemType.BUBBLE);
            ItemEffects.apply(player, ItemType.FLUID);
            ItemEffects.apply(player, ItemType.ROLLER_SHOE);
        }

        assertEquals(8, player.balloons());
        assertEquals(5, player.waterRange());
        assertEquals(8, player.speedLevel());
    }

    @Test
    void 효과를_아직_붙이지_않은_아이템은_능력치를_바꾸지_않는다() {
        for (ItemType item : ItemType.values()) {
            if (item == ItemType.BUBBLE || item == ItemType.FLUID || item == ItemType.ROLLER_SHOE) {
                continue;
            }
            Player player = kkokko();

            ItemEffects.apply(player, item);

            assertEquals(1, player.balloons(), item.name());
            assertEquals(1, player.waterRange(), item.name());
            assertEquals(5, player.speedLevel(), item.name());
        }
    }
}
