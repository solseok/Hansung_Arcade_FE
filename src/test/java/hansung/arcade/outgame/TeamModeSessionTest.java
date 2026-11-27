package hansung.arcade.outgame;

import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.RoomMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 오프라인 팀전: 고를 수 있는 팀은 경기 방식과 맵을 따르고, 고를 수 없는 팀은 빨강(1P)·파랑(2P)으로 옮긴다 */
class TeamModeSessionTest {

    private final LocalPicks picks = LocalPicks.inMemory();
    private OutgameSession session;

    @BeforeEach
    void setUp() {
        picks.saveTeam(0, Team.MINT);
        picks.saveTeam(1, Team.PINK);
        session = new OutgameSession(new MockLobbyService(), picks);
        session.login("부기", "꾸꾸");
    }

    @Test
    void 개인전_방은_8색을_모두_고를_수_있고_기억한_팀을_그대로_쓴다() {
        session.create("한 판", 8, RoomMode.SOLO);

        assertEquals(RoomMode.SOLO, session.mode());
        assertEquals(List.of(Team.values()), session.allowedTeams());
        assertEquals(Team.MINT, session.team(0));
        assertEquals(Team.PINK, session.team(1));
    }

    @Test
    void 팀전_캠프08은_빨강_파랑만_고를_수_있고_기억한_팀을_쓸_수_없으면_1P_빨강_2P_파랑이다() {
        session.create("한 판", 8, RoomMode.TEAM);

        assertEquals(RoomMode.TEAM, session.mode());
        assertEquals(RoomMode.TEAM, session.room().mode());
        assertEquals(List.of(Team.RED, Team.BLUE), session.allowedTeams());
        assertEquals(Team.RED, session.team(0));
        assertEquals(Team.BLUE, session.team(1));
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> session.selectTeam(0, Team.YELLOW));
        assertEquals("캠프 08 팀전에서는 빨강·파랑만 고를 수 있습니다.", refused.getMessage());
        assertEquals(Team.RED, session.team(0), "거절하면 팀이 그대로다");
        assertEquals(Team.MINT, picks.team(0).orElseThrow(), "거절한 팀은 기억하지 않는다");
    }

    @Test
    void 팀전에서_맵을_바꾸면_고를_수_있는_색이_바뀌고_캠프08로_돌아오면_노랑_초록은_빨강_파랑으로_옮긴다() {
        session.create("한 판", 8, RoomMode.TEAM);
        session.selectMap("patrit/patrit14");

        assertEquals(List.of(Team.RED, Team.YELLOW, Team.GREEN, Team.BLUE), session.allowedTeams());
        session.selectTeam(0, Team.YELLOW);
        session.selectTeam(1, Team.GREEN);

        session.selectMap("camp/camp08");

        assertEquals(List.of(Team.RED, Team.BLUE), session.allowedTeams());
        assertEquals(Team.RED, session.team(0));
        assertEquals(Team.BLUE, session.team(1));
        assertEquals(RoomMode.TEAM, session.room().mode(), "맵을 바꿔도 팀전이다");
    }

    @Test
    void 방_밖에서는_개인전처럼_8색을_모두_고를_수_있다() {
        assertEquals(RoomMode.SOLO, session.mode());
        assertEquals(List.of(Team.values()), session.allowedTeams());
    }
}
