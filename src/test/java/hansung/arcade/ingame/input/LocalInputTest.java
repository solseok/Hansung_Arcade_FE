package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.KEY_LOCATION_LEFT;
import static java.awt.event.KeyEvent.KEY_LOCATION_RIGHT;
import static java.awt.event.KeyEvent.KEY_LOCATION_STANDARD;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_PERIOD;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SLASH;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_W;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 키 이벤트를 직접 만들어 넣고, 틱마다 꺼내는 명령을 확인한다. 한 번의 {@link LocalInput#takeCommands()}가 한 틱이다.
 */
class LocalInputTest {

    private static final Component SOURCE = new JPanel();
    private static final int ME = 1;
    private static final int P1 = 1;
    private static final int P2 = 2;

    private final LocalInput solo = new LocalInput(List.of(ME));
    private final LocalInput duo = new LocalInput(List.of(P1, P2));

    @Test
    void 물풍선_키는_누른_뒤_첫_틱에만_한_번_들어간다() {
        press(solo, VK_SPACE);

        assertTrue(command(solo, ME).placeBalloon());
        // 아직 누르고 있어도 다음 틱부터는 들어가지 않는다
        assertFalse(command(solo, ME).placeBalloon());
        assertFalse(command(solo, ME).placeBalloon());
    }

    @Test
    void 누르고_있는_동안_되풀이되는_누름은_새로_누른_것으로_치지_않는다() {
        press(solo, VK_SPACE);
        command(solo, ME);

        // OS 키 자동 반복
        press(solo, VK_SPACE);
        press(solo, VK_SPACE);
        assertFalse(command(solo, ME).placeBalloon());

        // 떼고 다시 누르면 또 들어간다
        release(solo, VK_SPACE);
        press(solo, VK_SPACE);
        assertTrue(command(solo, ME).placeBalloon());
    }

    @Test
    void 틱이_돌기_전에_눌렀다_떼도_다음_틱에_들어간다() {
        press(solo, VK_SPACE);
        release(solo, VK_SPACE);

        assertTrue(command(solo, ME).placeBalloon());
        assertFalse(command(solo, ME).placeBalloon());
    }

    @Test
    void 한_프레임에_틱이_여러_번_돌아도_막_눌린_키는_한_틱에만_들어간다() {
        press(solo, VK_SPACE);
        press(solo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        press(solo, VK_UP, KEY_LOCATION_STANDARD, SHIFT_DOWN_MASK);

        // 한 프레임에 세 틱이 돈다
        List<PlayerCommand> ticks = List.of(command(solo, ME), command(solo, ME), command(solo, ME));

        assertEquals(List.of(true, false, false), ticks.stream().map(PlayerCommand::placeBalloon).toList());
        assertEquals(List.of(true, false, false), ticks.stream().map(PlayerCommand::useItem).toList());
        // 이동은 누르고 있는 동안 틱마다 들어간다
        assertEquals(List.of(Direction.UP, Direction.UP, Direction.UP), ticks.stream().map(PlayerCommand::move).toList());
    }

    @Test
    void 아무것도_안_누르면_가만히_있는다() {
        assertEquals(PlayerCommand.IDLE, command(solo, ME));
    }

    @Test
    void 받은_키만_받았다고_알린다() {
        assertTrue(press(solo, VK_UP));
        assertTrue(press(solo, VK_SPACE));
        assertFalse(press(solo, VK_W));
        assertTrue(release(solo, VK_UP));
        assertFalse(release(solo, VK_W));
    }

    @Test
    void 채팅_입력칸에서는_방향키만_받고_Space와_Shift는_글자_입력에_맡긴다() {
        assertFalse(solo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_SPACE, KEY_LOCATION_STANDARD, 0), true));
        assertFalse(solo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK), true));
        assertTrue(solo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_LEFT, KEY_LOCATION_STANDARD, 0), true));

        PlayerCommand command = command(solo, ME);
        assertEquals(PlayerCommand.moving(Direction.LEFT), command);
    }

    @Test
    void 포커스를_잃어_비우면_누르던_키도_막_누른_키도_없어진다() {
        press(solo, VK_UP);
        press(solo, VK_SPACE);

        solo.clear();

        assertEquals(PlayerCommand.IDLE, command(solo, ME));
        // 뗌 이벤트를 못 받았어도 다시 누르면 새로 누른 것이다
        press(solo, VK_SPACE);
        assertTrue(command(solo, ME).placeBalloon());
    }

    @Test
    void Shift를_뗀_이벤트를_놓쳐도_Shift가_모두_떼진_뒤에는_다시_누를_수_있다() {
        press(solo, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK);
        assertTrue(command(solo, ME).useItem());
        // 오른쪽 Shift의 뗌 이벤트가 오지 않았다. 다른 키 이벤트에 Shift가 눌려 있지 않다고 나온다
        press(solo, VK_UP);
        release(solo, VK_UP);

        press(solo, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK);
        assertTrue(command(solo, ME).useItem());
    }

    @Test
    void Shift를_누르고_있는_동안에는_다른_키를_눌러도_누른_채로_둔다() {
        press(solo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        command(solo, ME);
        press(solo, VK_UP, KEY_LOCATION_STANDARD, SHIFT_DOWN_MASK);

        // 자동 반복으로 다시 온 Shift 누름은 새로 누른 것이 아니다
        press(solo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        assertFalse(command(solo, ME).useItem());
    }

    @Test
    void 두_명이면_WASD는_1P를_방향키는_2P를_움직인다() {
        press(duo, VK_W);
        press(duo, VK_LEFT);

        assertEquals(Direction.UP, duo.heldDirection(0));
        assertEquals(Direction.LEFT, duo.heldDirection(1));

        // 한 사람이 키를 떼도 다른 사람은 계속 움직인다
        release(duo, VK_W);
        assertNull(duo.heldDirection(0));
        assertEquals(Direction.LEFT, duo.heldDirection(1));
    }

    @Test
    void 두_명이면_Space는_1P_슬래시는_2P의_물풍선이다() {
        PlayerCommand balloon = new PlayerCommand(null, true, false);

        press(duo, VK_SPACE);
        assertEquals(Map.of(P1, balloon, P2, PlayerCommand.IDLE), duo.takeCommands());
        release(duo, VK_SPACE);

        press(duo, VK_SLASH);
        assertEquals(Map.of(P1, PlayerCommand.IDLE, P2, balloon), duo.takeCommands());
    }

    @Test
    void 두_명이면_왼쪽_Shift는_1P의_아이템이고_2P는_운영체제에_맞는_오른쪽_수정_키가_아이템이다() {
        press(duo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        pressSecondItem(duo, SHIFT_DOWN_MASK);

        PlayerCommand item = new PlayerCommand(null, false, true);
        assertEquals(Map.of(P1, item, P2, item), duo.takeCommands());
    }

    @Test
    void 두_명이면_오른쪽_Shift와_왼쪽_Ctrl_왼쪽_Alt와_마침표는_아무_키도_아니다() {
        assertFalse(press(duo, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK));
        assertFalse(press(duo, VK_CONTROL, KEY_LOCATION_LEFT, CTRL_DOWN_MASK));
        assertFalse(press(duo, VK_ALT, KEY_LOCATION_LEFT, ALT_DOWN_MASK));
        assertFalse(press(duo, VK_PERIOD));

        assertEquals(Map.of(P1, PlayerCommand.IDLE, P2, PlayerCommand.IDLE), duo.takeCommands());
    }

    @Test
    void 두_명일_때_2P_아이템_키는_누르고_있는_동안_자동_반복이_와도_한_번만_쓴다() {
        pressSecondItem(duo, 0);
        assertTrue(command(duo, P2).useItem());

        pressSecondItem(duo, 0);
        assertFalse(command(duo, P2).useItem());
    }

    @Test
    void 두_명일_때_2P_아이템_키의_뗌을_놓쳐도_그_수정_키가_떼진_뒤에는_다시_누를_수_있다() {
        pressSecondItem(duo, 0);
        assertTrue(command(duo, P2).useItem());
        // 뗌 이벤트가 오지 않았다. 다음 키 이벤트에 그 수정 키가 눌려 있지 않다고 나온다
        press(duo, VK_UP);
        release(duo, VK_UP);

        pressSecondItem(duo, 0);
        assertTrue(command(duo, P2).useItem());
    }

    /** 이 컴퓨터의 2P 대표 아이템 키(macOS 오른쪽 Option, 그 밖 오른쪽 Ctrl)를 누른다. 그 키의 수정 키 표시를 더해 보낸다 */
    private static void pressSecondItem(LocalInput input, int otherModifiers) {
        KeyBindings.Key key = KeyBindings.PLAYER_2.item().get(0);
        int mask = key.code() == VK_ALT ? ALT_DOWN_MASK : CTRL_DOWN_MASK;
        assertTrue(press(input, key.code(), key.location(), otherModifiers | mask));
    }

    @Test
    void 한_사람이_물풍선_키를_누르고_있어도_다른_사람은_따로_누를_수_있다() {
        press(duo, VK_SPACE);
        command(duo, P1);
        press(duo, VK_SLASH);

        Map<Integer, PlayerCommand> tick = duo.takeCommands();
        assertFalse(tick.get(P1).placeBalloon());
        assertTrue(tick.get(P2).placeBalloon());
    }

    @Test
    void 두_명일_때_채팅_입력칸에서는_2P의_방향키만_받는다() {
        assertFalse(duo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_D, KEY_LOCATION_STANDARD, 0), true));
        assertFalse(duo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_SPACE, KEY_LOCATION_STANDARD, 0), true));
        assertFalse(duo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK), true));
        assertFalse(duo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_SLASH, KEY_LOCATION_STANDARD, 0), true));
        // 2P의 아이템 키(오른쪽 Ctrl·Option·Alt·AltGr)는 글을 치는 동안 받지 않아 Option·AltGr 글자 입력을 막지 않는다
        for (KeyBindings.Key key : KeyBindings.PLAYER_2.item()) {
            int location = key.location() == KeyBindings.Key.ANY_LOCATION ? KEY_LOCATION_RIGHT : key.location();
            assertFalse(duo.keyPressed(key(KeyEvent.KEY_PRESSED, key.code(), location, ALT_DOWN_MASK), true));
        }
        assertTrue(duo.keyPressed(key(KeyEvent.KEY_PRESSED, VK_UP, KEY_LOCATION_STANDARD, 0), true));

        Map<Integer, PlayerCommand> tick = duo.takeCommands();
        assertEquals(PlayerCommand.IDLE, tick.get(P1));
        assertEquals(PlayerCommand.moving(Direction.UP), tick.get(P2));
    }

    @Test
    void 채팅_입력칸을_열기_전부터_누르던_WASD는_입력칸에서_떼도_멈춘다() {
        press(duo, VK_D);
        assertTrue(duo.keyReleased(key(KeyEvent.KEY_RELEASED, VK_D, KEY_LOCATION_STANDARD, 0)));

        assertNull(duo.heldDirection(0));
    }

    @Test
    void 포커스를_잃으면_두_사람의_키를_모두_비운다() {
        press(duo, VK_W);
        press(duo, VK_UP);
        press(duo, VK_SPACE);
        press(duo, VK_SLASH);

        duo.clear();

        assertEquals(Map.of(P1, PlayerCommand.IDLE, P2, PlayerCommand.IDLE), duo.takeCommands());
    }

    @Test
    void 오른쪽_Shift를_눌렀다_떼도_왼쪽_Shift를_누르고_있으면_1P의_Shift는_누른_채로_둔다() {
        press(duo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        duo.takeCommands();
        press(duo, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK);

        // 오른쪽 Shift를 뗐다. 1P가 왼쪽 Shift를 누르고 있어서 Shift 표시가 남아 있다
        duo.keyReleased(key(KeyEvent.KEY_RELEASED, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK));
        // 그래서 1P의 왼쪽 Shift 자동 반복은 여전히 새로 누른 것이 아니다
        press(duo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);

        assertFalse(command(duo, P1).useItem());
    }

    @Test
    void 두_Shift가_겹쳐_1P의_뗌을_놓쳐도_Shift를_모두_떼면_바로잡힌다() {
        press(duo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        duo.takeCommands();
        // 1P가 왼쪽 Shift를 누른 채 누군가 오른쪽 Shift를 누르면, 왼쪽 Shift 뗌이 오지 않는다(Windows·macOS)
        press(duo, VK_SHIFT, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK);
        // 오른쪽 Shift까지 떼면 Shift가 하나도 없다고 온다
        duo.keyReleased(key(KeyEvent.KEY_RELEASED, VK_SHIFT, KEY_LOCATION_RIGHT, 0));

        press(duo, VK_SHIFT, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        assertTrue(command(duo, P1).useItem());
    }

    static PlayerCommand command(LocalInput input, int playerId) {
        return input.takeCommands().get(playerId);
    }

    static boolean press(LocalInput input, int keyCode) {
        return press(input, keyCode, KEY_LOCATION_STANDARD, 0);
    }

    static boolean press(LocalInput input, int keyCode, int location, int modifiersEx) {
        return input.keyPressed(key(KeyEvent.KEY_PRESSED, keyCode, location, modifiersEx), false);
    }

    static boolean release(LocalInput input, int keyCode) {
        return release(input, keyCode, KEY_LOCATION_STANDARD);
    }

    static boolean release(LocalInput input, int keyCode, int location) {
        return input.keyReleased(key(KeyEvent.KEY_RELEASED, keyCode, location, 0));
    }

    static KeyEvent key(int id, int keyCode, int location, int modifiersEx) {
        return new KeyEvent(SOURCE, id, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED, location);
    }
}
