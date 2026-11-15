package hansung.arcade.outgame.lobby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 한 클라이언트의 입장 상태를 보관하는 오프라인 서비스. */
public final class MockLobbyService implements LobbyService {
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private Integer joinedRoom;
    private boolean host;

    public MockLobbyService() {
        add(new Room(1, "함께 한 판 해요", 1, 4, "캠프 08", Room.Status.WAITING));
        add(new Room(2, "빌리지에서 만나요", 2, 4, "빌리지 10", Room.Status.WAITING));
        add(new Room(3, "마지막 자리까지 완료", 4, 4, "패트릿 14", Room.Status.WAITING));
        add(new Room(4, "신나는 캠프", 3, 4, "캠프 08", Room.Status.PLAYING));
        add(new Room(5, "친구들과 대결", 4, 4, "빌리지 10", Room.Status.PLAYING));
        for (int number = 6; number <= 22; number++) {
            add(new Room(number, "즐거운 한 판 " + number, 1, 4, "캠프 08",
                    number % 3 == 0 ? Room.Status.PLAYING : Room.Status.WAITING));
        }
    }

    private void add(Room room) {
        rooms.put(room.number(), room);
    }

    @Override
    public synchronized List<Room> rooms() {
        return List.copyOf(rooms.values());
    }

    @Override
    public synchronized Room join(int roomNumber) {
        if (joinedRoom != null) {
            throw new IllegalStateException("이미 방에 입장해 있습니다.");
        }
        Room room = rooms.get(roomNumber);
        if (room == null || !room.canJoin()) {
            throw new IllegalStateException("입장할 수 없는 방입니다. 목록을 다시 확인해 주세요.");
        }
        Room joined = withPlayers(room, room.players() + 1);
        add(joined);
        joinedRoom = roomNumber;
        return joined;
    }

    @Override
    public synchronized Room create(String title, int capacity) {
        if (joinedRoom != null) {
            throw new IllegalStateException("이미 방에 입장해 있습니다.");
        }
        if (title == null || title.isBlank() || title.strip().length() > 30) {
            throw new IllegalArgumentException("방 제목은 1~30자로 입력해 주세요.");
        }
        if (capacity < 2 || capacity > 8) {
            throw new IllegalArgumentException("정원은 2~8명으로 선택해 주세요.");
        }
        int number = rooms.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Room room = new Room(number, title.strip(), 1, capacity, "캠프 08", Room.Status.WAITING);
        add(room);
        joinedRoom = number;
        host = true;
        return room;
    }

    @Override
    public synchronized Room quickJoin() {
        int number = rooms.values().stream().filter(Room::canJoin).mapToInt(Room::number).findFirst()
                .orElseThrow(() -> new IllegalStateException("입장 가능한 방이 없습니다."));
        return join(number);
    }

    @Override
    public synchronized void leave() {
        if (joinedRoom != null) {
            Room room = rooms.get(joinedRoom);
            if (room.players() == 1) {
                rooms.remove(joinedRoom);
            } else {
                add(withPlayers(room, room.players() - 1));
            }
            joinedRoom = null;
            host = false;
        }
    }

    @Override
    public synchronized boolean isHost() {
        return host;
    }

    private Room withPlayers(Room room, int players) {
        return new Room(room.number(), room.title(), players, room.capacity(), room.map(), room.status());
    }
}
