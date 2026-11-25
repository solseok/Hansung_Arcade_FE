package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.MockLobbyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1P·2P가 마지막으로 고른 캐릭터·팀을 이 컴퓨터에 기억해 다음에 켜도 이어 쓴다 */
class LocalPicksTest {

    @TempDir
    Path dir;

    @Test
    void 고른_값은_파일에_적혀_다음에_읽어도_그대로다() {
        Path file = dir.resolve("sub/picks.properties");
        LocalPicks picks = LocalPicks.file(file);
        assertEquals(Optional.empty(), picks.character(0));
        assertEquals(Optional.empty(), picks.team(1));

        picks.saveCharacter(0, "kkukku");
        picks.saveCharacter(1, "random");
        picks.saveTeam(1, Team.GREEN);

        assertTrue(Files.exists(file));
        LocalPicks again = LocalPicks.file(file);
        assertEquals(Optional.of("kkukku"), again.character(0));
        assertEquals(Optional.empty(), again.team(0));
        assertEquals(Optional.of("random"), again.character(1));
        assertEquals(Optional.of(Team.GREEN), again.team(1));
    }

    @Test
    void 모르는_값은_고른_적이_없는_것으로_본다() throws IOException {
        Path file = dir.resolve("picks.properties");
        Files.writeString(file, "1p.character=pikachu\n1p.team=blue\n2p.character=sangzzi\n2p.team=PINK\n",
                StandardCharsets.UTF_8);

        LocalPicks picks = LocalPicks.file(file);

        assertEquals(Optional.empty(), picks.character(0));
        assertEquals(Optional.empty(), picks.team(0));
        assertEquals(Optional.of("sangzzi"), picks.character(1));
        assertEquals(Optional.of(Team.PINK), picks.team(1));
    }

    @Test
    void 파일을_쓰지_못해도_이번_실행_동안은_기억한다() throws IOException {
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "디렉터리가 아니라 파일");
        LocalPicks picks = LocalPicks.file(blocked.resolve("picks.properties"));

        picks.saveCharacter(0, "kkokko");

        assertEquals(Optional.of("kkokko"), picks.character(0));
    }

    @Test
    void 같은_컴퓨터의_두_창이_고른_값을_서로_지우지_않는다() {
        Path file = dir.resolve("picks.properties");
        LocalPicks first = LocalPicks.file(file);
        LocalPicks second = LocalPicks.file(file);

        first.saveCharacter(0, "kkokko");
        second.saveTeam(1, Team.GREEN);

        LocalPicks again = LocalPicks.file(file);
        assertEquals(Optional.of("kkokko"), again.character(0));
        assertEquals(Optional.of(Team.GREEN), again.team(1));
        // 창마다 자기가 아는 값만 쓴다 (다른 창이 고른 2P 팀은 첫 창의 2P에 섞이지 않는다)
        assertEquals(Optional.empty(), first.team(1));
    }

    @Test
    void 깨진_파일이나_폴더는_고른_적이_없는_것으로_본다() throws IOException {
        Path broken = dir.resolve("broken.properties");
        Files.writeString(broken, "1p.character=\\uZZZZ\n", StandardCharsets.UTF_8);
        assertEquals(Optional.empty(), LocalPicks.file(broken).character(0));

        Path notUtf8 = dir.resolve("latin.properties");
        Files.write(notUtf8, new byte[]{'1', 'p', '.', 't', 'e', 'a', 'm', '=', (byte) 0xC3, (byte) 0x28});
        assertEquals(Optional.empty(), LocalPicks.file(notUtf8).team(0));

        Path folder = Files.createDirectories(dir.resolve("folder.properties"));
        assertEquals(Optional.empty(), LocalPicks.file(folder).character(0));
    }

    @Test
    void 쓰다_실패하면_임시_파일을_남기지_않는다() throws IOException {
        // 기억 파일 자리에 비어 있지 않은 폴더가 있어 옮기기가 실패한다
        Path target = Files.createDirectories(dir.resolve("picks.properties"));
        Files.writeString(target.resolve("keep"), "x");
        LocalPicks picks = LocalPicks.file(target);

        picks.saveCharacter(0, "kkokko");
        picks.saveTeam(0, Team.PINK);

        assertEquals(Optional.of("kkokko"), picks.character(0));
        try (var files = Files.list(dir)) {
            assertEquals(List.of("picks.properties"), files.map(f -> f.getFileName().toString()).toList());
        }
    }

    @Test
    void 같은_값을_다시_고르면_파일을_다시_쓰지_않는다() throws IOException {
        Path file = dir.resolve("picks.properties");
        LocalPicks picks = LocalPicks.file(file);
        picks.saveTeam(0, Team.MINT);
        Files.delete(file);

        picks.saveTeam(0, Team.MINT);

        assertFalse(Files.exists(file));
    }

    @Test
    void 일P로만_로그인해_골라도_2P가_기억한_값은_그대로다() {
        Path file = dir.resolve("picks.properties");
        LocalPicks before = LocalPicks.file(file);
        before.saveCharacter(1, "kkokko");
        before.saveTeam(1, Team.GREEN);

        OutgameSession solo = new OutgameSession(new MockLobbyService(), LocalPicks.file(file));
        solo.login("부기");
        solo.select(Mascot.SANGZZI);
        solo.selectTeam(Team.PINK);

        LocalPicks after = LocalPicks.file(file);
        assertEquals(Optional.of("sangzzi"), after.character(0));
        assertEquals(Optional.of(Team.PINK), after.team(0));
        assertEquals(Optional.of("kkokko"), after.character(1));
        assertEquals(Optional.of(Team.GREEN), after.team(1));
    }

    @Test
    void 플레이어_번호와_캐릭터는_정해진_것만_받는다() {
        LocalPicks picks = LocalPicks.inMemory();

        assertThrows(IllegalArgumentException.class, () -> picks.saveCharacter(2, "kkukku"));
        assertThrows(IllegalArgumentException.class, () -> picks.saveCharacter(0, "pikachu"));
        assertThrows(IllegalArgumentException.class, () -> picks.team(-1));
    }

    @Test
    void 게임을_다시_켜면_1P_2P가_마지막으로_고른_캐릭터와_팀으로_시작한다() {
        Path file = dir.resolve("picks.properties");
        OutgameSession first = new OutgameSession(new MockLobbyService(), LocalPicks.file(file));
        first.login("부기", "꾸꾸");
        first.selectRandomCharacter(0);
        first.selectTeam(0, Team.PURPLE);
        first.select(1, Mascot.KKOKKO);
        first.selectTeam(1, Team.GREEN);

        OutgameSession next = new OutgameSession(new MockLobbyService(), LocalPicks.file(file));
        next.login("부기", "꾸꾸");

        assertTrue(next.randomCharacter(0));
        assertEquals(Team.PURPLE, next.team(0));
        assertFalse(next.randomCharacter(1));
        assertEquals(Mascot.KKOKKO, next.character(1));
        assertEquals(Team.GREEN, next.team(1));
    }

    @Test
    void 고른_적이_없으면_둘_다_상상부기이고_팀은_1P_빨강_2P_파랑이다() {
        OutgameSession session = new OutgameSession(new MockLobbyService(), LocalPicks.file(dir.resolve("none")));
        session.login("부기", "꾸꾸");

        assertEquals(Mascot.SANGSANGBUGI, session.character(0));
        assertEquals(Mascot.SANGSANGBUGI, session.character(1));
        assertEquals(Team.RED, session.team(0));
        assertEquals(Team.BLUE, session.team(1));
    }
}
