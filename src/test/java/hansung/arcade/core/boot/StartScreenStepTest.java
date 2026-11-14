package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameProperties;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void 없는_캐릭터를_적으면_설정_이름과_함께_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("player.character", "nobody"), List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> StartScreenStep.localProfile(properties));
        assertTrue(e.getMessage().contains("player.character"), e.getMessage());
    }
}
