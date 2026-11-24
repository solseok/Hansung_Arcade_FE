package hansung.arcade.outgame.lobby;

import java.util.List;

/**
 * 방 목록과 입장·퇴장·방 설정 바꾸기. seats는 이 컴퓨터에서 함께 들어가는 사람 수(1P면 1, 2P면 2)다.
 * 오프라인은 {@link MockLobbyService}, 온라인은 {@link ServerLobbyService}다.
 */
public interface LobbyService {
    /** 로그인한 플레이어 번호(1P부터)를 알린다. 다시 로그인하면 들고 있던 목록·방을 잊는다 */
    default void signedIn(List<Long> playerIds) {
    }

    /** 방 목록을 새로 받는다 (로비에 들어갈 때, 새로고침). 목업은 늘 최신이라 할 일이 없다 */
    default void reload() {
    }

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

    /**
     * 방장이 들어가 있는 방의 맵과 상태를 바꾼다. 정원은 처음 정한 정원과 맵 최대 인원 중 작은 값이 된다
     * (빌리지는 6명까지라 8인 방이 6인 방이 되고, 다른 맵으로 바꾸면 다시 8인 방이 된다).
     *
     * @param map 맵 화면 이름 (캠프 08 등, 랜덤 맵 포함)
     * @param status 게임을 시작하면 PLAYING, 대기방으로 돌아오면 WAITING
     * @return 바뀐 방
     * @throws IllegalStateException 방장이 아니거나, 지금 인원이 그 맵 최대 인원보다 많으면
     * @throws IllegalArgumentException 모르는 맵 이름이면
     */
    Room updateRoom(String map, Room.Status status);

    void leave();
}
