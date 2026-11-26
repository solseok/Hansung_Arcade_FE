package hansung.arcade.outgame.audio;

import hansung.arcade.outgame.LocalProperties;

import java.nio.file.Path;
import java.util.OptionalInt;

/**
 * 이 컴퓨터에 저장하는 소리 설정: 전체 음량(0~{@value Sound#MAX_VOLUME}%).
 * 고른 캐릭터·팀({@link hansung.arcade.outgame.LocalPicks})과 같은 방식으로 ~/.hansung-arcade/settings.properties에 적는다
 * ({@link LocalProperties}). 파일을 읽거나 쓰지 못해도 소리는 그대로 나고, 쓰지 못하면 이번 실행 동안만 기억한다.
 */
public final class SoundSettings {
    /** 전체 음량을 적는 키. 값은 0~100 정수(%) */
    static final String VOLUME = "sound.volume";
    /** 파일 첫 줄 주석. Properties.store는 주석의 한글을 \\u로 바꾸므로 영어로 둔다 */
    private static final String COMMENT = "Hansung Arcade: settings of this computer";

    private final LocalProperties values;

    private SoundSettings(LocalProperties values) {
        this.values = values;
    }

    /** 파일 없이 이번 실행 동안만 기억한다 (테스트, 저장할 곳을 정하기 전) */
    public static SoundSettings inMemory() {
        return new SoundSettings(LocalProperties.inMemory());
    }

    /** file에 기억한다. 있으면 읽어 오고, 없거나 읽지 못하면 저장한 적이 없는 것으로 본다 */
    public static SoundSettings file(Path file) {
        return new SoundSettings(LocalProperties.file(file, COMMENT, "소리 설정"));
    }

    /** 사용자 홈의 기본 파일 (~/.hansung-arcade/settings.properties) */
    public static Path defaultFile() {
        return LocalProperties.defaultFile("settings.properties");
    }

    /** 저장해 둔 전체 음량(%). 저장한 적이 없거나 0~100 정수가 아니면 비어 있다 */
    public OptionalInt volume() {
        String value = values.get(VOLUME);
        if (value == null) {
            return OptionalInt.empty();
        }
        try {
            int percent = Integer.parseInt(value.strip());
            return Sound.validVolume(percent) ? OptionalInt.of(percent) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * 전체 음량을 저장한다.
     *
     * @throws IllegalArgumentException 0~100이 아니면
     */
    public void saveVolume(int percent) {
        Sound.requireVolume(percent);
        values.put(VOLUME, Integer.toString(percent));
    }
}
