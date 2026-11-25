package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameProperties;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartScreenStepTest {

    @Test
    void 내_닉네임과_캐릭터는_설정값으로_정한다() {
        GameProperties properties = GameProperties.of(Map.of("player.nickname", "기본"),
                List.of("--player.nickname=부기", "--player.character=kkokko"));

        assertEquals(new PlayerProfile("부기", Mascot.KKOKKO), StartScreenStep.localProfile(properties));
    }

    @Test
    void 설정이_없으면_기본_닉네임과_상상부기를_쓴다() {
        PlayerProfile profile = StartScreenStep.localProfile(GameProperties.of(Map.of(), List.of()));

        assertEquals(Mascot.SANGSANGBUGI, profile.mascot());
        assertEquals("Player", profile.nickname());
    }

    @Test
    void 닉네임을_비워_두면_기본_닉네임을_쓴다() {
        GameProperties properties = GameProperties.of(Map.of(), List.of("--player.nickname=   "));
        assertEquals("Player", StartScreenStep.localProfile(properties).nickname());
    }

    @Test
    void 고정색은_설정값_player_color로_정한다() {
        GameProperties properties = GameProperties.of(Map.of("player.color", ""),
                List.of("--player.character=sangzzi", "--player.color=Magenta"));

        assertEquals(new PlayerProfile("Player", Mascot.SANGZZI, CharacterColor.MAGENTA),
                StartScreenStep.localProfile(properties));
    }

    @Test
    void 고정색을_비워_두거나_적지_않으면_원래_색이다() {
        assertNull(StartScreenStep.localProfile(GameProperties.of(Map.of("player.color", ""), List.of())).color());
        assertNull(StartScreenStep.localProfile(GameProperties.of(Map.of(), List.of("--player.color=  "))).color());
        assertNull(StartScreenStep.localProfile(GameProperties.of(Map.of(), List.of())).color());
    }

    @Test
    void 없는_색을_적으면_설정_이름과_가능한_색을_알려_주며_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("player.color", "pink"), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.localProfile(properties));
        assertTrue(e.getMessage().contains("player.color=pink"), e.getMessage());
        assertTrue(e.getMessage().contains("sky"), e.getMessage());
    }

    @Test
    void 없는_캐릭터를_적으면_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("player.character", "nobody"), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.localProfile(properties));
        assertTrue(e.getMessage().contains("player.character"), e.getMessage());
    }

    @Test
    void 팀은_설정값_player_team으로_정하고_없으면_1팀이다() {
        GameProperties properties = GameProperties.of(Map.of(), List.of("--player.team=2"));
        assertEquals(2, StartScreenStep.localProfile(properties).team());

        assertEquals(1, StartScreenStep.localProfile(GameProperties.of(Map.of(), List.of())).team());
    }

    @Test
    void 팀_번호가_1보다_작으면_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("player.team", "0"), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.localProfile(properties));
        assertTrue(e.getMessage().contains("player.team"), e.getMessage());
    }

    @Test
    void 경기_방식은_설정값_game_match_mode로_정하고_없으면_개인전이다() {
        assertEquals(MatchMode.SOLO, StartScreenStep.matchMode(GameProperties.of(Map.of(), List.of())));
        assertEquals(MatchMode.TEAM, StartScreenStep.matchMode(
                GameProperties.of(Map.of("game.match-mode", "solo"), List.of("--game.match-mode=team"))));
    }

    @Test
    void 없는_경기_방식을_적으면_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("game.match-mode", "duo"), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.matchMode(properties));
        assertTrue(e.getMessage().contains("game.match-mode"), e.getMessage());
    }

    @Test
    void 인원을_정하지_않으면_혼자_한다() {
        GameProperties properties = GameProperties.of(Map.of("player.nickname", "부기"), List.of());

        assertEquals(List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI)), StartScreenStep.localProfiles(properties));
    }

    @Test
    void 실행_인자로_2명을_주면_2P는_player2_설정을_쓴다() {
        GameProperties properties = GameProperties.of(Map.of("game.local-players", "1"),
                List.of("--game.local-players=2", "--player.nickname=부기", "--player2.nickname=꼬꼬",
                        "--player2.character=kkokko"));

        assertEquals(List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI), new PlayerProfile("꼬꼬", Mascot.KKOKKO, 2)),
                StartScreenStep.localProfiles(properties));
    }

    @Test
    void 설정이_없으면_2P는_Player2와_상상부기를_쓴다() {
        GameProperties properties = GameProperties.of(Map.of(), List.of("--game.local-players=2"));

        assertEquals(new PlayerProfile("Player2", Mascot.SANGSANGBUGI, 2), StartScreenStep.localProfiles(properties).get(1));
    }

    @Test
    void 인원이_1이나_2가_아니면_설정_이름과_함께_실패한다() {
        for (String count : List.of("0", "3", "two")) {
            GameProperties properties = GameProperties.of(Map.of(), List.of("--game.local-players=" + count));

            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> StartScreenStep.localProfiles(properties));
            assertTrue(e.getMessage().contains("game.local-players"), e.getMessage());
        }
    }

    @Test
    void 잘못된_2P_캐릭터는_player2_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of(),
                List.of("--game.local-players=2", "--player2.character=nobody"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.localProfiles(properties));
        assertTrue(e.getMessage().contains("player2.character"), e.getMessage());
    }

    @Test
    void 아이템_시험_모드는_설정값_game_test_items로_켜고_없으면_끈다() {
        assertFalse(StartScreenStep.testItems(GameProperties.of(Map.of(), List.of())));
        assertFalse(StartScreenStep.testItems(GameProperties.of(Map.of("game.test-items", "false"), List.of())));
        assertTrue(StartScreenStep.testItems(GameProperties.of(Map.of("game.test-items", "false"),
                List.of("--game.test-items=true"))));
        assertTrue(StartScreenStep.testItems(GameProperties.of(Map.of(), List.of("--game.test-items=TRUE"))));
    }

    @Test
    void 아이템_시험_모드에_true_false가_아닌_값을_적으면_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of(), List.of("--game.test-items=yes"));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> StartScreenStep.testItems(properties));
        assertTrue(error.getMessage().contains("game.test-items=yes"));
    }
}
