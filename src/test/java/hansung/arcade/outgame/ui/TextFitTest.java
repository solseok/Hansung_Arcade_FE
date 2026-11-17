package hansung.arcade.outgame.ui;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 방 제목은 30자까지 쓸 수 있어 방 칸보다 길 수 있다. 칸을 넘으면 글자 중간이 잘리지 않게 뒤를 줄이고 "…"를 붙인다.
 */
class TextFitTest {

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final Font TITLE = OutgameFonts.gulimCheBold(14);

    @Test
    void 칸을_넘는_글자는_줄이고_말줄임표를_붙인다() {
        String fitted = Shapes.fit("가".repeat(30), TITLE, FRC, LobbyLayout.CARD_TITLE.width());

        assertTrue(fitted.endsWith("…"), fitted);
        assertTrue(TITLE.getStringBounds(fitted, FRC).getWidth() <= LobbyLayout.CARD_TITLE.width() + 0.5, fitted);
    }

    @Test
    void 칸에_들어가는_글자는_그대로_둔다() {
        assertEquals("함께 한 판 해요", Shapes.fit("함께 한 판 해요", TITLE, FRC, LobbyLayout.CARD_TITLE.width()));
    }
}
