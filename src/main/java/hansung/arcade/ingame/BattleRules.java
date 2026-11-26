package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.match.Match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 *   <li>가둠: 충돌 박스가 물줄기와 {@link #SOAK_SHARE}(66%) 이상 겹친 플레이어는 누구의 물줄기든 물풍선에 갇힌다.
 *       겹침은 박스가 걸친 가로 줄·세로 열마다 따로 재므로, 네 칸이 만나는 곳에 걸쳐 서면 십자 물줄기 한가운데 옆이어도
 *       갇히지 않는다(크아의 걸치기).
 *       보호막을 쓰는 중이거나, 탱크가 막 부서졌거나, 점프해 공중에 있으면 맞지 않고, 탱크에 타고 있으면 갇히는 대신 탱크가 부서진다.
 *       여러 사람의 물줄기에 맞았으면 상대의 물줄기를 먼저 보고 그 주인을 가둔 사람으로 친다. 상대가 여럿이면
 *       많이 겹친 칸의 주인부터, 한 칸 안에서는 그 칸 가까이에서 터진 물줄기의 주인, 거리도 같으면 늦게 터진 물줄기의 주인이다
 *       ({@link hansung.arcade.ingame.balloon.BalloonField#wetOwnersAt}). 상대가 없으면 자기나 같은 편이 가둔 것이다.</li>
 *   <li>접촉: 갇힌 사람에게 보통 상태의 다른 플레이어가 닿으면, 같은 편이면 구하고(save) 상대면 터뜨린다(kill, 탈락).
 *       이전 틱부터 갇혀 있던 사람만 본다. 그래서 갇히는 틱에 이미 닿아 있었어도 그 틱에는 갇힌 채로 남고, 다음 틱부터 가린다.
 *       같은 틱에 상대와 같은 편이 함께 닿으면 번호와 상관없이 터뜨린다(처치가 먼저).
 *       kill·save는 닿은 사람 가운데 중심이 갇힌 사람과 가장 가까운 사람이 얻고, 거리까지 같으면 번호가 작은 사람이 얻는다.</li>
 *   <li>시간 초과: 갇힌 채 {@link Player#TRAP_SECONDS}초가 지나면 탈락한다. 가둔 물줄기의 주인이 상대면 그 주인이 kill을 얻는다.</li>
 * </ol>
 */
final class BattleRules {

    /**
     * 물줄기에 맞았다고 보는 겹침 비율. 크레이지아케이드 개발자 설명(넥슨 "넥아알려줄게" 걸치기 편)대로 충돌 영역이
     * 물줄기와 66% 이상 겹치면 맞는다
     */
    static final double SOAK_SHARE = 0.66;
    /** 픽셀을 나눠 잰 비율이라 경계에서 아주 조금 모자랄 수 있어 그만큼은 넘은 것으로 친다 */
    private static final double SHARE_SLACK = 1e-9;

    /** 갇힌 사람에게 닿아 일어난 일. 게임 상태가 알맞은 소리를 고를 때 쓴다 */
    enum Touch {
        /** 상대가 닿아 터뜨렸다 (kill) */
        POP,
        /** 같은 편이 닿아 구했다 (save) */
        RESCUE
    }

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

    /**
     * 한 틱의 가둠·접촉·시간 초과를 이 순서로 가린다. 경기가 진행 중일 때만 부른다.
     *
     * @return 이번 틱에 갇힌 사람에게 닿아 일어난 일. 시간 초과로 탈락한 것은 넣지 않는다
     */
    Set<Touch> settle() {
        Set<Player> justTrapped = trapSoakedPlayers();
        Set<Touch> touches = settleTouches(justTrapped);
        eliminateTimedOut();
        return touches;
    }

    /**
     * 물줄기에 맞은 사람을 가둔다. 자기나 같은 편의 물줄기여도 갇히고, 상대의 물줄기에도 맞았으면 그 상대가 가둔 것이다.
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
            List<Integer> owners = soakingOwners(player);
            if (!owners.isEmpty() && !gear.breakTank() && player.trap(trapperOf(player, owners))) {
                trapped.add(player);
            }
        }
        return trapped;
    }

    /**
     * player를 맞힌 물줄기의 주인들. 맞지 않았으면 빈 목록이다.
     *
     * <p>충돌 박스가 걸친 가로 줄마다, 세로 열마다 물줄기가 덮은 칸과 겹친 넓이를 충돌 박스 넓이에 대한 비율로 재어,
     * 어느 한 줄이나 한 열에서라도 {@link #SOAK_SHARE} 이상이면 맞은 것이다. 그래서 네 칸이 만나는 곳에 서면
     * 십자 물줄기가 그 가운데 세 칸을 덮어도 가로 줄기·세로 줄기와 각각 절반씩만 겹쳐 맞지 않는다(걸치기).</p>
     *
     * <p>주인은 겹친 넓이가 큰 칸부터 그 칸의 차례({@link WaterOwners#at})대로 늘어놓고, 앞에 나온 주인은 다시 넣지 않는다.</p>
     */
    private List<Integer> soakingOwners(Player player) {
        int left = player.leftCol();
        int right = player.rightCol();
        int top = player.topRow();
        int bottom = player.bottomRow();
        boolean soaked = false;
        for (int row = top; row <= bottom && !soaked; row++) {
            double wet = 0;
            for (int col = left; col <= right; col++) {
                if (!water.at(col, row).isEmpty()) {
                    wet += player.widthShareIn(col);
                }
            }
            soaked = player.heightShareIn(row) * wet >= SOAK_SHARE - SHARE_SLACK;
        }
        for (int col = left; col <= right && !soaked; col++) {
            double wet = 0;
            for (int row = top; row <= bottom; row++) {
                if (!water.at(col, row).isEmpty()) {
                    wet += player.heightShareIn(row);
                }
            }
            soaked = player.widthShareIn(col) * wet >= SOAK_SHARE - SHARE_SLACK;
        }
        if (!soaked) {
            return List.of();
        }
        record WetCell(double share, List<Integer> owners) { }
        List<WetCell> cells = new ArrayList<>();
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                List<Integer> owners = water.at(col, row);
                if (!owners.isEmpty()) {
                    cells.add(new WetCell(player.widthShareIn(col) * player.heightShareIn(row), owners));
                }
            }
        }
        // 넓이가 같으면 위·왼쪽 칸이 먼저다 (정렬은 같은 값의 순서를 지킨다)
        cells.sort(Comparator.comparingDouble(WetCell::share).reversed());
        Set<Integer> owners = new LinkedHashSet<>();
        cells.forEach(cell -> owners.addAll(cell.owners()));
        return List.copyOf(owners);
    }

    /** player를 맞힌 물줄기 주인들 가운데 가둔 사람으로 칠 주인: 차례에서 맨 앞의 상대, 상대가 없으면 맨 앞 주인 */
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
     * @return 터뜨렸으면 {@link Touch#POP}, 구했으면 {@link Touch#RESCUE}가 들어 있다
     */
    private Set<Touch> settleTouches(Set<Player> justTrapped) {
        Set<Touch> touches = EnumSet.noneOf(Touch.class);
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
                touches.add(Touch.POP);
                continue;
            }
            nearest(trapped, touching).ifPresent(saver -> {
                trapped.escape();
                match.recordSave(saver.id());
                touches.add(Touch.RESCUE);
            });
        }
        return touches;
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
