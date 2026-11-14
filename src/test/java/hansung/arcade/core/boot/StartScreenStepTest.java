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
}
