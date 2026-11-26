package hansung.arcade.outgame.audio;

import hansung.arcade.outgame.LocalPicks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 전체 음량을 이 컴퓨터(~/.hansung-arcade/settings.properties)에 저장해 다음에 켜도 쓴다 */
class SoundSettingsTest {

    @TempDir
    Path dir;

    @Test
    void 저장한_음량은_파일에_적혀_다음에_읽어도_그대로다() throws IOException {
        Path file = dir.resolve("sub/settings.properties");
        assertEquals(OptionalInt.empty(), SoundSettings.file(file).volume());

        SoundSettings.file(file).saveVolume(35);

        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("sound.volume=35"));
        assertEquals(OptionalInt.of(35), SoundSettings.file(file).volume());
    }

    @Test
    void 기본_파일은_고른_캐릭터_팀과_같은_폴더의_settings_properties다() {
        assertEquals(LocalPicks.defaultFile().getParent(), SoundSettings.defaultFile().getParent());
        assertEquals("settings.properties", SoundSettings.defaultFile().getFileName().toString());
    }

    @Test
    void 숫자가_아니거나_0에서_100을_벗어난_값은_저장한_적이_없는_것으로_본다() throws IOException {
        Path file = dir.resolve("settings.properties");
        for (String bad : new String[]{"loud", "-1", "101", "50.5", ""}) {
            Files.writeString(file, "sound.volume=" + bad + "\n", StandardCharsets.UTF_8);
            assertEquals(OptionalInt.empty(), SoundSettings.file(file).volume(), bad);
        }
        Files.writeString(file, "sound.volume = 0 \n", StandardCharsets.UTF_8);
        assertEquals(OptionalInt.of(0), SoundSettings.file(file).volume());
    }

    @Test
    void _0에서_100이_아닌_음량은_저장하지_않는다() {
        SoundSettings settings = SoundSettings.inMemory();
        assertThrows(IllegalArgumentException.class, () -> settings.saveVolume(101));
        assertThrows(IllegalArgumentException.class, () -> settings.saveVolume(-5));
        assertEquals(OptionalInt.empty(), settings.volume());
    }

    @Test
    void 파일을_쓰지_못해도_이번_실행_동안은_기억한다() throws IOException {
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "디렉터리가 아니라 파일");
        SoundSettings settings = SoundSettings.file(blocked.resolve("settings.properties"));

        settings.saveVolume(20);

        assertEquals(OptionalInt.of(20), settings.volume());
    }

    @Test
    void 같은_파일에_적힌_다른_값을_지우지_않는다() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "other.key=kept\n", StandardCharsets.UTF_8);

        SoundSettings.file(file).saveVolume(70);

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("other.key=kept") && text.contains("sound.volume=70"), text);
    }
}
