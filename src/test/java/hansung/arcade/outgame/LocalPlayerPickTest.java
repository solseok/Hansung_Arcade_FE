package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.SampleRooms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 한 컴퓨터 2인(2P)이면 캐릭터·팀(캐릭터 색)을 1P·2P가 따로 고른다 */
class LocalPlayerPickTest {

    private static OutgameSession twoPlayers() {
        OutgameSession session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
        session.login("부기", "꾸꾸");
        session.create("둘이서", 8);
        return session;
    }

    @Test
    void 둘째_플레이어_2P도_상상부기로_시작하고_팀만_1P와_다르다() {
        OutgameSession session = twoPlayers();

        assertEquals(Mascot.SANGSANGBUGI, session.character(0));
        assertEquals(Mascot.SANGSANGBUGI, session.character(1));
        assertNotEquals(session.team(0), session.team(1));
        assertEquals(OutgameSession.DEFAULT_CHARACTER_2P, session.character(1));
        assertEquals(OutgameSession.DEFAULT_TEAM_2P, session.team(1));
    }

    @Test
    void 둘째_플레이어_2P의_캐릭터와_팀을_바꿔도_1P는_그대로다() {
        OutgameSession session = twoPlayers();
        Mascot firstCharacter = session.character(0);
        Team firstTeam = session.team(0);

        session.select(1, Mascot.KKOKKO);
        session.selectTeam(1, Team.GREEN);

        assertEquals(firstCharacter, session.character(0));
        assertEquals(firstTeam, session.team(0));
        assertEquals(Mascot.KKOKKO, session.character(1));
        assertEquals(Team.GREEN, session.team(1));
        // 대기실 자리와 게임 시작 정보에도 각자 고른 것이 들어간다
        var players = session.players();
        assertEquals(firstCharacter, players.get(0).character());
        assertEquals(firstTeam.label(), players.get(0).team());
        assertEquals(Mascot.KKOKKO, players.get(1).character());
        assertEquals(Team.GREEN.label(), players.get(1).team());
        GameStartInfo start = session.start();
        assertEquals(Mascot.KKOKKO, start.players().get(1).character());
        assertEquals(Team.GREEN.label(), start.players().get(1).team());
    }

    @Test
    void 번호_없는_고르기는_1P의_것이다() {
        OutgameSession session = twoPlayers();
        Mascot second = session.character(1);

        session.select(Mascot.SANGZZI);
        session.selectTeam(Team.PURPLE);

        assertEquals(Mascot.SANGZZI, session.character());
        assertEquals(Mascot.SANGZZI, session.character(0));
        assertEquals(Team.PURPLE, session.team(0));
        assertEquals(second, session.character(1));
    }

    @Test
    void 랜덤은_고른_사람만_뽑는다() {
        OutgameSession session = twoPlayers();
        session.select(1, Mascot.KKUKKU);
        session.selectRandomCharacter(0);

        assertTrue(session.randomCharacter(0));
        assertFalse(session.randomCharacter(1));
        for (int i = 0; i < 20; i++) {
            assertEquals(Mascot.KKUKKU, session.start().players().get(1).character());
            session.returnToWaiting();
        }

        // 2P가 캐릭터를 고르면 2P의 랜덤만 풀린다
        session.selectRandomCharacter(1);
        session.select(1, Mascot.SANGZZI);
        assertTrue(session.randomCharacter(0));
        assertFalse(session.randomCharacter(1));
    }

    @Test
    void 둘째_플레이어_2P가_고르면_준비가_풀린다() {
        OutgameSession session = twoPlayers();
        session.setReady(true);
        session.select(1, Mascot.KKOKKO);
        assertFalse(session.ready());

        session.setReady(true);
        session.selectTeam(1, Team.PINK);
        assertFalse(session.ready());
    }

    @Test
    void 혼자_로그인했으면_2P는_고를_수_없다() {
        OutgameSession session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
        session.login("부기");
        session.create("혼자", 8);

        assertThrows(IllegalArgumentException.class, () -> session.select(1, Mascot.KKOKKO));
        assertThrows(IllegalArgumentException.class, () -> session.selectTeam(1, Team.GREEN));
        assertThrows(IllegalArgumentException.class, () -> session.selectRandomCharacter(1));
        assertThrows(IllegalArgumentException.class, () -> session.character(2));
    }
}
