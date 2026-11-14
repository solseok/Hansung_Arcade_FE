package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Player;

/**
 * 플레이어가 맵의 아이템을 먹었을 때 생기는 일. 아이템마다 할 일을 여기 한 곳에 모아 둔다.
 * 능력치는 {@link Player}가 캐릭터의 최댓값에서 멈춰 주므로, 최댓값에서 먹어도 아이템만 사라진다.
 */
public final class ItemEffects {

    private ItemEffects() {
    }

    /** player가 item을 먹었다. 누가 언제 먹을 수 있는지는 게임 상태(GameWorld)가 정한다 */
    public static void apply(Player player, ItemType item) {
        switch (item) {
            case BUBBLE -> player.addBalloons(1);
            case FLUID -> player.addWaterRange(1);
            case ROLLER_SHOE -> player.addSpeedLevel(1);
            // 아래 아이템의 효과는 아이템 효과 단계에서 붙인다. 지금은 먹으면 사라지기만 한다
            case ULTRA, RED_DEVIL, GLOVE, SHOE, TANK, NEEDLE, SHIELD, SPRING_SHOE -> {
            }
        }
    }
}
