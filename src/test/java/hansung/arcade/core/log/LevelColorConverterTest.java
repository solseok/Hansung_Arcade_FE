package hansung.arcade.core.log;

import ch.qos.logback.classic.Level;
import hansung.arcade.core.ui.HsuColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelColorConverterTest {

    @Test
    void ERROR는_굵은_빨강_WARN은_주황이다() {
        assertEquals(AnsiStyle.rgb(0xFF5F5F).withBold(), LevelColorConverter.styleOf(Level.ERROR));
        assertEquals(AnsiStyle.rgb(0xFFAF00), LevelColorConverter.styleOf(Level.WARN));
    }

    @Test
    void 나머지_레벨은_HSU_색이다() {
        assertEquals(AnsiStyle.of(HsuColor.LIGHT_GREEN), LevelColorConverter.styleOf(Level.INFO));
        assertEquals(AnsiStyle.of(HsuColor.LIGHT_BLUE), LevelColorConverter.styleOf(Level.DEBUG));
        assertEquals(AnsiStyle.of(HsuColor.LIGHT_GRAY), LevelColorConverter.styleOf(Level.TRACE));
    }

    @Test
    void WARN_이상만_경고로_본다() {
        assertTrue(LevelColorConverter.isAlert(Level.ERROR));
        assertTrue(LevelColorConverter.isAlert(Level.WARN));
        assertFalse(LevelColorConverter.isAlert(Level.INFO));
    }
}
