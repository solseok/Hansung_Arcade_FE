package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void 온라인_모드는_아직_지원하지_않는다() {
        GameContext context = contextWith(Map.of("network.mode", "online"));

        assertThrows(UnsupportedOperationException.class, () -> new NetworkStep().run(context));
    }

    @Test
    void 잘못된_모드나_포트면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkStep().run(contextWith(Map.of("network.mode", "lan"))));
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkStep().run(contextWith(Map.of("network.port", "70000"))));
    }
}
