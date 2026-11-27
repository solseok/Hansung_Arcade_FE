package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.GameMaps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 한 클라이언트의 입장 상태를 보관하는 오프라인 서비스. 방은 없이 시작하고, 이 컴퓨터에서 만든 방만 있다.
 * 들어간 방의 자리 열림·닫힘은 서버({@code Room})와 같은 규칙이다: 정원은 열린 자리 수이고,
 * 방장이 닫은 자리와 맵 최대 인원 밖 자리가 닫힌 자리다. 이 컴퓨터의 플레이어와 목업 참가자는 0번부터 차례로 앉는다.
 */
public final class MockLobbyService implements LobbyService {
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private Integer joinedRoom;
    private boolean host;
    private int localSeats;
    /** 들어간 방에서 방장이 닫은 자리. 맵 최대 인원 밖 자리는 맵을 되돌리면 이 값대로 돌아온다 */
    private final boolean[] closedByHost = new boolean[RoomState.SLOTS];

    public MockLobbyService() {
        this(List.of());
    }

    /** rooms가 미리 있는 로비 (테스트에서 목록·입장을 볼 때 쓴다) */
    public MockLobbyService(List<Room> rooms) {
        rooms.forEach(this::add);
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
        closeFrom(room.capacity());
        return joined;
    }

    @Override
    public synchronized Room create(String title, int capacity, int seats) {
        return create(title, capacity, seats, RoomMode.SOLO);
    }

    @Override
    public synchronized Room create(String title, int capacity, int seats, RoomMode mode) {
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
        Room room = new Room(number, title.strip(), seats, capacity, "캠프 08", Room.Status.WAITING,
                Objects.requireNonNull(mode));
        add(room);
        joinedRoom = number;
        localSeats = seats;
        closeFrom(capacity);
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
        int limit = GameMaps.byName(map).maxPlayers();
        if (old.players() > limit) {
            throw new IllegalStateException("이 맵은 " + limit + "명까지 입장할 수 있습니다.");
        }
        // 맵 밖으로 열린 자리가 빠져 정원이 2명보다 적어지면 방장이 닫은 자리를 작은 번호부터 연다 (서버와 같다)
        for (int slot = 0; openSlots(map) < 2 && slot < limit; slot++) {
            closedByHost[slot] = false;
        }
        Room updated = new Room(old.number(), old.title(), old.players(), openSlots(map), map, status, old.mode());
        add(updated);
        return updated;
    }

    @Override
    public synchronized List<Integer> closedSlots(Room room) {
        if (joinedRoom == null || joinedRoom != room.number()) {
            return LobbyService.super.closedSlots(room);
        }
        String map = rooms.get(joinedRoom).map();
        return IntStream.range(0, RoomState.SLOTS).filter(slot -> isClosed(slot, map)).boxed().toList();
    }

    @Override
    public synchronized Room setSlotOpen(int slot, boolean open) {
        if (joinedRoom == null || !host) {
            throw new IllegalStateException("방장만 자리를 열고 닫을 수 있습니다.");
        }
        if (slot < 0 || slot >= RoomState.SLOTS) {
            throw new IllegalArgumentException("slot은 0~7이어야 합니다: " + slot);
        }
        Room room = rooms.get(joinedRoom);
        if (room.status() != Room.Status.WAITING) {
            throw new IllegalStateException("게임 중에는 할 수 없습니다.");
        }
        if (open != isClosed(slot, room.map())) {
            return room;
        }
        if (open) {
            GameMaps.GameMap map = GameMaps.byName(room.map());
            if (slot >= map.maxPlayers()) {
                throw new IllegalStateException(
                        "%s은(는) 최대 %d명이라 이 자리는 열 수 없습니다.".formatted(map.name(), map.maxPlayers()));
            }
        } else if (slot < room.players()) {
            throw new IllegalStateException("사람이 있는 자리는 닫을 수 없습니다.");
        } else if (openSlots(room.map()) <= 2) {
            throw new IllegalStateException("정원은 2명보다 줄일 수 없습니다.");
        }
        closedByHost[slot] = !open;
        Room updated = new Room(room.number(), room.title(), room.players(), openSlots(room.map()), room.map(),
                room.status(), room.mode());
        add(updated);
        return updated;
    }

    /** 정원 capacity로 들어가며 capacity번부터 닫는다 */
    private void closeFrom(int capacity) {
        for (int slot = 0; slot < RoomState.SLOTS; slot++) {
            closedByHost[slot] = slot >= capacity;
        }
    }

    private boolean isClosed(int slot, String map) {
        return slot >= GameMaps.byName(map).maxPlayers() || closedByHost[slot];
    }

    /** map에서 열린 자리 수 */
    private int openSlots(String map) {
        return (int) IntStream.range(0, RoomState.SLOTS).filter(slot -> !isClosed(slot, map)).count();
    }

    private Room withPlayers(Room room, int players) {
        return new Room(
                room.number(), room.title(), players, room.capacity(), room.map(), room.status(), room.mode());
    }
}
