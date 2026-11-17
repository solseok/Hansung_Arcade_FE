package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.GameMaps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 한 클라이언트의 입장 상태를 보관하는 오프라인 서비스. */
public final class MockLobbyService implements LobbyService {
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private Integer joinedRoom;
    private boolean host;
    private int localSeats;
    /** 방을 만들거나 들어갈 때의 정원. 맵 최대 인원 때문에 줄었다가 다른 맵으로 바꾸면 이 값으로 돌아온다 */
    private int configuredCapacity;

    public MockLobbyService() {
        add(new Room(1, "함께 한 판 해요", 1, 8, "캠프 08", Room.Status.WAITING));
        add(new Room(2, "빌리지에서 만나요", 2, 6, "빌리지 10", Room.Status.WAITING));
        add(new Room(3, "마지막 자리까지 완료", 8, 8, "패트릿 14", Room.Status.WAITING));
        add(new Room(4, "신나는 캠프", 6, 8, "캠프 08", Room.Status.PLAYING));
        add(new Room(5, "친구들과 대결", 6, 6, "빌리지 10", Room.Status.PLAYING));
        // 8개씩 네 페이지: 대기방 페이지, 혼합 페이지, 마지막 게임중 페이지를 제공한다.
        for (int number = 6; number <= 30; number++) {
            add(
                    new Room(
                            number,
                            "즐거운 한 판 " + number,
                            1,
                            8,
                            "캠프 08",
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
    public synchronized Room join(int roomNumber, int seats) {
        if (joinedRoom != null) {
            throw new IllegalStateException("이미 방에 입장해 있습니다.");
        }
        Room room = rooms.get(roomNumber);
        if (seats < 1 || seats > 2) throw new IllegalArgumentException("참가 인원은 1~2명입니다.");
        if (room == null || !room.canJoin() || room.players() + seats > room.capacity()) {
            throw new IllegalStateException("입장할 수 없는 방입니다. 목록을 다시 확인해 주세요.");
        }
        Room joined = withPlayers(room, room.players() + seats);
        add(joined);
        joinedRoom = roomNumber;
        localSeats = seats;
        configuredCapacity = room.capacity();
        return joined;
    }

    @Override
    public synchronized Room create(String title, int capacity, int seats) {
        if (joinedRoom != null) {
            throw new IllegalStateException("이미 방에 입장해 있습니다.");
        }
        if (title == null || title.isBlank() || title.strip().length() > 30) {
            throw new IllegalArgumentException("방 제목은 1~30자로 입력해 주세요.");
        }
        if (capacity < 2 || capacity > 8 || seats < 1 || seats > 2 || seats > capacity) {
            throw new IllegalArgumentException("정원은 2~8명으로 선택해 주세요.");
        }
        int number = rooms.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Room room = new Room(number, title.strip(), seats, capacity, "캠프 08", Room.Status.WAITING);
        add(room);
        joinedRoom = number;
        localSeats = seats;
        configuredCapacity = capacity;
        host = true;
        return room;
    }

    @Override
    public synchronized Room quickJoin(int seats) {
        int number =
                rooms.values().stream()
                        .filter(r -> r.canJoin() && r.players() + seats <= r.capacity())
                        .mapToInt(Room::number)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("입장 가능한 방이 없습니다."));
        return join(number, seats);
    }

    @Override
    public synchronized void leave() {
        if (joinedRoom != null) {
            Room room = rooms.get(joinedRoom);
            if (room.players() <= localSeats) {
                rooms.remove(joinedRoom);
            } else {
                add(withPlayers(room, room.players() - localSeats));
            }
            joinedRoom = null;
            host = false;
        }
    }

    @Override
    public synchronized boolean isHost() {
        return host;
    }

    @Override
    public synchronized Room updateRoom(String map, Room.Status status) {
        if (joinedRoom == null || !host) {
            throw new IllegalStateException("방장만 방 설정을 변경할 수 있습니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("올바르지 않은 방 설정입니다.");
        }
        Room old = rooms.get(joinedRoom);
        int capacity = Math.min(configuredCapacity, GameMaps.byName(map).maxPlayers());
        if (old.players() > capacity) {
            throw new IllegalStateException("이 맵은 " + capacity + "명까지 입장할 수 있습니다.");
        }
        Room updated = new Room(old.number(), old.title(), old.players(), capacity, map, status);
        add(updated);
        return updated;
    }

    private Room withPlayers(Room room, int players) {
        return new Room(
                room.number(), room.title(), players, room.capacity(), room.map(), room.status());
    }
}
