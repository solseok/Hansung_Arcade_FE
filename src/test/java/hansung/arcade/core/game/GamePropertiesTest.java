package hansung.arcade.core.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePropertiesTest {

    @Test
    void 실행_인자가_기본값을_덮어쓴다() {
        GameProperties properties = GameProperties.of(
                Map.of("game.start-map", "camp08", "app.title", "Hansung Arcade"),
                List.of("--game.start-map=desert", "무시되는인자", "--플래그만"));

        assertEquals("desert", properties.get("game.start-map"));
        assertEquals("Hansung Arcade", properties.get("app.title"));
    }

    @Test
    void 값이_없으면_기본값을_쓰고_필수값이면_실패한다() {
        GameProperties properties = GameProperties.of(Map.of(), List.of());

        assertEquals("offline", properties.get("network.mode", "offline"));
        assertEquals(9000, properties.getInt("network.port", 9000));
        assertThrows(IllegalStateException.class, () -> properties.get("game.start-map"));
    }

    @Test
    void 숫자가_아닌_값을_숫자로_읽으면_실패한다() {
        GameProperties properties = GameProperties.of(Map.of("network.port", "abc"), List.of());

        assertThrows(IllegalStateException.class, () -> properties.getInt("network.port", 9000));
    }

    @Test
    void 리소스의_application_properties를_읽는다() {
        // 이 컴퓨터의 .env·환경 변수와 상관없이 본다
        GameProperties properties = GameProperties.load(GamePropertiesTest.class, List.of(), Map.of());

        // 시작 맵은 자주 바뀌므로 값 대신 "테마/맵" 형식만 확인한다
        assertTrue(properties.get("game.start-map").matches("[a-z]+/[a-z0-9]+"), properties.get("game.start-map"));
        assertEquals("online", properties.get("network.mode"));
        // .env·환경 변수가 없으면 이 컴퓨터에 띄운 서버로 간다
        assertEquals("localhost", properties.get("network.host"));
        assertEquals(9000, properties.getInt("network.port"));
    }
}
