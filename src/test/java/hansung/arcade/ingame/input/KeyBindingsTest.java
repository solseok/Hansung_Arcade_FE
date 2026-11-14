package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.input.KeyBindings.Action;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.awt.event.KeyEvent.KEY_LOCATION_LEFT;
import static java.awt.event.KeyEvent.KEY_LOCATION_RIGHT;
import static java.awt.event.KeyEvent.KEY_LOCATION_STANDARD;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SLASH;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_W;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindingsTest {

    @Test
    void 혼자_할_때는_방향키로_움직이고_Space로_물풍선_Shift로_아이템을_쓴다() {
        KeyBindings keys = KeyBindings.SOLO;

        assertEquals(Direction.UP, keys.directionOf(VK_UP));
        assertEquals(Direction.DOWN, keys.directionOf(VK_DOWN));
        assertEquals(Direction.LEFT, keys.directionOf(VK_LEFT));
        assertEquals(Direction.RIGHT, keys.directionOf(VK_RIGHT));
        assertEquals(Action.BALLOON, keys.actionOf(VK_SPACE, KEY_LOCATION_STANDARD));
        // Shift는 좌우 어느 쪽이든 된다
        assertEquals(Action.ITEM, keys.actionOf(VK_SHIFT, KEY_LOCATION_LEFT));
        assertEquals(Action.ITEM, keys.actionOf(VK_SHIFT, KEY_LOCATION_RIGHT));
    }

    @Test
    void 배치에_없는_키는_방향도_행동도_아니다() {
        KeyBindings keys = KeyBindings.SOLO;

        assertNull(keys.directionOf(VK_W));
        assertNull(keys.directionOf(VK_SPACE));
        assertNull(keys.actionOf(VK_A, KEY_LOCATION_STANDARD));
        assertNull(keys.actionOf(VK_UP, KEY_LOCATION_STANDARD));
    }

    @Test
    void 혼자면_혼자_할_때_배치를_쓴다() {
        assertEquals(List.of(KeyBindings.SOLO), KeyBindings.forLocalPlayers(1));
        assertThrows(IllegalArgumentException.class, () -> KeyBindings.forLocalPlayers(0));
    }

    @Test
    void 글을_치는_동안에는_방향키만_게임이_받는다() {
        for (int arrow : new int[]{VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT}) {
            assertTrue(KeyBindings.worksWhileTyping(arrow));
        }
        for (int typed : new int[]{VK_W, VK_A, VK_SPACE, VK_SHIFT, VK_SLASH}) {
            assertFalse(KeyBindings.worksWhileTyping(typed));
        }
    }

    @Test
    void 왼쪽_오른쪽을_정한_키는_그쪽만_맞는다() {
        KeyBindings.Key left = KeyBindings.Key.left(VK_SHIFT);
        KeyBindings.Key any = KeyBindings.Key.any(VK_SHIFT);

        assertTrue(left.matches(VK_SHIFT, KEY_LOCATION_LEFT));
        assertFalse(left.matches(VK_SHIFT, KEY_LOCATION_RIGHT));
        assertTrue(any.matches(VK_SHIFT, KEY_LOCATION_RIGHT));
        assertFalse(any.matches(VK_SPACE, KEY_LOCATION_STANDARD));
    }
}
