package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 방향키 입력 상태. 여러 키를 동시에 누르고 있으면 가장 마지막에 누른 방향을 쓴다.
 */
public final class DirectionInput {

    private final Deque<Direction> pressed = new ArrayDeque<>();

    public void press(Direction direction) {
        pressed.remove(direction);
        pressed.addLast(direction);
    }

    public void release(Direction direction) {
        pressed.remove(direction);
    }

    /** 창이 포커스를 잃으면 키 뗌 이벤트가 오지 않으므로 전부 비운다. */
    public void clear() {
        pressed.clear();
    }

    /** 현재 이동 방향. 아무 키도 안 눌렀으면 null. */
    public Direction current() {
        return pressed.peekLast();
    }
}
