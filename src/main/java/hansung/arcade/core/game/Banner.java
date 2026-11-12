package hansung.arcade.core.game;

import hansung.arcade.core.log.Ansi;
import hansung.arcade.core.ui.HsuColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 구동 시작 시 콘솔에 resources/banner.txt를 출력한다.
 * 로거로 찍으면 줄마다 시간·레벨 접두어가 붙어 그림이 깨지므로 표준 출력에 그대로 쓴다.
 *
 * <p>모든 줄이 공백인 열(WORD_GAP칸 넘게 연속)을 단어 경계로 보고, 단어마다 HSU 색을 번갈아 칠한다.
 * banner.txt를 바꿔도 경계는 자동으로 다시 찾는다.</p>
 */
final class Banner {

    private static final Logger log = LoggerFactory.getLogger(Banner.class);
    private static final String RESOURCE = "/banner.txt";
    private static final HsuColor[] WORD_COLORS = {HsuColor.SKY_BLUE, HsuColor.LIGHT_BLUE};
    private static final int WORD_GAP = 4;

    private Banner() {
    }

    static void print(Class<?> resourceAnchor, PrintStream out) {
        try (InputStream in = resourceAnchor.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return;
            }
            List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).stripTrailing().lines().toList();
            List<Range> words = wordRanges(lines);
            for (String line : lines) {
                out.println(paintLine(line, words));
            }
            out.println();
        } catch (IOException e) {
            // 배너는 장식이라 못 읽어도 구동은 계속한다.
            log.warn("배너를 읽지 못했습니다: {}", e.getMessage());
        }
    }

    /** 단어가 차지하는 열 범위 [start, end) */
    record Range(int start, int end) {
    }

    static List<Range> wordRanges(List<String> lines) {
        int width = lines.stream().mapToInt(String::length).max().orElse(0);
        boolean[] used = new boolean[width];
        for (String line : lines) {
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) != ' ') {
                    used[col] = true;
                }
            }
        }

        List<Range> ranges = new ArrayList<>();
        int start = -1;
        int lastUsed = -1;
        for (int col = 0; col < width; col++) {
            if (!used[col]) {
                continue;
            }
            if (start < 0) {
                start = col;
            } else if (col - lastUsed > WORD_GAP) {
                ranges.add(new Range(start, lastUsed + 1));
                start = col;
            }
            lastUsed = col;
        }
        if (start >= 0) {
            ranges.add(new Range(start, lastUsed + 1));
        }
        return ranges;
    }

    static String paintLine(String line, List<Range> words) {
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (int i = 0; i < words.size() && cursor < line.length(); i++) {
            int start = Math.min(words.get(i).start(), line.length());
            int end = Math.min(words.get(i).end(), line.length());
            sb.append(line, cursor, start);
            sb.append(Ansi.paint(line.substring(start, end), WORD_COLORS[i % WORD_COLORS.length]));
            cursor = end;
        }
        sb.append(line.substring(cursor));
        return sb.toString();
    }
}
