package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.match.Match;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * 물줄기 싸움의 판정. 게임 상태(GameWorld)가 경기 중인 틱마다, 물풍선을 다 움직이고 터뜨린 뒤 {@link #settle()}을 한 번 부른다.
 * 플레이어 목록, 경기(같은 편인지와 kill·save 기록), 플레이어마다의 아이템 효과, 칸을 덮은 물줄기의 주인만 보고 정한다.
 * Swing에 의존하지 않는다.
 *
 * <p>한 번 부르면 이 순서로 가린다.</p>
 * <ol>
 *   <li>가둠: 물줄기가 덮은 칸에 선(충돌 박스 가운데가 있는) 플레이어는 누구의 물줄기든 물풍선에 갇힌다.
 *       보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않고, 탱크에 타고 있으면 갇히는 대신 탱크가 부서진다.</li>
 *   <li>접촉: 갇힌 사람에게 보통 상태의 다른 플레이어가 닿으면, 같은 편이면 구하고(save) 상대면 터뜨린다(kill, 탈락).
 *       같은 틱에 여럿이 닿으면 번호가 작은 사람이 먼저 닿은 것으로 친다.</li>
 *   <li>시간 초과: 갇힌 채 {@link Player#TRAP_SECONDS}초가 지나면 탈락한다. 가둔 물줄기의 주인이 상대면 그 주인이 kill을 얻는다.</li>
 * </ol>
 */
final class BattleRules {

    /** 칸을 덮은 물줄기의 주인을 알려 준다 */
    @FunctionalInterface
    interface WaterOwners {

        /** (col, row) 칸을 덮은 물줄기의 주인 번호. 물줄기가 없으면 비어 있다 */
        OptionalInt at(int col, int row);
    }

    private final List<Player> players;
    private final Match match;
    private final Function<Player, PlayerGear> gears;
    private final WaterOwners water;

    /**
     * @param players 맵 위의 모든 플레이어
     * @param match   이번 판. 같은 편인지 보고 kill·save를 기록한다
     * @param gears   플레이어마다 아이템으로 얻은 능력과 쓰는 중인 효과
     * @param water   칸을 덮은 물줄기의 주인
     */
    BattleRules(List<Player> players, Match match, Function<Player, PlayerGear> gears, WaterOwners water) {
        this.players = players;
        this.match = match;
        this.gears = gears;
        this.water = water;
    }

    /** 한 틱의 가둠·접촉·시간 초과를 이 순서로 가린다. 경기가 진행 중일 때만 부른다 */
    void settle() {
        trapSoakedPlayers();
        settleTouches();
        eliminateTimedOut();
    }

    /**
     * 물줄기가 덮은 칸에 선 사람을 가둔다. 자기나 같은 편의 물줄기여도 갇힌다.
     * 보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않는다. 탱크에 타고 있으면 탱크가 대신 부서진다.
     */
    private void trapSoakedPlayers() {
        for (Player player : players) {
            PlayerGear gear = gears.apply(player);
            if (player.state() != PlayerState.NORMAL || gear.ignoresWater()) {
                continue;
            }
            water.at(player.col(), player.row()).ifPresent(owner -> {
                if (!gear.breakTank()) {
                    player.trap(owner);
                }
            });
        }
    }

    /**
     * 갇힌 사람에게 보통 상태의 플레이어가 닿으면 같은 편은 구하고 상대는 터뜨린다.
     * 같은 틱에 여럿이 닿으면 번호가 작은 사람이 먼저 닿은 것으로 친다.
     */
    private void settleTouches() {
        for (Player trapped : players) {
            if (trapped.state() != PlayerState.TRAPPED) {
                continue;
            }
            for (Player other : players) {
                if (other == trapped || other.state() != PlayerState.NORMAL || gears.apply(other).isAirborne()
                        || !other.touches(trapped)) {
                    continue;
                }
                if (match.isAlly(other, trapped)) {
                    trapped.escape();
                    match.recordSave(other.id());
                } else {
                    trapped.eliminate();
                    match.recordKill(other.id());
                }
                break;
            }
        }
    }

    /** 갇힌 채 시간이 다 된 사람을 탈락시킨다. 가둔 물줄기의 주인이 상대면 그 주인에게 kill을 준다 */
    private void eliminateTimedOut() {
        for (Player trapped : players) {
            if (!trapped.isTrapTimeUp()) {
                continue;
            }
            trapped.eliminate();
            trapped.trapperId().ifPresent(ownerId -> playerById(ownerId)
                    .filter(owner -> !match.isAlly(owner, trapped))
                    .ifPresent(owner -> match.recordKill(owner.id())));
        }
    }

    private Optional<Player> playerById(int id) {
        return players.stream().filter(player -> player.id() == id).findFirst();
    }
}
