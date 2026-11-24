package hansung.arcade.core.log;

import hansung.arcade.core.ui.HsuColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StackTraceColorConverterTest {

    private static final String ANSI_CODE = "\u001b\\[[0-9;]*m";
    private static final String TRACE = """
            java.lang.IllegalStateException: 시작 실패
            \tat hansung.arcade.core.boot.ResourceStep.run(ResourceStep.java:24)
            \tat hansung.arcade.core.GameApplication.boot(GameApplication.java:66)
            Caused by: java.lang.IllegalArgumentException: 맵 파일을 찾을 수 없습니다
            \tat hansung.arcade.ingame.map.MapLoader.loadResource(MapLoader.java:38)
            \t... 2 common frames omitted
            """;

    @Test
    void 호출_줄과_생략_줄을_구분한다() {
        assertTrue(StackTraceColorConverter.isFrame("\tat hansung.arcade.core.GameApplication.boot(GameApplication.java:66)"));
        assertTrue(StackTraceColorConverter.isFrame("\t... 2 common frames omitted"));
        assertFalse(StackTraceColorConverter.isFrame("java.lang.IllegalStateException: 시작 실패"));
        assertFalse(StackTraceColorConverter.isFrame("Caused by: java.lang.IllegalArgumentException: 없음"));
    }

    @Test
    void 색을_칠해도_글자와_줄바꿈은_그대로다() {
        String painted = StackTraceColorConverter.paintTrace(TRACE, LevelColorConverter.ERROR);

        assertEquals(TRACE, painted.replaceAll(ANSI_CODE, ""));
    }

    @Test
    void 예외_줄은_레벨_색_호출_줄은_회색이다() {
        assumeTrue(Ansi.isEnabled(), "NO_COLOR 환경에서는 색 코드를 검사하지 않는다");
        String header = Ansi.code(LevelColorConverter.ERROR);
        String frame = Ansi.code(AnsiStyle.of(HsuColor.GRAY));

        String[] lines = StackTraceColorConverter.paintTrace(TRACE, LevelColorConverter.ERROR).split("\n");

        assertTrue(lines[0].startsWith(header + "java.lang.IllegalStateException"));
        assertTrue(lines[1].startsWith(frame + "\tat "));
        assertTrue(lines[3].startsWith(header + "Caused by:"));
        assertTrue(lines[5].startsWith(frame + "\t... 2 common"));
    }

    @Test
    void 경고가_아니면_예외_줄은_칠하지_않는다() {
        String[] lines = StackTraceColorConverter.paintTrace(TRACE, null).split("\n");

        assertEquals("java.lang.IllegalStateException: 시작 실패", lines[0]);
    }
}
