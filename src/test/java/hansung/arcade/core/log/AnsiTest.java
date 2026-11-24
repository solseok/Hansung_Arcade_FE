package hansung.arcade.core.log;

import hansung.arcade.core.ui.HsuColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnsiTest {

    @Test
    void 색을_24비트_글자색_코드로_바꾼다() {
        assertEquals("\u001b[38;2;34;127;196m", Ansi.code(AnsiStyle.of(HsuColor.SKY_BLUE)));
        assertEquals("\u001b[38;2;255;95;95m", Ansi.code(AnsiStyle.rgb(0xFF5F5F)));
    }

    @Test
    void 굵게는_코드_앞에_붙는다() {
        assertEquals("\u001b[1;38;2;255;95;95m", Ansi.code(AnsiStyle.rgb(0xFF5F5F).withBold()));
    }

    @Test
    void 빈_문자열은_칠하지_않는다() {
        assertEquals("", Ansi.paint("", HsuColor.LIGHT_PINK));
    }

    @Test
    void 색을_칠해도_코드를_지우면_원래_글자와_같다() {
        String painted = Ansi.paint("구동 완료", HsuColor.LIGHT_GREEN);

        assertEquals("구동 완료", painted.replaceAll("\u001b\\[[0-9;]*m", ""));
    }
}
