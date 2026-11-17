package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.match.Match;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * 물줄기 싸움의 판정. 게임 상태(GameWorld)가 경기 중인 틱마다, 물풍선을 다 움직이고 터뜨린 뒤 {@link #settle()}을 한 번 부른다.
 * 플레이어 목록, 경기(같은 편인지와 kill·save 기록), 플레이어마다의 아이템 효과, 칸을 덮은 물줄기의 주인만 보고 정한다.
 * Swing에 의존하지 않는다.
 *
 * <p>한 번 부르면 이 순서로 가린다.</p>
 * <ol>
 *   <li>가둠: 물줄기가 덮은 칸에 선(충돌 박스 가운데가 있는) 플레이어는 누구의 물줄기든 물풍선에 갇힌다.
 *       보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않고, 탱크에 타고 있으면 갇히는 대신 탱크가 부서진다.
 *       그 칸을 여러 사람의 물줄기가 덮었으면 상대의 물줄기를 먼저 보고 그 주인을 가둔 사람으로 친다. 상대가 여럿이면
 *       그 칸 가까이에서 터진 물줄기의 주인, 거리도 같으면 늦게 터진 물줄기의 주인이다
 *       ({@link hansung.arcade.ingame.balloon.BalloonField#wetOwnersAt}). 상대가 없으면 자기나 같은 편이 가둔 것이다.</li>
 *   <li>접촉: 갇힌 사람에게 보통 상태의 다른 플레이어가 닿으면, 같은 편이면 구하고(save) 상대면 터뜨린다(kill, 탈락).
 *       이전 틱부터 갇혀 있던 사람만 본다. 그래서 갇히는 틱에 이미 닿아 있었어도 그 틱에는 갇힌 채로 남고, 다음 틱부터 가린다.
 *       같은 틱에 상대와 같은 편이 함께 닿으면 번호와 상관없이 터뜨린다(처치가 먼저).
 *       kill·save는 닿은 사람 가운데 중심이 갇힌 사람과 가장 가까운 사람이 얻고, 거리까지 같으면 번호가 작은 사람이 얻는다.</li>
 *   <li>시간 초과: 갇힌 채 {@link Player#TRAP_SECONDS}초가 지나면 탈락한다. 가둔 물줄기의 주인이 상대면 그 주인이 kill을 얻는다.</li>
 * </ol>
 */
final class BattleRules {

    /** 칸을 덮은 물줄기의 주인들을 알려 준다 */
    @FunctionalInterface
    interface WaterOwners {

        /**
         * (col, row) 칸을 덮은 물줄기들의 주인 번호. 상대가 여럿일 때 가둔 사람으로 칠 차례대로 늘어놓는다.
         * 물줄기가 없으면 빈 목록이다
         */
        List<Integer> at(int col, int row);
    }

    private final List<Player> players;
    private final Match match;
    private final Function<Player, PlayerGear> gears;
    private final WaterOwners water;

    /**
     * @param players 맵 위의 모든 플레이어
     * @param match   이번 판. 같은 편인지 보고 kill·save를 기록한다
     * @param gears   플레이어마다 아이템으로 얻은 능력과 쓰는 중인 효과
     * @param water   칸을 덮은 물줄기의 주인들
     */
    BattleRules(List<Player> players, Match match, Function<Player, PlayerGear> gears, WaterOwners water) {
        this.players = players;
        this.match = match;
        this.gears = gears;
        this.water = water;
    }

    /** 한 틱의 가둠·접촉·시간 초과를 이 순서로 가린다. 경기가 진행 중일 때만 부른다 */
    void settle() {
        Set<Player> justTrapped = trapSoakedPlayers();
        settleTouches(justTrapped);
        eliminateTimedOut();
    }

    /**
     * 물줄기가 덮은 칸에 선 사람을 가둔다. 자기나 같은 편의 물줄기여도 갇히고, 상대의 물줄기도 함께 덮었으면 그 상대가 가둔 것이다.
     * 보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않는다. 탱크에 타고 있으면 탱크가 대신 부서진다.
     *
     * @return 이번에 갇힌 사람들
     */
    private Set<Player> trapSoakedPlayers() {
        Set<Player> trapped = new HashSet<>();
        for (Player player : players) {
            PlayerGear gear = gears.apply(player);
            if (player.state() != PlayerState.NORMAL || gear.ignoresWater()) {
                continue;
            }
            List<Integer> owners = water.at(player.col(), player.row());
            if (!owners.isEmpty() && !gear.breakTank() && player.trap(trapperOf(player, owners))) {
                trapped.add(player);
            }
        }
        return trapped;
    }

    /** player가 선 칸을 덮은 물줄기 주인들 가운데 가둔 사람으로 칠 주인: 차례에서 맨 앞의 상대, 상대가 없으면 맨 앞 주인 */
    private int trapperOf(Player player, List<Integer> owners) {
        return owners.stream()
                .filter(owner -> playerById(owner).filter(other -> !match.isAlly(other, player)).isPresent())
                .findFirst()
                .orElse(owners.get(0));
    }

    /**
     * 갇힌 사람에게 보통 상태의 플레이어가 닿으면 상대는 터뜨리고 같은 편은 구한다. 점프해 공중에 있는 사람은 닿지 않는다.
     * 상대가 하나라도 닿았으면 같은 편이 함께 닿았어도 터뜨린다. 닿은 상대(상대가 없으면 같은 편) 가운데
     * {@link #nearest 가장 가까운 사람}이 kill(save)을 얻는다.
     *
     * @param justTrapped 이번 틱에 갇힌 사람들. 갇힌 모습이 한 틱도 보이지 않은 채 풀리거나 터지지 않도록 이번 틱에는 가리지 않는다
     */
    private void settleTouches(Set<Player> justTrapped) {
        for (Player trapped : players) {
            if (trapped.state() != PlayerState.TRAPPED || justTrapped.contains(trapped)) {
                continue;
            }
            List<Player> touching = players.stream()
                    .filter(other -> other != trapped && other.state() == PlayerState.NORMAL
                            && !gears.apply(other).isAirborne() && other.touches(trapped))
                    .toList();
            Optional<Player> killer = nearest(trapped, touching.stream().filter(other -> !match.isAlly(other, trapped)).toList());
            if (killer.isPresent()) {
                trapped.eliminate();
                match.recordKill(killer.get().id());
                continue;
            }
            nearest(trapped, touching).ifPresent(saver -> {
                trapped.escape();
                match.recordSave(saver.id());
            });
        }
    }

    /** candidates 가운데 중심이 trapped의 중심과 가장 가까운 사람. 거리까지 같으면 번호가 작은 사람이다 */
    private static Optional<Player> nearest(Player trapped, List<Player> candidates) {
        return candidates.stream().min(Comparator
                .comparingDouble((Player other) -> Math.hypot(other.x() - trapped.x(), other.y() - trapped.y()))
                .thenComparingInt(Player::id));
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
