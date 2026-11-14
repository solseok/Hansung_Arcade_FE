package hansung.arcade.ingame.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 고를 수 있는 마스코트 캐릭터. {@link #id()}는 resources/sprites/characters/ 의 파일 이름 앞부분이다.
 */
public enum Mascot {
    SANGSANGBUGI("sangsangbugi"),
    KKUKKU("kkukku"),
    KKOKKO("kkokko"),
    SANGZZI("sangzzi"),
    HANSUNGNYANGI("hansungnyangi");

    private final String id;

    Mascot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** 설정값 등에 적힌 id(대소문자 무시)에 해당하는 캐릭터 */
    public static Mascot fromId(String id) {
        String wanted = id.strip().toLowerCase(Locale.ROOT);
        for (Mascot mascot : values()) {
            if (mascot.id.equals(wanted)) {
                return mascot;
            }
        }
        throw new IllegalArgumentException("알 수 없는 캐릭터 '%s'입니다. 가능한 값: %s".formatted(id,
                Arrays.stream(values()).map(Mascot::id).collect(Collectors.joining(", "))));
    }
}
