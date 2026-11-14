package hansung.arcade.core.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 실행 설정. resources/application.properties 값을 실행 인자 {@code --키=값}이 덮어쓴다.
 *
 * <pre>
 * ./gradlew run --args="--game.start-map=camp/camp08 --network.mode=offline"
 * </pre>
 */
public final class GameProperties {

    private static final String RESOURCE = "/application.properties";
    private static final String ARG_PREFIX = "--";

    private final Map<String, String> values;

    private GameProperties(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static GameProperties load(Class<?> resourceAnchor, List<String> args) {
        return of(readResource(resourceAnchor), args);
    }

    public static GameProperties of(Map<String, String> defaults, List<String> args) {
        Map<String, String> merged = new HashMap<>(defaults);
        for (String arg : args) {
            if (!arg.startsWith(ARG_PREFIX) || !arg.contains("=")) {
                continue;
            }
            String pair = arg.substring(ARG_PREFIX.length());
            int eq = pair.indexOf('=');
            merged.put(pair.substring(0, eq).strip(), pair.substring(eq + 1).strip());
        }
        return new GameProperties(merged);
    }

    private static Map<String, String> readResource(Class<?> anchor) {
        Map<String, String> result = new HashMap<>();
        try (InputStream in = anchor.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return result;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            properties.stringPropertyNames().forEach(key -> result.put(key, properties.getProperty(key).strip()));
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(RESOURCE + "를 읽지 못했습니다.", e);
        }
    }

    /** 반드시 있어야 하는 값 */
    public String get(String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("설정값 " + key + "이(가) 없습니다. application.properties를 확인하세요.");
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("설정값 %s=%s 는 숫자가 아닙니다.".formatted(key, value), e);
        }
    }
}
