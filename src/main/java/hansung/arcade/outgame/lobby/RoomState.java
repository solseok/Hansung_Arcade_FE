package hansung.arcade.outgame.lobby;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.network.Message;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.Team;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * 서버가 알려 준 대기실 한 곳의 상태 (ROOM_STATE, 그리고 ROOM_CREATE·ROOM_JOIN·ROOM_QUICK_JOIN 성공 응답).
 *
 * <pre>
 * {"room":{"number":3,"title":"한 판","capacity":7,"mapId":"camp/camp08","status":"WAITING","closedSlots":[5]},
 *  "hostId":12,
 *  "players":[{"playerId":12,"nickname":"상상부기","slot":0,"character":"sangsangbugi","team":"RED","ready":false}]}
 * </pre>
 *
 * @param room        로비 목록과 같은 모양의 방 (맵은 화면 이름). 정원은 열린 자리 수다
 * @param mapId       방에 고른 맵 id (random 포함)
 * @param hostId      방장 플레이어 번호
 * @param seats       자리 번호 순서
 * @param closedSlots 닫힌 자리 번호 (작은 순서). 방장이 닫은 자리와 맵 최대 인원 밖 자리를 모두 넣는다
 */
public record RoomState(Room room, String mapId, long hostId, List<Seat> seats, List<Integer> closedSlots) {

    /** 서버에서 "랜덤 캐릭터"를 뜻하는 값 */
    public static final String RANDOM_CHARACTER = "random";
    /** 대기실 자리 수 (0~7) */
    public static final int SLOTS = 8;

    public RoomState {
        seats = seats.stream().sorted(Comparator.comparingInt(Seat::slot)).toList();
        closedSlots = closedSlots.stream().sorted().toList();
    }

    /** slot이 닫힌 자리인지 */
    public boolean isClosed(int slot) {
        return closedSlots.contains(slot);
    }

    /**
     * 자리 하나.
     *
     * @param slot      0~7, 화면의 자리 순서
     * @param character 캐릭터 id 또는 {@value #RANDOM_CHARACTER}
     * @param ready     방장(과 같은 컴퓨터의 2P)은 늘 false
     */
    public record Seat(long playerId, String nickname, int slot, String character, Team team, boolean ready) {

        public boolean randomCharacter() {
            return RANDOM_CHARACTER.equals(character);
        }

        /** 고른 캐릭터. 랜덤이면 null */
        public Mascot mascot() {
            return randomCharacter() ? null : Mascot.fromId(character);
        }
    }

    // ── JSON 모양 (서버 RoomMessages와 같은 이름) ──
    private record RoomInfoBody(int number, String title, int capacity, String mapId, String status,
            List<Integer> closedSlots) {
    }

    private record SeatBody(long playerId, String nickname, int slot, String character, String team, boolean ready) {
    }

    private record StateBody(RoomInfoBody room, long hostId, List<SeatBody> players) {
    }

    private record SummaryBody(int number, String title, int players, int capacity, String mapId, String status) {
    }

    private record ListBody(List<SummaryBody> rooms) {
    }

    /** ROOM_STATE body를 읽는다 */
    public static RoomState from(JsonNode body) {
        StateBody state = Message.read(body, StateBody.class);
        List<Seat> seats = state.players() == null ? List.of() : state.players().stream()
                .map(seat -> new Seat(seat.playerId(), seat.nickname(), seat.slot(), seat.character(),
                        Team.valueOf(seat.team()), seat.ready()))
                .toList();
        RoomInfoBody info = state.room();
        Room room = new Room(info.number(), info.title(), seats.size(), info.capacity(), mapName(info.mapId()),
                status(info.status()));
        // closedSlots가 없는 서버는 정원 이상인 자리를 닫힌 자리로 본다
        List<Integer> closed = info.closedSlots() != null ? info.closedSlots()
                : IntStream.range(Math.max(0, info.capacity()), SLOTS).boxed().toList();
        return new RoomState(room, info.mapId(), state.hostId(), seats, closed);
    }

    /** ROOM_LIST 응답과 LOBBY_ROOMS의 body {@code {"rooms":[...]}}를 읽는다 */
    public static List<Room> rooms(JsonNode body) {
        ListBody list = Message.read(body, ListBody.class);
        if (list.rooms() == null) {
            return List.of();
        }
        return list.rooms().stream()
                .map(room -> new Room(room.number(), room.title(), room.players(), room.capacity(),
                        mapName(room.mapId()), status(room.status())))
                .toList();
    }

    /** 방 상태만 바꾼 사본 (서버는 게임을 시작해도 방 사람들에게 ROOM_STATE를 보내지 않아, GAME_STARTED를 받을 때 게임 중으로 둔다) */
    public RoomState withStatus(Room.Status status) {
        Room changed = new Room(room.number(), room.title(), room.players(), room.capacity(), room.map(), status);
        return new RoomState(changed, mapId, hostId, seats, closedSlots);
    }

    /** playerId의 자리. 없으면 null */
    public Seat seatOf(long playerId) {
        return seats.stream().filter(seat -> seat.playerId() == playerId).findFirst().orElse(null);
    }

    private static String mapName(String mapId) {
        try {
            return GameMaps.byId(mapId).name();
        } catch (IllegalArgumentException e) {
            // 클라이언트가 모르는 맵이 생겨도 목록은 보여 준다
            return mapId;
        }
    }

    private static Room.Status status(String status) {
        return Room.Status.valueOf(status.toUpperCase(Locale.ROOT));
    }
}
