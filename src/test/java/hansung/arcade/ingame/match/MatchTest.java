package hansung.arcade.ingame.match;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static hansung.arcade.ingame.match.MatchOutcome.DRAW;
import static hansung.arcade.ingame.match.MatchOutcome.LOSE;
import static hansung.arcade.ingame.match.MatchOutcome.WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {

    private static final double TICK = 1.0 / 60;
    /** 한 판 전체의 틱 수 */
    private static final int FULL_TICKS = MatchClock.DURATION_SECONDS * MatchClock.TICKS_PER_SECOND;

    // ── 개인전 ─────────────────────────────────────────────

    @Test
    void 개인전에서_한_명만_남으면_바로_끝나고_그_사람만_이긴다() {
        Player a = player(1);
        Player b = player(2);
        Player c = player(3);
        Match match = new Match(MatchMode.SOLO, List.of(a, b, c));
        tick(match, 600);

        b.eliminate();
        tick(match, 1);
        assertFalse(match.isOver());

        c.eliminate();
        tick(match, 1);

        assertTrue(match.isOver());
        assertOutcomes(match, WIN, LOSE, LOSE);
        assertEquals(602 / 60.0, match.result().orElseThrow().playedSeconds(), 1e-9);
    }

    @Test
    void 개인전에서_마지막_둘이_한꺼번에_탈락하면_모두_비긴다() {
        Player a = player(1);
        Player b = player(2);
        Match match = new Match(MatchMode.SOLO, List.of(a, b));

        a.eliminate();
        b.eliminate();
        tick(match, 1);

        assertOutcomes(match, DRAW, DRAW);
    }

    @Test
    void 개인전에서_시간이_다_될_때_둘_이상_살아_있으면_탈락한_사람까지_모두_비긴다() {
        Player a = player(1);
        Player b = player(2);
        Player c = player(3);
        Match match = new Match(MatchMode.SOLO, List.of(a, b, c));
        c.eliminate();

        tick(match, FULL_TICKS - 1);
        assertFalse(match.isOver());
        tick(match, 1);

        assertTrue(match.isOver());
        assertOutcomes(match, DRAW, DRAW, DRAW);
        assertEquals(MatchClock.DURATION_SECONDS, match.result().orElseThrow().playedSeconds());
    }

    @Test
    void 개인전은_팀_번호가_같아도_각자_한_편이다() {
        Player a = player(1, 1);
        Player b = player(2, 1);
        Match match = new Match(MatchMode.SOLO, List.of(a, b));

        b.eliminate();
        tick(match, 1);

        assertOutcomes(match, WIN, LOSE);
    }

    @Test
    void 혼자_시작하면_일찍_끝나지_않고_시간이_다_될_때_살아_있으면_이긴다() {
        Match match = new Match(MatchMode.SOLO, List.of(player(1)));

        tick(match, FULL_TICKS - 1);
        assertFalse(match.isOver());
        tick(match, 1);

        assertOutcomes(match, WIN);
    }

    @Test
    void 혼자_시작해서_탈락해도_시간이_다_될_때까지_이어지고_비긴다() {
        Player alone = player(1);
        Match match = new Match(MatchMode.SOLO, List.of(alone));

        alone.eliminate();
        tick(match, FULL_TICKS - 1);
        assertFalse(match.isOver());
        tick(match, 1);

        assertOutcomes(match, DRAW);
    }

    // ── 팀전 ──────────────────────────────────────────────

    @Test
    void 팀전에서_한_팀만_남으면_바로_끝나고_먼저_탈락한_팀원도_함께_이긴다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Player b1 = player(3, 2);
        Player b2 = player(4, 2);
        Match match = new Match(MatchMode.TEAM, List.of(a1, a2, b1, b2));

        a2.eliminate();
        b1.eliminate();
        tick(match, 1);
        assertFalse(match.isOver());

        b2.eliminate();
        tick(match, 1);

        assertOutcomes(match, WIN, WIN, LOSE, LOSE);
    }

    @Test
    void 팀전에서_세_팀_중_두_팀이_탈락해야_끝난다() {
        Player a = player(1, 1);
        Player b = player(2, 2);
        Player c = player(3, 3);
        Match match = new Match(MatchMode.TEAM, List.of(a, b, c));

        c.eliminate();
        tick(match, 1);
        assertFalse(match.isOver());

        a.eliminate();
        tick(match, 1);

        assertOutcomes(match, LOSE, WIN, LOSE);
    }

    @Test
    void 팀전에서_마지막_두_팀이_한꺼번에_전멸하면_모두_비긴다() {
        Player a = player(1, 1);
        Player b = player(2, 2);
        Match match = new Match(MatchMode.TEAM, List.of(a, b));

        a.eliminate();
        b.eliminate();
        tick(match, 1);

        assertOutcomes(match, DRAW, DRAW);
    }

    @Test
    void 팀전에서_시간이_다_될_때_두_팀_이상_살아_있으면_모두_비긴다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Player b1 = player(3, 2);
        Match match = new Match(MatchMode.TEAM, List.of(a1, a2, b1));
        a1.eliminate();

        tick(match, FULL_TICKS);

        assertOutcomes(match, DRAW, DRAW, DRAW);
    }

    @Test
    void 팀전을_한_팀으로만_시작하면_팀원이_탈락해도_시간이_다_될_때까지_이어지고_그_팀이_이긴다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Match match = new Match(MatchMode.TEAM, List.of(a1, a2));

        a2.eliminate();
        tick(match, FULL_TICKS - 1);
        assertFalse(match.isOver());
        tick(match, 1);

        assertOutcomes(match, WIN, WIN);
    }

    @Test
    void 팀전을_한_팀으로만_시작해서_모두_탈락하면_시간이_다_될_때_비긴다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Match match = new Match(MatchMode.TEAM, List.of(a1, a2));

        a1.eliminate();
        a2.eliminate();
        tick(match, FULL_TICKS - 1);
        assertFalse(match.isOver());
        tick(match, 1);

        assertOutcomes(match, DRAW, DRAW);
    }

    // ── 끝난 뒤 ──────────────────────────────────────────

    @Test
    void 일찍_끝나면_시계가_멈추고_결과가_더는_바뀌지_않는다() {
        Player a = player(1);
        Player b = player(2);
        Match match = new Match(MatchMode.SOLO, List.of(a, b));
        tick(match, 90);
        b.eliminate();
        tick(match, 1);
        int remaining = match.clock().remainingSeconds();
        MatchResult result = match.result().orElseThrow();

        a.eliminate();
        tick(match, FULL_TICKS);

        assertEquals(remaining, match.clock().remainingSeconds());
        assertEquals(179, remaining);
        assertTrue(match.clock().isStopped());
        assertEquals(result, match.result().orElseThrow());
        assertOutcomes(match, WIN, LOSE);
    }

    @Test
    void 끝나기_전에는_결과가_없다() {
        Match match = new Match(MatchMode.SOLO, List.of(player(1), player(2)));
        tick(match, 60);

        assertFalse(match.isOver());
        assertTrue(match.result().isEmpty());
    }

    // ── kill · save ───────────────────────────────────────

    @Test
    void kill과_save는_플레이어마다_세고_결과에_플레이어_번호_순으로_담긴다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Player b1 = player(3, 2);
        // 넘겨준 순서와 상관없이 번호 순
        Match match = new Match(MatchMode.TEAM, List.of(b1, a2, a1));

        match.recordKill(1);
        match.recordKill(1);
        match.recordSave(2);
        match.recordKill(3);
        assertEquals(2, match.kills(1));
        assertEquals(1, match.saves(2));
        assertEquals(0, match.saves(1));

        b1.eliminate();
        tick(match, 1);

        MatchResult result = match.result().orElseThrow();
        assertEquals(MatchMode.TEAM, result.mode());
        assertEquals(List.of(
                new PlayerResult(1, WIN, 2, 0),
                new PlayerResult(2, WIN, 0, 1),
                new PlayerResult(3, LOSE, 1, 0)), result.players());
        assertEquals(new PlayerResult(2, WIN, 0, 1), result.of(2));
    }

    @Test
    void 경기가_끝난_뒤의_kill과_save는_세지_않는다() {
        Player a = player(1);
        Player b = player(2);
        Match match = new Match(MatchMode.SOLO, List.of(a, b));
        b.eliminate();
        tick(match, 1);

        match.recordKill(1);
        match.recordSave(1);

        assertEquals(0, match.kills(1));
        assertEquals(0, match.saves(1));
        assertEquals(new PlayerResult(1, WIN, 0, 0), match.result().orElseThrow().of(1));
    }

    @Test
    void 이번_판에_없는_플레이어는_기록할_수_없다() {
        Match match = new Match(MatchMode.SOLO, List.of(player(1)));

        assertThrows(IllegalArgumentException.class, () -> match.recordKill(9));
        assertThrows(IllegalArgumentException.class, () -> match.recordSave(9));
        assertThrows(IllegalArgumentException.class, () -> match.kills(9));
    }

    @Test
    void 플레이어_번호가_겹치면_경기를_만들_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new Match(MatchMode.SOLO, List.of(player(1), player(1))));
    }

    @Test
    void 경기_방식은_설정값_이름으로_대소문자_구분없이_찾는다() {
        assertEquals(MatchMode.SOLO, MatchMode.fromId("solo"));
        assertEquals(MatchMode.TEAM, MatchMode.fromId(" TEAM "));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> MatchMode.fromId("duo"));
        assertTrue(e.getMessage().contains("solo, team"), e.getMessage());
    }

    private static Player player(int id) {
        return player(id, PlayerProfile.DEFAULT_TEAM);
    }

    @Test
    void 같은_편은_개인전에서는_자기_자신뿐이고_팀전에서는_팀_번호가_같은_사람이다() {
        Player a1 = player(1, 1);
        Player a2 = player(2, 1);
        Player b1 = player(3, 2);

        Match solo = new Match(MatchMode.SOLO, List.of(a1, a2, b1));
        assertTrue(solo.isAlly(a1, a1));
        assertFalse(solo.isAlly(a1, a2), "개인전은 팀 번호가 같아도 상대다");

        Match team = new Match(MatchMode.TEAM, List.of(a1, a2, b1));
        assertTrue(team.isAlly(a1, a2));
        assertFalse(team.isAlly(a2, b1));
    }

    private static Player player(int id, int team) {
        return new Player(id, new PlayerProfile("P" + id, Mascot.DEFAULT, team), 0, 0);
    }

    /** 게임 루프처럼 1/60초씩 n번 갱신한다 */
    private static void tick(Match match, int n) {
        for (int i = 0; i < n; i++) {
            match.update(TICK);
        }
    }

    /** 플레이어 번호 순(1, 2, ...)의 승패 */
    private static void assertOutcomes(Match match, MatchOutcome... expected) {
        assertTrue(match.isOver(), "경기가 끝나지 않았다");
        List<MatchOutcome> actual = match.result().orElseThrow().players().stream().map(PlayerResult::outcome).toList();
        assertEquals(List.of(expected), actual);
    }
}
