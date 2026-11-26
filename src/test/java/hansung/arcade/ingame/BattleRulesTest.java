package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.ItemEffects;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.Match;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 물줄기 싸움 판정을 게임 상태 없이 한 번씩 불러 본다. 물줄기는 칸마다 주인을 정해 두고,
 * 플레이어는 맵 없이 픽셀 자리에 바로 세운다(칸 하나가 40px).
 */
class BattleRulesTest {

    private static final double TICK = 1.0 / 60;

    private final List<Player> players = new ArrayList<>();
    private final Map<Integer, PlayerGear> gears = new HashMap<>();
    /** "col,row" → 그 칸을 덮은 물줄기의 주인들. 가둔 사람으로 칠 차례대로 */
    private final Map<String, List<Integer>> water = new HashMap<>();

    /** team 편의 플레이어를 (col, row) 칸 가운데에 세운다. 번호는 넣은 차례대로 1부터 */
    private Player join(int team, int col, int row) {
        int id = players.size() + 1;
        Player player = new Player(id, new PlayerProfile("P" + id, Mascot.SANGSANGBUGI, team), col, row);
        players.add(player);
        gears.put(id, new PlayerGear());
        return player;
    }

    private void wet(int col, int row, int... owners) {
        water.put(col + "," + row, IntStream.of(owners).boxed().toList());
    }

    private List<Integer> ownersAt(int col, int row) {
        return water.getOrDefault(col + "," + row, List.of());
    }

    private BattleRules rules(Match match) {
        return new BattleRules(players, match, player -> gears.get(player.id()), this::ownersAt);
    }

    /** 플레이어들의 상태 시간을 seconds초 흘린다 (움직이지는 않는다) */
    private void elapse(double seconds) {
        for (Player player : players) {
            player.update(null, seconds, (col, row) -> false);
        }
    }

    @Test
    void 물줄기가_덮은_칸에_선_사람은_갇히고_가둔_물줄기의_주인을_기억한다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 3, 0);
        wet(0, 0, second.id());
        Match match = new Match(MatchMode.SOLO, players);

        rules(match).settle();

        assertEquals(PlayerState.TRAPPED, first.state());
        assertEquals(second.id(), first.trapperId().orElseThrow());
        assertEquals(PlayerState.NORMAL, second.state());
    }

    /** 걸치기: 크아 개발자 설명대로 충돌 박스가 물줄기와 66% 이상 겹칠 때만 갇힌다. 박스는 36 x 36px, 칸은 40px */
    @Nested
    class 걸치기 {

        @Test
        void 네_칸이_만나는_곳에_서면_십자_물줄기가_세_칸을_덮어도_갇히지_않는다() {
            Player me = join(1, 0, 0);
            Player other = join(2, 5, 5);
            // 1·2열과 1·2줄이 만나는 점에 선다. 박스가 네 칸에 25%씩 들어간다
            me.placeAt(2 * TileMap.TILE_SIZE, 2 * TileMap.TILE_SIZE);
            // 크아 영상처럼 1시 방향 칸(2,1)에서 터져 가로로 (1,1), 세로로 (2,2)까지 덮었다
            wet(2, 1, other.id());
            wet(1, 1, other.id());
            wet(2, 2, other.id());
            wet(3, 1, other.id());
            wet(2, 0, other.id());

            rules(new Match(MatchMode.SOLO, players)).settle();

            assertEquals(PlayerState.NORMAL, me.state());
        }

        @Test
        void 세로_물줄기에_몸이_64퍼센트_걸치면_몸_가운데가_물줄기_칸에_있어도_갇히지_않는다() {
            Player me = join(1, 0, 0);
            Player other = join(2, 5, 5);
            wetColumn(1, other.id());
            // 박스 57~93px: 1열(40~80px)에 23px = 64%. 몸 가운데(75px)는 1열이다
            me.placeAt(75, TileMap.centerY(1));
            assertEquals(1, me.col());

            rules(new Match(MatchMode.SOLO, players)).settle();

            assertEquals(PlayerState.NORMAL, me.state());
        }

        @Test
        void 세로_물줄기에_몸이_67퍼센트_걸치면_갇힌다() {
            Player me = join(1, 0, 0);
            Player other = join(2, 5, 5);
            wetColumn(1, other.id());
            // 박스 56~92px: 1열에 24px = 67%
            me.placeAt(74, TileMap.centerY(1));

            rules(new Match(MatchMode.SOLO, players)).settle();

            assertEquals(PlayerState.TRAPPED, me.state());
            assertEquals(other.id(), me.trapperId().orElseThrow());
        }

        @Test
        void 가로_물줄기도_같다() {
            Player me = join(1, 0, 0);
            Player other = join(2, 5, 5);
            for (int col = 0; col < 4; col++) {
                wet(col, 1, other.id());
            }
            me.placeAt(TileMap.centerX(1), 75);
            Match match = new Match(MatchMode.SOLO, players);
            rules(match).settle();
            assertEquals(PlayerState.NORMAL, me.state());

            me.placeAt(TileMap.centerX(1), 74);
            rules(match).settle();
            assertEquals(PlayerState.TRAPPED, me.state());
        }

        @Test
        void 물줄기_두_칸에_나란히_걸치면_한_줄_전체가_젖어_갇힌다() {
            Player me = join(1, 0, 0);
            Player other = join(2, 5, 5);
            // 가로 물줄기 위에서 두 칸에 반씩 걸쳐도 그 줄에는 몸이 다 들어가 있다
            wet(1, 1, other.id());
            wet(2, 1, other.id());
            me.placeAt(2 * TileMap.TILE_SIZE, TileMap.centerY(1));

            rules(new Match(MatchMode.SOLO, players)).settle();

            assertEquals(PlayerState.TRAPPED, me.state());
        }

        @Test
        void 여러_사람의_물줄기에_맞으면_많이_겹친_칸의_주인이_가둔_사람이다() {
            Player me = join(1, 0, 0);
            Player left = join(2, 5, 5);
            Player right = join(3, 6, 6);
            wet(1, 1, left.id());
            wet(2, 1, right.id());
            // 박스 66~102px: 2열에 22px, 1열에 14px. 줄 하나가 다 젖어 갇히고, 2열 주인이 가둔 사람이다
            me.placeAt(84, TileMap.centerY(1));
            Match match = new Match(MatchMode.SOLO, players);

            rules(match).settle();
            assertEquals(right.id(), me.trapperId().orElseThrow());
        }

        /** col열 0~3줄을 owners의 물줄기로 덮는다 */
        private void wetColumn(int col, int... owners) {
            for (int row = 0; row < 4; row++) {
                wet(col, row, owners);
            }
        }
    }

    @Test
    void 자기_물줄기와_상대_물줄기가_함께_덮은_칸에서는_상대가_가둔_사람이_되어_kill을_얻는다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 3, 0);
        // 차례로는 자기 물줄기가 앞이어도 상대를 먼저 본다
        wet(0, 0, first.id(), second.id());
        Match match = new Match(MatchMode.SOLO, players);
        BattleRules rules = rules(match);

        rules.settle();
        assertEquals(second.id(), first.trapperId().orElseThrow());

        elapse(Player.TRAP_SECONDS);
        rules.settle();
        assertFalse(first.isAlive());
        assertEquals(1, match.kills(second.id()));
    }

    @Test
    void 팀전에서_같은_편과_상대의_물줄기가_함께_덮으면_상대가_가둔_사람이다() {
        Player first = join(1, 0, 0);
        Player ally = join(1, 3, 0);
        Player enemy = join(2, 6, 0);
        wet(0, 0, ally.id(), enemy.id());

        rules(new Match(MatchMode.TEAM, players)).settle();

        assertEquals(enemy.id(), first.trapperId().orElseThrow());
    }

    @Test
    void 상대_물줄기가_여럿이면_주어진_차례에서_맨_앞_상대가_가둔_사람이다() {
        Player first = join(1, 0, 0);
        join(2, 3, 0);
        Player third = join(3, 6, 0);
        wet(0, 0, first.id(), third.id(), 2);

        rules(new Match(MatchMode.SOLO, players)).settle();

        assertEquals(third.id(), first.trapperId().orElseThrow());
    }

    @Test
    void 자기와_같은_편_물줄기뿐이면_맨_앞_주인이_가둔_사람이다() {
        Player first = join(1, 0, 0);
        Player ally = join(1, 3, 0);
        join(2, 6, 0);
        wet(0, 0, ally.id(), first.id());

        rules(new Match(MatchMode.TEAM, players)).settle();

        assertEquals(ally.id(), first.trapperId().orElseThrow());
    }

    @Test
    void 탱크에_타고_있으면_갇히는_대신_탱크가_부서진다() {
        Player first = join(1, 0, 0);
        join(2, 3, 0);
        ItemEffects.apply(first, gears.get(first.id()), ItemType.TANK);
        wet(0, 0, 2);
        BattleRules rules = rules(new Match(MatchMode.SOLO, players));

        rules.settle();
        assertEquals(PlayerState.NORMAL, first.state());
        assertFalse(gears.get(first.id()).hasTank());

        // 부서진 직후에는 같은 물줄기에 다시 맞지 않는다
        rules.settle();
        assertEquals(PlayerState.NORMAL, first.state());
    }

    @Test
    void 갇힌_사람에게_상대가_닿으면_터지고_닿은_사람이_kill을_얻는다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 3, 0);
        Match match = new Match(MatchMode.SOLO, players);
        first.trap(first.id());
        elapse(TICK);
        second.placeAt(first.x() + TileMap.TILE_SIZE / 2.0 - 1, first.y());

        rules(match).settle();

        assertEquals(PlayerState.DYING, first.state());
        assertEquals(1, match.kills(second.id()));
    }

    @Test
    void 팀전에서_갇힌_사람에게_같은_편이_닿으면_구하고_save를_얻는다() {
        Player first = join(1, 0, 0);
        Player second = join(1, 3, 0);
        join(2, 5, 5);
        Match match = new Match(MatchMode.TEAM, players);
        first.trap(first.id());
        elapse(TICK);
        second.placeAt(first.x(), first.y() + TileMap.TILE_SIZE / 2.0 - 1);

        rules(match).settle();

        assertEquals(PlayerState.ESCAPING, first.state());
        assertEquals(1, match.saves(second.id()));
        assertEquals(0, match.kills(second.id()));
    }

    /** 1P는 (2, 0) 칸 아래쪽 끝, 2P는 바로 밑 (2, 1) 칸 위쪽 끝에 세워 반 칸 안으로 붙인다. 1P 칸만 1P의 물줄기가 덮는다 */
    private void touchingAcrossCells(Player first, Player second) {
        // 1P는 0번 줄에 몸이 72% 들어가 물줄기에 맞고(66% 이상), 2P는 1번 줄에 서서 칸 경계 너머로 1P에 닿는다
        first.placeAt(TileMap.centerX(2), TileMap.TILE_SIZE - 8);
        second.placeAt(TileMap.centerX(2), TileMap.TILE_SIZE + 8);
        assertEquals(0, first.row());
        assertEquals(1, second.row());
        assertTrue(second.touches(first));
        wet(2, 0, first.id());
    }

    @Test
    void 갇히는_틱에_이미_닿아_있던_상대는_그_틱에는_터뜨리지_않고_다음_틱에_터뜨린다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 0, 0);
        touchingAcrossCells(first, second);
        Match match = new Match(MatchMode.SOLO, players);
        BattleRules rules = rules(match);

        rules.settle();
        assertEquals(PlayerState.TRAPPED, first.state());
        assertEquals(0, match.kills(second.id()));

        elapse(TICK);
        rules.settle();
        assertEquals(PlayerState.DYING, first.state());
        assertEquals(1, match.kills(second.id()));
    }

    @Test
    void 갇히는_틱에_이미_닿아_있던_팀원은_그_틱에는_구하지_않고_다음_틱에_구한다() {
        Player first = join(1, 0, 0);
        Player second = join(1, 0, 0);
        join(2, 5, 5);
        touchingAcrossCells(first, second);
        Match match = new Match(MatchMode.TEAM, players);
        BattleRules rules = rules(match);

        rules.settle();
        assertEquals(PlayerState.TRAPPED, first.state());
        assertEquals(0, match.saves(second.id()));

        elapse(TICK);
        rules.settle();
        assertEquals(PlayerState.ESCAPING, first.state());
        assertEquals(1, match.saves(second.id()));
    }

    @Test
    void 같은_틱에_팀원과_상대가_함께_닿으면_번호와_상관없이_터뜨린다() {
        // 팀원의 번호가 상대보다 작을 때와 클 때 모두
        for (boolean allyFirst : new boolean[]{true, false}) {
            players.clear();
            gears.clear();
            Player trapped = join(1, 2, 2);
            Player ally;
            Player enemy;
            if (allyFirst) {
                ally = join(1, 0, 0);
                enemy = join(2, 0, 0);
            } else {
                enemy = join(2, 0, 0);
                ally = join(1, 0, 0);
            }
            Match match = new Match(MatchMode.TEAM, players);
            trapped.trap(trapped.id());
            // 팀원이 상대보다 더 가까이 붙어 있다
            ally.placeAt(trapped.x() - 5, trapped.y());
            enemy.placeAt(trapped.x() + 15, trapped.y());

            Set<BattleRules.Touch> touches = rules(match).settle();

            String when = allyFirst ? "팀원 번호가 작을 때" : "팀원 번호가 클 때";
            assertEquals(Set.of(BattleRules.Touch.POP), touches, when);
            assertEquals(PlayerState.DYING, trapped.state(), when);
            assertEquals(1, match.kills(enemy.id()), when);
            assertEquals(0, match.saves(ally.id()), when);
        }
    }

    @Test
    void 한_틱에_한_사람은_터뜨리고_다른_사람은_구하면_둘_다_알린다() {
        Player popped = join(1, 0, 0);
        Player saved = join(1, 5, 0);
        Player enemy = join(2, 0, 3);
        Player ally = join(1, 5, 3);
        Match match = new Match(MatchMode.TEAM, players);
        popped.trap(popped.id());
        saved.trap(saved.id());
        enemy.placeAt(popped.x() + TileMap.TILE_SIZE / 2.0 - 1, popped.y());
        ally.placeAt(saved.x() + TileMap.TILE_SIZE / 2.0 - 1, saved.y());

        Set<BattleRules.Touch> touches = rules(match).settle();

        assertEquals(Set.of(BattleRules.Touch.POP, BattleRules.Touch.RESCUE), touches);
        assertEquals(PlayerState.DYING, popped.state());
        assertEquals(PlayerState.ESCAPING, saved.state());
    }

    @Test
    void 닿은_사람이_없으면_시간이_다_돼_탈락해도_알릴_일이_없다() {
        Player trapped = join(1, 0, 0);
        join(2, 5, 5);
        trapped.trap(trapped.id());
        elapse(Player.TRAP_SECONDS);

        Set<BattleRules.Touch> touches = rules(new Match(MatchMode.SOLO, players)).settle();

        assertFalse(trapped.isAlive());
        assertEquals(Set.of(), touches);
    }

    @Test
    void 상대가_여럿_닿으면_가장_가까운_상대가_kill을_얻는다() {
        Player trapped = join(1, 2, 2);
        Player far = join(2, 0, 0);
        Player near = join(3, 0, 0);
        Match match = new Match(MatchMode.SOLO, players);
        trapped.trap(trapped.id());
        far.placeAt(trapped.x() - 15, trapped.y());
        near.placeAt(trapped.x(), trapped.y() + 10);

        rules(match).settle();

        assertEquals(PlayerState.DYING, trapped.state());
        assertEquals(1, match.kills(near.id()));
        assertEquals(0, match.kills(far.id()));
    }

    @Test
    void 같은_거리에서_여럿_닿으면_번호가_작은_사람이_얻는다() {
        Player trapped = join(1, 2, 2);
        Player second = join(2, 0, 0);
        Player third = join(3, 0, 0);
        Match match = new Match(MatchMode.SOLO, players);
        trapped.trap(trapped.id());
        third.placeAt(trapped.x() + 10, trapped.y());
        second.placeAt(trapped.x() - 10, trapped.y());

        rules(match).settle();

        assertEquals(1, match.kills(second.id()));
        assertEquals(0, match.kills(third.id()));
    }

    @Test
    void 반_칸_넘게_떨어져_있으면_닿지_않는다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 3, 0);
        Match match = new Match(MatchMode.SOLO, players);
        first.trap(first.id());
        elapse(TICK);
        second.placeAt(first.x() + TileMap.TILE_SIZE / 2.0, first.y());

        rules(match).settle();

        assertEquals(PlayerState.TRAPPED, first.state());
    }

    @Test
    void 갇힌_채_5초가_지나면_탈락하고_가둔_주인이_상대면_kill을_얻는다() {
        Player first = join(1, 0, 0);
        Player second = join(2, 3, 0);
        Player third = join(3, 6, 0);
        Match match = new Match(MatchMode.SOLO, players);
        first.trap(second.id());
        third.trap(third.id());

        elapse(Player.TRAP_SECONDS - TICK);
        rules(match).settle();
        assertEquals(PlayerState.TRAPPED, first.state());

        elapse(TICK);
        rules(match).settle();
        assertEquals(PlayerState.DYING, first.state());
        assertEquals(PlayerState.DYING, third.state());
        assertEquals(1, match.kills(second.id()));
        assertEquals(0, match.kills(third.id()), "자기 물줄기에 갇혀 탈락하면 아무도 kill을 얻지 않는다");
    }

    @Test
    void 팀전에서_같은_편_물줄기에_갇혀_탈락하면_아무도_kill을_얻지_않는다() {
        Player first = join(1, 0, 0);
        Player second = join(1, 3, 0);
        join(2, 6, 0);
        Match match = new Match(MatchMode.TEAM, players);
        first.trap(second.id());

        elapse(Player.TRAP_SECONDS);
        rules(match).settle();

        assertFalse(first.isAlive());
        assertEquals(0, match.kills(second.id()));
        assertTrue(second.isAlive());
    }
}
