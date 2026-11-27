package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 경기 방식마다 고를 수 있는 팀과 서버 이름 읽기 */
class RoomModeTest {

    @Test
    void 개인전은_어느_맵이든_8색을_모두_고를_수_있다() {
        assertEquals(List.of(Team.values()), RoomMode.SOLO.teamsFor("camp/camp08"));
        assertEquals(List.of(Team.values()), RoomMode.SOLO.teamsFor("patrit/patrit14"));
    }

    @Test
    void 팀전은_빨강_파랑_노랑_초록이고_캠프08은_빨강_파랑뿐이다() {
        assertEquals(List.of(Team.RED, Team.YELLOW, Team.GREEN, Team.BLUE), RoomMode.TEAM.teamsFor("patrit/patrit14"));
        assertEquals(List.of(Team.RED, Team.YELLOW, Team.GREEN, Team.BLUE), RoomMode.TEAM.teamsFor("random"),
                "랜덤 맵은 4색");
        assertEquals(List.of(Team.RED, Team.BLUE), RoomMode.TEAM.teamsFor("camp/camp08"));
    }

    @Test
    void 고를_수_없는_팀_문구는_서버와_같다() {
        assertEquals("캠프 08 팀전에서는 빨강·파랑만 고를 수 있습니다.", RoomMode.TEAM.notAllowedMessage("camp/camp08"));
        assertEquals("팀전에서는 빨강·파랑·노랑·초록만 고를 수 있습니다.", RoomMode.TEAM.notAllowedMessage("random"));
    }

    @Test
    void 서버_이름을_읽고_없거나_모르면_개인전이다() {
        assertEquals(RoomMode.TEAM, RoomMode.parse("TEAM"));
        assertEquals(RoomMode.SOLO, RoomMode.parse("SOLO"));
        assertEquals(RoomMode.SOLO, RoomMode.parse(null));
        assertEquals(RoomMode.SOLO, RoomMode.parse("DUO"));
    }

    @Test
    void 팀전_방_카드는_제목_앞에_팀전을_붙인다() {
        Room team = new Room(1, "한 판", 1, 8, "캠프 08", Room.Status.WAITING, RoomMode.TEAM);
        Room solo = new Room(2, "한 판", 1, 8, "캠프 08", Room.Status.WAITING);

        assertEquals("[팀전] 한 판", RoomCard.titleText(team));
        assertEquals("한 판", RoomCard.titleText(solo));
    }
}
