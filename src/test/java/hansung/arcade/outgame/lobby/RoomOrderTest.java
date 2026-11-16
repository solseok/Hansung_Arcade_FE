package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 7. 대기중/게임중 탭으로 나누지 않고, 한 목록에서 대기 중인 방을 앞에, 게임 중인 방을 뒤에 둔다.
 * 그중에서도 바로 들어갈 수 있는 방(자리가 남은 대기 방)을 맨 앞에 둬서 먼저 들어가게 한다.
 */
@ExtendWith(RunOnEdt.class)
class RoomOrderTest {

    private static final Room OPEN_9 = new Room(9, "자리 있음", 3, 8, "캠프 08", Room.Status.WAITING);
    private static final Room FULL_2 = new Room(2, "가득 참", 8, 8, "캠프 08", Room.Status.WAITING);
    private static final Room PLAYING_1 = new Room(1, "게임 중", 4, 8, "빌리지 10", Room.Status.PLAYING);
    private static final Room OPEN_4 = new Room(4, "자리 있음", 1, 2, "패트릿 14", Room.Status.WAITING);
    private static final Room PLAYING_3 = new Room(3, "게임 중", 8, 8, "캠프 08", Room.Status.PLAYING);

    @Test
    void 들어갈_수_있는_대기_방_가득_찬_대기_방_게임_중인_방_순서() {
        List<Room> sorted = RoomOrder.sort(List.of(OPEN_9, FULL_2, PLAYING_1, OPEN_4, PLAYING_3));

        assertEquals(List.of(4, 9, 2, 1, 3), sorted.stream().map(Room::number).toList());
    }

    @Test
    void 원래_목록은_건드리지_않는다() {
        List<Room> rooms = new ArrayList<>(List.of(PLAYING_1, OPEN_4));

        RoomOrder.sort(rooms);

        assertEquals(List.of(PLAYING_1, OPEN_4), rooms);
    }

    @Test
    void 로비에_대기중_게임중_탭이_없다() {
        LobbyPanel lobby = lobby(new MockLobbyService());

        assertTrue(SwingTree.all(lobby, JTabbedPane.class).isEmpty());
    }

    @Test
    void 쪽을_넘겨_봐도_대기_방이_모두_게임_중인_방보다_앞에_있다() {
        MockLobbyService service = new MockLobbyService();
        LobbyPanel lobby = lobby(service);

        List<Room> shown = new ArrayList<>();
        AbstractButton next = SwingTree.button(lobby, "다음쪽");
        while (true) {
            for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
                card.room().ifPresent(shown::add);
            }
            if (!next.isEnabled()) {
                break;
            }
            next.doClick();
        }

        assertEquals(service.rooms().size(), shown.size(), "탭 없이 모든 방이 한 목록에 나와야 합니다.");
        assertEquals(RoomOrder.sort(service.rooms()), shown);
        int firstPlaying = indexOfFirst(shown, Room.Status.PLAYING);
        assertTrue(firstPlaying > 0);
        assertTrue(shown.subList(firstPlaying, shown.size()).stream().allMatch(room -> room.status() == Room.Status.PLAYING));
        assertTrue(shown.getFirst().canJoin(), "첫 칸은 바로 들어갈 수 있는 방이어야 합니다.");
    }

    private static int indexOfFirst(List<Room> rooms, Room.Status status) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).status() == status) {
                return i;
            }
        }
        return -1;
    }

    private static LobbyPanel lobby(MockLobbyService service) {
        OutgameSession session = new OutgameSession(service);
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        LobbyPanel lobby = new LobbyPanel(session, scenes);
        scenes.register("lobby", lobby);
        scenes.show("lobby", null);
        return lobby;
    }
}
