package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.Team;

import java.util.Arrays;
import java.util.List;

/**
 * 방의 경기 방식 (서버 {@code RoomMode}와 같은 이름). 개인전은 팀 8색을 모두 고를 수 있고,
 * 팀전은 빨강·파랑·노랑·초록 4색(캠프 08은 진영이 둘이라 빨강·파랑)만 고를 수 있다.
 */
public enum RoomMode {
    SOLO("개인전"), TEAM("팀전");

    private static final List<Team> TEAM_COLORS = List.of(Team.RED, Team.BLUE, Team.YELLOW, Team.GREEN);
    private static final List<Team> CAMP_TEAM_COLORS = List.of(Team.RED, Team.BLUE);
    /** 진영이 둘(철조망 양쪽)뿐인 맵 */
    private static final String TWO_SIDE_MAP = "camp/camp08";

    private final String label;

    RoomMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * mapId 방에서 고를 수 있는 팀 (Team 순서). 서버가 ROOM_STATE에 teams를 보내지 않을 때와 오프라인에서 쓴다.
     * 랜덤 맵 팀전은 4색이다.
     */
    public List<Team> teamsFor(String mapId) {
        if (this == SOLO) {
            return List.of(Team.values());
        }
        List<Team> allowed = TWO_SIDE_MAP.equals(mapId) ? CAMP_TEAM_COLORS : TEAM_COLORS;
        return Arrays.stream(Team.values()).filter(allowed::contains).toList();
    }

    /** 서버 이름(SOLO·TEAM)에 해당하는 방식. 없거나 모르면 개인전 */
    public static RoomMode parse(String name) {
        for (RoomMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return SOLO;
    }

    @Override
    public String toString() {
        return label;
    }
}
