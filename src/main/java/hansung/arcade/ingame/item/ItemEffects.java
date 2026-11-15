package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Player;

/**
 * 플레이어가 맵의 아이템을 먹었을 때 생기는 일. 아이템마다 할 일을 여기 한 곳에 모아 둔다.
 * 능력치는 {@link Player}가 캐릭터의 최댓값에서 멈춰 주므로, 최댓값에서 먹어도 아이템만 사라진다.
 *
 * <p>패시브 아이템은 먹는 순간 효과가 나고, 액티브 아이템(바늘·보호막·스프링 신발)은 {@link PlayerGear 아이템 칸}에 끼워 두었다가
 * 아이템 키로 쓴다({@link ItemActions#useItem}).</p>
 */
public final class ItemEffects {

    private ItemEffects() {
    }

    /** player가 item을 먹었다. 누가 언제 먹을 수 있는지는 게임 상태(GameWorld)가 정한다 */
    public static void apply(Player player, PlayerGear gear, ItemType item) {
        switch (item) {
            case BUBBLE -> player.addBalloons(1);
            case FLUID -> player.addWaterRange(1);
            case ROLLER_SHOE -> player.addSpeedLevel(1);
            case ULTRA -> maxOutWaterRange(player);
            case RED_DEVIL -> {
                maxOutWaterRange(player);
                gear.allowPushing();
            }
            case SHOE -> gear.allowPushing();
            case GLOVE -> gear.allowThrowing();
            case TANK -> gear.boardTank();
            case NEEDLE, SHIELD, SPRING_SHOE -> gear.equip(item);
        }
    }

    /** 물줄기 길이를 캐릭터의 최댓값까지 한 번에 올린다 */
    private static void maxOutWaterRange(Player player) {
        player.addWaterRange(player.profile().mascot().waterRange().max());
    }
}
