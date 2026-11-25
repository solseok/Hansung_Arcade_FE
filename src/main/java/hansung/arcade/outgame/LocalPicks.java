package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.RoomState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

/**
 * 이 컴퓨터의 1P·2P가 마지막으로 고른 캐릭터와 팀. 파일에 적어 두어 게임을 다시 켜도 이어 쓴다.
 * 1P·2P 자리(번호 0, 1)마다 기억하고, 고른 적이 없으면 비어 있다 (기본값은 {@link OutgameSession}이 정한다).
 *
 * <p>파일을 읽거나 쓰지 못해도 게임은 그대로 한다. 읽지 못하면 고른 적이 없는 것으로 보고, 쓰지 못하면 이번 실행 동안만 기억한다.</p>
 */
public final class LocalPicks {
    private static final Logger log = LoggerFactory.getLogger(LocalPicks.class);

    /** 파일. null이면 이번 실행 동안만 기억한다 */
    private final Path file;
    private final Properties values = new Properties();

    private LocalPicks(Path file) {
        this.file = file;
    }

    /** 파일 없이 이번 실행 동안만 기억한다 (테스트, 설정이 없을 때) */
    public static LocalPicks inMemory() {
        return new LocalPicks(null);
    }

    /** file에 기억한다. 있으면 읽어 오고, 없거나 읽지 못하면 비어 있는 채로 시작한다 */
    public static LocalPicks file(Path file) {
        LocalPicks picks = new LocalPicks(file);
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                picks.values.load(in);
            } catch (IOException | IllegalArgumentException e) {
                log.warn("고른 캐릭터·팀 파일을 읽지 못해 처음 값으로 시작합니다: {}", file, e);
                picks.values.clear();
            }
        }
        return picks;
    }

    /** 사용자 홈의 기본 파일 (~/.hansung-arcade/picks.properties) */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".hansung-arcade", "picks.properties");
    }

    /** local번째(0이 1P) 플레이어가 마지막으로 고른 캐릭터 id. ?(랜덤)면 {@value RoomState#RANDOM_CHARACTER} */
    public Optional<String> character(int local) {
        return Optional.ofNullable(values.getProperty(key(local, "character"))).filter(LocalPicks::knownCharacter);
    }

    /** local번째(0이 1P) 플레이어가 마지막으로 고른 팀 */
    public Optional<Team> team(int local) {
        String name = values.getProperty(key(local, "team"));
        return Arrays.stream(Team.values()).filter(team -> team.name().equals(name)).findFirst();
    }

    /** 캐릭터를 기억한다. id는 캐릭터 id 또는 {@value RoomState#RANDOM_CHARACTER} */
    public void saveCharacter(int local, String id) {
        if (!knownCharacter(id)) {
            throw new IllegalArgumentException("알 수 없는 캐릭터입니다: " + id);
        }
        save(key(local, "character"), id);
    }

    public void saveTeam(int local, Team team) {
        save(key(local, "team"), team.name());
    }

    private void save(String key, String value) {
        if (value.equals(values.setProperty(key, value)) || file == null) {
            return;
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                values.store(out, "1P·2P가 마지막으로 고른 캐릭터·팀");
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("고른 캐릭터·팀을 파일에 적지 못했습니다. 이번 실행 동안만 기억합니다: {}", file, e);
        }
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
