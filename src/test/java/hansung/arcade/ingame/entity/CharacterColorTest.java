package hansung.arcade.ingame.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterColorTest {

    @Test
    void 하늘과_마젠타는_정해_준_sRGB_그대로다() {
        assertEquals(rgb(83, 182, 195), CharacterColor.SKY.rgb());
        assertEquals(rgb(213, 47, 143), CharacterColor.MAGENTA.rgb());
    }

    @Test
    void 나머지_색은_주석에_적은_sRGB가_나온다() {
        assertEquals(0xE23532, CharacterColor.RED.rgb());
        assertEquals(0xF6D73C, CharacterColor.YELLOW.rgb());
        assertEquals(0xF58028, CharacterColor.ORANGE.rgb());
        assertEquals(0x3DB44A, CharacterColor.GREEN.rgb());
        assertEquals(0x2867E4, CharacterColor.BLUE.rgb());
        assertEquals(0x8A45D5, CharacterColor.PURPLE.rgb());
    }

    @Test
    void 노랑은_다른_색보다_밝다() {
        for (CharacterColor color : CharacterColor.values()) {
            if (color != CharacterColor.YELLOW) {
                assertTrue(CharacterColor.YELLOW.oklch().l() > color.oklch().l() + 0.1, color.id());
            }
        }
    }

    @Test
    void 설정에_쓰는_id는_여덟_가지다() {
        assertEquals(List.of("red", "yellow", "orange", "green", "sky", "blue", "purple", "magenta"),
                Arrays.stream(CharacterColor.values()).map(CharacterColor::id).toList());
    }

    @Test
    void 색은_id로_대소문자_구분없이_찾는다() {
        assertEquals(CharacterColor.SKY, CharacterColor.fromId(" Sky "));
        assertEquals(CharacterColor.MAGENTA, CharacterColor.fromId("MAGENTA"));
    }

    @Test
    void 없는_색은_가능한_값을_알려준다() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CharacterColor.fromId("pink"));
        assertTrue(e.getMessage().contains("'pink'"), e.getMessage());
        assertTrue(e.getMessage().contains("red, yellow, orange, green, sky, blue, purple, magenta"), e.getMessage());
    }

    private static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}
