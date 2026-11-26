package hansung.arcade.outgame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * 이 컴퓨터에 적어 두는 값들(Properties 파일). 게임을 다시 켜도 이어 쓸 값을 둔다
 * (고른 캐릭터·팀 {@link LocalPicks}, 전체 음량 {@link hansung.arcade.outgame.audio.SoundSettings}).
 * 파일은 사용자 홈의 ~/.hansung-arcade 폴더에 둔다 ({@link #defaultFile}).
 *
 * <p>파일을 읽거나 쓰지 못해도 게임은 그대로 한다. 읽지 못하면 비어 있는 것으로 보고, 쓰지 못하면 이번 실행 동안만 기억한다.
 * 같은 컴퓨터에 게임 창을 여럿 띄워도 서로 적은 값을 지우지 않도록, 적을 때마다 파일을 다시 읽어 바뀐 값만 고쳐 쓴다.</p>
 */
public final class LocalProperties {
    private static final Logger log = LoggerFactory.getLogger(LocalProperties.class);

    /** 파일. null이면 이번 실행 동안만 기억한다 */
    private final Path file;
    /** 파일 첫 줄 주석. Properties.store는 주석의 한글을 \\u로 바꾸므로 영어로 둔다 */
    private final String comment;
    /** 로그에 적는 이 파일의 내용 (예: "고른 캐릭터·팀") */
    private final String subject;
    /** 이 창이 아는 값: 시작할 때 읽은 값과 이 창에서 적은 값 */
    private final Properties values;
    /** 쓰기 실패를 이미 경고했는지. 같은 경고를 적을 때마다 되풀이하지 않는다 */
    private boolean warned;

    private LocalProperties(Path file, String comment, String subject, Properties values) {
        this.file = file;
        this.comment = comment;
        this.subject = subject;
        this.values = values;
    }

    /** 파일 없이 이번 실행 동안만 기억한다 (테스트, 설정이 없을 때) */
    public static LocalProperties inMemory() {
        return new LocalProperties(null, "", "", new Properties());
    }

    /**
     * file에 기억한다. 있으면 읽어 오고, 없거나 읽지 못하면 비어 있는 채로 시작한다.
     *
     * @param comment 파일 첫 줄 주석 (영어)
     * @param subject 로그에 적는 이 파일의 내용 (예: "고른 캐릭터·팀")
     */
    public static LocalProperties file(Path file, String comment, String subject) {
        return new LocalProperties(file, comment, subject, read(file, subject));
    }

    /** 사용자 홈의 기본 파일 (~/.hansung-arcade/{name}) */
    public static Path defaultFile(String name) {
        return Path.of(System.getProperty("user.home"), ".hansung-arcade", name);
    }

    /** key에 적어 둔 값. 없으면 null */
    public String get(String key) {
        return values.getProperty(key);
    }

    /** key에 value를 적는다. 이미 같은 값이면 파일을 다시 쓰지 않는다 */
    public void put(String key, String value) {
        if (value.equals(values.setProperty(key, value)) || file == null) {
            return;
        }
        Path temp = null;
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            // 다른 창이 적은 값을 지우지 않게 파일을 다시 읽어 이 값만 바꾼다
            Properties merged = read(file, subject);
            merged.setProperty(key, value);
            temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                merged.store(out, comment);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(temp);
            if (!warned) {
                warned = true;
                log.warn("{}: 파일에 적지 못해 이번 실행 동안만 기억합니다: {}", subject, file, e);
            } else {
                log.debug("{}: 파일에 적지 못했습니다: {} ({})", subject, file, e.toString());
            }
        }
    }

    /** 파일을 읽는다. 없거나 읽지 못하면 비어 있다 */
    private static Properties read(Path file, String subject) {
        Properties properties = new Properties();
        if (!Files.exists(file)) {
            return properties;
        }
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("{}: 파일을 읽지 못해 적어 둔 값이 없는 것으로 봅니다: {}", subject, file, e);
            properties.clear();
        }
        return properties;
    }

    private static void deleteQuietly(Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            log.debug("임시 파일을 지우지 못했습니다: {}", temp, e);
        }
    }
}
