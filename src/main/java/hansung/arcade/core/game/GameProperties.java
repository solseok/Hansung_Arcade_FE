package hansung.arcade.core.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실행 설정. resources/application.properties 값을 실행 인자 {@code --키=값}이 덮어쓴다.
 *
 * <pre>
 * ./gradlew run --args="--game.start-map=camp/camp08 --network.mode=offline"
 * </pre>
 *
 * 값 안의 {@code ${이름}}은 환경 변수로, 환경 변수에 없으면 프로젝트 폴더의 {@code .env}({@link DotEnv}) 값으로 바꾼다.
 * {@code ${이름:기본값}}처럼 쓰면 둘 다 없을 때 기본값을 쓴다. 채우지 못한 설정은 없는 것으로 보아,
 * {@link #get(String, String)}은 기본값을 돌려주고 {@link #get(String)}은 무엇을 채워야 하는지 알리며 실패한다.
 */
public final class GameProperties {

    private static final Logger log = LoggerFactory.getLogger(GameProperties.class);
    private static final String RESOURCE = "/application.properties";
    private static final String ARG_PREFIX = "--";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_.]*)(?::([^}]*))?}");

    private final Map<String, String> values;
    /** 채우지 못한 설정 → 비어 있는 자리표시자 이름 */
    private final Map<String, String> unresolved;

    private GameProperties(Map<String, String> values, Map<String, String> unresolved) {
        this.values = Map.copyOf(values);
        this.unresolved = Map.copyOf(unresolved);
    }

    /** application.properties, 실행 인자, 환경 변수, .env를 모두 읽는다 */
    public static GameProperties load(Class<?> resourceAnchor, List<String> args) {
        Map<String, String> dotEnv = DotEnv.read(DotEnv.DEFAULT_PATH);
        if (!dotEnv.isEmpty()) {
            log.info("{}에서 값 {}개를 읽었습니다.", DotEnv.DEFAULT_PATH, dotEnv.size());
        }
        Map<String, String> env = new HashMap<>(dotEnv);
        // 환경 변수가 .env보다 앞선다
        env.putAll(System.getenv());
        return load(resourceAnchor, args, env);
    }

    /** application.properties와 실행 인자를 읽고, {@code ${이름}}은 env로 채운다 */
    public static GameProperties load(Class<?> resourceAnchor, List<String> args, Map<String, String> env) {
        return of(readResource(resourceAnchor), args, env);
    }

    public static GameProperties of(Map<String, String> defaults, List<String> args) {
        return of(defaults, args, Map.of());
    }

    /**
     * @param env {@code ${이름}}을 채울 값 (환경 변수와 .env를 합친 것)
     */
    public static GameProperties of(Map<String, String> defaults, List<String> args, Map<String, String> env) {
        Map<String, String> merged = new HashMap<>(defaults);
        for (String arg : args) {
            if (!arg.startsWith(ARG_PREFIX) || !arg.contains("=")) {
                // 조용히 버리면 "--키 값"처럼 잘못 쓰고도 적용된 줄 알게 된다.
                log.warn("실행 인자 '{}'은(는) --키=값 형식이 아니라서 무시합니다.", arg);
                continue;
            }
            String pair = arg.substring(ARG_PREFIX.length());
            int eq = pair.indexOf('=');
            merged.put(pair.substring(0, eq).strip(), pair.substring(eq + 1).strip());
        }

        Map<String, String> resolved = new HashMap<>();
        Map<String, String> unresolved = new HashMap<>();
        merged.forEach((key, value) -> {
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuilder result = new StringBuilder();
            String missing = null;
            while (matcher.find()) {
                String replacement = env.get(matcher.group(1));
                if (replacement == null || replacement.isBlank()) {
                    replacement = matcher.group(2);
                }
                if (replacement == null) {
                    missing = missing == null ? matcher.group(1) : missing;
                    replacement = "";
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.strip()));
            }
            matcher.appendTail(result);
            if (missing != null) {
                unresolved.put(key, missing);
            } else {
                resolved.put(key, result.toString());
            }
        });
        return new GameProperties(resolved, unresolved);
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
            String placeholder = unresolved.get(key);
            if (placeholder != null) {
                throw new IllegalStateException("설정값 %s에 쓸 %s가 없습니다. 프로젝트 폴더의 .env(.env.example 참고)나 환경 변수에 %s를 넣으세요."
                        .formatted(key, placeholder, placeholder));
            }
            throw new IllegalStateException(
                    "설정값 " + key + "이(가) 없습니다. application.properties와 실행 인자를 확인하세요.");
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    /** 반드시 있어야 하는 숫자 값 */
    public int getInt(String key) {
        return parseInt(key, get(key));
    }

    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return parseInt(key, value);
    }

    private static int parseInt(String key, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("설정값 %s=%s 는 숫자가 아닙니다.".formatted(key, value), e);
        }
    }
}
