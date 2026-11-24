package hansung.arcade.core.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 커밋하지 않는 개인 설정 파일 {@code .env}를 읽는다. 서버 주소처럼 사람마다 다른 값을 둔다.
 *
 * <pre>
 * # 주석
 * SERVER_HOST=localhost
 * export SERVER_PORT="9000"
 * </pre>
 *
 * 한 줄에 {@code 이름=값} 하나다. 빈 줄과 {@code #}으로 시작하는 줄은 건너뛰고, 앞의 {@code export}와 값을 감싼 따옴표는 뗀다.
 * 따옴표 없는 값 뒤의 {@code " #"} 주석과 파일 맨 앞의 BOM도 뗀다.
 * UTF-8이 아닌 글자는 대체 문자로 읽고, 파일을 읽지 못하면 경고만 남긴다 (오프라인 실행은 .env 없이도 된다).
 */
public final class DotEnv {

    private static final Logger log = LoggerFactory.getLogger(DotEnv.class);

    /** 실행하는 폴더(프로젝트 폴더)의 .env */
    public static final Path DEFAULT_PATH = Path.of(".env");

    private DotEnv() {
    }

    /** 파일이 없거나 읽지 못하면 빈 Map */
    public static Map<String, String> read(Path path) {
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        try {
            // new String은 UTF-8이 아닌 바이트를 대체 문자로 바꾼다 (윈도 메모장 ANSI로 저장한 주석 등)
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return parse(List.of(text.split("\\R", -1)));
        } catch (IOException e) {
            log.warn("{}를 읽지 못해 쓰지 않습니다: {}", path, e.toString());
            return Map.of();
        }
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.replace("\uFEFF", "").strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            values.put(line.substring(0, eq).strip(), value(line.substring(eq + 1).strip()));
        }
        return values;
    }

    /** 따옴표로 감쌌으면 벗기고, 아니면 뒤의 " #" 주석을 뗀다 */
    private static String value(String raw) {
        String unquoted = unquote(raw);
        if (!unquoted.equals(raw)) {
            return unquoted;
        }
        int comment = raw.indexOf(" #");
        return comment < 0 ? raw : raw.substring(0, comment).strip();
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' || first == '\'') && first == last) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
