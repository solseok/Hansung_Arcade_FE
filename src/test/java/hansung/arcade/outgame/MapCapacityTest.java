package hansung.arcade.outgame;

import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 18. 맵마다 최대 인원이 있다: 캠프·패트릿 8명, 빌리지 6명. 방 정원은 맵 최대 인원을 넘지 않는다.
 */
class MapCapacityTest {

    @Test
    void 캠프_패트릿은_8명_빌리지는_6명() {
        assertEquals(8, GameMaps.byId("camp/camp08").maxPlayers());
        assertEquals(8, GameMaps.byId("patrit/patrit14").maxPlayers());
        assertEquals(6, GameMaps.byId("village/village10").maxPlayers());
    }

    @Test
    void 목업_방_목록의_빌리지_방은_6명까지다() {
        List<Room> village = new MockLobbyService().rooms().stream()
                .filter(room -> room.map().equals("빌리지 10")).toList();

        assertFalse(village.isEmpty());
        for (Room room : village) {
            assertTrue(room.capacity() <= 6 && room.players() <= room.capacity(), room.toString());
        }
    }

    @Test
    void 빌리지로_바꾸면_정원이_6명으로_줄고_다른_맵으로_바꾸면_돌아온다() {
        MockLobbyService service = new MockLobbyService();
        OutgameSession session = new OutgameSession(service);
        session.login("부기");
        Room room = session.create("정원 확인", 8);

        session.selectMap("village/village10");
        assertEquals(6, session.room().capacity());
        assertEquals(6, capacityOf(service, room.number()));

        session.selectMap("camp/camp08");
        assertEquals(8, session.room().capacity());
        assertEquals(8, capacityOf(service, room.number()));
    }

    @Test
    void 사람이_6명보다_많은_방은_빌리지로_바꿀_수_없다() {
        OutgameSession session = new OutgameSession(new CrowdedHostService());
        session.login("부기");
        session.enter(new Room(1, "북적이는 방", 7, 8, "캠프 08", Room.Status.WAITING));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> session.selectMap("village/village10"));

        assertTrue(e.getMessage().contains("6명"), e.getMessage());
        assertEquals("camp/camp08", session.mapId());
    }

    private static int capacityOf(MockLobbyService service, int number) {
        return service.rooms().stream().filter(r -> r.number() == number).findFirst().orElseThrow().capacity();
    }

    /** 7명이 있는 방의 방장인 서비스. 맵을 바꾸라는 요청이 오면 안 된다 */
    private static final class CrowdedHostService implements LobbyService {
        @Override
        public List<Room> rooms() {
            return List.of();
        }

        @Override
        public Room join(int roomNumber, int seats) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Room create(String title, int capacity, int seats) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isHost() {
            return true;
        }

        @Override
        public Room quickJoin(int seats) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void leave() {
        }

        @Override
        public Room changeMap(String mapName) {
            throw new AssertionError("6명이 넘는 방은 빌리지로 바꾸라고 요청하면 안 됩니다.");
        }
    }
}
