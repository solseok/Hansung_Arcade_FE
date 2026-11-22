package hansung.arcade.core.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** application.properties의 ${이름} 자리표시자를 환경 변수·.env로 채운다 */
class PlaceholderPropertiesTest {

    private static final Map<String, String> NETWORK = Map.of(
            "network.host", "${SERVER_HOST}",
            "network.port", "${SERVER_PORT}");

    @Test
    void 자리표시자를_환경_값으로_채운다() {
        GameProperties properties = GameProperties.of(NETWORK, List.of(),
                Map.of("SERVER_HOST", "arcade.example", "SERVER_PORT", "9100"));

        assertEquals("arcade.example", properties.get("network.host"));
        assertEquals(9100, properties.getInt("network.port"));
    }

    @Test
    void 채우지_못한_값은_기본값을_쓰고_필수로_읽으면_무엇을_넣을지_알려_준다() {
        GameProperties properties = GameProperties.of(NETWORK, List.of(), Map.of());

        assertEquals("localhost", properties.get("network.host", "localhost"));
        assertEquals(9000, properties.getInt("network.port", 9000));
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> properties.get("network.host"));
        assertTrue(failure.getMessage().contains("SERVER_HOST"), failure.getMessage());
        assertTrue(failure.getMessage().contains(".env"), failure.getMessage());
        assertThrows(IllegalStateException.class, () -> properties.getInt("network.port"));
    }

    @Test
    void 이름_뒤_콜론은_기본값이다() {
        GameProperties properties = GameProperties.of(
                Map.of("network.host", "${SERVER_HOST:localhost}", "network.port", "${SERVER_PORT:9000}"),
                List.of(), Map.of("SERVER_PORT", "9200"));

        assertEquals("localhost", properties.get("network.host"));
        assertEquals(9200, properties.getInt("network.port"));
    }

    @Test
    void 실행_인자가_자리표시자보다_앞선다() {
        GameProperties properties = GameProperties.of(NETWORK, List.of("--network.host=127.0.0.1"), Map.of());

        assertEquals("127.0.0.1", properties.get("network.host"));
    }

    @Test
    void 자리표시자는_값_일부에도_쓸_수_있다() {
        GameProperties properties = GameProperties.of(Map.of("app.title", "Arcade ${STAGE}"), List.of(),
                Map.of("STAGE", "dev"));

        assertEquals("Arcade dev", properties.get("app.title"));
    }

    @Test
    void env_파일은_주석_export_따옴표를_다룬다(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(".env");
        Files.writeString(file, """
                # 서버
                SERVER_HOST=arcade.example

                export SERVER_PORT="9300"
                NAME='상상 부기'
                잘못된줄
                """);

        Map<String, String> values = DotEnv.read(file);

        assertEquals(Map.of("SERVER_HOST", "arcade.example", "SERVER_PORT", "9300", "NAME", "상상 부기"), values);
    }

    @Test
    void env_파일이_없으면_빈_값이다(@TempDir Path dir) {
        assertEquals(Map.of(), DotEnv.read(dir.resolve(".env")));
    }

    @Test
    void 기본_설정_파일의_서버_주소는_자리표시자다() throws IOException {
        var properties = new java.util.Properties();
        try (var in = GameProperties.class.getResourceAsStream("/application.properties")) {
            properties.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }

        assertEquals("${SERVER_HOST}", properties.getProperty("network.host"));
        assertEquals("${SERVER_PORT}", properties.getProperty("network.port"));
        assertEquals("offline", properties.getProperty("network.mode"));
    }
}
