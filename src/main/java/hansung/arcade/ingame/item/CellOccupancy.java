package hansung.arcade.ingame.item;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 맵 칸에 무엇이 있는지 한 곳에서 본다. Swing에 의존하지 않는다.
 *
 * <p>"칸이 비었는지"는 기능마다 기준이 달라서, 칸에 있을 수 있는 것을 {@link Occupant}로 나눠 두고 각 기능이 필요한 것만
 * 골라 묻는다. 블럭·덤불·맵 밖처럼 맵 자체가 막은 칸은 여기서 보지 않고 각 기능이 맵으로 따로 본다.</p>
 * <ul>
 *   <li>밀린 상자를 받을 칸: 모두 안 된다.</li>
 *   <li>비행기가 아이템을 떨어뜨릴 칸: 물풍선·아이템·캐릭터 몸이 있으면 안 된다.</li>
 *   <li>스프링 신발로 내려앉을 칸: 물풍선이 있으면 안 된다 ({@link BalloonField#holds}를 그대로 쓴다).</li>
 *   <li>던진 물풍선이 내려앉을 칸: 다른 물풍선이 있으면 안 된다. 캐릭터는 보지 않는다 (BalloonField 안에서 같은 물풍선 기준을 쓴다).</li>
 * </ul>
 */
public final class CellOccupancy {

    /** 칸에 있을 수 있는 것 */
    public enum Occupant {
        /**
         * 물풍선이 차지했거나 곧 차지할 칸: 놓였거나 미끄러지는 물풍선이 있는 칸, 미끄러지는 물풍선이 막 들어가려는 칸,
         * 던진 물풍선이 내려앉을 칸 ({@link BalloonField#holds})
         */
        BALLOON,
        /** 바닥에 놓인 아이템 */
        ITEM,
        /** 몸(충돌 박스)이 조금이라도 걸친 캐릭터. 맵에서 사라진 사람은 빼고, 쓰러지는 연출 중인 사람은 아직 있다 */
        BODY,
        /** 스프링 신발로 뛰어 내려앉을 칸 */
        JUMP_LANDING,
        /** 물줄기가 보이는 칸 */
        WATER
    }

    private final BalloonField balloons;
    private final FieldItems items;
    private final List<Player> players;
    private final Function<Player, PlayerGear> gears;

    /**
     * @param balloons 맵 위의 물풍선과 물줄기
     * @param items    바닥에 놓인 아이템
     * @param players  맵 위의 모든 플레이어
     * @param gears    플레이어마다 아이템으로 얻은 능력과 쓰는 중인 효과
     */
    public CellOccupancy(BalloonField balloons, FieldItems items, List<Player> players, Function<Player, PlayerGear> gears) {
        this.balloons = Objects.requireNonNull(balloons);
        this.items = Objects.requireNonNull(items);
        this.players = Objects.requireNonNull(players);
        this.gears = Objects.requireNonNull(gears);
    }

    /** (col, row) 칸에 occupant가 있는지 */
    public boolean has(int col, int row, Occupant occupant) {
        return switch (occupant) {
            case BALLOON -> balloons.holds(col, row);
            case ITEM -> items.at(col, row).isPresent();
            case BODY -> players.stream()
                    .anyMatch(player -> player.state() != PlayerState.GONE && player.overlapsTile(col, row));
            case JUMP_LANDING -> players.stream().anyMatch(player -> gears.apply(player).isLandingOn(col, row));
            case WATER -> balloons.isWet(col, row);
        };
    }

    /** (col, row) 칸에 occupants 가운데 하나라도 있는지 */
    public boolean hasAny(int col, int row, Set<Occupant> occupants) {
        for (Occupant occupant : occupants) {
            if (has(col, row, occupant)) {
                return true;
            }
        }
        return false;
    }
}
