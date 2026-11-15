package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.input.KeyBindings.Action;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 플레이어 한 명의 키 상태. 이동 키는 누르고 있는 동안 계속 움직이고, 물풍선·아이템 키는 누른 순간 한 번만 동작한다.
 *
 * <p>물풍선·아이템 키를 누르면 그 뒤 처음 만드는 {@link #takeCommand 명령}에만 담긴다. 그래서 한 프레임에 틱이
 * 여러 번 돌아도 첫 틱만 받고, 틱이 돌기 전에 눌렀다 떼도 사라지지 않는다.
 * 떼기 전에 다시 오는 누름(키 자동 반복)은 새로 누른 것으로 치지 않는다. 한 행동에 키가 여럿이면(2P의 오른쪽 Option과 .)
 * 키마다 따로 가린다.</p>
 */
final class PlayerControls {

    private final int playerId;
    private final KeyBindings keys;
    private final DirectionInput movement = new DirectionInput();
    /** 누르고 있는 물풍선·아이템 키와 그 키의 행동. 떼기 전에 다시 오는 누름을 거른다 */
    private final Map<KeyBindings.Key, Action> held = new HashMap<>();
    /** 다음 명령에 담을, 막 누른 행동 */
    private final Set<Action> pressed = EnumSet.noneOf(Action.class);

    PlayerControls(int playerId, KeyBindings keys) {
        this.playerId = playerId;
        this.keys = keys;
    }

    int playerId() {
        return playerId;
    }

    /**
     * @param typing 채팅 입력칸에 글을 치는 중인지. 그때는 {@link KeyBindings#worksWhileTyping 방향키}만 받는다
     * @return 이 플레이어의 키여서 받았는지
     */
    boolean press(int keyCode, int keyLocation, boolean typing) {
        if (typing && !KeyBindings.worksWhileTyping(keyCode)) {
            return false;
        }
        Direction direction = keys.directionOf(keyCode);
        if (direction != null) {
            movement.press(direction);
            return true;
        }
        Action action = keys.actionOf(keyCode, keyLocation);
        if (action != null) {
            if (held.putIfAbsent(keys.actionKey(keyCode, keyLocation), action) == null) {
                pressed.add(action);
            }
            return true;
        }
        return false;
    }

    /** @return 이 플레이어의 키여서 받았는지 */
    boolean release(int keyCode, int keyLocation) {
        Direction direction = keys.directionOf(keyCode);
        if (direction != null) {
            movement.release(direction);
            return true;
        }
        KeyBindings.Key key = keys.actionKey(keyCode, keyLocation);
        if (key != null) {
            held.remove(key);
            return true;
        }
        return false;
    }

    /**
     * 수정 키(Shift·Ctrl·Alt·AltGr)가 눌린 채로 남아 있는데, 키 이벤트의 수정 키 상태(modifiersEx)에
     * 그 키와 같은 행동에 묶인 수정 키가 하나도 없으면 뗀 것으로 친다.
     * 좌우 Shift를 함께 누르면 한쪽의 누름·뗌 이벤트가 오지 않는 플랫폼이 있다(macOS, Windows). 2인 모드에서는
     * Shift를 1P(왼쪽)만 쓰지만, 1P가 왼쪽 Shift를 누른 채 누군가 오른쪽 Shift를 누르면 1P의 뗌이 빠질 수 있고,
     * 그대로 두면 1P의 다음 누름을 자동 반복으로 여겨 받지 않는다. 수정 키를 모두 떼면 그 뒤 이벤트에는 그 키가 없으므로
     * 그때 바로잡는다. 2P의 아이템 키(오른쪽 Ctrl·Option·Alt)도 같은 규칙을 따른다.
     * 수정 키 상태에 나오지 않는 . 같은 글자 키는 뗌 이벤트로만 뗀다.
     *
     * @param pressedKeyCode 이 이벤트가 누른 키. 누른 그 키는 수정 키 상태와 상관없이 그대로 둔다. 누름이 아니면 {@link KeyEvent#VK_UNDEFINED}
     */
    void forgetReleasedModifiers(int modifiersEx, int pressedKeyCode) {
        held.entrySet().removeIf(entry -> {
            int code = entry.getKey().code();
            return modifierMask(code) != 0 && code != pressedKeyCode
                    && (modifiersEx & modifierMasks(entry.getValue())) == 0;
        });
    }

    /** 이번 틱에 할 일. 막 누른 행동은 여기서 한 번 내주고 지운다. */
    PlayerCommand takeCommand() {
        PlayerCommand command = new PlayerCommand(movement.current(),
                pressed.contains(Action.BALLOON), pressed.contains(Action.ITEM));
        pressed.clear();
        return command;
    }

    /** 포커스를 잃으면 키 뗌 이벤트가 오지 않으므로 전부 비운다. */
    void clear() {
        movement.clear();
        held.clear();
        pressed.clear();
    }

    /** 지금 누르고 있는 이동 방향. 없으면 null */
    Direction heldDirection() {
        return movement.current();
    }

    /** action에 묶인 수정 키들의 눌림 표시를 합친 것 */
    private int modifierMasks(Action action) {
        int masks = 0;
        for (KeyBindings.Key key : keys.keysOf(action)) {
            masks |= modifierMask(key.code());
        }
        return masks;
    }

    /** 수정 키의 눌림 표시. 수정 키가 아니면 0 */
    private static int modifierMask(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_SHIFT -> InputEvent.SHIFT_DOWN_MASK;
            case KeyEvent.VK_CONTROL -> InputEvent.CTRL_DOWN_MASK;
            case KeyEvent.VK_ALT -> InputEvent.ALT_DOWN_MASK;
            case KeyEvent.VK_ALT_GRAPH -> InputEvent.ALT_GRAPH_DOWN_MASK;
            default -> 0;
        };
    }
}
