package hansung.arcade.outgame.lobby;

import java.util.*;

public final class RoomOrder {
    private RoomOrder() {}

    public static List<Room> sort(List<Room> rooms) {
        return rooms.stream()
                .sorted(
                        Comparator.comparingInt(
                                        (Room r) ->
                                                r.status() == Room.Status.PLAYING
                                                        ? 2
                                                        : r.canJoin() ? 0 : 1)
                                .thenComparingInt(Room::number))
                .toList();
    }
}
