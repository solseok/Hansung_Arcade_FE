package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

/**
 * 플레이어 한 명의 키 배치. 이동 방향 키 네 개와 물풍선·아이템 키.
 *
 * <p>한 키보드 2인 모드에서는 1P가 키보드 왼쪽(W/A/S/D 둘레), 2P가 오른쪽(방향키 둘레)을 쓴다.
 * Shift는 1P의 왼쪽 Shift만 쓴다. 좌우 Shift를 둘이 나눠 쓰면 한쪽을 누른 채 다른 쪽을 누르거나 뗀 이벤트가 오지 않는
 * 플랫폼(macOS, Windows)이 있어서다. 2P의 아이템 키는 운영체제마다 다르다({@link #player2}).
 * macOS에서는 Ctrl+방향키가 데스크톱 전환이라 방향키를 쓰는 2P에게 Ctrl을 주지 않고 오른쪽 Option을 준다.
 * Windows 등에서는 오른쪽 Ctrl이고, 오른쪽 Ctrl이 없는 키보드를 위해 오른쪽 Alt(유럽식 배열의 AltGr 포함)도 받는다.
 * 한국어 Windows 키보드는 오른쪽 Alt·Ctrl이 한/영·한자 키로 들어와 이 키들이 없으므로, 운영체제와 상관없이
 * 물풍선 키 / 옆의 . (마침표)도 2P 아이템 키로 받는다.
 * 왼쪽·오른쪽은 {@link KeyEvent#getKeyLocation()}으로 가린다. macOS JDK 21이 오른쪽 Option을 위치 STANDARD로 보내는 것은
 * {@link LocalInput}이 오른쪽으로 바로잡아 넘긴다.</p>
 *
 * @param up      위로 이동 ({@link KeyEvent#VK_UP} 같은 키 코드)
 * @param down    아래로 이동
 * @param left    왼쪽으로 이동
 * @param right   오른쪽으로 이동
 * @param balloon 물풍선 놓기
 * @param item    아이템 쓰기. 여러 개면 어느 것을 눌러도 된다. 첫 키가 대표 키다 (화면에 이름을 보여 줄 키)
 */
public record KeyBindings(int up, int down, int left, int right, Key balloon, List<Key> item) {

    /** 이 컴퓨터가 macOS인지. 키 이름과 2P 아이템 키가 달라진다 */
    public static final boolean MAC = isMac(System.getProperty("os.name", ""));

    /** 혼자 할 때: 방향키 이동, Space 물풍선, Shift(좌우 아무거나) 아이템 */
    public static final KeyBindings SOLO = new KeyBindings(VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT,
            Key.any(VK_SPACE), List.of(Key.any(VK_SHIFT)));
    /** 한 키보드 2인의 1P: W/A/S/D 이동, Space 물풍선, 왼쪽 Shift 아이템 */
    public static final KeyBindings PLAYER_1 = new KeyBindings(VK_W, VK_S, VK_A, VK_D,
            Key.any(VK_SPACE), List.of(Key.left(VK_SHIFT)));
    /** 한 키보드 2인의 2P: 방향키 이동, / 물풍선, 이 컴퓨터의 운영체제에 맞는 아이템 키와 .({@link #player2}) */
    public static final KeyBindings PLAYER_2 = player2(MAC);

    public KeyBindings {
        item = List.copyOf(item);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("아이템 키가 하나는 있어야 합니다.");
        }
    }

    /**
     * 한 키보드 2인의 2P 키 배치. 방향키 이동, / 물풍선.
     * 아이템은 macOS면 오른쪽 Option, 그 밖에서는 오른쪽 Ctrl이고 오른쪽 Ctrl이 없는 키보드를 위해 오른쪽 Alt·AltGr도 된다.
     * 이 수정 키들이 없는 키보드(한국어 Windows 등)를 위해 어디서나 . 도 된다. 화면에 이름을 보여 줄 대표 키는 수정 키다.
     *
     * @param mac macOS인지
     */
    static KeyBindings player2(boolean mac) {
        List<Key> item = mac
                ? List.of(Key.right(VK_ALT), Key.any(VK_PERIOD))
                : List.of(Key.right(VK_CONTROL), Key.right(VK_ALT), Key.any(VK_ALT_GRAPH), Key.any(VK_PERIOD));
        return new KeyBindings(VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT, Key.any(VK_SLASH), item);
    }

    static boolean isMac(String osName) {
        return osName.toLowerCase(Locale.ROOT).startsWith("mac");
    }

    /** 화면 아이템 칸에 적는 대표 아이템 키 이름 (예: Shift, L-Shift, Ctrl-R, Opt-R) */
    public String itemKeyName() {
        return item.get(0).shortName(MAC);
    }

    /** 안내 글에 적는 아이템 키 이름. 여러 개면 "오른쪽 Option 또는 마침표(.)"처럼 잇는다 */
    public String itemKeyGuide() {
        return item.stream().map(key -> key.koreanName(MAC)).collect(Collectors.joining(" 또는 "));
    }

    /**
     * 한 키보드로 함께 하는 인원에 맞는 키 배치. 0번째가 1P 몫이다.
     *
     * @throws IllegalArgumentException 1명이나 2명이 아닐 때
     */
    public static List<KeyBindings> forLocalPlayers(int count) {
        return forLocalPlayers(count, MAC);
    }

    /**
     * 운영체제를 정해 고르는 {@link #forLocalPlayers(int)}.
     *
     * @param mac macOS인지. 2P 아이템 키가 달라진다
     */
    static List<KeyBindings> forLocalPlayers(int count, boolean mac) {
        return switch (count) {
            case 1 -> List.of(SOLO);
            case 2 -> List.of(PLAYER_1, player2(mac));
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
        return actionKey(keyCode, keyLocation) != null ? Action.ITEM : null;
    }

    /** 이 배치에서 keyCode 키(좌우 위치 keyLocation)에 맞는 물풍선·아이템 키. 행동 키가 아니면 null. */
    Key actionKey(int keyCode, int keyLocation) {
        if (balloon.matches(keyCode, keyLocation)) {
            return balloon;
        }
        for (Key key : item) {
            if (key.matches(keyCode, keyLocation)) {
                return key;
            }
        }
        return null;
    }

    /** 누른 순간 한 번만 하는 행동 */
    public enum Action {
        BALLOON,
        ITEM
    }

    /** action에 묶인 키들 */
    public List<Key> keysOf(Action action) {
        return switch (action) {
            case BALLOON -> List.of(balloon);
            case ITEM -> item;
        };
    }

    /**
     * 키 하나. Shift처럼 좌우에 하나씩 있는 키는 {@link KeyEvent#getKeyLocation()}으로 어느 쪽인지 가린다.
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

        /** 오른쪽에 있는 것만 (오른쪽 Ctrl 등) */
        public static Key right(int code) {
            return new Key(code, KeyEvent.KEY_LOCATION_RIGHT);
        }

        public boolean matches(int keyCode, int keyLocation) {
            return keyCode == code && (location == ANY_LOCATION || location == keyLocation);
        }

        /** 좁은 칸에 적는 짧은 이름. 왼쪽 키는 앞에 "L-", 오른쪽 키는 뒤에 "-R"을 붙인다 (예: L-Shift, Ctrl-R) */
        public String shortName(boolean mac) {
            String name = baseName(mac);
            return switch (location) {
                case KeyEvent.KEY_LOCATION_LEFT -> "L-" + name;
                case KeyEvent.KEY_LOCATION_RIGHT -> name + "-R";
                default -> name;
            };
        }

        /** 안내 글에 적는 이름 (예: 왼쪽 Shift, 오른쪽 Option, 마침표(.)) */
        public String koreanName(boolean mac) {
            String name = switch (code) {
                case VK_ALT -> mac ? "Option" : "Alt";
                case VK_PERIOD -> "마침표(.)";
                default -> baseName(mac);
            };
            return switch (location) {
                case KeyEvent.KEY_LOCATION_LEFT -> "왼쪽 " + name;
                case KeyEvent.KEY_LOCATION_RIGHT -> "오른쪽 " + name;
                default -> name;
            };
        }

        private String baseName(boolean mac) {
            return switch (code) {
                case VK_SHIFT -> "Shift";
                case VK_CONTROL -> "Ctrl";
                case VK_ALT -> mac ? "Opt" : "Alt";
                case VK_ALT_GRAPH -> "AltGr";
                case VK_SPACE -> "Space";
                // getKeyText는 언어 설정에 따라 "Period"·"마침표"처럼 바뀐다
                case VK_PERIOD -> ".";
                default -> KeyEvent.getKeyText(code);
            };
        }
    }
}
