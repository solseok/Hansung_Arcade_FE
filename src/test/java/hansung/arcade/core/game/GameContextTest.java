package hansung.arcade.core.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextTest {

    private final GameContext context = new GameContext(GameContextTest.class, List.of());

    @Test
    void 등록한_객체를_타입으로_꺼낸다() {
        context.register(Integer.class, 42);

        assertTrue(context.contains(Integer.class));
        assertEquals(42, context.get(Integer.class));
    }

    @Test
    void 등록되지_않은_타입을_꺼내면_실패한다() {
        assertFalse(context.contains(String.class));
        assertThrows(IllegalStateException.class, () -> context.get(String.class));
    }

    @Test
    void 같은_타입을_두_번_등록하면_실패한다() {
        context.register(String.class, "처음");

        assertThrows(IllegalStateException.class, () -> context.register(String.class, "다시"));
        assertEquals("처음", context.get(String.class));
    }
}
