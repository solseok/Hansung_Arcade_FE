package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.RoomState;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * 이 컴퓨터의 1P·2P가 마지막으로 고른 캐릭터와 팀. 파일에 적어 두어 게임을 다시 켜도 이어 쓴다.
 * 1P·2P 자리(번호 0, 1)마다 기억하고, 고른 적이 없으면 비어 있다 (기본값은 {@link OutgameSession}이 정한다).
 *
 * <p>파일을 읽거나 쓰지 못해도 게임은 그대로 한다. 읽지 못하면 고른 적이 없는 것으로 보고, 쓰지 못하면 이번 실행 동안만 기억한다.
 * 같은 컴퓨터에 게임 창을 여럿 띄워도 서로 적은 값을 지우지 않는다 ({@link LocalProperties}).</p>
 */
public final class LocalPicks {
    /** 파일 첫 줄 주석. Properties.store는 주석의 한글을 \\u로 바꾸므로 영어로 둔다 */
    private static final String COMMENT = "Hansung Arcade: last character and team picked by 1P and 2P";
    private static final String SUBJECT = "고른 캐릭터·팀";

    private final LocalProperties values;

    private LocalPicks(LocalProperties values) {
        this.values = values;
    }

    /** 파일 없이 이번 실행 동안만 기억한다 (테스트, 설정이 없을 때) */
    public static LocalPicks inMemory() {
        return new LocalPicks(LocalProperties.inMemory());
    }

    /** file에 기억한다. 있으면 읽어 오고, 없거나 읽지 못하면 비어 있는 채로 시작한다 */
    public static LocalPicks file(Path file) {
        return new LocalPicks(LocalProperties.file(file, COMMENT, SUBJECT));
    }

    /** 사용자 홈의 기본 파일 (~/.hansung-arcade/picks.properties) */
    public static Path defaultFile() {
        return LocalProperties.defaultFile("picks.properties");
    }

    /** local번째(0이 1P) 플레이어가 마지막으로 고른 캐릭터 id. ?(랜덤)면 {@value RoomState#RANDOM_CHARACTER} */
    public Optional<String> character(int local) {
        return Optional.ofNullable(values.get(key(local, "character"))).filter(LocalPicks::knownCharacter);
    }

    /** local번째(0이 1P) 플레이어가 마지막으로 고른 팀 */
    public Optional<Team> team(int local) {
        String name = values.get(key(local, "team"));
        return Arrays.stream(Team.values()).filter(team -> team.name().equals(name)).findFirst();
    }

    /** 캐릭터를 기억한다. id는 캐릭터 id 또는 {@value RoomState#RANDOM_CHARACTER} */
    public void saveCharacter(int local, String id) {
        if (!knownCharacter(id)) {
            throw new IllegalArgumentException("알 수 없는 캐릭터입니다: " + id);
        }
        values.put(key(local, "character"), id);
    }

    public void saveTeam(int local, Team team) {
        values.put(key(local, "team"), team.name());
    }

    private static String key(int local, String field) {
        if (local < 0 || local >= OutgameSession.MAX_LOCAL_PLAYERS) {
            throw new IllegalArgumentException("이 컴퓨터의 플레이어 번호는 0(1P) 또는 1(2P)입니다: " + local);
        }
        return (local + 1) + "p." + field;
    }

    private static boolean knownCharacter(String id) {
        return RoomState.RANDOM_CHARACTER.equals(id)
                || Arrays.stream(Mascot.values()).anyMatch(mascot -> mascot.id().equals(id));
    }
}
