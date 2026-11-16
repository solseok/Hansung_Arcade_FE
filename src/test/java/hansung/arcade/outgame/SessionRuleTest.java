package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.MockLobbyService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 세션 규칙 가운데 기존 테스트가 묶지 않던 것.
 * 테스트만 보고 만든 최소 구현(캐릭터·팀을 바꿔도 준비가 그대로, 맵을 바꾸면 정원이 맵 최대 인원이 됨,
 * 랜덤 맵·캐릭터를 늘 같은 것으로 정함)을 잡는다.
 */
class SessionRuleTest {

    @Test
    void 캐릭터_팀_랜덤을_바꾸면_준비가_풀린다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        session.join(2);

        session.setReady(true);
        session.select(Mascot.KKUKKU);
        assertFalse(session.ready(), "캐릭터를 바꿨는데 준비가 그대로입니다.");

        session.setReady(true);
        session.selectTeam(Team.values()[2]);
        assertFalse(session.ready(), "팀을 바꿨는데 준비가 그대로입니다.");

        session.setReady(true);
        session.selectRandomCharacter();
        assertFalse(session.ready(), "랜덤 캐릭터로 바꿨는데 준비가 그대로입니다.");
    }

    @Test
    void 맵을_바꿔도_방장이_정한_정원을_넘지_않는다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        session.create("4인 방", 4);

        for (String map : new String[]{"camp/camp08", "patrit/patrit14", "village/village10", GameMaps.RANDOM_ID, "camp/camp08"}) {
            session.selectMap(map);
            assertEquals(4, session.room().capacity(), map + "으로 바꾸자 정원이 바뀌었습니다.");
        }
    }

    @Test
    void 랜덤_맵과_랜덤_캐릭터는_시작할_때마다_무작위로_정한다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        session.create("랜덤 방", 8);
        session.selectMap(GameMaps.RANDOM_ID);
        session.selectRandomCharacter();

        Set<String> maps = new HashSet<>();
        Set<Mascot> characters = new HashSet<>();
        for (int i = 0; i < 60; i++) {
            GameStartInfo start = session.start();
            maps.add(start.mapId());
            characters.add(start.players().getFirst().character());
        }

        Set<String> real = GameMaps.ids().stream().filter(id -> !id.equals(GameMaps.RANDOM_ID)).collect(Collectors.toSet());
        assertTrue(real.containsAll(maps), "실제 맵이 아닌 맵으로 시작했습니다: " + maps);
        assertTrue(maps.size() >= 2, "랜덤 맵이 늘 같은 맵입니다: " + maps);
        assertTrue(characters.size() >= 2, "랜덤 캐릭터가 늘 같은 캐릭터입니다: " + characters);
    }
}
