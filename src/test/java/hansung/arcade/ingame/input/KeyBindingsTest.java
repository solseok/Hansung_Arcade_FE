package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.input.KeyBindings.Action;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.awt.event.KeyEvent.KEY_LOCATION_LEFT;
import static java.awt.event.KeyEvent.KEY_LOCATION_RIGHT;
import static java.awt.event.KeyEvent.KEY_LOCATION_STANDARD;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_ALT_GRAPH;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_PERIOD;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SLASH;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_W;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void 두_명이_할_때_1P는_WASD로_움직이고_Space로_물풍선_왼쪽_Shift로_아이템을_쓴다() {
        KeyBindings keys = KeyBindings.PLAYER_1;

        assertEquals(Direction.UP, keys.directionOf(VK_W));
        assertEquals(Direction.LEFT, keys.directionOf(VK_A));
        assertEquals(Direction.DOWN, keys.directionOf(VK_S));
        assertEquals(Direction.RIGHT, keys.directionOf(VK_D));
        assertNull(keys.directionOf(VK_UP));
        assertEquals(Action.BALLOON, keys.actionOf(VK_SPACE, KEY_LOCATION_STANDARD));
        assertEquals(Action.ITEM, keys.actionOf(VK_SHIFT, KEY_LOCATION_LEFT));
        // 오른쪽 Shift는 1P 키가 아니다
        assertNull(keys.actionOf(VK_SHIFT, KEY_LOCATION_RIGHT));
    }

    @Test
    void 두_명이_할_때_2P는_방향키로_움직이고_슬래시로_물풍선을_놓는다() {
        for (boolean mac : new boolean[]{true, false}) {
            KeyBindings keys = KeyBindings.player2(mac);

            assertEquals(Direction.UP, keys.directionOf(VK_UP));
            assertEquals(Direction.DOWN, keys.directionOf(VK_DOWN));
            assertEquals(Direction.LEFT, keys.directionOf(VK_LEFT));
            assertEquals(Direction.RIGHT, keys.directionOf(VK_RIGHT));
            assertNull(keys.directionOf(VK_W));
            assertEquals(Action.BALLOON, keys.actionOf(VK_SLASH, KEY_LOCATION_STANDARD));
            assertNull(keys.actionOf(VK_SHIFT, KEY_LOCATION_RIGHT));
            assertNull(keys.actionOf(VK_PERIOD, KEY_LOCATION_STANDARD));
        }
    }

    @Test
    void macOS에서_2P의_아이템_키는_오른쪽_Option이고_Ctrl은_쓰지_않는다() {
        KeyBindings keys = KeyBindings.player2(true);

        assertEquals(Action.ITEM, keys.actionOf(VK_ALT, KEY_LOCATION_RIGHT));
        assertNull(keys.actionOf(VK_ALT, KEY_LOCATION_LEFT));
        // Ctrl+방향키는 macOS의 데스크톱 전환이라 방향키를 쓰는 2P에게 Ctrl을 주지 않는다
        assertNull(keys.actionOf(VK_CONTROL, KEY_LOCATION_RIGHT));
        for (KeyBindings.Key key : keys.item()) {
            assertNotEquals(VK_CONTROL, key.code());
        }
    }

    @Test
    void Windows에서_2P의_아이템_키는_오른쪽_Ctrl이고_오른쪽_Ctrl이_없으면_오른쪽_Alt나_AltGr을_쓴다() {
        KeyBindings keys = KeyBindings.player2(false);

        assertEquals(VK_CONTROL, keys.item().get(0).code());
        assertEquals(Action.ITEM, keys.actionOf(VK_CONTROL, KEY_LOCATION_RIGHT));
        assertEquals(Action.ITEM, keys.actionOf(VK_ALT, KEY_LOCATION_RIGHT));
        assertEquals(Action.ITEM, keys.actionOf(VK_ALT_GRAPH, KEY_LOCATION_RIGHT));
        assertEquals(Action.ITEM, keys.actionOf(VK_ALT_GRAPH, KEY_LOCATION_STANDARD));
        assertNull(keys.actionOf(VK_CONTROL, KEY_LOCATION_LEFT));
        assertNull(keys.actionOf(VK_ALT, KEY_LOCATION_LEFT));
    }

    @Test
    void 이_컴퓨터의_2P_배치는_운영체제에_맞춘_것이다() {
        assertEquals(KeyBindings.player2(KeyBindings.MAC), KeyBindings.PLAYER_2);
        assertTrue(KeyBindings.isMac("Mac OS X"));
        assertFalse(KeyBindings.isMac("Windows 11"));
        assertFalse(KeyBindings.isMac("Linux"));
    }

    @Test
    void Shift는_1P만_쓴다() {
        // 좌우 Shift를 나눠 쓰면 겹칠 때 이벤트가 빠진다
        assertEquals(List.of(VK_SPACE, VK_SHIFT),
                List.of(KeyBindings.PLAYER_1.balloon().code(), KeyBindings.PLAYER_1.item().get(0).code()));
        for (boolean mac : new boolean[]{true, false}) {
            KeyBindings second = KeyBindings.player2(mac);
            assertNotEquals(VK_SHIFT, second.balloon().code());
            for (KeyBindings.Key key : second.item()) {
                assertNotEquals(VK_SHIFT, key.code());
            }
        }
    }

    @Test
    void 두_명의_키는_하나도_겹치지_않는다() {
        Set<String> first = new HashSet<>(keysOf(KeyBindings.PLAYER_1));
        for (boolean mac : new boolean[]{true, false}) {
            for (String key : keysOf(KeyBindings.player2(mac))) {
                assertFalse(first.contains(key), key);
            }
        }
    }

    @Test
    void 아이템_칸에_적는_키_이름은_왼쪽이면_앞에_L_오른쪽이면_뒤에_R을_붙인다() {
        assertEquals("Shift", KeyBindings.Key.any(VK_SHIFT).shortName(false));
        assertEquals("L-Shift", KeyBindings.Key.left(VK_SHIFT).shortName(false));
        assertEquals("Ctrl-R", KeyBindings.Key.right(VK_CONTROL).shortName(false));
        assertEquals("Opt-R", KeyBindings.Key.right(VK_ALT).shortName(true));
        assertEquals("Alt-R", KeyBindings.Key.right(VK_ALT).shortName(false));
        assertEquals("Shift", KeyBindings.SOLO.itemKeyName());
        assertEquals("L-Shift", KeyBindings.PLAYER_1.itemKeyName());
    }

    @Test
    void 안내_글에_적는_키_이름은_좌우를_한글로_적는다() {
        assertEquals("오른쪽 Option", KeyBindings.Key.right(VK_ALT).koreanName(true));
        assertEquals("오른쪽 Alt", KeyBindings.Key.right(VK_ALT).koreanName(false));
        assertEquals("왼쪽 Shift", KeyBindings.Key.left(VK_SHIFT).koreanName(false));
        assertEquals("왼쪽 Shift", KeyBindings.PLAYER_1.itemKeyGuide());
    }

    @Test
    void 인원에_맞는_키_배치를_1P부터_돌려준다() {
        assertEquals(List.of(KeyBindings.SOLO), KeyBindings.forLocalPlayers(1));
        assertEquals(List.of(KeyBindings.PLAYER_1, KeyBindings.PLAYER_2), KeyBindings.forLocalPlayers(2));
        assertThrows(IllegalArgumentException.class, () -> KeyBindings.forLocalPlayers(0));
        assertThrows(IllegalArgumentException.class, () -> KeyBindings.forLocalPlayers(3));
    }

    @Test
    void 글을_치는_동안에는_방향키만_게임이_받는다() {
        for (int arrow : new int[]{VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT}) {
            assertTrue(KeyBindings.worksWhileTyping(arrow));
        }
        for (int typed : new int[]{VK_W, VK_A, VK_SPACE, VK_SHIFT, VK_SLASH, VK_PERIOD, VK_CONTROL, VK_ALT, VK_ALT_GRAPH}) {
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

    /** 배치에 쓰인 키를 "키 코드@위치"로. 이동 키와 좌우를 가리지 않는 키는 가운데·왼쪽·오른쪽 모두를 차지한다 */
    private static List<String> keysOf(KeyBindings keys) {
        List<KeyBindings.Key> used = new ArrayList<>();
        for (int code : new int[]{keys.up(), keys.down(), keys.left(), keys.right()}) {
            used.add(KeyBindings.Key.any(code));
        }
        used.add(keys.balloon());
        used.addAll(keys.item());

        List<String> result = new ArrayList<>();
        for (KeyBindings.Key key : used) {
            for (int location : new int[]{KEY_LOCATION_STANDARD, KEY_LOCATION_LEFT, KEY_LOCATION_RIGHT}) {
                if (key.matches(key.code(), location)) {
                    result.add(key.code() + "@" + location);
                }
            }
        }
        return result;
    }
}
