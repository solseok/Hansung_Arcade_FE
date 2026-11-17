package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 이 컴퓨터 키보드로 조작하는 플레이어들의 입력. 키 이벤트를 플레이어마다 {@link KeyBindings 키 배치}대로 나눠 받고,
 * 틱마다 플레이어별 {@link PlayerCommand}를 만든다.
 *
 * <p>macOS의 JDK 21은 오른쪽 Option을 위치 {@link KeyEvent#KEY_LOCATION_STANDARD}로 보내고, 뗄 때도
 * {@link KeyEvent#KEY_RELEASED} 대신 수정 키 표시에 Option이 없는 {@link KeyEvent#KEY_PRESSED}를 한 번 더 보낸다
 * (JDK 24는 위치 RIGHT, 뗌 RELEASED). 그래서 macOS에서는 왼쪽이 아닌 Option을 오른쪽 Option으로 읽고,
 * Option 표시 없이 온 Option 누름을 뗌으로 읽는다. 왼쪽 Option, 좌우 Shift·Ctrl은 JDK 21에서도 위치와 뗌이 맞게 온다.</p>
 *
 * <p>게임 루프와 같은 EDT에서만 쓴다.</p>
 */
public final class LocalInput {

    private final List<PlayerControls> players;
    /** macOS인지. 오른쪽 Option 이벤트를 바로잡는다 */
    private final boolean mac;

    /**
     * @param playerIds 이 키보드로 조작하는 플레이어 번호. 1P, 2P 순서이고 키 배치는 {@link KeyBindings#forLocalPlayers}를 따른다
     */
    public LocalInput(List<Integer> playerIds) {
        this(playerIds, KeyBindings.MAC);
    }

    /**
     * @param mac macOS 키 배치와 이벤트로 받을지. 테스트에서 이 컴퓨터와 다른 운영체제를 흉내 낼 때 쓴다
     */
    LocalInput(List<Integer> playerIds, boolean mac) {
        List<KeyBindings> bindings = KeyBindings.forLocalPlayers(playerIds.size(), mac);
        List<PlayerControls> controls = new ArrayList<>();
        for (int i = 0; i < playerIds.size(); i++) {
            controls.add(new PlayerControls(playerIds.get(i), bindings.get(i)));
        }
        this.players = List.copyOf(controls);
        this.mac = mac;
    }

    /**
     * @param typing 채팅 입력칸에서 받은 키인지. 그때는 방향키만 받고, 글자 키·Space·Shift 같은 나머지 키는 글을 치는 데 쓰도록 받지 않는다
     * @return 누군가의 키여서 받았는지. 받은 키는 부르는 쪽이 소비해서 다른 동작(입력칸 커서 이동 등)으로 이어지지 않게 한다
     */
    public boolean keyPressed(KeyEvent e, boolean typing) {
        int keyCode = e.getKeyCode();
        int location = keyLocation(e);
        if (isReleaseSentAsPress(e)) {
            return release(keyCode, location, e.getModifiersEx());
        }
        boolean used = false;
        for (PlayerControls player : players) {
            used |= player.press(keyCode, location, typing);
            player.forgetReleasedModifiers(e.getModifiersEx(), keyCode);
        }
        return used;
    }

    /**
     * 뗌은 채팅 입력칸에서 와도 받는다. 입력칸을 열기 전부터 누르고 있던 키를 입력칸에서 떼도 멈춰야 한다.
     *
     * @return 누군가의 키여서 받았는지
     */
    public boolean keyReleased(KeyEvent e) {
        return release(e.getKeyCode(), keyLocation(e), e.getModifiersEx());
    }

    private boolean release(int keyCode, int location, int modifiersEx) {
        boolean used = false;
        for (PlayerControls player : players) {
            used |= player.release(keyCode, location);
            player.forgetReleasedModifiers(modifiersEx, KeyEvent.VK_UNDEFINED);
        }
        return used;
    }

    /** 키의 좌우 위치. macOS에서 왼쪽이 아닌 Option은 오른쪽 Option이다 (JDK 21은 위치 STANDARD로 보낸다) */
    private int keyLocation(KeyEvent e) {
        if (mac && e.getKeyCode() == KeyEvent.VK_ALT && e.getKeyLocation() != KeyEvent.KEY_LOCATION_LEFT) {
            return KeyEvent.KEY_LOCATION_RIGHT;
        }
        return e.getKeyLocation();
    }

    /**
     * macOS에서 Option 표시 없이 온 Option 누름인지. JDK 21은 오른쪽 Option을 뗄 때 이렇게 보낸다.
     * 누름으로 받으면 그 Option을 누른 채로 남겨, 다음 누름을 자동 반복으로 여겨 거른다.
     * 진짜 Option 누름에는 JDK 21·24 모두 Option 표시가 있다.
     */
    private boolean isReleaseSentAsPress(KeyEvent e) {
        return mac && e.getKeyCode() == KeyEvent.VK_ALT && (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == 0;
    }

    /** 포커스를 잃으면 키 뗌 이벤트가 오지 않으므로 모든 플레이어의 키를 비운다. */
    public void clear() {
        players.forEach(PlayerControls::clear);
    }

    /**
     * 이번 틱에 플레이어마다 할 일 (플레이어 번호 → 명령). 막 누른 물풍선·아이템은 여기서 한 번 내주고 지우므로
     * 틱마다 한 번씩 부른다.
     */
    public Map<Integer, PlayerCommand> takeCommands() {
        Map<Integer, PlayerCommand> commands = new LinkedHashMap<>();
        for (PlayerControls player : players) {
            commands.put(player.playerId(), player.takeCommand());
        }
        return commands;
    }

    /** index번째(0부터, 0이 1P) 플레이어가 지금 누르고 있는 이동 방향. 없으면 null */
    public Direction heldDirection(int index) {
        return players.get(index).heldDirection();
    }
}
