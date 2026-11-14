package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 이 컴퓨터 키보드로 조작하는 플레이어들의 입력. 키 이벤트를 플레이어마다 {@link KeyBindings 키 배치}대로 나눠 받고,
 * 틱마다 플레이어별 {@link PlayerCommand}를 만든다.
 *
 * <p>게임 루프와 같은 EDT에서만 쓴다.</p>
 */
public final class LocalInput {

    private final List<PlayerControls> players;

    /**
     * @param playerIds 이 키보드로 조작하는 플레이어 번호. 1P, 2P 순서이고 키 배치는 {@link KeyBindings#forLocalPlayers}를 따른다
     */
    public LocalInput(List<Integer> playerIds) {
        List<KeyBindings> bindings = KeyBindings.forLocalPlayers(playerIds.size());
        List<PlayerControls> controls = new ArrayList<>();
        for (int i = 0; i < playerIds.size(); i++) {
            controls.add(new PlayerControls(playerIds.get(i), bindings.get(i)));
        }
        this.players = List.copyOf(controls);
    }

    /**
     * @param typing 채팅 입력칸에서 받은 키인지. 그때는 방향키만 받고, 글자 키·Space·Shift 같은 나머지 키는 글을 치는 데 쓰도록 받지 않는다
     * @return 누군가의 키여서 받았는지. 받은 키는 부르는 쪽이 소비해서 다른 동작(입력칸 커서 이동 등)으로 이어지지 않게 한다
     */
    public boolean keyPressed(KeyEvent e, boolean typing) {
        boolean used = false;
        for (PlayerControls player : players) {
            used |= player.press(e.getKeyCode(), e.getKeyLocation(), typing);
            player.forgetReleasedModifiers(e.getModifiersEx(), e.getKeyCode());
        }
        return used;
    }

    /**
     * 뗌은 채팅 입력칸에서 와도 받는다. 입력칸을 열기 전부터 누르고 있던 키를 입력칸에서 떼도 멈춰야 한다.
     *
     * @return 누군가의 키여서 받았는지
     */
    public boolean keyReleased(KeyEvent e) {
        boolean used = false;
        for (PlayerControls player : players) {
            used |= player.release(e.getKeyCode(), e.getKeyLocation());
            player.forgetReleasedModifiers(e.getModifiersEx(), KeyEvent.VK_UNDEFINED);
        }
        return used;
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
