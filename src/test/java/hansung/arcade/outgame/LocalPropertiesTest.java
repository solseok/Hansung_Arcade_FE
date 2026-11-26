package hansung.arcade.outgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 이 컴퓨터에 적는 파일: 적을 때 파일을 다시 읽지 못하면 다른 값을 지우지 않게 쓰지 않는다 */
class LocalPropertiesTest {

    @TempDir
    Path dir;

    @Test
    void 적을_때_파일을_다시_읽지_못하면_덮어쓰지_않고_이번_실행_동안만_기억한다() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"), "POSIX 권한이 있어야 하는 테스트");
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "other.key=kept\n", StandardCharsets.UTF_8);
        LocalProperties values = LocalProperties.file(file, "test", "테스트");
        // 다른 창이 적은 뒤 잠깐 읽을 수 없게 되었다
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        assumeFalse(Files.isReadable(file), "관리자 권한이면 권한을 막아도 읽힌다");
        try {
            values.put("sound.volume", "40");

            assertEquals("40", values.get("sound.volume"));
        } finally {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("other.key=kept"), "다른 창이 적은 값을 지웠습니다: " + text);
    }

    @Test
    void 내용이_깨진_파일은_새로_적는다() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "broken=\\uZZZZ\n", StandardCharsets.UTF_8);
        LocalProperties values = LocalProperties.file(file, "test", "테스트");

        values.put("sound.volume", "40");

        assertEquals("40", LocalProperties.file(file, "test", "테스트").get("sound.volume"));
    }
}
