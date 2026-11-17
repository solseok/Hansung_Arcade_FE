package hansung.arcade.outgame.lobby;

import java.util.List;

public interface LobbyService {
    List<Room> rooms();

    default Room join(int roomNumber) {
        return join(roomNumber, 1);
    }

    Room join(int roomNumber, int seats);

    default Room create(String title) {
        return create(title, 8);
    }

    default Room create(String title, int capacity) {
        return create(title, capacity, 1);
    }

    Room create(String title, int capacity, int seats);

    boolean isHost();

    default Room quickJoin() {
        return quickJoin(1);
    }

    Room quickJoin(int seats);

    Room updateRoom(String map, Room.Status status);

    void leave();
}
