package hansung.arcade.core.game;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 */
public final class DotEnv {

    /** 실행하는 폴더(프로젝트 폴더)의 .env */
    public static final Path DEFAULT_PATH = Path.of(".env");

    private DotEnv() {
    }

    /** 파일이 없으면 빈 Map */
    public static Map<String, String> read(Path path) {
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        try {
            return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(path + "를 읽지 못했습니다.", e);
        }
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
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
            values.put(line.substring(0, eq).strip(), unquote(line.substring(eq + 1).strip()));
        }
        return values;
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
