package hansung.arcade.outgame.lobby;

import java.util.List;

public interface LobbyService {
    List<Room> rooms();

    Room join(int roomNumber);

    default Room create(String title) {
        return create(title, 8);
    }

    Room create(String title, int capacity);

    boolean isHost();

    Room quickJoin();

    void leave();
}
