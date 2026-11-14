package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

import java.awt.event.KeyEvent;
import java.util.List;

import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SLASH;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_W;

/**
 * 플레이어 한 명의 키 배치. 이동 방향 키 네 개와 물풍선·아이템 키.
 *
 * <p>한 키보드 2인 모드에서는 1P가 키보드 왼쪽(W/A/S/D 둘레), 2P가 오른쪽(방향키 둘레)을 쓴다.
 * 좌우에 하나씩 있는 Shift·Ctrl은 {@link KeyEvent#getKeyLocation()}으로 어느 쪽인지 가린다.
 * macOS에서는 Ctrl+←/→가 데스크톱 전환 단축키라, 방향키를 쓰는 쪽에는 Ctrl을 두지 않았다.</p>
 *
 * @param up      위로 이동 ({@link KeyEvent#VK_UP} 같은 키 코드)
 * @param down    아래로 이동
 * @param left    왼쪽으로 이동
 * @param right   오른쪽으로 이동
 * @param balloon 물풍선 놓기
 * @param item    아이템 쓰기
 */
public record KeyBindings(int up, int down, int left, int right, Key balloon, Key item) {

    /** 혼자 할 때: 방향키 이동, Space 물풍선, Shift(좌우 아무거나) 아이템 */
    public static final KeyBindings SOLO = new KeyBindings(VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT,
            Key.any(VK_SPACE), Key.any(VK_SHIFT));
    /** 한 키보드 2인의 1P: W/A/S/D 이동, 왼쪽 Shift 물풍선, 왼쪽 Ctrl 아이템 */
    public static final KeyBindings PLAYER_1 = new KeyBindings(VK_W, VK_S, VK_A, VK_D,
            Key.left(VK_SHIFT), Key.left(VK_CONTROL));
    /** 한 키보드 2인의 2P: 방향키 이동, 오른쪽 Shift 물풍선, / 아이템 */
    public static final KeyBindings PLAYER_2 = new KeyBindings(VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT,
            Key.right(VK_SHIFT), Key.any(VK_SLASH));

    /**
     * 한 키보드로 함께 하는 인원에 맞는 키 배치. 0번째가 1P 몫이다.
     *
     * @throws IllegalArgumentException 1명이나 2명이 아닐 때
     */
    public static List<KeyBindings> forLocalPlayers(int count) {
        return switch (count) {
            case 1 -> List.of(SOLO);
            case 2 -> List.of(PLAYER_1, PLAYER_2);
            default -> throw new IllegalArgumentException("한 키보드로는 1~2명만 할 수 있습니다: " + count);
        };
    }

    /** 방향키는 글자를 만들지 않아서, 채팅 입력칸에 글을 치는 동안에도 게임이 받는다. */
    public static boolean worksWhileTyping(int keyCode) {
        return keyCode == VK_UP || keyCode == VK_DOWN || keyCode == VK_LEFT || keyCode == VK_RIGHT;
    }

    /** 이 배치에서 keyCode가 움직이는 방향. 이동 키가 아니면 null. */
    public Direction directionOf(int keyCode) {
        if (keyCode == up) {
            return Direction.UP;
        }
        if (keyCode == down) {
            return Direction.DOWN;
        }
        if (keyCode == left) {
            return Direction.LEFT;
        }
        if (keyCode == right) {
            return Direction.RIGHT;
        }
        return null;
    }

    /** 이 배치에서 keyCode 키(좌우 위치 keyLocation)가 하는 행동. 행동 키가 아니면 null. */
    public Action actionOf(int keyCode, int keyLocation) {
        if (balloon.matches(keyCode, keyLocation)) {
            return Action.BALLOON;
        }
        if (item.matches(keyCode, keyLocation)) {
            return Action.ITEM;
        }
        return null;
    }

    /** 누른 순간 한 번만 하는 행동 */
    public enum Action {
        BALLOON,
        ITEM
    }

    /**
     * 키 하나. Shift·Ctrl처럼 좌우에 하나씩 있는 키는 {@link KeyEvent#getKeyLocation()}으로 어느 쪽인지 가린다.
     *
     * @param code     키 코드 ({@link KeyEvent#VK_SHIFT} 등)
     * @param location {@link KeyEvent#KEY_LOCATION_LEFT}·{@link KeyEvent#KEY_LOCATION_RIGHT}, 어느 쪽이든 되면 {@link #ANY_LOCATION}
     */
    public record Key(int code, int location) {

        public static final int ANY_LOCATION = -1;

        /** 좌우를 가리지 않는 키 */
        public static Key any(int code) {
            return new Key(code, ANY_LOCATION);
        }

        /** 왼쪽에 있는 것만 (왼쪽 Shift 등) */
        public static Key left(int code) {
            return new Key(code, KeyEvent.KEY_LOCATION_LEFT);
        }

        /** 오른쪽에 있는 것만 (오른쪽 Shift 등) */
        public static Key right(int code) {
            return new Key(code, KeyEvent.KEY_LOCATION_RIGHT);
        }

        public boolean matches(int keyCode, int keyLocation) {
            return keyCode == code && (location == ANY_LOCATION || location == keyLocation);
        }
    }
}
