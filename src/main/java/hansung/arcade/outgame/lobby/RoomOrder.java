package hansung.arcade.outgame.lobby;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 로비 방 목록 순서. 탭으로 나누지 않고 한 목록에서 대기 중인 방을 앞에, 게임 중인 방을 뒤에 둔다.
 * 대기 방 중에서도 바로 들어갈 수 있는 방을 맨 앞에 둬서 먼저 들어가게 한다.
 * 들어갈 수 있는 대기 방 → 가득 찬 대기 방 → 게임 중인 방, 같은 무리 안에서는 방 번호 순.
 */
public final class RoomOrder {

    public static final Comparator<Room> ORDER = Comparator
            .comparing((Room room) -> !room.canJoin())
            .thenComparing(room -> room.status() == Room.Status.PLAYING)
            .thenComparingInt(Room::number);

    private RoomOrder() {
    }

    /** 정렬한 새 목록. 원래 목록은 그대로 둔다. */
    public static List<Room> sort(Collection<Room> rooms) {
        return rooms.stream().sorted(ORDER).toList();
    }
}
