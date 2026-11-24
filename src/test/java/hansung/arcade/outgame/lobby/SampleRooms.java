package hansung.arcade.outgame.lobby;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스트용 로비 방 목록. 게임의 오프라인 로비는 방 없이 시작하므로, 목록·페이지·입장을 보는 테스트는 이 방들을 넘겨 만든다.
 * 8개씩 네 페이지: 대기방 페이지, 혼합 페이지, 마지막 게임중 페이지다.
 */
public final class SampleRooms {

    private SampleRooms() {
    }

    public static List<Room> rooms() {
        List<Room> rooms = new ArrayList<>(List.of(
                new Room(1, "함께 한 판 해요", 1, 8, "캠프 08", Room.Status.WAITING),
                new Room(2, "빌리지에서 만나요", 2, 6, "빌리지 10", Room.Status.WAITING),
                new Room(3, "마지막 자리까지 완료", 8, 8, "패트릿 14", Room.Status.WAITING),
                new Room(4, "신나는 캠프", 6, 8, "캠프 08", Room.Status.PLAYING),
                new Room(5, "친구들과 대결", 6, 6, "빌리지 10", Room.Status.PLAYING)));
        for (int number = 6; number <= 30; number++) {
            rooms.add(new Room(number, "즐거운 한 판 " + number, 1, 8, "캠프 08",
                    number % 3 == 0 ? Room.Status.PLAYING : Room.Status.WAITING));
        }
        return List.copyOf(rooms);
    }
}
