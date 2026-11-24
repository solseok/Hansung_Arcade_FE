package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkStepTest {

    private static GameContext contextWith(Map<String, String> values) {
        GameContext context = new GameContext(NetworkStepTest.class, List.of());
        context.register(GameProperties.class, GameProperties.of(values, List.of()));
        return context;
    }

    @Test
    void 오프라인_설정을_등록한다() {
        GameContext context = contextWith(Map.of("network.mode", "OFFLINE", "network.port", "7777"));

        new NetworkStep().run(context);

        NetworkSettings settings = context.get(NetworkSettings.class);
        assertEquals(NetworkMode.OFFLINE, settings.mode());
        assertEquals("localhost", settings.host());
        assertEquals(7777, settings.port());
    }

    @Test
    void 오프라인은_서버_주소_자리표시자가_비어_있어도_된다() {
        GameContext context = contextWith(Map.of("network.mode", "offline",
                "network.host", "${SERVER_HOST}", "network.port", "${SERVER_PORT}"));

        new NetworkStep().run(context);

        assertEquals("localhost", context.get(NetworkSettings.class).host());
        assertFalse(context.contains(ServerClient.class));
    }

    @Test
    void 온라인은_서버_연결을_등록하고_접속은_로그인할_때_한다() {
        GameContext context = contextWith(Map.of("network.mode", "online",
                "network.host", "arcade.example", "network.port", "9100"));

        new NetworkStep().run(context);

        ServerClient client = context.get(ServerClient.class);
        assertEquals(new NetworkSettings(NetworkMode.ONLINE, "arcade.example", 9100), client.settings());
        assertTrue(client.connection().isEmpty());
        client.close();
    }

    @Test
    void 온라인인데_서버_주소를_채우지_못하면_env에_넣을_이름을_알려_주며_실패한다() {
        GameContext context = contextWith(Map.of("network.mode", "online",
                "network.host", "${SERVER_HOST}", "network.port", "${SERVER_PORT}"));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> new NetworkStep().run(context));

        assertTrue(failure.getMessage().contains("SERVER_HOST"), failure.getMessage());
    }

    @Test
    void 잘못된_모드나_온라인의_잘못된_포트면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkStep().run(contextWith(Map.of("network.mode", "lan"))));
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkStep().run(contextWith(Map.of("network.mode", "online",
                        "network.host", "localhost", "network.port", "70000"))));
        assertThrows(IllegalStateException.class,
                () -> new NetworkStep().run(contextWith(Map.of("network.mode", "online",
                        "network.host", "localhost", "network.port", "9000 # 집"))));
    }

    @Test
    void 오프라인은_쓰지_않는_포트가_잘못돼도_뜬다() {
        GameContext numberless = contextWith(Map.of("network.port", "9000 # 집"));
        new NetworkStep().run(numberless);
        assertEquals(9000, numberless.get(NetworkSettings.class).port());

        GameContext outOfRange = contextWith(Map.of("network.port", "70000"));
        new NetworkStep().run(outOfRange);
        assertEquals(9000, outOfRange.get(NetworkSettings.class).port());
    }
}
