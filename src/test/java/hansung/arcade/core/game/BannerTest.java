package hansung.arcade.core.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannerTest {

    private static final String ANSI_CODE = "\u001b\\[[0-9;]*m";

    @Test
    void 모든_줄이_공백인_넓은_열을_단어_경계로_본다() {
        List<String> lines = List.of(
                "AB  C      DE",
                " B   C     D ");

        List<Banner.Range> words = Banner.wordRanges(lines);

        // A~C 사이 공백(2~3칸)은 같은 단어, C와 D 사이 공백(5칸 이상)은 경계
        assertEquals(List.of(new Banner.Range(0, 6), new Banner.Range(11, 13)), words);
    }

    @Test
    void 짧은_줄도_색만_입히고_글자는_그대로_둔다() {
        List<String> lines = List.of(
                "AAAA        BBBB",
                "  A");
        List<Banner.Range> words = Banner.wordRanges(lines);

        for (String line : lines) {
            assertEquals(line, Banner.paintLine(line, words).replaceAll(ANSI_CODE, ""));
        }
    }
}
