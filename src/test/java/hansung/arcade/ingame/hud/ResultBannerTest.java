package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.ResultBanner.Headline;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.MatchOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 결과 띠에 띄울 글씨를 이 컴퓨터의 플레이어 수와 결과로 고르는지 */
class ResultBannerTest {

    private static final TileMap DUO = MapLoader.parse("fixture/duo", List.of("1...2", "....."));
    private static final PlayerProfile ONE = new PlayerProfile("one", Mascot.KKUKKU, 1);
    private static final PlayerProfile TWO = new PlayerProfile("two", Mascot.KKOKKO, 2);

    private static void finish(GameWorld world) {
        for (int i = 0; i < MatchClock.DURATION_SECONDS * MatchClock.TICKS_PER_SECOND && !world.match().isOver(); i++) {
            world.update(1.0 / MatchClock.TICKS_PER_SECOND, Map.of());
        }
    }

    private static Headline headlineOf(GameWorld world) {
        finish(world);
        return ResultBanner.headlineOf(world.match().result().orElseThrow(), world.localPlayers());
    }

    @Test
    void 혼자_하면_내_결과를_그대로_띄운다() {
        assertEquals(new Headline("WIN", MatchOutcome.WIN), headlineOf(new GameWorld(DUO, ONE)));
    }

    @Test
    void 둘이_하다_한쪽만_이기면_이긴_쪽_표시를_붙여_띄운다() {
        GameWorld firstWins = new GameWorld(DUO, List.of(ONE, TWO));
        firstWins.localPlayers().get(1).eliminate();
        assertEquals(new Headline("1P WIN", MatchOutcome.WIN), headlineOf(firstWins));

        GameWorld secondWins = new GameWorld(DUO, List.of(ONE, TWO), MatchMode.TEAM);
        secondWins.localPlayers().get(0).eliminate();
        assertEquals(new Headline("2P WIN", MatchOutcome.WIN), headlineOf(secondWins));
    }

    @Test
    void 둘의_결과가_같으면_그_결과만_띄운다() {
        // 시간이 다 될 때까지 둘 다 살아 있으면 비긴다
        assertEquals(new Headline("DRAW", MatchOutcome.DRAW), headlineOf(new GameWorld(DUO, List.of(ONE, TWO))));
        // 같은 팀이면 함께 이긴다
        GameWorld sameTeam = new GameWorld(DUO, List.of(ONE, new PlayerProfile("two", Mascot.KKOKKO, 1)),
                MatchMode.TEAM);
        assertEquals(new Headline("WIN", MatchOutcome.WIN), headlineOf(sameTeam));
    }
}
