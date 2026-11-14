package hansung.arcade.ingame.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileTest {

    @Test
    void 캐릭터는_id로_대소문자_구분없이_찾는다() {
        assertEquals(Mascot.KKUKKU, Mascot.fromId(" KKukku "));
        assertEquals(Mascot.HANSUNGNYANGI, Mascot.fromId("hansungnyangi"));
    }

    @Test
    void 없는_캐릭터는_가능한_값을_알려준다() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Mascot.fromId("pikachu"));
        assertTrue(e.getMessage().contains("sangsangbugi"), e.getMessage());
    }

    @Test
    void 닉네임은_앞뒤_공백을_지우고_비어_있으면_안_된다() {
        assertEquals("부기", new PlayerProfile("  부기 ", Mascot.SANGSANGBUGI).nickname());
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile("   ", Mascot.SANGSANGBUGI));
    }

    @Test
    void 임시_정보는_번호와_상관없이_기본_캐릭터를_쓴다() {
        assertEquals(new PlayerProfile("P1", Mascot.DEFAULT), PlayerProfile.placeholder(1));
        assertEquals(Mascot.DEFAULT, PlayerProfile.placeholder(2).mascot());
        assertEquals(PlayerProfile.placeholder(3), new Player(3, 0, 0).profile());
    }
}
