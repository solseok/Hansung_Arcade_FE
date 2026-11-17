package hansung.arcade.ingame.item;

import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.map.TileMap;

import java.util.Optional;

/**
 * 플레이어 한 명이 아이템으로 얻은 것. 능력치(물풍선 개수·물줄기 길이·이동속도)는 {@link Player}가 세고,
 * 여기서는 그 밖의 것을 센다. Swing에 의존하지 않는다.
 *
 * <ul>
 *   <li>패시브 아이템으로 생긴 능력: 물풍선 밀기(신발·빨간 악마), 물풍선 던지기(장갑), 탱크 탑승</li>
 *   <li>액티브 아이템 칸 하나. 같은 아이템을 또 먹으면 개수가 늘고, 다른 아이템을 먹으면 그것으로 바꿔 낀다.
 *       쓸 때마다 하나씩 줄고 다 쓰면 칸이 빈다.</li>
 *   <li>쓰는 중인 효과: 보호막({@link #SHIELD_SECONDS}초), 탱크가 부서진 뒤 잠깐의 무적({@link #TANK_GRACE_SECONDS}초),
 *       스프링 신발 점프({@link #JUMP_SECONDS}초)</li>
 * </ul>
 */
public final class PlayerGear {

    /** 보호막이 물줄기를 막아 주는 시간(초) */
    public static final double SHIELD_SECONDS = 3.0;
    /**
     * 탱크가 물줄기에 맞아 부서진 뒤 다시 맞지 않는 시간(초). 물줄기는 {@link hansung.arcade.ingame.balloon.Explosion#SECONDS}초 동안
     * 남으므로, 이 시간이 없으면 탱크를 부순 바로 그 물줄기에 다음 틱에 갇힌다.
     */
    public static final double TANK_GRACE_SECONDS = 1.0;
    /** 스프링 신발로 뛰어 내려앉기까지의 시간(초) */
    public static final double JUMP_SECONDS = 0.4;
    /** 액티브 아이템을 쌓아 둘 수 있는 가장 많은 개수. 칸에 두 자리 숫자로 보여 준다 */
    public static final int MAX_ACTIVE_COUNT = 99;
    private static final double TIME_SLACK = 1e-6;

    private boolean canPush;
    private boolean canThrow;
    private boolean tank;
    private ItemType activeItem;
    private int activeCount;
    private double shieldLeft;
    private double tankGraceLeft;
    private Jump jump;

    /** 물풍선 쪽으로 걸어가 부딪히면 물풍선을 밀 수 있는지 (신발, 빨간 악마) */
    public boolean canPushBalloons() {
        return canPush;
    }

    /** 물풍선 키로 물풍선을 집어 던질 수 있는지 (장갑) */
    public boolean canThrowBalloons() {
        return canThrow;
    }

    /** 탱크에 타고 있는지. 물줄기에 한 번 맞으면 갇히는 대신 탱크가 부서진다 */
    public boolean hasTank() {
        return tank;
    }

    void allowPushing() {
        canPush = true;
    }

    void allowThrowing() {
        canThrow = true;
    }

    void boardTank() {
        tank = true;
    }

    /**
     * 물줄기에 맞아 탱크가 부서진다. {@link #TANK_GRACE_SECONDS}초 동안은 다시 맞지 않는다.
     *
     * @return 타고 있던 탱크가 부서졌는지. 탱크가 없었으면 false
     */
    public boolean breakTank() {
        if (!tank) {
            return false;
        }
        tank = false;
        tankGraceLeft = TANK_GRACE_SECONDS;
        return true;
    }

    /** 탱크가 부서진 뒤 다시 맞지 않는 중인지. 캐릭터를 깜빡여 보여 준다 */
    public boolean isRecoveringFromTank() {
        return tankGraceLeft > TIME_SLACK;
    }

    /** 지금 물줄기에 맞지 않는지: 보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않는다 */
    public boolean ignoresWater() {
        return isShielded() || isRecoveringFromTank() || isAirborne();
    }

    /** 액티브 아이템 칸에 든 아이템. 비어 있으면 빈 값 */
    public Optional<ItemType> activeItem() {
        return Optional.ofNullable(activeItem);
    }

    /** 액티브 아이템 칸에 든 개수. 비어 있으면 0 */
    public int activeCount() {
        return activeCount;
    }

    /** 액티브 아이템을 칸에 낀다. 같은 아이템이면 개수를 하나 늘리고({@link #MAX_ACTIVE_COUNT}까지), 다르면 바꿔 낀다 */
    void equip(ItemType item) {
        if (!item.isActive()) {
            throw new IllegalArgumentException("액티브 아이템이 아닙니다: " + item);
        }
        if (item == activeItem) {
            activeCount = Math.min(MAX_ACTIVE_COUNT, activeCount + 1);
        } else {
            activeItem = item;
            activeCount = 1;
        }
    }

    /** 칸의 액티브 아이템을 하나 쓴다. 다 쓰면 칸이 빈다 */
    void useOne() {
        if (activeCount <= 0) {
            return;
        }
        activeCount--;
        if (activeCount == 0) {
            activeItem = null;
        }
    }

    /** 보호막을 쓰는 중인지 */
    public boolean isShielded() {
        return shieldLeft > TIME_SLACK;
    }

    /** 보호막이 남은 시간(초). 쓰는 중이 아니면 0 */
    public double shieldSecondsLeft() {
        return Math.max(0, shieldLeft);
    }

    void raiseShield() {
        shieldLeft = SHIELD_SECONDS;
    }

    /** 스프링 신발로 뛰어 공중에 있는지. 공중에서는 움직이거나 물풍선을 놓거나 아이템을 먹지 못하고, 물줄기에도 맞지 않는다 */
    public boolean isAirborne() {
        return jump != null;
    }

    /** 점프한 뒤 흐른 비율 (0~1). 공중에 있지 않으면 0 */
    public double jumpProgress() {
        return jump == null ? 0 : Math.min(1, jump.elapsed / JUMP_SECONDS);
    }

    /** 점프해 내려앉을 칸이 (col, row)인지. 그 칸에 상자가 밀려 들어오지 못하게 할 때 쓴다 */
    public boolean isLandingOn(int col, int row) {
        return jump != null && jump.toCol == col && jump.toRow == row;
    }

    /** player가 지금 자리에서 (toCol, toRow) 칸 가운데로 뛴다 */
    void startJump(Player player, int toCol, int toRow) {
        jump = new Jump(player.x(), player.y(), toCol, toRow);
    }

    /**
     * 쓰는 중인 효과의 시간을 dt초 흘린다. 점프 중이면 player를 뛰어오른 자리와 내려앉을 칸 사이로 옮기고,
     * 다 뛰었으면 내려앉을 칸 가운데에 세운다.
     *
     * @return 이번에 점프를 마치고 내려앉았는지
     */
    public boolean advance(double dt, Player player) {
        shieldLeft = Math.max(0, shieldLeft - dt);
        tankGraceLeft = Math.max(0, tankGraceLeft - dt);
        if (jump == null) {
            return false;
        }
        jump.elapsed += dt;
        double t = jumpProgress();
        double toX = TileMap.centerX(jump.toCol);
        double toY = TileMap.centerY(jump.toRow);
        if (jump.elapsed >= JUMP_SECONDS - TIME_SLACK) {
            player.placeAt(toX, toY);
            jump = null;
            return true;
        }
        player.placeAt(jump.fromX + (toX - jump.fromX) * t, jump.fromY + (toY - jump.fromY) * t);
        return false;
    }

    /** 스프링 신발 점프 한 번 */
    private static final class Jump {
        private final double fromX;
        private final double fromY;
        private final int toCol;
        private final int toRow;
        private double elapsed;

        Jump(double fromX, double fromY, int toCol, int toRow) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toCol = toCol;
            this.toRow = toRow;
        }
    }
}
